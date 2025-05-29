package com.rapido.chat.integration.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.rapido.chat.integration.viewmodel.ChatAction
import com.rapido.chat.integration.viewmodel.ChatUiState
import com.rapido.chat.integration.viewmodel.ChatViewModelInterface
import com.rapido.chat.ui.components.ChatInputBar
import com.rapido.chat.ui.components.MessageList
import com.rapido.voicemessagesdk.core.VoiceRecorderState
import com.rapido.voicemessagesdk.ui.VoiceMessageData

@Composable
fun ChatScreen(
    viewModel: ChatViewModelInterface
) {
    val tag = "ChatScreen"
    val uiState by viewModel.uiState.collectAsState()
    val voiceRecorderState by viewModel.voiceRecorderState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    
    // Track currently playing message
    var currentlyPlayingMessageId by remember { mutableStateOf<String?>(null) }

    // Update currently playing message when voice recorder state changes
    LaunchedEffect(voiceRecorderState) {
        when (val state = voiceRecorderState) {
            is VoiceRecorderState.Preview -> {
                if (!state.playing) {
                    currentlyPlayingMessageId = null
                }
            }
            is VoiceRecorderState.Idle -> {
                currentlyPlayingMessageId = null
            }
            else -> {}
        }
    }
    
    // Log voice recorder state changes
    LaunchedEffect(voiceRecorderState) {
        when (val state = voiceRecorderState) {
            is VoiceRecorderState.Recording -> {
                val duration = state.durationMs
                platformLogD(tag, "Recording in progress: $duration ms")
            }
            is VoiceRecorderState.Idle -> {
                platformLogD(tag, "Voice recorder idle")
            }
            is VoiceRecorderState.Preview -> {
                if (state.playing) {
                    platformLogD(tag, "Playing voice message")
                } else {
                    platformLogD(tag, "Voice playback paused")
                }
            }
            is VoiceRecorderState.RecordingCompleted -> {
                platformLogD(tag, "Recording completed")
            }
            is VoiceRecorderState.ReadyToSend -> {
                platformLogD(tag, "Voice message ready to send")
            }
            is VoiceRecorderState.Sending -> {
                platformLogD(tag, "Sending voice message...")
            }
            is VoiceRecorderState.Sent -> {
                platformLogD(tag, "Voice message sent successfully")
            }
            is VoiceRecorderState.SendFailed -> {
                platformLogD(tag, "Voice message send failed: ${state.error.message}")
            }
            is VoiceRecorderState.Error -> {
                platformLogD(tag, "Voice recorder error: ${state.exception.message}")
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChanged = { newText -> 
                    inputText = newText 
                },
                onSendTextMessage = {
                    if (inputText.isNotBlank()) {
                        platformLogD(tag, "Sending text message: $inputText")
                        viewModel.handleAction(ChatAction.SendTextMessage(inputText))
                        inputText = ""
                    }
                },
                onSendVoiceMessage = { voiceMessageData: VoiceMessageData ->
                    platformLogD(tag, "Sending voice message: ${voiceMessageData.durationMs}ms")
                    // Create a chat message from the voice message data using the new simplified action
                    viewModel.handleAction(ChatAction.SendVoiceMessageData(voiceMessageData))
                },
                onVoiceMessageError = { error ->
                    platformLogD(tag, "Voice message error: $error")
                    // TODO: Show error to user (snackbar, toast, etc.)
                },
                voiceMessageManager = viewModel.voiceMessageManager
            )
        }
    ) { paddingValues ->
        when (val currentState = uiState) {
            is ChatUiState.Success -> {
                MessageList(
                    messages = currentState.messages,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onVoiceMessageClick = { messageId ->
                        if (currentlyPlayingMessageId == messageId) {
                            // If clicking the same message that's playing, pause it
                            viewModel.handleAction(ChatAction.PauseVoiceMessage)
                            currentlyPlayingMessageId = null
                        } else {
                            // Start playing the clicked message
                            currentlyPlayingMessageId = messageId
                            viewModel.handleAction(ChatAction.PlayVoiceMessage.FromMessage(messageId))
                        }
                    },
                    voiceRecorderState = voiceRecorderState,
                    currentlyPlayingMessageId = currentlyPlayingMessageId
                )
            }
            is ChatUiState.Error -> {
                // Show error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${currentState.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is ChatUiState.Loading -> {
                // Show loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}