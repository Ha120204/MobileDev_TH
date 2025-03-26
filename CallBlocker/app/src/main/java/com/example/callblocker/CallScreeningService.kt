package com.example.callblocker

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.P)
class CallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: return
        Log.d("CallScreeningService", "Screening call from: $phoneNumber")

        val blockedNumbersManager = BlockedNumbersManager(this)

        if (blockedNumbersManager.isNumberBlocked(phoneNumber)) {
            Log.d("CallScreeningService", "Number is blocked: $phoneNumber")

            // Tạo response để từ chối cuộc gọi
            val response = CallResponse.Builder()
                .setDisallowCall(true)  // Không cho phép cuộc gọi
                .setRejectCall(true)    // Từ chối cuộc gọi
                .setSkipCallLog(false)  // Vẫn ghi nhật ký cuộc gọi
                .setSkipNotification(false)  // Vẫn hiển thị thông báo
                .build()

            respondToCall(callDetails, response)
        } else {
            // Cho phép cuộc gọi
            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

            respondToCall(callDetails, response)
        }
    }
}
