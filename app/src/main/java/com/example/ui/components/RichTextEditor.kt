@file:Suppress("DEPRECATION")
package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MarkdownVisualTransformation(
    private val tagColor: Color = Color.Gray.copy(alpha = 0.5f),
    private val highlightColor: Color = Color.Cyan,
    private val onPrimaryColor: Color = Color.White,
    private val primaryColor: Color = Color.Blue,
    private val isMathNote: Boolean = false
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        val rawText = text.text
        
        if (isMathNote) {
            // Formatting bold: **text**
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            boldRegex.findAll(rawText).forEach { match ->
                val range = match.groups[0]!!.range
                val innerRange = match.groups[1]!!.range
                builder.addStyle(SpanStyle(color = tagColor), range.first, range.first + 2)
                builder.addStyle(SpanStyle(color = tagColor), range.last - 1, range.last + 1)
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), innerRange.first, innerRange.last + 1)
            }
            
            // Italic: *text* (excluding **)
            val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)")
            italicRegex.findAll(rawText).forEach { match ->
                val range = match.groups[0]!!.range
                val innerRange = match.groups[1]!!.range
                builder.addStyle(SpanStyle(color = tagColor), range.first, range.first + 1)
                builder.addStyle(SpanStyle(color = tagColor), range.last, range.last + 1)
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), innerRange.first, innerRange.last + 1)
            }
            // Underline: __text__
            val underlineRegex = Regex("__(.*?)__")
            underlineRegex.findAll(rawText).forEach { match ->
                val range = match.groups[0]!!.range
                val innerRange = match.groups[1]!!.range
                builder.addStyle(SpanStyle(color = tagColor), range.first, range.first + 2)
                builder.addStyle(SpanStyle(color = tagColor), range.last - 1, range.last + 1)
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), innerRange.first, innerRange.last + 1)
            }
            // Math Boxes: ?[RBGY]|content?
            val mathBoxRegex = Regex("\\?([RBGY])\\|(.*?)\\?")
            mathBoxRegex.findAll(rawText).forEach { match ->
                val range = match.range
                val colorChar = match.groupValues[1]
                val innerRange = match.groups[2]!!.range
                
                // Hide delimiters: ?R|
                builder.addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), range.first, range.first + 3)
                // Hide delimiter: ?
                builder.addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), range.last, range.last + 1)
                
                val textColor = when (colorChar) {
                    "R" -> Color(0xFFFF8A80) // Light Red
                    "B" -> Color(0xFF80D8FF) // Light Blue
                    "G" -> Color(0xFFB9F6CA) // Light Green
                    "Y" -> Color(0xFFFFFF8D) // Light Yellow
                    else -> Color.White
                }
                builder.addStyle(
                    SpanStyle(
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    ),
                    innerRange.first,
                    innerRange.last + 1
                )
            }
        } else {
            // Formatting bold: **text**
            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
            boldRegex.findAll(rawText).forEach { match ->
                val range = match.groups[0]!!.range
                val innerRange = match.groups[1]!!.range
                builder.addStyle(SpanStyle(color = tagColor), range.first, range.first + 2)
                builder.addStyle(SpanStyle(color = tagColor), range.last - 1, range.last + 1)
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), innerRange.first, innerRange.last + 1)
            }
            
            // Italic: *text* (excluding **)
            val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)")
            italicRegex.findAll(rawText).forEach { match ->
                val range = match.groups[0]!!.range
                val innerRange = match.groups[1]!!.range
                builder.addStyle(SpanStyle(color = tagColor), range.first, range.first + 1)
                builder.addStyle(SpanStyle(color = tagColor), range.last, range.last + 1)
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), innerRange.first, innerRange.last + 1)
            }
            // Underline: __text__
            val underlineRegex = Regex("__(.*?)__")
            underlineRegex.findAll(rawText).forEach { match ->
                val range = match.groups[0]!!.range
                val innerRange = match.groups[1]!!.range
                builder.addStyle(SpanStyle(color = tagColor), range.first, range.first + 2)
                builder.addStyle(SpanStyle(color = tagColor), range.last - 1, range.last + 1)
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), innerRange.first, innerRange.last + 1)
            }
            val turquoiseColor = Color(0xFF00E5FF)
            // Minimized copy text: ?-text?
            val minimizedRegex = Regex("\\?\\-(.*?)\\?")
            minimizedRegex.findAll(rawText).forEach { match ->
                val range = match.range
                val innerRange = match.groups[1]!!.range
                val pipeIndex = rawText.indexOf('|', innerRange.first)
                
                // Hide delimiters
                builder.addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), range.first, range.first + 2)
                builder.addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), range.last, range.last + 1)
                
                if (pipeIndex != -1 && pipeIndex <= innerRange.last) {
                    // Hide pipe and payload
                    builder.addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), pipeIndex, innerRange.last + 1)
                    // Color text turquoise
                    builder.addStyle(
                        SpanStyle(
                            color = turquoiseColor,
                            fontWeight = FontWeight.Bold
                        ),
                        innerRange.first,
                        pipeIndex
                    )
                } else {
                    // Color text turquoise
                    builder.addStyle(
                        SpanStyle(
                            color = turquoiseColor,
                            fontWeight = FontWeight.Bold
                        ),
                        innerRange.first,
                        innerRange.last + 1
                    )
                }
            }
            // Expanded copy text: ?+text?
            val expandedRegex = Regex("\\?\\+(.*?)\\?")
            expandedRegex.findAll(rawText).forEach { match ->
                val range = match.range
                val innerRange = match.groups[1]!!.range
                val pipeIndex = rawText.indexOf('|', innerRange.first)
                
                // Hide delimiters
                builder.addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), range.first, range.first + 2)
                builder.addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), range.last, range.last + 1)
                
                if (pipeIndex != -1 && pipeIndex <= innerRange.last) {
                    val payloadStr = rawText.substring(pipeIndex + 1, innerRange.last + 1)
                    if (payloadStr.startsWith("expand:")) {
                        // This is a "Detail Expand" copy block!
                        // Hide display label, pipe, and the "expand:" prefix
                        // So we hide from innerRange.first to pipeIndex + 1 + "expand:".length (which is pipeIndex + 8)
                        builder.addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), innerRange.first, pipeIndex + 8)
                        // and show the rest of the payload as turquoise/bold
                        builder.addStyle(
                            SpanStyle(
                                color = turquoiseColor,
                                fontWeight = FontWeight.Bold
                            ),
                            pipeIndex + 8,
                            innerRange.last + 1
                        )
                    } else {
                        // Hide pipe and payload
                        builder.addStyle(SpanStyle(color = Color.Transparent, fontSize = 0.sp), pipeIndex, innerRange.last + 1)
                        // Color text turquoise
                        builder.addStyle(
                            SpanStyle(
                                color = turquoiseColor,
                                fontWeight = FontWeight.Bold
                            ),
                            innerRange.first,
                            pipeIndex
                        )
                    }
                } else {
                    // Color text turquoise
                    builder.addStyle(
                        SpanStyle(
                            color = turquoiseColor,
                            fontWeight = FontWeight.Bold
                        ),
                        innerRange.first,
                        innerRange.last + 1
                    )
                }
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MarkdownRichTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    selectedBgColor: Int,
    onBgColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Start writing your note here...",
    scrollState: ScrollState = rememberScrollState(),
    isViewingMode: Boolean = false,
    isMathNote: Boolean = false,
    floatingCanvasContent: @Composable () -> Unit = {}
) {
    var fontSizeValue by remember { mutableStateOf(15f) }
    var lineSpacingValue by remember { mutableStateOf(24f) }
    var showFontSizeSlider by remember { mutableStateOf(false) }
    var showLineSpacingSlider by remember { mutableStateOf(false) }
    var editingMathBoxRange by remember { mutableStateOf<IntRange?>(null) }
    var editingMathBoxColor by remember { mutableStateOf("") }
    var editingMathBoxValue by remember { mutableStateOf("") }
    var editingBlockInfo by remember { mutableStateOf<CopyBlockInfo?>(null) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Layer 1 (Bottom): The scrollable text input area and sliders
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (!isViewingMode) 48.dp else 0.dp)
            ) {
                if (!isViewingMode && showFontSizeSlider) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Font Size",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(64.dp)
                        )
                        Slider(
                            value = fontSizeValue,
                            onValueChange = { fontSizeValue = it.coerceIn(10f, 36f) },
                            valueRange = 10f..36f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        var textInput by remember(fontSizeValue) {
                            mutableStateOf(fontSizeValue.toInt().toString())
                        }
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { newValue ->
                                textInput = newValue
                                newValue.toIntOrNull()?.let {
                                    fontSizeValue = it.toFloat().coerceIn(10f, 36f)
                                }
                            },
                            textStyle = TextStyle(fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            modifier = Modifier
                                .width(52.dp)
                                .height(36.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
                if (!isViewingMode && showLineSpacingSlider) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spacing",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(64.dp)
                        )
                        Slider(
                            value = lineSpacingValue,
                            onValueChange = { lineSpacingValue = it.coerceIn(14f, 50f) },
                            valueRange = 14f..50f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        var textInput by remember(lineSpacingValue) {
                            mutableStateOf(lineSpacingValue.toInt().toString())
                        }
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { newValue ->
                                textInput = newValue
                                newValue.toIntOrNull()?.let {
                                    lineSpacingValue = it.toFloat().coerceIn(14f, 50f)
                                }
                            },
                            textStyle = TextStyle(fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            modifier = Modifier
                                .width(52.dp)
                                .height(36.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                val context = LocalContext.current
                val density = LocalDensity.current
                
                val primaryColor = MaterialTheme.colorScheme.primary
                val onPrimary = MaterialTheme.colorScheme.onPrimary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {
                    // Input field
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        onTextLayout = { textLayoutResult = it },
                        readOnly = isViewingMode,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = fontSizeValue.sp,
                            lineHeight = lineSpacingValue.sp
                        ),
                        cursorBrush = SolidColor(primaryColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        visualTransformation = MarkdownVisualTransformation(
                            highlightColor = MaterialTheme.colorScheme.secondary,
                            onPrimaryColor = MaterialTheme.colorScheme.onPrimary,
                            primaryColor = MaterialTheme.colorScheme.primary,
                            isMathNote = isMathNote
                        ),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                if (value.text.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    // Render floating touch targets & minimize badges
                    if (!isMathNote) {
                        textLayoutResult?.let { layoutResult ->
                            val textStr = value.text
                            if (layoutResult.layoutInput.text.text == textStr) {
                                val copyRegex = Regex("\\?([+-])(.*?)\\?")
                                copyRegex.findAll(textStr).forEach { match ->
                                    val range = match.range
                                    val state = match.groupValues[1]
                                    val innerText = match.groupValues[2]
                                    val innerRange = match.groups[2]!!.range
                                    
                                    val pipeIndex = textStr.indexOf('|', innerRange.first)
                                    val displayRangeEnd = if (pipeIndex != -1 && pipeIndex <= innerRange.last) pipeIndex else (innerRange.last + 1)
                                    
                                    val displayLabel = if (pipeIndex != -1 && pipeIndex <= innerRange.last) textStr.substring(innerRange.first, pipeIndex) else innerText
                                    val payload = if (pipeIndex != -1 && pipeIndex <= innerRange.last) textStr.substring(pipeIndex + 1, innerRange.last + 1) else innerText
                                    val isDetailExpand = state == "+" && payload.startsWith("expand:")
                                    val targetStart = if (isDetailExpand) (pipeIndex + 8) else innerRange.first
                                    val targetEnd = if (isDetailExpand) (innerRange.last + 1) else displayRangeEnd
                                    val lineBoxes = mutableMapOf<Int, Rect>()
                                    try {
                                        for (idx in targetStart until targetEnd) {
                                            if (idx >= textStr.length) continue
                                            val char = textStr[idx]
                                            if (char == '\n' || char == '\r') continue
                                            val rect = layoutResult.getBoundingBox(idx)
                                            if (rect.width > 0 && rect.height > 0) {
                                                val line = layoutResult.getLineForOffset(idx)
                                                val currentRect = lineBoxes[line]
                                                if (currentRect == null) {
                                                    lineBoxes[line] = rect
                                                } else {
                                                    lineBoxes[line] = Rect(
                                                        left = minOf(currentRect.left, rect.left),
                                                        top = minOf(currentRect.top, rect.top),
                                                        right = maxOf(currentRect.right, rect.right),
                                                        bottom = maxOf(currentRect.bottom, rect.bottom)
                                                    )
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        try {
                                            val path = layoutResult.getPathForRange(targetStart, targetEnd)
                                            val bounds = path.getBounds()
                                            if (bounds.width > 0 && bounds.height > 0) {
                                                lineBoxes[0] = bounds
                                            }
                                        } catch (ex: Exception) {}
                                    }
                                    lineBoxes.forEach { (_, bounds) ->
                                        val overlayLeft = with(density) { bounds.left.toDp() }
                                        val overlayTop = with(density) { bounds.top.toDp() }
                                        val overlayWidth = with(density) { bounds.width.toDp() }
                                        val overlayHeight = with(density) { bounds.height.toDp() }
                                        val hasValidLayout = true
                                        if (hasValidLayout) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = overlayLeft, y = overlayTop)
                                                    .size(width = overlayWidth, height = overlayHeight)
                                            ) {
                                                if (state == "-") {
                                                    // Click once to Copy, Double-Click to Open, Long-Press to Customize
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .combinedClickable(
                                                                onClick = {
                                                                    if (payload.startsWith("expand:")) {
                                                                        try {
                                                                            val newText = textStr.substring(0, match.range.first) + "?+$innerText?" + textStr.substring(match.range.last + 1)
                                                                            onValueChange(TextFieldValue(newText, TextRange(match.range.first + 2)))
                                                                        } catch (e: Exception) {}
                                                                    } else if (payload.startsWith("url:")) {
                                                                        val link = payload.removePrefix("url:")
                                                                        openBrowserLink(link, context)
                                                                    } else {
                                                                        copyToClipboard(payload, context)
                                                                        android.widget.Toast.makeText(context, "Copied: $displayLabel", android.widget.Toast.LENGTH_SHORT).show()
                                                                    }
                                                                },
                                                                onLongClick = {
                                                                    editingBlockInfo = CopyBlockInfo(
                                                                        matchRange = match.range,
                                                                        state = state,
                                                                        displayLabel = displayLabel,
                                                                        payload = payload,
                                                                        displayRangeStart = innerRange.first,
                                                                        displayRangeEnd = displayRangeEnd
                                                                    )
                                                                },
                                                                onDoubleClick = {
                                                                    try {
                                                                        val newText = textStr.substring(0, match.range.first) + "?+$innerText?" + textStr.substring(match.range.last + 1)
                                                                        onValueChange(TextFieldValue(newText, TextRange(match.range.first + 2)))
                                                                    } catch (e: Exception) {}
                                                                }
                                                            )
                                                    )
                                                } else {
                                                    // Expanded/Active state: clickable directly on the highlighted text area to lock/minimize
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clickable {
                                                                try {
                                                                    val newText = textStr.substring(0, match.range.first) + "?-$innerText?" + textStr.substring(match.range.last + 1)
                                                                    onValueChange(TextFieldValue(newText, TextRange(match.range.first + 2)))
                                                                } catch (e: Exception) {}
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // isMathNote is true: render floating overlay touch targets over colored boxes
                        textLayoutResult?.let { layoutResult ->
                            val textStr = value.text
                            if (layoutResult.layoutInput.text.text == textStr) {
                                val mathBoxRegex = Regex("\\?([RBGY])\\|(.*?)\\?")
                                mathBoxRegex.findAll(textStr).forEach { match ->
                                    val range = match.range
                                    var hasValidLayout = false
                                    var overlayLeft = 0.dp
                                    var overlayTop = 0.dp
                                    var overlayWidth = 0.dp
                                    var overlayHeight = 0.dp
                                    
                                    try {
                                        val path = layoutResult.getPathForRange(range.first, range.last + 1)
                                        val bounds = path.getBounds()
                                        if (bounds.width > 0 && bounds.height > 0) {
                                            overlayLeft = with(density) { bounds.left.toDp() }
                                            overlayTop = with(density) { bounds.top.toDp() }
                                            overlayWidth = with(density) { bounds.width.toDp() }
                                            overlayHeight = with(density) { bounds.height.toDp() }
                                            hasValidLayout = true
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                    if (hasValidLayout) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = overlayLeft, y = overlayTop)
                                                .size(width = overlayWidth, height = overlayHeight)
                                                .combinedClickable(
                                                    onClick = {
                                                        // Single-tap finishes typing/editing inside this box
                                                        // by moving the selection immediately after the closing '?'
                                                        onValueChange(
                                                            value.copy(
                                                                selection = TextRange(range.last + 1)
                                                            )
                                                        )
                                                    },
                                                    onLongClick = {
                                                        editingMathBoxRange = range
                                                        editingMathBoxColor = match.groupValues[1]
                                                        editingMathBoxValue = match.groupValues[2]
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Layer 2: The floating canvas content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (!isViewingMode) 48.dp else 0.dp)
            ) {
                floatingCanvasContent()
            }
            // Layer 3 (Top): The Stationary formatting toolbar
            if (!isViewingMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .zIndex(2f)
                ) {
                    MarkdownToolbar(
                        value = value,
                        onValueChange = onValueChange,
                        isMathNote = isMathNote,
                        selectedBgColor = selectedBgColor,
                        onBgColorSelected = onBgColorSelected,
                        showFontSizeSlider = showFontSizeSlider,
                        onToggleFontSizeSlider = {
                            showFontSizeSlider = !showFontSizeSlider
                            showLineSpacingSlider = false
                        },
                        showLineSpacingSlider = showLineSpacingSlider,
                        onToggleLineSpacingSlider = {
                            showLineSpacingSlider = !showLineSpacingSlider
                            showFontSizeSlider = false
                        }
                    )
                }
            }
        }
    }
    if (editingMathBoxRange != null) {
        AlertDialog(
            onDismissRequest = { editingMathBoxRange = null },
            title = { Text("Edit Math Box Value", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type the new value inside this colored box:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    OutlinedTextField(
                        value = editingMathBoxValue,
                        onValueChange = { editingMathBoxValue = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val range = editingMathBoxRange
                    if (range != null) {
                        val oldText = value.text
                        val colorChar = editingMathBoxColor
                        val newVal = editingMathBoxValue.trim()
                        val newMathBoxStr = "?$colorChar|$newVal?"
                        val prefix = oldText.substring(0, range.first)
                        val suffix = oldText.substring(range.last + 1)
                        val newFullText = prefix + newMathBoxStr + suffix
                        
                        onValueChange(
                            value.copy(
                                text = newFullText,
                                selection = TextRange(prefix.length + newMathBoxStr.length)
                            )
                        )
                    }
                    editingMathBoxRange = null
                }) {
                    Text("Done", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMathBoxRange = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF26273B)
        )
    }
    // Customizer dialog for copying block
    if (editingBlockInfo != null) {
        val info = editingBlockInfo!!
        var tempLabel by remember(info) { mutableStateOf(info.displayLabel) }
        
        val initialIsExpand = info.payload.startsWith("expand:")
        val initialIsUrl = info.payload.startsWith("url:")
        val initialUrl = if (initialIsUrl) info.payload.removePrefix("url:") else ""
        val initialPayload = when {
            initialIsExpand -> info.payload.removePrefix("expand:")
            initialIsUrl -> ""
            else -> info.payload
        }
        
        var isExpandEnabled by remember(info) { mutableStateOf(initialIsExpand) }
        var isUrlEnabled by remember(info) { mutableStateOf(initialIsUrl) }
        var urlLink by remember(info) { mutableStateOf(initialUrl) }
        var tempPayload by remember(info) { mutableStateOf(initialPayload) }
        
        AlertDialog(
            onDismissRequest = { editingBlockInfo = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "Customize Copy Block",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = tempLabel,
                        onValueChange = { tempLabel = it },
                        label = { Text("Display Label") },
                        placeholder = { Text("e.g. My Website") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                isExpandEnabled = !isExpandEnabled
                                if (isExpandEnabled) isUrlEnabled = false
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isExpandEnabled,
                            onCheckedChange = { 
                                isExpandEnabled = it
                                if (it) isUrlEnabled = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detail Expand on single tap",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!isExpandEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isUrlEnabled = !isUrlEnabled }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isUrlEnabled,
                                onCheckedChange = { isUrlEnabled = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Link option upon tap",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (isUrlEnabled && !isExpandEnabled) {
                        OutlinedTextField(
                            value = urlLink,
                            onValueChange = { urlLink = it },
                            label = { Text("URL Link Website") },
                            placeholder = { Text("e.g. google.com or http://...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = tempPayload,
                            onValueChange = { tempPayload = it },
                            label = { Text(if (isExpandEnabled) "Text to Expand (Payload)" else "Text to Copy (Payload)") },
                            placeholder = { Text("e.g. https://google.com") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sanitizedLabel = tempLabel.replace("?", "").replace("|", "")
                        val sanitizedPayload = when {
                            isExpandEnabled -> {
                                val payloadText = tempPayload.replace("?", "")
                                "expand:$payloadText"
                            }
                            isUrlEnabled -> {
                                val linkText = urlLink.replace("?", "")
                                "url:$linkText"
                            }
                            else -> {
                                tempPayload.replace("?", "")
                            }
                        }
                        val innerStr = if (sanitizedLabel == sanitizedPayload) sanitizedLabel else "$sanitizedLabel|$sanitizedPayload"
                        val newText = value.text.substring(0, info.matchRange.first) + "?-${innerStr}?" + value.text.substring(info.matchRange.last + 1)
                        onValueChange(TextFieldValue(newText, TextRange(info.matchRange.first + 2)))
                        editingBlockInfo = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val newText = value.text.substring(0, info.matchRange.first) + value.text.substring(info.matchRange.last + 1)
                            onValueChange(TextFieldValue(newText, TextRange(info.matchRange.first)))
                            editingBlockInfo = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { editingBlockInfo = null }) {
                        Text("Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        )
    }
}

@Composable
fun MarkdownToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isMathNote: Boolean,
    selectedBgColor: Int,
    onBgColorSelected: (Int) -> Unit,
    showFontSizeSlider: Boolean,
    onToggleFontSizeSlider: () -> Unit,
    showLineSpacingSlider: Boolean,
    onToggleLineSpacingSlider: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toolbarScrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .horizontalScroll(toolbarScrollState)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (isMathNote) {
            // Math Box Addition Buttons
            Text(
                text = "+ Box: ",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 2.dp)
            )
            
            // Red Box
            Button(
                onClick = {
                    val selection = value.selection
                    val text = value.text
                    val newText = text.substring(0, selection.start) + "?R|0?" + text.substring(selection.end)
                    val newSelection = TextRange(selection.start + 3, selection.start + 4)
                    onValueChange(TextFieldValue(newText, newSelection))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF532424), contentColor = Color(0xFFFF8A80)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(28.dp).padding(horizontal = 2.dp)
            ) {
                Text("Red", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            // Blue Box
            Button(
                onClick = {
                    val selection = value.selection
                    val text = value.text
                    val newText = text.substring(0, selection.start) + "?B|0?" + text.substring(selection.end)
                    val newSelection = TextRange(selection.start + 3, selection.start + 4)
                    onValueChange(TextFieldValue(newText, newSelection))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B3B52), contentColor = Color(0xFF80D8FF)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(28.dp).padding(horizontal = 2.dp)
            ) {
                Text("Blue", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            // Green Box
            Button(
                onClick = {
                    val selection = value.selection
                    val text = value.text
                    val newText = text.substring(0, selection.start) + "?G|0?" + text.substring(selection.end)
                    val newSelection = TextRange(selection.start + 3, selection.start + 4)
                    onValueChange(TextFieldValue(newText, newSelection))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF163E2B), contentColor = Color(0xFFB9F6CA)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(28.dp).padding(horizontal = 2.dp)
            ) {
                Text("Green", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            // Yellow Box
            Button(
                onClick = {
                    val selection = value.selection
                    val text = value.text
                    val newText = text.substring(0, selection.start) + "?Y|0?" + text.substring(selection.end)
                    val newSelection = TextRange(selection.start + 3, selection.start + 4)
                    onValueChange(TextFieldValue(newText, newSelection))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C421A), contentColor = Color(0xFFFFFF8D)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(28.dp).padding(horizontal = 2.dp)
            ) {
                Text("Yellow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
            VerticalDivider(modifier = Modifier.height(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
        } else {
            FormatButton(
                icon = Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                onClick = {
                    val selection = value.selection
                    val text = value.text
                    val newText: String
                    val newSelection: TextRange
                    if (selection.collapsed) {
                        newText = text.substring(0, selection.start) + "____" + text.substring(selection.end)
                        newSelection = TextRange(selection.start + 2)
                    } else {
                        val innerText = text.substring(selection.start, selection.end)
                        newText = text.substring(0, selection.start) + "__${innerText}__" + text.substring(selection.end)
                        newSelection = TextRange(selection.start, selection.end + 4)
                    }
                    onValueChange(TextFieldValue(newText, newSelection))
                }
            )
            FormatButton(
                icon = Icons.Default.ContentCopy,
                contentDescription = "Insert Copy Text Block",
                onClick = {
                    val selection = value.selection
                    val text = value.text
                    val newText: String
                    val newSelection: TextRange
                    if (selection.collapsed) {
                        newText = text.substring(0, selection.start) + "?-type text?" + text.substring(selection.end)
                        newSelection = TextRange(selection.start + 2, selection.start + 11)
                    } else {
                        val innerText = text.substring(selection.start, selection.end)
                        newText = text.substring(0, selection.start) + "?-$innerText?" + text.substring(selection.end)
                        newSelection = TextRange(selection.start + 2, selection.start + 2 + innerText.length)
                    }
                    onValueChange(TextFieldValue(newText, newSelection))
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            VerticalDivider(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            FormatButton(
                icon = Icons.Default.FormatListBulleted,
                contentDescription = "Bullet List",
                onClick = {
                    val selection = value.selection
                    val text = value.text
                    // Insert bullet list marker at start of line
                    val lineStart = text.lastIndexOf('\n', selection.start).coerceAtLeast(0)
                    val actualStart = if (lineStart > 0) lineStart + 1 else 0
                    val newText = text.substring(0, actualStart) + "• " + text.substring(actualStart)
                    onValueChange(TextFieldValue(newText, TextRange(selection.start + 2)))
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            VerticalDivider(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        // Font Size trigger button
        FormatButton(
            icon = Icons.Default.FormatSize,
            contentDescription = "Font Size",
            onClick = onToggleFontSizeSlider
        )
        // Line Spacing trigger button
        FormatButton(
            icon = Icons.Default.FormatLineSpacing,
            contentDescription = "Line Spacing size",
            onClick = onToggleLineSpacingSlider
        )
        Spacer(modifier = Modifier.width(8.dp))
        VerticalDivider(modifier = Modifier.height(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        val backgroundColors = listOf(
            Color(0xFF12131F), // Obsidian
            Color(0xFF261214), // Crimson Dusk
            Color(0xFF0C1924), // Twilight Deep
            Color(0xFF0B1B15), // Emerald Shadow
            Color(0xFF1E1428) // Violet Space
        )
        backgroundColors.forEach { color ->
            val argb = (color.value shr 32).toInt()
            val isSelected = selectedBgColor == argb
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickable { onBgColorSelected(argb) }
            )
        }
    }
}

data class CopyBlockInfo(
    val matchRange: IntRange,
    val state: String,
    val displayLabel: String,
    val payload: String,
    val displayRangeStart: Int,
    val displayRangeEnd: Int
)

@Composable
fun FormatButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
