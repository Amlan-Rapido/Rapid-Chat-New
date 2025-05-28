package com.rapido.voicemessagesdk.storage

import android.content.Context
import android.util.Log
import com.rapido.voicemessagesdk.PlatformContextProvider
import kotlinx.datetime.Clock
import java.io.File
import java.io.IOException

actual class PlatformAudioFileManager actual constructor() : AudioFileManager {
    
    companion object {
        private const val TAG = "PlatformAudioFileManager"
        private const val RECORDINGS_DIR_NAME = "voice_recordings"
    }

    private val context: Context by lazy {
        if (!PlatformContextProvider.isInitialized()) {
            throw IllegalStateException("PlatformContextProvider not initialized. Call initialize() first.")
        }
        PlatformContextProvider.appContext
    }

    private val recordingsCacheDir: File by lazy {
        try {
            // Use cache directory instead of external files directory
            val cacheDir = context.cacheDir
            Log.d(TAG, "Base cache directory: ${cacheDir.absolutePath}")

            File(cacheDir, RECORDINGS_DIR_NAME).apply {
                if (!exists()) {
                    Log.d(TAG, "Recordings cache directory doesn't exist, creating at: $absolutePath")
                    if (!mkdirs()) {
                        val error = "Failed to create recordings cache directory: $absolutePath"
                        Log.e(TAG, error)
                        throw IOException(error)
                    }
                    Log.d(TAG, "Successfully created recordings cache directory")
                } else if (!isDirectory) {
                    val error = "Path exists but is not a directory: $absolutePath"
                    Log.e(TAG, error)
                    throw IOException(error)
                } else {
                    Log.d(TAG, "Using existing recordings cache directory: $absolutePath")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing recordings cache directory", e)
            throw e
        }
    }

    actual override fun createRecordingFilePath(): String {
        try {
            val file = File(recordingsCacheDir, generateFileName())
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

    actual override fun deleteRecording(filePath: String): Boolean {
        try {
            val file = File(filePath)
            val baseCacheDir = recordingsCacheDir.absolutePath

            // Security check: ensure the file is within our recordings cache directory
            if (!file.absolutePath.startsWith(baseCacheDir)) {
                Log.e(
                    TAG,
                    "Security: Attempt to delete file outside recordings cache directory: $filePath"
                )
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

    actual override fun getRecordingsCacheDirectory(): String {
        return recordingsCacheDir.absolutePath
    }

    actual override fun clearRecordingsCache(): Int {
        return try {
            val files = recordingsCacheDir.listFiles() ?: return 0
            var deletedCount = 0
            
            files.forEach { file ->
                if (file.isFile && file.name.endsWith(AudioFileManager.RECORDING_FILE_EXTENSION)) {
                    if (file.delete()) {
                        deletedCount++
                        Log.d(TAG, "Deleted cached recording: ${file.name}")
                    } else {
                        Log.w(TAG, "Failed to delete cached recording: ${file.name}")
                    }
                }
            }
            
            Log.d(TAG, "Cleared $deletedCount recordings from cache")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing recordings cache", e)
            0
        }
    }

    actual override fun fileExists(filePath: String): Boolean {
        return try {
            File(filePath).exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if file exists: $filePath", e)
            false
        }
    }

    actual override fun getFileSize(filePath: String): Long {
        return try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                file.length()
            } else {
                -1L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size: $filePath", e)
            -1L
        }
    }

    private fun generateFileName(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val randomSuffix = kotlin.random.Random.nextInt(1000, 10000)
        return "recording_${timestamp}_$randomSuffix${AudioFileManager.RECORDING_FILE_EXTENSION}"
    }
} 