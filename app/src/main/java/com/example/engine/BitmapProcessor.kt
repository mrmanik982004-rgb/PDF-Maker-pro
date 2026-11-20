package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.model.ImageEditConfig
import com.example.model.PhotoFilter
import com.example.model.QuadPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BitmapProcessor {

    suspend fun copyUriToDraftFile(
        context: Context,
        uri: Uri,
        draftsDir: File,
        maxDimension: Int = 2600,
    ): File = withContext(Dispatchers.IO) {
        if (!draftsDir.exists()) draftsDir.mkdirs()
        val decoded = decodeSampledBitmapFromUri(context, uri, maxDimension)
            ?: throw IllegalStateException("Unable to decode selected image")
        val outFile = File(draftsDir, "draft_${UUID.randomUUID()}.jpg")
        FileOutputStream(outFile).use { fos ->
            decoded.compress(Bitmap.CompressFormat.JPEG, 94, fos)
        }
        decoded.recycle()
        outFile
    }

    fun decodeSampledBitmapFromFile(filePath: String, maxDimension: Int): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeFile(filePath, opts) ?: return null
        return scaleDownIfNeeded(raw, maxDimension)
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val exifRotation = readExifRotation(context, uri)
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        } ?: return null

        val oriented = if (exifRotation != 0) {
            val matrix = Matrix().apply { postRotate(exifRotation.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }
        return scaleDownIfNeeded(oriented, maxDimension)
    }

    fun fixFileExifOrientation(filePath: String) {
        val exif = runCatching { ExifInterface(filePath) }.getOrNull() ?: return
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        if (degrees == 0) return
        val bitmap = decodeSampledBitmapFromFile(filePath, 2800) ?: return
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        FileOutputStream(filePath).use { fos ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 94, fos)
        }
        rotated.recycle()
    }

    private fun readExifRotation(context: Context, uri: Uri): Int {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }.getOrDefault(0)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        val maxSide = max(width, height)
        while ((maxSide / inSampleSize) > maxDim * 1.4f) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val maxSide = max(w, h)
        if (maxSide <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        val targetW = max(1, (w * ratio).roundToInt())
        val targetH = max(1, (h * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    /**
     * Heuristic document boundary detection on a downscaled grayscale edge map.
     * Detects bright/high-contrast document boundaries relative to borders, returning
     * normalized QuadPoints in [0f..1f].
     */
    fun detectDocumentQuad(bitmap: Bitmap): QuadPoints {
        val analysisSize = 180
        val scaled = Bitmap.createScaledBitmap(bitmap, analysisSize, analysisSize, true)
        val pixels = IntArray(analysisSize * analysisSize)
        scaled.getPixels(pixels, 0, analysisSize, 0, 0, analysisSize, analysisSize)
        if (scaled !== bitmap) scaled.recycle()

        val luma = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val c = pixels[i]
            luma[i] = 0.299f * Color.red(c) + 0.587f * Color.green(c) + 0.114f * Color.blue(c)
        }

        // Compute average border luminance vs center luminance
        var centerSum = 0f
        var centerCount = 0
        for (y in 45 until 135) {
            for (x in 45 until 135) {
                centerSum += luma[y * analysisSize + x]
                centerCount++
            }
        }
        val centerAvg = centerSum / max(1, centerCount)

        // Scan inwards from each edge along multiple rays to find strong gradient or paper edge
        val edgeGradThreshold = 26f
        fun findLeftEdge(yStart: Int, yEnd: Int): Float {
            val hits = mutableListOf<Int>()
            for (y in yStart until yEnd step 6) {
                for (x in 6 until analysisSize / 2) {
                    val g = abs(luma[y * analysisSize + x + 2] - luma[y * analysisSize + x - 2])
                    val nearPaper = abs(luma[y * analysisSize + x + 4] - centerAvg) < 55f
                    if (g > edgeGradThreshold && nearPaper) {
                        hits.add(x)
                        break
                    }
                }
            }
            return if (hits.size >= 3) {
                hits.sorted()[hits.size / 2] / analysisSize.toFloat()
            } else 0.07f
        }

        fun findRightEdge(yStart: Int, yEnd: Int): Float {
            val hits = mutableListOf<Int>()
            for (y in yStart until yEnd step 6) {
                for (x in (analysisSize - 7) downTo analysisSize / 2) {
                    val g = abs(luma[y * analysisSize + x - 2] - luma[y * analysisSize + x + 2])
                    val nearPaper = abs(luma[y * analysisSize + x - 4] - centerAvg) < 55f
                    if (g > edgeGradThreshold && nearPaper) {
                        hits.add(x)
                        break
                    }
                }
            }
            return if (hits.size >= 3) {
                hits.sorted()[hits.size / 2] / analysisSize.toFloat()
            } else 0.93f
        }

        fun findTopEdge(xStart: Int, xEnd: Int): Float {
            val hits = mutableListOf<Int>()
            for (x in xStart until xEnd step 6) {
                for (y in 6 until analysisSize / 2) {
                    val g = abs(luma[(y + 2) * analysisSize + x] - luma[(y - 2) * analysisSize + x])
                    val nearPaper = abs(luma[(y + 4) * analysisSize + x] - centerAvg) < 55f
                    if (g > edgeGradThreshold && nearPaper) {
                        hits.add(y)
                        break
                    }
                }
            }
            return if (hits.size >= 3) {
                hits.sorted()[hits.size / 2] / analysisSize.toFloat()
            } else 0.07f
        }

        fun findBottomEdge(xStart: Int, xEnd: Int): Float {
            val hits = mutableListOf<Int>()
            for (x in xStart until xEnd step 6) {
                for (y in (analysisSize - 7) downTo analysisSize / 2) {
                    val g = abs(luma[(y - 2) * analysisSize + x] - luma[(y + 2) * analysisSize + x])
                    val nearPaper = abs(luma[(y - 4) * analysisSize + x] - centerAvg) < 55f
                    if (g > edgeGradThreshold && nearPaper) {
                        hits.add(y)
                        break
                    }
                }
            }
            return if (hits.size >= 3) {
                hits.sorted()[hits.size / 2] / analysisSize.toFloat()
            } else 0.93f
        }

        val tlX = findLeftEdge(18, analysisSize / 2).coerceIn(0.02f, 0.42f)
        val blX = findLeftEdge(analysisSize / 2, analysisSize - 18).coerceIn(0.02f, 0.42f)
        val trX = findRightEdge(18, analysisSize / 2).coerceIn(0.58f, 0.98f)
        val brX = findRightEdge(analysisSize / 2, analysisSize - 18).coerceIn(0.58f, 0.98f)

        val tlY = findTopEdge(18, analysisSize / 2).coerceIn(0.02f, 0.42f)
        val trY = findTopEdge(analysisSize / 2, analysisSize - 18).coerceIn(0.02f, 0.42f)
        val blY = findBottomEdge(18, analysisSize / 2).coerceIn(0.58f, 0.98f)
        val brY = findBottomEdge(analysisSize / 2, analysisSize - 18).coerceIn(0.58f, 0.98f)

        return QuadPoints(
            topLeft = tlX to tlY,
            topRight = trX to trY,
            bottomRight = brX to brY,
            bottomLeft = blX to blY,
        )
    }

    /**
     * Performs 4-corner perspective correction warp using Matrix.setPolyToPoly.
     */
    fun applyPerspectiveWarp(source: Bitmap, quad: QuadPoints): Bitmap {
        val w = source.width.toFloat()
        val h = source.height.toFloat()

        val src = floatArrayOf(
            quad.topLeft.first * w, quad.topLeft.second * h,
            quad.topRight.first * w, quad.topRight.second * h,
            quad.bottomRight.first * w, quad.bottomRight.second * h,
            quad.bottomLeft.first * w, quad.bottomLeft.second * h,
        )

        val topWidth = hypot((src[2] - src[0]).toDouble(), (src[3] - src[1]).toDouble())
        val bottomWidth = hypot((src[4] - src[6]).toDouble(), (src[5] - src[7]).toDouble())
        val leftHeight = hypot((src[6] - src[0]).toDouble(), (src[7] - src[1]).toDouble())
        val rightHeight = hypot((src[4] - src[2]).toDouble(), (src[5] - src[3]).toDouble())

        val dstW = max(64, ((topWidth + bottomWidth) / 2.0).roundToInt())
        val dstH = max(64, ((leftHeight + rightHeight) / 2.0).roundToInt())

        val dst = floatArrayOf(
            0f, 0f,
            dstW.toFloat(), 0f,
            dstW.toFloat(), dstH.toFloat(),
            0f, dstH.toFloat(),
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)

        val output = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    /**
     * Applies crop, rotation, filter preset, brightness, contrast, and saturation to a bitmap.
     */
    fun applyEdits(source: Bitmap, config: ImageEditConfig): Bitmap {
        // 1. Crop
        var current = if (!config.cropRect.isFull) {
            val left = (config.cropRect.left * source.width).roundToInt().coerceIn(0, source.width - 2)
            val top = (config.cropRect.top * source.height).roundToInt().coerceIn(0, source.height - 2)
            val right = (config.cropRect.right * source.width).roundToInt().coerceIn(left + 2, source.width)
            val bottom = (config.cropRect.bottom * source.height).roundToInt().coerceIn(top + 2, source.height)
            Bitmap.createBitmap(source, left, top, right - left, bottom - top)
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        // 2. Rotate
        val rot = ((config.rotationDegrees % 360) + 360) % 360
        if (rot != 0) {
            val matrix = Matrix().apply { postRotate(rot.toFloat()) }
            val rotated = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
            if (rotated !== current) current.recycle()
            current = rotated
        }

        // 3. Apply filter-specific pixel ops or ColorMatrix
        val filtered = when (config.filter) {
            PhotoFilter.BLACK_AND_WHITE -> applyAdaptiveThresholdBw(current)
            PhotoFilter.SHARPEN -> applySharpenConvolution(current, strength = 1.15f)
            PhotoFilter.CLEAN_SCAN -> applyCleanScan(current)
            PhotoFilter.DOCUMENT -> applyDocumentEnhance(current)
            PhotoFilter.AUTO_ENHANCE -> applyAutoEnhance(current)
            else -> current.copy(Bitmap.Config.ARGB_8888, true)
        }
        if (filtered !== current) current.recycle()
        current = filtered

        // 4. Apply ColorMatrix for preset + user brightness/contrast/saturation adjustments
        val cm = ColorMatrix()

        when (config.filter) {
            PhotoFilter.GRAYSCALE -> {
                val gray = ColorMatrix().apply { setSaturation(0f) }
                cm.postConcat(gray)
            }
            PhotoFilter.BRIGHTNESS -> {
                cm.postConcat(brightnessContrastMatrix(brightness = 0.16f, contrast = 1.05f))
            }
            PhotoFilter.CONTRAST -> {
                cm.postConcat(brightnessContrastMatrix(brightness = 0.0f, contrast = 1.38f))
            }
            PhotoFilter.COLOR_ENHANCE -> {
                val sat = ColorMatrix().apply { setSaturation(1.45f) }
                cm.postConcat(sat)
                cm.postConcat(brightnessContrastMatrix(brightness = 0.03f, contrast = 1.12f))
            }
            else -> Unit
        }

        // Apply user sliders on top
        if (abs(config.saturation - 1f) > 0.01f) {
            val sat = ColorMatrix().apply { setSaturation(config.saturation) }
            cm.postConcat(sat)
        }
        if (abs(config.brightness) > 0.01f || abs(config.contrast - 1f) > 0.01f) {
            cm.postConcat(brightnessContrastMatrix(config.brightness, config.contrast))
        }

        val finalOut = Bitmap.createBitmap(current.width, current.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalOut)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(current, 0f, 0f, paint)
        current.recycle()
        return finalOut
    }

    private fun brightnessContrastMatrix(brightness: Float, contrast: Float): ColorMatrix {
        val b = brightness * 255f
        val c = contrast
        val t = (1f - c) * 127.5f + b
        return ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }

    private fun applyAdaptiveThresholdBw(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val luma = IntArray(pixels.size)
        var sum = 0L
        for (i in pixels.indices) {
            val c = pixels[i]
            val y = ((299 * Color.red(c) + 587 * Color.green(c) + 114 * Color.blue(c)) / 1000)
            luma[i] = y
            sum += y
        }
        val mean = (sum / max(1, pixels.size)).toInt()
        val threshold = (mean * 0.92f).toInt().coerceIn(85, 185)
        for (i in pixels.indices) {
            pixels[i] = if (luma[i] >= threshold) Color.WHITE else Color.rgb(18, 20, 26)
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun applyDocumentEnhance(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val y = (299 * r + 587 * g + 114 * b) / 1000
            if (y > 165) {
                // Push bright paper background toward crisp white while preserving subtle ink
                val nr = min(255, r + ((255 - r) * 0.72f).roundToInt())
                val ng = min(255, g + ((255 - g) * 0.72f).roundToInt())
                val nb = min(255, b + ((255 - b) * 0.72f).roundToInt())
                pixels[i] = Color.rgb(nr, ng, nb)
            } else {
                // Deepen text/ink contrast
                val nr = max(0, (r * 0.78f).roundToInt())
                val ng = max(0, (g * 0.78f).roundToInt())
                val nb = max(0, (b * 0.78f).roundToInt())
                pixels[i] = Color.rgb(nr, ng, nb)
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun applyCleanScan(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        // Estimate local background illumination on a coarse grid and whiten paper shadows
        val grid = 12
        val cellW = max(1, w / grid)
        val cellH = max(1, h / grid)
        val cellMax = IntArray(grid * grid)
        for (gy in 0 until grid) {
            for (gx in 0 until grid) {
                var localMax = 128
                val y0 = gy * cellH
                val y1 = min(h, (gy + 1) * cellH)
                val x0 = gx * cellW
                val x1 = min(w, (gx + 1) * cellW)
                for (y in y0 until y1 step 3) {
                    for (x in x0 until x1 step 3) {
                        val c = pixels[y * w + x]
                        val yVal = (299 * Color.red(c) + 587 * Color.green(c) + 114 * Color.blue(c)) / 1000
                        if (yVal > localMax) localMax = yVal
                    }
                }
                cellMax[gy * grid + gx] = max(140, localMax)
            }
        }

        for (y in 0 until h) {
            val gy = min(grid - 1, y / cellH)
            for (x in 0 until w) {
                val gx = min(grid - 1, x / cellW)
                val bg = cellMax[gy * grid + gx].toFloat()
                val idx = y * w + x
                val c = pixels[idx]
                val scale = (255f / bg).coerceIn(1f, 1.85f)
                var nr = (Color.red(c) * scale).roundToInt().coerceIn(0, 255)
                var ng = (Color.green(c) * scale).roundToInt().coerceIn(0, 255)
                var nb = (Color.blue(c) * scale).roundToInt().coerceIn(0, 255)
                val avg = (nr + ng + nb) / 3
                if (avg > 215) {
                    nr = 255; ng = 255; nb = 255
                }
                pixels[idx] = Color.rgb(nr, ng, nb)
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun applyAutoEnhance(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val hist = IntArray(256)
        for (i in pixels.indices step 2) {
            val c = pixels[i]
            val y = ((299 * Color.red(c) + 587 * Color.green(c) + 114 * Color.blue(c)) / 1000).coerceIn(0, 255)
            hist[y]++
        }
        val total = max(1, pixels.size / 2)
        val lowCut = (total * 0.02f).toInt()
        val highCut = (total * 0.98f).toInt()
        var acc = 0
        var minY = 0
        var maxY = 255
        for (i in 0..255) {
            acc += hist[i]
            if (acc >= lowCut) { minY = i; break }
        }
        acc = 0
        for (i in 0..255) {
            acc += hist[i]
            if (acc >= highCut) { maxY = i; break }
        }
        val range = max(32, maxY - minY)
        val gain = 255f / range
        for (i in pixels.indices) {
            val c = pixels[i]
            val nr = ((Color.red(c) - minY) * gain).roundToInt().coerceIn(0, 255)
            val ng = ((Color.green(c) - minY) * gain).roundToInt().coerceIn(0, 255)
            val nb = ((Color.blue(c) - minY) * gain).roundToInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(nr, ng, nb)
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun applySharpenConvolution(src: Bitmap, strength: Float): Bitmap {
        val w = src.width
        val h = src.height
        val input = IntArray(w * h)
        val output = IntArray(w * h)
        src.getPixels(input, 0, w, 0, 0, w, h)
        val center = 1f + 4f * strength
        val edge = -strength
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                    output[y * w + x] = input[y * w + x]
                    continue
                }
                val c0 = input[y * w + x]
                val cL = input[y * w + x - 1]
                val cR = input[y * w + x + 1]
                val cT = input[(y - 1) * w + x]
                val cB = input[(y + 1) * w + x]
                val r = (Color.red(c0) * center + (Color.red(cL) + Color.red(cR) + Color.red(cT) + Color.red(cB)) * edge).roundToInt().coerceIn(0, 255)
                val g = (Color.green(c0) * center + (Color.green(cL) + Color.green(cR) + Color.green(cT) + Color.green(cB)) * edge).roundToInt().coerceIn(0, 255)
                val b = (Color.blue(c0) * center + (Color.blue(cL) + Color.blue(cR) + Color.blue(cT) + Color.blue(cB)) * edge).roundToInt().coerceIn(0, 255)
                output[y * w + x] = Color.rgb(r, g, b)
            }
        }
        return Bitmap.createBitmap(output, w, h, Bitmap.Config.ARGB_8888)
    }
}
