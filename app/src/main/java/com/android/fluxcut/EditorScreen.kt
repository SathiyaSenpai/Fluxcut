package com.android.fluxcut

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi

private val BG         = Color(0xFF0A0A0F)
private val SURFACE    = Color(0xFF1A1A2E)
private val SURFACE2   = Color(0xFF12121C)
private val SURFACE3   = Color(0xFF0F0F1A)
private val DIVIDER    = Color(0xFF232338)
private val ACCENT     = Color(0xFF6C63FF)
private val ON_SURFACE = Color(0xFFEEEEEE)
private val SUBTLE     = Color(0xFF666680)
private val ERR        = Color(0xFFFF5C5C)
private val PLAYHEAD   = Color(0xFF6C63FF)

private val GREEN = Color(0xFF34D399)
private val AMBER = Color(0xFFF59E0B)
private val ROSE  = Color(0xFFF43F5E)
private val SKY   = Color(0xFF38BDF8)

private enum class ClipTool(
    val label:     String,
    val icon:      ImageVector,
    val color:     Color,
    val needsClip: Boolean = true
) {
    SPLIT  ("Split",  Icons.Outlined.ContentCut,  ACCENT, needsClip = true),
    TRIM   ("Trim",   Icons.Outlined.Straighten,  ACCENT, needsClip = true),
    SPEED  ("Speed",  Icons.Outlined.Speed,       AMBER,  needsClip = true),
    CROP   ("Crop",   Icons.Outlined.CropFree,    GREEN,  needsClip = true),
    AUDIO  ("Audio",  Icons.Outlined.VolumeUp,    SKY,    needsClip = false),
    TEXT   ("Text",   Icons.Outlined.TextFields,  AMBER,  needsClip = false),
    FILTER ("Filter", Icons.Outlined.AutoFixHigh, ROSE,   needsClip = false),
    DELETE ("Delete", Icons.Outlined.Delete,      ERR,    needsClip = true),
}

private sealed class ExportUiState {
    object Idle                         : ExportUiState()
    data class InProgress(val pct: Int) : ExportUiState()
    data class Done(val path: String)   : ExportUiState()
    data class Failed(val msg: String)  : ExportUiState()
}

