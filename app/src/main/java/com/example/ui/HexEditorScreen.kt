package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HexEditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val file = viewModel.activeHexFile
    val fileName = file?.name ?: "Hex Editor"
    
    // Create editable copy of the bytes
    var localBytes by remember { mutableStateOf(viewModel.hexBytes) }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var editingByteIndex by remember { mutableStateOf<Int?>(null) }
    var editHexInput by remember { mutableStateOf("") }

    var showGoToOffset by remember { mutableStateOf(false) }
    var offsetInput by remember { mutableStateOf("") }

    var showSearchHex by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var isSearchAsText by remember { mutableStateOf(true) }
    var highlightedIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.FILE_MANAGER) }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchHex = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Hex/Text")
                    }
                    IconButton(onClick = { showGoToOffset = true }) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Go To Offset")
                    }
                    IconButton(onClick = { viewModel.saveHexChanges(localBytes) }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                },
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
        ) {
            // Status bar displaying size
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total Bytes: ${localBytes.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (highlightedIndex != null) {
                        Text(
                            "Found at index: $highlightedIndex (offset: ${String.format("0x%08X", highlightedIndex)})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dual Grid lines (each row handles 16 bytes)
            val lineCount = (localBytes.size + 15) / 16

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(lineCount) { rowIndex ->
                    val rowStartIndex = rowIndex * 16
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Render Address/Offset Column (Hex format)
                        val address = String.format("%08X", rowStartIndex)
                        Text(
                            text = "$address: ",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.width(72.dp)
                        )

                        // 2. Render 16 Bytes (Hex values)
                        Row(
                            modifier = Modifier.weight(1.5f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (colIndex in 0 until 16) {
                                val byteIndex = rowStartIndex + colIndex
                                if (byteIndex < localBytes.size) {
                                    val byteVal = localBytes[byteIndex]
                                    val hexString = String.format("%02X", byteVal)
                                    val isHighlighted = byteIndex == highlightedIndex
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                else Color.Transparent,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .clickable {
                                                editingByteIndex = byteIndex
                                                editHexInput = hexString
                                            }
                                            .padding(2.dp)
                                    ) {
                                        Text(
                                            text = hexString,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(18.dp))
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // 3. Render 16 Characters (ASCII representation)
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (colIndex in 0 until 16) {
                                val byteIndex = rowStartIndex + colIndex
                                if (byteIndex < localBytes.size) {
                                    val byteVal = localBytes[byteIndex]
                                    val charVal = if (byteVal in 32..126) byteVal.toInt().toChar() else '.'
                                    
                                    Text(
                                        text = charVal.toString(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                }
            }
        }

        // Edit byte dialog
        if (editingByteIndex != null) {
            val index = editingByteIndex!!
            AlertDialog(
                onDismissRequest = { editingByteIndex = null },
                title = { Text("Edit Byte at Offset ${String.format("0x%08X", index)}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editHexInput,
                            onValueChange = { editHexInput = it.take(2).uppercase() },
                            label = { Text("Hex Value (2 chars)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val originalValue = localBytes[index]
                        Text("Original value: ${String.format("0x%02X", originalValue)} ('${if (originalValue in 32..126) originalValue.toInt().toChar() else "."}')", fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        try {
                            val parsedByte = editHexInput.toInt(16).toByte()
                            val updatedBytes = localBytes.clone()
                            updatedBytes[index] = parsedByte
                            localBytes = updatedBytes
                            editingByteIndex = null
                        } catch (e: Exception) {
                            // Invalid hex
                        }
                    }) { Text("Apply") }
                },
                dismissButton = {
                    TextButton(onClick = { editingByteIndex = null }) { Text("Cancel") }
                }
            )
        }

        // Go to offset dialog
        if (showGoToOffset) {
            AlertDialog(
                onDismissRequest = { showGoToOffset = false },
                title = { Text("Go To Offset") },
                text = {
                    OutlinedTextField(
                        value = offsetInput,
                        onValueChange = { offsetInput = it },
                        label = { Text("Offset (Hex e.g. 1A or Dec e.g. 26)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        try {
                            val index = if (offsetInput.startsWith("0x", ignoreCase = true)) {
                                offsetInput.substring(2).toInt(16)
                            } else {
                                offsetInput.toIntOrNull() ?: offsetInput.toInt(16)
                            }
                            if (index in 0 until localBytes.size) {
                                val rowIndex = index / 16
                                scope.launch {
                                    lazyListState.scrollToItem(rowIndex)
                                    highlightedIndex = index
                                }
                            }
                            showGoToOffset = false
                        } catch (e: Exception) {
                            // Handle parsing error
                        }
                    }) { Text("Jump") }
                },
                dismissButton = {
                    TextButton(onClick = { showGoToOffset = false }) { Text("Cancel") }
                }
            )
        }

        // Search Hex or Text dialog
        if (showSearchHex) {
            AlertDialog(
                onDismissRequest = { showSearchHex = false },
                title = { Text("Search Binary File") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            label = { Text("Search Pattern") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSearchAsText,
                                onClick = { isSearchAsText = true }
                            )
                            Text("Text string", fontSize = 14.sp)
                            Spacer(Modifier.width(16.dp))
                            RadioButton(
                                selected = !isSearchAsText,
                                onClick = { isSearchAsText = false }
                            )
                            Text("Hex bytes (e.g. 4F 4B)", fontSize = 14.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val index = if (isSearchAsText) {
                            searchText(localBytes, searchInput)
                        } else {
                            searchHex(localBytes, searchInput)
                        }
                        if (index != -1) {
                            highlightedIndex = index
                            val rowIndex = index / 16
                            scope.launch {
                                lazyListState.scrollToItem(rowIndex)
                            }
                        } else {
                            highlightedIndex = null
                        }
                        showSearchHex = false
                    }) { Text("Search") }
                },
                dismissButton = {
                    TextButton(onClick = { showSearchHex = false }) { Text("Cancel") }
                }
            )
        }
    }
}

// Byte searching helpers
private fun searchText(bytes: ByteArray, query: String): Int {
    if (query.isEmpty()) return -1
    val queryBytes = query.toByteArray()
    return findSubarray(bytes, queryBytes)
}

private fun searchHex(bytes: ByteArray, query: String): Int {
    try {
        val queryBytes = query.trim().split("\\s+".toRegex())
            .map { it.toInt(16).toByte() }
            .toByteArray()
        return findSubarray(bytes, queryBytes)
    } catch (e: Exception) {
        return -1
    }
}

private fun findSubarray(largeArray: ByteArray, subArray: ByteArray): Int {
    if (subArray.isEmpty()) return -1
    for (i in 0..largeArray.size - subArray.size) {
        var found = true
        for (j in subArray.indices) {
            if (largeArray[i + j] != subArray[j]) {
                found = false
                break
            }
        }
        if (found) return i
    }
    return -1
}
