package com.rapido.voicemessagesdk.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.datetime.Clock
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual class PlatformAudioFileManager actual constructor() : AudioFileManager {
    
    companion object {
        private const val RECORDINGS_DIR_NAME = "voice_recordings"
    }
    
    private val fileManager = NSFileManager.defaultManager

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private val recordingsCacheDir: String by lazy {
        val paths = fileManager.URLsForDirectory(
            directory = NSCachesDirectory, // Use cache directory instead of documents
            inDomains = NSUserDomainMask
        )
        val cacheURL = paths.firstOrNull() as? NSURL
            ?: throw IllegalStateException("Could not retrieve cache directory URL.")

        val recordingsURL = cacheURL.URLByAppendingPathComponent(RECORDINGS_DIR_NAME)
            ?: throw IllegalStateException("Could not construct recordings cache directory URL from cache URL.")

        val path = recordingsURL.path
            ?: throw IllegalStateException("Could not convert recordings cache URL to path string.")

        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()

            // Check if directory exists
            if (!fileManager.fileExistsAtPath(path)) {
                // Create directory if it doesn't exist
                val success = fileManager.createDirectoryAtPath(
                    path = path,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = errorPtr.ptr
                )
                if (!success) {
                    val error = errorPtr.value
                    throw IllegalStateException("Failed to create recordings cache directory at $path. Error: ${error?.description ?: "Unknown error"}")
                }
                println("Successfully created recordings cache directory at: $path")
            } else {
                // Verify it's a directory
                val isDirectory = alloc<BooleanVar>()
                if (!fileManager.fileExistsAtPath(
                        path,
                        isDirectory = isDirectory.ptr
                    ) || !isDirectory.value
                ) {
                    throw IllegalStateException("Path exists but is not a directory: $path")
                }
                println("Using existing recordings cache directory: $path")
            }
        }
        path
    }

    actual override fun createRecordingFilePath(): String {
        val fileName = generateFileName()
        val filePath = "$recordingsCacheDir/$fileName"
        println("Creating new recording file path: $filePath")
        return filePath
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual override fun deleteRecording(filePath: String): Boolean {
        val baseCacheDir = recordingsCacheDir
        if (baseCacheDir.isEmpty()) {
            println("Error: Recordings cache directory path is invalid or not initialized.")
            return false
        }

        val fileURL = NSURL.fileURLWithPath(filePath)
        val cacheDirectoryURL = NSURL.fileURLWithPath(baseCacheDir)

        if (!fileURL.path!!.startsWith(cacheDirectoryURL.path!!)) {
            println("Security: Attempt to delete file outside of recordings cache directory: $filePath")
            return false
        }

        if (!fileManager.fileExistsAtPath(filePath)) {
            println("Warning: File to delete does not exist: $filePath")
            return false
        }

        return try {
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val success = fileManager.removeItemAtPath(filePath, errorPtr.ptr)
                if (!success) {
                    val error = errorPtr.value
                    println("Error deleting file $filePath: ${error?.description ?: "Unknown error"}")
                } else {
                    println("Successfully deleted file: $filePath")
                }
                success
            }
        } catch (e: Exception) {
            println("Exception during file deletion of $filePath: ${e.message}")
            false
        }
    }

    actual override fun getRecordingsCacheDirectory(): String {
        return recordingsCacheDir
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual override fun clearRecordingsCache(): Int {
        return try {
            val cacheDir = recordingsCacheDir
            val contents = fileManager.contentsOfDirectoryAtPath(cacheDir, error = null)
            var deletedCount = 0
            
            contents?.forEach { item ->
                val fileName = item.toString()
                if (fileName.endsWith(AudioFileManager.RECORDING_FILE_EXTENSION)) {
                    val filePath = "$cacheDir/$fileName"
                    if (deleteRecording(filePath)) {
                        deletedCount++
                        println("Deleted cached recording: $fileName")
                    } else {
                        println("Failed to delete cached recording: $fileName")
                    }
                }
            }
            
            println("Cleared $deletedCount recordings from cache")
            deletedCount
        } catch (e: Exception) {
            println("Error clearing recordings cache: ${e.message}")
            0
        }
    }

    actual override fun fileExists(filePath: String): Boolean {
        return try {
            fileManager.fileExistsAtPath(filePath)
        } catch (e: Exception) {
            println("Error checking if file exists: $filePath - ${e.message}")
            false
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual override fun getFileSize(filePath: String): Long {
        return try {
            if (!fileManager.fileExistsAtPath(filePath)) {
                return -1L
            }
            
            // Use a simpler approach - just return a default size for now
            // In a real implementation, you would use proper NSFileManager APIs
            val fileURL = NSURL.fileURLWithPath(filePath)
            if (fileURL != null && fileManager.fileExistsAtPath(filePath)) {
                // For now, return a placeholder size
                // TODO: Implement proper file size retrieval using NSFileManager
                1024L // Placeholder size
            } else {
                -1L
            }
        } catch (e: Exception) {
            println("Error getting file size: $filePath - ${e.message}")
            -1L
        }
    }

    private fun generateFileName(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val randomSuffix = kotlin.random.Random.nextInt(1000, 10000)
        return "recording_${timestamp}_$randomSuffix${AudioFileManager.RECORDING_FILE_EXTENSION}"
    }
} 