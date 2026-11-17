package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val EditorialDisplayFontFamily = FontFamily(
  Font(R.font.instrument_serif, FontWeight.Normal)
)

val EditorialSerifFontFamily = FontFamily(
  Font(R.font.instrument_serif, FontWeight.Normal)
)

val FunctionalSansFontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

val MistTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = FunctionalSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 72.sp,
    lineHeight = 74.sp,
    letterSpacing = (-1.6).sp
  ),
  displayMedium = TextStyle(
    fontFamily = FunctionalSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 46.sp,
    lineHeight = 50.sp,
    letterSpacing = (-0.8).sp
  ),
  headlineMedium = TextStyle(
    fontFamily = FunctionalSansFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 26.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.2).sp
  ),
  headlineSmall = TextStyle(
    fontFamily = FunctionalSansFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 21.sp,
    lineHeight = 27.sp,
    letterSpacing = 0.sp
  ),
  titleMedium = TextStyle(
    fontFamily = FunctionalSansFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp
  ),
  titleSmall = TextStyle(
    fontFamily = FunctionalSansFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = FunctionalSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.1.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = FunctionalSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.15.sp
  ),
  labelSmall = TextStyle(
    fontFamily = FunctionalSansFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.35.sp
  )
)

fun appLabelTextStyle(isItalic: Boolean, useSans: Boolean): TextStyle {
  return TextStyle(
    fontFamily = if (useSans) FunctionalSansFontFamily else EditorialSerifFontFamily,
    fontWeight = FontWeight.Normal,
    fontStyle = if (!useSans && isItalic) FontStyle.Italic else FontStyle.Normal,
    fontSize = if (useSans) 15.5.sp else 21.sp,
    lineHeight = if (useSans) 22.sp else 26.sp,
    letterSpacing = if (useSans) 0.1.sp else 0.25.sp
  )
}
