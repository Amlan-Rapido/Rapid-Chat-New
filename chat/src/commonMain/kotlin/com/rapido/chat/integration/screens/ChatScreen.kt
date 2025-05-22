package com.rapido.chat.integration.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rapido.chat.integration.viewmodel.ChatAction
import com.rapido.chat.integration.viewmodel.ChatUiState
import com.rapido.chat.integration.viewmodel.ChatViewModel
import com.rapido.chat.ui.components.ChatInputBar
import com.rapido.chat.ui.components.MessageList
import com.rapido.chat.ui.components.VoiceRecordingIndicator
import com.rapido.voice_recorder.VoiceRecorderState



@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceRecorderState by viewModel.voiceRecorderState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val isRecording = voiceRecorderState is VoiceRecorderState.Recording

    val currentPlayingMessageId = when (val state = voiceRecorderState) {
        is VoiceRecorderState.Playing -> {
            when (val currentUiState = uiState) {
                is ChatUiState.Success -> {
                    currentUiState.messages.find { message ->
                        message.audioUrl == state.audio.filePath
                    }?.id
                }
                else -> null
            }
        }
        else -> null
    }

    val currentPlaybackPosition = when (val state = voiceRecorderState) {
        is VoiceRecorderState.Playing -> state.positionMs
        is VoiceRecorderState.Paused -> state.positionMs
        else -> 0L
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            // Column to stack RecordingIndicator above ChatInputBar if needed
            Column {
                if (isRecording) {
                    val recordingDuration = (voiceRecorderState as? VoiceRecorderState.Recording)?.durationMs ?: 0L
                    VoiceRecordingIndicator(
                        durationMs = recordingDuration,
                        onCancelClick = {
                            viewModel.handleAction(ChatAction.CancelVoiceMessage)
                        }
                    )
                }
                ChatInputBar(
                    text = inputText,
                    onTextChanged = { newText -> inputText = newText },
                    onSendClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.handleAction(ChatAction.SendTextMessage(inputText))
                            inputText = ""
                        }
                    },
                    isRecording = isRecording,
                    onVoiceRecordStart = {
                        viewModel.handleAction(ChatAction.StartVoiceMessage)
                    },
                    onVoiceRecordEnd = {
                        viewModel.handleAction(ChatAction.FinishVoiceMessage)
                    },
                    onVoiceRecordCancel = {
                        viewModel.handleAction(ChatAction.CancelVoiceMessage)
                    }
                )
            }
        }
    ) { paddingValues ->
        MessageList(
            uiState = uiState,
            currentPlayingMessageId = currentPlayingMessageId,
            currentPlaybackPosition = currentPlaybackPosition,
            onPlayVoice = { messageId ->
                viewModel.handleAction(ChatAction.PlayVoiceMessage(messageId))
            },
            onPauseVoice = { messageId ->
                viewModel.handleAction(ChatAction.PauseVoiceMessage(messageId))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}