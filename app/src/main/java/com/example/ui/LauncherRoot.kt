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
import com.example.ui.components.GlassSurface
import com.example.ui.drawer.AppDrawerScreen
import com.example.ui.home.HomeScreen
import com.example.ui.overlays.AppContextMenuDialog
import com.example.ui.overlays.LockPermissionDialog
import com.example.ui.overlays.QuickActionsOverlay
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
  var isHomeRailScrubbing by remember { mutableStateOf(false) }

  val isOverlayBlockingLightTop = uiState.isDrawerOpen ||
    uiState.showQuickActions ||
    uiState.showLockPermissionPrompt ||
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
    prefs.reducedMotion -> 0
    uiState.isDrawerOpen -> 260
    else -> 210
  }

  val drawerProgress by animateFloatAsState(
    targetValue = if (uiState.isDrawerOpen) 1f else 0f,
    animationSpec = tween(
      durationMillis = transitionDurationMs,
      easing = FastOutSlowInEasing
    ),
    label = "drawer_progress"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MistColors.NearBlack)
  ) {
    // 1. Base sharp wallpaper layer (visible on Home & Wallpaper Studio)
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
        .background(
          Brush.verticalGradient(
            colorStops = arrayOf(
              0.0f to Color.Transparent,
              0.55f to Color.Transparent,
              1.0f to Color(0x780B0B0D)
            )
          )
        )
    )

    // User-configurable wallpaper darkening veil
    if (prefs.wallpaperDarkening > 0f) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MistColors.NearBlack.copy(alpha = prefs.wallpaperDarkening))
      )
    }

    // 2. Pre-blurred frosted-glass wallpaper layer for the App Drawer
    if (drawerProgress > 0.005f) {
      val drawerBmp = uiState.wallpaper.drawerBlurredBitmap ?: homeBmp
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer { alpha = drawerProgress }
      ) {
        if (drawerBmp != null) {
          Image(
            bitmap = drawerBmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
        // Soft frosted grey veil + balanced charcoal legibility scrim
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              MistColors.FrostedGrey.copy(
                alpha = (0.14f + prefs.drawerBlurIntensity * 0.14f).coerceIn(0.08f, 0.30f)
              )
            )
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0x68121215))
        )
      }
    }

    // 3. App Drawer layer (renders sleek sliding entrance beneath Home's active scrub wave when scrubbing from Home)
    if (drawerProgress > 0.005f && uiState.activeOverlay == LauncherOverlay.NONE) {
      AppDrawerScreen(
        uiState = uiState,
        iconCache = iconCache,
        hideAlphabetRail = isHomeRailScrubbing,
        onJumpTargetHandled = { viewModel.updateDrawerJumpTarget(null) },
        onLaunchApp = { viewModel.launchApp(it) },
        onLongPressApp = { viewModel.openContextMenu(it) },
        onSearchActiveChange = { viewModel.setSearchActive(it) },
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
        onCloseDrawer = { viewModel.closeDrawer() },
        modifier = Modifier.graphicsLayer {
          alpha = drawerProgress
          translationY = (1f - drawerProgress) * 72f
          val scale = 0.96f + 0.04f * drawerProgress
          scaleX = scale
          scaleY = scale
        }
      )
    }

    // 4. Home Screen layer (stays alive during active Home Alphabet Rail drag so touch scrub never drops)
    if ((drawerProgress < 0.995f || isHomeRailScrubbing) && uiState.activeOverlay == LauncherOverlay.NONE) {
      HomeScreen(
        uiState = uiState,
        clockSnapshot = clockSnapshot,
        iconCache = iconCache,
        contentAlpha = (1f - drawerProgress * 1.25f).coerceIn(0f, 1f),
        contentTranslationY = -drawerProgress * 40f,
        onOpenDrawer = { viewModel.openDrawer(withSearch = false) },
        onScrubToDrawerSection = { key ->
          isHomeRailScrubbing = true
          if (uiState.isDrawerOpen) {
            viewModel.updateDrawerJumpTarget(key)
          } else {
            viewModel.openDrawer(withSearch = false, jumpToSection = key)
          }
        },
        onScrubDragEnd = {
          isHomeRailScrubbing = false
          viewModel.updateDrawerJumpTarget(null)
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
        onDismissOnboarding = { viewModel.dismissOnboarding() }
      )
    }

    // 5. Secondary Settings / Customization overlays
    val overlayFadeDuration = if (prefs.reducedMotion) 0 else 180
    AnimatedVisibility(
      visible = uiState.activeOverlay == LauncherOverlay.SETTINGS,
      enter = fadeIn(tween(overlayFadeDuration)),
      exit = fadeOut(tween(overlayFadeDuration))
    ) {
      SettingsScreen(
        uiState = uiState,
        onSetIconStyle = viewModel::setIconStyle,
        onSetIconSize = viewModel::setIconSize,
        onSetTypographyStyle = viewModel::setTypographyStyle,
        onSetShowClock = viewModel::setShowClock,
        onSetShowDate = viewModel::setShowDate,
        onSetShowHomeLabels = viewModel::setShowHomeLabels,
        onSetShowAlphabetIndex = viewModel::setShowAlphabetIndex,
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
        onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
        onOpenNotificationListenerSettings = { viewModel.openNotificationListenerSettings() },
        onRequestDefaultHome = onRequestDefaultHome,
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
    val quickDuration = if (prefs.reducedMotion) 0 else 160
    AnimatedVisibility(
      visible = uiState.showQuickActions,
      enter = fadeIn(tween(quickDuration)),
      exit = fadeOut(tween(quickDuration))
    ) {
      QuickActionsOverlay(
        isDefaultLauncher = uiState.isDefaultLauncher,
        isMediaListenerEnabled = uiState.isMediaListenerEnabled,
        onLockScreen = { viewModel.lockScreenOrPrompt() },
        onOpenLibrary = { viewModel.openDrawer(withSearch = false) },
        onOpenSearch = { viewModel.openDrawer(withSearch = true) },
        onOpenWallpaper = { viewModel.openOverlay(LauncherOverlay.WALLPAPER_STUDIO) },
        onOpenFavorites = { viewModel.openOverlay(LauncherOverlay.FAVORITES_MANAGER) },
        onOpenPreferences = { viewModel.openOverlay(LauncherOverlay.SETTINGS) },
        onOpenMediaListenerSettings = { viewModel.openNotificationListenerSettings() },
        onOpenDeviceSettings = { viewModel.openDeviceSettings() },
        onRequestDefaultHome = {
          viewModel.toggleQuickActions(false)
          onRequestDefaultHome()
        },
        onDismiss = { viewModel.toggleQuickActions(false) }
      )
    }

    // 7. Accessibility Lock Permission Guidance Dialog
    AnimatedVisibility(
      visible = uiState.showLockPermissionPrompt,
      enter = fadeIn(tween(quickDuration)),
      exit = fadeOut(tween(quickDuration))
    ) {
      LockPermissionDialog(
        onOpenAccessibility = { viewModel.openAccessibilitySettings() },
        onOpenQuickActions = { viewModel.toggleQuickActions(true) },
        onDismiss = { viewModel.dismissLockPermissionPrompt() }
      )
    }

    // 8. Contextual Long-Press App Menu
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
