package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.data.apps.AppInfo
import com.example.ui.LauncherUiState
import com.example.ui.components.GlassSurface
import com.example.ui.components.MistAppIcon
import com.example.ui.theme.EditorialDisplayFontFamily
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors

@Composable
fun FavoritesManagerScreen(
  uiState: LauncherUiState,
  iconCache: AppIconCache,
  onToggleFavorite: (AppInfo) -> Unit,
  onMoveFavorite: (String, Int) -> Unit,
  onClose: () -> Unit
) {
  BackHandler(onBack = onClose)
  val prefs = uiState.preferences
  val favoriteSet = remember(uiState.favoriteApps) {
    uiState.favoriteApps.map { it.componentKey }.toSet()
  }
  val otherApps = remember(uiState.visibleApps, favoriteSet) {
    uiState.visibleApps.filterNot { it.componentKey in favoriteSet }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xD90B0B0D))
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("favorites_manager_screen"),
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
        IconButton(
          onClick = onClose,
          modifier = Modifier.testTag("favorites_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = MistColors.OffWhite
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.favorites_title),
          fontFamily = EditorialDisplayFontFamily,
          fontSize = 26.sp,
          color = MistColors.OffWhite
        )
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item(key = "fav_header") {
          Text(
            text = "HOME RAIL ORDER (FIRST ${prefs.homeShortcutLimit} SHOWN ON HOME)",
            fontFamily = FunctionalSansFontFamily,
            fontSize = 11.sp,
            letterSpacing = 1.0.sp,
            color = MistColors.FrostedGrey,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
          )
        }

        if (uiState.favoriteApps.isEmpty()) {
          item(key = "fav_empty") {
            GlassSurface(
              shape = RoundedCornerShape(16.dp),
              backgroundColor = Color(0x88171719)
            ) {
              Text(
                text = "Tap any app below to pin it to your Home shortcut rail.",
                fontFamily = FunctionalSansFontFamily,
                fontSize = 13.5.sp,
                color = MistColors.MistGrey,
                modifier = Modifier.padding(16.dp)
              )
            }
          }
        } else {
          itemsIndexed(
            items = uiState.favoriteApps,
            key = { _, app -> "fav_mgr_${app.componentKey}" }
          ) { index, app ->
            val onHome = index < prefs.homeShortcutLimit
            GlassSurface(
              shape = RoundedCornerShape(14.dp),
              backgroundColor = Color(0x99171719)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 56.dp)
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                MistAppIcon(
                  app = app,
                  iconCache = iconCache,
                  iconStyle = prefs.iconStyle,
                  tileSize = 34.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = app.label,
                    fontFamily = EditorialSerifFontFamily,
                    fontSize = 16.sp,
                    color = MistColors.OffWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (onHome) "Home slot ${index + 1}" else "Favorites drawer",
                    fontFamily = FunctionalSansFontFamily,
                    fontSize = 11.sp,
                    color = MistColors.FrostedGrey
                  )
                }

                IconButton(
                  onClick = { onMoveFavorite(app.componentKey, -1) },
                  enabled = index > 0
                ) {
                  Icon(
                    imageVector = Icons.Outlined.ArrowUpward,
                    contentDescription = stringResource(R.string.action_move_up),
                    tint = if (index > 0) MistColors.OffWhite else MistColors.FrostedGrey.copy(alpha = 0.3f),
                    modifier = Modifier.size(19.dp)
                  )
                }

                IconButton(
                  onClick = { onMoveFavorite(app.componentKey, 1) },
                  enabled = index < uiState.favoriteApps.lastIndex
                ) {
                  Icon(
                    imageVector = Icons.Outlined.ArrowDownward,
                    contentDescription = stringResource(R.string.action_move_down),
                    tint = if (index < uiState.favoriteApps.lastIndex) {
                      MistColors.OffWhite
                    } else {
                      MistColors.FrostedGrey.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(19.dp)
                  )
                }

                IconButton(onClick = { onToggleFavorite(app) }) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_remove_favorite),
                    tint = MistColors.MistGrey,
                    modifier = Modifier.size(19.dp)
                  )
                }
              }
            }
          }
        }

        item(key = "all_apps_header") {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "ADD FROM APPLICATIONS",
            fontFamily = FunctionalSansFontFamily,
            fontSize = 11.sp,
            letterSpacing = 1.0.sp,
            color = MistColors.FrostedGrey,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
          )
        }

        items(
          items = otherApps,
          key = { "fav_add_${it.componentKey}" }
        ) { app ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 52.dp)
              .clickable { onToggleFavorite(app) }
              .padding(horizontal = 8.dp, vertical = 6.dp)
          ) {
            MistAppIcon(
              app = app,
              iconCache = iconCache,
              iconStyle = prefs.iconStyle,
              tileSize = 32.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
              text = app.label,
              fontFamily = EditorialSerifFontFamily,
              fontSize = 16.sp,
              color = MistColors.OffWhite,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            Icon(
              imageVector = Icons.Outlined.StarOutline,
              contentDescription = stringResource(R.string.action_add_favorite),
              tint = MistColors.FrostedGrey,
              modifier = Modifier.size(20.dp)
            )
          }
          HorizontalDivider(color = MistColors.HairlineDivider)
        }

        item(key = "fav_bottom_space") {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}
