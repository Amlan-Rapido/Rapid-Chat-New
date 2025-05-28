package com.rapido.voicemessagesdk.storage

/**
 * Interface for managing audio file operations.
 * Handles file creation, deletion, and directory management using cache directory.
 */
interface AudioFileManager {
    /**
     * Creates a new file path for recording in the cache directory.
     * @return Full path to the new recording file
     */
    fun createRecordingFilePath(): String

    /**
     * Deletes a recording file.
     * @param filePath Path to the file to delete
     * @return true if deletion was successful, false otherwise
     */
    fun deleteRecording(filePath: String): Boolean

    /**
     * Gets the recordings cache directory path.
     * @return Path to the recordings cache directory
     */
    fun getRecordingsCacheDirectory(): String

    /**
     * Clears all recordings from the cache directory.
     * @return Number of files deleted
     */
    fun clearRecordingsCache(): Int

    /**
     * Checks if a file exists at the given path.
     * @param filePath Path to check
     * @return true if file exists, false otherwise
     */
    fun fileExists(filePath: String): Boolean

    /**
     * Gets the size of a file in bytes.
     * @param filePath Path to the file
     * @return Size in bytes, or -1 if file doesn't exist
     */
    fun getFileSize(filePath: String): Long

    companion object {
        const val RECORDING_FILE_EXTENSION = ".m4a"
    }
} 