package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.ui.drawer.DrawerFocusAlphaKey
import com.example.ui.drawer.DrawerFocusBlurRadiusKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class DrawerAlphabetJumpRobolectricTest {

  @get:Rule
  val composeTestRule = createEmptyComposeRule()

  private fun assertSectionCenteredAndFullyVisible(
    headerTag: String,
    firstAppTag: String,
    lastAppTag: String
  ) {
    val listBounds = composeTestRule.onNodeWithTag("drawer_app_list").fetchSemanticsNode().boundsInRoot
    val headerBounds = composeTestRule.onNodeWithTag(headerTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
    val lastBounds = composeTestRule.onNodeWithTag(lastAppTag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    composeTestRule.onNodeWithTag(headerTag, useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag(firstAppTag, useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag(lastAppTag, useUnmergedTree = true).assertIsDisplayed()

    val listCenterY = (listBounds.top + listBounds.bottom) / 2f
    val sectionCenterY = (headerBounds.top + lastBounds.bottom) / 2f
    assertTrue(
      "Expected $headerTag top (${headerBounds.top}) to sit well below list top (${listBounds.top}) for centering",
      headerBounds.top > listBounds.top + 80f
    )
    assertTrue(
      "Expected $headerTag block center ($sectionCenterY) to align near drawer viewport center ($listCenterY)",
      abs(sectionCenterY - listCenterY) < listBounds.height * 0.18f
    )
  }

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
        composeTestRule.onAllNodesWithTag("drawer_app_row_com.test.glassnotes", useUnmergedTree = true)
          .fetchSemanticsNodes().isNotEmpty()
      }
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag("app_drawer_screen").assertIsDisplayed()
      composeTestRule.onNodeWithTag("drawer_app_row_com.test.gmail", useUnmergedTree = true).assertIsDisplayed()
      assertSectionCenteredAndFullyVisible(
        headerTag = "drawer_section_G",
        firstAppTag = "drawer_app_row_com.test.gallery",
        lastAppTag = "drawer_app_row_com.test.glassnotes"
      )

      // Focused G section stays razor-sharp while visible neighbor sections above/below are attractively blurred
      val gHeaderCfg = composeTestRule.onNodeWithTag("drawer_section_G", useUnmergedTree = true).fetchSemanticsNode().config
      val gAppCfg = composeTestRule.onNodeWithTag("drawer_app_row_com.test.gmail", useUnmergedTree = true).fetchSemanticsNode().config
      assertEquals(0f, gHeaderCfg[DrawerFocusBlurRadiusKey], 0.05f)
      assertEquals(1f, gHeaderCfg[DrawerFocusAlphaKey], 0.05f)
      assertEquals(0f, gAppCfg[DrawerFocusBlurRadiusKey], 0.05f)
      assertEquals(1f, gAppCfg[DrawerFocusAlphaKey], 0.05f)

      val fAppCfg = composeTestRule.onNodeWithTag("drawer_app_row_com.test.files", useUnmergedTree = true).fetchSemanticsNode().config
      val hAppCfg = composeTestRule.onNodeWithTag("drawer_app_row_com.test.health", useUnmergedTree = true).fetchSemanticsNode().config
      assertTrue("Expected non-focused F app to be blurred", fAppCfg[DrawerFocusBlurRadiusKey] > 5f)
      assertTrue("Expected non-focused F app to be dimmed", fAppCfg[DrawerFocusAlphaKey] < 0.5f)
      assertTrue("Expected non-focused H app to be blurred", hAppCfg[DrawerFocusBlurRadiusKey] > 5f)
      assertTrue("Expected non-focused H app to be dimmed", hAppCfg[DrawerFocusAlphaKey] < 0.5f)

      // Jump to Z inside the open drawer, verify all Z apps are centered and none cut off, then click G again
      composeTestRule.onNodeWithTag("alphabet_rail_key_Z", useUnmergedTree = true).performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000L) {
        composeTestRule.onAllNodesWithTag("drawer_app_row_com.test.zoom", useUnmergedTree = true)
          .fetchSemanticsNodes().isNotEmpty()
      }
      composeTestRule.waitForIdle()
      assertSectionCenteredAndFullyVisible(
        headerTag = "drawer_section_Z",
        firstAppTag = "drawer_app_row_com.test.zeta",
        lastAppTag = "drawer_app_row_com.test.zoom"
      )

      composeTestRule.onNodeWithTag("alphabet_rail_key_G", useUnmergedTree = true).performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000L) {
        composeTestRule.onAllNodesWithTag("drawer_app_row_com.test.glassnotes", useUnmergedTree = true)
          .fetchSemanticsNodes().isNotEmpty()
      }
      composeTestRule.waitForIdle()
      assertSectionCenteredAndFullyVisible(
        headerTag = "drawer_section_G",
        firstAppTag = "drawer_app_row_com.test.gallery",
        lastAppTag = "drawer_app_row_com.test.glassnotes"
      )

      // Continuous sideways + downward drag from B down toward Z lands centered with all Z apps visible
      composeTestRule.onNodeWithTag("alphabet_rail").performTouchInput {
        val startX = right - 10f
        down(androidx.compose.ui.geometry.Offset(startX, height * 0.14f))
        advanceEventTime(20L)
        moveTo(androidx.compose.ui.geometry.Offset(startX - 48f, height * 0.48f))
        advanceEventTime(20L)
        moveTo(androidx.compose.ui.geometry.Offset(startX - 72f, height * 0.945f))
        advanceEventTime(20L)
        up()
      }
      composeTestRule.waitUntil(timeoutMillis = 5_000L) {
        composeTestRule.onAllNodesWithTag("drawer_section_Z", useUnmergedTree = true)
          .fetchSemanticsNodes().isNotEmpty()
      }
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("drawer_section_Z", useUnmergedTree = true).assertIsDisplayed()
      composeTestRule.onNodeWithTag("drawer_app_row_com.test.zeta", useUnmergedTree = true).assertIsDisplayed()
      composeTestRule.onNodeWithTag("drawer_app_row_com.test.zoom", useUnmergedTree = true).assertIsDisplayed()

      // Manually dragging the app list clears spotlight focus so previously blurred sections return crisp
      composeTestRule.onNodeWithTag("drawer_app_list").performTouchInput {
        down(center)
        moveBy(androidx.compose.ui.geometry.Offset(0f, -140f))
        advanceEventTime(20L)
        up()
      }
      composeTestRule.waitForIdle()
      val yAfterDragCfg = composeTestRule.onNodeWithTag("drawer_app_row_com.test.yoga", useUnmergedTree = true).fetchSemanticsNode().config
      assertEquals(0f, yAfterDragCfg[DrawerFocusBlurRadiusKey], 0.05f)
      assertEquals(1f, yAfterDragCfg[DrawerFocusAlphaKey], 0.05f)

      // Even while scrolled deep at section Z, swiping down on the drawer header returns to Home
      composeTestRule.onNodeWithTag("drawer_close_handle").performTouchInput {
        down(center)
        moveBy(androidx.compose.ui.geometry.Offset(0f, 320f))
        advanceEventTime(20L)
        up()
      }
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("app_drawer_screen").assertDoesNotExist()
      composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()

      // Open/scrub drawer via left-edge rail -> strictly 1 right-side alphabet rail (never 2!)
      composeTestRule.onNodeWithTag("left_alphabet_rail").performTouchInput {
        down(centerLeft)
        moveBy(androidx.compose.ui.geometry.Offset(36f, 60f))
      }
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("app_drawer_screen").assertIsDisplayed()
      composeTestRule.onAllNodesWithTag("alphabet_rail").assertCountEquals(1)
      composeTestRule.onNodeWithTag("left_alphabet_rail").performTouchInput {
        up()
      }
      composeTestRule.waitForIdle()
      composeTestRule.onAllNodesWithTag("alphabet_rail").assertCountEquals(1)
    }
  }
}
