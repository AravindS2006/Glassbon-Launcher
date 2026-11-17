package com.example.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.graphics.AppIconCache
import com.example.data.apps.AppInfo
import com.example.data.preferences.TypographyStyle
import com.example.ui.LauncherUiState
import com.example.ui.components.AlphabetRail
import com.example.ui.components.FAVORITES_INDEX_KEY
import com.example.ui.components.FloatingCircleButton
import com.example.ui.components.GlassSurface
import com.example.ui.components.MistAppIcon
import com.example.ui.theme.EditorialDisplayFontFamily
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.FunctionalSansFontFamily
import com.example.ui.theme.MistColors
import com.example.ui.theme.appLabelTextStyle
import kotlinx.coroutines.launch

@Composable
fun AppDrawerScreen(
  uiState: LauncherUiState,
  iconCache: AppIconCache,
  hideAlphabetRail: Boolean = false,
  onJumpTargetHandled: () -> Unit = {},
  onLaunchApp: (AppInfo) -> Unit,
  onLongPressApp: (AppInfo) -> Unit,
  onSearchActiveChange: (Boolean) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onCloseDrawer: () -> Unit,
  modifier: Modifier = Modifier
) {
  val prefs = uiState.preferences
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current

  // Niagara-style lightweight horizontal glide when scrubbing sections (single GPU transform on list)
  var isRailDragging by remember { mutableStateOf(false) }
  val listSlideOffsetX = remember { Animatable(0f) }
  val listDragShiftX = remember { Animatable(0f) }
  val scrubNudgePx = with(density) { 14.dp.toPx() }
  val dragPullPx = with(density) { 10.dp.toPx() }

  LaunchedEffect(isRailDragging || hideAlphabetRail, prefs.reducedMotion) {
    val targetShift = if ((isRailDragging || hideAlphabetRail) && !prefs.reducedMotion) -dragPullPx else 0f
    if (prefs.reducedMotion) {
      listDragShiftX.snapTo(0f)
    } else {
      listDragShiftX.animateTo(
        targetValue = targetShift,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
      )
    }
  }

  fun triggerSectionSlide() {
    if (prefs.reducedMotion) return
    coroutineScope.launch {
      listSlideOffsetX.snapTo(scrubNudgePx)
      listSlideOffsetX.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
      )
    }
  }

  BackHandler(enabled = uiState.isDrawerOpen) {
    if (uiState.isSearchActive) {
      keyboardController?.hide()
      focusManager.clearFocus()
      onSearchActiveChange(false)
    } else {
      onCloseDrawer()
    }
  }

  val sectionIndexMap = remember(uiState.sections, uiState.favoriteApps) {
    val map = LinkedHashMap<String, Int>()
    var cursor = 0
    if (uiState.favoriteApps.isNotEmpty()) {
      map[FAVORITES_INDEX_KEY] = 0
      cursor += 1 + uiState.favoriteApps.size
    }
    for (section in uiState.sections) {
      map[section.header] = cursor
      cursor += 1 + section.apps.size
    }
    map
  }

  // Scroll immediately when jumped to a section from Home screen's Alphabet Rail
  LaunchedEffect(uiState.drawerJumpTarget, sectionIndexMap) {
    val target = uiState.drawerJumpTarget ?: return@LaunchedEffect
    val index = findNearestSectionIndex(target, sectionIndexMap)
    if (index != null) {
      listState.scrollToItem(index)
      triggerSectionSlide()
    }
    if (!hideAlphabetRail) {
      onJumpTargetHandled()
    }
  }

  val activeIndexKey by remember(sectionIndexMap) {
    derivedStateOf {
      val firstVisible = listState.firstVisibleItemIndex
      var current: String? = null
      for ((key, startIndex) in sectionIndexMap) {
        if (startIndex <= firstVisible) {
          current = key
        } else {
          break
        }
      }
      current ?: sectionIndexMap.keys.firstOrNull()
    }
  }

  val availableSections = remember(uiState.sections) {
    uiState.sections.map { it.header }.toSet()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("app_drawer_screen")
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .imePadding()
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      AnimatedContent(
        targetState = uiState.isSearchActive,
        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(120)) },
        label = "drawer_header_transition"
      ) { searching ->
        if (searching) {
          DrawerSearchHeader(
            query = uiState.searchQuery,
            autoFocus = prefs.searchAutoFocusKeyboard,
            onQueryChange = onSearchQueryChange,
            onClear = { onSearchQueryChange("") },
            onClose = {
              keyboardController?.hide()
              focusManager.clearFocus()
              onSearchActiveChange(false)
            },
            onSubmitFirst = {
              uiState.searchResults.firstOrNull()?.let { firstApp ->
                keyboardController?.hide()
                onLaunchApp(firstApp)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
          )
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 6.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(width = 88.dp, height = 32.dp)
                .clip(CircleShape)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                  role = Role.Button,
                  onClick = onCloseDrawer
                )
                .testTag("drawer_close_handle"),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(width = 36.dp, height = 3.dp)
                  .clip(CircleShape)
                  .background(Color(0x55FFFFFF))
              )
            }
          }
        }
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        if (uiState.isSearchActive) {
          SearchResultsList(
            results = uiState.searchResults,
            query = uiState.searchQuery,
            uiState = uiState,
            iconCache = iconCache,
            onLaunchApp = { app ->
              keyboardController?.hide()
              onLaunchApp(app)
            },
            onLongPressApp = onLongPressApp
          )
        } else {
          // Hardware-accelerated LazyColumn (no per-row animateItem overhead) with Niagara-style list slide
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .graphicsLayer {
                translationX = listSlideOffsetX.value + listDragShiftX.value
              }
              .testTag("drawer_app_list"),
            contentPadding = PaddingValues(
              start = 24.dp,
              end = if (prefs.showAlphabetIndex) 56.dp else 24.dp,
              top = 8.dp,
              bottom = 96.dp
            )
          ) {
            if (uiState.favoriteApps.isNotEmpty()) {
              item(key = "header_favorites", contentType = "header") {
                DrawerSectionHeader(
                  title = stringResource(R.string.favorites_section_title),
                  modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                )
              }
              items(
                items = uiState.favoriteApps,
                key = { "fav_${it.componentKey}" },
                contentType = { "app_row" }
              ) { app ->
                DrawerAppRow(
                  app = app,
                  uiState = uiState,
                  iconCache = iconCache,
                  onClick = { onLaunchApp(app) },
                  onLongClick = { onLongPressApp(app) }
                )
              }
            }

            for (section in uiState.sections) {
              item(key = "section_${section.header}", contentType = "header") {
                DrawerSectionHeader(
                  title = section.header,
                  modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)
                )
              }
              items(
                items = section.apps,
                key = { "${section.header}_${it.componentKey}" },
                contentType = { "app_row" }
              ) { app ->
                DrawerAppRow(
                  app = app,
                  uiState = uiState,
                  iconCache = iconCache,
                  onClick = { onLaunchApp(app) },
                  onLongClick = { onLongPressApp(app) }
                )
              }
            }
          }

          // Right-edge curved vertical alphabet index
          if (prefs.showAlphabetIndex && uiState.sections.isNotEmpty() && !hideAlphabetRail) {
            Box(
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(end = 6.dp, bottom = 68.dp),
              contentAlignment = Alignment.Center
            ) {
              AlphabetRail(
                availableSections = availableSections,
                activeKey = activeIndexKey,
                hasFavorites = uiState.favoriteApps.isNotEmpty(),
                hapticEnabled = prefs.hapticFeedback,
                reducedMotion = prefs.reducedMotion,
                onDraggingChanged = { isRailDragging = it },
                onSelectKey = { targetKey ->
                  val targetIndex = findNearestSectionIndex(targetKey, sectionIndexMap)
                  if (targetIndex != null) {
                    coroutineScope.launch {
                      listState.scrollToItem(targetIndex)
                      triggerSectionSlide()
                    }
                  }
                }
              )
            }
          }
        }
      }
    }

    if (!uiState.isSearchActive) {
      FloatingCircleButton(
        icon = Icons.Rounded.Search,
        contentDescription = stringResource(R.string.cd_open_search),
        testTag = "drawer_search_button",
        onClick = { onSearchActiveChange(true) },
        hapticFeedback = prefs.hapticFeedback,
        reducedMotion = prefs.reducedMotion,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 24.dp, bottom = 20.dp)
      )
    }
  }
}

