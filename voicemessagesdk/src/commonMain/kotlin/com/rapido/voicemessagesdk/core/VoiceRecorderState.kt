package com.rapido.voicemessagesdk.core

sealed class VoiceRecorderState {
    data object Idle : VoiceRecorderState()

    data class Recording(
        val durationMs: Long
    ) : VoiceRecorderState()

    data class RecordingCompleted(
        val voiceMessage: VoiceMessage
    ) : VoiceRecorderState()

    data class Preview(
        val voiceMessage: VoiceMessage,
        val playing: Boolean = false,
        val currentPositionMs: Long = 0
    ) : VoiceRecorderState()

    data class ReadyToSend(
        val voiceMessage: VoiceMessage
    ) : VoiceRecorderState()

    // Future sync states
    data class Sending(
        val voiceMessage: VoiceMessage,
        val progress: Float = 0f
    ) : VoiceRecorderState()

    data class Sent(
        val voiceMessage: VoiceMessage,
        val remoteUrl: String? = null
    ) : VoiceRecorderState()

    data class SendFailed(
        val voiceMessage: VoiceMessage,
        val error: Throwable
    ) : VoiceRecorderState()

    data class Error(
        val exception: VoiceRecorderException,
        val source: ErrorSource,
        val voiceMessage: VoiceMessage? = null
    ) : VoiceRecorderState()

    enum class ErrorSource {
        RECORDING,
        PLAYBACK,
        FILE_OPERATION,
        NETWORK
    }

    val isRecording: Boolean
        get() = this is Recording

    val isPlaying: Boolean
        get() = this is Preview && playing

    val currentVoiceMessage: VoiceMessage?
        get() = when (this) {
            is RecordingCompleted -> voiceMessage
            is Preview -> voiceMessage
            is ReadyToSend -> voiceMessage
            is Sending -> voiceMessage
            is Sent -> voiceMessage
            is SendFailed -> voiceMessage
            is Error -> voiceMessage
            else -> null
        }

    val canPlay: Boolean
        get() = this is Preview || this is ReadyToSend

    val canSend: Boolean
        get() = this is ReadyToSend

    val canDelete: Boolean
        get() = this is Preview || this is ReadyToSend || this is SendFailed
} 