package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object ShellTerminal {

    suspend fun execute(command: String, currentDir: File? = null): String = withContext(Dispatchers.IO) {
        val output = java.lang.StringBuilder()
        try {
            val parts = command.trim().split("\\s+".toRegex())
            if (parts.isEmpty() || parts[0].isEmpty()) {
                return@withContext ""
            }

            // Custom built-ins for better terminal simulation
            when (parts[0]) {
                "cd" -> {
                    return@withContext "Custom cd command: Please navigate directories using the file panel."
                }
                "clear" -> {
                    return@withContext "__CLEAR_TERMINAL__"
                }
            }

            val processBuilder = ProcessBuilder(parts)
            if (currentDir != null && currentDir.exists() && currentDir.isDirectory) {
                processBuilder.directory(currentDir)
            }
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            
            if (output.isEmpty()) {
                output.append("Command executed with exit code ${process.exitValue()}")
            }
            output.toString()
        } catch (e: Exception) {
            "Error executing command '$command': ${e.localizedMessage ?: e.message}"
        }
    }
}