private fun findNearestSectionIndex(
  targetKey: String,
  sectionIndexMap: Map<String, Int>
): Int? {
  sectionIndexMap[targetKey]?.let { return it }
  if (targetKey == FAVORITES_INDEX_KEY) {
    return sectionIndexMap.values.firstOrNull()
  }
  val keys = sectionIndexMap.keys.filter { it != FAVORITES_INDEX_KEY }
  val exactOrNext = keys.firstOrNull { it >= targetKey && it != "#" }
    ?: keys.lastOrNull()
  return exactOrNext?.let { sectionIndexMap[it] }
}

@Composable
private fun DrawerSectionHeader(
  title: String,
  modifier: Modifier = Modifier
) {
  Text(
    text = title,
    fontFamily = EditorialDisplayFontFamily,
    fontSize = 19.sp,
    letterSpacing = 0.6.sp,
    color = MistColors.OffWhite.copy(alpha = 0.72f),
    modifier = modifier.padding(start = 4.dp)
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppRow(
  app: AppInfo,
  uiState: LauncherUiState,
  iconCache: AppIconCache,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val prefs = uiState.preferences
  val haptics = LocalHapticFeedback.current
  val interactionSource = remember(app.componentKey) { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val isItalic = prefs.typographyStyle == TypographyStyle.SERIF_ITALIC
  val useSans = prefs.typographyStyle == TypographyStyle.CLEAN_SANS

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(12.dp))
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = MistColors.PureWhite),
        onClick = onClick,
        onLongClick = {
          if (prefs.hapticFeedback) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          }
          onLongClick()
        }
      )
      .padding(horizontal = 4.dp, vertical = 7.dp)
      .testTag("drawer_app_row_${app.packageName}")
  ) {
    MistAppIcon(
      app = app,
      iconCache = iconCache,
      iconStyle = prefs.iconStyle,
      tileSize = prefs.iconSize.drawerTileDp.dp,
      isPressed = isPressed,
      reducedMotion = prefs.reducedMotion
    )

    Spacer(modifier = Modifier.width(16.dp))

    Text(
      text = app.label,
      style = appLabelTextStyle(isItalic = isItalic, useSans = useSans),
      color = MistColors.OffWhite.copy(alpha = 0.94f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun DrawerSearchHeader(
  query: String,
  autoFocus: Boolean,
  onQueryChange: (String) -> Unit,
  onClear: () -> Unit,
  onClose: () -> Unit,
  onSubmitFirst: () -> Unit,
  modifier: Modifier = Modifier
) {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(autoFocus) {
    if (autoFocus) {
      focusRequester.requestFocus()
    }
  }

  GlassSurface(
    modifier = modifier.widthIn(max = 600.dp),
    shape = RoundedCornerShape(18.dp),
    backgroundColor = Color(0xCC171719)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .padding(horizontal = 6.dp)
    ) {
      IconButton(
        onClick = onClose,
        modifier = Modifier.testTag("search_close_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.cd_close_search),
          tint = MistColors.OffWhite
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        if (query.isEmpty()) {
          Text(
            text = stringResource(R.string.search_placeholder),
            fontFamily = FunctionalSansFontFamily,
            fontSize = 15.sp,
            color = MistColors.FrostedGrey
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = TextStyle(
            fontFamily = FunctionalSansFontFamily,
            fontSize = 15.sp,
            color = MistColors.OffWhite
          ),
          cursorBrush = SolidColor(MistColors.PureWhite),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
          keyboardActions = KeyboardActions(onGo = { onSubmitFirst() }),
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("drawer_search_input")
        )
      }

      if (query.isNotEmpty()) {
        IconButton(
          onClick = onClear,
          modifier = Modifier.testTag("search_clear_button")
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.cd_clear_search),
            tint = MistColors.MistGrey
          )
        }
      }
    }
  }
}

@Composable
private fun SearchResultsList(
  results: List<AppInfo>,
  query: String,
  uiState: LauncherUiState,
  iconCache: AppIconCache,
  onLaunchApp: (AppInfo) -> Unit,
  onLongPressApp: (AppInfo) -> Unit
) {
  if (results.isEmpty()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 32.dp)
        .testTag("search_empty_state"),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = null,
        tint = MistColors.OffWhite.copy(alpha = 0.45f),
        modifier = Modifier.size(34.dp)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = stringResource(R.string.search_empty_title),
        fontFamily = EditorialSerifFontFamily,
        fontSize = 20.sp,
        color = MistColors.OffWhite.copy(alpha = 0.85f)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = if (query.isNotBlank()) {
          "\"$query\" did not match any installed app."
        } else {
          stringResource(R.string.search_empty_subtitle)
        },
        fontFamily = FunctionalSansFontFamily,
        fontSize = 13.sp,
        color = MistColors.MistGrey.copy(alpha = 0.7f)
      )
    }
  } else {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("search_results_list"),
      contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
      items(
        items = results,
        key = { "search_${it.componentKey}" },
        contentType = { "app_row" }
      ) { app ->
        DrawerAppRow(
          app = app,
          uiState = uiState,
          iconCache = iconCache,
          onClick = { onLaunchApp(app) },
          onLongClick = { onLongPressApp(app) }
        )
      }
    }
  }
}
