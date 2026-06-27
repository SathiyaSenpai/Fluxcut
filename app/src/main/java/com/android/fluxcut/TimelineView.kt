package com.android.fluxcut

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlin.math.roundToLong

private val CLIP_TRACK_HEIGHT = 44.dp
private val HANDLE_WIDTH      = 12.dp
private val RULER_HEIGHT      = 28.dp

@Composable
fun TimelineView(
    clips:           List<TimelineClip>,
    selectedClipId:  Int?,
    trimState:       TrimState?,
    playheadMs:      Long,
    totalMs:         Long,
    c:               EditorColors,
    pxPerMs:         Float    = 0.18f,
    onClipTap:       (Int)    -> Unit,
    onClipLongPress: (Int)    -> Unit,
    onTrimHead:      (Long)   -> Unit,
    onTrimTail:      (Long)   -> Unit,
    onCommitTrim:    ()       -> Unit,
    onCancelTrim:    ()       -> Unit,
    modifier:        Modifier = Modifier
) {
    val density      = LocalDensity.current
    val totalWidthDp = with(density) { (totalMs * pxPerMs).toDp().coerceAtLeast(300.dp) }
    val scrollState  = rememberScrollState()

    val trackOrder = listOf(TrackType.VIDEO, TrackType.AUDIO, TrackType.SUBTITLE)
    val clipsByTrack = clips.groupBy { it.track }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            TvRuler(
                totalMs      = totalMs,
                pxPerMs      = pxPerMs,
                totalWidthDp = totalWidthDp,
                c            = c
            )

            trackOrder.forEach { track ->
                TvTrackRow(
                    track          = track,
                    clips          = clipsByTrack[track] ?: emptyList(),
                    selectedClipId = selectedClipId,
                    trimState      = trimState,
                    pxPerMs        = pxPerMs,
                    totalWidthDp   = totalWidthDp,
                    c              = c,
                    onClipTap      = onClipTap,
                    onClipLongPress= onClipLongPress,
                    onTrimHead     = onTrimHead,
                    onTrimTail     = onTrimTail,
                    onCommitTrim   = onCommitTrim
                )
            }
        }

        val playheadOffsetPx = playheadMs * pxPerMs - scrollState.value
        val playheadDp       = with(density) { playheadOffsetPx.toDp() }
        if (playheadOffsetPx >= 0) {
            Box(
                modifier = Modifier
                    .offset(x = playheadDp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(c.playhead)
            )
        }
    }
}

@Composable
private fun TvRuler(
    totalMs:      Long,
    pxPerMs:      Float,
    totalWidthDp: Dp,
    c:            EditorColors
) {
    Canvas(
        modifier = Modifier
            .width(totalWidthDp)
            .height(RULER_HEIGHT)
            .background(c.timeRuler)
    ) {
        val tickInterval = when {
            pxPerMs > 0.3f -> 1_000L
            pxPerMs > 0.1f -> 2_000L
            else           -> 5_000L
        }
        var t = 0L
        while (t <= totalMs + tickInterval) {
            val x       = t * pxPerMs
            val isMajor = t % (tickInterval * 5) == 0L
            drawLine(
                color       = c.subtle,
                start       = Offset(x, if (isMajor) 0f else size.height * 0.5f),
                end         = Offset(x, size.height),
                strokeWidth = if (isMajor) 1.5f else 0.8f
            )
            t += tickInterval
        }
    }
}

