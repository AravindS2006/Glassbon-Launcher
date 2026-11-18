package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

private object MistDataStoreRegistry {
  private val stores = ConcurrentHashMap<String, DataStore<Preferences>>()

  fun get(context: Context): DataStore<Preferences> {
    val appCtx = context.applicationContext ?: context
    val key = runCatching { appCtx.filesDir.canonicalPath }.getOrDefault(appCtx.packageName)
    return stores.computeIfAbsent(key) {
      PreferenceDataStoreFactory.create(
        produceFile = { appCtx.preferencesDataStoreFile("mist_launcher_preferences") }
      )
    }
  }
}

class PreferencesRepository(private val context: Context) {

  private val dataStore: DataStore<Preferences>
    get() = MistDataStoreRegistry.get(context)

  private object Keys {
    val FAVORITE_KEYS_ORDERED = stringPreferencesKey("favorite_keys_ordered")
    val HOME_SHORTCUT_LIMIT = intPreferencesKey("home_shortcut_limit")
    val HIDDEN_KEYS = stringSetPreferencesKey("hidden_keys")
    val CUSTOM_WALLPAPER_PATH = stringPreferencesKey("custom_wallpaper_path")
    val GRAYSCALE_WALLPAPER = booleanPreferencesKey("grayscale_wallpaper")
    val WALLPAPER_DARKENING = floatPreferencesKey("wallpaper_darkening")
    val DRAWER_BLUR_INTENSITY = floatPreferencesKey("drawer_blur_intensity")
    val ICON_STYLE = stringPreferencesKey("icon_style")
    val ICON_SIZE = stringPreferencesKey("icon_size")
    val TYPOGRAPHY_STYLE = stringPreferencesKey("typography_style")
    val SHOW_CLOCK = booleanPreferencesKey("show_clock")
    val SHOW_DATE = booleanPreferencesKey("show_date")
    val SHOW_HOME_LABELS = booleanPreferencesKey("show_home_labels")
    val SHOW_ALPHABET_INDEX = booleanPreferencesKey("show_alphabet_index")
    val SHOW_LEFT_HIDDEN_ALPHABET = booleanPreferencesKey("show_left_hidden_alphabet")
    val ALPHABET_FOCUS_BLUR = stringPreferencesKey("alphabet_focus_blur")
    val HOME_ALPHABET_VISIBILITY = stringPreferencesKey("home_alphabet_visibility")
    val SWIPE_UP_OPENS_DRAWER = booleanPreferencesKey("swipe_up_opens_drawer")
    val SEARCH_AUTO_FOCUS_KEYBOARD = booleanPreferencesKey("search_auto_focus_keyboard")
    val PERFORMANCE_PROFILE = stringPreferencesKey("performance_profile")
    val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    val LOW_EFFECTS_MODE = booleanPreferencesKey("low_effects_mode")
    val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    val HAPTIC_PROFILE = stringPreferencesKey("haptic_profile")
    val QUICK_ACTION_MODE = stringPreferencesKey("quick_action_mode")
    val LOCK_APP_COMPONENT_KEY = stringPreferencesKey("lock_app_component_key")
    val ONBOARDING_DISMISSED = booleanPreferencesKey("onboarding_dismissed")
    val WALKTHROUGH_COMPLETED = booleanPreferencesKey("walkthrough_completed")
    val FAVORITES_SEEDED = booleanPreferencesKey("favorites_seeded")
  }

