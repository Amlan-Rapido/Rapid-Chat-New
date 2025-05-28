package com.rapido.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rapido.chat.data.model.ChatMessage
import com.rapido.voicemessagesdk.core.VoiceRecorderState

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    onVoiceMessageClick: (String) -> Unit = {},
    voiceRecorderState: VoiceRecorderState = VoiceRecorderState.Idle,
    currentlyPlayingMessageId: String? = null
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = messages.reversed(),
            key = { message -> message.id }
        ) { message ->
            val isPlaying = currentlyPlayingMessageId == message.id
            val currentPosition = if (isPlaying && voiceRecorderState is VoiceRecorderState.Preview) {
                (voiceRecorderState as VoiceRecorderState.Preview).currentPositionMs
            } else 0L

            ChatBubble(
                message = message,
                onPlayVoice = onVoiceMessageClick,
                onPauseVoice = { /* Handle pause */ },
                isCurrentlyPlaying = isPlaying,
                currentPlaybackPositionMs = currentPosition
            )
        }
    }
} 