@file:Suppress("DEPRECATION")
package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Note
import com.example.data.model.Sun
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import com.example.ui.viewmodel.NoteViewModel

// Sticky Note Whiteboard - Scribble and Stroke Serialization Models
data class ScribbleStroke(
    val colorHex: String,
    val thickness: Float,
    val points: List<Offset>
)

fun serializeScribbles(strokes: List<ScribbleStroke>): String {
    if (strokes.isEmpty()) return ""
    return strokes.joinToString("|") { stroke ->
        val pointsStr = stroke.points.joinToString(";") { "${it.x},${it.y}" }
        "${stroke.colorHex}_${stroke.thickness}_$pointsStr"
    }
}

fun deserializeScribbles(serialized: String): List<ScribbleStroke> {
    if (serialized.isBlank()) return emptyList()
    return try {
        serialized.split("|").mapNotNull { strokeStr ->
            val mainParts = strokeStr.split("_")
            if (mainParts.size < 3) return@mapNotNull null
            val colorHex = mainParts[0]
            val thickness = mainParts[1].toFloatOrNull() ?: 4f
            val pointsStr = mainParts[2]
            val points = pointsStr.split(";").mapNotNull { ptStr ->
                val coords = ptStr.split(",")
                if (coords.size == 2) {
                    val x = coords[0].toFloatOrNull()
                    val y = coords[1].toFloatOrNull()
                    if (x != null && y != null) Offset(x, y) else null
                } else null
            }
            if (points.isNotEmpty()) ScribbleStroke(colorHex, thickness, points) else null
        }
    } catch (e: Exception) {
        emptyList()
    }
}

