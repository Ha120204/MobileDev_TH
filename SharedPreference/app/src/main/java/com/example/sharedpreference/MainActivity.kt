package com.example.sharedpreference

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private lateinit var edtUser: EditText
    private lateinit var edtPass: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnDisplay: Button
    private lateinit var tvResult: TextView
    private lateinit var layoutUserList: LinearLayout

    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo PreferenceHelper
        preferenceHelper = PreferenceHelper(this)

        // Ánh xạ các thành phần giao diện
        edtUser = findViewById(R.id.edtUser)
        edtPass = findViewById(R.id.edtPass)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)
        btnDisplay = findViewById(R.id.btnDisplay)
        tvResult = findViewById(R.id.tvResult)
        layoutUserList = findViewById(R.id.layoutUserList)

        // Thiết lập sự kiện cho nút Lưu
        btnSave.setOnClickListener {
            val username = edtUser.text.toString().trim()
            val password = edtPass.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lưu thông tin vào SharedPreferences
            preferenceHelper.saveUserCredentials(username, password)
            Toast.makeText(this, "Đã lưu thông tin thành công", Toast.LENGTH_SHORT).show()

            // Xóa nội dung trong EditText
            clearInputFields()

            // Cập nhật hiển thị
            displayAllUsers()
        }

        // Thiết lập sự kiện cho nút Xóa
        btnDelete.setOnClickListener {
            showDeleteOptions()
        }

        // Thiết lập sự kiện cho nút Hiển thị
        btnDisplay.setOnClickListener {
            displayAllUsers()
        }

        // Hiển thị danh sách người dùng ban đầu
        displayAllUsers()
    }

    private fun clearInputFields() {
        edtUser.text.clear()
        edtPass.text.clear()
    }

    private fun displayAllUsers() {
        val allUsers = preferenceHelper.getAllUsersWithIndices()

        // Hiển thị tổng quan trong tvResult
        if (allUsers.isNotEmpty()) {
            val resultText = "Có ${allUsers.size} người dùng được lưu trữ"
            tvResult.text = resultText

            // Xóa danh sách cũ
            layoutUserList.removeAllViews()

            // Hiển thị danh sách chi tiết với nút xóa cho từng người dùng
            for (user in allUsers) {
                val userView = createUserViewWithDeleteButton(user.first, user.second, user.third)
                layoutUserList.addView(userView)
            }
        } else {
            tvResult.text = "Không có thông tin người dùng nào được lưu trữ"
            layoutUserList.removeAllViews()
        }
    }

    private fun createUserViewWithDeleteButton(index: Int, username: String, password: String): View {
        // Tạo layout cho mỗi người dùng
        val userLayout = LinearLayout(this)
        userLayout.orientation = LinearLayout.HORIZONTAL
        userLayout.setPadding(16, 16, 16, 16)

        // Layout cho thông tin người dùng (bên trái)
        val infoLayout = LinearLayout(this)
        infoLayout.orientation = LinearLayout.VERTICAL
        infoLayout.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )

        // Thêm TextView cho thông tin người dùng
        val tvUserInfo = TextView(this)
        tvUserInfo.text = "Người dùng $index:\nTên: $username\nMật khẩu: $password"
        tvUserInfo.textSize = 16f
        infoLayout.addView(tvUserInfo)

        // Thêm infoLayout vào userLayout
        userLayout.addView(infoLayout)

        // Tạo nút xóa (bên phải)
        val btnDeleteUser = Button(this)
        btnDeleteUser.text = "Xóa"
        btnDeleteUser.setOnClickListener {
            confirmDeleteUser(index)
        }

        // Thiết lập layout params cho nút xóa
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonParams.setMargins(8, 0, 0, 0)
        btnDeleteUser.layoutParams = buttonParams

        // Thêm nút xóa vào userLayout
        userLayout.addView(btnDeleteUser)

        // Tạo container cho userLayout và đường phân cách
        val containerLayout = LinearLayout(this)
        containerLayout.orientation = LinearLayout.VERTICAL

        // Thêm userLayout vào container
        containerLayout.addView(userLayout)

        // Thêm đường phân cách
        val divider = View(this)
        divider.setBackgroundColor(0xFFCCCCCC.toInt())
        val dividerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        )
        dividerParams.setMargins(0, 8, 0, 8)
        divider.layoutParams = dividerParams
        containerLayout.addView(divider)

        return containerLayout
    }

    private fun showDeleteOptions() {
        val options = arrayOf("Xóa tất cả người dùng", "Hủy")

        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> confirmDeleteAllUsers()
                    1 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun confirmDeleteAllUsers() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa tất cả thông tin người dùng?")
            .setPositiveButton("Xóa") { _, _ ->
                preferenceHelper.clearAllUserCredentials()
                Toast.makeText(this, "Đã xóa tất cả thông tin thành công", Toast.LENGTH_SHORT).show()
                displayAllUsers()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmDeleteUser(userIndex: Int) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa người dùng này?")
            .setPositiveButton("Xóa") { _, _ ->
                preferenceHelper.clearUserCredentials(userIndex)
                Toast.makeText(this, "Đã xóa thông tin người dùng thành công", Toast.LENGTH_SHORT).show()
                displayAllUsers()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
