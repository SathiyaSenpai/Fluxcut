package com.android.fluxcut

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

private fun playerFormatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min      = totalSec / 60
    val sec      = totalSec % 60
    val centis   = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(min, sec, centis)
}
