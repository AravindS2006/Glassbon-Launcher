package com.example.core.graphics

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.apps.AppInfo
import com.example.data.preferences.IconStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AppIconCache(private val context: Context) {

  private val maxEntries = 220
  private val lowPowerEntries = 48
  private val cache = object : LruCache<String, ImageBitmap>(maxEntries) {}
  private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

  fun setLowMemoryCap(lowPower: Boolean) {
    val target = if (lowPower) lowPowerEntries else maxEntries
    if (cache.maxSize() != target) {
      cache.resize(target)
    }
  }

  fun getCached(componentKey: String, style: IconStyle): ImageBitmap? {
    return cache.get("${componentKey}_${style.name}")
  }

  suspend fun loadIcon(app: AppInfo, style: IconStyle): ImageBitmap? {
    val cacheKey = "${app.componentKey}_${style.name}"
    cache.get(cacheKey)?.let { return it }

    return withContext(Dispatchers.Default) {
      val bitmap = renderIconBitmap(app, style) ?: return@withContext null
      bitmap.prepareToDraw()
      val imageBitmap = bitmap.asImageBitmap()
      cache.put(cacheKey, imageBitmap)
      imageBitmap
    }
  }

  suspend fun prewarm(apps: List<AppInfo>, style: IconStyle) {
    if (apps.isEmpty()) return
    withContext(Dispatchers.Default) {
      val limit = apps.size.coerceAtMost(180)
      for (i in 0 until limit) {
        val app = apps[i]
        val key = "${app.componentKey}_${style.name}"
        if (cache.get(key) == null) {
          val bitmap = renderIconBitmap(app, style)
          if (bitmap != null) {
            bitmap.prepareToDraw()
            cache.put(key, bitmap.asImageBitmap())
          }
          if (i % 4 == 3) yield()
        }
      }
    }
  }

  fun invalidatePackage(packageName: String?) {
    if (packageName == null) {
      cache.evictAll()
      return
    }
    val keys = cache.snapshot().keys.filter { it.startsWith("$packageName/") }
    keys.forEach { cache.remove(it) }
  }

  fun clear() {
    cache.evictAll()
  }

  private fun renderIconBitmap(app: AppInfo, style: IconStyle): Bitmap? {
    val candidates = resolveDrawableCandidates(app)
    if (candidates.isEmpty()) return null
    val sizePx = 72

    for (rawDrawable in candidates) {
      val rendered = runCatching {
        val drawable = cloneDrawable(rawDrawable)
        when (style) {
          IconStyle.ORIGINAL -> renderOriginalDrawable(drawable, sizePx)
          IconStyle.DOT_MONO -> renderGlassMonochromeDrawable(drawable, sizePx)
        }
      }.getOrNull() ?: continue

      if (countVisiblePixels(rendered) >= 8) {
        return rendered
      }
      rendered.recycle()
    }

    // Final non-blank safeguard using original full-color drawable if monochrome pass produced nothing
    if (style == IconStyle.DOT_MONO) {
      for (rawDrawable in candidates) {
        val fallback = runCatching {
          renderOriginalDrawable(cloneDrawable(rawDrawable), sizePx)
        }.getOrNull() ?: continue
        if (countVisiblePixels(fallback) >= 8) {
          return fallback
        }
        fallback.recycle()
      }
    }
    return null
  }

  private fun cloneDrawable(drawable: Drawable): Drawable {
    // CRITICAL: Call newDrawable() with NO host Resources argument so third-party Vector/Adaptive
    // drawables retain their own package Resources and never lose layers or throw NotFoundException.
    val cloned = runCatching { drawable.constantState?.newDrawable() }.getOrNull() ?: drawable
    val mutated = runCatching { cloned.mutate() }.getOrDefault(cloned)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mutated is android.graphics.drawable.BitmapDrawable) {
      val bmp = mutated.bitmap
      if (bmp != null && bmp.config == Bitmap.Config.HARDWARE) {
        val soft = runCatching { bmp.copy(Bitmap.Config.ARGB_8888, false) }.getOrNull()
        if (soft != null) {
          return android.graphics.drawable.BitmapDrawable(context.resources, soft)
        }
      }
    }
    return mutated
  }

  private fun resolveDrawableCandidates(app: AppInfo): List<Drawable> {
    val component = ComponentName(app.packageName, app.activityName)
    val densityDpi = context.resources.displayMetrics.densityDpi
    val pm = context.packageManager
    val candidates = ArrayList<Drawable>(8)

    runCatching {
      val activityInfo = launcherApps
        ?.getActivityList(app.packageName, Process.myUserHandle())
        ?.firstOrNull { it.componentName == component }
      if (activityInfo != null) {
        runCatching { activityInfo.getIcon(densityDpi) }.getOrNull()?.let { candidates.add(it) }
        if (densityDpi != 0) {
          runCatching { activityInfo.getIcon(0) }.getOrNull()?.let { candidates.add(it) }
        }
        runCatching { activityInfo.getBadgedIcon(densityDpi) }.getOrNull()?.let { candidates.add(it) }
      }
    }
    runCatching { pm.getActivityIcon(component) }.getOrNull()?.let { candidates.add(it) }
    runCatching {
      val mainIntent = android.content.Intent.makeMainActivity(component)
      @Suppress("DEPRECATION")
      pm.resolveActivity(mainIntent, 0)?.loadIcon(pm)
    }.getOrNull()?.let { candidates.add(it) }
    runCatching {
      val appInfo = pm.getApplicationInfo(app.packageName, 0)
      appInfo.loadUnbadgedIcon(pm)
    }.getOrNull()?.let { candidates.add(it) }
    runCatching { pm.getApplicationIcon(app.packageName) }.getOrNull()?.let { candidates.add(it) }
    runCatching {
      val appInfo = pm.getApplicationInfo(app.packageName, 0)
      if (appInfo.icon != 0) {
        val appRes = pm.getResourcesForApplication(appInfo)
        appRes.getDrawableForDensity(appInfo.icon, densityDpi, null)
      } else {
        null
      }
    }.getOrNull()?.let { candidates.add(it) }
    runCatching { pm.defaultActivityIcon }.getOrNull()?.let { candidates.add(it) }

    return candidates
  }

  private fun countVisiblePixels(bitmap: Bitmap): Int {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return 0
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    var visible = 0
    for (px in pixels) {
      if (Color.alpha(px) >= 20) visible++
    }
    return visible
  }

  private fun renderOriginalDrawable(drawable: Drawable, sizePx: Int): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable.setBounds(0, 0, sizePx, sizePx)
    drawable.draw(canvas)
    return bmp
  }

  private fun renderGlassMonochromeDrawable(drawable: Drawable, sizePx: Int): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // 1. Prefer Android 13+ native monochrome adaptive icon layer ONLY when it renders real non-solid strokes
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
      val rawMono = runCatching { drawable.monochrome }.getOrNull()
      if (rawMono != null) {
        val monoRaw = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val drawn = runCatching {
          val monoLayer = cloneDrawable(rawMono)
          val monoCanvas = Canvas(monoRaw)
          val pad = (sizePx * 0.16f).toInt()
          monoLayer.colorFilter = null
          monoLayer.setBounds(-pad, -pad, sizePx + pad, sizePx + pad)
          monoLayer.draw(monoCanvas)
          true
        }.getOrDefault(false)
        val visibleMono = if (drawn) countVisiblePixels(monoRaw) else 0
        val maxReasonableMono = (sizePx * sizePx * 0.94f).toInt()
        if (visibleMono in 16..maxReasonableMono) {
          val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(0xFFF4F3F1.toInt(), PorterDuff.Mode.SRC_IN)
          }
          canvas.drawBitmap(monoRaw, 0f, 0f, tintPaint)
          monoRaw.recycle()
          return bmp
        }
        monoRaw.recycle()
      }
    }

    // 2. Render adaptive foreground (or full drawable if legacy) into scratch buffer
    val isAdaptive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable
    val sourceLayer: Drawable? = if (isAdaptive) {
      (drawable as AdaptiveIconDrawable).foreground?.let { cloneDrawable(it) } ?: drawable
    } else {
      drawable
    }
    if (sourceLayer == null) return bmp

    val raw = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val rawCanvas = Canvas(raw)
    if (isAdaptive && sourceLayer !== drawable) {
      val bleed = (sizePx * 0.22f).toInt()
      sourceLayer.setBounds(-bleed, -bleed, sizePx + bleed, sizePx + bleed)
    } else {
      val inset = (sizePx * 0.08f).toInt()
      sourceLayer.setBounds(inset, inset, sizePx - inset, sizePx - inset)
    }
    sourceLayer.draw(rawCanvas)

    val pixels = IntArray(sizePx * sizePx)
    raw.getPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)

    // Inspect transparency and luminance range
    var transparentCount = 0
    var opaqueCount = 0
    var minLum = 1f
    var maxLum = 0f

    for (px in pixels) {
      val a = Color.alpha(px)
      if (a < 28) {
        transparentCount++
      } else {
        opaqueCount++
        val lum = (0.299f * Color.red(px) + 0.587f * Color.green(px) + 0.114f * Color.blue(px)) / 255f
        if (lum < minLum) minLum = lum
        if (lum > maxLum) maxLum = lum
      }
    }

    // Fallback: if adaptive foreground turned out empty, render full drawable instead
    if (opaqueCount < 16 && sourceLayer !== drawable) {
      rawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
      val inset = (sizePx * 0.06f).toInt()
      drawable.setBounds(inset, inset, sizePx - inset, sizePx - inset)
      drawable.draw(rawCanvas)
      raw.getPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
      transparentCount = 0
      opaqueCount = 0
      minLum = 1f
      maxLum = 0f
      for (px in pixels) {
        val a = Color.alpha(px)
        if (a < 28) transparentCount++ else {
          opaqueCount++
          val lum = (0.299f * Color.red(px) + 0.587f * Color.green(px) + 0.114f * Color.blue(px)) / 255f
          minLum = min(minLum, lum)
          maxLum = max(maxLum, lum)
        }
      }
    }

    if (opaqueCount == 0) {
      raw.recycle()
      return bmp
    }

    val hasTransparentBackdrop = transparentCount > (pixels.size * 0.18f)

    if (hasTransparentBackdrop) {
      val lumSpan = maxLum - minLum
      if (lumSpan > 0.42f && opaqueCount > pixels.size * 0.18f) {
        // Dual-tone foreground (e.g. colored badge with white symbol inside): keep inner symbol bright white and outer shell subtle smoked grey
        val threshold = minLum + lumSpan * 0.52f
        for (i in pixels.indices) {
          val px = pixels[i]
          val a = Color.alpha(px)
          if (a < 16) {
            pixels[i] = 0
          } else {
            val lum = (0.299f * Color.red(px) + 0.587f * Color.green(px) + 0.114f * Color.blue(px)) / 255f
            val outAlpha = if (lum >= threshold) {
              a
            } else {
              (a * 0.36f).toInt()
            }
            pixels[i] = (outAlpha shl 24) or 0xF4F3F1
          }
        }
        bmp.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
      } else {
        // Uniform-color or dark glyph on transparent backdrop: tint cleanly to bright frosted off-white
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          colorFilter = PorterDuffColorFilter(0xFFF4F3F1.toInt(), PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(raw, 0f, 0f, paint)
      }
    } else {
      // Opaque legacy/third-party square icon: sample dominant backdrop color from inner edges
      val sampleCoords = listOf(
        8 * sizePx + 8,
        8 * sizePx + (sizePx - 9),
        (sizePx - 9) * sizePx + 8,
        (sizePx - 9) * sizePx + (sizePx - 9)
      )
      var bgR = 0
      var bgG = 0
      var bgB = 0
      var validSamples = 0
      for (idx in sampleCoords) {
        val sp = pixels[idx]
        if (Color.alpha(sp) > 128) {
          bgR += Color.red(sp)
          bgG += Color.green(sp)
          bgB += Color.blue(sp)
          validSamples++
        }
      }
      if (validSamples > 0) {
        bgR /= validSamples
        bgG /= validSamples
        bgB /= validSamples
      }

      var extractedForeground = 0
      val outPixels = IntArray(pixels.size)
      for (i in pixels.indices) {
        val px = pixels[i]
        val a = Color.alpha(px)
        if (a < 24) {
          outPixels[i] = 0
          continue
        }
        val dist = abs(Color.red(px) - bgR) + abs(Color.green(px) - bgG) + abs(Color.blue(px) - bgB)
        if (dist > 34) {
          val fgAlpha = min(a, ((dist - 26) * 5).coerceIn(0, 255))
          outPixels[i] = (fgAlpha shl 24) or 0xF4F3F1
          if (fgAlpha > 64) extractedForeground++
        } else {
          outPixels[i] = 0
        }
      }

      if (extractedForeground > pixels.size * 0.02f) {
        bmp.setPixels(outPixels, 0, sizePx, 0, 0, sizePx, sizePx)
      } else {
        // Low-contrast / dark-on-dark / photo fallback: ensure bright luminous contrast on dark frosted tile
        val lumSpan = (maxLum - minLum).coerceAtLeast(0.05f)
        val isMostlyDark = maxLum < 0.45f
        val boostedPixels = IntArray(pixels.size)
        for (i in pixels.indices) {
          val px = pixels[i]
          val a = Color.alpha(px)
          if (a < 24) {
            boostedPixels[i] = 0
            continue
          }
          val lum = (0.299f * Color.red(px) + 0.587f * Color.green(px) + 0.114f * Color.blue(px)) / 255f
          val norm = ((lum - minLum) / lumSpan).coerceIn(0f, 1f)
          val lifted = if (isMostlyDark) {
            (0.55f + 0.45f * (1f - norm)).coerceIn(0.45f, 1f)
          } else {
            (0.38f + 0.62f * norm).coerceIn(0.35f, 1f)
          }
          val c = (lifted * 244f).toInt().coerceIn(85, 248)
          boostedPixels[i] = (a shl 24) or (c shl 16) or (c shl 8) or c
        }
        bmp.setPixels(boostedPixels, 0, sizePx, 0, 0, sizePx, sizePx)
      }
    }

    raw.recycle()
    return bmp
  }
}
