package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DnsProvider
import com.example.model.LanguageMode
import com.example.ui.theme.DnsTextStyles
import com.example.ui.theme.LocalDnsColors

@Composable
fun CurrentDnsPill(
  provider: DnsProvider,
  languageMode: LanguageMode,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dnsColors = LocalDnsColors.current
  val pillShape = RoundedCornerShape(32.dp)

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clip(pillShape)
        .background(
          brush =
            Brush.horizontalGradient(
              colors = listOf(dnsColors.glassCardElevatedFill, dnsColors.glassCardFill)
            )
        )
        .border(width = 1.dp, color = dnsColors.glassCardBorder, shape = pillShape)
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .testTag("current_dns_pill"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      Box(
        modifier =
          Modifier.size(40.dp)
            .clip(CircleShape)
            .background(
              if (dnsColors.isDark) Color.White.copy(alpha = 0.92f)
              else Color(provider.accentColorHex).copy(alpha = 0.14f)
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Public,
          contentDescription = null,
          tint = Color(provider.accentColorHex),
          modifier = Modifier.size(22.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column {
        if (languageMode != LanguageMode.KHMER) {
          Text(
            text = stringResource(R.string.current_dns),
            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.sp),
            color = dnsColors.textPrimary,
          )
        }
        if (languageMode != LanguageMode.ENGLISH) {
          Text(
            text = stringResource(R.string.current_dns_kh),
            style =
              if (languageMode == LanguageMode.KHMER) {
                DnsTextStyles.khmerSubtitle.copy(fontWeight = FontWeight.SemiBold)
              } else {
                DnsTextStyles.khmerCaption
              },
            color = if (languageMode == LanguageMode.KHMER) dnsColors.textPrimary else dnsColors.textMuted,
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = provider.hostname,
        style = DnsTextStyles.monoHostname.copy(fontWeight = FontWeight.SemiBold),
        color = dnsColors.hostnameAccent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = stringResource(R.string.cd_open_diagnostics),
        tint = dnsColors.textSecondary,
        modifier = Modifier.size(22.dp),
      )
    }
  }
}
