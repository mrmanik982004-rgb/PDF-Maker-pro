package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.engine.BitmapProcessor
import com.example.engine.PdfEngine
import com.example.model.DraftImageItem
import com.example.model.ImageAlignment
import com.example.model.ImageFitMode
import com.example.model.ImageQualityPreset
import com.example.model.PageBackgroundColor
import com.example.model.PageOrientation
import com.example.model.PageSizeOption
import com.example.model.PdfPageSettings
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PageSettingsScreen(
    settings: PdfPageSettings,
    draftImages: List<DraftImageItem>,
    onUpdateSettings: ((PdfPageSettings) -> PdfPageSettings) -> Unit,
    onBack: () -> Unit,
) {
    val brand = LocalBrandPalette.current
    var previewIndex by remember { mutableIntStateOf(0) }
    val safeIndex = previewIndex.coerceIn(0, max(0, draftImages.lastIndex))
    val currentDraft = draftImages.getOrNull(safeIndex)

    val previewBmp by produceState<Bitmap?>(
        initialValue = null,
        key1 = currentDraft?.workingFilePath,
        key2 = currentDraft?.editConfig,
    ) {
        val draft = currentDraft ?: return@produceState
        value = withContext(Dispatchers.Default) {
            val raw = BitmapProcessor.decodeSampledBitmapFromFile(draft.workingFilePath, 600)
                ?: return@withContext null
            val edited = BitmapProcessor.applyEdits(raw, draft.editConfig)
            if (edited !== raw) raw.recycle()
            edited
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("PDF Page Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .height(38.dp)
                            .testTag("page_settings_done_btn"),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            // Live PDF Page Preview Stage
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val imgW = previewBmp?.width ?: 600
                        val imgH = previewBmp?.height ?: 800
                        val pageRect = PdfEngine.resolvePageRect(settings, imgW, imgH)
                        val pageAspect = pageRect.width / max(1f, pageRect.height)
                        val boxAspect = maxWidth.value / max(1f, maxHeight.value)

                        val cardW = if (pageAspect > boxAspect) maxWidth * 0.92f else (maxHeight * 0.92f) * pageAspect
                        val cardH = if (pageAspect > boxAspect) (maxWidth * 0.92f) / pageAspect else maxHeight * 0.92f

                        val density = LocalDensity.current
                        val cardWPx = with(density) { cardW.toPx() }
                        val cardHPx = with(density) { cardH.toPx() }
                        val scaleFactor = cardWPx / max(1f, pageRect.width)

                        val drawRect = PdfEngine.computeImageDrawRect(
                            pageWidth = pageRect.width,
                            pageHeight = pageRect.height,
                            imageWidth = imgW.toFloat(),
                            imageHeight = imgH.toFloat(),
                            settings = settings,
                        )

                        val borderInsetDp = with(density) {
                            (max(6f, settings.marginPt * 0.5f) * scaleFactor).toDp()
                        }
                        val borderWidthDp = with(density) {
                            (settings.borderWidthPt * scaleFactor).coerceAtLeast(1f).toDp()
                        }

                        Box(
                            modifier = Modifier
                                .size(cardW, cardH)
                                .shadow(6.dp, RoundedCornerShape(4.dp))
                                .background(settings.backgroundColor.composeColor)
                                .clip(RoundedCornerShape(4.dp)),
                        ) {
                            if (previewBmp != null) {
                                val imgLeftPx = (drawRect.left * scaleFactor).roundToInt()
                                val imgTopPx = (drawRect.top * scaleFactor).roundToInt()
                                val imgWidthDp = with(density) { (drawRect.width() * scaleFactor).toDp() }
                                val imgHeightDp = with(density) { (drawRect.height() * scaleFactor).toDp() }

                                Image(
                                    bitmap = previewBmp!!.asImageBitmap(),
                                    contentDescription = "Live page layout preview",
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier
                                        .offset { IntOffset(imgLeftPx, imgTopPx) }
                                        .size(imgWidthDp, imgHeightDp),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("Preview Page", color = Color(0xFF475569))
                                }
                            }

                            if (settings.borderEnabled) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(borderInsetDp)
                                        .border(borderWidthDp, Color(settings.borderColorArgb)),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (draftImages.size > 1) {
                            IconButton(
                                onClick = { previewIndex = (safeIndex - 1).coerceAtLeast(0) },
                                enabled = safeIndex > 0,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous preview page")
                            }
                        }
                        Text(
                            text = "LIVE PREVIEW • ${settings.pageSize.label} (${settings.orientation.label})" +
                                if (draftImages.isNotEmpty()) " • Page ${safeIndex + 1}/${draftImages.size}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (draftImages.size > 1) {
                            IconButton(
                                onClick = { previewIndex = (safeIndex + 1).coerceAtMost(draftImages.lastIndex) },
                                enabled = safeIndex < draftImages.lastIndex,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next preview page")
                            }
                        }
                    }
                }
            }

            // Scrollable Page Settings Controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Page Size
                SectionHeader("Page Size")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PageSizeOption.entries.forEach { option ->
                        FilterChip(
                            selected = settings.pageSize == option,
                            onClick = { onUpdateSettings { it.copy(pageSize = option) } },
                            label = { Text(option.label) },
                        )
                    }
                }

                if (settings.pageSize == PageSizeOption.CUSTOM) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = settings.customWidthMm.toString(),
                            onValueChange = { txt ->
                                val num = txt.filter { it.isDigit() }.toIntOrNull() ?: 210
                                onUpdateSettings { it.copy(customWidthMm = num.coerceIn(40, 1200)) }
                            },
                            label = { Text("Width (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = settings.customHeightMm.toString(),
                            onValueChange = { txt ->
                                val num = txt.filter { it.isDigit() }.toIntOrNull() ?: 297
                                onUpdateSettings { it.copy(customHeightMm = num.coerceIn(40, 1200)) }
                            },
                            label = { Text("Height (mm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Orientation
                SectionHeader("Page Orientation")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PageOrientation.entries.forEach { orient ->
                        FilterChip(
                            selected = settings.orientation == orient,
                            onClick = { onUpdateSettings { it.copy(orientation = orient) } },
                            label = { Text(orient.label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Page Margin
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SectionHeader("Page Margin")
                        Text(
                            text = "${settings.marginPt.roundToInt()} pt",
                            style = MaterialTheme.typography.labelSmall,
                            color = brand.teal,
                        )
                    }
                    Slider(
                        value = settings.marginPt,
                        onValueChange = { v -> onUpdateSettings { it.copy(marginPt = v) } },
                        valueRange = 0f..56f,
                        colors = SliderDefaults.colors(thumbColor = brand.teal, activeTrackColor = brand.teal),
                    )
                }

                // Image Fit Mode
                SectionHeader("Image Scaling & Fit")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ImageFitMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.fitMode == mode,
                            onClick = { onUpdateSettings { it.copy(fitMode = mode) } },
                            label = { Text(mode.label) },
                        )
                    }
                }

                // Image Alignment
                SectionHeader("Image Alignment")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ImageAlignment.entries.forEach { align ->
                        FilterChip(
                            selected = settings.alignment == align,
                            onClick = { onUpdateSettings { it.copy(alignment = align) } },
                            label = { Text(align.label) },
                        )
                    }
                }

                // Page Background Color
                SectionHeader("Page Background Color")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PageBackgroundColor.entries.forEach { bg ->
                        val selected = settings.backgroundColor == bg
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onUpdateSettings { it.copy(backgroundColor = bg) } }
                                .padding(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(bg.composeColor)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) brand.crimson else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape,
                                    ),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = bg.label.split(" ").last(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Page Border
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Page Border", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Draw a crisp frame around each page",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.borderEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(borderEnabled = enabled) }
                        },
                    )
                }

                if (settings.borderEnabled) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Border Thickness", style = MaterialTheme.typography.bodyMedium)
                            Text("${settings.borderWidthPt.roundToInt()} pt", style = MaterialTheme.typography.labelSmall)
                        }
                        Slider(
                            value = settings.borderWidthPt,
                            onValueChange = { v -> onUpdateSettings { it.copy(borderWidthPt = v) } },
                            valueRange = 1f..8f,
                            colors = SliderDefaults.colors(thumbColor = brand.crimson, activeTrackColor = brand.crimson),
                        )
                    }
                }

                // Image Quality
                SectionHeader("Image Quality")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ImageQualityPreset.entries.forEach { q ->
                        FilterChip(
                            selected = settings.qualityPreset == q,
                            onClick = { onUpdateSettings { it.copy(qualityPreset = q) } },
                            label = { Text(q.label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
}
