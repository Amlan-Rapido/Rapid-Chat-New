package com.rapido.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rapido.chat.data.model.ChatMessage
import com.rapido.chat.data.model.MessageType
import com.rapido.chat.data.model.Sender
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * A chat bubble component that displays a message.
 *
 * @param message The chat message to display
 * @param onPlayVoice Callback when the play button is clicked for a voice message
 * @param onPauseVoice Callback when the pause button is clicked for a voice message
 * @param isCurrentlyPlaying Whether this voice message is currently playing
 * @param currentPlaybackPositionMs Current playback position in milliseconds (for voice messages)
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    onPlayVoice: (String) -> Unit = {},
    onPauseVoice: (String) -> Unit = {},
    isCurrentlyPlaying: Boolean = false,
    currentPlaybackPositionMs: Long = 0
) {
    val isUserMessage = message.sender == Sender.USER
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUserMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUserMessage) 8.dp else 0.dp,
                        topEnd = if (isUserMessage) 0.dp else 8.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp
                    )
                )
                .background(
                    if (isUserMessage) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            when (message.type) {
                MessageType.TEXT -> {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUserMessage) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MessageType.VOICE -> {
                    AudioPlaybackControl(
                        messageId = message.id,
                        audioDuration = message.audioDuration ?: 0L,
                        isPlaying = isCurrentlyPlaying,
                        currentPosition = currentPlaybackPositionMs,
                        onPlayClick = { onPlayVoice(message.id) },
                        onPauseClick = { onPauseVoice(message.id) }
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUserMessage) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUserMessage) 
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = if (isUserMessage) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                if (!isUserMessage) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    // Using kotlinx-datetime for better cross-platform compatibility
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    
    return "$hour:$minute"
}