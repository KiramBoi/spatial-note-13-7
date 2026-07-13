package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CopyBox(
    val id: String,
    val text: String,
    val x: Float, // in Dp
    val y: Float, // in Dp
    val width: Float = 180f,
    val height: Float = 120f,
    val isMinimized: Boolean = false,
    val colorArgb: Int = 0xFFFFF176.toInt(), // Yellow
    val dotShape: String = "Circle", // Circle, RoundedSquare, Hexagon, Triangle, Star
    val dotText: String = "Copy",
    val textColorArgb: Int = 0xFFFFFFFF.toInt(), // White by default
    val dotSize: Float = 48f,
    val isUrl: Boolean = false,
    val url: String = "",
    val dotFontSize: Float = 9f
)

@JsonClass(generateAdapter = true)
data class PictureItem(
    val id: String,
    val localUri: String, // local cached file path/uri
    val x: Float, // in Dp
    val y: Float, // in Dp
    val width: Float = 200f,
    val height: Float = 150f,
    val isMinimized: Boolean = false,
    val colorArgb: Int = 0xFF81C784.toInt(), // Green
    val dotShape: String = "Circle", // Circle, RoundedSquare, Hexagon, Triangle, Star
    val dotText: String = "Pic",
    val textColorArgb: Int = 0xFFFFFFFF.toInt(), // White by default
    val dotSize: Float = 48f
)

@JsonClass(generateAdapter = true)
data class CalcElement(
    val type: String, // "BOX" or "OPERATOR"
    val value: String // BOX -> "Red", "Blue", "Green", "Yellow"; OPERATOR -> "+", "-", "x", "/"
)

@JsonClass(generateAdapter = true)
data class CalculationLine(
    val id: String,
    val elements: List<CalcElement> = emptyList(),
    val isFinished: Boolean = false,
    val name: String = ""
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val bodyText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val bgColorArgb: Int = 0xFF12131F.toInt(), // Deep space obsidian
    val copyBoxesJson: String = "[]",
    val picturesJson: String = "[]",
    val isMathNote: Boolean = false,
    val calculationLinesJson: String = "[]",
    val redLabel: String = "Red",
    val blueLabel: String = "Blue",
    val greenLabel: String = "Green",
    val yellowLabel: String = "Yellow",
    val isFavorite: Boolean = false,
    
    // Custom Reader Space visual customization fields (Space planet types, Physics shapes, Sticky colors/textures)
    val readerPlanetStyle: String = "Gaseous Giant",
    val readerBlockShape: String = "Sphere",
    val readerStickyColorHex: String = "#FFF59D",
    val readerStickyTexture: String = "Plain",
    val readerBoardX: Float = 150f,
    val readerBoardY: Float = 150f,
    val readerPhysicsVx: Float = 0f,
    val readerPhysicsVy: Float = 0f,
    val readerStickyWidth: Float = 140f,
    val readerStickyHeight: Float = 140f
)

@Entity(tableName = "suns")
data class Sun(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val noteIdsJson: String = "[]", // JSON array of Int (note IDs)
    val createdAt: Long = System.currentTimeMillis(),
    val themeIndex: Int = 0, // 0 = Space, 1 = Physics, 2 = Underwater, 3 = Constellation, 4 = Forest
    val sunStyle: String = "Solar", // Sun style name
    val planetStyle: String = "Default", // Planet style name
    val customColorHex: String = "", // Optional custom hex override
    val notePlacementsJson: String = "" // New column for independent note positions & sizes
)
