package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun CropperDialog(
    imagePath: String,
    onDismiss: () -> Unit,
    onCropComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(imagePath) {
        try {
            BitmapFactory.decodeFile(imagePath)
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Error") },
            text = { Text("Failed to load image for cropping.") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        )
        return
    }
    // State fraction bounds
    var leftFraction by remember { mutableStateOf(0.15f) }
    var topFraction by remember { mutableStateOf(0.15f) }
    var rightFraction by remember { mutableStateOf(0.85f) }
    var bottomFraction by remember { mutableStateOf(0.85f) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header: Cancel (left) - Title (center) - Apply (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Text(
                        text = "Crop Image",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Button(
                        onClick = {
                            try {
                                val imgW = bitmap.width
                                val imgH = bitmap.height
                                val xLeft = (leftFraction * imgW).toInt().coerceIn(0, imgW - 1)
                                val yTop = (topFraction * imgH).toInt().coerceIn(0, imgH - 1)
                                val xRight = (rightFraction * imgW).toInt().coerceIn(0, imgW)
                                val yBottom = (bottomFraction * imgH).toInt().coerceIn(0, imgH)
                                val cropW = (xRight - xLeft).coerceAtLeast(10)
                                val cropH = (yBottom - yTop).coerceAtLeast(10)
                                val croppedBitmap = Bitmap.createBitmap(
                                    bitmap,
                                    xLeft,
                                    yTop,
                                    cropW,
                                    cropH
                                )
                                val filename = "cropped_" + System.currentTimeMillis() + ".png"
                                val file = File(context.filesDir, filename)
                                FileOutputStream(file).use { out ->
                                    croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                onCropComplete(file.absolutePath)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold)
                    }
                }
                // Interactive Cropper Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                    // Track local canvas dimensions
                    var canvasSize by remember { mutableStateOf(Size.Zero) }
                    // Pointer gesture state to track what's currently being dragged
                    // 0 = none, 1 = top-left corner, 2 = top-right, 3 = bottom-left, 4 = bottom-right, 5 = body
                    var dragMode by remember { mutableStateOf(0) }
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(canvasSize) {
                                if (canvasSize == Size.Zero) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val canvasW = canvasSize.width
                                        val canvasH = canvasSize.height
                                        if (canvasW <= 0f || canvasH <= 0f) return@detectDragGestures
                                        val imgW = bitmap.width.toFloat()
                                        val imgH = bitmap.height.toFloat()
                                        val scale = minOf(canvasW / imgW, canvasH / imgH)
                                        val dstW = imgW * scale
                                        val dstH = imgH * scale
                                        val dstX = (canvasW - dstW) / 2f
                                        val dstY = (canvasH - dstH) / 2f
                                        val lX = dstX + leftFraction * dstW
                                        val rX = dstX + rightFraction * dstW
                                        val tY = dstY + topFraction * dstH
                                        val bY = dstY + bottomFraction * dstH
                                        val touchThreshold = 48.dp.toPx()
                                        dragMode = when {
                                            // Check Corners
                                            Offset(offset.x - lX, offset.y - tY).getDistance() < touchThreshold -> 1
                                            Offset(offset.x - rX, offset.y - tY).getDistance() < touchThreshold -> 2
                                            Offset(offset.x - lX, offset.y - bY).getDistance() < touchThreshold -> 3
                                            Offset(offset.x - rX, offset.y - bY).getDistance() < touchThreshold -> 4
                                            // Check Body
                                            offset.x >= lX && offset.x <= rX && offset.y >= tY && offset.y <= bY -> 5
                                            else -> 0
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val canvasW = canvasSize.width
                                        val canvasH = canvasSize.height
                                        if (canvasW <= 0f || canvasH <= 0f) return@detectDragGestures
                                        val imgW = bitmap.width.toFloat()
                                        val imgH = bitmap.height.toFloat()
                                        val scale = minOf(canvasW / imgW, canvasH / imgH)
                                        val dstW = imgW * scale
                                        val dstH = imgH * scale
                                        if (dstW <= 0f || dstH <= 0f) return@detectDragGestures
                                        val dx = dragAmount.x / dstW
                                        val dy = dragAmount.y / dstH
                                        when (dragMode) {
                                            1 -> { // Top-Left
                                                leftFraction = max(0f, min(rightFraction - 0.1f, leftFraction + dx))
                                                topFraction = max(0f, min(bottomFraction - 0.1f, topFraction + dy))
                                            }
                                            2 -> { // Top-Right
                                                rightFraction = min(1f, max(leftFraction + 0.1f, rightFraction + dx))
                                                topFraction = max(0f, min(bottomFraction - 0.1f, topFraction + dy))
                                            }
                                            3 -> { // Bottom-Left
                                                leftFraction = max(0f, min(rightFraction - 0.1f, leftFraction + dx))
                                                bottomFraction = min(1f, max(topFraction + 0.1f, bottomFraction + dy))
                                            }
                                            4 -> { // Bottom-Right
                                                rightFraction = min(1f, max(leftFraction + 0.1f, rightFraction + dx))
                                                bottomFraction = min(1f, max(topFraction + 0.1f, bottomFraction + dy))
                                            }
                                            5 -> { // Body movement
                                                val w = rightFraction - leftFraction
                                                val h = bottomFraction - topFraction
                                                val nL = leftFraction + dx
                                                val nT = topFraction + dy
                                                if (nL >= 0f && nL + w <= 1f) {
                                                    leftFraction = nL
                                                    rightFraction = nL + w
                                                }
                                                if (nT >= 0f && nT + h <= 1f) {
                                                    topFraction = nT
                                                    bottomFraction = nT + h
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        dragMode = 0
                                    }
                                )
                            }
                    ) {
                        canvasSize = size
                        val canvasW = size.width
                        val canvasH = size.height
                        // Draw Image in aspect-fit
                        val imgW = imageBitmap.width.toFloat()
                        val imgH = imageBitmap.height.toFloat()
                        val scale = minOf(canvasW / imgW, canvasH / imgH)
                        val dstW = imgW * scale
                        val dstH = imgH * scale
                        val dstX = (canvasW - dstW) / 2f
                        val dstY = (canvasH - dstH) / 2f
                        // Draw image fit in center
                        drawImage(
                            image = imageBitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(dstX.toInt(), dstY.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(dstW.toInt(), dstH.toInt())
                        )
                        // Relative coordinates inside the actual image container area
                        val activeLeft = dstX + leftFraction * dstW
                        val activeTop = dstY + topFraction * dstH
                        val activeRight = dstX + rightFraction * dstW
                        val activeBottom = dstY + bottomFraction * dstH
                        // Draw dim overlay over background safely around the selection
                        val dimColor = Color.Black.copy(alpha = 0.6f)
                        // 1. Top dim rect
                        if (activeTop > 0f) {
                            drawRect(
                                color = dimColor,
                                topLeft = Offset(0f, 0f),
                                size = Size(canvasW, activeTop)
                            )
                        }
                        // 2. Left dim rect
                        if (activeLeft > 0f && activeBottom > activeTop) {
                            drawRect(
                                color = dimColor,
                                topLeft = Offset(0f, activeTop),
                                size = Size(activeLeft, activeBottom - activeTop)
                            )
                        }
                        // 3. Right dim rect
                        if (canvasW - activeRight > 0f && activeBottom > activeTop) {
                            drawRect(
                                color = dimColor,
                                topLeft = Offset(activeRight, activeTop),
                                size = Size(canvasW - activeRight, activeBottom - activeTop)
                            )
                        }
                        // 4. Bottom dim rect
                        if (canvasH - activeBottom > 0f) {
                            drawRect(
                                color = dimColor,
                                topLeft = Offset(0f, activeBottom),
                                size = Size(canvasW, canvasH - activeBottom)
                            )
                        }
                        // Draw frame borders
                        drawRect(
                            color = Color.Yellow,
                            topLeft = Offset(activeLeft, activeTop),
                            size = Size(activeRight - activeLeft, activeBottom - activeTop),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        // Draw corner anchors
                        val anchorW = 16.dp.toPx()
                        // Top-left
                        drawRect(Color.Yellow, topLeft = Offset(activeLeft - 4f, activeTop - 4f), size = Size(anchorW, 6f))
                        drawRect(Color.Yellow, topLeft = Offset(activeLeft - 4f, activeTop - 4f), size = Size(6f, anchorW))
                        // Top-right
                        drawRect(Color.Yellow, topLeft = Offset(activeRight - anchorW + 4f, activeTop - 4f), size = Size(anchorW, 6f))
                        drawRect(Color.Yellow, topLeft = Offset(activeRight - 6f, activeTop - 4f), size = Size(6f, anchorW))
                        // Bottom-left
                        drawRect(Color.Yellow, topLeft = Offset(activeLeft - 4f, activeBottom - 6f), size = Size(anchorW, 6f))
                        drawRect(Color.Yellow, topLeft = Offset(activeLeft - 4f, activeBottom - anchorW + 4f), size = Size(6f, anchorW))
                        // Bottom-right
                        drawRect(Color.Yellow, topLeft = Offset(activeRight - anchorW + 4f, activeBottom - 6f), size = Size(anchorW, 6f))
                        drawRect(Color.Yellow, topLeft = Offset(activeRight - 6f, activeBottom - anchorW + 4f), size = Size(6f, anchorW))
                    }
                }
            }
        }
    }
}