@Composable
private fun TvTrackRow(
    track:           TrackType,
    clips:           List<TimelineClip>,
    selectedClipId:  Int?,
    trimState:       TrimState?,
    pxPerMs:         Float,
    totalWidthDp:    Dp,
    c:               EditorColors,
    onClipTap:       (Int)  -> Unit,
    onClipLongPress: (Int)  -> Unit,
    onTrimHead:      (Long) -> Unit,
    onTrimTail:      (Long) -> Unit,
    onCommitTrim:    ()     -> Unit
) {
    val trackHeight = if (track == TrackType.SUBTITLE) 28.dp else CLIP_TRACK_HEIGHT

    Row(
        modifier          = Modifier
            .width(totalWidthDp)
            .height(trackHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(c.surface2)
        ) {
            clips.forEach { clip ->
                val isTrimming  = trimState?.clipId == clip.id
                val displayClip = if (isTrimming) {
                    clip.copy(
                        trimStartMs = trimState!!.pendingTrimStartMs,
                        durationMs  = trimState.pendingDurationMs
                    )
                } else clip

                TvClipBlock(
                    clip         = displayClip,
                    isSelected   = selectedClipId == clip.id,
                    isTrimming   = isTrimming,
                    trackHeight  = trackHeight,
                    pxPerMs      = pxPerMs,
                    c            = c,
                    onTap        = { onClipTap(clip.id) },
                    onLongPress  = { onClipLongPress(clip.id) },
                    onTrimHead   = onTrimHead,
                    onTrimTail   = onTrimTail,
                    onCommitTrim = onCommitTrim
                )
            }
        }
    }
}

@Composable
private fun TvClipBlock(
    clip:         TimelineClip,
    isSelected:   Boolean,
    isTrimming:   Boolean,
    trackHeight:  Dp,
    pxPerMs:      Float,
    c:            EditorColors,
    onTap:        () -> Unit,
    onLongPress:  () -> Unit,
    onTrimHead:   (Long) -> Unit,
    onTrimTail:   (Long) -> Unit,
    onCommitTrim: () -> Unit
) {
    val density = LocalDensity.current
    val startDp = with(density) { (clip.startMs * pxPerMs).toDp() }
    val widthDp = with(density) { (clip.durationMs * pxPerMs).toDp().coerceAtLeast(8.dp) }

    Box(
        modifier = Modifier
            .absoluteOffset(x = startDp)
            .width(widthDp)
            .fillMaxHeight()
            .padding(vertical = 3.dp, horizontal = 1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(clip.color.copy(alpha = if (isSelected) 0.85f else 0.45f))
            .then(
                if (isSelected)
                    Modifier.border(1.5.dp, clip.color, RoundedCornerShape(6.dp))
                else Modifier
            )
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
    ) {
        Text(
            text     = clip.name,
            style    = MaterialTheme.typography.labelSmall,
            color    = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = if (isTrimming) HANDLE_WIDTH + 2.dp else 5.dp)
        )

        if (isTrimming) {
            TvTrimHandle(
                side         = TvTrimSide.HEAD,
                pxPerMs      = pxPerMs,
                onDrag       = onTrimHead,
                onDragEnd    = onCommitTrim,
                modifier     = Modifier.align(Alignment.CenterStart)
            )
            TvTrimHandle(
                side         = TvTrimSide.TAIL,
                pxPerMs      = pxPerMs,
                onDrag       = onTrimTail,
                onDragEnd    = onCommitTrim,
                modifier     = Modifier.align(Alignment.CenterEnd)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(4.dp).fillMaxHeight()
                    .background(
                        Color.White.copy(alpha = if (isSelected) 0.8f else 0.3f),
                        RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                    )
                    .align(Alignment.CenterStart)
            )
            Box(
                modifier = Modifier
                    .width(4.dp).fillMaxHeight()
                    .background(
                        Color.White.copy(alpha = if (isSelected) 0.8f else 0.3f),
                        RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
                    )
                    .align(Alignment.CenterEnd)
            )
        }
    }
}

private enum class TvTrimSide { HEAD, TAIL }

@Composable
private fun TvTrimHandle(
    side:       TvTrimSide,
    pxPerMs:    Float,
    onDrag:     (Long) -> Unit,
    onDragEnd:  () -> Unit,
    modifier:   Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(HANDLE_WIDTH)
            .fillMaxHeight()
            .background(Color.White.copy(alpha = 0.85f))
            .pointerInput(side) {
                detectDragGestures(
                    onDragEnd    = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag       = { change, dragAmount ->
                        change.consume()
                        val deltaMs = (dragAmount.x / pxPerMs).roundToLong()
                        onDrag(deltaMs)
                    }
                )
            }
    )
}
