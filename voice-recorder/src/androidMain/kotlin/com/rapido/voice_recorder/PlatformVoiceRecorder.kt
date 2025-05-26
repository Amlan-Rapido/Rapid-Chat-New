package com.rapido.voice_recorder

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.AudioAttributes
import android.content.Context

actual class PlatformVoiceRecorder {
    companion object {
        private const val TAG = "PlatformVoiceRecorderAndroid"
    }

    private val platformAudioFileManager = PlatformAudioFileManager()
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFilePath: String? = null
    private var recordingStartTimeMs: Long = 0
    private var onPlaybackCompletedListener: (() -> Unit)? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager: AudioManager by lazy {
        PlatformContextProvider.appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Pause playback
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                        onPlaybackCompletedListener?.invoke()
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower the volume
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume playback or restore volume
                mediaPlayer?.let {
                    it.setVolume(1.0f, 1.0f)
                    if (!it.isPlaying) {
                        it.start()
                    }
                }
            }
        }
    }

    actual suspend fun startPlatformRecording(outputFilePath: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting recording to file: $outputFilePath")
                val file = File(outputFilePath)
                if (!file.parentFile?.exists()!!) {
                    Log.d(TAG, "Parent directory doesn't exist, creating: ${file.parentFile?.absolutePath}")
                    file.parentFile?.mkdirs()
                }

                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Log.d(TAG, "Using new MediaRecorder constructor (API >= 31)")
                    MediaRecorder(PlatformContextProvider.appContext)
                } else {
                    Log.d(TAG, "Using deprecated MediaRecorder constructor (API < 31)")
                    @Suppress("DEPRECATION")
                    MediaRecorder()
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

    actual suspend fun stopPlatformRecording(): RecordedAudio = withContext(Dispatchers.IO) {
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
            
            Log.d(TAG, "Recording stopped successfully. Duration: ${durationMs}ms, Size: ${file.length()} bytes")
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

    actual fun getCurrentRecordingFilePath(): String? = currentOutputFilePath

    actual suspend fun deletePlatformRecording(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to delete recording: $filePath")
            
            // Stop playback if this file is playing
            if (mediaPlayer != null) {
                Log.d(TAG, "Stopping playback before deletion")
                stopPlatformPlayback()
            }

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

    actual suspend fun startPlatformPlayback(filePath: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting playback of file: $filePath")
                stopPlatformPlayback()

                val file = File(filePath)
                if (!file.exists()) {
                    Log.e(TAG, "Playback file not found: $filePath")
                    throw IllegalStateException("Audio file not found: $filePath")
                }

                // Request audio focus
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()

                    audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(audioAttributes)
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        .build()

                    val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                    if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        Log.e(TAG, "Failed to get audio focus")
                        throw IllegalStateException("Could not get audio focus")
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val result = audioManager.requestAudioFocus(
                        audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                    )
                    if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        Log.e(TAG, "Failed to get audio focus")
                        throw IllegalStateException("Could not get audio focus")
                    }
                }

                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(filePath)
                    setOnCompletionListener {
                        Log.d(TAG, "Playback completed")
                        abandonAudioFocus()
                        onPlaybackCompletedListener?.invoke()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                        abandonAudioFocus()
                        false
                    }
                    Log.d(TAG, "Preparing MediaPlayer...")
                    prepare()
                    Log.d(TAG, "Starting playback...")
                    start()
                }
                Log.d(TAG, "Playback started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting playback: ${e.message}", e)
                abandonAudioFocus()
                mediaPlayer?.release()
                mediaPlayer = null
                throw e
            }
        }
    }

    actual suspend fun pausePlatformPlayback() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Pausing playback")
                mediaPlayer?.pause()
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing playback: ${e.message}", e)
                throw e
            }
        }
    }

    actual suspend fun resumePlatformPlayback() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Resuming playback")
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming playback: ${e.message}", e)
                throw e
            }
        }
    }

    actual suspend fun stopPlatformPlayback() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stopping playback")
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            abandonAudioFocus()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback: ${e.message}", e)
            throw e
        }
    }

    actual fun getCurrentPlaybackPositionMs(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting playback position: ${e.message}", e)
            0L
        }
    }

    actual fun setOnPlaybackCompletedListener(listener: () -> Unit) {
        onPlaybackCompletedListener = listener
    }

    actual fun release() {
        try {
            Log.d(TAG, "Releasing resources")
            mediaRecorder?.apply {
                try {
                    release()
                } catch (e: Exception) {
                    Log.w(TAG, "Non-critical error releasing MediaRecorder: ${e.message}")
                }
            }
            mediaRecorder = null

            mediaPlayer?.apply {
                try {
                    release()
                } catch (e: Exception) {
                    Log.w(TAG, "Non-critical error releasing MediaPlayer: ${e.message}")
                }
            }
            mediaPlayer = null

            currentOutputFilePath?.let {
                Log.d(TAG, "Cleaning up current recording file: $it")
                platformAudioFileManager.deleteRecording(it)
            }
            currentOutputFilePath = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during release: ${e.message}", e)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }
}
