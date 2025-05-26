package com.rapido.chat.data.repository

import com.rapido.chat.data.model.ChatMessage
import com.rapido.voice_recorder.RecordedAudio
import com.rapido.voice_recorder.VoiceRecorderState
import kotlinx.coroutines.flow.Flow
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
    
    // Voice recording operations
    suspend fun startVoiceRecording()
    suspend fun finishVoiceRecording()
    suspend fun deleteCurrentVoiceRecording()
    suspend fun finishAndSendVoiceMessage(): ChatMessage?
    
    // Voice playback operations
    suspend fun playVoiceMessage(messageId: String)
    suspend fun playVoiceRecording(audio: RecordedAudio)
    suspend fun pauseVoicePlayback()
    suspend fun resumeVoicePlayback()
    suspend fun stopVoicePlayback()
    
    // Get current voice recorder state
    val voiceRecorderState: StateFlow<VoiceRecorderState>
} 