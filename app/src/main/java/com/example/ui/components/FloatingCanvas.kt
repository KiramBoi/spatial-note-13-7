@file:Suppress("DEPRECATION")
package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import coil.compose.AsyncImage
import com.example.data.model.CopyBox
import com.example.data.model.PictureItem
import java.io.File

@Composable
fun FloatingCanvas(
    copyBoxes: List<CopyBox>,
    pictures: List<PictureItem>,
    onUpdateCopyBox: (CopyBox) -> Unit,
    onDeleteCopyBox: (String) -> Unit,
    onUpdatePicture: (PictureItem) -> Unit,
    onDeletePicture: (String) -> Unit,
    onSelectCropPicture: (PictureItem) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF0F101A),
    showGrid: Boolean = true,
    scrollOffset: Int = 0,
    isViewingMode: Boolean = false
) {
    val context = LocalContext.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val maxWidthDp = with(density) { maxWidthPx.toDp().value }
        val maxHeightDp = with(density) { maxHeightPx.toDp().value }
        val scrollOffsetDp = with(density) { scrollOffset.toDp().value }
        
        // Grid lines drawing helper to make it look like a high-fidelity workspace
        if (showGrid && !isViewingMode) {
            CanvasGridPattern()
        }
        
        // 1. Render all Copy Boxes
        copyBoxes.forEach { box ->
            BoxItemWrapper(
                id = box.id,
                x = box.x,
                y = box.y,
                scrollOffsetDp = scrollOffsetDp,
                isMinimized = box.isMinimized,
                dotColor = Color(box.colorArgb),
                dotTextColor = Color(box.textColorArgb),
                dotShape = box.dotShape,
                dotText = box.dotText,
                dotSize = box.dotSize,
                dotFontSize = box.dotFontSize,
                isViewingMode = isViewingMode,
                onMove = { dx, dy ->
                    if (!isViewingMode) {
                        onUpdateCopyBox(
                            box.copy(
                                x = (box.x + dx).coerceIn(0f, maxWidthDp - 60f),
                                y = (box.y + dy).coerceIn(0f, maxHeightDp + scrollOffsetDp - 60f)
                            )
                        )
                    }
                },
                onTapDot = {
                    if (box.isUrl) {
                        openBrowserLink(box.url, context)
                    } else {
                        copyToClipboard(box.text, context)
                    }
                },
                onLongTapDot = {
                    if (!isViewingMode) {
                        onUpdateCopyBox(box.copy(isMinimized = false))
                    }
                }
            ) {
                MaximizedCopyBoxView(
                    box = box,
                    context = context,
                    onTextChanged = { newText ->
                        if (!isViewingMode) {
                            onUpdateCopyBox(box.copy(text = newText))
                        }
                    },
                    onMove = { dx, dy ->
                        if (!isViewingMode) {
                            onUpdateCopyBox(
                                box.copy(
                                    x = (box.x + dx).coerceIn(0f, maxWidthDp - 60f),
                                    y = (box.y + dy).coerceIn(0f, maxHeightDp + scrollOffsetDp - 60f)
                                )
                            )
                        }
                    },
                    onMinimize = {
                        if (!isViewingMode) {
                            onUpdateCopyBox(box.copy(isMinimized = true))
                        }
                    },
                    onDelete = {
                        if (!isViewingMode) {
                            onDeleteCopyBox(box.id)
                        }
                    },
                    onCustomize = { color, textColor, shape, text, payload, size, fontSize, isUrl, url ->
                        if (!isViewingMode) {
                            onUpdateCopyBox(
                                box.copy(
                                    colorArgb = color.toArgb(),
                                    textColorArgb = textColor.toArgb(),
                                    dotShape = shape,
                                    dotText = text,
                                    text = payload,
                                    dotSize = size,
                                    dotFontSize = fontSize,
                                    isUrl = isUrl,
                                    url = url
                                )
                            )
                        }
                    },
                    onResize = { dw, dh ->
                        if (!isViewingMode) {
                            onUpdateCopyBox(
                                box.copy(
                                    width = (box.width + dw).coerceIn(120f, maxWidthDp - box.x),
                                    height = (box.height + dh).coerceIn(100f, maxHeightDp - box.y)
                                )
                            )
                        }
                    },
                    isViewingMode = isViewingMode
                )
            }
        }
        
        // 2. Render all Pictures - always Maximized!
        pictures.forEach { pic ->
            BoxItemWrapper(
                id = pic.id,
                x = pic.x,
                y = pic.y,
                scrollOffsetDp = scrollOffsetDp,
                isMinimized = false,
                dotColor = Color(pic.colorArgb),
                dotTextColor = Color(pic.textColorArgb),
                dotShape = pic.dotShape,
                dotText = pic.dotText,
                dotSize = pic.dotSize,
                isViewingMode = isViewingMode,
                onMove = { _, _ -> },
                onTapDot = {},
                onLongTapDot = {}
            ) {
                MaximizedPictureView(
                    picture = pic,
                    onMove = { dx, dy ->
                        if (!isViewingMode) {
                            onUpdatePicture(
                                pic.copy(
                                    x = (pic.x + dx).coerceIn(0f, maxWidthDp - 60f),
                                    y = (pic.y + dy).coerceIn(0f, maxHeightDp + scrollOffsetDp - 60f)
                                )
                            )
                        }
                    },
                    onDelete = {
                        if (!isViewingMode) {
                            onDeletePicture(pic.id)
                        }
                    },
                    onCrop = {
                        if (!isViewingMode) {
                            onSelectCropPicture(pic)
                        }
                    },
                    onResize = { dw, dh ->
                        if (!isViewingMode) {
                            onUpdatePicture(
                                pic.copy(
                                    width = (pic.width + dw).coerceIn(120f, maxWidthDp - pic.x),
                                    height = (pic.height + dh).coerceIn(100f, maxHeightDp + scrollOffsetDp - pic.y)
                                )
                            )
                        }
                    },
                    isViewingMode = isViewingMode
                )
            }
        }
    }
}

