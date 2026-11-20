package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.local.PdfDocumentEntity
import com.example.engine.PdfEngine
import com.example.model.SortOption
import com.example.model.ViewMode
import com.example.ui.components.PdfOptionsBottomSheet
import com.example.ui.navigation.AppRoute
import com.example.ui.navigation.PdfToolType
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.CreatePdfModalSheet
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MyPdfsScreen
import com.example.ui.screens.PageSettingsScreen
import com.example.ui.screens.PdfToolDetailScreen
import com.example.ui.screens.PdfToolsScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.PhotoEditorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.state.MainViewModel
import com.example.ui.theme.PdfMakerTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PdfMakerRoot()
        }
    }
}

private data class TopNavTab(
    val route: AppRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

@Composable
fun PdfMakerRoot(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val totalPdfCount by viewModel.totalPdfCount.collectAsStateWithLifecycle()
    val allDocuments by viewModel.allDocuments.collectAsStateWithLifecycle()
    val filteredDocuments by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val recentDocuments by viewModel.recentDocuments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val draftImages by viewModel.draftImages.collectAsStateWithLifecycle()
    val isImportingImages by viewModel.isImportingImages.collectAsStateWithLifecycle()
    val pageSettings by viewModel.pageSettings.collectAsStateWithLifecycle()
    val photoEditorState by viewModel.photoEditorState.collectAsStateWithLifecycle()
    val creationState by viewModel.creationState.collectAsStateWithLifecycle()
    val toolState by viewModel.toolState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    var showCreatePdfModal by remember { mutableStateOf(false) }
    var actionSheetDocument by remember { mutableStateOf<PdfDocumentEntity?>(null) }
    var preselectedToolDocId by remember { mutableStateOf<Long?>(null) }

    // Android Photo Picker (Zero permissions required)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImagesFromUris(uris) {
                navController.navigate(AppRoute.Workspace) {
                    launchSingleTop = true
                }
            }
        }
    }

    // SAF PDF Document Picker (Zero permissions required)
    val pdfImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.importExternalPdf(uri)
        }
    }

    fun launchPhotoPicker() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun navigateTopTab(route: AppRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val topTabs = remember {
        listOf(
            TopNavTab(AppRoute.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            TopNavTab(AppRoute.Library, "My PDFs", Icons.Filled.Folder, Icons.Outlined.Folder, "nav_library"),
            TopNavTab(AppRoute.Tools, "Tools", Icons.Filled.Build, Icons.Outlined.Build, "nav_tools"),
            TopNavTab(AppRoute.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings"),
        )
    }

    val showBottomBar = currentDest?.let { dest ->
        dest.hasRoute<AppRoute.Home>() ||
            dest.hasRoute<AppRoute.Library>() ||
            dest.hasRoute<AppRoute.Tools>() ||
            dest.hasRoute<AppRoute.Settings>()
    } ?: true

    PdfMakerTheme(themeMode = prefs.themeMode) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                AppRoute.Home -> currentDest?.hasRoute<AppRoute.Home>() == true
                                AppRoute.Library -> currentDest?.hasRoute<AppRoute.Library>() == true
                                AppRoute.Tools -> currentDest?.hasRoute<AppRoute.Tools>() == true
                                AppRoute.Settings -> currentDest?.hasRoute<AppRoute.Settings>() == true
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                modifier = Modifier.testTag(tab.testTag),
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
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Home,
                ) {
                    composable<AppRoute.Home> {
                        HomeDashboardScreen(
                            totalPdfCount = totalPdfCount,
                            draftCount = draftImages.size,
                            recentDocuments = recentDocuments,
                            onImageToPdfClick = { launchPhotoPicker() },
                            onCameraToPdfClick = { navController.navigate(AppRoute.CameraScanner) },
                            onMyPdfsClick = { navigateTopTab(AppRoute.Library) },
                            onRecentFilesClick = {
                                viewModel.setSortOption(SortOption.NEWEST_FIRST)
                                if (recentDocuments.isNotEmpty()) {
                                    navController.navigate(AppRoute.PdfViewer(recentDocuments.first().id))
                                } else {
                                    navigateTopTab(AppRoute.Library)
                                }
                            },
                            onMergePdfClick = {
                                preselectedToolDocId = null
                                navController.navigate(AppRoute.PdfToolDetail(PdfToolType.MERGE))
                            },
                            onSettingsClick = { navigateTopTab(AppRoute.Settings) },
                            onResumeDraftClick = { navController.navigate(AppRoute.Workspace) },
                            onOpenDocument = { doc -> navController.navigate(AppRoute.PdfViewer(doc.id)) },
                            onShareDocument = { doc -> PdfEngine.sharePdfFile(context, File(doc.filePath)) },
                            onMoreDocument = { doc -> actionSheetDocument = doc },
                        )
                    }

                    composable<AppRoute.Library> {
                        MyPdfsScreen(
                            documents = filteredDocuments,
                            searchQuery = searchQuery,
                            sortOption = prefs.sortOption,
                            viewMode = prefs.viewMode,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onSortChange = viewModel::setSortOption,
                            onViewModeToggle = {
                                val next = if (prefs.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                                viewModel.setViewMode(next)
                            },
                            onImportPdfClick = { pdfImportLauncher.launch(arrayOf("application/pdf")) },
                            onCreateNewPdfClick = { launchPhotoPicker() },
                            onOpenDocument = { doc -> navController.navigate(AppRoute.PdfViewer(doc.id)) },
                            onShareDocument = { doc -> PdfEngine.sharePdfFile(context, File(doc.filePath)) },
                            onMoreDocument = { doc -> actionSheetDocument = doc },
                        )
                    }

                    composable<AppRoute.Tools> {
                        PdfToolsScreen(
                            onSelectTool = { toolType ->
                                preselectedToolDocId = null
                                viewModel.resetToolState()
                                navController.navigate(AppRoute.PdfToolDetail(toolType))
                            },
                            onImagesToPdf = { launchPhotoPicker() },
                        )
                    }

                    composable<AppRoute.Settings> {
                        SettingsScreen(
                            preferences = prefs,
                            documents = allDocuments,
                            onThemeModeChange = viewModel::setThemeMode,
                            onDefaultPageSizeChange = viewModel::setDefaultPageSize,
                            onDefaultQualityChange = viewModel::setDefaultQuality,
                            onViewModeChange = viewModel::setViewMode,
                            onClearDraftCache = {
                                viewModel.clearDrafts()
                                viewModel.showMessage("Draft image cache cleared.")
                            },
                        )
                    }

                    composable<AppRoute.Workspace> {
                        WorkspaceScreen(
                            draftImages = draftImages,
                            isImporting = isImportingImages,
                            pageSettings = pageSettings,
                            onBack = { navController.popBackStack() },
                            onAddPhotosClick = { launchPhotoPicker() },
                            onCameraScanClick = { navController.navigate(AppRoute.CameraScanner) },
                            onEditImageClick = { item ->
                                viewModel.openPhotoEditor(item.id)
                                navController.navigate(AppRoute.PhotoEditor(item.id))
                            },
                            onRemoveImageClick = viewModel::removeDraftImage,
                            onMoveImage = viewModel::moveDraftImage,
                            onClearAll = viewModel::clearDrafts,
                            onOpenPageSettings = { navController.navigate(AppRoute.PageSettings) },
                            onCreatePdfClick = {
                                viewModel.resetCreationState()
                                showCreatePdfModal = true
                            },
                        )
                    }

                    composable<AppRoute.CameraScanner> {
                        CameraScannerScreen(
                            onCancel = { navController.popBackStack() },
                            onFinishScanning = { files ->
                                viewModel.addScannedFilesToDrafts(files) {
                                    navController.navigate(AppRoute.Workspace) {
                                        popUpTo(AppRoute.CameraScanner) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onPickFromGalleryInstead = {
                                navController.popBackStack()
                                launchPhotoPicker()
                            },
                        )
                    }

                    composable<AppRoute.PhotoEditor> {
                        PhotoEditorScreen(
                            state = photoEditorState,
                            onBack = { navController.popBackStack() },
                            onUpdateConfig = viewModel::updatePhotoEditConfig,
                            onUndo = viewModel::undoPhotoEdit,
                            onRedo = viewModel::redoPhotoEdit,
                            onReset = viewModel::resetPhotoEdit,
                            onSave = {
                                viewModel.savePhotoEdit()
                                navController.popBackStack()
                            },
                        )
                    }

                    composable<AppRoute.PageSettings> {
                        PageSettingsScreen(
                            settings = pageSettings,
                            draftImages = draftImages,
                            onUpdateSettings = viewModel::updatePageSettings,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable<AppRoute.PdfViewer> { entry ->
                        val route = entry.toRoute<AppRoute.PdfViewer>()
                        val doc by produceState<PdfDocumentEntity?>(initialValue = null, key1 = route.documentId) {
                            value = viewModel.getDocumentById(route.documentId)
                        }
                        PdfViewerScreen(
                            document = doc,
                            onBack = { navController.popBackStack() },
                            onShare = { file -> PdfEngine.sharePdfFile(context, file) },
                            onPrint = { file -> PdfEngine.printPdfFile(context, file) },
                        )
                    }

                    composable<AppRoute.PdfToolDetail> { entry ->
                        val route = entry.toRoute<AppRoute.PdfToolDetail>()
                        PdfToolDetailScreen(
                            toolType = route.toolType,
                            allDocuments = allDocuments,
                            preselectedDocumentId = preselectedToolDocId,
                            toolState = toolState,
                            onBack = { navController.popBackStack() },
                            onImportExternalPdf = { pdfImportLauncher.launch(arrayOf("application/pdf")) },
                            onMergePdfs = viewModel::mergePdfs,
                            onSplitPdf = viewModel::splitPdf,
                            onReorderRotatePdf = viewModel::reorderAndRotatePdf,
                            onCompressPdf = viewModel::compressPdf,
                            onExportPdfToImages = { doc -> viewModel.exportPdfToImages(doc) },
                            onOpenDocument = { doc -> navController.navigate(AppRoute.PdfViewer(doc.id)) },
                            onResetToolState = viewModel::resetToolState,
                        )
                    }
                }
            }
        }

        if (showCreatePdfModal) {
            CreatePdfModalSheet(
                creationState = creationState,
                initialSettings = pageSettings,
                pageCount = draftImages.size,
                onDismiss = {
                    showCreatePdfModal = false
                    viewModel.resetCreationState()
                },
                onConfirmCreate = viewModel::createPdf,
                onOpenCreatedPdf = { doc ->
                    showCreatePdfModal = false
                    viewModel.resetCreationState()
                    viewModel.clearDrafts()
                    navController.navigate(AppRoute.PdfViewer(doc.id)) {
                        popUpTo(AppRoute.Home)
                    }
                },
                onShareCreatedPdf = { doc ->
                    PdfEngine.sharePdfFile(context, File(doc.filePath))
                },
                onSaveCreatedToDownloads = { doc ->
                    viewModel.saveToDownloads(doc)
                },
                onDoneAfterSuccess = {
                    showCreatePdfModal = false
                    viewModel.resetCreationState()
                    viewModel.clearDrafts()
                    navController.navigate(AppRoute.Library) {
                        popUpTo(AppRoute.Home)
                    }
                },
            )
        }

        actionSheetDocument?.let { doc ->
            PdfOptionsBottomSheet(
                document = doc,
                onDismiss = { actionSheetDocument = null },
                onOpen = { navController.navigate(AppRoute.PdfViewer(doc.id)) },
                onShare = { PdfEngine.sharePdfFile(context, File(doc.filePath)) },
                onPrint = { PdfEngine.printPdfFile(context, File(doc.filePath)) },
                onSaveDownloads = { viewModel.saveToDownloads(doc) },
                onRename = { newName -> viewModel.renamePdf(doc, newName) },
                onDelete = { viewModel.deletePdf(doc) },
                onSplit = {
                    preselectedToolDocId = doc.id
                    viewModel.resetToolState()
                    navController.navigate(AppRoute.PdfToolDetail(PdfToolType.SPLIT))
                },
                onReorderRotate = {
                    preselectedToolDocId = doc.id
                    viewModel.resetToolState()
                    navController.navigate(AppRoute.PdfToolDetail(PdfToolType.REORDER_ROTATE))
                },
                onCompress = {
                    preselectedToolDocId = doc.id
                    viewModel.resetToolState()
                    navController.navigate(AppRoute.PdfToolDetail(PdfToolType.COMPRESS))
                },
                onExportImages = {
                    viewModel.exportPdfToImages(doc)
                    viewModel.showMessage("Exporting pages to Pictures/PDFMaker...")
                },
            )
        }
    }
}
