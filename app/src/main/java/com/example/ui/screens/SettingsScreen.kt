package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.PdfMakerApp
import com.example.data.local.PdfDocumentEntity
import com.example.data.local.UserPreferences
import com.example.model.ImageQualityPreset
import com.example.model.PageSizeOption
import com.example.model.ThemeMode
import com.example.model.ViewMode
import com.example.ui.theme.LocalBrandPalette
import com.example.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    documents: List<PdfDocumentEntity>,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDefaultPageSizeChange: (PageSizeOption) -> Unit,
    onDefaultQualityChange: (ImageQualityPreset) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onClearDraftCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val totalBytes = remember(documents) { documents.sumOf { it.fileSizeBytes } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Customize theme, PDF defaults, and local storage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Theme Mode
        item {
            SettingsCard(
                icon = Icons.Filled.DarkMode,
                title = "Appearance",
                subtitle = "Switch between Light Mode, Dark Mode, or System",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("theme_${mode.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        // Default Page Size
        item {
            SettingsCard(
                icon = Icons.Filled.Description,
                title = "Default PDF Page Size",
                subtitle = "Applied automatically when starting a new PDF",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        PageSizeOption.A4,
                        PageSizeOption.A3,
                        PageSizeOption.A5,
                        PageSizeOption.LETTER,
                        PageSizeOption.LEGAL,
                        PageSizeOption.ORIGINAL,
                    ).forEach { size ->
                        FilterChip(
                            selected = preferences.defaultPageSize == size,
                            onClick = { onDefaultPageSizeChange(size) },
                            label = { Text(size.label) },
                        )
                    }
                }
            }
        }

        // Default Image Quality
        item {
            SettingsCard(
                icon = Icons.Filled.HighQuality,
                title = "Default Image Quality",
                subtitle = "Controls image resolution and compression balance",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ImageQualityPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = preferences.defaultQuality == preset,
                            onClick = { onDefaultQualityChange(preset) },
                            label = { Text(preset.label) },
                        )
                    }
                }
            }
        }

        // Default Library View
        item {
            SettingsCard(
                icon = Icons.Filled.GridView,
                title = "My PDFs Layout",
                subtitle = "Choose how saved PDFs are displayed",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ViewMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.viewMode == mode,
                            onClick = { onViewModeChange(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }
        }

        // Storage & Cache
        item {
            SettingsCard(
                icon = Icons.Filled.DeleteSweep,
                title = "Local Storage",
                subtitle = "${documents.size} PDFs using ${Formatters.formatFileSize(totalBytes)} on device",
            ) {
                OutlinedButton(
                    onClick = {
                        val draftsDir = (context.applicationContext as PdfMakerApp).libraryRepository.draftsDir
                        draftsDir.listFiles()?.forEach { it.delete() }
                        onClearDraftCache()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp),
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Temporary Draft Cache")
                }
            }
        }

        // Offline Privacy Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = brand.navyHero),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = brand.teal.copy(alpha = 0.2f),
                        modifier = Modifier.size(46.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = brand.teal,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% Offline & Private",
                            style = MaterialTheme.typography.titleSmall,
                            color = brand.onHero,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PDF Maker works completely offline with zero Internet permission. Your documents never leave your phone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = brand.onHeroMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