// Global clipboard helper
fun copyToClipboard(text: String, context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Copied Note text", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
}

fun openBrowserLink(url: String, context: Context) {
    try {
        var formattedUrl = url.trim()
        if (formattedUrl.isEmpty()) {
            Toast.makeText(context, "URL is empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(formattedUrl))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Invalid URL web link", Toast.LENGTH_SHORT).show()
    }
}

// Convert Color object to ARGB Int format
fun Color.toArgb(): Int {
    return (this.value shr 32).toInt()
}

@Composable
fun CanvasGridPattern() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val intervalPx = 32.dp.toPx()
        val gridColor = Color(0xFF1E1F30).copy(alpha = 0.4f)
        
        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += intervalPx
        }
        
        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += intervalPx
        }
    }
}

@Composable
fun BoxItemWrapper(
    id: String,
    x: Float,
    y: Float,
    scrollOffsetDp: Float = 0f,
    isMinimized: Boolean,
    dotColor: Color,
    dotTextColor: Color,
    dotShape: String,
    dotText: String,
    dotSize: Float = 48f,
    dotFontSize: Float = 9f,
    isViewingMode: Boolean = false,
    onMove: (Float, Float) -> Unit,
    onTapDot: () -> Unit,
    onLongTapDot: () -> Unit,
    content: @Composable () -> Unit
) {
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnTapDot by rememberUpdatedState(onTapDot)
    val currentOnLongTapDot by rememberUpdatedState(onLongTapDot)
    if (isMinimized) {
        // Rendered as Dot shape
        Box(
            modifier = Modifier
                .offset { IntOffset(x.dp.roundToPx(), (y - scrollOffsetDp).dp.roundToPx()) }
                .then(
                    if (isViewingMode) {
                        Modifier.pointerInput(id) {
                            detectTapGestures(
                                onTap = { currentOnTapDot() }
                            )
                        }
                    } else {
                        Modifier
                            .pointerInput(id) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentOnMove(dragAmount.x.toDp().value, dragAmount.y.toDp().value)
                                    }
                                )
                            }
                            .pointerInput(id) {
                                detectTapGestures(
                                    onTap = { currentOnTapDot() },
                                    onLongPress = { currentOnLongTapDot() }
                                )
                            }
                    }
                )
        ) {
            CustomDotIcon(
                shape = dotShape,
                color = dotColor,
                text = dotText,
                size = dotSize.dp,
                textColor = dotTextColor,
                fontSize = dotFontSize
            )
        }
    } else {
        // Maximized view box
        Box(
            modifier = Modifier
                .offset { IntOffset(x.dp.roundToPx(), (y - scrollOffsetDp).dp.roundToPx()) }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaximizedCopyBoxView(
    box: CopyBox,
    context: Context,
    onTextChanged: (String) -> Unit,
    onMove: (Float, Float) -> Unit,
    onMinimize: () -> Unit,
    onDelete: () -> Unit,
    onCustomize: (Color, Color, String, String, String, Float, Float, Boolean, String) -> Unit,
    onResize: (Float, Float) -> Unit,
    isViewingMode: Boolean = false
) {
    var showCustomizationDialog by remember { mutableStateOf(false) }
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnTextChanged by rememberUpdatedState(onTextChanged)
    val currentOnMinimize by rememberUpdatedState(onMinimize)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnCustomize by rememberUpdatedState(onCustomize)
    val currentOnResize by rememberUpdatedState(onResize)
    Card(
        modifier = Modifier
            .size(width = box.width.dp, height = box.height.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isViewingMode) Color.Transparent else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isViewingMode) 0.dp else 8.dp),
        border = if (isViewingMode) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isViewingMode) {
                // Drag and control header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .pointerInput(box.id) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentOnMove(dragAmount.x.toDp().value, dragAmount.y.toDp().value)
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Drag icon indication
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "Hold and drag header to move",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Copy Box",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Copy text click
                        IconButton(
                            onClick = {
                                if (box.isUrl) {
                                    openBrowserLink(box.url, context)
                                } else {
                                    copyToClipboard(box.text, context)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (box.isUrl) Icons.Default.OpenInNew else Icons.Default.ContentCopy,
                                contentDescription = if (box.isUrl) "Open URL Link" else "Copy content",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        // Dot Customize Settings Option
                        IconButton(
                            onClick = { showCustomizationDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = "Customize Dot",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        // Minimize icon
                        IconButton(
                            onClick = currentOnMinimize,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Minimize,
                                contentDescription = "Minimize box",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        // Delete icon
                        IconButton(
                            onClick = currentOnDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete box",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }
            // Content text area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                if (isViewingMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (box.isUrl) {
                                    openBrowserLink(box.url, context)
                                } else {
                                    copyToClipboard(box.text, context)
                                    Toast.makeText(context, "Copied content!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = box.text.ifEmpty { "Empty Copy Box" },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    TextField(
                        value = box.text,
                        onValueChange = currentOnTextChanged,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Type contents...", fontSize = 13.sp) },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    // Drag to resize handle located at bottom-right corner
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .pointerInput(box.id) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentOnResize(dragAmount.x.toDp().value, dragAmount.y.toDp().value)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SouthEast,
                            contentDescription = "Resize",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
    if (showCustomizationDialog && !isViewingMode) {
        CustomizeDotDialog(
            currentColor = Color(box.colorArgb),
            currentTextColor = Color(box.textColorArgb),
            currentShape = box.dotShape,
            currentText = box.dotText,
            currentDotSize = box.dotSize,
            currentDotFontSize = box.dotFontSize,
            isCopyBox = true,
            currentIsUrl = box.isUrl,
            currentUrl = box.url,
            currentPayload = box.text,
            onDismiss = { showCustomizationDialog = false },
            onSave = { color, textColor, shape, text, payload, size, fontSize, isUrl, url ->
                currentOnCustomize(color, textColor, shape, text, payload, size, fontSize, isUrl, url)
                showCustomizationDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaximizedPictureView(
    picture: PictureItem,
    onMove: (Float, Float) -> Unit,
    onDelete: () -> Unit,
    onCrop: () -> Unit,
    onResize: (Float, Float) -> Unit,
    isViewingMode: Boolean = false
) {
    var showCustomizationDialog by remember { mutableStateOf(false) }
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnCrop by rememberUpdatedState(onCrop)
    val currentOnResize by rememberUpdatedState(onResize)
    Box(
        modifier = Modifier
            .size(width = picture.width.dp, height = picture.height.dp)
            .then(
                if (isViewingMode) {
                    Modifier
                } else {
                    Modifier
                        .combinedClickable(
                            onLongClick = { showCustomizationDialog = true },
                            onClick = {}
                        )
                        .pointerInput(picture.id) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    currentOnMove(dragAmount.x.toDp().value, dragAmount.y.toDp().value)
                                }
                            )
                        }
                }
            )
    ) {
        val imageFile = File(picture.localUri)
        if (imageFile.exists()) {
            AsyncImage(
                model = imageFile,
                contentDescription = "Uploaded image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder
            Box(
                modifier = Modifier.fillMaxSize().background(Color.DarkGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Image not found",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (!isViewingMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .pointerInput(picture.id) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnResize(dragAmount.x.toDp().value, dragAmount.y.toDp().value)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Resize Picture",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
    if (showCustomizationDialog && !isViewingMode) {
        AlertDialog(
            onDismissRequest = { showCustomizationDialog = false },
            title = { Text("Customize Image") },
            text = { Text("Choose an action to perform on this image item.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentOnCrop()
                        showCustomizationDialog = false
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Crop Picture")
                    }
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            currentOnDelete()
                            showCustomizationDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                    TextButton(onClick = { showCustomizationDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun CustomizeDotDialog(
    currentColor: Color,
    currentTextColor: Color,
    currentShape: String,
    currentText: String,
    currentDotSize: Float = 48f,
    currentDotFontSize: Float = 9f,
    isCopyBox: Boolean = false,
    currentIsUrl: Boolean = false,
    currentUrl: String = "",
    currentPayload: String = "",
    onDismiss: () -> Unit,
    onSave: (Color, Color, String, String, String, Float, Float, Boolean, String) -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }
    var selectedTextColor by remember { mutableStateOf(currentTextColor) }
    var selectedShape by remember { mutableStateOf(currentShape) }
    var customTextState by remember { mutableStateOf(currentText) }
    var selectedDotSize by remember { mutableStateOf(currentDotSize) }
    var selectedDotFontSize by remember { mutableStateOf(currentDotFontSize) }
    var selectedIsUrl by remember { mutableStateOf(currentIsUrl) }
    var selectedUrl by remember { mutableStateOf(currentUrl) }
    var payloadState by remember { mutableStateOf(currentPayload) }
    val colorPalette = listOf(
        Color(0xFFEF5350), // Red
        Color(0xFFBA68C8), // Violet
        Color(0xFF64B5F6), // Blue
        Color(0xFF4DB6AC), // Cyan
        Color(0xFF81C784), // Green
        Color(0xFFFFF176), // Yellow
        Color(0xFFFFB74D), // Orange
        Color(0xFFE0E0E0), // Gray
        Color(0xFF90A4AE) // BlueGray
    )
    val shapesList = listOf(
        "Circle",
        "RoundedSquare",
        "Star",
        "ThinRectangle",
        "None"
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Dot Form Customizer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Render real-time preview of the Dot
                Text("Real-time preview", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CustomDotIcon(
                        shape = selectedShape,
                        color = selectedColor,
                        text = customTextState,
                        size = selectedDotSize.dp,
                        textColor = selectedTextColor,
                        fontSize = selectedDotFontSize
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Text Input
                OutlinedTextField(
                    value = customTextState,
                    onValueChange = { if (it.length <= 100) customTextState = it },
                    label = { Text("Dot form text (Max 100 Chars / 20 Words)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (isCopyBox) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = payloadState,
                        onValueChange = { payloadState = it },
                        label = { Text("Text to copy (payload)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
                if (isCopyBox) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIsUrl = !selectedIsUrl }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIsUrl,
                            onCheckedChange = { selectedIsUrl = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Link option upon tap",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    if (selectedIsUrl) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = selectedUrl,
                            onValueChange = { selectedUrl = it },
                            label = { Text("URL Link Website") },
                            placeholder = { Text("e.g. google.com or http://...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                // Shape selection
                Text(
                    text = "Dot Shape",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    shapesList.forEach { shape ->
                        val isSelected = selectedShape.lowercase() == shape.lowercase()
                        Button(
                            onClick = { selectedShape = shape },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Text(
                                text = if (shape == "RoundedSquare") "Square" else if (shape == "ThinRectangle") "ThinRect" else shape,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                // Dot Size Selection
                Text(
                    text = "Dot Size",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "Small (36)" to 36f,
                        "Medium (48)" to 48f,
                        "Large (64)" to 64f
                    )
                    presets.forEach { (label, value) ->
                        val isSelected = selectedDotSize == value
                        FilledTonalButton(
                            onClick = { selectedDotSize = value },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (isSelected) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            },
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedDotSize.toInt()} dp",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.width(42.dp)
                    )
                    Slider(
                        value = selectedDotSize,
                        onValueChange = { selectedDotSize = it },
                        valueRange = 30f..80f,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                // Dot Font Size Selection
                Text(
                    text = "Text Font Size",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedDotFontSize.toInt()} sp",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.width(42.dp)
                    )
                    Slider(
                        value = selectedDotFontSize,
                        onValueChange = { selectedDotFontSize = it },
                        valueRange = 6f..16f,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                // Color Selection
                Text(
                    text = "Dot Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colorPalette.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 2.5.dp else 1.dp,
                                    color = if (selectedColor == color) Color.White else Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                // Text/Font Color Selection
                Text(
                    text = "Font Color",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val fontColorPalette = listOf(
                        Color.White,
                        Color.Black,
                        Color(0xFFEF5350), // Red
                        Color(0xFFFFF176), // Yellow
                        Color(0xFF81C784), // Green
                        Color(0xFF64B5F6) // Blue
                    )
                    fontColorPalette.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedTextColor == color) 2.5.dp else 1.dp,
                                    color = if (selectedTextColor == color) (if (color == Color.White) Color.Black else Color.White) else Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { selectedTextColor = color }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                // Actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(selectedColor, selectedTextColor, selectedShape, customTextState, payloadState, selectedDotSize, selectedDotFontSize, selectedIsUrl, selectedUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
