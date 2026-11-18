package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ConnectionStatus
import com.example.ui.theme.LocalDnsColors

@Composable
fun PowerConnectButton(
  status: ConnectionStatus,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonSize: Dp = 188.dp,
) {
  val dnsColors = LocalDnsColors.current

  val targetRingColor =
    when (status) {
      ConnectionStatus.CONNECTED -> dnsColors.powerRingConnected
      ConnectionStatus.CONNECTING -> dnsColors.powerRingConnecting
      ConnectionStatus.ERROR -> dnsColors.statusErrorDot
      ConnectionStatus.DISCONNECTED -> dnsColors.powerRingDisconnected
    }

  val targetGlowColor =
    when (status) {
      ConnectionStatus.CONNECTED -> dnsColors.powerGlowConnected
      ConnectionStatus.CONNECTING -> dnsColors.powerRingConnecting
      ConnectionStatus.ERROR -> dnsColors.statusErrorDot
      ConnectionStatus.DISCONNECTED -> dnsColors.powerRingDisconnected
    }

  val ringColor by
    animateColorAsState(
      targetValue = targetRingColor,
      animationSpec = spring(stiffness = 300f),
      label = "powerRingColor",
    )
  val glowColor by
    animateColorAsState(
      targetValue = targetGlowColor,
      animationSpec = spring(stiffness = 300f),
      label = "powerGlowColor",
    )

  val infiniteTransition = rememberInfiniteTransition(label = "powerPulse")
  val pulseFactor by
    infiniteTransition.animateFloat(
      initialValue = 0.88f,
      targetValue = 1.0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "pulseFactor",
    )

  val activeGlowAlpha =
    when (status) {
      ConnectionStatus.CONNECTED -> 0.55f * pulseFactor
      ConnectionStatus.CONNECTING -> 0.45f * pulseFactor
      ConnectionStatus.ERROR -> 0.35f
      ConnectionStatus.DISCONNECTED -> 0.14f
    }

  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier.size(buttonSize + 36.dp),
    contentAlignment = Alignment.Center,
  ) {
    // Outer soft radial glow + concentric neon rings
    Canvas(modifier = Modifier.size(buttonSize + 36.dp)) {
      val radius = size.minDimension / 2f
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                glowColor.copy(alpha = activeGlowAlpha),
                glowColor.copy(alpha = activeGlowAlpha * 0.35f),
                Color.Transparent,
              ),
            center = center,
            radius = radius,
          ),
        center = center,
        radius = radius,
      )

      // Outer crisp ring
      drawCircle(
        color = ringColor.copy(alpha = if (status == ConnectionStatus.DISCONNECTED) 0.55f else 0.92f),
        radius = radius * 0.80f,
        style = Stroke(width = 3.5.dp.toPx()),
      )

      // Inner crisp ring
      drawCircle(
        color = ringColor.copy(alpha = if (status == ConnectionStatus.DISCONNECTED) 0.35f else 0.72f),
        radius = radius * 0.67f,
        style = Stroke(width = 2.5.dp.toPx()),
      )
    }

    // Interactive circular core button
    Box(
      modifier =
        Modifier.size(buttonSize * 0.68f)
          .minimumInteractiveComponentSize()
          .clip(CircleShape)
          .background(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    dnsColors.powerButtonCore.copy(alpha = 0.96f),
                    if (dnsColors.isDark) Color(0xFF071327) else Color(0xFFE9F2FF),
                  )
              )
          )
          .border(
            width = 1.5.dp,
            color = ringColor.copy(alpha = 0.65f),
            shape = CircleShape,
          )
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = ringColor),
            role = Role.Button,
            onClick = onClick,
          )
          .testTag("power_connect_button"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.PowerSettingsNew,
        contentDescription = stringResource(R.string.cd_power_button),
        tint =
          if (status == ConnectionStatus.DISCONNECTED) {
            dnsColors.textSecondary.copy(alpha = 0.85f)
          } else {
            ringColor
          },
        modifier = Modifier.size(buttonSize * 0.36f),
      )
    }
  }
}
