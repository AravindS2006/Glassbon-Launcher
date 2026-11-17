package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlin.math.exp
import kotlin.math.roundToInt

const val FAVORITES_INDEX_KEY = "★"

@Composable
fun AlphabetRail(
  availableSections: Set<String>,
  activeKey: String?,
  hasFavorites: Boolean,
  hapticEnabled: Boolean,
  onSelectKey: (String) -> Unit,
  modifier: Modifier = Modifier,
  invisibleMode: Boolean = false,
  reducedMotion: Boolean = false,
  onDragEnd: () -> Unit = {}
) {
  val entries = remember { listOf(FAVORITES_INDEX_KEY) + AppIndexHelper.ALPHABET_RAIL_LETTERS }
  val haptics = LocalHapticFeedback.current
  val density = LocalDensity.current

  var railHeightPx by remember { mutableIntStateOf(1) }
  var isDragging by remember { mutableStateOf(false) }
  var pointerY by remember { mutableFloatStateOf(-1f) }
  var lastSelectedIndex by remember { mutableIntStateOf(-1) }
  var scrubbedKey by remember { mutableStateOf<String?>(null) }

  val maxCurvePx = with(density) { 36.dp.toPx() }
  val sigmaPx = with(density) { 46.dp.toPx() }
  val bubbleHalfSizePx = with(density) { 26.dp.toPx() }

  val waveStrength by animateFloatAsState(
    targetValue = if (isDragging && !reducedMotion) 1f else 0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "alphabet_wave_strength"
  )

  fun dispatchIndexForY(y: Float) {
    if (railHeightPx <= 0) return
    val clampedY = y.coerceIn(0f, railHeightPx.toFloat())
    pointerY = clampedY
    val idx = ((clampedY / railHeightPx.toFloat()) * entries.size)
      .toInt()
      .coerceIn(0, entries.lastIndex)
    val selected = entries[idx]
    scrubbedKey = selected
    if (idx != lastSelectedIndex) {
      lastSelectedIndex = idx
      if (hapticEnabled) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      onSelectKey(selected)
    }
  }

  Box(
    modifier = modifier
      .testTag("alphabet_rail")
      .width(if (invisibleMode && !isDragging) 28.dp else 44.dp),
    contentAlignment = Alignment.CenterEnd
  ) {
    // Floating frosted circular letter indicator beside the curved wave
    val displayBubbleKey = scrubbedKey ?: activeKey
    val bubbleY = if (pointerY >= 0f) {
      (pointerY - bubbleHalfSizePx).coerceIn(0f, (railHeightPx - bubbleHalfSizePx * 2).coerceAtLeast(0f))
    } else {
      0f
    }

    AnimatedVisibility(
      visible = isDragging && displayBubbleKey != null,
      enter = fadeIn() + scaleIn(initialScale = 0.8f),
      exit = fadeOut() + scaleOut(targetScale = 0.8f),
      modifier = Modifier
        .align(Alignment.TopEnd)
        .offset {
          IntOffset(
            x = -with(density) { 74.dp.roundToPx() },
            y = bubbleY.roundToInt()
          )
        }
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(MistColors.AlphabetBubbleFill)
          .border(
            width = 0.9.dp,
            brush = Brush.linearGradient(
              listOf(MistColors.SpecularRimTop, MistColors.SpecularRimMid, MistColors.SpecularRimBottom)
            ),
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        if (displayBubbleKey == FAVORITES_INDEX_KEY) {
          Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = MistColors.PureWhite,
            modifier = Modifier.size(22.dp)
          )
        } else {
          Text(
            text = displayBubbleKey.orEmpty(),
            fontFamily = EditorialDisplayFontFamily,
            fontSize = 28.sp,
            color = MistColors.PureWhite
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .width(34.dp)
        .onSizeChanged { railHeightPx = it.height }
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
        .alpha(if (invisibleMode && !isDragging) 0f else 1f)
        .padding(vertical = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
      entries.forEachIndexed { index, key ->
        val isActive = key == (if (isDragging) scrubbedKey else activeKey)
        val isAvailable = when (key) {
          FAVORITES_INDEX_KEY -> hasFavorites
          else -> key in availableSections
        }
        val favCd = stringResource(R.string.cd_favorites_index)

        // Compute Gaussian arc offset around active pointer position
        val letterCenterY = ((index + 0.5f) / entries.size.toFloat()) * railHeightPx.toFloat()
        val dist = if (pointerY >= 0f) kotlin.math.abs(letterCenterY - pointerY) else 10_000f
        val gaussian = exp(-(dist * dist) / (2f * sigmaPx * sigmaPx))
        val targetOffsetX = -maxCurvePx * gaussian * waveStrength
        val letterScale = 1f + (0.42f * gaussian * waveStrength)

        Box(
          modifier = Modifier
            .size(width = 26.dp, height = 18.dp)
            .graphicsLayer {
              translationX = targetOffsetX
              scaleX = letterScale
              scaleY = letterScale
            }
            .clip(CircleShape)
            .background(
              if (isActive && !isDragging) Color(0x36FFFFFF) else Color.Transparent
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
                isActive -> MistColors.PureWhite
                isAvailable -> MistColors.OffWhite.copy(alpha = 0.88f)
                else -> MistColors.OffWhite.copy(alpha = 0.36f)
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
                isActive -> MistColors.PureWhite
                isAvailable -> MistColors.OffWhite.copy(alpha = 0.84f)
                else -> MistColors.OffWhite.copy(alpha = 0.34f)
              }
            )
          }
        }
      }
    }
  }
}
