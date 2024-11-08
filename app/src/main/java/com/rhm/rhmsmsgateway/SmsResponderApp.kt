package com.rhm.rhmsmsgateway

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.rhm.rhmsmsgateway.helpers.getDeviceInfo
import com.rhm.rhmsmsgateway.services.SmsForegroundService
import com.rhm.rhmsmsgateway.viewModels.UserViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SmsResponderApp(context: Context,viewModel: UserViewModel, navController: NavHostController) {
    val deviceId = viewModel.deviceId
    val userId = viewModel.userId
    val userEmail by viewModel.userEmail
    val messageCount by viewModel.messageCount
    RequestSmsPermissions {
        val deviceInfo = getDeviceInfo(context)
        println(deviceInfo)
        val phoneNumber = deviceInfo["phoneNumber"]
        viewModel.registerDevice(deviceInfo, onResult = { success, message ->{}})
        startSmsForegroundService(context,deviceId, userId, phoneNumber.toString())
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display user and device information
        Text("Welcome, ${userEmail ?: "User"}", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Device ID: $deviceId", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Display message count for the session
        Text("Messages received in this session: $messageCount", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Logout button to clear session and navigate back to login
        Button(
            onClick = {
                viewModel.logout()
                stopSmsForegroundService(context)
                navController.navigate("login") {
                    popUpTo("sms_responder") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestSmsPermissions(onPermissionsGranted: () -> Unit) {
    val receiveSmsPermissionState = rememberPermissionState(android.Manifest.permission.RECEIVE_SMS)
    val sendSmsPermissionState = rememberPermissionState(android.Manifest.permission.SEND_SMS)
    val readPhoneStatePermissionState = rememberPermissionState(android.Manifest.permission.READ_PHONE_STATE)
    val readPhoneNumbersPermissionState = rememberPermissionState(android.Manifest.permission.READ_PHONE_NUMBERS)
    LaunchedEffect(Unit) {
        if (!receiveSmsPermissionState.status.isGranted) {
            receiveSmsPermissionState.launchPermissionRequest()
        }
        if (!sendSmsPermissionState.status.isGranted) {
            sendSmsPermissionState.launchPermissionRequest()
        }
        if (!readPhoneStatePermissionState.status.isGranted){
            readPhoneStatePermissionState.launchPermissionRequest()
        }
        if (!readPhoneNumbersPermissionState.status.isGranted){
            readPhoneNumbersPermissionState.launchPermissionRequest()
        }
    }

    if (receiveSmsPermissionState.status.isGranted && sendSmsPermissionState.status.isGranted
        && readPhoneStatePermissionState.status.isGranted  && readPhoneNumbersPermissionState.status.isGranted
        ) {
        onPermissionsGranted()
    } else {
        Text("Permissions are required to listen and respond to SMS.")
    }
}

fun startSmsForegroundService(context: Context, deviceId: String, userId: String, phoneNumber: String) {
    val intent = Intent(context, SmsForegroundService::class.java).apply {
        putExtra("deviceId", deviceId)
        putExtra("userId", userId)
        putExtra("phoneNumber", phoneNumber)
    }
    context.startForegroundService(intent)
}

private fun stopSmsForegroundService(context: Context) {
    val intent = Intent(context, SmsForegroundService::class.java)
    context.stopService(intent)
}
