package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Title
            Text(
                text = "Appearance & Interface",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )

            // Theme Setting Row
            SettingRow(
                icon = Icons.Default.Palette,
                title = "App Theme",
                subtitle = when (viewModel.appTheme) {
                    "dark" -> "Dark Theme"
                    "light" -> "Light Theme"
                    else -> "System Default"
                },
                onClick = { showThemeDialog = true }
            )

            // Font Setting Row
            SettingRow(
                icon = Icons.Default.TextFields,
                title = "Editor Font Size",
                subtitle = "${viewModel.editorFontSize} sp (Used for Text and Hex editor canvases)",
                onClick = { showFontDialog = true }
            )

            // Language Setting Row
            SettingRow(
                icon = Icons.Default.Language,
                title = "App Language",
                subtitle = when (viewModel.appLanguage) {
                    "es" -> "Español"
                    else -> "English"
                },
                onClick = { showLangDialog = true }
            )

            HorizontalDivider()

            // Backup & Restore Section
            Text(
                text = "System Maintenance",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )

            // Backup row
            SettingRow(
                icon = Icons.Default.Backup,
                title = "Backup Configurations",
                subtitle = "Saves your theme, size, and language settings to storage/nexus_backup.xml",
                onClick = { viewModel.backupConfigurations() }
            )

            // Restore row
            SettingRow(
                icon = Icons.Default.Restore,
                title = "Restore Configurations",
                subtitle = "Restores your preferences from storage/nexus_backup.xml",
                onClick = { viewModel.restoreConfigurations() }
            )
        }

        // Theme Dialog
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose Theme") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTheme("system")
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = viewModel.appTheme == "system", onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text("System Default")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTheme("dark")
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = viewModel.appTheme == "dark", onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text("Light-On-Dark (Dark Mode)")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTheme("light")
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = viewModel.appTheme == "light", onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text("Dark-On-Light (Light Mode)")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) { Text("Dismiss") }
                }
            )
        }

        // Font Size Dialog
        if (showFontDialog) {
            val sizes = listOf(12, 14, 16, 18, 20, 22)
            AlertDialog(
                onDismissRequest = { showFontDialog = false },
                title = { Text("Choose Editor Font Size") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        sizes.forEach { size ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateFontSize(size)
                                        showFontDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(selected = viewModel.editorFontSize == size, onClick = null)
                                Spacer(Modifier.width(16.dp))
                                Text("$size sp", fontSize = size.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFontDialog = false }) { Text("Dismiss") }
                }
            )
        }

        // Language Dialog
        if (showLangDialog) {
            AlertDialog(
                onDismissRequest = { showLangDialog = false },
                title = { Text("Choose Language") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLanguage("en")
                                    showLangDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = viewModel.appLanguage == "en", onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text("English")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLanguage("es")
                                    showLangDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = viewModel.appLanguage == "es", onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text("Español")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLangDialog = false }) { Text("Dismiss") }
                }
            )
        }
    }
}

@Composable
fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
