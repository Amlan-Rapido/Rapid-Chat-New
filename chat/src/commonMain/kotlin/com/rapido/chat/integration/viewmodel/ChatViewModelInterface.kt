package com.rapido.chat.integration.viewmodel

import com.rapido.voicemessagesdk.core.VoiceMessageManager
import com.rapido.voicemessagesdk.core.VoiceRecorderState
import kotlinx.coroutines.flow.StateFlow

interface ChatViewModelInterface {
    val uiState: StateFlow<ChatUiState>
    val voiceRecorderState: StateFlow<VoiceRecorderState>
    val voiceMessageManager: VoiceMessageManager
    
    fun handleAction(action: ChatAction)
    fun onCleared()
} 