package com.rapido.chat.integration.viewmodel

/**
 * Represents the different actions that can be performed in the chat UI.
 */
sealed class ChatAction {

    data class SendTextMessage(val content: String) : ChatAction()

    data object StartVoiceMessage : ChatAction()

    data object DeleteVoiceMessage : ChatAction()

    data object FinishVoiceMessage : ChatAction()

    data class PlayVoiceMessage(val messageId: String) : ChatAction()

    data class PauseVoiceMessage(val messageId: String) : ChatAction()

    data class ResumeVoiceMessage(val messageId: String) : ChatAction()

    data class StopVoiceMessage(val messageId: String) : ChatAction()

    data class DeleteMessage(val messageId: String) : ChatAction()
} 