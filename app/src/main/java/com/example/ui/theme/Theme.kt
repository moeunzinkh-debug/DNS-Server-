package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class DnsExtendedColors(
  val isDark: Boolean,
  val backgroundTop: Color,
  val backgroundMid: Color,
  val backgroundBottom: Color,
  val backgroundGlow: Color,
  val glassCardFill: Color,
  val glassCardElevatedFill: Color,
  val glassCardBorder: Color,
  val iconButtonFill: Color,
  val powerRingConnected: Color,
  val powerGlowConnected: Color,
  val powerRingConnecting: Color,
  val powerRingDisconnected: Color,
  val powerButtonCore: Color,
  val statusConnectedDot: Color,
  val statusErrorDot: Color,
  val hostnameAccent: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textMuted: Color,
)

private val DarkExtendedColors =
  DnsExtendedColors(
    isDark = true,
    backgroundTop = SapphireDeep,
    backgroundMid = SapphireMid,
    backgroundBottom = SapphireOcean,
    backgroundGlow = SapphireMist,
    glassCardFill = DarkGlassSurface,
    glassCardElevatedFill = DarkGlassSurfaceElevated,
    glassCardBorder = DarkGlassBorder,
    iconButtonFill = DarkIconButtonFill,
    powerRingConnected = NeonLimeRing,
    powerGlowConnected = NeonEmerald,
    powerRingConnecting = ConnectingCyan,
    powerRingDisconnected = Color(0xFF476996),
    powerButtonCore = Color(0xFF0A1D3B),
    statusConnectedDot = NeonEmeraldSoft,
    statusErrorDot = ErrorCoral,
    hostnameAccent = DarkHostnameCyan,
    textPrimary = Color.White,
    textSecondary = Color(0xFFE2EEFF),
    textMuted = Color(0xFFB3CCE8),
  )

private val LightExtendedColors =
  DnsExtendedColors(
    isDark = false,
    backgroundTop = SkyTop,
    backgroundMid = SkyMid,
    backgroundBottom = SkyBottom,
    backgroundGlow = SkyGlow,
    glassCardFill = LightGlassSurface,
    glassCardElevatedFill = LightGlassSurfaceElevated,
    glassCardBorder = LightGlassBorder,
    iconButtonFill = LightIconButtonFill,
    powerRingConnected = LightConnectedRing,
    powerGlowConnected = LightConnectedGreen,
    powerRingConnecting = Color(0xFF0284C7),
    powerRingDisconnected = Color(0xFF64748B),
    powerButtonCore = Color(0xFFF8FBFF),
    statusConnectedDot = LightConnectedGreen,
    statusErrorDot = Color(0xFFDC2626),
    hostnameAccent = LightHostnameBlue,
    textPrimary = Color(0xFF0B1F3A),
    textSecondary = Color(0xFF1E3A5F),
    textMuted = Color(0xFF475569),
  )

val LocalDnsColors = staticCompositionLocalOf { DarkExtendedColors }

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF04223B),
    secondary = NeonEmeraldSoft,
    onSecondary = Color(0xFF042E16),
    tertiary = ProviderDnsSbPurple,
    background = SapphireDeep,
    onBackground = Color.White,
    surface = Color(0xFF0D2854),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF163B75),
    onSurfaceVariant = Color(0xFFC8DCF4),
    outline = DarkGlassBorder,
    error = ErrorCoral,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    secondary = LightConnectedGreen,
    onSecondary = Color.White,
    tertiary = ProviderDnsSbPurple,
    background = SkyTop,
    onBackground = Color(0xFF0B1F3A),
    surface = Color.White,
    onSurface = Color(0xFF0B1F3A),
    surfaceVariant = Color(0xFFE2EEFF),
    onSurfaceVariant = Color(0xFF334155),
    outline = LightGlassBorder,
    error = Color(0xFFDC2626),
  )

@Composable
fun DnsServerTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

  CompositionLocalProvider(LocalDnsColors provides extendedColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      content = content,
    )
  }
}
