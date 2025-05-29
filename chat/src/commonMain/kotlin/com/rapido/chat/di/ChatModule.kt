package com.rapido.chat.di

import com.rapido.chat.data.repository.ChatRepository
import com.rapido.chat.data.repository.repositoryimpl.ChatRepositoryImpl
import com.rapido.chat.integration.viewmodel.ChatViewModel
import com.rapido.chat.integration.viewmodel.ChatViewModelInterface
import com.rapido.voicemessagesdk.di.voiceRecorderModule
import org.koin.core.module.Module
import org.koin.dsl.module


fun chatModule(): Module = module {
    // Include voice recorder module
    includes(voiceRecorderModule())
    
    // Chat repository - now uses VoiceRecorder and VoiceMessageManager directly
    single<ChatRepository> { ChatRepositoryImpl(get(), get()) }

    // ViewModel - provides ChatViewModel when ChatViewModelInterface is requested
    factory<ChatViewModelInterface> { ChatViewModel(get()) }
} 