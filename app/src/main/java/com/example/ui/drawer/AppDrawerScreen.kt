package com.example.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.graphics.AppIconCache
import com.example.core.haptics.LocalMistHaptics
import com.example.core.haptics.MistHapticEvent
import com.example.data.apps.AppIndexHelper
import com.example.data.apps.AppInfo
import com.example.data.preferences.AlphabetFocusBlur
import com.example.data.preferences.TypographyStyle
import com.example.ui.DrawerRowItem
import com.example.ui.LauncherUiState
import com.example.ui.components.AlphabetRail
import com.example.ui.components.AlphabetRailSide
import com.example.ui.components.FAVORITES_INDEX_KEY
import com.example.ui.components.FloatingCircleButton
import com.example.ui.components.GlassSurface
import com.example.ui.components.MistAppIcon
import com.example.ui.theme.EditorialDisplayFontFamily
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors
import com.example.ui.theme.appLabelTextStyle
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

@Composable
fun AppDrawerScreen(
  uiState: LauncherUiState,
  iconCache: AppIconCache,
  drawerJumpEvents: SharedFlow<String>,
  drawerContinuousScrubEvents: SharedFlow<Float> = remember { MutableSharedFlow() },
  hideRightAlphabetRail: Boolean = false,
  hideLeftAlphabetRail: Boolean = false,
  homeScrubPullProvider: () -> Float = { 0f },
  onJumpTargetHandled: () -> Unit = {},
  onLaunchApp: (AppInfo) -> Unit,
  onLongPressApp: (AppInfo) -> Unit,
  onSearchActiveChange: (Boolean) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onCloseDrawer: () -> Unit,
  modifier: Modifier = Modifier
) {
  val prefs = uiState.preferences
  val listState = rememberLazyListState()
  val searchListState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current
  val haptics = LocalMistHaptics.current

  // Smooth horizontal clearance ONLY for left-edge wave (never push apps off the left edge of the screen!)
  var draggingSide by remember { mutableStateOf<AlphabetRailSide?>(null) }
  val drawerScrubPullState = remember { mutableFloatStateOf(0f) }
  val listDragShiftX = remember { Animatable(0f) }
  val baseLeftClearancePx = with(density) { 10.dp.toPx() }
  val extraLeftClearancePx = with(density) { 14.dp.toPx() }
  val defaultRegularHeaderHeightPx = with(density) { 49.dp.toPx() }
  val defaultFavHeaderHeightPx = with(density) { 37.dp.toPx() }
  val defaultRowHeightPx = with(density) {
    maxOf(48.dp, prefs.iconSize.drawerTileDp.dp + 14.dp).toPx()
  }
  val viewportHeightPxState = remember { mutableIntStateOf(0) }

  val effectiveSide = when {
    hideRightAlphabetRail -> AlphabetRailSide.RIGHT
    hideLeftAlphabetRail -> AlphabetRailSide.LEFT
    else -> draggingSide
  }

  val effectiveReducedMotion = prefs.effectiveReducedMotion
  val effectiveLowEffects = prefs.effectiveLowEffects
  val isScrubbingAnyEdge = effectiveSide != null

  // Physical swipe-down-to-home state (from top of list or from header handle at any scroll depth)
  val pullDownOffset = remember { Animatable(0f) }
  val dismissThresholdPx = with(density) { 64.dp.toPx() }
  val maxPullPx = with(density) { 260.dp.toPx() }
  val flingVelocityThresholdPx = with(density) { 450.dp.toPx() }

  LaunchedEffect(uiState.isDrawerOpen) {
    pullDownOffset.snapTo(0f)
  }

  LaunchedEffect(uiState.isSearchActive, uiState.searchQuery) {
    if (uiState.isSearchActive) {
      searchListState.scrollToItem(0)
    }
  }

  val currentIsScrubbingState = rememberUpdatedState(isScrubbingAnyEdge)
  val currentIsScrubbing by currentIsScrubbingState
  val currentIsSearchActive by rememberUpdatedState(uiState.isSearchActive)
  val currentReducedMotion by rememberUpdatedState(effectiveReducedMotion)
  val currentOnCloseDrawer by rememberUpdatedState(onCloseDrawer)

  fun settlePullOrDismiss(velocityY: Float) {
    if (pullDownOffset.value >= dismissThresholdPx ||
      (pullDownOffset.value > 16f && velocityY >= flingVelocityThresholdPx)
    ) {
      haptics.trigger(MistHapticEvent.WAVE_RELEASE)
      if (currentIsSearchActive) {
        keyboardController?.hide()
        focusManager.clearFocus()
      }
      currentOnCloseDrawer()
    } else if (pullDownOffset.value > 0f) {
      coroutineScope.launch {
        if (currentReducedMotion) {
          pullDownOffset.snapTo(0f)
        } else {
          pullDownOffset.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
          )
        }
      }
    }
  }

  val drawerDismissNestedScroll = remember(
    listState,
    searchListState,
    dismissThresholdPx,
    maxPullPx,
    flingVelocityThresholdPx
  ) {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && pullDownOffset.value > 0f && available.y < 0f) {
          val current = pullDownOffset.value
          val next = (current + available.y).coerceAtLeast(0f)
          val consumedY = next - current
          coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            pullDownOffset.snapTo(next)
          }
          return Offset(0f, consumedY)
        }
        return Offset.Zero
      }

      override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
      ): Offset {
        val activeCanScrollBackward = if (currentIsSearchActive) {
          searchListState.canScrollBackward
        } else {
          listState.canScrollBackward
        }
        if (source == NestedScrollSource.UserInput &&
          !currentIsScrubbing &&
          !activeCanScrollBackward &&
          available.y > 0f
        ) {
          val next = (pullDownOffset.value + available.y * 0.85f).coerceIn(0f, maxPullPx)
          coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            pullDownOffset.snapTo(next)
          }
          return Offset(0f, available.y)
        }
        return Offset.Zero
      }

      override suspend fun onPreFling(available: Velocity): Velocity {
        if (pullDownOffset.value > 0f) {
          settlePullOrDismiss(available.y)
          return available
        }
        return Velocity.Zero
      }
    }
  }

  val headerPullModifier = Modifier.pointerInput(dismissThresholdPx, maxPullPx, flingVelocityThresholdPx) {
    val velocityTracker = VelocityTracker()
    detectVerticalDragGestures(
      onDragStart = { velocityTracker.resetTracking() },
      onVerticalDrag = { change, dragAmount ->
        velocityTracker.addPosition(change.uptimeMillis, change.position)
        val next = (pullDownOffset.value + dragAmount * 0.85f).coerceIn(0f, maxPullPx)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
          pullDownOffset.snapTo(next)
        }
        change.consume()
      },
      onDragEnd = {
        settlePullOrDismiss(velocityTracker.calculateVelocity().y)
      },
      onDragCancel = {
        settlePullOrDismiss(0f)
      }
    )
  }

  LaunchedEffect(effectiveSide, effectiveReducedMotion) {
    if (effectiveReducedMotion || effectiveSide == null || effectiveSide == AlphabetRailSide.RIGHT) {
      if (effectiveReducedMotion) {
        listDragShiftX.snapTo(0f)
      } else if (listDragShiftX.value != 0f) {
        listDragShiftX.animateTo(
          targetValue = 0f,
          animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessMediumLow
          )
        )
      }
    } else {
      snapshotFlow {
        val pull = if (hideLeftAlphabetRail) {
          homeScrubPullProvider()
        } else {
          drawerScrubPullState.floatValue
        }
        baseLeftClearancePx + extraLeftClearancePx * pull
      }.collectLatest { targetShift ->
        listDragShiftX.animateTo(
          targetValue = targetShift,
          animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessMedium
          )
        )
      }
    }
  }

  BackHandler(enabled = uiState.isDrawerOpen) {
    if (uiState.isSearchActive) {
      keyboardController?.hide()
      focusManager.clearFocus()
      onSearchActiveChange(false)
    } else {
      onCloseDrawer()
    }
  }

  val sectionIndexMap = uiState.sectionIndexMap
  val entries = remember { listOf(FAVORITES_INDEX_KEY) + AppIndexHelper.ALPHABET_RAIL_LETTERS }

  val pinnedJumpKeyState = remember { mutableStateOf<String?>(null) }
  val targetContinuousRowState = remember { mutableFloatStateOf(-1f) }
  val targetScrubSectionState = remember { mutableStateOf<String?>(null) }
  val focusBlend = remember { Animatable(0f) }
  val cachedBlurEffect = rememberCachedAlphabetBlurEffect(
    blurPreset = prefs.alphabetFocusBlur,
    lowEffectsMode = effectiveLowEffects
  )
  val focusedSectionProvider: () -> String? = remember(pinnedJumpKeyState, prefs.alphabetFocusBlur) {
    {
      if (prefs.alphabetFocusBlur == AlphabetFocusBlur.OFF) null else pinnedJumpKeyState.value
    }
  }
  val focusBlendProvider: () -> Float = remember(focusBlend) { { focusBlend.value } }
  val isSpotlightMotionProvider: () -> Boolean = remember(listState, targetContinuousRowState, currentIsScrubbingState) {
    {
      currentIsScrubbingState.value ||
        listState.isScrollInProgress ||
        targetContinuousRowState.floatValue >= 0f
    }
  }

  LaunchedEffect(prefs.alphabetFocusBlur, effectiveReducedMotion) {
    snapshotFlow { prefs.alphabetFocusBlur != AlphabetFocusBlur.OFF && pinnedJumpKeyState.value != null }
      .collectLatest { active ->
        if (effectiveReducedMotion) {
          focusBlend.snapTo(if (active) 1f else 0f)
        } else {
          focusBlend.animateTo(
            targetValue = if (active) 1f else 0f,
            animationSpec = tween(
              durationMillis = if (active) 170 else 140,
              easing = FastOutSlowInEasing
            )
          )
        }
      }
  }

  LaunchedEffect(uiState.isDrawerOpen, uiState.isSearchActive) {
    if (!uiState.isDrawerOpen || uiState.isSearchActive) {
      pinnedJumpKeyState.value = null
      targetContinuousRowState.floatValue = -1f
      targetScrubSectionState.value = null
    }
  }

  fun measuredRowHeightPx(index: Int): Float {
    val visibleInfo = listState.layoutInfo.visibleItemsInfo
    visibleInfo.firstOrNull { it.index == index }?.let {
      return it.size.toFloat().coerceAtLeast(1f)
    }
    val rowItem = uiState.drawerRows.getOrNull(index)
    return when (rowItem) {
      is DrawerRowItem.Header -> {
        if (rowItem.isFavoritesHeader) {
          visibleInfo.firstOrNull { it.contentType == "header" && it.index == 0 }
            ?.size?.toFloat()?.coerceAtLeast(1f)
            ?: defaultFavHeaderHeightPx
        } else {
          visibleInfo.firstOrNull { it.contentType == "header" && it.index > 0 }
            ?.size?.toFloat()?.coerceAtLeast(1f)
            ?: defaultRegularHeaderHeightPx
        }
      }
      is DrawerRowItem.App, null -> {
        val visibleApps = visibleInfo.filter { it.contentType == "app_row" }
        if (visibleApps.isNotEmpty()) {
          (visibleApps.sumOf { it.size }.toFloat() / visibleApps.size).coerceAtLeast(1f)
        } else {
          defaultRowHeightPx
        }
      }
    }
  }

  fun computeCenteredTarget(sectionKey: String): CenteredScrollTarget? {
    val startIndex = sectionIndexMap[sectionKey] ?: return null
    val rows = uiState.drawerRows
    if (startIndex !in rows.indices) return null
    val endIndex = findSectionEndIndex(startIndex, rows)
    val viewportH = listState.layoutInfo.viewportSize.height
      .takeIf { it > 0 }
      ?: viewportHeightPxState.intValue
    if (viewportH <= 0) {
      return CenteredScrollTarget(
        startIndex = startIndex,
        endIndex = endIndex,
        anchorIndex = startIndex,
        anchorOffsetPx = 0,
        continuousAnchorPos = startIndex.toFloat()
      )
    }

    var sectionHeightPx = 0f
    for (idx in startIndex until endIndex) {
      sectionHeightPx += measuredRowHeightPx(idx)
    }

    // Center the section vertically when it fits; if taller than the viewport, align header at top so top apps never hide
    val desiredTopPx = ((viewportH - sectionHeightPx) / 2f).roundToInt().coerceAtLeast(0)
    var anchorIndex = startIndex
    var anchorOffsetPx = 0
    if (desiredTopPx > 0 && startIndex > 0) {
      var remaining = desiredTopPx.toFloat()
      var k = startIndex - 1
      while (k >= 0) {
        val h = measuredRowHeightPx(k).coerceAtLeast(1f)
        if (remaining <= h) {
          anchorIndex = k
          anchorOffsetPx = (h - remaining).roundToInt().coerceAtLeast(0)
          break
        }
        remaining -= h
        if (k == 0) {
          anchorIndex = 0
          anchorOffsetPx = 0
        }
        k--
      }
    }
    val anchorH = measuredRowHeightPx(anchorIndex).coerceAtLeast(1f)
    val continuousAnchorPos = anchorIndex + (anchorOffsetPx / anchorH).coerceIn(0f, 0.999f)
    return CenteredScrollTarget(
      startIndex = startIndex,
      endIndex = endIndex,
      anchorIndex = anchorIndex,
      anchorOffsetPx = anchorOffsetPx,
      continuousAnchorPos = continuousAnchorPos
    )
  }

  suspend fun scrollToCenteredSection(sectionKey: String) {
    val target = computeCenteredTarget(sectionKey) ?: return
    listState.scrollToItem(target.anchorIndex, target.anchorOffsetPx)
    if (listState.layoutInfo.visibleItemsInfo.none { it.index == target.startIndex }) {
      withFrameNanos { }
    }
    val visible = listState.layoutInfo.visibleItemsInfo
    val headerItem = visible.firstOrNull { it.index == target.startIndex }
    val lastSectionItem = visible.lastOrNull { it.index in target.startIndex until target.endIndex }
    if (headerItem != null && lastSectionItem != null && lastSectionItem.index == target.endIndex - 1) {
      val measuredSectionH = (lastSectionItem.offset + lastSectionItem.size) - headerItem.offset
      val viewportH = (listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset)
        .takeIf { it > 0 }
        ?: viewportHeightPxState.intValue
      if (viewportH > 0) {
        val desiredTop = ((viewportH - measuredSectionH) / 2f).roundToInt().coerceAtLeast(0)
        val delta = (headerItem.offset - desiredTop).toFloat()
        if (abs(delta) > 1.5f) {
          listState.scroll { scrollBy(delta) }
        }
      }
    }
  }

  fun resolveAndUpdateContinuousScrub(continuousIdx: Float) {
    if (uiState.drawerRows.isEmpty() || sectionIndexMap.isEmpty()) return
    val touchedIdx = continuousIdx.toInt().coerceIn(0, entries.lastIndex)
    val resolved = findNearestSection(entries[touchedIdx], sectionIndexMap) ?: return
    pinnedJumpKeyState.value = resolved.first
    targetScrubSectionState.value = resolved.first
    targetContinuousRowState.floatValue = computeCenteredTarget(resolved.first)?.continuousAnchorPos
      ?: resolved.second.toFloat()
  }

  // Clear pinned section highlight as soon as user manually drags the app list
  LaunchedEffect(listState) {
    listState.interactionSource.interactions.collect { interaction ->
      if (interaction is DragInteraction.Start) {
        pinnedJumpKeyState.value = null
        targetContinuousRowState.floatValue = -1f
        targetScrubSectionState.value = null
      }
    }
  }

  // Initial jump when opening the drawer from Home screen's Alphabet Rail:
  // Wait for LazyColumn's initial measurement pass + viewport height so centering never drops or pins to top.
  LaunchedEffect(uiState.drawerJumpTarget, uiState.isLoading, sectionIndexMap, uiState.drawerRows.size) {
    val target = uiState.drawerJumpTarget ?: return@LaunchedEffect
    if (targetContinuousRowState.floatValue >= 0f) {
      onJumpTargetHandled()
      return@LaunchedEffect
    }
    if (uiState.isLoading || uiState.drawerRows.isEmpty() || sectionIndexMap.isEmpty()) {
      return@LaunchedEffect
    }
    val resolved = findNearestSection(target, sectionIndexMap)
    if (resolved != null && resolved.second < uiState.drawerRows.size) {
      val index = resolved.second
      if (listState.layoutInfo.totalItemsCount <= index) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
          .first { it > index }
      }
      if (listState.layoutInfo.viewportSize.height <= 0 && viewportHeightPxState.intValue <= 0) {
        snapshotFlow { listState.layoutInfo.viewportSize.height.coerceAtLeast(viewportHeightPxState.intValue) }
          .first { it > 0 }
      }
      if (targetContinuousRowState.floatValue < 0f) {
        pinnedJumpKeyState.value = resolved.first
        targetContinuousRowState.floatValue = -1f
        targetScrubSectionState.value = null
        scrollToCenteredSection(resolved.first)
      }
    }
    onJumpTargetHandled()
  }

  // Discrete jump events (taps on Home rail when drawer is open)
  LaunchedEffect(drawerJumpEvents, sectionIndexMap) {
    drawerJumpEvents.collect { target ->
      val resolved = findNearestSection(target, sectionIndexMap)
      if (resolved != null) {
        pinnedJumpKeyState.value = resolved.first
        targetContinuousRowState.floatValue = -1f
        targetScrubSectionState.value = null
        scrollToCenteredSection(resolved.first)
      }
    }
  }

  // Continuous scrub stream from Home rail -> smoothly glides to the centered section for the touched letter
  LaunchedEffect(drawerContinuousScrubEvents, sectionIndexMap, uiState.drawerRows.size) {
    drawerContinuousScrubEvents.collect { continuousIdx ->
      resolveAndUpdateContinuousScrub(continuousIdx)
    }
  }

  // Vsync-paced frame follower: smoothly interpolates between centered alphabet targets at 60-120Hz and refines exact section centering on settle
  LaunchedEffect(effectiveReducedMotion, uiState.drawerRows.size, sectionIndexMap) {
    if (uiState.drawerRows.isEmpty()) return@LaunchedEffect

    var smoothedRow = -1f
    val maxRow = (uiState.drawerRows.size - 1).coerceAtLeast(0).toFloat()

    snapshotFlow { targetContinuousRowState.floatValue }
      .collectLatest { target ->
        if (target < 0f) {
          smoothedRow = -1f
          return@collectLatest
        }
        val clampedTarget = target.coerceIn(0f, maxRow)

        if (effectiveReducedMotion) {
          smoothedRow = clampedTarget
          val sectionKey = targetScrubSectionState.value
          if (sectionKey != null) {
            scrollToCenteredSection(sectionKey)
          } else {
            val itemIndex = clampedTarget.toInt().coerceIn(0, uiState.drawerRows.lastIndex)
            val offsetPx = ((clampedTarget - itemIndex) * measuredRowHeightPx(itemIndex)).roundToInt().coerceAtLeast(0)
            listState.scrollToItem(itemIndex, offsetPx)
          }
          targetContinuousRowState.floatValue = -1f
          return@collectLatest
        }

        var lastFrameNanos = 0L
        while (smoothedRow < 0f || abs(clampedTarget - smoothedRow) > 0.005f) {
          var pendingScroll: Pair<Int, Int>? = null
          withFrameNanos { frameNanos ->
            if (smoothedRow < 0f) {
              val curIdx = listState.firstVisibleItemIndex.coerceIn(0, uiState.drawerRows.lastIndex)
              val curH = measuredRowHeightPx(curIdx).coerceAtLeast(1f)
              smoothedRow = (curIdx + (listState.firstVisibleItemScrollOffset / curH)).coerceIn(0f, maxRow)
            }
            val dt = if (lastFrameNanos == 0L) 0.016f else {
              ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            }
            val factor = 1f - exp(-26f * dt)
            smoothedRow += (clampedTarget - smoothedRow) * factor
            if (abs(clampedTarget - smoothedRow) <= 0.005f) {
              smoothedRow = clampedTarget
            }
            lastFrameNanos = frameNanos

            val itemIndex = smoothedRow.toInt().coerceIn(0, uiState.drawerRows.lastIndex)
            val offsetPx = ((smoothedRow - itemIndex) * measuredRowHeightPx(itemIndex)).roundToInt().coerceAtLeast(0)
            pendingScroll = itemIndex to offsetPx
          }
          pendingScroll?.let { (itemIndex, offsetPx) ->
            listState.scrollToItem(itemIndex, offsetPx)
          }
        }
        targetScrubSectionState.value?.let { settledKey ->
          scrollToCenteredSection(settledKey)
        }
        targetContinuousRowState.floatValue = -1f
      }
  }

  // Only read inside activeKeyProvider lambda passed to AlphabetRail -> AppDrawerScreen NEVER recomposes while scrolling!
  val activeIndexKeyState = remember(sectionIndexMap) {
    derivedStateOf {
      val pinned = pinnedJumpKeyState.value
      if (pinned != null && pinned in sectionIndexMap) {
        pinned
      } else {
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isNotEmpty() && !listState.canScrollForward && listState.canScrollBackward) {
          sectionIndexMap.keys.lastOrNull()
        } else {
          val firstVisible = listState.firstVisibleItemIndex
          var current: String? = null
          for ((key, startIndex) in sectionIndexMap) {
            if (startIndex <= firstVisible) {
              current = key
            } else {
              break
            }
          }
          current ?: sectionIndexMap.keys.firstOrNull()
        }
      }
    }
  }

  val availableSections = remember(uiState.sections) {
    uiState.sections.map { it.header }.toSet()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("app_drawer_screen")
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .imePadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .nestedScroll(drawerDismissNestedScroll)
        .graphicsLayer {
          val dy = pullDownOffset.value
          if (dy > 0f) {
            translationY = dy
            alpha = (1f - (dy / (dismissThresholdPx * 3f))).coerceIn(0.65f, 1f)
          }
        }
    ) {
      AnimatedContent(
        targetState = uiState.isSearchActive,
        transitionSpec = {
          if (effectiveReducedMotion) {
            EnterTransition.None togetherWith ExitTransition.None
          } else {
            fadeIn(tween(140)) togetherWith fadeOut(tween(110))
          }
        },
        label = "drawer_header_transition",
        modifier = headerPullModifier
      ) { searching ->
        if (searching) {
          DrawerSearchHeader(
            query = uiState.searchQuery,
            autoFocus = prefs.searchAutoFocusKeyboard,
            onQueryChange = onSearchQueryChange,
            onClear = { onSearchQueryChange("") },
            onClose = {
              keyboardController?.hide()
              focusManager.clearFocus()
              onSearchActiveChange(false)
            },
            onSubmitFirst = {
              uiState.searchResults.firstOrNull()?.let { firstApp ->
                keyboardController?.hide()
                onLaunchApp(firstApp)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
          )
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 6.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(width = 88.dp, height = 32.dp)
                .clip(CircleShape)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  role = Role.Button,
                  onClick = {
                    haptics.trigger(MistHapticEvent.WAVE_RELEASE)
                    onCloseDrawer()
                  }
                )
                .testTag("drawer_close_handle"),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(width = 36.dp, height = 3.dp)
                  .clip(CircleShape)
                  .background(Color(0x55FFFFFF))
              )
            }
          }
        }
      }

      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        viewportHeightPxState.intValue = constraints.maxHeight
        val bottomCenterPadding = maxOf(160.dp, maxHeight * 0.55f)

        if (uiState.isSearchActive) {
          SearchResultsList(
            results = uiState.searchResults,
            query = uiState.searchQuery,
            uiState = uiState,
            iconCache = iconCache,
            listState = searchListState,
            emptyStatePullModifier = headerPullModifier,
            onLaunchApp = { app ->
              keyboardController?.hide()
              onLaunchApp(app)
            },
            onLongPressApp = onLongPressApp
          )
        } else {
          val useSans = prefs.typographyStyle == TypographyStyle.CLEAN_SANS
          val isItalic = prefs.typographyStyle == TypographyStyle.SERIF_ITALIC
          val favoritesTitle = stringResource(R.string.favorites_section_title)

          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                translationX = listDragShiftX.value
              }
              .testTag("drawer_app_list"),
            contentPadding = PaddingValues(
              start = if (prefs.showLeftHiddenAlphabet) 36.dp else 24.dp,
              end = if (prefs.showAlphabetIndex) 56.dp else 24.dp,
              top = 8.dp,
              bottom = bottomCenterPadding
            )
          ) {
            items(
              items = uiState.drawerRows,
              key = { it.rowKey },
              contentType = { it.contentType }
            ) { rowItem ->
              val rowSectionKey = rowItem.sectionKey
              val spotlightModifier = Modifier.drawerFocusSpotlight(
                rowSectionKey = rowSectionKey,
                focusedSectionProvider = focusedSectionProvider,
                focusBlendProvider = focusBlendProvider,
                isMovingProvider = isSpotlightMotionProvider,
                blurPreset = prefs.alphabetFocusBlur,
                lowEffectsMode = effectiveLowEffects,
                cachedBlurEffect = cachedBlurEffect
              )

              when (rowItem) {
                is DrawerRowItem.Header -> {
                  DrawerSectionHeader(
                    title = if (rowItem.isFavoritesHeader) favoritesTitle else rowItem.title,
                    useSans = useSans || rowItem.isFavoritesHeader,
                    isHighlightedProvider = { focusedSectionProvider() == rowSectionKey },
                    modifier = spotlightModifier
                      .padding(
                        top = if (rowItem.isFavoritesHeader) 8.dp else 20.dp,
                        bottom = 6.dp
                      )
                      .testTag(
                        if (rowItem.isFavoritesHeader) "drawer_section_favorites" else "drawer_section_${rowItem.title}"
                      )
                  )
                }
                is DrawerRowItem.App -> {
                  val app = rowItem.app
                  DrawerAppRow(
                    app = app,
                    iconCache = iconCache,
                    iconStyle = prefs.iconStyle,
                    tileSize = prefs.iconSize.drawerTileDp.dp,
                    isItalic = isItalic,
                    useSans = useSans,
                    hapticFeedback = prefs.hapticFeedback,
                    reducedMotion = effectiveReducedMotion,
                    lowPowerMode = effectiveLowEffects,
                    onClick = { onLaunchApp(app) },
                    onLongClick = { onLongPressApp(app) },
                    modifier = spotlightModifier
                  )
                }
              }
            }
          }

          // Left-edge hidden mirrored Alphabet Rail inside the drawer
          if (prefs.showLeftHiddenAlphabet && uiState.sections.isNotEmpty() && !hideLeftAlphabetRail) {
            Box(
              modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(bottom = 68.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              AlphabetRail(
                availableSections = availableSections,
                activeKeyProvider = { activeIndexKeyState.value },
                hasFavorites = uiState.favoriteApps.isNotEmpty(),
                hapticEnabled = prefs.hapticFeedback,
                side = AlphabetRailSide.LEFT,
                invisibleMode = true,
                reducedMotion = effectiveReducedMotion,
                useSansBubble = useSans,
                onDraggingChanged = { dragging ->
                  draggingSide = if (dragging) AlphabetRailSide.LEFT else null
                  if (!dragging) drawerScrubPullState.floatValue = 0f
                },
                onDragPullChanged = { pull ->
                  drawerScrubPullState.floatValue = pull
                },
                onContinuousScrub = { continuousIdx ->
                  resolveAndUpdateContinuousScrub(continuousIdx)
                },
                onSelectKey = { targetKey ->
                  val resolved = findNearestSection(targetKey, sectionIndexMap)
                  if (resolved != null) {
                    pinnedJumpKeyState.value = resolved.first
                    targetContinuousRowState.floatValue = -1f
                    targetScrubSectionState.value = null
                    coroutineScope.launch { scrollToCenteredSection(resolved.first) }
                  }
                }
              )
            }
          }

          // Right-edge curved vertical alphabet index
          if (prefs.showAlphabetIndex && uiState.sections.isNotEmpty() && !hideRightAlphabetRail) {
            Box(
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 6.dp, bottom = 68.dp),
              contentAlignment = Alignment.Center
            ) {
              AlphabetRail(
                availableSections = availableSections,
                activeKeyProvider = { activeIndexKeyState.value },
                hasFavorites = uiState.favoriteApps.isNotEmpty(),
                hapticEnabled = prefs.hapticFeedback,
                side = AlphabetRailSide.RIGHT,
                reducedMotion = effectiveReducedMotion,
                useSansBubble = useSans,
                onDraggingChanged = { dragging ->
                  draggingSide = if (dragging) AlphabetRailSide.RIGHT else null
                  if (!dragging) drawerScrubPullState.floatValue = 0f
                },
                onDragPullChanged = { pull ->
                  drawerScrubPullState.floatValue = pull
                },
                onContinuousScrub = { continuousIdx ->
                  resolveAndUpdateContinuousScrub(continuousIdx)
                },
                onSelectKey = { targetKey ->
                  val resolved = findNearestSection(targetKey, sectionIndexMap)
                  if (resolved != null) {
                    pinnedJumpKeyState.value = resolved.first
                    targetContinuousRowState.floatValue = -1f
                    targetScrubSectionState.value = null
                    coroutineScope.launch { scrollToCenteredSection(resolved.first) }
                  }
                }
              )
            }
          }
        }
      }
    }

    if (!uiState.isSearchActive) {
      FloatingCircleButton(
        icon = Icons.Rounded.Search,
        contentDescription = stringResource(R.string.cd_open_search),
        testTag = "drawer_search_button",
        onClick = { onSearchActiveChange(true) },
        hapticFeedback = prefs.hapticFeedback,
        reducedMotion = effectiveReducedMotion,
        lowPowerMode = effectiveLowEffects,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 24.dp, bottom = 20.dp)
      )
    }
  }
}

