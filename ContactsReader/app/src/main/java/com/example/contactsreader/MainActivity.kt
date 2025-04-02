package com.example.contactsreader

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var listViewContacts: ListView
    private lateinit var contactsList: ArrayList<String>
    private lateinit var contactsAdapter: ArrayAdapter<String>

    private val REQUEST_READ_CONTACTS_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo các thành phần UI
        listViewContacts = findViewById(R.id.listViewContacts)
        contactsList = ArrayList()

        // Kiểm tra và xin quyền READ_CONTACTS
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Nếu chưa có quyền, yêu cầu quyền từ người dùng
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_READ_CONTACTS_PERMISSION
            )
        } else {
            // Nếu đã có quyền, tải danh sách liên hệ
            loadContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_CONTACTS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Người dùng đã cấp quyền, tải danh sách liên hệ
                loadContacts()
            } else {
                // Người dùng từ chối cấp quyền
                Toast.makeText(this, "Bạn cần cấp quyền để xem danh bạ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadContacts() {
        // Lấy dữ liệu từ Content Provider
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " ASC" // Sắp xếp theo tên
        )

        cursor?.use {
            if (it.count > 0) {
                while (it.moveToNext()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    // Kiểm tra nếu cột tồn tại
                    if (nameIndex != -1) {
                        val name = it.getString(nameIndex)
                        name?.let { contactName ->
                            contactsList.add(contactName)
                        }
                    }
                }
            }
        }

        // Hiển thị dữ liệu lên ListView
        contactsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, contactsList)
        listViewContacts.adapter = contactsAdapter
    }
}
