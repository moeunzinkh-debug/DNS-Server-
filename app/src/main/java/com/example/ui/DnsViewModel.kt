package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultDnsProviders
import com.example.data.DnsPreferences
import com.example.data.DnsPreferencesRepository
import com.example.dns.DnsTunnelManager
import com.example.dns.DnsVpnService
import com.example.dns.DotUpstreamClient
import com.example.dns.TunnelSnapshot
import com.example.model.ConnectionStatus
import com.example.model.DnsLookupTestResult
import com.example.model.DnsProbeResult
import com.example.model.DnsProvider
import com.example.model.LanguageMode
import com.example.model.ThemeMode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DnsUiState(
  val providers: List<DnsProvider> = DefaultDnsProviders.builtInProviders,
  val selectedProvider: DnsProvider = DefaultDnsProviders.builtInProviders.first(),
  val tunnel: TunnelSnapshot = TunnelSnapshot(),
  val themeMode: ThemeMode = ThemeMode.DARK,
  val languageMode: LanguageMode = LanguageMode.BILINGUAL,
  val strictDoTOnly: Boolean = false,
  val probeResults: Map<String, DnsProbeResult> = emptyMap(),
  val isBenchmarking: Boolean = false,
  val sessionDurationSeconds: Long = 0L,
  val lookupDomainInput: String = "google.com",
  val isLookingUpDomain: Boolean = false,
  val lookupResult: DnsLookupTestResult? = null,
)

sealed interface DnsUiEvent {
  data class RequestVpnPermission(val intent: Intent) : DnsUiEvent
  data class ShowMessage(val message: String) : DnsUiEvent
}

class DnsViewModel(application: Application) : AndroidViewModel(application) {

  private val preferencesRepo = DnsPreferencesRepository(application)
  private val upstreamClient = DotUpstreamClient()

  private val probeResults = MutableStateFlow<Map<String, DnsProbeResult>>(emptyMap())
  private val isBenchmarking = MutableStateFlow(false)
  private val lookupDomainInput = MutableStateFlow("google.com")
  private val isLookingUpDomain = MutableStateFlow(false)
  private val lookupResult = MutableStateFlow<DnsLookupTestResult?>(null)

  private val _events = MutableSharedFlow<DnsUiEvent>()
  val events: SharedFlow<DnsUiEvent> = _events.asSharedFlow()

  private val tickerFlow = flow {
    while (true) {
      emit(System.currentTimeMillis())
      delay(1000L)
    }
  }

  private val diagnosticStateFlow =
    combine(probeResults, isBenchmarking, lookupDomainInput, isLookingUpDomain, lookupResult) {
      probes,
      benchmarking,
      domainInput,
      lookingUp,
      lookupRes ->
      DiagnosticState(probes, benchmarking, domainInput, lookingUp, lookupRes)
    }

