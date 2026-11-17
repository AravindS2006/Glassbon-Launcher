package com.example

import com.example.data.apps.AppIndexHelper
import com.example.data.apps.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppIndexHelperTest {

  private fun app(label: String, pkg: String = "com.test.${label.lowercase()}") = AppInfo(
    label = label,
    packageName = pkg,
    activityName = "$pkg.MainActivity",
    componentKey = "$pkg/$pkg.MainActivity",
    sectionChar = AppIndexHelper.computeSectionChar(label, Locale.US),
    isSystemApp = false
  )

  @Test
  fun `computeSectionChar normalizes accents and non-letters`() {
    assertEquals("A", AppIndexHelper.computeSectionChar("Albums", Locale.US))
    assertEquals("E", AppIndexHelper.computeSectionChar("Éclair", Locale.US))
    assertEquals("#", AppIndexHelper.computeSectionChar("1Password", Locale.US))
    assertEquals("#", AppIndexHelper.computeSectionChar("_Notes", Locale.US))
    assertEquals("#", AppIndexHelper.computeSectionChar("   ", Locale.US))
  }

  @Test
  fun `sortAppsAlphabetically places A-Z before hash group`() {
    val unsorted = listOf(
      app("1Password"),
      app("Chrome"),
      app("Authenticator"),
      app("Brave")
    )
    val sorted = AppIndexHelper.sortAppsAlphabetically(unsorted, Locale.US)
    assertEquals(
      listOf("Authenticator", "Brave", "Chrome", "1Password"),
      sorted.map { it.label }
    )
  }

  @Test
  fun `groupAppsBySection creates ordered sections`() {
    val sorted = AppIndexHelper.sortAppsAlphabetically(
      listOf(app("Albums"), app("Assistant"), app("Brave"), app("7Zip")),
      Locale.US
    )
    val sections = AppIndexHelper.groupAppsBySection(sorted)
    assertEquals(listOf("A", "B", "#"), sections.map { it.header })
    assertEquals(listOf("Albums", "Assistant"), sections[0].apps.map { it.label })
    assertEquals(listOf("Brave"), sections[1].apps.map { it.label })
    assertEquals(listOf("7Zip"), sections[2].apps.map { it.label })
  }

  @Test
  fun `filterApps performs case-insensitive matching`() {
    val apps = listOf(app("Camera"), app("Calendar"), app("Spotify"))
    val filtered = AppIndexHelper.filterApps(apps, "cal")
    assertEquals(listOf("Calendar"), filtered.map { it.label })

    val empty = AppIndexHelper.filterApps(apps, "nonexistent")
    assertEquals(0, empty.size)
  }
}
