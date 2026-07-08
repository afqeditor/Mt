package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SQLiteHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DbViewerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val file = viewModel.activeDbFile
    val fileName = file?.name ?: "Database Viewer"

    var showTableDropdown by remember { mutableStateOf(false) }
    var selectedCellRow by remember { mutableStateOf<SQLiteHelper.RowData?>(null) }
    var selectedCellColumn by remember { mutableStateOf<String?>(null) }
    var cellEditValue by remember { mutableStateOf("") }

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
                    IconButton(onClick = { viewModel.exportCurrentTable() }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export to CSV")
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
            // Table Selector Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .clickable { showTableDropdown = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Table Name",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            viewModel.selectedTable?.name ?: "No tables found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Select Table",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                DropdownMenu(
                    expanded = showTableDropdown,
                    onDismissRequest = { showTableDropdown = false }
                ) {
                    viewModel.dbTables.forEach { table ->
                        DropdownMenuItem(
                            text = { Text(table.name) },
                            onClick = {
                                viewModel.selectDbTable(table)
                                showTableDropdown = false
                            }
                        )
                    }
                }
            }

            if (viewModel.dbLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.tableColumns.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No columns or data found in this table.")
                }
            } else {
                val horizontalScrollState = rememberScrollState()

                // Scrollable Table Grid Sheet
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        viewModel.tableColumns.forEach { colName ->
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = colName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Content rows
                    LazyColumn(modifier = Modifier.fillMaxHeight()) {
                        items(viewModel.tableRows) { row ->
                            Row(
                                modifier = Modifier
                                    .border(0.25.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                viewModel.tableColumns.forEach { colName ->
                                    val cellValue = row.values[colName] ?: ""
                                    Box(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clickable {
                                                selectedCellRow = row
                                                selectedCellColumn = colName
                                                cellEditValue = cellValue
                                            }
                                            .padding(horizontal = 8.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = cellValue,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cell editing dialog
        if (selectedCellRow != null && selectedCellColumn != null) {
            val col = selectedCellColumn!!
            val row = selectedCellRow!!
            AlertDialog(
                onDismissRequest = {
                    selectedCellRow = null
                    selectedCellColumn = null
                },
                title = { Text("Edit Column: $col") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cellEditValue,
                            onValueChange = { cellEditValue = it },
                            label = { Text("Value") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (row.rowid != null) {
                            Text("RowID: ${row.rowid}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.editDatabaseCell(col, cellEditValue, row)
                        selectedCellRow = null
                        selectedCellColumn = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedCellRow = null
                        selectedCellColumn = null
                    }) { Text("Cancel") }
                }
            )
        }
    }
}
