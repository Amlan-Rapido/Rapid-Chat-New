package com.rapido.voicemessagesdk.playback

expect class PlatformAudioPlayer() : AudioPlayer {
    override suspend fun startPlayback(filePath: String)
    override suspend fun pausePlayback()
    override suspend fun resumePlayback()
    override suspend fun stopPlayback()
    override fun getCurrentPlaybackPositionMs(): Long
    override fun setOnPlaybackCompletedListener(listener: () -> Unit)
    override fun release()
} 