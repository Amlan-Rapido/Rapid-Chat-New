package com.rapido.chat.integration.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rapido.chat.integration.viewmodel.ChatAction
import com.rapido.chat.integration.viewmodel.ChatUiState
import com.rapido.chat.integration.viewmodel.ChatViewModelInterface
import com.rapido.chat.ui.components.ChatInputBar
import com.rapido.chat.ui.components.MessageList
import com.rapido.chat.ui.components.VoiceRecordingIndicator
import com.rapido.voice_recorder.VoiceRecorderState

@Composable
fun ChatScreen(
    viewModel: ChatViewModelInterface
) {
    val tag = "ChatScreen"
    val uiState by viewModel.uiState.collectAsState()
    val voiceRecorderState by viewModel.voiceRecorderState.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val isRecording = voiceRecorderState is VoiceRecorderState.Recording
    val isPreview = voiceRecorderState is VoiceRecorderState.Preview
    
    // Calculate preview state values
    val previewState = voiceRecorderState as? VoiceRecorderState.Preview
    val isPlaying = previewState?.playing ?: false
    val playbackProgress = if (previewState != null && previewState.audio.durationMs > 0) {
        previewState.currentPositionMs.toFloat() / previewState.audio.durationMs.toFloat()
    } else 0f

    // Track currently playing message
    var currentlyPlayingMessageId by remember { mutableStateOf<String?>(null) }

    // Update currently playing message when voice recorder state changes
    LaunchedEffect(voiceRecorderState) {
        when (voiceRecorderState) {
            is VoiceRecorderState.Preview -> {
                if (!(voiceRecorderState as VoiceRecorderState.Preview).playing) {
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            Column {
                // Show voice recording indicator when recording or in preview
                if (isRecording || isPreview) {
                    val recordingDuration = when (voiceRecorderState) {
                        is VoiceRecorderState.Recording -> (voiceRecorderState as VoiceRecorderState.Recording).durationMs
                        is VoiceRecorderState.Preview -> (voiceRecorderState as VoiceRecorderState.Preview).audio.durationMs
                        else -> 0L
                    }
                    
                    VoiceRecordingIndicator(
                        durationMs = recordingDuration,
                        isRecording = isRecording,
                        isPlaying = isPlaying,
                        progress = playbackProgress,
                        onStopRecording = {
                            platformLogD(tag, "Stopping voice recording")
                            viewModel.handleAction(ChatAction.FinishVoiceMessage)
                        },
                        onPlayPauseClick = {
                            if (isPlaying) {
                                viewModel.handleAction(ChatAction.PauseVoiceMessage)
                            } else {
                                previewState?.audio?.let { audio ->
                                    viewModel.handleAction(ChatAction.PlayVoiceMessage.FromPreview(audio))
                                }
                            }
                        },
                        onDeleteClick = {
                            viewModel.handleAction(ChatAction.DeleteVoiceMessage)
                        },
                        onSendClick = {
                            viewModel.handleAction(ChatAction.SendVoiceMessage)
                            // Clear preview state after sending
                            viewModel.handleAction(ChatAction.StopVoiceMessage)
                        }
                    )
                }
                
                ChatInputBar(
                    text = inputText,
                    onTextChanged = { newText -> 
                        // Only allow text input when not recording/previewing voice
                        if (!isRecording && !isPreview) {
                            inputText = newText 
                        }
                    },
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
                    }
                )
            }
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