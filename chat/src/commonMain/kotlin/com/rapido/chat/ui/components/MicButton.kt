package com.rapido.chat.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * A microphone button for recording voice messages.
 *
 * @param isRecording Whether recording is currently in progress
 * @param onLongPress Callback when the button is long-pressed, used to start recording
 * @param onLongPressRelease Callback when the button is released after a long press, used to finish recording
 * @param onTap Callback when the button is tapped (not long-pressed)
 */
@Composable
fun MicButton(
    isRecording: Boolean,
    onLongPress: () -> Unit,
    onLongPressRelease: () -> Unit,
    onTap: () -> Unit = {}
) {
    // Animation for the button scale when recording
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.2f else 1f, 
        label = "MicButtonScale"
    )
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isRecording) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primaryContainer
            )
            .border(
                width = if (isRecording) 2.dp else 0.dp,
                color = if (isRecording) MaterialTheme.colorScheme.error else Color.Transparent,
                shape = CircleShape
            )
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() },
                    onPress = { awaitRelease(); if (isRecording) onLongPressRelease() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = if (isRecording) "Release to finish recording" else "Long press to record voice",
            tint = if (isRecording) 
                MaterialTheme.colorScheme.onError 
            else 
                MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxSize(0.6f)
        )
    }
}
