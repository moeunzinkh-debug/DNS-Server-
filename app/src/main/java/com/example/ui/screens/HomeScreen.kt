package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ConnectionStatus
import com.example.model.DnsProvider
import com.example.model.LanguageMode
import com.example.model.ThemeMode
import com.example.ui.DnsUiState
import com.example.ui.components.AtmosphereBackground
import com.example.ui.components.CurrentDnsPill
import com.example.ui.components.PowerConnectButton
import com.example.ui.components.ProviderSelectionCard
import com.example.ui.theme.DnsTextStyles
import com.example.ui.theme.LocalDnsColors

@Composable
fun HomeScreen(
  uiState: DnsUiState,
  snackbarHostState: SnackbarHostState,
  onToggleConnection: () -> Unit,
  onSelectProvider: (DnsProvider) -> Unit,
  onRunBenchmark: () -> Unit,
  onToggleThemeQuickly: (Boolean) -> Unit,
  onThemeModeChange: (ThemeMode) -> Unit,
  onLanguageModeChange: (LanguageMode) -> Unit,
  onStrictDoTChange: (Boolean) -> Unit,
  onAddCustomProvider: (name: String, hostname: String, primaryIp: String, secondaryIp: String) -> Unit,
  onDeleteCustomProvider: (String) -> Unit,
  onLookupDomainChange: (String) -> Unit,
  onRunLookup: () -> Unit,
) {
  val dnsColors = LocalDnsColors.current
  var providersExpanded by rememberSaveable { mutableStateOf(false) }
  var showDiagnosticsSheet by rememberSaveable { mutableStateOf(false) }
  var showSettingsSheet by rememberSaveable { mutableStateOf(false) }

  AtmosphereBackground {
    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.navigationBarsPadding(),
        )
      },
    ) { innerPadding ->
      BoxWithConstraints(
        modifier =
          Modifier.fillMaxSize()
            .padding(innerPadding)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
      ) {
        val isWideLayout = maxWidth >= 760.dp

        Column(
          modifier =
            Modifier.fillMaxSize()
              .widthIn(max = if (isWideLayout) 1040.dp else 560.dp)
              .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
          // Top Bar: App Title + Settings + Contrast Theme Toggle
          TopGlassHeader(
            isDark = dnsColors.isDark,
            onOpenSettings = { showSettingsSheet = true },
            onToggleTheme = { onToggleThemeQuickly(dnsColors.isDark) },
          )

          if (isWideLayout) {
            // Adaptive two-column layout for tablets/foldables
            Row(
              modifier = Modifier.fillMaxSize().padding(top = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(28.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                ConnectionStatusHeader(uiState = uiState)
                Spacer(modifier = Modifier.height(24.dp))
                PowerConnectButton(
                  status = uiState.tunnel.status,
                  onClick = onToggleConnection,
                  buttonSize = 204.dp,
                )
              }

              Column(
                modifier =
                  Modifier.weight(1.1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                ProviderSelectionCard(
                  providers = uiState.providers,
                  selectedProvider = uiState.selectedProvider,
                  languageMode = uiState.languageMode,
                  expanded = providersExpanded,
                  isBenchmarking = uiState.isBenchmarking,
                  probeResults = uiState.probeResults,
                  onToggleExpand = { providersExpanded = !providersExpanded },
                  onSelectProvider = onSelectProvider,
                  onRunBenchmark = onRunBenchmark,
                )
                CurrentDnsPill(
                  provider = uiState.selectedProvider,
                  languageMode = uiState.languageMode,
                  onClick = { showDiagnosticsSheet = true },
                )
              }
            }
          } else {
            // Mobile portrait layout faithful to the DNS Server visual composition
            Column(
              modifier =
                Modifier.fillMaxSize()
                  .verticalScroll(rememberScrollState()),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Spacer(modifier = Modifier.height(14.dp))
              ConnectionStatusHeader(uiState = uiState)
              Spacer(modifier = Modifier.height(20.dp))

              PowerConnectButton(
                status = uiState.tunnel.status,
                onClick = onToggleConnection,
                buttonSize = 186.dp,
              )

              Spacer(modifier = Modifier.weight(1f, fill = true))
              Spacer(modifier = Modifier.height(20.dp))

              ProviderSelectionCard(
                providers = uiState.providers,
                selectedProvider = uiState.selectedProvider,
                languageMode = uiState.languageMode,
                expanded = providersExpanded,
                isBenchmarking = uiState.isBenchmarking,
                probeResults = uiState.probeResults,
                onToggleExpand = { providersExpanded = !providersExpanded },
                onSelectProvider = onSelectProvider,
                onRunBenchmark = onRunBenchmark,
              )

              Spacer(modifier = Modifier.height(14.dp))

              CurrentDnsPill(
                provider = uiState.selectedProvider,
                languageMode = uiState.languageMode,
                onClick = { showDiagnosticsSheet = true },
              )

              Spacer(modifier = Modifier.height(8.dp))
            }
          }
        }
      }
    }
  }

  if (showDiagnosticsSheet) {
    ProviderDiagnosticsSheet(
      uiState = uiState,
      onDomainInputChange = onLookupDomainChange,
      onRunLookup = onRunLookup,
      onDismiss = { showDiagnosticsSheet = false },
    )
  }

  if (showSettingsSheet) {
    SettingsSheet(
      uiState = uiState,
      onThemeModeChange = onThemeModeChange,
      onLanguageModeChange = onLanguageModeChange,
      onStrictDoTChange = onStrictDoTChange,
      onAddCustomProvider = onAddCustomProvider,
      onDeleteCustomProvider = onDeleteCustomProvider,
      onDismiss = { showSettingsSheet = false },
    )
  }
}

