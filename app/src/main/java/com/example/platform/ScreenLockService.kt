package com.example.platform

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Minimal-privilege AccessibilityService used exclusively to execute
 * [GLOBAL_ACTION_LOCK_SCREEN] when the user taps Glassbon's Lock button.
 *
 * `canRetrieveWindowContent` and `accessibilityEventTypes` are both disabled
 * in XML so the service reads zero screen content and consumes zero idle CPU/battery.
 */
class ScreenLockAccessibilityService : AccessibilityService() {

  override fun onServiceConnected() {
    super.onServiceConnected()
    activeInstance = WeakReference(this)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

  override fun onInterrupt() = Unit

  override fun onUnbind(intent: Intent?): Boolean {
    activeInstance = null
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    activeInstance = null
    super.onDestroy()
  }

  companion object {
    @Volatile
    private var activeInstance: WeakReference<ScreenLockAccessibilityService>? = null

    fun isRunning(): Boolean = activeInstance?.get() != null

    fun lockScreenNow(): Boolean {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
      val service = activeInstance?.get() ?: return false
      return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }
  }
}

object ScreenLockHelper {

  fun isAccessibilityServiceEnabled(context: Context): Boolean {
    if (ScreenLockAccessibilityService.isRunning()) return true
    val accessibilityOn = try {
      Settings.Secure.getInt(
        context.contentResolver,
        Settings.Secure.ACCESSIBILITY_ENABLED,
        0
      ) == 1
    } catch (_: Throwable) {
      false
    }
    if (!accessibilityOn) return false

    val enabledServices = try {
      Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
      ).orEmpty()
    } catch (_: Throwable) {
      ""
    }
    if (enabledServices.isBlank()) return false

    val expected = ComponentName(context, ScreenLockAccessibilityService::class.java)
    val fullName = expected.flattenToString()
    val shortName = expected.flattenToShortString()
    return enabledServices.split(':').any { entry ->
      entry.equals(fullName, ignoreCase = true) || entry.equals(shortName, ignoreCase = true)
    }
  }

  fun lockScreen(): Boolean {
    return ScreenLockAccessibilityService.lockScreenNow()
  }

  fun openAccessibilitySettings(context: Context): Boolean {
    val needsNewTask = context !is Activity
    val candidates = listOf(
      Settings.ACTION_ACCESSIBILITY_SETTINGS,
      Settings.ACTION_SECURITY_SETTINGS,
      Settings.ACTION_SETTINGS
    )
    for (action in candidates) {
      val intent = Intent(action).apply {
        if (needsNewTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      val started = try {
        context.startActivity(intent)
        true
      } catch (_: Throwable) {
        false
      }
      if (started) return true
    }
    return false
  }
}
