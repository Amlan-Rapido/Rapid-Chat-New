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

expect fun platformLogD(tag: String, message: String)

@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {
    val tag = "ChatScreen"
    val uiState by viewModel.uiState.collectAsState()
    val voiceRecorderState by viewModel.voiceRecorderState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val isRecording = voiceRecorderState is VoiceRecorderState.Recording
    
    // Log voice recorder state changes
    LaunchedEffect(voiceRecorderState) {
        when (voiceRecorderState) {
            is VoiceRecorderState.Recording -> {
                val duration = (voiceRecorderState as VoiceRecorderState.Recording).durationMs
                platformLogD(tag, "Recording in progress: $duration ms")
            }
            is VoiceRecorderState.Idle -> {
                platformLogD(tag, "Voice recorder idle")
            }
            is VoiceRecorderState.Preview -> {
                val previewState = voiceRecorderState as VoiceRecorderState.Preview
                if (previewState.playing) {
                    platformLogD(tag, "Playing voice message")
                } else {
                    platformLogD(tag, "Voice playback paused")
                }
            }
            is VoiceRecorderState.Error -> {
                val errorState = voiceRecorderState as VoiceRecorderState.Error
                platformLogD(tag, "Voice recorder error: ${errorState.exception.message}")
            }
        }
    }

    val currentPlayingMessageId = when (val state = voiceRecorderState) {
        is VoiceRecorderState.Preview -> {
            if (state.playing) {
                when (val currentUiState = uiState) {
                    is ChatUiState.Success -> {
                        currentUiState.messages.find { message ->
                            message.audioUrl == state.audio.filePath
                        }?.id
                    }
                    else -> null
                }
            } else null
        }
        else -> null
    }

    val currentPlaybackPosition = when (val state = voiceRecorderState) {
        is VoiceRecorderState.Preview -> state.currentPositionMs
        else -> 0L
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            Column {
                if (isRecording) {
                    val recordingDuration = (voiceRecorderState as? VoiceRecorderState.Recording)?.durationMs ?: 0L
                    VoiceRecordingIndicator(
                        durationMs = recordingDuration,
                        onDeleteClick = {
                            platformLogD(tag, "Deleting voice recording")
                            viewModel.handleAction(ChatAction.DeleteVoiceMessage)
                        }
                    )
                }
                ChatInputBar(
                    text = inputText,
                    onTextChanged = { newText -> inputText = newText },
                    onSendClick = {
                        if (inputText.isNotBlank()) {
                            platformLogD(tag, "Sending text message: $inputText")
                            viewModel.handleAction(ChatAction.SendTextMessage(inputText))
                            inputText = ""
                        }
                    },
                    isRecording = isRecording,
                    onVoiceRecordStart = {
                        platformLogD(tag, "Starting voice recording")
                        viewModel.handleAction(ChatAction.StartVoiceMessage)
                    },
                    onVoiceRecordEnd = {
                        platformLogD(tag, "Finishing voice recording")
                        viewModel.handleAction(ChatAction.FinishVoiceMessage)
                    },
                    onVoiceRecordDelete = {
                        platformLogD(tag, "Deleting voice recording")
                        viewModel.handleAction(ChatAction.DeleteVoiceMessage)
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
                platformLogD(tag, "Playing voice message: $messageId")
                viewModel.handleAction(ChatAction.PlayVoiceMessage(messageId))
            },
            onPauseVoice = { messageId ->
                platformLogD(tag, "Pausing voice message: $messageId")
                viewModel.handleAction(ChatAction.PauseVoiceMessage(messageId))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}