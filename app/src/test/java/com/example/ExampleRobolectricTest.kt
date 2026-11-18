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
    val internalCorePermission = "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
    val requested = pkgInfo.requestedPermissions?.toList().orEmpty()
      .filterNot { it == internalCorePermission }
    assertTrue("Expected zero Play-Protect/system manifest permissions, found: $requested", requested.isEmpty())
    assertFalse(requested.any { it.startsWith("android.permission.") })
    assertFalse(requested.contains(android.Manifest.permission.WRITE_SETTINGS))
    assertFalse(requested.contains(android.Manifest.permission.REQUEST_DELETE_PACKAGES))
    assertFalse(requested.contains(android.Manifest.permission.INTERNET))

    val servicePermissions = pkgInfo.services?.mapNotNull { it.permission }.orEmpty()
    assertTrue(pkgInfo.services.isNullOrEmpty())
    assertFalse(servicePermissions.contains(android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE))
    assertFalse(servicePermissions.contains(android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE))

    val receiverPermissions = pkgInfo.receivers?.mapNotNull { it.permission }.orEmpty()
    assertFalse(receiverPermissions.contains(android.Manifest.permission.BIND_DEVICE_ADMIN))

    assertTrue(BuildConfig.VERSION_NAME.isNotBlank())
    assertTrue(BuildConfig.VERSION_CODE >= 10_000)
  }
}
