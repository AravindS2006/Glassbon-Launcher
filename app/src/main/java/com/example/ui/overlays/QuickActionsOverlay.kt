package com.example.ui.overlays

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GlassSurface
import com.example.ui.theme.EditorialDisplayFontFamily
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors

@Composable
fun QuickActionsOverlay(
  isDefaultLauncher: Boolean,
  isMediaListenerEnabled: Boolean,
  onLockScreen: () -> Unit,
  onOpenLibrary: () -> Unit,
  onOpenSearch: () -> Unit,
  onOpenWallpaper: () -> Unit,
  onOpenFavorites: () -> Unit,
  onOpenPreferences: () -> Unit,
  onOpenMediaListenerSettings: () -> Unit,
  onOpenDeviceSettings: () -> Unit,
  onRequestDefaultHome: () -> Unit,
  onDismiss: () -> Unit
) {
  BackHandler(onBack = onDismiss)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0x660B0B0D))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismiss
      )
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = 24.dp, vertical = 20.dp)
      .testTag("quick_actions_overlay")
  ) {
    GlassSurface(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .widthIn(min = 260.dp, max = 320.dp)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {}
        ),
      shape = RoundedCornerShape(22.dp)
    ) {
      Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
          text = stringResource(R.string.quick_actions_title),
          fontFamily = EditorialDisplayFontFamily,
          fontSize = 20.sp,
          color = MistColors.OffWhite,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )

        HorizontalDivider(color = MistColors.HairlineDivider)

        QuickActionRow(
          icon = Icons.Outlined.Lock,
          label = stringResource(R.string.action_lock_screen),
          testTag = "qa_lock_screen",
          onClick = onLockScreen
        )
        QuickActionRow(
          icon = Icons.Outlined.Apps,
          label = stringResource(R.string.action_app_library),
          testTag = "qa_app_library",
          onClick = onOpenLibrary
        )
        QuickActionRow(
          icon = Icons.Outlined.Search,
          label = stringResource(R.string.action_search_apps),
          testTag = "qa_search_apps",
          onClick = onOpenSearch
        )
        QuickActionRow(
          icon = Icons.Outlined.Image,
          label = stringResource(R.string.action_wallpaper_studio),
          testTag = "qa_wallpaper",
          onClick = onOpenWallpaper
        )
        QuickActionRow(
          icon = Icons.Outlined.StarOutline,
          label = stringResource(R.string.action_manage_favorites),
          testTag = "qa_favorites",
          onClick = onOpenFavorites
        )
        QuickActionRow(
          icon = Icons.Outlined.Tune,
          label = stringResource(R.string.action_launcher_settings),
          testTag = "qa_preferences",
          onClick = onOpenPreferences
        )

        HorizontalDivider(
          color = MistColors.HairlineDivider,
          modifier = Modifier.padding(vertical = 4.dp)
        )

        QuickActionRow(
          icon = Icons.Outlined.MusicNote,
          label = stringResource(R.string.media_listener_label),
          badge = if (isMediaListenerEnabled) "Active" else "Enable",
          testTag = "qa_media_listener",
          onClick = onOpenMediaListenerSettings
        )
        QuickActionRow(
          icon = Icons.Outlined.Home,
          label = stringResource(R.string.action_set_default_home),
          badge = if (isDefaultLauncher) "Active" else "Set",
          testTag = "qa_default_home",
          onClick = onRequestDefaultHome
        )
        QuickActionRow(
          icon = Icons.Outlined.Settings,
          label = stringResource(R.string.action_system_settings),
          testTag = "qa_device_settings",
          onClick = onOpenDeviceSettings
        )
      }
    }
  }
}

@Composable
fun LockPermissionDialog(
  onOpenAccessibility: () -> Unit,
  onOpenQuickActions: () -> Unit,
  onDismiss: () -> Unit
) {
  BackHandler(onBack = onDismiss)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0x7809090B))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismiss
      )
      .padding(24.dp)
      .testTag("lock_permission_dialog"),
    contentAlignment = Alignment.Center
  ) {
    GlassSurface(
      modifier = Modifier
        .widthIn(max = 360.dp)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {}
        ),
      shape = RoundedCornerShape(22.dp)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MistColors.PureWhite,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.lock_permission_title),
            fontFamily = EditorialDisplayFontFamily,
            fontSize = 20.sp,
            color = MistColors.OffWhite
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = stringResource(R.string.lock_permission_body),
          fontFamily = FunctionalSansFontFamily,
          fontSize = 13.5.sp,
          lineHeight = 19.sp,
          color = MistColors.MistGrey.copy(alpha = 0.88f)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = onOpenQuickActions,
            modifier = Modifier.testTag("lock_dialog_quick_actions_button")
          ) {
            Text(
              text = stringResource(R.string.quick_actions_title),
              fontFamily = FunctionalSansFontFamily,
              fontSize = 12.5.sp,
              color = MistColors.MistGrey
            )
          }
          TextButton(
            onClick = onOpenAccessibility,
            modifier = Modifier.testTag("lock_dialog_accessibility_button")
          ) {
            Text(
              text = stringResource(R.string.lock_permission_cta),
              fontFamily = FunctionalSansFontFamily,
              fontSize = 12.5.sp,
              color = MistColors.PureWhite
            )
          }
        }
      }
    }
  }
}

@Composable
private fun QuickActionRow(
  icon: ImageVector,
  label: String,
  testTag: String,
  onClick: () -> Unit,
  badge: String? = null
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Start,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = MistColors.PureWhite),
        onClick = onClick
      )
      .padding(horizontal = 18.dp, vertical = 10.dp)
      .testTag(testTag)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MistColors.OffWhite.copy(alpha = 0.85f),
      modifier = Modifier.size(19.dp)
    )
    Spacer(modifier = Modifier.width(14.dp))
    Text(
      text = label,
      fontFamily = FunctionalSansFontFamily,
      fontSize = 14.sp,
      color = MistColors.OffWhite,
      modifier = Modifier.weight(1f)
    )
    if (badge != null) {
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = badge,
        fontFamily = FunctionalSansFontFamily,
        fontSize = 11.sp,
        color = MistColors.MistGrey.copy(alpha = 0.78f)
      )
    }
  }
}
