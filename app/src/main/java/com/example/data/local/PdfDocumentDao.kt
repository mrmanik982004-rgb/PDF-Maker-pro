package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDocumentDao {
    @Query("SELECT * FROM pdf_documents ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT COUNT(*) FROM pdf_documents")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PdfDocumentEntity?

    @Query("SELECT * FROM pdf_documents WHERE filePath = :path LIMIT 1")
    suspend fun getByPath(path: String): PdfDocumentEntity?

    @Query("SELECT * FROM pdf_documents")
    suspend fun getAllSnapshot(): List<PdfDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doc: PdfDocumentEntity): Long

    @Update
    suspend fun update(doc: PdfDocumentEntity)

    @Delete
    suspend fun delete(doc: PdfDocumentEntity)

    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deleteById(id: Long)
}
