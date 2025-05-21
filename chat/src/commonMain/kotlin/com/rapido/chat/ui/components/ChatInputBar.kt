package com.rapido.chat.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment

/**
 * The input bar at the bottom of the chat screen.
 *
 * @param text Current text in the input field
 * @param onTextChanged Callback when text changes
 * @param onSendClick Callback when the send button is clicked
 * @param isRecording Whether voice recording is in progress
 * @param onVoiceRecordStart Callback when voice recording starts
 * @param onVoiceRecordEnd Callback when voice recording ends
 * @param onVoiceRecordCancel Callback when voice recording is cancelled
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    isRecording: Boolean,
    onVoiceRecordStart: () -> Unit,
    onVoiceRecordEnd: () -> Unit,
    onVoiceRecordCancel: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 4.dp
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text input field
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp),
                    placeholder = { Text("Type a message") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { 
                            if (text.isNotBlank()) {
                                onSendClick()
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Send button (visible when text is not empty)
                if (text.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSendClick()
                            focusManager.clearFocus()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // Mic button (visible when text is empty)
                    MicButton(
                        isRecording = isRecording,
                        onLongPress = onVoiceRecordStart,
                        onLongPressRelease = onVoiceRecordEnd,
                        onTap = { /* Optional action on tap */ }
                    )
                }
            }
        }
    }
}