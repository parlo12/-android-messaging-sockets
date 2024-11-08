package com.rhm.rhmsmsgateway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.rhm.rhmsmsgateway.screens.LoginScreen
import com.rhm.rhmsmsgateway.viewModels.UserViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: UserViewModel = viewModel()
            val navController = rememberNavController()
            AppNavHost(navController, viewModel)
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, viewModel: UserViewModel) {
    NavHost(
        navController = navController,
        startDestination = if (viewModel.isLoggedIn) "sms_responder" else "login"
    ) {
        composable("login") {
            LoginScreen(viewModel = viewModel, onLoginSuccess = {
                navController.navigate("sms_responder") {
                    popUpTo("login") { inclusive = true }  // Clears the back stack
                }
            })
        }
        composable("sms_responder") {
            SmsResponderApp(
                context = LocalContext.current,
                viewModel = viewModel,
                navController = navController
            )
        }
    }
}

@Preview
@Composable
fun PreviewMainScreen() {
    val viewModel: UserViewModel = viewModel()
    val navController = rememberNavController()
    AppNavHost(
        navController = navController,
        viewModel = viewModel
    )
}