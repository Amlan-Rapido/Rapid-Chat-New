package com.rapido.voice_recorder

expect class PlatformVoiceRecorder() {

    suspend fun startPlatformRecording(outputFilePath: String)

    suspend fun stopPlatformRecording(): RecordedAudio

    suspend fun cancelPlatformRecording()

    fun release()
}