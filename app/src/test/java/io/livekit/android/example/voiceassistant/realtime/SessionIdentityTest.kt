package io.livekit.android.example.voiceassistant.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionIdentityTest {
    private class MemoryStorage : IdentityStorage {
        private val values = mutableMapOf<String, String>()

        override fun read(key: String): String? = values[key]

        override fun write(key: String, value: String) {
            values[key] = value
        }
    }

    @Test
    fun conversationRotationPreservesInstallationIdentity() {
        var next = 0
        val identities = SessionIdentityStore(MemoryStorage()) { "generated-${++next}" }

        val before = identities.current()
        val after = identities.rotateConversation()

        assertEquals(before.installationId, after.installationId)
        assertNotEquals(before.conversationId, after.conversationId)
        assertTrue(Regex("[A-Za-z0-9_.:-]{1,128}").matches(after.conversationId))
    }

    @Test
    fun identitySurvivesStoreRecreationAndInvalidValuesAreReplaced() {
        val storage = MemoryStorage()
        storage.write("installation_id", "../../bad")
        val first = SessionIdentityStore(storage) { "safe-id" }.current()
        val second = SessionIdentityStore(storage) { "unused-id" }.current()

        assertEquals("safe-id", first.installationId)
        assertEquals(first, second)
    }
}
