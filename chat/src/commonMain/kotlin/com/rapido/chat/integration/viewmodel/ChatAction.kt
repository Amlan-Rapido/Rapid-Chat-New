package com.rapido.chat.integration.viewmodel

import com.rapido.voicemessagesdk.core.VoiceMessage
import com.rapido.voicemessagesdk.ui.VoiceMessageData


/**
 * Represents the different actions that can be performed in the chat UI.
 */
sealed class ChatAction {

    data class SendTextMessage(val text: String) : ChatAction()

    // NEW SIMPLIFIED VOICE MESSAGE ACTION
    data class SendVoiceMessageData(val voiceMessageData: VoiceMessageData) : ChatAction()

    // VOICE PLAYBACK ACTIONS (still needed for playing sent messages)
    sealed class PlayVoiceMessage : ChatAction() {
        data class FromMessage(val messageId: String) : PlayVoiceMessage()
        data class FromPreview(val audio: VoiceMessage) : PlayVoiceMessage()
    }

    data object PauseVoiceMessage : ChatAction()
    data object ResumeVoiceMessage : ChatAction()
    data object StopVoiceMessage : ChatAction()

    // MESSAGE MANAGEMENT
    data class DeleteMessage(val messageId: String) : ChatAction()
} 