package com.rapido.voice_recorder

data class RecordedAudio(
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long
)
