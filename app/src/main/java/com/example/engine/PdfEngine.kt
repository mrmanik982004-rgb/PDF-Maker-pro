package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.CompressionLevel
import com.example.model.DraftImageItem
import com.example.model.ImageAlignment
import com.example.model.ImageFitMode
import com.example.model.PageOrientation
import com.example.model.PageSizeOption
import com.example.model.PdfCreateOptions
import com.example.model.PdfPageSettings
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class GeneratedPdfResult(
    val pdfFile: File,
    val thumbnailFile: File?,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val isEncrypted: Boolean,
)

object PdfEngine {

    suspend fun createPdfFromImages(
        items: List<DraftImageItem>,
        options: PdfCreateOptions,
        outputPdfFile: File,
        thumbnailFile: File,
        onProgress: (Float, String) -> Unit,
    ): GeneratedPdfResult = withContext(Dispatchers.IO) {
        require(items.isNotEmpty()) { "At least one image is required to create a PDF." }
        outputPdfFile.parentFile?.mkdirs()
        thumbnailFile.parentFile?.mkdirs()

        val settings = options.pageSettings
        val effectiveMaxEdge = when (options.compressionLevel) {
            CompressionLevel.LOW -> settings.qualityPreset.maxEdgePx
            CompressionLevel.MEDIUM -> min(settings.qualityPreset.maxEdgePx, 1750)
            CompressionLevel.HIGH -> min(settings.qualityPreset.maxEdgePx, 1300)
        }
        val effectiveQuality = when (options.compressionLevel) {
            CompressionLevel.LOW -> settings.qualityPreset.jpegQuality
            CompressionLevel.MEDIUM -> min(settings.qualityPreset.jpegQuality, 72)
            CompressionLevel.HIGH -> min(settings.qualityPreset.jpegQuality, 52)
        } / 100f

        var firstThumbnailSaved = false
        val document = PDDocument(MemoryUsageSetting.setupTempFileOnly())
        try {
            val total = items.size
            items.forEachIndexed { index, item ->
                onProgress(
                    (index.toFloat() / total.toFloat()) * 0.85f,
                    "Processing page ${index + 1} of $total..."
                )

                val decoded = BitmapProcessor.decodeSampledBitmapFromFile(
                    item.workingFilePath,
                    effectiveMaxEdge
                ) ?: throw IllegalStateException("Could not load image ${item.displayName}")

                val processed = BitmapProcessor.applyEdits(decoded, item.editConfig)
                if (processed !== decoded) decoded.recycle()

                val pageRect = resolvePageRect(settings, processed.width, processed.height)
                val page = PDPage(pageRect)
                document.addPage(page)

                val imageXObject = JPEGFactory.createFromImage(document, processed, effectiveQuality)
                val drawRect = computeImageDrawRect(
                    pageWidth = pageRect.width,
                    pageHeight = pageRect.height,
                    imageWidth = processed.width.toFloat(),
                    imageHeight = processed.height.toFloat(),
                    settings = settings,
                )

                if (!firstThumbnailSaved) {
                    saveThumbnailFromBitmap(processed, thumbnailFile)
                    firstThumbnailSaved = true
                }
                processed.recycle()

                PDPageContentStream(document, page).use { cs ->
                    // 1. Page Background Color
                    val bgArgb = settings.backgroundColor.argb
                    cs.setNonStrokingColor(
                        Color.red(bgArgb) / 255f,
                        Color.green(bgArgb) / 255f,
                        Color.blue(bgArgb) / 255f,
                    )
                    cs.addRect(0f, 0f, pageRect.width, pageRect.height)
                    cs.fill()

                    // 2. Draw Image (PDFBox coordinate origin is bottom-left)
                    val pdfY = pageRect.height - drawRect.bottom
                    cs.drawImage(
                        imageXObject,
                        drawRect.left,
                        pdfY,
                        drawRect.width(),
                        drawRect.height(),
                    )

                    // 3. Optional Page Border
                    if (settings.borderEnabled && settings.borderWidthPt > 0f) {
                        val bArgb = settings.borderColorArgb
                        val inset = max(6f, settings.marginPt * 0.5f)
                        cs.setStrokingColor(
                            Color.red(bArgb) / 255f,
                            Color.green(bArgb) / 255f,
                            Color.blue(bArgb) / 255f,
                        )
                        cs.setLineWidth(settings.borderWidthPt)
                        cs.addRect(
                            inset,
                            inset,
                            max(10f, pageRect.width - inset * 2f),
                            max(10f, pageRect.height - inset * 2f),
                        )
                        cs.stroke()
                    }

                    // 4. Optional Page Number
                    if (options.includePageNumbers) {
                        val label = "Page ${index + 1} of $total"
                        val font = PDType1Font.HELVETICA
                        val fontSize = 10f
                        val textWidth = font.getStringWidth(label) / 1000f * fontSize
                        val isDarkPage = settings.backgroundColor == com.example.model.PageBackgroundColor.NAVY_DARK
                        if (isDarkPage) {
                            cs.setNonStrokingColor(0.92f, 0.94f, 0.98f)
                        } else {
                            cs.setNonStrokingColor(0.28f, 0.34f, 0.43f)
                        }
                        cs.beginText()
                        cs.setFont(font, fontSize)
                        cs.newLineAtOffset((pageRect.width - textWidth) / 2f, 14f)
                        cs.showText(label)
                        cs.endText()
                    }
                }
            }

            if (options.passwordEnabled && options.password.isNotBlank()) {
                onProgress(0.9f, "Encrypting PDF...")
                val ap = AccessPermission()
                val spp = StandardProtectionPolicy(options.password, options.password, ap).apply {
                    encryptionKeyLength = 128
                    permissions = ap
                }
                document.protect(spp)
            }

            onProgress(0.95f, "Saving PDF to storage...")
            document.save(outputPdfFile)
            onProgress(1f, "Done!")

            GeneratedPdfResult(
                pdfFile = outputPdfFile,
                thumbnailFile = if (thumbnailFile.exists()) thumbnailFile else null,
                pageCount = total,
                fileSizeBytes = outputPdfFile.length(),
                isEncrypted = options.passwordEnabled && options.password.isNotBlank(),
            )
        } finally {
            document.close()
        }
    }