data class NotePlacement(
    val noteId: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

fun serializePlacements(placements: Map<Int, NotePlacement>): String {
    return placements.values.joinToString("|") {
        "${it.noteId}_${it.x}_${it.y}_${it.width}_${it.height}"
    }
}

fun deserializePlacements(serialized: String): Map<Int, NotePlacement> {
    if (serialized.isBlank()) return emptyMap()
    val map = mutableMapOf<Int, NotePlacement>()
    try {
        serialized.split("|").forEach { part ->
            val sub = part.split("_")
            if (sub.size >= 5) {
                val noteId = sub[0].toIntOrNull()
                val x = sub[1].toFloatOrNull()
                val y = sub[2].toFloatOrNull()
                val w = sub[3].toFloatOrNull()
                val h = sub[4].toFloatOrNull()
                if (noteId != null && x != null && y != null && w != null && h != null) {
                    map[noteId] = NotePlacement(noteId, x, y, w, h)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return map
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSpaceScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onOpenNote: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }
    val notes by viewModel.allNotes.collectAsState()
    val suns by viewModel.allSuns.collectAsState()
    var activeSunId by viewModel.selectedSunId
    
    // Auto-select first sun (Whiteboard)
    LaunchedEffect(suns) {
        if (activeSunId == null && suns.isNotEmpty()) {
            activeSunId = suns.first().id
        }
    }
    val activeSun = suns.find { it.id == activeSunId }
    
    // Mode toggles
    var isEditMode by remember { mutableStateOf(false) } // Default is Viewing mode (read-only)
    var activeTool by remember { mutableStateOf("Drag") } // "Drag", "Pen", "Eraser"
    var penColorHex by remember { mutableStateOf("#E53935") } // Default Neon Red
    var penThickness by remember { mutableStateOf(5f) }
    
    // Zoom & Pan offset states
    var scale by remember { mutableStateOf(1f) }
    var boardOffset by remember { mutableStateOf(Offset.Zero) }
    
    // Live drawing strokes state, synced from database
    val liveStrokes = remember { mutableStateListOf<ScribbleStroke>() }
    LaunchedEffect(activeSun?.id) {
        liveStrokes.clear()
        activeSun?.let { sun ->
            liveStrokes.addAll(deserializeScribbles(sun.planetStyle))
        }
        // Reset scale & pan on board swap
        scale = 1f
        boardOffset = Offset.Zero
        // Default to View Mode on board swap
        isEditMode = false
        activeTool = "Drag"
    }
    
    // Dialog & overlay controls
    var showCreateSunDialog by remember { mutableStateOf(false) }
    var showEditPlanetsDialog by remember { mutableStateOf(false) }
    var showCustomizeSystemDialog by remember { mutableStateOf(false) }
    var showDeleteSunConfirmDialog by remember { mutableStateOf<Sun?>(null) }
    var selectedNoteForCustomization by remember { mutableStateOf<Note?>(null) }
    
    // Background configuration (Support custom aspect ratios and style parts)
    val styleParts = remember(activeSun?.sunStyle) {
        (activeSun?.sunStyle ?: "Old Green Chalkboard").split("|")
    }
    val styleName = styleParts.getOrNull(0) ?: "Old Green Chalkboard"
    val aspectName = styleParts.getOrNull(1) ?: "Widescreen"
    
    val isLightBg = styleName == "Modern Whiteboard"
    val dynamicBackground = remember(styleName) {
        when (styleName) {
            "Old Green Chalkboard" -> Brush.verticalGradient(listOf(Color(0xFF1B4D3E), Color(0xFF143B2F)))
            "Modern Whiteboard" -> Brush.verticalGradient(listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC)))
            "Cork Board" -> Brush.verticalGradient(listOf(Color(0xFFD7CCC8), Color(0xFFBCAAA4)))
            "Digital Grid Board" -> Brush.verticalGradient(listOf(Color(0xFF0B0D19), Color(0xFF161933)))
            else -> Brush.verticalGradient(listOf(Color(0xFF1B4D3E), Color(0xFF143B2F)))
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeSun?.name ?: "Interactive Whiteboards",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLightBg) Color.Black else Color.White
                            )
                        )
                        if (isEditMode && activeSun != null) {
                            Text(
                                text = "Board Design: ${activeSun.sunStyle}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isLightBg) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = if (isLightBg) Color.Black else Color.White
                        )
                    }
                },
                actions = {
                    if (isEditMode && activeSun != null) {
                        // Assign Notes button
                        IconButton(onClick = { showEditPlanetsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Assign Sticky Notes",
                                tint = if (isLightBg) Color.Black else Color.White
                            )
                        }
                        // Whiteboard settings
                        IconButton(onClick = { showCustomizeSystemDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Whiteboard Settings",
                                tint = if (isLightBg) Color.Black else Color.White
                            )
                        }
                        // Delete Whiteboard
                        IconButton(onClick = { showDeleteSunConfirmDialog = activeSun }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Whiteboard",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLightBg) Color(0xFFCFD8DC) else Color(0xFF0F1124)
                )
            )
        },
        containerColor = Color(0xFF14172E)
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(dynamicBackground)
        ) {
            if (suns.isEmpty()) {
                EmptyUniverseState(onCreateSun = { showCreateSunDialog = true })
            } else if (activeSun != null) {
                val linkedNotes = remember(activeSun, notes) {
                    val ids = viewModel.deserializeIntList(activeSun.noteIdsJson)
                    notes.filter { it.id in ids }
                }
                
                // Determine board dimensions in Dp
                val boardWidthDp = when (aspectName) {
                    "Widescreen" -> 3200.dp
                    "Square" -> 2400.dp
                    "Portrait" -> 1600.dp
                    "Infinite Desk" -> 4000.dp
                    else -> 3200.dp
                }
                
                val boardHeightDp = when (aspectName) {
                    "Widescreen" -> 1800.dp
                    "Square" -> 2400.dp
                    "Portrait" -> 2840.dp
                    "Infinite Desk" -> 2500.dp
                    else -> 1800.dp
                }
                
                val bottomPadding = 60.dp
                
                // Main Infinite Scrollable/Zoomable Board Area
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding) // Bottom selector dock padding
                        .pointerInput(isEditMode, activeTool) {
                            // If in View Mode, or in Edit Mode but using the "Drag/Pan" tool, support dual-finger pinch & drag the whole board!
                            if (!isEditMode || activeTool == "Drag") {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val oldScale = scale
                                    scale = (scale * zoom).coerceIn(0.15f, 2.5f)
                                    // Zoom around centroid
                                    boardOffset = centroid - (centroid - boardOffset) * (scale / oldScale) + pan
                                }
                            }
                        }
                ) {
                    val screenWidthPx = constraints.maxWidth.toFloat()
                    val screenHeightPx = constraints.maxHeight.toFloat()
                    
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val boardWidthPx = with(density) { boardWidthDp.toPx() }
                    val boardHeightPx = with(density) { boardHeightDp.toPx() }
                    
                    // Auto-center on load or screen resize
                    LaunchedEffect(activeSun.id, screenWidthPx, screenHeightPx, boardWidthPx, boardHeightPx) {
                        if (screenWidthPx > 0 && screenHeightPx > 0) {
                            scale = 1f
                            boardOffset = Offset.Zero
                        }
                    }
                    
                    // Constrain offsets to keep the board sheet partially on the screen (no flying off!)
                    val clampedOffset = remember(boardOffset, scale, screenWidthPx, screenHeightPx, boardWidthPx, boardHeightPx) {
                        if (screenWidthPx <= 0f || screenHeightPx <= 0f) boardOffset
                        else {
                            val margin = 100f * scale
                            val minX = -boardWidthPx * scale + margin
                            val maxX = screenWidthPx - margin
                            val minY = -boardHeightPx * scale + margin
                            val maxY = screenHeightPx - margin
                            Offset(
                                x = boardOffset.x.coerceIn(minX, maxX),
                                y = boardOffset.y.coerceIn(minY, maxY)
                            )
                        }
                    }
                    
                    // Visual Desktop container (creates a gorgeous desk border under the board)
                    Box(
                        modifier = Modifier
                            .size(boardWidthDp, boardHeightDp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = clampedOffset.x
                                translationY = clampedOffset.y
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(8.dp),
                                clip = false,
                                ambientColor = Color.Black.copy(alpha = 0.5f),
                                spotColor = Color.Black.copy(alpha = 0.6f)
                            )
                            .border(
                                width = 4.dp,
                                color = if (isLightBg) Color(0xFF90A4AE) else Color(0xFF2F356B),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        // 1. Board Background Canvas Grids & Chalk Dust
                        BoardBackground(style = styleName)
                        
                        // 2. Vector Drawings & Scribble Canvas
                        ScribbleCanvas(
                            isEditMode = isEditMode,
                            activeTool = activeTool,
                            penColorHex = penColorHex,
                            penThickness = penThickness,
                            liveStrokes = liveStrokes,
                            activeSun = activeSun,
                            viewModel = viewModel
                        )
                        
                        // 3. Draggable/Interactive Sticky Notes
                        WhiteboardLayout(
                            activeSun = activeSun,
                            notes = linkedNotes,
                            isEditMode = isEditMode,
                            scale = scale,
                            onOpenNote = onOpenNote,
                            onConfigureSticky = { selectedNoteForCustomization = it },
                            viewModel = viewModel
                        )
                    }
                }
                
                // --- Right-Aligned Slim Vertical Toolbox for Edit Mode ---
                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 12.dp, top = 80.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = (if (isLightBg) Color.White else Color(0xFF0F1124)).copy(alpha = 0.9f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isLightBg) Color.Gray.copy(alpha = 0.2f) else Color(0xFF1F2245)
                            ),
                            tonalElevation = 8.dp,
                            modifier = Modifier.width(48.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Reset View
                                IconButton(
                                    onClick = {
                                        scale = 1f
                                        boardOffset = Offset.Zero
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset View",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                HorizontalDivider(
                                    modifier = Modifier.width(24.dp),
                                    color = (if (isLightBg) Color.Black else Color.White).copy(alpha = 0.1f)
                                )
                                
                                // Move/Pan Tool
                                val isDrag = activeTool == "Drag"
                                IconButton(
                                    onClick = { activeTool = "Drag" },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isDrag) Color(0xFFFF9800) else Color.Transparent,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenWith,
                                        contentDescription = "Pan Board",
                                        tint = if (isDrag) Color.Black else (if (isLightBg) Color.Black else Color.White),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                // Pen Tool
                                val isPen = activeTool == "Pen"
                                IconButton(
                                    onClick = { activeTool = "Pen" },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isPen) Color(0xFFFF9800) else Color.Transparent,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Pen Tool",
                                        tint = if (isPen) Color.Black else (if (isLightBg) Color.Black else Color.White),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                // Eraser Tool
                                val isEraser = activeTool == "Eraser"
                                IconButton(
                                    onClick = { activeTool = "Eraser" },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isEraser) Color(0xFFFF9800) else Color.Transparent,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Eraser",
                                        tint = if (isEraser) Color.Black else (if (isLightBg) Color.Black else Color.White),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                HorizontalDivider(
                                    modifier = Modifier.width(24.dp),
                                    color = (if (isLightBg) Color.Black else Color.White).copy(alpha = 0.1f)
                                )
                                
                                // Clear Ink
                                IconButton(
                                    onClick = {
                                        liveStrokes.clear()
                                        viewModel.updateSun(activeSun.copy(planetStyle = ""))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear Ink",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Color popout for Pen Tool
                if (isEditMode && activeTool == "Pen") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 68.dp, top = 80.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = (if (isLightBg) Color.White else Color(0xFF0F1124)).copy(alpha = 0.9f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isLightBg) Color.Gray.copy(alpha = 0.2f) else Color(0xFF1F2245)
                            ),
                            tonalElevation = 8.dp,
                            modifier = Modifier.width(44.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val colorsList = listOf("#E53935", "#1E88E5", "#43A047", "#FFB300", "#FFFFFF", "#000000")
                                colorsList.forEach { hex ->
                                    val isSelected = penColorHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                Color(android.graphics.Color.parseColor(hex)),
                                                CircleShape
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) (if (isLightBg) Color.Black else Color.White) else Color.Gray.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable { penColorHex = hex }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Bottom Whiteboards Selector Dock (always visible, ultra compact!)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomSystemsDock(
                        suns = suns,
                        activeSunId = activeSunId,
                        notes = notes,
                        onSelectSystem = { activeSunId = it },
                        onCreateSystem = { showCreateSunDialog = true },
                        viewModel = viewModel,
                        isLightBg = isLightBg,
                        isEditMode = isEditMode,
                        onToggleEditMode = {
                            isEditMode = !isEditMode
                            activeTool = "Drag"
                        }
                    )
                }
            }
        }
    }
    
    // --- POPUPS & DIALOGS ---
    // 1. Create Whiteboard Dialog
    if (showCreateSunDialog) {
        var sunName by remember { mutableStateOf("") }
        var selectedStyle by remember { mutableStateOf("Old Green Chalkboard") }
        var selectedAspect by remember { mutableStateOf("Widescreen") }
        var customColorHex by remember { mutableStateOf("#FF9800") }
        AlertDialog(
            onDismissRequest = { showCreateSunDialog = false },
            title = { Text("Create New Whiteboard", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = sunName,
                        onValueChange = { sunName = it },
                        label = { Text("Whiteboard Title") },
                        placeholder = { Text("e.g., Biology Studies, Physics Formulas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Board Design Background Style", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    val styleOptions = listOf("Old Green Chalkboard", "Modern Whiteboard", "Cork Board", "Digital Grid Board")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styleOptions.forEach { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                                label = { Text(style) }
                            )
                        }
                    }
                    Text("Board Size & Aspect Ratio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    val aspectOptions = listOf("Widescreen", "Square", "Portrait", "Infinite Desk")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aspectOptions.forEach { aspect ->
                            FilterChip(
                                selected = selectedAspect == aspect,
                                onClick = { selectedAspect = aspect },
                                label = { Text(aspect) }
                            )
                        }
                    }
                    Text("Board Accent Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    val paletteColors = listOf("#FF9800", "#FF5722", "#00E5FF", "#00E676", "#9C27B0", "#E91E63", "#FFD600")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paletteColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                    .border(
                                        width = if (customColorHex == hex) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { customColorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sunName.isNotBlank()) {
                            viewModel.saveSun(
                                name = sunName.trim(),
                                noteIds = emptyList(),
                                themeIndex = 2, // Forced to Whiteboard theme index
                                sunStyle = "$selectedStyle|$selectedAspect",
                                planetStyle = "",
                                customColorHex = customColorHex
                            )
                            showCreateSunDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black)
                ) {
                    Text("Create Whiteboard", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSunDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // 2. Assign / Link Notes Checklist Dialog
    if (showEditPlanetsDialog && activeSun != null) {
        val assignedIds = remember(activeSun) {
            viewModel.deserializeIntList(activeSun.noteIdsJson).toSet()
        }
        var selectedNoteIds by remember { mutableStateOf(assignedIds) }
        var searchQuery by remember { mutableStateOf("") }
        var sortBy by remember { mutableStateOf("Newest") } // "Newest" or "Alphabetical"
        var showOnlyFavorites by remember { mutableStateOf(false) }
        var groupByType by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showEditPlanetsDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Assign Sticky Notes to Board",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Link study notes to display them on this whiteboard workspace.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Sleek Search Input Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search notes...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Filter & Sort row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorites Filter Chip
                        FilterChip(
                            selected = showOnlyFavorites,
                            onClick = { showOnlyFavorites = !showOnlyFavorites },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (showOnlyFavorites) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text("Favorites Only", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFFFF9800)
                            )
                        )

                        // Group by Type Chip
                        FilterChip(
                            selected = groupByType,
                            onClick = { groupByType = !groupByType },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (groupByType) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text("Group by Type", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFFFF9800)
                            )
                        )

                        // Sort Order Chip
                        FilterChip(
                            selected = true,
                            onClick = {
                                sortBy = if (sortBy == "Newest") "Alphabetical" else "Newest"
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (sortBy == "Newest") Icons.Default.ArrowDownward else Icons.Default.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = { Text(sortBy, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Process & Filter list of notes
                    val filteredNotes = remember(notes, searchQuery, sortBy, showOnlyFavorites) {
                        var result = notes.filter { note ->
                            val matchesSearch = note.title.contains(searchQuery, ignoreCase = true) || 
                                                note.bodyText.contains(searchQuery, ignoreCase = true)
                            val matchesFav = !showOnlyFavorites || note.isFavorite
                            matchesSearch && matchesFav
                        }
                        
                        result = when (sortBy) {
                            "Alphabetical" -> result.sortedBy { it.title.lowercase() }
                            "Newest" -> result.sortedByDescending { it.timestamp }
                            else -> result.sortedByDescending { it.timestamp }
                        }
                        result
                    }

                    if (filteredNotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (notes.isEmpty()) "No notes found. Create some notes in the dashboard first!" else "No notes match your filter query.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (groupByType) {
                                val standardNotes = filteredNotes.filter { !it.isMathNote }
                                val mathNotes = filteredNotes.filter { it.isMathNote }
                                
                                if (standardNotes.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Standard Spatial Notes (${standardNotes.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFFF9800),
                                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                        )
                                    }
                                    items(standardNotes, key = { it.id }) { note ->
                                        NoteSelectionItem(
                                            note = note,
                                            isSelected = note.id in selectedNoteIds,
                                            onToggle = { isChecked ->
                                                selectedNoteIds = if (isChecked) {
                                                    selectedNoteIds + note.id
                                                } else {
                                                    selectedNoteIds - note.id
                                                }
                                            }
                                        )
                                    }
                                }
                                
                                if (mathNotes.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Math Ledgers (${mathNotes.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFFF9800),
                                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                        )
                                    }
                                    items(mathNotes, key = { it.id }) { note ->
                                        NoteSelectionItem(
                                            note = note,
                                            isSelected = note.id in selectedNoteIds,
                                            onToggle = { isChecked ->
                                                selectedNoteIds = if (isChecked) {
                                                    selectedNoteIds + note.id
                                                } else {
                                                    selectedNoteIds - note.id
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                items(filteredNotes, key = { it.id }) { note ->
                                    NoteSelectionItem(
                                        note = note,
                                        isSelected = note.id in selectedNoteIds,
                                        onToggle = { isChecked ->
                                            selectedNoteIds = if (isChecked) {
                                                selectedNoteIds + note.id
                                            } else {
                                                selectedNoteIds - note.id
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditPlanetsDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val updatedSun = activeSun.copy(
                                    noteIdsJson = viewModel.serializeIntList(selectedNoteIds.toList())
                                )
                                viewModel.updateSun(updatedSun)
                                showEditPlanetsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black)
                        ) {
                            Text("Apply Sticky Notes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    
    // 3. Customize Whiteboard Settings Dialog
    if (showCustomizeSystemDialog && activeSun != null) {
        val boardParts = activeSun.sunStyle.split("|")
        val initialStyle = boardParts.getOrNull(0) ?: "Old Green Chalkboard"
        val initialAspect = boardParts.getOrNull(1) ?: "Widescreen"
        
        var sunName by remember { mutableStateOf(activeSun.name) }
        var selectedStyle by remember { mutableStateOf(initialStyle) }
        var selectedAspect by remember { mutableStateOf(initialAspect) }
        var customColorHex by remember { mutableStateOf(if (activeSun.customColorHex.isEmpty()) "#FF9800" else activeSun.customColorHex) }
        AlertDialog(
            onDismissRequest = { showCustomizeSystemDialog = false },
            title = { Text("Whiteboard Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = sunName,
                        onValueChange = { sunName = it },
                        label = { Text("Whiteboard Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Board Background Design", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    val styleOptions = listOf("Old Green Chalkboard", "Modern Whiteboard", "Cork Board", "Digital Grid Board")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styleOptions.forEach { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                                label = { Text(style) }
                            )
                        }
                    }
                    Text("Board Size & Aspect Ratio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    val aspectOptions = listOf("Widescreen", "Square", "Portrait", "Infinite Desk")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        aspectOptions.forEach { aspect ->
                            FilterChip(
                                selected = selectedAspect == aspect,
                                onClick = { selectedAspect = aspect },
                                label = { Text(aspect) }
                            )
                        }
                    }
                    Text("Board Theme Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    val paletteColors = listOf("#FF9800", "#FF5722", "#00E5FF", "#00E676", "#9C27B0", "#E91E63", "#FFD600")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paletteColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                    .border(
                                        width = if (customColorHex == hex) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { customColorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sunName.isNotBlank()) {
                            val updatedSun = activeSun.copy(
                                name = sunName.trim(),
                                themeIndex = 2, // Always Sticky Note Board
                                sunStyle = "$selectedStyle|$selectedAspect",
                                customColorHex = customColorHex
                            )
                            viewModel.updateSun(updatedSun)
                            showCustomizeSystemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black)
                ) {
                    Text("Save Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomizeSystemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // 4. Delete Whiteboard Confirmation Dialog
    if (showDeleteSunConfirmDialog != null) {
        val sunToDelete = showDeleteSunConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteSunConfirmDialog = null },
            title = { Text("Delete Whiteboard?") },
            text = { Text("Are you sure you want to delete '${sunToDelete.name}'? All scribbles will be permanently lost. The actual study notes will remain completely untouched.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSun(sunToDelete)
                        showDeleteSunConfirmDialog = null
                        activeSunId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Board", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSunConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // 5. Sticky Note Node Customizer Panel
    if (selectedNoteForCustomization != null) {
        val targetNote = selectedNoteForCustomization!!
        AlertDialog(
            onDismissRequest = { selectedNoteForCustomization = null },
            title = {
                Column {
                    Text(
                        text = targetNote.title.ifBlank { "Untitled Sticky Note" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Sticky Customizer Settings",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Action: open note details
                    Button(
                        onClick = {
                            selectedNoteForCustomization = null
                            onOpenNote(targetNote.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Full Note Content", fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Text("Sticky Paper Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    val pastelColors = listOf(
                        "#FFF59D" to "Pastel Yellow",
                        "#F8BBD0" to "Pastel Pink",
                        "#C8E6C9" to "Pastel Green",
                        "#B3E5FC" to "Pastel Blue",
                        "#E1BEE7" to "Pastel Purple"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pastelColors.forEach { (hex, name) ->
                            val isSel = targetNote.readerStickyColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), RoundedCornerShape(4.dp))
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) Color.Black else Color.Gray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        val updated = targetNote.copy(readerStickyColorHex = hex)
                                        viewModel.updateNote(updated)
                                        selectedNoteForCustomization = updated
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Paper Texture Lining Style", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    val textures = listOf("Plain", "Lined", "Grid", "Parchment")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        textures.forEach { tex ->
                            val isSel = targetNote.readerStickyTexture == tex
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    val updated = targetNote.copy(readerStickyTexture = tex)
                                    viewModel.updateNote(updated)
                                    selectedNoteForCustomization = updated
                                },
                                label = { Text(tex) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedNoteForCustomization = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.Black)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// --- CORE LAYOUT COMPONENT: WHITEBOARD GRIDS ---
@Composable
fun BoardBackground(style: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        // Fill solid background based on chosen board design
        val bgBrush = when (style) {
            "Old Green Chalkboard" -> Brush.verticalGradient(listOf(Color(0xFF1B4D3E), Color(0xFF143B2F)))
            "Modern Whiteboard" -> Brush.verticalGradient(listOf(Color(0xFFECEFF1), Color(0xFFCFD8DC)))
            "Cork Board" -> Brush.verticalGradient(listOf(Color(0xFFD7CCC8), Color(0xFFBCAAA4)))
            "Digital Grid Board" -> Brush.verticalGradient(listOf(Color(0xFF0B0D19), Color(0xFF161933)))
            else -> Brush.verticalGradient(listOf(Color(0xFF1B4D3E), Color(0xFF143B2F)))
        }
        drawRect(brush = bgBrush)
        // Draw ambient detail overlays (extending layout for panning support)
        when (style) {
            "Old Green Chalkboard" -> {
                // Dusty chalk board spots
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)),
                    radius = w * 0.4f,
                    center = Offset(w * 0.5f, h * 0.4f)
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.03f), Color.Transparent)),
                    radius = w * 0.5f,
                    center = Offset(w * 0.2f, h * 0.7f)
                )
            }
            "Modern Whiteboard" -> {
                // Subtle blue graph grid
                val step = 45.dp.toPx()
                val lineCol = Color(0xFF90CAF9).copy(alpha = 0.15f)
                var px = 0f
                while (px < w * 3f) {
                    drawLine(lineCol, Offset(px - w, -h), Offset(px - w, h * 2), strokeWidth = 1f)
                    px += step
                }
                var py = 0f
                while (py < h * 3f) {
                    drawLine(lineCol, Offset(-w, py - h), Offset(w * 2, py - h), strokeWidth = 1f)
                    py += step
                }
            }
            "Digital Grid Board" -> {
                // High contrast technical grid lines
                val step = 32.dp.toPx()
                val lineCol = Color(0xFF00E5FF).copy(alpha = 0.07f)
                var px = 0f
                while (px < w * 3f) {
                    drawLine(lineCol, Offset(px - w, -h), Offset(px - w, h * 2), strokeWidth = 1f)
                    px += step
                }
                var py = 0f
                while (py < h * 3f) {
                    drawLine(lineCol, Offset(-w, py - h), Offset(w * 2, py - h), strokeWidth = 1f)
                    py += step
                }
            }
        }
    }
}

// --- CORE LAYOUT COMPONENT: DRAWING CANVAS ---
@Composable
fun ScribbleCanvas(
    isEditMode: Boolean,
    activeTool: String,
    penColorHex: String,
    penThickness: Float,
    liveStrokes: SnapshotStateList<ScribbleStroke>,
    activeSun: Sun?,
    viewModel: NoteViewModel
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isEditMode, activeTool, activeSun?.id) {
                if (isEditMode && activeTool != "Drag" && activeSun != null) {
                    detectDragGestures(
                        onDragStart = { startPos ->
                            val stroke = ScribbleStroke(
                                colorHex = if (activeTool == "Eraser") "Eraser" else penColorHex,
                                thickness = if (activeTool == "Eraser") 32f else penThickness,
                                points = listOf(startPos)
                            )
                            liveStrokes.add(stroke)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeTool == "Eraser") {
                                val currentPos = change.position
                                liveStrokes.removeAll { stroke ->
                                    stroke.points.any { pt ->
                                        (pt - currentPos).getDistance() < 30f
                                    }
                                }
                            } else {
                                val lastIndex = liveStrokes.lastIndex
                                if (lastIndex != -1) {
                                    val lastStroke = liveStrokes[lastIndex]
                                    val nextPt = lastStroke.points.last() + dragAmount
                                    liveStrokes[lastIndex] = lastStroke.copy(points = lastStroke.points + nextPt)
                                }
                            }
                        },
                        onDragEnd = {
                            liveStrokes.removeAll { it.colorHex == "Eraser" }
                            viewModel.updateSun(activeSun.copy(planetStyle = serializeScribbles(liveStrokes)))
                        },
                        onDragCancel = {
                            liveStrokes.removeAll { it.colorHex == "Eraser" }
                            viewModel.updateSun(activeSun.copy(planetStyle = serializeScribbles(liveStrokes)))
                        }
                    )
                }
            }
    ) {
        liveStrokes.forEach { stroke ->
            if (stroke.colorHex != "Eraser" && stroke.points.size > 1) {
                val path = Path().apply {
                    val first = stroke.points.first()
                    moveTo(first.x, first.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(android.graphics.Color.parseColor(stroke.colorHex)),
                    style = Stroke(
                        width = stroke.thickness,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (stroke.colorHex != "Eraser" && stroke.points.size == 1) {
                val pt = stroke.points.first()
                drawCircle(
                    color = Color(android.graphics.Color.parseColor(stroke.colorHex)),
                    radius = stroke.thickness / 2f,
                    center = pt
                )
            }
        }
    }
}

// --- CORE LAYOUT COMPONENT: STICKY NOTES DISPATCHER ---
@Composable
fun WhiteboardLayout(
    activeSun: Sun,
    notes: List<Note>,
    isEditMode: Boolean,
    scale: Float,
    onOpenNote: (Int) -> Unit,
    onConfigureSticky: (Note) -> Unit,
    viewModel: NoteViewModel
) {
    val placements = remember(activeSun.notePlacementsJson) {
        deserializePlacements(activeSun.notePlacementsJson)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val wDp = maxWidth.value
        val hDp = maxHeight.value
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notes on this board.\nTap 'Assign Sticky Notes' in the top bar to place note cards!",
                    textAlign = TextAlign.Center,
                    color = if (activeSun.sunStyle == "Modern Whiteboard") Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)
                )
            }
        }
        notes.forEachIndexed { index, note ->
            val placement = placements[note.id]
            
            // Stagger default positions if not set
            val defaultX = 150f + (index % 5) * 60f
            val defaultY = 150f + (index / 5) * 60f
            
            val stickyWidth = placement?.width ?: 140f
            val stickyHeight = placement?.height ?: 140f
            val stickyX = (placement?.x ?: defaultX).coerceIn(4f, wDp - stickyWidth)
            val stickyY = (placement?.y ?: defaultY).coerceIn(4f, hDp - stickyHeight)
            
            val noteWithPlacement = note.copy(
                readerStickyWidth = stickyWidth,
                readerStickyHeight = stickyHeight
            )
            
            StickyNoteCard(
                note = noteWithPlacement,
                initialX = stickyX,
                initialY = stickyY,
                maxX = wDp,
                maxY = hDp,
                isEditMode = isEditMode,
                scale = scale,
                onDragEnd = { finalX, finalY ->
                    val updatedPlacements = placements.toMutableMap()
                    updatedPlacements[note.id] = NotePlacement(
                        noteId = note.id,
                        x = finalX,
                        y = finalY,
                        width = stickyWidth,
                        height = stickyHeight
                    )
                    viewModel.updateSun(activeSun.copy(
                        notePlacementsJson = serializePlacements(updatedPlacements)
                    ))
                },
                onResizeEnd = { finalW, finalH ->
                    val updatedPlacements = placements.toMutableMap()
                    updatedPlacements[note.id] = NotePlacement(
                        noteId = note.id,
                        x = stickyX,
                        y = stickyY,
                        width = finalW,
                        height = finalH
                    )
                    viewModel.updateSun(activeSun.copy(
                        notePlacementsJson = serializePlacements(updatedPlacements)
                    ))
                },
                onClick = {
                    if (isEditMode) {
                        onConfigureSticky(noteWithPlacement)
                    } else {
                        onOpenNote(note.id)
                    }
                },
                boardStyle = activeSun.sunStyle
            )
        }
    }
}

// --- INDIVIDUAL STICKY NOTE CARD ---
@Composable
fun StickyNoteCard(
    note: Note,
    initialX: Float,
    initialY: Float,
    maxX: Float,
    maxY: Float,
    isEditMode: Boolean,
    scale: Float,
    onDragEnd: (Float, Float) -> Unit,
    onResizeEnd: (Float, Float) -> Unit = { _, _ -> },
    onClick: () -> Unit,
    boardStyle: String
) {
    val paperColor = Color(android.graphics.Color.parseColor(note.readerStickyColorHex))
    var offsetX by remember(note.id) { mutableStateOf(initialX) }
    var offsetY by remember(note.id) { mutableStateOf(initialY) }
    
    var widthState by remember(note.id) { mutableStateOf(note.readerStickyWidth) }
    var heightState by remember(note.id) { mutableStateOf(note.readerStickyHeight) }
    
    // Sync dimensions from external model changes
    LaunchedEffect(note.readerStickyWidth, note.readerStickyHeight) {
        widthState = note.readerStickyWidth
        heightState = note.readerStickyHeight
    }
    
    // Sync positions from external databases
    LaunchedEffect(initialX, initialY) {
        offsetX = initialX
        offsetY = initialY
    }
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnResizeEnd by rememberUpdatedState(onResizeEnd)
    val currentOnClick by rememberUpdatedState(onClick)
    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .size(width = widthState.dp, height = heightState.dp)
            .pointerInput(note.id, isEditMode, scale) {
                if (isEditMode) {
                    detectDragGestures(
                        onDragStart = { },
                        onDragEnd = { currentOnDragEnd(offsetX, offsetY) },
                        onDragCancel = { currentOnDragEnd(offsetX, offsetY) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Correct translation delta for current whiteboard zoom level (Compose handles scale in pointer input, no double division needed)
                            offsetX = (offsetX + dragAmount.x.toDp().value).coerceIn(4f, maxX - widthState)
                            offsetY = (offsetY + dragAmount.y.toDp().value).coerceIn(4f, maxY - heightState)
                        }
                    )
                }
            }
            .shadow(
                elevation = if (isEditMode) 8.dp else 4.dp,
                shape = RoundedCornerShape(2.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .background(paperColor)
            .clickable { currentOnClick() }
    ) {
        // Render paper grids and lines textures
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when (note.readerStickyTexture) {
                "Lined" -> {
                    val linePadding = 18.dp.toPx()
                    var currY = linePadding * 1.5f
                    while (currY < h) {
                        drawLine(
                            color = Color(0xFF90CAF9).copy(alpha = 0.6f),
                            start = Offset(0f, currY),
                            end = Offset(w, currY),
                            strokeWidth = 1f
                        )
                        currY += linePadding
                    }
                    drawLine(
                        color = Color(0xFFEF9A9A).copy(alpha = 0.8f),
                        start = Offset(20.dp.toPx(), 0f),
                        end = Offset(20.dp.toPx(), h),
                        strokeWidth = 1f
                    )
                }
                "Grid" -> {
                    val step = 14.dp.toPx()
                    var currX = 0f
                    while (currX < w) {
                        drawLine(Color(0xFFE0E0E0), Offset(currX, 0f), Offset(currX, h), strokeWidth = 1f)
                        currX += step
                    }
                    var currY = 0f
                    while (currY < h) {
                        drawLine(Color(0xFFE0E0E0), Offset(0f, currY), Offset(w, currY), strokeWidth = 1f)
                        currY += step
                    }
                }
                "Parchment" -> {
                    drawRect(
                        brush = Brush.radialGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.08f))
                        )
                    )
                }
            }
            // Small red pushpin on corkboard style
            if (boardStyle == "Cork Board") {
                drawCircle(Color(0xFFE53935), 4.dp.toPx(), Offset(w / 2, 8.dp.toPx()))
                drawCircle(Color(0xFFB71C1C), 2.dp.toPx(), Offset(w / 2 - 1.dp.toPx(), 7.dp.toPx()))
            }
        }
        
        // Card Textual Snippet Content
        val paddingVal = if (widthState < 60f || heightState < 60f) 4.dp else 12.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVal)
        ) {
            val titleFontSize = if (widthState < 60f || heightState < 40f) 8.sp else 11.sp
            Text(
                text = note.title.ifBlank { "Untitled Note" },
                fontSize = titleFontSize,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF263238),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (heightState >= 60f && widthState >= 60f) {
                Spacer(modifier = Modifier.height(4.dp))
                val calculatedMaxLines = ((heightState - 40f) / 15f).toInt().coerceIn(1, 10)
                Text(
                    text = note.bodyText.ifBlank { "Click to add description details." },
                    fontSize = 9.sp,
                    color = Color(0xFF455A64),
                    maxLines = calculatedMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 11.sp
                )
            }
        }
        
        // Resize handle in bottom-right corner, only seen/interactable in Edit Mode
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .pointerInput(note.id) {
                        detectDragGestures(
                            onDragStart = { },
                            onDragEnd = {
                                currentOnResizeEnd(widthState, heightState)
                            },
                            onDragCancel = {
                                currentOnResizeEnd(widthState, heightState)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                widthState = (widthState + dragAmount.x.toDp().value).coerceAtLeast(30f)
                                heightState = (heightState + dragAmount.y.toDp().value).coerceAtLeast(30f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SouthEast,
                    contentDescription = "Resize Sticky Note",
                    tint = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

// --- NOTE SELECTION ITEM COMPOSABLE ---
@Composable
fun NoteSelectionItem(
    note: Note,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isSelected) },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFFF9800).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle(it ?: false) },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF9800))
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            // Note custom color dot/tag
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(android.graphics.Color.parseColor(note.readerStickyColorHex)), RoundedCornerShape(2.dp))
                    .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = Color.Red,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = if (note.bodyText.isNotBlank()) note.bodyText else "Empty description details.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Category Label
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (note.isMathNote) Color(0xFFFFD180) else Color(0xFFCFD8DC),
                contentColor = Color.Black
            ) {
                Text(
                    text = if (note.isMathNote) "Math" else "Note",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// --- BOTTOM MANAGER DOCK: WHITEBOARD BOARD LIST ---
@Composable
fun BottomSystemsDock(
    suns: List<Sun>,
    activeSunId: Int?,
    notes: List<Note>,
    onSelectSystem: (Int) -> Unit,
    onCreateSystem: () -> Unit,
    viewModel: NoteViewModel,
    isLightBg: Boolean,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit
) {
    val barColor = if (isLightBg) Color(0xFFCFD8DC).copy(alpha = 0.9f) else Color(0xFF0F1124).copy(alpha = 0.9f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(barColor)
            .border(
                width = 1.dp,
                color = if (isLightBg) Color.Gray.copy(alpha = 0.2f) else Color(0xFF222549)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeSunId != null) {
                // Edit/View toggle button (beside to the left of the "+" button)
                Button(
                    onClick = onToggleEditMode,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditMode) Color(0xFFE53935) else Color(0xFFFF9800),
                        contentColor = if (isEditMode) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                        contentDescription = if (isEditMode) "View Mode" else "Edit Mode",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditMode) "View" else "Edit", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            // New Board Button (Shortened to "+")
            Button(
                onClick = onCreateSystem,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Board", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            
            // Thin vertical divider (custom safe Box)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background((if (isLightBg) Color.Black else Color.White).copy(alpha = 0.1f))
            )
            
            // Interactive list of all whiteboard sessions (Thin chips)
            suns.forEach { sun ->
                val isSelected = sun.id == activeSunId
                val sysAccentColor = remember(sun.customColorHex) {
                    if (sun.customColorHex.isEmpty()) Color(0xFFFF9800) else Color(android.graphics.Color.parseColor(sun.customColorHex))
                }
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) sysAccentColor.copy(alpha = 0.12f) else Color(0xFF1F2245).copy(alpha = 0.3f))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) sysAccentColor else (if (isLightBg) Color.Gray.copy(alpha = 0.4f) else Color(0xFF2A2E5B)),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectSystem(sun.id) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(sysAccentColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sun.name,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isLightBg) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val totalNotes = viewModel.deserializeIntList(sun.noteIdsJson).size
                    Text(
                        text = "($totalNotes)",
                        fontSize = 9.sp,
                        color = (if (isLightBg) Color.Black else Color.White).copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// Simple Empty Universe State
@Composable
fun EmptyUniverseState(onCreateSun: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF9800).copy(alpha = 0.25f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = null,
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to Whiteboard Workspace",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "An interactive workspace where study topics turn into gorgeous customizable whiteboards. Drag cards, color-code ideas, zoom out to brain-map, and draw freehand diagrams directly on the background!",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.6f),
                lineHeight = 18.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 450.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCreateSun,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create First Whiteboard", fontWeight = FontWeight.Bold)
        }
    }
}
