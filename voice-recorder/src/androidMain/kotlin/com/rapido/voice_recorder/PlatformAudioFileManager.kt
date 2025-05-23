package com.rapido.voice_recorder

import android.content.Context
import android.os.Environment
import kotlinx.datetime.Clock
import java.io.File
import java.io.IOException

actual class PlatformAudioFileManager {
    private val context: Context by lazy { PlatformContextProvider.appContext }

    private val recordingsDir: File by lazy {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: context.filesDir

        File(baseDir, "recordings").apply {
            if (!exists() && !mkdirs()) {
                throw IOException("Failed to create recordings directory: $absolutePath")
            } else if (!isDirectory) {
                throw IOException("Path exists but is not a directory: $absolutePath")
            }
        }
    }

    actual fun createRecordingFilePath(): String {
        return File(recordingsDir, generateFileName()).absolutePath
    }

    actual fun deleteRecording(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.canonicalPath.startsWith(recordingsDir.canonicalPath)) {
                println("Security Warning: Deletion outside of recordings dir: $filePath")
                return false
            }
            file.exists() && file.isFile && file.delete()
        } catch (e: Exception) {
            println("Failed to delete recording: ${e.message}")
            false
        }
    }

    actual fun getRecordingsDirectory(): String {
        return recordingsDir.absolutePath
    }

    actual companion object {
        actual val RECORDING_FILE_EXTENSION: String = ".m4a"

        actual fun generateFileName(): String {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val randomSuffix = kotlin.random.Random.nextInt(1000, 10000)
            return "recording_${timestamp}_$randomSuffix$RECORDING_FILE_EXTENSION"
        }
    }
}
