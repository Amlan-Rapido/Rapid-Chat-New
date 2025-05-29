package com.rapido.voicemessagesdk.di

import org.koin.core.module.Module
import org.koin.dsl.module
import com.rapido.voicemessagesdk.core.VoiceRecorder
import com.rapido.voicemessagesdk.core.VoiceRecorderImpl
import com.rapido.voicemessagesdk.core.VoiceMessageManager
import com.rapido.voicemessagesdk.core.VoiceMessageManagerImpl
import com.rapido.voicemessagesdk.recording.AudioRecorder
import com.rapido.voicemessagesdk.recording.PlatformAudioRecorder
import com.rapido.voicemessagesdk.playback.AudioPlayer
import com.rapido.voicemessagesdk.playback.PlatformAudioPlayer
import com.rapido.voicemessagesdk.storage.AudioFileManager
import com.rapido.voicemessagesdk.storage.PlatformAudioFileManager

expect fun createPlatformAudioRecorder(): PlatformAudioRecorder
expect fun createPlatformAudioPlayer(): PlatformAudioPlayer
expect fun createPlatformAudioFileManager(): PlatformAudioFileManager

fun voiceRecorderModule(): Module = module {
    single<AudioFileManager> { createPlatformAudioFileManager() }
    single<AudioRecorder> { createPlatformAudioRecorder() }
    single<AudioPlayer> { createPlatformAudioPlayer() }
    single<VoiceRecorder> { VoiceRecorderImpl(get(), get(), get()) }
    single<VoiceMessageManager> { VoiceMessageManagerImpl(get()) }
} 