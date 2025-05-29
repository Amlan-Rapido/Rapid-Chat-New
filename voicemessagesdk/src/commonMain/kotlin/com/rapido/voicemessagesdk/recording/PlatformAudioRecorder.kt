package com.rapido.voicemessagesdk.recording

expect class PlatformAudioRecorder() : AudioRecorder {
    override suspend fun startRecording(outputFilePath: String)
    override suspend fun stopRecording(): com.rapido.voicemessagesdk.core.VoiceMessage
    override fun getCurrentRecordingFilePath(): String?
    override fun deleteRecording(filePath: String): Boolean
    override fun release()
} 