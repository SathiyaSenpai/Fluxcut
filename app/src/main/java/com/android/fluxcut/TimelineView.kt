package com.android.fluxcut

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val TRACK_HEIGHT = 48.dp
private val LABEL_WIDTH  = 44.dp
private val RULER_HEIGHT = 24.dp
private val HANDLE_W     = 10.dp

private data class TrackDef(
    val type:   TrackType,
    val label:  String,
    val color:  Color,
    val height: Dp = TRACK_HEIGHT
)

@Composable
fun MultiTrackTimeline(
    clips:           List<TimelineClip>,
    selectedClipId:  Int?,
    trimState:       TrimState?,
    playheadMs:      Long,
    totalMs:         Long,
    onClipTap:       (Int) -> Unit,
    onClipLongPress: (Int) -> Unit,
    onTrimHead:      (Long) -> Unit,
    onTrimTail:      (Long) -> Unit,
    onCommitTrim:    () -> Unit,
    onCancelTrim:    () -> Unit,
    modifier:        Modifier = Modifier,
    pxPerMs:         Float    = 0.18f
) {
    require(pxPerMs > 0f) { "pxPerMs must be positive" }
    val density      = LocalDensity.current
    val scrollState  = rememberScrollState()
    val totalWidthDp = with(density) { (totalMs * pxPerMs).toDp().coerceAtLeast(320.dp) }
    val clipsByTrack = clips.groupBy { it.track }

    val colorScheme = MaterialTheme.colorScheme
    val tracks = remember(colorScheme) {
        listOf(
            TrackDef(TrackType.VIDEO,    "VID", colorScheme.primary),
            TrackDef(TrackType.AUDIO,    "AUD", colorScheme.tertiary),
            TrackDef(TrackType.SUBTITLE, "SUB", colorScheme.secondary, height = 30.dp),
            TrackDef(TrackType.TITLE,    "TTL", colorScheme.primaryContainer, height = 30.dp),
            TrackDef(TrackType.FX,       "FX",  colorScheme.errorContainer, height = 30.dp),
        )
    }

    Column(modifier = modifier.background(colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                Spacer(Modifier.height(RULER_HEIGHT))
                tracks.forEach { track ->
                    Box(
                        modifier         = Modifier
                            .width(LABEL_WIDTH)
                            .height(track.height)
                            .background(colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(track.label, fontSize = 8.sp, color = track.color, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(1.dp))
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = { if (trimState != null) onCancelTrim() }
                        )
                ) {
                    Column(modifier = Modifier.width(totalWidthDp).testTag("TimelineContent")) {
                        TimelineRuler(totalMs = totalMs, pxPerMs = pxPerMs, widthDp = totalWidthDp)

                        tracks.forEach { track ->
                            TrackRow(
                                track           = track,
                                clips           = clipsByTrack[track.type] ?: emptyList(),
                                selectedClipId  = selectedClipId,
                                trimState       = trimState,
                                pxPerMs         = pxPerMs,
                                widthDp         = totalWidthDp,
                                onClipTap       = onClipTap,
                                onClipLongPress = onClipLongPress,
                                onTrimHead      = onTrimHead,
                                onTrimTail      = onTrimTail,
                                onCommitTrim    = onCommitTrim
                            )
                            Spacer(Modifier.height(1.dp))
                        }
                    }
                }

                val capOffsetPx = with(density) { 5.dp.toPx() }

                Box(
                    modifier = Modifier
                        .offset {
                            val x = (playheadMs * pxPerMs) - scrollState.value
                            IntOffset(x.roundToInt(), 0)
                        }
                        .width(2.dp)
                        .fillMaxHeight()
                        .testTag("Playhead")
                        .background(
                            Brush.verticalGradient(
                                listOf(colorScheme.primary, colorScheme.primary.copy(alpha = 0.25f))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .offset {
                            val x = (playheadMs * pxPerMs) - scrollState.value - capOffsetPx
                            IntOffset(x.roundToInt(), 0)
                        }
                        .size(10.dp)
                        .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun TimelineRuler(totalMs: Long, pxPerMs: Float, widthDp: Dp) {
    val colorScheme = MaterialTheme.colorScheme
    Canvas(
        modifier = Modifier
            .width(widthDp)
            .height(RULER_HEIGHT)
            .background(colorScheme.surfaceVariant)
    ) {
        val interval = when {
            pxPerMs > 0.5f -> 1_000L
            pxPerMs > 0.1f -> 2_000L
            else           -> 5_000L
        }
        var t = 0L
        while (t <= totalMs + interval) {
            val x       = t * pxPerMs
            val isMajor = t % (interval * 5) == 0L
            drawLine(
                color       = if (isMajor) colorScheme.onSurfaceVariant.copy(alpha = 0.9f) else colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                start       = Offset(x, if (isMajor) 0f else size.height * 0.55f),
                end         = Offset(x, size.height),
                strokeWidth = if (isMajor) 1.5f else 0.7f
            )
            t += interval
        }
    }
}

@Composable
private fun TrackRow(
    track:           TrackDef,
    clips:           List<TimelineClip>,
    selectedClipId:  Int?,
    trimState:       TrimState?,
    pxPerMs:         Float,
    widthDp:         Dp,
    onClipTap:       (Int) -> Unit,
    onClipLongPress: (Int) -> Unit,
    onTrimHead:      (Long) -> Unit,
    onTrimTail:      (Long) -> Unit,
    onCommitTrim:    () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .width(widthDp)
            .height(track.height)
            .background(colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(track.color.copy(alpha = 0.03f)))

        clips.forEach { clip ->
            val ts = trimState
            val isTrimming = ts != null && ts.clipId == clip.id
            val displayClip = if (isTrimming && ts != null) {
                clip.copy(
                    trimStartMs = ts.pendingTrimStartMs,
                    durationMs  = ts.pendingDurationMs
                )
            } else clip

            ClipBlock(
                clip         = displayClip,
                trackColor   = track.color,
                isSelected   = selectedClipId == clip.id,
                isTrimming   = isTrimming,
                trackHeight  = track.height,
                pxPerMs      = pxPerMs,
                onTap        = { onClipTap(clip.id) },
                onLongPress  = { onClipLongPress(clip.id) },
                onTrimHead   = onTrimHead,
                onTrimTail   = onTrimTail,
                onCommitTrim = onCommitTrim
            )
        }
    }
}

@Composable
private fun ClipBlock(
    clip:         TimelineClip,
    trackColor:   Color,
    isSelected:   Boolean,
    isTrimming:   Boolean,
    trackHeight:  Dp,
    pxPerMs:      Float,
    onTap:        () -> Unit,
    onLongPress:  () -> Unit,
    onTrimHead:   (Long) -> Unit,
    onTrimTail:   (Long) -> Unit,
    onCommitTrim: () -> Unit
) {
    val density = LocalDensity.current
    val startDp = with(density) { (clip.startMs * pxPerMs).toDp() }
    val widthDp = with(density) { (clip.durationMs * pxPerMs).toDp().coerceAtLeast(6.dp) }

    Box(
        modifier = Modifier
            .absoluteOffset(x = startDp)
            .width(widthDp)
            .height(trackHeight)
            .padding(vertical = 3.dp, horizontal = 1.dp)
            .shadow(if (isSelected) 4.dp else 0.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        trackColor.copy(alpha = if (isSelected) 0.90f else 0.55f),
                        trackColor.copy(alpha = if (isSelected) 0.70f else 0.35f)
                    )
                )
            )
            .then(if (isSelected) Modifier.border(1.5.dp, trackColor, RoundedCornerShape(6.dp)) else Modifier)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
    ) {
        Text(
            text     = clip.name,
            fontSize = 8.sp,
            color    = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = if (isTrimming) HANDLE_W + 3.dp else 5.dp)
        )

        if (isTrimming) {
                TrimHandle(
                    side         = TrimSide.HEAD,
                    pxPerMs      = pxPerMs,
                    color        = trackColor,
                    onDrag       = onTrimHead,
                    onDragEnd    = onCommitTrim,
                    modifier     = Modifier.align(Alignment.CenterStart).testTag("TrimHandleHead")
                )
                TrimHandle(
                    side         = TrimSide.TAIL,
                    pxPerMs      = pxPerMs,
                    color        = trackColor,
                    onDrag       = onTrimTail,
                    onDragEnd    = onCommitTrim,
                    modifier     = Modifier.align(Alignment.CenterEnd).testTag("TrimHandleTail")
                )
        } else {
            StaticHandle(
                modifier   = Modifier.align(Alignment.CenterStart),
                isSelected = isSelected,
                shape      = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
            )
            StaticHandle(
                modifier   = Modifier.align(Alignment.CenterEnd),
                isSelected = isSelected,
                shape      = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
            )
        }
    }
}

@Composable
private fun StaticHandle(modifier: Modifier, isSelected: Boolean, shape: RoundedCornerShape) {
    Box(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight()
            .background(Color.White.copy(alpha = if (isSelected) 0.8f else 0.25f), shape)
    )
}

private enum class TrimSide { HEAD, TAIL }

@Composable
private fun TrimHandle(
    side:      TrimSide,
    pxPerMs:   Float,
    color:     Color,
    onDrag:    (Long) -> Unit,
    onDragEnd: () -> Unit,
    modifier:  Modifier = Modifier
) {
    val safePxPerMs = pxPerMs.coerceAtLeast(0.001f)
    Box(
        modifier = modifier
            .width(HANDLE_W)
            .fillMaxHeight()
            .background(color.copy(alpha = 0.95f))
            .pointerInput(side) {
                detectDragGestures(
                    onDragEnd    = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag       = { change, drag ->
                        change.consume()
                        val deltaMs = (drag.x / safePxPerMs).roundToLong()
                            .coerceIn(-30_000L, 30_000L) // Clamp single step to 30s
                        onDrag(deltaMs)
                    }
                )
            }
    ) {
        Column(
            modifier            = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(10.dp)
                        .background(Color.White.copy(alpha = 0.7f))
                )
            }
        }
    }
}
