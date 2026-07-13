package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CopyBox
import com.example.data.model.PictureItem
import com.example.data.model.CalculationLine
import com.example.data.model.Note
import com.example.data.model.Sun
import com.example.data.repository.NoteRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Stack

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val allNotes: StateFlow<List<Note>>
    val allSuns: StateFlow<List<Sun>>

    private val moshi = Moshi.Builder().build()
    private val copyBoxAdapter = moshi.adapter<List<CopyBox>>(
        Types.newParameterizedType(List::class.java, CopyBox::class.java)
    )
    private val pictureAdapter = moshi.adapter<List<PictureItem>>(
        Types.newParameterizedType(List::class.java, PictureItem::class.java)
    )
    private val calcAdapter = moshi.adapter<List<CalculationLine>>(
        Types.newParameterizedType(List::class.java, CalculationLine::class.java)
    )

    // Current editing note states
    val activeNoteId = mutableStateOf<Int?>(null)
    val selectedSunId = mutableStateOf<Int?>(null)
    val currentTitle = mutableStateOf("")
    val currentBodyText = mutableStateOf("")
    val currentBgColor = mutableStateOf(0xFF1E1F2E.toInt())
    val isMathNote = mutableStateOf(false)
    val isFavorite = mutableStateOf(false)

    // Active components lists
    val activeCopyBoxes = mutableStateListOf<CopyBox>()
    val activePictures = mutableStateListOf<PictureItem>()
    val activeCalculationLines = mutableStateListOf<CalculationLine>()

    // Math Ledger colors custom names labels
    val redLabel = mutableStateOf("Red")
    val blueLabel = mutableStateOf("Blue")
    val greenLabel = mutableStateOf("Green")
    val yellowLabel = mutableStateOf("Yellow")

    // Undo-Redo Stack Snapshot History structures
    private data class NoteHistoryState(
        val title: String,
        val bodyText: String,
        val bgColorArgb: Int,
        val copyBoxes: List<CopyBox>,
        val pictures: List<PictureItem>,
        val calculationLines: List<CalculationLine>,
        val redLabel: String,
        val blueLabel: String,
        val greenLabel: String,
        val yellowLabel: String
    )

    private val undoStack = Stack<NoteHistoryState>()
    private val redoStack = Stack<NoteHistoryState>()

    val canUndo = mutableStateOf(false)
    val canRedo = mutableStateOf(false)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NoteRepository(database.noteDao())
        allNotes = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        allSuns = repository.allSuns.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun loadNote(noteId: Int) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId) ?: return@launch
            activeNoteId.value = note.id
            currentTitle.value = note.title
            currentBodyText.value = note.bodyText
            currentBgColor.value = note.bgColorArgb
            isMathNote.value = note.isMathNote
            isFavorite.value = note.isFavorite

            // Unpack components
            activeCopyBoxes.clear()
            if (note.copyBoxesJson.isNotBlank()) {
                try {
                    copyBoxAdapter.fromJson(note.copyBoxesJson)?.let { activeCopyBoxes.addAll(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            activePictures.clear()
            if (note.picturesJson.isNotBlank()) {
                try {
                    pictureAdapter.fromJson(note.picturesJson)?.let { activePictures.addAll(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            activeCalculationLines.clear()
            if (note.calculationLinesJson.isNotBlank()) {
                try {
                    calcAdapter.fromJson(note.calculationLinesJson)?.let { activeCalculationLines.addAll(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Load Math custom labels
            redLabel.value = note.redLabel.ifBlank { "Red" }
            blueLabel.value = note.blueLabel.ifBlank { "Blue" }
            greenLabel.value = note.greenLabel.ifBlank { "Green" }
            yellowLabel.value = note.yellowLabel.ifBlank { "Yellow" }

            // Reset undo / redo on fresh load
            undoStack.clear()
            redoStack.clear()
            updateHistoryFlags()
        }
    }

    fun updateTitleAndBody(title: String, body: String) {
        currentTitle.value = title
        currentBodyText.value = body
    }

    fun takeHistorySnapshot() {
        val snapshot = NoteHistoryState(
            title = currentTitle.value,
            bodyText = currentBodyText.value,
            bgColorArgb = currentBgColor.value,
            copyBoxes = activeCopyBoxes.toList(),
            pictures = activePictures.toList(),
            calculationLines = activeCalculationLines.toList(),
            redLabel = redLabel.value,
            blueLabel = blueLabel.value,
            greenLabel = greenLabel.value,
            yellowLabel = yellowLabel.value
        )
        // If snapshot is identical to peak undo state, ignore to prevent cluttering
        if (undoStack.isNotEmpty() && undoStack.peek() == snapshot) {
            return
        }
        undoStack.push(snapshot)
        redoStack.clear()
        updateHistoryFlags()
    }

    private fun updateHistoryFlags() {
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        // Push current state to redo
        val currentState = NoteHistoryState(
            title = currentTitle.value,
            bodyText = currentBodyText.value,
            bgColorArgb = currentBgColor.value,
            copyBoxes = activeCopyBoxes.toList(),
            pictures = activePictures.toList(),
            calculationLines = activeCalculationLines.toList(),
            redLabel = redLabel.value,
            blueLabel = blueLabel.value,
            greenLabel = greenLabel.value,
            yellowLabel = yellowLabel.value
        )
        redoStack.push(currentState)

        // Pop last state and apply
        val previousState = undoStack.pop()
        applyHistoryState(previousState)
        updateHistoryFlags()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        // Push current state to undo
        val currentState = NoteHistoryState(
            title = currentTitle.value,
            bodyText = currentBodyText.value,
            bgColorArgb = currentBgColor.value,
            copyBoxes = activeCopyBoxes.toList(),
            pictures = activePictures.toList(),
            calculationLines = activeCalculationLines.toList(),
            redLabel = redLabel.value,
            blueLabel = blueLabel.value,
            greenLabel = greenLabel.value,
            yellowLabel = yellowLabel.value
        )
        undoStack.push(currentState)

        // Pop next state and apply
        val nextState = redoStack.pop()
        applyHistoryState(nextState)
        updateHistoryFlags()
    }

    private fun applyHistoryState(state: NoteHistoryState) {
        currentTitle.value = state.title
        currentBodyText.value = state.bodyText
        currentBgColor.value = state.bgColorArgb

        activeCopyBoxes.clear()
        activeCopyBoxes.addAll(state.copyBoxes)

        activePictures.clear()
        activePictures.addAll(state.pictures)

        activeCalculationLines.clear()
        activeCalculationLines.addAll(state.calculationLines)

        redLabel.value = state.redLabel
        blueLabel.value = state.blueLabel
        greenLabel.value = state.greenLabel
        yellowLabel.value = state.yellowLabel
    }

    fun initNewNote(isMath: Boolean) {
        activeNoteId.value = null
        currentTitle.value = ""
        currentBodyText.value = ""
        currentBgColor.value = if (isMath) 0xFF0F1124.toInt() else 0xFF1E1F2E.toInt()
        isMathNote.value = isMath
        isFavorite.value = false

        activeCopyBoxes.clear()
        activePictures.clear()
        activeCalculationLines.clear()

        redLabel.value = "Red"
        blueLabel.value = "Blue"
        greenLabel.value = "Green"
        yellowLabel.value = "Yellow"

        undoStack.clear()
        redoStack.clear()
        updateHistoryFlags()
    }

    fun saveActiveNote(onComplete: () -> Unit) {
        viewModelScope.launch {
            val noteId = activeNoteId.value
            val existingNote = if (noteId != null) repository.getNoteById(noteId) else null
            
            val copyJson = try { copyBoxAdapter.toJson(activeCopyBoxes) } catch (e: Exception) { "" }
            val picJson = try { pictureAdapter.toJson(activePictures) } catch (e: Exception) { "" }
            val calcJson = try { calcAdapter.toJson(activeCalculationLines) } catch (e: Exception) { "" }

            val note = Note(
                id = noteId ?: 0,
                title = currentTitle.value.trim(),
                bodyText = currentBodyText.value,
                bgColorArgb = currentBgColor.value,
                timestamp = System.currentTimeMillis(),
                isMathNote = isMathNote.value,
                copyBoxesJson = copyJson ?: "",
                picturesJson = picJson ?: "",
                calculationLinesJson = calcJson ?: "",
                isFavorite = isFavorite.value,
                redLabel = redLabel.value,
                blueLabel = blueLabel.value,
                greenLabel = greenLabel.value,
                yellowLabel = yellowLabel.value,
                readerStickyColorHex = existingNote?.readerStickyColorHex ?: (if (isMathNote.value) "#E1BEE7" else "#FFF59D"),
                readerStickyTexture = existingNote?.readerStickyTexture ?: "Plain",
                readerBoardX = existingNote?.readerBoardX ?: 50f,
                readerBoardY = existingNote?.readerBoardY ?: 50f,
                readerStickyWidth = existingNote?.readerStickyWidth ?: 140f,
                readerStickyHeight = existingNote?.readerStickyHeight ?: 140f
            )

            if (noteId != null) {
                repository.updateNote(note)
            } else {
                repository.insertNote(note)
            }
            onComplete()
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isFavorite = !note.isFavorite))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    // Components operations with automatic Undo history snapshot tracking
    fun addCopyBox(yOffsetDp: Float) {
        takeHistorySnapshot()
        val newBox = CopyBox(
            id = System.currentTimeMillis().toString(),
            text = "",
            x = 40f,
            y = yOffsetDp + 30f,
            width = 180f,
            height = 100f
        )
        activeCopyBoxes.add(newBox)
    }

    fun updateCopyBox(box: CopyBox) {
        val index = activeCopyBoxes.indexOfFirst { it.id == box.id }
        if (index != -1) {
            activeCopyBoxes[index] = box
        }
    }

    fun deleteCopyBox(boxId: String) {
        takeHistorySnapshot()
        activeCopyBoxes.removeAll { it.id == boxId }
    }

    fun addPictureItem(filePath: String, yOffsetDp: Float) {
        takeHistorySnapshot()
        val newPic = PictureItem(
            id = System.currentTimeMillis().toString(),
            localUri = filePath,
            x = 50f,
            y = yOffsetDp + 40f,
            width = 200f,
            height = 160f
        )
        activePictures.add(newPic)
    }

    fun updatePicture(pic: PictureItem) {
        val index = activePictures.indexOfFirst { it.id == pic.id }
        if (index != -1) {
            activePictures[index] = pic
        }
    }

    fun deletePicture(picId: String) {
        takeHistorySnapshot()
        activePictures.removeAll { it.id == picId }
    }

    // Whiteboard operations (Suns)
    fun saveSun(
        name: String,
        noteIds: List<Int>,
        themeIndex: Int,
        sunStyle: String,
        planetStyle: String,
        customColorHex: String
    ) {
        viewModelScope.launch {
            val sun = Sun(
                id = 0,
                name = name,
                noteIdsJson = serializeIntList(noteIds),
                themeIndex = themeIndex,
                sunStyle = sunStyle,
                planetStyle = planetStyle,
                customColorHex = customColorHex
            )
            repository.insertSun(sun)
        }
    }

    fun updateSun(sun: Sun) {
        viewModelScope.launch {
            repository.updateSun(sun)
        }
    }

    fun deleteSun(sun: Sun) {
        viewModelScope.launch {
            repository.deleteSun(sun)
        }
    }

    // Helper utilities for List<Int> serialization
    fun serializeIntList(list: List<Int>): String {
        return list.joinToString(",")
    }

    fun deserializeIntList(serialized: String): List<Int> {
        if (serialized.isBlank()) return emptyList()
        return try {
            serialized.split(",").mapNotNull { it.toIntOrNull() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
