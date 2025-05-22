package com.rapido.chat.ui.utils

/**
 * Formats a duration in milliseconds to a MM:SS string.
 *
 * @param durationMs Duration in milliseconds
 * @return Formatted duration string in the format MM:SS
 */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}