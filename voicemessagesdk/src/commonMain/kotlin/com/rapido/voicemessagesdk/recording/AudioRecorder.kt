package com.rapido.voicemessagesdk.recording

import com.rapido.voicemessagesdk.core.RecordedAudio

/**
 * Interface for audio recording operations.
 * Handles the low-level recording functionality.
 */
interface AudioRecorder {
    /**
     * Starts recording to the specified file path.
     * @param outputFilePath Path where the recording should be saved
     */
    suspend fun startRecording(outputFilePath: String)

    /**
     * Stops the current recording and returns the recorded audio information.
     * @return RecordedAudio containing the recording details
     */
    suspend fun stopRecording(): RecordedAudio

    /**
     * Gets the current recording file path.
     * @return Current recording file path, or null if not recording
     */
    fun getCurrentRecordingFilePath(): String?

    /**
     * Deletes a recording file.
     * @param filePath Path to the file to delete
     * @return true if deletion was successful, false otherwise
     */
    fun deleteRecording(filePath: String): Boolean

    /**
     * Releases recording resources.
     */
    fun release()
} 