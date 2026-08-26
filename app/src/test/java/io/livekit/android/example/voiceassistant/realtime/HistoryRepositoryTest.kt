package io.livekit.android.example.voiceassistant.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryRepositoryTest {
    private class MemoryStorage : HistoryStorage {
        val values = mutableMapOf<String, String>()

        override fun read(key: String): String? = values[key]

        override fun write(key: String, value: String) {
            values[key] = value
        }
    }

    @Test
    fun historyRedactsCredentialsAndCapsFinalMessages() {
        val storage = MemoryStorage()
        val repository = HistoryRepository(storage)
        val messages = (1..210).map {
            finalText("m$it", "Bearer secret-token-$it")
        }

        repository.save("conv-1", messages)
        val restored = repository.load("conv-1")

        assertEquals(200, restored.size)
        assertEquals("m11", restored.first().id)
        assertTrue(restored.all { "secret-token" !in it.text })
        assertFalse(storage.values.values.single().contains("secret-token"))
    }

    @Test
    fun interimApprovalPendingAndFailedMessagesAreNotPersisted() {
        val repository = HistoryRepository(MemoryStorage())
        val messages = listOf(
            interimVoice(),
            approvalStatus(),
            finalText("pending", "pending", DeliveryState.PENDING),
            finalText("failed", "failed", DeliveryState.FAILED),
            finalText("final", "safe")
        )

        repository.save("conv-1", messages)

        assertEquals(listOf("final"), repository.load("conv-1").map { it.id })
    }

    @Test
    fun redactorCoversAssignmentsJwtHexAndBase64Credentials() {
        val text = """
            api_key=top-secret-value
            eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.signature-value
            0123456789abcdef0123456789abcdef0123456789abcdef
            QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo4ODg4ODg4ODg=
        """.trimIndent()

        val redacted = redactForHistory(text)

        assertTrue("[REDACTED]" in redacted)
        assertFalse("top-secret-value" in redacted)
        assertFalse("eyJhbGci" in redacted)
        assertFalse("0123456789abcdef" in redacted)
        assertFalse("QUJDREV" in redacted)
    }

    private fun finalText(
        id: String,
        text: String,
        delivery: DeliveryState = DeliveryState.SENT
    ) = TimelineMessage(
        id = id,
        text = text,
        role = MessageRole.USER,
        source = MessageSource.TEXT,
        timestampMs = id.filter(Char::isDigit).toLongOrNull() ?: 1,
        isFinal = true,
        delivery = delivery
    )

    private fun interimVoice() = TimelineMessage(
        id = "interim",
        text = "partial",
        role = MessageRole.USER,
        source = MessageSource.VOICE,
        timestampMs = 1,
        isFinal = false,
        delivery = DeliveryState.SENT
    )

    private fun approvalStatus() = TimelineMessage(
        id = "approval",
        text = "approval",
        role = MessageRole.SYSTEM,
        source = MessageSource.STATUS,
        timestampMs = 2,
        isFinal = true,
        delivery = DeliveryState.SENT,
        statusType = "approval.request"
    )
}
