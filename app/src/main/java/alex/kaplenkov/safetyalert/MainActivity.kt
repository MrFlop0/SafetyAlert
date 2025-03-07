package alex.kaplenkov.safetyalert

import alex.kaplenkov.safetyalert.presentation.ui.MainScreen
import alex.kaplenkov.safetyalert.ui.theme.SafetyAlertTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafetyAlertTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = MainScreen
                ) {
                    composable<MainScreen> {
                        MainScreen(navController)
                    }
                }

            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SafetyAlertTheme {
        Greeting("Android")
    }
}