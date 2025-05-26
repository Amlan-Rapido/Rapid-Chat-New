package com.rapido.chat.integration.viewmodel

import com.rapido.chat.data.repository.ChatRepository
import com.rapido.voice_recorder.VoiceRecorderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val voiceRecorderState: StateFlow<VoiceRecorderState> = chatRepository.voiceRecorderState
    
    init {
        observeChatMessages()
    }

    fun handleAction(action: ChatAction) {
        when (action) {
            is ChatAction.SendTextMessage -> sendTextMessage(action.content)
            is ChatAction.StartVoiceMessage -> startVoiceMessage()
            is ChatAction.DeleteVoiceMessage -> deleteVoiceMessage()
            is ChatAction.FinishVoiceMessage -> finishVoiceMessage()
            is ChatAction.PlayVoiceMessage -> playVoiceMessage(action.messageId)
            is ChatAction.PauseVoiceMessage -> pauseVoiceMessage(action.messageId)
            is ChatAction.ResumeVoiceMessage -> resumeVoiceMessage(action.messageId)
            is ChatAction.StopVoiceMessage -> stopVoiceMessage(action.messageId)
            is ChatAction.DeleteMessage -> deleteMessage(action.messageId)
        }
    }

    private fun observeChatMessages() {
        chatRepository.getChatMessages()
            .onEach { messages ->
                _uiState.value = ChatUiState.Success(messages)
            }
            .catch { error ->
                _uiState.value = ChatUiState.Error(error.message ?: "Unknown error occurred")
            }
            .launchIn(viewModelScope)
    }

    private fun sendTextMessage(content: String) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            try {
                chatRepository.sendTextMessage(content)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to send message: ${e.message}")
            }
        }
    }

    private fun startVoiceMessage() {
        viewModelScope.launch {
            try {
                chatRepository.startVoiceMessage()
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to start recording: ${e.message}")
            }
        }
    }

    private fun deleteVoiceMessage() {
        viewModelScope.launch {
            try {
                chatRepository.deleteVoiceMessage()
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to cancel recording: ${e.message}")
            }
        }
    }

    private fun finishVoiceMessage() {
        viewModelScope.launch {
            try {
                chatRepository.finishAndSendVoiceMessage()
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to send voice message: ${e.message}")
            }
        }
    }

    private fun playVoiceMessage(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.playVoiceMessage(messageId)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to play voice message: ${e.message}")
            }
        }
    }

    private fun pauseVoiceMessage(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.pauseVoiceMessage(messageId)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to pause voice message: ${e.message}")
            }
        }
    }

    private fun resumeVoiceMessage(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.resumeVoiceMessage(messageId)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to resume voice message: ${e.message}")
            }
        }
    }

    private fun stopVoiceMessage(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.stopVoiceMessage(messageId)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to stop voice message: ${e.message}")
            }
        }
    }

    private fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(messageId)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Failed to delete message: ${e.message}")
            }
        }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
} 