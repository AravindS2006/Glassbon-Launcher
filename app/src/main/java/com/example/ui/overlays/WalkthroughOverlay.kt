package com.example.ui.overlays

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EditorialDisplayFontFamily
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private enum class SpatialAnchor {
  CENTER,
  TOP_LEFT,
  BOTH_EDGES,
  LEFT_GUTTER,
  TOP_MID,
  BOTTOM_RIGHT
}

private data class WalkthroughStep(
  val badge: String,
  val title: String,
  val description: String,
  val tip: String,
  val icon: ImageVector,
  val anchor: SpatialAnchor
)

private val WALKTHROUGH_STEPS = listOf(
  WalkthroughStep(
    badge = "Atmosphere & Efficiency",
    title = "Pure Calm, Zero Battery Drain",
    description = "Glassbon caches pre-scaled wallpapers and frosted drawer layers off the main thread so your Home screen stays featherlight with zero background polling.",
    tip = "Static frosted surfaces replace expensive live blur shaders.",
    icon = Icons.Outlined.BatteryChargingFull,
    anchor = SpatialAnchor.CENTER
  ),
  WalkthroughStep(
    badge = "Upper-Left Anchor",
    title = "Unified Clock & Calendar Pill",
    description = "Tap the crisp clock digits anytime to open Alarms, or tap the frosted date pill beside it to launch Calendar. Ink adapts automatically to bright or dark wallpapers.",
    tip = "Switch typography styles anytime in Preferences.",
    icon = Icons.Outlined.Schedule,
    anchor = SpatialAnchor.TOP_LEFT
  ),
  WalkthroughStep(
    badge = "Left & Right Edges",
    title = "Dual-Edge Curved Wave",
    description = "Swipe or touch along the visible right A–Z rail—or the hidden left edge—to bloom curved Gaussian letters and glide directly to any app.",
    tip = "Direct vertical swipes stay calm; only the alphabet rails open App Library.",
    icon = Icons.Outlined.Gesture,
    anchor = SpatialAnchor.BOTH_EDGES
  ),
  WalkthroughStep(
    badge = "Left Gutter & Preview",
    title = "Compact Icons & Pop-Up Preview",
    description = "Keep 4–7 daily essentials tucked cleanly inside the left scrub gutter. Long-press any app anywhere to summon the frosted Pop-Up App Preview.",
    tip = "Pin favorites, reorder slots, or hide clutter without leaving your flow.",
    icon = Icons.Outlined.Apps,
    anchor = SpatialAnchor.LEFT_GUTTER
  ),
  WalkthroughStep(
    badge = "Dynamic Layout",
    title = "Smart Audio Media Capsule",
    description = "When music or podcasts play, a frosted media card expands smoothly beneath the clock using hardware media keys. When paused or stopped, that space returns to Home.",
    tip = "Zero sensitive notification permissions required.",
    icon = Icons.Outlined.MusicNote,
    anchor = SpatialAnchor.TOP_MID
  ),
  WalkthroughStep(
    badge = "Bottom-Right Control",
    title = "Quick Actions & Customization",
    description = "Tap the floating bottom-right control for Quick Actions—or customize its tap behavior in Preferences for Search, App Library, or Screen Lock Settings.",
    tip = "Wallpaper Studio, Favorites, Preferences, and this Tour live cleanly inside Quick Actions.",
    icon = Icons.Outlined.Tune,
    anchor = SpatialAnchor.BOTTOM_RIGHT
  )
)

