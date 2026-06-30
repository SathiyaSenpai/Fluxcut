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
            fps         = config.fps
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

    private fun buildFfmpegCommand(
        clips: List<TimelineClip>,
        outputPath: String,
        resolution: String,
        fps: Int
    ): String {
        val scale = resolutionToScale(resolution)

        return if (clips.size == 1) {
            val src = clips.first().sourceUri!!
            "-i \"$src\" -vf \"scale=$scale,fps=$fps\" -c:v libx264 -preset fast -crf 23 -c:a aac -b:a 128k -movflags +faststart -y \"$outputPath\""
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
            "-vf \"scale=$scale,fps=$fps\" -c:v libx264 -preset fast -crf 23 " +
            "-c:a aac -b:a 128k -movflags +faststart -y \"$outputPath\""
        }
    }

    private fun resolutionToScale(resolution: String): String = when (resolution) {
        "720p"  -> "1280:720"
        "4K"    -> "3840:2160"
        else    -> "1920:1080"
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
