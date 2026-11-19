package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.graphics.AppIconCache
import com.example.core.haptics.LocalMistHaptics
import com.example.core.haptics.MistHapticEvent
import com.example.core.haptics.rememberMistHaptics
import com.example.data.apps.AppIndexHelper
import com.example.data.apps.AppInfo
import com.example.data.preferences.IconStyle
import com.example.platform.rememberSystemClockSnapshot
import com.example.ui.components.AlphabetRailSide
import com.example.ui.components.GlassSurface
import com.example.ui.components.MistAppIcon
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
  val mistHaptics = rememberMistHaptics(enabled = prefs.hapticFeedback, profile = prefs.hapticProfile)
  var activeHomeScrubSide by remember { mutableStateOf<AlphabetRailSide?>(null) }
  val homeScrubPullRatio = remember { mutableFloatStateOf(0f) }

  // Calmly absorb system Back press when resting on the Home workspace so Android does not finish & reload Home
  val isRestingOnHome = !uiState.isDrawerOpen &&
    uiState.activeOverlay == LauncherOverlay.NONE &&
    !uiState.showQuickActions &&
    !uiState.showWalkthrough &&
    uiState.contextMenuApp == null &&
    !uiState.showLockAppPickerDialog
  BackHandler(enabled = isRestingOnHome) {
    // Deliberate calm no-op on Home screen
  }

  val isOverlayBlockingLightTop = uiState.isDrawerOpen ||
    uiState.showQuickActions ||
    uiState.showWalkthrough ||
    uiState.contextMenuApp != null ||
    uiState.showLockAppPickerDialog ||
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

  // Read drawerProgressState.value ONLY inside derivedStateOf and graphicsLayer lambdas (0 per-frame recompositions!)
  val drawerProgressState = animateFloatAsState(
    targetValue = if (uiState.isDrawerOpen) 1f else 0f,
    animationSpec = if (prefs.effectiveReducedMotion) {
      snap()
    } else {
      spring(
        dampingRatio = 0.92f,
        stiffness = 480f,
        visibilityThreshold = 0.002f
      )
    },
    label = "drawer_progress"
  )

  val showDrawerLayer by remember {
    derivedStateOf { drawerProgressState.value > 0.005f }
  }
  val showHomeLayer by remember {
    derivedStateOf { drawerProgressState.value < 0.995f || activeHomeScrubSide != null }
  }

  CompositionLocalProvider(LocalMistHaptics provides mistHaptics) {
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
        hideRightAlphabetRail = activeHomeScrubSide != null,
        hideLeftAlphabetRail = activeHomeScrubSide == AlphabetRailSide.LEFT,
        homeScrubPullProvider = { homeScrubPullRatio.floatValue },
        onJumpTargetHandled = { viewModel.updateDrawerJumpTarget(null) },
        onLaunchApp = { viewModel.launchApp(it, context) },
        onLongPressApp = { viewModel.openContextMenu(it) },
        onSearchActiveChange = { viewModel.setSearchActive(it) },
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
        onCloseDrawer = { viewModel.closeDrawer() },
        modifier = Modifier.graphicsLayer {
          val p = drawerProgressState.value
          alpha = p
          translationY = (1f - p) * 56f
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
          if (viewModel.uiState.value.isDrawerOpen) {
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
        onLaunchApp = { viewModel.launchApp(it, context) },
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
        onDismissOnboarding = { viewModel.dismissOnboarding() },
        onDismissQuickActionsHint = { viewModel.dismissQuickActionsHint() }
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
        onSetAlphabetFocusBlur = viewModel::setAlphabetFocusBlur,
        onSetHomeAlphabetVisibility = viewModel::setHomeAlphabetVisibility,
        onSetHomeShortcutLimit = viewModel::setHomeShortcutLimit,
        onSetQuickActionMode = viewModel::setQuickActionMode,
        onSetSwipeUpOpensDrawer = viewModel::setSwipeUpOpensDrawer,
        onSetSearchAutoFocus = viewModel::setSearchAutoFocusKeyboard,
        onSetReducedMotion = viewModel::setReducedMotion,
        onSetLowEffectsMode = viewModel::setLowEffectsMode,
        onSetHapticFeedback = viewModel::setHapticFeedback,
        onSetHapticProfile = viewModel::setHapticProfile,
        onOpenWallpaperStudio = { viewModel.openOverlay(LauncherOverlay.WALLPAPER_STUDIO) },
        onOpenFavoritesManager = { viewModel.openOverlay(LauncherOverlay.FAVORITES_MANAGER) },
        onOpenHiddenApps = { viewModel.openOverlay(LauncherOverlay.HIDDEN_APPS) },
        onOpenLockAppPicker = {
          viewModel.openLockAppPicker(launchAfterPick = false)
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
        linkedLockAppLabel = uiState.linkedLockApp?.label,
        reducedMotion = prefs.effectiveReducedMotion,
        onLockScreen = { viewModel.triggerScreenLock(context) },
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

    // 6b. Link an Android Lock Screen app dialog
    if (uiState.showLockAppPickerDialog) {
      LockAppPickerDialog(
        suggestedApps = uiState.suggestedLockApps,
        allApps = uiState.allApps,
        linkedApp = uiState.linkedLockApp,
        iconCache = iconCache,
        iconStyle = prefs.iconStyle,
        reducedMotion = prefs.effectiveReducedMotion,
        lowPowerMode = prefs.effectiveLowEffects,
        onSelectApp = { viewModel.selectLockScreenApp(it, context) },
        onClearLink = { viewModel.clearLockScreenApp() },
        onOpenSecuritySettings = {
          viewModel.dismissLockAppPicker()
          viewModel.openLockScreenSettings(context)
        },
        onDismiss = { viewModel.dismissLockAppPicker() }
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
      enter = if (prefs.effectiveReducedMotion) EnterTransition.None else fadeIn(tween(150)),
      exit = if (prefs.effectiveReducedMotion) ExitTransition.None else fadeOut(tween(150)),
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
}

private val BOTTOM_ATMOSPHERIC_SCRIM = Brush.verticalGradient(
  colorStops = arrayOf(
    0.0f to Color.Transparent,
    0.55f to Color.Transparent,
    1.0f to Color(0x780B0B0D)
  )
)

@Composable
private fun LockAppPickerDialog(
  suggestedApps: List<AppInfo>,
  allApps: List<AppInfo>,
  linkedApp: AppInfo?,
  iconCache: AppIconCache,
  iconStyle: IconStyle,
  reducedMotion: Boolean,
  lowPowerMode: Boolean,
  onSelectApp: (AppInfo) -> Unit,
  onClearLink: () -> Unit,
  onOpenSecuritySettings: () -> Unit,
  onDismiss: () -> Unit
) {
  val haptics = LocalMistHaptics.current
  var filterQuery by remember { mutableStateOf("") }
  val suggestedKeys = remember(suggestedApps) { suggestedApps.map { it.componentKey }.toSet() }
  val filteredSuggested = remember(suggestedApps, filterQuery) {
    AppIndexHelper.filterApps(suggestedApps, filterQuery)
  }
  val filteredOtherApps = remember(allApps, suggestedKeys, filterQuery) {
    val others = allApps.filterNot { it.componentKey in suggestedKeys }
    AppIndexHelper.filterApps(others, filterQuery)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    GlassSurface(
      shape = RoundedCornerShape(24.dp),
      backgroundColor = Color(0xEC16171B),
      modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 28.dp)
        .fillMaxWidth()
        .widthIn(max = 440.dp)
        .heightIn(max = 580.dp)
        .testTag("lock_app_picker_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 18.dp)
      ) {
        Text(
          text = stringResource(R.string.lock_app_dialog_title),
          fontFamily = EditorialDisplayFontFamily,
          fontSize = 24.sp,
          color = MistColors.PureWhite
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = if (linkedApp != null) {
            "Currently linked to ${linkedApp.label}. Pick another Android Lock Screen app or utility below."
          } else {
            stringResource(R.string.lock_app_dialog_body)
          },
          fontFamily = FunctionalSansFontFamily,
          fontSize = 12.5.sp,
          lineHeight = 18.sp,
          color = MistColors.OffWhite.copy(alpha = 0.86f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x36FFFFFF))
            .border(0.7.dp, Color(0x40FFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MistColors.MistGrey,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (filterQuery.isEmpty()) {
              Text(
                text = "Filter installed apps (e.g. Lock Screen)…",
                fontFamily = FunctionalSansFontFamily,
                fontSize = 13.sp,
                color = MistColors.FrostedGrey
              )
            }
            BasicTextField(
              value = filterQuery,
              onValueChange = { filterQuery = it },
              singleLine = true,
              textStyle = TextStyle(
                fontFamily = FunctionalSansFontFamily,
                fontSize = 13.5.sp,
                color = MistColors.PureWhite
              ),
              cursorBrush = SolidColor(MistColors.PureWhite),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("lock_app_picker_search")
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .heightIn(min = 120.dp, max = 280.dp)
            .testTag("lock_app_picker_list"),
          contentPadding = PaddingValues(vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          if (filteredSuggested.isNotEmpty()) {
            item(key = "hdr_suggested") {
              Text(
                text = "DETECTED LOCK SCREEN APPS",
                fontFamily = FunctionalSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                letterSpacing = 0.9.sp,
                color = MistColors.FrostedGrey,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
              )
            }
            items(
              items = filteredSuggested,
              key = { "sugg_${it.componentKey}" }
            ) { app ->
              LockPickerAppRow(
                app = app,
                isLinked = linkedApp?.componentKey == app.componentKey,
                iconCache = iconCache,
                iconStyle = iconStyle,
                reducedMotion = reducedMotion,
                lowPowerMode = lowPowerMode,
                onClick = {
                  haptics.trigger(MistHapticEvent.CONFIRM)
                  onSelectApp(app)
                }
              )
            }
          }

          if (filteredOtherApps.isNotEmpty()) {
            item(key = "hdr_all") {
              Text(
                text = if (filteredSuggested.isEmpty()) "INSTALLED APPS" else "ALL OTHER APPS",
                fontFamily = FunctionalSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                letterSpacing = 0.9.sp,
                color = MistColors.FrostedGrey,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
              )
            }
            items(
              items = filteredOtherApps,
              key = { "all_${it.componentKey}" }
            ) { app ->
              LockPickerAppRow(
                app = app,
                isLinked = linkedApp?.componentKey == app.componentKey,
                iconCache = iconCache,
                iconStyle = iconStyle,
                reducedMotion = reducedMotion,
                lowPowerMode = lowPowerMode,
                onClick = {
                  haptics.trigger(MistHapticEvent.CONFIRM)
                  onSelectApp(app)
                }
              )
            }
          }

          if (filteredSuggested.isEmpty() && filteredOtherApps.isEmpty()) {
            item(key = "empty_lock_apps") {
              Text(
                text = "No matching apps found. Install a Lock Screen app or open Android Lock Screen Settings below.",
                fontFamily = FunctionalSansFontFamily,
                fontSize = 12.5.sp,
                color = MistColors.MistGrey,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp)
              )
            }
          }
        }

        HorizontalDivider(
          color = MistColors.HairlineDivider,
          modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedButton(
          onClick = {
            haptics.trigger(MistHapticEvent.TAP_LIGHT)
            onOpenSecuritySettings()
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("lock_open_security_settings_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MistColors.OffWhite,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.lock_dialog_open_security_settings),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MistColors.OffWhite
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (linkedApp != null) Arrangement.SpaceBetween else Arrangement.End
        ) {
          if (linkedApp != null) {
            TextButton(
              onClick = {
                haptics.trigger(MistHapticEvent.TAP_LIGHT)
                onClearLink()
              },
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("lock_clear_link_button")
            ) {
              Text(
                text = "Unlink App",
                fontFamily = FunctionalSansFontFamily,
                fontSize = 12.5.sp,
                color = MistColors.MistGrey
              )
            }
          }
          TextButton(
            onClick = {
              haptics.trigger(MistHapticEvent.TAP_LIGHT)
              onDismiss()
            },
            modifier = Modifier
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
}

@Composable
private fun LockPickerAppRow(
  app: AppInfo,
  isLinked: Boolean,
  iconCache: AppIconCache,
  iconStyle: IconStyle,
  reducedMotion: Boolean,
  lowPowerMode: Boolean,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(if (isLinked) Color(0x28FFFFFF) else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .testTag("lock_picker_app_${app.packageName}")
  ) {
    MistAppIcon(
      app = app,
      iconCache = iconCache,
      iconStyle = iconStyle,
      tileSize = 32.dp,
      reducedMotion = reducedMotion,
      lowPowerMode = lowPowerMode
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = app.label,
        fontFamily = FunctionalSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MistColors.PureWhite,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = app.packageName,
        fontFamily = FunctionalSansFontFamily,
        fontSize = 10.5.sp,
        color = MistColors.FrostedGrey,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (isLinked) {
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = "Linked",
        tint = MistColors.PureWhite,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
