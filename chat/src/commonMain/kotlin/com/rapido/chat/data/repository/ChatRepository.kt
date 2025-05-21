package com.rapido.chat.data.repository

import com.rapido.chat.data.model.ChatMessage
import com.rapido.voice_recorder.VoiceRecorderState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for chat operations.
 * Provides methods to send and retrieve messages.
 */
interface ChatRepository {
    // Get chat messages
    fun getChatMessages(): Flow<List<ChatMessage>>
    
    // Send text message
    suspend fun sendTextMessage(content: String): ChatMessage
    
    // Voice message operations
    suspend fun startVoiceMessage()
    suspend fun cancelVoiceMessage()
    suspend fun finishAndSendVoiceMessage(): ChatMessage?
    
    // Voice message playback
    suspend fun playVoiceMessage(messageId: String)
    suspend fun pauseVoiceMessage(messageId: String)
    suspend fun resumeVoiceMessage(messageId: String)
    suspend fun stopVoiceMessage(messageId: String)
    
    // Delete operations
    suspend fun deleteMessage(messageId: String): Boolean
    
    // Get current voice recorder state
    val voiceRecorderState: StateFlow<VoiceRecorderState>
} 