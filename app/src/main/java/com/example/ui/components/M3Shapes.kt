package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CustomDotIcon(
    shape: String,
    color: Color,
    text: String,
    size: Dp = 48.dp,
    textColor: Color = Color.White,
    fontSize: Float = 9f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            
            val strokeColor = Color.White.copy(alpha = 0.7f)
            val strokeWidthPx = 1.5.dp.toPx()
            
            when (shape.lowercase()) {
                "none" -> {
                    // Do not draw any background, shape or outline
                }
                "thinrectangle" -> {
                    val path = Path().apply {
                        val narrowHalfW = w * 0.22f
                        moveTo(cx - narrowHalfW, 4f + strokeWidthPx / 2f)
                        lineTo(cx + narrowHalfW, 4f + strokeWidthPx / 2f)
                        lineTo(cx + narrowHalfW, h - 4f - strokeWidthPx / 2f)
                        lineTo(cx - narrowHalfW, h - 4f - strokeWidthPx / 2f)
                        close()
                    }
                    drawPath(path, color = color, style = Fill)
                    drawPath(path, color = strokeColor, style = Stroke(width = strokeWidthPx))
                }
                "triangle" -> {
                    val path = Path().apply {
                        moveTo(cx, 4f + strokeWidthPx / 2f)
                        lineTo(w - 4f - strokeWidthPx / 2f, h - 4f - strokeWidthPx / 2f)
                        lineTo(4f + strokeWidthPx / 2f, h - 4f - strokeWidthPx / 2f)
                        close()
                    }
                    drawPath(path, color = color, style = Fill)
                    drawPath(path, color = strokeColor, style = Stroke(width = strokeWidthPx))
                }
                "hexagon" -> {
                    val path = Path().apply {
                        val radius = minOf(w, h) / 2f - 4f - strokeWidthPx / 2f
                        for (i in 0 until 6) {
                            // Rotate by 90 deg so it points up
                            val angle = i * Math.PI / 3f - Math.PI / 2f
                            val x = cx + radius * cos(angle).toFloat()
                            val y = cy + radius * sin(angle).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(path, color = color, style = Fill)
                    drawPath(path, color = strokeColor, style = Stroke(width = strokeWidthPx))
                }
                "star" -> {
                    val path = Path().apply {
                        val outerRadius = minOf(w, h) / 2f - 2f - strokeWidthPx / 2f
                        val innerRadius = outerRadius * 0.45f
                        var angle = -Math.PI / 2f
                        val increment = Math.PI / 5f
                        for (i in 0 until 10) {
                            val r = if (i % 2 == 0) outerRadius else innerRadius
                            val x = cx + r * cos(angle).toFloat()
                            val y = cy + r * sin(angle).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                            angle += increment
                        }
                        close()
                    }
                    drawPath(path, color = color, style = Fill)
                    drawPath(path, color = strokeColor, style = Stroke(width = strokeWidthPx))
                }
                "roundedsquare" -> {
                    // Draw soft rounded square
                    val shrink = strokeWidthPx / 2f
                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(shrink, shrink),
                        size = androidx.compose.ui.geometry.Size(w - strokeWidthPx, h - strokeWidthPx),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = androidx.compose.ui.geometry.Offset(shrink, shrink),
                        size = androidx.compose.ui.geometry.Size(w - strokeWidthPx, h - strokeWidthPx),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        style = Stroke(width = strokeWidthPx)
                    )
                }
                else -> {
                    // Default is Circle
                    val r = minOf(w, h) / 2f - 2f - strokeWidthPx / 2f
                    drawCircle(color = color, radius = r)
                    drawCircle(color = strokeColor, radius = r, style = Stroke(width = strokeWidthPx))
                }
            }
        }
        
        // Let's render the customized text in center
        if (text.isNotEmpty()) {
            Text(
                text = text,
                color = textColor,
                fontSize = fontSize.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
                maxLines = 4
            )
        }
    }
}
