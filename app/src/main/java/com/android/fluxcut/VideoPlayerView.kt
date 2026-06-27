package com.android.fluxcut

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@UnstableApi
@Composable
fun VideoPlayerView(
    player:      ExoPlayer,
    modifier:    Modifier = Modifier,
    resizeMode:  Int      = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player        = player
                this.useController = false
                this.resizeMode    = resizeMode
            }
        },
        update = { view ->
            if (view.player !== player) view.player = player
        },
        modifier = modifier
    )
}

@Composable
fun PlayerControls(
    isPlaying:    Boolean,
    playheadMs:   Long,
    totalMs:      Long,
    onTogglePlay: () -> Unit,
    onSeek:       (Long) -> Unit,
    modifier:     Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (totalMs > 0L) {
            Slider(
                value         = playheadMs.toFloat(),
                valueRange    = 0f..totalMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                modifier      = Modifier.fillMaxWidth(),
                colors        = SliderDefaults.colors(
                    thumbColor         = Color.White,
                    activeTrackColor   = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }

        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector        = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint               = Color.White
                )
            }
            Text(
                text  = "${playerFormatMs(playheadMs)} / ${playerFormatMs(totalMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

private fun playerFormatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min      = totalSec / 60
    val sec      = totalSec % 60
    val centis   = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(min, sec, centis)
}
