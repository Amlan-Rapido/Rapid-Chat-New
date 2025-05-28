package com.rapido.chat.data

import com.rapido.voicemessagesdk.core.RecordedAudio
import com.rapido.voicemessagesdk.core.VoiceRecorder
import com.rapido.voicemessagesdk.core.VoiceRecorderState
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
    
    override suspend fun finishAndSendRecording(): RecordedAudio = voiceRecorder.finishAndSendRecording()
    
    override suspend fun deleteRecording() = voiceRecorder.deleteRecording()
    
    override suspend fun playRecording(audio: RecordedAudio) = voiceRecorder.playRecording(audio)
    
    override suspend fun pausePlayback() = voiceRecorder.pausePlayback()
    
    override suspend fun resumePlayback() = voiceRecorder.resumePlayback()
    
    override suspend fun stopPlayback() = voiceRecorder.stopPlayback()
    
    override suspend fun deleteRecording(audio: RecordedAudio): Boolean =
        voiceRecorder.deleteRecording(audio)
    
    override fun release() = voiceRecorder.release()
} 