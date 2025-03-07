package alex.kaplenkov.safetyalert.presentation.ui

import alex.kaplenkov.safetyalert.R
import alex.kaplenkov.safetyalert.ui.theme.Blue95
import alex.kaplenkov.safetyalert.ui.theme.PrimaryBlue
import alex.kaplenkov.safetyalert.ui.theme.PrimaryGrey
import alex.kaplenkov.safetyalert.ui.theme.Red
import alex.kaplenkov.safetyalert.ui.theme.SafetyAlertTheme
import alex.kaplenkov.safetyalert.ui.theme.SecondaryBlue
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable

@Composable
fun MainScreen(controller: NavHostController) {
    var selectedType by remember { mutableStateOf<String?>(null) }
    val types = listOf(
        ViolationType("Курение в неположенном месте", R.drawable.smoke_icon),
        ViolationType("Несоблюдение обязательного использования поручня", R.drawable.stairs_icon),
        ViolationType("Нарушение норм по ношению каски", R.drawable.helmet_icon)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            modifier = Modifier.align(Alignment.TopCenter),
            text = "SafetyAlert",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Blue95
        )
        Spacer(modifier = Modifier.height(100.dp))

        IconButton(
            modifier = Modifier.align(Alignment.TopEnd),
            onClick = { controller.navigate("ddd")}
        ) {
            Icon(
                modifier = Modifier.size(60.dp),
                painter = painterResource(R.drawable.setting_icon),
                contentDescription = "Settings"
            )
        }

        StartButton(
            modifier = Modifier.align(Alignment.Center)
        )
//        ViolationSelector(types) {
//
//        }
        Spacer(modifier = Modifier.height(50.dp))
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                types.forEach {
                    val (backgroundColor, tintColor) =
                        if (selectedType == it.text) {
                            SecondaryBlue to Red
                        } else {
                            PrimaryBlue to Color.Black
                        }
                    IconButton(
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(60.dp)
                            .background(backgroundColor),
                        onClick = { selectedType = it.text }
                    ) {
                        Icon(
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            painter = painterResource(id = it.iconRes),
                            contentDescription = it.text,
                            tint = tintColor
                        )
                    }
                }
            }

            val chosenType = selectedType ?: "Выберите тип нарушения"
            val textColor = selectedType?.let { Color.Black } ?: Color.Red
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp, start = 25.dp, end = 25.dp, top = 25.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(PrimaryGrey),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 16.dp),
                    text = chosenType,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ViolationSelector(violations: List<ViolationType>, onClick: () -> Unit) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceEvenly
//    ) {
//        violations.forEach {
//            IconButton(
//                modifier = Modifier.clip(CircleShape)
//                    .background(if ())
//            ) {
//                Icon(
//                    modifier = Modifier.size(50.dp),
//                    painter = painterResource(id = it.iconRes),
//                    contentDescription = it.text
//                )
//            }
//        }
//    }
}

@Composable
private fun StartButton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            modifier = Modifier.clip(CircleShape).size(100.dp),
            onClick = { },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = SecondaryBlue,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start Recording",
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            modifier = Modifier.padding(top = 7.dp),
            text = "Нажмите для начала сьемки",
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Thin

        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun Preview() {
//    SafetyAlertTheme {
//        MainScreen()
//    }
//}

private data class ViolationType(val text: String, @DrawableRes val iconRes: Int)

@Serializable
object MainScreen