@UnstableApi
@Composable
fun EditorScreen(project: Project, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo    = remember { ProjectRepository(context) }
    val vm: EditorViewModel = viewModel(
        factory = EditorViewModel.Factory(project, repo, context)
    )

    val state        by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    var activeTool  by remember { mutableStateOf<ClipTool?>(null) }
    var showExport  by remember { mutableStateOf(false) }
    var exportState by remember { mutableStateOf<ExportUiState>(ExportUiState.Idle) }
    var autoFired   by remember { mutableStateOf(false) }

    val importLauncher = rememberMediaImportLauncher { imported ->
        val nextId  = (state.clips.maxOfOrNull { it.id } ?: 0) + 1
        val startMs = state.clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
        val clip    = imported.toTimelineClip(
            id         = nextId,
            startMs    = startMs,
            videoColor = ACCENT,
            audioColor = GREEN
        )
        vm.addClip(clip)
        vm.selectClip(clip.id)
    }

    LaunchedEffect(state.clips.isEmpty(), autoFired) {
        if (!autoFired && state.clips.isEmpty()) { autoFired = true; importLauncher() }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHost.showSnackbar(it); vm.clearError() }
    }

    if (showExport) {
        ExportScreen(
            project     = project,
            clips       = state.clips,
            exportState = exportState,
            onExport    = { cfg ->
                exportState = ExportUiState.InProgress(0)
                FFmpegEngine.export(context, cfg) { r ->
                    exportState = when (r) {
                        is ExportResult.Progress -> ExportUiState.InProgress(r.percent)
                        is ExportResult.Success  -> ExportUiState.Done(r.outputPath)
                        is ExportResult.Failure  -> ExportUiState.Failed(r.message)
                    }
                }
            },
            onReset = { exportState = ExportUiState.Idle },
            onBack  = { showExport = false }
        )
        return
    }

    Scaffold(
        containerColor = BG,
        snackbarHost   = { SnackbarHost(snackbarHost) }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {

            EditorTopBar(
                title     = state.project?.title ?: project.title,
                onBack    = onBack,
                onAddClip = { importLauncher() },
                onExport  = { showExport = true }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.36f)
                    .background(Color.Black)
            ) {
                VideoPlayerView(player = vm.player, modifier = Modifier.fillMaxSize())
                if (state.clips.isEmpty()) {
                    EmptyPreviewOverlay { importLauncher() }
                }
            }

            TransportRow(
                isPlaying   = state.isPlaying,
                playheadMs  = state.playheadMs,
                totalMs     = state.totalDurationMs,
                onToggle    = { vm.togglePlayPause() },
                onSeek      = { vm.seekTo(it) },
                onStepBack  = { vm.stepBack() },
                onStepFwd   = { vm.stepForward() },
                onSkipStart = { vm.skipToStart() },
                onSkipEnd   = { vm.skipToEnd() }
            )

            ToolChipRow(
                activeTool     = activeTool,
                selectedClipId = state.selectedClipId,
                onSelect       = { tool ->
                    if (tool == ClipTool.DELETE) {
                        state.selectedClipId?.let { vm.deleteClip(it) }
                        activeTool = null
                    } else {
                        activeTool = if (activeTool == tool) null else tool
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.48f)
            ) {
                MultiTrackTimeline(
                    clips           = state.clips,
                    selectedClipId  = state.selectedClipId,
                    trimState       = state.trimState,
                    playheadMs      = state.playheadMs,
                    totalMs         = state.totalDurationMs.coerceAtLeast(1L),
                    onClipTap       = { vm.selectClip(it) },
                    onClipLongPress = { id -> activeTool = ClipTool.TRIM; vm.startTrim(id) },
                    onTrimHead      = { vm.updateTrimHead(it) },
                    onTrimTail      = { vm.updateTrimTail(it) },
                    onCommitTrim    = { vm.commitTrim(); activeTool = null },
                    onCancelTrim    = { vm.cancelTrim(); activeTool = null },
                    modifier        = Modifier.fillMaxSize()
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible  = activeTool != null,
                    enter    = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
                    exit     = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(160)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    activeTool?.let { tool ->
                        ToolPanel(
                            tool      = tool,
                            state     = state,
                            vm        = vm,
                            onDismiss = { activeTool = null },
                            onImport  = { importLauncher() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    title:     String,
    onBack:    () -> Unit,
    onAddClip: () -> Unit,
    onExport:  () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ON_SURFACE, maxLines = 1)
                Text("Video Editor", fontSize = 10.sp, color = SUBTLE)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ON_SURFACE)
            }
        },
        actions = {
            IconButton(onClick = onAddClip) {
                Icon(Icons.Outlined.Add, contentDescription = "Add clip", tint = ACCENT)
            }
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ACCENT)
                    .clickable { onExport() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Outlined.FileUpload, contentDescription = "Export", tint = Color.White, modifier = Modifier.size(15.dp))
                    Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SURFACE)
    )
}