  val uiState: StateFlow<DnsUiState> =
    combine(
        preferencesRepo.preferencesFlow,
        DnsTunnelManager.state,
        diagnosticStateFlow,
        tickerFlow,
      ) { prefs: DnsPreferences, tunnel: TunnelSnapshot, diag: DiagnosticState, nowMs: Long ->
        val allProviders = DefaultDnsProviders.builtInProviders + prefs.customProviders
        val selected =
          DefaultDnsProviders.findById(prefs.selectedProviderId, prefs.customProviders)
        val durationSec =
          if (tunnel.status == ConnectionStatus.CONNECTED && tunnel.connectedSinceMillis != null) {
            maxOf(0L, (nowMs - tunnel.connectedSinceMillis) / 1000L)
          } else {
            0L
          }
        DnsUiState(
          providers = allProviders,
          selectedProvider = selected,
          tunnel = tunnel,
          themeMode = prefs.themeMode,
          languageMode = prefs.languageMode,
          strictDoTOnly = prefs.strictDoTOnly,
          probeResults = diag.probes,
          isBenchmarking = diag.benchmarking,
          sessionDurationSeconds = durationSec,
          lookupDomainInput = diag.domainInput,
          isLookingUpDomain = diag.lookingUp,
          lookupResult = diag.lookupResult,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DnsUiState(),
      )

  fun selectProvider(provider: DnsProvider) {
    viewModelScope.launch {
      preferencesRepo.setSelectedProviderId(provider.id)
      val status = DnsTunnelManager.state.value.status
      if (status == ConnectionStatus.CONNECTED || status == ConnectionStatus.CONNECTING) {
        DnsVpnService.switchProvider(getApplication(), provider.id)
      }
    }
  }

  fun toggleConnection() {
    val currentStatus = uiState.value.tunnel.status
    if (currentStatus == ConnectionStatus.CONNECTED || currentStatus == ConnectionStatus.CONNECTING) {
      DnsVpnService.stop(getApplication())
      return
    }

    val prepareIntent = VpnService.prepare(getApplication())
    if (prepareIntent != null) {
      viewModelScope.launch { _events.emit(DnsUiEvent.RequestVpnPermission(prepareIntent)) }
    } else {
      DnsVpnService.start(getApplication(), uiState.value.selectedProvider.id)
    }
  }

  fun onVpnPermissionResult(granted: Boolean) {
    if (granted) {
      DnsVpnService.start(getApplication(), uiState.value.selectedProvider.id)
    } else {
      viewModelScope.launch {
        _events.emit(DnsUiEvent.ShowMessage("VPN permission is required to route DNS queries"))
      }
    }
  }

  fun toggleThemeQuickly(currentlyDark: Boolean) {
    viewModelScope.launch {
      val next = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
      preferencesRepo.setThemeMode(next)
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { preferencesRepo.setThemeMode(mode) }
  }

  fun setLanguageMode(mode: LanguageMode) {
    viewModelScope.launch { preferencesRepo.setLanguageMode(mode) }
  }

  fun setStrictDoTOnly(strict: Boolean) {
    viewModelScope.launch {
      preferencesRepo.setStrictDoTOnly(strict)
      val status = DnsTunnelManager.state.value.status
      if (status == ConnectionStatus.CONNECTED) {
        DnsVpnService.switchProvider(getApplication(), uiState.value.selectedProvider.id)
      }
    }
  }

  fun addCustomProvider(name: String, hostname: String, primaryIpv4: String, secondaryIpv4: String) {
    val trimmedName = name.trim()
    val trimmedHost = hostname.trim()
    val trimmedIp = primaryIpv4.trim()
    val trimmedSecondary = secondaryIpv4.trim()
    if (trimmedName.isBlank() || trimmedHost.isBlank() || trimmedIp.isBlank()) {
      viewModelScope.launch {
        _events.emit(DnsUiEvent.ShowMessage("Please provide a name, DoT hostname, and IPv4 address"))
      }
      return
    }
    val provider =
      DnsProvider(
        id = "custom_${System.currentTimeMillis()}",
        name = trimmedName,
        hostname = trimmedHost,
        primaryIpv4 = trimmedIp,
        secondaryIpv4 = trimmedSecondary,
        taglineKhmer = "ម៉ាស៊ីនមេ DNS ផ្ទាល់ខ្លួន",
        taglineEnglish = "Custom DNS-over-TLS resolver",
        features = listOf("Custom", "DNS-over-TLS"),
        accentColorHex = 0xFF0EA5E9,
        isCustom = true,
      )
    viewModelScope.launch {
      preferencesRepo.addCustomProvider(provider)
      if (DnsTunnelManager.state.value.status == ConnectionStatus.CONNECTED) {
        DnsVpnService.switchProvider(getApplication(), provider.id)
      }
    }
  }

  fun deleteCustomProvider(providerId: String) {
    viewModelScope.launch {
      preferencesRepo.deleteCustomProvider(providerId)
      if (DnsTunnelManager.state.value.status == ConnectionStatus.CONNECTED) {
        DnsVpnService.switchProvider(getApplication(), "google")
      }
    }
  }

  fun runAllLatencyTests() {
    if (isBenchmarking.value) return
    val currentProviders = uiState.value.providers
    viewModelScope.launch {
      isBenchmarking.value = true
      try {
        val results =
          currentProviders
            .map { provider -> async { upstreamClient.probeProvider(provider) } }
            .awaitAll()
        probeResults.update { existing ->
          existing.toMutableMap().apply {
            results.forEach { put(it.providerId, it) }
          }
        }
      } finally {
        isBenchmarking.value = false
      }
    }
  }

  fun updateLookupDomainInput(value: String) {
    lookupDomainInput.value = value
  }

  fun runDomainLookup() {
    val domain = lookupDomainInput.value.trim()
    if (domain.isBlank() || isLookingUpDomain.value) return
    val provider = uiState.value.selectedProvider
    viewModelScope.launch {
      isLookingUpDomain.value = true
      try {
        lookupResult.value = upstreamClient.lookupDomain(domain, provider)
      } finally {
        isLookingUpDomain.value = false
      }
    }
  }

  private data class DiagnosticState(
    val probes: Map<String, DnsProbeResult>,
    val benchmarking: Boolean,
    val domainInput: String,
    val lookingUp: Boolean,
    val lookupResult: DnsLookupTestResult?,
  )
}
