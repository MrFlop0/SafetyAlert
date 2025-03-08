package alex.kaplenkov.safetyalert

import alex.kaplenkov.safetyalert.presentation.ui.CameraScreen
import alex.kaplenkov.safetyalert.presentation.ui.LoginScreen
import alex.kaplenkov.safetyalert.presentation.ui.MainScreen
import alex.kaplenkov.safetyalert.presentation.ui.RegisterScreen
import alex.kaplenkov.safetyalert.presentation.ui.SettingScreen
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
                    startDestination = RegisterScreen
                ) {
                    composable<MainScreen> {
                        MainScreen(navController)
                    }
                    composable<SettingScreen> {
                        SettingScreen(navController)
                    }
                    composable<LoginScreen> {
                        LoginScreen(navController)
                    }
                    composable<RegisterScreen> {
                        RegisterScreen(navController)
                    }
                    composable<CameraScreen> {
                        CameraScreen(navController)
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