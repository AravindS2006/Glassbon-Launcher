package com.example.platform

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class GlassbonDeviceAdminReceiver : DeviceAdminReceiver() {

  companion object {
    fun componentName(context: Context): ComponentName {
      return ComponentName(context, GlassbonDeviceAdminReceiver::class.java)
    }

    fun isAdminActive(context: Context): Boolean {
      val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        ?: return false
      return runCatching {
        dpm.isAdminActive(componentName(context))
      }.getOrDefault(false)
    }

    fun lockScreenNow(context: Context): Boolean {
      val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        ?: return false
      return runCatching {
        if (!dpm.isAdminActive(componentName(context))) return false
        dpm.lockNow()
        true
      }.getOrDefault(false)
    }

    fun createEnableAdminIntent(context: Context): Intent {
      return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName(context))
        putExtra(
          DevicePolicyManager.EXTRA_ADD_EXPLANATION,
          "Allows Glassbon's floating power button to lock your screen in one tap. No other device policies are requested."
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }
  }
}
