package com.rapido.voicemessagesdk.core

import kotlinx.datetime.Clock

data class VoiceMessage(
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val id: String = generateId(),
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val messageId: String? = null  // For chat integration when message is sent
) {
    companion object {
        private fun generateId(): String {
            // Generate a simple unique ID based on timestamp and random value
            return "${Clock.System.now().toEpochMilliseconds()}-${(0..9999).random()}"
        }
    }
} 