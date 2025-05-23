package com.rapido.voice_recorder

sealed class VoiceRecorderException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class AlreadyRecordingException : VoiceRecorderException("Already recording")
    class InvalidStateException(message: String) : VoiceRecorderException(message)
    class RecordingFailedException(message: String? = null, cause: Throwable? = null) : VoiceRecorderException(message ?: "Recording failed", cause)
    class PlaybackFailedException(message: String? = null, cause: Throwable? = null) : VoiceRecorderException(message ?: "Playback failed", cause)
    class FileOperationException(message: String? = null, cause: Throwable? = null) : VoiceRecorderException(message ?: "File operation failed", cause)
} 