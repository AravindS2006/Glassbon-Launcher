package com.example.ui.overlays

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.graphics.AppIconCache
import com.example.core.haptics.LocalMistHaptics
import com.example.core.haptics.MistHapticEvent
import com.example.data.apps.AppInfo
import com.example.data.preferences.IconStyle
import com.example.ui.components.MistAppIcon
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors

@Composable
fun AppContextMenuDialog(
  app: AppInfo,
  isFavorite: Boolean,
  favoriteIndex: Int,
  favoriteCount: Int,
  isHidden: Boolean,
  iconCache: AppIconCache,
  iconStyle: IconStyle,
  reducedMotion: Boolean = false,
  lowPowerMode: Boolean = false,
  onToggleFavorite: () -> Unit,
  onMoveFavoriteEarlier: () -> Unit,
  onMoveFavoriteLater: () -> Unit,
  onOpenAppInfo: () -> Unit,
  onToggleHide: () -> Unit,
  onUninstall: () -> Unit,
  onDismiss: () -> Unit
) {
  BackHandler(onBack = onDismiss)

  // Finite spring reveal evaluated strictly on GPU graphicsLayer (zero ongoing idle cost)
  val reveal = remember(app.componentKey) { Animatable(if (reducedMotion) 1f else 0f) }
  LaunchedEffect(app.componentKey, reducedMotion) {
    if (reducedMotion) {
      reveal.snapTo(1f)
    } else {
      reveal.animateTo(
        targetValue = 1f,
        animationSpec = spring(
          dampingRatio = 0.78f,
          stiffness = Spring.StiffnessMedium
        )
      )
    }
  }

  val cardShape = remember { RoundedCornerShape(24.dp) }
  val statusLabel = when {
    isHidden -> "Hidden"
    isFavorite && favoriteIndex >= 0 -> "Home #${favoriteIndex + 1}"
    else -> "App"
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0x770B0B0D))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismiss
      )
      .padding(24.dp)
      .testTag("app_context_menu"),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 336.dp)
        .graphicsLayer {
          val p = reveal.value
          alpha = p.coerceIn(0f, 1f)
          val s = 0.92f + 0.08f * p
          scaleX = s
          scaleY = s
        }
        .clip(cardShape)
        .background(POPUP_FILL_BRUSH)
        .border(0.85.dp, POPUP_RIM_BRUSH, cardShape)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {}
        )
    ) {
      Column(modifier = Modifier.padding(vertical = 12.dp)) {
        // Frosted Pop-Up Preview Header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
          MistAppIcon(
            app = app,
            iconCache = iconCache,
            iconStyle = iconStyle,
            tileSize = 44.dp,
            reducedMotion = reducedMotion,
            lowPowerMode = lowPowerMode
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = app.label,
                fontFamily = FunctionalSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MistColors.OffWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0x22FFFFFF))
                  .border(0.6.dp, Color(0x36FFFFFF), RoundedCornerShape(8.dp))
                  .padding(horizontal = 7.dp, vertical = 2.dp)
              ) {
                Text(
                  text = statusLabel,
                  fontFamily = FunctionalSansFontFamily,
                  fontWeight = FontWeight.Medium,
                  fontSize = 10.sp,
                  maxLines = 1,
                  softWrap = false,
                  color = MistColors.OffWhite.copy(alpha = 0.88f)
                )
              }
            }
            Text(
              text = app.packageName,
              fontFamily = FunctionalSansFontFamily,
              fontSize = 11.sp,
              color = MistColors.FrostedGrey,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        HorizontalDivider(color = MistColors.HairlineDivider)

        ContextActionRow(
          icon = if (isFavorite) Icons.Rounded.Star else Icons.Outlined.StarOutline,
          label = stringResource(
            if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite
          ),
          testTag = "ctx_toggle_favorite",
          onClick = onToggleFavorite
        )

        if (isFavorite && favoriteIndex > 0) {
          ContextActionRow(
            icon = Icons.Outlined.ArrowUpward,
            label = stringResource(R.string.action_move_up),
            testTag = "ctx_move_up",
            onClick = onMoveFavoriteEarlier
          )
        }

        if (isFavorite && favoriteIndex in 0 until favoriteCount - 1) {
          ContextActionRow(
            icon = Icons.Outlined.ArrowDownward,
            label = stringResource(R.string.action_move_down),
            testTag = "ctx_move_down",
            onClick = onMoveFavoriteLater
          )
        }

        ContextActionRow(
          icon = Icons.Outlined.Info,
          label = stringResource(R.string.action_app_info),
          testTag = "ctx_app_info",
          onClick = onOpenAppInfo
        )

        ContextActionRow(
          icon = Icons.Outlined.VisibilityOff,
          label = stringResource(
            if (isHidden) R.string.action_unhide_app else R.string.action_hide_app
          ),
          testTag = "ctx_toggle_hide",
          onClick = onToggleHide
        )

        if (!app.isSystemApp) {
          HorizontalDivider(
            color = MistColors.HairlineDivider,
            modifier = Modifier.padding(vertical = 4.dp)
          )
          ContextActionRow(
            icon = Icons.Outlined.DeleteOutline,
            label = stringResource(R.string.action_uninstall_app),
            testTag = "ctx_uninstall",
            onClick = onUninstall
          )
        }
      }
    }
  }
}

@Composable
private fun ContextActionRow(
  icon: ImageVector,
  label: String,
  testTag: String,
  onClick: () -> Unit
) {
  val haptics = LocalMistHaptics.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(bounded = true, color = MistColors.PureWhite),
        onClick = {
          haptics.trigger(MistHapticEvent.TAP_LIGHT)
          onClick()
        }
      )
      .padding(horizontal = 18.dp, vertical = 10.dp)
      .testTag(testTag)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MistColors.OffWhite.copy(alpha = 0.88f),
      modifier = Modifier.size(19.dp)
    )
    Spacer(modifier = Modifier.width(14.dp))
    Text(
      text = label,
      fontFamily = FunctionalSansFontFamily,
      fontSize = 14.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      color = MistColors.OffWhite,
      modifier = Modifier.weight(1f)
    )
  }
}

private val POPUP_FILL_BRUSH = Brush.verticalGradient(
  colors = listOf(Color(0xEB1B1C20), Color(0xEC111215))
)

private val POPUP_RIM_BRUSH = Brush.linearGradient(
  colors = listOf(MistColors.SpecularRimTop, MistColors.SpecularRimMid, MistColors.SpecularRimBottom)
)
