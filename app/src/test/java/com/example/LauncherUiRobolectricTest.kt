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
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class LauncherUiRobolectricTest {

  private val composeTestRule = createAndroidComposeRule<MainActivity>()

  private val resetPreferencesRule = object : ExternalResource() {
    override fun before() {
      runBlocking {
        com.example.data.preferences.PreferencesRepository(
          androidx.test.core.app.ApplicationProvider.getApplicationContext()
        ).resetAllForTest()
      }
    }
  }

  @get:Rule
  val ruleChain: RuleChain = RuleChain.outerRule(resetPreferencesRule).around(composeTestRule)

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

    // When no Lock Screen app is installed on device, bottom-right button shows Preferences fallback and opens Preferences on tap
    composeTestRule.onNodeWithContentDescription("Open Glassbon preferences or hold for quick actions")
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_quick_actions_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()

    // Also verify picker dialog opens cleanly from Preferences -> Linked Lock Screen App -> Unlink/Cancel
    composeTestRule.onNodeWithTag("settings_lock_app_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lock_app_picker_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("lock_dialog_cancel_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()

    // Install a Lock Screen utility -> Glassbon auto-detects & links it automatically, switching icon to Power
    val customLockPkg = "com.example.lockscreenapp"
    registerApp(customLockPkg, "Touch Lock")
    activity.sendBroadcast(Intent(Intent.ACTION_PACKAGE_ADDED).apply {
      data = android.net.Uri.parse("package:$customLockPkg")
    })
    composeTestRule.waitForIdle()
    val viewModel = androidx.lifecycle.ViewModelProvider(activity)[com.example.ui.LauncherViewModel::class.java]
    composeTestRule.waitUntil(timeoutMillis = 10_000L) {
      Thread.sleep(25)
      composeTestRule.onAllNodesWithContentDescription("Lock screen or hold for quick actions")
        .fetchSemanticsNodes().isNotEmpty() &&
        viewModel.uiState.value.linkedLockApp?.packageName == customLockPkg
    }
    composeTestRule.waitForIdle()

    // Bottom-right button is now the Power button and tapping it directly launches the linked Lock Screen app
    composeTestRule.onNodeWithContentDescription("Lock screen or hold for quick actions")
      .assertIsDisplayed()
    ShadowSystemClock.advanceBy(Duration.ofMillis(400))
    composeTestRule.onNodeWithTag("home_quick_actions_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lock_app_picker_dialog").assertDoesNotExist()
    val directLaunch = shadowOf(activity).nextStartedActivity
      ?: shadowOf(activity.application).nextStartedActivity
    assertEquals(customLockPkg, directLaunch?.component?.packageName)

    // Manual unlink via Preferences -> Linked Lock Screen App falls back to Preferences icon cleanly
    composeTestRule.onNodeWithTag("home_quick_actions_button").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_actions_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("qa_lock_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("qa_calm_screen").assertDoesNotExist()
    composeTestRule.onNodeWithTag("qa_preferences").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_lock_app_row").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lock_clear_link_button").assertIsDisplayed().performClick()
    composeTestRule.waitUntil(timeoutMillis = 10_000L) {
      Thread.sleep(25)
      composeTestRule.onAllNodesWithTag("lock_app_picker_dialog").fetchSemanticsNodes().isEmpty() &&
        viewModel.uiState.value.linkedLockApp == null
    }
    assertNull(viewModel.uiState.value.linkedLockApp)
    composeTestRule.onNodeWithTag("settings_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription("Open Glassbon preferences or hold for quick actions")
      .assertIsDisplayed()

    // Quick Actions omits Screen Lock Settings cleanly and still provides Default Home switcher
    composeTestRule.onNodeWithTag("home_quick_actions_button").performTouchInput { longClick() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_actions_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("qa_lock_settings").assertDoesNotExist()
    composeTestRule.onNodeWithTag("qa_default_home").assertIsDisplayed()
    composeTestRule.runOnUiThread {
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
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
    composeTestRule.waitUntil(timeoutMillis = 10_000L) {
      Thread.sleep(20)
      composeTestRule.onAllNodesWithText("Low Power active", substring = true)
        .fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Low Power active", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("perf_mode_ultra_smooth").performClick()
    composeTestRule.waitUntil(timeoutMillis = 10_000L) {
      Thread.sleep(20)
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

    // Step forward through all 5 steps (music widget temporarily removed from tour) and finish
    repeat(4) {
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

  @Test
  fun `skipping tour surfaces hold for preferences hint pill when power icon is active and dismisses on open`() {
    val activity = composeTestRule.activity
    val lockPkg = "com.example.lockscreenapp"
    registerApp(lockPkg, "Lock Screen")
    activity.sendBroadcast(Intent(Intent.ACTION_PACKAGE_ADDED).apply {
      data = android.net.Uri.parse("package:$lockPkg")
    })
    composeTestRule.waitForIdle()
    val viewModel = androidx.lifecycle.ViewModelProvider(activity)[com.example.ui.LauncherViewModel::class.java]
    composeTestRule.waitUntil(timeoutMillis = 10_000L) {
      Thread.sleep(25)
      composeTestRule.onAllNodesWithContentDescription("Lock screen or hold for quick actions")
        .fetchSemanticsNodes().isNotEmpty() &&
        viewModel.uiState.value.linkedLockApp?.packageName == lockPkg
    }
    composeTestRule.waitForIdle()

    // User opens tour and immediately hits Skip
    composeTestRule.onNodeWithTag("home_start_tour_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("walkthrough_skip_button").performClick()
    composeTestRule.waitForIdle()
    // Skipping persists walkthroughCompleted asynchronously (DataStore on IO); wait for it and for the
    // walkthrough overlay to finish fading out so it can't swallow the next tap or keep the onboarding card up.
    composeTestRule.waitUntil(timeoutMillis = 10_000L) {
      Thread.sleep(20)
      viewModel.uiState.value.preferences.walkthroughCompleted &&
        composeTestRule.onAllNodesWithTag("walkthrough_overlay").fetchSemanticsNodes().isEmpty()
    }
    composeTestRule.waitForIdle()
    // Dismiss onboarding card if still visible (e.g. when device has another default Home app) and wait until
    // the dismissal is persisted, since the hint pill only appears once the onboarding card is gone.
    if (composeTestRule.onAllNodesWithTag("onboarding_dismiss_button").fetchSemanticsNodes().isNotEmpty()) {
      composeTestRule.onNodeWithTag("onboarding_dismiss_button").performClick()
      composeTestRule.waitUntil(timeoutMillis = 10_000L) {
        Thread.sleep(20)
        viewModel.uiState.value.preferences.onboardingDismissed
      }
      composeTestRule.waitForIdle()
    }

    // Subtle Hold ⏻ for Quick Actions hint appears cleanly above the Power icon
    try {
      composeTestRule.waitUntil(timeoutMillis = 10_000L) {
        Thread.sleep(20)
        composeTestRule.onAllNodesWithTag("power_hold_hint_pill").fetchSemanticsNodes().isNotEmpty()
      }
    } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
      // Surface the exact gating state so any future flake is diagnosable from CI logs alone
      val s = viewModel.uiState.value
      val p = s.preferences
      throw AssertionError(
        "power_hold_hint_pill never appeared: isDefaultLauncher=${s.isDefaultLauncher}, " +
          "onboardingDismissed=${p.onboardingDismissed}, walkthroughCompleted=${p.walkthroughCompleted}, " +
          "quickActionsDiscovered=${p.quickActionsDiscovered}, quickActionMode=${p.quickActionMode}, " +
          "isDrawerOpen=${s.isDrawerOpen}, activeOverlay=${s.activeOverlay}, showWalkthrough=${s.showWalkthrough}",
        e
      )
    }
    composeTestRule.onNodeWithTag("power_hold_hint_pill").assertIsDisplayed()
    composeTestRule.onNodeWithText("Hold ⏻ for Quick Actions").assertIsDisplayed()

    // Tapping the hint action opens Quick Actions and permanently marks Quick Actions as discovered
    composeTestRule.onNodeWithTag("power_hold_hint_action").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_actions_overlay").assertIsDisplayed()
    composeTestRule.waitUntil(timeoutMillis = 10_000L) {
      Thread.sleep(25)
      viewModel.uiState.value.preferences.quickActionsDiscovered
    }
    composeTestRule.runOnUiThread {
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("power_hold_hint_pill").assertDoesNotExist()
  }
}