  val preferences: Flow<LauncherPreferences> = dataStore.data.map { prefs ->
    val orderedFavorites = prefs[Keys.FAVORITE_KEYS_ORDERED]
      ?.split("|")
      ?.filter { it.isNotBlank() }
      ?: emptyList()

    LauncherPreferences(
      favoriteKeys = orderedFavorites,
      homeShortcutLimit = (prefs[Keys.HOME_SHORTCUT_LIMIT] ?: 6).coerceIn(4, 7),
      hiddenKeys = prefs[Keys.HIDDEN_KEYS] ?: emptySet(),
      customWallpaperPath = prefs[Keys.CUSTOM_WALLPAPER_PATH]?.takeIf { it.isNotBlank() },
      grayscaleWallpaper = prefs[Keys.GRAYSCALE_WALLPAPER] ?: true,
      wallpaperDarkening = (prefs[Keys.WALLPAPER_DARKENING] ?: 0.14f).coerceIn(0f, 0.85f),
      drawerBlurIntensity = (prefs[Keys.DRAWER_BLUR_INTENSITY] ?: 0.78f).coerceIn(0f, 1f),
      iconStyle = parseEnum(prefs[Keys.ICON_STYLE], IconStyle.DOT_MONO),
      iconSize = parseEnum(prefs[Keys.ICON_SIZE], IconSize.COMPACT),
      typographyStyle = parseEnum(prefs[Keys.TYPOGRAPHY_STYLE], TypographyStyle.CLEAN_SANS),
      showClock = prefs[Keys.SHOW_CLOCK] ?: true,
      showDate = prefs[Keys.SHOW_DATE] ?: true,
      showHomeLabels = prefs[Keys.SHOW_HOME_LABELS] ?: false,
      showAlphabetIndex = prefs[Keys.SHOW_ALPHABET_INDEX] ?: true,
      showLeftHiddenAlphabet = prefs[Keys.SHOW_LEFT_HIDDEN_ALPHABET] ?: true,
      alphabetFocusBlur = parseEnum(prefs[Keys.ALPHABET_FOCUS_BLUR], AlphabetFocusBlur.FROSTED),
      homeAlphabetVisibility = parseEnum(
        prefs[Keys.HOME_ALPHABET_VISIBILITY],
        HomeAlphabetVisibility.VISIBLE
      ).let { if (it == HomeAlphabetVisibility.OFF) HomeAlphabetVisibility.VISIBLE else it },
      swipeUpOpensDrawer = false,
      searchAutoFocusKeyboard = prefs[Keys.SEARCH_AUTO_FOCUS_KEYBOARD] ?: true,
      performanceProfile = parseEnum(prefs[Keys.PERFORMANCE_PROFILE], PerformanceProfile.ULTRA_SMOOTH),
      reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
      lowEffectsMode = prefs[Keys.LOW_EFFECTS_MODE] ?: false,
      hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
      hapticProfile = parseEnum(prefs[Keys.HAPTIC_PROFILE], HapticProfile.BALANCED),
      quickActionMode = parseEnum(prefs[Keys.QUICK_ACTION_MODE], QuickActionMode.LOCK_SCREEN),
      lockAppComponentKey = prefs[Keys.LOCK_APP_COMPONENT_KEY]?.takeIf { it.isNotBlank() },
      onboardingDismissed = prefs[Keys.ONBOARDING_DISMISSED] ?: false,
      walkthroughCompleted = prefs[Keys.WALKTHROUGH_COMPLETED] ?: false,
      favoritesSeeded = prefs[Keys.FAVORITES_SEEDED] ?: false
    )
  }

  suspend fun updateFavorites(favoriteKeys: List<String>, markSeeded: Boolean = true) {
    dataStore.edit { prefs ->
      prefs[Keys.FAVORITE_KEYS_ORDERED] = favoriteKeys.distinct().joinToString("|")
      if (markSeeded) {
        prefs[Keys.FAVORITES_SEEDED] = true
      }
    }
  }

  suspend fun setHiddenKeys(hiddenKeys: Set<String>) {
    dataStore.edit { prefs ->
      prefs[Keys.HIDDEN_KEYS] = hiddenKeys
    }
  }

  suspend fun setCustomWallpaperPath(path: String?) {
    dataStore.edit { prefs ->
      if (path.isNullOrBlank()) {
        prefs.remove(Keys.CUSTOM_WALLPAPER_PATH)
      } else {
        prefs[Keys.CUSTOM_WALLPAPER_PATH] = path
      }
    }
  }

  suspend fun setGrayscaleWallpaper(enabled: Boolean) {
    dataStore.edit { it[Keys.GRAYSCALE_WALLPAPER] = enabled }
  }

  suspend fun setWallpaperDarkening(amount: Float) {
    dataStore.edit { it[Keys.WALLPAPER_DARKENING] = amount.coerceIn(0f, 0.85f) }
  }

