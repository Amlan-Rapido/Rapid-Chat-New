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
) : ChatViewModelInterface {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    override val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    override val voiceRecorderState: StateFlow<VoiceRecorderState> = chatRepository.voiceRecorderState
    
    init {
        viewModelScope.launch {
            chatRepository.messages
                .onEach { messages ->
                    _uiState.value = ChatUiState.Success(messages)
                }
                .catch { error ->
                    _uiState.value = ChatUiState.Error(error.message ?: "Unknown error")
                }
                .launchIn(viewModelScope)
        }
    }

    override fun handleAction(action: ChatAction) {
        when (action) {
            is ChatAction.SendTextMessage -> {
                viewModelScope.launch {
                    chatRepository.sendTextMessage(action.text)
                }
            }
            is ChatAction.StartVoiceMessage -> {
                viewModelScope.launch {
                    chatRepository.startVoiceRecording()
                }
            }
            is ChatAction.FinishVoiceMessage -> {
                viewModelScope.launch {
                    chatRepository.finishVoiceRecording()
                }
            }
            is ChatAction.DeleteVoiceMessage -> {
                viewModelScope.launch {
                    chatRepository.deleteCurrentVoiceRecording()
                }
            }
            is ChatAction.SendVoiceMessage -> {
                viewModelScope.launch {
                    chatRepository.finishAndSendVoiceMessage()
                }
            }
            is ChatAction.PlayVoiceMessage.FromMessage -> {
                viewModelScope.launch {
                    chatRepository.playVoiceMessage(action.messageId)
                }
            }
            is ChatAction.PlayVoiceMessage.FromPreview -> {
                viewModelScope.launch {
                    chatRepository.playVoiceRecording(action.audio)
                }
            }
            is ChatAction.PauseVoiceMessage -> {
                viewModelScope.launch {
                    chatRepository.pauseVoicePlayback()
                }
            }
            is ChatAction.ResumeVoiceMessage -> {
                viewModelScope.launch {
                    chatRepository.resumeVoicePlayback()
                }
            }
            is ChatAction.StopVoiceMessage -> {
                viewModelScope.launch {
                    chatRepository.stopVoicePlayback()
                }
            }
            is ChatAction.DeleteMessage -> {
                viewModelScope.launch {
                    chatRepository.deleteMessage(action.messageId)
                }
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
} 