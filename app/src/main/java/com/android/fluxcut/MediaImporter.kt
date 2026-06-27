package com.android.fluxcut

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportedMedia(
    val uri: Uri,
    val fileName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val mimeType: String
)

fun extractMediaMetadata(context: Context, uri: Uri): ImportedMedia {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)

        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L

        val width = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull() ?: 0

        val height = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull() ?: 0

        val mimeType = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            ?: "video/mp4"

        val fileName = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?: "clip_${System.currentTimeMillis()}"

        ImportedMedia(uri, fileName, durationMs, width, height, mimeType)
    } finally {
        retriever.release()
    }
}

fun ImportedMedia.toTimelineClip(
    id: Int,
    startMs: Long,
    videoColor: Color,
    audioColor: Color
): TimelineClip {
    val isVideo = mimeType.startsWith("video")
    return TimelineClip(
        id         = id,
        name       = fileName.substringBeforeLast('.'),
        track      = if (isVideo) TrackType.VIDEO else TrackType.AUDIO,
        startMs    = startMs,
        durationMs = if (durationMs > 0L) durationMs else 5000L,
        color      = if (isVideo) videoColor else audioColor,
        hasAudio   = isVideo,
        sourceUri  = uri.toString()
    )
}

@Composable
fun rememberMediaImportLauncher(
    onResult: (ImportedMedia) -> Unit
): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val media = extractMediaMetadata(context, uri)
                withContext(Dispatchers.Main) { onResult(media) }
            }
        }
    }

    return {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        )
    }
}
