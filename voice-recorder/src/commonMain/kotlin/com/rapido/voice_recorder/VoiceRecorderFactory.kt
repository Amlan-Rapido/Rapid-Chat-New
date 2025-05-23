package com.rapido.voice_recorder

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Factory for creating VoiceRecorder instances.
 * Uses Koin for dependency injection.
 */
object VoiceRecorderFactory : KoinComponent {
    private val voiceRecorder: VoiceRecorder by inject()
    fun create(): VoiceRecorder = voiceRecorder
}