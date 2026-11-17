package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.graphics.AppIconCache
import com.example.data.apps.AppIndexHelper
import com.example.data.apps.AppInfo
import com.example.data.apps.AppRepository
import com.example.data.apps.AppSection
import com.example.data.media.ActiveMediaState
import com.example.data.media.MediaRepository
import com.example.data.preferences.HomeAlphabetVisibility
import com.example.data.preferences.IconSize
import com.example.data.preferences.IconStyle
import com.example.data.preferences.LauncherPreferences
import com.example.data.preferences.PerformanceProfile
import com.example.data.preferences.PreferencesRepository
import com.example.data.preferences.QuickActionMode
import com.example.data.preferences.TypographyStyle
import com.example.data.wallpaper.ProcessedWallpaper
import com.example.data.wallpaper.WallpaperProcessor
import com.example.platform.DefaultLauncherHelper
import com.example.platform.SystemActionsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LauncherOverlay {
  NONE,
  SETTINGS,
  WALLPAPER_STUDIO,
  FAVORITES_MANAGER,
  HIDDEN_APPS
}

@Immutable
sealed interface DrawerRowItem {
  val rowKey: String
  val contentType: String

  @Immutable
  data class Header(
    override val rowKey: String,
    val title: String,
    val isFavoritesHeader: Boolean = false
  ) : DrawerRowItem {
    override val contentType: String = "header"
  }

  @Immutable
  data class App(
    override val rowKey: String,
    val app: AppInfo
  ) : DrawerRowItem {
    override val contentType: String = "app_row"
  }
}

@Immutable
data class LauncherUiState(
  val isLoading: Boolean = true,
  val allApps: List<AppInfo> = emptyList(),
  val visibleApps: List<AppInfo> = emptyList(),
  val sections: List<AppSection> = emptyList(),
  val favoriteApps: List<AppInfo> = emptyList(),
  val drawerRows: List<DrawerRowItem> = emptyList(),
  val sectionIndexMap: Map<String, Int> = emptyMap(),
  val homeShortcutApps: List<AppInfo> = emptyList(),
  val hiddenApps: List<AppInfo> = emptyList(),
  val searchQuery: String = "",
  val searchResults: List<AppInfo> = emptyList(),
  val isDrawerOpen: Boolean = false,
  val drawerJumpTarget: String? = null,
  val isSearchActive: Boolean = false,
  val showQuickActions: Boolean = false,
  val showAmbientCalmScreen: Boolean = false,
  val showWalkthrough: Boolean = false,
  val walkthroughStep: Int = 0,
  val activeOverlay: LauncherOverlay = LauncherOverlay.NONE,
  val contextMenuApp: AppInfo? = null,
  val isDefaultLauncher: Boolean = false,
  val activeMedia: ActiveMediaState = ActiveMediaState(),
  val preferences: LauncherPreferences = LauncherPreferences(),
  val wallpaper: ProcessedWallpaper = ProcessedWallpaper(null, null, isTopBright = true),
  val statusMessage: String? = null
)

private data class DerivedAppsBundle(
  val loading: Boolean,
  val allApps: List<AppInfo>,
  val visibleApps: List<AppInfo>,
  val sections: List<AppSection>,
  val favoriteApps: List<AppInfo>,
  val drawerRows: List<DrawerRowItem>,
  val sectionIndexMap: Map<String, Int>,
  val homeShortcutApps: List<AppInfo>,
  val hiddenApps: List<AppInfo>,
  val prefs: LauncherPreferences
)

