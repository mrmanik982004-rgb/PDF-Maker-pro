package com.example.ui.state

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PdfMakerApp
import com.example.data.local.PdfDocumentEntity
import com.example.data.local.UserPreferences
import com.example.engine.BitmapProcessor
import com.example.engine.PdfEngine
import com.example.model.CompressionLevel
import com.example.model.DraftImageItem
import com.example.model.ImageEditConfig
import com.example.model.ImageQualityPreset
import com.example.model.PageSizeOption
import com.example.model.PdfCreateOptions
import com.example.model.PdfPageSettings
import com.example.model.SortOption
import com.example.model.ThemeMode
import com.example.model.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface PdfCreationUiState {
    data object Idle : PdfCreationUiState
    data class Creating(val progress: Float, val status: String) : PdfCreationUiState
    data class Success(val document: PdfDocumentEntity) : PdfCreationUiState
    data class Error(val message: String) : PdfCreationUiState
}

sealed interface ToolOperationState {
    data object Idle : ToolOperationState
    data class Running(val progress: Float, val status: String) : ToolOperationState
    data class Completed(
        val title: String,
        val detail: String,
        val createdDocuments: List<PdfDocumentEntity> = emptyList(),
    ) : ToolOperationState
    data class Error(val message: String) : ToolOperationState
}

