package com.example.dns

import com.example.model.ConnectionStatus
import com.example.model.DnsQueryLogEntry
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TunnelSnapshot(
  val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
  val activeProviderId: String? = null,
  val connectedSinceMillis: Long? = null,
  val totalQueries: Long = 0L,
  val blockedQueries: Long = 0L,
  val avgLatencyMs: Long = 0L,
  val recentLogs: List<DnsQueryLogEntry> = emptyList(),
  val lastError: String? = null,
)

object DnsTunnelManager {

  private const val MAX_LOG_ENTRIES = 60
  private val logIdCounter = AtomicLong(0L)
  private val latencyAccumulator = AtomicLong(0L)

  private val _state = MutableStateFlow(TunnelSnapshot())
  val state: StateFlow<TunnelSnapshot> = _state.asStateFlow()

  fun setConnecting(providerId: String) {
    _state.update {
      it.copy(
        status = ConnectionStatus.CONNECTING,
        activeProviderId = providerId,
        lastError = null,
      )
    }
  }

  fun setConnected(providerId: String) {
    latencyAccumulator.set(0L)
    _state.value =
      TunnelSnapshot(
        status = ConnectionStatus.CONNECTED,
        activeProviderId = providerId,
        connectedSinceMillis = System.currentTimeMillis(),
        totalQueries = 0L,
        blockedQueries = 0L,
        avgLatencyMs = 0L,
        recentLogs = emptyList(),
        lastError = null,
      )
  }

  fun updateActiveProvider(providerId: String) {
    _state.update { it.copy(activeProviderId = providerId, lastError = null) }
  }

  fun setDisconnected() {
    _state.update {
      it.copy(
        status = ConnectionStatus.DISCONNECTED,
        connectedSinceMillis = null,
        lastError = null,
      )
    }
  }

  fun setError(message: String) {
    _state.update {
      it.copy(
        status = ConnectionStatus.ERROR,
        connectedSinceMillis = null,
        lastError = message,
      )
    }
  }

  fun recordQuery(
    domain: String,
    recordType: String,
    providerName: String,
    hostname: String,
    latencyMs: Long,
    status: String,
    encryptedTls: Boolean,
  ) {
    val isBlocked =
      status == "NXDOMAIN" || status == "REFUSED" || status == "BLOCKED"
    val totalLat = latencyAccumulator.addAndGet(latencyMs)
    val entry =
      DnsQueryLogEntry(
        id = logIdCounter.incrementAndGet(),
        domain = domain,
        recordType = recordType,
        providerName = providerName,
        hostname = hostname,
        latencyMs = latencyMs,
        status = status,
        timestampMillis = System.currentTimeMillis(),
        encryptedTls = encryptedTls,
      )
    _state.update { current ->
      val newTotal = current.totalQueries + 1
      val newBlocked = current.blockedQueries + if (isBlocked) 1 else 0
      val newAvg = if (newTotal > 0) totalLat / newTotal else 0L
      val updatedLogs = (listOf(entry) + current.recentLogs).take(MAX_LOG_ENTRIES)
      current.copy(
        totalQueries = newTotal,
        blockedQueries = newBlocked,
        avgLatencyMs = newAvg,
        recentLogs = updatedLogs,
      )
    }
  }
}
