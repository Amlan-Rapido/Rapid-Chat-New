package com.rapido.voice_recorder

sealed class VoiceRecorderState {
    data object Idle : VoiceRecorderState()
    
    data class Recording(
        val durationMs: Long
    ) : VoiceRecorderState()
    
    data class Preview(
        val audio: RecordedAudio,
        val playing: Boolean = false,
        val currentPositionMs: Long = 0
    ) : VoiceRecorderState()
    
    data class Error(
        val exception: Exception,
        val source: ErrorSource
    ) : VoiceRecorderState()
    
    enum class ErrorSource {
        RECORDING,
        PLAYBACK,
        FILE_OPERATION
    }
    
    val isRecording: Boolean
        get() = this is Recording
    
    val isPlaying: Boolean
        get() = this is Preview && playing
    
    val currentAudio: RecordedAudio?
        get() = when (this) {
            is Preview -> audio
            else -> null
        }
}