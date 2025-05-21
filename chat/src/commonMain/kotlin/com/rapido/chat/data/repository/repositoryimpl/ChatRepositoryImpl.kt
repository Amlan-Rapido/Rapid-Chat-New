package com.rapido.chat.data.repository.repositoryimpl

import com.rapido.chat.data.VoiceRecorderIntegration
import com.rapido.chat.data.model.ChatMessage
import com.rapido.chat.data.model.MessageType
import com.rapido.chat.data.model.Sender
import com.rapido.chat.data.repository.ChatRepository
import com.rapido.voice_recorder.RecordedAudio
import com.rapido.voice_recorder.VoiceRecorderState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Implementation of the ChatRepository interface.
 * Manages chat messages and interacts with the VoiceRecorderIntegration.
 *
 * @param voiceRecorderIntegration Integration for handling voice recording functionality
 */
class ChatRepositoryImpl(
    private val voiceRecorderIntegration: VoiceRecorderIntegration
) : ChatRepository {
    
    // In-memory storage for messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    
    // Map to store audio recordings by message ID
    private val audioRecordings = mutableMapOf<String, RecordedAudio>()
    
    // Current recording for when a voice message is being created
    private var currentRecording: RecordedAudio? = null

    override fun getChatMessages(): Flow<List<ChatMessage>> = _messages.asStateFlow()
    
    override val voiceRecorderState: StateFlow<VoiceRecorderState> = voiceRecorderIntegration.state
    
    override suspend fun sendTextMessage(content: String): ChatMessage {
        val message = ChatMessage(
            id = generateRandomId(),
            sender = Sender.USER,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            type = MessageType.TEXT,
            content = content
        )
        
        addMessage(message)
        return message
    }
    
    override suspend fun startVoiceMessage() {
        voiceRecorderIntegration.startRecording()
    }
    
    override suspend fun cancelVoiceMessage() {
        voiceRecorderIntegration.cancelRecording()
        currentRecording = null
    }
    
    override suspend fun finishAndSendVoiceMessage(): ChatMessage? {
        return try {
            val recordedAudio = voiceRecorderIntegration.stopRecording()
            currentRecording = recordedAudio
            
            val message = ChatMessage(
                id = generateRandomId(),
                sender = Sender.USER,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                type = MessageType.VOICE,
                content = "",
                audioUrl = recordedAudio.filePath,
                audioDuration = recordedAudio.durationMs
            )
            
            audioRecordings[message.id] = recordedAudio
            addMessage(message)
            message
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun playVoiceMessage(messageId: String) {
        audioRecordings[messageId]?.let { audio ->
            voiceRecorderIntegration.playRecording(audio)
        }
    }
    
    override suspend fun pauseVoiceMessage(messageId: String) {
        if (audioRecordings.containsKey(messageId)) {
            voiceRecorderIntegration.pausePlayback()
        }
    }
    
    override suspend fun resumeVoiceMessage(messageId: String) {
        if (audioRecordings.containsKey(messageId)) {
            voiceRecorderIntegration.resumePlayback()
        }
    }
    
    override suspend fun stopVoiceMessage(messageId: String) {
        if (audioRecordings.containsKey(messageId)) {
            voiceRecorderIntegration.stopPlayback()
        }
    }
    
    override suspend fun deleteMessage(messageId: String): Boolean {
        val currentMessages = _messages.value
        val messageToDelete = currentMessages.find { it.id == messageId } ?: return false
        
        // If it's a voice message, delete the audio file
        if (messageToDelete.type == MessageType.VOICE) {
            audioRecordings[messageId]?.let { audio ->
                voiceRecorderIntegration.deleteRecording(audio)
                audioRecordings.remove(messageId)
            }
        }
        
        _messages.value = currentMessages.filter { it.id != messageId }
        return true
    }
    
    private fun addMessage(message: ChatMessage) {
        _messages.value += message
    }

    private fun generateRandomId(): String {
        return Random.nextInt(100000, 999999).toString()
    }
} 