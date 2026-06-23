package com.android.fluxcut

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Editor Color Tokens ───────────────────────────────────────────────────────

data class EditorColors(
    val bg: Color,
    val previewBg: Color,
    val surface: Color,
    val surface2: Color,
    val onSurface: Color,
    val subtle: Color,
    val accent: Color,
    val trackVideo: Color,
    val trackAudio: Color,
    val trackSub: Color,
    val playhead: Color,
    val waveform: Color,
    val timeRuler: Color,
    val toolBar: Color,
)

@Composable
fun editorColors(dark: Boolean) = EditorColors(
    bg           = if (dark) Color(0xFF0D0D12) else Color(0xFFF0EFF5),
    previewBg    = if (dark) Color(0xFF000000) else Color(0xFF1A1A2E),
    surface      = if (dark) Color(0xFF1A1A28) else Color(0xFFFFFFFF),
    surface2     = if (dark) Color(0xFF23233A) else Color(0xFFEAE9F2),
    onSurface    = if (dark) Color(0xFFEEEEEE) else Color(0xFF111111),
    subtle       = if (dark) Color(0xFF666680) else Color(0xFF888888),
    accent       = Color(0xFF6C63FF),
    trackVideo   = Color(0xFF6C63FF),           // purple  — video track
    trackAudio   = Color(0xFF00BFA5),           // teal    — audio track
    trackSub     = Color(0xFFFFB300),           // amber   — subtitle track
    playhead     = Color(0xFFFF4D6D),           // red     — scrubber line
    waveform     = Color(0xFF00BFA5).copy(alpha = 0.7f),
    timeRuler    = if (dark) Color(0xFF333350) else Color(0xFFDDDCEE),
    toolBar      = if (dark) Color(0xFF13131F) else Color(0xFFFFFFFF),
)

// ── Clip Data Model ───────────────────────────────────────────────────────────

enum class TrackType { VIDEO, AUDIO, SUBTITLE }

data class TimelineClip(
    val id: Int,
    val name: String,
    val track: TrackType,
    val startMs: Long,        // start position in ms on timeline
    val durationMs: Long,     // clip length in ms
    val color: Color,
    val hasAudio: Boolean = false,
)

// ── Tool Definition ───────────────────────────────────────────────────────────

data class EditorTool(val icon: ImageVector, val label: String)

private val primaryTools = listOf(
    EditorTool(Icons.Outlined.ContentCut,    "Split"),
    EditorTool(Icons.Outlined.Speed,         "Speed"),
    EditorTool(Icons.Outlined.Tune,          "Adjust"),
    EditorTool(Icons.Outlined.FilterVintage, "Filter"),
    EditorTool(Icons.Outlined.TextFields,    "Text"),
    EditorTool(Icons.Outlined.MusicNote,     "Audio"),
    EditorTool(Icons.Outlined.Subtitles,     "Captions"),
    EditorTool(Icons.Outlined.AutoFixHigh,   "AI Tools"),
    EditorTool(Icons.Outlined.Layers,        "Overlay"),
    EditorTool(Icons.Outlined.SwapHoriz,     "Transition"),
)

// ── Main Editor Screen ────────────────────────────────────────────────────────

