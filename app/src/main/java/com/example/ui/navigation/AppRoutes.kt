package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Library : AppRoute

    @Serializable
    data object Tools : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object Workspace : AppRoute

    @Serializable
    data object CameraScanner : AppRoute

    @Serializable
    data class PhotoEditor(val imageId: String) : AppRoute

    @Serializable
    data object PageSettings : AppRoute

    @Serializable
    data class PdfViewer(val documentId: Long) : AppRoute

    @Serializable
    data class PdfToolDetail(val toolType: String) : AppRoute
}

object PdfToolType {
    const val MERGE = "merge"
    const val SPLIT = "split"
    const val REORDER_ROTATE = "reorder_rotate"
    const val COMPRESS = "compress"
    const val PDF_TO_IMAGES = "pdf_to_images"
}
