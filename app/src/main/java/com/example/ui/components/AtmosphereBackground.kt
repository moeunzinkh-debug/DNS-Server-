package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LocalDnsColors

@Composable
fun AtmosphereBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val dnsColors = LocalDnsColors.current
  val baseGradient =
    Brush.verticalGradient(
      colors =
        listOf(
          dnsColors.backgroundTop,
          dnsColors.backgroundMid,
          dnsColors.backgroundBottom,
        )
    )

  Box(modifier = modifier.fillMaxSize().background(baseGradient)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Soft central luminous radial aura behind the hero power button
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                dnsColors.backgroundGlow.copy(alpha = if (dnsColors.isDark) 0.28f else 0.35f),
                Color.Transparent,
              ),
            center = Offset(w * 0.5f, h * 0.38f),
            radius = w * 0.65f,
          ),
        center = Offset(w * 0.5f, h * 0.38f),
        radius = w * 0.65f,
      )

      // Subtle atmospheric bokeh orbs matching the deep-ocean glass aesthetic
      val bokehAlpha = if (dnsColors.isDark) 0.10f else 0.18f
      val orbs =
        listOf(
          Triple(Offset(w * 0.16f, h * 0.24f), w * 0.14f, bokehAlpha * 0.9f),
          Triple(Offset(w * 0.84f, h * 0.20f), w * 0.10f, bokehAlpha * 0.8f),
          Triple(Offset(w * 0.12f, h * 0.54f), w * 0.18f, bokehAlpha * 0.7f),
          Triple(Offset(w * 0.88f, h * 0.48f), w * 0.15f, bokehAlpha * 0.85f),
          Triple(Offset(w * 0.72f, h * 0.82f), w * 0.22f, bokehAlpha * 0.65f),
          Triple(Offset(w * 0.25f, h * 0.78f), w * 0.12f, bokehAlpha * 0.6f),
        )
      orbs.forEach { (center, radius, alpha) ->
        drawCircle(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color.White.copy(alpha = alpha),
                  dnsColors.backgroundGlow.copy(alpha = alpha * 0.4f),
                  Color.Transparent,
                ),
              center = center,
              radius = radius,
            ),
          center = center,
          radius = radius,
        )
      }
    }
    content()
  }
}
