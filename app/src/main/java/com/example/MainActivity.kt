package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ThemeMode
import com.example.ui.DnsUiEvent
import com.example.ui.DnsViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.DnsServerTheme

class MainActivity : ComponentActivity() {

  private val viewModel: DnsViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val snackbarHostState = remember { SnackbarHostState() }

      val vpnPermissionLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
          viewModel.onVpnPermissionResult(result.resultCode == Activity.RESULT_OK)
        }

      val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { _ ->
          // Notification permission is optional; VPN tunnel runs regardless
        }

      LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val granted =
            ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
          if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }

      LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
          when (event) {
            is DnsUiEvent.RequestVpnPermission -> vpnPermissionLauncher.launch(event.intent)
            is DnsUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
          }
        }
      }

      val isDarkTheme =
        when (uiState.themeMode) {
          ThemeMode.DARK -> true
          ThemeMode.LIGHT -> false
          ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

      DnsServerTheme(darkTheme = isDarkTheme) {
        HomeScreen(
          uiState = uiState,
          snackbarHostState = snackbarHostState,
          onToggleConnection = viewModel::toggleConnection,
          onSelectProvider = viewModel::selectProvider,
          onRunBenchmark = viewModel::runAllLatencyTests,
          onToggleThemeQuickly = viewModel::toggleThemeQuickly,
          onThemeModeChange = viewModel::setThemeMode,
          onLanguageModeChange = viewModel::setLanguageMode,
          onStrictDoTChange = viewModel::setStrictDoTOnly,
          onAddCustomProvider = viewModel::addCustomProvider,
          onDeleteCustomProvider = viewModel::deleteCustomProvider,
          onLookupDomainChange = viewModel::updateLookupDomainInput,
          onRunLookup = viewModel::runDomainLookup,
        )
      }
    }
  }
}
