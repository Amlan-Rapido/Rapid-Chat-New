package com.rapido.voice_recorder

expect class PlatformAudioFileManager {

    fun createRecordingFilePath(): String

    fun deleteRecording(filePath: String): Boolean

    fun getRecordingsDirectory(): String
    
    companion object {
        val RECORDING_FILE_EXTENSION: String
        
        fun generateFileName(): String
    }
} 