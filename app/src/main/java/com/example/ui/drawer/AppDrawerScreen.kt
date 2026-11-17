package com.example.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.data.apps.AppIndexHelper
import com.example.data.apps.AppInfo
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

  // Smooth elastic horizontal glide away from the active thumb edge synced with sideways pull
  var draggingSide by remember { mutableStateOf<AlphabetRailSide?>(null) }
  val drawerScrubPullState = remember { mutableFloatStateOf(0f) }
  val listDragShiftX = remember { Animatable(0f) }
  val baseShiftPx = with(density) { 14.dp.toPx() }
  val extraShiftPx = with(density) { 26.dp.toPx() }
  val defaultRowHeightPx = with(density) { 56.dp.toPx() }

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

  val currentIsScrubbing by rememberUpdatedState(isScrubbingAnyEdge)
  val currentIsSearchActive by rememberUpdatedState(uiState.isSearchActive)
  val currentReducedMotion by rememberUpdatedState(effectiveReducedMotion)
  val currentOnCloseDrawer by rememberUpdatedState(onCloseDrawer)

  fun settlePullOrDismiss(velocityY: Float) {
    if (pullDownOffset.value >= dismissThresholdPx ||
      (pullDownOffset.value > 16f && velocityY >= flingVelocityThresholdPx)
    ) {
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
    if (effectiveReducedMotion || effectiveSide == null) {
      if (effectiveReducedMotion) {
        listDragShiftX.snapTo(0f)
      } else {
        listDragShiftX.animateTo(
          targetValue = 0f,
          animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessMediumLow
          )
        )
      }
    } else {
      val dirSign = if (effectiveSide == AlphabetRailSide.RIGHT) -1f else 1f
      snapshotFlow {
        val pull = if (hideRightAlphabetRail || hideLeftAlphabetRail) {
          homeScrubPullProvider()
        } else {
          drawerScrubPullState.floatValue
        }
        dirSign * (baseShiftPx + extraShiftPx * pull)
      }.collect { targetShift ->
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
  val rowAnchors = remember(entries, sectionIndexMap, uiState.drawerRows.size) {
    buildContinuousRowAnchors(entries, sectionIndexMap, uiState.drawerRows.size)
  }

  var pinnedJumpKey by remember { mutableStateOf<String?>(null) }
  val targetContinuousRowState = remember { mutableFloatStateOf(-1f) }

  // Clear pinned section highlight as soon as user manually drags the app list
  LaunchedEffect(listState) {
    listState.interactionSource.interactions.collect { interaction ->
      if (interaction is DragInteraction.Start) {
        pinnedJumpKey = null
        targetContinuousRowState.floatValue = -1f
      }
    }
  }

  // Initial jump when opening the drawer from Home screen's Alphabet Rail:
  // Wait for LazyColumn's initial measurement pass if cold-opening so scrollToItem never drops.
  LaunchedEffect(uiState.drawerJumpTarget, sectionIndexMap, uiState.drawerRows.size) {
    val target = uiState.drawerJumpTarget ?: return@LaunchedEffect
    val resolved = findNearestSection(target, sectionIndexMap)
    if (resolved != null && resolved.second < uiState.drawerRows.size) {
      val index = resolved.second
      if (listState.layoutInfo.totalItemsCount <= index) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
          .first { it > index }
      }
      pinnedJumpKey = resolved.first
      targetContinuousRowState.floatValue = -1f
      listState.scrollToItem(index, scrollOffset = 0)
    }
    onJumpTargetHandled()
  }

  // Discrete jump events (taps on Home rail when drawer is open)
  LaunchedEffect(drawerJumpEvents, sectionIndexMap) {
    drawerJumpEvents.collect { target ->
      val resolved = findNearestSection(target, sectionIndexMap)
      if (resolved != null) {
        pinnedJumpKey = resolved.first
        targetContinuousRowState.floatValue = -1f
        listState.scrollToItem(resolved.second, scrollOffset = 0)
      }
    }
  }

  // Continuous scrub stream from Home rail -> updates lock-free float target
  LaunchedEffect(drawerContinuousScrubEvents, rowAnchors) {
    drawerContinuousScrubEvents.collect { continuousIdx ->
      if (uiState.drawerRows.isNotEmpty()) {
        pinnedJumpKey = null
        targetContinuousRowState.floatValue = resolveContinuousRow(continuousIdx, rowAnchors, uiState.drawerRows.size)
      }
    }
  }

  // Vsync-paced frame follower: smoothly interpolates LazyColumn item + pixel offset at 60-120Hz while dragging
  LaunchedEffect(isScrubbingAnyEdge, effectiveReducedMotion, uiState.drawerRows.size) {
    if (!isScrubbingAnyEdge || uiState.drawerRows.isEmpty()) return@LaunchedEffect

    var smoothedRow = -1f
    var lastFrameNanos = 0L

    while (true) {
      var pendingScroll: Pair<Int, Int>? = null
      withFrameNanos { frameNanos ->
        val target = targetContinuousRowState.floatValue
        if (target >= 0f) {
          val visibleInfo = listState.layoutInfo.visibleItemsInfo
          val avgRowPx = if (visibleInfo.isNotEmpty()) {
            (visibleInfo.sumOf { it.size }.toFloat() / visibleInfo.size).coerceAtLeast(1f)
          } else {
            defaultRowHeightPx
          }
          val maxRow = (uiState.drawerRows.size - 1).coerceAtLeast(0).toFloat()
          val clampedTarget = target.coerceIn(0f, maxRow)

          if (effectiveReducedMotion) {
            smoothedRow = clampedTarget
          } else {
            if (smoothedRow < 0f) {
              smoothedRow = (listState.firstVisibleItemIndex +
                (listState.firstVisibleItemScrollOffset / avgRowPx)).coerceIn(0f, maxRow)
            }
            val dt = if (lastFrameNanos == 0L) 0.016f else {
              ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            }
            val factor = 1f - exp(-26f * dt)
            smoothedRow += (clampedTarget - smoothedRow) * factor
            if (abs(clampedTarget - smoothedRow) < 0.005f) {
              smoothedRow = clampedTarget
            }
          }
          lastFrameNanos = frameNanos

          val itemIndex = smoothedRow.toInt().coerceIn(0, uiState.drawerRows.lastIndex)
          val offsetPx = ((smoothedRow - itemIndex) * avgRowPx).roundToInt().coerceAtLeast(0)
          pendingScroll = itemIndex to offsetPx
        } else {
          lastFrameNanos = frameNanos
        }
      }
      pendingScroll?.let { (itemIndex, offsetPx) ->
        listState.scrollToItem(itemIndex, offsetPx)
      }
    }
  }

  // Only read inside activeKeyProvider lambda passed to AlphabetRail -> AppDrawerScreen NEVER recomposes while scrolling!
  val activeIndexKeyState = remember(sectionIndexMap) {
    derivedStateOf {
      val pinned = pinnedJumpKey
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
        transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(110)) },
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
                  onClick = onCloseDrawer
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

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
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
              bottom = 96.dp
            )
          ) {
            items(
              items = uiState.drawerRows,
              key = { it.rowKey },
              contentType = { it.contentType }
            ) { rowItem ->
              when (rowItem) {
                is DrawerRowItem.Header -> {
                  DrawerSectionHeader(
                    title = if (rowItem.isFavoritesHeader) favoritesTitle else rowItem.title,
                    useSans = useSans || rowItem.isFavoritesHeader,
                    modifier = Modifier
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
                    onLongClick = { onLongPressApp(app) }
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
                  if (uiState.drawerRows.isNotEmpty()) {
                    pinnedJumpKey = null
                    targetContinuousRowState.floatValue =
                      resolveContinuousRow(continuousIdx, rowAnchors, uiState.drawerRows.size)
                  }
                },
                onSelectKey = { targetKey ->
                  val resolved = findNearestSection(targetKey, sectionIndexMap)
                  if (resolved != null) {
                    pinnedJumpKey = resolved.first
                    targetContinuousRowState.floatValue = resolved.second.toFloat()
                    coroutineScope.launch { listState.scrollToItem(resolved.second, scrollOffset = 0) }
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
                  if (uiState.drawerRows.isNotEmpty()) {
                    pinnedJumpKey = null
                    targetContinuousRowState.floatValue =
                      resolveContinuousRow(continuousIdx, rowAnchors, uiState.drawerRows.size)
                  }
                },
                onSelectKey = { targetKey ->
                  val resolved = findNearestSection(targetKey, sectionIndexMap)
                  if (resolved != null) {
                    pinnedJumpKey = resolved.first
                    targetContinuousRowState.floatValue = resolved.second.toFloat()
                    coroutineScope.launch { listState.scrollToItem(resolved.second, scrollOffset = 0) }
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
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 24.dp, bottom = 20.dp)
      )
    }
  }
}

private fun buildContinuousRowAnchors(
  entries: List<String>,
  sectionIndexMap: Map<String, Int>,
  totalRows: Int
): FloatArray {
  val anchors = FloatArray(entries.size + 1)
  if (totalRows <= 0 || sectionIndexMap.isEmpty()) return anchors

  val known = FloatArray(entries.size) { Float.NaN }
  for (i in entries.indices) {
    val idx = sectionIndexMap[entries[i]]
    if (idx != null) {
      known[i] = idx.toFloat()
    }
  }
  if (known[0].isNaN()) known[0] = 0f

  var lastIdx = 0
  var i = 1
  while (i < entries.size) {
    if (!known[i].isNaN()) {
      val startVal = known[lastIdx]
      val endVal = known[i]
      val span = i - lastIdx
      for (k in lastIdx..i) {
        anchors[k] = startVal + ((k - lastIdx).toFloat() / span) * (endVal - startVal)
      }
      lastIdx = i
    }
    i++
  }

  val tailStart = known[lastIdx]
  val endRow = (totalRows - 1).coerceAtLeast(0).toFloat()
  val tailSpan = (entries.size - lastIdx).coerceAtLeast(1)
  for (k in lastIdx..entries.size) {
    anchors[k] = tailStart + ((k - lastIdx).toFloat() / tailSpan) * (endRow - tailStart)
  }
  return anchors
}

private fun resolveContinuousRow(
  continuousIdx: Float,
  anchors: FloatArray,
  totalRows: Int
): Float {
  if (totalRows <= 1 || anchors.size < 2) return 0f
  val maxEntry = (anchors.size - 2).coerceAtLeast(0)
  val i = continuousIdx.toInt().coerceIn(0, maxEntry)
  val frac = (continuousIdx - i).coerceIn(0f, 1f)
  return (anchors[i] + frac * (anchors[i + 1] - anchors[i]))
    .coerceIn(0f, (totalRows - 1).toFloat())
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
  modifier: Modifier = Modifier
) {
  Text(
    text = title,
    fontFamily = if (useSans) FunctionalSansFontFamily else EditorialDisplayFontFamily,
    fontSize = if (useSans) 16.sp else 19.sp,
    letterSpacing = 0.6.sp,
    color = MistColors.OffWhite.copy(alpha = 0.72f),
    modifier = modifier.padding(start = 4.dp)
  )
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
  val haptics = LocalHapticFeedback.current
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
        onClick = onClick,
        onLongClick = {
          if (hapticFeedback) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
        onClick = onClose,
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
          onClick = onClear,
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
