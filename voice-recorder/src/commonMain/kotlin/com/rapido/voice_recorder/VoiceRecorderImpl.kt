package com.rapido.voice_recorder

import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class VoiceRecorderImpl(
    private val platformRecorder: PlatformVoiceRecorder,
): VoiceRecorder {
    private val _state = MutableStateFlow<VoiceRecorderState>(VoiceRecorderState.Idle)
    override val state: StateFlow<VoiceRecorderState> = _state.asStateFlow()

    private var recordingStartTimeMs: Long = 0

    override suspend fun startRecording() {
        when (state.value) {
            is VoiceRecorderState.Recording -> throw IllegalStateException("Already recording")
            else -> {
                try {
                    // Generate a unique output file path
                    val outputFilePath = generateOutputFilePath()

                    // Start platform recording
                    platformRecorder.startPlatformRecording(outputFilePath)

                    // Update state and record start time
                    recordingStartTimeMs = getCurrentTimeMs()
                    _state.value = VoiceRecorderState.Recording(0L)
                } catch (e: Exception) {
                    _state.value = VoiceRecorderState.Error(e)
                    throw e
                }
            }
        }
    }

    override suspend fun stopRecording(): RecordedAudio {
        when (val currentState = state.value) {
            is VoiceRecorderState.Recording -> {
                try {
                    // Stop platform recording
                    val recordedAudio = platformRecorder.stopPlatformRecording()

                    // Update state
                    _state.value = VoiceRecorderState.Completed(recordedAudio)

                    return recordedAudio
                } catch (e: Exception) {
                    _state.value = VoiceRecorderState.Error(e)
                    throw e
                }
            }
            else -> throw IllegalStateException("Not recording, current state: $currentState")
        }
    }

    override fun release() {
        platformRecorder.release()
    }

    override suspend fun cancelRecording() {
        when (state.value) {
            is VoiceRecorderState.Recording -> {
                try {
                    // Cancel platform recording
                    platformRecorder.cancelPlatformRecording()

                    // Update state
                    _state.value = VoiceRecorderState.Idle
                } catch (e: Exception) {
                    _state.value = VoiceRecorderState.Error(e)
                    throw e
                }
            }
            else -> throw IllegalStateException("Not recording")
        }
    }

    private fun generateOutputFilePath(): String {
        val timestamp = getCurrentTimeMs()
        val random = Random.nextInt(1000, 9999)
        return "recording_${timestamp}_$random.m4a"
    }

    private fun getCurrentTimeMs(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}