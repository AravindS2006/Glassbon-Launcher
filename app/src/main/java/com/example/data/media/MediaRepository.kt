package com.example.data.media

import android.content.Context
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.annotation.RequiresApi
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
  private var pendingRefreshJob: Job? = null
  private var pauseRequestedAtMs: Long = 0L
  private var playbackCallback: Any? = null

  fun startObserving() {
    registerPlaybackCallback()
    refreshSessions()
  }

  fun stopObserving() {
    unregisterPlaybackCallback()
    pendingRefreshJob?.cancel()
    pendingRefreshJob = null
    clearPauseGrace()
  }

  /**
   * On API 26+ AudioManager reports playback start/stop for every app without any permission, so the Home media
   * card follows music started or stopped elsewhere (headphones, notification, other apps) instead of only
   * refreshing on launcher start or after its own buttons are pressed.
   */
  private fun registerPlaybackCallback() {
    if (playbackCallback != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val mgr = audioManager ?: return
    playbackCallback = runCatching {
      createPlaybackCallback().also { mgr.registerAudioPlaybackCallback(it, Handler(Looper.getMainLooper())) }
    }.getOrNull()
  }

  private fun unregisterPlaybackCallback() {
    val callback = playbackCallback ?: return
    playbackCallback = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      runCatching { audioManager?.unregisterAudioPlaybackCallback(callback as AudioManager.AudioPlaybackCallback) }
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createPlaybackCallback() = object : AudioManager.AudioPlaybackCallback() {
    override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
      scheduleRefresh()
    }
  }

  /** Coalesces bursts of playback callbacks and gives isMusicActive a moment to settle. */
  private fun scheduleRefresh(delayMs: Long = 150L) {
    pendingRefreshJob?.cancel()
    pendingRefreshJob = scope.launch {
      delay(delayMs)
      refreshSessions()
    }
  }

  fun refreshSessions() {
    val active = runCatching { audioManager?.isMusicActive == true }.getOrDefault(false)
    val now = SystemClock.elapsedRealtime()
    val sincePauseRequest = now - pauseRequestedAtMs
    if (active && explicitPauseExpiresAtMs > now && sincePauseRequest < PAUSE_SETTLE_MS) {
      // The player hasn't processed our pause key yet; keep showing "Paused" and check again once it settles.
      scheduleRefresh(PAUSE_SETTLE_MS - sincePauseRequest)
      return
    }
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
      pauseRequestedAtMs = SystemClock.elapsedRealtime()
      explicitPauseExpiresAtMs = pauseRequestedAtMs + 12_000L
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
      scheduleRefresh()
    }
  }

  fun skipToNext() {
    clearPauseGrace()
    sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    scheduleRefresh()
  }

  fun skipToPrevious() {
    clearPauseGrace()
    sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    scheduleRefresh()
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

  private companion object {
    const val PAUSE_SETTLE_MS = 1_000L
  }

  private fun clearPauseGrace() {
    pauseGraceJob?.cancel()
    pauseGraceJob = null
    explicitPauseExpiresAtMs = 0L
  }
}
