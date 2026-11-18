package com.example.core.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import com.example.data.preferences.HapticProfile

enum class MistHapticEvent {
  WAVE_ENGAGE,
  WAVE_LETTER_TICK,
  WAVE_RELEASE,
  TAP_LIGHT,
  CONFIRM,
  LONG_PRESS,
  BOUNDARY
}

@Immutable
class MistHaptics(
  private val enabled: Boolean,
  private val profile: HapticProfile,
  private val view: View?
) {
  fun trigger(event: MistHapticEvent, overrideProfile: HapticProfile = profile) {
    if (!enabled || view == null) return
    val constant = resolveHapticConstant(event, overrideProfile) ?: return
    @Suppress("DEPRECATION")
    view.performHapticFeedback(constant)
  }

  fun preview(targetProfile: HapticProfile) {
    if (view == null) return
    val constant = resolveHapticConstant(MistHapticEvent.CONFIRM, targetProfile) ?: return
    @Suppress("DEPRECATION")
    view.performHapticFeedback(constant)
  }

  companion object {
    val NoOp = MistHaptics(enabled = false, profile = HapticProfile.BALANCED, view = null)

    internal fun resolveHapticConstant(
      event: MistHapticEvent,
      profile: HapticProfile
    ): Int? {
      return when (profile) {
        HapticProfile.SUBTLE -> when (event) {
          MistHapticEvent.WAVE_ENGAGE -> tickOrClock()
          MistHapticEvent.WAVE_LETTER_TICK -> HapticFeedbackConstants.CLOCK_TICK
          MistHapticEvent.WAVE_RELEASE -> null
          MistHapticEvent.TAP_LIGHT -> null
          MistHapticEvent.CONFIRM -> HapticFeedbackConstants.CLOCK_TICK
          MistHapticEvent.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
          MistHapticEvent.BOUNDARY -> HapticFeedbackConstants.CLOCK_TICK
        }

        HapticProfile.BALANCED -> when (event) {
          MistHapticEvent.WAVE_ENGAGE -> gestureStartOrKey()
          MistHapticEvent.WAVE_LETTER_TICK -> segmentFrequentOrClock()
          MistHapticEvent.WAVE_RELEASE -> gestureEndOrRelease()
          MistHapticEvent.TAP_LIGHT -> HapticFeedbackConstants.CLOCK_TICK
          MistHapticEvent.CONFIRM -> confirmOrKey()
          MistHapticEvent.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
          MistHapticEvent.BOUNDARY -> rejectOrLongPress()
        }

        HapticProfile.CRISP -> when (event) {
          MistHapticEvent.WAVE_ENGAGE -> HapticFeedbackConstants.VIRTUAL_KEY
          MistHapticEvent.WAVE_LETTER_TICK -> textMoveOrClock()
          MistHapticEvent.WAVE_RELEASE -> gestureEndOrRelease()
          MistHapticEvent.TAP_LIGHT -> HapticFeedbackConstants.KEYBOARD_TAP
          MistHapticEvent.CONFIRM -> confirmOrActivate()
          MistHapticEvent.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
          MistHapticEvent.BOUNDARY -> rejectOrLongPress()
        }
      }
    }

    private fun tickOrClock(): Int {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
      } else {
        HapticFeedbackConstants.CLOCK_TICK
      }
    }

    private fun segmentFrequentOrClock(): Int {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
      } else {
        HapticFeedbackConstants.CLOCK_TICK
      }
    }

    private fun textMoveOrClock(): Int {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        HapticFeedbackConstants.TEXT_HANDLE_MOVE
      } else {
        HapticFeedbackConstants.CLOCK_TICK
      }
    }

    private fun gestureStartOrKey(): Int {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.GESTURE_START
      } else {
        HapticFeedbackConstants.VIRTUAL_KEY
      }
    }

    private fun gestureEndOrRelease(): Int {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.GESTURE_END
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        HapticFeedbackConstants.VIRTUAL_KEY_RELEASE
      } else {
        HapticFeedbackConstants.CLOCK_TICK
      }
    }

    private fun confirmOrKey(): Int {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
      } else {
        HapticFeedbackConstants.VIRTUAL_KEY
      }
    }

    private fun confirmOrActivate(): Int {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.TOGGLE_ON
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
      } else {
        HapticFeedbackConstants.VIRTUAL_KEY
      }
    }

    private fun rejectOrLongPress(): Int {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.REJECT
      } else {
        HapticFeedbackConstants.LONG_PRESS
      }
    }
  }
}

val LocalMistHaptics = staticCompositionLocalOf { MistHaptics.NoOp }

@Composable
fun rememberMistHaptics(
  enabled: Boolean,
  profile: HapticProfile
): MistHaptics {
  val view = LocalView.current
  return remember(enabled, profile, view) {
    MistHaptics(enabled = enabled, profile = profile, view = view)
  }
}
