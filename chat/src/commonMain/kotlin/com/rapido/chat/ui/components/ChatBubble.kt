package com.rapido.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rapido.chat.data.model.ChatMessage
import com.rapido.chat.data.model.MessageType
import com.rapido.chat.data.model.Sender
import com.rapido.chat.ui.utils.formatDuration
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ChatBubble(
    message: ChatMessage,
    onPlayVoice: (String) -> Unit = {},
    onPauseVoice: (String) -> Unit = {},
    isCurrentlyPlaying: Boolean = false,
    currentPlaybackPositionMs: Long = 0
) {
    when (message.sender) {
        Sender.SYSTEM -> SystemMessage(message)
        else -> RegularChatBubble(
            message = message,
            onPlayVoice = onPlayVoice,
            onPauseVoice = onPauseVoice,
            isCurrentlyPlaying = isCurrentlyPlaying,
            currentPlaybackPositionMs = currentPlaybackPositionMs
        )
    }
}

@Composable
private fun SystemMessage(message: ChatMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(vertical = 8.dp, horizontal = 16.dp)
        )
    }
}

@Composable
private fun RegularChatBubble(
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Play/Pause button
                        IconButton(
                            onClick = { 
                                if (isCurrentlyPlaying) {
                                    onPauseVoice(message.id)
                                } else {
                                    onPlayVoice(message.id)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isCurrentlyPlaying) "Pause" else "Play",
                                tint = if (isUserMessage) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Duration and progress
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatDuration(message.audioDuration ?: 0L),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isUserMessage) 
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            if (message.audioDuration != null && message.audioDuration > 0) {
                                LinearProgressIndicator(
                                    progress = if (isCurrentlyPlaying) {
                                        currentPlaybackPositionMs.toFloat() / message.audioDuration.toFloat()
                                    } else 0f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    color = if (isUserMessage) 
                                        MaterialTheme.colorScheme.onPrimary 
                                    else 
                                        MaterialTheme.colorScheme.primary,
                                    trackColor = if (isUserMessage)
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
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
                    text = formatDuration(message.timestamp),
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

