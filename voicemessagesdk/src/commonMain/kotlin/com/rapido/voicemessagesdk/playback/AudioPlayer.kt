package com.rapido.voicemessagesdk.playback

/**
 * Interface for audio playback operations.
 * Handles the low-level playback functionality.
 */
interface AudioPlayer {
    /**
     * Starts playing an audio file.
     * @param filePath Path to the audio file to play
     */
    suspend fun startPlayback(filePath: String)

    /**
     * Pauses the current playback.
     */
    suspend fun pausePlayback()

    /**
     * Resumes the paused playback.
     */
    suspend fun resumePlayback()

    /**
     * Stops the current playback.
     */
    suspend fun stopPlayback()

    /**
     * Gets the current playback position in milliseconds.
     * @return Current position in milliseconds
     */
    fun getCurrentPlaybackPositionMs(): Long

    /**
     * Sets a listener for playback completion events.
     * @param listener Callback to invoke when playback completes
     */
    fun setOnPlaybackCompletedListener(listener: () -> Unit)

    /**
     * Releases playback resources.
     */
    fun release()
} 