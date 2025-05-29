package com.rapido.voicemessagesdk.core

import com.rapido.voicemessagesdk.ui.VoiceMessageCallbacks
import kotlinx.coroutines.flow.StateFlow

/**
 * High-level manager that coordinates all voice message operations.
 * This is the main interface that chat SDKs should use for voice message functionality.
 */
interface VoiceMessageManager {
    /**
     * Current state of the voice recorder.
     */
    val state: StateFlow<VoiceRecorderState>

    /**
     * Set callbacks to receive voice message events.
     * @param callbacks The callback interface implementation
     */
    fun setCallbacks(callbacks: VoiceMessageCallbacks?)

    /**
     * Start recording a new voice message.
     * @throws VoiceRecorderException if recording cannot be started
     */
    suspend fun startRecording()

    /**
     * Stop the current recording and transition to completed state.
     * @return The recorded voice message
     * @throws VoiceRecorderException if not currently recording
     */
    suspend fun stopRecording(): VoiceMessage

    /**
     * Cancel the current recording and delete the file.
     * @throws VoiceRecorderException if operation fails
     */
    suspend fun cancelRecording()

    /**
     * Enter preview mode for the recorded voice message.
     * @param voiceMessage The voice message to preview
     * @throws VoiceRecorderException if transition is not valid
     */
    suspend fun enterPreviewMode(voiceMessage: VoiceMessage)

    /**
     * Start playing the voice message preview.
     * @param voiceMessage The voice message to play
     * @throws VoiceRecorderException if playback cannot be started
     */
    suspend fun startPreview(voiceMessage: VoiceMessage)

    /**
     * Pause the current preview playback.
     * @throws VoiceRecorderException if not currently playing
     */
    suspend fun pausePreview()

    /**
     * Resume the paused preview playback.
     * @throws VoiceRecorderException if not currently paused
     */
    suspend fun resumePreview()

    /**
     * Stop the current preview playback.
     */
    suspend fun stopPreview()

    /**
     * Mark the voice message as ready to send.
     * @param voiceMessage The voice message to mark as ready
     * @throws VoiceRecorderException if transition is not valid
     */
    suspend fun markReadyToSend(voiceMessage: VoiceMessage)

    /**
     * Delete a voice message and its associated file.
     * @param voiceMessage The voice message to delete
     * @return true if deletion was successful
     * @throws VoiceRecorderException if deletion fails
     */
    suspend fun deleteVoiceMessage(voiceMessage: VoiceMessage): Boolean

    /**
     * Reset to idle state.
     */
    suspend fun reset()

    /**
     * Transition to idle state without deleting files.
     * This is useful when the file ownership has been transferred to another system.
     */
    suspend fun transitionToIdle()

    /**
     * Release all resources.
     */
    fun release()
} 