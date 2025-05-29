package com.rapido.voicemessagesdk.core

import com.rapido.voicemessagesdk.ui.VoiceMessageCallbacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Implementation of VoiceMessageManager that coordinates voice recording operations
 * and provides callbacks for integration with chat SDKs.
 */
class VoiceMessageManagerImpl(
    private val voiceRecorder: VoiceRecorder
) : VoiceMessageManager {

    override val state: StateFlow<VoiceRecorderState> = voiceRecorder.state

    private var callbacks: VoiceMessageCallbacks? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Observe state changes and emit appropriate callbacks
        scope.launch {
            state.collect { newState ->
                handleStateChange(newState)
            }
        }
    }

    override fun setCallbacks(callbacks: VoiceMessageCallbacks?) {
        this.callbacks = callbacks
    }

    override suspend fun startRecording() {
        try {
            voiceRecorder.startRecording()
            callbacks?.onRecordingStarted()
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override suspend fun stopRecording(): VoiceMessage {
        return try {
            val voiceMessage = voiceRecorder.finishAndSendRecording()
            callbacks?.onRecordingFinished(voiceMessage)
            voiceMessage
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override suspend fun cancelRecording() {
        try {
            voiceRecorder.deleteRecording()
            callbacks?.onRecordingCancelled()
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override suspend fun enterPreviewMode(voiceMessage: VoiceMessage) {
        try {
            voiceRecorder.enterPreviewMode(voiceMessage)
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override suspend fun startPreview(voiceMessage: VoiceMessage) {
        try {
            voiceRecorder.playRecording(voiceMessage)
            callbacks?.onPreviewStarted(voiceMessage)
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override suspend fun pausePreview() {
        try {
            voiceRecorder.pausePlayback()
            val currentVoiceMessage = state.value.currentVoiceMessage
            currentVoiceMessage?.let { callbacks?.onPreviewPaused(it) }
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override suspend fun resumePreview() {
        try {
            voiceRecorder.resumePlayback()
            val currentVoiceMessage = state.value.currentVoiceMessage
            currentVoiceMessage?.let { callbacks?.onPreviewStarted(it) }
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override suspend fun stopPreview() {
        try {
            voiceRecorder.stopPlayback()
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override suspend fun markReadyToSend(voiceMessage: VoiceMessage) {
        try {
            voiceRecorder.markReadyToSend(voiceMessage)
            callbacks?.onReadyToSend(voiceMessage)
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override suspend fun deleteVoiceMessage(voiceMessage: VoiceMessage): Boolean {
        return try {
            val result = voiceRecorder.deleteRecording(voiceMessage)
            if (result) {
                callbacks?.onRecordingCancelled()
            }
            result
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override suspend fun reset() {
        // If there's a current voice message, try to clean it up
        val currentVoiceMessage = state.value.currentVoiceMessage
        if (currentVoiceMessage != null) {
            try {
                deleteVoiceMessage(currentVoiceMessage)
            } catch (e: VoiceRecorderException) {
                // Log error but don't throw as this is a cleanup operation
                callbacks?.onError(e)
            }
        }
    }

    override suspend fun transitionToIdle() {
        try {
            voiceRecorder.transitionToIdle()
        } catch (e: VoiceRecorderException) {
            callbacks?.onError(e)
            throw e
        }
    }

    override fun release() {
        scope.cancel()
        voiceRecorder.release()
    }

    private fun handleStateChange(newState: VoiceRecorderState) {
        when (newState) {
            is VoiceRecorderState.Error -> {
                callbacks?.onError(newState.exception)
            }
            is VoiceRecorderState.Preview -> {
                if (!newState.playing && newState.currentPositionMs == 0L) {
                    // Playback completed
                    callbacks?.onPreviewCompleted(newState.voiceMessage)
                }
            }
            else -> {
                // Other state changes are handled by the method calls themselves
            }
        }
    }
} 