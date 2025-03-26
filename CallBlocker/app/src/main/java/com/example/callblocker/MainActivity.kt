package com.example.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.callblocker.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var blockedNumbersManager: BlockedNumbersManager
    private lateinit var adapter: BlockedNumberAdapter
    private val callReceiver = CallReceiver()

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.MANAGE_OWN_CALLS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.CALL_PHONE
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            registerCallReceiver()
            setupPhoneAccount()
        } else {
            Toast.makeText(
                this,
                getString(R.string.permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkDefaultDialerStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        blockedNumbersManager = BlockedNumbersManager(this)

        setupRecyclerView()
        setupAddButton()
        setupDefaultDialerButton()
        checkAndRequestPermissions()
    }

    private fun setupRecyclerView() {
        val blockedNumbers = blockedNumbersManager.getBlockedNumbers()
        adapter = BlockedNumberAdapter(blockedNumbers) { position ->
            val number = blockedNumbers[position]
            blockedNumbersManager.removeBlockedNumber(number)
            blockedNumbers.removeAt(position)
            adapter.notifyItemRemoved(position)
        }

        binding.blockedNumbersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            contentDescription = getString(R.string.blocked_numbers_list)
        }
    }

    private fun setupAddButton() {
        binding.addButton.setOnClickListener {
            val phoneNumber = binding.phoneNumberEditText.text.toString().trim()

            if (phoneNumber.isNotEmpty()) {
                blockedNumbersManager.addBlockedNumber(phoneNumber)

                val blockedNumbers = blockedNumbersManager.getBlockedNumbers()
                adapter = BlockedNumberAdapter(blockedNumbers) { position ->
                    val number = blockedNumbers[position]
                    blockedNumbersManager.removeBlockedNumber(number)
                    blockedNumbers.removeAt(position)
                    adapter.notifyItemRemoved(position)
                }

                binding.blockedNumbersRecyclerView.adapter = adapter
                binding.phoneNumberEditText.text.clear()

                Toast.makeText(
                    this,
                    getString(R.string.number_added, phoneNumber),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.please_enter_number),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupDefaultDialerButton() {
        binding.setDefaultDialerButton.setOnClickListener {
            requestDefaultDialerRole()
        }
    }

    private fun requestDefaultDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Sử dụng RoleManager cho Android 10+
            val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            defaultDialerLauncher.launch(intent)
        } else {
            // Sử dụng phương pháp cũ cho Android 9 và thấp hơn
            try {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback nếu intent không khả dụng
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        "Vui lòng chọn 'Ứng dụng điện thoại' và chọn ứng dụng này",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Không thể mở cài đặt. Vui lòng đặt ứng dụng làm ứng dụng điện thoại mặc định trong cài đặt",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun checkDefaultDialerStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
            val isDefault = telecomManager.defaultDialerPackage == packageName

            binding.defaultDialerInfoLayout.visibility = if (isDefault) View.GONE else View.VISIBLE

            // Hiển thị thông báo trạng thái
            if (isDefault) {
                Toast.makeText(
                    this,
                    "Ứng dụng đã được đặt làm ứng dụng điện thoại mặc định. Tính năng chặn cuộc gọi đã được kích hoạt.",
                    Toast.LENGTH_LONG
                ).show()

                // Hiển thị banner thông báo thành công
                binding.successBanner.visibility = View.VISIBLE
            } else {
                binding.successBanner.visibility = View.GONE
            }
        }
    }

    private fun setupPhoneAccount() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val phoneAccountHelper = PhoneAccountHelper(this)
            phoneAccountHelper.registerPhoneAccount()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            registerCallReceiver()
            setupPhoneAccount()
            checkDefaultDialerStatus()
        }
    }

    private fun registerCallReceiver() {
        val intentFilter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(callReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()
        checkDefaultDialerStatus()
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
