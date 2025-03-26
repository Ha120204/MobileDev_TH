package com.example.callblocker

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
class PhoneAccountHelper(private val context: Context) {

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    fun registerPhoneAccount() {
        val componentName = ComponentName(context, CallBlockerConnectionService::class.java)
        val phoneAccountHandle = PhoneAccountHandle(componentName, ACCOUNT_ID)

        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, ACCOUNT_LABEL)
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .build()

        telecomManager.registerPhoneAccount(phoneAccount)
    }

    fun isDefaultDialer(): Boolean {
        return telecomManager.defaultDialerPackage == context.packageName
    }

    companion object {
        private const val ACCOUNT_ID = "call_blocker_account"
        private const val ACCOUNT_LABEL = "Call Blocker"
    }
}
