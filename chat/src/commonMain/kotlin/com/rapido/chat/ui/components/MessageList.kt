package com.rapido.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rapido.chat.data.model.ChatMessage
import com.rapido.chat.data.model.MessageType

@Composable
fun MessageList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    onVoiceMessageClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier,
        reverseLayout = true,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = messages,
            key = { message -> message.id }
        ) { message ->
            MessageItem(
                message = message,
                onVoiceMessageClick = onVoiceMessageClick
            )
        }
    }
}

@Composable
private fun MessageItem(
    message: ChatMessage,
    onVoiceMessageClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        when (message.type) {
            MessageType.TEXT -> {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            MessageType.VOICE -> {
                VoiceMessageItem(
                    message = message,
                    onClick = { onVoiceMessageClick(message.id) }
                )
            }
        }
    }
} 