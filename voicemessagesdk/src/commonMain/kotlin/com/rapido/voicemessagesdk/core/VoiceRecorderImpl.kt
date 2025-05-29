package com.rapido.voicemessagesdk.core

import com.rapido.voicemessagesdk.recording.AudioRecorder
import com.rapido.voicemessagesdk.playback.AudioPlayer
import com.rapido.voicemessagesdk.storage.AudioFileManager
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
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val audioFileManager: AudioFileManager
) : VoiceRecorder {
    private val _state = MutableStateFlow<VoiceRecorderState>(VoiceRecorderState.Idle)
    override val state: StateFlow<VoiceRecorderState> = _state.asStateFlow()

    private var recordingStartTimeMs: Long = 0
    private var updateJob: Job? = null

    // Create a CoroutineScope with a SupervisorJob to handle errors gracefully
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        audioPlayer.setOnPlaybackCompletedListener {
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
            val outputFilePath = audioFileManager.createRecordingFilePath()
            audioRecorder.startRecording(outputFilePath)
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

    override suspend fun finishAndSendRecording(): VoiceMessage {
        when (val currentState = state.value) {
            is VoiceRecorderState.Recording -> {
                try {
                    updateJob?.cancel()
                    val recordedAudio = audioRecorder.stopRecording()
                    _state.value = VoiceRecorderState.RecordingCompleted(recordedAudio)
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

    override suspend fun deleteRecording() {
        when (state.value) {
            is VoiceRecorderState.Recording -> {
                try {
                    updateJob?.cancel()
                    // Get the current file path before stopping
                    val currentFilePath = audioRecorder.getCurrentRecordingFilePath()

                    // Stop recording first
                    audioRecorder.stopRecording()

                    // Delete the file if we have it
                    currentFilePath?.let {
                        audioRecorder.deleteRecording(it)
                    }

                    _state.value = VoiceRecorderState.Idle
                } catch (e: Exception) {
                    val wrappedException = VoiceRecorderException.RecordingFailedException(cause = e)
                    _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.RECORDING)
                    throw wrappedException
                }
            }
            is VoiceRecorderState.Preview -> {
                try {
                    val previewState = state.value as VoiceRecorderState.Preview
                    // Stop playback if playing
                    if (previewState.playing) {
                        stopPlayback()
                    }
                    // Delete the audio file
                    audioFileManager.deleteRecording(previewState.voiceMessage.filePath)
                    _state.value = VoiceRecorderState.Idle
                } catch (e: Exception) {
                    val wrappedException = VoiceRecorderException.FileOperationException(cause = e)
                    _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.FILE_OPERATION)
                    throw wrappedException
                }
            }
            is VoiceRecorderState.RecordingCompleted -> {
                try {
                    val completedState = state.value as VoiceRecorderState.RecordingCompleted
                    // Delete the audio file
                    audioFileManager.deleteRecording(completedState.voiceMessage.filePath)
                    _state.value = VoiceRecorderState.Idle
                } catch (e: Exception) {
                    val wrappedException = VoiceRecorderException.FileOperationException(cause = e)
                    _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.FILE_OPERATION)
                    throw wrappedException
                }
            }
            is VoiceRecorderState.ReadyToSend -> {
                try {
                    val readyState = state.value as VoiceRecorderState.ReadyToSend
                    // Delete the audio file
                    audioFileManager.deleteRecording(readyState.voiceMessage.filePath)
                    _state.value = VoiceRecorderState.Idle
                } catch (e: Exception) {
                    val wrappedException = VoiceRecorderException.FileOperationException(cause = e)
                    _state.value = VoiceRecorderState.Error(wrappedException, VoiceRecorderState.ErrorSource.FILE_OPERATION)
                    throw wrappedException
                }
            }
            else -> throw VoiceRecorderException.InvalidStateException("Cannot delete recording in current state: ${state.value}")
        }
    }

    override suspend fun playRecording(audio: VoiceMessage) {
        when (state.value) {
            is VoiceRecorderState.Idle,
            is VoiceRecorderState.Preview,
            is VoiceRecorderState.RecordingCompleted,
            is VoiceRecorderState.ReadyToSend -> {
                try {
                    audioPlayer.startPlayback(audio.filePath)
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
                        audioPlayer.pausePlayback()
                        updateJob?.cancel()
                        _state.value = currentState.copy(
                            playing = false,
                            currentPositionMs = audioPlayer.getCurrentPlaybackPositionMs()
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
                        audioPlayer.resumePlayback()
                        _state.value = currentState.copy(playing = true)
                        startPositionTracking(currentState.voiceMessage)
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
                        audioPlayer.stopPlayback()
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

    override suspend fun deleteRecording(audio: VoiceMessage): Boolean {
        return try {
            // First stop playback if this audio is playing
            if (state.value is VoiceRecorderState.Preview &&
                (state.value as VoiceRecorderState.Preview).voiceMessage == audio) {
                stopPlayback()
            }

            val result = audioFileManager.deleteRecording(audio.filePath)
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

    override suspend fun enterPreviewMode(voiceMessage: VoiceMessage) {
        when (state.value) {
            is VoiceRecorderState.RecordingCompleted -> {
                _state.value = VoiceRecorderState.Preview(voiceMessage, playing = false)
            }
            is VoiceRecorderState.ReadyToSend -> {
                // Allow going back to preview from ready to send
                _state.value = VoiceRecorderState.Preview(voiceMessage, playing = false)
            }
            else -> throw VoiceRecorderException.InvalidStateException("Cannot enter preview mode from current state: ${state.value}")
        }
    }

    override suspend fun markReadyToSend(voiceMessage: VoiceMessage) {
        when (state.value) {
            is VoiceRecorderState.Preview -> {
                // Stop playback if currently playing
                if ((state.value as VoiceRecorderState.Preview).playing) {
                    stopPlayback()
                }
                _state.value = VoiceRecorderState.ReadyToSend(voiceMessage)
            }
            is VoiceRecorderState.RecordingCompleted -> {
                // Allow direct transition from completed to ready to send
                _state.value = VoiceRecorderState.ReadyToSend(voiceMessage)
            }
            else -> throw VoiceRecorderException.InvalidStateException("Cannot mark ready to send from current state: ${state.value}")
        }
    }

    override suspend fun transitionToIdle() {
        // Stop any ongoing playback without deleting files
        if (state.value is VoiceRecorderState.Preview && (state.value as VoiceRecorderState.Preview).playing) {
            stopPlayback()
        }
        updateJob?.cancel()
        _state.value = VoiceRecorderState.Idle
    }

    override fun release() {
        updateJob?.cancel()
        scope.cancel() // Cancel all coroutines
        audioRecorder.release()
        audioPlayer.release()
    }

    private fun startPositionTracking(audio: VoiceMessage) {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                try {
                    val position = audioPlayer.getCurrentPlaybackPositionMs()
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