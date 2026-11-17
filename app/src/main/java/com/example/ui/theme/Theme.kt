package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MistColorScheme = darkColorScheme(
  primary = MistColors.OffWhite,
  onPrimary = MistColors.NearBlack,
  primaryContainer = MistColors.SecondaryCharcoal,
  onPrimaryContainer = MistColors.OffWhite,
  secondary = MistColors.MistGrey,
  onSecondary = MistColors.NearBlack,
  secondaryContainer = MistColors.DeepCharcoal,
  onSecondaryContainer = MistColors.OffWhite,
  tertiary = MistColors.FrostedGrey,
  onTertiary = MistColors.NearBlack,
  background = MistColors.NearBlack,
  onBackground = MistColors.OffWhite,
  surface = MistColors.DeepCharcoal,
  onSurface = MistColors.OffWhite,
  surfaceVariant = MistColors.SecondaryCharcoal,
  onSurfaceVariant = MistColors.MistGrey,
  outline = MistColors.FrostedGrey,
  outlineVariant = MistColors.HairlineDivider
)

val MistShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MistLauncherTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = MistColorScheme,
    typography = MistTypography,
    shapes = MistShapes,
    content = content
  )
}
