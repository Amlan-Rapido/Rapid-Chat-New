package com.rapido.voice_recorder

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class PlatformVoiceRecorder {
    private val platformAudioFileManager = PlatformAudioFileManager()
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0
    private var onPlaybackCompletedListener: (() -> Unit)? = null

    actual suspend fun startPlatformRecording(outputFilePath: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(outputFilePath)
                file.parentFile?.mkdirs()

                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(PlatformContextProvider.appContext)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(outputFilePath)

                    prepare()
                    start()
                }

                currentOutputFilePath = outputFilePath
                recordingStartTimeMs = System.currentTimeMillis()
                Log.d("VoiceRecorder", "Recording started: $outputFilePath")
            } catch (e: Exception) {
                mediaRecorder?.release()
                mediaRecorder = null
                currentOutputFilePath = null
                throw e
            }
        }
    }

    actual suspend fun stopPlatformRecording(): RecordedAudio = withContext(Dispatchers.IO) {
        try {
            val recorder = mediaRecorder ?: throw IllegalStateException("No recording in progress")
            val filePath = currentOutputFilePath ?: throw IllegalStateException("No output file path")

            val durationMs = System.currentTimeMillis() - recordingStartTimeMs

            recorder.stop()
            recorder.release()
            mediaRecorder = null

            val file = File(filePath)
            currentOutputFilePath = null

            RecordedAudio(filePath, durationMs, file.length())
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            currentOutputFilePath = null
            throw e
        }
    }

    actual fun getCurrentRecordingFilePath(): String? = currentOutputFilePath

    actual suspend fun deletePlatformRecording(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Stop playback if this file is playing
            if (mediaPlayer != null) {
                stopPlatformPlayback()
            }

            // Stop recording if this is the current recording
            if (filePath == currentOutputFilePath) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        // Ignore stop errors
                    }
                    release()
                }
                mediaRecorder = null
                currentOutputFilePath = null
            }

            // Delete the file
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Error deleting recording: ${e.message}")
            false
        }
    }

    actual suspend fun startPlatformPlayback(filePath: String) = withContext(Dispatchers.IO) {
        try {
            stopPlatformPlayback()

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

    actual suspend fun pausePlatformPlayback() {
        withContext(Dispatchers.IO){
            mediaPlayer?.pause()
        }
    }

    actual suspend fun resumePlatformPlayback()  {
        withContext(Dispatchers.IO){
            mediaPlayer?.start()
        }
    }

    actual suspend fun stopPlatformPlayback() = withContext(Dispatchers.IO) {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    actual fun getCurrentPlaybackPositionMs(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    actual fun setOnPlaybackCompletedListener(listener: () -> Unit) {
        onPlaybackCompletedListener = listener
    }

    actual fun release() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null

        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        currentOutputFilePath?.let {
            platformAudioFileManager.deleteRecording(it)
        }
        currentOutputFilePath = null
    }
}
