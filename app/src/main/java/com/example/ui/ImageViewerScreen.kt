package com.example.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val file = viewModel.activeImageFile
    val fileName = file?.name ?: "Image Viewer"

    var imageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showExifSheet by remember { mutableStateOf(false) }

    // Real local decode
    LaunchedEffect(file) {
        if (file != null && file.exists()) {
            withContext(Dispatchers.IO) {
                try {
                    imageBitmap = BitmapFactory.decodeFile(file.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

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
                    IconButton(onClick = { viewModel.zoomIn() }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                    }
                    IconButton(onClick = { viewModel.zoomOut() }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                    }
                    IconButton(onClick = { viewModel.rotateImage() }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate 90°")
                    }
                    IconButton(onClick = { showExifSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "EXIF Details")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = imageBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Image Preview",
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .graphicsLayer(
                            scaleX = viewModel.imageZoom,
                            scaleY = viewModel.imageZoom,
                            rotationZ = viewModel.imageRotation
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                // Optional gesture tracking if desired
                            }
                        }
                )
            } else {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // EXIF & Properties overlay
        if (showExifSheet && file != null) {
            AlertDialog(
                onDismissRequest = { showExifSheet = false },
                title = { Text("Image Properties", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        viewModel.imageExif.forEach { (key, value) ->
                            Column {
                                Text(key, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        
                        // Dimensions
                        imageBitmap?.let { bmp ->
                            Column {
                                Text("Resolution", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text("${bmp.width} x ${bmp.height} px", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExifSheet = false }) { Text("Close") }
                }
            )
        }
    }
}
