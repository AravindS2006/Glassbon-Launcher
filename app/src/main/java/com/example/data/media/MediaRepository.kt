package com.example.data.media

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import com.example.platform.SystemActionsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class ActiveMediaState(
  val hasPermission: Boolean = true,
  val hasSession: Boolean = false,
  val isPlaying: Boolean = false,
  val title: String = "",
  val artist: String = "",
  val artwork: ImageBitmap? = null,
  val trackKey: String = "",
  val packageName: String? = null
)

/**
 * Zero-permission system audio playback bridge backed strictly by Android [AudioManager]
 * hardware media key events (`dispatchMediaKeyEvent`), avoiding `BIND_NOTIFICATION_LISTENER_SERVICE`.
 */
class MediaRepository(private val context: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val audioManager =
    context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  private val _mediaState = MutableStateFlow(ActiveMediaState())
  val mediaState: StateFlow<ActiveMediaState> = _mediaState.asStateFlow()

  private var pauseGraceJob: Job? = null
  private var explicitPauseExpiresAtMs: Long = 0L

  fun startObserving() {
    refreshSessions()
  }

  fun stopObserving() {
    clearPauseGrace()
  }

  fun refreshSessions() {
    val active = runCatching { audioManager?.isMusicActive == true }.getOrDefault(false)
    val now = SystemClock.elapsedRealtime()
    val inPauseGrace = !active && now < explicitPauseExpiresAtMs

    if (active) {
      clearPauseGrace()
      _mediaState.value = ActiveMediaState(
        hasPermission = true,
        hasSession = true,
        isPlaying = true,
        title = "System Audio",
        artist = "Tap to open music player"
      )
    } else if (inPauseGrace) {
      _mediaState.value = ActiveMediaState(
        hasPermission = true,
        hasSession = true,
        isPlaying = false,
        title = "Playback Paused",
        artist = "Tap play to resume"
      )
    } else {
      _mediaState.value = ActiveMediaState(hasPermission = true, hasSession = false)
    }
  }

  fun togglePlayPause() {
    val currentlyPlaying = runCatching { audioManager?.isMusicActive == true }.getOrDefault(false)
    sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    if (currentlyPlaying) {
      explicitPauseExpiresAtMs = SystemClock.elapsedRealtime() + 12_000L
      pauseGraceJob?.cancel()
      pauseGraceJob = scope.launch {
        _mediaState.value = ActiveMediaState(
          hasPermission = true,
          hasSession = true,
          isPlaying = false,
          title = "Playback Paused",
          artist = "Tap play to resume"
        )
        delay(12_000L)
        clearPauseGrace()
        refreshSessions()
      }
    } else {
      clearPauseGrace()
      scope.launch {
        delay(150L)
        refreshSessions()
      }
    }
  }

  fun skipToNext() {
    clearPauseGrace()
    sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    scope.launch {
      delay(150L)
      refreshSessions()
    }
  }

  fun skipToPrevious() {
    clearPauseGrace()
    sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    scope.launch {
      delay(150L)
      refreshSessions()
    }
  }

  fun openActivePlayer() {
    SystemActionsHelper.openMusicApp(context)
  }

  private fun sendMediaKey(keyCode: Int) {
    val mgr = audioManager ?: return
    val eventTime = SystemClock.uptimeMillis()
    runCatching {
      mgr.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
      mgr.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
    }
  }

  private fun clearPauseGrace() {
    pauseGraceJob?.cancel()
    pauseGraceJob = null
    explicitPauseExpiresAtMs = 0L
  }
}
