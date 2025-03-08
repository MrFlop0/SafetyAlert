package alex.kaplenkov.safetyalert.presentation.ui.common

import alex.kaplenkov.safetyalert.ui.theme.PrimaryGrey
import alex.kaplenkov.safetyalert.ui.theme.SecondaryBlue
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BoxScope.TextButton(
    modifier: Modifier = Modifier,
    params: List<String>,
    label: String,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(50.dp)
            .align(Alignment.TopCenter),
        colors = ButtonColors(
            containerColor = SecondaryBlue,
            contentColor = Color.White,
            disabledContainerColor = PrimaryGrey,
            disabledContentColor = Color.White
        ),
        enabled = checkFields(params),
        onClick = onClick
    ) {
        Text(
            text = label,
            fontSize = 14.sp
        )
    }
}

private fun checkFields(params: List<String> ): Boolean =
    params.all { it.isNotEmpty() }