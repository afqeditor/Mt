package com.example.data

import java.io.File

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val size: Long = if (file.isDirectory) 0 else file.length(),
    val isDirectory: Boolean = file.isDirectory,
    val lastModified: Long = file.lastModified(),
    val isSelected: Boolean = false,
    val extension: String = file.extension.lowercase()
) {
    val isZip: Boolean get() = extension in listOf("zip", "7z", "tar", "gz", "gzip")
    val isApk: Boolean get() = extension == "apk"
    val isDb: Boolean get() = extension in listOf("db", "sqlite", "sqlite3")
    val isImage: Boolean get() = extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    val isText: Boolean get() = extension in listOf("txt", "log", "xml", "json", "html", "css", "java", "kt", "sh", "properties", "md")
    val isCode: Boolean get() = extension in listOf("xml", "json", "html", "css", "java", "kt", "sh")
    
    val formattedSize: String get() {
        if (isDirectory) return ""
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
