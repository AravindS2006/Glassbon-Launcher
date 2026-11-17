package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name verify HOME intent filter and Play-Protect zero sensitive permissions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Glassbon", appName)

    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_HOME)
      addCategory(Intent.CATEGORY_DEFAULT)
    }
    val matches = context.packageManager.queryIntentActivities(
      homeIntent,
      PackageManager.MATCH_DEFAULT_ONLY
    )
    assertTrue(matches.any { it.activityInfo.packageName == context.packageName })

    val pkgInfo = context.packageManager.getPackageInfo(
      context.packageName,
      PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS
    )
    val requested = pkgInfo.requestedPermissions?.toList().orEmpty()
    assertFalse(requested.contains(android.Manifest.permission.REQUEST_DELETE_PACKAGES))
    assertFalse(requested.contains(android.Manifest.permission.INTERNET))
    assertFalse(requested.contains(android.Manifest.permission.READ_SMS))
    assertFalse(requested.contains(android.Manifest.permission.RECEIVE_SMS))
    assertFalse(requested.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE))
    assertTrue(requested.contains(android.Manifest.permission.WRITE_SETTINGS))

    val servicePermissions = pkgInfo.services?.mapNotNull { it.permission }.orEmpty()
    assertTrue(pkgInfo.services.isNullOrEmpty())
    assertFalse(servicePermissions.contains(android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE))
    assertFalse(servicePermissions.contains(android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE))

    val receiverPermissions = pkgInfo.receivers?.mapNotNull { it.permission }.orEmpty()
    assertFalse(receiverPermissions.contains(android.Manifest.permission.BIND_DEVICE_ADMIN))
  }
}
