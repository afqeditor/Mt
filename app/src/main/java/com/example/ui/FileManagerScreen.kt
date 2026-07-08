package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FileItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var fileNameInput by remember { mutableStateOf("") }

    var showZipDialog by remember { mutableStateOf(false) }
    var zipNameInput by remember { mutableStateOf("") }

    var showRenameDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var selectedContextFile by remember { mutableStateOf<FileItem?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nexus Manager", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Folder")
                    }
                    IconButton(onClick = { showCreateFileDialog = true }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "Create File")
                    }
                    IconButton(onClick = { viewModel.refreshPanel(viewModel.activePanel) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            if (viewModel.activePanel.selectedPaths.value.isNotEmpty()) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    val count = viewModel.activePanel.selectedPaths.value.size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$count selected",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        IconButton(
                            onClick = { viewModel.copySelected() },
                            modifier = Modifier.testTag("copy_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                Text("Copy", fontSize = 10.sp)
                            }
                        }
                        IconButton(
                            onClick = { viewModel.moveSelected() },
                            modifier = Modifier.testTag("move_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MoveToInbox, contentDescription = "Move")
                                Text("Move", fontSize = 10.sp)
                            }
                        }
                        IconButton(
                            onClick = { viewModel.deleteSelected() },
                            modifier = Modifier.testTag("delete_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                Text("Delete", fontSize = 10.sp, color = Color.Red)
                            }
                        }
                        IconButton(onClick = { showZipDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Archive, contentDescription = "ZIP")
                                Text("ZIP", fontSize = 10.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.clearSelection(viewModel.activePanel) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                Text("Clear", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isWideScreen) {
                // Side-by-side Dual Pane for tablets / landscape
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        PaneHeader(
                            title = "Left Panel",
                            panelState = viewModel.leftPanel,
                            isActive = viewModel.activePanelId == "left",
                            onActivate = { viewModel.setActivePanel("left") },
                            onUp = { viewModel.navigatePanelTo(viewModel.leftPanel, File(viewModel.leftPanel.currentPath).parent ?: viewModel.leftPanel.currentPath) }
                        )
                        FileListView(
                            viewModel = viewModel,
                            panelState = viewModel.leftPanel,
                            onFileClick = { fileItem -> handleFileClick(fileItem, viewModel) },
                            onFileLongClick = { fileItem ->
                                selectedContextFile = fileItem
                                showContextMenu = true
                            }
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        PaneHeader(
                            title = "Right Panel",
                            panelState = viewModel.rightPanel,
                            isActive = viewModel.activePanelId == "right",
                            onActivate = { viewModel.setActivePanel("right") },
                            onUp = { viewModel.navigatePanelTo(viewModel.rightPanel, File(viewModel.rightPanel.currentPath).parent ?: viewModel.rightPanel.currentPath) }
                        )
                        FileListView(
                            viewModel = viewModel,
                            panelState = viewModel.rightPanel,
                            onFileClick = { fileItem -> handleFileClick(fileItem, viewModel) },
                            onFileLongClick = { fileItem ->
                                selectedContextFile = fileItem
                                showContextMenu = true
                            }
                        )
                    }
                }
            } else {
                // Tabbed layout for standard mobile views
                TabRow(
                    selectedTabIndex = if (viewModel.activePanelId == "left") 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ) {
                    Tab(
                        selected = viewModel.activePanelId == "left",
                        onClick = { viewModel.setActivePanel("left") },
                        text = { Text("Left Pane", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = viewModel.activePanelId == "right",
                        onClick = { viewModel.setActivePanel("right") },
                        text = { Text("Right Pane", fontWeight = FontWeight.SemiBold) }
                    )
                }

                PaneHeader(
                    title = if (viewModel.activePanelId == "left") "Left Directory" else "Right Directory",
                    panelState = viewModel.activePanel,
                    isActive = true,
                    onActivate = {},
                    onUp = { viewModel.navigateActivePanelUp() }
                )

                FileListView(
                    viewModel = viewModel,
                    panelState = viewModel.activePanel,
                    onFileClick = { fileItem -> handleFileClick(fileItem, viewModel) },
                    onFileLongClick = { fileItem ->
                        selectedContextFile = fileItem
                        showContextMenu = true
                    }
                )
            }
        }

        // Dialogs & Sheets

        if (showContextMenu && selectedContextFile != null) {
            val item = selectedContextFile!!
            AlertDialog(
                onDismissRequest = { showContextMenu = false },
                title = { Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Path: ${item.path}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!item.isDirectory) {
                            Text("Size: ${item.formattedSize}", fontSize = 12.sp)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        // Action choices
                        TextButton(onClick = {
                            showContextMenu = false
                            viewModel.openInTextEditor(item.file)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open in Text Editor")
                        }

                        TextButton(onClick = {
                            showContextMenu = false
                            viewModel.openInHexEditor(item.file)
                        }) {
                            Icon(Icons.Default.DeveloperMode, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open in Hex Editor")
                        }

                        if (item.isDb) {
                            TextButton(onClick = {
                                showContextMenu = false
                                viewModel.openInDatabaseViewer(item.file)
                            }) {
                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Browse SQLite Database")
                            }
                        }

                        if (item.isApk) {
                            TextButton(onClick = {
                                showContextMenu = false
                                viewModel.openInApkManager(item.file)
                            }) {
                                Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Open in APK Manager")
                            }
                        }

                        if (item.isZip) {
                            TextButton(onClick = {
                                showContextMenu = false
                                viewModel.unzipFile(item.file)
                            }) {
                                Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Extract ZIP Here")
                            }
                        }

                        TextButton(onClick = {
                            showContextMenu = false
                            fileToRename = item.file
                            renameInput = item.name
                            showRenameDialog = true
                        }) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Rename File")
                        }

                        TextButton(onClick = {
                            showContextMenu = false
                            viewModel.toggleFileSelection(viewModel.activePanel, item.path)
                        }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (viewModel.activePanel.selectedPaths.value.contains(item.path)) "Deselect" else "Select")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showContextMenu = false }) { Text("Close") }
                }
            )
        }

        if (showCreateFolderDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                title = { Text("New Directory") },
                text = {
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (folderNameInput.isNotBlank()) {
                            viewModel.createFolder(folderNameInput)
                            folderNameInput = ""
                            showCreateFolderDialog = false
                        }
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolderDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showCreateFileDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFileDialog = false },
                title = { Text("New File") },
                text = {
                    OutlinedTextField(
                        value = fileNameInput,
                        onValueChange = { fileNameInput = it },
                        label = { Text("File Name (e.g. notes.txt)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (fileNameInput.isNotBlank()) {
                            viewModel.createTextFile(fileNameInput)
                            fileNameInput = ""
                            showCreateFileDialog = false
                        }
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFileDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showZipDialog) {
            AlertDialog(
                onDismissRequest = { showZipDialog = false },
                title = { Text("Create ZIP Archive") },
                text = {
                    OutlinedTextField(
                        value = zipNameInput,
                        onValueChange = { zipNameInput = it },
                        label = { Text("Archive Name") },
                        placeholder = { Text("archive.zip") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (zipNameInput.isNotBlank()) {
                            viewModel.zipSelected(zipNameInput)
                            zipNameInput = ""
                            showZipDialog = false
                        }
                    }) { Text("Compress") }
                },
                dismissButton = {
                    TextButton(onClick = { showZipDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showRenameDialog && fileToRename != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename File") },
                text = {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("New Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameFile(fileToRename!!, renameInput)
                            fileToRename = null
                            renameInput = ""
                            showRenameDialog = false
                        }
                    }) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun PaneHeader(
    title: String,
    panelState: MainViewModel.PanelState,
    isActive: Boolean,
    onActivate: () -> Unit,
    onUp: () -> Unit
) {
    Card(
        onClick = onActivate,
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onUp,
                enabled = panelState.currentPath != "/"
            ) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Up Directory")
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = panelState.currentPath,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(4.dp))
            
            // Fast selection tools
            IconButton(onClick = { panelState.searchQuery = if (panelState.searchQuery.isEmpty()) " " else "" }) {
                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
            }
        }
    }
    
    // Dynamic search bar injection
    if (panelState.searchQuery.isNotEmpty()) {
        OutlinedTextField(
            value = panelState.searchQuery.trim(),
            onValueChange = {
                panelState.searchQuery = it
                // Refresh immediately
            },
            placeholder = { Text("Filter files...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListView(
    viewModel: MainViewModel,
    panelState: MainViewModel.PanelState,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit
) {
    // Dynamic refresher on changes
    LaunchedEffect(panelState.currentPath, panelState.searchQuery) {
        viewModel.refreshPanel(panelState)
    }

    if (panelState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (panelState.files.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "This directory is empty or inaccessible",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(panelState.files) { item ->
                val isSelected = panelState.selectedPaths.value.contains(item.path)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(item.lastModified))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onFileClick(item) },
                            onLongClick = { onFileLongClick(item) }
                        )
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getFileIcon(item),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = getFileIconColor(item)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formattedDate,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!item.isDirectory) {
                                Text(
                                    text = item.formattedSize,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { viewModel.toggleFileSelection(panelState, item.path) },
                        modifier = Modifier.testTag("file_checkbox_${item.name}")
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
            }
        }
    }
}

private fun handleFileClick(item: FileItem, viewModel: MainViewModel) {
    if (item.isDirectory) {
        viewModel.navigatePanelTo(viewModel.activePanel, item.path)
    } else {
        // Smart routing based on extension
        when {
            item.isImage -> viewModel.openInImageViewer(item.file)
            item.isDb -> viewModel.openInDatabaseViewer(item.file)
            item.isApk -> viewModel.openInApkManager(item.file)
            item.isText -> viewModel.openInTextEditor(item.file)
            else -> viewModel.openInHexEditor(item.file) // fallback opening in Hex Editor!
        }
    }
}

private fun getFileIcon(item: FileItem): ImageVector {
    return when {
        item.isDirectory -> Icons.Default.Folder
        item.isZip -> Icons.Default.FolderZip
        item.isApk -> Icons.Default.Android
        item.isDb -> Icons.Default.Storage
        item.isImage -> Icons.Default.Image
        item.isText -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }
}

@Composable
private fun getFileIconColor(item: FileItem): Color {
    return when {
        item.isDirectory -> MaterialTheme.colorScheme.primary
        item.isZip -> Color(0xFFFFA000) // Amber
        item.isApk -> Color(0xFF4CAF50) // Green
        item.isDb -> Color(0xFF9C27B0) // Purple
        item.isImage -> Color(0xFF00BCD4) // Cyan
        item.isText -> Color(0xFF607D8B) // Blue Grey
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
