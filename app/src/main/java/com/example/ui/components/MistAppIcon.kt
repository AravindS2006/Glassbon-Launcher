package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.graphics.AppIconCache
import com.example.data.apps.AppInfo
import com.example.data.preferences.IconStyle
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors

@Composable
fun MistAppIcon(
  app: AppInfo,
  iconCache: AppIconCache,
  iconStyle: IconStyle,
  tileSize: Dp,
  modifier: Modifier = Modifier,
  isPressed: Boolean = false,
  reducedMotion: Boolean = false,
  highContrastBackdrop: Boolean = false
) {
  val cached = iconCache.getCached(app.componentKey, iconStyle)
  val iconBitmap by produceState<ImageBitmap?>(
    initialValue = cached,
    key1 = app.componentKey,
    key2 = iconStyle
  ) {
    if (value == null) {
      value = iconCache.loadIcon(app, iconStyle)
    }
  }

  val shape = remember(tileSize) { RoundedCornerShape((tileSize.value * 0.30f).dp) }
  val isGlassMono = iconStyle == IconStyle.DOT_MONO

  Box(
    modifier = modifier
      .size(tileSize)
      .graphicsLayer {
        val s = if (isPressed && !reducedMotion) 0.92f else 1f
        scaleX = s
        scaleY = s
      }
      .clip(shape)
      .then(
        if (isGlassMono) {
          Modifier
            .background(if (highContrastBackdrop) TILE_FILL_CONTRAST_BRUSH else TILE_FILL_BRUSH)
            .border(width = 0.85.dp, brush = RIM_HIGHLIGHT_BRUSH, shape = shape)
        } else {
          Modifier
        }
      ),
    contentAlignment = Alignment.Center
  ) {
    if (isGlassMono) {
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth()
          .height(tileSize * 0.40f)
          .background(INNER_SHEEN_BRUSH)
      )
    }

    val bmp = iconBitmap
    if (bmp != null) {
      Image(
        bitmap = bmp,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxSize()
          .padding(if (isGlassMono) (tileSize.value * 0.19f).dp else 0.dp)
      )
    } else {
      Text(
        text = app.label.take(1).uppercase(),
        fontFamily = FunctionalSansFontFamily,
        fontSize = (tileSize.value * 0.36f).sp,
        color = MistColors.OffWhite.copy(alpha = 0.88f)
      )
    }
  }
}

private val TILE_FILL_BRUSH = Brush.verticalGradient(
  colors = listOf(MistColors.IconTileFillTop, MistColors.IconTileFillBottom)
)

private val TILE_FILL_CONTRAST_BRUSH = Brush.verticalGradient(
  colors = listOf(Color(0xD8202126), Color(0xCC111215))
)

private val RIM_HIGHLIGHT_BRUSH = Brush.linearGradient(
  colors = listOf(
    MistColors.SpecularRimTop,
    MistColors.SpecularRimMid,
    MistColors.SpecularRimBottom
  )
)

private val INNER_SHEEN_BRUSH = Brush.verticalGradient(
  colors = listOf(MistColors.InnerTopSheen, Color.Transparent)
)
