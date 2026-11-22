package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SnackSemanticColors(
  val success: Color,
  val successContainer: Color,
  val warning: Color,
  val warningContainer: Color,
  val danger: Color,
  val dangerContainer: Color,
  val info: Color,
  val infoContainer: Color,
)

private val LightSemanticColors = SnackSemanticColors(
  success = EmeraldSuccess,
  successContainer = EmeraldSuccessContainer,
  warning = AmberWarning,
  warningContainer = AmberWarningContainer,
  danger = CoralDanger,
  dangerContainer = CoralDangerContainer,
  info = CobaltInfo,
  infoContainer = CobaltInfoContainer,
)

private val DarkSemanticColors = SnackSemanticColors(
  success = DarkEmeraldSuccess,
  successContainer = DarkEmeraldSuccessContainer,
  warning = DarkAmberWarning,
  warningContainer = DarkAmberWarningContainer,
  danger = DarkCoralDanger,
  dangerContainer = DarkCoralDangerContainer,
  info = DarkCobaltInfo,
  infoContainer = DarkCobaltInfoContainer,
)

private val LocalSnackSemanticColors = staticCompositionLocalOf { DarkSemanticColors }

val MaterialTheme.semanticColors: SnackSemanticColors
  @Composable
  @ReadOnlyComposable
  get() = LocalSnackSemanticColors.current

private val DarkColorScheme = darkColorScheme(
  primary = DarkSaffronPrimary,
  onPrimary = SaffronOnContainer,
  primaryContainer = DarkSaffronContainer,
  onPrimaryContainer = SaffronContainer,
  secondary = DarkForestSecondary,
  onSecondary = ForestOnContainer,
  secondaryContainer = DarkForestContainer,
  onSecondaryContainer = ForestContainer,
  tertiary = TurmericContainer,
  onTertiary = TurmericOnContainer,
  tertiaryContainer = Color(0xFF4F3400),
  onTertiaryContainer = TurmericContainer,
  background = DarkBackground,
  onBackground = Color(0xFFECEFE9),
  surface = DarkSurface,
  onSurface = Color(0xFFECEFE9),
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = Color(0xFFB5BDB7),
  outline = DarkOutline,
  outlineVariant = Color(0xFF2E3530),
  surfaceContainerLowest = Color(0xFF0E110F),
  surfaceContainerLow = Color(0xFF161A17),
  surfaceContainer = DarkSurface,
  surfaceContainerHigh = Color(0xFF222824),
  surfaceContainerHighest = Color(0xFF2C332E),
  error = DarkCoralDanger,
  onError = Color(0xFF5D0F0A),
  errorContainer = DarkCoralDangerContainer,
  onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColorScheme = lightColorScheme(
  primary = SaffronPrimary,
  onPrimary = SaffronOnPrimary,
  primaryContainer = SaffronContainer,
  onPrimaryContainer = SaffronOnContainer,
  secondary = ForestSecondary,
  onSecondary = ForestOnSecondary,
  secondaryContainer = ForestContainer,
  onSecondaryContainer = ForestOnContainer,
  tertiary = TurmericTertiary,
  onTertiary = TurmericOnTertiary,
  tertiaryContainer = TurmericContainer,
  onTertiaryContainer = TurmericOnContainer,
  background = WarmIvoryBackground,
  onBackground = DeepSlateInk,
  surface = WarmIvorySurface,
  onSurface = DeepSlateInk,
  surfaceVariant = WarmIvorySurfaceVariant,
  onSurfaceVariant = MutedSlateInk,
  outline = WarmIvoryOutline,
  outlineVariant = WarmIvoryOutline,
  surfaceContainerLowest = WarmIvorySurface,
  surfaceContainerLow = Color(0xFFF7F3EC),
  surfaceContainer = WarmIvorySurfaceVariant,
  surfaceContainerHigh = Color(0xFFECE5DA),
  surfaceContainerHighest = Color(0xFFE5DDD0),
  error = CoralDanger,
  onError = Color.White,
  errorContainer = CoralDangerContainer,
  onErrorContainer = Color(0xFF6B120C),
)

val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
)

@Composable
fun HomelyTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors
  CompositionLocalProvider(LocalSnackSemanticColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = AppShapes,
      content = content,
    )
  }
}