@Composable
fun EditorScreen(
    project: Project,
    onBackClick: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val c = editorColors(dark)

    // Playback state
    var isPlaying       by remember { mutableStateOf(false) }
    var playheadMs      by remember { mutableLongStateOf(0L) }
    var timelineZoom    by remember { mutableFloatStateOf(1f) }   // px per ms multiplier
    var selectedClipId  by remember { mutableStateOf<Int?>(null) }
    var activeTool      by remember { mutableStateOf<String?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }

    // Sample clips — replace with real Room data later
    val clips = remember {
        mutableStateListOf(
            TimelineClip(1, "Main Clip",   TrackType.VIDEO,    0L,    8000L,  c.trackVideo, hasAudio = true),
            TimelineClip(2, "B-Roll",      TrackType.VIDEO,    8500L, 5000L,  c.trackVideo, hasAudio = false),
            TimelineClip(3, "Voiceover",   TrackType.AUDIO,    0L,    6000L,  c.trackAudio),
            TimelineClip(4, "Background",  TrackType.AUDIO,    0L,    13500L, c.trackAudio),
            TimelineClip(5, "Subtitle 1",  TrackType.SUBTITLE, 500L,  3000L,  c.trackSub),
            TimelineClip(6, "Subtitle 2",  TrackType.SUBTITLE, 5000L, 2500L,  c.trackSub),
        )
    }

    val totalDurationMs = clips.maxOfOrNull { it.startMs + it.durationMs } ?: 10000L

    // Animate playhead when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startMs  = playheadMs
            val startTime = System.currentTimeMillis()
            while (isPlaying) {
                val elapsed = System.currentTimeMillis() - startTime
                val newPos  = startMs + elapsed
                if (newPos >= totalDurationMs) {
                    playheadMs = 0L
                    isPlaying  = false
                } else {
                    playheadMs = newPos
                }
                kotlinx.coroutines.delay(16L)   // ~60fps tick
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 1. Top Bar ─────────────────────────────────────────────────
            EditorTopBar(
                projectName  = project.title,
                c            = c,
                onBackClick  = onBackClick,
                onExportClick = { showExportSheet = true }
            )

            // ── 2. Preview Canvas ──────────────────────────────────────────
            PreviewCanvas(
                project    = project,
                c          = c,
                playheadMs = playheadMs,
                totalMs    = totalDurationMs,
                isPlaying  = isPlaying,
                modifier   = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
            )

            // ── 3. Playback Controls ───────────────────────────────────────
            PlaybackControls(
                isPlaying  = isPlaying,
                playheadMs = playheadMs,
                totalMs    = totalDurationMs,
                c          = c,
                onPlayPause = { isPlaying = !isPlaying },
                onSkipBack  = { playheadMs = 0L; isPlaying = false },
                onSkipFwd   = { playheadMs = totalDurationMs; isPlaying = false },
                onStepBack  = { playheadMs = (playheadMs - 1000L).coerceAtLeast(0L) },
                onStepFwd   = { playheadMs = (playheadMs + 1000L).coerceAtMost(totalDurationMs) },
            )

            // ── 4. Tool Strip ──────────────────────────────────────────────
            ToolStrip(
                tools       = primaryTools,
                activeTool  = activeTool,
                c           = c,
                onToolClick = { tool ->
                    activeTool = if (activeTool == tool) null else tool
                }
            )

            // ── 5. Timeline ────────────────────────────────────────────────
            TimelinePanel(
                clips         = clips,
                playheadMs    = playheadMs,
                totalMs       = totalDurationMs,
                zoom          = timelineZoom,
                selectedClipId = selectedClipId,
                c             = c,
                onPlayheadMove = { ms -> playheadMs = ms.coerceIn(0L, totalDurationMs) },
                onClipSelect   = { id -> selectedClipId = if (selectedClipId == id) null else id },
                onZoomChange   = { z -> timelineZoom = z.coerceIn(0.3f, 5f) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
            )
        }

        // ── Export Sheet ───────────────────────────────────────────────────
        if (showExportSheet) {
            ExportBottomSheet(
                project = project,
                c = c,
                dark = dark,
                onDismiss = { showExportSheet = false },
                onExport  = { showExportSheet = false }
            )
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
fun EditorTopBar(
    projectName: String,
    c: EditorColors,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.toolBar)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back + title
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = c.onSurface,
                modifier = Modifier.size(22.dp).clickable { onBackClick() }
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    projectName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Editing", fontSize = 11.sp, color = c.subtle)
            }
        }

        // Undo / Redo / Export
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo", tint = c.subtle, modifier = Modifier.size(20.dp))
            Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "Redo", tint = c.subtle, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.accent)
                    .clickable { onExportClick() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("Export", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Preview Canvas ────────────────────────────────────────────────────────────

@Composable
fun PreviewCanvas(
    project: Project,
    c: EditorColors,
    playheadMs: Long,
    totalMs: Long,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val aspectW = project.aspectRatio.split(":").getOrNull(0)?.toFloatOrNull() ?: 9f
    val aspectH = project.aspectRatio.split(":").getOrNull(1)?.toFloatOrNull() ?: 16f

    Box(
        modifier = modifier.background(c.previewBg),
        contentAlignment = Alignment.Center
    ) {
        // Video frame placeholder — shows aspect ratio correctly
        Box(
            modifier = Modifier
                .fillMaxHeight(0.88f)
                .aspectRatio(aspectW / aspectH)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, c.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    tint = c.accent.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${project.aspectRatio} · ${project.resolution} · ${project.fps}fps",
                    fontSize = 11.sp,
                    color = c.subtle,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatMs(playheadMs),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.onSurface.copy(alpha = 0.6f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // Preview timecode overlay — bottom left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = "${formatMs(playheadMs)} / ${formatMs(totalMs)}",
                fontSize = 10.sp,
                color = Color.White,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        // Resolution badge — top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.accent.copy(alpha = 0.85f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(project.resolution, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Playback Controls ─────────────────────────────────────────────────────────

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    playheadMs: Long,
    totalMs: Long,
    c: EditorColors,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipFwd: () -> Unit,
    onStepBack: () -> Unit,
    onStepFwd: () -> Unit,
) {
    val progress = if (totalMs > 0) playheadMs.toFloat() / totalMs.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.toolBar)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        // Scrub bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.surface2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(listOf(c.accent, c.trackAudio))
                    )
            )
        }

        Spacer(Modifier.height(6.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBtn(Icons.Outlined.SkipPrevious, "Skip to start", c.subtle) { onSkipBack() }
            IconBtn(Icons.Outlined.Replay10, "Step back 1s", c.subtle) { onStepBack() }

            // Play/Pause — larger
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(c.accent)
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconBtn(Icons.Outlined.Forward10, "Step fwd 1s", c.subtle) { onStepFwd() }
            IconBtn(Icons.Outlined.SkipNext, "Skip to end", c.subtle) { onSkipFwd() }
        }
    }
}

@Composable
private fun IconBtn(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = desc,
        tint = tint,
        modifier = Modifier.size(22.dp).clickable { onClick() }
    )
}

