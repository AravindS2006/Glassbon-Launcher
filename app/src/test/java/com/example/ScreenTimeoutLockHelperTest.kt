package com.example

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.platform.ScreenTimeoutLockHelper
import com.example.platform.ScreenTimeoutLockResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScreenTimeoutLockHelperTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    setCanWriteSettings(false)
  }

  @After
  fun tearDown() {
    ScreenTimeoutLockHelper.unregisterReceiver(context)
    setCanWriteSettings(false)
  }

  private fun setCanWriteSettings(allowed: Boolean) {
    val appOps = context.getSystemService(AppOpsManager::class.java)
    shadowOf(appOps).setMode(
      AppOpsManager.OPSTR_WRITE_SETTINGS,
      Process.myUid(),
      context.packageName,
      if (allowed) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_ERRORED
    )
  }

  @Test
  fun `triggerShortTimeoutLock requests permission when WRITE_SETTINGS is not granted`() {
    setCanWriteSettings(false)
    val result = ScreenTimeoutLockHelper.triggerShortTimeoutLock(context)
    assertEquals(ScreenTimeoutLockResult.NEEDS_WRITE_SETTINGS_PERMISSION, result)
  }

  @Test
  fun `triggerShortTimeoutLock sets 100ms timeout and restores original on screen off without overwriting saved duration`() {
    setCanWriteSettings(true)
    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 60_000)

    val firstResult = ScreenTimeoutLockHelper.triggerShortTimeoutLock(context)
    assertEquals(ScreenTimeoutLockResult.LOCKING_VIA_TIMEOUT, firstResult)
    assertEquals(
      ScreenTimeoutLockHelper.SHORT_LOCK_TIMEOUT_MS,
      Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
    )

    // Rapid second tap while timeout is already 100ms must NOT overwrite saved 60_000ms
    val secondResult = ScreenTimeoutLockHelper.triggerShortTimeoutLock(context)
    assertEquals(ScreenTimeoutLockResult.LOCKING_VIA_TIMEOUT, secondResult)

    // Screen-off broadcast restores the user's original 60_000ms timeout
    context.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF).setPackage(context.packageName))
    ScreenTimeoutLockHelper.restoreOriginalTimeoutIfNeeded(context)

    assertEquals(
      60_000,
      Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
    )
  }
}
