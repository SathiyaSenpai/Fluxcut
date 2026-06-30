package com.android.fluxcut

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi

private enum class ClipTool(
    val label:     String,
    val icon:      ImageVector,
    val needsClip: Boolean = true
) {
    SPLIT  ("Split",  Icons.Outlined.ContentCut,  needsClip = true),
    TRIM   ("Trim",   Icons.Outlined.Straighten,  needsClip = true),
    SPEED  ("Speed",  Icons.Outlined.Speed,       needsClip = true),
    CROP   ("Crop",   Icons.Outlined.CropFree,    needsClip = true),
    AUDIO  ("Audio",  Icons.AutoMirrored.Outlined.VolumeUp,    needsClip = false),
    TEXT   ("Text",   Icons.Outlined.TextFields,  needsClip = false),
    FILTER ("Filter", Icons.Outlined.AutoFixHigh, needsClip = false),
    DELETE ("Delete", Icons.Outlined.Delete,      needsClip = true),
}

@Composable
private fun colorForTool(tool: ClipTool): Color {
    val cs = MaterialTheme.colorScheme
    return when (tool) {
        ClipTool.SPLIT  -> cs.primary
        ClipTool.TRIM   -> cs.primary
        ClipTool.SPEED  -> cs.secondary
        ClipTool.CROP   -> cs.tertiary
        ClipTool.AUDIO  -> cs.primaryContainer
        ClipTool.TEXT   -> cs.secondary
        ClipTool.FILTER -> cs.errorContainer
        ClipTool.DELETE -> cs.error
    }
}

private sealed class ExportUiState {
    object Idle                         : ExportUiState()
    data class InProgress(val pct: Int) : ExportUiState()
    data class Done(val path: String)   : ExportUiState()
    data class Failed(val msg: String)  : ExportUiState()
}

