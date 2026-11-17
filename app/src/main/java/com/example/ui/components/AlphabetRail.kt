package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
  activeKey: String?,
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
  var scrubbedKey by remember { mutableStateOf<String?>(null) }

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
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
          )
        )
      }
    } else {
      if (reducedMotion) {
        waveStrength.snapTo(0f)
      } else {
        waveStrength.animateTo(
          targetValue = 0f,
          animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing)
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
      scrubbedKey = selected
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
    // Zero-cost transparent touch strip when invisibleMode is idle; wakes wave only during drag
    Box(
      modifier = Modifier
        .align(containerAlign)
        .width(touchWidth)
        .fillMaxHeight()
        .onSizeChanged { railHeightPxState.intValue = it.height }
        .pointerInput(entries) {
          detectTapGestures { offset ->
            lastSelectedIndex = -1
            dispatchIndexForY(offset.y)
            scrubbedKey = null
            onDragEnd()
          }
        }
        .pointerInput(entries) {
          detectVerticalDragGestures(
            onDragStart = { offset ->
              isDragging = true
              isWaveVisible = true
              lastSelectedIndex = -1
              dispatchIndexForY(offset.y)
            },
            onDragEnd = {
              isDragging = false
              lastSelectedIndex = -1
              scrubbedKey = null
              onDragEnd()
            },
            onDragCancel = {
              isDragging = false
              lastSelectedIndex = -1
              scrubbedKey = null
              onDragEnd()
            }
          ) { change, _ ->
            change.consume()
            dispatchIndexForY(change.position.y)
          }
        }
    )

    if (isWaveVisible) {
      val bubbleKey = scrubbedKey ?: activeKey
      val bubbleAlignment = if (isLeft) Alignment.TopStart else Alignment.TopEnd
      Box(
        modifier = Modifier
          .align(bubbleAlignment)
          .offset {
            val h = railHeightPxState.intValue.toFloat()
            val py = pointerYState.floatValue
            val y = if (py >= 0f) {
              (py - bubbleHalfSizePx).coerceIn(0f, (h - bubbleHalfSizePx * 2).coerceAtLeast(0f))
            } else {
              0f
            }
            val w = waveStrength.value
            val x = ((bubbleBaseOffsetXPx + (bubbleSwingXPx * w)) * dirSign).roundToInt()
            IntOffset(x = x, y = y.roundToInt())
          }
          .graphicsLayer {
            val w = waveStrength.value
            alpha = w
            val s = 0.72f + (0.28f * w)
            scaleX = s
            scaleY = s
          }
          .size(54.dp)
          .clip(CircleShape)
          .background(MistColors.AlphabetBubbleFill)
          .border(width = 0.9.dp, brush = BUBBLE_RIM_BRUSH, shape = CircleShape),
        contentAlignment = Alignment.Center
      ) {
        if (bubbleKey == FAVORITES_INDEX_KEY) {
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

      Column(
        modifier = Modifier
          .align(containerAlign)
          .width(34.dp)
          .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
      ) {
        val favCd = stringResource(R.string.cd_favorites_index)
        val useDarkIdleInk = isEdgeBright && !invisibleMode
        val baseInk = if (useDarkIdleInk) MistColors.DeepCharcoal else MistColors.OffWhite
        val activeInk = if (useDarkIdleInk) MistColors.DeepCharcoal else MistColors.PureWhite

        entries.forEachIndexed { index, key ->
          val isActive = key == (scrubbedKey ?: activeKey)
          val isAvailable = when (key) {
            FAVORITES_INDEX_KEY -> hasFavorites
            else -> key in availableSections
          }

          Box(
            modifier = Modifier
              .size(width = 26.dp, height = 18.dp)
              .graphicsLayer {
                val w = waveStrength.value
                val py = pointerYState.floatValue
                val h = railHeightPxState.intValue.toFloat()
                if (w > 0.001f && py >= 0f && h > 0f) {
                  val letterCenterY = ((index + 0.5f) / entries.size.toFloat()) * h
                  val dist = abs(letterCenterY - py)
                  val gaussian = exp(-(dist * dist) / (2f * sigmaPx * sigmaPx))
                  translationX = dirSign * maxCurvePx * gaussian * w
                  val s = 1f + (0.44f * gaussian * w)
                  scaleX = s
                  scaleY = s
                  alpha = if (invisibleMode) w else (0.72f + 0.28f * gaussian * w)
                } else {
                  translationX = 0f
                  scaleX = 1f
                  scaleY = 1f
                  alpha = if (invisibleMode) 0f else 1f
                }
              }
              .clip(CircleShape)
              .background(
                if (isActive && !isDragging && !invisibleMode) {
                  if (useDarkIdleInk) Color(0x28171719) else Color(0x36FFFFFF)
                } else {
                  Color.Transparent
                }
              )
              .semantics {
                contentDescription = if (key == FAVORITES_INDEX_KEY) favCd else "Jump to $key"
              },
            contentAlignment = Alignment.Center
          ) {
            if (key == FAVORITES_INDEX_KEY) {
              Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = when {
                  isActive -> activeInk
                  isAvailable -> baseInk.copy(alpha = 0.88f)
                  else -> baseInk.copy(alpha = 0.36f)
                },
                modifier = Modifier.size(11.dp)
              )
            } else {
              Text(
                text = key,
                fontFamily = FunctionalSansFontFamily,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = when {
                  isActive -> activeInk
                  isAvailable -> baseInk.copy(alpha = 0.86f)
                  else -> baseInk.copy(alpha = 0.36f)
                }
              )
            }
          }
        }
      }
    }
  }
}

private val BUBBLE_RIM_BRUSH = Brush.linearGradient(
  listOf(MistColors.SpecularRimTop, MistColors.SpecularRimMid, MistColors.SpecularRimBottom)
)
