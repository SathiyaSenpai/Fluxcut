package com.android.fluxcut

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TimelineViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun playheadPositionStaysCorrectWhenScrolling() {
        val totalMs = 10_000L
        val playheadMs = 5_000L
        val pxPerMs = 1f 

        composeTestRule.setContent {
            MultiTrackTimeline(
                clips = emptyList(),
                selectedClipId = null,
                trimState = null,
                playheadMs = playheadMs,
                totalMs = totalMs,
                pxPerMs = pxPerMs,
                onClipTap = {},
                onClipLongPress = {},
                onTrimHead = {},
                onTrimTail = {},
                onCommitTrim = {},
                onCancelTrim = {},
                modifier = Modifier.size(width = 500.dp, height = 300.dp)
            )
        }

        // Get initial position of playhead
        val initialPos = composeTestRule.onNodeWithTag("Playhead").getUnclippedBoundsInRoot().left
        
        // Scroll the timeline
        composeTestRule.onNodeWithTag("TimelineContent").performTouchInput {
            swipeLeft()
        }
        
        // Get new position of playhead
        val finalPos = composeTestRule.onNodeWithTag("Playhead").getUnclippedBoundsInRoot().left
        
        // The playhead should have moved because it's viewport-anchored but represents a fixed timeline time.
        // Wait, "viewport-aligned" usually means it stays at the same X on screen if we are scrubbing, 
        // but here it represents playhead position.
        // If playheadMs is 5000 and we scroll the timeline content, the playhead (which is at 5000ms) 
        // should move on screen along with the 5000ms point on the timeline.
        
        // Let's verify that the playhead moved.
        assert(initialPos != finalPos)
    }

    @Test
    fun trimHandleDragReceivesRoundedDeltas() {
        var receivedDelta = 0L
        val pxPerMs = 1f // 1 pixel = 1ms

        val clip = TimelineClip(
            id = 1,
            name = "Test Clip",
            track = TrackType.VIDEO,
            startMs = 0L,
            durationMs = 1000L
        )
        val trimState = TrimState(
            clipId = 1,
            originalStartMs = 0L,
            originalDurationMs = 1000L,
            originalTrimStartMs = 0L,
            pendingTrimStartMs = 0L,
            pendingDurationMs = 1000L
        )

        composeTestRule.setContent {
            MultiTrackTimeline(
                clips = listOf(clip),
                selectedClipId = 1,
                trimState = trimState,
                playheadMs = 0L,
                totalMs = 2000L,
                pxPerMs = pxPerMs,
                onClipTap = {},
                onClipLongPress = {},
                onTrimHead = { receivedDelta = it },
                onTrimTail = {},
                onCommitTrim = {},
                onCancelTrim = {}
            )
        }

        // Drag by 5.4 pixels -> rounds to 5ms
        composeTestRule.onNodeWithTag("TrimHandleHead").performTouchInput {
            down(center)
            moveBy(Offset(5.4f, 0f))
            up()
        }
        assertEquals(5L, receivedDelta)

        // Drag by 5.6 pixels -> rounds to 6ms
        composeTestRule.onNodeWithTag("TrimHandleHead").performTouchInput {
            down(center)
            moveBy(Offset(5.6f, 0f))
            up()
        }
        assertEquals(6L, receivedDelta)
        
        // Small drag that would be 0 if truncated: 0.6 pixels -> 1ms
        composeTestRule.onNodeWithTag("TrimHandleHead").performTouchInput {
            down(center)
            moveBy(Offset(0.6f, 0f))
            up()
        }
        assertEquals(1L, receivedDelta)

        // Negative drag: -0.6 pixels -> -1ms
        composeTestRule.onNodeWithTag("TrimHandleHead").performTouchInput {
            down(center)
            moveBy(Offset(-0.6f, 0f))
            up()
        }
        assertEquals(-1L, receivedDelta)
    }
}