@UnstableApi
@Composable
fun EditorScreen(args: EditorArgs, onBack: () -> Unit) {
    val project = args.project
    val context = LocalContext.current
    val repo    = remember { ProjectRepository(context) }
    val vm: EditorViewModel = viewModel(
        key     = "editor_${project.id}",
        factory = EditorViewModel.Factory(project, repo, context)
    )

    val state        by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    var activeTool  by remember { mutableStateOf<ClipTool?>(null) }
    var showExport  by remember { mutableStateOf(false) }
    var exportState by remember { mutableStateOf<ExportUiState>(ExportUiState.Idle) }
    var autoFired   by remember { mutableStateOf(false) }

    val videoColor = MaterialTheme.colorScheme.primary
    val audioColor = MaterialTheme.colorScheme.tertiary
    val importLauncher = rememberMediaImportLauncher { imported ->
        val nextId  = (state.clips.maxOfOrNull { it.id } ?: 0) + 1
        val startMs = state.clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
        val clip    = imported.toTimelineClip(
            id         = nextId,
            startMs    = startMs,
            videoColor = videoColor,
            audioColor = audioColor
        )
        vm.addClip(clip)
        vm.selectClip(clip.id)
    }

    LaunchedEffect(state.clips.isEmpty(), autoFired) {
        if (args.autoOpenPicker && !autoFired && state.clips.isEmpty()) { 
            autoFired = true; importLauncher() 
        }
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
        topBar = {
            EditorTopBar(
                title     = state.project?.title ?: project.title,
                onBack    = onBack,
                onAddClip = { importLauncher() },
                onExport  = { showExport = true }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost   = { SnackbarHost(snackbarHost) }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {

            // Project Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Aspect Ratio Container
                val projectRatio = remember(project.aspectRatio) {
                    val parts = project.aspectRatio.split(":")
                    if (parts.size == 2) {
                        val w = parts[0].toFloatOrNull() ?: 16f
                        val h = parts[1].toFloatOrNull() ?: 9f
                        w / h
                    } else 16f / 9f
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .aspectRatio(projectRatio, matchHeightConstraintsFirst = true)
                        .background(Color(0xFF151520))
                ) {
                    VideoPlayerView(
                        player = vm.player,
                        modifier = Modifier.fillMaxSize()
                    )
                }

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
                            onDismiss = { activeTool = null }
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
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Text("Video Editor", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        actions = {
            IconButton(onClick = onAddClip) {
                Icon(Icons.Outlined.Add, contentDescription = "Add clip", tint = MaterialTheme.colorScheme.primary)
            }
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
            Text("No media yet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Button(
                onClick = onPick,
                colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        if (totalMs > 0L) {
            Slider(
                value         = playheadMs.toFloat(),
                valueRange    = 0f..totalMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                modifier      = Modifier.fillMaxWidth().height(26.dp),
                colors        = SliderDefaults.colors(
                    thumbColor         = MaterialTheme.colorScheme.primary,
                    activeTrackColor   = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
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
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .background(MaterialTheme.colorScheme.primary)
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
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(17.dp))
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding        = PaddingValues(horizontal = 2.dp)
    ) {
        items(ClipTool.entries) { tool ->
            val enabled   = !tool.needsClip || selectedClipId != null
            val isActive  = activeTool == tool
            val toolColor = colorForTool(tool)
            val chipColor = if (enabled) toolColor else MaterialTheme.colorScheme.onSurfaceVariant

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) chipColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
                    .border(
                        width = if (isActive) 1.5.dp else 1.dp,
                        color = if (isActive) chipColor else MaterialTheme.colorScheme.outline,
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

@Composable
private fun ToolPanel(
    tool:      ClipTool,
    state:     EditorUiState,
    vm:        EditorViewModel,
    onDismiss: () -> Unit
) {
    Surface(
        modifier       = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        shape          = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color          = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val toolColor = colorForTool(tool)
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
                            .background(toolColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, contentDescription = null, tint = toolColor, modifier = Modifier.size(14.dp))
                    }
                    Text(tool.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
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
        Text("Select a clip on the timeline first", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) { Text("Cancel") }
            Button(
                onClick  = { vm.splitClipAtPlayhead(clipId); onDismiss() },
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
        Text("Select a clip to trim", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = { vm.cancelTrim(); onDismiss() },
                modifier = Modifier.weight(1f),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Cancel")
            }
            Button(
                onClick  = { vm.commitTrim(); onDismiss() },
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                        .background(if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { speed = p }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "${p}x",
                        fontSize   = 13.sp,
                        color      = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = "${"%.2f".format(speed)}x",
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(42.dp)
            )
            Slider(
                value         = speed,
                valueRange    = 0.25f..4f,
                onValueChange = { speed = it },
                modifier      = Modifier.weight(1f),
                colors        = SliderDefaults.colors(
                    thumbColor         = MaterialTheme.colorScheme.secondary,
                    activeTrackColor   = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) { Text("Cancel") }
            Button(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
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
                        .background(if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { selected = r }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        r,
                        fontSize   = 12.sp,
                        color      = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Text("Crop & aspect ratio applied on export via FFmpeg.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) { Text("Cancel") }
            Button(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
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
        AudioFader("Master",      masterVol, MaterialTheme.colorScheme.primary) { masterVol = it }
        AudioFader("Music",       musicVol,  MaterialTheme.colorScheme.tertiary)  { musicVol  = it }
        AudioFader("SFX / Voice", sfxVol,   MaterialTheme.colorScheme.secondary)  { sfxVol    = it }
        Button(
            onClick  = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) { Text("Done", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun AudioFader(label: String, value: Float, accent: Color, onChange: (Float) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(70.dp))
        Slider(
            value         = value,
            onValueChange = onChange,
            modifier      = Modifier.weight(1f),
            colors        = SliderDefaults.colors(
                thumbColor         = accent,
                activeTrackColor   = accent,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
        Text(
            text      = "${(value * 100).toInt()}%",
            fontSize  = 10.sp,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier  = Modifier.width(34.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TextPanel(onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val styles   = listOf("Lower Third" to cs.primary, "Bold Title" to cs.secondary, "Subtitle" to cs.tertiary, "Watermark" to cs.onSurfaceVariant)
    var selected by remember { mutableStateOf("Lower Third") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(styles) { (name, color) ->
                val active = selected == name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, if (active) color else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { selected = name }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        name,
                        fontSize   = 12.sp,
                        color      = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Text("Text overlays are added to the TITLE track.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick  = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) { Text("Add \"$selected\"", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun FilterPanel(onDismiss: () -> Unit) {
    val filters = listOf(
        "None"      to MaterialTheme.colorScheme.primary,
        "Cinematic" to MaterialTheme.colorScheme.secondary,
        "B&W"       to Color(0xFF888888),
        "Warm"      to Color(0xFFEF4444),
        "Cool"      to Color(0xFF60A5FA),
        "Vivid"     to MaterialTheme.colorScheme.tertiary
    )
    var selected by remember { mutableStateOf("None") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filters) { (name, color) ->
                val active = selected == name
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, if (active) color else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
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
                        color      = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) { Text("Cancel") }
            Button(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
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
    var resolution by remember { mutableStateOf(project.resolution.ifEmpty { "1080p" }) }
    var fps        by remember { mutableIntStateOf(project.fps.takeIf { it > 0 } ?: 30) }
    val videoCount = clips.count { it.track == TrackType.VIDEO && it.sourceUri != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = onBack,
                enabled  = exportState !is ExportUiState.InProgress
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Export Video", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(project.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

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
                            listOf("720p", "1080p", "2K", "4K").forEach { r ->
                                ExportOptionChip(r, resolution == r, MaterialTheme.colorScheme.primary) { resolution = r }
                            }
                        }
                    }

                    ExportSection("Frame Rate") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(24, 30, 60, 120).forEach { f ->
                                ExportOptionChip("${f}fps", fps == f, MaterialTheme.colorScheme.primary) { fps = f }
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
                            containerColor         = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer
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
                                color       = MaterialTheme.colorScheme.primary,
                                trackColor  = MaterialTheme.colorScheme.surfaceContainer,
                                strokeWidth = 8.dp
                            )
                            Text(
                                "${exportState.pct}%",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text("Exporting…", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "$resolution · ${fps}fps · ${project.aspectRatio}",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress   = { exportState.pct / 100f },
                            modifier   = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color      = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                        Text(
                            "Please keep the app open while exporting",
                            fontSize  = 11.sp,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                .border(2.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(40.dp))
                        }
                        Text("Export Complete!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            exportState.path.substringAfterLast('/'),
                            fontSize  = 11.sp,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text("Saved to Gallery", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick  = { onReset(); onBack() },
                            modifier = Modifier.weight(1f),
                            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) { Text("Back to Editor") }
                        Button(
                            onClick  = onReset,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                .border(2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
                        }
                        Text("Export Failed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            exportState.msg.take(160),
                            fontSize  = 11.sp,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick  = { onReset(); onBack() },
                            modifier = Modifier.weight(1f),
                            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) { Text("Back to Editor") }
                        Button(
                            onClick  = onReset,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
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
            Text(project.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "$videoCount video clip${if (videoCount != 1) "s" else ""}  ·  ${project.aspectRatio}",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("$resolution @ ${fps}fps", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ExportSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun ExportOptionChip(label: String, active: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) accent else MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, if (active) accent else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            fontSize   = 13.sp,
            color      = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun InfoCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun fmtMs(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d.%02d".format(s / 60, s % 60, (ms % 1000) / 10)
}
