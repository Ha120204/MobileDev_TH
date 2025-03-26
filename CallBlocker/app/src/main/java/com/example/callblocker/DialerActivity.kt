package com.example.callblocker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.callblocker.databinding.DialScreenBinding

class DialerActivity : ComponentActivity() {
    private lateinit var binding: DialScreenBinding

    // Sử dụng Activity Result API thay vì onRequestPermissionsResult
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Quyền đã được cấp, thực hiện cuộc gọi
            val phoneNumber = binding.phoneNumberEditTextDial.text.toString()
            if (phoneNumber.isNotEmpty()) {
                makePhoneCall(phoneNumber)
            }
        } else {
            // Quyền bị từ chối
            Toast.makeText(this, "Quyền gọi điện bị từ chối", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDialpad()
        setupCallButton()
    }

    private fun setupDialpad() {
        val buttons = arrayOf(
            binding.button0, binding.button1, binding.button2, binding.button3,
            binding.button4, binding.button5, binding.button6, binding.button7,
            binding.button8, binding.button9, binding.buttonStar, binding.buttonHash
        )

        val phoneNumberEditText = binding.phoneNumberEditTextDial

        buttons.forEachIndexed { index, button ->
            val digit = when (index) {
                9 -> "0"
                10 -> "*"
                11 -> "#"
                else -> (index + 1).toString()
            }

            button.setOnClickListener {
                phoneNumberEditText.append(digit)
            }
        }
    }

    private fun setupCallButton() {
        binding.callButton.setOnClickListener {
            val phoneNumber = binding.phoneNumberEditTextDial.text.toString()
            if (phoneNumber.isNotEmpty()) {
                checkPermissionAndMakeCall(phoneNumber)
            } else {
                Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionAndMakeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Yêu cầu quyền sử dụng Activity Result API
            requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        } else {
            // Đã có quyền, thực hiện cuộc gọi
            makePhoneCall(phoneNumber)
        }
    }

    private fun makePhoneCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Không thể thực hiện cuộc gọi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Không cần phương thức này nữa khi sử dụng Activity Result API
    // @Override
    // public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    //     ...
    // }
}
