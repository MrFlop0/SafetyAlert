package alex.kaplenkov.safetyalert.presentation.ui.common

import alex.kaplenkov.safetyalert.ui.theme.SecondaryBlue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTextField(
    modifier: Modifier = Modifier,
    text: String,
    placeholder: String,
    isPasswordTextField: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = text,
        onValueChange = { onValueChange(it) },
        label = { Text(text = placeholder) },
        maxLines = 1,
        singleLine = true,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedBorderColor = SecondaryBlue,
            cursorColor = Color.Black
        ),
        shape = RoundedCornerShape(15.dp),
        modifier = modifier.fillMaxWidth(),
        visualTransformation = if (isPasswordTextField) PasswordVisualTransformation()
        else VisualTransformation.None
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    CommonTextField(
        text = "",
        placeholder = "Enter Login",
        isPasswordTextField = true
    ) { }
}