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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.haptics.LocalMistHaptics
import com.example.core.haptics.MistHapticEvent
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

/**
 * Shared controller for the single alphabetic drawer wave.
 * Both the visible right-edge rail and the lightweight left-edge touch sensor
 * write into this single state holder so only ONE 28-letter rail is ever composed.
 */
@Stable
class AlphabetRailController(
  val entries: List<String> = listOf(FAVORITES_INDEX_KEY) + AppIndexHelper.ALPHABET_RAIL_LETTERS
) {
  var railHeightPx: Int by mutableIntStateOf(1)
  var sensorHeightPx: Int by mutableIntStateOf(1)
  var isDragging: Boolean by mutableStateOf(false)
  var pointerY: Float by mutableFloatStateOf(-1f)
  var smoothedPointerY: Float by mutableFloatStateOf(-1f)
  var dragPullTarget: Float by mutableFloatStateOf(0f)
  var lastSelectedIndex: Int by mutableIntStateOf(-1)
  var scrubbedKey: String? by mutableStateOf(null)
  val waveStrength = Animatable(0f)
  val dragStretch = Animatable(0f)
}

@Composable
fun rememberAlphabetRailController(): AlphabetRailController {
  return remember { AlphabetRailController() }
}

@Composable
fun AlphabetRail(
  availableSections: Set<String>,
  activeKeyProvider: () -> String?,
  hasFavorites: Boolean,
  hapticEnabled: Boolean,
  onSelectKey: (String) -> Unit,
  modifier: Modifier = Modifier,
  controller: AlphabetRailController = rememberAlphabetRailController(),
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
  val entries = controller.entries
  val haptics = LocalMistHaptics.current
  val density = LocalDensity.current

  var isWaveVisible by remember(invisibleMode) { mutableStateOf(!invisibleMode) }

  LaunchedEffect(controller.isDragging, reducedMotion, invisibleMode) {
    val dragging = controller.isDragging
    onDraggingChanged(dragging)
    if (dragging) {
      isWaveVisible = true
      if (reducedMotion) {
        controller.waveStrength.snapTo(1f)
      } else {
        controller.waveStrength.animateTo(
          targetValue = 1f,
          animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
          )
        )
      }
    } else {
      if (reducedMotion) {
        controller.waveStrength.snapTo(0f)
      } else if (controller.waveStrength.value > 0f) {
        controller.waveStrength.animateTo(
          targetValue = 0f,
          animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        )
      }
      if (invisibleMode && !controller.isDragging) {
        isWaveVisible = false
      }
    }
  }

  // Vsync-paced sideways stretch & vertical wave follower (settles cleanly with 0 idle frame churn)
  LaunchedEffect(controller.isDragging, reducedMotion) {
    if (controller.isDragging) {
      snapshotFlow { controller.pointerY to controller.dragPullTarget }
        .collectLatest { (targetY, targetPull) ->
          if (reducedMotion) {
            if (targetY >= 0f) controller.smoothedPointerY = targetY
            controller.dragStretch.snapTo(targetPull)
            return@collectLatest
          }
          var lastFrameNanos = 0L
          while (
            (targetY >= 0f && (controller.smoothedPointerY < 0f || abs(targetY - controller.smoothedPointerY) > 0.35f)) ||
            abs(targetPull - controller.dragStretch.value) > 0.002f
          ) {
            var nextStretch = controller.dragStretch.value
            withFrameNanos { frameNanos ->
              val dt = if (lastFrameNanos == 0L) 0.016f else {
                ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
              }
              val factor = 1f - exp(-26f * dt)
              if (targetY >= 0f) {
                val curY = controller.smoothedPointerY
                controller.smoothedPointerY = if (curY < 0f || abs(targetY - curY) <= 0.35f) {
                  targetY
                } else {
                  curY + (targetY - curY) * factor
                }
              }
              val curStretch = controller.dragStretch.value
              val diffStretch = targetPull - curStretch
              nextStretch = if (abs(diffStretch) <= 0.002f) {
                targetPull
              } else {
                curStretch + diffStretch * factor
              }
              lastFrameNanos = frameNanos
            }
            controller.dragStretch.snapTo(nextStretch)
          }
          if (targetY >= 0f) controller.smoothedPointerY = targetY
          controller.dragStretch.snapTo(targetPull)
        }
    } else {
      if (reducedMotion) {
        controller.dragStretch.snapTo(0f)
        controller.smoothedPointerY = controller.pointerY
      } else if (controller.dragStretch.value > 0f) {
        controller.dragStretch.animateTo(
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
    val heightPx = controller.railHeightPx
    if (heightPx <= 0) return
    val maxY = (heightPx - verticalPadPx).coerceAtLeast(verticalPadPx)
    val clampedY = y.coerceIn(verticalPadPx, maxY)
    controller.pointerY = clampedY
    val usableH = (heightPx - verticalPadPx * 2f).coerceAtLeast(1f)
    val continuousIdx = (((clampedY - verticalPadPx) / usableH) * entries.size)
      .coerceIn(0f, entries.size.toFloat())
    val idx = continuousIdx.toInt().coerceIn(0, entries.lastIndex)

    if (emitContinuous) {
      onContinuousScrub?.invoke(continuousIdx)
    }

    if (idx != controller.lastSelectedIndex) {
      val hadPreviousSelection = controller.lastSelectedIndex != -1
      controller.lastSelectedIndex = idx
      val selected = entries[idx]
      controller.scrubbedKey = selected
      if (hapticEnabled && hadPreviousSelection) {
        haptics.trigger(MistHapticEvent.WAVE_LETTER_TICK)
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
        .onSizeChanged { controller.railHeightPx = it.height.coerceAtLeast(1) }
        .pointerInput(entries, isLeft, controller) {
          val touchSlopPx = viewConfiguration.touchSlop
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val downX = down.position.x
            val downY = down.position.y
            var scrubUnlocked = false
            controller.lastSelectedIndex = -1
            controller.isDragging = true
            isWaveVisible = true
            controller.dragPullTarget = 0f
            controller.smoothedPointerY = downY.coerceIn(
              verticalPadPx,
              (controller.railHeightPx - verticalPadPx).coerceAtLeast(verticalPadPx)
            )
            onDragPullChanged(0f)
            if (hapticEnabled) {
              haptics.trigger(MistHapticEvent.WAVE_ENGAGE)
            }
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
              controller.dragPullTarget = pullRatio
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

            controller.isDragging = false
            controller.dragPullTarget = 0f
            onDragPullChanged(0f)
            controller.lastSelectedIndex = -1
            controller.scrubbedKey = null
            if (hapticEnabled) {
              haptics.trigger(MistHapticEvent.WAVE_RELEASE)
            }
            onDragEnd()
          }
        }
    )

    if (isWaveVisible || controller.isDragging || controller.waveStrength.value > 0.01f) {
      AlphabetWaveBubble(
        bubbleKeyProvider = { controller.scrubbedKey ?: activeKeyProvider() },
        waveStrengthProvider = { controller.waveStrength.value },
        dragStretchProvider = { controller.dragStretch.value },
        pointerYProvider = { controller.smoothedPointerY.takeIf { it >= 0f } ?: controller.pointerY },
        railHeightProvider = { controller.railHeightPx.toFloat() },
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
        invisibleMode = invisibleMode && !controller.isDragging,
        useDarkIdleInk = isEdgeBright && !invisibleMode,
        dirSign = dirSign,
        verticalPadPx = verticalPadPx,
        baseCurvePx = baseCurvePx,
        extraCurvePx = extraCurvePx,
        baseSigmaPx = baseSigmaPx,
        extraSigmaPx = extraSigmaPx,
        activeKeyProvider = activeKeyProvider,
        isDraggingProvider = { controller.isDragging },
        waveStrengthProvider = { controller.waveStrength.value },
        dragStretchProvider = { controller.dragStretch.value },
        pointerYProvider = { controller.smoothedPointerY.takeIf { it >= 0f } ?: controller.pointerY },
        railHeightProvider = { controller.railHeightPx.toFloat() },
        modifier = Modifier
          .align(containerAlign)
          .width(34.dp)
          .fillMaxHeight()
          .padding(vertical = 4.dp)
      )
    }
  }
}

/**
 * Lightweight single-Box left-edge touch sensor (`1` node instead of `28+`).
 * Sliding on the left edge drives the exact same shared [AlphabetRailController]
 * as the right edge so both sides share one alphabetic drawer list with zero duplicate UI cost.
 */
@Composable
fun LeftAlphabetEdgeSensor(
  controller: AlphabetRailController,
  hapticEnabled: Boolean,
  onSelectKey: (String) -> Unit,
  modifier: Modifier = Modifier,
  onDraggingChanged: (Boolean) -> Unit = {},
  onDragPullChanged: (Float) -> Unit = {},
  onContinuousScrub: ((Float) -> Unit)? = null,
  onDragEnd: () -> Unit = {}
) {
  val entries = controller.entries
  val haptics = LocalMistHaptics.current
  val density = LocalDensity.current
  val verticalPadPx = with(density) { 4.dp.toPx() }
  val maxPullInputPx = with(density) { 80.dp.toPx() }

  fun dispatchMappedY(sensorY: Float, emitSelect: Boolean, emitContinuous: Boolean) {
    val sensorH = controller.sensorHeightPx.coerceAtLeast(1)
    val railH = controller.railHeightPx.coerceAtLeast(1)
    val fraction = ((sensorY - verticalPadPx) / (sensorH - verticalPadPx * 2f).coerceAtLeast(1f))
      .coerceIn(0f, 1f)
    val mappedY = verticalPadPx + fraction * (railH - verticalPadPx * 2f).coerceAtLeast(1f)
    controller.pointerY = mappedY

    val continuousIdx = (fraction * entries.size).coerceIn(0f, entries.size.toFloat())
    val idx = continuousIdx.toInt().coerceIn(0, entries.lastIndex)

    if (emitContinuous) {
      onContinuousScrub?.invoke(continuousIdx)
    }

    if (idx != controller.lastSelectedIndex) {
      val hadPreviousSelection = controller.lastSelectedIndex != -1
      controller.lastSelectedIndex = idx
      val selected = entries[idx]
      controller.scrubbedKey = selected
      if (hapticEnabled && hadPreviousSelection) {
        haptics.trigger(MistHapticEvent.WAVE_LETTER_TICK)
      }
      if (emitSelect) {
        onSelectKey(selected)
      }
    }
  }

  Box(
    modifier = modifier
      .testTag("left_alphabet_rail")
      .width(28.dp)
      .fillMaxHeight()
      .onSizeChanged { controller.sensorHeightPx = it.height.coerceAtLeast(1) }
      .pointerInput(entries, controller) {
        val touchSlopPx = viewConfiguration.touchSlop
        awaitEachGesture {
          val down = awaitFirstDown(requireUnconsumed = false)
          down.consume()
          val downX = down.position.x
          val downY = down.position.y
          var scrubUnlocked = false
          controller.lastSelectedIndex = -1
          controller.isDragging = true
          onDraggingChanged(true)
          controller.dragPullTarget = 0f
          onDragPullChanged(0f)
          if (hapticEnabled) {
            haptics.trigger(MistHapticEvent.WAVE_ENGAGE)
          }
          dispatchMappedY(downY, emitSelect = true, emitContinuous = false)
          controller.smoothedPointerY = controller.pointerY

          while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
            if (change == null || !change.pressed) {
              change?.consume()
              break
            }
            change.consume()

            val inwardDx = (change.position.x - downX).coerceAtLeast(0f)
            val pullRatio = (inwardDx / maxPullInputPx).coerceIn(0f, 1f)
            controller.dragPullTarget = pullRatio
            onDragPullChanged(pullRatio)

            if (!scrubUnlocked && hypot(change.position.x - downX, change.position.y - downY) >= touchSlopPx) {
              scrubUnlocked = true
            }
            if (scrubUnlocked) {
              dispatchMappedY(
                sensorY = change.position.y,
                emitSelect = onContinuousScrub == null,
                emitContinuous = onContinuousScrub != null
              )
            }
          }

          controller.isDragging = false
          onDraggingChanged(false)
          controller.dragPullTarget = 0f
          onDragPullChanged(0f)
          controller.lastSelectedIndex = -1
          controller.scrubbedKey = null
          if (hapticEnabled) {
            haptics.trigger(MistHapticEvent.WAVE_RELEASE)
          }
          onDragEnd()
        }
      }
  )
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
