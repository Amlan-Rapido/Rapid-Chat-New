package com.rapido.chat.integration.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rapido.chat.integration.viewmodel.ChatAction
import com.rapido.chat.integration.viewmodel.ChatViewModel
import com.rapido.chat.ui.components.ChatInputBar
import com.rapido.chat.ui.components.MessageList
import com.rapido.chat.ui.components.VoiceRecordingIndicator
import com.rapido.voice_recorder.VoiceRecorderState

/**
 * The main chat screen composable.
 *
 * @param viewModel The ViewModel for this screen
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val voiceRecorderState by viewModel.voiceRecorderState.collectAsState()
    
    // Track input text state
    var inputText by remember { mutableStateOf("") }
    
    // Track playback state
    val isRecording = voiceRecorderState is VoiceRecorderState.Recording
    val currentPlayingMessage = when (val state = voiceRecorderState) {
        is VoiceRecorderState.Playing -> state.audio.filePath
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Messages list
                MessageList(
                    uiState = uiState,
                    currentPlayingMessageId = currentPlayingMessage,
                    currentPlaybackPosition = currentPlaybackPosition,
                    onPlayVoice = { messageId ->
                        viewModel.handleAction(ChatAction.PlayVoiceMessage(messageId))
                    },
                    onPauseVoice = { messageId ->
                        viewModel.handleAction(ChatAction.PauseVoiceMessage(messageId))
                    },
                    modifier = Modifier.weight(1f)
                )
                
                // Recording indicator (shown when recording)
                if (isRecording) {
                    val recordingDuration = (voiceRecorderState as? VoiceRecorderState.Recording)?.durationMs ?: 0L
                    
                    VoiceRecordingIndicator(
                        durationMs = recordingDuration,
                        onCancelClick = {
                            viewModel.handleAction(ChatAction.CancelVoiceMessage)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Input bar
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
    }
} 