@Composable
private fun TopGlassHeader(
  isDark: Boolean,
  onOpenSettings: () -> Unit,
  onToggleTheme: () -> Unit,
) {
  val dnsColors = LocalDnsColors.current

  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.app_name),
      style = MaterialTheme.typography.headlineMedium,
      color = dnsColors.textPrimary,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      GlassSquareIconButton(
        onClick = onOpenSettings,
        contentDescription = stringResource(R.string.cd_settings),
        testTag = "settings_button",
      ) {
        Icon(
          imageVector = Icons.Rounded.Settings,
          contentDescription = stringResource(R.string.cd_settings),
          tint = dnsColors.textPrimary,
          modifier = Modifier.size(22.dp),
        )
      }

      GlassSquareIconButton(
        onClick = onToggleTheme,
        contentDescription = stringResource(R.string.cd_toggle_theme),
        testTag = "theme_toggle_button",
      ) {
        Icon(
          imageVector = Icons.Rounded.Contrast,
          contentDescription =
            if (isDark) "Switch to light mode" else "Switch to dark mode",
          tint = dnsColors.textPrimary,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}

@Composable
private fun GlassSquareIconButton(
  onClick: () -> Unit,
  contentDescription: String,
  testTag: String,
  content: @Composable () -> Unit,
) {
  val dnsColors = LocalDnsColors.current
  val shape = RoundedCornerShape(14.dp)

  Box(
    modifier =
      Modifier.size(48.dp)
        .minimumInteractiveComponentSize()
        .clip(shape)
        .background(dnsColors.iconButtonFill)
        .border(width = 1.dp, color = dnsColors.glassCardBorder, shape = shape)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
      content()
    }
  }
}

