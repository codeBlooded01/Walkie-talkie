package com.talkie.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.talkie.app.presentation.ui.TalkieApp
import com.talkie.app.presentation.ui.auth.LoginScreen
import com.talkie.app.presentation.ui.auth.RegisterScreen
import com.talkie.app.presentation.ui.DispatcherDashboardScreen

enum class TalkieAuthScreen { LOGIN, REGISTER }

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val talkieViewModel: TalkieViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    TalkieAppNavigation(authViewModel, talkieViewModel)
                }
            }
        }
    }
}

@Composable
fun TalkieAppNavigation(
    authViewModel: AuthViewModel,
    talkieViewModel: TalkieViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    var currentAuthScreen by remember { mutableStateOf(TalkieAuthScreen.LOGIN) }

    when (val state = authState) {
        is AuthState.Success -> {
            if (state.role == "DISPATCHER") {
                DispatcherDashboardScreen(
                    authViewModel = authViewModel
                )
            } else {
                TalkieApp(
                    viewModel = talkieViewModel,
                    onLogout = { authViewModel.logout() }
                )
            }
        }
        else -> {
            if (currentAuthScreen == TalkieAuthScreen.LOGIN) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { currentAuthScreen = TalkieAuthScreen.REGISTER }
                )
            } else {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { currentAuthScreen = TalkieAuthScreen.LOGIN }
                )
            }
        }
    }
}


