package com.rapido.voicemessagesdk.recording

import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.rapido.voicemessagesdk.PlatformContextProvider
import com.rapido.voicemessagesdk.core.RecordedAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class PlatformAudioRecorder actual constructor() : AudioRecorder {
    
    companion object {
        private const val TAG = "PlatformAudioRecorder"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0

    actual override suspend fun startRecording(outputFilePath: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting recording to file: $outputFilePath")
                val file = File(outputFilePath)
                if (!file.parentFile?.exists()!!) {
                    Log.d(
                        TAG,
                        "Parent directory doesn't exist, creating: ${file.parentFile?.absolutePath}"
                    )
                    file.parentFile?.mkdirs()
                }

                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Log.d(TAG, "Using new MediaRecorder constructor (API >= 31)")
                    MediaRecorder(PlatformContextProvider.appContext)
                } else {
                    Log.d(TAG, "Using deprecated MediaRecorder constructor (API < 31)")
                    @Suppress("DEPRECATION")
                    (MediaRecorder())
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(outputFilePath)

                    Log.d(TAG, "Preparing MediaRecorder...")
                    prepare()
                    Log.d(TAG, "Starting MediaRecorder...")
                    start()
                }

                currentOutputFilePath = outputFilePath
                recordingStartTimeMs = System.currentTimeMillis()
                Log.d(TAG, "Recording started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recording: ${e.message}", e)
                mediaRecorder?.release()
                mediaRecorder = null
                currentOutputFilePath = null
                throw e
            }
        }
    }

    actual override suspend fun stopRecording(): RecordedAudio = withContext(Dispatchers.IO) {
        try {
            val recorder = mediaRecorder ?: throw IllegalStateException("No recording in progress")
            val filePath = currentOutputFilePath ?: throw IllegalStateException("No output file path")
            Log.d(TAG, "Stopping recording: $filePath")

            val durationMs = System.currentTimeMillis() - recordingStartTimeMs

            recorder.stop()
            recorder.release()
            mediaRecorder = null

            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Recording file not found after stopping: $filePath")
                throw IllegalStateException("Recording file not found: $filePath")
            }

            Log.d(
                TAG,
                "Recording stopped successfully. Duration: ${durationMs}ms, Size: ${file.length()} bytes"
            )
            currentOutputFilePath = null

            RecordedAudio(filePath, durationMs, file.length())
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}", e)
            mediaRecorder?.release()
            mediaRecorder = null
            currentOutputFilePath = null
            throw e
        }
    }

    actual override fun getCurrentRecordingFilePath(): String? = currentOutputFilePath

    actual override fun deleteRecording(filePath: String): Boolean {
        return try {
            Log.d(TAG, "Attempting to delete recording: $filePath")

            // Stop recording if this is the current recording
            if (filePath == currentOutputFilePath) {
                Log.d(TAG, "Stopping current recording before deletion")
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "Non-critical error stopping recorder: ${e.message}")
                    }
                    release()
                }
                mediaRecorder = null
                currentOutputFilePath = null
            }

            // Delete the file
            val file = File(filePath)
            val result = if (file.exists() && file.isFile) {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "Successfully deleted file: $filePath")
                } else {
                    Log.e(TAG, "Failed to delete file: $filePath")
                }
                deleted
            } else {
                Log.w(TAG, "File doesn't exist or is not a file: $filePath")
                false
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting recording: ${e.message}", e)
            false
        }
    }

    actual override fun release() {
        try {
            Log.d(TAG, "Releasing recording resources")
            mediaRecorder?.apply {
                try {
                    release()
                } catch (e: Exception) {
                    Log.w(TAG, "Non-critical error releasing MediaRecorder: ${e.message}")
                }
            }
            mediaRecorder = null
            currentOutputFilePath = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during recording release: ${e.message}", e)
        }
    }
} 