@Composable
private fun ConnectionStatusHeader(uiState: DnsUiState) {
  val dnsColors = LocalDnsColors.current
  val status = uiState.tunnel.status
  val lang = uiState.languageMode

  val headlineText =
    when (status) {
      ConnectionStatus.CONNECTED -> stringResource(R.string.status_connected)
      ConnectionStatus.CONNECTING -> stringResource(R.string.status_connecting)
      ConnectionStatus.DISCONNECTED -> stringResource(R.string.status_disconnected)
      ConnectionStatus.ERROR -> stringResource(R.string.status_error)
    }

  val khmerHeadline =
    when (status) {
      ConnectionStatus.CONNECTED -> stringResource(R.string.status_connected_kh)
      ConnectionStatus.CONNECTING -> stringResource(R.string.status_connecting_kh)
      ConnectionStatus.DISCONNECTED -> stringResource(R.string.status_disconnected_kh)
      ConnectionStatus.ERROR -> stringResource(R.string.status_error_kh)
    }

  val dotColor =
    when (status) {
      ConnectionStatus.CONNECTED -> dnsColors.statusConnectedDot
      ConnectionStatus.CONNECTING -> dnsColors.powerRingConnecting
      ConnectionStatus.ERROR -> dnsColors.statusErrorDot
      ConnectionStatus.DISCONNECTED -> dnsColors.powerRingDisconnected
    }

  val subtitleEn =
    when (status) {
      ConnectionStatus.CONNECTED -> stringResource(R.string.secured_with_dot)
      ConnectionStatus.CONNECTING -> "Establishing encrypted DoT tunnel..."
      ConnectionStatus.ERROR -> uiState.tunnel.lastError ?: "Check network or VPN permission"
      ConnectionStatus.DISCONNECTED -> stringResource(R.string.tap_to_connect_en)
    }

  val subtitleKh =
    when (status) {
      ConnectionStatus.CONNECTED -> stringResource(R.string.secured_with_dot_kh)
      ConnectionStatus.CONNECTING -> "កំពុងរៀបចំការតភ្ជាប់ DNS over TLS..."
      ConnectionStatus.ERROR -> "សូមពិនិត្យអ៊ីនធឺណិត ឬសិទ្ធិ VPN"
      ConnectionStatus.DISCONNECTED -> stringResource(R.string.tap_to_connect_kh)
    }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
  ) {
    if (lang != LanguageMode.KHMER) {
      Text(
        text = headlineText,
        style = MaterialTheme.typography.headlineLarge,
        color = dnsColors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("connection_status_title"),
      )
    }
    if (lang != LanguageMode.ENGLISH) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = khmerHeadline,
        style =
          if (lang == LanguageMode.KHMER) {
            DnsTextStyles.khmerSubtitle.copy(
              fontWeight = FontWeight.Bold,
              fontSize = MaterialTheme.typography.headlineMedium.fontSize,
            )
          } else {
            DnsTextStyles.khmerSubtitle
          },
        color = if (lang == LanguageMode.KHMER) dnsColors.textPrimary else dnsColors.textSecondary,
        textAlign = TextAlign.Center,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (lang == LanguageMode.KHMER) subtitleKh else subtitleEn,
        style =
          if (lang == LanguageMode.KHMER) DnsTextStyles.khmerCaption
          else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = dnsColors.textSecondary,
        textAlign = TextAlign.Center,
      )
    }

    if (lang == LanguageMode.BILINGUAL) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitleKh,
        style = DnsTextStyles.khmerCaption,
        color = dnsColors.textMuted,
        textAlign = TextAlign.Center,
      )
    }

    AnimatedVisibility(visible = status == ConnectionStatus.CONNECTED) {
      val qCount = uiState.tunnel.totalQueries
      val avgMs = uiState.tunnel.avgLatencyMs
      val providerDesc =
        if (lang == LanguageMode.ENGLISH) uiState.selectedProvider.taglineEnglish
        else uiState.selectedProvider.taglineKhmer
      Text(
        text =
          if (qCount > 0) "$providerDesc • $qCount queries (${avgMs}ms)"
          else providerDesc,
        style =
          if (lang == LanguageMode.ENGLISH) MaterialTheme.typography.labelMedium
          else DnsTextStyles.khmerCaption,
        color = dnsColors.hostnameAccent,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 6.dp),
      )
    }
  }
}
