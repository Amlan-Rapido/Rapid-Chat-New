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
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AVFAudio.AVAudioQualityHigh
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVEncoderAudioQualityKey
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import kotlinx.cinterop.alloc
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class PlatformVoiceRecorder {
    // Use atomic reference to handle potential threading issues
    private val recorderRef = kotlin.concurrent.AtomicReference<AVAudioRecorder?>(null)
    private val playerRef = kotlin.concurrent.AtomicReference<AVAudioPlayer?>(null)
    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0
    private var onPlaybackCompletedListener: (() -> Unit)? = null
    
    // Create a delegate for handling audio player events
    private val playerDelegate = object : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            onPlaybackCompletedListener?.invoke()
        }
    }

    @OptIn(BetaInteropApi::class)
    actual suspend fun startPlatformRecording(outputFilePath: String) {
        try {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory("record", null)
            audioSession.setActive(true, null)

            // Create a file URL for the recording
            val fileManager = NSFileManager.defaultManager

            // Ensure the directory exists
            val directory = NSURL.fileURLWithPath(getDocumentsDirectory())
            val recordingURL = directory.URLByAppendingPathComponent(outputFilePath.split("/").last())
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

                if (error.value != null) {
                    throw Exception("Failed to initialize recorder: ${error.value?.localizedDescription}")
                }

                if (recorder != null) {
                    recorder.prepareToRecord()
                    if (recorder.record()) {
                        // Recording started successfully
                        recorderRef.value = recorder
                        currentOutputFilePath = recordingURL.path
                        recordingStartTimeMs = getCurrentTimeMs()
                    } else {
                        throw Exception("Failed to start recording")
                    }
                } else {
                    throw Exception("Failed to create AVAudioRecorder")
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

    actual suspend fun cancelPlatformRecording() {
        val recorder = recorderRef.value
        val filePath = currentOutputFilePath

        // Stop recording
        recorder?.stop()
        recorderRef.value = null

        // Delete the file if it exists
        if (filePath != null) {
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(filePath)) {
                fileManager.removeItemAtPath(filePath, null)
            }
        }

        // Reset state
        currentOutputFilePath = null
    }
    
    // Playback methods
    @OptIn(BetaInteropApi::class)
    actual suspend fun startPlatformPlayback(filePath: String) {
        try {
            // Stop any existing playback
            stopPlatformPlayback()
            
            // Set up audio session for playback
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory("playback", null)
            audioSession.setActive(true, null)
            
            // Create URL for the audio file
            val fileURL = NSURL.fileURLWithPath(filePath)
            
            // Create and configure the player
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                
                val player = AVAudioPlayer(contentsOfURL = fileURL, error = error.ptr)
                
                if (error.value != null) {
                    throw Exception("Failed to initialize player: ${error.value?.localizedDescription}")
                }
                
                if (player != null) {
                    player.delegate = playerDelegate
                    player.prepareToPlay()
                    if (player.play()) {
                        // Playback started successfully
                        playerRef.value = player
                    } else {
                        throw Exception("Failed to start playback")
                    }
                } else {
                    throw Exception("Failed to create AVAudioPlayer")
                }
            }
        } catch (e: Exception) {
            playerRef.value = null
            throw e
        }
    }
    
    actual suspend fun pausePlatformPlayback() {
        val player = playerRef.value ?: return
        player.pause()
    }
    
    actual suspend fun resumePlatformPlayback() {
        val player = playerRef.value ?: return
        player.play()
    }
    
    actual suspend fun stopPlatformPlayback() {
        val player = playerRef.value ?: return
        player.stop()
        playerRef.value = null
    }
    
    actual fun getCurrentPlaybackPositionMs(): Long {
        val player = playerRef.value ?: return 0L
        return (player.currentTime * 1000).toLong()
    }
    
    actual suspend fun deletePlatformRecording(filePath: String): Boolean {
        // Stop playback if needed
        if (playerRef.value != null) {
            stopPlatformPlayback()
        }
        
        // Delete the file
        val fileManager = NSFileManager.defaultManager
        return if (fileManager.fileExistsAtPath(filePath)) {
            try {
                fileManager.removeItemAtPath(filePath, null)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
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
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(currentOutputFilePath!!)) {
                fileManager.removeItemAtPath(currentOutputFilePath!!, null)
            }
            currentOutputFilePath = null
        }

        // Deactivate audio session
        try {
            AVAudioSession.sharedInstance().setActive(false, null)
        } catch (_: Exception) {
            // Ignore errors during cleanup
        }
    }

    private fun getDocumentsDirectory(): String {
        val paths = NSFileManager.defaultManager.URLsForDirectory(
            directory = platform.Foundation.NSDocumentDirectory,
            inDomains = platform.Foundation.NSUserDomainMask
        )
        return (paths.firstOrNull() as? NSURL)?.path ?: ""
    }

    private fun getCurrentTimeMs(): Long {
        return (platform.Foundation.NSDate.date().timeIntervalSince1970 * 1000).toLong()
    }
}