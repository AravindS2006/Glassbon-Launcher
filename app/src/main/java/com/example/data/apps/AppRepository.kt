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

  private val localeReceiver = object : BroadcastReceiver() {
    override fun onReceive(ctx: Context?, intent: Intent?) {
      refreshApps()
    }
  }

  private var registered = false
  private var refreshJob: kotlinx.coroutines.Job? = null
  private var lastRefreshElapsedMs: Long = 0L

  fun startObserving() {
    if (!registered) {
      registered = true
      runCatching {
        launcherApps?.registerCallback(launcherAppsCallback, Handler(Looper.getMainLooper()))
      }
      val packageFilter = IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_CHANGED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addDataScheme("package")
      }
      val localeFilter = IntentFilter(Intent.ACTION_LOCALE_CHANGED)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(packageReceiver, packageFilter, Context.RECEIVER_NOT_EXPORTED)
        context.registerReceiver(localeReceiver, localeFilter, Context.RECEIVER_NOT_EXPORTED)
      } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(packageReceiver, packageFilter)
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(localeReceiver, localeFilter)
      }
    }
    refreshApps()
  }

  fun stopObserving() {
    if (!registered) return
    registered = false
    refreshJob?.cancel()
    refreshJob = null
    runCatching { launcherApps?.unregisterCallback(launcherAppsCallback) }
    runCatching { context.unregisterReceiver(packageReceiver) }
    runCatching { context.unregisterReceiver(localeReceiver) }
  }

  fun refreshIfNeeded(minIntervalMs: Long = 1_000L) {
    if (SystemClock.elapsedRealtime() - lastRefreshElapsedMs >= minIntervalMs) {
      refreshApps()
    }
  }

  fun refreshApps() {
    lastRefreshElapsedMs = SystemClock.elapsedRealtime()
    refreshJob?.cancel()
    refreshJob = scope.launch {
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

  fun launchApp(app: AppInfo, callerContext: Context = context): Boolean {
    val now = SystemClock.elapsedRealtime()
    if (lastLaunchedComponent == app.componentKey && now - lastLaunchElapsedMs < 350L) {
      // Duplicate tap swallowed: the first tap already launched the app, so this is not a failure.
      // Returning false here used to surface "<app> is unavailable" and re-open the lock app picker.
      return true
    }
    val component = ComponentName(app.packageName, app.activityName)
    val launched = try {
      val launchIntent = Intent.makeMainActivity(component).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
      }
      callerContext.startActivity(launchIntent)
      true
    } catch (primaryEx: ActivityNotFoundException) {
      launchViaLauncherAppsOrRefresh(component, app.packageName, primaryEx)
    } catch (_: SecurityException) {
      launchViaLauncherAppsOrRefresh(component, app.packageName, null)
    } catch (_: IllegalArgumentException) {
      refreshApps()
      false
    }
    // Only arm the debounce after a successful launch so a failed attempt can be retried immediately.
    if (launched) {
      lastLaunchedComponent = app.componentKey
      lastLaunchElapsedMs = now
    }
    return launched
  }

  private fun launchViaLauncherAppsOrRefresh(
    component: ComponentName,
    packageName: String,
    primaryFailure: ActivityNotFoundException?
  ): Boolean {
    return try {
      val exists = launcherApps?.getActivityList(packageName, Process.myUserHandle())
        ?.any { it.componentName == component } == true
      if (exists) {
        launcherApps?.startMainActivity(component, Process.myUserHandle(), null, null)
        true
      } else {
        if (primaryFailure != null) refreshApps()
        false
      }
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
