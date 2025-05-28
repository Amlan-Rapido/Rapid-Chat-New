package com.rapido.chat.data

import com.rapido.voicemessagesdk.core.RecordedAudio
import kotlinx.coroutines.flow.StateFlow
import com.rapido.voicemessagesdk.core.VoiceRecorderState

/**
 * Interface for integrating with the voice recorder functionality.
 * This acts as a bridge between the chat module and the voice recorder module.
 */
interface VoiceRecorderIntegration {

    val state: StateFlow<VoiceRecorderState>
    
    // Recording operations
    suspend fun startRecording()
    suspend fun finishAndSendRecording(): RecordedAudio
    suspend fun deleteRecording()
    
    // Playback operations
    suspend fun playRecording(audio: RecordedAudio)
    suspend fun pausePlayback()
    suspend fun resumePlayback()
    suspend fun stopPlayback()
    
    // File management
    suspend fun deleteRecording(audio: RecordedAudio): Boolean
    
    // Cleanup
    fun release()
} 