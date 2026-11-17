package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
  onOpenDrawer: () -> Unit,
  onScrubToDrawerSection: (String) -> Unit,
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
  onDismissOnboarding: () -> Unit,
  modifier: Modifier = Modifier
) {
  val prefs = uiState.preferences
  val haptics = LocalHapticFeedback.current
  var dragAccumulated by remember { mutableFloatStateOf(0f) }

  val availableSections = remember(uiState.sections) {
    uiState.sections.map { it.header }.toSet()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("home_screen")
      .pointerInput(prefs.swipeUpOpensDrawer) {
        if (!prefs.swipeUpOpensDrawer) return@pointerInput
        detectVerticalDragGestures(
          onDragStart = { dragAccumulated = 0f },
          onDragEnd = { dragAccumulated = 0f },
          onDragCancel = { dragAccumulated = 0f }
        ) { _, dragAmount ->
          dragAccumulated += dragAmount
          if (dragAccumulated < -64f) {
            dragAccumulated = 0f
            if (prefs.hapticFeedback) {
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onOpenDrawer()
          }
        }
      }
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = 24.dp, vertical = 18.dp)
  ) {
    val hasActiveMedia = uiState.activeMedia.hasSession
    val useSans = prefs.typographyStyle == TypographyStyle.CLEAN_SANS

    // Structural Home Layout: Atmospheric Header (Clock + Date + optional Media) + Reclaimed Space for Favorite Rail
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          end = if (prefs.homeAlphabetVisibility != HomeAlphabetVisibility.OFF) 34.dp else 0.dp,
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
            reducedMotion = prefs.reducedMotion,
            onOpenPlayer = onOpenActiveMediaPlayer,
            onPrevious = onSkipMediaPrevious,
            onPlayPause = onToggleMediaPlayPause,
            onNext = onSkipMediaNext
          )
        }
      }

      Spacer(modifier = Modifier.height(if (hasActiveMedia) 16.dp else 26.dp))

      // When no music is playing, favorites start cleanly in the reclaimed space directly below Clock & Day
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
                .background(Color(0x26141518))
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
                reducedMotion = prefs.reducedMotion
              )

              if (prefs.showHomeLabels) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = app.label,
                  style = appLabelTextStyle(
                    isItalic = prefs.typographyStyle == TypographyStyle.SERIF_ITALIC,
                    useSans = prefs.typographyStyle == TypographyStyle.CLEAN_SANS
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

    // Home right-edge curved Alphabet Rail (Visible, Invisible Edge, or Off)
    if (prefs.homeAlphabetVisibility != HomeAlphabetVisibility.OFF) {
      Box(
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .fillMaxHeight()
          .padding(top = 52.dp, bottom = 78.dp)
          .testTag("home_alphabet_rail_container"),
        contentAlignment = Alignment.CenterEnd
      ) {
        AlphabetRail(
          availableSections = availableSections,
          activeKey = uiState.drawerJumpTarget,
          hasFavorites = uiState.favoriteApps.isNotEmpty(),
          hapticEnabled = prefs.hapticFeedback,
          invisibleMode = prefs.homeAlphabetVisibility == HomeAlphabetVisibility.INVISIBLE_EDGE,
          reducedMotion = prefs.reducedMotion,
          useSansBubble = useSans,
          onSelectKey = onScrubToDrawerSection,
          onDragEnd = onScrubDragEnd
        )
      }
    }

    // Subtle Default-Home onboarding pill (shown only when Glassbon is not yet default & not dismissed)
    if (!uiState.isDefaultLauncher && !prefs.onboardingDismissed) {
      GlassSurface(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(end = 74.dp, bottom = 6.dp)
          .widthIn(max = 310.dp)
          .graphicsLayer {
            val p = drawerProgressProvider()
            alpha = (1f - p * 1.4f).coerceIn(0f, 1f)
          }
          .testTag("default_home_onboarding"),
        shape = RoundedCornerShape(18.dp)
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Text(
            text = stringResource(R.string.onboarding_title),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 13.sp,
            color = MistColors.OffWhite
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = stringResource(R.string.onboarding_body),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = MistColors.MistGrey.copy(alpha = 0.85f)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

    // Subtle bottom-center swipe/tap indicator to open the application library
    val drawerCd = stringResource(R.string.cd_open_drawer)
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .size(width = 88.dp, height = 48.dp)
        .graphicsLayer {
          val p = drawerProgressProvider()
          alpha = (1f - p * 1.4f).coerceIn(0f, 1f)
        }
        .clip(CircleShape)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          role = Role.Button,
          onClick = onOpenDrawer
        )
        .semantics { contentDescription = drawerCd }
        .testTag("home_drawer_handle"),
      contentAlignment = Alignment.BottomCenter
    ) {
      Box(
        modifier = Modifier
          .padding(bottom = 8.dp)
          .size(width = 36.dp, height = 3.dp)
          .clip(CircleShape)
          .background(Color(0x55FFFFFF))
      )
    }

    // Bottom-right floating glass power control (Tap = Lock Screen / Configured mode, Long-press = Quick Actions)
    FloatingCircleButton(
      icon = Icons.Rounded.PowerSettingsNew,
      contentDescription = stringResource(R.string.cd_open_quick_actions),
      testTag = "home_quick_actions_button",
      onClick = onPowerButtonTap,
      onLongClick = onPowerButtonLongPress,
      hapticFeedback = prefs.hapticFeedback,
      reducedMotion = prefs.reducedMotion,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .graphicsLayer {
          val p = drawerProgressProvider()
          alpha = (1f - p * 1.4f).coerceIn(0f, 1f)
        }
    )
  }
}
