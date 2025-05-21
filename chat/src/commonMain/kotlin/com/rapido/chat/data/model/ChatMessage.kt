package com.rapido.chat.data.model

data class ChatMessage(
    val id: String,
    val sender: Sender,
    val timestamp: Long,
    val type: MessageType,
    val content: String,
    val audioUrl: String? = null,
    val audioDuration: Long? = null
) 