package com.rapido.voice_recorder

import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

actual class PlatformVoiceRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0

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

    actual fun release() {
        mediaRecorder?.release()
        mediaRecorder = null

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