package com.rapido.voicemessagesdk.core

/**
 * Represents the current status of a voice message throughout its lifecycle.
 */
enum class VoiceMessageStatus {
    /** Voice message is currently being recorded */
    RECORDING,
    
    /** Recording completed, in preview mode */
    PREVIEW,
    
    /** Voice message is ready to be sent */
    READY_TO_SEND,
    
    /** Voice message is being uploaded/sent */
    SENDING,
    
    /** Voice message has been successfully sent */
    SENT,
    
    /** Sending failed */
    SEND_FAILED
} 