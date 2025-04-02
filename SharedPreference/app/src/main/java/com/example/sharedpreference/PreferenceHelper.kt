package com.example.sharedpreference

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {

    private val PREF_NAME = "UserPrefs"
    private val KEY_USER_COUNT = "user_count"
    private val KEY_USERNAME_PREFIX = "username_"
    private val KEY_PASSWORD_PREFIX = "password_"

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // Lưu thông tin người dùng
    fun saveUserCredentials(username: String, password: String) {
        val userCount = sharedPreferences.getInt(KEY_USER_COUNT, 0)
        val newUserIndex = userCount + 1

        val editor = sharedPreferences.edit()
        editor.putString("$KEY_USERNAME_PREFIX$newUserIndex", username)
        editor.putString("$KEY_PASSWORD_PREFIX$newUserIndex", password)
        editor.putInt(KEY_USER_COUNT, newUserIndex)
        editor.apply()
    }

    // Xóa tất cả thông tin người dùng
    fun clearAllUserCredentials() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    // Xóa thông tin của một người dùng cụ thể
    fun clearUserCredentials(userIndex: Int) {
        val editor = sharedPreferences.edit()
        editor.remove("$KEY_USERNAME_PREFIX$userIndex")
        editor.remove("$KEY_PASSWORD_PREFIX$userIndex")
        editor.apply()

        // Cập nhật lại danh sách người dùng để không có khoảng trống
        reorganizeUsers()
    }

    // Sắp xếp lại danh sách người dùng sau khi xóa
    private fun reorganizeUsers() {
        val userCount = getUserCount()
        val allUsers = getAllUsers()

        // Xóa tất cả thông tin người dùng
        val editor = sharedPreferences.edit()
        for (i in 1..userCount) {
            editor.remove("$KEY_USERNAME_PREFIX$i")
            editor.remove("$KEY_PASSWORD_PREFIX$i")
        }
        editor.remove(KEY_USER_COUNT)
        editor.apply()

        // Lưu lại thông tin người dùng với chỉ số mới
        editor.putInt(KEY_USER_COUNT, allUsers.size)
        for ((index, user) in allUsers.withIndex()) {
            val userIndex = index + 1
            editor.putString("$KEY_USERNAME_PREFIX$userIndex", user.first)
            editor.putString("$KEY_PASSWORD_PREFIX$userIndex", user.second)
        }
        editor.apply()
    }

    // Lấy tên người dùng theo index
    fun getUsername(userIndex: Int): String {
        return sharedPreferences.getString("$KEY_USERNAME_PREFIX$userIndex", "") ?: ""
    }

    // Lấy mật khẩu theo index
    fun getPassword(userIndex: Int): String {
        return sharedPreferences.getString("$KEY_PASSWORD_PREFIX$userIndex", "") ?: ""
    }

    // Lấy tổng số người dùng
    fun getUserCount(): Int {
        return sharedPreferences.getInt(KEY_USER_COUNT, 0)
    }

    // Kiểm tra xem đã có thông tin người dùng chưa
    fun hasUserCredentials(): Boolean {
        return getUserCount() > 0
    }

    // Lấy thông tin tất cả người dùng
    fun getAllUsers(): List<Pair<String, String>> {
        val userCount = getUserCount()
        val users = mutableListOf<Pair<String, String>>()

        for (i in 1..userCount) {
            val username = getUsername(i)
            val password = getPassword(i)

            // Chỉ thêm vào danh sách nếu cả username và password đều tồn tại
            if (username.isNotEmpty() && password.isNotEmpty()) {
                users.add(Pair(username, password))
            }
        }

        return users
    }

    // Lấy thông tin tất cả người dùng kèm theo chỉ số
    fun getAllUsersWithIndices(): List<Triple<Int, String, String>> {
        val userCount = getUserCount()
        val users = mutableListOf<Triple<Int, String, String>>()

        for (i in 1..userCount) {
            val username = getUsername(i)
            val password = getPassword(i)

            // Chỉ thêm vào danh sách nếu cả username và password đều tồn tại
            if (username.isNotEmpty() && password.isNotEmpty()) {
                users.add(Triple(i, username, password))
            }
        }

        return users
    }
}
