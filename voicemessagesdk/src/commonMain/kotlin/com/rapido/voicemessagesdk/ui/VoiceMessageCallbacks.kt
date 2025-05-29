package com.rapido.voicemessagesdk.ui

import com.rapido.voicemessagesdk.core.VoiceMessage
import com.rapido.voicemessagesdk.core.VoiceRecorderException

/**
 * Callback interface for voice message events.
 * Implement this interface to receive notifications about voice message lifecycle events.
 */
interface VoiceMessageCallbacks {
    /**
     * Called when recording starts.
     */
    fun onRecordingStarted() {}

    /**
     * Called when recording is completed and a voice message is created.
     * @param voiceMessage The recorded voice message
     */
    fun onRecordingFinished(voiceMessage: VoiceMessage) {}

    /**
     * Called when recording is cancelled/deleted.
     */
    fun onRecordingCancelled() {}

    /**
     * Called when preview playback starts.
     * @param voiceMessage The voice message being previewed
     */
    fun onPreviewStarted(voiceMessage: VoiceMessage) {}

    /**
     * Called when preview playback is paused.
     * @param voiceMessage The voice message being previewed
     */
    fun onPreviewPaused(voiceMessage: VoiceMessage) {}

    /**
     * Called when preview playback completes.
     * @param voiceMessage The voice message that finished playing
     */
    fun onPreviewCompleted(voiceMessage: VoiceMessage) {}

    /**
     * Called when a voice message is marked as ready to send.
     * @param voiceMessage The voice message ready to be sent
     */
    fun onReadyToSend(voiceMessage: VoiceMessage) {}

    /**
     * Called when an error occurs during any voice message operation.
     * @param error The exception that occurred
     */
    fun onError(error: VoiceRecorderException) {}
} 