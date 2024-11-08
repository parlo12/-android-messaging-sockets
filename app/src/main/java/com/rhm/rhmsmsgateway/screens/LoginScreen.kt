package com.rhm.rhmsmsgateway.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rhm.rhmsmsgateway.viewModels.UserViewModel

@Composable
fun LoginScreen(viewModel: UserViewModel, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("hello2@buganzi.com") }
    var password by remember { mutableStateOf("password123") }
    var loginStatus by remember { mutableStateOf<String?>(null) }
    val isLoading by viewModel.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    )
    {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.loginUser(email, password) { success, message ->
                        loginStatus = message
                        if (success) {
                            onLoginSuccess()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
            loginStatus?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it)
            }
        }
    }
}
