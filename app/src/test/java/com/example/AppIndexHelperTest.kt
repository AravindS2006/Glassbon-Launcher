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

  @Test
  fun `filterApps ranks prefix and word-start matches above substrings`() {
    // Input is alphabetical, as produced by sortAppsAlphabetically
    val apps = listOf(app("Authenticator"), app("Maps"), app("Photomath"), app("YouTube Music"))
    assertEquals(
      listOf("Maps", "Photomath"),
      AppIndexHelper.filterApps(apps, "ma").map { it.label }
    )
    // Equal-rank substring matches keep alphabetical order
    assertEquals(
      listOf("Authenticator", "YouTube Music"),
      AppIndexHelper.filterApps(apps, "ic").map { it.label }
    )
    assertEquals("YouTube Music", AppIndexHelper.filterApps(apps, "mus").first().label)
  }

  @Test
  fun `filterApps is accent-insensitive and matches initials and multiple words`() {
    val apps = listOf(app("Café Finder"), app("Google Maps", "com.google.android.apps.maps"), app("Gmail", "com.google.android.gm"))
    assertEquals(listOf("Café Finder"), AppIndexHelper.filterApps(apps, "cafe").map { it.label })
    assertEquals("Google Maps", AppIndexHelper.filterApps(apps, "goo ma").single().label)
    // "gm": Gmail is a label prefix (80) so it outranks the Google Maps initials match (50)
    assertEquals(listOf("Gmail", "Google Maps"), AppIndexHelper.filterApps(apps, "gm").map { it.label })
  }

  @Test
  fun `filterApps ignores reverse-domain package prefix`() {
    val apps = listOf(app("Notes", "com.vendor.notes"), app("Weather", "com.vendor.weather"))
    assertEquals(0, AppIndexHelper.filterApps(apps, "com").size)
    assertEquals(2, AppIndexHelper.filterApps(apps, "vendor").size)
  }
}
