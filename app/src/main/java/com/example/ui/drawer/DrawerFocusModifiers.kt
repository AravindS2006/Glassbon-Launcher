package com.example.ui.drawer

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.data.preferences.AlphabetFocusBlur

val DrawerFocusBlurRadiusKey = SemanticsPropertyKey<Float>("DrawerFocusBlurRadiusDp")
var SemanticsPropertyReceiver.drawerFocusBlurRadiusDp by DrawerFocusBlurRadiusKey

val DrawerFocusAlphaKey = SemanticsPropertyKey<Float>("DrawerFocusAlpha")
var SemanticsPropertyReceiver.drawerFocusAlpha by DrawerFocusAlphaKey

private val LeftCenterTransformOrigin = TransformOrigin(0f, 0.5f)

/**
 * Allocates at most one reusable bounded [BlurEffect] for the entire App Drawer so scrolling and
 * alphabet scrubbing never regenerate GPU Gaussian blur kernels or reallocate unbounded FBOs.
 */
@Composable
fun rememberCachedAlphabetBlurEffect(
  blurPreset: AlphabetFocusBlur,
  lowEffectsMode: Boolean
): RenderEffect? {
  val density = LocalDensity.current
  return remember(blurPreset, lowEffectsMode, density) {
    if (
      lowEffectsMode ||
      blurPreset == AlphabetFocusBlur.OFF ||
      Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    ) {
      null
    } else {
      val radiusPx = with(density) { blurPreset.blurRadiusDp.dp.toPx() }
      if (radiusPx > 0.5f) {
        BlurEffect(
          radiusX = radiusPx,
          radiusY = radiusPx,
          edgeTreatment = TileMode.Clamp
        )
      } else {
        null
      }
    }
  }
}

/**
 * Stateless, zero-recomposition GPU modifier that keeps the centered alphabet section crisp and
 * subtly recedes neighboring sections.
 *
 * State reads happen exclusively inside [graphicsLayer] and [semantics]:
 * - During active touch scrub or scroll ([isMovingProvider] == true), Gaussian blur FBO passes
 *   are bypassed so the list slides at native 60-120Hz with zero Skia/HWUI stall.
 * - Once settled in standard mode, attaches the single drawer-wide [cachedBlurEffect].
 * - In Low Power / Low-Effects mode, [cachedBlurEffect] is null so zero offscreen blur FBOs
 *   are ever allocated.
 */
fun Modifier.drawerFocusSpotlight(
  rowSectionKey: String,
  focusedSectionProvider: () -> String?,
  focusBlendProvider: () -> Float,
  isMovingProvider: () -> Boolean,
  blurPreset: AlphabetFocusBlur,
  lowEffectsMode: Boolean,
  cachedBlurEffect: RenderEffect?
): Modifier = this
  .graphicsLayer {
    val focusedSection = focusedSectionProvider()
    val hasFocus = focusedSection != null && blurPreset != AlphabetFocusBlur.OFF
    val isFocused = hasFocus && rowSectionKey == focusedSection
    val isNonFocused = hasFocus && rowSectionKey != focusedSection

    val rawBlend = if (hasFocus) focusBlendProvider().coerceIn(0f, 1f) else 0f
    // Cubic-out easing gives silky physical deceleration with zero CPU overhead
    val inv = 1f - rawBlend
    val easedBlend = 1f - (inv * inv * inv)

    if (isFocused) {
      alpha = 1f
      val liftScale = if (easedBlend <= 0.001f) 1f else lerp(1f, 1.018f, easedBlend)
      scaleX = liftScale
      scaleY = liftScale
      translationX = if (easedBlend <= 0.001f) 0f else lerp(0f, 6.dp.toPx(), easedBlend)
    } else if (isNonFocused) {
      alpha = if (easedBlend <= 0.001f) 1f else lerp(1f, blurPreset.dimAlpha, easedBlend)
      val scale = if (easedBlend <= 0.001f) 1f else lerp(1f, blurPreset.scaleFactor, easedBlend)
      scaleX = scale
      scaleY = scale
      translationX = if (easedBlend <= 0.001f) 0f else lerp(0f, -5.dp.toPx(), easedBlend)
    } else {
      alpha = 1f
      scaleX = 1f
      scaleY = 1f
      translationX = 0f
    }
    transformOrigin = LeftCenterTransformOrigin

    val applyBlur =
      !lowEffectsMode &&
        !isMovingProvider() &&
        cachedBlurEffect != null &&
        isNonFocused &&
        rawBlend >= 0.72f
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      renderEffect = if (applyBlur) cachedBlurEffect else null
    }
  }
  .semantics {
    val focusedSection = focusedSectionProvider()
    val isNonFocused =
      focusedSection != null &&
        blurPreset != AlphabetFocusBlur.OFF &&
        rowSectionKey != focusedSection
    val rawBlend = if (isNonFocused) focusBlendProvider().coerceIn(0f, 1f) else 0f
    val inv = 1f - rawBlend
    val easedBlend = 1f - (inv * inv * inv)
    val applyBlur =
      !lowEffectsMode &&
        !isMovingProvider() &&
        isNonFocused &&
        blurPreset != AlphabetFocusBlur.OFF &&
        rawBlend >= 0.72f

    drawerFocusBlurRadiusDp = if (applyBlur) blurPreset.blurRadiusDp else 0f
    drawerFocusAlpha = if (easedBlend <= 0.001f) 1f else lerp(1f, blurPreset.dimAlpha, easedBlend)
  }
