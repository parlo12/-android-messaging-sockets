package com.rhm.rhmsmsgateway.helpers

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.provider.Settings

@SuppressLint("MissingPermission")
fun getDeviceInfo(context: Context): Map<String, String?> {
    val deviceModel = Build.MODEL

    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//    val simSerialNumber = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
//        telephonyManager.simSerialNumber
//    } else {
//        null
//    }
    val simSerialNumber = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

    val phoneNumber = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {
        telephonyManager.line1Number
    } else {
        null
    }

    // Fetching Android ID and storing it into a constant
    val mId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    return mapOf(
        "model" to deviceModel,
        "SIM" to simSerialNumber,
        "phoneNumber" to phoneNumber,
        "deviceId" to mId
    )
}
