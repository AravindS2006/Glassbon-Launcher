package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class DrawerAlphabetJumpRobolectricTest {

  @get:Rule
  val composeTestRule = createEmptyComposeRule()

  private fun registerSyntheticApps(context: Context) {
    val pm = context.packageManager
    val shadowPm = shadowOf(pm)
    val mainIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val appNames = listOf(
      "Alpha", "Amber",
      "Brave", "Bridge",
      "Calendar", "Camera",
      "Drive", "Docs",
      "Earth", "Echo",
      "Files", "Focus",
      "Gallery", "Gmail", "GlassNotes",
      "Health", "Home",
      "Inbox", "Index",
      "Journal", "Jotter",
      "Keep", "Kino",
      "Lens", "Loop",
      "Maps", "Messages",
      "Notes", "News",
      "Orbit", "Origin",
      "Photos", "Podcasts",
      "Quest", "Quick",
      "Reader", "Recorder",
      "Spotify", "Settings",
      "Terminal", "Tasks",
      "Utility", "Vault",
      "Wallet", "Weather",
      "Xplore", "Yoga",
      "Zeta", "Zoom"
    )

    for (name in appNames) {
      val pkg = "com.test.${name.lowercase()}"
      val cls = "$pkg.MainActivity"
      val appInfo = ApplicationInfo().apply {
        packageName = pkg
        nonLocalizedLabel = name
        flags = 0
      }
      val activityInfo = ActivityInfo().apply {
        packageName = pkg
        this.name = cls
        nonLocalizedLabel = name
        applicationInfo = appInfo
        exported = true
      }
      val resolveInfo = ResolveInfo().apply {
        this.activityInfo = activityInfo
        nonLocalizedLabel = name
      }
      shadowPm.addResolveInfoForIntent(mainIntent, resolveInfo)
      // Ensure ComponentName lookup stays consistent
      ComponentName(pkg, cls)
    }
  }

  @Test
  fun `tapping G on alphabet rail opens drawer directly to G section without missing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    registerSyntheticApps(context)

    ActivityScenario.launch(MainActivity::class.java).use {
      composeTestRule.waitUntil(timeoutMillis = 5_000L) {
        composeTestRule.onAllNodesWithTag("alphabet_rail_key_G", useUnmergedTree = true)
          .fetchSemanticsNodes().isNotEmpty()
      }

      // Click the exact rendered letter 'G' on Home's right alphabet rail
      composeTestRule.onNodeWithTag("alphabet_rail_key_G", useUnmergedTree = true).performClick()

      composeTestRule.waitUntil(timeoutMillis = 5_000L) {
        composeTestRule.onAllNodesWithTag("drawer_section_G", useUnmergedTree = true)
          .fetchSemanticsNodes().isNotEmpty()
      }

      composeTestRule.onNodeWithTag("app_drawer_screen").assertIsDisplayed()
      composeTestRule.onNodeWithTag("drawer_section_G", useUnmergedTree = true).assertIsDisplayed()
      composeTestRule.onNodeWithTag("drawer_app_row_com.test.gallery", useUnmergedTree = true).assertIsDisplayed()
      composeTestRule.onNodeWithTag("drawer_app_row_com.test.gmail", useUnmergedTree = true).assertIsDisplayed()

      // Jump to Z inside the open drawer, then click G again to verify in-drawer rail alignment
      composeTestRule.onNodeWithTag("alphabet_rail_key_Z", useUnmergedTree = true).performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000L) {
        composeTestRule.onAllNodesWithTag("drawer_section_Z", useUnmergedTree = true)
          .fetchSemanticsNodes().isNotEmpty()
      }
      composeTestRule.onNodeWithTag("drawer_section_Z", useUnmergedTree = true).assertIsDisplayed()

      composeTestRule.onNodeWithTag("alphabet_rail_key_G", useUnmergedTree = true).performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000L) {
        composeTestRule.onAllNodesWithTag("drawer_section_G", useUnmergedTree = true)
          .fetchSemanticsNodes().isNotEmpty()
      }
      composeTestRule.onNodeWithTag("drawer_section_G", useUnmergedTree = true).assertIsDisplayed()
    }
  }
}
