package com.rapido.voice_recorder

import kotlinx.coroutines.flow.StateFlow

interface VoiceRecorder {
    val state: StateFlow<VoiceRecorderState>
    suspend fun startRecording()
    suspend fun stopRecording(): RecordedAudio
    suspend fun cancelRecording()
    fun release()
}