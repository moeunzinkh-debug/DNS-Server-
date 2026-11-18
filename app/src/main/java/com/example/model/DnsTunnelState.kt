package com.example.model

enum class ConnectionStatus {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  ERROR,
}

data class DnsQueryLogEntry(
  val id: Long,
  val domain: String,
  val recordType: String,
  val providerName: String,
  val hostname: String,
  val latencyMs: Long,
  val status: String,
  val timestampMillis: Long,
  val encryptedTls: Boolean,
)

data class DnsProbeResult(
  val providerId: String,
  val latencyMs: Long?,
  val isReachable: Boolean,
  val tlsProtocol: String? = null,
  val errorMessage: String? = null,
  val testedAtMillis: Long = System.currentTimeMillis(),
)

data class DnsLookupTestResult(
  val domain: String,
  val providerHostname: String,
  val answers: List<String>,
  val rcodeName: String,
  val latencyMs: Long,
  val tlsVersion: String?,
  val cipherSuite: String?,
  val error: String? = null,
)
