package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp)
                ) {
                    Column {
                        Icon(
                            Icons.Default.Android,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Nexus Manager",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "v1.0 Pro Developer Tool",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                
                // Drawer Items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("Dual-Pane Browser") },
                    selected = viewModel.currentScreen == Screen.FILE_MANAGER,
                    onClick = {
                        viewModel.navigateTo(Screen.FILE_MANAGER)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    label = { Text("Nexus Terminal") },
                    selected = viewModel.currentScreen == Screen.TERMINAL,
                    onClick = {
                        viewModel.navigateTo(Screen.TERMINAL)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = viewModel.currentScreen == Screen.SETTINGS,
                    onClick = {
                        viewModel.navigateTo(Screen.SETTINGS)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Main Router based on ViewModel state
            when (viewModel.currentScreen) {
                Screen.FILE_MANAGER -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        FileManagerScreen(viewModel = viewModel)
                        // Absolute positioned Menu FAB to open side drawer
                        FloatingActionButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(24.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Drawer")
                        }
                    }
                }
                Screen.TERMINAL -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TerminalScreen(viewModel = viewModel)
                        FloatingActionButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(24.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Drawer")
                        }
                    }
                }
                Screen.SETTINGS -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SettingsScreen(viewModel = viewModel)
                        FloatingActionButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(24.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Drawer")
                        }
                    }
                }
                // Contextual Editors / Viewers
                Screen.TEXT_EDITOR -> TextEditorScreen(viewModel = viewModel)
                Screen.HEX_EDITOR -> HexEditorScreen(viewModel = viewModel)
                Screen.SQLITE_VIEWER -> DbViewerScreen(viewModel = viewModel)
                Screen.IMAGE_VIEWER -> ImageViewerScreen(viewModel = viewModel)
                Screen.APK_MANAGER -> ApkManagerScreen(viewModel = viewModel)
            }

            // Global operation loading overlay card
            if (viewModel.isOperating) {
                AlertDialog(
                    onDismissRequest = {}, // Disallow manual dismiss
                    confirmButton = {},
                    title = { Text("Processing Task", fontWeight = FontWeight.Bold) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = viewModel.progressMessage,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }
}
