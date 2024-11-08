package com.rhm.rhmsmsgateway.viewModels

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rhm.rhmsmsgateway.services.LoginRequest
import com.rhm.rhmsmsgateway.services.RetrofitInstance
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import android.provider.Settings
import android.util.Log
import com.rhm.rhmsmsgateway.services.RegisterDeviceRequest

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences = application.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    val isLoggedIn: Boolean get() = sharedPreferences.contains("token")
    val userId: String get() = sharedPreferences.getString("userId", "") ?: ""

    val isLoading = mutableStateOf(false)
    val isLoadingRegisterDevice = mutableStateOf(false)

    val messageCount = mutableStateOf(0)  // Message counter for the session
    val userEmail = mutableStateOf<String?>(null)

    init {
        userEmail.value = sharedPreferences.getString("user_email", null)
    }

    var deviceId: String = sharedPreferences.getString("deviceId", "").toString()

    fun incrementMessageCount() {
        messageCount.value += 1
    }

    fun logout() {
        val temp = sharedPreferences.getString("deviceId","")
        sharedPreferences.edit().clear().apply()
        sharedPreferences.edit()
            .putString("deviceId", temp)
            .apply();
    }

    fun loginUser(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = RetrofitInstance.api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    println(response.body())
                    val token = response.body()?.token ?: ""
                    sharedPreferences.edit()
                        .putString("token", token)
                        .putString("refreshToken", response.body()?.refreshToken ?: "")
                        .putString("name", response.body()?.data?.name ?: "")
                        .putString("userId", response.body()?.data?._id ?: "")
                        .apply()
                    onResult(true, "Login successful")
                } else {
                    onResult(false, "Login failed: ${response.message()}")
                }
            } catch (e: IOException) {
                onResult(false, "Network error: ${e.message}")
            } catch (e: HttpException) {
                onResult(false, "Server error: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun registerDevice(deviceInfo: Map<String, String?>,onResult: (Boolean, String) -> Unit) {
        val request = RegisterDeviceRequest(
            model = deviceInfo["model"] ?: "",
            SIM = deviceInfo["SIM"] ?: "",
            phoneNumber = deviceInfo["phoneNumber"] ?: "",
            subAdminId = sharedPreferences.getString("userId", "").toString()
        )
        val token = "Bearer ${sharedPreferences.getString("token", "")}"
        Log.d("RegisterDevice", "Request: $request")
        Log.d("RegisterDevice", token)
        viewModelScope.launch {
            isLoadingRegisterDevice.value = true
            try {
                val response = RetrofitInstance.api.registerDevice(token,request)
                Log.d("RegisterDevice", "Response: $response")
                if (response.isSuccessful && response.body() != null) {
                    println(response.body())
                    deviceId = response.body()?.data?._id ?: ""
                    sharedPreferences.edit()
                        .putString("deviceId", deviceId)
                        .apply()

                    onResult(true, "Device Registered successful")
                } else {
                    onResult(false, "Device Registration failed: ${response.message()}")
                }
            } catch (e: IOException) {
                onResult(false, "Network error: ${e.message}")
            } catch (e: HttpException) {
                onResult(false, "Server error: ${e.message}")
            } finally {
                isLoadingRegisterDevice.value = false
            }
        }
    }
}
