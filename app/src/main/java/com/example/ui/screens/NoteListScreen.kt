@file:Suppress("DEPRECATION")
package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Public
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Note
import com.example.ui.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNavigateToEdit: (Int?) -> Unit,
    onNavigateToReaderSpace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.allNotes.collectAsState()
    val context = LocalContext.current
    val pendingDeletionNoteState = remember { mutableStateOf<Note?>(null) }
    val pendingDeletionNote = pendingDeletionNoteState.value
    var showCreateDialog by remember { mutableStateOf(false) }
    // Persistent state variables for grouping, sorting, and layouts
    var isGroupedByType by rememberSaveable { mutableStateOf(false) }
    var sortByOption by rememberSaveable { mutableStateOf("Recent") } // "Recent", "Oldest", "Name"
    var layoutMode by rememberSaveable { mutableStateOf("Original") } // "Original", "Minimal", "Compact"
    var isFavoritesToggled by rememberSaveable { mutableStateOf(false) }
    
    val sortedNotes = remember(notes, sortByOption, isFavoritesToggled) {
        val baseSorted = when (sortByOption) {
            "Recent" -> notes.sortedByDescending { it.timestamp }
            "Oldest" -> notes.sortedBy { it.timestamp }
            "Name" -> notes.sortedBy { it.title.lowercase() }
            else -> notes
        }
        if (isFavoritesToggled) {
            val favorites = baseSorted.filter { it.isFavorite }
            val nonFavorites = baseSorted.filter { !it.isFavorite }
            favorites + nonFavorites
        } else {
            baseSorted
        }
    }
    val displayedNotes = remember(sortedNotes, pendingDeletionNote) {
        if (pendingDeletionNote != null) {
            sortedNotes.filter { it.id != pendingDeletionNote.id }
        } else {
            sortedNotes
        }
    }
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    "Create New Entry",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCreateDialog = false
                                if (pendingDeletionNote != null) {
                                    viewModel.deleteNote(pendingDeletionNote)
                                    pendingDeletionNoteState.value = null
                                }
                                viewModel.initNewNote(isMath = false)
                                onNavigateToEdit(null)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoteAlt,
                                contentDescription = "Spatial Note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Spatial Note",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Create visual canvases with spatial copy-boxes, custom shapes, and picture attachments.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCreateDialog = false
                                if (pendingDeletionNote != null) {
                                    viewModel.deleteNote(pendingDeletionNote)
                                    pendingDeletionNoteState.value = null
                                }
                                viewModel.initNewNote(isMath = true)
                                onNavigateToEdit(null)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Math Ledger",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Math Ledger",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "Traditional gridded calculation pad with real-time colored totals and dynamic equation lines.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Row 1: App branding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NoteAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ledger Board",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                    // Reader Space Entry Button
                    IconButton(
                        onClick = onNavigateToReaderSpace,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Reader Space",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Row 2: Smart Filter, Group, and Layout Actions panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Group chip & Sort dropdown
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Grouping toggle chip
                        FilterChip(
                            selected = isGroupedByType,
                            onClick = { isGroupedByType = !isGroupedByType },
                            label = { Text("Group by Type") }
                        )
                        // Sorting dropdown trigger
                        var showSortMenu by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = false,
                                onClick = { showSortMenu = true },
                                label = { Text("Sort: $sortByOption") },
                                trailingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Recent") },
                                    onClick = { sortByOption = "Recent"; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Oldest") },
                                    onClick = { sortByOption = "Oldest"; showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name") },
                                    onClick = { sortByOption = "Name"; showSortMenu = false }
                                )
                            }
                        }
                        // Starred favorites toggle chip
                        FilterChip(
                            selected = isFavoritesToggled,
                            onClick = { isFavoritesToggled = !isFavoritesToggled },
                            label = { Text("Starred", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isFavoritesToggled) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isFavoritesToggled) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD700).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFFFFD700),
                                selectedLeadingIconColor = Color(0xFFFFD700)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Right: Compact segmented layout controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { layoutMode = "Original" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Original Grid View",
                                tint = if (layoutMode == "Original") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = { layoutMode = "Minimal" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewList,
                                contentDescription = "Minimalist List",
                                tint = if (layoutMode == "Minimal") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = { layoutMode = "Compact" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewHeadline,
                                contentDescription = "Compact Rows",
                                tint = if (layoutMode == "Compact") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Note", modifier = Modifier.size(28.dp))
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (displayedNotes.isEmpty()) {
                // Empty state illustration/guideline
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your board is empty",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create sophisticated notes with spatial copy-boxes, picture canvas attachments, and rich format layout.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.widthIn(max = 300.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isGroupedByType) {
                        // RENDER ALL NOTES FLAT
                        if (layoutMode == "Original") {
                            // Render original grid (chunked in pairs of 2)
                            val chunked = displayedNotes.chunked(2)
                            items(chunked) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    pair.forEach { note ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            NoteGridCard(
                                                note = note,
                                                onClick = {
                                                    if (pendingDeletionNote != null) {
                                                        viewModel.deleteNote(pendingDeletionNote)
                                                        pendingDeletionNoteState.value = null
                                                    }
                                                    viewModel.loadNote(note.id)
                                                    onNavigateToEdit(note.id)
                                                },
                                                onDelete = {
                                                    if (pendingDeletionNote != null) {
                                                        viewModel.deleteNote(pendingDeletionNote)
                                                    }
                                                    pendingDeletionNoteState.value = note
                                                },
                                                onToggleFavorite = {
                                                    viewModel.toggleFavorite(note)
                                                }
                                            )
                                        }
                                    }
                                    if (pair.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            // List layout modes (Minimal or Compact)
                            items(displayedNotes, key = { it.id }) { note ->
                                if (layoutMode == "Minimal") {
                                    MinimalistNoteItem(
                                        note = note,
                                        onClick = {
                                            if (pendingDeletionNote != null) {
                                                viewModel.deleteNote(pendingDeletionNote)
                                                pendingDeletionNoteState.value = null
                                            }
                                            viewModel.loadNote(note.id)
                                            onNavigateToEdit(note.id)
                                        },
                                        onToggleFavorite = {
                                            viewModel.toggleFavorite(note)
                                        }
                                    )
                                } else {
                                    CompactNoteItem(
                                        note = note,
                                        onClick = {
                                            if (pendingDeletionNote != null) {
                                                viewModel.deleteNote(pendingDeletionNote)
                                                pendingDeletionNoteState.value = null
                                            }
                                            viewModel.loadNote(note.id)
                                            onNavigateToEdit(note.id)
                                        },
                                        onDelete = {
                                            if (pendingDeletionNote != null) {
                                                viewModel.deleteNote(pendingDeletionNote)
                                            }
                                            pendingDeletionNoteState.value = note
                                        },
                                        onToggleFavorite = {
                                            viewModel.toggleFavorite(note)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // RENDER GROUPED BY TYPE
                        val spatialNotes = displayedNotes.filter { !it.isMathNote }
                        val mathLedgerNotes = displayedNotes.filter { it.isMathNote }
                        
                        if (spatialNotes.isNotEmpty()) {
                            item {
                                GroupSectionHeader(title = "Spatial Canvases", color = MaterialTheme.colorScheme.primary, icon = Icons.Default.NoteAlt)
                            }
                            if (layoutMode == "Original") {
                                val chunked = spatialNotes.chunked(2)
                                items(chunked) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        pair.forEach { note ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                NoteGridCard(
                                                    note = note,
                                                    onClick = {
                                                        if (pendingDeletionNote != null) {
                                                            viewModel.deleteNote(pendingDeletionNote)
                                                            pendingDeletionNoteState.value = null
                                                        }
                                                        viewModel.loadNote(note.id)
                                                        onNavigateToEdit(note.id)
                                                    },
                                                    onDelete = {
                                                        if (pendingDeletionNote != null) {
                                                            viewModel.deleteNote(pendingDeletionNote)
                                                        }
                                                        pendingDeletionNoteState.value = note
                                                    },
                                                    onToggleFavorite = {
                                                        viewModel.toggleFavorite(note)
                                                    }
                                                )
                                            }
                                        }
                                        if (pair.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                items(spatialNotes, key = { it.id }) { note ->
                                    if (layoutMode == "Minimal") {
                                        MinimalistNoteItem(
                                            note = note,
                                            onClick = {
                                                if (pendingDeletionNote != null) {
                                                    viewModel.deleteNote(pendingDeletionNote)
                                                    pendingDeletionNoteState.value = null
                                                }
                                                viewModel.loadNote(note.id)
                                                onNavigateToEdit(note.id)
                                            },
                                            onToggleFavorite = {
                                                viewModel.toggleFavorite(note)
                                            }
                                        )
                                    } else {
                                        CompactNoteItem(
                                            note = note,
                                            onClick = {
                                                if (pendingDeletionNote != null) {
                                                    viewModel.deleteNote(pendingDeletionNote)
                                                    pendingDeletionNoteState.value = null
                                                }
                                                viewModel.loadNote(note.id)
                                                onNavigateToEdit(note.id)
                                            },
                                            onDelete = {
                                                if (pendingDeletionNote != null) {
                                                    viewModel.deleteNote(pendingDeletionNote)
                                                }
                                                pendingDeletionNoteState.value = note
                                            },
                                            onToggleFavorite = {
                                                viewModel.toggleFavorite(note)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (mathLedgerNotes.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                GroupSectionHeader(title = "Math Ledgers", color = MaterialTheme.colorScheme.secondary, icon = Icons.Default.Calculate)
                            }
                            if (layoutMode == "Original") {
                                val chunked = mathLedgerNotes.chunked(2)
                                items(chunked) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        pair.forEach { note ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                NoteGridCard(
                                                    note = note,
                                                    onClick = {
                                                        if (pendingDeletionNote != null) {
                                                            viewModel.deleteNote(pendingDeletionNote)
                                                            pendingDeletionNoteState.value = null
                                                        }
                                                        viewModel.loadNote(note.id)
                                                        onNavigateToEdit(note.id)
                                                    },
                                                    onDelete = {
                                                        if (pendingDeletionNote != null) {
                                                            viewModel.deleteNote(pendingDeletionNote)
                                                        }
                                                        pendingDeletionNoteState.value = note
                                                    },
                                                    onToggleFavorite = {
                                                        viewModel.toggleFavorite(note)
                                                    }
                                                )
                                            }
                                        }
                                        if (pair.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                items(mathLedgerNotes, key = { it.id }) { note ->
                                    if (layoutMode == "Minimal") {
                                        MinimalistNoteItem(
                                            note = note,
                                            onClick = {
                                                if (pendingDeletionNote != null) {
                                                    viewModel.deleteNote(pendingDeletionNote)
                                                    pendingDeletionNoteState.value = null
                                                }
                                                viewModel.loadNote(note.id)
                                                onNavigateToEdit(note.id)
                                            },
                                            onToggleFavorite = {
                                                viewModel.toggleFavorite(note)
                                            }
                                        )
                                    } else {
                                        CompactNoteItem(
                                            note = note,
                                            onClick = {
                                                if (pendingDeletionNote != null) {
                                                    viewModel.deleteNote(pendingDeletionNote)
                                                    pendingDeletionNoteState.value = null
                                                }
                                                viewModel.loadNote(note.id)
                                                onNavigateToEdit(note.id)
                                            },
                                            onDelete = {
                                                if (pendingDeletionNote != null) {
                                                    viewModel.deleteNote(pendingDeletionNote)
                                                }
                                                pendingDeletionNoteState.value = note
                                            },
                                            onToggleFavorite = {
                                                viewModel.toggleFavorite(note)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Custom persistent delete confirmation popup overlay
            pendingDeletionNote?.let { note ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(0.95f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Deleted \"${note.title.ifEmpty { "Untitled" }}\"",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    pendingDeletionNoteState.value = null
                                }
                            ) {
                                Text(
                                    text = "Undo",
                                    color = MaterialTheme.colorScheme.inversePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.deleteNote(note)
                                    pendingDeletionNoteState.value = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "OK",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteGridCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val formattedDate = DateFormat.format("MMM dd, yyyy HH:mm", note.timestamp).toString()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 220.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(note.bgColorArgb).copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = note.title.ifEmpty { "Untitled" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite Note",
                                tint = if (note.isFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Delete Note",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                // Content teaser
                Text(
                    text = note.bodyText.replace("*", "").replace("_", ""),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                )
                if (note.isMathNote) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Math Ledger",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Math",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupSectionHeader(
    title: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = color,
                letterSpacing = 0.5.sp
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.12f)
        )
    }
}

@Composable
fun MinimalistNoteItem(
    note: Note,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = DateFormat.format("MMM dd, yyyy", note.timestamp).toString()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1F2E).copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored left capsule marker
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(Color(note.bgColorArgb), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite Note",
                        tint = if (note.isFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (note.isMathNote) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "Math",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactNoteItem(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = DateFormat.format("MMM dd, yyyy", note.timestamp).toString()
    val cleanBody = note.bodyText.replace("*", "").replace("_", "").take(60).replace("\n", " ")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(note.bgColorArgb).copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, Color(note.bgColorArgb).copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (note.isMathNote) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Math",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
                if (cleanBody.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = cleanBody,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite Note",
                        tint = if (note.isFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Delete Note",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
