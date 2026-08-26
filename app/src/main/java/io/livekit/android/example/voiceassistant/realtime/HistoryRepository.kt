package io.livekit.android.example.voiceassistant.realtime

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

interface HistoryStorage {
    fun read(key: String): String?

    fun write(key: String, value: String)
}

class SharedPreferencesHistoryStorage(
    private val preferences: SharedPreferences
) : HistoryStorage {
    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}

private data class HistoryItem(
    val id: String,
    val text: String,
    val role: MessageRole,
    val source: MessageSource,
    val timestampMs: Long
)

class HistoryRepository(
    private val storage: HistoryStorage,
    private val gson: Gson = Gson()
) {
    fun save(conversationId: String, messages: List<TimelineMessage>) {
        val items = messages
            .asSequence()
            .filter(TimelineMessage::persistable)
            .takeLast(MAX_HISTORY_MESSAGES)
            .map {
                HistoryItem(
                    id = it.id,
                    text = redactForHistory(it.text),
                    role = it.role,
                    source = it.source,
                    timestampMs = it.timestampMs
                )
            }
            .toList()
        storage.write(historyKey(conversationId), gson.toJson(items))
    }

    fun load(conversationId: String): List<TimelineMessage> {
        val value = storage.read(historyKey(conversationId)) ?: return emptyList()
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        val items: List<HistoryItem> = runCatching {
            gson.fromJson<List<HistoryItem>>(value, type)
        }.getOrNull() ?: return emptyList()

        return items
            .takeLast(MAX_HISTORY_MESSAGES)
            .filter { it.role in setOf(MessageRole.USER, MessageRole.HERMES) }
            .filter { it.source != MessageSource.STATUS }
            .map {
                TimelineMessage(
                    id = it.id,
                    text = redactForHistory(it.text),
                    role = it.role,
                    source = it.source,
                    timestampMs = it.timestampMs,
                    isFinal = true,
                    delivery = DeliveryState.SENT
                )
            }
    }

    private fun historyKey(conversationId: String): String {
        require(identifierPattern.matches(conversationId)) {
            "conversation identifier is invalid"
        }
        return "history:$conversationId"
    }

    private companion object {
        const val MAX_HISTORY_MESSAGES = 200
        val identifierPattern = Regex("[A-Za-z0-9_.:-]{1,128}")
    }
}

private val bearerPattern = Regex(
    """(?i)\bBearer\s+[A-Za-z0-9._~+/=-]{8,}"""
)
private val assignmentPattern = Regex(
    """(?i)\b(api[-_ ]?key|secret|token|password)\b\s*[:=]\s*[^\s,;]+"""
)
private val jwtPattern = Regex(
    """\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b"""
)
private val longHexPattern = Regex("""\b[0-9a-fA-F]{32,}\b""")
private val longBase64Pattern = Regex(
    """\b(?=[A-Za-z0-9+/=_-]{40,}\b)(?=[A-Za-z0-9+/=_-]*[A-Z])(?=[A-Za-z0-9+/=_-]*[a-z])[A-Za-z0-9+/=_-]+"""
)

fun redactForHistory(text: String): String = text
    .replace(bearerPattern, "Bearer [REDACTED]")
    .replace(assignmentPattern) { match ->
        "${match.groupValues[1]}=[REDACTED]"
    }
    .replace(jwtPattern, "[REDACTED]")
    .replace(longHexPattern, "[REDACTED]")
    .replace(longBase64Pattern, "[REDACTED]")

private fun <T> Sequence<T>.takeLast(count: Int): Sequence<T> = toList()
    .takeLast(count)
    .asSequence()
