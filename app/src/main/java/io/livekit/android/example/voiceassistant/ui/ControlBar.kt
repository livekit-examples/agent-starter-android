package io.livekit.android.example.voiceassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ControlBar(
    onMicClick: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onUploadClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onExitClick:() -> Unit = {},
    modifier: Modifier = Modifier
) {

    var height by remember { mutableIntStateOf(0) }
    val cornerRadius = height / 2f
    Row (
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
            .onSizeChanged { height = it.height }
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.Red)
    ) {

        Button(onClick = onMicClick) {

        }
        Button(onClick = onCameraClick) {

        }
        Button(onClick = onUploadClick) {

        }
        Button(onClick = onChatClick) {

        }
        Button(onClick = onExitClick) {

        }
    }
}

@Preview
@Composable
fun ControlBarPreview(){
    ControlBar(
        modifier = Modifier.fillMaxWidth()
            .height(40.dp)
    )
}