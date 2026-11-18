package com.example.ui.drawer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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

/**
 * Applies an atmospheric frosted depth-of-field effect to non-focused alphabet sections
 * while keeping the centered section crisp and luminous.
 *
 * On Android 12+ (API 31+) with standard effects enabled, combines GPU RenderEffect blur
 * ([BlurredEdgeTreatment.Unbounded]) with depth alpha and subtle scale recession; falls back
 * gracefully to clean optical fade + scale in Low-Effects mode or older Android releases.
 */
fun Modifier.drawerFocusSpotlight(
  isBlurActive: Boolean,
  isInFocusedSection: Boolean,
  blurPreset: AlphabetFocusBlur,
  reducedMotion: Boolean,
  lowEffectsMode: Boolean
): Modifier = composed {
  val targetBlurAmount = if (isBlurActive && !isInFocusedSection && blurPreset != AlphabetFocusBlur.OFF) {
    1f
  } else {
    0f
  }

  val blurProgress by animateFloatAsState(
    targetValue = targetBlurAmount,
    animationSpec = if (reducedMotion) {
      snap()
    } else {
      tween(durationMillis = 190, easing = FastOutSlowInEasing)
    },
    label = "drawer_focus_blur_progress"
  )

  val targetAlpha = if (blurProgress <= 0.001f) {
    1f
  } else {
    lerp(1f, blurPreset.dimAlpha, blurProgress)
  }
  val targetScale = if (blurProgress <= 0.001f) {
    1f
  } else {
    lerp(1f, blurPreset.scaleFactor, blurProgress)
  }
  val blurRadiusDp = if (!lowEffectsMode && blurProgress > 0.01f) {
    blurPreset.blurRadiusDp * blurProgress
  } else {
    0f
  }

  this
    .then(
      if (blurRadiusDp > 0.1f) {
        Modifier.blur(
          radius = blurRadiusDp.dp,
          edgeTreatment = BlurredEdgeTreatment.Unbounded
        )
      } else {
        Modifier
      }
    )
    .graphicsLayer {
      alpha = targetAlpha
      scaleX = targetScale
      scaleY = targetScale
      transformOrigin = TransformOrigin(0f, 0.5f)
    }
    .semantics {
      drawerFocusBlurRadiusDp = blurRadiusDp
      drawerFocusAlpha = targetAlpha
    }
}
