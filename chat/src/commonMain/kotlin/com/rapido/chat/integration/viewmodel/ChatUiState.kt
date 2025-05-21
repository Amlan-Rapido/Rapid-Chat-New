package com.rapido.chat.integration.viewmodel

import com.rapido.chat.data.model.ChatMessage

sealed class ChatUiState {

    data object Loading : ChatUiState()
    data class Success(val messages: List<ChatMessage>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()

} 