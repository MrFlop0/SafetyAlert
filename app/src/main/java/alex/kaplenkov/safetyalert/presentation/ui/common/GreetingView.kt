package alex.kaplenkov.safetyalert.presentation.ui.common

import alex.kaplenkov.safetyalert.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GreetingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
    ) {
        Icon(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopCenter),
            painter = painterResource(R.drawable.ic_launcher_foreground),
            tint = Color.Unspecified,
            contentDescription = null
        )
        Text(
            modifier = Modifier.align(Alignment.BottomCenter),
            text = "Добро пожаловать в Safety Alert!",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}