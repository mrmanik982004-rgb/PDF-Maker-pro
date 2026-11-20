package com.example.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.engine.GeneratedPdfResult
import com.example.engine.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfLibraryRepository(
    private val context: Context,
    private val dao: PdfDocumentDao,
) {
    val pdfsDir: File = File(context.filesDir, "pdfs").apply { mkdirs() }
    val thumbnailsDir: File = File(context.filesDir, "thumbnails").apply { mkdirs() }
    val draftsDir: File = File(context.filesDir, "drafts").apply { mkdirs() }

    val allDocuments: Flow<List<PdfDocumentEntity>> = dao.observeAll()
    val totalCount: Flow<Int> = dao.observeCount()

    suspend fun syncWithDisk() = withContext(Dispatchers.IO) {
        val snapshot = dao.getAllSnapshot()
        snapshot.forEach { entity ->
            if (!File(entity.filePath).exists()) {
                dao.delete(entity)
            }
        }
    }

    suspend fun registerGeneratedPdf(result: GeneratedPdfResult): PdfDocumentEntity =
        withContext(Dispatchers.IO) {
            val entity = PdfDocumentEntity(
                fileName = result.pdfFile.nameWithoutExtension,
                filePath = result.pdfFile.absolutePath,
                thumbnailPath = result.thumbnailFile?.absolutePath,
                fileSizeBytes = result.fileSizeBytes,
                pageCount = result.pageCount,
                createdAt = System.currentTimeMillis(),
                isEncrypted = result.isEncrypted,
            )
            val id = dao.insert(entity)
            entity.copy(id = id)
        }

    suspend fun importPdfFromUri(uri: Uri): PdfDocumentEntity = withContext(Dispatchers.IO) {
        val displayName = resolveDisplayName(uri) ?: "Imported_${System.currentTimeMillis()}.pdf"
        val cleanName = if (displayName.endsWith(".pdf", true)) displayName else "$displayName.pdf"
        val outFile = PdfEngine.uniqueFile(pdfsDir, cleanName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Cannot open selected PDF file")

        val (pages, encrypted) = runCatching { PdfEngine.getPdfMetadata(outFile) }.getOrDefault(1 to false)
        val thumbFile = File(thumbnailsDir, "${outFile.nameWithoutExtension}.jpg")
        if (!encrypted) {
            PdfEngine.generateThumbnailForPdf(outFile, thumbFile)
        }
        val entity = PdfDocumentEntity(
            fileName = outFile.nameWithoutExtension,
            filePath = outFile.absolutePath,
            thumbnailPath = thumbFile.takeIf { it.exists() }?.absolutePath,
            fileSizeBytes = outFile.length(),
            pageCount = pages,
            createdAt = System.currentTimeMillis(),
            isEncrypted = encrypted,
        )
        val id = dao.insert(entity)
        entity.copy(id = id)
    }

    suspend fun renameDocument(doc: PdfDocumentEntity, newBaseName: String): PdfDocumentEntity =
        withContext(Dispatchers.IO) {
            val sanitized = newBaseName.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
            if (sanitized.isEmpty()) return@withContext doc
            val oldFile = File(doc.filePath)
            val newFile = PdfEngine.uniqueFile(pdfsDir, "$sanitized.pdf")
            if (oldFile.exists()) {
                oldFile.renameTo(newFile)
            }
            val updated = doc.copy(
                fileName = newFile.nameWithoutExtension,
                filePath = newFile.absolutePath,
            )
            dao.update(updated)
            updated
        }

    suspend fun deleteDocument(doc: PdfDocumentEntity) = withContext(Dispatchers.IO) {
        runCatching { File(doc.filePath).delete() }
        doc.thumbnailPath?.let { runCatching { File(it).delete() } }
        dao.delete(doc)
    }

    suspend fun getById(id: Long): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }
}
