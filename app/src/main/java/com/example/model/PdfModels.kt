package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode"),
}

enum class SortOption(val label: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    NAME_ASC("Name (A–Z)"),
    FILE_SIZE_DESC("Largest Size"),
}

enum class ViewMode(val label: String) {
    LIST("List View"),
    GRID("Grid View"),
}

enum class PhotoFilter(val displayName: String) {
    ORIGINAL("Original"),
    BLACK_AND_WHITE("Black & White"),
    GRAYSCALE("Grayscale"),
    DOCUMENT("Document"),
    AUTO_ENHANCE("Auto Enhance"),
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    SHARPEN("Sharpen"),
    COLOR_ENHANCE("Color Enhance"),
    CLEAN_SCAN("Clean Scan"),
}

data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    val isFull: Boolean
        get() = left <= 0.005f && top <= 0.005f && right >= 0.995f && bottom >= 0.995f
}

data class ImageEditConfig(
    val filter: PhotoFilter = PhotoFilter.ORIGINAL,
    val brightness: Float = 0f,       // -1.0 .. +1.0
    val contrast: Float = 1f,         // 0.5 .. 2.0
    val saturation: Float = 1f,       // 0.0 .. 2.0
    val rotationDegrees: Int = 0,     // 0, 90, 180, 270
    val cropRect: NormalizedCropRect = NormalizedCropRect(),
)

data class DraftImageItem(
    val id: String = UUID.randomUUID().toString(),
    val workingFilePath: String,
    val displayName: String,
    val width: Int = 0,
    val height: Int = 0,
    val editConfig: ImageEditConfig = ImageEditConfig(),
    val previewRevision: Int = 0,
)

enum class PageSizeOption(
    val label: String,
    val widthPt: Float,
    val heightPt: Float,
    val subtitle: String,
) {
    A4("A4", 595f, 842f, "210 × 297 mm"),
    A3("A3", 842f, 1191f, "297 × 420 mm"),
    A5("A5", 420f, 595f, "148 × 210 mm"),
    LETTER("Letter", 612f, 792f, "8.5 × 11 in"),
    LEGAL("Legal", 612f, 1008f, "8.5 × 14 in"),
    ORIGINAL("Original Image", 0f, 0f, "Match photo ratio"),
    CUSTOM("Custom Size", 595f, 842f, "Set mm dimensions"),
}

enum class PageOrientation(val label: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
}

enum class ImageFitMode(val label: String) {
    FIT_PAGE("Fit to Page"),
    FIT_WIDTH("Fit to Width"),
    FIT_HEIGHT("Fit to Height"),
    FULL_PAGE("Full Page"),
}

enum class ImageAlignment(val label: String) {
    CENTER("Center"),
    TOP("Top"),
    BOTTOM("Bottom"),
    LEFT("Left"),
    RIGHT("Right"),
}

enum class PageBackgroundColor(val label: String, val argb: Int, val composeColor: Color) {
    WHITE("Pure White", android.graphics.Color.WHITE, Color(0xFFFFFFFF)),
    IVORY("Warm Ivory", android.graphics.Color.rgb(253, 251, 243), Color(0xFFFDFBF3)),
    SOFT_GRAY("Cool Paper", android.graphics.Color.rgb(241, 245, 249), Color(0xFFF1F5F9)),
    NAVY_DARK("Midnight Navy", android.graphics.Color.rgb(15, 23, 42), Color(0xFF0F172A)),
}

enum class ImageQualityPreset(val label: String, val jpegQuality: Int, val maxEdgePx: Int) {
    LOW("Compact (50%)", 55, 1400),
    MEDIUM("Balanced (75%)", 75, 1920),
    HIGH("High (90%)", 90, 2400),
    ORIGINAL("Original (100%)", 98, 3200),
}

enum class CompressionLevel(val label: String, val jpegQuality: Int, val renderDpi: Float) {
    LOW("Light Compression", 85, 160f),
    MEDIUM("Recommended", 68, 130f),
    HIGH("Maximum Compression", 48, 100f),
}

data class PdfPageSettings(
    val pageSize: PageSizeOption = PageSizeOption.A4,
    val customWidthMm: Int = 210,
    val customHeightMm: Int = 297,
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val marginPt: Float = 24f,
    val fitMode: ImageFitMode = ImageFitMode.FIT_PAGE,
    val alignment: ImageAlignment = ImageAlignment.CENTER,
    val backgroundColor: PageBackgroundColor = PageBackgroundColor.WHITE,
    val borderEnabled: Boolean = false,
    val borderWidthPt: Float = 2f,
    val borderColorArgb: Int = android.graphics.Color.rgb(15, 23, 42),
    val qualityPreset: ImageQualityPreset = ImageQualityPreset.HIGH,
)

data class PdfCreateOptions(
    val fileName: String,
    val pageSettings: PdfPageSettings,
    val compressionLevel: CompressionLevel = CompressionLevel.LOW,
    val passwordEnabled: Boolean = false,
    val password: String = "",
    val includePageNumbers: Boolean = false,
)

data class QuadPoints(
    val topLeft: Pair<Float, Float> = 0.08f to 0.08f,
    val topRight: Pair<Float, Float> = 0.92f to 0.08f,
    val bottomRight: Pair<Float, Float> = 0.92f to 0.92f,
    val bottomLeft: Pair<Float, Float> = 0.08f to 0.92f,
)
