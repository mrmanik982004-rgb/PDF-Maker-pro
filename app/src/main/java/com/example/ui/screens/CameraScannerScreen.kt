package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.PdfMakerApp
import com.example.engine.BitmapProcessor
import com.example.model.QuadPoints
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun CameraScannerScreen(
    onCancel: () -> Unit,
    onFinishScanning: (List<File>) -> Unit,
    onPickFromGalleryInstead: () -> Unit,
) {
    val context = LocalContext.current
    val brand = LocalBrandPalette.current
    val scope = rememberCoroutineScope()
    val draftsDir = remember { (context.applicationContext as PdfMakerApp).libraryRepository.draftsDir }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val scannedFiles = remember { mutableStateListOf<File>() }
    var flashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedQuad by remember { mutableStateOf(QuadPoints()) }
    var isWarping by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brand.navyHero)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = brand.teal.copy(alpha = 0.18f),
                    modifier = Modifier.size(84.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = brand.teal,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Camera Access Needed",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Allow camera permission to scan paper documents, detect edges, and apply perspective correction offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = brand.teal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Grant Camera Permission")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onPickFromGalleryInstead,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Pick From Gallery Instead", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Back", color = Color(0xFF94A3B8))
                }
            }
        }
        return
    }

    if (pendingBitmap != null && pendingCaptureFile != null) {
        // Document Edge Detection & 4-Corner Perspective Crop Review
        val bmp = pendingBitmap!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090E1A))
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        pendingCaptureFile?.delete()
                        pendingCaptureFile = null
                        pendingBitmap?.recycle()
                        pendingBitmap = null
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Retake", tint = Color.White)
                }
                Text(
                    text = "Adjust Document Edges",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                TextButton(
                    onClick = {
                        detectedQuad = QuadPoints(
                            0.01f to 0.01f,
                            0.99f to 0.01f,
                            0.99f to 0.99f,
                            0.01f to 0.99f,
                        )
                    },
                ) {
                    Text("Full", color = Color(0xFF5EEAD4))
                }
            }

            // Interactive 4-Corner Perspective Polygon Overlay
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                val imgAspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1).toFloat()
                val boxAspect = maxWidth.value / maxHeight.value.coerceAtLeast(1f)
                val renderW = if (imgAspect > boxAspect) maxWidth else maxHeight * imgAspect
                val renderH = if (imgAspect > boxAspect) maxWidth / imgAspect else maxHeight

                Box(
                    modifier = Modifier.size(renderW, renderH),
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Captured document for cropping",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize(),
                    )

                    var activeCorner by remember { mutableIntStateOf(-1) }
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        val pts = listOf(
                                            Offset(detectedQuad.topLeft.first * w, detectedQuad.topLeft.second * h),
                                            Offset(detectedQuad.topRight.first * w, detectedQuad.topRight.second * h),
                                            Offset(detectedQuad.bottomRight.first * w, detectedQuad.bottomRight.second * h),
                                            Offset(detectedQuad.bottomLeft.first * w, detectedQuad.bottomLeft.second * h),
                                        )
                                        var nearestIdx = -1
                                        var bestDist = Float.MAX_VALUE
                                        pts.forEachIndexed { idx, p ->
                                            val d = hypot(offset.x - p.x, offset.y - p.y)
                                            if (d < bestDist && d < 110f) {
                                                bestDist = d
                                                nearestIdx = idx
                                            }
                                        }
                                        activeCorner = nearestIdx
                                    },
                                    onDragEnd = { activeCorner = -1 },
                                    onDragCancel = { activeCorner = -1 },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (activeCorner == -1) return@detectDragGestures
                                        val nx = (change.position.x / size.width.toFloat()).coerceIn(0.01f, 0.99f)
                                        val ny = (change.position.y / size.height.toFloat()).coerceIn(0.01f, 0.99f)
                                        detectedQuad = when (activeCorner) {
                                            0 -> detectedQuad.copy(topLeft = nx to ny)
                                            1 -> detectedQuad.copy(topRight = nx to ny)
                                            2 -> detectedQuad.copy(bottomRight = nx to ny)
                                            3 -> detectedQuad.copy(bottomLeft = nx to ny)
                                            else -> detectedQuad
                                        }
                                    },
                                )
                            },
                    ) {
                        val w = size.width
                        val h = size.height
                        val tl = Offset(detectedQuad.topLeft.first * w, detectedQuad.topLeft.second * h)
                        val tr = Offset(detectedQuad.topRight.first * w, detectedQuad.topRight.second * h)
                        val br = Offset(detectedQuad.bottomRight.first * w, detectedQuad.bottomRight.second * h)
                        val bl = Offset(detectedQuad.bottomLeft.first * w, detectedQuad.bottomLeft.second * h)

                        val path = Path().apply {
                            moveTo(tl.x, tl.y)
                            lineTo(tr.x, tr.y)
                            lineTo(br.x, br.y)
                            lineTo(bl.x, bl.y)
                            close()
                        }
                        drawPath(path, color = Color(0x3314B8A6))
                        drawPath(path, color = Color(0xFF14B8A6), style = Stroke(width = 6f, cap = StrokeCap.Round))

                        listOf(tl, tr, br, bl).forEach { corner ->
                            drawCircle(color = Color.White, radius = 22f, center = corner)
                            drawCircle(color = Color(0xFFD92638), radius = 14f, center = corner)
                        }
                    }
                }
            }

            // Bottom Actions
            Surface(color = brand.navyHero) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch(Dispatchers.Default) {
                                val auto = BitmapProcessor.detectDocumentQuad(bmp)
                                withContext(Dispatchers.Main) { detectedQuad = auto }
                            }
                        },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = Color(0xFF5EEAD4))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            pendingCaptureFile?.delete()
                            pendingCaptureFile = null
                            pendingBitmap?.recycle()
                            pendingBitmap = null
                        },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Retake", color = Color.White)
                    }

                    Button(
                        enabled = !isWarping,
                        onClick = {
                            isWarping = true
                            val rawFile = pendingCaptureFile!!
                            val quad = detectedQuad
                            scope.launch(Dispatchers.IO) {
                                val fullRes = BitmapProcessor.decodeSampledBitmapFromFile(rawFile.absolutePath, 2400) ?: bmp
                                val warped = BitmapProcessor.applyPerspectiveWarp(fullRes, quad)
                                if (fullRes !== bmp) fullRes.recycle()
                                if (!draftsDir.exists()) draftsDir.mkdirs()
                                val finalScan = File(draftsDir, "scan_${UUID.randomUUID()}.jpg")
                                FileOutputStream(finalScan).use { fos ->
                                    warped.compress(Bitmap.CompressFormat.JPEG, 93, fos)
                                }
                                warped.recycle()
                                rawFile.delete()
                                withContext(Dispatchers.Main) {
                                    scannedFiles.add(finalScan)
                                    pendingCaptureFile = null
                                    pendingBitmap?.recycle()
                                    pendingBitmap = null
                                    isWarping = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_scan_crop_btn"),
                    ) {
                        if (isWarping) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm")
                        }
                    }
                }
            }
        }
        return
    }

    // Live CameraX Capture Mode
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember(flashOn) {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            .build()
    }

    DisposableEffect(lifecycleOwner, imageCapture) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener({
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            }.onFailure {
                cameraError = it.message ?: "Camera unavailable on this device."
            }
        }, executor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        // Subtle document alignment corner frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padX = size.width * 0.1f
            val padY = size.height * 0.18f
            val len = min(size.width, size.height) * 0.08f
            val stroke = Stroke(width = 6f, cap = StrokeCap.Round)
            val frameColor = Color(0xAA14B8A6)

            // Top-left
            drawLine(frameColor, Offset(padX, padY), Offset(padX + len, padY), stroke.width)
            drawLine(frameColor, Offset(padX, padY), Offset(padX, padY + len), stroke.width)
            // Top-right
            drawLine(frameColor, Offset(size.width - padX, padY), Offset(size.width - padX - len, padY), stroke.width)
            drawLine(frameColor, Offset(size.width - padX, padY), Offset(size.width - padX, padY + len), stroke.width)
            // Bottom-left
            drawLine(frameColor, Offset(padX, size.height - padY), Offset(padX + len, size.height - padY), stroke.width)
            drawLine(frameColor, Offset(padX, size.height - padY), Offset(padX, size.height - padY - len), stroke.width)
            // Bottom-right
            drawLine(frameColor, Offset(size.width - padX, size.height - padY), Offset(size.width - padX - len, size.height - padY), stroke.width)
            drawLine(frameColor, Offset(size.width - padX, size.height - padY), Offset(size.width - padX, size.height - padY - len), stroke.width)
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close scanner", tint = Color.White)
            }

            Surface(
                color = brand.navyHero.copy(alpha = 0.85f),
                shape = RoundedCornerShape(50),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CropFree,
                        contentDescription = null,
                        tint = Color(0xFF5EEAD4),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Document Scanner",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }

            IconButton(
                onClick = { flashOn = !flashOn },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = if (flashOn) "Flash On" else "Flash Off",
                    tint = if (flashOn) Color(0xFFFDE047) else Color.White,
                )
            }
        }

        if (cameraError != null) {
            Surface(
                color = brand.navyHero.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Camera Preview Unavailable", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(cameraError!!, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onPickFromGalleryInstead,
                        colors = ButtonDefaults.buttonColors(containerColor = brand.teal),
                    ) {
                        Text("Import From Gallery Instead")
                    }
                }
            }
        }

        // Bottom Controls + Continuous Batch Strip
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(bottom = 18.dp, top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (scannedFiles.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(scannedFiles) { idx, file ->
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color(0xFF14B8A6), RoundedCornerShape(8.dp)),
                        ) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Scanned page ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onPickFromGalleryInstead) {
                    Text("Gallery", color = Color.White)
                }

                // Shutter button
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(4.dp, brand.crimson, CircleShape)
                        .clickable(enabled = !isCapturing) {
                            isCapturing = true
                            val rawFile = File(context.cacheDir, "raw_scan_${System.currentTimeMillis()}.jpg")
                            val outputOpts = ImageCapture.OutputFileOptions.Builder(rawFile).build()
                            imageCapture.takePicture(
                                outputOpts,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        scope.launch(Dispatchers.IO) {
                                            BitmapProcessor.fixFileExifOrientation(rawFile.absolutePath)
                                            val previewBmp = BitmapProcessor.decodeSampledBitmapFromFile(
                                                rawFile.absolutePath,
                                                1200,
                                            )
                                            val quad = previewBmp?.let { BitmapProcessor.detectDocumentQuad(it) }
                                                ?: QuadPoints()
                                            withContext(Dispatchers.Main) {
                                                isCapturing = false
                                                if (previewBmp != null) {
                                                    pendingCaptureFile = rawFile
                                                    pendingBitmap = previewBmp
                                                    detectedQuad = quad
                                                }
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                        cameraError = exception.message
                                    }
                                },
                            )
                        }
                        .testTag("camera_shutter_btn"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = brand.crimson,
                                strokeWidth = 3.dp,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, brand.navyHero, CircleShape),
                            )
                        }
                    }
                }

                if (scannedFiles.isNotEmpty()) {
                    Button(
                        onClick = { onFinishScanning(scannedFiles.toList()) },
                        colors = ButtonDefaults.buttonColors(containerColor = brand.teal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("finish_scanning_btn"),
                    ) {
                        Text("Next (${scannedFiles.size})")
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }
            }
        }
    }
}
