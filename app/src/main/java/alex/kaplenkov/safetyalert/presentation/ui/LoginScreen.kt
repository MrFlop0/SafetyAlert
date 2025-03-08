package alex.kaplenkov.safetyalert.presentation.ui

import alex.kaplenkov.safetyalert.presentation.ui.common.CommonTextField
import alex.kaplenkov.safetyalert.presentation.ui.common.GreetingView
import alex.kaplenkov.safetyalert.presentation.ui.common.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable


@Composable
fun LoginScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        GreetingView()
        Column(
            verticalArrangement = Arrangement.SpaceAround
        ) {
            CommonTextField(
                modifier = Modifier.padding(16.dp),
                text = email,
                placeholder = "Введите вашу почту",
                isPasswordTextField = false
            ) { email = it }

            CommonTextField(
                modifier = Modifier.padding(16.dp),
                text = password,
                placeholder = "Введите пароль",
                isPasswordTextField = true
            ) { password = it }
        }

        Box {
            TextButton(
                label = "Войти",
                params = listOf(password, email)
            ) { navController.navigate(MainScreen) }
        }


    }
}

@Serializable
object LoginScreen

@Preview(showBackground = true)
@Composable
private fun preview() {
    val n = rememberNavController()
    LoginScreen(n)
}

