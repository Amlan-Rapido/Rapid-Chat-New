package com.rapido.chat.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun MicButton(
    isRecording: Boolean,
    onLongPress: () -> Unit,
    onLongPressRelease: () -> Unit,
    onTap: () -> Unit
) {
    IconButton(
        onClick = { onTap() },
        modifier = Modifier
            .size(48.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { 
                        if (!isRecording) {
                            onLongPress()
                        }
                    },
                    onPress = { 
                        val released = tryAwaitRelease()
                        if (released && isRecording) {
                            onLongPressRelease()
                        }
                    }
                )
            }
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = if (isRecording) "Recording in progress" else "Record voice message",
            tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}
