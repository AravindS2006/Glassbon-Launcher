package com.example.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.haptics.LocalMistHaptics
import com.example.core.haptics.MistHapticEvent
import com.example.ui.LauncherUiState
import com.example.ui.components.GlassSurface
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors
import kotlin.math.roundToInt

@Composable
fun WallpaperStudioScreen(
  uiState: LauncherUiState,
  onPickWallpaperUri: (Uri) -> Unit,
  onRestoreDefault: () -> Unit,
  onSetGrayscale: (Boolean) -> Unit,
  onSetDarkening: (Float) -> Unit,
  onSetBlurIntensity: (Float) -> Unit,
  onClose: () -> Unit
) {
  BackHandler(onBack = onClose)
  val prefs = uiState.preferences
  val haptics = LocalMistHaptics.current

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) onPickWallpaperUri(uri)
    }
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = 20.dp, vertical = 12.dp)
      .testTag("wallpaper_studio_screen")
  ) {
    // Top bar leaves the middle of the screen completely open to preview the live wallpaper!
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .align(Alignment.TopStart)
        .fillMaxWidth()
    ) {
      GlassSurface(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color(0xBA171719)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(end = 16.dp)
        ) {
          IconButton(
            onClick = {
              haptics.trigger(MistHapticEvent.TAP_LIGHT)
              onClose()
            },
            modifier = Modifier.testTag("wallpaper_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = MistColors.OffWhite
            )
          }
          Text(
            text = stringResource(R.string.wallpaper_title),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 22.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MistColors.OffWhite
          )
        }
      }
    }

    // Bottom studio controls
    GlassSurface(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .widthIn(max = 560.dp)
        .fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      backgroundColor = Color(0xDB171719)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              haptics.trigger(MistHapticEvent.CONFIRM)
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = MistColors.OffWhite,
              contentColor = MistColors.NearBlack
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("wallpaper_pick_button")
          ) {
            Text(
              text = "Choose Photo",
              fontFamily = FunctionalSansFontFamily,
              fontSize = 13.5.sp,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis
            )
          }

          if (prefs.customWallpaperPath != null) {
            OutlinedButton(
              onClick = {
                haptics.trigger(MistHapticEvent.TAP_LIGHT)
                onRestoreDefault()
              },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MistColors.OffWhite),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("wallpaper_restore_button")
            ) {
              Text(
                text = "Restore Forest",
                fontFamily = FunctionalSansFontFamily,
                fontSize = 13.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              haptics.trigger(MistHapticEvent.TAP_LIGHT)
              onSetGrayscale(!prefs.grayscaleWallpaper)
            }
            .padding(vertical = 2.dp)
        ) {
          Text(
            text = "Monochrome Grayscale",
            fontFamily = FunctionalSansFontFamily,
            fontSize = 14.sp,
            color = MistColors.OffWhite,
            modifier = Modifier.weight(1f)
          )
          Switch(
            checked = prefs.grayscaleWallpaper,
            onCheckedChange = {
              haptics.trigger(MistHapticEvent.TAP_LIGHT)
              onSetGrayscale(it)
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = MistColors.NearBlack,
              checkedTrackColor = MistColors.OffWhite,
              uncheckedThumbColor = MistColors.MistGrey,
              uncheckedTrackColor = MistColors.SecondaryCharcoal
            )
          )
        }

        Column {
          Row(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "Darkening Vignette",
              fontFamily = FunctionalSansFontFamily,
              fontSize = 13.sp,
              color = MistColors.MistGrey,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = "${(prefs.wallpaperDarkening * 100).roundToInt()}%",
              fontFamily = FunctionalSansFontFamily,
              fontSize = 12.sp,
              color = MistColors.FrostedGrey
            )
          }
          Slider(
            value = prefs.wallpaperDarkening,
            onValueChange = onSetDarkening,
            valueRange = 0f..0.85f,
            colors = SliderDefaults.colors(
              thumbColor = MistColors.OffWhite,
              activeTrackColor = MistColors.OffWhite,
              inactiveTrackColor = MistColors.SecondaryCharcoal
            )
          )
        }

        Column {
          Row(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "Drawer Frosted Diffusion",
              fontFamily = FunctionalSansFontFamily,
              fontSize = 13.sp,
              color = MistColors.MistGrey,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = "${(prefs.drawerBlurIntensity * 100).roundToInt()}%",
              fontFamily = FunctionalSansFontFamily,
              fontSize = 12.sp,
              color = MistColors.FrostedGrey
            )
          }
          Slider(
            value = prefs.drawerBlurIntensity,
            onValueChange = onSetBlurIntensity,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
              thumbColor = MistColors.OffWhite,
              activeTrackColor = MistColors.OffWhite,
              inactiveTrackColor = MistColors.SecondaryCharcoal
            )
          )
        }
      }
    }
  }
}
