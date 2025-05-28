package com.rapido.voicemessagesdk.playback

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class PlatformAudioPlayer actual constructor() : AudioPlayer {
    
    private val playerRef = kotlin.concurrent.AtomicReference<AVAudioPlayer?>(null)
    private var onPlaybackCompletedListener: (() -> Unit)? = null

    // Create a delegate for handling audio player events
    private val playerDelegate = object : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            println("[PlatformAudioPlayer] Playback completed, success: $successfully")
            onPlaybackCompletedListener?.invoke()
        }
    }

    @OptIn(BetaInteropApi::class)
    actual override suspend fun startPlayback(filePath: String) {
        try {
            println("[PlatformAudioPlayer] Starting playback of file: $filePath")

            // Stop any existing playback
            stopPlayback()

            // Set up audio session for playback
            val audioSession = AVAudioSession.sharedInstance()
            println("[PlatformAudioPlayer] Configuring audio session")
            audioSession.setCategory(platform.AVFAudio.AVAudioSessionCategoryPlayback, null)
            audioSession.setActive(true, null)

            // Create URL for the audio file
            val fileURL = NSURL.fileURLWithPath(filePath)
            if (!NSFileManager.defaultManager.fileExistsAtPath(filePath)) {
                println("[PlatformAudioPlayer] Audio file not found at path: $filePath")
                throw Exception("Audio file not found at path: $filePath")
            }
            println("[PlatformAudioPlayer] Audio file exists at path: $filePath")

            // Create and configure the player
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()

                println("[PlatformAudioPlayer] Creating AVAudioPlayer")
                val player = AVAudioPlayer(contentsOfURL = fileURL, error = error.ptr)

                if (player == null) {
                    val errorMsg = error.value?.description ?: "Unknown error"
                    throw Exception("Failed to initialize player: $errorMsg")
                }

                println("[PlatformAudioPlayer] Configuring player")
                player.delegate = playerDelegate
                player.setVolume(1.0f)
                if (!player.prepareToPlay()) {
                    println("[PlatformAudioPlayer] Failed to prepare player")
                    throw Exception("Failed to prepare player")
                }
                println("[PlatformAudioPlayer] Player prepared successfully")

                if (!player.play()) {
                    println("[PlatformAudioPlayer] Failed to start playback")
                    throw Exception("Failed to start playback")
                }

                // Playback started successfully
                println("[PlatformAudioPlayer] Playback started successfully")
                playerRef.value = player
            }
        } catch (e: Exception) {
            println("[PlatformAudioPlayer] Error during playback: ${e.message}")
            playerRef.value = null
            throw e
        }
    }

    actual override suspend fun pausePlayback() {
        try {
            println("[PlatformAudioPlayer] Pausing playback")
            playerRef.value?.pause()
        } catch (e: Exception) {
            println("[PlatformAudioPlayer] Error pausing playback: ${e.message}")
            throw e
        }
    }

    actual override suspend fun resumePlayback() {
        try {
            println("[PlatformAudioPlayer] Resuming playback")
            playerRef.value?.play()
        } catch (e: Exception) {
            println("[PlatformAudioPlayer] Error resuming playback: ${e.message}")
            throw e
        }
    }

    actual override suspend fun stopPlayback() {
        try {
            println("[PlatformAudioPlayer] Stopping playback")
            playerRef.value?.stop()
            playerRef.value = null
        } catch (e: Exception) {
            println("[PlatformAudioPlayer] Error stopping playback: ${e.message}")
            throw e
        }
    }

    actual override fun getCurrentPlaybackPositionMs(): Long {
        return try {
            val player = playerRef.value ?: return 0L
            (player.currentTime * 1000).toLong()
        } catch (e: Exception) {
            println("[PlatformAudioPlayer] Error getting playback position: ${e.message}")
            0L
        }
    }

    actual override fun setOnPlaybackCompletedListener(listener: () -> Unit) {
        onPlaybackCompletedListener = listener
    }

    actual override fun release() {
        try {
            println("[PlatformAudioPlayer] Releasing playback resources")
            
            // Clean up playback resources
            playerRef.value?.stop()
            playerRef.value = null

            // Deactivate audio session
            try {
                AVAudioSession.sharedInstance().setActive(false, null)
            } catch (_: Exception) {
                // Ignore errors during cleanup
            }
        } catch (e: Exception) {
            println("[PlatformAudioPlayer] Error during release: ${e.message}")
        }
    }
} 