// ── Tool Strip ────────────────────────────────────────────────────────────────

@Composable
fun ToolStrip(
    tools: List<EditorTool>,
    activeTool: String?,
    c: EditorColors,
    onToolClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tools.forEach { tool ->
            val isActive = activeTool == tool.label
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) c.accent.copy(alpha = 0.18f) else Color.Transparent)
                    .border(
                        width = if (isActive) 1.dp else 0.dp,
                        color = if (isActive) c.accent else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onToolClick(tool.label) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    tool.icon,
                    contentDescription = tool.label,
                    tint = if (isActive) c.accent else c.subtle,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    tool.label,
                    fontSize = 10.sp,
                    color = if (isActive) c.accent else c.subtle,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

// ── Timeline Panel ────────────────────────────────────────────────────────────

private val TRACK_HEIGHT = 44.dp
private val TRACK_LABEL_WIDTH = 36.dp
private val PX_PER_MS_BASE = 0.06f     // base: 1ms = 0.06dp

@Composable
fun TimelinePanel(
    clips: List<TimelineClip>,
    playheadMs: Long,
    totalMs: Long,
    zoom: Float,
    selectedClipId: Int?,
    c: EditorColors,
    onPlayheadMove: (Long) -> Unit,
    onClipSelect: (Int) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pxPerMs = PX_PER_MS_BASE * zoom
    val scrollState = rememberScrollState()

    // Group clips by track type
    val videoClips    = clips.filter { it.track == TrackType.VIDEO }
    val audioClips    = clips.filter { it.track == TrackType.AUDIO }
    val subtitleClips = clips.filter { it.track == TrackType.SUBTITLE }

    Column(
        modifier = modifier
            .background(c.bg)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoomDelta, _ ->
                    onZoomChange(zoom * zoomDelta)
                }
            }
    ) {
        // Time ruler
        TimeRuler(
            totalMs    = totalMs,
            pxPerMs    = pxPerMs,
            playheadMs = playheadMs,
            c          = c,
            labelWidth = TRACK_LABEL_WIDTH,
            onScrub    = onPlayheadMove,
        )

        // Track rows
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // VIDEO tracks
                TrackRow(
                    label      = "V",
                    labelColor = c.trackVideo,
                    clips      = videoClips,
                    totalMs    = totalMs,
                    pxPerMs    = pxPerMs,
                    selectedId = selectedClipId,
                    c          = c,
                    onClipTap  = onClipSelect,
                )

                // AUDIO tracks — two rows (voiceover + bg music)
                TrackRow(
                    label      = "A",
                    labelColor = c.trackAudio,
                    clips      = audioClips,
                    totalMs    = totalMs,
                    pxPerMs    = pxPerMs,
                    selectedId = selectedClipId,
                    c          = c,
                    onClipTap  = onClipSelect,
                    showWaveform = true
                )

                // SUBTITLE tracks
                TrackRow(
                    label      = "S",
                    labelColor = c.trackSub,
                    clips      = subtitleClips,
                    totalMs    = totalMs,
                    pxPerMs    = pxPerMs,
                    selectedId = selectedClipId,
                    c          = c,
                    onClipTap  = onClipSelect,
                    trackHeight = 28.dp
                )

                // Add track button
                Row(
                    modifier = Modifier
                        .padding(start = TRACK_LABEL_WIDTH + 4.dp, top = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.surface2)
                        .clickable { }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add track", tint = c.subtle, modifier = Modifier.size(14.dp))
                    Text("Add Track", fontSize = 11.sp, color = c.subtle)
                }
            }

            // Playhead overlay — rendered on top of all tracks
            PlayheadOverlay(
                playheadMs     = playheadMs,
                totalMs        = totalMs,
                pxPerMs        = pxPerMs,
                labelWidth     = TRACK_LABEL_WIDTH,
                scrollState    = scrollState,
                c              = c,
                modifier       = Modifier.fillMaxSize()
            )
        }
    }
}

