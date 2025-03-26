package com.example.callblocker

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
class InCallServiceImpl : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Call added: ${call.details.handle?.schemeSpecificPart}")

        currentCall = call

        // Kiểm tra nếu số điện thoại nằm trong danh sách chặn
        val phoneNumber = call.details.handle?.schemeSpecificPart
        if (phoneNumber != null) {
            val blockedNumbersManager = BlockedNumbersManager(this)
            if (blockedNumbersManager.isNumberBlocked(phoneNumber)) {
                Log.d(TAG, "Blocked number detected, rejecting call")

                // Từ chối cuộc gọi ngay lập tức
                call.disconnect()
                return
            }
        }

        // Nếu không phải số bị chặn, mở màn hình cuộc gọi
        val intent = Intent(this, InCallActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

        // Đăng ký callback để theo dõi trạng thái cuộc gọi
        call.registerCallback(callCallback)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed: ${call.details.handle?.schemeSpecificPart}")

        if (currentCall == call) {
            currentCall = null
        }

        call.unregisterCallback(callCallback)
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d(TAG, "Call state changed to: $state")
        }
    }

    companion object {
        private const val TAG = "InCallServiceImpl"
        private var currentCall: Call? = null

        fun getCurrentCall(): Call? = currentCall

        fun setMuted(muted: Boolean) {
            // Không có phương thức setMuted trực tiếp trong Connection
            // Cần sử dụng các phương thức khác nếu cần thiết
            Log.d(TAG, "Mute functionality is not available directly in this implementation.")
        }

        fun setAudioRoute(route: Int) {
            // Đặt audio route nếu cần thiết
            Log.d(TAG, "Set audio route: $route")
        }
    }
}
