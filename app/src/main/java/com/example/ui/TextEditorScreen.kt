package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val file = viewModel.activeEditorFile
    val fileName = file?.name ?: "Editor"
    
    var textState by remember { mutableStateOf(viewModel.editorContent) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showSearchReplace by remember { mutableStateOf(false) }

    // Sync state changes back to ViewModel
    LaunchedEffect(textState) {
        viewModel.editorContent = textState
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveTextEditorContent()
                        viewModel.navigateTo(Screen.FILE_MANAGER)
                    }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchReplace = !showSearchReplace }) {
                        Icon(Icons.Default.FindReplace, contentDescription = "Search & Replace")
                    }
                    IconButton(onClick = {
                        viewModel.editorContent = textState
                        viewModel.saveTextEditorContent()
                    }) {
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
            // Search and Replace Bar
            if (showSearchReplace) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Find") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = replaceQuery,
                                onValueChange = { replaceQuery = it },
                                label = { Text("Replace") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showSearchReplace = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    textState = textState.replace(searchQuery, replaceQuery)
                                }
                            }) {
                                Text("Replace All")
                            }
                        }
                    }
                }
            }

            // Editor Area with line numbers
            val lines = textState.split("\n")
            val lineCount = lines.size.coerceAtLeast(1)

            Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                // Line Numbers Sidebar
                LazyColumn(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    items(lineCount) { index ->
                        Text(
                            text = "${index + 1}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = viewModel.editorFontSize.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp, bottom = 2.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Code/Text Canvas
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp)
                ) {
                    BasicTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = viewModel.editorFontSize.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.fillMaxSize(),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = getSyntaxHighlightingTransformation(viewModel.editorLanguage),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }
            }
        }
    }
}

// Custom code highlighter for Kotlin, Java, XML, JSON, CSS, HTML
private fun getSyntaxHighlightingTransformation(lang: String): VisualTransformation {
    return VisualTransformation { text ->
        val annotatedString = AnnotatedString.Builder(text.text).apply {
            val code = text.text
            
            // Standard keywords mapping
            val keywords = when (lang) {
                "kt", "kotlin" -> listOf(
                    "package", "import", "class", "interface", "object", "fun", "val", "var",
                    "return", "if", "else", "for", "while", "when", "null", "true", "false",
                    "private", "public", "internal", "protected", "override", "suspend", "coroutine"
                )
                "java" -> listOf(
                    "package", "import", "class", "interface", "public", "private", "protected",
                    "static", "final", "void", "int", "float", "double", "boolean", "char",
                    "if", "else", "for", "while", "return", "new", "null", "true", "false"
                )
                "json" -> listOf("true", "false", "null")
                "html", "xml" -> listOf(
                    "html", "head", "body", "title", "div", "span", "p", "a", "h1", "h2", "h3",
                    "manifest", "application", "activity", "intent-filter", "action", "category",
                    "uses-permission", "resources", "string"
                )
                "css" -> listOf("body", "background", "color", "margin", "padding", "font-family")
                else -> emptyList()
            }

            // Highlighting colors (Material 3 aesthetic tokens)
            val keywordStyle = SpanStyle(color = Color(0xFFE91E63), fontWeight = FontWeight.Bold) // Pink accent
            val stringStyle = SpanStyle(color = Color(0xFF4CAF50)) // Green
            val numberStyle = SpanStyle(color = Color(0xFFFF9800)) // Orange
            val commentStyle = SpanStyle(color = Color(0xFF78909C)) // Blue Grey Slate
            val annotationStyle = SpanStyle(color = Color(0xFF9C27B0)) // Purple

            // 1. Highlight numbers
            val numberRegex = "\\b\\d+\\b".toRegex()
            numberRegex.findAll(code).forEach { match ->
                addStyle(numberStyle, match.range.first, match.range.last + 1)
            }

            // 2. Highlight words/keywords
            val wordRegex = "\\b\\w+\\b".toRegex()
            wordRegex.findAll(code).forEach { match ->
                if (keywords.contains(match.value)) {
                    addStyle(keywordStyle, match.range.first, match.range.last + 1)
                }
            }

            // 3. Highlight Annotations / Tags
            if (lang == "kt" || lang == "java") {
                val annotationRegex = "@\\w+".toRegex()
                annotationRegex.findAll(code).forEach { match ->
                    addStyle(annotationStyle, match.range.first, match.range.last + 1)
                }
            }

            // 4. Highlight string literals (quotes)
            val stringRegex = "\"[^\"]*\"".toRegex()
            stringRegex.findAll(code).forEach { match ->
                addStyle(stringStyle, match.range.first, match.range.last + 1)
            }

            // 5. Highlight single-line comments
            val commentRegex = "//.*".toRegex()
            commentRegex.findAll(code).forEach { match ->
                addStyle(commentStyle, match.range.first, match.range.last + 1)
            }

            // 6. XML/HTML Tag highlights (brackets)
            if (lang == "xml" || lang == "html") {
                val tagBracketRegex = "[<>]".toRegex()
                tagBracketRegex.findAll(code).forEach { match ->
                    addStyle(keywordStyle, match.range.first, match.range.last + 1)
                }
            }
        }.toAnnotatedString()
        TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
