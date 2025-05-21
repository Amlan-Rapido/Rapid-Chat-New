package com.rapido.voice_recorder

sealed class VoiceRecorderState {
    data object Idle : VoiceRecorderState()
    data class Recording(val durationMs: Long) : VoiceRecorderState()
    data class Completed(val recordedAudio: RecordedAudio) : VoiceRecorderState()
    data class Playing(val audio: RecordedAudio, val positionMs: Long) : VoiceRecorderState()
    data class Paused(val audio: RecordedAudio, val positionMs: Long) : VoiceRecorderState()
    data class Error(val error: Throwable, val errorSource: ErrorSource) : VoiceRecorderState()
    
    enum class ErrorSource {
        RECORDING, PLAYBACK, OTHER
    }
}