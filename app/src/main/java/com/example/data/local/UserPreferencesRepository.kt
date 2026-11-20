package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ImageQualityPreset
import com.example.model.PageSizeOption
import com.example.model.SortOption
import com.example.model.ThemeMode
import com.example.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pdf_maker_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultPageSize: PageSizeOption = PageSizeOption.A4,
    val defaultQuality: ImageQualityPreset = ImageQualityPreset.HIGH,
    val sortOption: SortOption = SortOption.NEWEST_FIRST,
    val viewMode: ViewMode = ViewMode.LIST,
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_PAGE_SIZE = stringPreferencesKey("default_page_size")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val VIEW_MODE = stringPreferencesKey("view_mode")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            defaultPageSize = prefs[Keys.DEFAULT_PAGE_SIZE]?.let { runCatching { PageSizeOption.valueOf(it) }.getOrNull() }
                ?: PageSizeOption.A4,
            defaultQuality = prefs[Keys.DEFAULT_QUALITY]?.let { runCatching { ImageQualityPreset.valueOf(it) }.getOrNull() }
                ?: ImageQualityPreset.HIGH,
            sortOption = prefs[Keys.SORT_OPTION]?.let { runCatching { SortOption.valueOf(it) }.getOrNull() }
                ?: SortOption.NEWEST_FIRST,
            viewMode = prefs[Keys.VIEW_MODE]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() }
                ?: ViewMode.LIST,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultPageSize(size: PageSizeOption) {
        context.dataStore.edit { it[Keys.DEFAULT_PAGE_SIZE] = size.name }
    }

    suspend fun setDefaultQuality(quality: ImageQualityPreset) {
        context.dataStore.edit { it[Keys.DEFAULT_QUALITY] = quality.name }
    }

    suspend fun setSortOption(sort: SortOption) {
        context.dataStore.edit { it[Keys.SORT_OPTION] = sort.name }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { it[Keys.VIEW_MODE] = mode.name }
    }
}
