package com.rapido.voice_recorder

import kotlinx.coroutines.flow.StateFlow

interface VoiceRecorder {
    val state: StateFlow<VoiceRecorderState>
    
    // Recording operations
    suspend fun startRecording()
    suspend fun stopRecording(): RecordedAudio
    suspend fun cancelRecording()
    
    // Playback operations
    suspend fun playRecording(audio: RecordedAudio)
    suspend fun pausePlayback()
    suspend fun resumePlayback()
    suspend fun stopPlayback()
    
    // File management
    suspend fun deleteRecording(audio: RecordedAudio): Boolean
    
    // Cleanup
    fun release()
}