package com.rapido.voicemessagesdk.recording

import com.rapido.voicemessagesdk.core.VoiceMessage
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioQualityHigh
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
actual class PlatformAudioRecorder actual constructor() : AudioRecorder {
    
    private val recorderRef = kotlin.concurrent.AtomicReference<AVAudioRecorder?>(null)
    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0

    @OptIn(BetaInteropApi::class)
    actual override suspend fun startRecording(outputFilePath: String) {
        try {
            println("[PlatformAudioRecorder] Starting recording to file: $outputFilePath")
            
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(platform.AVFAudio.AVAudioSessionCategoryRecord, null)
            audioSession.setActive(true, null)

            // Create a file URL for the recording
            val recordingURL = NSURL.fileURLWithPath(outputFilePath)
                ?: throw Exception("Could not create URL for recording")

            // Create recording settings
            val recordSettings = mapOf<Any?, Any?>(
                AVFormatIDKey to kAudioFormatMPEG4AAC,
                AVSampleRateKey to 44100.0,
                AVNumberOfChannelsKey to 2,
                AVEncoderAudioQualityKey to AVAudioQualityHigh
            )

            // Create and configure the recorder
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()

                val recorder = AVAudioRecorder(
                    uRL = recordingURL,
                    settings = recordSettings,
                    error = error.ptr
                )

                if (recorder == null) {
                    val errorMsg = error.value?.description ?: "Unknown error"
                    throw Exception("Failed to initialize recorder: $errorMsg")
                }

                recorder.prepareToRecord()
                if (recorder.record()) {
                    // Recording started successfully
                    recorderRef.value = recorder
                    currentOutputFilePath = outputFilePath
                    recordingStartTimeMs = getCurrentTimeMs()
                    println("[PlatformAudioRecorder] Recording started successfully")
                } else {
                    throw Exception("Failed to start recording")
                }
            }
        } catch (e: Exception) {
            println("[PlatformAudioRecorder] Error starting recording: ${e.message}")
            recorderRef.value?.stop()
            recorderRef.value = null
            currentOutputFilePath = null
            throw e
        }
    }

    actual override suspend fun stopRecording(): VoiceMessage {
        val recorder = recorderRef.value ?: throw IllegalStateException("No recording in progress")
        val filePath = currentOutputFilePath ?: throw IllegalStateException("No output file path")

        try {
            println("[PlatformAudioRecorder] Stopping recording: $filePath")
            
            // Calculate duration
            val durationMs = getCurrentTimeMs() - recordingStartTimeMs

            // Stop recording
            recorder.stop()
            recorderRef.value = null

            // Get file size
            val fileManager = NSFileManager.defaultManager
            val fileAttributes = fileManager.attributesOfItemAtPath(filePath, null)
            val fileSize = (fileAttributes?.get("NSFileSize") as? Number)?.toLong() ?: 0L

            // Reset state
            currentOutputFilePath = null

            println("[PlatformAudioRecorder] Recording stopped successfully. Duration: ${durationMs}ms, Size: ${fileSize} bytes")

            // Return recorded audio data
            return VoiceMessage(
                filePath = filePath,
                durationMs = durationMs,
                sizeBytes = fileSize
            )
        } catch (e: Exception) {
            println("[PlatformAudioRecorder] Error stopping recording: ${e.message}")
            recorderRef.value = null
            currentOutputFilePath = null
            throw e
        }
    }

    actual override fun getCurrentRecordingFilePath(): String? = currentOutputFilePath

    actual override fun deleteRecording(filePath: String): Boolean {
        return try {
            println("[PlatformAudioRecorder] Attempting to delete recording: $filePath")
            
            // Stop recording if this is the current recording
            if (filePath == currentOutputFilePath) {
                println("[PlatformAudioRecorder] Stopping current recording before deletion")
                recorderRef.value?.stop()
                recorderRef.value = null
                currentOutputFilePath = null
            }

            // Delete the file
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(filePath)) {
                val success = fileManager.removeItemAtPath(filePath, null)
                if (success) {
                    println("[PlatformAudioRecorder] Successfully deleted file: $filePath")
                } else {
                    println("[PlatformAudioRecorder] Failed to delete file: $filePath")
                }
                success
            } else {
                println("[PlatformAudioRecorder] File doesn't exist: $filePath")
                false
            }
        } catch (e: Exception) {
            println("[PlatformAudioRecorder] Error deleting recording: ${e.message}")
            false
        }
    }

    actual override fun release() {
        try {
            println("[PlatformAudioRecorder] Releasing recording resources")
            
            // Clean up recording resources
            recorderRef.value?.stop()
            recorderRef.value = null

            // Clean up any partial recordings
            currentOutputFilePath?.let { filePath ->
                println("[PlatformAudioRecorder] Cleaning up current recording file: $filePath")
                deleteRecording(filePath)
            }
            currentOutputFilePath = null

            // Deactivate audio session
            try {
                AVAudioSession.sharedInstance().setActive(false, null)
            } catch (_: Exception) {
                // Ignore errors during cleanup
            }
        } catch (e: Exception) {
            println("[PlatformAudioRecorder] Error during release: ${e.message}")
        }
    }

    private fun getCurrentTimeMs(): Long {
        return (NSDate.date().timeIntervalSince1970 * 1000).toLong()
    }
} 