data class PhotoEditorUiState(
    val imageId: String = "",
    val workingFilePath: String = "",
    val currentConfig: ImageEditConfig = ImageEditConfig(),
    val undoStack: List<ImageEditConfig> = emptyList(),
    val redoStack: List<ImageEditConfig> = emptyList(),
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PdfMakerApp
    private val libraryRepo = app.libraryRepository
    private val prefsRepo = app.preferencesRepository

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences(),
    )

    val totalPdfCount: StateFlow<Int> = libraryRepo.totalCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0,
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allDocuments: StateFlow<List<PdfDocumentEntity>> = libraryRepo.allDocuments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val filteredDocuments: StateFlow<List<PdfDocumentEntity>> = combine(
        libraryRepo.allDocuments,
        _searchQuery,
        preferences,
    ) { docs, query, prefs ->
        val filtered = if (query.isBlank()) {
            docs
        } else {
            val q = query.trim().lowercase()
            docs.filter { it.fileName.lowercase().contains(q) }
        }
        when (prefs.sortOption) {
            SortOption.NEWEST_FIRST -> filtered.sortedByDescending { it.createdAt }
            SortOption.OLDEST_FIRST -> filtered.sortedBy { it.createdAt }
            SortOption.NAME_ASC -> filtered.sortedBy { it.fileName.lowercase() }
            SortOption.FILE_SIZE_DESC -> filtered.sortedByDescending { it.fileSizeBytes }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val recentDocuments: StateFlow<List<PdfDocumentEntity>> = libraryRepo.allDocuments.combine(
        MutableStateFlow(Unit)
    ) { docs, _ ->
        docs.sortedByDescending { it.createdAt }.take(5)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    private val _draftImages = MutableStateFlow<List<DraftImageItem>>(emptyList())
    val draftImages: StateFlow<List<DraftImageItem>> = _draftImages.asStateFlow()

    private val _isImportingImages = MutableStateFlow(false)
    val isImportingImages: StateFlow<Boolean> = _isImportingImages.asStateFlow()

    private val _pageSettings = MutableStateFlow(PdfPageSettings())
    val pageSettings: StateFlow<PdfPageSettings> = _pageSettings.asStateFlow()

    private val _photoEditorState = MutableStateFlow(PhotoEditorUiState())
    val photoEditorState: StateFlow<PhotoEditorUiState> = _photoEditorState.asStateFlow()

    private val _creationState = MutableStateFlow<PdfCreationUiState>(PdfCreationUiState.Idle)
    val creationState: StateFlow<PdfCreationUiState> = _creationState.asStateFlow()

    private val _toolState = MutableStateFlow<ToolOperationState>(ToolOperationState.Idle)
    val toolState: StateFlow<ToolOperationState> = _toolState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            libraryRepo.syncWithDisk()
            val prefs = prefsRepo.preferences.first()
            _pageSettings.update {
                it.copy(
                    pageSize = prefs.defaultPageSize,
                    qualityPreset = prefs.defaultQuality,
                )
            }
        }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Preferences
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setDefaultPageSize(size: PageSizeOption) = viewModelScope.launch {
        prefsRepo.setDefaultPageSize(size)
        _pageSettings.update { it.copy(pageSize = size) }
    }
    fun setDefaultQuality(quality: ImageQualityPreset) = viewModelScope.launch {
        prefsRepo.setDefaultQuality(quality)
        _pageSettings.update { it.copy(qualityPreset = quality) }
    }
    fun setSortOption(sort: SortOption) = viewModelScope.launch { prefsRepo.setSortOption(sort) }
    fun setViewMode(mode: ViewMode) = viewModelScope.launch { prefsRepo.setViewMode(mode) }

    // Draft Images Workspace
    fun addImagesFromUris(uris: List<Uri>, onComplete: () -> Unit = {}) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImportingImages.value = true
            try {
                val added = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        runCatching {
                            val copied = BitmapProcessor.copyUriToDraftFile(app, uri, libraryRepo.draftsDir)
                            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(copied.absolutePath, opts)
                            val name = resolveUriDisplayName(uri) ?: "Image"
                            DraftImageItem(
                                workingFilePath = copied.absolutePath,
                                displayName = name,
                                width = opts.outWidth,
                                height = opts.outHeight,
                            )
                        }.getOrNull()
                    }
                }
                _draftImages.update { it + added }
                if (added.isNotEmpty()) {
                    onComplete()
                } else {
                    showMessage("Could not import the selected images.")
                }
            } finally {
                _isImportingImages.value = false
            }
        }
    }

    fun addScannedFilesToDrafts(files: List<File>, onComplete: () -> Unit = {}) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) {
                files.mapNotNull { file ->
                    if (!file.exists()) return@mapNotNull null
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                    DraftImageItem(
                        workingFilePath = file.absolutePath,
                        displayName = file.nameWithoutExtension,
                        width = opts.outWidth,
                        height = opts.outHeight,
                    )
                }
            }
            _draftImages.update { it + added }
            onComplete()
        }
    }

    fun removeDraftImage(id: String) {
        _draftImages.update { list -> list.filterNot { it.id == id } }
    }

    fun moveDraftImage(fromIndex: Int, toIndex: Int) {
        _draftImages.update { list ->
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) {
                return@update list
            }
            val mutable = list.toMutableList()
            val moved = mutable.removeAt(fromIndex)
            mutable.add(toIndex, moved)
            mutable
        }
    }

    fun clearDrafts() {
        _draftImages.value = emptyList()
    }

    // Photo Editor with Undo/Redo
    fun openPhotoEditor(imageId: String) {
        val item = _draftImages.value.firstOrNull { it.id == imageId } ?: return
        _photoEditorState.value = PhotoEditorUiState(
            imageId = item.id,
            workingFilePath = item.workingFilePath,
            currentConfig = item.editConfig,
            undoStack = emptyList(),
            redoStack = emptyList(),
        )
    }

    fun updatePhotoEditConfig(newConfig: ImageEditConfig, recordUndo: Boolean = true) {
        _photoEditorState.update { state ->
            if (state.currentConfig == newConfig) return@update state
            val newUndo = if (recordUndo) {
                (state.undoStack + state.currentConfig).takeLast(25)
            } else {
                state.undoStack
            }
            state.copy(
                currentConfig = newConfig,
                undoStack = newUndo,
                redoStack = if (recordUndo) emptyList() else state.redoStack,
            )
        }
    }

    fun undoPhotoEdit() {
        _photoEditorState.update { state ->
            val prev = state.undoStack.lastOrNull() ?: return@update state
            state.copy(
                currentConfig = prev,
                undoStack = state.undoStack.dropLast(1),
                redoStack = state.redoStack + state.currentConfig,
            )
        }
    }

    fun redoPhotoEdit() {
        _photoEditorState.update { state ->
            val next = state.redoStack.lastOrNull() ?: return@update state
            state.copy(
                currentConfig = next,
                undoStack = state.undoStack + state.currentConfig,
                redoStack = state.redoStack.dropLast(1),
            )
        }
    }

    fun resetPhotoEdit() {
        updatePhotoEditConfig(ImageEditConfig(), recordUndo = true)
    }

    fun savePhotoEdit() {
        val editorState = _photoEditorState.value
        _draftImages.update { list ->
            list.map { item ->
                if (item.id == editorState.imageId) {
                    item.copy(
                        editConfig = editorState.currentConfig,
                        previewRevision = item.previewRevision + 1,
                    )
                } else {
                    item
                }
            }
        }
    }

    // Page Settings
    fun updatePageSettings(transform: (PdfPageSettings) -> PdfPageSettings) {
        _pageSettings.update(transform)
    }

    // PDF Creation
    fun createPdf(options: PdfCreateOptions) {
        val items = _draftImages.value
        if (items.isEmpty()) {
            _creationState.value = PdfCreationUiState.Error("No images selected.")
            return
        }
        viewModelScope.launch {
            _creationState.value = PdfCreationUiState.Creating(0.05f, "Preparing images...")
            try {
                val cleanName = options.fileName.trim().ifEmpty { "PDF_${System.currentTimeMillis()}" }
                val pdfFile = PdfEngine.uniqueFile(libraryRepo.pdfsDir, "$cleanName.pdf")
                val thumbFile = File(libraryRepo.thumbnailsDir, "${pdfFile.nameWithoutExtension}.jpg")

                val result = PdfEngine.createPdfFromImages(
                    items = items,
                    options = options,
                    outputPdfFile = pdfFile,
                    thumbnailFile = thumbFile,
                ) { progress, status ->
                    _creationState.value = PdfCreationUiState.Creating(progress, status)
                }
                val savedEntity = libraryRepo.registerGeneratedPdf(result)
                _creationState.value = PdfCreationUiState.Success(savedEntity)
            } catch (e: Exception) {
                _creationState.value = PdfCreationUiState.Error(e.message ?: "Failed to create PDF")
            }
        }
    }

    fun resetCreationState() {
        _creationState.value = PdfCreationUiState.Idle
    }

    // Library operations
    fun importExternalPdf(uri: Uri, onImported: (PdfDocumentEntity) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { libraryRepo.importPdfFromUri(uri) }
                .onSuccess {
                    showMessage("Imported \"${it.fileName}.pdf\"")
                    onImported(it)
                }
                .onFailure {
                    showMessage(it.message ?: "Could not import PDF")
                }
        }
    }

    fun renamePdf(doc: PdfDocumentEntity, newName: String) {
        viewModelScope.launch {
            libraryRepo.renameDocument(doc, newName)
            showMessage("Renamed to \"$newName.pdf\"")
        }
    }

    fun deletePdf(doc: PdfDocumentEntity) {
        viewModelScope.launch {
            libraryRepo.deleteDocument(doc)
            showMessage("Deleted \"${doc.fileName}.pdf\"")
        }
    }

    fun saveToDownloads(doc: PdfDocumentEntity) {
        viewModelScope.launch {
            val uri = PdfEngine.savePdfToDownloads(app, File(doc.filePath))
            if (uri != null) {
                showMessage("Saved to Downloads/PDFMaker/${doc.fileName}.pdf")
            } else {
                PdfEngine.sharePdfFile(app, File(doc.filePath))
            }
        }
    }

    suspend fun getDocumentById(id: Long): PdfDocumentEntity? = libraryRepo.getById(id)

    // PDF Tools
    fun resetToolState() {
        _toolState.value = ToolOperationState.Idle
    }

    fun mergePdfs(docs: List<PdfDocumentEntity>, outputBaseName: String) {
        viewModelScope.launch {
            _toolState.value = ToolOperationState.Running(0.1f, "Merging ${docs.size} PDFs...")
            try {
                val outName = outputBaseName.trim().ifEmpty { "Merged_${System.currentTimeMillis()}" }
                val outFile = PdfEngine.uniqueFile(libraryRepo.pdfsDir, "$outName.pdf")
                val result = PdfEngine.mergePdfs(
                    inputFiles = docs.map { File(it.filePath) },
                    outputFile = outFile,
                ) { p ->
                    _toolState.value = ToolOperationState.Running(p, "Combining pages...")
                }
                val saved = libraryRepo.registerGeneratedPdf(result)
                _toolState.value = ToolOperationState.Completed(
                    title = "PDFs Merged Successfully",
                    detail = "${saved.fileName}.pdf (${saved.pageCount} pages)",
                    createdDocuments = listOf(saved),
                )
            } catch (e: Exception) {
                _toolState.value = ToolOperationState.Error(e.message ?: "Merge failed (encrypted PDFs cannot be merged directly)")
            }
        }
    }

    fun splitPdf(
        doc: PdfDocumentEntity,
        mode: PdfEngine.SplitMode,
        startPage: Int,
        endPage: Int,
        pagesPerSplit: Int,
    ) {
        viewModelScope.launch {
            _toolState.value = ToolOperationState.Running(0.25f, "Splitting \"${doc.fileName}.pdf\"...")
            try {
                val results = PdfEngine.splitPdf(
                    sourceFile = File(doc.filePath),
                    mode = mode,
                    startPage = startPage,
                    endPage = endPage,
                    pagesPerSplit = pagesPerSplit,
                    outputDir = libraryRepo.pdfsDir,
                    baseName = doc.fileName,
                )
                val savedList = results.map { libraryRepo.registerGeneratedPdf(it) }
                _toolState.value = ToolOperationState.Completed(
                    title = "PDF Split Complete",
                    detail = "Created ${savedList.size} PDF file(s) in My PDFs.",
                    createdDocuments = savedList,
                )
            } catch (e: Exception) {
                _toolState.value = ToolOperationState.Error(e.message ?: "Failed to split PDF")
            }
        }
    }

    fun reorderAndRotatePdf(
        doc: PdfDocumentEntity,
        specs: List<PdfEngine.PageTransformSpec>,
        outputBaseName: String,
    ) {
        viewModelScope.launch {
            _toolState.value = ToolOperationState.Running(0.3f, "Saving updated page order & rotation...")
            try {
                val clean = outputBaseName.trim().ifEmpty { "${doc.fileName}_edited" }
                val outFile = PdfEngine.uniqueFile(libraryRepo.pdfsDir, "$clean.pdf")
                val result = PdfEngine.reorderAndRotatePdf(File(doc.filePath), specs, outFile)
                val saved = libraryRepo.registerGeneratedPdf(result)
                _toolState.value = ToolOperationState.Completed(
                    title = "Pages Updated",
                    detail = "Saved \"${saved.fileName}.pdf\" (${saved.pageCount} pages).",
                    createdDocuments = listOf(saved),
                )
            } catch (e: Exception) {
                _toolState.value = ToolOperationState.Error(e.message ?: "Failed to update PDF pages")
            }
        }
    }

    fun compressPdf(doc: PdfDocumentEntity, level: CompressionLevel, outputBaseName: String) {
        viewModelScope.launch {
            _toolState.value = ToolOperationState.Running(0.1f, "Compressing PDF pages...")
            try {
                val clean = outputBaseName.trim().ifEmpty { "${doc.fileName}_compressed" }
                val outFile = PdfEngine.uniqueFile(libraryRepo.pdfsDir, "$clean.pdf")
                val result = PdfEngine.compressPdf(File(doc.filePath), level, outFile) { p ->
                    _toolState.value = ToolOperationState.Running(p, "Optimizing images (${(p * 100).toInt()}%)...")
                }
                val saved = libraryRepo.registerGeneratedPdf(result)
                val oldKb = (doc.fileSizeBytes / 1024.0).coerceAtLeast(1.0)
                val newKb = (saved.fileSizeBytes / 1024.0).coerceAtLeast(1.0)
                val pct = (((oldKb - newKb) / oldKb) * 100).toInt()
                val diffText = if (pct > 0) "Reduced by $pct%" else "Saved compressed copy"
                _toolState.value = ToolOperationState.Completed(
                    title = "Compression Complete",
                    detail = "$diffText • ${saved.fileName}.pdf",
                    createdDocuments = listOf(saved),
                )
            } catch (e: Exception) {
                _toolState.value = ToolOperationState.Error(e.message ?: "Compression failed")
            }
        }
    }

    fun exportPdfToImages(doc: PdfDocumentEntity, password: String = "") {
        viewModelScope.launch {
            _toolState.value = ToolOperationState.Running(0.1f, "Rendering PDF pages to images...")
            try {
                val uris = PdfEngine.exportPdfToImages(
                    context = app,
                    pdfFile = File(doc.filePath),
                    baseName = doc.fileName,
                    password = password,
                ) { p ->
                    _toolState.value = ToolOperationState.Running(p, "Exporting page ${(p * doc.pageCount).toInt() + 1}...")
                }
                _toolState.value = ToolOperationState.Completed(
                    title = "Exported ${uris.size} Image(s)",
                    detail = "Saved to Pictures/PDFMaker on your device.",
                )
            } catch (e: Exception) {
                _toolState.value = ToolOperationState.Error(e.message ?: "Failed to export images")
            }
        }
    }

    private fun resolveUriDisplayName(uri: Uri): String? {
        return app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }
}
