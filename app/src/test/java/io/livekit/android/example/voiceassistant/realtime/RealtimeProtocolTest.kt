package io.livekit.android.example.voiceassistant.realtime

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeProtocolTest {
    @Test
    fun stopPacketContainsOnlyVersionOperationAndCommand() {
        val json = controlPacketJson(ControlPacket(1, "op-1", "stop", null))!!
        val fields = JsonParser.parseString(json).asJsonObject.keySet()

        assertEquals(setOf("version", "op_id", "command"), fields)
    }

    @Test
    fun newPacketIncludesValidatedConversationIdentifier() {
        val json = controlPacketJson(ControlPacket(1, "op-2", "new", "conv-next"))!!
        val fields = JsonParser.parseString(json).asJsonObject

        assertEquals("conv-next", fields["conversation_id"].asString)
        assertNull(controlPacketJson(ControlPacket(1, "op-2", "new", "../bad")))
    }

    @Test
    fun privilegedAndUnknownCommandsCannotBeEncoded() {
        assertNull(controlPacketJson(ControlPacket(1, "op-1", "approve")))
        assertNull(controlPacketJson(ControlPacket(2, "op-1", "stop")))
        assertNull(controlPacketJson(ControlPacket(1, "../bad", "stop")))
    }

    @Test
    fun statusParserAcceptsVersionedSafeFields() {
        val packet = parseStatusPacket(
            """{"version":1,"type":"tool.started","tool":"computer"}"""
                .encodeToByteArray()
        )

        assertEquals("tool.started", packet?.type)
        assertEquals("computer", packet?.tool)
    }

    @Test
    fun statusParserRejectsUnknownFieldsAndOversizePayload() {
        assertNull(
            parseStatusPacket(
                """{"version":1,"type":"tool.started","tool":"computer","args":"private"}"""
                    .encodeToByteArray()
            )
        )
        assertNull(parseStatusPacket(ByteArray(4097) { 'x'.code.toByte() }))
    }

    @Test
    fun latencyStatusContainsOnlyNumericDurations() {
        val packet = parseStatusPacket(
            """{"version":1,"type":"latency","op_id":"op-1","durations_ms":{"send_to_first":240}}"""
                .encodeToByteArray()
        )

        assertEquals(240, packet?.durationsMs?.get("send_to_first"))
        assertTrue(packet?.durationsMs?.values?.all { it >= 0 } == true)
    }
}
