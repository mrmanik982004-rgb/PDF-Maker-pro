package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.PdfDocumentEntity
import com.example.model.CompressionLevel
import com.example.model.ImageQualityPreset
import com.example.model.PageSizeOption
import com.example.model.PdfCreateOptions
import com.example.model.PdfPageSettings
import com.example.ui.components.PdfThumbnailBox
import com.example.ui.state.PdfCreationUiState
import com.example.ui.theme.LocalBrandPalette
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreatePdfModalSheet(
    creationState: PdfCreationUiState,
    initialSettings: PdfPageSettings,
    pageCount: Int,
    onDismiss: () -> Unit,
    onConfirmCreate: (PdfCreateOptions) -> Unit,
    onOpenCreatedPdf: (PdfDocumentEntity) -> Unit,
    onShareCreatedPdf: (PdfDocumentEntity) -> Unit,
    onSaveCreatedToDownloads: (PdfDocumentEntity) -> Unit,
    onDoneAfterSuccess: () -> Unit,
) {
    val brand = LocalBrandPalette.current
    var fileName by remember { mutableStateOf(Formatters.defaultPdfFileName()) }
    var selectedPageSize by remember(initialSettings) { mutableStateOf(initialSettings.pageSize) }
    var selectedQuality by remember(initialSettings) { mutableStateOf(initialSettings.qualityPreset) }
    var compressionLevel by remember { mutableStateOf(CompressionLevel.LOW) }
    var passwordEnabled by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var includePageNumbers by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            if (creationState !is PdfCreationUiState.Creating) onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            when (creationState) {
                is PdfCreationUiState.Idle, is PdfCreationUiState.Error -> {
                    Text(
                        text = "Export PDF Document",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "$pageCount ${if (pageCount == 1) "page" else "pages"} ready to compile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (creationState is PdfCreationUiState.Error) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = creationState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("PDF File Name") },
                        suffix = { Text(".pdf") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_filename_input"),
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Page Size", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PageSizeOption.entries.forEach { size ->
                            FilterChip(
                                selected = selectedPageSize == size,
                                onClick = { selectedPageSize = size },
                                label = { Text(size.label) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Image Quality", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ImageQualityPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = selectedQuality == preset,
                                onClick = { selectedQuality = preset },
                                label = { Text(preset.label) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("PDF Compression", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CompressionLevel.entries.forEach { level ->
                            FilterChip(
                                selected = compressionLevel == level,
                                onClick = { compressionLevel = level },
                                label = { Text(level.label) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Add Page Numbers", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Display Page 1 of $pageCount at bottom",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = includePageNumbers,
                            onCheckedChange = { includePageNumbers = it },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = brand.crimson,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Password Protection", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = "Encrypt PDF with a password",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = passwordEnabled,
                            onCheckedChange = { passwordEnabled = it },
                        )
                    }

                    if (passwordEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Enter PDF Password") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Toggle password visibility",
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            onConfirmCreate(
                                PdfCreateOptions(
                                    fileName = fileName,
                                    pageSettings = initialSettings.copy(
                                        pageSize = selectedPageSize,
                                        qualityPreset = selectedQuality,
                                    ),
                                    compressionLevel = compressionLevel,
                                    passwordEnabled = passwordEnabled,
                                    password = password,
                                    includePageNumbers = includePageNumbers,
                                )
                            )
                        },
                        enabled = !passwordEnabled || password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brand.crimson,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_create_pdf_btn"),
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate PDF Now")
                    }
                }

                is PdfCreationUiState.Creating -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = brand.crimson.copy(alpha = 0.12f),
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = brand.crimson,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Creating Your PDF...", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = creationState.status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        LinearProgressIndicator(
                            progress = { creationState.progress },
                            color = brand.crimson,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(creationState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                is PdfCreationUiState.Success -> {
                    val doc = creationState.document
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = brand.success.copy(alpha = 0.14f),
                            modifier = Modifier.size(68.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "PDF created successfully",
                                    tint = brand.success,
                                    modifier = Modifier.size(38.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PDF Created Successfully!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PdfThumbnailBox(
                                    document = doc,
                                    modifier = Modifier.size(width = 52.dp, height = 68.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${doc.fileName}.pdf",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${Formatters.formatFileSize(doc.fileSizeBytes)} • ${doc.pageCount} pages" +
                                            if (doc.isEncrypted) " • Password Locked" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { onOpenCreatedPdf(doc) },
                                colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open PDF")
                            }
                            Button(
                                onClick = { onShareCreatedPdf(doc) },
                                colors = ButtonDefaults.buttonColors(containerColor = brand.teal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onSaveCreatedToDownloads(doc) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Downloads")
                            }
                            TextButton(
                                onClick = onDoneAfterSuccess,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        }
    }
}
