package io.livekit.android.example.voiceassistant.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HermesInputTest {
    @Test
    fun muteIsLocalAndApproveDoesNotExist() {
        assertEquals(InputIntent.Local(HermesCommand.MUTE), parseInput("/mute"))
        assertEquals(InputIntent.Message("/approve"), parseInput("/approve"))
    }

    @Test
    fun newStopAndStatusUseControlChannel() {
        assertEquals(InputIntent.Control(HermesCommand.NEW), parseInput("/new"))
        assertEquals(InputIntent.Control(HermesCommand.STOP), parseInput("/stop"))
        assertEquals(InputIntent.Control(HermesCommand.STATUS), parseInput("/status"))
    }

    @Test
    fun agentsTasksAndMemoryRemainHermesMessages() {
        assertEquals(InputIntent.Message("/agents"), parseInput("/agents"))
        assertEquals(InputIntent.Message("/tasks"), parseInput("/tasks"))
        assertEquals(InputIntent.Message("/memory"), parseInput("/memory"))
    }

    @Test
    fun atSignSuggestsSupportedHermesRoutes() {
        assertTrue(
            suggestInputs("@c").containsAll(listOf("@coder", "@computer-operator"))
        )
        assertEquals(listOf("@main"), suggestInputs("@mai"))
    }

    @Test
    fun slashSuggestionsAreExactAndApproveIsAbsent() {
        val suggestions = suggestInputs("/")

        assertTrue(suggestions.containsAll(listOf("/new", "/call", "/memory")))
        assertTrue("/approve" !in suggestions)
        assertEquals(listOf("/mute"), suggestInputs("/mu"))
    }
}
