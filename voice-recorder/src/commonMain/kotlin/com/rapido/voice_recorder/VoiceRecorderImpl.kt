package com.rapido.voice_recorder

import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class VoiceRecorderImpl(
    private val platformRecorder: PlatformVoiceRecorder,
): VoiceRecorder {
    private val _state = MutableStateFlow<VoiceRecorderState>(VoiceRecorderState.Idle)
    override val state: StateFlow<VoiceRecorderState> = _state.asStateFlow()

    private var recordingStartTimeMs: Long = 0
    private var playbackJob: Job? = null
    private var currentAudio: RecordedAudio? = null
    
    init {
        platformRecorder.setOnPlaybackCompletedListener {
            CoroutineScope(Dispatchers.Default).launch {
                handlePlaybackCompleted()
            }
        }
    }

    override suspend fun startRecording() {
        when (state.value) {
            is VoiceRecorderState.Recording -> throw IllegalStateException("Already recording")
            is VoiceRecorderState.Playing, is VoiceRecorderState.Paused -> {
                // Stop playback before starting recording
                stopPlayback()
                startRecordingInternal()
            }
            else -> startRecordingInternal()
        }
    }
    
    private suspend fun startRecordingInternal() {
        try {
            // Generate a unique output file path
            val outputFilePath = generateOutputFilePath()

            // Start platform recording
            platformRecorder.startPlatformRecording(outputFilePath)

            // Update state and record start time
            recordingStartTimeMs = getCurrentTimeMs()
            _state.value = VoiceRecorderState.Recording(0L)
        } catch (e: Exception) {
            _state.value = VoiceRecorderState.Error(e, VoiceRecorderState.ErrorSource.RECORDING)
            throw e
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
                    currentAudio = recordedAudio

                    return recordedAudio
                } catch (e: Exception) {
                    _state.value = VoiceRecorderState.Error(e, VoiceRecorderState.ErrorSource.RECORDING)
                    throw e
                }
            }
            else -> throw IllegalStateException("Not recording, current state: $currentState")
        }
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
                    _state.value = VoiceRecorderState.Error(e, VoiceRecorderState.ErrorSource.RECORDING)
                    throw e
                }
            }
            else -> throw IllegalStateException("Not recording")
        }
    }
    
    override suspend fun playRecording(audio: RecordedAudio) {
        when (state.value) {
            is VoiceRecorderState.Idle, 
            is VoiceRecorderState.Completed, 
            is VoiceRecorderState.Paused -> {
                try {
                    currentAudio = audio
                    platformRecorder.startPlatformPlayback(audio.filePath)
                    _state.value = VoiceRecorderState.Playing(audio, 0)
                    
                    // Start position tracking
                    startPositionTracking(audio)
                } catch (e: Exception) {
                    _state.value = VoiceRecorderState.Error(e, VoiceRecorderState.ErrorSource.PLAYBACK)
                    throw e
                }
            }
            else -> throw IllegalStateException("Cannot start playback in current state: ${state.value}")
        }
    }
    
    override suspend fun pausePlayback() {
        when (val currentState = state.value) {
            is VoiceRecorderState.Playing -> {
                try {
                    platformRecorder.pausePlatformPlayback()
                    playbackJob?.cancel()
                    _state.value = VoiceRecorderState.Paused(
                        currentState.audio, 
                        platformRecorder.getCurrentPlaybackPositionMs()
                    )
                } catch (e: Exception) {
                    _state.value = VoiceRecorderState.Error(e, VoiceRecorderState.ErrorSource.PLAYBACK)
                    throw e
                }
            }
            else -> throw IllegalStateException("Cannot pause in current state: ${state.value}")
        }
    }
    
    override suspend fun resumePlayback() {
        when (val currentState = state.value) {
            is VoiceRecorderState.Paused -> {
                try {
                    platformRecorder.resumePlatformPlayback()
                    _state.value = VoiceRecorderState.Playing(
                        currentState.audio, 
                        currentState.positionMs
                    )
                    
                    // Resume position tracking
                    startPositionTracking(currentState.audio)
                } catch (e: Exception) {
                    _state.value = VoiceRecorderState.Error(e, VoiceRecorderState.ErrorSource.PLAYBACK)
                    throw e
                }
            }
            else -> throw IllegalStateException("Cannot resume in current state: ${state.value}")
        }
    }
    
    override suspend fun stopPlayback() {
        when (state.value) {
            is VoiceRecorderState.Playing, 
            is VoiceRecorderState.Paused -> {
                try {
                    platformRecorder.stopPlatformPlayback()
                    playbackJob?.cancel()
                    currentAudio?.let {
                        _state.value = VoiceRecorderState.Completed(it)
                    } ?: run {
                        _state.value = VoiceRecorderState.Idle
                    }
                } catch (e: Exception) {
                    _state.value = VoiceRecorderState.Error(e, VoiceRecorderState.ErrorSource.PLAYBACK)
                    throw e
                }
            }
            else -> {
                // If we're not playing or paused, this is a no-op
            }
        }
    }
    
    override suspend fun deleteRecording(audio: RecordedAudio): Boolean {
        // First stop playback if this audio is playing
        if ((state.value is VoiceRecorderState.Playing && 
             (state.value as VoiceRecorderState.Playing).audio == audio) ||
            (state.value is VoiceRecorderState.Paused && 
             (state.value as VoiceRecorderState.Paused).audio == audio)) {
            stopPlayback()
        }
        
        return try {
            val result = platformRecorder.deletePlatformRecording(audio.filePath)
            if (result && currentAudio == audio) {
                currentAudio = null
                _state.value = VoiceRecorderState.Idle
            }
            result
        } catch (e: Exception) {
            _state.value = VoiceRecorderState.Error(e, VoiceRecorderState.ErrorSource.OTHER)
            false
        }
    }

    override fun release() {
        playbackJob?.cancel()
        platformRecorder.release()
    }
    
    private fun startPositionTracking(audio: RecordedAudio) {
        playbackJob?.cancel()
        playbackJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    val position = platformRecorder.getCurrentPlaybackPositionMs()
                    _state.value = VoiceRecorderState.Playing(audio, position)
                    delay(100) // Update position every 100ms
                } catch (e: Exception) {
                    // If there's an error getting position, we'll just stop updating
                    break
                }
            }
        }
    }
    
    private fun handlePlaybackCompleted() {
        playbackJob?.cancel()
        currentAudio?.let {
            _state.value = VoiceRecorderState.Completed(it)
        } ?: run {
            _state.value = VoiceRecorderState.Idle
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