@Composable
private fun EmptyPreviewOverlay(onPick: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(ACCENT.copy(alpha = 0.18f))
                    .border(1.5.dp, ACCENT.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.VideoLibrary, contentDescription = null, tint = ACCENT, modifier = Modifier.size(28.dp))
            }
            Text("No media yet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ON_SURFACE)
            Button(
                onClick = onPick,
                colors  = ButtonDefaults.buttonColors(containerColor = ACCENT),
                shape   = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Pick Video / Photo", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TransportRow(
    isPlaying:   Boolean,
    playheadMs:  Long,
    totalMs:     Long,
    onToggle:    () -> Unit,
    onSeek:      (Long) -> Unit,
    onStepBack:  () -> Unit,
    onStepFwd:   () -> Unit,
    onSkipStart: () -> Unit,
    onSkipEnd:   () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SURFACE)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        if (totalMs > 0L) {
            Slider(
                value         = playheadMs.toFloat(),
                valueRange    = 0f..totalMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                modifier      = Modifier.fillMaxWidth().height(26.dp),
                colors        = SliderDefaults.colors(
                    thumbColor         = ACCENT,
                    activeTrackColor   = ACCENT,
                    inactiveTrackColor = DIVIDER
                )
            )
        } else {
            Spacer(Modifier.height(12.dp))
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = "${fmtMs(playheadMs)} / ${fmtMs(totalMs)}",
                fontSize   = 10.sp,
                color      = SUBTLE,
                fontFamily = FontFamily.Monospace
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                TBtn(Icons.Filled.SkipPrevious, onSkipStart)
                TBtn(Icons.Filled.Replay5,      onStepBack)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ACCENT)
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                TBtn(Icons.Filled.Forward5, onStepFwd)
                TBtn(Icons.Filled.SkipNext, onSkipEnd)
            }
            Spacer(Modifier.width(72.dp))
        }
    }
}

