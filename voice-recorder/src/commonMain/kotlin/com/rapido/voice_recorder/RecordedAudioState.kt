package com.rapido.voice_recorder

sealed class VoiceRecorderState {
    data object Idle : VoiceRecorderState()
    data class Recording(val durationMs: Long) : VoiceRecorderState()
    data class Completed(val recordedAudio: RecordedAudio) : VoiceRecorderState()
    data class Error(val error: Throwable) : VoiceRecorderState()
}