private data class TransientUiState(
  val searchQuery: String = "",
  val isDrawerOpen: Boolean = false,
  val drawerJumpTarget: String? = null,
  val isSearchActive: Boolean = false,
  val showQuickActions: Boolean = false,
  val showAmbientCalmScreen: Boolean = false,
  val showWalkthrough: Boolean = false,
  val walkthroughStep: Int = 0,
  val activeOverlay: LauncherOverlay = LauncherOverlay.NONE,
  val contextMenuApp: AppInfo? = null,
  val isDefaultLauncher: Boolean = false,
  val statusMessage: String? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

  private val appContext = application.applicationContext
  val iconCache = AppIconCache(appContext)
  private val appRepository = AppRepository(appContext) { pkg ->
    iconCache.invalidatePackage(pkg)
  }
  private val mediaRepository = MediaRepository(appContext)
  private val preferencesRepository = PreferencesRepository(appContext)
  private val wallpaperProcessor = WallpaperProcessor(appContext)

  private val transientState = MutableStateFlow(
    TransientUiState(
      isDefaultLauncher = DefaultLauncherHelper.isDefaultLauncher(appContext)
    )
  )
  private val wallpaperState = MutableStateFlow(
    ProcessedWallpaper(homeBitmap = null, drawerBlurredBitmap = null, isTopBright = true)
  )

  private val _drawerJumpEvents = MutableSharedFlow<String>(
    extraBufferCapacity = 8,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val drawerJumpEvents: SharedFlow<String> = _drawerJumpEvents.asSharedFlow()

  private val _drawerContinuousScrubEvents = MutableSharedFlow<Float>(
    extraBufferCapacity = 4,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val drawerContinuousScrubEvents: SharedFlow<Float> = _drawerContinuousScrubEvents.asSharedFlow()

  // Heavy grouping, row flattening & filtering run on Dispatchers.Default ONLY when apps or preferences change
  private val derivedAppsFlow = combine(
    appRepository.apps,
    appRepository.isLoading,
    preferencesRepository.preferences
  ) { allApps, loading, prefs ->
    val appByKey = allApps.associateBy { it.componentKey }
    val hiddenSet = prefs.hiddenKeys
    val visibleApps = allApps.filterNot { it.componentKey in hiddenSet }
    val sections = AppIndexHelper.groupAppsBySection(visibleApps)
    val favoriteApps = prefs.favoriteKeys.mapNotNull { key -> appByKey[key] }
    val homeShortcutApps = favoriteApps.take(prefs.homeShortcutLimit)
    val hiddenApps = hiddenSet.mapNotNull { key -> appByKey[key] }
      .sortedBy { it.label.lowercase() }

    val drawerRows = ArrayList<DrawerRowItem>(visibleApps.size + favoriteApps.size + sections.size + 2)
    val sectionIndexMap = LinkedHashMap<String, Int>(sections.size + 1)
    if (favoriteApps.isNotEmpty()) {
      sectionIndexMap["★"] = 0
      drawerRows.add(DrawerRowItem.Header("header_favorites", "", isFavoritesHeader = true))
      for (fav in favoriteApps) {
        drawerRows.add(DrawerRowItem.App("fav_${fav.componentKey}", fav))
      }
    }
    for (section in sections) {
      sectionIndexMap[section.header] = drawerRows.size
      drawerRows.add(DrawerRowItem.Header("section_${section.header}", section.header))
      for (app in section.apps) {
        drawerRows.add(DrawerRowItem.App("${section.header}_${app.componentKey}", app))
      }
    }

    DerivedAppsBundle(
      loading = loading,
      allApps = allApps,
      visibleApps = visibleApps,
      sections = sections,
      favoriteApps = favoriteApps,
      drawerRows = drawerRows,
      sectionIndexMap = sectionIndexMap,
      homeShortcutApps = homeShortcutApps,
      hiddenApps = hiddenApps,
      prefs = prefs
    )
  }.flowOn(Dispatchers.Default)

  val uiState: StateFlow<LauncherUiState> = combine(
    derivedAppsFlow,
    wallpaperState,
    transientState,
    mediaRepository.mediaState
  ) { derived, wallpaper, transient, media ->
    val searchResults = if (transient.isSearchActive) {
      AppIndexHelper.filterApps(derived.visibleApps, transient.searchQuery)
    } else {
      emptyList()
    }

    LauncherUiState(
      isLoading = derived.loading,
      allApps = derived.allApps,
      visibleApps = derived.visibleApps,
      sections = derived.sections,
      favoriteApps = derived.favoriteApps,
      drawerRows = derived.drawerRows,
      sectionIndexMap = derived.sectionIndexMap,
      homeShortcutApps = derived.homeShortcutApps,
      hiddenApps = derived.hiddenApps,
      searchQuery = transient.searchQuery,
      searchResults = searchResults,
      isDrawerOpen = transient.isDrawerOpen,
      drawerJumpTarget = transient.drawerJumpTarget,
      isSearchActive = transient.isSearchActive,
      showQuickActions = transient.showQuickActions,
      showAmbientCalmScreen = transient.showAmbientCalmScreen,
      showWalkthrough = transient.showWalkthrough,
      walkthroughStep = transient.walkthroughStep,
      activeOverlay = transient.activeOverlay,
      contextMenuApp = transient.contextMenuApp,
      isDefaultLauncher = transient.isDefaultLauncher,
      activeMedia = media,
      preferences = derived.prefs,
      wallpaper = wallpaper,
      statusMessage = transient.statusMessage
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000L),
    initialValue = LauncherUiState()
  )

  init {
    appRepository.startObserving()

    viewModelScope.launch {
      val (apps, prefs) = combine(appRepository.apps, preferencesRepository.preferences) { a, p ->
        a to p
      }.first { (a, p) -> p.favoritesSeeded || a.isNotEmpty() }

      if (!prefs.favoritesSeeded && apps.isNotEmpty()) {
        val seeded = pickInitialFavorites(apps, prefs.homeShortcutLimit)
        preferencesRepository.updateFavorites(seeded, markSeeded = true)
      }
    }

    viewModelScope.launch {
      preferencesRepository.preferences
        .map { prefs ->
          WallpaperKey(
            path = prefs.customWallpaperPath,
            grayscale = prefs.grayscaleWallpaper,
            blur = prefs.drawerBlurIntensity,
            lowEffects = prefs.effectiveLowEffects
          )
        }
        .distinctUntilChanged()
        .collect { key ->
          val processed = wallpaperProcessor.loadProcessedWallpaper(
            customPath = key.path,
            grayscale = key.grayscale,
            blurIntensity = key.blur,
            lowEffectsMode = key.lowEffects
          )
          wallpaperState.value = processed
        }
    }

    // One-shot background prewarm of app icons in Ultra-Smooth mode so App Drawer opens & scrolls at 60-120 FPS;
    // skipped in Low Power mode to avoid background CPU/RAM work.
    viewModelScope.launch {
      combine(
        appRepository.apps,
        preferencesRepository.preferences
          .map { it.iconStyle to it.isPowerSaver }
          .distinctUntilChanged()
      ) { apps, (style, powerSaver) -> Triple(apps, style, powerSaver) }
        .collect { (apps, style, powerSaver) ->
          if (!powerSaver && apps.isNotEmpty()) {
            iconCache.prewarm(apps, style)
          }
        }
    }
  }

  private data class WallpaperKey(
    val path: String?,
    val grayscale: Boolean,
    val blur: Float,
    val lowEffects: Boolean
  )

  private fun pickInitialFavorites(apps: List<AppInfo>, limit: Int): List<String> {
    val priorityHints = listOf(
      "dialer", "phone", "contacts",
      "messaging", "messages", "mms",
      "camera",
      "chrome", "browser",
      "gallery", "photos",
      "settings",
      "calendar", "clock"
    )
    val chosen = LinkedHashSet<String>()
    for (hint in priorityHints) {
      if (chosen.size >= limit) break
      val match = apps.firstOrNull { app ->
        app.componentKey !in chosen &&
          (app.packageName.contains(hint, ignoreCase = true) ||
            app.label.contains(hint, ignoreCase = true))
      }
      if (match != null) chosen.add(match.componentKey)
    }
    for (app in apps) {
      if (chosen.size >= limit) break
      chosen.add(app.componentKey)
    }
    return chosen.toList()
  }

  fun onAppStart() {
    mediaRepository.startObserving()
    refreshSystemStatus()
  }

  fun onAppStop() {
    mediaRepository.stopObserving()
  }

  fun refreshSystemStatus() {
    transientState.update {
      it.copy(
        isDefaultLauncher = DefaultLauncherHelper.isDefaultLauncher(appContext)
      )
    }
  }

  fun onHomeIntentReceived() {
    transientState.update {
      it.copy(
        isDrawerOpen = false,
        drawerJumpTarget = null,
        isSearchActive = false,
        searchQuery = "",
        showQuickActions = false,
        showAmbientCalmScreen = false,
        showWalkthrough = false,
        activeOverlay = LauncherOverlay.NONE,
        contextMenuApp = null
      )
    }
  }

  fun openDrawer(withSearch: Boolean = false, jumpToSection: String? = null) {
    transientState.update {
      it.copy(
        isDrawerOpen = true,
        drawerJumpTarget = jumpToSection,
        isSearchActive = withSearch,
        searchQuery = if (withSearch) it.searchQuery else "",
        showQuickActions = false,
        showAmbientCalmScreen = false,
        activeOverlay = LauncherOverlay.NONE,
        contextMenuApp = null
      )
    }
  }

  fun updateDrawerJumpTarget(sectionKey: String?) {
    if (transientState.value.drawerJumpTarget == sectionKey) return
    transientState.update { it.copy(drawerJumpTarget = sectionKey) }
  }

  fun emitDrawerJump(sectionKey: String) {
    _drawerJumpEvents.tryEmit(sectionKey)
  }

  fun emitDrawerContinuousScrub(continuousIndex: Float) {
    _drawerContinuousScrubEvents.tryEmit(continuousIndex)
  }

  fun closeDrawer() {
    transientState.update {
      it.copy(
        isDrawerOpen = false,
        drawerJumpTarget = null,
        isSearchActive = false,
        searchQuery = "",
        contextMenuApp = null
      )
    }
  }

  fun setSearchActive(active: Boolean) {
    transientState.update {
      it.copy(
        isSearchActive = active,
        searchQuery = if (active) it.searchQuery else ""
      )
    }
  }

  fun updateSearchQuery(query: String) {
    transientState.update { it.copy(searchQuery = query) }
  }

  fun onHomePowerButtonTap() {
    when (uiState.value.preferences.quickActionMode) {
      QuickActionMode.QUICK_PANEL -> toggleQuickActions(true)
      QuickActionMode.OLED_CALM_SCREEN -> setAmbientCalmScreen(true)
      QuickActionMode.DIRECT_SEARCH -> openDrawer(withSearch = true)
      QuickActionMode.DIRECT_LIBRARY -> openDrawer(withSearch = false)
    }
  }

  fun setAmbientCalmScreen(show: Boolean) {
    transientState.update {
      it.copy(
        showAmbientCalmScreen = show,
        showQuickActions = false,
        contextMenuApp = null,
        activeOverlay = if (show) LauncherOverlay.NONE else it.activeOverlay
      )
    }
  }

  fun openClockApp() {
    if (!SystemActionsHelper.openClockApp(appContext)) {
      transientState.update { it.copy(statusMessage = "No Clock app found.") }
    }
  }

  fun openCalendarApp() {
    if (!SystemActionsHelper.openCalendarApp(appContext)) {
      transientState.update { it.copy(statusMessage = "No Calendar app found.") }
    }
  }

  fun toggleMediaPlayPause() = mediaRepository.togglePlayPause()

  fun skipMediaNext() = mediaRepository.skipToNext()

  fun skipMediaPrevious() = mediaRepository.skipToPrevious()

  fun openActiveMediaPlayer() = mediaRepository.openActivePlayer()

  fun toggleQuickActions(show: Boolean) {
    transientState.update {
      it.copy(showQuickActions = show, contextMenuApp = null, showAmbientCalmScreen = false)
    }
  }

  fun openOverlay(overlay: LauncherOverlay) {
    transientState.update {
      it.copy(
        activeOverlay = overlay,
        showQuickActions = false,
        showAmbientCalmScreen = false,
        contextMenuApp = null
      )
    }
  }

  fun closeOverlay() {
    transientState.update { it.copy(activeOverlay = LauncherOverlay.NONE) }
  }

  fun openContextMenu(app: AppInfo) {
    transientState.update { it.copy(contextMenuApp = app, showQuickActions = false) }
  }

  fun closeContextMenu() {
    transientState.update { it.copy(contextMenuApp = null) }
  }

  fun startWalkthrough(initialStep: Int = 0) {
    transientState.update {
      it.copy(
        isDrawerOpen = false,
        drawerJumpTarget = null,
        isSearchActive = false,
        showQuickActions = false,
        showAmbientCalmScreen = false,
        activeOverlay = LauncherOverlay.NONE,
        contextMenuApp = null,
        showWalkthrough = true,
        walkthroughStep = initialStep.coerceIn(0, 5)
      )
    }
  }

  fun setWalkthroughStep(step: Int) {
    transientState.update { it.copy(walkthroughStep = step.coerceIn(0, 5)) }
  }

  fun finishWalkthrough() {
    transientState.update { it.copy(showWalkthrough = false, walkthroughStep = 0) }
    viewModelScope.launch {
      preferencesRepository.setWalkthroughCompleted(true)
    }
  }

  fun launchApp(app: AppInfo) {
    val launched = appRepository.launchApp(app)
    if (!launched) {
      transientState.update {
        it.copy(statusMessage = "${app.label} is unavailable.")
      }
    }
  }

  fun clearStatusMessage() {
    transientState.update { it.copy(statusMessage = null) }
  }

  fun toggleFavorite(app: AppInfo) {
    viewModelScope.launch {
      val current = uiState.value.preferences.favoriteKeys.toMutableList()
      if (app.componentKey in current) {
        current.remove(app.componentKey)
      } else {
        current.add(app.componentKey)
      }
      preferencesRepository.updateFavorites(current)
      closeContextMenu()
    }
  }

  fun moveFavorite(componentKey: String, delta: Int) {
    viewModelScope.launch {
      val current = uiState.value.preferences.favoriteKeys.toMutableList()
      val index = current.indexOf(componentKey)
      if (index == -1) return@launch
      val target = (index + delta).coerceIn(0, current.lastIndex)
      if (target == index) return@launch
      val item = current.removeAt(index)
      current.add(target, item)
      preferencesRepository.updateFavorites(current)
    }
  }

  fun toggleHideApp(app: AppInfo) {
    viewModelScope.launch {
      val hidden = uiState.value.preferences.hiddenKeys.toMutableSet()
      if (app.componentKey in hidden) {
        hidden.remove(app.componentKey)
      } else {
        hidden.add(app.componentKey)
      }
      preferencesRepository.setHiddenKeys(hidden)
      closeContextMenu()
    }
  }

  fun openAppInfo(app: AppInfo) {
    SystemActionsHelper.openAppInfo(appContext, app.packageName)
    closeContextMenu()
  }

  fun requestUninstall(app: AppInfo) {
    SystemActionsHelper.requestUninstall(appContext, app.packageName)
    closeContextMenu()
  }

  fun openDeviceSettings() {
    SystemActionsHelper.openDeviceSettings(appContext)
    toggleQuickActions(false)
  }

  fun openLockScreenSettings(context: Context = appContext) {
    if (!SystemActionsHelper.openLockScreenSettings(context)) {
      transientState.update { it.copy(statusMessage = "Screen lock settings unavailable.") }
    }
    toggleQuickActions(false)
    setAmbientCalmScreen(false)
  }

  fun applyPickedWallpaper(uri: Uri) {
    viewModelScope.launch {
      val savedPath = wallpaperProcessor.persistPickedWallpaper(uri)
      if (savedPath != null) {
        preferencesRepository.setCustomWallpaperPath(savedPath)
      } else {
        transientState.update { it.copy(statusMessage = "Could not load selected image.") }
      }
    }
  }

  fun restoreDefaultWallpaper() {
    viewModelScope.launch {
      wallpaperProcessor.clearCustomWallpaperFiles()
      preferencesRepository.setCustomWallpaperPath(null)
    }
  }

  fun setGrayscaleWallpaper(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setGrayscaleWallpaper(enabled) }
  }

  fun setWallpaperDarkening(amount: Float) {
    viewModelScope.launch { preferencesRepository.setWallpaperDarkening(amount) }
  }

  fun setDrawerBlurIntensity(amount: Float) {
    viewModelScope.launch { preferencesRepository.setDrawerBlurIntensity(amount) }
  }

  fun setIconStyle(style: IconStyle) {
    viewModelScope.launch { preferencesRepository.setIconStyle(style) }
  }

  fun setIconSize(size: IconSize) {
    viewModelScope.launch { preferencesRepository.setIconSize(size) }
  }

  fun setTypographyStyle(style: TypographyStyle) {
    viewModelScope.launch { preferencesRepository.setTypographyStyle(style) }
  }

  fun setHomeShortcutLimit(limit: Int) {
    viewModelScope.launch { preferencesRepository.setHomeShortcutLimit(limit) }
  }

  fun setShowClock(visible: Boolean) {
    viewModelScope.launch { preferencesRepository.setShowClock(visible) }
  }

  fun setShowDate(visible: Boolean) {
    viewModelScope.launch { preferencesRepository.setShowDate(visible) }
  }

  fun setShowHomeLabels(visible: Boolean) {
    viewModelScope.launch { preferencesRepository.setShowHomeLabels(visible) }
  }

  fun setShowAlphabetIndex(visible: Boolean) {
    viewModelScope.launch { preferencesRepository.setShowAlphabetIndex(visible) }
  }

  fun setShowLeftHiddenAlphabet(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setShowLeftHiddenAlphabet(enabled) }
  }

  fun setHomeAlphabetVisibility(visibility: HomeAlphabetVisibility) {
    viewModelScope.launch { preferencesRepository.setHomeAlphabetVisibility(visibility) }
  }

  fun setSwipeUpOpensDrawer(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setSwipeUpOpensDrawer(enabled) }
  }

  fun setSearchAutoFocusKeyboard(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setSearchAutoFocusKeyboard(enabled) }
  }

  fun setPerformanceProfile(profile: PerformanceProfile) {
    viewModelScope.launch { preferencesRepository.setPerformanceProfile(profile) }
  }

  fun setReducedMotion(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setReducedMotion(enabled) }
  }

  fun setLowEffectsMode(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setLowEffectsMode(enabled) }
  }

  fun setHapticFeedback(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setHapticFeedback(enabled) }
  }

  fun setQuickActionMode(mode: QuickActionMode) {
    viewModelScope.launch { preferencesRepository.setQuickActionMode(mode) }
  }

  fun dismissOnboarding() {
    viewModelScope.launch { preferencesRepository.setOnboardingDismissed(true) }
  }

  override fun onCleared() {
    super.onCleared()
    appRepository.stopObserving()
    mediaRepository.stopObserving()
    iconCache.clear()
  }
}
