package io.livekit.android.example.voiceassistant.realtime

data class LatencyPayload(
    val opId: String,
    val durationsMs: Map<String, Int>
)

class LatencyTracker(val opId: String) {
    private val marks = linkedMapOf<String, Long>()

    init {
        require(identifierPattern.matches(opId)) { "latency operation identifier is invalid" }
    }

    fun mark(name: String, timestampNanos: Long = System.nanoTime()) {
        require(markNamePattern.matches(name)) { "latency mark name is invalid" }
        marks.putIfAbsent(name, timestampNanos)
    }

    fun durationMs(start: String, end: String): Int? {
        val startNanos = marks[start] ?: return null
        val endNanos = marks[end] ?: return null
        return millisecondsBetween(startNanos, endNanos)
    }

    fun payload(): LatencyPayload {
        val durations = linkedMapOf<String, Int>()
        marks.entries.zipWithNext().forEach { (start, end) ->
            durations["${start.key}_to_${end.key}"] =
                millisecondsBetween(start.value, end.value)
        }
        return LatencyPayload(opId, durations)
    }

    private fun millisecondsBetween(startNanos: Long, endNanos: Long): Int =
        ((endNanos - startNanos) / NANOS_PER_MILLISECOND)
            .coerceIn(0, Int.MAX_VALUE.toLong())
            .toInt()

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
        val identifierPattern = Regex("[A-Za-z0-9_.:-]{1,128}")
        val markNamePattern = Regex("[A-Za-z0-9_.:-]{1,96}")
    }
}
