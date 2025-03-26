package alex.kaplenkov.safetyalert

import alex.kaplenkov.safetyalert.presentation.ui.CameraScreen
import alex.kaplenkov.safetyalert.presentation.ui.LoginScreen
import alex.kaplenkov.safetyalert.presentation.ui.MainScreen
import alex.kaplenkov.safetyalert.presentation.ui.RegisterScreen
import alex.kaplenkov.safetyalert.presentation.ui.ReportListScreen
import alex.kaplenkov.safetyalert.presentation.ui.SettingScreen
import alex.kaplenkov.safetyalert.presentation.ui.SummaryScreen
import alex.kaplenkov.safetyalert.ui.theme.SafetyAlertTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
                    composable<SummaryScreen> {
                        SummaryScreen(navController)
                    }
                    composable<ReportListScreen> {
                        ReportListScreen(navController)
                    }
                }
            }
        }
    }
}