package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScanner(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    isScanning: Boolean = true,
    radarColor: Color = NeonCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isScanning) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rotation"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse"
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension / 2f

        // Outer circle
        drawCircle(
            color = radarColor.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Middle circle
        drawCircle(
            color = radarColor.copy(alpha = 0.2f),
            radius = radius * 0.66f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Inner circle
        drawCircle(
            color = radarColor.copy(alpha = 0.15f),
            radius = radius * 0.33f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Crosshairs
        drawLine(
            color = radarColor.copy(alpha = 0.15f),
            start = Offset(center.x, 0f),
            end = Offset(center.x, this.size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = radarColor.copy(alpha = 0.15f),
            start = Offset(0f, center.y),
            end = Offset(this.size.width, center.y),
            strokeWidth = 1.dp.toPx()
        )

        // Pulse wave
        if (isScanning) {
            drawCircle(
                color = radarColor.copy(alpha = (1f - pulseRadius) * 0.4f),
                radius = radius * pulseRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Sweep radar beam
            val angleRad = Math.toRadians(rotationAngle.toDouble())
            val endX = center.x + radius * cos(angleRad).toFloat()
            val endY = center.y + radius * sin(angleRad).toFloat()

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        radarColor.copy(alpha = 0.9f),
                        radarColor.copy(alpha = 0.1f)
                    ),
                    start = center,
                    end = Offset(endX, endY)
                ),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 2.5.dp.toPx()
            )
        }

        // Center dot
        drawCircle(
            color = radarColor,
            radius = 3.dp.toPx(),
            center = center
        )
    }
}
