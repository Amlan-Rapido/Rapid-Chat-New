package com.rapido.voice_recorder

object VoiceRecorderFactory {
    fun create(): VoiceRecorder {
        return VoiceRecorderImpl(PlatformVoiceRecorder())
    }
}