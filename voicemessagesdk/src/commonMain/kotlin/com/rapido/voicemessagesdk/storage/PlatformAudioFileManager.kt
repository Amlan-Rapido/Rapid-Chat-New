package com.rapido.voicemessagesdk.storage

import kotlinx.datetime.Clock

expect class PlatformAudioFileManager() : AudioFileManager {
    override fun createRecordingFilePath(): String
    override fun deleteRecording(filePath: String): Boolean
    override fun getRecordingsCacheDirectory(): String
    override fun clearRecordingsCache(): Int
    override fun fileExists(filePath: String): Boolean
    override fun getFileSize(filePath: String): Long
}

/**
 * Generates a unique filename for a new recording.
 * @return Filename with extension
 */
fun AudioFileManager.Companion.generateFileName(): String {
    val timestamp = Clock.System.now().toEpochMilliseconds()
    val randomSuffix = kotlin.random.Random.nextInt(1000, 10000)
    return "recording_${timestamp}_$randomSuffix$RECORDING_FILE_EXTENSION"
} 