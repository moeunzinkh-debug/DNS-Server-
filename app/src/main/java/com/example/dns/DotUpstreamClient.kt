package com.example.dns

import android.os.SystemClock
import com.example.model.DnsLookupTestResult
import com.example.model.DnsProbeResult
import com.example.model.DnsProvider
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DnsExchangeResult(
  val responseBytes: ByteArray,
  val latencyMs: Long,
  val encryptedTls: Boolean,
  val tlsProtocol: String? = null,
  val cipherSuite: String? = null,
)

class DotUpstreamClient {

  private val txCounter = AtomicInteger(1000)

  fun exchange(
    queryBytes: ByteArray,
    provider: DnsProvider,
    strictDoTOnly: Boolean,
    protectSocket: ((Socket) -> Boolean)? = null,
    protectDatagram: ((DatagramSocket) -> Boolean)? = null,
  ): DnsExchangeResult {
    val startNs = SystemClock.elapsedRealtimeNanos()
    val candidateIps = listOf(provider.primaryIpv4, provider.secondaryIpv4).filter { it.isNotBlank() }

    var lastException: Exception? = null
    for (ip in candidateIps) {
      try {
        return exchangeOverTls(
          queryBytes = queryBytes,
          serverIp = ip,
          tlsHostname = provider.hostname,
          startNs = startNs,
          protectSocket = protectSocket,
        )
      } catch (e: Exception) {
        lastException = e
      }
    }

    if (!strictDoTOnly) {
      for (ip in candidateIps) {
        try {
          return exchangeOverUdp53(
            queryBytes = queryBytes,
            serverIp = ip,
            startNs = startNs,
            protectDatagram = protectDatagram,
          )
        } catch (e: Exception) {
          lastException = e
        }
      }
    }

    throw lastException ?: IllegalStateException("No upstream DNS IP configured for ${provider.name}")
  }

  private fun exchangeOverTls(
    queryBytes: ByteArray,
    serverIp: String,
    tlsHostname: String,
    startNs: Long,
    protectSocket: ((Socket) -> Boolean)?,
  ): DnsExchangeResult {
    val plainSocket = Socket()
    try {
      protectSocket?.invoke(plainSocket)
      plainSocket.tcpNoDelay = true
      plainSocket.soTimeout = TIMEOUT_MS
      plainSocket.connect(InetSocketAddress(serverIp, DOT_PORT), TIMEOUT_MS)

      val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
      val sslSocket =
        factory.createSocket(plainSocket, tlsHostname, DOT_PORT, true) as SSLSocket
      sslSocket.soTimeout = TIMEOUT_MS
      val params = sslSocket.sslParameters
      if (tlsHostname.isNotBlank()) {
        params.serverNames = listOf(SNIHostName(tlsHostname))
        params.endpointIdentificationAlgorithm = "HTTPS"
      }
      sslSocket.sslParameters = params
      sslSocket.startHandshake()

      val dos = DataOutputStream(sslSocket.outputStream)
      dos.writeShort(queryBytes.size)
      dos.write(queryBytes)
      dos.flush()

      val dis = DataInputStream(sslSocket.inputStream)
      val responseLen = dis.readUnsignedShort()
      if (responseLen <= 0 || responseLen > 65535) {
        throw IllegalStateException("Invalid DoT response length: $responseLen")
      }
      val responseBytes = ByteArray(responseLen)
      dis.readFully(responseBytes)

      val latencyMs = maxOf(1L, (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000L)
      val session = sslSocket.session
      return DnsExchangeResult(
        responseBytes = responseBytes,
        latencyMs = latencyMs,
        encryptedTls = true,
        tlsProtocol = session?.protocol,
        cipherSuite = session?.cipherSuite,
      )
    } finally {
      runCatching { plainSocket.close() }
    }
  }

  private fun exchangeOverUdp53(
    queryBytes: ByteArray,
    serverIp: String,
    startNs: Long,
    protectDatagram: ((DatagramSocket) -> Boolean)?,
  ): DnsExchangeResult {
    val socket = DatagramSocket()
    try {
      protectDatagram?.invoke(socket)
      socket.soTimeout = TIMEOUT_MS
      val address = InetAddress.getByName(serverIp)
      val requestPacket = DatagramPacket(queryBytes, queryBytes.size, address, 53)
      socket.send(requestPacket)

      val recvBuffer = ByteArray(4096)
      val recvPacket = DatagramPacket(recvBuffer, recvBuffer.size)
      socket.receive(recvPacket)
      val responseBytes = recvPacket.data.copyOf(recvPacket.length)
      val latencyMs = maxOf(1L, (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000L)
      return DnsExchangeResult(
        responseBytes = responseBytes,
        latencyMs = latencyMs,
        encryptedTls = false,
      )
    } finally {
      runCatching { socket.close() }
    }
  }

  suspend fun probeProvider(provider: DnsProvider): DnsProbeResult =
    withContext(Dispatchers.IO) {
      val txId = txCounter.incrementAndGet() and 0xFFFF
      val query = DnsPacketCodec.buildQueryPacket("example.com", queryType = 1, transactionId = txId)
      try {
        val result =
          exchange(
            queryBytes = query,
            provider = provider,
            strictDoTOnly = true,
          )
        DnsProbeResult(
          providerId = provider.id,
          latencyMs = result.latencyMs,
          isReachable = true,
          tlsProtocol = result.tlsProtocol,
        )
      } catch (e: Exception) {
        DnsProbeResult(
          providerId = provider.id,
          latencyMs = null,
          isReachable = false,
          errorMessage = e.message ?: "Unreachable",
        )
      }
    }

  suspend fun lookupDomain(
    domain: String,
    provider: DnsProvider,
    recordType: Int = 1,
  ): DnsLookupTestResult =
    withContext(Dispatchers.IO) {
      val cleanDomain = domain.trim().trimEnd('.')
      val txId = txCounter.incrementAndGet() and 0xFFFF
      val query = DnsPacketCodec.buildQueryPacket(cleanDomain, queryType = recordType, transactionId = txId)
      try {
        val result =
          exchange(
            queryBytes = query,
            provider = provider,
            strictDoTOnly = true,
          )
        val summary = DnsPacketCodec.parseResponseSummary(result.responseBytes)
        DnsLookupTestResult(
          domain = cleanDomain,
          providerHostname = provider.hostname,
          answers = summary.answers,
          rcodeName = summary.rcodeName,
          latencyMs = result.latencyMs,
          tlsVersion = result.tlsProtocol,
          cipherSuite = result.cipherSuite,
        )
      } catch (e: Exception) {
        DnsLookupTestResult(
          domain = cleanDomain,
          providerHostname = provider.hostname,
          answers = emptyList(),
          rcodeName = "ERROR",
          latencyMs = 0L,
          tlsVersion = null,
          cipherSuite = null,
          error = e.message ?: "TLS lookup failed",
        )
      }
    }

  companion object {
    private const val DOT_PORT = 853
    private const val TIMEOUT_MS = 3500
  }
}
