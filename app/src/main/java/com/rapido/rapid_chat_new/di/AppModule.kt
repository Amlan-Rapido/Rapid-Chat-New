package com.rapido.rapid_chat_new.di

import com.rapido.chat.data.VoiceRecorderIntegration
import com.rapido.chat.data.VoiceRecorderIntegrationImpl
import com.rapido.chat.data.repository.ChatRepository
import com.rapido.chat.data.repository.repositoryimpl.ChatRepositoryImpl
import com.rapido.chat.integration.viewmodel.ChatViewModel
import com.rapido.rapid_chat_new.presentation.AndroidChatViewModel
import com.rapido.rapid_chat_new.utils.PermissionManager
import com.rapido.voice_recorder.PlatformAudioFileManager
import com.rapido.voice_recorder.PlatformVoiceRecorder
import com.rapido.voice_recorder.VoiceRecorder
import com.rapido.voice_recorder.VoiceRecorderImpl
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Platform dependencies
    single { PlatformVoiceRecorder() }
    single { PlatformAudioFileManager() }
    
    // Voice recorder module
    single<VoiceRecorder> { VoiceRecorderImpl(get(), get()) }
    
    // Voice recorder integration
    single<VoiceRecorderIntegration> { VoiceRecorderIntegrationImpl(get()) }
    
    // Chat repository
    single<ChatRepository> { ChatRepositoryImpl(get()) }
    
    // Common ViewModel (scoped to prevent multiple instances)
    single { ChatViewModel(get()) }
    
    // Android ViewModel
    viewModel { AndroidChatViewModel(get()) }
    
    // Permission manager
    single { PermissionManager(androidContext()) }
} 