    fun resolvePageRect(settings: PdfPageSettings, imageW: Int, imageH: Int): PDRectangle {
        val baseRect = when (settings.pageSize) {
            PageSizeOption.A4 -> PDRectangle.A4
            PageSizeOption.A3 -> PDRectangle.A3
            PageSizeOption.A5 -> PDRectangle.A5
            PageSizeOption.LETTER -> PDRectangle.LETTER
            PageSizeOption.LEGAL -> PDRectangle.LEGAL
            PageSizeOption.CUSTOM -> {
                val ptPerMm = 72f / 25.4f
                val w = max(120f, settings.customWidthMm * ptPerMm)
                val h = max(120f, settings.customHeightMm * ptPerMm)
                PDRectangle(w, h)
            }
            PageSizeOption.ORIGINAL -> {
                val safeW = max(100, imageW)
                val safeH = max(100, imageH)
                val scale = 595f / min(safeW, safeH).toFloat()
                return PDRectangle(safeW * scale, safeH * scale)
            }
        }
        val minDim = min(baseRect.width, baseRect.height)
        val maxDim = max(baseRect.width, baseRect.height)
        return when (settings.orientation) {
            PageOrientation.PORTRAIT -> PDRectangle(minDim, maxDim)
            PageOrientation.LANDSCAPE -> PDRectangle(maxDim, minDim)
        }
    }

