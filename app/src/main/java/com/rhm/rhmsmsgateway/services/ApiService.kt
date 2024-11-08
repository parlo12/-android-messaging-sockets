package com.rhm.rhmsmsgateway.services

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,  // Adjust fields as per your actual API response
    val refreshToken: String,
    val message: String,
    val data: UserData,
)

data class UserData(
    val _id: String,
    val name: String,
    val email: String,
    val role: String,
    val subAdminId: String
)

data class RegisterDeviceRequest(
    val model: String,
    val SIM: String,
    val phoneNumber: String,
    val subAdminId: String
)

data class RegisterDeviceResponse(
    val message: String,
    val data: DeviceDate,
)

data class DeviceDate(
    val _id: String,
    val model: String,
    val status: String,
    val phoneNumber: String,
    val userId: String
)

interface ApiService {
    @POST("/users/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("/devices/register")
    suspend fun registerDevice(@Header("Authorization") token: String, @Body request: RegisterDeviceRequest): Response<RegisterDeviceResponse>
}
