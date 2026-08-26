package io.livekit.android.example.voiceassistant.realtime

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

const val CONTROL_TOPIC = "hermes.control"
const val STATUS_TOPIC = "hermes.status"
const val REALTIME_PROTOCOL_VERSION = 1
private const val MAX_PACKET_BYTES = 4096

private val identifierPattern = Regex("[A-Za-z0-9_.:-]{1,128}")
private val safeNamePattern = Regex("[A-Za-z0-9_.:-]{1,64}")
private val fingerprintPattern = Regex("[0-9a-f]{12}")
private val durationNamePattern = Regex("[A-Za-z0-9_.:-]{1,96}")
private val controlCommands = setOf("new", "stop", "status")

data class ControlPacket(
    val version: Int = REALTIME_PROTOCOL_VERSION,
    @SerializedName("op_id") val opId: String,
    val command: String,
    @SerializedName("conversation_id") val conversationId: String? = null
)

data class StatusPacket(
    val version: Int,
    val type: String,
    @SerializedName("conversation_fingerprint")
    val conversationFingerprint: String? = null,
    val tool: String? = null,
    val status: String? = null,
    val mention: String? = null,
    val state: String? = null,
    @SerializedName("op_id") val opId: String? = null,
    @SerializedName("duration_ms") val durationMs: Int? = null,
    @SerializedName("durations_ms") val durationsMs: Map<String, Int>? = null,
    val duration: Double? = null,
    @SerializedName("duration_seconds") val durationSeconds: Double? = null,
    val error: Boolean? = null,
    @SerializedName("is_final") val isFinal: Boolean? = null,
    @SerializedName("is_interruption") val isInterruption: Boolean? = null,
    @SerializedName("streamed") val streamed: Boolean? = null,
    @SerializedName("connection_reused") val connectionReused: Boolean? = null,
    @SerializedName("ttfb_ms") val ttfbMs: Int? = null,
    @SerializedName("audio_duration_ms") val audioDurationMs: Int? = null,
    @SerializedName("end_of_utterance_delay_ms") val endOfUtteranceDelayMs: Int? = null,
    @SerializedName("transcription_delay_ms") val transcriptionDelayMs: Int? = null,
    @SerializedName("on_user_turn_completed_delay_ms")
    val onUserTurnCompletedDelayMs: Int? = null,
    @SerializedName("total_duration_ms") val totalDurationMs: Int? = null,
    @SerializedName("prediction_duration_ms") val predictionDurationMs: Int? = null,
    @SerializedName("detection_delay_ms") val detectionDelayMs: Int? = null
)

private val realtimeGson = Gson()

fun controlPacketJson(packet: ControlPacket): String? {
    if (packet.version != REALTIME_PROTOCOL_VERSION) return null
    if (!identifierPattern.matches(packet.opId)) return null
    if (packet.command !in controlCommands) return null
    if (packet.command == "new") {
        if (packet.conversationId == null ||
            !identifierPattern.matches(packet.conversationId)
        ) {
            return null
        }
    } else if (packet.conversationId != null) {
        return null
    }
    return realtimeGson.toJson(packet)
}

private val statusFields = mapOf(
    "session.ready" to setOf("conversation_fingerprint"),
    "tool.started" to setOf("tool"),
    "tool.completed" to setOf("tool", "duration", "error"),
    "subagent.start" to setOf("status"),
    "subagent.complete" to setOf("status", "duration_seconds"),
    "approval.request" to emptySet(),
    "run.completed" to emptySet(),
    "run.failed" to emptySet(),
    "run.cancelled" to emptySet(),
    "delegation" to setOf("mention", "status"),
    "first_hermes_delta" to setOf("duration_ms"),
    "agent.state" to setOf("state"),
    "user.state" to setOf("state"),
    "user.transcription" to setOf("is_final"),
    "speech.overlap" to setOf(
        "is_interruption",
        "detection_delay_ms",
        "prediction_duration_ms"
    ),
    "metrics.eou" to setOf(
        "end_of_utterance_delay_ms",
        "transcription_delay_ms",
        "on_user_turn_completed_delay_ms"
    ),
    "metrics.tts" to setOf("ttfb_ms", "streamed", "connection_reused"),
    "metrics.stt" to setOf(
        "duration_ms",
        "audio_duration_ms",
        "streamed",
        "connection_reused"
    ),
    "metrics.interruption" to setOf(
        "total_duration_ms",
        "prediction_duration_ms",
        "detection_delay_ms"
    ),
    "latency" to setOf("op_id", "durations_ms")
)

private val stringFields = setOf(
    "conversation_fingerprint",
    "tool",
    "status",
    "mention",
    "state",
    "op_id"
)
private val booleanFields = setOf(
    "error",
    "is_final",
    "is_interruption",
    "streamed",
    "connection_reused"
)
private val numberFields = statusFields.values.flatten().toSet() -
    stringFields - booleanFields - setOf("durations_ms")

fun parseStatusPacket(data: ByteArray): StatusPacket? {
    if (data.isEmpty() || data.size > MAX_PACKET_BYTES) return null
    val json = runCatching {
        JsonParser.parseString(data.decodeToString()).asJsonObject
    }.getOrNull() ?: return null

    if (json.keySet().let { "version" !in it || "type" !in it }) return null
    val version = json.intValue("version") ?: return null
    if (version != REALTIME_PROTOCOL_VERSION) return null
    val type = json.stringValue("type") ?: return null
    val allowed = statusFields[type] ?: return null
    if ((json.keySet() - (allowed + setOf("version", "type"))).isNotEmpty()) {
        return null
    }
    if (!validateStatusValues(json, allowed)) return null

    return runCatching {
        realtimeGson.fromJson(json, StatusPacket::class.java)
    }.getOrNull()
}

private fun validateStatusValues(json: JsonObject, fields: Set<String>): Boolean {
    for (field in fields.intersect(json.keySet())) {
        val value = json[field]
        when (field) {
            in stringFields -> {
                val text = json.stringValue(field) ?: return false
                val valid = when (field) {
                    "conversation_fingerprint" -> fingerprintPattern.matches(text)
                    "tool" -> safeNamePattern.matches(text)
                    "mention" -> "@$text" in SUPPORTED_MENTIONS
                    "op_id" -> identifierPattern.matches(text)
                    else -> text.length in 1..128 && text.none(Char::isISOControl)
                }
                if (!valid) return false
            }
            in booleanFields -> if (!value.isJsonPrimitive ||
                !value.asJsonPrimitive.isBoolean
            ) {
                return false
            }
            in numberFields -> if (json.nonNegativeNumber(field) == null) return false
            "durations_ms" -> {
                if (!value.isJsonObject) return false
                for ((name, duration) in value.asJsonObject.entrySet()) {
                    if (!durationNamePattern.matches(name) ||
                        !duration.isJsonPrimitive ||
                        !duration.asJsonPrimitive.isNumber ||
                        runCatching { duration.asInt }.getOrNull()?.let { it >= 0 } != true
                    ) {
                        return false
                    }
                }
            }
        }
    }
    return true
}

private fun JsonObject.stringValue(name: String): String? = runCatching {
    get(name).takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
}.getOrNull()

private fun JsonObject.intValue(name: String): Int? = runCatching {
    get(name).takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt
}.getOrNull()

private fun JsonObject.nonNegativeNumber(name: String): Double? = runCatching {
    get(name).takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.asDouble
        ?.takeIf { it.isFinite() && it >= 0 }
}.getOrNull()
