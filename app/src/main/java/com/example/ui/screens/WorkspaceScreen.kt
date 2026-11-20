package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.model.DraftImageItem
import com.example.model.PdfPageSettings
import com.example.model.PhotoFilter
import com.example.ui.components.DraftImageRenderedPreview
import com.example.ui.theme.LocalBrandPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    draftImages: List<DraftImageItem>,
    isImporting: Boolean,
    pageSettings: PdfPageSettings,
    onBack: () -> Unit,
    onAddPhotosClick: () -> Unit,
    onCameraScanClick: () -> Unit,
    onEditImageClick: (DraftImageItem) -> Unit,
    onRemoveImageClick: (String) -> Unit,
    onMoveImage: (Int, Int) -> Unit,
    onClearAll: () -> Unit,
    onOpenPageSettings: () -> Unit,
    onCreatePdfClick: () -> Unit,
) {
    val brand = LocalBrandPalette.current
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<String, Int>() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Create PDF Workspace", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${draftImages.size} ${if (draftImages.size == 1) "page" else "pages"} • Drag & drop to reorder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (draftImages.isNotEmpty()) {
                        TextButton(onClick = onClearAll) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Page Settings summary chip + Add buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenPageSettings)
                                .testTag("workspace_page_settings_btn"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    tint = brand.teal,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Page Settings",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "${pageSettings.pageSize.label} • ${pageSettings.orientation.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onAddPhotosClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("workspace_add_photos_btn"),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Add photos", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }

                        OutlinedButton(
                            onClick = onCameraScanClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("workspace_camera_btn"),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Scan page", modifier = Modifier.size(20.dp))
                        }
                    }

                    Button(
                        onClick = onCreatePdfClick,
                        enabled = draftImages.isNotEmpty() && !isImporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brand.crimson,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("create_pdf_cta"),
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create PDF (${draftImages.size} ${if (draftImages.size == 1) "Page" else "Pages"})",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (draftImages.isEmpty() && !isImporting) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = brand.crimson.copy(alpha = 0.12f),
                        modifier = Modifier.size(80.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                tint = brand.crimson,
                                modifier = Modifier.size(38.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Images Selected", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pick photos from your gallery or scan pages with the camera to start building your PDF.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onAddPhotosClick,
                            colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Photos")
                        }
                        Button(
                            onClick = onCameraScanClick,
                            colors = ButtonDefaults.buttonColors(containerColor = brand.teal),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera Scan")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(draftImages, key = { _, item -> item.id }) { index, item ->
                        val isDragging = draggedItemId == item.id
                        val translationY = if (isDragging) dragOffsetY else 0f

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isDragging) 10.dp else 1.dp,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 10f else 0f)
                                .onSizeChanged { itemHeights[item.id] = it.height }
                                .graphicsLayer {
                                    this.translationY = translationY
                                    scaleX = if (isDragging) 1.02f else 1f
                                    scaleY = if (isDragging) 1.02f else 1f
                                }
                                .border(
                                    width = if (isDragging) 2.dp else 1.dp,
                                    color = if (isDragging) brand.teal else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(18.dp),
                                )
                                .pointerInput(item.id, draftImages.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedItemId = item.id
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            draggedItemId = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedItemId = null
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val currentIndex = draftImages.indexOfFirst { it.id == item.id }
                                            if (currentIndex == -1) return@detectDragGesturesAfterLongPress
                                            val avgHeight = (itemHeights[item.id] ?: 240).toFloat()
                                            if (dragOffsetY > avgHeight * 0.65f && currentIndex < draftImages.lastIndex) {
                                                onMoveImage(currentIndex, currentIndex + 1)
                                                dragOffsetY -= avgHeight
                                            } else if (dragOffsetY < -avgHeight * 0.65f && currentIndex > 0) {
                                                onMoveImage(currentIndex, currentIndex - 1)
                                                dragOffsetY += avgHeight
                                            }
                                        },
                                    )
                                }
                                .clickable { onEditImageClick(item) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 76.dp, height = 98.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                ) {
                                    DraftImageRenderedPreview(
                                        item = item,
                                        maxDimension = 360,
                                        contentDescription = "Page ${index + 1} preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Surface(
                                        color = brand.navyHero.copy(alpha = 0.88f),
                                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                                        modifier = Modifier.align(Alignment.TopStart),
                                    ) {
                                        Text(
                                            text = String.format("%02d", index + 1),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val filterText = if (item.editConfig.filter == PhotoFilter.ORIGINAL) {
                                        "Original"
                                    } else {
                                        item.editConfig.filter.displayName
                                    }
                                    Text(
                                        text = "$filterText • Rot ${item.editConfig.rotationDegrees}°",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onEditImageClick(item) },
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Edit photo",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Filter & Edit",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    IconButton(
                                        onClick = { onRemoveImageClick(item.id) },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove page ${index + 1}",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = { if (index > 0) onMoveImage(index, index - 1) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowUpward,
                                                contentDescription = "Move up",
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                        IconButton(
                                            onClick = { if (index < draftImages.lastIndex) onMoveImage(index, index + 1) },
                                            enabled = index < draftImages.lastIndex,
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowDownward,
                                                contentDescription = "Move down",
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.DragHandle,
                                        contentDescription = "Long press and drag to reorder",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isImporting) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Text("Importing photos...", style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
