package io.livekit.android.example.voiceassistant.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {
    @Test
    fun optimisticTextReconcilesWithoutDuplicate() {
        val optimistic = reduceTimeline(
            emptyList(),
            TimelineUpdate.LocalText("local-1", "hello", 10)
        )
        val sent = reduceTimeline(
            optimistic,
            TimelineUpdate.TextSent("local-1", "stream-7", 12)
        )

        assertEquals(1, sent.size)
        assertEquals(DeliveryState.SENT, sent.single().delivery)
        assertEquals("stream-7", sent.single().transportId)
        assertTrue(sent.single().persistable)
    }

    @Test
    fun finalVoiceTranscriptReplacesInterimSegment() {
        val interim = reduceTimeline(
            emptyList(),
            TimelineUpdate.Transcript("seg-1", "হারমিস", false, true, 20)
        )
        val final = reduceTimeline(
            interim,
            TimelineUpdate.Transcript("seg-1", "হারমিস শুনো", true, true, 25)
        )

        assertEquals(1, final.size)
        assertEquals("হারমিস শুনো", final.single().text)
        assertTrue(final.single().isFinal)
        assertTrue(final.single().persistable)
    }

    @Test
    fun voiceAndTextShareTimestampOrderedTimeline() {
        val updates = listOf(
            TimelineUpdate.Transcript("v1", "voice", true, true, 30),
            TimelineUpdate.LocalText("t1", "text", 40),
            TimelineUpdate.Transcript("a1", "reply", true, false, 50)
        )

        val result = updates.fold(emptyList<TimelineMessage>(), ::reduceTimeline)

        assertEquals(
            listOf(MessageSource.VOICE, MessageSource.TEXT, MessageSource.HERMES),
            result.map { it.source }
        )
    }

    @Test
    fun streamingHermesTextUpdatesOneBubbleInPlace() {
        val first = reduceTimeline(
            emptyList(),
            TimelineUpdate.Transcript("agent-1", "আমি ", false, false, 50)
        )
        val completed = reduceTimeline(
            first,
            TimelineUpdate.Transcript("agent-1", "আমি দেখছি", true, false, 55)
        )

        assertEquals(1, completed.size)
        assertEquals("আমি দেখছি", completed.single().text)
        assertEquals(MessageRole.HERMES, completed.single().role)
        assertEquals(50, completed.single().timestampMs)
    }

    @Test
    fun statusInterimPendingAndFailedMessagesAreNotPersistable() {
        var timeline = reduceTimeline(
            emptyList(),
            TimelineUpdate.LocalText("pending", "pending", 10)
        )
        timeline = reduceTimeline(
            timeline,
            TimelineUpdate.Transcript("interim", "partial", false, true, 20)
        )
        timeline = reduceTimeline(
            timeline,
            TimelineUpdate.Status("op-1", "Working", "tool.started", 30)
        )
        timeline = reduceTimeline(
            timeline,
            TimelineUpdate.LocalText("failed", "failed", 40)
        )
        timeline = reduceTimeline(timeline, TimelineUpdate.TextFailed("failed", 45))

        assertFalse(timeline.any { it.persistable })
    }

    @Test
    fun transportEchoReconcilesWithOptimisticMessage() {
        var timeline = reduceTimeline(
            emptyList(),
            TimelineUpdate.LocalText("local-1", "hello", 10)
        )
        timeline = reduceTimeline(
            timeline,
            TimelineUpdate.TextSent("local-1", "stream-7", 12)
        )
        timeline = reduceTimeline(
            timeline,
            TimelineUpdate.RemoteText("stream-7", "hello", true, 15)
        )

        assertEquals(1, timeline.size)
        assertEquals(DeliveryState.SENT, timeline.single().delivery)
    }

    @Test
    fun transportEchoCanArriveBeforeSendCompletionWithoutDuplicate() {
        var timeline = reduceTimeline(
            emptyList(),
            TimelineUpdate.LocalText("local-1", "hello", 10)
        )
        timeline = reduceTimeline(
            timeline,
            TimelineUpdate.RemoteText(
                transportId = "stream-7",
                text = "hello",
                isFinal = true,
                timestampMs = 11,
                localId = "local-1"
            )
        )
        timeline = reduceTimeline(
            timeline,
            TimelineUpdate.TextSent("local-1", "stream-7", 12)
        )

        assertEquals(1, timeline.size)
        assertEquals("stream-7", timeline.single().transportId)
    }
}
