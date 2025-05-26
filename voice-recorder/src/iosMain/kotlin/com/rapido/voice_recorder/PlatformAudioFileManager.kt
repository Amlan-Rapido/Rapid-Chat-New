package com.rapido.voice_recorder

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSURL
import platform.Foundation.NSError
import kotlinx.datetime.Clock

actual class PlatformAudioFileManager {
    private val fileManager = NSFileManager.defaultManager

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private val recordingsDir: String by lazy {
        val paths = fileManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask
        )
        val documentsURL = paths.firstOrNull() as? NSURL
            ?: throw IllegalStateException("Could not retrieve documents directory URL.")

        val recordingsURL = documentsURL.URLByAppendingPathComponent("recordings")
            ?: throw IllegalStateException("Could not construct recordings directory URL from documents URL.")

        val path = recordingsURL.path
            ?: throw IllegalStateException("Could not convert recordings URL to path string.")

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
                    throw IllegalStateException("Failed to create recordings directory at $path. Error: ${error?.localizedDescription ?: "Unknown error"}")
                }
            } else {
                // Verify it's a directory
                var isDirectory: ObjCObjectVar<Boolean> = alloc()
                if (!fileManager.fileExistsAtPath(path, isDirectory = isDirectory.ptr) || !isDirectory.value) {
                    throw IllegalStateException("Path exists but is not a directory: $path")
                }
            }
        }
        path
    }

    actual fun createRecordingFilePath(): String {
        return "$recordingsDir/${generateFileName()}"
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual fun deleteRecording(filePath: String): Boolean {
        val baseRecordingsDir = recordingsDir
        if (baseRecordingsDir.isEmpty()) {
            println("Error: Recordings directory path is invalid or not initialized.")
            return false
        }

        val fileURL = NSURL.fileURLWithPath(filePath)
        val recordingsDirectoryURL = NSURL.fileURLWithPath(baseRecordingsDir)

        if (!fileURL.path!!.startsWith(recordingsDirectoryURL.path!!)) {
            println("Security: Attempt to delete file outside of recordings directory: $filePath")
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
                    println("Error deleting file $filePath: ${error?.localizedDescription ?: "Unknown error"}")
                }
                success
            }
        } catch (e: Exception) {
            println("Exception during file deletion of $filePath: ${e.message}")
            false
        }
    }

    actual fun getRecordingsDirectory(): String {
        return recordingsDir
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