package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.graphics.AppIconCache
import com.example.data.apps.AppInfo
import com.example.data.preferences.HomeAlphabetVisibility
import com.example.data.preferences.TypographyStyle
import com.example.platform.ClockSnapshot
import com.example.ui.LauncherUiState
import com.example.ui.components.AlphabetRail
import com.example.ui.components.AlphabetRailSide
import com.example.ui.components.ClockView
import com.example.ui.components.FloatingCircleButton
import com.example.ui.components.GlassSurface
import com.example.ui.components.LiquidMediaCard
import com.example.ui.components.MistAppIcon
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors
import com.example.ui.theme.appLabelTextStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
  uiState: LauncherUiState,
  clockSnapshot: ClockSnapshot,
  iconCache: AppIconCache,
  drawerProgressProvider: () -> Float,
  activeHomeScrubSide: AlphabetRailSide? = null,
  onScrubToDrawerSection: (String, AlphabetRailSide) -> Unit,
  onScrubPullChanged: (Float) -> Unit = {},
  onContinuousScrub: ((Float) -> Unit)? = null,
  onScrubDragEnd: () -> Unit,
  onLaunchApp: (AppInfo) -> Unit,
  onLongPressApp: (AppInfo) -> Unit,
  onPowerButtonTap: () -> Unit,
  onPowerButtonLongPress: () -> Unit,
  onClockClick: () -> Unit,
  onDateClick: () -> Unit,
  onToggleMediaPlayPause: () -> Unit,
  onSkipMediaNext: () -> Unit,
  onSkipMediaPrevious: () -> Unit,
  onOpenActiveMediaPlayer: () -> Unit,
  onRequestDefaultHome: () -> Unit,
  onStartTour: () -> Unit,
  onDismissOnboarding: () -> Unit,
  modifier: Modifier = Modifier
) {
  val prefs = uiState.preferences
  val haptics = LocalHapticFeedback.current

  val availableSections = remember(uiState.sections) {
    uiState.sections.map { it.header }.toSet()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("home_screen")
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(vertical = 18.dp)
  ) {
    val hasActiveMedia = uiState.activeMedia.hasSession
    val useSans = prefs.typographyStyle == TypographyStyle.CLEAN_SANS

    // Zero-cost static adaptive edge scrim only when wallpaper edges are bright
    if (uiState.wallpaper.isEdgesBright && prefs.homeAlphabetVisibility == HomeAlphabetVisibility.VISIBLE) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .width(56.dp)
          .fillMaxHeight()
          .graphicsLayer {
            val p = drawerProgressProvider()
            alpha = (1f - p * 1.4f).coerceIn(0f, 1f)
          }
          .background(RIGHT_EDGE_CONTRAST_SCRIM)
      )
    }

    // Main Home Column: inset 28dp from left edge to preserve an ergonomic hidden left scrub gutter
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          start = 28.dp,
          end = 44.dp,
          bottom = 68.dp
        )
        .graphicsLayer {
          val p = drawerProgressProvider()
          alpha = (1f - p * 1.25f).coerceIn(0f, 1f)
          translationY = -p * 40f
        }
    ) {
      Spacer(modifier = Modifier.height(if (hasActiveMedia) 8.dp else 20.dp))

      ClockView(
        snapshot = clockSnapshot,
        showClock = prefs.showClock,
        showDate = prefs.showDate,
        isTopBright = uiState.wallpaper.isTopBright,
        typographyStyle = prefs.typographyStyle,
        isMediaActive = hasActiveMedia,
        onClockClick = onClockClick,
        onDateClick = onDateClick
      )

      AnimatedVisibility(
        visible = hasActiveMedia,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column {
          Spacer(modifier = Modifier.height(14.dp))
          LiquidMediaCard(
            media = uiState.activeMedia,
            reducedMotion = prefs.effectiveReducedMotion,
            onOpenPlayer = onOpenActiveMediaPlayer,
            onPrevious = onSkipMediaPrevious,
            onPlayPause = onToggleMediaPlayPause,
            onNext = onSkipMediaNext
          )
        }
      }

      Spacer(modifier = Modifier.height(if (hasActiveMedia) 16.dp else 26.dp))

      // Compact icon-only favorite rail offset past the invisible left scrub gutter
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.TopStart
      ) {
        Column(
          modifier = Modifier
            .verticalScroll(rememberScrollState())
            .testTag("home_favorites_rail"),
          verticalArrangement = Arrangement.spacedBy(if (hasActiveMedia) 11.dp else 14.dp),
          horizontalAlignment = Alignment.Start
        ) {
          uiState.homeShortcutApps.forEach { app ->
            val interactionSource = remember(app.componentKey) { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val rowModifier = if (prefs.showHomeLabels) {
              Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (uiState.wallpaper.isMiddleBright) Color(0x52141518) else Color(0x26141518))
                .border(0.65.dp, Color(0x28FFFFFF), RoundedCornerShape(16.dp))
                .padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp)
            } else {
              Modifier
                .clip(RoundedCornerShape(16.dp))
                .padding(horizontal = 2.dp, vertical = 2.dp)
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .testTag("home_favorite_${app.packageName}")
                .heightIn(min = 48.dp)
                .then(rowModifier)
                .combinedClickable(
                  interactionSource = interactionSource,
                  indication = ripple(bounded = true, color = MistColors.PureWhite),
                  onClick = { onLaunchApp(app) },
                  onLongClick = {
                    if (prefs.hapticFeedback) {
                      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onLongPressApp(app)
                  }
                )
                .semantics { contentDescription = app.label }
            ) {
              MistAppIcon(
                app = app,
                iconCache = iconCache,
                iconStyle = prefs.iconStyle,
                tileSize = prefs.iconSize.homeTileDp.dp,
                isPressed = isPressed,
                reducedMotion = prefs.effectiveReducedMotion,
                lowPowerMode = prefs.effectiveLowEffects,
                highContrastBackdrop = uiState.wallpaper.isMiddleBright
              )

              if (prefs.showHomeLabels) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = app.label,
                  style = appLabelTextStyle(
                    isItalic = prefs.typographyStyle == TypographyStyle.SERIF_ITALIC,
                    useSans = useSans
                  ),
                  color = MistColors.OffWhite.copy(alpha = 0.94f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.widthIn(max = 190.dp)
                )
              }
            }
          }
        }
      }
    }

    // Left-edge hidden mirrored curved Alphabet Rail (0% idle draw cost; hidden once drawer opens unless actively dragging left edge)
    val showHomeLeftRail = prefs.showLeftHiddenAlphabet &&
      (!uiState.isDrawerOpen || activeHomeScrubSide == AlphabetRailSide.LEFT)
    if (showHomeLeftRail) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterStart)
          .fillMaxHeight()
          .padding(top = 52.dp, bottom = 78.dp)
          .testTag("home_left_alphabet_rail_container"),
        contentAlignment = Alignment.CenterStart
      ) {
        AlphabetRail(
          availableSections = availableSections,
          activeKeyProvider = { null },
          hasFavorites = uiState.favoriteApps.isNotEmpty(),
          hapticEnabled = prefs.hapticFeedback,
          side = AlphabetRailSide.LEFT,
          invisibleMode = true,
          reducedMotion = prefs.effectiveReducedMotion,
          useSansBubble = useSans,
          onSelectKey = { onScrubToDrawerSection(it, AlphabetRailSide.LEFT) },
          onDragPullChanged = onScrubPullChanged,
          onContinuousScrub = onContinuousScrub,
          onDragEnd = onScrubDragEnd
        )
      }
    }

    // Right-edge curved Alphabet Rail (strict mutual exclusion with AppDrawerScreen's right rail -> never 2 rails!)
    val showHomeRightRail = !uiState.isDrawerOpen || activeHomeScrubSide == AlphabetRailSide.RIGHT
    if (showHomeRightRail) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .fillMaxHeight()
          .padding(top = 52.dp, bottom = 78.dp, end = 6.dp)
          .testTag("home_alphabet_rail_container"),
        contentAlignment = Alignment.CenterEnd
      ) {
        AlphabetRail(
          availableSections = availableSections,
          activeKeyProvider = { null },
          hasFavorites = uiState.favoriteApps.isNotEmpty(),
          hapticEnabled = prefs.hapticFeedback,
          side = AlphabetRailSide.RIGHT,
          invisibleMode = prefs.homeAlphabetVisibility == HomeAlphabetVisibility.INVISIBLE_EDGE,
          reducedMotion = prefs.effectiveReducedMotion,
          useSansBubble = useSans,
          isEdgeBright = uiState.wallpaper.isEdgesBright,
          onSelectKey = { onScrubToDrawerSection(it, AlphabetRailSide.RIGHT) },
          onDragPullChanged = onScrubPullChanged,
          onContinuousScrub = onContinuousScrub,
          onDragEnd = onScrubDragEnd
        )
      }
    }

    // Subtle first-run onboarding & tour pill (leaves edges and power button unobstructed)
    val showOnboardingPill = !prefs.onboardingDismissed && (!uiState.isDefaultLauncher || !prefs.walkthroughCompleted)
    if (showOnboardingPill) {
      val needsDefaultPrompt = !uiState.isDefaultLauncher
      GlassSurface(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(start = 28.dp, end = 92.dp, bottom = 6.dp)
          .widthIn(max = 320.dp)
          .graphicsLayer {
            val p = drawerProgressProvider()
            alpha = (1f - p * 1.4f).coerceIn(0f, 1f)
          }
          .testTag("default_home_onboarding"),
        shape = RoundedCornerShape(18.dp)
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Text(
            text = stringResource(
              if (needsDefaultPrompt) R.string.onboarding_title else R.string.onboarding_tour_only_title
            ),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 13.sp,
            color = MistColors.OffWhite
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = stringResource(
              if (needsDefaultPrompt) R.string.onboarding_body else R.string.onboarding_tour_only_body
            ),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = MistColors.MistGrey.copy(alpha = 0.85f)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!prefs.walkthroughCompleted) {
              TextButton(
                onClick = onStartTour,
                modifier = Modifier
                  .heightIn(min = 36.dp)
                  .testTag("home_start_tour_button")
              ) {
                Text(
                  text = stringResource(R.string.onboarding_start_tour),
                  fontFamily = FunctionalSansFontFamily,
                  fontSize = 12.sp,
                  color = MistColors.PureWhite
                )
              }
            }
            if (needsDefaultPrompt) {
              TextButton(
                onClick = onRequestDefaultHome,
                modifier = Modifier
                  .heightIn(min = 36.dp)
                  .testTag("onboarding_set_default_button")
              ) {
                Text(
                  text = stringResource(R.string.onboarding_cta),
                  fontFamily = FunctionalSansFontFamily,
                  fontSize = 12.sp,
                  color = MistColors.PureWhite
                )
              }
            }
            TextButton(
              onClick = onDismissOnboarding,
              modifier = Modifier
                .heightIn(min = 36.dp)
                .testTag("onboarding_dismiss_button")
            ) {
              Text(
                text = stringResource(R.string.onboarding_dismiss),
                fontFamily = FunctionalSansFontFamily,
                fontSize = 12.sp,
                color = MistColors.FrostedGrey
              )
            }
          }
        }
      }
    }

    // Bottom-right floating glass Lock / Calm button (Tap = Configured Quick Action, Long-press = Quick Actions menu)
    FloatingCircleButton(
      icon = Icons.Rounded.PowerSettingsNew,
      contentDescription = stringResource(R.string.cd_open_quick_actions),
      testTag = "home_quick_actions_button",
      onClick = onPowerButtonTap,
      onLongClick = onPowerButtonLongPress,
      hapticFeedback = prefs.hapticFeedback,
      reducedMotion = prefs.effectiveReducedMotion,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 22.dp)
        .graphicsLayer {
          val p = drawerProgressProvider()
          alpha = (1f - p * 1.4f).coerceIn(0f, 1f)
        }
    )
  }
}

private val RIGHT_EDGE_CONTRAST_SCRIM = Brush.horizontalGradient(
  colors = listOf(Color.Transparent, Color(0x300B0B0D))
)
