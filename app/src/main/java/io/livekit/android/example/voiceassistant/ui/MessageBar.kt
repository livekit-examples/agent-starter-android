package io.livekit.android.example.voiceassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import io.livekit.android.example.voiceassistant.ui.theme.Blue500
import io.livekit.android.example.voiceassistant.ui.theme.Dimens
import io.livekit.android.example.voiceassistant.ui.theme.LightLine

@Composable
fun ChatBar(
    onChatSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(
        modifier = modifier
            .drawBehind {
                val strokeWidth = 1 * density
                drawLine(
                    LightLine,
                    Offset(0f, 0f),
                    Offset(size.width, 0f),
                    strokeWidth
                )
            }
            .background(MaterialTheme.colorScheme.surface)
            .padding(Dimens.spacer)
    ) {
        val (sendButton, messageInput) = createRefs()

        var message by rememberSaveable {
            mutableStateOf("")
        }
        LKTextField(
            value = message,
            onValueChange = { message = it },
            shape = RoundedCornerShape(5.dp),
            colors = TextFieldDefaults.colors().copy(
                disabledTextColor = Color.Transparent,
                focusedContainerColor = Color(0xFF222222),
                unfocusedContainerColor = Color(0xFF222222),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            placeholder = {
                Text("Type your message...")
            },
            modifier = Modifier
                .constrainAs(messageInput) {
                    start.linkTo(parent.start)
                    end.linkTo(sendButton.start, Dimens.spacer)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.fillToConstraints
                    height = Dimension.wrapContent
                }
        )

        val sendButtonColors = ButtonDefaults.buttonColors(
            containerColor = Blue500,
            contentColor = Color.White
        )

        Button(
            colors = sendButtonColors,
            shape = RoundedCornerShape(5.dp),
            onClick = {
                onChatSend(message)
                message = ""
            },
            enabled = message.isNotEmpty(),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .padding(0.dp)
                .constrainAs(sendButton) {
                    end.linkTo(parent.end)
                    top.linkTo(messageInput.top)
                    width = Dimension.wrapContent
                    height = Dimension.value(42.dp)
                }
        ) {
            Text(text = "Send", modifier = Modifier.padding(0.dp))
        }
    }
}

@Preview
@Composable
fun ChatWidgetPreview() {
    Column {
        ChatBar(
            onChatSend = {
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

//
//@Composable
//fun MessageBar(
//    modifier: Modifier = Modifier,
//    onSend: (message: String) -> Unit = {}
//) {
//    var text by rememberSaveable { mutableStateOf("") }
//
//    val textFieldValue: String
//    val textStyle: TextStyle
//    if (text.isEmpty()) {
//        textFieldValue = "Enter text..."
//        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground)
//    } else {
//        textFieldValue = text
//        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground)
//    }
//
//    val colors: TextFieldColors = TextFieldDefaults.colors()
//    ConstraintLayout(modifier = modifier) {
//
//        val (textField, button) = createRefs()
//        TextField(
//            value = textFieldValue,
//            onValueChange = { text = it },
//            textStyle = textStyle,
//            singleLine = true,
//            cursorBrush = SolidColor(colors.cursorColor),
//            modifier = Modifier.constrainAs(textField) {
//                top.linkTo(parent.top)
//                bottom.linkTo(parent.bottom)
//                start.linkTo(parent.start)
//                end.linkTo(button.end)
//                width = Dimension.fillToConstraints
//                height = Dimension.fillToConstraints
//            }
//        )
//
//        Button(
//            onClick = { onSend(text) },
//            modifier = Modifier.constrainAs(button) {
//                top.linkTo(parent.top)
//                bottom.linkTo(parent.bottom)
//                end.linkTo(parent.end)
//                width = Dimension.value(40.dp)
//                height = Dimension.fillToConstraints
//            }
//        ) { }
//    }
//}

//@Preview
//@Composable
//fun MessageBarPreview() {
//    MessageBar(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(100.dp)
//    )
//}