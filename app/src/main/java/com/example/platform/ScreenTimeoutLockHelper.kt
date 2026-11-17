package com.example.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.core.content.ContextCompat

enum class ScreenTimeoutLockResult {
  LOCKING_VIA_TIMEOUT,
  NEEDS_WRITE_SETTINGS_PERMISSION,
  FAILED
}

object ScreenTimeoutLockHelper {

  private const val PREFS_NAME = "glassbon_screen_lock_prefs"
  private const val KEY_SAVED_TIMEOUT_MS = "saved_original_screen_off_timeout_ms"
  private const val KEY_PENDING_RESTORE = "pending_timeout_restore"

  const val SHORT_LOCK_TIMEOUT_MS = 100
  const val MIN_VALID_USER_TIMEOUT_MS = 5_000
  const val DEFAULT_FALLBACK_TIMEOUT_MS = 30_000
  private const val WATCHDOG_RESTORE_DELAY_MS = 4_000L

  private var watchdogHandler: Handler? = null
  private var watchdogRunnable: Runnable? = null
  private var screenStateReceiver: BroadcastReceiver? = null
  private var registeredAppContext: Context? = null

  @Volatile
  private var pendingSavedTimeoutMs: Int? = null

  fun canWriteSystemSettings(context: Context): Boolean {
    return runCatching { Settings.System.canWrite(context) }.getOrDefault(false)
  }

  fun openWriteSettingsPermission(context: Context): Boolean {
    val needsNewTask = context !is Activity
    val specificIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
      if (needsNewTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (tryStartActivity(context, specificIntent)) return true

    val genericIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
      if (needsNewTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return tryStartActivity(context, genericIntent)
  }

  fun ensureReceiverRegistered(context: Context) {
    val appCtx = context.applicationContext
    if (screenStateReceiver != null && registeredAppContext === appCtx) return
    if (screenStateReceiver != null) {
      unregisterReceiver(registeredAppContext ?: appCtx)
    }
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
          Intent.ACTION_SCREEN_OFF,
          Intent.ACTION_SCREEN_ON,
          Intent.ACTION_USER_PRESENT -> restoreOriginalTimeoutIfNeeded(appCtx)
        }
      }
    }
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_SCREEN_OFF)
      addAction(Intent.ACTION_SCREEN_ON)
      addAction(Intent.ACTION_USER_PRESENT)
    }
    runCatching {
      ContextCompat.registerReceiver(
        appCtx,
        receiver,
        filter,
        ContextCompat.RECEIVER_NOT_EXPORTED
      )
      screenStateReceiver = receiver
      registeredAppContext = appCtx
    }
  }

  fun unregisterReceiver(context: Context) {
    val receiver = screenStateReceiver
    val targetCtx = registeredAppContext ?: context.applicationContext
    screenStateReceiver = null
    registeredAppContext = null
    pendingSavedTimeoutMs = null
    cancelWatchdog()
    if (receiver != null) {
      runCatching {
        targetCtx.unregisterReceiver(receiver)
      }
    }
  }

  fun triggerShortTimeoutLock(context: Context): ScreenTimeoutLockResult {
    if (!canWriteSystemSettings(context)) {
      return ScreenTimeoutLockResult.NEEDS_WRITE_SETTINGS_PERMISSION
    }

    val appCtx = context.applicationContext
    ensureReceiverRegistered(appCtx)

    val resolver = appCtx.contentResolver
    val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val currentTimeout = runCatching {
      Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_FALLBACK_TIMEOUT_MS)
    }.getOrDefault(DEFAULT_FALLBACK_TIMEOUT_MS)

    // Only capture original timeout if it is >= 5s so double-taps never overwrite it with 100ms
    val timeoutToRestore = if (currentTimeout >= MIN_VALID_USER_TIMEOUT_MS) {
      currentTimeout
    } else {
      (pendingSavedTimeoutMs ?: prefs.getInt(KEY_SAVED_TIMEOUT_MS, DEFAULT_FALLBACK_TIMEOUT_MS))
        .coerceAtLeast(MIN_VALID_USER_TIMEOUT_MS)
    }

    pendingSavedTimeoutMs = timeoutToRestore
    prefs.edit()
      .putInt(KEY_SAVED_TIMEOUT_MS, timeoutToRestore)
      .putBoolean(KEY_PENDING_RESTORE, true)
      .commit()

    // Ensure our own window is not holding the display awake
    if (context is Activity) {
      runCatching {
        context.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }

    val applied = runCatching {
      Settings.System.putInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, SHORT_LOCK_TIMEOUT_MS)
    }.getOrDefault(false)

    if (!applied) {
      pendingSavedTimeoutMs = null
      prefs.edit().putBoolean(KEY_PENDING_RESTORE, false).commit()
      return ScreenTimeoutLockResult.FAILED
    }

    scheduleWatchdogRestore(appCtx)
    return ScreenTimeoutLockResult.LOCKING_VIA_TIMEOUT
  }

  fun restoreOriginalTimeoutIfNeeded(context: Context) {
    val appCtx = context.applicationContext
    val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val pending = prefs.getBoolean(KEY_PENDING_RESTORE, false) || pendingSavedTimeoutMs != null
    val canWrite = canWriteSystemSettings(appCtx)

    val currentTimeout = runCatching {
      Settings.System.getInt(
        appCtx.contentResolver,
        Settings.System.SCREEN_OFF_TIMEOUT,
        DEFAULT_FALLBACK_TIMEOUT_MS
      )
    }.getOrDefault(DEFAULT_FALLBACK_TIMEOUT_MS)

    if (!pending && currentTimeout >= MIN_VALID_USER_TIMEOUT_MS) {
      return
    }

    if (!canWrite) {
      return
    }

    cancelWatchdog()
    val restoreTarget = (
      pendingSavedTimeoutMs
        ?: prefs.getInt(KEY_SAVED_TIMEOUT_MS, DEFAULT_FALLBACK_TIMEOUT_MS)
      ).coerceAtLeast(MIN_VALID_USER_TIMEOUT_MS)

    runCatching {
      Settings.System.putInt(
        appCtx.contentResolver,
        Settings.System.SCREEN_OFF_TIMEOUT,
        restoreTarget
      )
    }
    pendingSavedTimeoutMs = null
    prefs.edit().putBoolean(KEY_PENDING_RESTORE, false).commit()
  }

  private fun scheduleWatchdogRestore(appContext: Context) {
    cancelWatchdog()
    val handler = Handler(Looper.getMainLooper())
    val runnable = Runnable {
      watchdogRunnable = null
      watchdogHandler = null
      restoreOriginalTimeoutIfNeeded(appContext)
    }
    watchdogHandler = handler
    watchdogRunnable = runnable
    handler.postDelayed(runnable, WATCHDOG_RESTORE_DELAY_MS)
  }

  private fun cancelWatchdog() {
    val runnable = watchdogRunnable
    val handler = watchdogHandler
    watchdogRunnable = null
    watchdogHandler = null
    if (runnable != null && handler != null) {
      handler.removeCallbacks(runnable)
    }
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
