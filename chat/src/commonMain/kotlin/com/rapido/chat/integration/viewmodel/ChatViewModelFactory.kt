package com.rapido.chat.integration.viewmodel

import com.rapido.chat.data.repository.ChatRepository

class ChatViewModelFactory(
    private val chatRepository: ChatRepository
) {
    fun create(): ChatViewModel = ChatViewModel(chatRepository)
} 