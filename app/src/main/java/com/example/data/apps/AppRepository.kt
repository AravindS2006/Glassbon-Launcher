package com.example.data.apps

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.os.UserHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AppRepository(
  private val context: Context,
  private val onIconsInvalidated: (String?) -> Unit = {}
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
  val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private var lastLaunchElapsedMs: Long = 0L
  private var lastLaunchedComponent: String? = null

  private val launcherApps: LauncherApps? =
    context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

  private val launcherAppsCallback = object : LauncherApps.Callback() {
    override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
      onIconsInvalidated(packageName)
      refreshApps()
    }

    override fun onPackageAdded(packageName: String?, user: UserHandle?) {
      onIconsInvalidated(packageName)
      refreshApps()
    }

    override fun onPackageChanged(packageName: String?, user: UserHandle?) {
      onIconsInvalidated(packageName)
      refreshApps()
    }

    override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
      onIconsInvalidated(null)
      refreshApps()
    }

    override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
      onIconsInvalidated(null)
      refreshApps()
    }
  }

  private val packageReceiver = object : BroadcastReceiver() {
    override fun onReceive(ctx: Context?, intent: Intent?) {
      val pkg = intent?.data?.schemeSpecificPart
      onIconsInvalidated(pkg)
      refreshApps()
    }
  }

  private var registered = false

  fun startObserving() {
    if (!registered) {
      registered = true
      runCatching {
        launcherApps?.registerCallback(launcherAppsCallback, Handler(Looper.getMainLooper()))
      }
      val filter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_CHANGED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addAction(Intent.ACTION_LOCALE_CHANGED)
        addDataScheme("package")
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
      } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(packageReceiver, filter)
      }
    }
    refreshApps()
  }

  fun stopObserving() {
    if (!registered) return
    registered = false
    runCatching { launcherApps?.unregisterCallback(launcherAppsCallback) }
    runCatching { context.unregisterReceiver(packageReceiver) }
  }

  fun refreshApps() {
    scope.launch {
      val discovered = queryLaunchableApps()
      _apps.value = discovered
      _isLoading.value = false
    }
  }

  suspend fun queryLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
    val locale = Locale.getDefault()
    val ownPackage = context.packageName
    val pm = context.packageManager

    val launcherEntries = runCatching {
      launcherApps?.getActivityList(null, Process.myUserHandle()).orEmpty()
    }.getOrDefault(emptyList())

    val rawList: List<AppInfo> = if (launcherEntries.isNotEmpty()) {
      launcherEntries.mapNotNull { info ->
        val component = info.componentName
        if (component.packageName == ownPackage) return@mapNotNull null
        val label = info.label?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
          ?: component.packageName
        val isSystem = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        AppInfo(
          label = label,
          packageName = component.packageName,
          activityName = component.className,
          componentKey = "${component.packageName}/${component.className}",
          sectionChar = AppIndexHelper.computeSectionChar(label, locale),
          isSystemApp = isSystem
        )
      }
    } else {
      val mainIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
      }
      val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
      } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(mainIntent, 0)
      }
      resolved.mapNotNull { ri ->
        val ai = ri.activityInfo ?: return@mapNotNull null
        if (ai.packageName == ownPackage) return@mapNotNull null
        val label = ri.loadLabel(pm)?.toString()?.trim().takeUnless { it.isNullOrEmpty() }
          ?: ai.packageName
        val appFlags = ai.applicationInfo?.flags ?: 0
        AppInfo(
          label = label,
          packageName = ai.packageName,
          activityName = ai.name,
          componentKey = "${ai.packageName}/${ai.name}",
          sectionChar = AppIndexHelper.computeSectionChar(label, locale),
          isSystemApp = (appFlags and ApplicationInfo.FLAG_SYSTEM) != 0
        )
      }
    }

    val deduplicated = rawList.distinctBy { it.componentKey }
    AppIndexHelper.sortAppsAlphabetically(deduplicated, locale)
  }

  fun launchApp(app: AppInfo): Boolean {
    val now = SystemClock.elapsedRealtime()
    if (lastLaunchedComponent == app.componentKey && now - lastLaunchElapsedMs < 350L) {
      return false
    }
    lastLaunchedComponent = app.componentKey
    lastLaunchElapsedMs = now

    val component = ComponentName(app.packageName, app.activityName)
    return try {
      if (launcherApps != null) {
        val exists = launcherApps.getActivityList(app.packageName, Process.myUserHandle())
          .any { it.componentName == component }
        if (exists) {
          launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
          return true
        }
      }
      val launchIntent = Intent.makeMainActivity(component).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
      }
      context.startActivity(launchIntent)
      true
    } catch (_: ActivityNotFoundException) {
      refreshApps()
      false
    } catch (_: SecurityException) {
      false
    } catch (_: IllegalArgumentException) {
      refreshApps()
      false
    }
  }
}
