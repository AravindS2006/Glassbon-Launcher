package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.apps.AppIndexHelper
import com.example.ui.theme.EditorialDisplayFontFamily
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.roundToInt

const val FAVORITES_INDEX_KEY = "★"

enum class AlphabetRailSide {
  RIGHT,
  LEFT
}

@Composable
fun AlphabetRail(
  availableSections: Set<String>,
  activeKeyProvider: () -> String?,
  hasFavorites: Boolean,
  hapticEnabled: Boolean,
  onSelectKey: (String) -> Unit,
  modifier: Modifier = Modifier,
  side: AlphabetRailSide = AlphabetRailSide.RIGHT,
  invisibleMode: Boolean = false,
  reducedMotion: Boolean = false,
  useSansBubble: Boolean = true,
  isEdgeBright: Boolean = false,
  onDraggingChanged: (Boolean) -> Unit = {},
  onDragPullChanged: (Float) -> Unit = {},
  onContinuousScrub: ((Float) -> Unit)? = null,
  onDragEnd: () -> Unit = {}
) {
  val entries = remember { listOf(FAVORITES_INDEX_KEY) + AppIndexHelper.ALPHABET_RAIL_LETTERS }
  val haptics = LocalHapticFeedback.current
  val density = LocalDensity.current

  val railHeightPxState = remember { mutableIntStateOf(1) }
  var isDragging by remember { mutableStateOf(false) }
  var isWaveVisible by remember(invisibleMode) { mutableStateOf(!invisibleMode) }
  val pointerYState = remember { mutableFloatStateOf(-1f) }
  val smoothedPointerYState = remember { mutableFloatStateOf(-1f) }
  val dragPullTargetState = remember { mutableFloatStateOf(0f) }
  var lastSelectedIndex by remember { mutableIntStateOf(-1) }
  val scrubbedKeyState = remember { mutableStateOf<String?>(null) }

  val waveStrength = remember { Animatable(0f) }
  val dragStretch = remember { Animatable(0f) }

  LaunchedEffect(isDragging, reducedMotion, invisibleMode) {
    onDraggingChanged(isDragging)
    if (isDragging) {
      isWaveVisible = true
      if (reducedMotion) {
        waveStrength.snapTo(1f)
      } else {
        waveStrength.animateTo(
          targetValue = 1f,
          animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
          )
        )
      }
    } else {
      if (reducedMotion) {
        waveStrength.snapTo(0f)
      } else if (waveStrength.value > 0f) {
        waveStrength.animateTo(
          targetValue = 0f,
          animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        )
      }
      if (invisibleMode && !isDragging) {
        isWaveVisible = false
      }
    }
  }

  // Vsync-paced sideways stretch & vertical wave follower (settles cleanly with 0 idle frame churn)
  LaunchedEffect(isDragging, reducedMotion) {
    if (isDragging) {
      snapshotFlow { pointerYState.floatValue to dragPullTargetState.floatValue }
        .collectLatest { (targetY, targetPull) ->
          if (reducedMotion) {
            if (targetY >= 0f) smoothedPointerYState.floatValue = targetY
            dragStretch.snapTo(targetPull)
            return@collectLatest
          }
          var lastFrameNanos = 0L
          while (
            (targetY >= 0f && (smoothedPointerYState.floatValue < 0f || abs(targetY - smoothedPointerYState.floatValue) > 0.35f)) ||
            abs(targetPull - dragStretch.value) > 0.002f
          ) {
            var nextStretch = dragStretch.value
            withFrameNanos { frameNanos ->
              val dt = if (lastFrameNanos == 0L) 0.016f else {
                ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
              }
              val factor = 1f - exp(-26f * dt)
              if (targetY >= 0f) {
                val curY = smoothedPointerYState.floatValue
                smoothedPointerYState.floatValue = if (curY < 0f || abs(targetY - curY) <= 0.35f) {
                  targetY
                } else {
                  curY + (targetY - curY) * factor
                }
              }
              val curStretch = dragStretch.value
              val diffStretch = targetPull - curStretch
              nextStretch = if (abs(diffStretch) <= 0.002f) {
                targetPull
              } else {
                curStretch + diffStretch * factor
              }
              lastFrameNanos = frameNanos
            }
            dragStretch.snapTo(nextStretch)
          }
          if (targetY >= 0f) smoothedPointerYState.floatValue = targetY
          dragStretch.snapTo(targetPull)
        }
    } else {
      if (reducedMotion) {
        dragStretch.snapTo(0f)
        smoothedPointerYState.floatValue = pointerYState.floatValue
      } else if (dragStretch.value > 0f) {
        dragStretch.animateTo(
          targetValue = 0f,
          animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessMedium
          )
        )
      }
    }
  }

  val isLeft = side == AlphabetRailSide.LEFT
  val dirSign = if (isLeft) 1f else -1f
  val verticalPadPx = with(density) { 4.dp.toPx() }
  val baseCurvePx = with(density) { 36.dp.toPx() }
  val extraCurvePx = with(density) { 46.dp.toPx() }
  val baseSigmaPx = with(density) { 54.dp.toPx() }
  val extraSigmaPx = with(density) { 24.dp.toPx() }
  val maxPullInputPx = with(density) { 80.dp.toPx() }
  val bubbleHalfSizePx = with(density) { 27.dp.toPx() }
  val bubbleBaseOffsetXPx = with(density) { 74.dp.roundToPx() }
  val bubbleSwingXPx = with(density) { 18.dp.toPx() }
  val bubbleStretchXPx = with(density) { 42.dp.toPx() }

  fun dispatchContinuousForY(y: Float, emitSelect: Boolean, emitContinuous: Boolean) {
    val heightPx = railHeightPxState.intValue
    if (heightPx <= 0) return
    val maxY = (heightPx - verticalPadPx).coerceAtLeast(verticalPadPx)
    val clampedY = y.coerceIn(verticalPadPx, maxY)
    pointerYState.floatValue = clampedY
    val usableH = (heightPx - verticalPadPx * 2f).coerceAtLeast(1f)
    val continuousIdx = (((clampedY - verticalPadPx) / usableH) * entries.size)
      .coerceIn(0f, entries.size.toFloat())
    val idx = continuousIdx.toInt().coerceIn(0, entries.lastIndex)

    if (emitContinuous) {
      onContinuousScrub?.invoke(continuousIdx)
    }

    if (idx != lastSelectedIndex) {
      lastSelectedIndex = idx
      val selected = entries[idx]
      scrubbedKeyState.value = selected
      if (hapticEnabled) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      if (emitSelect) {
        onSelectKey(selected)
      }
    }
  }

  val railTag = if (isLeft) "left_alphabet_rail" else "alphabet_rail"
  val containerAlign = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
  val touchWidth = if (invisibleMode) 30.dp else 38.dp

  Box(
    modifier = modifier
      .testTag(railTag)
      .width(42.dp),
    contentAlignment = containerAlign
  ) {
    // Unified pointer handler supporting sideways elastic stretch + continuous pixel scrubbing
    Box(
      modifier = Modifier
        .align(containerAlign)
        .width(touchWidth)
        .fillMaxHeight()
        .onSizeChanged { railHeightPxState.intValue = it.height }
        .pointerInput(entries, isLeft) {
          val touchSlopPx = viewConfiguration.touchSlop
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val downX = down.position.x
            val downY = down.position.y
            var scrubUnlocked = false
            lastSelectedIndex = -1
            isDragging = true
            isWaveVisible = true
            dragPullTargetState.floatValue = 0f
            smoothedPointerYState.floatValue = downY.coerceIn(verticalPadPx, (railHeightPxState.intValue - verticalPadPx).coerceAtLeast(verticalPadPx))
            onDragPullChanged(0f)
            dispatchContinuousForY(downY, emitSelect = true, emitContinuous = false)

            while (true) {
              val event = awaitPointerEvent()
              val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
              if (change == null || !change.pressed) {
                change?.consume()
                break
              }
              change.consume()

              val inwardDx = if (isLeft) {
                (change.position.x - downX).coerceAtLeast(0f)
              } else {
                (downX - change.position.x).coerceAtLeast(0f)
              }
              val pullRatio = (inwardDx / maxPullInputPx).coerceIn(0f, 1f)
              dragPullTargetState.floatValue = pullRatio
              onDragPullChanged(pullRatio)

              if (!scrubUnlocked && hypot(change.position.x - downX, change.position.y - downY) >= touchSlopPx) {
                scrubUnlocked = true
              }
              if (scrubUnlocked) {
                dispatchContinuousForY(
                  y = change.position.y,
                  emitSelect = onContinuousScrub == null,
                  emitContinuous = onContinuousScrub != null
                )
              }
            }

            isDragging = false
            dragPullTargetState.floatValue = 0f
            onDragPullChanged(0f)
            lastSelectedIndex = -1
            scrubbedKeyState.value = null
            onDragEnd()
          }
        }
    )

    if (isWaveVisible) {
      AlphabetWaveBubble(
        bubbleKeyProvider = { scrubbedKeyState.value ?: activeKeyProvider() },
        waveStrengthProvider = { waveStrength.value },
        dragStretchProvider = { dragStretch.value },
        pointerYProvider = { smoothedPointerYState.floatValue.takeIf { it >= 0f } ?: pointerYState.floatValue },
        railHeightProvider = { railHeightPxState.intValue.toFloat() },
        dirSign = dirSign,
        bubbleHalfSizePx = bubbleHalfSizePx,
        bubbleBaseOffsetXPx = bubbleBaseOffsetXPx,
        bubbleSwingXPx = bubbleSwingXPx,
        bubbleStretchXPx = bubbleStretchXPx,
        useSansBubble = useSansBubble,
        modifier = Modifier.align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
      )

      AlphabetLetterColumn(
        entries = entries,
        railTag = railTag,
        availableSections = availableSections,
        hasFavorites = hasFavorites,
        invisibleMode = invisibleMode,
        useDarkIdleInk = isEdgeBright && !invisibleMode,
        dirSign = dirSign,
        verticalPadPx = verticalPadPx,
        baseCurvePx = baseCurvePx,
        extraCurvePx = extraCurvePx,
        baseSigmaPx = baseSigmaPx,
        extraSigmaPx = extraSigmaPx,
        activeKeyProvider = activeKeyProvider,
        isDraggingProvider = { isDragging },
        waveStrengthProvider = { waveStrength.value },
        dragStretchProvider = { dragStretch.value },
        pointerYProvider = { smoothedPointerYState.floatValue.takeIf { it >= 0f } ?: pointerYState.floatValue },
        railHeightProvider = { railHeightPxState.intValue.toFloat() },
        modifier = Modifier
          .align(containerAlign)
          .width(34.dp)
          .fillMaxHeight()
          .padding(vertical = 4.dp)
      )
    }
  }
}

