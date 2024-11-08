package com.rhm.rhmsmsgateway.services;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver(private val onSmsReceived: (String, String) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            val pdus = bundle.get("pdus") as Array<*>
            for (pdu in pdus) {
                val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                val sender = smsMessage.originatingAddress
                val messageBody = smsMessage.messageBody

                Log.d("SmsReceiver", "Received SMS: $messageBody from $sender")

                if (sender != null && messageBody != null) {
                    onSmsReceived(sender, messageBody)
                }
            }
        }
    }
}
