package com.android.fluxcut

import androidx.compose.ui.graphics.Color

enum class TrackType {
    VIDEO,
    AUDIO,
    SUBTITLE,
    TITLE,
    FX
}

data class Project(
    val id:             Int,
    val title:          String,
    val date:           String,
    val duration:       String,
    val resolution:     String,
    val aspectRatio:    String,
    val fps:            Int,
    val thumbnailColor: Color,
    val thumbnailUri:   String? = null,
    val thumbnailMimeType: String? = null,
    val lastModified:   Long = System.currentTimeMillis()
)

data class TimelineClip(
    val id:          Int,
    val name:        String,
    val track:       TrackType,
    val startMs:     Long,
    val durationMs:  Long,
    val trimStartMs: Long    = 0L,
    val color:       Color   = Color(0xFF4A90D9),
    val hasAudio:    Boolean = false,
    val isImage:     Boolean = false,
    val mimeType:    String? = null,
    val sourceUri:   String? = null
) {
    val endMs: Long get() = startMs + durationMs
    val sourceEndMs: Long get() = trimStartMs + durationMs
}

data class EditorColors(
    val timeRuler: Color,
    val subtle:    Color,
    val surface2:   Color,
    val playhead:  Color
)

data class EditorArgs(
    val project: Project,
    val autoOpenPicker: Boolean = true
)
