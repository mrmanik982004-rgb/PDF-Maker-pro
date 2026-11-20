package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.PdfLibraryRepository
import com.example.data.local.UserPreferencesRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PdfMakerApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val libraryRepository: PdfLibraryRepository by lazy {
        PdfLibraryRepository(this, database.pdfDocumentDao())
    }

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
