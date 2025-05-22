package com.rapido.chat.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
        modifier = Modifier
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
            // Text input field
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
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
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                minLines = 1
            )
            
            Spacer(modifier = Modifier.width(8.dp))

            if (text.isNotBlank()) {
                IconButton(
                    onClick = {
                        onSendClick()
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
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