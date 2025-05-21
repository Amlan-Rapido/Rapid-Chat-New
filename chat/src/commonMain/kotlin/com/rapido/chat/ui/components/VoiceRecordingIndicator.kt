package com.rapido.chat.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * A component that shows a visual indicator when voice recording is in progress.
 *
 * @param durationMs The current duration of the recording in milliseconds
 * @param onCancelClick Callback when the cancel button is clicked
 */
@Composable
fun VoiceRecordingIndicator(
    durationMs: Long,
    onCancelClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recording waveform animation
            WaveformAnimation(
                modifier = Modifier
                    .width(100.dp)
                    .height(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Recording duration
            Text(
                text = formatDuration(durationMs),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Cancel button
            IconButton(
                onClick = onCancelClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Cancel recording",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun WaveformAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveformTransition")
    val lineColor = MaterialTheme.colorScheme.error
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveformAnimation"
    )
    
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        // Draw multiple bars with varied heights to simulate a waveform
        val barCount = 10
        val barWidth = canvasWidth / (barCount * 2f)
        
        for (i in 0 until barCount) {
            val phase = (i.toFloat() / barCount + animationProgress) % 1f
            val amplitude = sin(phase * 2 * 3.14) * 0.5f + 0.5f
            val height = canvasHeight * amplitude
            
            val x = i * (barWidth * 2) + barWidth / 2
            val startY = (canvasHeight - height) / 2
            val endY = startY + height
            
            drawLine(
                color = lineColor,
                start = Offset(x, startY.toFloat()),
                end = Offset(x, endY.toFloat()),
                strokeWidth = barWidth * 0.8f,
                cap = StrokeCap.Round
            )
        }
    }
} 