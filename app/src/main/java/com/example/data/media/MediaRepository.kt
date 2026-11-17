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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

  private val mainHandler = Handler(Looper.getMainLooper())
  private val sessionManager =
    context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

  private val _mediaState = MutableStateFlow(ActiveMediaState())
  val mediaState: StateFlow<ActiveMediaState> = _mediaState.asStateFlow()

  private var activeController: MediaController? = null
  private var listening = false

  private val controllerCallback = object : MediaController.Callback() {
    override fun onPlaybackStateChanged(state: PlaybackState?) {
      publishControllerState(activeController)
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
      publishControllerState(activeController)
    }

    override fun onSessionDestroyed() {
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
    if (listening) {
      runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
      listening = false
    }
    detachController()
  }

  fun refreshSessions() {
    val granted = GlassbonMediaListenerService.isPermissionGranted(context)
    if (!granted) {
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
      controller.transportControls.pause()
    } else {
      controller.transportControls.play()
    }
  }

  fun skipToNext() {
    activeController?.transportControls?.skipToNext()
  }

  fun skipToPrevious() {
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

  private fun bindBestController(controllers: List<MediaController>) {
    val best = controllers.firstOrNull {
      val st = it.playbackState?.state
      st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING
    } ?: controllers.firstOrNull {
      val title = it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
      !title.isNullOrBlank()
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
      _mediaState.value = ActiveMediaState(hasPermission = true, hasSession = false)
      return
    }
    val meta = controller.metadata
    val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
    val artist = (meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
      ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
      ?: "").trim()

    if (title.isEmpty()) {
      _mediaState.value = ActiveMediaState(hasPermission = true, hasSession = false)
      return
    }

    val rawArt = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
      ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
      ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    val scaledArt = rawArt?.let { downscaleArtwork(it, 220) }?.asImageBitmap()

    val pbState = controller.playbackState?.state
    val isPlaying = pbState == PlaybackState.STATE_PLAYING || pbState == PlaybackState.STATE_BUFFERING

    _mediaState.value = ActiveMediaState(
      hasPermission = true,
      hasSession = true,
      isPlaying = isPlaying,
      title = title,
      artist = artist,
      artwork = scaledArt,
      trackKey = "$title|$artist|${controller.packageName}",
      packageName = controller.packageName
    )
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
