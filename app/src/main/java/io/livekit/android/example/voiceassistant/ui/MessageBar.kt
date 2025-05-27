package io.livekit.android.example.voiceassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
fun MessageBar(
    modifier: Modifier = Modifier,
    onSend: (message: String) -> Unit = {}
) {
    var text by rememberSaveable { mutableStateOf("") }

    val textFieldValue: String
    val textStyle: TextStyle
    if (text.isEmpty()) {
        textFieldValue = "Enter text..."
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground)
    } else {
        textFieldValue = text
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground)
    }

    val colors: TextFieldColors = TextFieldDefaults.colors()
    ConstraintLayout(modifier = modifier.background(Color.Green)) {

        val (textField, button) = createRefs()
        BasicTextField(
            value = textFieldValue,
            onValueChange = { text = it },
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(colors.cursorColor),
            modifier = Modifier.constrainAs(textField) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(button.end)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }
        )

        Button(
            onClick = { onSend(text) },
            modifier = Modifier.constrainAs(button) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
                width = Dimension.value(40.dp)
                height = Dimension.fillToConstraints
            }
        ) { }
    }
}

@Preview
@Composable
fun MessageBarPreview() {
    MessageBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    )
}