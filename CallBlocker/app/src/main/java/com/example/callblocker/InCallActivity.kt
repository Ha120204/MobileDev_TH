package com.example.callblocker

import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.View
import androidx.activity.ComponentActivity
import com.example.callblocker.databinding.ActivityInCallBinding

class InCallActivity : ComponentActivity() {
    private lateinit var binding: ActivityInCallBinding
    private var call: Call? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCallButtons()
        updateUI()
    }

    private fun setupCallButtons() {
        binding.endCallButton.setOnClickListener {
            call?.disconnect()
            finish()
        }

        binding.muteButton.setOnClickListener {
            val currentMuteState = binding.muteButton.isSelected
            binding.muteButton.isSelected = !currentMuteState
            InCallServiceImpl.setMuted(!currentMuteState)
        }

        binding.speakerButton.setOnClickListener {
            val currentSpeakerState = binding.speakerButton.isSelected
            binding.speakerButton.isSelected = !currentSpeakerState

            val route = if (currentSpeakerState) {
                CallAudioState.ROUTE_EARPIECE
            } else {
                CallAudioState.ROUTE_SPEAKER
            }

            InCallServiceImpl.setAudioRoute(route)
        }
    }

    private fun updateUI() {
        call = InCallServiceImpl.getCurrentCall()

        call?.let { currentCall ->
            // Hiển thị thông tin người gọi
            val phoneNumber = currentCall.details.handle?.schemeSpecificPart ?: "Số không xác định"
            binding.callerNumberTextView.text = phoneNumber

            // Kiểm tra nếu số điện thoại nằm trong danh sách chặn
            val blockedNumbersManager = BlockedNumbersManager(this)
            if (blockedNumbersManager.isNumberBlocked(phoneNumber)) {
                binding.blockedCallBanner.visibility = View.VISIBLE
            } else {
                binding.blockedCallBanner.visibility = View.GONE
            }

            // Cập nhật trạng thái cuộc gọi
            val callCallback = object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    runOnUiThread {
                        updateCallState(state)

                        if (state == Call.STATE_DISCONNECTED) {
                            finish()
                        }
                    }
                }
            }

            currentCall.registerCallback(callCallback)
            updateCallState(currentCall.state)
        } ?: run {
            // Không có cuộc gọi, đóng activity
            finish()
        }
    }

    private fun updateCallState(state: Int) {
        val stateText = when (state) {
            Call.STATE_NEW -> "Đang khởi tạo"
            Call.STATE_RINGING -> "Đang đổ chuông"
            Call.STATE_DIALING -> "Đang gọi"
            Call.STATE_ACTIVE -> "Đang kết nối"
            Call.STATE_HOLDING -> "Đang giữ"
            Call.STATE_DISCONNECTED -> "Đã kết thúc"
            Call.STATE_DISCONNECTING -> "Đang kết thúc"
            else -> "Không xác định"
        }

        binding.callStateTextView.text = stateText
    }

    override fun onDestroy() {
        super.onDestroy()
        call?.unregisterCallback(null)
    }
}
