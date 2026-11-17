package com.example.ui

import android.app.Application
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
import com.example.data.preferences.PreferencesRepository
import com.example.data.preferences.QuickActionMode
import com.example.data.preferences.TypographyStyle
import com.example.data.wallpaper.ProcessedWallpaper
import com.example.data.wallpaper.WallpaperProcessor
import com.example.platform.DefaultLauncherHelper
import com.example.platform.GlassbonLockAccessibilityService
import com.example.platform.GlassbonMediaListenerService
import com.example.platform.SystemActionsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
data class LauncherUiState(
  val isLoading: Boolean = true,
  val allApps: List<AppInfo> = emptyList(),
  val visibleApps: List<AppInfo> = emptyList(),
  val sections: List<AppSection> = emptyList(),
  val favoriteApps: List<AppInfo> = emptyList(),
  val homeShortcutApps: List<AppInfo> = emptyList(),
  val hiddenApps: List<AppInfo> = emptyList(),
  val searchQuery: String = "",
  val searchResults: List<AppInfo> = emptyList(),
  val isDrawerOpen: Boolean = false,
  val drawerJumpTarget: String? = null,
  val isSearchActive: Boolean = false,
  val showQuickActions: Boolean = false,
  val showLockPermissionPrompt: Boolean = false,
  val activeOverlay: LauncherOverlay = LauncherOverlay.NONE,
  val contextMenuApp: AppInfo? = null,
  val isDefaultLauncher: Boolean = false,
  val isLockAccessibilityEnabled: Boolean = false,
  val isMediaListenerEnabled: Boolean = false,
  val activeMedia: ActiveMediaState = ActiveMediaState(),
  val preferences: LauncherPreferences = LauncherPreferences(),
  val wallpaper: ProcessedWallpaper = ProcessedWallpaper(null, null, isTopBright = true),
  val statusMessage: String? = null
)

