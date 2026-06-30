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
    val isImage = context.contentResolver.getType(uri)?.startsWith("image") == true || 
                  uri.toString().endsWith(".jpg") || uri.toString().endsWith(".png") || uri.toString().endsWith(".jpeg")
    
    return try {
        retriever.setDataSource(context, uri)

        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L

        val mimeType = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            ?: (if (isImage) "image/jpeg" else "video/mp4")

        var width = retriever
            .extractMetadata(if (isImage) MediaMetadataRetriever.METADATA_KEY_IMAGE_WIDTH else MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull() ?: 0

        var height = retriever
            .extractMetadata(if (isImage) MediaMetadataRetriever.METADATA_KEY_IMAGE_HEIGHT else MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull() ?: 0
            
        // Fallback for image dimensions if retriever fails
        if (isImage && (width == 0 || height == 0)) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeStream(stream, null, options)
                width = options.outWidth
                height = options.outHeight
            }
        }

        val fileName = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?: "clip_${System.currentTimeMillis()}"

        ImportedMedia(uri, fileName, durationMs, width, height, mimeType)
    } catch (e: Exception) {
        // Ultimate fallback for metadata extraction failure
        val fileName = uri.lastPathSegment ?: "media_${System.currentTimeMillis()}"
        ImportedMedia(uri, fileName, 0L, 1920, 1080, if (isImage) "image/jpeg" else "video/mp4")
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
    val isImage = mimeType.startsWith("image")
    return TimelineClip(
        id         = id,
        name       = fileName.substringBeforeLast('.'),
        track      = if (isVideo || isImage) TrackType.VIDEO else TrackType.AUDIO,
        startMs    = startMs,
        durationMs = if (durationMs > 0L) durationMs else 5000L,
        color      = if (isVideo || isImage) videoColor else audioColor,
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
