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

  /**
   * Returns the apps matching [query], best matches first. Matching is case- and accent-insensitive
   * ("cafe" finds "Café"). Ranking, highest first:
   * exact label > label prefix > word prefix ("mus" -> "YouTube Music") > initials ("gm" -> "Google Maps") >
   * label substring > every query word matches > package name.
   * Ties keep the original (alphabetical) order.
   */
  fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
    val q = normalizeForSearch(query)
    if (q.isEmpty()) return apps
    val tokens = q.split(' ')
    return apps
      .mapIndexedNotNull { index, app ->
        val score = matchScore(app, q, tokens)
        if (score > 0) IndexedValue(index, app) to score else null
      }
      .sortedWith(compareByDescending<Pair<IndexedValue<AppInfo>, Int>> { it.second }.thenBy { it.first.index })
      .map { it.first.value }
  }

  internal fun normalizeForSearch(text: String): String {
    val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
    return COMBINING_MARKS.replace(decomposed, "")
      .lowercase(Locale.ROOT)
      .trim()
      .replace(WHITESPACE, " ")
  }

  private fun matchScore(app: AppInfo, q: String, tokens: List<String>): Int {
    val label = normalizeForSearch(app.label)
    val words = label.split(WORD_SEPARATORS).filter { it.isNotEmpty() }
    return when {
      label == q -> 100
      label.startsWith(q) -> 80
      words.any { it.startsWith(q) } -> 60
      q.length >= 2 && !q.contains(' ') && words.joinToString("") { it.take(1) }.startsWith(q) -> 50
      label.contains(q) -> 40
      tokens.size > 1 && tokens.all { token -> words.any { it.startsWith(token) } } -> 35
      tokens.size > 1 && tokens.all { label.contains(it) } -> 30
      // Skip the reverse-domain prefix ("com.", "org.") so short queries like "co" don't match every app
      app.packageName.substringAfter('.').lowercase(Locale.ROOT).contains(q) -> 10
      else -> 0
    }
  }

  private val COMBINING_MARKS = Regex("\\p{Mn}+")
  private val WHITESPACE = Regex("\\s+")
  private val WORD_SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
}
