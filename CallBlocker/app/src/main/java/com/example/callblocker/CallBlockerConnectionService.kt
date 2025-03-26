package com.example.callblocker

import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
class CallBlockerConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val connection = CallConnection()
        val phoneNumber = request?.address?.schemeSpecificPart

        if (phoneNumber != null) {
            Log.d("ConnectionService", "Incoming call from: $phoneNumber")

            val blockedNumbersManager = BlockedNumbersManager(this)
            if (blockedNumbersManager.isNumberBlocked(phoneNumber)) {
                Log.d("ConnectionService", "Number is blocked, rejecting call")
                connection.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                connection.destroy() // Ngắt kết nối
            }
        }

        // Nếu không phải số bị chặn, thiết lập kết nối bình thường
        connection.setInitializing()
        connection.setActive()
        return connection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        // Không cần xử lý cuộc gọi đi
        val connection = CallConnection()
        connection.setInitializing()
        connection.setActive()
        return connection
    }

    class CallConnection : Connection() {
        init {
            // Cấu hình các thuộc tính cơ bản
            audioModeIsVoip = true
            connectionCapabilities = CAPABILITY_MUTE or CAPABILITY_SUPPORT_HOLD
        }
    }
}
