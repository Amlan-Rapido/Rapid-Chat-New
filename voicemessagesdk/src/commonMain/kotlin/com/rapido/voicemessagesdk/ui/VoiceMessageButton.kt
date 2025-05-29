package com.rapido.voicemessagesdk.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.rapido.voicemessagesdk.core.VoiceMessage
import com.rapido.voicemessagesdk.core.VoiceMessageManager
import com.rapido.voicemessagesdk.core.VoiceRecorderState
import kotlinx.coroutines.launch

/**
 * Data class representing a completed voice message ready to be sent
 */
data class VoiceMessageData(
    val voiceMessage: VoiceMessage,
    val durationMs: Long,
    val filePath: String
)

/**
 * Callback interface for VoiceMessageButton events
 */
interface VoiceMessageButtonCallbacks {
    /**
     * Called when a voice message is ready to be sent
     */
    fun onVoiceMessageReady(data: VoiceMessageData)
    
    /**
     * Called when an error occurs
     */
    fun onError(error: String)
}

/**
 * Self-contained voice message button that handles the entire voice message flow.
 * This composable manages recording, preview, and sending of voice messages internally.
 * 
 * @param voiceMessageManager The voice message manager instance
 * @param callbacks Callbacks for voice message events
 * @param modifier Modifier for the button
 * @param isEnabled Whether the button is enabled
 */
@Composable
fun VoiceMessageButton(
    voiceMessageManager: VoiceMessageManager,
    callbacks: VoiceMessageButtonCallbacks,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    // Observe the voice recorder state
    val voiceRecorderState by voiceMessageManager.state.collectAsState()
    
    // Determine current state and extract relevant data
    val isRecording = voiceRecorderState is VoiceRecorderState.Recording
    val isPreview = voiceRecorderState is VoiceRecorderState.Preview || 
                   voiceRecorderState is VoiceRecorderState.RecordingCompleted ||
                   voiceRecorderState is VoiceRecorderState.ReadyToSend
    
    val currentVoiceMessage = voiceRecorderState.currentVoiceMessage
    val isPlaying = voiceRecorderState.isPlaying
    
    // Calculate progress for playback
    val progress = if (voiceRecorderState is VoiceRecorderState.Preview && currentVoiceMessage != null) {
        if (currentVoiceMessage.durationMs > 0) {
            val previewState = voiceRecorderState as VoiceRecorderState.Preview
            previewState.currentPositionMs.toFloat() / currentVoiceMessage.durationMs.toFloat()
        } else 0f
    } else 0f
    
    // Handle errors
    LaunchedEffect(voiceRecorderState) {
        if (voiceRecorderState is VoiceRecorderState.Error) {
            val errorState = voiceRecorderState as VoiceRecorderState.Error
            callbacks.onError(errorState.exception.message ?: "Unknown error")
        }
    }
    
    // Auto-transition to preview mode when recording completes
    LaunchedEffect(voiceRecorderState) {
        if (voiceRecorderState is VoiceRecorderState.RecordingCompleted) {
            val completedState = voiceRecorderState as VoiceRecorderState.RecordingCompleted
            voiceMessageManager.enterPreviewMode(completedState.voiceMessage)
        }
    }
    
    // Show different UI based on state
    when {
        isRecording -> {
            val recordingState = voiceRecorderState as VoiceRecorderState.Recording
            RecordingIndicator(
                durationMs = recordingState.durationMs,
                onStop = {
                    scope.launch {
                        try {
                            voiceMessageManager.stopRecording()
                        } catch (e: Exception) {
                            callbacks.onError(e.message ?: "Failed to stop recording")
                        }
                    }
                },
                onCancel = {
                    scope.launch {
                        try {
                            voiceMessageManager.cancelRecording()
                        } catch (e: Exception) {
                            callbacks.onError(e.message ?: "Failed to cancel recording")
                        }
                    }
                },
                modifier = modifier
            )
        }
        
        isPreview && currentVoiceMessage != null -> {
            PreviewControls(
                voiceMessage = currentVoiceMessage,
                isPlaying = isPlaying,
                progress = progress,
                onPlayPause = {
                    scope.launch {
                        try {
                            if (isPlaying) {
                                voiceMessageManager.pausePreview()
                            } else {
                                voiceMessageManager.startPreview(currentVoiceMessage)
                            }
                        } catch (e: Exception) {
                            callbacks.onError(e.message ?: "Playback error")
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        try {
                            voiceMessageManager.deleteVoiceMessage(currentVoiceMessage)
                        } catch (e: Exception) {
                            callbacks.onError(e.message ?: "Failed to delete")
                        }
                    }
                },
                onSend = {
                    scope.launch {
                        try {
                            // Mark as ready to send and notify callback
                            voiceMessageManager.markReadyToSend(currentVoiceMessage)
                            val data = VoiceMessageData(
                                voiceMessage = currentVoiceMessage,
                                durationMs = currentVoiceMessage.durationMs,
                                filePath = currentVoiceMessage.filePath
                            )
                            callbacks.onVoiceMessageReady(data)
                            // Transition to idle without deleting the file - file ownership transfers to chat system
                            voiceMessageManager.transitionToIdle()
                        } catch (e: Exception) {
                            callbacks.onError(e.message ?: "Failed to send")
                        }
                    }
                },
                modifier = modifier
            )
        }
        
        else -> {
            // Default mic button
            MicButton(
                onClick = {
                    if (isEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            try {
                                voiceMessageManager.startRecording()
                            } catch (e: Exception) {
                                callbacks.onError(e.message ?: "Failed to start recording")
                            }
                        }
                    }
                },
                isEnabled = isEnabled,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun MicButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = if (isEnabled) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Record voice message",
            tint = if (isEnabled) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun RecordingIndicator(
    durationMs: Long,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1.1f,
        animationSpec = tween(durationMillis = 1000),
        label = "recording_pulse"
    )
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel button
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel recording",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            // Recording indicator
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(scale)
                        .background(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            // Stop button
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop recording",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

@Composable
private fun PreviewControls(
    voiceMessage: VoiceMessage,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete recording",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            // Duration and progress
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatDuration(voiceMessage.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            
            // Play/Pause button
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Send button
            AnimatedVisibility(
                visible = true,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send voice message",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    return if (minutes > 0) {
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    } else {
        "0:${seconds.toString().padStart(2, '0')}"
    }
} 