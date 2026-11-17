package com.example.data.apps

import androidx.compose.runtime.Immutable

@Immutable
data class AppInfo(
  val label: String,
  val packageName: String,
  val activityName: String,
  val componentKey: String,
  val sectionChar: String,
  val isSystemApp: Boolean
)

@Immutable
data class AppSection(
  val header: String,
  val apps: List<AppInfo>
)
