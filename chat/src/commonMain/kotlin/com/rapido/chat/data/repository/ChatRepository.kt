package com.rapido.chat.data.repository

import com.rapido.chat.data.model.ChatMessage
import com.rapido.voicemessagesdk.core.VoiceMessage
import com.rapido.voicemessagesdk.core.VoiceMessageManager
import com.rapido.voicemessagesdk.core.VoiceRecorderState
import com.rapido.voicemessagesdk.ui.VoiceMessageData
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for chat operations.
 * Provides methods to send and retrieve messages.
 */
interface ChatRepository {
    val messages: StateFlow<List<ChatMessage>>
    
    // Text message operations
    suspend fun sendTextMessage(content: String)
    suspend fun deleteMessage(messageId: String)
    
    // Voice message operations (simplified integration)
    suspend fun sendVoiceMessageData(voiceMessageData: VoiceMessageData): ChatMessage?
    
    // Voice playback operations (for playing sent voice messages)
    suspend fun playVoiceMessage(messageId: String)
    suspend fun playVoiceRecording(audio: VoiceMessage)
    suspend fun pauseVoicePlayback()
    suspend fun resumeVoicePlayback()
    suspend fun stopVoicePlayback()
    
    // Get current voice recorder state
    val voiceRecorderState: StateFlow<VoiceRecorderState>
    
    // Expose voice message manager for simplified integration
    val voiceMessageManager: VoiceMessageManager
} 