package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.platform.GlassbonDeviceAdminReceiver
import com.example.platform.GlassbonMediaListenerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name verify HOME intent filter and Play-Protect-safe platform components`() {
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

    val adminReceiverInfo = context.packageManager.getReceiverInfo(
      ComponentName(context, GlassbonDeviceAdminReceiver::class.java),
      PackageManager.GET_META_DATA
    )
    assertNotNull(adminReceiverInfo)

    val mediaServiceInfo = context.packageManager.getServiceInfo(
      ComponentName(context, GlassbonMediaListenerService::class.java),
      PackageManager.GET_META_DATA
    )
    assertNotNull(mediaServiceInfo)

    val pkgInfo = context.packageManager.getPackageInfo(
      context.packageName,
      PackageManager.GET_PERMISSIONS
    )
    val requested = pkgInfo.requestedPermissions?.toList().orEmpty()
    assertFalse(requested.contains(android.Manifest.permission.REQUEST_DELETE_PACKAGES))
  }
}
