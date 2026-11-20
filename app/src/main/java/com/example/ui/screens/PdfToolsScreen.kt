package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PdfDocumentEntity
import com.example.engine.PdfEngine
import com.example.model.CompressionLevel
import com.example.ui.navigation.PdfToolType
import com.example.ui.state.ToolOperationState
import com.example.ui.theme.LocalBrandPalette
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfToolsScreen(
    onSelectTool: (String) -> Unit,
    onImagesToPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val tools = listOf(
        ToolTile(
            title = "Merge Multiple PDFs",
            subtitle = "Combine 2 or more PDF files in custom order",
            icon = Icons.Filled.MergeType,
            accent = brand.crimson,
            onClick = { onSelectTool(PdfToolType.MERGE) },
        ),
        ToolTile(
            title = "Split PDF",
            subtitle = "Extract page ranges or split into separate files",
            icon = Icons.AutoMirrored.Filled.CallSplit,
            accent = brand.teal,
            onClick = { onSelectTool(PdfToolType.SPLIT) },
        ),
        ToolTile(
            title = "Reorder & Rotate Pages",
            subtitle = "Change page sequence or rotate pages 90°",
            icon = Icons.Filled.SwapVert,
            accent = brand.navyHeroElevated,
            onClick = { onSelectTool(PdfToolType.REORDER_ROTATE) },
        ),
        ToolTile(
            title = "Compress PDF",
            subtitle = "Reduce PDF file size while preserving readability",
            icon = Icons.Filled.Compress,
            accent = brand.crimson,
            onClick = { onSelectTool(PdfToolType.COMPRESS) },
        ),
        ToolTile(
            title = "Images to PDF",
            subtitle = "Convert multiple gallery photos into a clean PDF",
            icon = Icons.Filled.Collections,
            accent = brand.teal,
            onClick = onImagesToPdf,
        ),
        ToolTile(
            title = "PDF to Images",
            subtitle = "Export PDF pages as crisp PNG images to Pictures",
            icon = Icons.Filled.Image,
            accent = brand.navyHeroElevated,
            onClick = { onSelectTool(PdfToolType.PDF_TO_IMAGES) },
        ),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("PDF Utilities & Tools", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "100% offline processing directly on your device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(tools) { tool ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = tool.onClick),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tool.accent.copy(alpha = 0.13f),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.title,
                                tint = tool.accent,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tool.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private data class ToolTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PdfToolDetailScreen(
    toolType: String,
    allDocuments: List<PdfDocumentEntity>,
    preselectedDocumentId: Long?,
    toolState: ToolOperationState,
    onBack: () -> Unit,
    onImportExternalPdf: () -> Unit,
    onMergePdfs: (List<PdfDocumentEntity>, String) -> Unit,
    onSplitPdf: (PdfDocumentEntity, PdfEngine.SplitMode, Int, Int, Int) -> Unit,
    onReorderRotatePdf: (PdfDocumentEntity, List<PdfEngine.PageTransformSpec>, String) -> Unit,
    onCompressPdf: (PdfDocumentEntity, CompressionLevel, String) -> Unit,
    onExportPdfToImages: (PdfDocumentEntity) -> Unit,
    onOpenDocument: (PdfDocumentEntity) -> Unit,
    onResetToolState: () -> Unit,
) {
    val brand = LocalBrandPalette.current
    val unencryptedDocs = remember(allDocuments) { allDocuments.filterNot { it.isEncrypted } }

    val title = when (toolType) {
        PdfToolType.MERGE -> "Merge Multiple PDFs"
        PdfToolType.SPLIT -> "Split PDF"
        PdfToolType.REORDER_ROTATE -> "Reorder & Rotate Pages"
        PdfToolType.COMPRESS -> "Compress PDF"
        PdfToolType.PDF_TO_IMAGES -> "PDF to Images"
        else -> "PDF Tool"
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { onResetToolState(); onBack() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onImportExternalPdf, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.FileOpen, contentDescription = "Import external PDF", tint = brand.teal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            when (toolState) {
                is ToolOperationState.Running -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = brand.crimson)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(toolState.status, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { toolState.progress },
                            color = brand.crimson,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                        )
                    }
                }

                is ToolOperationState.Completed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = brand.success.copy(alpha = 0.14f),
                            modifier = Modifier.size(72.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = brand.success,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(toolState.title, style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = toolState.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        if (toolState.createdDocuments.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val first = toolState.createdDocuments.first()
                                    onResetToolState()
                                    onOpenDocument(first)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) {
                                Text("Open Output PDF")
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        OutlinedButton(
                            onClick = { onResetToolState(); onBack() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text("Done")
                        }
                    }
                }

                is ToolOperationState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Operation Failed", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(toolState.message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(onClick = onResetToolState) { Text("Try Again") }
                    }
                }

                is ToolOperationState.Idle -> {
                    if (unencryptedDocs.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("No Unlocked PDFs Available", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create a PDF first or tap the Import icon in the top-right to load a PDF from your device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onImportExternalPdf,
                                colors = ButtonDefaults.buttonColors(containerColor = brand.teal),
                            ) {
                                Icon(Icons.Filled.FileOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import PDF From Device")
                            }
                        }
                    } else {
                        when (toolType) {
                            PdfToolType.MERGE -> MergeToolBody(
                                docs = unencryptedDocs,
                                onMerge = onMergePdfs,
                            )

                            PdfToolType.SPLIT -> SplitToolBody(
                                docs = unencryptedDocs,
                                initialDocId = preselectedDocumentId,
                                onSplit = onSplitPdf,
                            )

                            PdfToolType.REORDER_ROTATE -> ReorderRotateToolBody(
                                docs = unencryptedDocs,
                                initialDocId = preselectedDocumentId,
                                onSave = onReorderRotatePdf,
                            )

                            PdfToolType.COMPRESS -> CompressToolBody(
                                docs = unencryptedDocs,
                                initialDocId = preselectedDocumentId,
                                onCompress = onCompressPdf,
                            )

                            PdfToolType.PDF_TO_IMAGES -> PdfToImagesToolBody(
                                docs = unencryptedDocs,
                                initialDocId = preselectedDocumentId,
                                onExport = onExportPdfToImages,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeToolBody(
    docs: List<PdfDocumentEntity>,
    onMerge: (List<PdfDocumentEntity>, String) -> Unit,
) {
    val brand = LocalBrandPalette.current
    val selectedOrder = remember { mutableStateListOf<PdfDocumentEntity>() }
    var outputName by remember { mutableStateOf("Merged_${Formatters.defaultPdfFileName()}") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = outputName,
            onValueChange = { outputName = it },
            label = { Text("Output PDF Name") },
            suffix = { Text(".pdf") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Select 2 or more PDFs (${selectedOrder.size} selected)",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(docs, key = { it.id }) { doc ->
                val idx = selectedOrder.indexOfFirst { it.id == doc.id }
                val isSelected = idx >= 0
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) selectedOrder.removeAt(idx) else selectedOrder.add(doc)
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked && !isSelected) selectedOrder.add(doc)
                                else if (!checked && isSelected) selectedOrder.removeAt(idx)
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${doc.fileName}.pdf", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${doc.pageCount} pages • ${Formatters.formatFileSize(doc.fileSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isSelected) {
                            Surface(color = brand.crimson, shape = CircleShape) {
                                Text(
                                    text = "#${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (idx > 0) {
                                        val item = selectedOrder.removeAt(idx)
                                        selectedOrder.add(idx - 1, item)
                                    }
                                },
                                enabled = idx > 0,
                            ) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up")
                            }
                            IconButton(
                                onClick = {
                                    if (idx < selectedOrder.lastIndex) {
                                        val item = selectedOrder.removeAt(idx)
                                        selectedOrder.add(idx + 1, item)
                                    }
                                },
                                enabled = idx < selectedOrder.lastIndex,
                            ) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onMerge(selectedOrder.toList(), outputName) },
            enabled = selectedOrder.size >= 2,
            colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("merge_execute_btn"),
        ) {
            Text("Merge ${selectedOrder.size} PDFs")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SplitToolBody(
    docs: List<PdfDocumentEntity>,
    initialDocId: Long?,
    onSplit: (PdfDocumentEntity, PdfEngine.SplitMode, Int, Int, Int) -> Unit,
) {
    val brand = LocalBrandPalette.current
    var selectedDoc by remember {
        mutableStateOf(docs.firstOrNull { it.id == initialDocId } ?: docs.first())
    }
    var splitMode by remember { mutableStateOf(PdfEngine.SplitMode.RANGE) }
    var startPage by remember(selectedDoc) { mutableStateOf("1") }
    var endPage by remember(selectedDoc) { mutableStateOf(selectedDoc.pageCount.toString()) }
    var everyN by remember { mutableStateOf("2") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Select Source PDF", style = MaterialTheme.typography.titleSmall)
        SingleDocSelector(docs = docs, selected = selectedDoc, onSelect = { selectedDoc = it }, modifier = Modifier.weight(1f))

        Text("Split Mode", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = splitMode == PdfEngine.SplitMode.RANGE,
                onClick = { splitMode = PdfEngine.SplitMode.RANGE },
                label = { Text("Page Range") },
            )
            FilterChip(
                selected = splitMode == PdfEngine.SplitMode.EVERY_N_PAGES,
                onClick = { splitMode = PdfEngine.SplitMode.EVERY_N_PAGES },
                label = { Text("Every N Pages") },
            )
            FilterChip(
                selected = splitMode == PdfEngine.SplitMode.ALL_PAGES,
                onClick = { splitMode = PdfEngine.SplitMode.ALL_PAGES },
                label = { Text("All Single Pages") },
            )
        }

        when (splitMode) {
            PdfEngine.SplitMode.RANGE -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startPage,
                        onValueChange = { startPage = it.filter(Char::isDigit) },
                        label = { Text("Start Page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = endPage,
                        onValueChange = { endPage = it.filter(Char::isDigit) },
                        label = { Text("End Page (max ${selectedDoc.pageCount})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            PdfEngine.SplitMode.EVERY_N_PAGES -> {
                OutlinedTextField(
                    value = everyN,
                    onValueChange = { everyN = it.filter(Char::isDigit) },
                    label = { Text("Pages per split file") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PdfEngine.SplitMode.ALL_PAGES -> {
                Text(
                    text = "Will extract ${selectedDoc.pageCount} separate 1-page PDFs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = {
                onSplit(
                    selectedDoc,
                    splitMode,
                    startPage.toIntOrNull() ?: 1,
                    endPage.toIntOrNull() ?: selectedDoc.pageCount,
                    everyN.toIntOrNull() ?: 1,
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("Split PDF")
        }
    }
}

private data class PageEditThumb(
    val originalIndex: Int,
    val rotationDeg: Int,
    val thumb: Bitmap?,
)

@Composable
private fun ReorderRotateToolBody(
    docs: List<PdfDocumentEntity>,
    initialDocId: Long?,
    onSave: (PdfDocumentEntity, List<PdfEngine.PageTransformSpec>, String) -> Unit,
) {
    val brand = LocalBrandPalette.current
    var selectedDoc by remember {
        mutableStateOf<PdfDocumentEntity?>(docs.firstOrNull { it.id == initialDocId })
    }

    if (selectedDoc == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Choose a PDF to Reorder or Rotate", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            SingleDocSelector(
                docs = docs,
                selected = null,
                onSelect = { selectedDoc = it },
                modifier = Modifier.weight(1f),
            )
        }
        return
    }

    val doc = selectedDoc!!
    val pageSpecs = remember { mutableStateListOf<PageEditThumb>() }
    var isLoadingThumbs by remember { mutableStateOf(true) }
    var outputName by remember(doc.id) { mutableStateOf("${doc.fileName}_edited") }

    LaunchedEffect(doc.id) {
        isLoadingThumbs = true
        pageSpecs.clear()
        val file = File(doc.filePath)
        val count = doc.pageCount.coerceAtLeast(1)
        val loaded = withContext(Dispatchers.IO) {
            (0 until count).map { idx ->
                PageEditThumb(
                    originalIndex = idx,
                    rotationDeg = 0,
                    thumb = PdfEngine.renderPdfPage(file, idx, maxWidthPx = 320),
                )
            }
        }
        pageSpecs.addAll(loaded)
        isLoadingThumbs = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Output Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    for (i in pageSpecs.indices) {
                        pageSpecs[i] = pageSpecs[i].copy(rotationDeg = (pageSpecs[i].rotationDeg + 90) % 360)
                    }
                },
                modifier = Modifier.height(54.dp),
            ) {
                Icon(Icons.Filled.RotateRight, contentDescription = "Rotate all 90°")
                Spacer(modifier = Modifier.width(4.dp))
                Text("All 90°")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoadingThumbs) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = brand.teal)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(pageSpecs) { idx, item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.75f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (item.thumb != null) {
                                    Image(
                                        bitmap = item.thumb.asImageBitmap(),
                                        contentDescription = "Page ${idx + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                Surface(
                                    color = brand.navyHero.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                                    modifier = Modifier.align(Alignment.TopStart),
                                ) {
                                    Text(
                                        text = "P${idx + 1} (${item.rotationDeg}°)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                IconButton(
                                    onClick = {
                                        if (idx > 0) {
                                            val moved = pageSpecs.removeAt(idx)
                                            pageSpecs.add(idx - 1, moved)
                                        }
                                    },
                                    enabled = idx > 0,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move earlier", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        pageSpecs[idx] = item.copy(rotationDeg = (item.rotationDeg + 90) % 360)
                                    },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(Icons.Filled.RotateRight, contentDescription = "Rotate 90°", tint = brand.crimson, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (idx < pageSpecs.lastIndex) {
                                            val moved = pageSpecs.removeAt(idx)
                                            pageSpecs.add(idx + 1, moved)
                                        }
                                    },
                                    enabled = idx < pageSpecs.lastIndex,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move later", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                onSave(
                    doc,
                    pageSpecs.map { PdfEngine.PageTransformSpec(it.originalIndex, it.rotationDeg) },
                    outputName,
                )
            },
            enabled = pageSpecs.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("Save Updated PDF")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompressToolBody(
    docs: List<PdfDocumentEntity>,
    initialDocId: Long?,
    onCompress: (PdfDocumentEntity, CompressionLevel, String) -> Unit,
) {
    val brand = LocalBrandPalette.current
    var selectedDoc by remember {
        mutableStateOf(docs.firstOrNull { it.id == initialDocId } ?: docs.first())
    }
    var level by remember { mutableStateOf(CompressionLevel.MEDIUM) }
    var outputName by remember(selectedDoc.id) { mutableStateOf("${selectedDoc.fileName}_compressed") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Select PDF to Compress", style = MaterialTheme.typography.titleSmall)
        SingleDocSelector(docs = docs, selected = selectedDoc, onSelect = { selectedDoc = it }, modifier = Modifier.weight(1f))

        OutlinedTextField(
            value = outputName,
            onValueChange = { outputName = it },
            label = { Text("Compressed File Name") },
            suffix = { Text(".pdf") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Compression Level", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompressionLevel.entries.forEach { lvl ->
                FilterChip(
                    selected = level == lvl,
                    onClick = { level = lvl },
                    label = { Text(lvl.label) },
                )
            }
        }

        Button(
            onClick = { onCompress(selectedDoc, level, outputName) },
            colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("Compress PDF (${Formatters.formatFileSize(selectedDoc.fileSizeBytes)})")
        }
    }
}

@Composable
private fun PdfToImagesToolBody(
    docs: List<PdfDocumentEntity>,
    initialDocId: Long?,
    onExport: (PdfDocumentEntity) -> Unit,
) {
    val brand = LocalBrandPalette.current
    var selectedDoc by remember {
        mutableStateOf(docs.firstOrNull { it.id == initialDocId } ?: docs.first())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Select PDF to Export as PNG Images", style = MaterialTheme.typography.titleSmall)
        SingleDocSelector(docs = docs, selected = selectedDoc, onSelect = { selectedDoc = it }, modifier = Modifier.weight(1f))

        Button(
            onClick = { onExport(selectedDoc) },
            colors = ButtonDefaults.buttonColors(containerColor = brand.teal),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("Export ${selectedDoc.pageCount} Page(s) as Images")
        }
    }
}

@Composable
private fun SingleDocSelector(
    docs: List<PdfDocumentEntity>,
    selected: PdfDocumentEntity?,
    onSelect: (PdfDocumentEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(docs, key = { it.id }) { doc ->
            val isSelected = selected?.id == doc.id
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) brand.crimson else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(doc) },
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${doc.fileName}.pdf", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${doc.pageCount} pages • ${Formatters.formatFileSize(doc.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = brand.crimson)
                    }
                }
            }
        }
    }
}
