package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DnsProbeResult
import com.example.model.DnsProvider
import com.example.model.LanguageMode
import com.example.ui.theme.DnsTextStyles
import com.example.ui.theme.LocalDnsColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderSelectionCard(
  providers: List<DnsProvider>,
  selectedProvider: DnsProvider,
  languageMode: LanguageMode,
  expanded: Boolean,
  isBenchmarking: Boolean,
  probeResults: Map<String, DnsProbeResult>,
  onToggleExpand: () -> Unit,
  onSelectProvider: (DnsProvider) -> Unit,
  onRunBenchmark: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dnsColors = LocalDnsColors.current
  val chevronRotation by
    animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "expandChevron")

  val cardShape = RoundedCornerShape(24.dp)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(cardShape)
        .background(
          brush =
            Brush.verticalGradient(
              colors = listOf(dnsColors.glassCardElevatedFill, dnsColors.glassCardFill)
            )
        )
        .border(width = 1.dp, color = dnsColors.glassCardBorder, shape = cardShape)
        .padding(horizontal = 18.dp, vertical = 16.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          if (languageMode != LanguageMode.KHMER) {
            Text(
              text = stringResource(R.string.select_dns_provider),
              style = MaterialTheme.typography.titleMedium,
              color = dnsColors.textPrimary,
            )
          }
          if (languageMode != LanguageMode.ENGLISH) {
            Text(
              text = stringResource(R.string.select_dns_provider_kh),
              style =
                if (languageMode == LanguageMode.KHMER) {
                  DnsTextStyles.khmerSubtitle.copy(fontWeight = FontWeight.Bold)
                } else {
                  DnsTextStyles.khmerCaption
                },
              color = if (languageMode == LanguageMode.KHMER) dnsColors.textPrimary else dnsColors.textMuted,
            )
          }
        }

        IconButton(
          onClick = onToggleExpand,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("expand_providers_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = stringResource(R.string.cd_expand_providers),
            tint = dnsColors.textPrimary,
            modifier = Modifier.rotate(chevronRotation),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        providers.forEach { provider ->
          val isSelected = provider.id == selectedProvider.id
          val pillColor = Color(provider.accentColorHex)
          ProviderPillChip(
            provider = provider,
            isSelected = isSelected,
            pillColor = pillColor,
            probeResult = probeResults[provider.id],
            onClick = { onSelectProvider(provider) },
          )
        }
      }

      AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
          HorizontalDivider(color = dnsColors.glassCardBorder.copy(alpha = 0.45f))
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                if (languageMode == LanguageMode.KHMER) "ព័ត៌មានលម្អិត និងល្បឿន DNS"
                else "Provider Details & Live DoT Speed",
              style = MaterialTheme.typography.labelLarge,
              color = dnsColors.textSecondary,
            )

            TextButton(
              onClick = onRunBenchmark,
              enabled = !isBenchmarking,
              modifier = Modifier.testTag("benchmark_all_button"),
            ) {
              if (isBenchmarking) {
                CircularProgressIndicator(
                  modifier = Modifier.size(14.dp),
                  strokeWidth = 2.dp,
                  color = dnsColors.hostnameAccent,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (languageMode == LanguageMode.KHMER) "កំពុងវាស់..." else "Testing...",
                  color = dnsColors.hostnameAccent,
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.Speed,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = dnsColors.hostnameAccent,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (languageMode == LanguageMode.KHMER) "វាស់ល្បឿន" else "Test Speed",
                  color = dnsColors.hostnameAccent,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          providers.forEach { provider ->
            val isSelected = provider.id == selectedProvider.id
            val probe = probeResults[provider.id]
            val rowShape = RoundedCornerShape(14.dp)

            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clip(rowShape)
                  .background(
                    if (isSelected) Color(provider.accentColorHex).copy(alpha = 0.18f)
                    else Color.Transparent
                  )
                  .border(
                    width = if (isSelected) 1.dp else 0.5.dp,
                    color =
                      if (isSelected) Color(provider.accentColorHex).copy(alpha = 0.7f)
                      else dnsColors.glassCardBorder.copy(alpha = 0.35f),
                    shape = rowShape,
                  )
                  .clickable { onSelectProvider(provider) }
                  .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier.size(10.dp).clip(CircleShape).background(Color(provider.accentColorHex))
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = provider.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = dnsColors.textPrimary,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = provider.hostname,
                    style = DnsTextStyles.monoSmall,
                    color = dnsColors.hostnameAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                val descriptionText =
                  when (languageMode) {
                    LanguageMode.ENGLISH -> provider.taglineEnglish
                    LanguageMode.KHMER -> provider.taglineKhmer
                    LanguageMode.BILINGUAL -> provider.taglineKhmer
                  }
                Text(
                  text = descriptionText,
                  style =
                    if (languageMode == LanguageMode.ENGLISH) MaterialTheme.typography.bodySmall
                    else DnsTextStyles.khmerCaption,
                  color = dnsColors.textMuted,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              val latencyBadgeText =
                when {
                  probe == null -> provider.primaryIpv4
                  probe.isReachable && probe.latencyMs != null -> "${probe.latencyMs} ms"
                  else -> "Timeout"
                }
              val latencyBadgeColor =
                when {
                  probe == null -> dnsColors.textMuted
                  probe.isReachable -> dnsColors.statusConnectedDot
                  else -> dnsColors.statusErrorDot
                }
              Text(
                text = latencyBadgeText,
                style = DnsTextStyles.monoSmall.copy(fontWeight = FontWeight.SemiBold),
                color = latencyBadgeColor,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProviderPillChip(
  provider: DnsProvider,
  isSelected: Boolean,
  pillColor: Color,
  probeResult: DnsProbeResult?,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = if (isSelected) pillColor else pillColor.copy(alpha = 0.85f),
    contentColor = Color.White,
    tonalElevation = if (isSelected) 6.dp else 0.dp,
    shadowElevation = if (isSelected) 6.dp else 1.dp,
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .border(
          width = if (isSelected) 1.5.dp else 0.5.dp,
          color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.28f),
          shape = CircleShape,
        )
        .testTag("provider_chip_${provider.id}"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = provider.name,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
      )
      if (probeResult?.isReachable == true && probeResult.latencyMs != null && !isSelected) {
        Text(
          text = "${probeResult.latencyMs}ms",
          style = DnsTextStyles.monoSmall,
          color = Color.White.copy(alpha = 0.88f),
        )
      }
      if (isSelected) {
        Box(
          modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.White),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "${provider.name} selected",
            tint = pillColor,
            modifier = Modifier.size(13.dp),
          )
        }
      }
    }
  }
}
