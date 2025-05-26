package com.rapido.voice_recorder

expect class PlatformVoiceRecorder {

    // Recording
    suspend fun startPlatformRecording(outputFilePath: String)

    suspend fun stopPlatformRecording(): RecordedAudio

    // Playback
    suspend fun startPlatformPlayback(filePath: String)

    suspend fun pausePlatformPlayback()

    suspend fun resumePlatformPlayback()

    suspend fun stopPlatformPlayback()

    fun getCurrentPlaybackPositionMs(): Long

    suspend fun deletePlatformRecording(filePath: String): Boolean

    // This function is a way to observe playback completion in KMP
    // equivalent to Android's MediaPlayer.setOnCompletionListener
    fun setOnPlaybackCompletedListener(listener: () -> Unit)

    fun release()
}