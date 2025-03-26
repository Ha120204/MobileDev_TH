package com.example.autoreplymissedcalls

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val PERMISSIONS_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_NUMBERS
    )

    private val callReceiver = CallReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Tìm các view từ layout
        val editTextMessage = findViewById<EditText>(R.id.editTextMessage)
        val buttonSaveMessage = findViewById<Button>(R.id.buttonSaveMessage)
        val switchAutoReply = findViewById<Switch>(R.id.switchAutoReply)
        val statusTextView = findViewById<TextView>(R.id.statusTextView)

        // Đọc tin nhắn đã lưu
        val sharedPreferences = getSharedPreferences("AutoReplyPrefs", MODE_PRIVATE)
        val savedMessage = sharedPreferences.getString("message",
            "Xin lỗi, tôi đang bận. Tôi sẽ gọi lại cho bạn sau.")
        editTextMessage.setText(savedMessage)

        // Đọc trạng thái bật/tắt
        val isEnabled = sharedPreferences.getBoolean("isEnabled", true)
        switchAutoReply.isChecked = isEnabled

        // Lưu tin nhắn khi nhấn nút
        buttonSaveMessage.setOnClickListener {
            val message = editTextMessage.text.toString()
            sharedPreferences.edit().putString("message", message).apply()
            Toast.makeText(this, "Đã lưu tin nhắn", Toast.LENGTH_SHORT).show()
        }

        // Lưu trạng thái bật/tắt
        switchAutoReply.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("isEnabled", isChecked).apply()
            Toast.makeText(
                this,
                if (isChecked) "Đã bật trả lời tự động" else "Đã tắt trả lời tự động",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Kiểm tra và yêu cầu các quyền cần thiết
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
            statusTextView.text = "Trạng thái: Đang chờ cấp quyền..."
        } else {
            statusTextView.text = "Trạng thái: Đã sẵn sàng (đã cấp đủ quyền)"
            // Đăng ký receiver động
            registerCallReceiver()
        }
    }

    private fun registerCallReceiver() {
        try {
            val intentFilter = IntentFilter().apply {
                addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                addAction("android.intent.action.NEW_OUTGOING_CALL")
            }
            registerReceiver(callReceiver, intentFilter)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Lỗi khi đăng ký receiver: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val statusTextView = findViewById<TextView>(R.id.statusTextView)

            if (allPermissionsGranted()) {
                statusTextView.text = "Trạng thái: Đã sẵn sàng (đã cấp đủ quyền)"
                Toast.makeText(
                    this,
                    "Tất cả quyền đã được cấp. Ứng dụng sẽ tự động trả lời cuộc gọi nhỡ.",
                    Toast.LENGTH_LONG
                ).show()

                // Đăng ký receiver sau khi có quyền
                registerCallReceiver()
            } else {
                statusTextView.text = "Trạng thái: Không hoạt động (thiếu quyền)"
                Toast.makeText(
                    this,
                    "Quyền bị từ chối. Ứng dụng sẽ không hoạt động đúng.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(callReceiver)
        } catch (e: Exception) {
            // Receiver có thể chưa được đăng ký
        }
    }
}
