package io.livekit.android.example.voiceassistant.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LatencyTrackerTest {
    @Test
    fun textFirstRenderLatencyUsesMonotonicMarks() {
        val tracker = LatencyTracker("op-1")
        tracker.mark("send_pressed", 1_000_000_000)
        tracker.mark("first_ui_delta", 1_240_000_000)

        assertEquals(240, tracker.durationMs("send_pressed", "first_ui_delta"))
    }

    @Test
    fun firstMarkWinsAndPayloadContainsNoMessageContent() {
        val tracker = LatencyTracker("op-1")
        tracker.mark("send_pressed", 1_000_000_000)
        tracker.mark("send_pressed", 9_000_000_000)
        tracker.mark("packet_sent", 1_010_000_000)

        val payload = tracker.payload()

        assertEquals(10, payload.durationsMs["send_pressed_to_packet_sent"])
        assertTrue("text" !in payload.toString().lowercase())
        assertNull(tracker.durationMs("missing", "packet_sent"))
    }
}
