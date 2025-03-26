package com.example.callblocker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                if (phoneNumber != null) {
                    Log.d("CallReceiver", "Incoming call from: $phoneNumber")

                    val blockedNumbersManager = BlockedNumbersManager(context)
                    if (blockedNumbersManager.isNumberBlocked(phoneNumber)) {
                        Log.d("CallReceiver", "Number is blocked, showing notification")

                        // Hiển thị thông báo cho người dùng
                        showBlockedCallNotification(context, phoneNumber)
                    }
                }
            }
        }
    }

    private fun showBlockedCallNotification(context: Context, phoneNumber: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo notification channel cho Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Blocked Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi có cuộc gọi từ số bị chặn"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tạo thông báo
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.blocked_call_notification))
            .setContentText(context.getString(R.string.blocked_call_message, phoneNumber))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .build()

        // Hiển thị thông báo
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "blocked_calls_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
