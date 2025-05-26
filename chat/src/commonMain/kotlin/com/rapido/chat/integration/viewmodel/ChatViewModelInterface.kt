package com.rapido.chat.integration.viewmodel

import com.rapido.voice_recorder.VoiceRecorderState
import kotlinx.coroutines.flow.StateFlow

interface ChatViewModelInterface {
    val uiState: StateFlow<ChatUiState>
    val voiceRecorderState: StateFlow<VoiceRecorderState>
    
    fun handleAction(action: ChatAction)
    fun onCleared()
} 