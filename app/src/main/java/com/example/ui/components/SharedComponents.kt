package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.PdfDocumentEntity
import com.example.engine.BitmapProcessor
import com.example.model.DraftImageItem
import com.example.ui.theme.LocalBrandPalette
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfThumbnailBox(
    document: PdfDocumentEntity,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val thumbExists = remember(document.thumbnailPath) {
        document.thumbnailPath?.let { File(it).exists() } == true
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbExists && !document.isEncrypted) {
            AsyncImage(
                model = File(document.thumbnailPath!!),
                contentDescription = "${document.fileName} thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(6.dp),
            ) {
                Icon(
                    imageVector = if (document.isEncrypted) Icons.Filled.Lock else Icons.Filled.PictureAsPdf,
                    contentDescription = "PDF icon",
                    tint = brand.crimson,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (document.isEncrypted) "LOCKED" else "PDF",
                    style = MaterialTheme.typography.labelSmall,
                    color = brand.crimson,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Corner crimson PDF badge
        Surface(
            color = brand.crimson,
            shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            Text(
                text = "${document.pageCount}P",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
fun PdfDocumentRowCard(
    document: PdfDocumentEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PdfThumbnailBox(
                document = document,
                modifier = Modifier.size(width = 56.dp, height = 72.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${document.fileName}.pdf",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (document.isEncrypted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Password protected",
                            tint = LocalBrandPalette.current.crimson,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.formatDate(document.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetaPill(text = Formatters.formatFileSize(document.fileSizeBytes))
                    MetaPill(text = "${document.pageCount} ${if (document.pageCount == 1) "page" else "pages"}")
                }
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share ${document.fileName}",
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options for ${document.fileName}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PdfDocumentGridCard(
    document: PdfDocumentEntity,
    onOpen: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                PdfThumbnailBox(
                    document = document,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.82f),
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .clickable(onClick = onMoreClick),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${document.fileName}.pdf",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${Formatters.formatFileSize(document.fileSizeBytes)} • ${document.pageCount}p",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MetaPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun DraftImageRenderedPreview(
    item: DraftImageItem,
    maxDimension: Int = 540,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = item.workingFilePath,
        key2 = item.editConfig,
        key3 = item.previewRevision,
    ) {
        value = withContext(Dispatchers.Default) {
            val raw = BitmapProcessor.decodeSampledBitmapFromFile(item.workingFilePath, maxDimension)
                ?: return@withContext null
            val edited = BitmapProcessor.applyEdits(raw, item.editConfig)
            if (edited !== raw) raw.recycle()
            edited
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfOptionsBottomSheet(
    document: PdfDocumentEntity,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onSaveDownloads: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onSplit: () -> Unit,
    onReorderRotate: () -> Unit,
    onCompress: () -> Unit,
    onExportImages: () -> Unit,
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PdfThumbnailBox(
                    document = document,
                    modifier = Modifier.size(width = 44.dp, height = 56.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${document.fileName}.pdf",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${Formatters.formatFileSize(document.fileSizeBytes)} • ${document.pageCount} pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            SheetActionItem(Icons.Filled.OpenInNew, "Open in PDF Viewer", onClick = { onDismiss(); onOpen() })
            SheetActionItem(Icons.Filled.Share, "Share PDF", onClick = { onDismiss(); onShare() })
            SheetActionItem(Icons.Filled.Print, "Print PDF", onClick = { onDismiss(); onPrint() })
            SheetActionItem(Icons.Filled.Download, "Save Copy to Downloads", onClick = { onDismiss(); onSaveDownloads() })
            SheetActionItem(Icons.Filled.DriveFileRenameOutline, "Rename", onClick = { showRenameDialog = true })

            if (!document.isEncrypted) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                SheetActionItem(Icons.AutoMirrored.Filled.CallSplit, "Split PDF", onClick = { onDismiss(); onSplit() })
                SheetActionItem(Icons.Filled.RotateRight, "Reorder & Rotate Pages", onClick = { onDismiss(); onReorderRotate() })
                SheetActionItem(Icons.Filled.Compress, "Compress PDF", onClick = { onDismiss(); onCompress() })
                SheetActionItem(Icons.Filled.Image, "Export Pages as Images", onClick = { onDismiss(); onExportImages() })
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            SheetActionItem(
                icon = Icons.Filled.DeleteOutline,
                label = "Delete PDF",
                tint = MaterialTheme.colorScheme.error,
                onClick = { showDeleteConfirm = true },
            )
        }
    }

    if (showRenameDialog) {
        var nameInput by remember { mutableStateOf(document.fileName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename PDF") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("File name") },
                    singleLine = true,
                    suffix = { Text(".pdf") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            onRename(nameInput.trim())
                        }
                        showRenameDialog = false
                        onDismiss()
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete PDF?") },
            text = { Text("Remove \"${document.fileName}.pdf\" from your library and device storage?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()
                        onDelete()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SheetActionItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            fontWeight = FontWeight.Medium,
        )
    }
}
