package com.rapido.rapid_chat_new.di

import com.rapido.chat.data.VoiceRecorderIntegration
import com.rapido.chat.data.VoiceRecorderIntegrationImpl
import com.rapido.chat.data.repository.ChatRepository
import com.rapido.chat.data.repository.repositoryimpl.ChatRepositoryImpl
import com.rapido.chat.integration.viewmodel.ChatViewModel
import com.rapido.rapid_chat_new.presentation.AndroidChatViewModel
import com.rapido.rapid_chat_new.utils.PermissionManager
import com.rapido.voicemessagesdk.di.voiceRecorderModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Include the voice recorder module from voicemessagesdk
    includes(voiceRecorderModule())
    
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