    fun computeImageDrawRect(
        pageWidth: Float,
        pageHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        settings: PdfPageSettings,
    ): RectF {
        val margin = if (settings.fitMode == ImageFitMode.FULL_PAGE) 0f else settings.marginPt
        val availW = max(20f, pageWidth - margin * 2f)
        val availH = max(20f, pageHeight - margin * 2f)
        val imgAspect = imageWidth / max(1f, imageHeight)
        val availAspect = availW / availH

        val (drawW, drawH) = when (settings.fitMode) {
            ImageFitMode.FULL_PAGE -> {
                pageWidth to pageHeight
            }
            ImageFitMode.FIT_WIDTH -> {
                val w = availW
                val h = w / imgAspect
                if (h <= availH) w to h else (availH * imgAspect) to availH
            }
            ImageFitMode.FIT_HEIGHT -> {
                val h = availH
                val w = h * imgAspect
                if (w <= availW) w to h else availW to (availW / imgAspect)
            }
            ImageFitMode.FIT_PAGE -> {
                if (imgAspect > availAspect) {
                    availW to (availW / imgAspect)
                } else {
                    (availH * imgAspect) to availH
                }
            }
        }

        val left = when (settings.alignment) {
            ImageAlignment.LEFT -> margin
            ImageAlignment.RIGHT -> pageWidth - margin - drawW
            else -> (pageWidth - drawW) / 2f
        }
        val top = when (settings.alignment) {
            ImageAlignment.TOP -> margin
            ImageAlignment.BOTTOM -> pageHeight - margin - drawH
            else -> (pageHeight - drawH) / 2f
        }
        return RectF(left, top, left + drawW, top + drawH)
    }

