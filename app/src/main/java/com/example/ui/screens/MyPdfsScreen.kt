package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PdfDocumentEntity
import com.example.model.SortOption
import com.example.model.ViewMode
import com.example.ui.components.PdfDocumentGridCard
import com.example.ui.components.PdfDocumentRowCard
import com.example.ui.theme.LocalBrandPalette

@Composable
fun MyPdfsScreen(
    documents: List<PdfDocumentEntity>,
    searchQuery: String,
    sortOption: SortOption,
    viewMode: ViewMode,
    onSearchQueryChange: (String) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onViewModeToggle: () -> Unit,
    onImportPdfClick: () -> Unit,
    onCreateNewPdfClick: () -> Unit,
    onOpenDocument: (PdfDocumentEntity) -> Unit,
    onShareDocument: (PdfDocumentEntity) -> Unit,
    onMoreDocument: (PdfDocumentEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("My PDFs", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "${documents.size} ${if (documents.size == 1) "file" else "files"} • ${sortOption.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onImportPdfClick,
                    modifier = Modifier.size(48.dp).testTag("library_import_pdf_btn"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileOpen,
                        contentDescription = "Import external PDF",
                        tint = brand.teal,
                    )
                }

                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.size(48.dp).testTag("library_sort_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort PDFs",
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                    ) {
                        SortOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt.label,
                                        color = if (opt == sortOption) brand.crimson else MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    onSortChange(opt)
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onViewModeToggle,
                    modifier = Modifier.size(48.dp).testTag("library_view_toggle_btn"),
                ) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                        contentDescription = if (viewMode == ViewMode.LIST) "Switch to Grid View" else "Switch to List View",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search PDF files...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input"),
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (documents.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_pdfs),
                    contentDescription = "Empty PDF library",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(24.dp)),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) "No Matching PDFs" else "Your PDF Library is Empty",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        "Try a different filename keyword."
                    } else {
                        "Convert gallery photos, scan paper documents, or import an existing PDF."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCreateNewPdfClick,
                        colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New PDF")
                    }
                    OutlinedButton(
                        onClick = onImportPdfClick,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(Icons.Filled.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import PDF")
                    }
                }
            }
        } else {
            when (viewMode) {
                ViewMode.LIST -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(documents, key = { it.id }) { doc ->
                            PdfDocumentRowCard(
                                document = doc,
                                onOpen = { onOpenDocument(doc) },
                                onShare = { onShareDocument(doc) },
                                onMoreClick = { onMoreDocument(doc) },
                            )
                        }
                    }
                }

                ViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 152.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 84.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(documents, key = { it.id }) { doc ->
                            PdfDocumentGridCard(
                                document = doc,
                                onOpen = { onOpenDocument(doc) },
                                onMoreClick = { onMoreDocument(doc) },
                            )
                        }
                    }
                }
            }
        }
    }
}
