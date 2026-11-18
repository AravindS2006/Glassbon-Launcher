package com.example.data.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

@Immutable
data class ProcessedWallpaper(
  val homeBitmap: ImageBitmap?,
  val drawerBlurredBitmap: ImageBitmap?,
  val isTopBright: Boolean,
  val isMiddleBright: Boolean = false,
  val isEdgesBright: Boolean = false
)

class WallpaperProcessor(private val context: Context) {

  suspend fun persistPickedWallpaper(uri: Uri): String? = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
    val targetFile = File(dir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")

    val decoded = decodeSampledFromUri(uri, maxDimension = 1280) ?: return@withContext null
    return@withContext try {
      FileOutputStream(targetFile).use { out ->
        decoded.compress(Bitmap.CompressFormat.JPEG, 90, out)
      }
      dir.listFiles()?.forEach { old ->
        if (old.name.startsWith("custom_wallpaper_") && old.absolutePath != targetFile.absolutePath) {
          old.delete()
        }
      }
      targetFile.absolutePath
    } catch (_: Exception) {
      targetFile.delete()
      null
    } finally {
      decoded.recycle()
    }
  }

  fun clearCustomWallpaperFiles() {
    val dir = File(context.filesDir, "wallpapers")
    dir.listFiles()?.forEach { it.delete() }
    synchronized(cacheLock) {
      cachedConfigKey = null
      cachedResult = null
    }
  }

  suspend fun loadProcessedWallpaper(
    customPath: String?,
    grayscale: Boolean,
    blurIntensity: Float,
    lowEffectsMode: Boolean
  ): ProcessedWallpaper = withContext(Dispatchers.IO) {
    val blurBucket = if (lowEffectsMode) 0 else (blurIntensity * 10).toInt()
    val configKey = "${customPath ?: "default"}|$grayscale|$blurBucket|$lowEffectsMode"
    synchronized(cacheLock) {
      if (configKey == cachedConfigKey && cachedResult != null) {
        return@withContext cachedResult!!
      }
    }

    val sourceBitmap = loadSourceBitmap(customPath, maxDimension = 1280)
      ?: return@withContext ProcessedWallpaper(null, null, isTopBright = true)

    val processedHome = renderOptimizedHomeBitmap(sourceBitmap, grayscale)
    if (processedHome !== sourceBitmap) {
      sourceBitmap.recycle()
    }

    val regions = computeRegionLuminance(processedHome)

    val drawerBitmap = if (lowEffectsMode || blurIntensity <= 0.05f) {
      processedHome
    } else {
      val blurRadius = (5 + (blurIntensity * 13f)).toInt().coerceIn(4, 18)
      createBlurredVariant(processedHome, blurRadius)
    }

    val result = ProcessedWallpaper(
      homeBitmap = processedHome.asImageBitmap(),
      drawerBlurredBitmap = drawerBitmap.asImageBitmap(),
      isTopBright = regions.isTopBright,
      isMiddleBright = regions.isMiddleBright,
      isEdgesBright = regions.isEdgesBright
    )
    synchronized(cacheLock) {
      cachedConfigKey = configKey
      cachedResult = result
    }
    result
  }

  private fun loadSourceBitmap(customPath: String?, maxDimension: Int): Bitmap? {
    if (!customPath.isNullOrBlank()) {
      val file = File(customPath)
      if (file.exists()) {
        val decoded = decodeSampledFromFile(file, maxDimension)
        if (decoded != null) return decoded
      }
    }
    return decodeSampledFromResource(R.drawable.img_default_forest_wallpaper, maxDimension)
  }

