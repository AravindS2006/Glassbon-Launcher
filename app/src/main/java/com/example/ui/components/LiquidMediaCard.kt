package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.media.ActiveMediaState
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors
import kotlin.math.sin

@Composable
fun LiquidMediaCard(
  media: ActiveMediaState,
  reducedMotion: Boolean,
  onOpenPlayer: () -> Unit,
  onPrevious: () -> Unit,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  modifier: Modifier = Modifier
) {
  val cardShape = RoundedCornerShape(20.dp)
  val cardFill = Brush.verticalGradient(
    colors = listOf(MistColors.MediaCardFillTop, MistColors.MediaCardFillBottom)
  )
  val borderBrush = Brush.linearGradient(
    colors = listOf(MistColors.SpecularRimTop, MistColors.SpecularRimMid, MistColors.SpecularRimBottom)
  )

  Box(
    modifier = modifier
      .widthIn(max = 360.dp)
      .fillMaxWidth()
      .clip(cardShape)
      .background(cardFill)
      .border(0.85.dp, borderBrush, cardShape)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = MistColors.PureWhite),
        onClick = onOpenPlayer
      )
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag("home_media_card")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      LiquidWipeArtwork(
        trackKey = media.trackKey,
        artwork = media.artwork,
        reducedMotion = reducedMotion
      )

      Spacer(modifier = Modifier.width(14.dp))

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = media.title,
          fontFamily = FunctionalSansFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.5.sp,
          color = MistColors.OffWhite,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("media_title_text")
        )
        if (media.artist.isNotBlank()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = media.artist,
            fontFamily = FunctionalSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = MistColors.OffWhite.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("media_artist_text")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          IconButton(
            onClick = onPrevious,
            modifier = Modifier
              .size(36.dp)
              .testTag("media_prev_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.SkipPrevious,
              contentDescription = stringResource(R.string.cd_media_previous),
              tint = MistColors.OffWhite.copy(alpha = 0.90f),
              modifier = Modifier.size(22.dp)
            )
          }

          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color(0x28FFFFFF))
              .border(1.dp, MistColors.OffWhite.copy(alpha = 0.75f), CircleShape)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = MistColors.PureWhite),
                onClick = onPlayPause
              )
              .testTag("media_play_pause_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription = stringResource(R.string.cd_media_play_pause),
              tint = MistColors.PureWhite,
              modifier = Modifier.size(20.dp)
            )
          }

          IconButton(
            onClick = onNext,
            modifier = Modifier
              .size(36.dp)
              .testTag("media_next_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.SkipNext,
              contentDescription = stringResource(R.string.cd_media_next),
              tint = MistColors.OffWhite.copy(alpha = 0.90f),
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LiquidWipeArtwork(
  trackKey: String,
  artwork: ImageBitmap?,
  reducedMotion: Boolean
) {
  val shape = RoundedCornerShape(14.dp)
  val wipeProgress = remember { Animatable(1f) }

  LaunchedEffect(trackKey, reducedMotion) {
    if (reducedMotion) {
      wipeProgress.snapTo(1f)
    } else {
      wipeProgress.snapTo(0f)
      wipeProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
      )
    }
  }

  val liquidWaveClip = remember(wipeProgress.value) {
    val p = wipeProgress.value
    GenericShape { size, _ ->
      if (p >= 0.999f) {
        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
      } else {
        val baseX = (p * 1.18f - 0.09f) * size.width
        val amp = size.width * 0.09f * sin(p * Math.PI).toFloat()
        moveTo(0f, 0f)
        lineTo((baseX + amp).coerceAtLeast(0f), 0f)
        cubicTo(
          (baseX - amp).coerceIn(0f, size.width), size.height * 0.35f,
          (baseX + amp * 1.3f).coerceIn(0f, size.width), size.height * 0.65f,
          (baseX - amp * 0.4f).coerceAtLeast(0f), size.height
        )
        lineTo(0f, size.height)
        close()
      }
    }
  }

  Box(
    modifier = Modifier
      .size(76.dp)
      .clip(shape)
      .background(Color(0x9916161A))
      .border(0.75.dp, MistColors.SpecularRimTop.copy(alpha = 0.45f), shape)
  ) {
    AnimatedContent(
      targetState = artwork,
      transitionSpec = {
        fadeIn(tween(240)) togetherWith fadeOut(tween(340))
      },
      label = "liquid_media_art"
    ) { bmp ->
      val p = wipeProgress.value
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(liquidWaveClip)
          .drawWithContent {
            drawContent()
            // Liquid specular glass sheen riding along the wipe wavefront
            if (p > 0.02f && p < 0.98f) {
              val frontX = p * size.width
              drawRect(
                brush = Brush.horizontalGradient(
                  colors = listOf(
                    Color.Transparent,
                    Color(0x66FFFFFF),
                    Color.Transparent
                  ),
                  startX = (frontX - size.width * 0.18f),
                  endX = (frontX + size.width * 0.12f)
                )
              )
            }
          },
        contentAlignment = Alignment.Center
      ) {
        if (bmp != null) {
          Image(
            bitmap = bmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MistColors.OffWhite.copy(alpha = 0.72f),
            modifier = Modifier.size(30.dp)
          )
        }
      }
    }

    // Subtle glass top corner reflection
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.linearGradient(
            colors = listOf(Color(0x28FFFFFF), Color.Transparent),
            start = Offset.Zero,
            end = Offset(120f, 120f)
          )
        )
    )
  }
}
