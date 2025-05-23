package com.rapido.voice_recorder.di

import com.rapido.voice_recorder.PlatformAudioFileManager
import com.rapido.voice_recorder.PlatformVoiceRecorder

actual fun createPlatformVoiceRecorder(): PlatformVoiceRecorder = PlatformVoiceRecorder()
actual fun createAudioFileManager(): PlatformAudioFileManager = PlatformAudioFileManager()