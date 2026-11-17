package com.example.platform

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat

class GlassbonMediaListenerService : NotificationListenerService() {

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    // Active media updates are observed directly via MediaSessionManager callbacks.
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    // Active media updates are observed directly via MediaSessionManager callbacks.
  }

  companion object {
    fun isPermissionGranted(context: Context): Boolean {
      return runCatching {
        NotificationManagerCompat.getEnabledListenerPackages(context)
          .contains(context.packageName)
      }.getOrDefault(false)
    }

    fun componentName(context: Context): ComponentName {
      return ComponentName(context, GlassbonMediaListenerService::class.java)
    }
  }
}