  private fun decodeSampledFromResource(resId: Int, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, bounds)
    val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
    val opts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeResource(context.resources, resId, opts)
  }

  private fun decodeSampledFromFile(file: File, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
    val opts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeFile(file.absolutePath, opts)
  }

  private fun decodeSampledFromUri(uri: Uri, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
    val opts = BitmapFactory.Options().apply {
      inSampleSize = sampleSize
      inPreferredConfig = Bitmap.Config.RGB_565
    }
    return context.contentResolver.openInputStream(uri)?.use { stream ->
      BitmapFactory.decodeStream(stream, null, opts)
    }
  }

  private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var inSampleSize = 1
    val largest = max(width, height)
    while (largest / inSampleSize > maxDimension) {
      inSampleSize *= 2
    }
    return inSampleSize.coerceAtLeast(1)
  }

  private fun renderOptimizedHomeBitmap(source: Bitmap, grayscale: Boolean): Bitmap {
    if (!grayscale && source.config == Bitmap.Config.RGB_565) return source
    val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.RGB_565)
    val canvas = Canvas(out)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    if (grayscale) {
      val matrix = ColorMatrix().apply { setSaturation(0f) }
      paint.colorFilter = ColorMatrixColorFilter(matrix)
    }
    canvas.drawBitmap(source, 0f, 0f, paint)
    return out
  }

  private data class RegionBrightness(
    val isTopBright: Boolean,
    val isMiddleBright: Boolean,
    val isEdgesBright: Boolean
  )

  private fun computeRegionLuminance(bitmap: Bitmap): RegionBrightness {
    val w = bitmap.width.coerceAtLeast(1)
    val h = bitmap.height.coerceAtLeast(1)
    val stepX = max(1, w / 12)
    val stepY = max(1, h / 12)

    var topSum = 0.0
    var topCount = 0
    var midSum = 0.0
    var midCount = 0
    var edgeSum = 0.0
    var edgeCount = 0

    for (y in 0 until h step stepY) {
      val yFrac = y.toFloat() / h.toFloat()
      for (x in 0 until w step stepX) {
        val xFrac = x.toFloat() / w.toFloat()
        val px = bitmap.getPixel(x, y)
        val lum = (0.299 * Color.red(px) + 0.587 * Color.green(px) + 0.114 * Color.blue(px)) / 255.0

        if (yFrac <= 0.26f && xFrac <= 0.70f) {
          topSum += lum
          topCount++
        }
        if (yFrac in 0.25f..0.82f && xFrac <= 0.45f) {
          midSum += lum
          midCount++
        }
        if (yFrac in 0.20f..0.85f && (xFrac <= 0.14f || xFrac >= 0.86f)) {
          edgeSum += lum
          edgeCount++
        }
      }
    }

    val topAvg = if (topCount > 0) topSum / topCount else 0.5
    val midAvg = if (midCount > 0) midSum / midCount else 0.4
    val edgeAvg = if (edgeCount > 0) edgeSum / edgeCount else 0.4

    return RegionBrightness(
      isTopBright = topAvg > 0.58,
      isMiddleBright = midAvg > 0.56,
      isEdgesBright = edgeAvg > 0.54
    )
  }

  private fun createBlurredVariant(source: Bitmap, radius: Int): Bitmap {
    val targetWidth = 180
    val aspect = source.height.toFloat() / source.width.toFloat().coerceAtLeast(1f)
    val targetHeight = (targetWidth * aspect).toInt().coerceIn(100, 380)
    val small = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    return fastBoxBlur(small, radius.coerceIn(2, 18))
  }

  private fun fastBoxBlur(bitmap: Bitmap, radius: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val temp = IntArray(w * h)

    repeat(2) {
      for (y in 0 until h) {
        var rSum = 0
        var gSum = 0
        var bSum = 0
        val rowOffset = y * w
        val window = radius * 2 + 1
        for (dx in -radius..radius) {
          val px = pixels[rowOffset + dx.coerceIn(0, w - 1)]
          rSum += (px ushr 16) and 0xFF
          gSum += (px ushr 8) and 0xFF
          bSum += px and 0xFF
        }
        for (x in 0 until w) {
          temp[rowOffset + x] = (0xFF shl 24) or
            ((rSum / window) shl 16) or
            ((gSum / window) shl 8) or
            (bSum / window)
          val addX = (x + radius + 1).coerceIn(0, w - 1)
          val subX = (x - radius).coerceIn(0, w - 1)
          val addPx = pixels[rowOffset + addX]
          val subPx = pixels[rowOffset + subX]
          rSum += ((addPx ushr 16) and 0xFF) - ((subPx ushr 16) and 0xFF)
          gSum += ((addPx ushr 8) and 0xFF) - ((subPx ushr 8) and 0xFF)
          bSum += (addPx and 0xFF) - (subPx and 0xFF)
        }
      }
      for (x in 0 until w) {
        var rSum = 0
        var gSum = 0
        var bSum = 0
        val window = radius * 2 + 1
        for (dy in -radius..radius) {
          val px = temp[dy.coerceIn(0, h - 1) * w + x]
          rSum += (px ushr 16) and 0xFF
          gSum += (px ushr 8) and 0xFF
          bSum += px and 0xFF
        }
        for (y in 0 until h) {
          pixels[y * w + x] = (0xFF shl 24) or
            ((rSum / window) shl 16) or
            ((gSum / window) shl 8) or
            (bSum / window)
          val addY = (y + radius + 1).coerceIn(0, h - 1)
          val subY = (y - radius).coerceIn(0, h - 1)
          val addPx = temp[addY * w + x]
          val subPx = temp[subY * w + x]
          rSum += ((addPx ushr 16) and 0xFF) - ((subPx ushr 16) and 0xFF)
          gSum += ((addPx ushr 8) and 0xFF) - ((subPx ushr 8) and 0xFF)
          bSum += (addPx and 0xFF) - (subPx and 0xFF)
        }
      }
    }

    val result = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
    result.setPixels(pixels, 0, w, 0, 0, w, h)
    if (bitmap !== result) bitmap.recycle()
    return result
  }

  private companion object {
    val cacheLock = Any()
    var cachedConfigKey: String? = null
    var cachedResult: ProcessedWallpaper? = null
  }
}
