package com.rapido.voice_recorder

import kotlinx.cinterop.BetaInteropApi
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AVFAudio.AVAudioQualityHigh
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.Foundation.*
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class PlatformVoiceRecorder {
    private val platformAudioFileManager = PlatformAudioFileManager()
    private val recorderRef = kotlin.concurrent.AtomicReference<AVAudioRecorder?>(null)
    private val playerRef = kotlin.concurrent.AtomicReference<AVAudioPlayer?>(null)
    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0
    private var onPlaybackCompletedListener: (() -> Unit)? = null
    
    // Create a delegate for handling audio player events
    private val playerDelegate = object : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            debugLog("Playback completed, success: $successfully")
            onPlaybackCompletedListener?.invoke()
        }
    }

    @OptIn(BetaInteropApi::class)
    actual suspend fun startPlatformRecording(outputFilePath: String) {
        try {
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
                    throw Exception("Failed to initialize recorder")
                }

                recorder.prepareToRecord()
                if (recorder.record()) {
                    // Recording started successfully
                    recorderRef.value = recorder
                    currentOutputFilePath = outputFilePath
                    recordingStartTimeMs = getCurrentTimeMs()
                } else {
                    throw Exception("Failed to start recording")
                }
            }
        } catch (e: Exception) {
            recorderRef.value?.stop()
            recorderRef.value = null
            throw e
        }
    }

    actual suspend fun stopPlatformRecording(): RecordedAudio {
        val recorder = recorderRef.value ?: throw IllegalStateException("No recording in progress")
        val filePath = currentOutputFilePath ?: throw IllegalStateException("No output file path")

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

        // Return recorded audio data
        return RecordedAudio(
            filePath = filePath,
            durationMs = durationMs,
            sizeBytes = fileSize
        )
    }

    actual fun getCurrentRecordingFilePath(): String? = currentOutputFilePath

    actual suspend fun deletePlatformRecording(filePath: String): Boolean {
        // Stop playback if needed
        if (playerRef.value != null) {
            stopPlatformPlayback()
        }
        
        // Stop recording if this is the current recording
        if (filePath == currentOutputFilePath) {
            recorderRef.value?.stop()
            recorderRef.value = null
            currentOutputFilePath = null
        }
        
        return platformAudioFileManager.deleteRecording(filePath)
    }
    
    // Playback methods
    @OptIn(BetaInteropApi::class)
    actual suspend fun startPlatformPlayback(filePath: String) {
        try {
            debugLog("Starting playback of file: $filePath")
            
            // Stop any existing playback
            stopPlatformPlayback()
            
            // Set up audio session for playback
            val audioSession = AVAudioSession.sharedInstance()
            debugLog("Configuring audio session")
            audioSession.setCategory(platform.AVFAudio.AVAudioSessionCategoryPlayback, null)
            audioSession.setActive(true, null)
            
            // Create URL for the audio file
            val fileURL = NSURL.fileURLWithPath(filePath)
            if (!NSFileManager.defaultManager.fileExistsAtPath(filePath)) {
                debugLog("Audio file not found at path: $filePath")
                throw Exception("Audio file not found at path: $filePath")
            }
            debugLog("Audio file exists at path: $filePath")
            
            // Create and configure the player
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                
                debugLog("Creating AVAudioPlayer")
                val player = AVAudioPlayer(contentsOfURL = fileURL, error = error.ptr)
                
                if (player == null) {
                    throw Exception("Failed to initialize player")
                }
                
                debugLog("Configuring player")
                player.delegate = playerDelegate
                player.setVolume(1.0f)
                if (!player.prepareToPlay()) {
                    debugLog("Failed to prepare player")
                    throw Exception("Failed to prepare player")
                }
                debugLog("Player prepared successfully")
                
                if (!player.play()) {
                    debugLog("Failed to start playback")
                    throw Exception("Failed to start playback")
                }
                
                // Playback started successfully
                debugLog("Playback started successfully")
                playerRef.value = player
            }
        } catch (e: Exception) {
            debugLog("Error during playback: ${e.message}")
            playerRef.value = null
            throw e
        }
    }

    actual suspend fun stopPlatformPlayback() {
        playerRef.value?.stop()
        playerRef.value = null
    }

    actual suspend fun pausePlatformPlayback() {
        playerRef.value?.pause()
    }

    actual suspend fun resumePlatformPlayback() {
        playerRef.value?.play()
    }

    actual fun getCurrentPlaybackPositionMs(): Long {
        val player = playerRef.value ?: return 0L
        return (player.currentTime * 1000).toLong()
    }

    actual fun setOnPlaybackCompletedListener(listener: () -> Unit) {
        onPlaybackCompletedListener = listener
    }

    actual fun release() {
        // Clean up recording resources
        recorderRef.value?.stop()
        recorderRef.value = null
        
        // Clean up playback resources
        playerRef.value?.stop()
        playerRef.value = null

        // Clean up any partial recordings
        if (currentOutputFilePath != null) {
            platformAudioFileManager.deleteRecording(currentOutputFilePath!!)
            currentOutputFilePath = null
        }

        // Deactivate audio session
        try {
            AVAudioSession.sharedInstance().setActive(false, null)
        } catch (_: Exception) {
            // Ignore errors during cleanup
        }
    }

    private fun getCurrentTimeMs(): Long {
        return (NSDate.date().timeIntervalSince1970 * 1000).toLong()
    }

    private fun debugLog(message: String) {
        println("[PlatformVoiceRecorder] $message")
    }
}