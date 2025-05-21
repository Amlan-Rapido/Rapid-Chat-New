package com.rapido.chat.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rapido.chat.data.model.ChatMessage
import com.rapido.chat.integration.viewmodel.ChatUiState

/**
 * A list of chat messages.
 *
 * @param uiState The current UI state
 * @param currentPlayingMessageId ID of the message currently being played (if any)
 * @param currentPlaybackPosition Current playback position in milliseconds (for voice messages)
 * @param onPlayVoice Callback when a voice message play button is clicked
 * @param onPauseVoice Callback when a voice message pause button is clicked
 * @param contentPadding Padding for the list content
 */
@Composable
fun MessageList(
    uiState: ChatUiState,
    currentPlayingMessageId: String? = null,
    currentPlaybackPosition: Long = 0,
    onPlayVoice: (String) -> Unit = {},
    onPauseVoice: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is ChatUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            is ChatUiState.Error -> {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center)
                )
            }
            
            is ChatUiState.Success -> {
                val listState = rememberLazyListState()
                val messages = uiState.messages
                
                // Automatically scroll to bottom when new messages are added
                val shouldScrollToBottom by remember {
                    derivedStateOf {
                        messages.isNotEmpty()
                    }
                }
                
                LaunchedEffect(shouldScrollToBottom, messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    reverseLayout = false
                ) {
                    // Add items in chronological order
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        ChatBubble(
                            message = message,
                            onPlayVoice = onPlayVoice,
                            onPauseVoice = onPauseVoice,
                            isCurrentlyPlaying = message.id == currentPlayingMessageId,
                            currentPlaybackPositionMs = if (message.id == currentPlayingMessageId) 
                                currentPlaybackPosition else 0
                        )
                    }
                }
            }
        }
    }
} 