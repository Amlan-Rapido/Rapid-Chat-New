package com.rapido.voice_recorder.di

import org.koin.core.module.Module
import org.koin.dsl.module
import com.rapido.voice_recorder.PlatformAudioFileManager
import com.rapido.voice_recorder.PlatformVoiceRecorder
import com.rapido.voice_recorder.VoiceRecorder
import com.rapido.voice_recorder.VoiceRecorderImpl

expect fun createPlatformVoiceRecorder(): PlatformVoiceRecorder
expect fun createAudioFileManager(): PlatformAudioFileManager

fun voiceRecorderModule(): Module = module {
    single<PlatformAudioFileManager> { createAudioFileManager() }
    single<PlatformVoiceRecorder> { createPlatformVoiceRecorder() }
    single<VoiceRecorder> { VoiceRecorderImpl(get(), get()) }
} 