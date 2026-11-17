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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AppIconCache(private val context: Context) {

  private val maxEntries = 220
  private val cache = object : LruCache<String, ImageBitmap>(maxEntries) {}
  private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

  fun getCached(componentKey: String, style: IconStyle): ImageBitmap? {
    return cache.get("${componentKey}_${style.name}")
  }

  suspend fun loadIcon(app: AppInfo, style: IconStyle): ImageBitmap? {
    val cacheKey = "${app.componentKey}_${style.name}"
    cache.get(cacheKey)?.let { return it }

    return withContext(Dispatchers.Default) {
      val bitmap = renderIconBitmap(app, style) ?: return@withContext null
      val imageBitmap = bitmap.asImageBitmap()
      cache.put(cacheKey, imageBitmap)
      imageBitmap
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
    val drawable = resolveDrawable(app) ?: return null
    val sizePx = 96

    return when (style) {
      IconStyle.ORIGINAL -> renderOriginalDrawable(drawable, sizePx)
      IconStyle.DOT_MONO -> renderGlassMonochromeDrawable(drawable, sizePx)
    }
  }

  private fun resolveDrawable(app: AppInfo): Drawable? {
    val component = ComponentName(app.packageName, app.activityName)
    val densityDpi = context.resources.displayMetrics.densityDpi

    runCatching {
      val activityInfo = launcherApps
        ?.getActivityList(app.packageName, Process.myUserHandle())
        ?.firstOrNull { it.componentName == component }
      if (activityInfo != null) {
        val icon = activityInfo.getIcon(densityDpi) ?: activityInfo.getIcon(0)
        if (icon != null) return icon
      }
    }
    return runCatching { context.packageManager.getActivityIcon(component) }.getOrNull()
      ?: runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
      ?: runCatching { context.packageManager.defaultActivityIcon }.getOrNull()
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

    // 1. Prefer Android 13+ native monochrome adaptive icon layer when provided
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
      val monoLayer = drawable.monochrome
      if (monoLayer != null) {
        val pad = (sizePx * 0.12f).toInt()
        monoLayer.mutate()
        monoLayer.colorFilter = PorterDuffColorFilter(0xFFF4F3F1.toInt(), PorterDuff.Mode.SRC_IN)
        monoLayer.setBounds(-pad, -pad, sizePx + pad, sizePx + pad)
        monoLayer.draw(canvas)
        return bmp
      }
    }

    // 2. Render adaptive foreground (or full drawable if legacy) into compact 96x96 buffer
    val isAdaptive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable
    val sourceLayer: Drawable? = if (isAdaptive) {
      (drawable as AdaptiveIconDrawable).foreground ?: drawable
    } else {
      drawable
    }
    if (sourceLayer == null) return bmp

    val raw = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val rawCanvas = Canvas(raw)
    if (isAdaptive && sourceLayer !== drawable) {
      val bleed = (sizePx * 0.16f).toInt()
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
              (a * 0.32f).toInt()
            }
            pixels[i] = (outAlpha shl 24) or 0xF4F3F1
          }
        }
        bmp.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
      } else {
        // Uniform-color glyph or vector silhouette on transparent backdrop
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          colorFilter = PorterDuffColorFilter(0xFFF4F3F1.toInt(), PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(raw, 0f, 0f, paint)
      }
    } else {
      // Opaque legacy/third-party square icon: sample dominant backdrop color from inner edges
      val sampleCoords = listOf(
        10 * sizePx + 10,
        10 * sizePx + (sizePx - 11),
        (sizePx - 11) * sizePx + 10,
        (sizePx - 11) * sizePx + (sizePx - 11)
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
        if (dist > 44) {
          val fgAlpha = min(a, ((dist - 36) * 4).coerceIn(0, 255))
          outPixels[i] = (fgAlpha shl 24) or 0xF4F3F1
          if (fgAlpha > 80) extractedForeground++
        } else {
          outPixels[i] = 0
        }
      }

      if (extractedForeground > pixels.size * 0.025f) {
        bmp.setPixels(outPixels, 0, sizePx, 0, 0, sizePx, sizePx)
      } else {
        // Low-contrast / photo fallback: crisp high-contrast grayscale
        val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
        val contrast = 1.35f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        grayMatrix.postConcat(
          ColorMatrix(
            floatArrayOf(
              contrast, 0f, 0f, 0f, translate,
              0f, contrast, 0f, 0f, translate,
              0f, 0f, contrast, 0f, translate,
              0f, 0f, 0f, 1f, 0f
            )
          )
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          colorFilter = ColorMatrixColorFilter(grayMatrix)
        }
        canvas.drawBitmap(raw, 0f, 0f, paint)
      }
    }

    raw.recycle()
    return bmp
  }
}
