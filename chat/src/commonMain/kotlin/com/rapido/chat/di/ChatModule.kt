package com.rapido.chat.di

import com.rapido.chat.data.VoiceRecorderIntegration
import com.rapido.chat.data.VoiceRecorderIntegrationImpl
import com.rapido.chat.data.repository.ChatRepository
import com.rapido.chat.data.repository.repositoryimpl.ChatRepositoryImpl
import com.rapido.chat.integration.viewmodel.ChatViewModel
import com.rapido.chat.integration.viewmodel.ChatViewModelInterface
import com.rapido.voice_recorder.di.voiceRecorderModule
import org.koin.core.module.Module
import org.koin.dsl.module


fun chatModule(): Module = module {
    // Include voice recorder module
    includes(voiceRecorderModule())
    
    // Voice recorder integration
    single<VoiceRecorderIntegration> { VoiceRecorderIntegrationImpl(get()) }
    
    // Chat repository
    single<ChatRepository> { ChatRepositoryImpl(get()) }

    // ViewModel - provides ChatViewModel when ChatViewModelInterface is requested
    factory<ChatViewModelInterface> { ChatViewModel(get()) }
} 