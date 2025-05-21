package com.rapido.chat.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A control for playing and pausing audio recordings.
 *
 * @param messageId ID of the message containing this audio
 * @param audioDuration Duration of the audio in milliseconds
 * @param isPlaying Whether the audio is currently playing
 * @param currentPosition Current playback position in milliseconds
 * @param onPlayClick Callback when the play button is clicked
 * @param onPauseClick Callback when the pause button is clicked
 */
@Composable
fun AudioPlaybackControl(
    messageId: String,
    audioDuration: Long,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(200.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause button
            IconButton(
                onClick = { if (isPlaying) onPauseClick() else onPlayClick() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Column {
                // Progress bar
                LinearProgressIndicator(
                    progress = if (audioDuration > 0) currentPosition.toFloat() / audioDuration.toFloat() else 0f,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Duration text
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = MaterialTheme.typography.labelSmall
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = formatDuration(audioDuration),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}