@Composable
private fun AlphabetWaveBubble(
  bubbleKeyProvider: () -> String?,
  waveStrengthProvider: () -> Float,
  dragStretchProvider: () -> Float,
  pointerYProvider: () -> Float,
  railHeightProvider: () -> Float,
  dirSign: Float,
  bubbleHalfSizePx: Float,
  bubbleBaseOffsetXPx: Int,
  bubbleSwingXPx: Float,
  bubbleStretchXPx: Float,
  useSansBubble: Boolean,
  modifier: Modifier = Modifier
) {
  val bubbleKey = bubbleKeyProvider()
  val isBubbleIcon = bubbleKey == FAVORITES_INDEX_KEY

  Box(
    modifier = modifier
      .offset {
        val h = railHeightProvider()
        val py = pointerYProvider()
        val y = if (py >= 0f) {
          (py - bubbleHalfSizePx).coerceIn(0f, (h - bubbleHalfSizePx * 2).coerceAtLeast(0f))
        } else {
          0f
        }
        val w = waveStrengthProvider()
        val stretch = dragStretchProvider()
        val x = ((bubbleBaseOffsetXPx + (bubbleSwingXPx * w) + (bubbleStretchXPx * stretch)) * dirSign).roundToInt()
        IntOffset(x = x, y = y.roundToInt())
      }
      .graphicsLayer {
        val w = waveStrengthProvider()
        val stretch = dragStretchProvider()
        alpha = w
        val s = 0.72f + (0.28f * w) + (0.08f * stretch)
        scaleX = s
        scaleY = s
      }
      .size(54.dp)
      .drawBehind {
        drawCircle(color = MistColors.AlphabetBubbleFill)
        drawCircle(brush = BUBBLE_RIM_BRUSH, style = Stroke(width = 0.9.dp.toPx()))
      },
    contentAlignment = Alignment.Center
  ) {
    if (isBubbleIcon) {
      Icon(
        imageVector = Icons.Rounded.Star,
        contentDescription = null,
        tint = MistColors.PureWhite,
        modifier = Modifier.size(22.dp)
      )
    } else {
      Text(
        text = bubbleKey.orEmpty(),
        fontFamily = if (useSansBubble) FunctionalSansFontFamily else EditorialDisplayFontFamily,
        fontWeight = if (useSansBubble) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = if (useSansBubble) 26.sp else 29.sp,
        color = MistColors.PureWhite
      )
    }
  }
}