// ── Time Ruler ────────────────────────────────────────────────────────────────

@Composable
fun TimeRuler(
    totalMs: Long,
    pxPerMs: Float,
    playheadMs: Long,
    c: EditorColors,
    labelWidth: Dp,
    onScrub: (Long) -> Unit
) {
    val totalWidthDp = (totalMs * pxPerMs).dp + labelWidth + 40.dp
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(c.timeRuler)
            .horizontalScroll(scrollState)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val clickX = offset.x - labelWidth.toPx()
                    if (clickX >= 0f) {
                        val ms = (clickX / pxPerMs).toLong()
                        onScrub(ms)
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val dragX = change.position.x - labelWidth.toPx()
                    if (dragX >= 0f) onScrub((dragX / pxPerMs).toLong())
                }
            }
    ) {
        Canvas(modifier = Modifier.width(totalWidthDp).fillMaxHeight()) {
            val offsetX = labelWidth.toPx()
            val tickInterval = when {
                pxPerMs > 0.3f -> 1000L     // 1s ticks when zoomed in
                pxPerMs > 0.1f -> 2000L
                else -> 5000L
            }
            var t = 0L
            while (t <= totalMs + tickInterval) {
                val x = offsetX + t * pxPerMs
                val isMajor = t % (tickInterval * 5) == 0L
                drawLine(
                    color = c.subtle,
                    start = Offset(x, if (isMajor) 0f else size.height * 0.5f),
                    end   = Offset(x, size.height),
                    strokeWidth = if (isMajor) 1.5f else 0.8f
                )
                t += tickInterval
            }
        }

        // Time labels — drawn every 5 ticks
        Row(
            modifier = Modifier
                .width(totalWidthDp)
                .fillMaxHeight()
                .padding(start = labelWidth),
        ) {
            val tickInterval = when {
                pxPerMs > 0.3f -> 1000L
                pxPerMs > 0.1f -> 2000L
                else -> 5000L
            }
            val labelInterval = tickInterval * 5
            var t = 0L
            while (t <= totalMs) {
                val widthDp = (labelInterval * pxPerMs).dp
                Box(modifier = Modifier.width(widthDp), contentAlignment = Alignment.TopStart) {
                    Text(
                        formatMs(t),
                        fontSize = 9.sp,
                        color = c.subtle,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                    )
                }
                t += labelInterval
            }
        }
    }
}

// ── Track Row ─────────────────────────────────────────────────────────────────

@Composable
fun TrackRow(
    label: String,
    labelColor: Color,
    clips: List<TimelineClip>,
    totalMs: Long,
    pxPerMs: Float,
    selectedId: Int?,
    c: EditorColors,
    onClipTap: (Int) -> Unit,
    showWaveform: Boolean = false,
    trackHeight: Dp = TRACK_HEIGHT,
) {
    val totalWidthDp = (totalMs * pxPerMs).dp + TRACK_LABEL_WIDTH + 40.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track label — colored vertical bar
        Box(
            modifier = Modifier
                .width(TRACK_LABEL_WIDTH)
                .fillMaxHeight()
                .background(c.surface),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(0.6f)
                    .background(labelColor, RoundedCornerShape(2.dp))
            )
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Track content — clip blocks
        Box(
            modifier = Modifier
                .width(totalWidthDp - TRACK_LABEL_WIDTH)
                .fillMaxHeight()
                .background(c.surface2)
        ) {
            clips.forEach { clip ->
                val startDp   = (clip.startMs * pxPerMs).dp
                val widthDp   = (clip.durationMs * pxPerMs).dp
                val isSelected = selectedId == clip.id

                ClipBlock(
                    clip        = clip,
                    widthDp     = widthDp,
                    trackHeight = trackHeight,
                    isSelected  = isSelected,
                    showWaveform = showWaveform,
                    c           = c,
                    modifier    = Modifier
                        .absoluteOffset(x = startDp)
                        .clickable { onClipTap(clip.id) }
                )
            }
        }
    }
}