    suspend fun getPdfMetadata(file: File, password: String = ""): Pair<Int, Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        renderer.pageCount to false
                    }
                }
            }.getOrElse {
                PDDocument.load(file, password).use { doc ->
                    doc.numberOfPages to doc.isEncrypted
                }
            }
        }

    suspend fun generateThumbnailForPdf(
        pdfFile: File,
        thumbnailFile: File,
        password: String = "",
    ): Boolean = withContext(Dispatchers.IO) {
        thumbnailFile.parentFile?.mkdirs()
        renderPdfPage(pdfFile, 0, maxWidthPx = 420, password = password)?.let { bmp ->
            saveThumbnailFromBitmap(bmp, thumbnailFile)
            bmp.recycle()
            true
        } ?: false
    }

    suspend fun renderPdfPage(
        pdfFile: File,
        pageIndex: Int,
        maxWidthPx: Int = 1400,
        password: String = "",
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext null
        // Fast path with Android native PdfRenderer when unencrypted & no password provided
        if (password.isEmpty()) {
            val nativeBmp = runCatching {
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (pageIndex !in 0 until renderer.pageCount) return@use null
                        renderer.openPage(pageIndex).use { page ->
                            val scale = maxWidthPx.toFloat() / max(1, page.width)
                            val w = max(1, (page.width * scale).roundToInt())
                            val h = max(1, (page.height * scale).roundToInt())
                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                    }
                }
            }.getOrNull()
            if (nativeBmp != null) return@withContext nativeBmp
        }

        // Fallback / encrypted path via PDFBox
        runCatching {
            PDDocument.load(pdfFile, password).use { doc ->
                if (pageIndex !in 0 until doc.numberOfPages) return@use null
                val pdfRenderer = PDFRenderer(doc)
                val page = doc.getPage(pageIndex)
                val dpi = (maxWidthPx / max(1f, page.mediaBox.width / 72f)).coerceIn(72f, 240f)
                pdfRenderer.renderImageWithDPI(pageIndex, dpi)
            }
        }.getOrNull()
    }

    suspend fun mergePdfs(
        inputFiles: List<File>,
        outputFile: File,
        onProgress: (Float) -> Unit,
    ): GeneratedPdfResult = withContext(Dispatchers.IO) {
        require(inputFiles.size >= 2) { "Select at least 2 PDFs to merge." }
        outputFile.parentFile?.mkdirs()
        onProgress(0.2f)
        val merger = PDFMergerUtility()
        merger.destinationFileName = outputFile.absolutePath
        inputFiles.forEach { merger.addSource(it) }
        onProgress(0.55f)
        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
        onProgress(0.9f)
        val (pages, encrypted) = getPdfMetadata(outputFile)
        val thumb = File(outputFile.parentFile?.parentFile, "thumbnails/${outputFile.nameWithoutExtension}.jpg")
        generateThumbnailForPdf(outputFile, thumb)
        onProgress(1f)
        GeneratedPdfResult(outputFile, thumb.takeIf { it.exists() }, pages, outputFile.length(), encrypted)
    }

    enum class SplitMode { RANGE, EVERY_N_PAGES, ALL_PAGES }

    suspend fun splitPdf(
        sourceFile: File,
        mode: SplitMode,
        startPage: Int,
        endPage: Int,
        pagesPerSplit: Int,
        outputDir: File,
        baseName: String,
    ): List<GeneratedPdfResult> = withContext(Dispatchers.IO) {
        outputDir.mkdirs()
        val results = mutableListOf<GeneratedPdfResult>()
        PDDocument.load(sourceFile).use { sourceDoc ->
            val total = sourceDoc.numberOfPages
            when (mode) {
                SplitMode.RANGE -> {
                    val s = startPage.coerceIn(1, total)
                    val e = endPage.coerceIn(s, total)
                    val splitter = Splitter().apply {
                        setStartPage(s)
                        setEndPage(e)
                        setSplitAtPage(e - s + 1)
                    }
                    val parts = splitter.split(sourceDoc)
                    parts.forEachIndexed { idx, part ->
                        part.use { doc ->
                            val outFile = uniqueFile(outputDir, "${baseName}_p${s}_${e}_${idx + 1}.pdf")
                            doc.save(outFile)
                            val thumb = File(outputDir.parentFile, "thumbnails/${outFile.nameWithoutExtension}.jpg")
                            generateThumbnailForPdf(outFile, thumb)
                            results += GeneratedPdfResult(outFile, thumb.takeIf { it.exists() }, doc.numberOfPages, outFile.length(), false)
                        }
                    }
                }
                SplitMode.EVERY_N_PAGES -> {
                    val step = pagesPerSplit.coerceIn(1, total)
                    val splitter = Splitter().apply { setSplitAtPage(step) }
                    val parts = splitter.split(sourceDoc)
                    parts.forEachIndexed { idx, part ->
                        part.use { doc ->
                            val outFile = uniqueFile(outputDir, "${baseName}_part_${idx + 1}.pdf")
                            doc.save(outFile)
                            val thumb = File(outputDir.parentFile, "thumbnails/${outFile.nameWithoutExtension}.jpg")
                            generateThumbnailForPdf(outFile, thumb)
                            results += GeneratedPdfResult(outFile, thumb.takeIf { it.exists() }, doc.numberOfPages, outFile.length(), false)
                        }
                    }
                }
                SplitMode.ALL_PAGES -> {
                    val splitter = Splitter().apply { setSplitAtPage(1) }
                    val parts = splitter.split(sourceDoc)
                    parts.forEachIndexed { idx, part ->
                        part.use { doc ->
                            val outFile = uniqueFile(outputDir, "${baseName}_page_${idx + 1}.pdf")
                            doc.save(outFile)
                            val thumb = File(outputDir.parentFile, "thumbnails/${outFile.nameWithoutExtension}.jpg")
                            generateThumbnailForPdf(outFile, thumb)
                            results += GeneratedPdfResult(outFile, thumb.takeIf { it.exists() }, 1, outFile.length(), false)
                        }
                    }
                }
            }
        }
        results
    }

    data class PageTransformSpec(val originalIndex: Int, val extraRotationDeg: Int = 0)

    suspend fun reorderAndRotatePdf(
        sourceFile: File,
        specs: List<PageTransformSpec>,
        outputFile: File,
    ): GeneratedPdfResult = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        PDDocument.load(sourceFile).use { src ->
            PDDocument(MemoryUsageSetting.setupTempFileOnly()).use { dst ->
                specs.forEach { spec ->
                    if (spec.originalIndex in 0 until src.numberOfPages) {
                        val imported = dst.importPage(src.getPage(spec.originalIndex))
                        val newRot = (imported.rotation + spec.extraRotationDeg) % 360
                        imported.rotation = (newRot + 360) % 360
                    }
                }
                dst.save(outputFile)
            }
        }
        val (pages, encrypted) = getPdfMetadata(outputFile)
        val thumb = File(outputFile.parentFile?.parentFile, "thumbnails/${outputFile.nameWithoutExtension}.jpg")
        generateThumbnailForPdf(outputFile, thumb)
        GeneratedPdfResult(outputFile, thumb.takeIf { it.exists() }, pages, outputFile.length(), encrypted)
    }

    suspend fun compressPdf(
        sourceFile: File,
        level: CompressionLevel,
        outputFile: File,
        onProgress: (Float) -> Unit,
    ): GeneratedPdfResult = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        PDDocument.load(sourceFile).use { src ->
            val renderer = PDFRenderer(src)
            val total = src.numberOfPages
            PDDocument(MemoryUsageSetting.setupTempFileOnly()).use { dst ->
                for (i in 0 until total) {
                    onProgress((i.toFloat() / max(1, total)) * 0.9f)
                    val srcPage = src.getPage(i)
                    val box = srcPage.mediaBox
                    val bmp = renderer.renderImageWithDPI(i, level.renderDpi)
                    val newPage = PDPage(box)
                    dst.addPage(newPage)
                    val imgX = JPEGFactory.createFromImage(dst, bmp, level.jpegQuality / 100f)
                    bmp.recycle()
                    PDPageContentStream(dst, newPage).use { cs ->
                        cs.drawImage(imgX, 0f, 0f, box.width, box.height)
                    }
                }
                dst.save(outputFile)
            }
        }
        onProgress(0.95f)
        val (pages, encrypted) = getPdfMetadata(outputFile)
        val thumb = File(outputFile.parentFile?.parentFile, "thumbnails/${outputFile.nameWithoutExtension}.jpg")
        generateThumbnailForPdf(outputFile, thumb)
        onProgress(1f)
        GeneratedPdfResult(outputFile, thumb.takeIf { it.exists() }, pages, outputFile.length(), encrypted)
    }

    suspend fun exportPdfToImages(
        context: Context,
        pdfFile: File,
        baseName: String,
        password: String = "",
        onProgress: (Float) -> Unit,
    ): List<Uri> = withContext(Dispatchers.IO) {
        val (pageCount, _) = getPdfMetadata(pdfFile, password)
        val savedUris = mutableListOf<Uri>()
        for (i in 0 until pageCount) {
            onProgress((i.toFloat() / max(1, pageCount)))
            val bmp = renderPdfPage(pdfFile, i, maxWidthPx = 1800, password = password) ?: continue
            val fileName = "${baseName}_page_${i + 1}.png"
            val uri = saveImageToGalleryOrAppExports(context, bmp, fileName)
            bmp.recycle()
            if (uri != null) savedUris += uri
        }
        onProgress(1f)
        savedUris
    }

    private fun saveImageToGalleryOrAppExports(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PDFMaker")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } else {
            val dir = File(context.filesDir, "exports").apply { mkdirs() }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        }
    }

    fun saveThumbnailFromBitmap(bitmap: Bitmap, targetFile: File) {
        targetFile.parentFile?.mkdirs()
        val maxSide = 360f
        val scale = min(maxSide / max(1, bitmap.width), maxSide / max(1, bitmap.height)).coerceAtMost(1f)
        val w = max(1, (bitmap.width * scale).roundToInt())
        val h = max(1, (bitmap.height * scale).roundToInt())
        val thumb = Bitmap.createScaledBitmap(bitmap, w, h, true)
        FileOutputStream(targetFile).use { fos ->
            thumb.compress(Bitmap.CompressFormat.JPEG, 82, fos)
        }
        if (thumb !== bitmap) thumb.recycle()
    }

    fun uniqueFile(dir: File, desiredName: String): File {
        val cleanName = desiredName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        var candidate = File(dir, cleanName)
        if (!candidate.exists()) return candidate
        val stem = candidate.nameWithoutExtension
        val ext = candidate.extension
        var counter = 1
        while (candidate.exists()) {
            val suffix = if (ext.isNotEmpty()) ".$ext" else ""
            candidate = File(dir, "${stem}_$counter$suffix")
            counter++
        }
        return candidate
    }

    fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }

    fun printPdfFile(context: Context, file: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(file.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback,
            ) {
                try {
                    FileInputStream(file).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(file.nameWithoutExtension, adapter, null)
    }

    suspend fun savePdfToDownloads(context: Context, sourceFile: File): Uri? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/PDFMaker")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            null
        }
    }
}