private data class CenteredScrollTarget(
  val startIndex: Int,
  val endIndex: Int,
  val anchorIndex: Int,
  val anchorOffsetPx: Int,
  val continuousAnchorPos: Float
)

private fun findSectionEndIndex(
  startIndex: Int,
  drawerRows: List<DrawerRowItem>
): Int {
  var end = startIndex + 1
  while (end < drawerRows.size && drawerRows[end] !is DrawerRowItem.Header) {
    end++
  }
  return end
}

private fun findNearestSection(
  targetKey: String,
  sectionIndexMap: Map<String, Int>
): Pair<String, Int>? {
  sectionIndexMap[targetKey]?.let { return targetKey to it }
  if (targetKey == FAVORITES_INDEX_KEY) {
    val first = sectionIndexMap.entries.firstOrNull() ?: return null
    return first.key to first.value
  }
  val keys = sectionIndexMap.keys.filter { it != FAVORITES_INDEX_KEY }
  if (targetKey == "#") {
    val lastKey = keys.lastOrNull() ?: return null
    return sectionIndexMap[lastKey]?.let { lastKey to it }
  }
  val exactOrNext = keys.firstOrNull { it != "#" && it >= targetKey }
    ?: keys.lastOrNull()
  return exactOrNext?.let { key -> sectionIndexMap[key]?.let { key to it } }
}

