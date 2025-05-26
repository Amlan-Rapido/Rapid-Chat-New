package com.rapido.voice_recorder

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.datetime.Clock
import java.io.File
import java.io.IOException

actual class PlatformAudioFileManager {
    actual companion object {
        private const val TAG = "PlatformAudioFileManager"
        actual val RECORDING_FILE_EXTENSION: String = ".m4a"
        
        actual fun generateFileName(): String {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val randomSuffix = kotlin.random.Random.nextInt(1000, 10000)
            return "recording_${timestamp}_$randomSuffix$RECORDING_FILE_EXTENSION"
        }
    }

    private val context: Context by lazy { 
        if (!PlatformContextProvider.isInitialized()) {
            throw IllegalStateException("PlatformContextProvider not initialized. Call initialize() first.")
        }
        PlatformContextProvider.appContext 
    }

    private val recordingsDir: File by lazy {
        try {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir
            
            Log.d(TAG, "Base directory for recordings: ${baseDir.absolutePath}")
            
            File(baseDir, "recordings").apply {
                if (!exists()) {
                    Log.d(TAG, "Recordings directory doesn't exist, creating at: $absolutePath")
                    if (!mkdirs()) {
                        val error = "Failed to create recordings directory: $absolutePath"
                        Log.e(TAG, error)
                        throw IOException(error)
                    }
                    Log.d(TAG, "Successfully created recordings directory")
                } else if (!isDirectory) {
                    val error = "Path exists but is not a directory: $absolutePath"
                    Log.e(TAG, error)
                    throw IOException(error)
                } else {
                    Log.d(TAG, "Using existing recordings directory: $absolutePath")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing recordings directory", e)
            throw e
        }
    }

    actual fun createRecordingFilePath(): String {
        try {
            val file = File(recordingsDir, generateFileName())
            Log.d(TAG, "Creating new recording file path: ${file.absolutePath}")
            
            // Ensure parent directory exists
            file.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    throw IOException("Failed to create parent directory: ${parent.absolutePath}")
                }
            }
            
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating recording file path", e)
            throw e
        }
    }

    actual fun deleteRecording(filePath: String): Boolean {
        try {
            val file = File(filePath)
            val baseDir = recordingsDir.absolutePath
            
            // Security check: ensure the file is within our recordings directory
            if (!file.absolutePath.startsWith(baseDir)) {
                Log.e(TAG, "Security: Attempt to delete file outside recordings directory: $filePath")
                return false
            }
            
            if (!file.exists()) {
                Log.w(TAG, "File to delete does not exist: $filePath")
                return false
            }
            
            val result = file.delete()
            if (result) {
                Log.d(TAG, "Successfully deleted file: $filePath")
            } else {
                Log.e(TAG, "Failed to delete file: $filePath")
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting recording: $filePath", e)
            return false
        }
    }

    actual fun getRecordingsDirectory(): String {
        return recordingsDir.absolutePath
    }
}
