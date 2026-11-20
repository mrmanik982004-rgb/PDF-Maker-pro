package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.BitmapProcessor
import com.example.model.ImageEditConfig
import com.example.model.NormalizedCropRect
import com.example.model.PhotoFilter
import com.example.ui.state.PhotoEditorUiState
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class EditorTab { FILTERS, ADJUST, CROP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    state: PhotoEditorUiState,
    onBack: () -> Unit,
    onUpdateConfig: (ImageEditConfig, Boolean) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    val brand = LocalBrandPalette.current
    var activeTab by remember { mutableStateOf(EditorTab.FILTERS) }

    // Decode base preview once (max 900px for responsive live filtering)
    val basePreview by produceState<Bitmap?>(initialValue = null, key1 = state.workingFilePath) {
        value = withContext(Dispatchers.IO) {
            BitmapProcessor.decodeSampledBitmapFromFile(state.workingFilePath, 900)
        }
    }

    // Small square thumbnail base for generating the 10 filter preview chips
    val thumbBase by produceState<Bitmap?>(initialValue = null, key1 = state.workingFilePath) {
        value = withContext(Dispatchers.IO) {
            BitmapProcessor.decodeSampledBitmapFromFile(state.workingFilePath, 160)
        }
    }

    // Render live edited preview whenever config changes
    val editedPreview by produceState<Bitmap?>(
        initialValue = null,
        key1 = basePreview,
        key2 = state.currentConfig,
        key3 = activeTab == EditorTab.CROP,
    ) {
        val src = basePreview ?: return@produceState
        // In CROP mode, show uncropped image with rotation/filter so user can drag crop edges freely
        val previewConfig = if (activeTab == EditorTab.CROP) {
            state.currentConfig.copy(cropRect = NormalizedCropRect())
        } else {
            state.currentConfig
        }
        value = withContext(Dispatchers.Default) {
            BitmapProcessor.applyEdits(src, previewConfig)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = brand.navyHero,
        topBar = {
            TopAppBar(
                title = { Text("Edit Photo", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onUndo,
                        enabled = state.canUndo,
                        modifier = Modifier.size(44.dp).testTag("editor_undo_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (state.canUndo) Color.White else Color.White.copy(alpha = 0.3f),
                        )
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = state.canRedo,
                        modifier = Modifier.size(44.dp).testTag("editor_redo_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (state.canRedo) Color.White else Color.White.copy(alpha = 0.3f),
                        )
                    }
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset edits", tint = Color.White)
                    }
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(38.dp)
                            .testTag("editor_save_btn"),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = brand.navyHero),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 12.dp, bottom = 14.dp),
                ) {
                    // Quick Rotate + Tool Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = activeTab == EditorTab.FILTERS,
                                onClick = { activeTab = EditorTab.FILTERS },
                                label = { Text("Filters") },
                                leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            )
                            FilterChip(
                                selected = activeTab == EditorTab.ADJUST,
                                onClick = { activeTab = EditorTab.ADJUST },
                                label = { Text("Adjust") },
                                leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            )
                            FilterChip(
                                selected = activeTab == EditorTab.CROP,
                                onClick = { activeTab = EditorTab.CROP },
                                label = { Text("Crop") },
                                leadingIcon = { Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            )
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    onUpdateConfig(
                                        state.currentConfig.copy(
                                            rotationDegrees = (state.currentConfig.rotationDegrees + 270) % 360
                                        ),
                                        true,
                                    )
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate Left 90 degrees")
                            }
                            IconButton(
                                onClick = {
                                    onUpdateConfig(
                                        state.currentConfig.copy(
                                            rotationDegrees = (state.currentConfig.rotationDegrees + 90) % 360
                                        ),
                                        true,
                                    )
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Right 90 degrees")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (activeTab) {
                        EditorTab.FILTERS -> {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(PhotoFilter.entries) { filter ->
                                    val selected = state.currentConfig.filter == filter
                                    FilterOptionThumb(
                                        filter = filter,
                                        thumbBase = thumbBase,
                                        selected = selected,
                                        accentColor = brand.crimson,
                                        onClick = {
                                            onUpdateConfig(state.currentConfig.copy(filter = filter), true)
                                        },
                                    )
                                }
                            }
                        }

                        EditorTab.ADJUST -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                AdjustmentSliderRow(
                                    label = "Brightness",
                                    valueText = "${(state.currentConfig.brightness * 100).roundToInt()}%",
                                    value = state.currentConfig.brightness,
                                    valueRange = -0.45f..0.45f,
                                    accentColor = brand.crimson,
                                    onValueChange = {
                                        onUpdateConfig(state.currentConfig.copy(brightness = it), false)
                                    },
                                    onValueChangeFinished = {
                                        onUpdateConfig(state.currentConfig, true)
                                    },
                                )
                                AdjustmentSliderRow(
                                    label = "Contrast",
                                    valueText = "${(state.currentConfig.contrast * 100).roundToInt()}%",
                                    value = state.currentConfig.contrast,
                                    valueRange = 0.5f..1.9f,
                                    accentColor = brand.teal,
                                    onValueChange = {
                                        onUpdateConfig(state.currentConfig.copy(contrast = it), false)
                                    },
                                    onValueChangeFinished = {
                                        onUpdateConfig(state.currentConfig, true)
                                    },
                                )
                                AdjustmentSliderRow(
                                    label = "Saturation",
                                    valueText = "${(state.currentConfig.saturation * 100).roundToInt()}%",
                                    value = state.currentConfig.saturation,
                                    valueRange = 0f..2f,
                                    accentColor = brand.crimson,
                                    onValueChange = {
                                        onUpdateConfig(state.currentConfig.copy(saturation = it), false)
                                    },
                                    onValueChangeFinished = {
                                        onUpdateConfig(state.currentConfig, true)
                                    },
                                )
                            }
                        }

                        EditorTab.CROP -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Drag corners on image to crop",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            onUpdateConfig(
                                                state.currentConfig.copy(cropRect = NormalizedCropRect()),
                                                true,
                                            )
                                        },
                                    ) {
                                        Text("Reset Crop")
                                    }
                                    Button(
                                        onClick = { activeTab = EditorTab.FILTERS },
                                        colors = ButtonDefaults.buttonColors(containerColor = brand.teal),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("Done Crop")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = editedPreview
            if (bmp == null) {
                CircularProgressIndicator(color = brand.teal)
            } else {
                val imgAspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1).toFloat()
                val boxAspect = maxWidth.value / maxHeight.value.coerceAtLeast(1f)
                val renderW = if (imgAspect > boxAspect) maxWidth else maxHeight * imgAspect
                val renderH = if (imgAspect > boxAspect) maxWidth / imgAspect else maxHeight

                Box(modifier = Modifier.size(renderW, renderH)) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Live photo preview",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                    )

                    if (activeTab == EditorTab.CROP) {
                        var activeHandle by remember { mutableIntStateOf(-1) }
                        val crop = state.currentConfig.cropRect
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(crop) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val w = size.width.toFloat()
                                            val h = size.height.toFloat()
                                            val corners = listOf(
                                                Offset(crop.left * w, crop.top * h),
                                                Offset(crop.right * w, crop.top * h),
                                                Offset(crop.right * w, crop.bottom * h),
                                                Offset(crop.left * w, crop.bottom * h),
                                            )
                                            var best = -1
                                            var bestD = Float.MAX_VALUE
                                            corners.forEachIndexed { idx, p ->
                                                val d = hypot(offset.x - p.x, offset.y - p.y)
                                                if (d < bestD && d < 120f) {
                                                    bestD = d
                                                    best = idx
                                                }
                                            }
                                            activeHandle = best
                                        },
                                        onDragEnd = { activeHandle = -1 },
                                        onDragCancel = { activeHandle = -1 },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            if (activeHandle == -1) return@detectDragGestures
                                            val nx = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            val ny = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                            val cur = state.currentConfig.cropRect
                                            val updated = when (activeHandle) {
                                                0 -> cur.copy(left = nx.coerceAtMost(cur.right - 0.12f), top = ny.coerceAtMost(cur.bottom - 0.12f))
                                                1 -> cur.copy(right = nx.coerceAtLeast(cur.left + 0.12f), top = ny.coerceAtMost(cur.bottom - 0.12f))
                                                2 -> cur.copy(right = nx.coerceAtLeast(cur.left + 0.12f), bottom = ny.coerceAtLeast(cur.top + 0.12f))
                                                3 -> cur.copy(left = nx.coerceAtMost(cur.right - 0.12f), bottom = ny.coerceAtLeast(cur.top + 0.12f))
                                                else -> cur
                                            }
                                            onUpdateConfig(state.currentConfig.copy(cropRect = updated), false)
                                        },
                                    )
                                },
                        ) {
                            val l = crop.left * size.width
                            val t = crop.top * size.height
                            val r = crop.right * size.width
                            val b = crop.bottom * size.height

                            // Darken outside crop box
                            drawRect(Color(0x88000000), Offset.Zero, Size(size.width, t))
                            drawRect(Color(0x88000000), Offset(0f, b), Size(size.width, size.height - b))
                            drawRect(Color(0x88000000), Offset(0f, t), Size(l, b - t))
                            drawRect(Color(0x88000000), Offset(r, t), Size(size.width - r, b - t))

                            drawRect(
                                color = Color(0xFF14B8A6),
                                topLeft = Offset(l, t),
                                size = Size(r - l, b - t),
                                style = Stroke(width = 4f),
                            )
                            listOf(Offset(l, t), Offset(r, t), Offset(r, b), Offset(l, b)).forEach { pt ->
                                drawCircle(Color.White, radius = 18f, center = pt)
                                drawCircle(Color(0xFFD92638), radius = 11f, center = pt)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterOptionThumb(
    filter: PhotoFilter,
    thumbBase: Bitmap?,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val previewBmp by produceState<Bitmap?>(initialValue = null, key1 = thumbBase, key2 = filter) {
        val base = thumbBase ?: return@produceState
        value = withContext(Dispatchers.Default) {
            BitmapProcessor.applyEdits(base, ImageEditConfig(filter = filter))
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) accentColor else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp),
                )
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (previewBmp != null) {
                Image(
                    bitmap = previewBmp!!.asImageBitmap(),
                    contentDescription = filter.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = filter.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AdjustmentSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = accentColor)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
            ),
            modifier = Modifier.height(32.dp),
        )
    }
}
