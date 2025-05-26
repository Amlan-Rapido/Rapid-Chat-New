package com.rapido.chat.integration.viewmodel

import com.rapido.voice_recorder.RecordedAudio

/**
 * Represents the different actions that can be performed in the chat UI.
 */
sealed class ChatAction {

    data class SendTextMessage(val text: String) : ChatAction()

    data object StartVoiceMessage : ChatAction()

    data object DeleteVoiceMessage : ChatAction()

    data object FinishVoiceMessage : ChatAction()

    data object SendVoiceMessage : ChatAction()

    sealed class PlayVoiceMessage : ChatAction() {
        data class FromMessage(val messageId: String) : PlayVoiceMessage()
        data class FromPreview(val audio: RecordedAudio) : PlayVoiceMessage()
    }

    data object PauseVoiceMessage : ChatAction()

    data object ResumeVoiceMessage : ChatAction()

    data object StopVoiceMessage : ChatAction()

    data class DeleteMessage(val messageId: String) : ChatAction()
} 