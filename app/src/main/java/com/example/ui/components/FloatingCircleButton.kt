package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MistColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingCircleButton(
  icon: ImageVector,
  contentDescription: String,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  hapticFeedback: Boolean = true,
  reducedMotion: Boolean = false
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val haptics = LocalHapticFeedback.current
  val scale by animateFloatAsState(
    targetValue = if (isPressed && !reducedMotion) 0.90f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "floating_btn_scale"
  )

  val fillBrush = Brush.verticalGradient(
    colors = listOf(MistColors.FloatingButtonFillTop, MistColors.FloatingButtonFillBottom)
  )
  val borderBrush = Brush.linearGradient(
    colors = listOf(MistColors.SpecularRimTop, MistColors.SpecularRimMid, MistColors.SpecularRimBottom)
  )

  Box(
    modifier = modifier
      .testTag(testTag)
      .size(54.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .drawBehind {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(Color(0x52000000), Color.Transparent),
            radius = size.minDimension * 0.68f
          )
        )
      }
      .clip(CircleShape)
      .background(fillBrush)
      .border(0.85.dp, borderBrush, CircleShape)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = MistColors.PureWhite),
        role = Role.Button,
        onClick = onClick,
        onLongClick = if (onLongClick != null) {
          {
            if (hapticFeedback) {
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onLongClick()
          }
        } else {
          null
        }
      ),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = MistColors.OffWhite,
      modifier = Modifier.size(22.dp)
    )
  }
}
