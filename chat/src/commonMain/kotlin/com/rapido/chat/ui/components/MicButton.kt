package com.rapido.chat.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A simple microphone button that responds to long press for recording.
 *
 * @param isRecording Whether the button is in recording state
 * @param onLongPress Callback when long press starts
 * @param onLongPressRelease Callback when long press ends
 * @param onTap Callback when the button is tapped
 */
@Composable
fun MicButton(
    isRecording: Boolean,
    onLongPress: () -> Unit,
    onLongPressRelease: () -> Unit,
    onTap: () -> Unit
) {
    // Track whether we've handled the long press start
    var longPressStarted by remember { mutableStateOf(false) }
    
    // Use Box instead of IconButton to avoid click conflicts
    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(isRecording) {
                detectTapGestures(
//                    onTap = {
//                        // Only handle tap if we haven't started a long press
//                        if (!longPressStarted) {
//                            onTap()
//                        }
//                        // Reset long press state
//                        longPressStarted = false
                    //},
                    onLongPress = {
                        // delay(1000)
                        if (!isRecording) {
                            longPressStarted = true
                            onLongPress()
                        }
                    },
                    onPress = {
                        // Set pressed state
                        val wasRecording = isRecording
                        
                        // Wait for pointer release
                        if (tryAwaitRelease()) {
                            // If we were already recording, this is a release event
                            if (wasRecording && longPressStarted) {
                                onLongPressRelease()
                                longPressStarted = false
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // If recording is active, ensure we reset state when recording is stopped from elsewhere
        LaunchedEffect(isRecording) {
            if (!isRecording) {
                longPressStarted = false
            }
        }
        
        Surface(
            shape = CircleShape,
            color = if (isRecording) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isRecording)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = if (isRecording) "Recording in progress" else "Record voice message",
                    tint = if (isRecording) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
