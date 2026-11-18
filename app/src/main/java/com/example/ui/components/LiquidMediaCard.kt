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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.haptics.LocalMistHaptics
import com.example.core.haptics.MistHapticEvent
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
  val cardShape = remember { RoundedCornerShape(22.dp) }
  val haptics = LocalMistHaptics.current

  Box(
    modifier = modifier
      .widthIn(max = 390.dp)
      .fillMaxWidth()
      .clip(cardShape)
      .background(MEDIA_CARD_FILL_BRUSH)
      .border(0.75.dp, MEDIA_CARD_BORDER_BRUSH, cardShape)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = MistColors.PureWhite),
        onClick = {
          haptics.trigger(MistHapticEvent.TAP_LIGHT)
          onOpenPlayer()
        }
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
          fontSize = 15.sp,
          color = MistColors.PureWhite.copy(alpha = 0.95f),
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
            fontSize = 12.5.sp,
            color = MistColors.OffWhite.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("media_artist_text")
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          IconButton(
            onClick = {
              haptics.trigger(MistHapticEvent.TAP_LIGHT)
              onPrevious()
            },
            modifier = Modifier
              .size(48.dp)
              .testTag("media_prev_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.SkipPrevious,
              contentDescription = stringResource(R.string.cd_media_previous),
              tint = MistColors.OffWhite.copy(alpha = 0.92f),
              modifier = Modifier.size(24.dp)
            )
          }

          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = MistColors.PureWhite),
                onClick = {
                  haptics.trigger(MistHapticEvent.CONFIRM)
                  onPlayPause()
                }
              )
              .testTag("media_play_pause_button"),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
                .border(1.dp, MistColors.PureWhite.copy(alpha = 0.78f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.cd_media_play_pause),
                tint = MistColors.PureWhite,
                modifier = Modifier.size(22.dp)
              )
            }
          }

          IconButton(
            onClick = {
              haptics.trigger(MistHapticEvent.TAP_LIGHT)
              onNext()
            },
            modifier = Modifier
              .size(48.dp)
              .testTag("media_next_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.SkipNext,
              contentDescription = stringResource(R.string.cd_media_next),
              tint = MistColors.OffWhite.copy(alpha = 0.92f),
              modifier = Modifier.size(24.dp)
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
  val shape = remember { RoundedCornerShape(16.dp) }
  val wipeProgress = remember { Animatable(1f) }
  val wipePath = remember { Path() }

  LaunchedEffect(trackKey, reducedMotion) {
    if (reducedMotion) {
      wipeProgress.snapTo(1f)
    } else {
      wipeProgress.snapTo(0f)
      wipeProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
      )
    }
  }

  Box(
    modifier = Modifier
      .size(76.dp)
      .clip(shape)
      .background(Color(0x6616171A))
      .border(0.75.dp, Color(0x4DFFFFFF), shape)
  ) {
    AnimatedContent(
      targetState = artwork,
      transitionSpec = {
        fadeIn(tween(220)) togetherWith fadeOut(tween(300))
      },
      label = "liquid_media_art"
    ) { bmp ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .drawWithContent {
            val p = wipeProgress.value
            if (p >= 0.998f) {
              drawContent()
            } else {
              val w = size.width
              val h = size.height
              val baseX = (p * 1.18f - 0.09f) * w
              val amp = w * 0.09f * sin(p * Math.PI).toFloat()

              wipePath.reset()
              wipePath.moveTo(0f, 0f)
              wipePath.lineTo((baseX + amp).coerceAtLeast(0f), 0f)
              wipePath.cubicTo(
                (baseX - amp).coerceIn(0f, w), h * 0.35f,
                (baseX + amp * 1.25f).coerceIn(0f, w), h * 0.65f,
                (baseX - amp * 0.4f).coerceAtLeast(0f), h
              )
              wipePath.lineTo(0f, h)
              wipePath.close()

              clipPath(wipePath) {
                this@drawWithContent.drawContent()
              }

              if (p > 0.02f) {
                val frontX = p * w
                drawRect(
                  brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color(0x66FFFFFF), Color.Transparent),
                    startX = frontX - w * 0.16f,
                    endX = frontX + w * 0.10f
                  )
                )
              }
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

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(ART_CORNER_SHEEN_BRUSH)
    )
  }
}

private val MEDIA_CARD_FILL_BRUSH = Brush.verticalGradient(
  colors = listOf(MistColors.MediaCardFillTop, MistColors.MediaCardFillBottom)
)

private val MEDIA_CARD_BORDER_BRUSH = Brush.linearGradient(
  colors = listOf(MistColors.SpecularRimTop, MistColors.SpecularRimMid, MistColors.SpecularRimBottom)
)

private val ART_CORNER_SHEEN_BRUSH = Brush.linearGradient(
  colors = listOf(Color(0x28FFFFFF), Color.Transparent),
  start = Offset.Zero,
  end = Offset(110f, 110f)
)
