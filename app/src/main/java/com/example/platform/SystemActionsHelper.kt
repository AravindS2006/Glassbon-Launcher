package com.example.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings

object SystemActionsHelper {

  fun openAppInfo(context: Context, packageName: String): Boolean {
    return try {
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (_: ActivityNotFoundException) {
      false
    } catch (_: SecurityException) {
      false
    }
  }

  fun requestUninstall(context: Context, packageName: String): Boolean {
    return openAppInfo(context, packageName)
  }

  fun openDeviceSettings(context: Context): Boolean {
    return try {
      val intent = Intent(Settings.ACTION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (_: ActivityNotFoundException) {
      false
    }
  }

  fun openLockScreenSettings(context: Context): Boolean {
    val candidates = listOf(
      Settings.ACTION_SECURITY_SETTINGS,
      Settings.ACTION_DISPLAY_SETTINGS,
      Settings.ACTION_SETTINGS
    )
    val needsNewTask = context !is android.app.Activity
    for (action in candidates) {
      val intent = Intent(action).apply {
        if (needsNewTask) {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      }
      if (tryStartActivity(context, intent)) return true
    }
    return false
  }

  fun openClockApp(context: Context): Boolean {
    val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (tryStartActivity(context, alarmIntent)) return true

    val timersIntent = Intent(AlarmClock.ACTION_SHOW_TIMERS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (tryStartActivity(context, timersIntent)) return true

    val clockPackages = listOf(
      "com.google.android.deskclock",
      "com.android.deskclock",
      "com.sec.android.app.clockpackage",
      "com.oneplus.deskclock",
      "com.coloros.alarmclock"
    )
    for (pkg in clockPackages) {
      val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (launchIntent != null && tryStartActivity(context, launchIntent)) {
        return true
      }
    }
    return false
  }

  fun openCalendarApp(context: Context): Boolean {
    val categoryIntent = Intent.makeMainSelectorActivity(
      Intent.ACTION_MAIN,
      Intent.CATEGORY_APP_CALENDAR
    ).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (tryStartActivity(context, categoryIntent)) return true

    val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      data = builder.build()
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return tryStartActivity(context, viewIntent)
  }

  fun openMusicApp(context: Context): Boolean {
    val musicIntent = Intent.makeMainSelectorActivity(
      Intent.ACTION_MAIN,
      Intent.CATEGORY_APP_MUSIC
    ).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return tryStartActivity(context, musicIntent)
  }

  private fun tryStartActivity(context: Context, intent: Intent): Boolean {
    return try {
      context.startActivity(intent)
      true
    } catch (_: ActivityNotFoundException) {
      false
    } catch (_: SecurityException) {
      false
    }
  }
}
