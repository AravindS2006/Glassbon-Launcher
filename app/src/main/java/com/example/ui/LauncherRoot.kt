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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.graphics.AppIconCache
import com.example.platform.rememberSystemClockSnapshot
import com.example.ui.components.AlphabetRailSide
import com.example.ui.components.GlassSurface
import com.example.ui.drawer.AppDrawerScreen
import com.example.ui.home.HomeScreen
import com.example.ui.overlays.AppContextMenuDialog
import com.example.ui.overlays.QuickActionsOverlay
import com.example.ui.overlays.WalkthroughOverlay
import com.example.ui.settings.FavoritesManagerScreen
import com.example.ui.settings.HiddenAppsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.WallpaperStudioScreen
import com.example.ui.theme.EditorialDisplayFontFamily
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
  val context = LocalContext.current
  val clockSnapshot = rememberSystemClockSnapshot()
  var activeHomeScrubSide by remember { mutableStateOf<AlphabetRailSide?>(null) }
  val homeScrubPullRatio = remember { mutableFloatStateOf(0f) }

  val isOverlayBlockingLightTop = uiState.isDrawerOpen ||
    uiState.showQuickActions ||
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
        drawerContinuousScrubEvents = viewModel.drawerContinuousScrubEvents,
        hideRightAlphabetRail = activeHomeScrubSide == AlphabetRailSide.RIGHT,
        hideLeftAlphabetRail = activeHomeScrubSide == AlphabetRailSide.LEFT,
        homeScrubPullProvider = { homeScrubPullRatio.floatValue },
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
        activeHomeScrubSide = activeHomeScrubSide,
        onScrubToDrawerSection = { key, side ->
          activeHomeScrubSide = side
          if (uiState.isDrawerOpen) {
            viewModel.emitDrawerJump(key)
          } else {
            viewModel.openDrawer(withSearch = false, jumpToSection = key)
          }
        },
        onScrubPullChanged = { pull ->
          homeScrubPullRatio.floatValue = pull
        },
        onContinuousScrub = { continuousIdx ->
          viewModel.emitDrawerContinuousScrub(continuousIdx)
        },
        onScrubDragEnd = {
          activeHomeScrubSide = null
          homeScrubPullRatio.floatValue = 0f
        },
        onLaunchApp = { viewModel.launchApp(it) },
        onLongPressApp = { viewModel.openContextMenu(it) },
        onPowerButtonTap = { viewModel.onHomePowerButtonTap(context) },
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
        onOpenWriteSettingsLockSetup = {
          viewModel.closeOverlay()
          viewModel.openLockPermissionPrompt()
        },
        onOpenLockScreenSettings = { viewModel.openLockScreenSettings(context) },
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
        isScreenLockEnabled = uiState.isScreenLockEnabled,
        reducedMotion = prefs.effectiveReducedMotion,
        onLockScreen = { viewModel.triggerScreenLock(context) },
        onOpenLockScreenSettings = { viewModel.openLockScreenSettings(context) },
        onOpenLibrary = { viewModel.openDrawer(withSearch = false) },
        onOpenSearch = { viewModel.openDrawer(withSearch = true) },
        onOpenWallpaper = { viewModel.openOverlay(LauncherOverlay.WALLPAPER_STUDIO) },
        onOpenFavorites = { viewModel.openOverlay(LauncherOverlay.FAVORITES_MANAGER) },
        onOpenPreferences = { viewModel.openOverlay(LauncherOverlay.SETTINGS) },
        onStartWalkthrough = { viewModel.startWalkthrough() },
        onOpenDeviceSettings = { viewModel.openDeviceSettings() },
        onRequestDefaultHome = {
          viewModel.toggleQuickActions(false)
          onRequestDefaultHome()
        },
        onDismiss = { viewModel.toggleQuickActions(false) }
      )
    }

    // 6b. Play-Protect-Safe Modify System Settings Prompt for 100ms Timeout Lock
    if (uiState.showLockPermissionDialog) {
      WriteSettingsLockDialog(
        onGrantWriteSettings = { viewModel.openWriteSettingsPermission(context) },
        onOpenSecuritySettings = {
          viewModel.dismissLockPermissionDialog()
          viewModel.openLockScreenSettings(context)
        },
        onDismiss = { viewModel.dismissLockPermissionDialog() }
      )
    }

    // 7. Interactive Frosted Feature Walkthrough Tour
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

@Composable
private fun WriteSettingsLockDialog(
  onGrantWriteSettings: () -> Unit,
  onOpenSecuritySettings: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    GlassSurface(
      shape = RoundedCornerShape(24.dp),
      backgroundColor = Color(0xEC16171B),
      modifier = Modifier
        .padding(horizontal = 24.dp)
        .widthIn(min = 280.dp, max = 420.dp)
        .testTag("lock_permission_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 22.dp, vertical = 20.dp)
      ) {
        Text(
          text = stringResource(R.string.lock_dialog_title),
          fontFamily = EditorialDisplayFontFamily,
          fontSize = 24.sp,
          color = MistColors.PureWhite
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = stringResource(R.string.lock_dialog_body),
          fontFamily = FunctionalSansFontFamily,
          fontSize = 13.sp,
          lineHeight = 19.sp,
          color = MistColors.OffWhite.copy(alpha = 0.88f)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = onGrantWriteSettings,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MistColors.PureWhite,
            contentColor = MistColors.NearBlack
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("lock_grant_write_settings_button")
        ) {
          Text(
            text = stringResource(R.string.lock_dialog_grant_write_settings),
            fontFamily = FunctionalSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onOpenSecuritySettings,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("lock_open_security_settings_button")
        ) {
          Text(
            text = stringResource(R.string.lock_dialog_open_security_settings),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 13.sp,
            color = MistColors.OffWhite
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
          onClick = onDismiss,
          modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .heightIn(min = 48.dp)
            .testTag("lock_dialog_cancel_button")
        ) {
          Text(
            text = stringResource(R.string.lock_dialog_cancel),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 12.5.sp,
            color = MistColors.MistGrey
          )
        }
      }
    }
  }
}
