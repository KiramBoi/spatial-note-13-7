@file:Suppress("DEPRECATION")
package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PictureItem
import com.example.data.model.CalculationLine
import com.example.data.model.CalcElement
import com.example.ui.components.CropperDialog
import com.example.ui.components.FloatingCanvas
import com.example.ui.components.MarkdownRichTextEditor
import com.example.ui.viewmodel.NoteViewModel
import java.io.File
import java.io.FileOutputStream

// Utility functions for Math Ledger real-time parsing and calculations
fun calculateLiveTotals(text: String): Map<String, Double> {
    val totals = mutableMapOf("Red" to 0.0, "Blue" to 0.0, "Green" to 0.0, "Yellow" to 0.0)
    val mathBoxRegex = Regex("\\?([RBGY])\\|(.*?)\\?")
    mathBoxRegex.findAll(text).forEach { match ->
        val colorChar = match.groupValues[1]
        val content = match.groupValues[2]
        
        val value = content.trim().toDoubleOrNull() ?: if (content.trim().isNotEmpty()) 1.0 else 0.0
        val colorName = when (colorChar) {
            "R" -> "Red"
            "B" -> "Blue"
            "G" -> "Green"
            "Y" -> "Yellow"
            else -> ""
        }
        if (colorName.isNotEmpty()) {
            totals[colorName] = (totals[colorName] ?: 0.0) + value
        }
    }
    return totals
}

fun evaluateCalculation(elements: List<CalcElement>, totals: Map<String, Double>): Double {
    if (elements.isEmpty()) return 0.0
    // Filter out trailing operators to avoid parsing errors when in construction mode
    val activeElements = if (elements.isNotEmpty() && elements.last().type == "OPERATOR" && elements.last().value !in listOf("(", ")")) {
        elements.dropLast(1)
    } else {
        elements
    }
    if (activeElements.isEmpty()) return 0.0
    // First, convert elements to tokens (numbers, bracket strings, and operator strings)
    val tokens = mutableListOf<String>()
    for (el in activeElements) {
        if (el.type == "BOX") {
            val fromTotals = totals[el.value]
            val valueStr = if (fromTotals != null) {
                fromTotals.toString()
            } else {
                (el.value.toDoubleOrNull() ?: 0.0).toString()
            }
            tokens.add(valueStr)
        } else {
            tokens.add(el.value)
        }
    }
    if (tokens.isEmpty()) return 0.0
    class FormulaParser(val tokens: List<String>) {
        var index = 0
        fun peek(): String? {
            return if (index < tokens.size) tokens[index] else null
        }
        fun consume(): String {
            val token = tokens[index]
            index++
            return token
        }
        fun parseExpression(): Double {
            var val1 = parseTerm()
            while (true) {
                val next = peek()
                if (next == "+" || next == "-") {
                    val op = consume()
                    val val2 = parseTerm()
                    if (op == "+") {
                        val1 += val2
                    } else {
                        val1 -= val2
                    }
                } else {
                    break
                }
            }
            return val1
        }
        fun parseTerm(): Double {
            var val1 = parseFactor()
            while (true) {
                val next = peek()
                if (next == "x" || next == "X" || next == "*" || next == "/") {
                    val op = consume()
                    val val2 = parseFactor()
                    if (op == "/") {
                        val1 = if (val2 != 0.0) val1 / val2 else 0.0
                    } else {
                        val1 *= val2
                    }
                } else {
                    break
                }
            }
            return val1
        }
        fun parseFactor(): Double {
            val token = peek() ?: return 0.0
            if (token == "(") {
                consume() // "("
                val exprVal = parseExpression()
                if (peek() == ")") {
                    consume() // ")"
                }
                return exprVal
            } else if (token == ")") {
                consume() // ")"
                return 0.0
            } else {
                consume()
                return token.toDoubleOrNull() ?: 0.0
            }
        }
    }
    try {
        val parser = FormulaParser(tokens)
        return parser.parseExpression()
    } catch (e: Exception) {
        return 0.0
    }
}

