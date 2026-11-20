package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.haptics.LocalMistHaptics
import com.example.core.haptics.MistHapticEvent
import com.example.data.preferences.TypographyStyle
import com.example.platform.ClockSnapshot
import com.example.ui.theme.EditorialDisplayFontFamily
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors

@Composable
fun ClockView(
  snapshot: ClockSnapshot,
  showClock: Boolean,
  showDate: Boolean,
  isTopBright: Boolean,
  typographyStyle: TypographyStyle = TypographyStyle.CLEAN_SANS,
  onClockClick: () -> Unit,
  onDateClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!showClock && !showDate) return
  val haptics = LocalMistHaptics.current

  val primaryBrush = remember(isTopBright) {
    if (isTopBright) {
      Brush.verticalGradient(
        colors = listOf(
          MistColors.DeepCharcoal,
          MistColors.DeepCharcoal.copy(alpha = 0.82f)
        )
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          MistColors.PureWhite,
          MistColors.OffWhite.copy(alpha = 0.80f)
        )
      )
    }
  }

  val rippleColor = if (isTopBright) {
    MistColors.DeepCharcoal.copy(alpha = 0.22f)
  } else {
    MistColors.PureWhite.copy(alpha = 0.24f)
  }

  val pillBgColor = if (isTopBright) Color(0x1E121417) else Color(0x28FFFFFF)
  val pillBorderColor = if (isTopBright) Color(0x34121417) else Color(0x48FFFFFF)
  val pillTextColor = if (isTopBright) MistColors.DeepCharcoal else MistColors.OffWhite.copy(alpha = 0.94f)
  val pillIconColor = if (isTopBright) {
    MistColors.DeepCharcoal.copy(alpha = 0.78f)
  } else {
    MistColors.PureWhite.copy(alpha = 0.85f)
  }

  val subtleShadow = if (isTopBright) {
    Shadow(color = Color(0x24FFFFFF), offset = Offset(0f, 1f), blurRadius = 6f)
  } else {
    Shadow(color = Color(0x48000000), offset = Offset(0f, 2f), blurRadius = 10f)
  }

  val useSans = typographyStyle == TypographyStyle.CLEAN_SANS
  val clockFontFamily = if (useSans) FunctionalSansFontFamily else EditorialDisplayFontFamily

  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val isNarrow = maxWidth < 290.dp
    val clockFontSize = when {
      useSans && isNarrow -> 56.sp
      useSans -> 70.sp
      isNarrow -> 62.sp
      else -> 78.sp
    }
    val clockLineHeight = when {
      useSans && isNarrow -> 58.sp
      useSans -> 72.sp
      isNarrow -> 64.sp
      else -> 78.sp
    }

    Column(
      horizontalAlignment = Alignment.Start,
      modifier = Modifier.testTag("home_clock_area")
    ) {
      if (showClock) {
        // Optical left-bearing alignment (-3.dp) keeps large clock numerals flush with the Calendar pill below
        Text(
          text = snapshot.formattedTime,
          fontFamily = clockFontFamily,
          fontWeight = if (useSans) FontWeight.SemiBold else FontWeight.Normal,
          fontStyle = if (!useSans && typographyStyle == TypographyStyle.SERIF_ITALIC) {
            FontStyle.Italic
          } else {
            FontStyle.Normal
          },
          fontSize = clockFontSize,
          lineHeight = clockLineHeight,
          letterSpacing = if (useSans) (-2.6).sp else (-1.4).sp,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Clip,
          style = TextStyle(
            brush = primaryBrush,
            shadow = subtleShadow,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
          ),
          modifier = Modifier
            .offset(x = (-3).dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true, color = rippleColor),
              role = Role.Button,
              onClick = {
                haptics.trigger(MistHapticEvent.TAP_LIGHT)
                onClockClick()
              }
            )
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .testTag("home_clock_text")
        )
      }

      if (showClock && showDate) {
        Spacer(modifier = Modifier.height(8.dp))
      }

      if (showDate) {
        val pillShape = remember { RoundedCornerShape(12.dp) }
        Box(
          modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(pillShape)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = ripple(bounded = true, color = rippleColor),
              role = Role.Button,
              onClick = {
                haptics.trigger(MistHapticEvent.TAP_LIGHT)
                onDateClick()
              }
            ),
          contentAlignment = Alignment.CenterStart
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(pillShape)
              .background(pillBgColor)
              .border(0.7.dp, pillBorderColor, pillShape)
              .padding(horizontal = 12.dp, vertical = 7.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.CalendarToday,
              contentDescription = null,
              tint = pillIconColor,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
              text = snapshot.formattedDate.uppercase(),
              fontFamily = FunctionalSansFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = if (isNarrow) 11.5.sp else 12.sp,
              lineHeight = 15.sp,
              letterSpacing = 0.85.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              color = pillTextColor,
              style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
              ),
              modifier = Modifier.testTag("home_date_text")
            )
          }
        }
      }
    }
  }
}
