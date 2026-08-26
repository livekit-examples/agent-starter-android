package io.livekit.android.example.voiceassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.livekit.android.LiveKit
import io.livekit.android.example.voiceassistant.screen.VoiceAssistantScreen
import io.livekit.android.example.voiceassistant.ui.theme.LiveKitVoiceAssistantExampleTheme
import io.livekit.android.example.voiceassistant.viewmodel.VoiceAssistantViewModel
import io.livekit.android.util.LoggingLevel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LiveKit.loggingLevel = LoggingLevel.INFO

        setContent {
            LiveKitVoiceAssistantExampleTheme(dynamicColor = false) {
                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val viewModel = viewModel<VoiceAssistantViewModel>()
                        VoiceAssistantScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
