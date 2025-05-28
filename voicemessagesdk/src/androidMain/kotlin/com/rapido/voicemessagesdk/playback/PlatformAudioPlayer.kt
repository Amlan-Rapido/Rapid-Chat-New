package com.rapido.voicemessagesdk.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.rapido.voicemessagesdk.PlatformContextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class PlatformAudioPlayer actual constructor() : AudioPlayer {
    
    companion object {
        private const val TAG = "PlatformAudioPlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
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

    actual override suspend fun startPlayback(filePath: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting playback of file: $filePath")
                stopPlayback()

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

                    audioFocusRequest =
                        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
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

    actual override suspend fun pausePlayback() {
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

    actual override suspend fun resumePlayback() {
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

    actual override suspend fun stopPlayback() = withContext(Dispatchers.IO) {
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

    actual override fun getCurrentPlaybackPositionMs(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting playback position: ${e.message}", e)
            0L
        }
    }

    actual override fun setOnPlaybackCompletedListener(listener: () -> Unit) {
        onPlaybackCompletedListener = listener
    }

    actual override fun release() {
        try {
            Log.d(TAG, "Releasing playback resources")
            mediaPlayer?.apply {
                try {
                    release()
                } catch (e: Exception) {
                    Log.w(TAG, "Non-critical error releasing MediaPlayer: ${e.message}")
                }
            }
            mediaPlayer = null
            abandonAudioFocus()
        } catch (e: Exception) {
            Log.e(TAG, "Error during playback release: ${e.message}", e)
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