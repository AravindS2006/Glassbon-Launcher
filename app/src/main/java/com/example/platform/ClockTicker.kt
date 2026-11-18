package com.example.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class ClockSnapshot(
  val formattedTime: String,
  val formattedDate: String
)

fun formatCurrentClockSnapshot(context: Context, nowMillis: Long = System.currentTimeMillis()): ClockSnapshot {
  val now = Date(nowMillis)
  val locale = Locale.getDefault()
  val is24Hour = DateFormat.is24HourFormat(context)
  val timePattern = if (is24Hour) "HH:mm" else "h:mm"
  val timeFormatter = SimpleDateFormat(timePattern, locale)
  val weekdayFormatter = SimpleDateFormat("EEE", locale)
  val monthDaySkeleton = DateFormat.getBestDateTimePattern(locale, "MMMd")
  val monthDayFormatter = SimpleDateFormat(monthDaySkeleton, locale)
  return ClockSnapshot(
    formattedTime = timeFormatter.format(now),
    formattedDate = "${weekdayFormatter.format(now)} · ${monthDayFormatter.format(now)}"
  )
}

@Composable
fun rememberSystemClockSnapshot(): ClockSnapshot {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var snapshot by remember { mutableStateOf(formatCurrentClockSnapshot(context)) }

  DisposableEffect(context, lifecycleOwner) {
    val refresh = { snapshot = formatCurrentClockSnapshot(context) }

    val receiver = object : BroadcastReceiver() {
      override fun onReceive(ctx: Context?, intent: Intent?) {
        refresh()
      }
    }
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_TIME_TICK)
      addAction(Intent.ACTION_TIME_CHANGED)
      addAction(Intent.ACTION_TIMEZONE_CHANGED)
      addAction(Intent.ACTION_LOCALE_CHANGED)
    }

    var receiverRegistered = false
    fun registerTickReceiver() {
      if (!receiverRegistered) {
        receiverRegistered = true
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
      }
    }
    fun unregisterTickReceiver() {
      if (receiverRegistered) {
        receiverRegistered = false
        runCatching { context.unregisterReceiver(receiver) }
      }
    }

    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
      registerTickReceiver()
    }

    val lifecycleObserver = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> {
          registerTickReceiver()
          refresh()
        }
        Lifecycle.Event.ON_RESUME -> refresh()
        Lifecycle.Event.ON_STOP -> unregisterTickReceiver()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

    onDispose {
      unregisterTickReceiver()
      lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
    }
  }

  return snapshot
}
