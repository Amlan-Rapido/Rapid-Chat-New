package com.rapido.rapid_chat_new.presentation

import androidx.lifecycle.ViewModel
import com.rapido.chat.integration.viewmodel.ChatViewModel
import com.rapido.chat.integration.viewmodel.ChatViewModelInterface

class AndroidChatViewModel(
    private val chatViewModel: ChatViewModel
) : ViewModel(), ChatViewModelInterface by chatViewModel {
    
    override fun onCleared() {
        super.onCleared()
        chatViewModel.onCleared()
    }
} 