@Composable
private fun TBtn(icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, contentDescription = null, tint = ON_SURFACE, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun ToolChipRow(
    activeTool:     ClipTool?,
    selectedClipId: Int?,
    onSelect:       (ClipTool) -> Unit
) {
    LazyRow(
        modifier              = Modifier
            .fillMaxWidth()
            .background(SURFACE2)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding        = PaddingValues(horizontal = 2.dp)
    ) {
        items(ClipTool.entries) { tool ->
            val enabled   = !tool.needsClip || selectedClipId != null
            val isActive  = activeTool == tool
            val chipColor = if (enabled) tool.color else SUBTLE

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) chipColor.copy(alpha = 0.25f) else SURFACE.copy(alpha = 0.6f))
                    .border(
                        width = if (isActive) 1.5.dp else 1.dp,
                        color = if (isActive) chipColor else DIVIDER,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(enabled = enabled) { onSelect(tool) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(tool.icon, contentDescription = tool.label, tint = chipColor, modifier = Modifier.size(14.dp))
                Text(
                    tool.label,
                    fontSize   = 12.sp,
                    color      = chipColor,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
    }
}

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

private val TRACKS = listOf(
    TrackDef(TrackType.VIDEO,    "VID", ACCENT),
    TrackDef(TrackType.AUDIO,    "AUD", GREEN),
    TrackDef(TrackType.SUBTITLE, "SUB", AMBER, height = 30.dp),
    TrackDef(TrackType.TITLE,    "TTL", SKY,   height = 30.dp),
    TrackDef(TrackType.FX,       "FX",  ROSE,  height = 30.dp),
)

@Composable
private fun MultiTrackTimeline(
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
    val density      = LocalDensity.current
    val scrollState  = rememberScrollState()
    val totalWidthDp = with(density) { (totalMs * pxPerMs).toDp().coerceAtLeast(320.dp) }
    val clipsByTrack = clips.groupBy { it.track }

    Column(modifier = modifier.background(SURFACE3)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                Spacer(Modifier.height(RULER_HEIGHT))
                TRACKS.forEach { track ->
                    Box(
                        modifier         = Modifier
                            .width(LABEL_WIDTH)
                            .height(track.height)
                            .background(SURFACE2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(track.label, fontSize = 8.sp, color = track.color, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(1.dp))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                Column(modifier = Modifier.width(totalWidthDp)) {
                    TimelineRuler(totalMs = totalMs, pxPerMs = pxPerMs, widthDp = totalWidthDp)

                    TRACKS.forEach { track ->
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

                val playheadOffsetDp = with(density) { (playheadMs * pxPerMs).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = playheadOffsetDp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(listOf(PLAYHEAD, PLAYHEAD.copy(alpha = 0.25f)))
                        )
                )
                Box(
                    modifier = Modifier
                        .offset(x = playheadOffsetDp - 5.dp, y = 0.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                        .background(PLAYHEAD)
                )
            }
        }
    }
}

@Composable
private fun TimelineRuler(totalMs: Long, pxPerMs: Float, widthDp: Dp) {
    Canvas(
        modifier = Modifier
            .width(widthDp)
            .height(RULER_HEIGHT)
            .background(SURFACE2)
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
                color       = if (isMajor) SUBTLE.copy(alpha = 0.9f) else SUBTLE.copy(alpha = 0.4f),
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
    Box(
        modifier = Modifier
            .width(widthDp)
            .height(track.height)
            .background(SURFACE3)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(track.color.copy(alpha = 0.03f)))

        clips.forEach { clip ->
            val isTrimming  = trimState?.clipId == clip.id
            val displayClip = if (isTrimming) {
                clip.copy(
                    trimStartMs = trimState!!.pendingTrimStartMs,
                    durationMs  = trimState.pendingDurationMs
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
                modifier     = Modifier.align(Alignment.CenterStart)
            )
            TrimHandle(
                side         = TrimSide.TAIL,
                pxPerMs      = pxPerMs,
                color        = trackColor,
                onDrag       = onTrimTail,
                onDragEnd    = onCommitTrim,
                modifier     = Modifier.align(Alignment.CenterEnd)
            )
        } else {
            StaticHandle(
                modifier   = Modifier.align(Alignment.CenterStart),
                color      = trackColor,
                isSelected = isSelected,
                shape      = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
            )
            StaticHandle(
                modifier   = Modifier.align(Alignment.CenterEnd),
                color      = trackColor,
                isSelected = isSelected,
                shape      = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
            )
        }
    }
}

@Composable
private fun StaticHandle(modifier: Modifier, color: Color, isSelected: Boolean, shape: RoundedCornerShape) {
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
                        onDrag((drag.x / pxPerMs).toLong())
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

@Composable
private fun ToolPanel(
    tool:      ClipTool,
    state:     EditorUiState,
    vm:        EditorViewModel,
    onDismiss: () -> Unit,
    onImport:  () -> Unit
) {
    Surface(
        modifier       = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        shape          = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color          = SURFACE,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(tool.color.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(14.dp))
                    }
                    Text(tool.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ON_SURFACE)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = SUBTLE, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DIVIDER, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            when (tool) {
                ClipTool.SPLIT  -> SplitPanel(state, vm, onDismiss)
                ClipTool.TRIM   -> TrimPanel(state, vm, onDismiss)
                ClipTool.SPEED  -> SpeedPanel(onDismiss)
                ClipTool.CROP   -> CropPanel(onDismiss)
                ClipTool.AUDIO  -> AudioPanel(onDismiss)
                ClipTool.TEXT   -> TextPanel(onDismiss)
                ClipTool.FILTER -> FilterPanel(onDismiss)
                ClipTool.DELETE -> { }
            }
        }
    }
}

@Composable
private fun SplitPanel(state: EditorUiState, vm: EditorViewModel, onDismiss: () -> Unit) {
    val clipId = state.selectedClipId
    val clip   = clipId?.let { id -> state.clips.find { it.id == id } }

    if (clip == null) {
        Text("Select a clip on the timeline first", fontSize = 13.sp, color = SUBTLE)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SURFACE2)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InfoCell("Clip",     clip.name)
            InfoCell("At",       fmtMs(state.playheadMs))
            InfoCell("Duration", fmtMs(clip.durationMs))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                border   = BorderStroke(1.dp, DIVIDER),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SUBTLE)
            ) { Text("Cancel") }
            Button(
                onClick  = { vm.splitClipAtPlayhead(clipId); onDismiss() },
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = ACCENT)
            ) {
                Icon(Icons.Outlined.ContentCut, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Split Here", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TrimPanel(state: EditorUiState, vm: EditorViewModel, onDismiss: () -> Unit) {
    val clipId    = state.selectedClipId
    val trimState = state.trimState

    LaunchedEffect(clipId) {
        if (trimState == null && clipId != null) vm.startTrim(clipId)
    }

    if (trimState == null) {
        Text("Select a clip to trim", fontSize = 13.sp, color = SUBTLE)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SURFACE2)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InfoCell("In",       fmtMs(trimState.pendingTrimStartMs))
            InfoCell("Duration", fmtMs(trimState.pendingDurationMs))
            InfoCell("Out",      fmtMs(trimState.pendingTrimStartMs + trimState.pendingDurationMs))
        }
        Text(
            "Drag handles on the clip to adjust trim",
            fontSize  = 11.sp,
            color     = SUBTLE,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = { vm.cancelTrim(); onDismiss() },
                modifier = Modifier.weight(1f),
                border   = BorderStroke(1.dp, DIVIDER),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SUBTLE)
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Cancel")
            }
            Button(
                onClick  = { vm.commitTrim(); onDismiss() },
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = ACCENT)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Apply", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SpeedPanel(onDismiss: () -> Unit) {
    var speed   by remember { mutableFloatStateOf(1f) }
    val presets = listOf(0.25f, 0.5f, 1f, 1.5f, 2f, 4f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { p ->
                val active = speed == p
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) AMBER else SURFACE2)
                        .border(1.dp, if (active) AMBER else DIVIDER, RoundedCornerShape(8.dp))
                        .clickable { speed = p }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "${p}x",
                        fontSize   = 13.sp,
                        color      = if (active) Color.White else SUBTLE,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = "${"%.2f".format(speed)}x",
                fontSize = 12.sp,
                color    = ON_SURFACE,
                modifier = Modifier.width(42.dp)
            )
            Slider(
                value         = speed,
                valueRange    = 0.25f..4f,
                onValueChange = { speed = it },
                modifier      = Modifier.weight(1f),
                colors        = SliderDefaults.colors(
                    thumbColor         = AMBER,
                    activeTrackColor   = AMBER,
                    inactiveTrackColor = DIVIDER
                )
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                border   = BorderStroke(1.dp, DIVIDER),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SUBTLE)
            ) { Text("Cancel") }
            Button(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = AMBER)
            ) { Text("Apply ${"%.2f".format(speed)}x", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun CropPanel(onDismiss: () -> Unit) {
    val ratios   = listOf("Free", "16:9", "9:16", "1:1", "4:3", "4:5")
    var selected by remember { mutableStateOf("Free") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ratios) { r ->
                val active = selected == r
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) GREEN else SURFACE2)
                        .border(1.dp, if (active) GREEN else DIVIDER, RoundedCornerShape(8.dp))
                        .clickable { selected = r }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        r,
                        fontSize   = 12.sp,
                        color      = if (active) Color.White else SUBTLE,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Text("Crop & aspect ratio applied on export via FFmpeg.", fontSize = 11.sp, color = SUBTLE)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                border   = BorderStroke(1.dp, DIVIDER),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SUBTLE)
            ) { Text("Cancel") }
            Button(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = GREEN)
            ) { Text("Apply $selected", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun AudioPanel(onDismiss: () -> Unit) {
    var masterVol by remember { mutableFloatStateOf(1f) }
    var musicVol  by remember { mutableFloatStateOf(0.5f) }
    var sfxVol    by remember { mutableFloatStateOf(0.8f) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AudioFader("Master",      masterVol, ACCENT) { masterVol = it }
        AudioFader("Music",       musicVol,  GREEN)  { musicVol  = it }
        AudioFader("SFX / Voice", sfxVol,   AMBER)  { sfxVol    = it }
        Button(
            onClick  = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = SKY)
        ) { Text("Done", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun AudioFader(label: String, value: Float, accent: Color, onChange: (Float) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, color = ON_SURFACE, modifier = Modifier.width(70.dp))
        Slider(
            value         = value,
            onValueChange = onChange,
            modifier      = Modifier.weight(1f),
            colors        = SliderDefaults.colors(
                thumbColor         = accent,
                activeTrackColor   = accent,
                inactiveTrackColor = DIVIDER
            )
        )
        Text(
            text      = "${(value * 100).toInt()}%",
            fontSize  = 10.sp,
            color     = SUBTLE,
            modifier  = Modifier.width(34.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TextPanel(onDismiss: () -> Unit) {
    val styles   = listOf("Lower Third" to ACCENT, "Bold Title" to AMBER, "Subtitle" to GREEN, "Watermark" to SUBTLE)
    var selected by remember { mutableStateOf("Lower Third") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(styles) { (name, color) ->
                val active = selected == name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) color.copy(alpha = 0.2f) else SURFACE2)
                        .border(1.dp, if (active) color else DIVIDER, RoundedCornerShape(8.dp))
                        .clickable { selected = name }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        name,
                        fontSize   = 12.sp,
                        color      = if (active) color else SUBTLE,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Text("Text overlays are added to the TITLE track.", fontSize = 11.sp, color = SUBTLE)
        Button(
            onClick  = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = AMBER)
        ) { Text("Add \"$selected\"", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun FilterPanel(onDismiss: () -> Unit) {
    val filters = listOf(
        "None"      to Color(0xFF6C63FF),
        "Cinematic" to Color(0xFFF59E0B),
        "B&W"       to Color(0xFF888888),
        "Warm"      to Color(0xFFEF4444),
        "Cool"      to Color(0xFF60A5FA),
        "Vivid"     to Color(0xFF34D399)
    )
    var selected by remember { mutableStateOf("None") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filters) { (name, color) ->
                val active = selected == name
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, if (active) color else DIVIDER, RoundedCornerShape(10.dp))
                        .clickable { selected = name }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.radialGradient(listOf(color.copy(alpha = 0.7f), color.copy(alpha = 0.2f)))
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        name,
                        fontSize   = 10.sp,
                        color      = if (active) color else SUBTLE,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                border   = BorderStroke(1.dp, DIVIDER),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SUBTLE)
            ) { Text("Cancel") }
            Button(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = ROSE)
            ) { Text("Apply $selected", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun ExportScreen(
    project:     Project,
    clips:       List<TimelineClip>,
    exportState: ExportUiState,
    onExport:    (ExportConfig) -> Unit,
    onReset:     () -> Unit,
    onBack:      () -> Unit
) {
    var resolution by remember { mutableStateOf("1080p") }
    var fps        by remember { mutableIntStateOf(project.fps.takeIf { it > 0 } ?: 30) }
    val videoCount = clips.count { it.track == TrackType.VIDEO && it.sourceUri != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SURFACE)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = onBack,
                enabled  = exportState !is ExportUiState.InProgress
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ON_SURFACE)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Export Video", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ON_SURFACE)
                Text(project.title, fontSize = 11.sp, color = SUBTLE, maxLines = 1)
            }
        }

        HorizontalDivider(color = DIVIDER, thickness = 0.5.dp)

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (exportState) {
                is ExportUiState.Idle -> {
                    ExportInfoCard(project = project, videoCount = videoCount, resolution = resolution, fps = fps)

                    ExportSection("Resolution") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("720p", "1080p", "4K").forEach { r ->
                                ExportOptionChip(r, resolution == r, ACCENT) { resolution = r }
                            }
                        }
                    }

                    ExportSection("Frame Rate") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(24, 30, 60).forEach { f ->
                                ExportOptionChip("${f}fps", fps == f, ACCENT) { fps = f }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick  = {
                            onExport(
                                ExportConfig(
                                    clips        = clips,
                                    resolution   = resolution,
                                    fps          = fps,
                                    projectTitle = project.title,
                                    aspectRatio  = project.aspectRatio
                                )
                            )
                        },
                        enabled  = videoCount > 0,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = ACCENT,
                            disabledContainerColor = SURFACE
                        )
                    ) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = if (videoCount == 0) "No video clips to export" else "Export to Gallery",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is ExportUiState.InProgress -> {
                    Spacer(Modifier.weight(1f))
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress    = { exportState.pct / 100f },
                                modifier    = Modifier.size(96.dp),
                                color       = ACCENT,
                                trackColor  = SURFACE,
                                strokeWidth = 8.dp
                            )
                            Text(
                                "${exportState.pct}%",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = ON_SURFACE
                            )
                        }
                        Text("Exporting…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ON_SURFACE)
                        Text(
                            "$resolution · ${fps}fps · ${project.aspectRatio}",
                            fontSize = 12.sp,
                            color    = SUBTLE
                        )
                        LinearProgressIndicator(
                            progress   = { exportState.pct / 100f },
                            modifier   = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color      = ACCENT,
                            trackColor = SURFACE
                        )
                        Text(
                            "Please keep the app open while exporting",
                            fontSize  = 11.sp,
                            color     = SUBTLE,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }

                is ExportUiState.Done -> {
                    Spacer(Modifier.weight(1f))
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(GREEN.copy(alpha = 0.15f))
                                .border(2.dp, GREEN.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GREEN, modifier = Modifier.size(40.dp))
                        }
                        Text("Export Complete!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ON_SURFACE)
                        Text(
                            exportState.path.substringAfterLast('/'),
                            fontSize  = 11.sp,
                            color     = SUBTLE,
                            textAlign = TextAlign.Center
                        )
                        Text("Saved to Gallery", fontSize = 12.sp, color = GREEN)
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick  = { onReset(); onBack() },
                            modifier = Modifier.weight(1f),
                            border   = BorderStroke(1.dp, DIVIDER),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = ON_SURFACE)
                        ) { Text("Back to Editor") }
                        Button(
                            onClick  = onReset,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = ACCENT)
                        ) { Text("Export Again", fontWeight = FontWeight.SemiBold) }
                    }
                }

                is ExportUiState.Failed -> {
                    Spacer(Modifier.weight(1f))
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(ERR.copy(alpha = 0.15f))
                                .border(2.dp, ERR.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ERR, modifier = Modifier.size(40.dp))
                        }
                        Text("Export Failed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ON_SURFACE)
                        Text(
                            exportState.msg.take(160),
                            fontSize  = 11.sp,
                            color     = SUBTLE,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick  = { onReset(); onBack() },
                            modifier = Modifier.weight(1f),
                            border   = BorderStroke(1.dp, DIVIDER),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = ON_SURFACE)
                        ) { Text("Back to Editor") }
                        Button(
                            onClick  = onReset,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = ERR)
                        ) { Text("Try Again", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportInfoCard(project: Project, videoCount: Int, resolution: String, fps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SURFACE)
            .border(1.dp, DIVIDER, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(project.thumbnailColor)
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(project.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ON_SURFACE)
            Text(
                "$videoCount video clip${if (videoCount != 1) "s" else ""}  ·  ${project.aspectRatio}",
                fontSize = 11.sp,
                color    = SUBTLE
            )
            Text("$resolution @ ${fps}fps", fontSize = 11.sp, color = ACCENT, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ExportSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SUBTLE)
        content()
    }
}

@Composable
private fun ExportOptionChip(label: String, active: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) accent else SURFACE)
            .border(1.dp, if (active) accent else DIVIDER, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            fontSize   = 13.sp,
            color      = if (active) Color.White else SUBTLE,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun InfoCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = SUBTLE)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ON_SURFACE)
    }
}

private fun fmtMs(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d.%02d".format(s / 60, s % 60, (ms % 1000) / 10)
}
