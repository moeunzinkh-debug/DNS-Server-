package com.example.dns

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DefaultDnsProviders
import com.example.data.DnsPreferencesRepository
import com.example.model.DnsProvider
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DnsVpnService : VpnService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val running = AtomicBoolean(false)
  private val activeProviderRef = AtomicReference<DnsProvider>(DefaultDnsProviders.builtInProviders.first())
  private val strictDoTRef = AtomicBoolean(false)
  private val ipIdCounter = AtomicInteger(100)

  private val upstreamClient = DotUpstreamClient()
  private var queryExecutor: ExecutorService? = null
  private var tunInterface: ParcelFileDescriptor? = null
  private var packetLoopJob: Job? = null
  private val writeLock = Any()

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopTunnel()
        return START_NOT_STICKY
      }
      ACTION_SWITCH_PROVIDER -> {
        val providerId = intent.getStringExtra(EXTRA_PROVIDER_ID)
        serviceScope.launch {
          val prefs = DnsPreferencesRepository(applicationContext).preferencesFlow.first()
          val provider =
            DefaultDnsProviders.findById(
              id = providerId ?: prefs.selectedProviderId,
              customProviders = prefs.customProviders,
            )
          strictDoTRef.set(prefs.strictDoTOnly)
          activeProviderRef.set(provider)
          DnsTunnelManager.updateActiveProvider(provider.id)
          if (running.get()) {
            updateForegroundNotification(provider)
          }
        }
        return START_STICKY
      }
      else -> {
        val providerId = intent?.getStringExtra(EXTRA_PROVIDER_ID)
        startTunnel(providerId)
        return START_STICKY
      }
    }
  }

  private fun startTunnel(requestedProviderId: String?) {
    serviceScope.launch {
      val prefs = DnsPreferencesRepository(applicationContext).preferencesFlow.first()
      val provider =
        DefaultDnsProviders.findById(
          id = requestedProviderId ?: prefs.selectedProviderId,
          customProviders = prefs.customProviders,
        )
      strictDoTRef.set(prefs.strictDoTOnly)
      activeProviderRef.set(provider)

      if (running.get()) {
        DnsTunnelManager.updateActiveProvider(provider.id)
        updateForegroundNotification(provider)
        return@launch
      }

      DnsTunnelManager.setConnecting(provider.id)
      startForegroundWithNotification(provider)

      try {
        val builder =
          Builder()
            .setSession("DNS Server • ${provider.hostname}")
            .addAddress(VPN_CLIENT_IP, 32)
            .addDnsServer(VPN_DNS_SERVER_IP)
            .addRoute(VPN_DNS_SERVER_IP, 32)
            .setMtu(1500)

        runCatching { builder.addDisallowedApplication(packageName) }

        val pfd = builder.establish()
        if (pfd == null) {
          DnsTunnelManager.setError("VPN permission required")
          stopSelfCleanly()
          return@launch
        }

        tunInterface = pfd
        running.set(true)
        queryExecutor = Executors.newFixedThreadPool(8)
        DnsTunnelManager.setConnected(provider.id)
        launchPacketLoop(pfd)
      } catch (e: Exception) {
        DnsTunnelManager.setError(e.message ?: "Failed to establish VPN tunnel")
        stopSelfCleanly()
      }
    }
  }

  private fun launchPacketLoop(pfd: ParcelFileDescriptor) {
    packetLoopJob?.cancel()
    packetLoopJob =
      serviceScope.launch {
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteArray(32767)

        while (running.get()) {
          val bytesRead =
            try {
              inputStream.read(buffer)
            } catch (_: Exception) {
              break
            }
          if (bytesRead <= 0) {
            if (bytesRead < 0) break
            continue
          }

          val packet = DnsPacketCodec.parseIpv4UdpPacket(buffer, bytesRead) ?: continue
          if (packet.dstPort != 53) continue

          val executor = queryExecutor ?: continue
          executor.execute {
            handleDnsPacket(packet, outputStream)
          }
        }
      }
  }

  private fun handleDnsPacket(packet: Ipv4UdpPacket, outputStream: FileOutputStream) {
    if (!running.get()) return
    val provider = activeProviderRef.get()
    val queryInfo = DnsPacketCodec.parseQueryInfo(packet.payload)

    try {
      val result =
        upstreamClient.exchange(
          queryBytes = packet.payload,
          provider = provider,
          strictDoTOnly = strictDoTRef.get(),
          protectSocket = { socket -> protect(socket) },
          protectDatagram = { datagram -> protect(datagram) },
        )

      val summary = DnsPacketCodec.parseResponseSummary(result.responseBytes)
      val responsePacket =
        DnsPacketCodec.buildIpv4UdpResponsePacket(
          srcIp = packet.dstIp,
          srcPort = packet.dstPort,
          dstIp = packet.srcIp,
          dstPort = packet.srcPort,
          dnsPayload = result.responseBytes,
          ipId = ipIdCounter.incrementAndGet(),
        )

      synchronized(writeLock) {
        if (running.get()) {
          runCatching { outputStream.write(responsePacket) }
        }
      }

      DnsTunnelManager.recordQuery(
        domain = queryInfo.domain,
        recordType = queryInfo.recordTypeName,
        providerName = provider.name,
        hostname = provider.hostname,
        latencyMs = result.latencyMs,
        status = summary.rcodeName,
        encryptedTls = result.encryptedTls,
      )
    } catch (_: Exception) {
      DnsTunnelManager.recordQuery(
        domain = queryInfo.domain,
        recordType = queryInfo.recordTypeName,
        providerName = provider.name,
        hostname = provider.hostname,
        latencyMs = 0L,
        status = "TIMEOUT",
        encryptedTls = strictDoTRef.get(),
      )
    }
  }

  private fun startForegroundWithNotification(provider: DnsProvider) {
    val notification = buildNotification(provider)
    val fgsType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
      } else {
        0
      }
    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
  }

  private fun updateForegroundNotification(provider: DnsProvider) {
    val manager = getSystemService(NotificationManager::class.java)
    manager?.notify(NOTIFICATION_ID, buildNotification(provider))
  }

  private fun buildNotification(provider: DnsProvider): Notification {
    val launchIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    val stopIntent =
      PendingIntent.getService(
        this,
        1,
        Intent(this, DnsVpnService::class.java).apply { action = ACTION_STOP },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_lock_lock)
      .setContentTitle(getString(R.string.vpn_notification_title))
      .setContentText(getString(R.string.vpn_notification_text, provider.name, provider.hostname))
      .setContentIntent(launchIntent)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .addAction(0, getString(R.string.vpn_action_disconnect), stopIntent)
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          getString(R.string.vpn_notification_channel_name),
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = getString(R.string.vpn_notification_channel_desc)
          setShowBadge(false)
        }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun stopTunnel() {
    DnsTunnelManager.setDisconnected()
    stopSelfCleanly()
  }

  private fun stopSelfCleanly() {
    running.set(false)
    packetLoopJob?.cancel()
    packetLoopJob = null
    runCatching { queryExecutor?.shutdownNow() }
    queryExecutor = null
    runCatching { tunInterface?.close() }
    tunInterface = null
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  override fun onRevoke() {
    stopTunnel()
    super.onRevoke()
  }

  override fun onDestroy() {
    running.set(false)
    packetLoopJob?.cancel()
    runCatching { queryExecutor?.shutdownNow() }
    runCatching { tunInterface?.close() }
    serviceScope.cancel()
    super.onDestroy()
  }

  companion object {
    const val ACTION_START = "com.example.dns.ACTION_START"
    const val ACTION_STOP = "com.example.dns.ACTION_STOP"
    const val ACTION_SWITCH_PROVIDER = "com.example.dns.ACTION_SWITCH_PROVIDER"
    const val EXTRA_PROVIDER_ID = "extra_provider_id"

    private const val CHANNEL_ID = "dns_vpn_channel"
    private const val NOTIFICATION_ID = 4201
    private const val VPN_CLIENT_IP = "10.8.0.2"
    private const val VPN_DNS_SERVER_IP = "10.8.0.1"

    fun start(context: Context, providerId: String) {
      val intent =
        Intent(context, DnsVpnService::class.java).apply {
          action = ACTION_START
          putExtra(EXTRA_PROVIDER_ID, providerId)
        }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun switchProvider(context: Context, providerId: String) {
      val intent =
        Intent(context, DnsVpnService::class.java).apply {
          action = ACTION_SWITCH_PROVIDER
          putExtra(EXTRA_PROVIDER_ID, providerId)
        }
      context.startService(intent)
    }

    fun stop(context: Context) {
      val intent =
        Intent(context, DnsVpnService::class.java).apply {
          action = ACTION_STOP
        }
      context.startService(intent)
    }
  }
}
