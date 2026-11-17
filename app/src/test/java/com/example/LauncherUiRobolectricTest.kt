package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class LauncherUiRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `home screen renders clock date alphabet rail and power lock dialog on tap`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_clock_area").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_clock_text", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithTag("home_date_text", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithTag("home_alphabet_rail_container").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_quick_actions_button").assertIsDisplayed()

    // Single tap on power button prompts to enable Accessibility Screen Lock when not active
    composeTestRule.onNodeWithTag("home_quick_actions_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lock_permission_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lock_dialog_quick_actions_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_actions_overlay").assertIsDisplayed()
  }

  @Test
  fun `open drawer search empty state and return home`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_drawer_handle").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("app_drawer_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("drawer_search_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("drawer_search_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("drawer_search_input").performTextInput("zzzz_no_match")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("search_empty_state").assertIsDisplayed()
    composeTestRule.onNodeWithTag("search_clear_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("search_close_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("drawer_close_handle").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
  }

  @Test
  fun `long press power opens quick actions preferences wallpaper studio and favorites manager`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_quick_actions_button").performTouchInput { longClick() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("qa_preferences").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("home_quick_actions_button").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("qa_wallpaper").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("wallpaper_studio_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("wallpaper_back_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("home_quick_actions_button").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("qa_favorites").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("favorites_manager_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("favorites_back_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
  }
}