@Composable
private fun DrawerSectionHeader(
  title: String,
  useSans: Boolean = true,
  isHighlightedProvider: () -> Boolean = { false },
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .padding(start = 4.dp)
      .drawBehind {
        if (isHighlightedProvider()) {
          drawCircle(
            color = MistColors.PureWhite.copy(alpha = 0.92f),
            radius = 3.dp.toPx(),
            center = Offset(x = -9.dp.toPx(), y = size.height / 2f)
          )
        }
      }
  ) {
    Text(
      text = title,
      fontFamily = if (useSans) FunctionalSansFontFamily else EditorialDisplayFontFamily,
      fontSize = if (useSans) 16.sp else 19.sp,
      letterSpacing = 0.7.sp,
      color = MistColors.PureWhite.copy(alpha = 0.92f)
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppRow(
  app: AppInfo,
  iconCache: AppIconCache,
  iconStyle: com.example.data.preferences.IconStyle,
  tileSize: androidx.compose.ui.unit.Dp,
  isItalic: Boolean,
  useSans: Boolean,
  hapticFeedback: Boolean,
  reducedMotion: Boolean,
  lowPowerMode: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val haptics = LocalMistHaptics.current
  val interactionSource = remember(app.componentKey) { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = MistColors.PureWhite),
        onClick = {
          if (hapticFeedback) {
            haptics.trigger(MistHapticEvent.CONFIRM)
          }
          onClick()
        },
        onLongClick = {
          if (hapticFeedback) {
            haptics.trigger(MistHapticEvent.LONG_PRESS)
          }
          onLongClick()
        }
      )
      .padding(horizontal = 4.dp, vertical = 7.dp)
      .testTag("drawer_app_row_${app.packageName}")
  ) {
    MistAppIcon(
      app = app,
      iconCache = iconCache,
      iconStyle = iconStyle,
      tileSize = tileSize,
      isPressed = isPressed,
      reducedMotion = reducedMotion,
      lowPowerMode = lowPowerMode
    )

    Spacer(modifier = Modifier.width(16.dp))

    Text(
      text = app.label,
      style = appLabelTextStyle(isItalic = isItalic, useSans = useSans),
      color = MistColors.OffWhite.copy(alpha = 0.94f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun DrawerSearchHeader(
  query: String,
  autoFocus: Boolean,
  onQueryChange: (String) -> Unit,
  onClear: () -> Unit,
  onClose: () -> Unit,
  onSubmitFirst: () -> Unit,
  modifier: Modifier = Modifier
) {
  val haptics = LocalMistHaptics.current
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(autoFocus) {
    if (autoFocus) {
      focusRequester.requestFocus()
    }
  }

  GlassSurface(
    modifier = modifier.widthIn(max = 600.dp),
    shape = RoundedCornerShape(18.dp),
    backgroundColor = Color(0xCC171719)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .padding(horizontal = 6.dp)
    ) {
      IconButton(
        onClick = {
          haptics.trigger(MistHapticEvent.TAP_LIGHT)
          onClose()
        },
        modifier = Modifier.testTag("search_close_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.cd_close_search),
          tint = MistColors.OffWhite
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        if (query.isEmpty()) {
          Text(
            text = stringResource(R.string.search_placeholder),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 15.sp,
            color = MistColors.FrostedGrey
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = TextStyle(
            fontFamily = FunctionalSansFontFamily,
            fontSize = 15.sp,
            color = MistColors.OffWhite
          ),
          cursorBrush = SolidColor(MistColors.PureWhite),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
          keyboardActions = KeyboardActions(onGo = { onSubmitFirst() }),
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("drawer_search_input")
        )
      }

      if (query.isNotEmpty()) {
        IconButton(
          onClick = {
            haptics.trigger(MistHapticEvent.TAP_LIGHT)
            onClear()
          },
          modifier = Modifier.testTag("search_clear_button")
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.cd_clear_search),
            tint = MistColors.MistGrey
          )
        }
      }
    }
  }
}

@Composable
private fun SearchResultsList(
  results: List<AppInfo>,
  query: String,
  uiState: LauncherUiState,
  iconCache: AppIconCache,
  listState: LazyListState,
  emptyStatePullModifier: Modifier = Modifier,
  onLaunchApp: (AppInfo) -> Unit,
  onLongPressApp: (AppInfo) -> Unit
) {
  val prefs = uiState.preferences
  if (results.isEmpty()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .then(emptyStatePullModifier)
        .padding(horizontal = 32.dp)
        .testTag("search_empty_state"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = null,
        tint = MistColors.OffWhite.copy(alpha = 0.45f),
        modifier = Modifier.size(34.dp)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = stringResource(R.string.search_empty_title),
        fontFamily = if (prefs.typographyStyle == TypographyStyle.CLEAN_SANS) {
          FunctionalSansFontFamily
        } else {
          EditorialSerifFontFamily
        },
        fontSize = 20.sp,
        color = MistColors.OffWhite.copy(alpha = 0.85f)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = if (query.isNotBlank()) {
          "\"$query\" did not match any installed app."
        } else {
          stringResource(R.string.search_empty_subtitle)
        },
        fontFamily = FunctionalSansFontFamily,
        fontSize = 13.sp,
        color = MistColors.MistGrey.copy(alpha = 0.7f)
      )
    }
  } else {
    val isItalic = prefs.typographyStyle == TypographyStyle.SERIF_ITALIC
    val useSans = prefs.typographyStyle == TypographyStyle.CLEAN_SANS

    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .testTag("search_results_list"),
      contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
      items(
        items = results,
        key = { "search_${it.componentKey}" },
        contentType = { "app_row" }
      ) { app ->
        DrawerAppRow(
          app = app,
          iconCache = iconCache,
          iconStyle = prefs.iconStyle,
          tileSize = prefs.iconSize.drawerTileDp.dp,
          isItalic = isItalic,
          useSans = useSans,
          hapticFeedback = prefs.hapticFeedback,
          reducedMotion = prefs.effectiveReducedMotion,
          lowPowerMode = prefs.effectiveLowEffects,
          onClick = { onLaunchApp(app) },
          onLongClick = { onLongPressApp(app) }
        )
      }
    }
  }
}
