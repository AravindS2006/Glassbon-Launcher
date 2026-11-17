package com.example.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import java.lang.ref.WeakReference

class GlassbonLockAccessibilityService : AccessibilityService() {

  override fun onServiceConnected() {
    super.onServiceConnected()
    serviceRef = WeakReference(this)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // No window content or event observation required; used solely for GLOBAL_ACTION_LOCK_SCREEN.
  }

  override fun onInterrupt() {
    // No-op
  }

  override fun onUnbind(intent: Intent?): Boolean {
    serviceRef.clear()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    serviceRef.clear()
    super.onDestroy()
  }

  companion object {
    @Volatile
    private var serviceRef: WeakReference<GlassbonLockAccessibilityService> = WeakReference(null)

    fun isServiceEnabled(context: Context): Boolean {
      if (serviceRef.get() != null) return true
      val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        ?: return false
      val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
      val expectedId = "${context.packageName}/${GlassbonLockAccessibilityService::class.java.name}"
      val shortExpectedId = "${context.packageName}/.platform.GlassbonLockAccessibilityService"
      if (enabledServices.any {
          val id = it.id ?: ""
          id.equals(expectedId, ignoreCase = true) || id.equals(shortExpectedId, ignoreCase = true)
        }
      ) {
        return true
      }
      val settingValue = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
      }.getOrNull().orEmpty()
      return settingValue.contains(context.packageName, ignoreCase = true) &&
        settingValue.contains("GlassbonLockAccessibilityService", ignoreCase = true)
    }

    fun lockScreenNow(): Boolean {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
      val service = serviceRef.get() ?: return false
      return runCatching {
        service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
      }.getOrDefault(false)
    }
  }
}
