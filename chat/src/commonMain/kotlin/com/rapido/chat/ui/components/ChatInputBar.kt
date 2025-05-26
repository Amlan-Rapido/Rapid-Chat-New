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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * The input bar at the bottom of the chat screen.
 *
 * @param text Current text in the input field
 * @param onTextChanged Callback when text changes
 * @param onSendClick Callback when the send button is clicked
 * @param isRecording Whether voice recording is in progress
 * @param onVoiceRecordStart Callback when voice recording starts
 * @param onVoiceRecordEnd Callback when voice recording ends
 * @param onVoiceRecordDelete Callback when voice recording is deleted
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    isRecording: Boolean,
    onVoiceRecordStart: () -> Unit,
    onVoiceRecordEnd: () -> Unit,
    onVoiceRecordDelete: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    // Track if text field is focused to manage mic button visibility
    var isTextFieldFocused by remember { mutableStateOf(false) }
    
    // When recording starts, clear focus from the text field
    LaunchedEffect(isRecording) {
        if (isRecording) {
            focusManager.clearFocus()
        }
    }
    
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
                    .padding(vertical = 2.dp)
                    .onFocusChanged { isTextFieldFocused = it.isFocused },
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
                maxLines = 1,
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                minLines = 1,
                enabled = !isRecording // Disable text field while recording
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (text.isNotBlank()) {
                // Show send button when text is not empty
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
                // Show mic button when text field is empty
                MicButton(
                    isRecording = isRecording,
                    onLongPress = {
                        // Start recording when long press begins
                        focusManager.clearFocus() 
                        onVoiceRecordStart()
                    },
                    onLongPressRelease = {
                        // End recording when long press ends
                        onVoiceRecordEnd()
                    },
                    onTap = {
                        // Optional: Handle tap action
                        if (isRecording) {
                            onVoiceRecordDelete()
                        }
                    }
                )
            }
        }
    }
}