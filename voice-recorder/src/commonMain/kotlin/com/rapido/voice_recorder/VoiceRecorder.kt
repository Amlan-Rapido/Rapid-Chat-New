package com.rapido.voice_recorder

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
    suspend fun stopRecording(): RecordedAudio

    /**
     * Cancels the current recording and deletes the partial recording file.
     * @throws VoiceRecorderException.InvalidStateException if not currently recording
     * @throws VoiceRecorderException.RecordingFailedException if canceling the recording fails
     */
    suspend fun cancelRecording()
    
    /**
     * Starts playing a recorded audio file.
     * @throws VoiceRecorderException.InvalidStateException if in an invalid state for playback
     * @throws VoiceRecorderException.PlaybackFailedException if playback fails to start
     */
    suspend fun playRecording(audio: RecordedAudio)

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
    suspend fun deleteRecording(audio: RecordedAudio): Boolean
    
    /**
     * Releases all resources used by the voice recorder.
     * After calling this, the recorder instance should not be used anymore.
     */
    fun release()
}