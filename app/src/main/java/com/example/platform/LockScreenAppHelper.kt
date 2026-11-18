package com.example.platform

import com.example.data.apps.AppInfo

object LockScreenAppHelper {

  private val exactLockLabels = setOf(
    "lock screen",
    "screen lock",
    "screen off",
    "one tap lock",
    "tap lock",
    "quick lock",
    "sleep screen",
    "lock"
  )

  private val knownLockPackages = setOf(
    "com.coloros.lockscreen",
    "com.oplus.lockscreen",
    "com.miui.lockscreen",
    "com.vivo.lockscreen",
    "com.huawei.lockscreen",
    "com.hihonor.lockscreen",
    "com.lge.lockscreensettings",
    "com.asus.lockscreen",
    "com.motorola.lockscreen",
    "com.oppo.lockscreen",
    "com.realme.lockscreen",
    "com.oneplus.lockscreen"
  )

  private val excludedPackages = setOf(
    "com.android.settings",
    "com.google.android.gms",
    "com.aistudio.mistlauncher.kxmpzq",
    "com.example"
  )

  fun resolveLinkedApp(linkedComponentKey: String?, allApps: List<AppInfo>): AppInfo? {
    if (linkedComponentKey.isNullOrBlank()) return null
    return allApps.firstOrNull { it.componentKey == linkedComponentKey }
  }

  fun scoreCandidate(app: AppInfo): Int {
    val pkg = app.packageName.lowercase()
    if (pkg in excludedPackages) return 0

    val normalizedLabel = app.label.trim().lowercase().replace(Regex("\\s+"), " ")
    if (pkg in knownLockPackages) return 100
    if (normalizedLabel in exactLockLabels) return 95
    if (
      (normalizedLabel.contains("lock") && normalizedLabel.contains("screen")) ||
      (normalizedLabel.contains("screen") && normalizedLabel.contains("off")) ||
      normalizedLabel.contains("screenlock") ||
      normalizedLabel.contains("lockscreen")
    ) {
      return 85
    }
    if (
      pkg.contains("lockscreen") ||
      pkg.contains("screenlock") ||
      pkg.contains("screenoff") ||
      pkg.endsWith(".lock")
    ) {
      return 80
    }
    if (normalizedLabel.contains("lock") && !normalizedLabel.contains("clock") && !normalizedLabel.contains("block")) {
      return 45
    }
    return 0
  }

  fun findSuggestedLockApps(allApps: List<AppInfo>): List<AppInfo> {
    return allApps
      .mapNotNull { app ->
        val score = scoreCandidate(app)
        if (score > 0) app to score else null
      }
      .sortedWith(compareByDescending<Pair<AppInfo, Int>> { it.second }.thenBy { it.first.label.lowercase() })
      .map { it.first }
  }

  fun pickAutoLinkCandidate(allApps: List<AppInfo>): AppInfo? {
    val scored = allApps
      .mapNotNull { app ->
        val score = scoreCandidate(app)
        if (score >= 80) app to score else null
      }
      .sortedWith(compareByDescending<Pair<AppInfo, Int>> { it.second }.thenBy { it.first.label.lowercase() })
    return scored.firstOrNull()?.first
  }
}
