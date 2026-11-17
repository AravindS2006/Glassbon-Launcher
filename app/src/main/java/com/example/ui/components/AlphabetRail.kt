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
import kotlin.math.abs
import kotlin.math.exp
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
  onDragEnd: () -> Unit = {}
) {
  val entries = remember { listOf(FAVORITES_INDEX_KEY) + AppIndexHelper.ALPHABET_RAIL_LETTERS }
  val haptics = LocalHapticFeedback.current
  val density = LocalDensity.current

  val railHeightPxState = remember { mutableIntStateOf(1) }
  var isDragging by remember { mutableStateOf(false) }
  var isWaveVisible by remember(invisibleMode) { mutableStateOf(!invisibleMode) }
  val pointerYState = remember { mutableFloatStateOf(-1f) }
  var lastSelectedIndex by remember { mutableIntStateOf(-1) }
  val scrubbedKeyState = remember { mutableStateOf<String?>(null) }

  val waveStrength = remember { Animatable(0f) }

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
          animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
        )
      }
      if (invisibleMode && !isDragging) {
        isWaveVisible = false
      }
    }
  }

  val isLeft = side == AlphabetRailSide.LEFT
  val dirSign = if (isLeft) 1f else -1f
  val maxCurvePx = with(density) { 40.dp.toPx() }
  val sigmaPx = with(density) { 52.dp.toPx() }
  val bubbleHalfSizePx = with(density) { 27.dp.toPx() }
  val bubbleBaseOffsetXPx = with(density) { 74.dp.roundToPx() }
  val bubbleSwingXPx = with(density) { 16.dp.toPx() }

  fun dispatchIndexForY(y: Float) {
    val heightPx = railHeightPxState.intValue
    if (heightPx <= 0) return
    val clampedY = y.coerceIn(0f, heightPx.toFloat())
    pointerYState.floatValue = clampedY
    val idx = ((clampedY / heightPx.toFloat()) * entries.size)
      .toInt()
      .coerceIn(0, entries.lastIndex)
    if (idx != lastSelectedIndex) {
      lastSelectedIndex = idx
      val selected = entries[idx]
      scrubbedKeyState.value = selected
      if (hapticEnabled) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      onSelectKey(selected)
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
    // Unified zero-slop pointer handler: blooms wave immediately on down and tracks at native frame rate
    Box(
      modifier = Modifier
        .align(containerAlign)
        .width(touchWidth)
        .fillMaxHeight()
        .onSizeChanged { railHeightPxState.intValue = it.height }
        .pointerInput(entries) {
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            lastSelectedIndex = -1
            isDragging = true
            isWaveVisible = true
            dispatchIndexForY(down.position.y)

            while (true) {
              val event = awaitPointerEvent()
              val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
              if (change == null || !change.pressed) {
                change?.consume()
                break
              }
              change.consume()
              dispatchIndexForY(change.position.y)
            }

            isDragging = false
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
        pointerYProvider = { pointerYState.floatValue },
        railHeightProvider = { railHeightPxState.intValue.toFloat() },
        isLeft = isLeft,
        dirSign = dirSign,
        bubbleHalfSizePx = bubbleHalfSizePx,
        bubbleBaseOffsetXPx = bubbleBaseOffsetXPx,
        bubbleSwingXPx = bubbleSwingXPx,
        useSansBubble = useSansBubble,
        modifier = Modifier.align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
      )

      AlphabetLetterColumn(
        entries = entries,
        availableSections = availableSections,
        hasFavorites = hasFavorites,
        invisibleMode = invisibleMode,
        useDarkIdleInk = isEdgeBright && !invisibleMode,
        dirSign = dirSign,
        maxCurvePx = maxCurvePx,
        sigmaPx = sigmaPx,
        activeKeyProvider = activeKeyProvider,
        isDraggingProvider = { isDragging },
        waveStrengthProvider = { waveStrength.value },
        pointerYProvider = { pointerYState.floatValue },
        railHeightProvider = { railHeightPxState.intValue.toFloat() },
        modifier = Modifier
          .align(containerAlign)
          .width(34.dp)
          .padding(vertical = 4.dp)
      )
    }
  }
}

@Composable
private fun AlphabetWaveBubble(
  bubbleKeyProvider: () -> String?,
  waveStrengthProvider: () -> Float,
  pointerYProvider: () -> Float,
  railHeightProvider: () -> Float,
  isLeft: Boolean,
  dirSign: Float,
  bubbleHalfSizePx: Float,
  bubbleBaseOffsetXPx: Int,
  bubbleSwingXPx: Float,
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
        val x = ((bubbleBaseOffsetXPx + (bubbleSwingXPx * w)) * dirSign).roundToInt()
        IntOffset(x = x, y = y.roundToInt())
      }
      .graphicsLayer {
        val w = waveStrengthProvider()
        alpha = w
        val s = 0.72f + (0.28f * w)
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
  availableSections: Set<String>,
  hasFavorites: Boolean,
  invisibleMode: Boolean,
  useDarkIdleInk: Boolean,
  dirSign: Float,
  maxCurvePx: Float,
  sigmaPx: Float,
  activeKeyProvider: () -> String?,
  isDraggingProvider: () -> Boolean,
  waveStrengthProvider: () -> Float,
  pointerYProvider: () -> Float,
  railHeightProvider: () -> Float,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(1.dp)
  ) {
    val favCd = stringResource(R.string.cd_favorites_index)
    val baseInk = if (useDarkIdleInk) MistColors.DeepCharcoal else MistColors.OffWhite
    val activePillColor = if (useDarkIdleInk) Color(0x28171719) else Color(0x36FFFFFF)

    entries.forEachIndexed { index, key ->
      val isAvailable = when (key) {
        FAVORITES_INDEX_KEY -> hasFavorites
        else -> key in availableSections
      }
      val entryCount = entries.size.toFloat()

      Box(
        modifier = Modifier
          .size(width = 26.dp, height = 18.dp)
          .graphicsLayer {
            val w = waveStrengthProvider()
            val py = pointerYProvider()
            val h = railHeightProvider()
            if (w > 0.001f && py >= 0f && h > 0f) {
              val letterCenterY = ((index + 0.5f) / entryCount) * h
              val dist = abs(letterCenterY - py)
              val gaussian = exp(-(dist * dist) / (2f * sigmaPx * sigmaPx))
              translationX = dirSign * maxCurvePx * gaussian * w
              val s = 1f + (0.44f * gaussian * w)
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
              drawCircle(color = activePillColor, radius = size.minDimension * 0.48f)
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
            lineHeight = 12.sp,
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
