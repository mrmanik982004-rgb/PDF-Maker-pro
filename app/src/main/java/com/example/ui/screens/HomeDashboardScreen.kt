package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PdfDocumentEntity
import com.example.ui.components.PdfDocumentRowCard
import com.example.ui.theme.LocalBrandPalette

@Composable
fun HomeDashboardScreen(
    totalPdfCount: Int,
    draftCount: Int,
    recentDocuments: List<PdfDocumentEntity>,
    onImageToPdfClick: () -> Unit,
    onCameraToPdfClick: () -> Unit,
    onMyPdfsClick: () -> Unit,
    onRecentFilesClick: () -> Unit,
    onMergePdfClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onResumeDraftClick: () -> Unit,
    onOpenDocument: (PdfDocumentEntity) -> Unit,
    onShareDocument: (PdfDocumentEntity) -> Unit,
    onMoreDocument: (PdfDocumentEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top App Branding + Total PDFs Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = brand.crimson,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = "PDF Maker logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PDF Maker",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Image to PDF",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onMyPdfsClick)
                        .testTag("total_pdfs_pill"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(brand.crimson),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$totalPdfCount PDFs",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // Hero Banner Card (Dark Navy Blue + Crimson & Teal Accents + Generated Artwork)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = brand.navyHero),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_scanner),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.28f),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        brand.navyHero.copy(alpha = 0.94f),
                                        brand.navyHero.copy(alpha = 0.72f),
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Surface(
                            color = brand.teal.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "OFFLINE STUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF5EEAD4),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Convert Photos & Scans to Crisp PDFs",
                            style = MaterialTheme.typography.headlineSmall,
                            color = brand.onHero,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Auto edge crop, 10 pro filters, custom page layouts & password lock.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = brand.onHeroMuted,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Button(
                                onClick = onImageToPdfClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brand.crimson,
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hero_image_to_pdf_btn"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Collections,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Photos", maxLines = 1)
                            }
                            Button(
                                onClick = onCameraToPdfClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brand.teal,
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hero_camera_scan_btn"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Doc", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Active Workspace Draft Banner if user already has staged pages
        if (draftCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onResumeDraftClick),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unsaved PDF Draft Ready",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "$draftCount ${if (draftCount == 1) "image" else "images"} in workspace",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Resume draft",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        // Quick Actions Dashboard Grid (6 Core Features)
        item {
            Text(
                text = "Quick Workspace",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(10.dp))

            val tiles = listOf(
                DashboardTileSpec(
                    title = "Image to PDF",
                    subtitle = "Pick gallery photos",
                    icon = Icons.Filled.Collections,
                    accentColor = brand.crimson,
                    testTag = "tile_image_to_pdf",
                    onClick = onImageToPdfClick,
                ),
                DashboardTileSpec(
                    title = "Camera to PDF",
                    subtitle = "Auto edge scanner",
                    icon = Icons.Filled.CameraAlt,
                    accentColor = brand.teal,
                    testTag = "tile_camera_to_pdf",
                    onClick = onCameraToPdfClick,
                ),
                DashboardTileSpec(
                    title = "My PDFs",
                    subtitle = "$totalPdfCount saved files",
                    icon = Icons.Filled.FolderOpen,
                    accentColor = brand.navyHeroElevated,
                    testTag = "tile_my_pdfs",
                    onClick = onMyPdfsClick,
                ),
                DashboardTileSpec(
                    title = "Recent Files",
                    subtitle = "Quick open latest",
                    icon = Icons.Filled.History,
                    accentColor = brand.teal,
                    testTag = "tile_recent_files",
                    onClick = onRecentFilesClick,
                ),
                DashboardTileSpec(
                    title = "Merge PDF",
                    subtitle = "Combine multiple",
                    icon = Icons.Filled.MergeType,
                    accentColor = brand.crimson,
                    testTag = "tile_merge_pdf",
                    onClick = onMergePdfClick,
                ),
                DashboardTileSpec(
                    title = "Settings",
                    subtitle = "Theme & defaults",
                    icon = Icons.Filled.Settings,
                    accentColor = brand.navyHeroElevated,
                    testTag = "tile_settings",
                    onClick = onSettingsClick,
                ),
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 600.dp) 3 else 2
                val rows = tiles.chunked(columns)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowItems.forEach { tile ->
                                DashboardFeatureCard(
                                    spec = tile,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(columns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Recent Files Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Recent Files",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (recentDocuments.isNotEmpty()) {
                    TextButton(onClick = onMyPdfsClick) {
                        Text("View All")
                    }
                }
            }
        }

        if (recentDocuments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_pdfs),
                            contentDescription = "No PDFs yet",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No PDFs Created Yet",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select photos from your gallery or scan a document to build your first PDF.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            items(recentDocuments, key = { it.id }) { doc ->
                PdfDocumentRowCard(
                    document = doc,
                    onOpen = { onOpenDocument(doc) },
                    onShare = { onShareDocument(doc) },
                    onMoreClick = { onMoreDocument(doc) },
                )
            }
        }
    }
}

private data class DashboardTileSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
private fun DashboardFeatureCard(
    spec: DashboardTileSpec,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .testTag(spec.testTag)
            .clickable(onClick = spec.onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = spec.accentColor.copy(alpha = 0.13f),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = spec.title,
                        tint = spec.accentColor,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = spec.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = spec.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