// ── Clip Block ────────────────────────────────────────────────────────────────

@Composable
fun ClipBlock(
    clip: TimelineClip,
    widthDp: Dp,
    trackHeight: Dp,
    isSelected: Boolean,
    showWaveform: Boolean,
    c: EditorColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
    ) {
        // Clip name
        Text(
            clip.name,
            fontSize = 9.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
        )

        // Waveform for audio tracks
        if (showWaveform) {
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 14.dp, bottom = 4.dp)) {
                val bars = (size.width / 4f).toInt()
                for (i in 0 until bars) {
                    val x = i * 4f
                    val barHeight = (size.height * 0.3f) + (size.height * 0.5f) * ((i * 7 + 3) % 11).toFloat() / 11f
                    drawLine(
                        color = c.waveform,
                        start = Offset(x, size.height / 2f - barHeight / 2f),
                        end   = Offset(x, size.height / 2f + barHeight / 2f),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Left trim handle
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = if (isSelected) 0.8f else 0.3f), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                .align(Alignment.CenterStart)
        )
        // Right trim handle
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = if (isSelected) 0.8f else 0.3f), RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                .align(Alignment.CenterEnd)
        )
    }
}

// ── Playhead Overlay ──────────────────────────────────────────────────────────

@Composable
fun PlayheadOverlay(
    playheadMs: Long,
    totalMs: Long,
    pxPerMs: Float,
    labelWidth: Dp,
    scrollState: androidx.compose.foundation.ScrollState,
    c: EditorColors,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val x = labelWidth.toPx() + (playheadMs * pxPerMs) - scrollState.value.toFloat()
        if (x < labelWidth.toPx() || x > size.width) return@Canvas

        // Playhead line
        drawLine(
            color = c.playhead,
            start = Offset(x, 0f),
            end   = Offset(x, size.height),
            strokeWidth = 2f
        )
        // Playhead head triangle
        drawCircle(
            color  = c.playhead,
            radius = 6f,
            center = Offset(x, 6f)
        )
    }
}

// ── Export Bottom Sheet ───────────────────────────────────────────────────────

@Composable
fun ExportBottomSheet(
    project: Project,
    c: EditorColors,
    dark: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    var selectedRes by remember { mutableStateOf(project.resolution) }
    var selectedFps by remember { mutableIntStateOf(project.fps) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(c.surface)
                .clickable { /* consume */ }
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.subtle)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            Text("Export Video", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("Project: ${project.title}", fontSize = 13.sp, color = c.subtle)

            Spacer(Modifier.height(20.dp))

            // Resolution picker
            Text("Resolution", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.subtle)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("720p", "1080p", "4K").forEach { res ->
                    val active = selectedRes == res
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) c.accent else c.surface2)
                            .clickable { selectedRes = res }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(res, fontSize = 13.sp, color = if (active) Color.White else c.onSurface, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // FPS picker
            Text("Frame Rate", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.subtle)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(24, 30, 60).forEach { fps ->
                    val active = selectedFps == fps
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) c.accent else c.surface2)
                            .clickable { selectedFps = fps }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("${fps}fps", fontSize = 13.sp, color = if (active) Color.White else c.onSurface, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Export summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.surface2)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ExportSummaryRow("Format",     "MP4 / H.264", c)
                    ExportSummaryRow("Resolution", selectedRes, c)
                    ExportSummaryRow("Frame Rate", "${selectedFps}fps", c)
                    ExportSummaryRow("Aspect Ratio", project.aspectRatio, c)
                    ExportSummaryRow("Watermark",  "None ✓", c)
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export to Gallery", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ExportSummaryRow(label: String, value: String, c: EditorColors) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = c.subtle)
        Text(value, fontSize = 12.sp, color = c.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val centis = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(min, sec, centis)
}