package com.rapido.voice_recorder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class VoiceRecorderImpl(
    private val platformRecorder: PlatformVoiceRecorder,
    private val platformAudioFileManager: PlatformAudioFileManager
) : VoiceRecorder {
    private val _state = MutableStateFlow<VoiceRecorderState>(VoiceRecorderState.Idle)
    override val state: StateFlow<VoiceRecorderState> = _state.asStateFlow()

    private var recordingStartTimeMs: Long = 0
    private var updateJob: Job? = null
    
    // Create a CoroutineScope with a SupervisorJob to handle errors gracefully
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        platformRecorder.setOnPlaybackCompletedListener {
            scope.launch {
                handlePlaybackCompleted()
            }
        }
    }

    override suspend fun startRecording() {
        when (state.value) {
            is VoiceRecorderState.Recording -> throw VoiceRecorderException.AlreadyRecordingException()
            is VoiceRecorderState.Preview -> {
                // Stop playback before starting recording
                stopPlayback()
                startRecordingInternal()
            }
            else -> startRecordingInternal()
        }
    }
    
    private suspend fun startRecordingInternal() {
        try {
            val outputFilePath = platformAudioFileManager.createRecordingFilePath()
            platformRecorder.startPlatformRecording(outputFilePath)
            recordingStartTimeMs = getCurrentTimeMs()
            _state.value = VoiceRecorderState.Recording(0L)

            launchRepeatingUpdate(100) {
                val duration = getCurrentTimeMs() - recordingStartTimeMs
                _state.value = VoiceRecorderState.Recording(duration)
            }

        } catch (e: Exception) {
            val wrappedException = VoiceRecorderException.RecordingFailedException(cause = e)
            _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.RECORDING)
            throw wrappedException
        }
    }

    override suspend fun stopRecording(): RecordedAudio {
        when (val currentState = state.value) {
            is VoiceRecorderState.Recording -> {
                try {
                    updateJob?.cancel()
                    val recordedAudio = platformRecorder.stopPlatformRecording()
                    _state.value = VoiceRecorderState.Preview(recordedAudio, playing = false)
                    return recordedAudio
                } catch (e: Exception) {
                    val wrappedException = VoiceRecorderException.RecordingFailedException(cause = e)
                    _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.RECORDING)
                    throw wrappedException
                }
            }
            else -> throw VoiceRecorderException.InvalidStateException("Not recording, current state: $currentState")
        }
    }

    override suspend fun cancelRecording() {
        when (state.value) {
            is VoiceRecorderState.Recording -> {
                try {
                    updateJob?.cancel()
                    platformRecorder.cancelPlatformRecording()
                    _state.value = VoiceRecorderState.Idle
                } catch (e: Exception) {
                    val wrappedException = VoiceRecorderException.RecordingFailedException(cause = e)
                    _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.RECORDING)
                    throw wrappedException
                }
            }
            else -> throw VoiceRecorderException.InvalidStateException("Not recording")
        }
    }
    
    override suspend fun playRecording(audio: RecordedAudio) {
        when (state.value) {
            is VoiceRecorderState.Idle,
            is VoiceRecorderState.Preview -> {
                try {
                    platformRecorder.startPlatformPlayback(audio.filePath)
                    _state.value = VoiceRecorderState.Preview(audio, playing = true)
                    startPositionTracking(audio)
                } catch (e: Exception) {
                    val wrappedException = VoiceRecorderException.PlaybackFailedException(cause = e)
                    _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.PLAYBACK)
                    throw wrappedException
                }
            }
            else -> throw VoiceRecorderException.InvalidStateException("Cannot start playback in current state: ${state.value}")
        }
    }
    
    override suspend fun pausePlayback() {
        when (val currentState = state.value) {
            is VoiceRecorderState.Preview -> {
                if (currentState.playing) {
                    try {
                        platformRecorder.pausePlatformPlayback()
                        updateJob?.cancel()
                        _state.value = currentState.copy(
                            playing = false,
                            currentPositionMs = platformRecorder.getCurrentPlaybackPositionMs()
                        )
                    } catch (e: Exception) {
                        val wrappedException = VoiceRecorderException.PlaybackFailedException(cause = e)
                        _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.PLAYBACK)
                        throw wrappedException
                    }
                }
            }
            else -> throw VoiceRecorderException.InvalidStateException("Cannot pause in current state: ${state.value}")
        }
    }
    
    override suspend fun resumePlayback() {
        when (val currentState = state.value) {
            is VoiceRecorderState.Preview -> {
                if (!currentState.playing) {
                    try {
                        platformRecorder.resumePlatformPlayback()
                        _state.value = currentState.copy(playing = true)
                        startPositionTracking(currentState.audio)
                    } catch (e: Exception) {
                        val wrappedException = VoiceRecorderException.PlaybackFailedException(cause = e)
                        _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.PLAYBACK)
                        throw wrappedException
                    }
                }
            }
            else -> throw VoiceRecorderException.InvalidStateException("Cannot resume in current state: ${state.value}")
        }
    }
    
    override suspend fun stopPlayback() {
        when (val currentState = state.value) {
            is VoiceRecorderState.Preview -> {
                if (currentState.playing) {
                    try {
                        platformRecorder.stopPlatformPlayback()
                        updateJob?.cancel()
                        _state.value = currentState.copy(
                            playing = false,
                            currentPositionMs = 0
                        )
                    } catch (e: Exception) {
                        val wrappedException = VoiceRecorderException.PlaybackFailedException(cause = e)
                        _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.PLAYBACK)
                        throw wrappedException
                    }
                }
            }
            else -> {
                // If we're not in preview state, this is a no-op
            }
        }
    }
    
    override suspend fun deleteRecording(audio: RecordedAudio): Boolean {
        return try {
            // First stop playback if this audio is playing
            if (state.value is VoiceRecorderState.Preview && 
                (state.value as VoiceRecorderState.Preview).audio == audio) {
                stopPlayback()
            }
            
            val result = platformAudioFileManager.deleteRecording(audio.filePath)
            if (result) {
                _state.value = VoiceRecorderState.Idle
            }
            result
        } catch (e: Exception) {
            val wrappedException = VoiceRecorderException.FileOperationException(cause = e)
            _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.FILE_OPERATION)
            throw wrappedException
        }
    }

    override fun release() {
        updateJob?.cancel()
        scope.cancel() // Cancel all coroutines
        platformRecorder.release()
    }
    
    private fun startPositionTracking(audio: RecordedAudio) {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                try {
                    val position = platformRecorder.getCurrentPlaybackPositionMs()
                    _state.value = (state.value as? VoiceRecorderState.Preview)?.copy(
                        currentPositionMs = position
                    ) ?: break
                    delay(100) // Update position every 100ms
                } catch (e: Exception) {
                    // If there's an error getting position, we'll just stop updating
                    break
                }
            }
        }
    }
    
    private fun handlePlaybackCompleted() {
        updateJob?.cancel()
        (state.value as? VoiceRecorderState.Preview)?.let { currentState ->
            _state.value = currentState.copy(
                playing = false,
                currentPositionMs = 0
            )
        }
    }

    private fun getCurrentTimeMs(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }

    private fun launchRepeatingUpdate(intervalMs: Long, block: suspend () -> Unit) {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                block()
                delay(intervalMs)
            }
        }
    }
}