@Composable
fun WalkthroughOverlay(
  currentStep: Int,
  reducedMotion: Boolean = false,
  onStepChange: (Int) -> Unit,
  onFinish: () -> Unit
) {
  BackHandler(onBack = onFinish)

  val pagerState = rememberPagerState(
    initialPage = currentStep.coerceIn(0, WALKTHROUGH_STEPS.lastIndex),
    pageCount = { WALKTHROUGH_STEPS.size }
  )
  val scope = rememberCoroutineScope()

  // Keep ViewModel step synced with horizontal swipes
  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }
      .distinctUntilChanged()
      .collect { page -> onStepChange(page) }
  }

  // Finite spring card entrance (0 continuous loops)
  val reveal = remember { Animatable(if (reducedMotion) 1f else 0f) }
  LaunchedEffect(reducedMotion) {
    if (reducedMotion) {
      reveal.snapTo(1f)
    } else {
      reveal.animateTo(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMedium)
      )
    }
  }

  val activeStep = WALKTHROUGH_STEPS[pagerState.currentPage]
  val cardShape = remember { RoundedCornerShape(26.dp) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0x9E08090B))
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {}
      )
      .testTag("walkthrough_overlay")
  ) {
    // Spatial edge & corner anchor beacons highlighting where each gesture lives on screen
    SpatialAnchorBeacons(anchor = activeStep.anchor)

    // Centered Frosted Tour Deck
    Box(
      modifier = Modifier
        .align(Alignment.Center)
        .padding(horizontal = 24.dp)
        .widthIn(min = 280.dp, max = 420.dp)
        .graphicsLayer {
          val p = reveal.value
          alpha = p.coerceIn(0f, 1f)
          val s = 0.94f + 0.06f * p
          scaleX = s
          scaleY = s
        }
        .clip(cardShape)
        .background(TOUR_CARD_FILL_BRUSH)
        .border(0.9.dp, TOUR_CARD_RIM_BRUSH, cardShape)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 18.dp)
      ) {
        // Top Header: Step counter + Skip
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0x1EFFFFFF))
              .border(0.6.dp, Color(0x36FFFFFF), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = "TOUR · ${pagerState.currentPage + 1} / ${WALKTHROUGH_STEPS.size}",
              fontFamily = FunctionalSansFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.5.sp,
              letterSpacing = 0.9.sp,
              color = MistColors.OffWhite,
              modifier = Modifier.testTag("walkthrough_step_counter")
            )
          }

          TextButton(
            onClick = onFinish,
            modifier = Modifier
              .heightIn(min = 40.dp)
              .testTag("walkthrough_skip_button")
          ) {
            Text(
              text = stringResource(R.string.tour_skip),
              fontFamily = FunctionalSansFontFamily,
              fontSize = 13.sp,
              color = MistColors.MistGrey
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Swipeable Multi-Step Cards
        HorizontalPager(
          state = pagerState,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("walkthrough_pager")
        ) { pageIndex ->
          val step = WALKTHROUGH_STEPS[pageIndex]
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 22.dp, vertical = 6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0x26FFFFFF))
                  .border(0.7.dp, Color(0x42FFFFFF), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = step.icon,
                  contentDescription = null,
                  tint = MistColors.PureWhite,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = step.badge.uppercase(),
                fontFamily = FunctionalSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.0.sp,
                color = MistColors.MistGrey.copy(alpha = 0.85f)
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = step.title,
              fontFamily = EditorialDisplayFontFamily,
              fontSize = 26.sp,
              lineHeight = 30.sp,
              color = MistColors.PureWhite,
              modifier = if (pageIndex == pagerState.currentPage) {
                Modifier.testTag("walkthrough_step_title")
              } else {
                Modifier
              }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = step.description,
              fontFamily = FunctionalSansFontFamily,
              fontSize = 13.5.sp,
              lineHeight = 20.sp,
              color = MistColors.OffWhite.copy(alpha = 0.88f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Concise Frosted Tip Callout
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x16FFFFFF))
                .border(0.6.dp, Color(0x28FFFFFF), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
              Text(
                text = step.tip,
                fontFamily = FunctionalSansFontFamily,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MistColors.FrostedGrey
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Dot Strip
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          WALKTHROUGH_STEPS.indices.forEach { idx ->
            val selected = idx == pagerState.currentPage
            Box(
              modifier = Modifier
                .padding(horizontal = 3.5.dp)
                .size(width = if (selected) 20.dp else 6.dp, height = 6.dp)
                .clip(CircleShape)
                .background(if (selected) MistColors.PureWhite else Color(0x45FFFFFF))
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MistColors.HairlineDivider)
        Spacer(modifier = Modifier.height(12.dp))

        // Footer Navigation Row (Back / Next / Finish)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
        ) {
          if (pagerState.currentPage > 0) {
            TextButton(
              onClick = {
                val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                scope.launch {
                  if (reducedMotion) pagerState.scrollToPage(target)
                  else pagerState.animateScrollToPage(target)
                }
              },
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("walkthrough_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = MistColors.OffWhite,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.tour_back),
                fontFamily = FunctionalSansFontFamily,
                fontSize = 13.sp,
                color = MistColors.OffWhite
              )
            }
          } else {
            Spacer(modifier = Modifier.width(4.dp))
          }

          val isLastStep = pagerState.currentPage == WALKTHROUGH_STEPS.lastIndex
          Button(
            onClick = {
              if (isLastStep) {
                onFinish()
              } else {
                val target = (pagerState.currentPage + 1).coerceAtMost(WALKTHROUGH_STEPS.lastIndex)
                scope.launch {
                  if (reducedMotion) pagerState.scrollToPage(target)
                  else pagerState.animateScrollToPage(target)
                }
              }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MistColors.PureWhite,
              contentColor = MistColors.NearBlack
            ),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag(if (isLastStep) "walkthrough_done_button" else "walkthrough_next_button")
          ) {
            Text(
              text = stringResource(if (isLastStep) R.string.tour_finish else R.string.tour_next),
              fontFamily = FunctionalSansFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp
            )
            if (!isLastStep) {
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SpatialAnchorBeacons(anchor: SpatialAnchor) {
  Box(modifier = Modifier.fillMaxSize()) {
    when (anchor) {
      SpatialAnchor.CENTER -> Unit

      SpatialAnchor.TOP_LEFT -> {
        SpatialBeaconPill(
          label = "Clock & Calendar Area",
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 24.dp, top = 28.dp)
        )
      }

      SpatialAnchor.BOTH_EDGES -> {
        Box(
          modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = 6.dp, top = 64.dp, bottom = 84.dp)
            .size(width = 8.dp, height = 220.dp)
            .clip(CircleShape)
            .background(Color(0x55FFFFFF))
            .border(0.8.dp, Color(0xAAFFFFFF), CircleShape)
        )
        Box(
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 8.dp, top = 64.dp, bottom = 84.dp)
            .size(width = 8.dp, height = 220.dp)
            .clip(CircleShape)
            .background(Color(0x55FFFFFF))
            .border(0.8.dp, Color(0xAAFFFFFF), CircleShape)
        )
        SpatialBeaconPill(
          label = "Drag Left or Right Edge",
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 24.dp)
        )
      }

      SpatialAnchor.LEFT_GUTTER -> {
        SpatialBeaconPill(
          label = "Left Favorites Rail · Hold Any App",
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 24.dp, top = 28.dp)
        )
      }

      SpatialAnchor.TOP_MID -> {
        SpatialBeaconPill(
          label = "Dynamic Media Capsule Zone",
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 24.dp, top = 86.dp)
        )
      }

      SpatialAnchor.BOTTOM_RIGHT -> {
        SpatialBeaconPill(
          label = "Quick Actions · Hold for Menu",
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 22.dp, bottom = 78.dp)
        )
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 22.dp, bottom = 18.dp)
            .size(54.dp)
            .clip(CircleShape)
            .border(1.5.dp, Color(0xCCFFFFFF), CircleShape)
        )
      }
    }
  }
}

@Composable
private fun SpatialBeaconPill(
  label: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xCC1C1E23))
      .border(0.8.dp, Color(0x77FFFFFF), RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      fontFamily = FunctionalSansFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 11.5.sp,
      color = MistColors.PureWhite
    )
  }
}

private val TOUR_CARD_FILL_BRUSH = Brush.verticalGradient(
  colors = listOf(Color(0xF01C1D22), Color(0xF2111216))
)

private val TOUR_CARD_RIM_BRUSH = Brush.linearGradient(
  colors = listOf(MistColors.SpecularRimTop, MistColors.SpecularRimMid, MistColors.SpecularRimBottom)
)
