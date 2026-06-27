package com.android.fluxcut

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class EditorUiState(
    val project:         Project?          = null,
    val clips:           List<TimelineClip> = emptyList(),
    val selectedClipId:  Int?              = null,
    val playheadMs:      Long              = 0L,
    val isPlaying:       Boolean           = false,
    val totalDurationMs: Long              = 0L,
    val isSaving:        Boolean           = false,
    val saveSuccess:     Boolean           = false,
    val errorMessage:    String?           = null,
    val trimState:       TrimState?        = null
)

data class TrimState(
    val clipId:             Int,
    val originalStartMs:    Long,
    val originalDurationMs: Long,
    val originalTrimStartMs:Long,
    val pendingTrimStartMs: Long,
    val pendingDurationMs:  Long
)

class EditorViewModel(
    private val project:    Project,
    private val repository: ProjectRepository,
    context: Context
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _uiState = MutableStateFlow(EditorUiState(project = project))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        attachPlayerListener()
        observeClips()
    }

    private fun observeClips() {
        viewModelScope.launch {
            repository.observeProjectWithClips(project.id)
                .filterNotNull()
                .collect { pwc ->
                    val clips = pwc.clips.map { it.toTimelineClip() }
                    val total = clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L
                    _uiState.update { s ->
                        s.copy(clips = clips, totalDurationMs = total)
                    }
                    rebuildPlayerPlaylist(clips)
                }
        }
    }

    private fun rebuildPlayerPlaylist(clips: List<TimelineClip>) {
        val items = clips
            .filter { it.track == TrackType.VIDEO && it.sourceUri != null }
            .sortedBy { it.startMs }
            .map { clip ->
                MediaItem.Builder()
                    .setUri(Uri.parse(clip.sourceUri))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.trimStartMs)
                            .setEndPositionMs(clip.trimStartMs + clip.durationMs)
                            .build()
                    )
                    .build()
            }

        val wasPlaying = player.isPlaying
        val posMs      = player.currentPosition
        player.setMediaItems(items)
        player.prepare()
        if (wasPlaying) {
            player.seekTo(posMs)
            player.play()
        }
    }

    private fun attachPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startTicker() else stopTicker()
            }
        })
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(playheadMs = player.currentPosition) }
                delay(30.milliseconds)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        _uiState.update { it.copy(playheadMs = player.currentPosition) }
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0))
        _uiState.update { it.copy(playheadMs = positionMs) }
    }

    fun skipToStart() {
        seekTo(0L)
        player.pause()
    }

    fun skipToEnd() {
        seekTo(_uiState.value.totalDurationMs)
        player.pause()
    }

    fun stepBack(ms: Long = 1000L) {
        seekTo((_uiState.value.playheadMs - ms).coerceAtLeast(0L))
    }

    fun stepForward(ms: Long = 1000L) {
        seekTo((_uiState.value.playheadMs + ms).coerceAtMost(_uiState.value.totalDurationMs))
    }

    fun selectClip(clipId: Int?) {
        _uiState.update { it.copy(selectedClipId = clipId, trimState = null) }
    }

    fun addClip(newClip: TimelineClip) {
        val clips = (_uiState.value.clips + newClip).sortedBy { it.startMs }
        _uiState.update { it.copy(clips = clips, totalDurationMs = calcTotal(clips)) }
        persistTimeline(clips)
    }

    fun startTrim(clipId: Int) {
        val clip = _uiState.value.clips.find { it.id == clipId } ?: return
        _uiState.update { s ->
            s.copy(
                selectedClipId = clipId,
                trimState = TrimState(
                    clipId               = clipId,
                    originalStartMs      = clip.startMs,
                    originalDurationMs   = clip.durationMs,
                    originalTrimStartMs  = clip.trimStartMs,
                    pendingTrimStartMs   = clip.trimStartMs,
                    pendingDurationMs    = clip.durationMs
                )
            )
        }
    }

    fun updateTrimHead(deltaMs: Long) {
        val ts            = _uiState.value.trimState ?: return
        val minDurationMs = 100L
        val newTrimStart  = (ts.pendingTrimStartMs + deltaMs)
            .coerceAtLeast(0)
            .coerceAtMost(ts.pendingTrimStartMs + ts.pendingDurationMs - minDurationMs)
        val trimDelta   = newTrimStart - ts.pendingTrimStartMs
        val newDuration = (ts.pendingDurationMs - trimDelta).coerceAtLeast(minDurationMs)
        _uiState.update { s ->
            s.copy(trimState = ts.copy(pendingTrimStartMs = newTrimStart, pendingDurationMs = newDuration))
        }
    }

    fun updateTrimTail(deltaMs: Long) {
        val ts = _uiState.value.trimState ?: return
        val newDuration = (ts.pendingDurationMs + deltaMs).coerceAtLeast(100L)
        _uiState.update { s -> s.copy(trimState = ts.copy(pendingDurationMs = newDuration)) }
    }

    fun commitTrim() {
        val ts = _uiState.value.trimState ?: return
        applyTrimToClip(ts.clipId, ts.pendingTrimStartMs, ts.pendingDurationMs)
        _uiState.update { it.copy(trimState = null) }
    }

    fun cancelTrim() {
        _uiState.update { it.copy(trimState = null) }
    }

    private fun applyTrimToClip(clipId: Int, newTrimStartMs: Long, newDurationMs: Long) {
        val clips = _uiState.value.clips.toMutableList()
        val idx   = clips.indexOfFirst { it.id == clipId }
        if (idx < 0) return
        val old        = clips[idx]
        val headDelta  = newTrimStartMs - old.trimStartMs
        clips[idx]     = old.copy(
            trimStartMs = newTrimStartMs,
            durationMs  = newDurationMs,
            startMs     = old.startMs + headDelta
        )
        repackClips(clips, fromIndex = idx + 1)
        _uiState.update { it.copy(clips = clips, totalDurationMs = calcTotal(clips)) }
        persistTimeline(clips)
    }

    fun splitClipAtPlayhead(clipId: Int) = splitClipAt(clipId, _uiState.value.playheadMs)

    fun splitClipAt(clipId: Int, splitAtMs: Long) {
        val clips    = _uiState.value.clips.toMutableList()
        val idx      = clips.indexOfFirst { it.id == clipId }
        if (idx < 0) return
        val clip     = clips[idx]
        val clipEndMs = clip.startMs + clip.durationMs
        if (splitAtMs <= clip.startMs || splitAtMs >= clipEndMs) {
            _uiState.update { it.copy(errorMessage = "Split point is outside the selected clip.") }
            return
        }
        val leftDurationMs  = splitAtMs - clip.startMs
        val rightDurationMs = clip.durationMs - leftDurationMs
        clips[idx] = clip.copy(durationMs = leftDurationMs)
        val rightClip = clip.copy(
            id          = generateClipId(),
            startMs     = splitAtMs,
            durationMs  = rightDurationMs,
            trimStartMs = clip.trimStartMs + leftDurationMs
        )
        clips.add(idx + 1, rightClip)
        _uiState.update { it.copy(clips = clips, totalDurationMs = calcTotal(clips)) }
        persistTimeline(clips)
    }

    fun deleteClip(clipId: Int) {
        val clips = _uiState.value.clips.toMutableList()
        val idx   = clips.indexOfFirst { it.id == clipId }
        if (idx < 0) return
        clips.removeAt(idx)
        repackClips(clips, fromIndex = idx)
        _uiState.update { s ->
            s.copy(clips = clips, selectedClipId = null, totalDurationMs = calcTotal(clips))
        }
        persistTimeline(clips)
    }

    private fun persistTimeline(clips: List<TimelineClip>) {
        viewModelScope.launch {
            try {
                repository.saveTimeline(project, clips)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Auto-save failed: ${e.message}") }
            }
        }
    }

    fun saveNow() {
        val clips = _uiState.value.clips
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.saveTimeline(project, clips)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                delay(1500.milliseconds)
                _uiState.update { it.copy(saveSuccess = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Save failed: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun repackClips(clips: MutableList<TimelineClip>, fromIndex: Int) {
        for (i in fromIndex until clips.size) {
            val prev = clips.getOrNull(i - 1) ?: continue
            if (clips[i].track == prev.track) {
                clips[i] = clips[i].copy(startMs = prev.startMs + prev.durationMs)
            }
        }
    }

    private fun calcTotal(clips: List<TimelineClip>): Long =
        clips.maxOfOrNull { it.startMs + it.durationMs } ?: 0L

    private fun generateClipId(): Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        player.release()
    }

    class Factory(
        private val project:    Project,
        private val repository: ProjectRepository,
        private val context:    Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditorViewModel(project, repository, context) as T
    }
}
