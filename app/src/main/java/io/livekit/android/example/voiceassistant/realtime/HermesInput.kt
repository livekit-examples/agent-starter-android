package io.livekit.android.example.voiceassistant.realtime

enum class HermesCommand(val wireValue: String) {
    NEW("new"),
    STATUS("status"),
    AGENTS("agents"),
    TASKS("tasks"),
    STOP("stop"),
    VOICE("voice"),
    CALL("call"),
    ENDCALL("endcall"),
    MUTE("mute"),
    UNMUTE("unmute"),
    MEMORY("memory"),
    HELP("help")
}

sealed interface InputIntent {
    data class Local(val command: HermesCommand) : InputIntent

    data class Control(val command: HermesCommand) : InputIntent

    data class Message(val text: String) : InputIntent
}

val SUPPORTED_MENTIONS = listOf(
    "@main",
    "@architect",
    "@researcher",
    "@coder",
    "@browser",
    "@computer-operator",
    "@qa",
    "@reviewer",
    "@security",
    "@ops"
)

val SUPPORTED_SLASH_COMMANDS = listOf(
    "/new",
    "/status",
    "/agents",
    "/tasks",
    "/stop",
    "/voice",
    "/call",
    "/endcall",
    "/mute",
    "/unmute",
    "/memory",
    "/help"
)

private val LOCAL_COMMANDS = mapOf(
    "/mute" to HermesCommand.MUTE,
    "/unmute" to HermesCommand.UNMUTE,
    "/voice" to HermesCommand.VOICE,
    "/call" to HermesCommand.CALL,
    "/endcall" to HermesCommand.ENDCALL,
    "/help" to HermesCommand.HELP
)

private val CONTROL_COMMANDS = mapOf(
    "/new" to HermesCommand.NEW,
    "/stop" to HermesCommand.STOP,
    "/status" to HermesCommand.STATUS
)

fun parseInput(text: String): InputIntent {
    val normalized = text.trim().lowercase()
    LOCAL_COMMANDS[normalized]?.let { return InputIntent.Local(it) }
    CONTROL_COMMANDS[normalized]?.let { return InputIntent.Control(it) }
    return InputIntent.Message(text)
}

fun suggestInputs(text: String): List<String> {
    val prefix = text.trimStart().lowercase()
    if (prefix.isEmpty() || prefix.any(Char::isWhitespace)) return emptyList()
    return when {
        prefix.startsWith("@") -> SUPPORTED_MENTIONS.filter { it.startsWith(prefix) }
        prefix.startsWith("/") -> SUPPORTED_SLASH_COMMANDS.filter {
            it.startsWith(prefix)
        }
        else -> emptyList()
    }
}
