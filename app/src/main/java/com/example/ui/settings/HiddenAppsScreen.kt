package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.core.graphics.AppIconCache
import com.example.core.haptics.LocalMistHaptics
import com.example.core.haptics.MistHapticEvent
import com.example.data.apps.AppInfo
import com.example.ui.LauncherUiState
import com.example.ui.components.GlassSurface
import com.example.ui.components.MistAppIcon
import com.example.ui.theme.EditorialDisplayFontFamily
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors

@Composable
fun HiddenAppsScreen(
  uiState: LauncherUiState,
  iconCache: AppIconCache,
  onUnhideApp: (AppInfo) -> Unit,
  onClose: () -> Unit
) {
  BackHandler(onBack = onClose)
  val haptics = LocalMistHaptics.current

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xD90B0B0D))
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("hidden_apps_screen"),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 620.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp)
      ) {
        IconButton(onClick = {
          haptics.trigger(MistHapticEvent.TAP_LIGHT)
          onClose()
        }) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = MistColors.OffWhite
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Hidden Applications",
          fontFamily = if (uiState.preferences.typographyStyle == com.example.data.preferences.TypographyStyle.CLEAN_SANS) {
            FunctionalSansFontFamily
          } else {
            EditorialDisplayFontFamily
          },
          fontSize = 24.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = MistColors.OffWhite,
          modifier = Modifier.weight(1f)
        )
      }

      if (uiState.hiddenApps.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No hidden applications. Long-press any app in the library to hide it.",
            fontFamily = FunctionalSansFontFamily,
            fontSize = 14.sp,
            color = MistColors.MistGrey
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(
            items = uiState.hiddenApps,
            key = { "hidden_${it.componentKey}" }
          ) { app ->
            GlassSurface(
              shape = RoundedCornerShape(14.dp),
              backgroundColor = Color(0x99171719)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 56.dp)
                  .padding(horizontal = 14.dp, vertical = 8.dp)
              ) {
                MistAppIcon(
                  app = app,
                  iconCache = iconCache,
                  iconStyle = uiState.preferences.iconStyle,
                  tileSize = 34.dp,
                  lowPowerMode = uiState.preferences.effectiveLowEffects
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                  text = app.label,
                  fontFamily = if (uiState.preferences.typographyStyle == com.example.data.preferences.TypographyStyle.CLEAN_SANS) {
                    FunctionalSansFontFamily
                  } else {
                    EditorialSerifFontFamily
                  },
                  fontSize = 15.5.sp,
                  color = MistColors.OffWhite,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                  haptics.trigger(MistHapticEvent.CONFIRM)
                  onUnhideApp(app)
                }) {
                  Text(
                    text = "Restore",
                    fontFamily = FunctionalSansFontFamily,
                    fontSize = 13.sp,
                    color = MistColors.OffWhite
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
