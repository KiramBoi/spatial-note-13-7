package com.example.data.repository

import com.example.data.db.NoteDao
import com.example.data.model.Note
import com.example.data.model.Sun
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)
    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)
    suspend fun updateNote(note: Note): Long = noteDao.insertNote(note)
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    // Sun repository methods
    val allSuns: Flow<List<Sun>> = noteDao.getAllSuns()
    suspend fun insertSun(sun: Sun): Long = noteDao.insertSun(sun)
    suspend fun updateSun(sun: Sun): Long = noteDao.insertSun(sun)
    suspend fun deleteSun(sun: Sun) = noteDao.deleteSun(sun)
}
