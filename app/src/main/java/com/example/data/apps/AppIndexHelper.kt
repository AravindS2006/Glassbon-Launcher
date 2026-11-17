package com.example.data.apps

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

object AppIndexHelper {

  val ALPHABET_RAIL_LETTERS: List<String> = ('A'..'Z').map { it.toString() } + "#"

  fun computeSectionChar(label: String, locale: Locale = Locale.getDefault()): String {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return "#"
    val firstChar = trimmed.first()
    val decomposed = Normalizer.normalize(firstChar.toString(), Normalizer.Form.NFD)
    val asciiBase = decomposed.firstOrNull { Character.isLetterOrDigit(it) } ?: firstChar
    val upper = asciiBase.uppercaseChar()
    return if (upper in 'A'..'Z') upper.toString() else "#"
  }

  fun sortAppsAlphabetically(apps: List<AppInfo>, locale: Locale = Locale.getDefault()): List<AppInfo> {
    val collator = Collator.getInstance(locale).apply {
      strength = Collator.PRIMARY
    }
    return apps.sortedWith { a, b ->
      val aIsHash = a.sectionChar == "#"
      val bIsHash = b.sectionChar == "#"
      when {
        aIsHash && !bIsHash -> 1
        !aIsHash && bIsHash -> -1
        else -> {
          val cmp = collator.compare(a.label, b.label)
          if (cmp != 0) cmp else a.componentKey.compareTo(b.componentKey)
        }
      }
    }
  }

  fun groupAppsBySection(sortedApps: List<AppInfo>): List<AppSection> {
    if (sortedApps.isEmpty()) return emptyList()
    val grouped = LinkedHashMap<String, MutableList<AppInfo>>()
    for (app in sortedApps) {
      grouped.getOrPut(app.sectionChar) { mutableListOf() }.add(app)
    }
    return grouped.entries.map { (header, items) ->
      AppSection(header = header, apps = items)
    }
  }

  fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return apps
    return apps.filter { app ->
      app.label.contains(normalizedQuery, ignoreCase = true) ||
        app.packageName.contains(normalizedQuery, ignoreCase = true)
    }
  }
}
