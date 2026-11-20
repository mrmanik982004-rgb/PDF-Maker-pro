package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PdfDocumentEntity
import com.example.engine.PdfEngine
import com.example.ui.theme.LocalBrandPalette
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    document: PdfDocumentEntity?,
    onBack: () -> Unit,
    onShare: (File) -> Unit,
    onPrint: (File) -> Unit,
) {
    val brand = LocalBrandPalette.current
    val scope = rememberCoroutineScope()

    if (document == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = brand.crimson)
        }
        return
    }

    val pdfFile = remember(document.filePath) { File(document.filePath) }
    var passwordInput by remember { mutableStateOf("") }
    var activePassword by remember { mutableStateOf("") }
    var needsPassword by remember(document.id) { mutableStateOf(document.isEncrypted) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var pageCount by remember { mutableIntStateOf(document.pageCount.coerceAtLeast(1)) }
    var currentPage by remember { mutableIntStateOf(0) }
    var renderedPage by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(false) }

    var fullScreen by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val nextScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
        zoomScale = nextScale
        panOffset = if (nextScale <= 1.02f) {
            Offset.Zero
        } else {
            panOffset + offsetChange
        }
    }

    fun loadPage(pageIdx: Int, pwd: String) {
        scope.launch {
            isLoadingPage = true
            val meta = runCatching { PdfEngine.getPdfMetadata(pdfFile, pwd) }.getOrNull()
            if (meta == null) {
                isLoadingPage = false
                needsPassword = true
                if (pwd.isNotEmpty()) passwordError = "Incorrect password. Try again."
                return@launch
            }
            pageCount = meta.first.coerceAtLeast(1)
            val safeIdx = pageIdx.coerceIn(0, pageCount - 1)
            val bmp = PdfEngine.renderPdfPage(pdfFile, safeIdx, maxWidthPx = 1500, password = pwd)
            renderedPage?.recycle()
            renderedPage = bmp
            currentPage = safeIdx
            needsPassword = false
            passwordError = null
            isLoadingPage = false
        }
    }

    LaunchedEffect(document.id) {
        if (!document.isEncrypted) {
            loadPage(0, "")
        }
    }

    if (needsPassword) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = brand.crimson.copy(alpha = 0.14f),
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = brand.crimson,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Password Protected PDF", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter password to open \"${document.fileName}.pdf\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            activePassword = passwordInput
                            loadPage(0, passwordInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brand.crimson),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text("Unlock")
                    }
                }
            }
        }
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = if (fullScreen) Color.Black else brand.navyHero,
        topBar = {
            AnimatedVisibility(visible = !fullScreen, enter = fadeIn(), exit = fadeOut()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "${document.fileName}.pdf",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Page ${currentPage + 1} of $pageCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { onShare(pdfFile) }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Filled.Share, contentDescription = "Share PDF", tint = Color.White)
                        }
                        IconButton(onClick = { onPrint(pdfFile) }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Filled.Print, contentDescription = "Print PDF", tint = Color.White)
                        }
                        IconButton(
                            onClick = { fullScreen = true },
                            modifier = Modifier.size(44.dp).testTag("viewer_fullscreen_btn"),
                        ) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = "Full screen mode", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = brand.navyHero),
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = !fullScreen, enter = fadeIn(), exit = fadeOut()) {
                Surface(color = brand.navyHeroElevated) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Page Navigation
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (currentPage > 0) {
                                        zoomScale = 1f; panOffset = Offset.Zero
                                        loadPage(currentPage - 1, activePassword)
                                    }
                                },
                                enabled = currentPage > 0,
                                modifier = Modifier.size(48.dp).testTag("viewer_prev_page"),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous page",
                                    tint = if (currentPage > 0) Color.White else Color.Gray,
                                )
                            }
                            Surface(
                                color = brand.crimson,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = "${currentPage + 1} / $pageCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (currentPage < pageCount - 1) {
                                        zoomScale = 1f; panOffset = Offset.Zero
                                        loadPage(currentPage + 1, activePassword)
                                    }
                                },
                                enabled = currentPage < pageCount - 1,
                                modifier = Modifier.size(48.dp).testTag("viewer_next_page"),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next page",
                                    tint = if (currentPage < pageCount - 1) Color.White else Color.Gray,
                                )
                            }
                        }

                        // Zoom Controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    zoomScale = (zoomScale - 0.5f).coerceAtLeast(1f)
                                    if (zoomScale <= 1.02f) panOffset = Offset.Zero
                                },
                                enabled = zoomScale > 1f,
                                modifier = Modifier.size(44.dp).testTag("viewer_zoom_out"),
                            ) {
                                Icon(
                                    Icons.Filled.ZoomOut,
                                    contentDescription = "Zoom out",
                                    tint = if (zoomScale > 1f) Color.White else Color.Gray,
                                )
                            }
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF5EEAD4),
                            )
                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.5f).coerceAtMost(4f) },
                                enabled = zoomScale < 4f,
                                modifier = Modifier.size(44.dp).testTag("viewer_zoom_in"),
                            ) {
                                Icon(
                                    Icons.Filled.ZoomIn,
                                    contentDescription = "Zoom in",
                                    tint = if (zoomScale < 4f) Color.White else Color.Gray,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoomScale > 1.2f) {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                            } else {
                                zoomScale = 2.2f
                            }
                        },
                        onTap = {
                            if (fullScreen) fullScreen = false
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isLoadingPage || renderedPage == null) {
                CircularProgressIndicator(color = brand.teal)
            } else {
                Image(
                    bitmap = renderedPage!!.asImageBitmap(),
                    contentDescription = "PDF page ${currentPage + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                        .transformable(state = transformState),
                )
            }

            if (fullScreen) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp),
                ) {
                    IconButton(onClick = { fullScreen = false }, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Filled.FullscreenExit,
                            contentDescription = "Exit full screen",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
