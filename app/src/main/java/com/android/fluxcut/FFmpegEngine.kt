package com.android.fluxcut

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

data class ExportConfig(
    val clips: List<TimelineClip>,
    val resolution: String,
    val fps: Int,
    val projectTitle: String,
    val aspectRatio: String
)

sealed class ExportResult {
    data class Progress(val percent: Int) : ExportResult()
    data class Success(val outputPath: String) : ExportResult()
    data class Failure(val message: String) : ExportResult()
}

object FFmpegEngine {

    fun export(
        context: Context,
        config: ExportConfig,
        onResult: (ExportResult) -> Unit
    ) {
        val videoClips = config.clips.filter {
            it.track == TrackType.VIDEO && it.sourceUri != null
        }

        if (videoClips.isEmpty()) {
            onResult(ExportResult.Failure("No video clips to export."))
            return
        }

        val outputFile = buildOutputFile(context, config.projectTitle)
        val command = buildFfmpegCommand(
            clips       = videoClips,
            outputPath  = outputFile.absolutePath,
            resolution  = config.resolution,
            fps         = config.fps,
            aspectRatio = config.aspectRatio
        )

        onResult(ExportResult.Progress(0))

        FFmpegKit.executeAsync(command, { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                addToGallery(context, outputFile)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(ExportResult.Success(outputFile.absolutePath))
                }
            } else {
                val logs = session.failStackTrace ?: "Unknown FFmpeg error"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(ExportResult.Failure(logs))
                }
            }
        }, { log ->
            android.util.Log.d("FFmpegEngine", log.message)
        }, { stats ->
            val totalMs = config.clips.maxOfOrNull { it.startMs + it.durationMs } ?: 1L
            val pct = ((stats.time.toFloat() / totalMs) * 100).toInt().coerceIn(1, 99)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(ExportResult.Progress(pct))
            }
        })
    }

    fun extractAudio(
        context: Context,
        videoUri: android.net.Uri,
        onResult: (ExportResult) -> Unit
    ) {
        val safeTitle = "Extracted_${System.currentTimeMillis()}"
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "FluxCut")
        dir.mkdirs()
        val outputFile = File(dir, "${safeTitle}.mp3")

        val inputPath = com.arthenica.ffmpegkit.FFmpegKitConfig.getSafParameterForRead(context, videoUri)
        val command = "-i \"$inputPath\" -vn -acodec libmp3lame -q:a 2 -y \"${outputFile.absolutePath}\""

        onResult(ExportResult.Progress(0))

        FFmpegKit.executeAsync(command, { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                // Add to Audio MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, outputFile.name)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/FluxCut")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        outputFile.inputStream().use { it.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(ExportResult.Success(outputFile.absolutePath))
                }
            } else {
                val logs = session.failStackTrace ?: "Extraction failed"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(ExportResult.Failure(logs))
                }
            }
        }, { log ->
            android.util.Log.d("FFmpegEngine", log.message)
        }, { stats ->
            // Progress estimation for audio extraction is tricky without duration, 
            // but we'll just show activity
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult(ExportResult.Progress(50))
            }
        })
    }

    private fun buildFfmpegCommand(
        clips: List<TimelineClip>,
        outputPath: String,
        resolution: String,
        fps: Int,
        aspectRatio: String
    ): String {
        // Parse resolution
        val targetHeight = when (resolution) {
            "720p"  -> 720
            "2K"    -> 1440
            "4K"    -> 2160
            else    -> 1080
        }

        // Parse aspect ratio
        val ratioParts = aspectRatio.split(":")
        val (rw, rh) = if (ratioParts.size == 2) {
            val w = ratioParts[0].toFloatOrNull() ?: 16f
            val h = ratioParts[1].toFloatOrNull() ?: 9f
            w to h
        } else 16f to 9f

        val targetWidth = ((targetHeight * rw) / rh).toInt().let { if (it % 2 != 0) it + 1 else it }
        val finalHeight = if (targetHeight % 2 != 0) targetHeight + 1 else targetHeight

        val filter = "scale=$targetWidth:$finalHeight:force_original_aspect_ratio=decrease,pad=$targetWidth:$finalHeight:(ow-iw)/2:(oh-ih)/2"

        return if (clips.size == 1) {
            val src = clips.first().sourceUri!!
            "-i \"$src\" -vf \"$filter,fps=$fps\" -c:v libx264 -preset fast -crf 23 -c:a aac -b:a 128k -movflags +faststart -y \"$outputPath\""
        } else {
            val listFile = File(outputPath).parentFile?.let {
                File(it, "concat_list_${System.currentTimeMillis()}.txt")
            } ?: File(outputPath.replace(".mp4", "_list.txt"))

            listFile.printWriter().use { w ->
                clips.sortedBy { it.startMs }.forEach { clip ->
                    w.println("file '${clip.sourceUri}'")
                }
            }

            "-f concat -safe 0 -i \"${listFile.absolutePath}\" " +
            "-vf \"$filter,fps=$fps\" -c:v libx264 -preset fast -crf 23 " +
            "-c:a aac -b:a 128k -movflags +faststart -y \"$outputPath\""
        }
    }


    private fun buildOutputFile(context: Context, projectTitle: String): File {
        val safeTitle = projectTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val timestamp = System.currentTimeMillis()
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "FluxCut")
        dir.mkdirs()
        return File(dir, "FluxCut_${safeTitle}_$timestamp.mp4")
    }

    private fun addToGallery(context: Context, file: File) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/FluxCut")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return
        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
}
