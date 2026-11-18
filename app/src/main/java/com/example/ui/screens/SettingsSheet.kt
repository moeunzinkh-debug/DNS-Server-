package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.LanguageMode
import com.example.model.ThemeMode
import com.example.ui.DnsUiState
import com.example.ui.theme.DnsTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
  uiState: DnsUiState,
  onThemeModeChange: (ThemeMode) -> Unit,
  onLanguageModeChange: (LanguageMode) -> Unit,
  onStrictDoTChange: (Boolean) -> Unit,
  onAddCustomProvider: (name: String, hostname: String, primaryIp: String, secondaryIp: String) -> Unit,
  onDeleteCustomProvider: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  var showAddCustomDialog by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Text(
        text =
          if (uiState.languageMode == LanguageMode.KHMER) "ការកំណត់ DNS Server"
          else "DNS Server Settings",
        style = MaterialTheme.typography.titleLarge,
      )

      // Language selector
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Language / ភាសា",
          style = MaterialTheme.typography.titleMedium,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = uiState.languageMode == LanguageMode.BILINGUAL,
            onClick = { onLanguageModeChange(LanguageMode.BILINGUAL) },
            label = { Text("ខ្មែរ + EN") },
            modifier = Modifier.testTag("lang_bilingual_chip"),
          )
          FilterChip(
            selected = uiState.languageMode == LanguageMode.KHMER,
            onClick = { onLanguageModeChange(LanguageMode.KHMER) },
            label = { Text("ភាសាខ្មែរ") },
            modifier = Modifier.testTag("lang_khmer_chip"),
          )
          FilterChip(
            selected = uiState.languageMode == LanguageMode.ENGLISH,
            onClick = { onLanguageModeChange(LanguageMode.ENGLISH) },
            label = { Text("English") },
            modifier = Modifier.testTag("lang_english_chip"),
          )
        }
      }

      // Theme selector
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Appearance / រូបរាង",
          style = MaterialTheme.typography.titleMedium,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = uiState.themeMode == ThemeMode.DARK,
            onClick = { onThemeModeChange(ThemeMode.DARK) },
            label = { Text("Sapphire Dark") },
          )
          FilterChip(
            selected = uiState.themeMode == ThemeMode.LIGHT,
            onClick = { onThemeModeChange(ThemeMode.LIGHT) },
            label = { Text("Sky Light") },
          )
          FilterChip(
            selected = uiState.themeMode == ThemeMode.SYSTEM,
            onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
            label = { Text("System") },
          )
        }
      }

      HorizontalDivider()

      // Strict DNS-over-TLS toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
          Text(
            text = "Strict DNS-over-TLS (Port 853)",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text =
              "Require TLS encryption for all queries. Disable only if your Wi-Fi blocks port 853.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = uiState.strictDoTOnly,
          onCheckedChange = onStrictDoTChange,
          modifier = Modifier.testTag("strict_dot_switch"),
        )
      }

      HorizontalDivider()

      // Custom DNS Resolvers
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Custom DNS Providers",
          style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(
          onClick = { showAddCustomDialog = true },
          modifier = Modifier.testTag("add_custom_dns_button"),
        ) {
          Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add")
        }
      }

      val customProviders = uiState.providers.filter { it.isCustom }
      if (customProviders.isEmpty()) {
        Text(
          text = "No custom DNS servers added yet. Tap Add to configure your own DoT server.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          customProviders.forEach { custom ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = custom.name, style = MaterialTheme.typography.labelLarge)
                  Text(
                    text = "${custom.hostname} (${custom.primaryIpv4})",
                    style = DnsTextStyles.monoSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                IconButton(onClick = { onDeleteCustomProvider(custom.id) }) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete ${custom.name}",
                  )
                }
              }
            }
          }
        }
      }

      HorizontalDivider()

      // System Private DNS Settings Shortcut
      OutlinedButton(
        onClick = {
          val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
          runCatching { context.startActivity(intent) }
        },
        modifier = Modifier.fillMaxWidth().testTag("open_android_network_settings"),
      ) {
        Icon(imageVector = Icons.Rounded.OpenInNew, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Open Android Private DNS / Network Settings")
      }
    }
  }

  if (showAddCustomDialog) {
    AddCustomDnsDialog(
      onConfirm = { name, host, ip1, ip2 ->
        onAddCustomProvider(name, host, ip1, ip2)
        showAddCustomDialog = false
      },
      onDismiss = { showAddCustomDialog = false },
    )
  }
}

@Composable
private fun AddCustomDnsDialog(
  onConfirm: (name: String, hostname: String, primaryIp: String, secondaryIp: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var hostname by remember { mutableStateOf("") }
  var primaryIp by remember { mutableStateOf("") }
  var secondaryIp by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Custom DoT Server") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Display Name") },
          placeholder = { Text("e.g. NextDNS") },
          singleLine = true,
        )
        OutlinedTextField(
          value = hostname,
          onValueChange = { hostname = it },
          label = { Text("DoT Hostname (SNI)") },
          placeholder = { Text("e.g. dns.nextdns.io") },
          singleLine = true,
        )
        OutlinedTextField(
          value = primaryIp,
          onValueChange = { primaryIp = it },
          label = { Text("Primary IPv4") },
          placeholder = { Text("e.g. 45.90.28.0") },
          singleLine = true,
        )
        OutlinedTextField(
          value = secondaryIp,
          onValueChange = { secondaryIp = it },
          label = { Text("Secondary IPv4 (optional)") },
          placeholder = { Text("e.g. 45.90.30.0") },
          singleLine = true,
        )
        Spacer(modifier = Modifier.height(2.dp))
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(name, hostname, primaryIp, secondaryIp) },
        enabled = name.isNotBlank() && hostname.isNotBlank() && primaryIp.isNotBlank(),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
