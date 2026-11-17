package com.example.data.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.platform.GlassbonMediaListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@Immutable
data class ActiveMediaState(
  val hasPermission: Boolean = false,
  val hasSession: Boolean = false,
  val isPlaying: Boolean = false,
  val title: String = "",
  val artist: String = "",
  val artwork: ImageBitmap? = null,
  val trackKey: String = "",
  val packageName: String? = null
)

class MediaRepository(private val context: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val mainHandler = Handler(Looper.getMainLooper())
  private val sessionManager =
    context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

  private val _mediaState = MutableStateFlow(ActiveMediaState())
  val mediaState: StateFlow<ActiveMediaState> = _mediaState.asStateFlow()

  private var activeController: MediaController? = null
  private var listening = false
  private var cachedTrackKey: String? = null
  private var cachedArtwork: ImageBitmap? = null
  private var artworkJob: Job? = null
  private var pauseGraceJob: Job? = null
  private var explicitPauseToken: Any? = null
  private var explicitPauseExpiresAtMs: Long = 0L

  private val controllerCallback = object : MediaController.Callback() {
    override fun onPlaybackStateChanged(state: PlaybackState?) {
      publishControllerState(activeController)
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
      publishControllerState(activeController)
    }

    override fun onSessionDestroyed() {
      clearPauseGrace()
      refreshSessions()
    }
  }

  private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
    bindBestController(controllers.orEmpty())
  }

  fun startObserving() {
    val granted = GlassbonMediaListenerService.isPermissionGranted(context)
    if (!granted) {
      stopObserving()
      _mediaState.value = ActiveMediaState(hasPermission = false)
      return
    }
    if (!listening) {
      runCatching {
        sessionManager?.addOnActiveSessionsChangedListener(
          sessionsListener,
          GlassbonMediaListenerService.componentName(context),
          mainHandler
        )
        listening = true
      }
    }
    refreshSessions()
  }

  fun stopObserving() {
    artworkJob?.cancel()
    artworkJob = null
    clearPauseGrace()
    if (listening) {
      runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
      listening = false
    }
    detachController()
  }

  fun refreshSessions() {
    val granted = GlassbonMediaListenerService.isPermissionGranted(context)
    if (!granted) {
      clearPauseGrace()
      detachController()
      _mediaState.value = ActiveMediaState(hasPermission = false)
      return
    }
    val controllers = runCatching {
      sessionManager?.getActiveSessions(GlassbonMediaListenerService.componentName(context))
    }.getOrNull().orEmpty()
    bindBestController(controllers)
  }

  fun togglePlayPause() {
    val controller = activeController ?: return
    val state = controller.playbackState?.state
    if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
      // Allow a brief grace window when user pauses directly from Glassbon so they can resume cleanly
      val token = controller.sessionToken
      explicitPauseToken = token
      explicitPauseExpiresAtMs = android.os.SystemClock.elapsedRealtime() + 12_000L
      pauseGraceJob?.cancel()
      pauseGraceJob = scope.launch {
        kotlinx.coroutines.delay(12_000L)
        if (explicitPauseToken == token) {
          clearPauseGrace()
          refreshSessions()
        }
      }
      controller.transportControls.pause()
    } else {
      clearPauseGrace()
      controller.transportControls.play()
    }
  }

  fun skipToNext() {
    clearPauseGrace()
    activeController?.transportControls?.skipToNext()
  }

  fun skipToPrevious() {
    clearPauseGrace()
    activeController?.transportControls?.skipToPrevious()
  }

  fun openActivePlayer() {
    val controller = activeController ?: return
    val pending = controller.sessionActivity
    if (pending != null) {
      val sent = runCatching { pending.send(); true }.getOrDefault(false)
      if (sent) return
    }
    val launchIntent = context.packageManager.getLaunchIntentForPackage(controller.packageName)
    if (launchIntent != null) {
      launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      runCatching { context.startActivity(launchIntent) }
    }
  }

  private fun clearPauseGrace() {
    pauseGraceJob?.cancel()
    pauseGraceJob = null
    explicitPauseToken = null
    explicitPauseExpiresAtMs = 0L
  }

  private fun bindBestController(controllers: List<MediaController>) {
    val now = android.os.SystemClock.elapsedRealtime()
    val playingController = controllers.firstOrNull {
      val st = it.playbackState?.state
      val title = it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
      (st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING) && !title.isNullOrBlank()
    }

    val best = playingController ?: controllers.firstOrNull {
      val st = it.playbackState?.state
      val title = it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
      st == PlaybackState.STATE_PAUSED &&
        it.sessionToken == explicitPauseToken &&
        now < explicitPauseExpiresAtMs &&
        !title.isNullOrBlank()
    }

    if (playingController != null) {
      clearPauseGrace()
    }

    if (best?.sessionToken != activeController?.sessionToken) {
      detachController()
      activeController = best
      best?.registerCallback(controllerCallback, mainHandler)
    }
    publishControllerState(best)
  }

  private fun detachController() {
    runCatching { activeController?.unregisterCallback(controllerCallback) }
    activeController = null
  }

  private fun publishControllerState(controller: MediaController?) {
    if (controller == null) {
      artworkJob?.cancel()
      cachedTrackKey = null
      cachedArtwork = null
      _mediaState.value = ActiveMediaState(hasPermission = true, hasSession = false)
      return
    }
    val meta = controller.metadata
    val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
    val artist = (meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
      ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
      ?: "").trim()

    val pbState = controller.playbackState?.state
    val isPlaying = pbState == PlaybackState.STATE_PLAYING || pbState == PlaybackState.STATE_BUFFERING
    val inPauseGrace = pbState == PlaybackState.STATE_PAUSED &&
      controller.sessionToken == explicitPauseToken &&
      android.os.SystemClock.elapsedRealtime() < explicitPauseExpiresAtMs

    if (title.isEmpty() || (!isPlaying && !inPauseGrace)) {
      artworkJob?.cancel()
      cachedTrackKey = null
      cachedArtwork = null
      _mediaState.value = ActiveMediaState(hasPermission = true, hasSession = false)
      return
    }

    if (isPlaying) {
      clearPauseGrace()
    }
    val trackKey = "$title|$artist|${controller.packageName}"

    // Reuse cached artwork when only play/pause state updates so UI stays 0ms latency
    if (trackKey == cachedTrackKey && cachedArtwork != null) {
      _mediaState.value = ActiveMediaState(
        hasPermission = true,
        hasSession = true,
        isPlaying = isPlaying,
        title = title,
        artist = artist,
        artwork = cachedArtwork,
        trackKey = trackKey,
        packageName = controller.packageName
      )
      return
    }

    val rawArt = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
      ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
      ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

    if (rawArt == null) {
      cachedTrackKey = trackKey
      cachedArtwork = null
      _mediaState.value = ActiveMediaState(
        hasPermission = true,
        hasSession = true,
        isPlaying = isPlaying,
        title = title,
        artist = artist,
        artwork = null,
        trackKey = trackKey,
        packageName = controller.packageName
      )
      return
    }

    // Publish text/playback immediately, downscale artwork off-main-thread
    _mediaState.value = ActiveMediaState(
      hasPermission = true,
      hasSession = true,
      isPlaying = isPlaying,
      title = title,
      artist = artist,
      artwork = if (trackKey == cachedTrackKey) cachedArtwork else _mediaState.value.artwork,
      trackKey = trackKey,
      packageName = controller.packageName
    )

    artworkJob?.cancel()
    artworkJob = scope.launch {
      val scaled = withContext(Dispatchers.Default) {
        downscaleArtwork(rawArt, 180).asImageBitmap()
      }
      cachedTrackKey = trackKey
      cachedArtwork = scaled
      _mediaState.value = _mediaState.value.copy(
        artwork = scaled,
        trackKey = trackKey
      )
    }
  }

  private fun downscaleArtwork(bitmap: Bitmap, maxDim: Int): Bitmap {
    val largest = max(bitmap.width, bitmap.height)
    if (largest <= maxDim || largest <= 0) return bitmap
    val ratio = maxDim.toFloat() / largest.toFloat()
    val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }
}
