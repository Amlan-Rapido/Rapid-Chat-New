package com.rapido.chat.data.repository.repositoryimpl

import com.rapido.chat.data.VoiceRecorderIntegration
import com.rapido.chat.data.model.ChatMessage
import com.rapido.chat.data.model.MessageType
import com.rapido.chat.data.model.Sender
import com.rapido.chat.data.repository.ChatRepository
import com.rapido.chat.integration.screens.platformLogD
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

    init {
        // Add initial welcome messages
        addWelcomeMessages()
    }

    override val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    override val voiceRecorderState: StateFlow<VoiceRecorderState> = voiceRecorderIntegration.state
    
    override suspend fun sendTextMessage(content: String) {
        if (content.isBlank()) return
        
        val message = ChatMessage(
            id = generateRandomId(),
            sender = Sender.USER,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            type = MessageType.TEXT,
            content = content
        )
        addMessage(message)
    }
    
    override suspend fun deleteMessage(messageId: String) {
        val message = _messages.value.find { it.id == messageId } ?: return
        
        // If it's a voice message, delete the audio file
        if (message.type == MessageType.VOICE) {
            audioRecordings[messageId]?.let { audio ->
                voiceRecorderIntegration.deleteRecording(audio)
                audioRecordings.remove(messageId)
            }
        }
        
        _messages.value = _messages.value.filter { it.id != messageId }
    }
    
    override suspend fun startVoiceRecording() {
        voiceRecorderIntegration.startRecording()
    }
    
    override suspend fun finishVoiceRecording() {
        currentRecording = voiceRecorderIntegration.finishAndSendRecording()
    }
    
    override suspend fun deleteCurrentVoiceRecording() {
        voiceRecorderIntegration.deleteRecording()
        currentRecording = null
    }
    
    override suspend fun finishAndSendVoiceMessage(): ChatMessage? {
        return try {
            val recordedAudio = currentRecording ?: voiceRecorderIntegration.finishAndSendRecording()
            
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
            currentRecording = null
            message
        } catch (e: Exception) {
            platformLogD("ChatRepository", "Exception ${e.message} occurred during finishAndSendVoiceMessage")
            null
        }
    }
    
    override suspend fun playVoiceMessage(messageId: String) {
        audioRecordings[messageId]?.let { audio ->
            voiceRecorderIntegration.playRecording(audio)
        }
    }
    
    override suspend fun playVoiceRecording(audio: RecordedAudio) {
        voiceRecorderIntegration.playRecording(audio)
    }
    
    override suspend fun pauseVoicePlayback() {
        voiceRecorderIntegration.pausePlayback()
    }
    
    override suspend fun resumeVoicePlayback() {
        voiceRecorderIntegration.resumePlayback()
    }
    
    override suspend fun stopVoicePlayback() {
        voiceRecorderIntegration.stopPlayback()
    }
    
    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private fun generateRandomId(): String {
        return kotlin.random.Random.nextInt(100000, 999999).toString()
    }
    
    private fun addWelcomeMessages() {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        
        // Add a welcome message from the system
        val welcomeMessage = ChatMessage(
            id = generateRandomId(),
            sender = Sender.SYSTEM,
            timestamp = currentTime - 60000,
            type = MessageType.TEXT,
            content = "Welcome to Rapid Chat! This is a simple chat application where you can send text and voice messages."
        )
        
        // Add a hint about voice messages
        val voiceHintMessage = ChatMessage(
            id = generateRandomId(),
            sender = Sender.SYSTEM,
            timestamp = currentTime,
            type = MessageType.TEXT,
            content = "Try recording a voice message by tapping the microphone button. Tap the stop button when you're done recording."
        )
        
        // Add both messages
        _messages.value = listOf(welcomeMessage, voiceHintMessage)
    }
} 