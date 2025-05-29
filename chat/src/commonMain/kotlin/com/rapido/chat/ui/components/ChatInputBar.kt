package com.rapido.chat.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.rapido.voicemessagesdk.core.VoiceMessageManager
import com.rapido.voicemessagesdk.ui.VoiceMessageButton
import com.rapido.voicemessagesdk.ui.VoiceMessageButtonCallbacks
import com.rapido.voicemessagesdk.ui.VoiceMessageData

/**
 * The input bar at the bottom of the chat screen.
 *
 * @param text Current text in the input field
 * @param onTextChanged Callback when text changes
 * @param onSendTextMessage Callback when the send button is clicked for text
 * @param onSendVoiceMessage Callback when a voice message is ready to be sent
 * @param onVoiceMessageError Callback when a voice message error occurs
 * @param voiceMessageManager The voice message manager instance
 * @param modifier Modifier for the input bar
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendTextMessage: () -> Unit,
    onSendVoiceMessage: (VoiceMessageData) -> Unit,
    onVoiceMessageError: (String) -> Unit,
    voiceMessageManager: VoiceMessageManager,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    // Create voice message callbacks
    val voiceMessageCallbacks = remember {
        object : VoiceMessageButtonCallbacks {
            override fun onVoiceMessageReady(data: VoiceMessageData) {
                onSendVoiceMessage(data)
            }

            override fun onError(error: String) {
                onVoiceMessageError(error)
            }
        }
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text input field - only show when not recording/previewing
            val voiceRecorderState by voiceMessageManager.state.collectAsState()
            val isVoiceActive = voiceRecorderState.currentVoiceMessage != null
            
            if (!isVoiceActive) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Type a message",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // Send or voice message button
            Box(
                modifier = if (isVoiceActive) Modifier.fillMaxWidth() else Modifier.padding(start = 8.dp)
            ) {
                if (text.isNotBlank() && !isVoiceActive) {
                    // Show send button for text
                    IconButton(
                        onClick = onSendTextMessage
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // Show voice message button
                    VoiceMessageButton(
                        voiceMessageManager = voiceMessageManager,
                        callbacks = voiceMessageCallbacks,
                        modifier = if (isVoiceActive) Modifier.fillMaxWidth() else Modifier
                    )
                }
            }
        }
    }
}