package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
  isMediaActive: Boolean = false,
  onClockClick: () -> Unit,
  onDateClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!showClock && !showDate) return

  val primaryTextColor = if (isTopBright) MistColors.DeepCharcoal else MistColors.OffWhite
  val secondaryTextColor = if (isTopBright) {
    MistColors.DeepCharcoal.copy(alpha = 0.80f)
  } else {
    MistColors.OffWhite.copy(alpha = 0.82f)
  }
  val subtleShadow = if (isTopBright) {
    Shadow(
      color = Color(0x28FFFFFF),
      offset = Offset(0f, 1f),
      blurRadius = 6f
    )
  } else {
    Shadow(
      color = Color(0x55000000),
      offset = Offset(0f, 2f),
      blurRadius = 10f
    )
  }

  val useSans = typographyStyle == TypographyStyle.CLEAN_SANS
  val clockFontFamily = if (useSans) FunctionalSansFontFamily else EditorialDisplayFontFamily
  val clockFontSize = when {
    useSans && !isMediaActive -> 74.sp
    useSans -> 64.sp
    !isMediaActive -> 80.sp
    else -> 70.sp
  }
  val clockLineHeight = when {
    useSans && !isMediaActive -> 76.sp
    useSans -> 66.sp
    !isMediaActive -> 80.sp
    else -> 70.sp
  }

  Column(
    modifier = modifier.testTag("home_clock_area")
  ) {
    if (showClock) {
      Text(
        text = snapshot.formattedTime,
        fontFamily = clockFontFamily,
        fontWeight = if (useSans) FontWeight.Medium else FontWeight.Normal,
        fontStyle = if (!useSans && typographyStyle == TypographyStyle.SERIF_ITALIC) {
          FontStyle.Italic
        } else {
          FontStyle.Normal
        },
        fontSize = clockFontSize,
        lineHeight = clockLineHeight,
        letterSpacing = if (useSans) (-2.2).sp else (-1.4).sp,
        color = primaryTextColor,
        style = TextStyle(shadow = subtleShadow),
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = primaryTextColor.copy(alpha = 0.25f)),
            role = Role.Button,
            onClick = onClockClick
          )
          .padding(horizontal = 4.dp, vertical = 2.dp)
          .testTag("home_clock_text")
      )
    }
    if (showDate) {
      Text(
        text = snapshot.formattedDate,
        fontFamily = FunctionalSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = if (isMediaActive) 13.5.sp else 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.35.sp,
        color = secondaryTextColor,
        style = TextStyle(shadow = subtleShadow),
        modifier = Modifier
          .padding(top = if (showClock) 3.dp else 0.dp)
          .clip(RoundedCornerShape(8.dp))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = primaryTextColor.copy(alpha = 0.25f)),
            role = Role.Button,
            onClick = onDateClick
          )
          .padding(horizontal = 6.dp, vertical = 6.dp)
          .testTag("home_date_text")
      )
    }
  }
}
