package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.preferences.HomeAlphabetVisibility
import com.example.data.preferences.IconSize
import com.example.data.preferences.IconStyle
import com.example.data.preferences.PerformanceProfile
import com.example.data.preferences.QuickActionMode
import com.example.data.preferences.TypographyStyle
import com.example.ui.LauncherUiState
import com.example.ui.components.GlassSurface
import com.example.ui.theme.EditorialDisplayFontFamily
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors

@Composable
fun SettingsScreen(
  uiState: LauncherUiState,
  onSetPerformanceProfile: (PerformanceProfile) -> Unit,
  onSetIconStyle: (IconStyle) -> Unit,
  onSetIconSize: (IconSize) -> Unit,
  onSetTypographyStyle: (TypographyStyle) -> Unit,
  onSetShowClock: (Boolean) -> Unit,
  onSetShowDate: (Boolean) -> Unit,
  onSetShowHomeLabels: (Boolean) -> Unit,
  onSetShowAlphabetIndex: (Boolean) -> Unit,
  onSetShowLeftHiddenAlphabet: (Boolean) -> Unit,
  onSetHomeAlphabetVisibility: (HomeAlphabetVisibility) -> Unit,
  onSetHomeShortcutLimit: (Int) -> Unit,
  onSetQuickActionMode: (QuickActionMode) -> Unit,
  onSetSwipeUpOpensDrawer: (Boolean) -> Unit,
  onSetSearchAutoFocus: (Boolean) -> Unit,
  onSetReducedMotion: (Boolean) -> Unit,
  onSetLowEffectsMode: (Boolean) -> Unit,
  onSetHapticFeedback: (Boolean) -> Unit,
  onOpenWallpaperStudio: () -> Unit,
  onOpenFavoritesManager: () -> Unit,
  onOpenHiddenApps: () -> Unit,
  onOpenLockScreenSettings: () -> Unit,
  onRequestDefaultHome: () -> Unit,
  onStartWalkthrough: () -> Unit,
  onClose: () -> Unit
) {
  BackHandler(onBack = onClose)
  val prefs = uiState.preferences

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xD90B0B0D))
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("settings_screen"),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 620.dp)
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp)
      ) {
        IconButton(
          onClick = onClose,
          modifier = Modifier.testTag("settings_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = MistColors.OffWhite
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.settings_title),
          fontFamily = FunctionalSansFontFamily,
          fontSize = 24.sp,
          color = MistColors.OffWhite
        )
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
      ) {
        // PERFORMANCE & BATTERY
        SectionBlock(title = "Performance & Battery") {
          SegmentedPreferenceRow(
            title = "Performance Mode",
            subtitle = if (prefs.isPowerSaver) {
              "Low Power active — blur, motion & background prewarm reduced for battery"
            } else {
              "Ultra-Smooth active — 120Hz GPU springs, frosted blur & prewarmed icons"
            },
            options = PerformanceProfile.entries,
            selected = prefs.performanceProfile,
            label = { it.displayName },
            optionTestTag = { "perf_mode_${it.name.lowercase()}" },
            onSelect = onSetPerformanceProfile
          )
        }

        // APPEARANCE
        SectionBlock(title = "Appearance") {
          SegmentedPreferenceRow(
            title = "Icon Style",
            options = IconStyle.entries,
            selected = prefs.iconStyle,
            label = { it.displayName },
            onSelect = onSetIconStyle
          )
          DividerLine()
          SegmentedPreferenceRow(
            title = "Icon Size",
            options = IconSize.entries,
            selected = prefs.iconSize,
            label = { it.displayName },
            onSelect = onSetIconSize
          )
          DividerLine()
          SegmentedPreferenceRow(
            title = "Typography",
            options = TypographyStyle.entries,
            selected = prefs.typographyStyle,
            label = { it.displayName },
            onSelect = onSetTypographyStyle
          )
          DividerLine()
          NavigationPreferenceRow(
            title = "Wallpaper & Frosted Glass",
            subtitle = if (prefs.grayscaleWallpaper) "Grayscale atmosphere" else "Full color",
            testTag = "settings_wallpaper_row",
            onClick = onOpenWallpaperStudio
          )
          DividerLine()
          TogglePreferenceRow(
            title = "Show Clock",
            checked = prefs.showClock,
            onCheckedChange = onSetShowClock
          )
          DividerLine()
          TogglePreferenceRow(
            title = "Show Date",
            checked = prefs.showDate,
            onCheckedChange = onSetShowDate
          )
          DividerLine()
          TogglePreferenceRow(
            title = "Home Shortcut Labels",
            subtitle = "Display names beside left-edge favorites",
            checked = prefs.showHomeLabels,
            onCheckedChange = onSetShowHomeLabels
          )
          DividerLine()
          SegmentedPreferenceRow(
            title = "Home Alphabet Bar",
            options = HomeAlphabetVisibility.entries.filter { it != HomeAlphabetVisibility.OFF },
            selected = prefs.homeAlphabetVisibility,
            label = { it.displayName },
            onSelect = onSetHomeAlphabetVisibility
          )
          DividerLine()
          TogglePreferenceRow(
            title = "Left Hidden Alphabet Edge",
            subtitle = "Reveal curved wave drawer on left-edge drag",
            checked = prefs.showLeftHiddenAlphabet,
            onCheckedChange = onSetShowLeftHiddenAlphabet
          )
          DividerLine()
          TogglePreferenceRow(
            title = "Drawer Alphabet Index",
            checked = prefs.showAlphabetIndex,
            onCheckedChange = onSetShowAlphabetIndex
          )
        }

        // HOME
        SectionBlock(title = "Home") {
          NavigationPreferenceRow(
            title = "Favorites & Shortcuts",
            subtitle = "${uiState.homeShortcutApps.size} active on Home",
            testTag = "settings_favorites_row",
            onClick = onOpenFavoritesManager
          )
          DividerLine()
          SegmentedPreferenceRow(
            title = "Home Shortcut Count",
            options = listOf(4, 5, 6, 7),
            selected = prefs.homeShortcutLimit,
            label = { "$it" },
            onSelect = onSetHomeShortcutLimit
          )
          DividerLine()
          SegmentedPreferenceRow(
            title = "Bottom-Right Lock Button Tap",
            options = QuickActionMode.entries,
            selected = prefs.quickActionMode,
            label = { it.displayName },
            onSelect = onSetQuickActionMode
          )
          DividerLine()
          NavigationPreferenceRow(
            title = "Hidden Applications",
            subtitle = if (uiState.hiddenApps.isEmpty()) "None hidden" else "${uiState.hiddenApps.size} hidden",
            testTag = "settings_hidden_apps_row",
            onClick = onOpenHiddenApps
          )
        }

        // BEHAVIOR
        SectionBlock(title = "Behavior") {
          TogglePreferenceRow(
            title = "Auto-Focus Search Keyboard",
            checked = prefs.searchAutoFocusKeyboard,
            onCheckedChange = onSetSearchAutoFocus
          )
          DividerLine()
          TogglePreferenceRow(
            title = "Reduced Motion",
            subtitle = "Minimize transition animations",
            checked = prefs.reducedMotion,
            onCheckedChange = onSetReducedMotion
          )
          DividerLine()
          TogglePreferenceRow(
            title = "Low-Effects Mode",
            subtitle = "Disable wallpaper blur for weaker devices",
            checked = prefs.lowEffectsMode,
            onCheckedChange = onSetLowEffectsMode
          )
          DividerLine()
          TogglePreferenceRow(
            title = "Haptic Feedback",
            checked = prefs.hapticFeedback,
            onCheckedChange = onSetHapticFeedback
          )
        }

        // SYSTEM INTEGRATION (ZERO SENSITIVE PERMISSIONS)
        SectionBlock(title = "System Integration") {
          NavigationPreferenceRow(
            title = "Screen Lock & Security Settings",
            subtitle = "Open Android Lock Screen, PIN & display timeout settings",
            testTag = "settings_screen_lock_row",
            onClick = onOpenLockScreenSettings
          )
          DividerLine()
          NavigationPreferenceRow(
            title = "Default Home Application",
            subtitle = if (uiState.isDefaultLauncher) "Glassbon is currently default" else "Tap to set Glassbon as default Home",
            testTag = "settings_default_home_row",
            onClick = onRequestDefaultHome
          )
        }

        // HELP & WALKTHROUGH
        SectionBlock(title = "Guide") {
          NavigationPreferenceRow(
            title = "Interactive Feature Tour",
            subtitle = "Walk through gestures, curved alphabet wave & previews",
            testTag = "settings_walkthrough_row",
            onClick = onStartWalkthrough
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun SectionBlock(
  title: String,
  content: @Composable () -> Unit
) {
  Column {
    Text(
      text = title.uppercase(),
      fontFamily = FunctionalSansFontFamily,
      fontSize = 11.sp,
      letterSpacing = 1.2.sp,
      color = MistColors.FrostedGrey,
      modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
    GlassSurface(
      shape = RoundedCornerShape(18.dp),
      backgroundColor = Color(0x99171719)
    ) {
      Column {
        content()
      }
    }
  }
}

@Composable
private fun DividerLine() {
  HorizontalDivider(
    color = MistColors.HairlineDivider,
    modifier = Modifier.padding(horizontal = 16.dp)
  )
}

@Composable
private fun TogglePreferenceRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  subtitle: String? = null
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = MistColors.PureWhite),
        onClick = { onCheckedChange(!checked) }
      )
      .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontFamily = FunctionalSansFontFamily,
        fontSize = 14.5.sp,
        color = MistColors.OffWhite
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          fontFamily = FunctionalSansFontFamily,
          fontSize = 12.sp,
          color = MistColors.FrostedGrey
        )
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = MistColors.NearBlack,
        checkedTrackColor = MistColors.OffWhite,
        uncheckedThumbColor = MistColors.MistGrey,
        uncheckedTrackColor = MistColors.SecondaryCharcoal,
        uncheckedBorderColor = MistColors.FrostedGrey.copy(alpha = 0.5f)
      )
    )
  }
}

