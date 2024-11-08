package com.rhm.rhmsmsgateway.services

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.net.URISyntaxException

class SmsForegroundService : Service() {

    private lateinit var smsReceiver: SmsReceiver
    private var deviceId: String? = null
    private var userId: String? = null
    private var phoneNumber: String? = null
    private lateinit var socket: Socket
    private val gson = Gson()

    // Define unique action strings for sent and delivered statuses
    private val SMS_SENT_ACTION = "SMS_SENT"
    private val SMS_DELIVERED_ACTION = "SMS_DELIVERED"


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        deviceId = intent?.getStringExtra("deviceId")
        userId = intent?.getStringExtra("userId")
        phoneNumber = intent?.getStringExtra("phoneNumber")
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize Socket.IO connection
        try {
            val sharedPreferences = application.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            socket = IO.socket("ws://165.22.179.230:4000") // Replace with your server URL
            // socket = IO.socket("ws://10.0.2.2:4000") // Replace with your server URL
            socket.connect()
            val mainChannel = sharedPreferences.getString("userId","_")
            socket.on(mainChannel) { args ->
                if (args.isNotEmpty()) {
                    try {
                        val data = args[0] as JSONObject
                        Log.d("SmsForegroundService", "Received message: $data")
                        if (data.getString("origin").toString().equals("CRM")) {
                            handleSocketIncomingMessage(data)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            println("Listening for messages on $mainChannel")
            socket.on("pendingMessages") { args ->
                if (args.isNotEmpty()) {
                    try {
                        val data = args[0] as JSONArray
                        Log.d("SmsForegroundService", "Received pending messages: $data")
                        for (i in 0 until data.length()) {
                            val message = data.getJSONObject(i)
                            sendSmsResponse(
                                message.getString("receiver"),
                                message.getString("content"),
                                message.getString("messageId")
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            socket.on("connect") { args ->
                println("Socket IO connected");
            }
            socket.on("disconnect") { args ->
                println("Socket IO disconnected");
            }

        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
        // Set up the SMS receiver
        smsReceiver = SmsReceiver { sender, message ->
            // Respond to the sender
            // sendSmsResponse(sender, "Hello $username on device $deviceId, thank you for your message!")
            emitMessageToSocket(sender, message)
        }

        // Register broadcast receivers for sent and delivered status
        registerReceiver(sentStatusReceiver, IntentFilter(SMS_SENT_ACTION), RECEIVER_EXPORTED)
        registerReceiver(
            deliveredStatusReceiver,
            IntentFilter(SMS_DELIVERED_ACTION),
            RECEIVER_EXPORTED
        )

        // Register the SMS receiver
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        registerReceiver(smsReceiver, filter)

        // Start the service in the foreground
        startForegroundService()
        Log.d("SmsForegroundService", "Service started")
        socket.emit("getPendingMessages", JSONObject())
    }

    private fun startForegroundService() {
        val notificationChannelId = "SMSServiceChannel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "SMS Foreground Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("SMS Listening Service")
            .setContentText("Listening for incoming SMS messages")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun handleSocketIncomingMessage(data: JSONObject) {
        val sender = data.getString("sender")
        val receiver = data.getString("receiver")
        val content = data.getString("content")
        val messageId = data.getString("messageId")

        // Send SMS to the `receiver` phone number with the `content`
        sendSmsResponse(receiver, content, messageId)
    }

    private fun sendSmsResponse(phoneNumber: String, message: String, messageId: String) {
        val smsManager = android.telephony.SmsManager.getDefault()

        // Create PendingIntent for sent and delivered actions
        val sentIntent = PendingIntent.getBroadcast(
            this, 0, Intent(SMS_SENT_ACTION).apply {
                putExtra("messageId", messageId)
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        val deliveredIntent = PendingIntent.getBroadcast(
            this, 0, Intent(SMS_DELIVERED_ACTION).apply {
                putExtra("messageId", messageId)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, deliveredIntent)
        Log.d("SmsForegroundService", "SMS sent to $phoneNumber: $message")
    }

    private fun emitMessageToSocket(sender: String, message: String) {
        val payload = mapOf(
            "sender" to sender,
            "receiver" to phoneNumber, // Use `deviceId` as the receiver for server recognition
            "content" to message,
            "userId" to userId, // Use actual userId
            "deviceId" to deviceId,
            "origin" to "Android"
        )
        socket.emit("message", JSONObject(payload))
        Log.d("SmsForegroundService", "Emitted message: $payload")
    }

    // BroadcastReceiver to listen for SMS sent status
    private val sentStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.d("SmsForegroundService", "SMS sent successfully")
                    intent.getStringExtra("messageId")?.let {
                        val payload = mapOf(
                            "status" to "sent",
                            "messageId" to it
                        )
                        socket.emit("messageStatus", JSONObject(payload))
                        Log.d("SmsForegroundService", "Emitted message status: $payload")

                        socket.emit("messageSent")
                    }
                }

                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Log.e(
                    "SmsForegroundService",
                    "SMS not sent: Generic failure"
                )

                SmsManager.RESULT_ERROR_NO_SERVICE -> Log.e(
                    "SmsForegroundService",
                    "SMS not sent: No service"
                )

                SmsManager.RESULT_ERROR_NULL_PDU -> Log.e(
                    "SmsForegroundService",
                    "SMS not sent: Null PDU"
                )

                SmsManager.RESULT_ERROR_RADIO_OFF -> Log.e(
                    "SmsForegroundService",
                    "SMS not sent: Radio off"
                )
            }
        }
    }

    // BroadcastReceiver to listen for SMS delivered status
    private val deliveredStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.d("SmsForegroundService", "SMS delivered successfully")
                    intent.getStringExtra("messageId")?.let {
                        val payload = mapOf(
                            "status" to "sent",
                            "messageId" to it
                        )
                        socket.emit("messageStatus", JSONObject(payload))
                        Log.d("SmsForegroundService", "Emitted message status: $payload")

                        socket.emit("messageSent")
                    }
                }

                Activity.RESULT_CANCELED -> Log.e("SmsForegroundService", "SMS not delivered")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver to prevent memory leaks
        unregisterReceiver(smsReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

