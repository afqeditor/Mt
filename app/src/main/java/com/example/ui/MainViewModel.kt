package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class Screen {
    FILE_MANAGER,
    TEXT_EDITOR,
    HEX_EDITOR,
    SQLITE_VIEWER,
    TERMINAL,
    IMAGE_VIEWER,
    APK_MANAGER,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)

    // Current screen
    var currentScreen by mutableStateOf(Screen.FILE_MANAGER)
        private set

    // Settings States
    var appTheme by mutableStateOf(prefs.getString("theme", "system") ?: "system")
        private set
    var editorFontSize by mutableStateOf(prefs.getInt("font_size", 14))
        private set
    var appLanguage by mutableStateOf(prefs.getString("language", "en") ?: "en")
        private set

    // Panel states for Dual-Pane layout
    class PanelState(val id: String, initialPath: String) {
        var currentPath by mutableStateOf(initialPath)
        var files by mutableStateOf<List<FileItem>>(emptyList())
        var selectedPaths = mutableStateOf<Set<String>>(emptySet())
        var searchQuery by mutableStateOf("")
        var isLoading by mutableStateOf(false)
    }

    private val defaultPath = try {
        val ext = Environment.getExternalStorageDirectory()
        if (ext.exists() && ext.canRead()) ext.absolutePath else application.filesDir.absolutePath
    } catch (e: Exception) {
        application.filesDir.absolutePath
    }

    val leftPanel = PanelState("left", defaultPath)
    val rightPanel = PanelState("right", defaultPath)
    
    // Active panel: left or right
    var activePanelId by mutableStateOf("left")
        private set

    val activePanel: PanelState get() = if (activePanelId == "left") leftPanel else rightPanel
    val inactivePanel: PanelState get() = if (activePanelId == "left") rightPanel else leftPanel

    // Loading & Progress Overlays
    var progressMessage by mutableStateOf("")
        private set
    var isOperating by mutableStateOf(false)
        private set

    // Text/Code Editor State
    var activeEditorFile by mutableStateOf<File?>(null)
        private set
    var editorContent by mutableStateOf("")
    var editorLanguage by mutableStateOf("txt")
        private set

    // Hex Editor State
    var activeHexFile by mutableStateOf<File?>(null)
        private set
    var hexBytes by mutableStateOf<ByteArray>(ByteArray(0))
    var hexEditorOffset by mutableStateOf("0")

    // SQLite Viewer State
    var activeDbFile by mutableStateOf<File?>(null)
        private set
    var dbTables by mutableStateOf<List<SQLiteHelper.TableInfo>>(emptyList())
    var selectedTable by mutableStateOf<SQLiteHelper.TableInfo?>(null)
    var tableColumns by mutableStateOf<List<String>>(emptyList())
    var tableRows by mutableStateOf<List<SQLiteHelper.RowData>>(emptyList())
    var dbLoading by mutableStateOf(false)

    // Image Viewer State
    var activeImageFile by mutableStateOf<File?>(null)
        private set
    var imageRotation by mutableStateOf(0f)
    var imageZoom by mutableStateOf(1f)
    var imageExif by mutableStateOf<Map<String, String>>(emptyMap())

    // APK Manager State
    var activeApkFile by mutableStateOf<File?>(null)
        private set
    var parsedApkInfo by mutableStateOf<ApkHelper.ApkInfo?>(null)
    var apkResources by mutableStateOf<List<String>>(emptyList())
    var apkLoading by mutableStateOf(false)
    var apkActionLogs by mutableStateOf<List<String>>(emptyList())

    // Terminal State
    private val _terminalOutput = MutableStateFlow<List<String>>(listOf("Nexus Terminal v1.0", "Type safe commands. Type 'clear' to reset.", ""))
    val terminalOutput: StateFlow<List<String>> = _terminalOutput.asStateFlow()

    init {
        refreshPanel(leftPanel)
        refreshPanel(rightPanel)
    }

    // Navigation and screen routing
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun setActivePanel(id: String) {
        activePanelId = id
    }

    // File managers triggers
    fun refreshPanel(panel: PanelState) {
        viewModelScope.launch {
            panel.isLoading = true
            val items = FileOperationHelper.getFiles(panel.currentPath)
            panel.files = if (panel.searchQuery.isEmpty()) {
                items
            } else {
                items.filter { it.name.contains(panel.searchQuery, ignoreCase = true) }
            }
            // Retain selection if they still exist
            val existingPaths = panel.files.map { it.path }.toSet()
            panel.selectedPaths.value = panel.selectedPaths.value.intersect(existingPaths)
            panel.isLoading = false
        }
    }

    fun navigatePanelTo(panel: PanelState, path: String) {
        panel.currentPath = path
        panel.selectedPaths.value = emptySet()
        panel.searchQuery = ""
        refreshPanel(panel)
    }

    fun navigateActivePanelUp() {
        val currentFile = File(activePanel.currentPath)
        val parent = currentFile.parentFile
        if (parent != null && parent.exists() && parent.canRead()) {
            navigatePanelTo(activePanel, parent.absolutePath)
        }
    }

    fun toggleFileSelection(panel: PanelState, path: String) {
        val current = panel.selectedPaths.value
        if (current.contains(path)) {
            panel.selectedPaths.value = current - path
        } else {
            panel.selectedPaths.value = current + path
        }
    }

    fun clearSelection(panel: PanelState) {
        panel.selectedPaths.value = emptySet()
    }

    fun selectAll(panel: PanelState) {
        panel.selectedPaths.value = panel.files.map { it.path }.toSet()
    }

    // Core operations
    fun copySelected() {
        val sourcePaths = activePanel.selectedPaths.value.toList()
        if (sourcePaths.isEmpty()) return
        val destDir = File(inactivePanel.currentPath)
        
        viewModelScope.launch {
            isOperating = true
            val files = sourcePaths.map { File(it) }
            FileOperationHelper.copy(files, destDir) { progressMessage = it }
            activePanel.selectedPaths.value = emptySet()
            refreshPanel(leftPanel)
            refreshPanel(rightPanel)
            isOperating = false
        }
    }

    fun moveSelected() {
        val sourcePaths = activePanel.selectedPaths.value.toList()
        if (sourcePaths.isEmpty()) return
        val destDir = File(inactivePanel.currentPath)
        
        viewModelScope.launch {
            isOperating = true
            val files = sourcePaths.map { File(it) }
            FileOperationHelper.move(files, destDir) { progressMessage = it }
            activePanel.selectedPaths.value = emptySet()
            refreshPanel(leftPanel)
            refreshPanel(rightPanel)
            isOperating = false
        }
    }

    fun deleteSelected() {
        val sourcePaths = activePanel.selectedPaths.value.toList()
        if (sourcePaths.isEmpty()) return
        
        viewModelScope.launch {
            isOperating = true
            for (path in sourcePaths) {
                progressMessage = "Deleting: ${File(path).name}"
                FileOperationHelper.delete(File(path))
            }
            activePanel.selectedPaths.value = emptySet()
            refreshPanel(leftPanel)
            refreshPanel(rightPanel)
            isOperating = false
        }
    }

    fun renameFile(file: File, newName: String) {
        viewModelScope.launch {
            val success = FileOperationHelper.rename(file, newName)
            if (success) {
                refreshPanel(leftPanel)
                refreshPanel(rightPanel)
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val newFolder = File(activePanel.currentPath, name)
            newFolder.mkdirs()
            refreshPanel(activePanel)
        }
    }

    fun createTextFile(name: String) {
        viewModelScope.launch {
            val newFile = File(activePanel.currentPath, name)
            newFile.createNewFile()
            refreshPanel(activePanel)
        }
    }

    fun zipSelected(zipName: String) {
        val sourcePaths = activePanel.selectedPaths.value.toList()
        if (sourcePaths.isEmpty()) return
        val zipFile = File(activePanel.currentPath, zipName.let { if (it.endsWith(".zip")) it else "$it.zip" })
        
        viewModelScope.launch {
            isOperating = true
            val files = sourcePaths.map { File(it) }
            FileOperationHelper.createZip(files, zipFile) { progressMessage = it }
            activePanel.selectedPaths.value = emptySet()
            refreshPanel(activePanel)
            isOperating = false
        }
    }

    fun unzipFile(file: File) {
        val extractDir = File(file.parentFile, file.nameWithoutExtension)
        viewModelScope.launch {
            isOperating = true
            FileOperationHelper.extractZip(file, extractDir) { progressMessage = it }
            refreshPanel(activePanel)
            isOperating = false
        }
    }

    // Text Editor Actions
    fun openInTextEditor(file: File) {
        activeEditorFile = file
        editorLanguage = file.extension.lowercase()
        viewModelScope.launch {
            editorContent = try {
                file.readText()
            } catch (e: Exception) {
                "Error reading file: ${e.localizedMessage}"
            }
            navigateTo(Screen.TEXT_EDITOR)
        }
    }

    fun saveTextEditorContent() {
        val file = activeEditorFile ?: return
        viewModelScope.launch {
            try {
                file.writeText(editorContent)
                refreshPanel(leftPanel)
                refreshPanel(rightPanel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Hex Editor Actions
    fun openInHexEditor(file: File) {
        activeHexFile = file
        hexEditorOffset = "0"
        viewModelScope.launch {
            hexBytes = try {
                file.readBytes()
            } catch (e: Exception) {
                ByteArray(0)
            }
            navigateTo(Screen.HEX_EDITOR)
        }
    }

    fun saveHexChanges(newBytes: ByteArray) {
        val file = activeHexFile ?: return
        viewModelScope.launch {
            try {
                file.writeBytes(newBytes)
                hexBytes = newBytes
                refreshPanel(leftPanel)
                refreshPanel(rightPanel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Database Viewer Actions
    fun openInDatabaseViewer(file: File) {
        activeDbFile = file
        viewModelScope.launch {
            dbLoading = true
            dbTables = SQLiteHelper.getTables(file)
            selectedTable = dbTables.firstOrNull()
            loadSelectedTableData()
            navigateTo(Screen.SQLITE_VIEWER)
            dbLoading = false
        }
    }

    fun selectDbTable(table: SQLiteHelper.TableInfo) {
        selectedTable = table
        loadSelectedTableData()
    }

    private fun loadSelectedTableData() {
        val file = activeDbFile ?: return
        val table = selectedTable ?: return
        viewModelScope.launch {
            dbLoading = true
            val data = SQLiteHelper.getTableData(file, table.name)
            tableColumns = data.first
            tableRows = data.second
            dbLoading = false
        }
    }

    fun editDatabaseCell(columnName: String, newValue: String, rowData: SQLiteHelper.RowData) {
        val file = activeDbFile ?: return
        val table = selectedTable ?: return
        viewModelScope.launch {
            val success = SQLiteHelper.updateCell(
                dbFile = file,
                tableName = table.name,
                columnName = columnName,
                newValue = newValue,
                rowid = rowData.rowid,
                fallbackConditions = if (rowData.rowid == null) rowData.values else null
            )
            if (success) {
                loadSelectedTableData()
            }
        }
    }

    fun exportCurrentTable() {
        val file = activeDbFile ?: return
        val table = selectedTable ?: return
        viewModelScope.launch {
            isOperating = true
            progressMessage = "Exporting table ${table.name} to CSV..."
            val csvFile = SQLiteHelper.exportTableToCSV(file, table.name, file.parentFile ?: File(defaultPath))
            if (csvFile != null) {
                progressMessage = "Exported: ${csvFile.name}"
                refreshPanel(activePanel)
            } else {
                progressMessage = "Export failed."
            }
            isOperating = false
        }
    }

    // Image Viewer Actions
    fun openInImageViewer(file: File) {
        activeImageFile = file
        imageRotation = 0f
        imageZoom = 1f
        // Dynamic EXIF information load
        imageExif = mapOf(
            "Name" to file.name,
            "Path" to file.absolutePath,
            "Size" to "${file.length() / 1024} KB"
        )
        navigateTo(Screen.IMAGE_VIEWER)
    }

    fun rotateImage() {
        imageRotation = (imageRotation + 90f) % 360f
    }

    fun zoomIn() {
        imageZoom = (imageZoom + 0.25f).coerceAtMost(3f)
    }

    fun zoomOut() {
        imageZoom = (imageZoom - 0.25f).coerceAtLeast(0.5f)
    }

    // APK Manager Actions
    fun openInApkManager(file: File) {
        activeApkFile = file
        apkActionLogs = emptyList()
        viewModelScope.launch {
            apkLoading = true
            parsedApkInfo = ApkHelper.parseApk(getApplication(), file)
            apkResources = ApkHelper.listResources(file).take(100) // limit for UI performance
            navigateTo(Screen.APK_MANAGER)
            apkLoading = false
        }
    }

    fun runSignApk() {
        val file = activeApkFile ?: return
        val destFile = File(file.parentFile, "${file.nameWithoutExtension}_signed.apk")
        viewModelScope.launch {
            apkLoading = true
            val logs = mutableListOf<String>()
            ApkHelper.signApk(file, destFile) { log ->
                logs.add(log)
                apkActionLogs = logs.toList()
            }
            refreshPanel(activePanel)
            apkLoading = false
        }
    }

    fun runZipalignApk() {
        val file = activeApkFile ?: return
        val destFile = File(file.parentFile, "${file.nameWithoutExtension}_aligned.apk")
        viewModelScope.launch {
            apkLoading = true
            val logs = mutableListOf<String>()
            ApkHelper.zipalignApk(file, destFile) { log ->
                logs.add(log)
                apkActionLogs = logs.toList()
            }
            refreshPanel(activePanel)
            apkLoading = false
        }
    }

    // Terminal Commands
    fun sendTerminalCommand(command: String) {
        if (command.trim().isEmpty()) return
        viewModelScope.launch {
            val currentList = _terminalOutput.value.toMutableList()
            currentList.add("> $command")
            
            val response = ShellTerminal.execute(command, File(activePanel.currentPath))
            if (response == "__CLEAR_TERMINAL__") {
                _terminalOutput.value = listOf("Nexus Terminal v1.0", "Type safe commands. Type 'clear' to reset.", "")
            } else {
                currentList.add(response)
                currentList.add("")
                _terminalOutput.value = currentList
            }
        }
    }

    // Settings Updates
    fun updateTheme(theme: String) {
        appTheme = theme
        prefs.edit().putString("theme", theme).apply()
    }

    fun updateFontSize(size: Int) {
        editorFontSize = size
        prefs.edit().putInt("font_size", size).apply()
    }

    fun updateLanguage(lang: String) {
        appLanguage = lang
        prefs.edit().putString("language", lang).apply()
    }

    fun backupConfigurations() {
        viewModelScope.launch {
            isOperating = true
            progressMessage = "Backing up preferences..."
            val backupFile = File(defaultPath, "nexus_backup.xml")
            val sourceFile = File(getApplication<Application>().filesDir.parentFile, "shared_prefs/nexus_prefs.xml")
            if (sourceFile.exists()) {
                sourceFile.copyTo(backupFile, overwrite = true)
                progressMessage = "Saved to: ${backupFile.name}"
                refreshPanel(activePanel)
            } else {
                progressMessage = "No preferences found to backup."
            }
            isOperating = false
        }
    }

    fun restoreConfigurations() {
        viewModelScope.launch {
            isOperating = true
            progressMessage = "Restoring preferences..."
            val backupFile = File(defaultPath, "nexus_backup.xml")
            val targetFile = File(getApplication<Application>().filesDir.parentFile, "shared_prefs/nexus_prefs.xml")
            if (backupFile.exists()) {
                targetFile.parentFile?.mkdirs()
                backupFile.copyTo(targetFile, overwrite = true)
                progressMessage = "Restored. Restart required."
                // Reload live values
                appTheme = prefs.getString("theme", "system") ?: "system"
                editorFontSize = prefs.getInt("font_size", 14)
                appLanguage = prefs.getString("language", "en") ?: "en"
            } else {
                progressMessage = "nexus_backup.xml not found."
            }
            isOperating = false
        }
    }
}