@Composable
private fun AlphabetLetterColumn(
  entries: List<String>,
  railTag: String,
  availableSections: Set<String>,
  hasFavorites: Boolean,
  invisibleMode: Boolean,
  useDarkIdleInk: Boolean,
  dirSign: Float,
  verticalPadPx: Float,
  baseCurvePx: Float,
  extraCurvePx: Float,
  baseSigmaPx: Float,
  extraSigmaPx: Float,
  activeKeyProvider: () -> String?,
  isDraggingProvider: () -> Boolean,
  waveStrengthProvider: () -> Float,
  dragStretchProvider: () -> Float,
  pointerYProvider: () -> Float,
  railHeightProvider: () -> Float,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top
  ) {
    val favCd = stringResource(R.string.cd_favorites_index)
    val baseInk = if (useDarkIdleInk) MistColors.DeepCharcoal else MistColors.OffWhite
    val activePillColor = if (useDarkIdleInk) Color(0x28171719) else Color(0x36FFFFFF)
    val entryCount = entries.size.toFloat()

    entries.forEachIndexed { index, key ->
      val isAvailable = when (key) {
        FAVORITES_INDEX_KEY -> hasFavorites
        else -> key in availableSections
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("${railTag}_key_$key")
          .graphicsLayer {
            val w = waveStrengthProvider()
            val py = pointerYProvider()
            val h = railHeightProvider()
            val stretch = dragStretchProvider()
            if (w > 0.001f && py >= 0f && h > 0f) {
              val usableH = (h - verticalPadPx * 2f).coerceAtLeast(1f)
              val letterCenterY = verticalPadPx + ((index + 0.5f) / entryCount) * usableH
              val dist = abs(letterCenterY - py)
              val sigma = baseSigmaPx + (extraSigmaPx * stretch)
              val gaussian = exp(-(dist * dist) / (2f * sigma * sigma))
              val curve = baseCurvePx + (extraCurvePx * stretch)
              translationX = dirSign * curve * gaussian * w
              val s = 1f + ((0.44f + 0.16f * stretch) * gaussian * w)
              scaleX = s
              scaleY = s
              alpha = if (invisibleMode) w else (0.76f + 0.24f * gaussian * w)
            } else {
              translationX = 0f
              scaleX = 1f
              scaleY = 1f
              alpha = if (invisibleMode) 0f else 1f
            }
          }
          .drawBehind {
            if (!invisibleMode && !isDraggingProvider() && activeKeyProvider() == key) {
              drawCircle(color = activePillColor, radius = (size.minDimension * 0.44f).coerceAtMost(11.dp.toPx()))
            }
          }
          .semantics {
            contentDescription = if (key == FAVORITES_INDEX_KEY) favCd else "Jump to $key"
          },
        contentAlignment = Alignment.Center
      ) {
        if (key == FAVORITES_INDEX_KEY) {
          Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = if (isAvailable) baseInk.copy(alpha = 0.90f) else baseInk.copy(alpha = 0.36f),
            modifier = Modifier.size(11.dp)
          )
        } else {
          Text(
            text = key,
            fontFamily = FunctionalSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            color = if (isAvailable) baseInk.copy(alpha = 0.88f) else baseInk.copy(alpha = 0.36f)
          )
        }
      }
    }
  }
}

private val BUBBLE_RIM_BRUSH = Brush.linearGradient(
  listOf(MistColors.SpecularRimTop, MistColors.SpecularRimMid, MistColors.SpecularRimBottom)
)
