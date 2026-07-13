package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NoteListScreen
import com.example.ui.screens.ReaderSpaceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NoteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val noteViewModel: NoteViewModel = viewModel()
                
                // Lightweight, high-performance in-memory navigation state controller
                var currentScreen by remember { mutableStateOf("list") }
                var editingNoteId by remember { mutableStateOf<Int?>(null) }
                var navigationSourceScreen by remember { mutableStateOf("list") }
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    when (currentScreen) {
                        "list" -> {
                            NoteListScreen(
                                viewModel = noteViewModel,
                                onNavigateToEdit = { noteId ->
                                    editingNoteId = noteId
                                    if (noteId != null) {
                                        noteViewModel.loadNote(noteId)
                                    }
                                    navigationSourceScreen = "list"
                                    currentScreen = "editor"
                                },
                                onNavigateToReaderSpace = {
                                    currentScreen = "reader_space"
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        "editor" -> {
                            NoteEditorScreen(
                                viewModel = noteViewModel,
                                onBack = {
                                    currentScreen = navigationSourceScreen
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        "reader_space" -> {
                            ReaderSpaceScreen(
                                viewModel = noteViewModel,
                                onBack = {
                                    currentScreen = "list"
                                },
                                onOpenNote = { noteId ->
                                    editingNoteId = noteId
                                    noteViewModel.loadNote(noteId)
                                    navigationSourceScreen = "reader_space"
                                    currentScreen = "editor"
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
