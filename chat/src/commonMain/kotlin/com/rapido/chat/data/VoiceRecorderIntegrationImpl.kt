package com.rapido.chat.data

import com.rapido.voice_recorder.RecordedAudio
import com.rapido.voice_recorder.VoiceRecorder
import com.rapido.voice_recorder.VoiceRecorderState
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementation of VoiceRecorderIntegration that uses the VoiceRecorder module.
 * This class adapts the VoiceRecorder interface to meet the needs of the chat module.
 *
 * @param voiceRecorder The platform-specific voice recorder implementation
 */
class VoiceRecorderIntegrationImpl(
    private val voiceRecorder: VoiceRecorder
) : VoiceRecorderIntegration {
    
    override val state: StateFlow<VoiceRecorderState> = voiceRecorder.state
    
    override suspend fun startRecording() = voiceRecorder.startRecording()
    
    override suspend fun stopRecording(): RecordedAudio = voiceRecorder.stopRecording()
    
    override suspend fun cancelRecording() = voiceRecorder.cancelRecording()
    
    override suspend fun playRecording(audio: RecordedAudio) = voiceRecorder.playRecording(audio)
    
    override suspend fun pausePlayback() = voiceRecorder.pausePlayback()
    
    override suspend fun resumePlayback() = voiceRecorder.resumePlayback()
    
    override suspend fun stopPlayback() = voiceRecorder.stopPlayback()
    
    override suspend fun deleteRecording(audio: RecordedAudio): Boolean = 
        voiceRecorder.deleteRecording(audio)
    
    override fun release() = voiceRecorder.release()
} 