private data class TransientUiState(
  val searchQuery: String = "",
  val isDrawerOpen: Boolean = false,
  val drawerJumpTarget: String? = null,
  val isSearchActive: Boolean = false,
  val showQuickActions: Boolean = false,
  val showLockPermissionPrompt: Boolean = false,
  val activeOverlay: LauncherOverlay = LauncherOverlay.NONE,
  val contextMenuApp: AppInfo? = null,
  val isDefaultLauncher: Boolean = false,
  val isLockAccessibilityEnabled: Boolean = false,
  val isMediaListenerEnabled: Boolean = false,
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
      isDefaultLauncher = DefaultLauncherHelper.isDefaultLauncher(appContext),
      isLockAccessibilityEnabled = GlassbonLockAccessibilityService.isServiceEnabled(appContext),
      isMediaListenerEnabled = GlassbonMediaListenerService.isPermissionGranted(appContext)
    )
  )
  private val wallpaperState = MutableStateFlow(
    ProcessedWallpaper(homeBitmap = null, drawerBlurredBitmap = null, isTopBright = true)
  )

  private val baseStateFlow = combine(
    appRepository.apps,
    appRepository.isLoading,
    preferencesRepository.preferences,
    wallpaperState
  ) { apps, loading, prefs, wallpaper ->
    BaseStateBundle(apps, loading, prefs, wallpaper)
  }

  val uiState: StateFlow<LauncherUiState> = combine(
    baseStateFlow,
    transientState,
    mediaRepository.mediaState
  ) { base, transient, media ->
    val allApps = base.apps
    val prefs = base.prefs
    val appByKey = allApps.associateBy { it.componentKey }
    val hiddenSet = prefs.hiddenKeys
    val visibleApps = allApps.filterNot { it.componentKey in hiddenSet }
    val sections = AppIndexHelper.groupAppsBySection(visibleApps)

    val favoriteApps = prefs.favoriteKeys.mapNotNull { key -> appByKey[key] }
    val homeShortcutApps = favoriteApps.take(prefs.homeShortcutLimit)
    val hiddenApps = hiddenSet.mapNotNull { key -> appByKey[key] }
      .sortedBy { it.label.lowercase() }

    val searchResults = if (transient.isSearchActive) {
      AppIndexHelper.filterApps(visibleApps, transient.searchQuery)
    } else {
      emptyList()
    }

    LauncherUiState(
      isLoading = base.loading,
      allApps = allApps,
      visibleApps = visibleApps,
      sections = sections,
      favoriteApps = favoriteApps,
      homeShortcutApps = homeShortcutApps,
      hiddenApps = hiddenApps,
      searchQuery = transient.searchQuery,
      searchResults = searchResults,
      isDrawerOpen = transient.isDrawerOpen,
      drawerJumpTarget = transient.drawerJumpTarget,
      isSearchActive = transient.isSearchActive,
      showQuickActions = transient.showQuickActions,
      showLockPermissionPrompt = transient.showLockPermissionPrompt,
      activeOverlay = transient.activeOverlay,
      contextMenuApp = transient.contextMenuApp,
      isDefaultLauncher = transient.isDefaultLauncher,
      isLockAccessibilityEnabled = transient.isLockAccessibilityEnabled,
      isMediaListenerEnabled = transient.isMediaListenerEnabled,
      activeMedia = media,
      preferences = prefs,
      wallpaper = base.wallpaper,
      statusMessage = transient.statusMessage
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000L),
    initialValue = LauncherUiState()
  )

  private data class BaseStateBundle(
    val apps: List<AppInfo>,
    val loading: Boolean,
    val prefs: LauncherPreferences,
    val wallpaper: ProcessedWallpaper
  )

  init {
    appRepository.startObserving()
    mediaRepository.startObserving()

    viewModelScope.launch {
      combine(appRepository.apps, preferencesRepository.preferences) { apps, prefs ->
        apps to prefs
      }.collect { (apps, prefs) ->
        if (!prefs.favoritesSeeded && apps.isNotEmpty()) {
          val seeded = pickInitialFavorites(apps, prefs.homeShortcutLimit)
          preferencesRepository.updateFavorites(seeded, markSeeded = true)
        }
      }
    }

    viewModelScope.launch {
      preferencesRepository.preferences
        .map { prefs ->
          WallpaperKey(
            path = prefs.customWallpaperPath,
            grayscale = prefs.grayscaleWallpaper,
            blur = prefs.drawerBlurIntensity,
            lowEffects = prefs.lowEffectsMode
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

  fun refreshSystemStatus() {
    mediaRepository.startObserving()
    transientState.update {
      it.copy(
        isDefaultLauncher = DefaultLauncherHelper.isDefaultLauncher(appContext),
        isLockAccessibilityEnabled = GlassbonLockAccessibilityService.isServiceEnabled(appContext),
        isMediaListenerEnabled = GlassbonMediaListenerService.isPermissionGranted(appContext)
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
        showLockPermissionPrompt = false,
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
        showLockPermissionPrompt = false,
        activeOverlay = LauncherOverlay.NONE,
        contextMenuApp = null
      )
    }
  }

  fun updateDrawerJumpTarget(sectionKey: String?) {
    transientState.update { it.copy(drawerJumpTarget = sectionKey) }
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
      QuickActionMode.LOCK_SCREEN -> lockScreenOrPrompt()
      QuickActionMode.QUICK_PANEL -> toggleQuickActions(true)
      QuickActionMode.DIRECT_SEARCH -> openDrawer(withSearch = true)
      QuickActionMode.DIRECT_LIBRARY -> openDrawer(withSearch = false)
    }
  }

  fun lockScreenOrPrompt() {
    if (GlassbonLockAccessibilityService.lockScreenNow()) {
      transientState.update { it.copy(showQuickActions = false, showLockPermissionPrompt = false) }
    } else {
      transientState.update { it.copy(showQuickActions = false, showLockPermissionPrompt = true) }
    }
  }

  fun dismissLockPermissionPrompt() {
    transientState.update { it.copy(showLockPermissionPrompt = false) }
  }

  fun openAccessibilitySettings() {
    dismissLockPermissionPrompt()
    SystemActionsHelper.openAccessibilitySettings(appContext)
  }

  fun openNotificationListenerSettings() {
    toggleQuickActions(false)
    SystemActionsHelper.openNotificationListenerSettings(appContext)
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
      it.copy(showQuickActions = show, contextMenuApp = null, showLockPermissionPrompt = false)
    }
  }

  fun openOverlay(overlay: LauncherOverlay) {
    transientState.update {
      it.copy(
        activeOverlay = overlay,
        showQuickActions = false,
        showLockPermissionPrompt = false,
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

  fun setHomeAlphabetVisibility(visibility: HomeAlphabetVisibility) {
    viewModelScope.launch { preferencesRepository.setHomeAlphabetVisibility(visibility) }
  }

  fun setSwipeUpOpensDrawer(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setSwipeUpOpensDrawer(enabled) }
  }

  fun setSearchAutoFocusKeyboard(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setSearchAutoFocusKeyboard(enabled) }
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