  suspend fun setDrawerBlurIntensity(amount: Float) {
    dataStore.edit { it[Keys.DRAWER_BLUR_INTENSITY] = amount.coerceIn(0f, 1f) }
  }

  suspend fun setIconStyle(style: IconStyle) {
    dataStore.edit { it[Keys.ICON_STYLE] = style.name }
  }

  suspend fun setIconSize(size: IconSize) {
    dataStore.edit { it[Keys.ICON_SIZE] = size.name }
  }

  suspend fun setTypographyStyle(style: TypographyStyle) {
    dataStore.edit { it[Keys.TYPOGRAPHY_STYLE] = style.name }
  }

  suspend fun setHomeShortcutLimit(limit: Int) {
    dataStore.edit { it[Keys.HOME_SHORTCUT_LIMIT] = limit.coerceIn(4, 7) }
  }

  suspend fun setShowClock(visible: Boolean) {
    dataStore.edit { it[Keys.SHOW_CLOCK] = visible }
  }

  suspend fun setShowDate(visible: Boolean) {
    dataStore.edit { it[Keys.SHOW_DATE] = visible }
  }

  suspend fun setShowHomeLabels(visible: Boolean) {
    dataStore.edit { it[Keys.SHOW_HOME_LABELS] = visible }
  }

  suspend fun setShowAlphabetIndex(visible: Boolean) {
    dataStore.edit { it[Keys.SHOW_ALPHABET_INDEX] = visible }
  }

  suspend fun setShowLeftHiddenAlphabet(enabled: Boolean) {
    dataStore.edit { it[Keys.SHOW_LEFT_HIDDEN_ALPHABET] = enabled }
  }

  suspend fun setAlphabetFocusBlur(preset: AlphabetFocusBlur) {
    dataStore.edit { it[Keys.ALPHABET_FOCUS_BLUR] = preset.name }
  }

  suspend fun setHomeAlphabetVisibility(visibility: HomeAlphabetVisibility) {
    dataStore.edit { it[Keys.HOME_ALPHABET_VISIBILITY] = visibility.name }
  }

  suspend fun setSwipeUpOpensDrawer(enabled: Boolean) {
    dataStore.edit { it[Keys.SWIPE_UP_OPENS_DRAWER] = enabled }
  }

  suspend fun setSearchAutoFocusKeyboard(enabled: Boolean) {
    dataStore.edit { it[Keys.SEARCH_AUTO_FOCUS_KEYBOARD] = enabled }
  }

  suspend fun setPerformanceProfile(profile: PerformanceProfile) {
    dataStore.edit { it[Keys.PERFORMANCE_PROFILE] = profile.name }
  }

  suspend fun setReducedMotion(enabled: Boolean) {
    dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
  }

  suspend fun setLowEffectsMode(enabled: Boolean) {
    dataStore.edit { it[Keys.LOW_EFFECTS_MODE] = enabled }
  }

  suspend fun setHapticFeedback(enabled: Boolean) {
    dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
  }

  suspend fun setHapticProfile(profile: HapticProfile) {
    dataStore.edit { it[Keys.HAPTIC_PROFILE] = profile.name }
  }

  suspend fun setQuickActionMode(mode: QuickActionMode) {
    dataStore.edit { it[Keys.QUICK_ACTION_MODE] = mode.name }
  }

  suspend fun setLockAppComponentKey(componentKey: String?) {
    dataStore.edit { prefs ->
      if (componentKey.isNullOrBlank()) {
        prefs.remove(Keys.LOCK_APP_COMPONENT_KEY)
      } else {
        prefs[Keys.LOCK_APP_COMPONENT_KEY] = componentKey
      }
    }
  }

  suspend fun setOnboardingDismissed(dismissed: Boolean) {
    dataStore.edit { it[Keys.ONBOARDING_DISMISSED] = dismissed }
  }

  suspend fun setWalkthroughCompleted(completed: Boolean) {
    dataStore.edit { it[Keys.WALKTHROUGH_COMPLETED] = completed }
  }

  private inline fun <reified T : Enum<T>> parseEnum(raw: String?, fallback: T): T {
    if (raw.isNullOrBlank()) return fallback
    return enumValues<T>().firstOrNull { it.name == raw } ?: fallback
  }
}
