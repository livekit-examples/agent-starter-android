package io.livekit.android.example.voiceassistant.realtime

import android.content.SharedPreferences
import java.util.UUID

interface IdentityStorage {
    fun read(key: String): String?

    fun write(key: String, value: String)
}

class SharedPreferencesIdentityStorage(
    private val preferences: SharedPreferences
) : IdentityStorage {
    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}

data class SessionIdentity(
    val installationId: String,
    val conversationId: String
)

class SessionIdentityStore(
    private val storage: IdentityStorage,
    private val generateId: () -> String = { UUID.randomUUID().toString() }
) {
    @Synchronized
    fun current(): SessionIdentity {
        val installation = validStored(INSTALLATION_KEY) ?: newIdentifier().also {
            storage.write(INSTALLATION_KEY, it)
        }
        val conversation = validStored(CONVERSATION_KEY) ?: newIdentifier().also {
            storage.write(CONVERSATION_KEY, it)
        }
        return SessionIdentity(installation, conversation)
    }

    @Synchronized
    fun rotateConversation(): SessionIdentity {
        val before = current()
        var next = newIdentifier()
        if (next == before.conversationId) {
            next = UUID.randomUUID().toString()
        }
        storage.write(CONVERSATION_KEY, next)
        return SessionIdentity(before.installationId, next)
    }

    private fun validStored(key: String): String? = storage.read(key)
        ?.takeIf(identifierPattern::matches)

    private fun newIdentifier(): String {
        val normalized = generateId()
            .replace(invalidCharacterPattern, "-")
            .trim('-')
            .take(128)
        return normalized.takeIf(identifierPattern::matches)
            ?: UUID.randomUUID().toString()
    }

    private companion object {
        const val INSTALLATION_KEY = "installation_id"
        const val CONVERSATION_KEY = "conversation_id"
        val identifierPattern = Regex("[A-Za-z0-9_.:-]{1,128}")
        val invalidCharacterPattern = Regex("[^A-Za-z0-9_.:-]+")
    }
}
