package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.engine.BitmapProcessor
import com.example.engine.PdfEngine
import com.example.model.CompressionLevel
import com.example.model.DraftImageItem
import com.example.model.ImageEditConfig
import com.example.model.ImageFitMode
import com.example.model.NormalizedCropRect
import com.example.model.PageOrientation
import com.example.model.PageSizeOption
import com.example.model.PdfCreateOptions
import com.example.model.PdfPageSettings
import com.example.model.PhotoFilter
import com.example.model.QuadPoints
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.theme.PdfMakerTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var tempDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(context)
        tempDir = File(context.cacheDir, "test_pdf_work").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    private fun createSampleImageFile(name: String, width: Int = 300, height: Int = 400): File {
        val file = File(tempDir, name)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        for (y in 40 until 120) {
            for (x in 30 until width - 30) {
                bmp.setPixel(x, y, Color.rgb(15, 23, 42))
            }
        }
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bmp.recycle()
        return file
    }

    @Test
    fun appName_matchesPdfMaker() {
        assertEquals("PDF Maker", context.getString(R.string.app_name))
    }

    @Test
    fun allTenPhotoFilters_andPerspectiveWarp_succeed() {
        val bmp = Bitmap.createBitmap(180, 220, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(240, 240, 240))

        PhotoFilter.entries.forEach { filter ->
            val out = BitmapProcessor.applyEdits(
                bmp,
                ImageEditConfig(
                    filter = filter,
                    brightness = 0.1f,
                    contrast = 1.15f,
                    saturation = 1.1f,
                    rotationDegrees = 90,
                    cropRect = NormalizedCropRect(0.05f, 0.05f, 0.95f, 0.95f),
                ),
            )
            assertTrue(out.width > 0 && out.height > 0)
            out.recycle()
        }

        val quad = BitmapProcessor.detectDocumentQuad(bmp)
        assertTrue(quad.topLeft.first < quad.topRight.first)
        val warped = BitmapProcessor.applyPerspectiveWarp(bmp, QuadPoints())
        assertTrue(warped.width >= 64 && warped.height >= 64)
        warped.recycle()
        bmp.recycle()
    }

    @Test
    fun pdfCreation_encryption_merge_split_rotate_compress_workEndToEnd() = runBlocking {
        val img1 = createSampleImageFile("page1.jpg")
        val img2 = createSampleImageFile("page2.jpg")
        val drafts = listOf(
            DraftImageItem(workingFilePath = img1.absolutePath, displayName = "Page 1"),
            DraftImageItem(workingFilePath = img2.absolutePath, displayName = "Page 2"),
        )

        val settings = PdfPageSettings(
            pageSize = PageSizeOption.A4,
            orientation = PageOrientation.PORTRAIT,
            marginPt = 20f,
            fitMode = ImageFitMode.FIT_PAGE,
            borderEnabled = true,
        )

        val pdfA = File(tempDir, "docA.pdf")
        val thumbA = File(tempDir, "thumbA.jpg")
        val resA = PdfEngine.createPdfFromImages(
            items = drafts,
            options = PdfCreateOptions(
                fileName = "docA",
                pageSettings = settings,
                compressionLevel = CompressionLevel.LOW,
                passwordEnabled = false,
                includePageNumbers = true,
            ),
            outputPdfFile = pdfA,
            thumbnailFile = thumbA,
        ) { _, _ -> }

        assertTrue(resA.pdfFile.exists() && resA.fileSizeBytes > 500)
        assertEquals(2, resA.pageCount)

        // Password protected PDF
        val pdfEncrypted = File(tempDir, "secret.pdf")
        val resEnc = PdfEngine.createPdfFromImages(
            items = drafts.take(1),
            options = PdfCreateOptions(
                fileName = "secret",
                pageSettings = settings,
                passwordEnabled = true,
                password = "mySecret123",
            ),
            outputPdfFile = pdfEncrypted,
            thumbnailFile = File(tempDir, "thumbEnc.jpg"),
        ) { _, _ -> }
        assertTrue(resEnc.isEncrypted)
        val (encPages, isEnc) = PdfEngine.getPdfMetadata(pdfEncrypted, "mySecret123")
        assertEquals(1, encPages)
        assertTrue(isEnc)

        // Merge PDFs
        val pdfB = File(tempDir, "docB.pdf")
        PdfEngine.createPdfFromImages(
            items = drafts.take(1),
            options = PdfCreateOptions(fileName = "docB", pageSettings = settings),
            outputPdfFile = pdfB,
            thumbnailFile = File(tempDir, "thumbB.jpg"),
        ) { _, _ -> }

        val mergedFile = File(tempDir, "merged.pdf")
        val mergedRes = PdfEngine.mergePdfs(listOf(pdfA, pdfB), mergedFile) {}
        assertEquals(3, mergedRes.pageCount)

        // Split PDF
        val splitOutDir = File(tempDir, "splits")
        val splitParts = PdfEngine.splitPdf(
            sourceFile = mergedFile,
            mode = PdfEngine.SplitMode.ALL_PAGES,
            startPage = 1,
            endPage = 3,
            pagesPerSplit = 1,
            outputDir = splitOutDir,
            baseName = "split",
        )
        assertEquals(3, splitParts.size)

        // Reorder & Rotate PDF
        val rotatedFile = File(tempDir, "rotated.pdf")
        val rotatedRes = PdfEngine.reorderAndRotatePdf(
            sourceFile = pdfA,
            specs = listOf(
                PdfEngine.PageTransformSpec(1, 90),
                PdfEngine.PageTransformSpec(0, 180),
            ),
            outputFile = rotatedFile,
        )
        assertEquals(2, rotatedRes.pageCount)

        // Compress PDF
        val compressedFile = File(tempDir, "compressed.pdf")
        val compRes = PdfEngine.compressPdf(pdfA, CompressionLevel.HIGH, compressedFile) {}
        assertEquals(2, compRes.pageCount)
        assertTrue(compRes.fileSizeBytes > 0)
    }

    @Test
    fun homeDashboard_displaysAllPrimaryTiles() {
        composeRule.setContent {
            PdfMakerTheme {
                HomeDashboardScreen(
                    totalPdfCount = 7,
                    draftCount = 0,
                    recentDocuments = emptyList(),
                    onImageToPdfClick = {},
                    onCameraToPdfClick = {},
                    onMyPdfsClick = {},
                    onRecentFilesClick = {},
                    onMergePdfClick = {},
                    onSettingsClick = {},
                    onResumeDraftClick = {},
                    onOpenDocument = {},
                    onShareDocument = {},
                    onMoreDocument = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("total_pdfs_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("tile_image_to_pdf").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("tile_camera_to_pdf").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("tile_my_pdfs").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("tile_recent_files").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("tile_merge_pdf").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("tile_settings").performScrollTo().assertIsDisplayed()
    }
}