@Composable
private fun NavigationPreferenceRow(
  title: String,
  subtitle: String,
  testTag: String,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = MistColors.PureWhite),
        onClick = onClick
      )
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag(testTag)
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontFamily = FunctionalSansFontFamily,
        fontSize = 14.5.sp,
        color = MistColors.OffWhite
      )
      Text(
        text = subtitle,
        fontFamily = FunctionalSansFontFamily,
        fontSize = 12.sp,
        color = MistColors.FrostedGrey
      )
    }
    Icon(
      imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint = MistColors.FrostedGrey,
      modifier = Modifier.size(20.dp)
    )
  }
}

@Composable
private fun <T> SegmentedPreferenceRow(
  title: String,
  options: List<T>,
  selected: T,
  label: (T) -> String,
  onSelect: (T) -> Unit,
  subtitle: String? = null,
  optionTestTag: ((T) -> String)? = null
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Text(
      text = title,
      fontFamily = FunctionalSansFontFamily,
      fontSize = 14.5.sp,
      color = MistColors.OffWhite
    )
    if (subtitle != null) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        fontFamily = FunctionalSansFontFamily,
        fontSize = 12.sp,
        color = MistColors.FrostedGrey
      )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      options.forEach { option ->
        val isSelected = option == selected
        val shape = RoundedCornerShape(10.dp)
        val tagModifier = optionTestTag?.let { Modifier.testTag(it(option)) } ?: Modifier
        Box(
          modifier = Modifier
            .then(tagModifier)
            .heightIn(min = 38.dp)
            .clip(shape)
            .background(
              if (isSelected) MistColors.OffWhite else MistColors.SecondaryCharcoal.copy(alpha = 0.7f)
            )
            .border(
              width = 0.75.dp,
              color = if (isSelected) MistColors.OffWhite else MistColors.FrostedPanelBorder,
              shape = shape
            )
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true),
              onClick = { onSelect(option) }
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = label(option),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 12.5.sp,
            color = if (isSelected) MistColors.NearBlack else MistColors.OffWhite
          )
        }
      }
    }
  }
}
