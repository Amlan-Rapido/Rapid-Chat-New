package com.rapido.voice_recorder

import android.media.MediaPlayer
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

actual class PlatformVoiceRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0
    private var onPlaybackCompletedListener: (() -> Unit)? = null

    actual suspend fun startPlatformRecording(outputFilePath: String) {
        withContext(Dispatchers.IO) {
            try {
                // Ensure file directory exists
                val file = File(outputFilePath)
                file.parentFile?.mkdirs()

                // Initialize MediaRecorder
                mediaRecorder =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        MediaRecorder(getAppContext())
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }

                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(outputFilePath)

                    try {
                        prepare()
                        start()

                        // Store the output file path and start time
                        currentOutputFilePath = outputFilePath
                        recordingStartTimeMs = System.currentTimeMillis()
                    } catch (e: IOException) {
                        release()
                        mediaRecorder = null
                        throw e
                    }
                } ?: throw IllegalStateException("Failed to initialize MediaRecorder")
            } catch (e: Exception) {
                mediaRecorder?.release()
                mediaRecorder = null
                throw e
            }
        }
    }

    actual suspend fun stopPlatformRecording(): RecordedAudio = withContext(Dispatchers.IO) {
        try {
            val filePath = currentOutputFilePath ?: throw IllegalStateException("No recording in progress")
            val file = File(filePath)

            // Calculate duration
            val durationMs = System.currentTimeMillis() - recordingStartTimeMs

            // Stop recording
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            // Reset state
            currentOutputFilePath = null

            // Return recorded audio data
            RecordedAudio(
                filePath = filePath,
                durationMs = durationMs,
                sizeBytes = file.length()
            )
        } catch (e: Exception) {
            release()
            throw e
        }
    }

    actual suspend fun cancelPlatformRecording() = withContext(Dispatchers.IO) {
        try {
            // Stop and release recorder
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            // Delete the output file if it exists
            currentOutputFilePath?.let { filePath ->
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            }

            // Reset state
            currentOutputFilePath = null
        } catch (e: Exception) {
            // Just release resources on error during cancel
            release()
        }
    }

    // Playback methods
    actual suspend fun startPlatformPlayback(filePath: String) = withContext(Dispatchers.IO) {
        try {
            // Stop any existing playback
            stopPlatformPlayback()
            
            // Create and set up new MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    onPlaybackCompletedListener?.invoke()
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            throw e
        }
    }

    actual suspend fun pausePlatformPlayback()  {
        withContext(Dispatchers.IO) {
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    actual suspend fun resumePlatformPlayback()  {
        withContext(Dispatchers.IO) {
            try {
                mediaPlayer?.start()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    actual suspend fun stopPlatformPlayback() = withContext(Dispatchers.IO) {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            // Just cleanup on error
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    actual fun getCurrentPlaybackPositionMs(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    actual suspend fun deletePlatformRecording(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Make sure we're not playing this file
            if (mediaPlayer != null) {
                stopPlatformPlayback()
            }
            
            // Delete the file
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    actual fun setOnPlaybackCompletedListener(listener: () -> Unit) {
        onPlaybackCompletedListener = listener
    }

    actual fun release() {
        // Clean up recording resources
        mediaRecorder?.release()
        mediaRecorder = null

        // Clean up playback resources
        mediaPlayer?.release()
        mediaPlayer = null

        // Clean up any partial recordings
        currentOutputFilePath?.let { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        currentOutputFilePath = null
    }

    private fun getAppContext() = PlatformContextProvider.appContext

}