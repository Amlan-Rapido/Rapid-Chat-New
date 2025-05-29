package com.rapido.voicemessagesdk.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for voice recording and playback functionality.
 * All operations that can fail will update the [state] with an Error state and throw a [VoiceRecorderException].
 */
interface VoiceRecorder {
    /**
     * Current state of the voice recorder.
     * Observe this to react to state changes and errors.
     */
    val state: StateFlow<VoiceRecorderState>

    /**
     * Starts recording audio.
     * @throws VoiceRecorderException.AlreadyRecordingException if already recording
     * @throws VoiceRecorderException.RecordingFailedException if recording fails
     */
    suspend fun startRecording()

    /**
     * Stops the current recording and returns the recorded audio.
     * @throws VoiceRecorderException.InvalidStateException if not currently recording
     * @throws VoiceRecorderException.RecordingFailedException if stopping the recording fails
     * @return RecordedAudio object containing the recording details
     */
    suspend fun finishAndSendRecording(): VoiceMessage

    /**
     * Deletes the current recording and cleans up resources.
     * @throws VoiceRecorderException.InvalidStateException if not currently recording
     * @throws VoiceRecorderException.RecordingFailedException if deleting the recording fails
     */
    suspend fun deleteRecording()

    /**
     * Starts playing a recorded audio file.
     * @throws VoiceRecorderException.InvalidStateException if in an invalid state for playback
     * @throws VoiceRecorderException.PlaybackFailedException if playback fails to start
     */
    suspend fun playRecording(audio: VoiceMessage)

    /**
     * Pauses the current playback.
     * @throws VoiceRecorderException.InvalidStateException if not currently playing
     * @throws VoiceRecorderException.PlaybackFailedException if pausing playback fails
     */
    suspend fun pausePlayback()

    /**
     * Resumes the paused playback.
     * @throws VoiceRecorderException.InvalidStateException if not currently paused
     * @throws VoiceRecorderException.PlaybackFailedException if resuming playback fails
     */
    suspend fun resumePlayback()

    /**
     * Stops the current playback.
     * No-op if not currently playing.
     * @throws VoiceRecorderException.PlaybackFailedException if stopping playback fails
     */
    suspend fun stopPlayback()

    /**
     * Deletes a recorded audio file.
     * @return true if deletion was successful, false otherwise
     * @throws VoiceRecorderException.FileOperationException if deletion fails with an error
     */
    suspend fun deleteRecording(audio: VoiceMessage): Boolean

    /**
     * Transitions from RecordingCompleted state to Preview state for the given voice message.
     * @param voiceMessage The voice message to preview
     * @throws VoiceRecorderException.InvalidStateException if not in RecordingCompleted state
     */
    suspend fun enterPreviewMode(voiceMessage: VoiceMessage)

    /**
     * Transitions from Preview state to ReadyToSend state for the given voice message.
     * @param voiceMessage The voice message to mark as ready to send
     * @throws VoiceRecorderException.InvalidStateException if not in Preview state
     */
    suspend fun markReadyToSend(voiceMessage: VoiceMessage)

    /**
     * Transitions to idle state without deleting any files.
     * This is useful when the file ownership has been transferred to another system.
     */
    suspend fun transitionToIdle()

    /**
     * Releases all resources used by the voice recorder.
     * After calling this, the recorder instance should not be used anymore.
     */
    fun release()
} 