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
    fontFamily = EditorialDisplayFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 72.sp,
    lineHeight = 74.sp,
    letterSpacing = (-1.2).sp
  ),
  displayMedium = TextStyle(
    fontFamily = EditorialDisplayFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 46.sp,
    lineHeight = 50.sp,
    letterSpacing = (-0.5).sp
  ),
  headlineMedium = TextStyle(
    fontFamily = EditorialDisplayFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = 0.sp
  ),
  headlineSmall = TextStyle(
    fontFamily = EditorialSerifFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.1.sp
  ),
  titleMedium = TextStyle(
    fontFamily = EditorialSerifFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 19.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.15.sp
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
