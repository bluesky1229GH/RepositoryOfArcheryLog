package com.example.archerylog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.archerylog.data.EndWithShots
import com.example.archerylog.data.Shot

@Composable
fun TargetFace(
    ends: List<EndWithShots>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onTap: ((x: Float, y: Float, normalizedArrowRadius: Float) -> Unit)? = null
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .pointerInput(onTap, enabled) {
                if (onTap != null && enabled) {
                    detectTapGestures { offset ->
                        val radius = size.width / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val nx = (offset.x - center.x) / radius
                        val ny = (offset.y - center.y) / radius
                        val normalizedArrowRadius = 5.dp.toPx() / radius
                        onTap(nx, ny, normalizedArrowRadius)
                    }
                }
            }
    ) {
        val radius = size.width / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        
        val colors = listOf(
            Color.White,
            Color.Black,
            Color(0xFF42A5F5), // Blue
            Color(0xFFEF5350), // Red
            Color(0xFFFFD54F)  // Yellow
        )
        
        for (i in 1..10) {
            val ringRadius = radius * (11 - i) / 10f
            val colorIndex = (i - 1) / 2
            drawCircle(
                color = colors[colorIndex],
                radius = ringRadius,
                center = center
            )
            drawCircle(
                color = if (colorIndex == 1) Color.White else Color.Black,
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        drawCircle(
            color = Color.Black,
            radius = radius * 0.05f,
            center = center,
            style = Stroke(width = 0.5.dp.toPx())
        )
        
        drawLine(Color.Black, start = Offset(center.x - 5f, center.y), end = Offset(center.x + 5f, center.y), strokeWidth = 1f)
        drawLine(Color.Black, start = Offset(center.x, center.y - 5f), end = Offset(center.x, center.y + 5f), strokeWidth = 1f)

        ends.forEach { endWithShots ->
            val endNumber = endWithShots.end.endNumber
            val dotColor = getEndMarkerColor(endNumber)
            
            endWithShots.shots.forEach { shot ->
                if (shot.x != null && shot.y != null) {
                    val sx = center.x + shot.x * radius
                    val sy = center.y + shot.y * radius
                    drawCircle(
                        color = dotColor,
                        radius = 5.dp.toPx(),
                        center = Offset(sx, sy)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 5.dp.toPx(),
                        center = Offset(sx, sy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
        
        // Draw locked overlay if disabled
        if (!enabled) {
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = radius,
                center = center
            )
            // Optional: Draw a subtle ring to highlight the lock
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = radius * 0.95f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

fun getEndMarkerColor(endNumber: Int): Color {
    val markerColors = listOf(
        Color(0xFF00E676), // Green
        Color(0xFFFF1744), // Red
        Color(0xFF2979FF), // Blue
        Color(0xFFFF9100), // Orange
        Color(0xFFD500F9), // Purple
        Color(0xFF00E5FF)  // Cyan
    )
    return markerColors[(endNumber - 1).coerceAtLeast(0) % markerColors.size]
}

fun getScoreBackgroundColor(score: String): Color {
    return when(score) {
        "X", "10", "9" -> Color(0xFFFFD54F) // Yellow
        "8", "7" -> Color(0xFFEF5350) // Red
        "6", "5" -> Color(0xFF42A5F5) // Blue
        "4", "3" -> Color.Black
        "2", "1" -> Color.White
        "M" -> Color.LightGray
        else -> Color.Transparent
    }
}

fun getScoreTextColor(score: String): Color {
    return when(score) {
        "X", "10", "9" -> Color.Black
        "8", "7" -> Color.White
        "6", "5" -> Color.White
        "4", "3" -> Color.White
        "2", "1" -> Color.Black
        "M" -> Color.Black
        else -> Color.Black
    }
}
