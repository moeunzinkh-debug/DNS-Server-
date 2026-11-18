package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ConnectionStatus
import com.example.model.DnsQueryLogEntry
import com.example.model.LanguageMode
import com.example.ui.DnsUiState
import com.example.ui.theme.DnsTextStyles
import com.example.ui.theme.LocalDnsColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProviderDiagnosticsSheet(
  uiState: DnsUiState,
  onDomainInputChange: (String) -> Unit,
  onRunLookup: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  val dnsColors = LocalDnsColors.current
  val provider = uiState.selectedProvider

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
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Provider Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier =
              Modifier.size(14.dp).clip(CircleShape).background(Color(provider.accentColorHex))
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = provider.name,
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = provider.hostname,
              style = DnsTextStyles.monoHostname,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        IconButton(
          onClick = {
            copyToClipboard(context, "DoT Hostname", provider.hostname)
          },
          modifier = Modifier.testTag("copy_hostname_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = "Copy Private DNS hostname",
          )
        }
      }

      Text(
        text =
          if (uiState.languageMode == LanguageMode.ENGLISH) provider.taglineEnglish
          else "${provider.taglineKhmer} • ${provider.taglineEnglish}",
        style = DnsTextStyles.khmerCaption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      // IP Addresses & Features
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          InfoRow(label = "IPv4 Primary", value = provider.primaryIpv4)
          if (provider.secondaryIpv4.isNotBlank()) {
            InfoRow(label = "IPv4 Secondary", value = provider.secondaryIpv4)
          }
          if (provider.primaryIpv6.isNotBlank()) {
            InfoRow(label = "IPv6 Primary", value = provider.primaryIpv6)
          }
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            provider.features.forEach { feature ->
              Surface(
                shape = CircleShape,
                color = Color(provider.accentColorHex).copy(alpha = 0.16f),
              ) {
                Text(
                  text = feature,
                  style = MaterialTheme.typography.labelMedium,
                  color = Color(provider.accentColorHex),
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
          }
        }
      }

      // Live Tunnel Session Stats
      if (uiState.tunnel.status == ConnectionStatus.CONNECTED) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricTile(
            label = "Duration",
            value = formatDuration(uiState.sessionDurationSeconds),
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            label = "Queries",
            value = uiState.tunnel.totalQueries.toString(),
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            label = "Avg RTT",
            value =
              if (uiState.tunnel.avgLatencyMs > 0) "${uiState.tunnel.avgLatencyMs}ms" else "—",
            modifier = Modifier.weight(1f),
          )
        }
      }

      HorizontalDivider()

      // Live DoT DNS Lookup Tool
      Text(
        text =
          if (uiState.languageMode == LanguageMode.KHMER) "តេស្តដោះស្រាយឈ្មោះដែន (Live DoT)"
          else "Live DNS-over-TLS Lookup Test",
        style = MaterialTheme.typography.titleMedium,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = uiState.lookupDomainInput,
          onValueChange = onDomainInputChange,
          singleLine = true,
          label = { Text("Domain (e.g. google.com)") },
          modifier = Modifier.weight(1f).testTag("lookup_domain_input"),
        )
        Button(
          onClick = onRunLookup,
          enabled = !uiState.isLookingUpDomain && uiState.lookupDomainInput.isNotBlank(),
          modifier = Modifier.height(52.dp).testTag("run_lookup_button"),
        ) {
          if (uiState.isLookingUpDomain) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          } else {
            Icon(
              imageVector = Icons.Rounded.Search,
              contentDescription = "Resolve domain",
            )
          }
        }
      }

      uiState.lookupResult?.let { result ->
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "${result.domain} (${result.rcodeName})",
                style = DnsTextStyles.monoSmall.copy(fontWeight = FontWeight.Bold),
              )
              if (result.error == null) {
                Text(
                  text = "${result.latencyMs} ms",
                  style = DnsTextStyles.monoSmall,
                  color = dnsColors.statusConnectedDot,
                )
              }
            }
            if (result.error != null) {
              Text(
                text = result.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
              )
            } else {
              if (result.answers.isEmpty()) {
                Text(
                  text = "No A records returned (blocked or empty response)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              } else {
                Text(
                  text = result.answers.joinToString(", "),
                  style = DnsTextStyles.monoSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              result.tlsVersion?.let { tls ->
                Text(
                  text = "$tls • ${result.cipherSuite ?: ""}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      HorizontalDivider()

      // Real Intercepted Query Log
      Text(
        text =
          if (uiState.languageMode == LanguageMode.KHMER) "កំណត់ត្រាសំណើ DNS ផ្ទាល់"
          else "Live Tunnel Query Log",
        style = MaterialTheme.typography.titleMedium,
      )

      if (uiState.tunnel.recentLogs.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Shield,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text =
                if (uiState.tunnel.status == ConnectionStatus.CONNECTED) {
                  "Tunnel active — browse or open any app to see real-time encrypted DNS lookups here."
                } else {
                  "Connect the DNS VPN on the main screen to inspect live encrypted DNS queries from your device."
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          uiState.tunnel.recentLogs.take(20).forEach { entry ->
            QueryLogRow(entry = entry)
          }
        }
      }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = DnsTextStyles.monoSmall.copy(fontWeight = FontWeight.Medium),
    )
  }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = value,
        style = DnsTextStyles.monoHostname.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun QueryLogRow(entry: DnsQueryLogEntry) {
  val dnsColors = LocalDnsColors.current
  val timeFormatted = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(entry.timestampMillis))
  val statusColor =
    when (entry.status) {
      "NOERROR" -> dnsColors.statusConnectedDot
      "NXDOMAIN", "REFUSED" -> MaterialTheme.colorScheme.tertiary
      else -> MaterialTheme.colorScheme.error
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .border(
          width = 0.5.dp,
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
          shape = RoundedCornerShape(12.dp),
        )
        .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      if (entry.encryptedTls) {
        Icon(
          imageVector = Icons.Rounded.Lock,
          contentDescription = "Encrypted with TLS",
          tint = dnsColors.statusConnectedDot,
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Column {
        Text(
          text = entry.domain,
          style = DnsTextStyles.monoSmall.copy(fontWeight = FontWeight.Medium),
          maxLines = 1,
        )
        Text(
          text = "$timeFormatted • ${entry.recordType} • ${entry.providerName}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = entry.status,
        style = DnsTextStyles.monoSmall.copy(fontWeight = FontWeight.SemiBold),
        color = statusColor,
      )
      if (entry.latencyMs > 0) {
        Text(
          text = "${entry.latencyMs}ms",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private fun formatDuration(totalSeconds: Long): String {
  val hrs = totalSeconds / 3600
  val mins = (totalSeconds % 3600) / 60
  val secs = totalSeconds % 60
  return if (hrs > 0) {
    String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
  } else {
    String.format(Locale.US, "%02d:%02d", mins, secs)
  }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
  clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
  Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
}
