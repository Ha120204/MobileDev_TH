package com.example.autoreplymissedcalls

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var ringStartTime: Long = 0
        private var isRinging = false
        private var savedNumber: String? = null

        // Tag cho log
        private const val TAG = "MissedCallSMS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive được gọi với action: ${intent.action}")

        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            Log.d(TAG, "Bỏ qua intent với action khác: ${intent.action}")
            return
        }

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "Trạng thái điện thoại thay đổi: $stateStr, số gọi đến: $incomingNumber")

        // Nếu số điện thoại gọi đến được cung cấp, lưu lại
        if (incomingNumber != null && incomingNumber.isNotEmpty()) {
            savedNumber = incomingNumber
            Log.d(TAG, "Đã lưu số điện thoại gọi đến: $incomingNumber")
        }

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Điện thoại đang đổ chuông
                isRinging = true
                ringStartTime = System.currentTimeMillis()
                Log.d(TAG, "Điện thoại đang đổ chuông. Số gọi đến: $savedNumber")

                // Hiển thị toast để xác nhận receiver đã nhận được sự kiện
                try {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "Có cuộc gọi đến từ: $savedNumber",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi hiển thị toast: ${e.message}")
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Cuộc gọi được trả lời
                Log.d(TAG, "Cuộc gọi được trả lời")
                isRinging = false
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Điện thoại trở về trạng thái rảnh (cuộc gọi kết thúc hoặc bị từ chối)
                Log.d(TAG, "Điện thoại trở về trạng thái rảnh. isRinging=$isRinging")

                if (isRinging) {
                    // Điện thoại vừa đổ chuông và bây giờ đã dừng mà không vào trạng thái OFFHOOK
                    // => Đây là cuộc gọi nhỡ
                    val ringDuration = System.currentTimeMillis() - ringStartTime
                    Log.d(TAG, "Phát hiện cuộc gọi nhỡ. Thời gian đổ chuông: $ringDuration ms")

                    if (savedNumber != null && savedNumber!!.isNotEmpty()) {
                        Log.d(TAG, "Chuẩn bị gửi SMS đến người gọi nhỡ: $savedNumber")
                        sendSMS(context, savedNumber!!)
                    } else {
                        // Nếu không có số điện thoại, thử đọc từ nhật ký cuộc gọi
                        Log.d(TAG, "Không có số gọi đến đã lưu, kiểm tra nhật ký cuộc gọi...")
                        Handler(Looper.getMainLooper()).postDelayed({
                            checkMissedCall(context)
                        }, 2000) // Chờ 2 giây để nhật ký cuộc gọi được cập nhật
                    }
                }
                isRinging = false
            }
        }

        lastState = when (stateStr) {
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            else -> lastState
        }
    }

    private fun checkMissedCall(context: Context) {
        Log.d(TAG, "Bắt đầu kiểm tra nhật ký cuộc gọi nhỡ")

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Không có quyền READ_CALL_LOG")
            return
        }

        try {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE
            )

            val selection = "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?"
            val selectionArgs = arrayOf(
                CallLog.Calls.MISSED_TYPE.toString(),
                (System.currentTimeMillis() - 10000).toString() // Cuộc gọi nhỡ trong 10 giây qua
            )

            val sortOrder = "${CallLog.Calls.DATE} DESC"

            Log.d(TAG, "Truy vấn nhật ký cuộc gọi với selection: $selection")

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                    if (numberIndex != -1) {
                        val missedCallNumber = cursor.getString(numberIndex)
                        Log.d(TAG, "Tìm thấy cuộc gọi nhỡ gần đây từ: $missedCallNumber")

                        // Gửi SMS đến số vừa gọi nhỡ
                        sendSMS(context, missedCallNumber)
                    } else {
                        Log.e(TAG, "Không tìm thấy cột NUMBER trong cursor")
                    }
                } else {
                    Log.d(TAG, "Không tìm thấy cuộc gọi nhỡ gần đây trong nhật ký")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra cuộc gọi nhỡ: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun sendSMS(context: Context, phoneNumber: String) {
        Log.d(TAG, "Bắt đầu gửi SMS đến người gọi nhỡ: $phoneNumber")

        if (phoneNumber.isEmpty()) {
            Log.e(TAG, "Không thể gửi SMS: Số điện thoại trống")
            return
        }

        // Kiểm tra quyền SEND_SMS
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Không có quyền SEND_SMS")
            return
        }

        try {
            // Đọc cài đặt từ SharedPreferences
            val sharedPreferences = context.getSharedPreferences("AutoReplyPrefs", Context.MODE_PRIVATE)
            val isEnabled = sharedPreferences.getBoolean("isEnabled", true)

            // Kiểm tra nếu tính năng đã tắt
            if (!isEnabled) {
                Log.d(TAG, "Tính năng trả lời tự động đang tắt")
                return
            }

            val message = sharedPreferences.getString(
                "message",
                "Xin lỗi, tôi đang bận. Tôi sẽ gọi lại cho bạn sau."
            ) ?: "Xin lỗi, tôi đang bận. Tôi sẽ gọi lại cho bạn sau."

            Log.d(TAG, "Nội dung tin nhắn: $message")

            // Hiển thị toast để xác nhận việc gửi SMS
            try {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Đang gửi SMS đến người gọi nhỡ: $phoneNumber",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi hiển thị toast: ${e.message}")
            }

            val smsManager = SmsManager.getDefault()

            Log.d(TAG, "Gửi SMS đến người gọi nhỡ $phoneNumber: $message")

            // Kiểm tra nếu tin nhắn quá dài
            if (message.length > 160) {
                val messageParts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    messageParts,
                    null,
                    null
                )
            } else {
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            }

            Log.d(TAG, "SMS đã được gửi thành công đến người gọi nhỡ")

            // Hiển thị toast xác nhận đã gửi SMS
            try {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Đã gửi SMS đến người gọi nhỡ: $phoneNumber",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi hiển thị toast: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi SMS: ${e.message}")
            e.printStackTrace()

            // Hiển thị toast thông báo lỗi
            try {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Lỗi khi gửi SMS: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi hiển thị toast: ${e.message}")
            }
        }
    }
}
