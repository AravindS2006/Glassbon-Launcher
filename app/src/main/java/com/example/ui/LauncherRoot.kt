package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.graphics.AppIconCache
import com.example.platform.rememberSystemClockSnapshot
import com.example.ui.components.AlphabetRailSide
import com.example.ui.components.GlassSurface
import com.example.ui.drawer.AppDrawerScreen
import com.example.ui.home.HomeScreen
import com.example.ui.overlays.AppContextMenuDialog
import com.example.ui.overlays.LockPermissionDialog
import com.example.ui.overlays.QuickActionsOverlay
import com.example.ui.overlays.WalkthroughOverlay
import com.example.ui.settings.FavoritesManagerScreen
import com.example.ui.settings.HiddenAppsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.WallpaperStudioScreen
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors
import kotlinx.coroutines.delay

@Composable
fun LauncherRoot(
  uiState: LauncherUiState,
  iconCache: AppIconCache,
  viewModel: LauncherViewModel,
  onRequestDefaultHome: () -> Unit,
  onLightStatusBarChanged: (Boolean) -> Unit
) {
  val prefs = uiState.preferences
  val clockSnapshot = rememberSystemClockSnapshot()
  var activeHomeScrubSide by remember { mutableStateOf<AlphabetRailSide?>(null) }

  val isOverlayBlockingLightTop = uiState.isDrawerOpen ||
    uiState.showQuickActions ||
    uiState.showLockPermissionPrompt ||
    uiState.showWalkthrough ||
    uiState.contextMenuApp != null ||
    uiState.activeOverlay == LauncherOverlay.SETTINGS ||
    uiState.activeOverlay == LauncherOverlay.FAVORITES_MANAGER ||
    uiState.activeOverlay == LauncherOverlay.HIDDEN_APPS

  val useDarkStatusIcons = !isOverlayBlockingLightTop && uiState.wallpaper.isTopBright
  LaunchedEffect(useDarkStatusIcons) {
    onLightStatusBarChanged(useDarkStatusIcons)
  }

  LaunchedEffect(uiState.statusMessage) {
    if (uiState.statusMessage != null) {
      delay(2600L)
      viewModel.clearStatusMessage()
    }
  }

  val transitionDurationMs = when {
    prefs.effectiveReducedMotion -> 0
    uiState.isDrawerOpen -> 220
    else -> 180
  }

  // Read drawerProgressState.value ONLY inside derivedStateOf and graphicsLayer lambdas (0 per-frame recompositions!)
  val drawerProgressState = animateFloatAsState(
    targetValue = if (uiState.isDrawerOpen) 1f else 0f,
    animationSpec = tween(
      durationMillis = transitionDurationMs,
      easing = FastOutSlowInEasing
    ),
    label = "drawer_progress"
  )

  val showDrawerLayer by remember {
    derivedStateOf { drawerProgressState.value > 0.005f }
  }
  val showHomeLayer by remember {
    derivedStateOf { drawerProgressState.value < 0.995f || activeHomeScrubSide != null }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MistColors.NearBlack)
  ) {
    // 1. Base sharp wallpaper layer
    val homeBmp = uiState.wallpaper.homeBitmap
    if (homeBmp != null) {
      Image(
        bitmap = homeBmp,
        contentDescription = stringResource(R.string.cd_home_wallpaper),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      Image(
        painter = painterResource(id = R.drawable.img_default_forest_wallpaper),
        contentDescription = stringResource(R.string.cd_home_wallpaper),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    }

    // Subtle bottom atmospheric gradient for contrast
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(BOTTOM_ATMOSPHERIC_SCRIM)
    )

    // User-configurable wallpaper darkening veil
    if (prefs.wallpaperDarkening > 0f) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MistColors.NearBlack.copy(alpha = prefs.wallpaperDarkening))
      )
    }

    // 2. Pre-blurred frosted-glass wallpaper layer for the App Drawer (skips duplicate image pass in Low Power mode)
    if (showDrawerLayer) {
      val drawerBmp = uiState.wallpaper.drawerBlurredBitmap ?: homeBmp
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer { alpha = drawerProgressState.value }
      ) {
        if (!prefs.effectiveLowEffects && drawerBmp != null) {
          Image(
            bitmap = drawerBmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(if (prefs.effectiveLowEffects) Color(0xBD0D0E11) else Color(0x76141519))
        )
      }
    }

    // 3. App Drawer layer (translationY + alpha only; avoids expensive whole-screen scale texture resizing)
    if (showDrawerLayer && uiState.activeOverlay == LauncherOverlay.NONE) {
      AppDrawerScreen(
        uiState = uiState,
        iconCache = iconCache,
        drawerJumpEvents = viewModel.drawerJumpEvents,
        hideRightAlphabetRail = activeHomeScrubSide == AlphabetRailSide.RIGHT,
        hideLeftAlphabetRail = activeHomeScrubSide == AlphabetRailSide.LEFT,
        onJumpTargetHandled = { viewModel.updateDrawerJumpTarget(null) },
        onLaunchApp = { viewModel.launchApp(it) },
        onLongPressApp = { viewModel.openContextMenu(it) },
        onSearchActiveChange = { viewModel.setSearchActive(it) },
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
        onCloseDrawer = { viewModel.closeDrawer() },
        modifier = Modifier.graphicsLayer {
          val p = drawerProgressState.value
          alpha = p
          translationY = (1f - p) * 28f
        }
      )
    }

    // 4. Home Screen layer (stays alive during active Home Alphabet Rail drag so touch scrub never drops)
    if (showHomeLayer && uiState.activeOverlay == LauncherOverlay.NONE) {
      HomeScreen(
        uiState = uiState,
        clockSnapshot = clockSnapshot,
        iconCache = iconCache,
        drawerProgressProvider = { drawerProgressState.value },
        onScrubToDrawerSection = { key, side ->
          activeHomeScrubSide = side
          if (uiState.isDrawerOpen) {
            viewModel.emitDrawerJump(key)
          } else {
            viewModel.openDrawer(withSearch = false, jumpToSection = key)
          }
        },
        onScrubDragEnd = {
          activeHomeScrubSide = null
        },
        onLaunchApp = { viewModel.launchApp(it) },
        onLongPressApp = { viewModel.openContextMenu(it) },
        onPowerButtonTap = { viewModel.onHomePowerButtonTap() },
        onPowerButtonLongPress = { viewModel.toggleQuickActions(true) },
        onClockClick = { viewModel.openClockApp() },
        onDateClick = { viewModel.openCalendarApp() },
        onToggleMediaPlayPause = { viewModel.toggleMediaPlayPause() },
        onSkipMediaNext = { viewModel.skipMediaNext() },
        onSkipMediaPrevious = { viewModel.skipMediaPrevious() },
        onOpenActiveMediaPlayer = { viewModel.openActiveMediaPlayer() },
        onRequestDefaultHome = onRequestDefaultHome,
        onStartTour = { viewModel.startWalkthrough() },
        onDismissOnboarding = { viewModel.dismissOnboarding() }
      )
    }

    // 5. Secondary Settings / Customization overlays
    val overlayFadeDuration = if (prefs.effectiveReducedMotion) 0 else 160
    AnimatedVisibility(
      visible = uiState.activeOverlay == LauncherOverlay.SETTINGS,
      enter = fadeIn(tween(overlayFadeDuration)),
      exit = fadeOut(tween(overlayFadeDuration))
    ) {
      SettingsScreen(
        uiState = uiState,
        onSetPerformanceProfile = viewModel::setPerformanceProfile,
        onSetIconStyle = viewModel::setIconStyle,
        onSetIconSize = viewModel::setIconSize,
        onSetTypographyStyle = viewModel::setTypographyStyle,
        onSetShowClock = viewModel::setShowClock,
        onSetShowDate = viewModel::setShowDate,
        onSetShowHomeLabels = viewModel::setShowHomeLabels,
        onSetShowAlphabetIndex = viewModel::setShowAlphabetIndex,
        onSetShowLeftHiddenAlphabet = viewModel::setShowLeftHiddenAlphabet,
        onSetHomeAlphabetVisibility = viewModel::setHomeAlphabetVisibility,
        onSetHomeShortcutLimit = viewModel::setHomeShortcutLimit,
        onSetQuickActionMode = viewModel::setQuickActionMode,
        onSetSwipeUpOpensDrawer = viewModel::setSwipeUpOpensDrawer,
        onSetSearchAutoFocus = viewModel::setSearchAutoFocusKeyboard,
        onSetReducedMotion = viewModel::setReducedMotion,
        onSetLowEffectsMode = viewModel::setLowEffectsMode,
        onSetHapticFeedback = viewModel::setHapticFeedback,
        onOpenWallpaperStudio = { viewModel.openOverlay(LauncherOverlay.WALLPAPER_STUDIO) },
        onOpenFavoritesManager = { viewModel.openOverlay(LauncherOverlay.FAVORITES_MANAGER) },
        onOpenHiddenApps = { viewModel.openOverlay(LauncherOverlay.HIDDEN_APPS) },
        onOpenDeviceAdminActivation = { viewModel.openDeviceAdminActivation() },
        onOpenNotificationListenerSettings = { viewModel.openNotificationListenerSettings() },
        onRequestDefaultHome = onRequestDefaultHome,
        onStartWalkthrough = { viewModel.startWalkthrough() },
        onClose = { viewModel.closeOverlay() }
      )
    }

    AnimatedVisibility(
      visible = uiState.activeOverlay == LauncherOverlay.WALLPAPER_STUDIO,
      enter = fadeIn(tween(overlayFadeDuration)),
      exit = fadeOut(tween(overlayFadeDuration))
    ) {
      WallpaperStudioScreen(
        uiState = uiState,
        onPickWallpaperUri = viewModel::applyPickedWallpaper,
        onRestoreDefault = viewModel::restoreDefaultWallpaper,
        onSetGrayscale = viewModel::setGrayscaleWallpaper,
        onSetDarkening = viewModel::setWallpaperDarkening,
        onSetBlurIntensity = viewModel::setDrawerBlurIntensity,
        onClose = { viewModel.closeOverlay() }
      )
    }

    AnimatedVisibility(
      visible = uiState.activeOverlay == LauncherOverlay.FAVORITES_MANAGER,
      enter = fadeIn(tween(overlayFadeDuration)),
      exit = fadeOut(tween(overlayFadeDuration))
    ) {
      FavoritesManagerScreen(
        uiState = uiState,
        iconCache = iconCache,
        onToggleFavorite = viewModel::toggleFavorite,
        onMoveFavorite = viewModel::moveFavorite,
        onClose = { viewModel.closeOverlay() }
      )
    }

    AnimatedVisibility(
      visible = uiState.activeOverlay == LauncherOverlay.HIDDEN_APPS,
      enter = fadeIn(tween(overlayFadeDuration)),
      exit = fadeOut(tween(overlayFadeDuration))
    ) {
      HiddenAppsScreen(
        uiState = uiState,
        iconCache = iconCache,
        onUnhideApp = viewModel::toggleHideApp,
        onClose = { viewModel.closeOverlay() }
      )
    }

    // 6. Quick Actions Panel
    val quickDuration = if (prefs.effectiveReducedMotion) 0 else 150
    AnimatedVisibility(
      visible = uiState.showQuickActions,
      enter = fadeIn(tween(quickDuration)),
      exit = fadeOut(tween(quickDuration))
    ) {
      QuickActionsOverlay(
        isDefaultLauncher = uiState.isDefaultLauncher,
        isMediaListenerEnabled = uiState.isMediaListenerEnabled,
        reducedMotion = prefs.effectiveReducedMotion,
        onLockScreen = { viewModel.lockScreenOrPrompt() },
        onOpenLibrary = { viewModel.openDrawer(withSearch = false) },
        onOpenSearch = { viewModel.openDrawer(withSearch = true) },
        onOpenWallpaper = { viewModel.openOverlay(LauncherOverlay.WALLPAPER_STUDIO) },
        onOpenFavorites = { viewModel.openOverlay(LauncherOverlay.FAVORITES_MANAGER) },
        onOpenPreferences = { viewModel.openOverlay(LauncherOverlay.SETTINGS) },
        onStartWalkthrough = { viewModel.startWalkthrough() },
        onOpenMediaListenerSettings = { viewModel.openNotificationListenerSettings() },
        onOpenDeviceSettings = { viewModel.openDeviceSettings() },
        onRequestDefaultHome = {
          viewModel.toggleQuickActions(false)
          onRequestDefaultHome()
        },
        onDismiss = { viewModel.toggleQuickActions(false) }
      )
    }

    // 7. Device Admin Lock Permission Guidance Dialog
    AnimatedVisibility(
      visible = uiState.showLockPermissionPrompt,
      enter = fadeIn(tween(quickDuration)),
      exit = fadeOut(tween(quickDuration))
    ) {
      LockPermissionDialog(
        onActivateDeviceAdmin = { viewModel.openDeviceAdminActivation() },
        onOpenQuickActions = { viewModel.toggleQuickActions(true) },
        onDismiss = { viewModel.dismissLockPermissionPrompt() }
      )
    }

    // 7b. Interactive Frosted Feature Walkthrough Tour
    AnimatedVisibility(
      visible = uiState.showWalkthrough,
      enter = fadeIn(tween(quickDuration)),
      exit = fadeOut(tween(quickDuration))
    ) {
      WalkthroughOverlay(
        currentStep = uiState.walkthroughStep,
        reducedMotion = prefs.effectiveReducedMotion,
        onStepChange = { viewModel.setWalkthroughStep(it) },
        onFinish = { viewModel.finishWalkthrough() }
      )
    }

    // 8. Contextual Frosted Pop-Up App Preview
    val contextApp = uiState.contextMenuApp
    if (contextApp != null) {
      val favIndex = uiState.favoriteApps.indexOfFirst { it.componentKey == contextApp.componentKey }
      AppContextMenuDialog(
        app = contextApp,
        isFavorite = favIndex >= 0,
        favoriteIndex = favIndex,
        favoriteCount = uiState.favoriteApps.size,
        isHidden = contextApp.componentKey in prefs.hiddenKeys,
        iconCache = iconCache,
        iconStyle = prefs.iconStyle,
        reducedMotion = prefs.effectiveReducedMotion,
        lowPowerMode = prefs.effectiveLowEffects,
        onToggleFavorite = { viewModel.toggleFavorite(contextApp) },
        onMoveFavoriteEarlier = { viewModel.moveFavorite(contextApp.componentKey, -1) },
        onMoveFavoriteLater = { viewModel.moveFavorite(contextApp.componentKey, 1) },
        onOpenAppInfo = { viewModel.openAppInfo(contextApp) },
        onToggleHide = { viewModel.toggleHideApp(contextApp) },
        onUninstall = { viewModel.requestUninstall(contextApp) },
        onDismiss = { viewModel.closeContextMenu() }
      )
    }

    // 9. Transient status pill
    AnimatedVisibility(
      visible = uiState.statusMessage != null,
      enter = fadeIn(tween(150)),
      exit = fadeOut(tween(150)),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 86.dp)
    ) {
      GlassSurface(shape = RoundedCornerShape(16.dp)) {
        Text(
          text = uiState.statusMessage.orEmpty(),
          fontFamily = FunctionalSansFontFamily,
          fontSize = 13.sp,
          color = MistColors.OffWhite,
          modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("launcher_status_pill")
        )
      }
    }
  }
}

private val BOTTOM_ATMOSPHERIC_SCRIM = Brush.verticalGradient(
  colorStops = arrayOf(
    0.0f to Color.Transparent,
    0.55f to Color.Transparent,
    1.0f to Color(0x780B0B0D)
  )
)
