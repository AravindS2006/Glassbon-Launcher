package com.example

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class LauncherUiRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  private fun registerApp(pkg: String, label: String) {
    val shadowPm = shadowOf(composeTestRule.activity.packageManager)
    val mainIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val cls = "$pkg.MainActivity"
    val appInfo = ApplicationInfo().apply {
      packageName = pkg
      nonLocalizedLabel = label
      flags = 0
    }
    val activityInfo = ActivityInfo().apply {
      packageName = pkg
      name = cls
      nonLocalizedLabel = label
      applicationInfo = appInfo
      exported = true
    }
    val resolveInfo = ResolveInfo().apply {
      this.activityInfo = activityInfo
      nonLocalizedLabel = label
    }
    shadowPm.addResolveInfoForIntent(mainIntent, resolveInfo)
    ComponentName(pkg, cls)
  }

  @Test
  fun `home screen absorbs system back without reload and links lock button to android lock app`() {
    val activity = composeTestRule.activity
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_clock_area").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_clock_text", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithTag("home_date_text", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithTag("home_alphabet_rail_container").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_quick_actions_button").assertIsDisplayed()

    // Pressing system Back while resting on Home must NOT finish/reload MainActivity
    composeTestRule.runOnUiThread {
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
    assertFalse("Back press on Home must not finish MainActivity", activity.isFinishing)
    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()

    // Register a normal app (not named Lock Screen) so unlinked Lock tap opens the picker dialog
    val customLockPkg = "com.example.lockscreenapp"
    registerApp(customLockPkg, "Touch Lock")
    activity.sendBroadcast(Intent(Intent.ACTION_PACKAGE_ADDED).apply {
      data = android.net.Uri.parse("package:$customLockPkg")
    })
    composeTestRule.waitForIdle()

    // Single tap on bottom-right Lock button opens Link Lock Screen App picker
    composeTestRule.onNodeWithTag("home_quick_actions_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lock_app_picker_dialog").assertIsDisplayed()

    // Picking an app from the dialog links it AND launches it immediately
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithTag("lock_picker_app_$customLockPkg", useUnmergedTree = true)
        .fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("lock_picker_app_$customLockPkg", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lock_app_picker_dialog").assertDoesNotExist()
    val firstLaunch = shadowOf(activity.application).nextStartedActivity
      ?: shadowOf(activity).nextStartedActivity
    assertEquals(customLockPkg, firstLaunch?.component?.packageName)

    // Subsequent tap on Lock button launches the linked app directly without showing the picker
    ShadowSystemClock.advanceBy(Duration.ofMillis(400))
    composeTestRule.onNodeWithTag("home_quick_actions_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lock_app_picker_dialog").assertDoesNotExist()
    val directLaunch = shadowOf(activity.application).nextStartedActivity
      ?: shadowOf(activity).nextStartedActivity
    assertEquals(customLockPkg, directLaunch?.component?.packageName)

    // Long press opens Quick Actions with Lock Screen row (showing linked badge) and NO OLED Calm Screen row
    composeTestRule.onNodeWithTag("home_quick_actions_button").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_actions_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("qa_lock_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("qa_calm_screen").assertDoesNotExist()
    composeTestRule.onNodeWithTag("qa_lock_settings").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    val startedIntent = shadowOf(activity).nextStartedActivity
    assertEquals(Settings.ACTION_SECURITY_SETTINGS, startedIntent?.action)
    composeTestRule.onNodeWithTag("quick_actions_overlay").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
  }

  @Test
  fun `open drawer search empty state and return home`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_drawer_handle").assertDoesNotExist()

    // Direct swipe up on Home screen must not open the drawer
    composeTestRule.onNodeWithTag("home_screen").performTouchInput {
      val centerX = width / 2f
      down(androidx.compose.ui.geometry.Offset(centerX, height * 0.7f))
      moveBy(androidx.compose.ui.geometry.Offset(0f, -height * 0.35f))
      up()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_drawer_screen").assertDoesNotExist()

    // Only the alphabet rail opens the drawer from Home
    composeTestRule.onNodeWithTag("alphabet_rail").performTouchInput {
      click(centerRight)
    }
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

    // Hidden left-edge alphabet rail opens drawer with zero duplicate right-side alphabet rails (<= 1, never 2!)
    composeTestRule.onNodeWithTag("home_left_alphabet_rail_container").assertIsDisplayed()
    composeTestRule.onNodeWithTag("left_alphabet_rail").performTouchInput {
      down(centerLeft)
      moveBy(androidx.compose.ui.geometry.Offset(40f, 60f))
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_drawer_screen").assertIsDisplayed()
    org.junit.Assert.assertTrue(
      composeTestRule.onAllNodesWithTag("alphabet_rail").fetchSemanticsNodes().size <= 1
    )
    composeTestRule.onNodeWithTag("left_alphabet_rail").performTouchInput {
      up()
    }
    composeTestRule.waitForIdle()
    org.junit.Assert.assertTrue(
      composeTestRule.onAllNodesWithTag("alphabet_rail").fetchSemanticsNodes().size <= 1
    )
    composeTestRule.onNodeWithTag("drawer_app_list").performTouchInput {
      swipeDown()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_drawer_screen").assertDoesNotExist()
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

    // Defaults to Ultra-Smooth and can switch to Low Power and back
    composeTestRule.onNodeWithTag("perf_mode_ultra_smooth").assertIsDisplayed()
    composeTestRule.onNodeWithTag("perf_mode_power_saver").assertIsDisplayed().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithText("Low Power active", substring = true)
        .fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Low Power active", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("perf_mode_ultra_smooth").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithText("Ultra-Smooth active", substring = true)
        .fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Ultra-Smooth active", substring = true).assertIsDisplayed()

    // Alphabet Focus Blur, Haptic Feel presets & GitHub-tag BuildConfig version are accessible in Preferences
    composeTestRule.onNodeWithTag("focus_blur_deep").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("haptic_profile_crisp").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_lock_app_row").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_version_info").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Glassbon v${BuildConfig.VERSION_NAME}", substring = true).assertIsDisplayed()

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

  @Test
  fun `interactive walkthrough tour launches steps navigates and replays from quick actions`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_start_tour_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("walkthrough_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("walkthrough_step_counter", useUnmergedTree = true).assertIsDisplayed()

    // Step forward through all 6 steps and finish
    repeat(5) {
      composeTestRule.onNodeWithTag("walkthrough_next_button").performClick()
      composeTestRule.waitForIdle()
    }
    composeTestRule.onNodeWithTag("walkthrough_back_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("walkthrough_done_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("walkthrough_overlay").assertDoesNotExist()
    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()

    // Replay tour anytime from Quick Actions -> Guided Feature Tour
    composeTestRule.onNodeWithTag("home_quick_actions_button").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("qa_walkthrough").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("walkthrough_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("walkthrough_skip_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("walkthrough_overlay").assertDoesNotExist()
  }
}
