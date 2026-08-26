package io.livekit.android.example.voiceassistant.realtime

enum class MessageRole {
    USER,
    HERMES,
    SYSTEM
}

enum class MessageSource {
    TEXT,
    VOICE,
    HERMES,
    STATUS
}

enum class DeliveryState {
    PENDING,
    SENT,
    FAILED
}

data class TimelineMessage(
    val id: String,
    val text: String,
    val role: MessageRole,
    val source: MessageSource,
    val timestampMs: Long,
    val isFinal: Boolean,
    val delivery: DeliveryState,
    val transportId: String? = null,
    val statusType: String? = null
) {
    val persistable: Boolean
        get() = isFinal &&
            delivery == DeliveryState.SENT &&
            role in setOf(MessageRole.USER, MessageRole.HERMES) &&
            source != MessageSource.STATUS
}

sealed interface TimelineUpdate {
    data class LocalText(
        val localId: String,
        val text: String,
        val timestampMs: Long
    ) : TimelineUpdate

    data class TextSent(
        val localId: String,
        val transportId: String,
        val timestampMs: Long
    ) : TimelineUpdate

    data class TextFailed(val localId: String, val timestampMs: Long) : TimelineUpdate

    data class RemoteText(
        val transportId: String,
        val text: String,
        val isFinal: Boolean,
        val timestampMs: Long,
        val isUser: Boolean = true,
        val localId: String? = null
    ) : TimelineUpdate

    data class Transcript(
        val segmentId: String,
        val text: String,
        val isFinal: Boolean,
        val isUser: Boolean,
        val timestampMs: Long
    ) : TimelineUpdate

    data class Status(
        val eventId: String,
        val text: String,
        val statusType: String,
        val timestampMs: Long
    ) : TimelineUpdate
}

fun reduceTimeline(
    current: List<TimelineMessage>,
    update: TimelineUpdate
): List<TimelineMessage> = when (update) {
    is TimelineUpdate.LocalText -> current.upsert(
        TimelineMessage(
            id = localMessageId(update.localId),
            text = update.text,
            role = MessageRole.USER,
            source = MessageSource.TEXT,
            timestampMs = update.timestampMs,
            isFinal = true,
            delivery = DeliveryState.PENDING
        )
    )
    is TimelineUpdate.TextSent -> current.updateFirst(
        predicate = { it.id == localMessageId(update.localId) },
        transform = {
            it.copy(
                delivery = DeliveryState.SENT,
                transportId = update.transportId
            )
        }
    )
    is TimelineUpdate.TextFailed -> current.updateFirst(
        predicate = { it.id == localMessageId(update.localId) },
        transform = { it.copy(delivery = DeliveryState.FAILED) }
    )
    is TimelineUpdate.RemoteText -> {
        val existing = current.indexOfFirst {
            it.transportId == update.transportId ||
                (update.localId != null && it.id == localMessageId(update.localId))
        }
        if (existing >= 0) {
            current.replaceAt(
                existing,
                current[existing].copy(
                    text = update.text,
                    isFinal = update.isFinal,
                    delivery = DeliveryState.SENT,
                    transportId = update.transportId
                )
            )
        } else {
            current.upsert(
                TimelineMessage(
                    id = transportMessageId(update.transportId),
                    text = update.text,
                    role = if (update.isUser) MessageRole.USER else MessageRole.HERMES,
                    source = if (update.isUser) {
                        MessageSource.TEXT
                    } else {
                        MessageSource.HERMES
                    },
                    timestampMs = update.timestampMs,
                    isFinal = update.isFinal,
                    delivery = DeliveryState.SENT,
                    transportId = update.transportId
                )
            )
        }
    }
    is TimelineUpdate.Transcript -> current.upsert(
        TimelineMessage(
            id = transcriptMessageId(update.segmentId),
            text = update.text,
            role = if (update.isUser) MessageRole.USER else MessageRole.HERMES,
            source = if (update.isUser) MessageSource.VOICE else MessageSource.HERMES,
            timestampMs = update.timestampMs,
            isFinal = update.isFinal,
            delivery = DeliveryState.SENT,
            transportId = update.segmentId
        )
    )
    is TimelineUpdate.Status -> current.upsert(
        TimelineMessage(
            id = statusMessageId(update.eventId),
            text = update.text,
            role = MessageRole.SYSTEM,
            source = MessageSource.STATUS,
            timestampMs = update.timestampMs,
            isFinal = true,
            delivery = DeliveryState.SENT,
            statusType = update.statusType
        )
    )
}

private fun localMessageId(localId: String) = "local:$localId"

private fun transportMessageId(transportId: String) = "transport:$transportId"

private fun transcriptMessageId(segmentId: String) = "transcript:$segmentId"

private fun statusMessageId(eventId: String) = "status:$eventId"

private fun List<TimelineMessage>.upsert(message: TimelineMessage): List<TimelineMessage> {
    val existing = indexOfFirst { it.id == message.id }
    if (existing >= 0) {
        return replaceAt(
            existing,
            message.copy(timestampMs = this[existing].timestampMs)
        )
    }

    val insertion = indexOfFirst { it.timestampMs > message.timestampMs }
    return toMutableList().apply {
        if (insertion < 0) add(message) else add(insertion, message)
    }
}

private inline fun List<TimelineMessage>.updateFirst(
    predicate: (TimelineMessage) -> Boolean,
    transform: (TimelineMessage) -> TimelineMessage
): List<TimelineMessage> {
    val index = indexOfFirst(predicate)
    if (index < 0) return this
    return replaceAt(index, transform(this[index]))
}

private fun List<TimelineMessage>.replaceAt(
    index: Int,
    message: TimelineMessage
): List<TimelineMessage> = toMutableList().apply { this[index] = message }
