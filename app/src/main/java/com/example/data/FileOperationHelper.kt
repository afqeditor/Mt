package com.example.data

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileOperationHelper {

    suspend fun getFiles(directoryPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val dir = File(directoryPath)
            if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()
            
            val filesList = dir.listFiles() ?: return@withContext emptyList()
            filesList.map { FileItem(it) }
                .sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun copy(sources: List<File>, destinationDir: File, onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        for (source in sources) {
            onProgress("Copying: ${source.name}")
            val target = File(destinationDir, source.name)
            if (source.isDirectory) {
                copyDirectory(source, target)
            } else {
                copyFile(source, target)
            }
        }
    }

    private fun copyDirectory(source: File, destination: File) {
        if (!destination.exists()) {
            destination.mkdirs()
        }
        source.listFiles()?.forEach { file ->
            val target = File(destination, file.name)
            if (file.isDirectory) {
                copyDirectory(file, target)
            } else {
                copyFile(file, target)
            }
        }
    }

    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { inStream ->
            FileOutputStream(destination).use { outStream ->
                val buffer = ByteArray(1024 * 64)
                var length: Int
                while (inStream.read(buffer).also { length = it } > 0) {
                    outStream.write(buffer, 0, length)
                }
            }
        }
    }

    suspend fun move(sources: List<File>, destinationDir: File, onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        for (source in sources) {
            onProgress("Moving: ${source.name}")
            val target = File(destinationDir, source.name)
            if (source.renameTo(target)) {
                // Succeeded directly (same storage volume)
            } else {
                // Fallback copy + delete (cross volume)
                if (source.isDirectory) {
                    copyDirectory(source, target)
                    delete(source)
                } else {
                    copyFile(source, target)
                    source.delete()
                }
            }
        }
    }

    suspend fun delete(file: File): Unit = withContext(Dispatchers.IO) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { delete(it) }
        }
        file.delete()
    }

    suspend fun rename(file: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        val destination = File(file.parentFile, newName)
        file.renameTo(destination)
    }

    suspend fun createZip(filesToZip: List<File>, zipFile: File, onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        onProgress("Creating ZIP...")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { out ->
            for (file in filesToZip) {
                addFileToZip(out, file, file.name)
            }
        }
    }

    private fun addFileToZip(out: ZipOutputStream, file: File, path: String) {
        if (file.isDirectory) {
            val files = file.listFiles() ?: return
            for (f in files) {
                addFileToZip(out, f, "$path/${f.name}")
            }
        } else {
            val buffer = ByteArray(4096)
            BufferedInputStream(FileInputStream(file)).use { stream ->
                out.putNextEntry(ZipEntry(path))
                var count: Int
                while (stream.read(buffer, 0, 4096).also { count = it } != -1) {
                    out.write(buffer, 0, count)
                }
                out.closeEntry()
            }
        }
    }

    suspend fun extractZip(zipFile: File, extractDir: File, onProgress: (String) -> Unit = {}) = withContext(Dispatchers.IO) {
        onProgress("Extracting ZIP...")
        if (!extractDir.exists()) {
            extractDir.mkdirs()
        }
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
            var entry = zipIn.nextEntry
            val buffer = ByteArray(4096)
            while (entry != null) {
                val filePath = File(extractDir, entry.name)
                if (!entry.isDirectory) {
                    filePath.parentFile?.mkdirs()
                    BufferedOutputStream(FileOutputStream(filePath)).use { bos ->
                        var read: Int
                        while (zipIn.read(buffer).also { read = it } != -1) {
                            bos.write(buffer, 0, read)
                        }
                    }
                } else {
                    filePath.mkdirs()
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    fun isRootAvailable(): Boolean {
        val paths = listOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su")
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    fun getDefaultStorageDirectory(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }
}
