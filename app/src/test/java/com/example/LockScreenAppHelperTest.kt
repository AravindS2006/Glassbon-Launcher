package com.example

import com.example.core.haptics.MistHapticEvent
import com.example.core.haptics.MistHaptics
import com.example.data.apps.AppInfo
import com.example.data.preferences.HapticProfile
import com.example.platform.LockScreenAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenAppHelperTest {

  private fun fakeApp(
    pkg: String,
    label: String,
    activity: String = "$pkg.MainActivity"
  ) = AppInfo(
    label = label,
    packageName = pkg,
    activityName = activity,
    componentKey = "$pkg/$activity",
    sectionChar = label.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
    isSystemApp = false
  )

  @Test
  fun `lock screen candidate discovery ranks lock utilities and excludes settings and launcher`() {
    val clock = fakeApp("com.google.android.deskclock", "Clock")
    val settings = fakeApp("com.android.settings", "Lock Screen Settings")
    val selfApp = fakeApp("com.aistudio.mistlauncher.kxmpzq", "Glassbon Lock")
    val oemLock = fakeApp("com.oplus.lockscreen", "Screen Lock")
    val playLock = fakeApp("com.example.lockutility", "Lock Screen")
    val notes = fakeApp("com.example.notes", "Notes")

    val allApps = listOf(clock, settings, selfApp, notes, playLock, oemLock)
    val suggested = LockScreenAppHelper.findSuggestedLockApps(allApps)

    assertEquals(listOf(oemLock, playLock), suggested)
    assertEquals(oemLock, LockScreenAppHelper.pickAutoLinkCandidate(allApps))
    assertEquals(playLock, LockScreenAppHelper.resolveLinkedApp(playLock.componentKey, allApps))
    assertNull(LockScreenAppHelper.resolveLinkedApp("missing/pkg", allApps))
  }

  @Test
  fun `auto candidate returns null when only unrelated apps are installed`() {
    val allApps = listOf(
      fakeApp("com.example.browser", "Browser"),
      fakeApp("com.example.camera", "Camera")
    )
    assertTrue(LockScreenAppHelper.findSuggestedLockApps(allApps).isEmpty())
    assertNull(LockScreenAppHelper.pickAutoLinkCandidate(allApps))
  }

  @Test
  fun `mist haptics resolves tactile profiles cleanly`() {
    assertNull(MistHaptics.resolveHapticConstant(MistHapticEvent.TAP_LIGHT, HapticProfile.SUBTLE))
    assertNull(MistHaptics.resolveHapticConstant(MistHapticEvent.WAVE_RELEASE, HapticProfile.SUBTLE))
    assertNotNull(MistHaptics.resolveHapticConstant(MistHapticEvent.WAVE_LETTER_TICK, HapticProfile.SUBTLE))

    for (event in MistHapticEvent.entries) {
      assertNotNull(MistHaptics.resolveHapticConstant(event, HapticProfile.BALANCED))
      assertNotNull(MistHaptics.resolveHapticConstant(event, HapticProfile.CRISP))
    }
  }
}