fun formatDouble(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format("%.2f", value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Manage local State representing traditional Rich Text editor
    var titleState by remember { viewModel.currentTitle }
    var richTextState by remember {
        mutableStateOf(TextFieldValue(viewModel.currentBodyText.value))
    }
    val editorScrollState = rememberScrollState()
    var isViewingMode by remember {
        mutableStateOf(viewModel.activeNoteId.value != null)
    }
    
    LaunchedEffect(viewModel.currentBodyText.value) {
        val newText = viewModel.currentBodyText.value
        if (richTextState.text != newText) {
            richTextState = TextFieldValue(
                text = newText,
                selection = if (richTextState.selection.end <= newText.length) {
                    richTextState.selection
                } else {
                    androidx.compose.ui.text.TextRange(newText.length)
                }
            )
        }
    }
    
    var selectedBgColor by remember { viewModel.currentBgColor }
    val isMathNote by remember { viewModel.isMathNote }
    val density = LocalDensity.current
    
    // Manage Media Picker contract for images upload
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val scrollOffsetDp = with(density) { editorScrollState.value.toDp().value }
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val destFile = File(context.filesDir, "img_" + System.currentTimeMillis() + ".png")
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    viewModel.addPictureItem(destFile.absolutePath, scrollOffsetDp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    // Interactive Crop selection
    var pictureToCrop by remember { mutableStateOf<PictureItem?>(null) }
    // Live calculations mapping
    val liveTotals = remember(richTextState.text) {
        calculateLiveTotals(richTextState.text)
    }
    // Inline calculation builder state
    var builderLine by remember { mutableStateOf<CalculationLine?>(null) }
    var editingLineId by remember { mutableStateOf<String?>(null) }
    var showCustomNumberDialog by remember { mutableStateOf(false) }
    var customNumberInputVal by remember { mutableStateOf("") }
    var editingNumElementLine by remember { mutableStateOf<CalculationLine?>(null) }
    var editingNumElementIndex by remember { mutableStateOf<Int?>(null) }
    var editingNumElementValue by remember { mutableStateOf("") }
    var renamingColorByLabel by remember { mutableStateOf<String?>(null) }
    var newLabelText by remember { mutableStateOf("") }
    
    // Physical/system back press handler to save and go back
    BackHandler {
        if (isViewingMode) {
            isViewingMode = false
        } else {
            viewModel.currentBodyText.value = richTextState.text
            viewModel.saveActiveNote(onBack)
        }
    }
    
    Scaffold(
        topBar = {
            if (!isViewingMode) {
                Column(modifier = Modifier.background(Color(selectedBgColor))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.currentBodyText.value = richTextState.text
                                viewModel.saveActiveNote(onBack)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back & Auto Save",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Elegant inline Title Editor directly in top title - BasicTextField for tight layout
                        BasicTextField(
                            value = titleState,
                            onValueChange = { 
                                titleState = it
                                viewModel.updateTitleAndBody(it, richTextState.text)
                            },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(Color.White),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (titleState.isEmpty()) {
                                        Text(
                                            text = "Note Title",
                                            color = Color.White.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        // Eye toggle icon
                        IconButton(
                            onClick = { isViewingMode = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Viewing Mode",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Undo - Redo Buttons
                        val undoEnabled by remember { viewModel.canUndo }
                        val redoEnabled by remember { viewModel.canRedo }
                        if (undoEnabled) {
                            IconButton(
                                onClick = { viewModel.undo() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "Undo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (redoEnabled) {
                            IconButton(
                                onClick = { viewModel.redo() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Redo,
                                    contentDescription = "Redo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        // Copy entire Note contents
                        IconButton(
                            onClick = {
                                val sb = java.lang.StringBuilder()
                                val title = titleState.ifBlank { "Untitled Note" }
                                sb.append("Title: ").append(title).append("\n\n")
                                val body = richTextState.text
                                if (body.isNotBlank()) {
                                    sb.append("Body:\n").append(body).append("\n\n")
                                }
                                if (isMathNote) {
                                    sb.append("--- Math Ledger Totals ---\n")
                                    liveTotals.forEach { (color, sum) ->
                                        sb.append(color).append(": ").append(formatDouble(sum)).append("\n")
                                    }
                                    if (viewModel.activeCalculationLines.isNotEmpty()) {
                                        sb.append("\n--- Calculation Equations ---\n")
                                        viewModel.activeCalculationLines.forEach { line ->
                                            val eq = line.elements.joinToString(" ") { el -> el.value }
                                            val res = evaluateCalculation(line.elements, liveTotals)
                                            sb.append(eq).append(" = ").append(formatDouble(res)).append("\n")
                                        }
                                    }
                                } else {
                                    val boxes = viewModel.activeCopyBoxes
                                    if (boxes.isNotEmpty()) {
                                        sb.append("--- Copy Boxes ---\n")
                                        boxes.forEachIndexed { index, box ->
                                            if (box.text.isNotBlank()) {
                                                sb.append(index + 1).append(". ").append(box.text).append("\n")
                                            }
                                        }
                                    }
                                }
                                val copiedText = sb.toString().trim()
                                if (copiedText.isNotEmpty()) {
                                    com.example.ui.components.copyToClipboard(copiedText, context)
                                    android.widget.Toast.makeText(context, "Copied entire note details!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy entire note",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Header Save check icon button
                        IconButton(
                            onClick = {
                                viewModel.currentBodyText.value = richTextState.text
                                viewModel.saveActiveNote(onBack)
                            },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save and complete",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(selectedBgColor),
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isViewingMode) PaddingValues(0.dp) else innerPadding)
            ) {
                // MATH LEDGER HEADER PANEL (Totals + Equations + Equations Wizard) - Made highly compact & thinner
                if (isMathNote) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Math Ledger",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                    if (viewModel.activeCalculationLines.size < 4 && !isViewingMode && builderLine == null) {
                                        IconButton(
                                            onClick = {
                                                builderLine = CalculationLine(id = System.currentTimeMillis().toString(), elements = emptyList())
                                                editingLineId = null
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add line",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                if (!isViewingMode) {
                                    Text("Tap counter to rename", fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.4f))
                                }
                            }
                            
                            // 1. Totals Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Red", "Blue", "Green", "Yellow").forEach { color ->
                                    val sum = liveTotals[color] ?: 0.0
                                    val label = when (color) {
                                        "Red" -> viewModel.redLabel.value
                                        "Blue" -> viewModel.blueLabel.value
                                        "Green" -> viewModel.greenLabel.value
                                        "Yellow" -> viewModel.yellowLabel.value
                                        else -> color
                                    }
                                    val (bgCol, textCol) = when (color) {
                                        "Red" -> Color(0xFF532424) to Color(0xFFFF8A80)
                                        "Blue" -> Color(0xFF1B3B52) to Color(0xFF80D8FF)
                                        "Green" -> Color(0xFF163E2B) to Color(0xFFB9F6CA)
                                        "Yellow" -> Color(0xFF4C421A) to Color(0xFFFFFF8D)
                                        else -> Color.DarkGray to Color.White
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(bgCol, RoundedCornerShape(3.dp))
                                            .border(0.5.dp, textCol.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                            .clickable(enabled = !isViewingMode) {
                                                newLabelText = label
                                                renamingColorByLabel = color
                                            }
                                            .padding(vertical = 1.dp, horizontal = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(textCol, RoundedCornerShape(1.dp))
                                            )
                                            Text(
                                                text = "$label:${formatDouble(sum)}",
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = textCol,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                            // 2. Saved Calculation Lines
                            if (viewModel.activeCalculationLines.isNotEmpty()) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    viewModel.activeCalculationLines.forEach { line ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Left: Equation terms
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (line.name.isNotBlank()) {
                                                    Text(
                                                        text = line.name,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    )
                                                    Text(":", fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(end = 4.dp))
                                                }
                                                line.elements.forEachIndexed { index, el ->
                                                    if (el.type == "BOX") {
                                                        val isColorBox = el.value in listOf("Red", "Blue", "Green", "Yellow")
                                                        if (isColorBox) {
                                                            val (bg, txt) = when (el.value) {
                                                                "Red" -> Color(0xFF532424) to Color(0xFFFF8A80)
                                                                "Blue" -> Color(0xFF1B3B52) to Color(0xFF80D8FF)
                                                                "Green" -> Color(0xFF163E2B) to Color(0xFFB9F6CA)
                                                                "Yellow" -> Color(0xFF4C421A) to Color(0xFFFFFF8D)
                                                                else -> Color.White to Color.Black
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(8.dp)
                                                                    .height(16.dp)
                                                                    .background(txt, RoundedCornerShape(2.dp))
                                                            )
                                                        } else {
                                                            // White num box - tap directly to edit
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(Color.White, RoundedCornerShape(4.dp))
                                                                    .clickable {
                                                                        editingNumElementLine = line
                                                                        editingNumElementIndex = index
                                                                        editingNumElementValue = el.value
                                                                    }
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(el.value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                            }
                                                        }
                                                    } else {
                                                        Text(
                                                            text = el.value,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color.White.copy(alpha = 0.8f),
                                                            modifier = Modifier.padding(horizontal = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            // Right: evaluated result and action buttons
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val calculatedResult = evaluateCalculation(line.elements, liveTotals)
                                                Text(
                                                    text = "= ${formatDouble(calculatedResult)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                
                                                if (!isViewingMode) {
                                                    // Edit
                                                    IconButton(
                                                        onClick = {
                                                            builderLine = line
                                                            editingLineId = line.id
                                                        },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Equation", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                                                    }
                                                    // Delete
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.activeCalculationLines.remove(line)
                                                            viewModel.takeHistorySnapshot()
                                                        },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "Delete Equation", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 3. Calculation Creator Builder Panel
                if (builderLine != null) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentLine = builderLine!!
                        val elementCount = currentLine.elements.size
                        val lastType = currentLine.elements.lastOrNull()?.let {
                            if (it.value == "(") "OPERATOR"
                            else if (it.value == ")") "BOX"
                            else it.type
                        } ?: "OPERATOR"
                        val isNextBox = lastType == "OPERATOR"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (editingLineId != null) "Edit Equation:" else "Build Equation:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${currentLine.elements.count { it.type == "BOX" }} Boxes",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        // Interactive construction preview
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (currentLine.elements.isEmpty()) {
                                Text("Empty equation. Tap colors below...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                            } else {
                                currentLine.elements.forEachIndexed { index, el ->
                                    if (el.type == "BOX") {
                                        val isColorBox = el.value in listOf("Red", "Blue", "Green", "Yellow")
                                        if (isColorBox) {
                                            val (bg, txt) = when (el.value) {
                                                "Red" -> Color(0xFF532424) to Color(0xFFFF8A80)
                                                "Blue" -> Color(0xFF1B3B52) to Color(0xFF80D8FF)
                                                "Green" -> Color(0xFF163E2B) to Color(0xFFB9F6CA)
                                                "Yellow" -> Color(0xFF4C421A) to Color(0xFFFFFF8D)
                                                else -> Color.White to Color.Black
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(8.dp)
                                                    .height(16.dp)
                                                    .background(txt, RoundedCornerShape(2.dp))
                                            )
                                        } else {
                                            // White num box - tap directly to edit in builder
                                            Box(
                                                modifier = Modifier
                                                    .background(Color.White, RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        editingNumElementLine = null
                                                        editingNumElementIndex = index
                                                        editingNumElementValue = el.value
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(el.value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        }
                                    } else {
                                        Text(el.value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            val runningEval = evaluateCalculation(currentLine.elements, liveTotals)
                            Text("= ${formatDouble(runningEval)}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        // Equation Name Input Field
                        OutlinedTextField(
                            value = currentLine.name,
                            onValueChange = { builderLine = currentLine.copy(name = it) },
                            placeholder = { Text("Equation Name / Description (optional)", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        // Selection Choices Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isNextBox && elementCount < 25) {
                                // Pick next BOX color
                                // Filter colors already chosen in this equation
                                val alreadyChosen = currentLine.elements.filter { it.type == "BOX" }.map { it.value }.toSet()
                                listOf("Red", "Blue", "Green", "Yellow").forEach { color ->
                                    val isAvailable = color !in alreadyChosen
                                    if (isAvailable) {
                                        val label = when (color) {
                                            "Red" -> viewModel.redLabel.value
                                            "Blue" -> viewModel.blueLabel.value
                                            "Green" -> viewModel.greenLabel.value
                                            "Yellow" -> viewModel.yellowLabel.value
                                            else -> color
                                        }
                                        val (bg, txt) = when (color) {
                                            "Red" -> Color(0xFF532424) to Color(0xFFFF8A80)
                                            "Blue" -> Color(0xFF1B3B52) to Color(0xFF80D8FF)
                                            "Green" -> Color(0xFF163E2B) to Color(0xFFB9F6CA)
                                            "Yellow" -> Color(0xFF4C421A) to Color(0xFFFFFF8D)
                                            else -> Color.DarkGray to Color.White
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(bg, RoundedCornerShape(6.dp))
                                                .border(1.dp, txt.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                .clickable {
                                                    val updated = currentLine.elements + CalcElement("BOX", color)
                                                    builderLine = currentLine.copy(elements = updated)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = txt)
                                        }
                                    }
                                }
                            }
                            // Custom free input number box
                            Box(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(6.dp))
                                    .border(1.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        customNumberInputVal = ""
                                        showCustomNumberDialog = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Num", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            // Open bracket "(" can always be added when waiting for a box
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        val updated = currentLine.elements + CalcElement("OPERATOR", "(")
                                        builderLine = currentLine.copy(elements = updated)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("(", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            if (!isNextBox && elementCount < 25) {
                                // Pick OPERATOR
                                listOf("+", "-", "x", "/").forEach { op ->
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .clickable {
                                                val updated = currentLine.elements + CalcElement("OPERATOR", op)
                                                builderLine = currentLine.copy(elements = updated)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(op, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                                // Close bracket ")" can be added when waiting for an operator if bracket stack is open
                                val openBracketsCount = currentLine.elements.count { it.value == "(" }
                                val closeBracketsCount = currentLine.elements.count { it.value == ")" }
                                if (openBracketsCount > closeBracketsCount) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .clickable {
                                                val updated = currentLine.elements + CalcElement("OPERATOR", ")")
                                                builderLine = currentLine.copy(elements = updated)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(")", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            // Action buttons: Backspace, Cancel, Finish
                            if (elementCount > 0) {
                                IconButton(
                                    onClick = {
                                        val updated = currentLine.elements.dropLast(1)
                                        builderLine = currentLine.copy(elements = updated)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = Color.White)
                                }
                            }
                            IconButton(
                                onClick = {
                                    builderLine = null
                                    editingLineId = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = "Cancel Builder", tint = MaterialTheme.colorScheme.error)
                            }
                            // Finish button (available only if we have at least 1 box, and brackets are balanced, and currently waiting for an operator / closed)
                            val openBracketsCount = currentLine.elements.count { it.value == "(" }
                            val closeBracketsCount = currentLine.elements.count { it.value == ")" }
                            val hasBox = currentLine.elements.any { it.type == "BOX" }
                            val canFinish = hasBox && openBracketsCount == closeBracketsCount && !isNextBox
                            if (canFinish) {
                                IconButton(
                                    onClick = {
                                        val built = builderLine!!
                                        val editId = editingLineId
                                        if (editId != null) {
                                            // Replace existing
                                            val index = viewModel.activeCalculationLines.indexOfFirst { it.id == editId }
                                            if (index != -1) {
                                                viewModel.activeCalculationLines[index] = built
                                            }
                                        } else {
                                            // Add new
                                            viewModel.activeCalculationLines.add(built)
                                        }
                                        viewModel.takeHistorySnapshot()
                                        builderLine = null
                                        editingLineId = null
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Save Equation", tint = Color(0xFFB9F6CA))
                                }
                            }
                        }
                    }
                }
                
                // SMART CANVAS WORKSPACE RENDERING
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val scrollOffsetDp = with(density) { editorScrollState.value.toDp().value }
                    // Bottom-layer text editor workspace
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (isViewingMode || isMathNote) 0.dp else 48.dp)
                            .padding(16.dp)
                    ) {
                        MarkdownRichTextEditor(
                            value = richTextState,
                            onValueChange = {
                                richTextState = it
                                viewModel.updateTitleAndBody(titleState, it.text)
                            },
                            selectedBgColor = selectedBgColor,
                            onBgColorSelected = { selectedBgColor = it },
                            scrollState = editorScrollState,
                            isViewingMode = isViewingMode,
                            isMathNote = isMathNote,
                            modifier = Modifier.fillMaxSize(),
                            floatingCanvasContent = {
                                if (!isMathNote) {
                                    FloatingCanvas(
                                        copyBoxes = viewModel.activeCopyBoxes,
                                        pictures = viewModel.activePictures,
                                        onUpdateCopyBox = viewModel::updateCopyBox,
                                        onDeleteCopyBox = viewModel::deleteCopyBox,
                                        onUpdatePicture = viewModel::updatePicture,
                                        onDeletePicture = viewModel::deletePicture,
                                        onSelectCropPicture = { pictureToCrop = it },
                                        backgroundColor = Color.Transparent,
                                        showGrid = false,
                                        scrollOffset = editorScrollState.value,
                                        isViewingMode = isViewingMode
                                    )
                                }
                            }
                        )
                    }
                    if (!isViewingMode && !isMathNote) {
                        // Control Floating bar at the bottom center to spawn CopyBoxes / Upload Photos
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Add:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 1.dp)
                                )
                                FilledTonalButton(
                                    onClick = { viewModel.addCopyBox(scrollOffsetDp) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.PostAdd, contentDescription = "Add copy item", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Copy Box", fontSize = 10.sp, maxLines = 1, softWrap = false)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        pickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Picture", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Picture", fontSize = 10.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Crop dialog overlay
    pictureToCrop?.let { pic ->
        CropperDialog(
            imagePath = pic.localUri,
            onDismiss = { pictureToCrop = null },
            onCropComplete = { newCroppedFilePath ->
                viewModel.updatePicture(pic.copy(localUri = newCroppedFilePath))
                pictureToCrop = null
            }
        )
    }
    
    // Counter renaming dialog overlay
    renamingColorByLabel?.let { color ->
        AlertDialog(
            onDismissRequest = { renamingColorByLabel = null },
            title = { Text("Rename ${color} Counter", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a new display name for the ${color} counter:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                    OutlinedTextField(
                        value = newLabelText,
                        onValueChange = { newLabelText = it },
                        placeholder = { Text("e.g. Savings, Grocery, Rent", color = Color.White.copy(alpha = 0.4f)) },
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
                    if (newLabelText.isNotBlank()) {
                        when (color) {
                            "Red" -> viewModel.redLabel.value = newLabelText
                            "Blue" -> viewModel.blueLabel.value = newLabelText
                            "Green" -> viewModel.greenLabel.value = newLabelText
                            "Yellow" -> viewModel.yellowLabel.value = newLabelText
                        }
                        viewModel.takeHistorySnapshot()
                    }
                    renamingColorByLabel = null
                }) {
                    Text("Rename", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingColorByLabel = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF26273B)
        )
    }
    
    if (showCustomNumberDialog && builderLine != null) {
        val currentLine = builderLine!!
        AlertDialog(
            onDismissRequest = { showCustomNumberDialog = false },
            title = { Text("Input Custom Number", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Enter a numeric value to use directly in this equation:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    OutlinedTextField(
                        value = customNumberInputVal,
                        onValueChange = { customNumberInputVal = it },
                        placeholder = { Text("e.g. 5, 12.5, 100", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
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
                    val cleanVal = customNumberInputVal.trim()
                    if (cleanVal.toDoubleOrNull() != null) {
                        val updated = currentLine.elements + CalcElement("BOX", cleanVal)
                        builderLine = currentLine.copy(elements = updated)
                        showCustomNumberDialog = false
                    }
                }) {
                    Text("Add", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomNumberDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF26273B)
        )
    }
    
    if (editingNumElementIndex != null) {
        AlertDialog(
            onDismissRequest = { editingNumElementIndex = null },
            title = { Text("Edit Number Value", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type the new value for this number:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    OutlinedTextField(
                        value = editingNumElementValue,
                        onValueChange = { editingNumElementValue = it },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
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
                    val newVal = editingNumElementValue.trim()
                    if (newVal.toDoubleOrNull() != null) {
                        val lineToEdit = editingNumElementLine
                        val indexToEdit = editingNumElementIndex
                        if (lineToEdit != null && indexToEdit != null) {
                            val updatedElements = lineToEdit.elements.mapIndexed { idx, el ->
                                if (idx == indexToEdit) el.copy(value = newVal) else el
                            }
                            val updatedLine = lineToEdit.copy(elements = updatedElements)
                            val idxInList = viewModel.activeCalculationLines.indexOf(lineToEdit)
                            if (idxInList != -1) {
                                viewModel.activeCalculationLines[idxInList] = updatedLine
                            }
                        } else if (indexToEdit != null) {
                            val current = builderLine
                            if (current != null) {
                                val updatedElements = current.elements.mapIndexed { idx, el ->
                                    if (idx == indexToEdit) el.copy(value = newVal) else el
                                }
                                builderLine = current.copy(elements = updatedElements)
                            }
                        }
                        editingNumElementIndex = null
                    }
                }) {
                    Text("Done", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNumElementIndex = null }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF26273B)
        )
    }
}
