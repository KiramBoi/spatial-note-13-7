package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.data.model.Note
import com.example.data.model.Sun
import com.example.data.model.CopyBox
import com.example.data.model.PictureItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

class Converters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val copyBoxListType = Types.newParameterizedType(List::class.java, CopyBox::class.java)
    private val picListType = Types.newParameterizedType(List::class.java, PictureItem::class.java)
    
    private val copyBoxAdapter = moshi.adapter<List<CopyBox>>(copyBoxListType)
    private val picAdapter = moshi.adapter<List<PictureItem>>(picListType)

    @TypeConverter
    fun fromCopyBoxesJson(json: String): List<CopyBox> {
        if (json.isEmpty()) return emptyList()
        return try {
            copyBoxAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toCopyBoxesJson(list: List<CopyBox>): String {
        return copyBoxAdapter.toJson(list)
    }

    @TypeConverter
    fun fromPicturesJson(json: String): List<PictureItem> {
        if (json.isEmpty()) return emptyList()
        return try {
            picAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toPicturesJson(list: List<PictureItem>): String {
        return picAdapter.toJson(list)
    }
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Delete
    suspend fun deleteNote(note: Note)

    // Sun queries for Reader Space
    @Query("SELECT * FROM suns ORDER BY createdAt DESC")
    fun getAllSuns(): Flow<List<Sun>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSun(sun: Sun): Long

    @Delete
    suspend fun deleteSun(sun: Sun)
}

@Database(entities = [Note::class, Sun::class], version = 9, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spatial_notes_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
