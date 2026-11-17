package com.example.data.preferences

import androidx.compose.runtime.Immutable

enum class IconStyle(val displayName: String) {
  DOT_MONO("Glass Mono"),
  ORIGINAL("Original")
}

enum class IconSize(val displayName: String, val homeTileDp: Int, val drawerTileDp: Int) {
  COMPACT("Compact", 42, 32),
  BALANCED("Balanced", 46, 36),
  GENEROUS("Generous", 50, 40)
}

enum class TypographyStyle(val displayName: String) {
  EDITORIAL_SERIF("Instrument Serif"),
  SERIF_ITALIC("Italic Serif"),
  CLEAN_SANS("Plus Jakarta Sans")
}

enum class HomeAlphabetVisibility(val displayName: String) {
  VISIBLE("Visible"),
  INVISIBLE_EDGE("Invisible Edge"),
  OFF("Off")
}

enum class QuickActionMode(val displayName: String) {
  QUICK_PANEL("Quick Actions Panel"),
  DIRECT_SEARCH("Open Search"),
  DIRECT_LIBRARY("Open App Library"),
  LOCK_SETTINGS("Screen Lock Settings")
}

enum class PerformanceProfile(val displayName: String) {
  ULTRA_SMOOTH("Ultra-Smooth"),
  POWER_SAVER("Low Power")
}

@Immutable
data class LauncherPreferences(
  val favoriteKeys: List<String> = emptyList(),
  val homeShortcutLimit: Int = 6,
  val hiddenKeys: Set<String> = emptySet(),
  val customWallpaperPath: String? = null,
  val grayscaleWallpaper: Boolean = true,
  val wallpaperDarkening: Float = 0.14f,
  val drawerBlurIntensity: Float = 0.78f,
  val iconStyle: IconStyle = IconStyle.DOT_MONO,
  val iconSize: IconSize = IconSize.COMPACT,
  val typographyStyle: TypographyStyle = TypographyStyle.CLEAN_SANS,
  val showClock: Boolean = true,
  val showDate: Boolean = true,
  val showHomeLabels: Boolean = false,
  val showAlphabetIndex: Boolean = true,
  val showLeftHiddenAlphabet: Boolean = true,
  val homeAlphabetVisibility: HomeAlphabetVisibility = HomeAlphabetVisibility.VISIBLE,
  val swipeUpOpensDrawer: Boolean = false,
  val searchAutoFocusKeyboard: Boolean = true,
  val performanceProfile: PerformanceProfile = PerformanceProfile.ULTRA_SMOOTH,
  val reducedMotion: Boolean = false,
  val lowEffectsMode: Boolean = false,
  val hapticFeedback: Boolean = true,
  val quickActionMode: QuickActionMode = QuickActionMode.QUICK_PANEL,
  val onboardingDismissed: Boolean = false,
  val walkthroughCompleted: Boolean = false,
  val favoritesSeeded: Boolean = false
) {
  val isPowerSaver: Boolean
    get() = performanceProfile == PerformanceProfile.POWER_SAVER

  val effectiveReducedMotion: Boolean
    get() = reducedMotion || isPowerSaver

  val effectiveLowEffects: Boolean
    get() = lowEffectsMode || isPowerSaver
}
