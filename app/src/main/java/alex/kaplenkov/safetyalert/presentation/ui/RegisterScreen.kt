package alex.kaplenkov.safetyalert.presentation.ui

import alex.kaplenkov.safetyalert.presentation.ui.common.CommonTextField
import alex.kaplenkov.safetyalert.presentation.ui.common.GreetingView
import alex.kaplenkov.safetyalert.presentation.ui.common.TextButton
import alex.kaplenkov.safetyalert.ui.theme.DarkBlue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Composable
fun RegisterScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }

        GreetingView()

        Column(
            verticalArrangement = Arrangement.SpaceAround
        ) {
            CommonTextField(
                modifier = Modifier.padding(16.dp),
                text = name,
                placeholder = "Введите ваше полное имя",
                isPasswordTextField = false
            ) { name = it }

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
                label = "Зарегистрироваться",
                params = listOf(name, password, email)
            ) { navController.navigate(MainScreen) }
            
            Row(
                modifier = Modifier
                    .padding(top = 80.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "Уже есть аккаунт?"
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    modifier = Modifier.clickable {
                        navController.navigate(LoginScreen)
                    },
                    text = "Войти",
                    color = DarkBlue
                )
            }
        }


    }
}

@Serializable
object RegisterScreen
