package com.rapido.voicemessagesdk.di

import com.rapido.voicemessagesdk.recording.PlatformAudioRecorder
import com.rapido.voicemessagesdk.playback.PlatformAudioPlayer
import com.rapido.voicemessagesdk.storage.PlatformAudioFileManager

actual fun createPlatformAudioRecorder(): PlatformAudioRecorder = PlatformAudioRecorder()
actual fun createPlatformAudioPlayer(): PlatformAudioPlayer = PlatformAudioPlayer()
actual fun createPlatformAudioFileManager(): PlatformAudioFileManager = PlatformAudioFileManager() 