package com.example.callblocker

import android.content.Context
import android.content.SharedPreferences

class BlockedNumbersManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "blocked_numbers", Context.MODE_PRIVATE
    )

    fun getBlockedNumbers(): MutableList<String> {
        val blockedNumbersSet = sharedPreferences.getStringSet(BLOCKED_NUMBERS_KEY, HashSet())
        return blockedNumbersSet?.toMutableList() ?: mutableListOf()
    }

    fun addBlockedNumber(phoneNumber: String) {
        val currentNumbers = getBlockedNumbers()
        if (!currentNumbers.contains(phoneNumber)) {
            currentNumbers.add(phoneNumber)
            saveBlockedNumbers(currentNumbers)
        }
    }

    fun removeBlockedNumber(phoneNumber: String) {
        val currentNumbers = getBlockedNumbers()
        currentNumbers.remove(phoneNumber)
        saveBlockedNumbers(currentNumbers)
    }

    fun isNumberBlocked(phoneNumber: String): Boolean {
        // Chuẩn hóa số điện thoại trước khi kiểm tra
        val normalizedNumber = normalizePhoneNumber(phoneNumber)

        return getBlockedNumbers().any { blockedNumber ->
            val normalizedBlockedNumber = normalizePhoneNumber(blockedNumber)
            normalizedNumber.endsWith(normalizedBlockedNumber) || normalizedBlockedNumber.endsWith(normalizedNumber)
        }
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Loại bỏ các ký tự không phải số
        return phoneNumber.replace(Regex("[^0-9+]"), "")
    }

    private fun saveBlockedNumbers(numbers: List<String>) {
        val editor = sharedPreferences.edit()
        editor.putStringSet(BLOCKED_NUMBERS_KEY, numbers.toSet())
        editor.apply()
    }

    companion object {
        private const val BLOCKED_NUMBERS_KEY = "blocked_numbers_key"
    }
}
