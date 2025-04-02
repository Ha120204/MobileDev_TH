package com.example.firebaseauthdemo

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    // Khai báo các biến UI
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: Button
    private lateinit var btnShowData: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvUserData: TextView
    private lateinit var progressBar: ProgressBar

    // Khai báo biến Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo các biến UI
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnLogin = findViewById(R.id.btnLogin)
        btnShowData = findViewById(R.id.btnShowData)
        tvStatus = findViewById(R.id.tvStatus)
        tvUserData = findViewById(R.id.tvUserData)
        progressBar = findViewById(R.id.progressBar)

        // Khởi tạo Firebase Auth và Database
        auth = Firebase.auth
        database = Firebase.database.reference

        // Kiểm tra trạng thái đăng nhập
        checkLoginStatus()

        // Thiết lập các sự kiện click cho các nút
        setupButtonListeners()

        // Thêm hiệu ứng animation cho các nút
        setupButtonAnimations()

        // Thiết lập TextWatcher để kiểm tra định dạng email và mật khẩu
        setupTextWatchers()
    }

    private fun checkLoginStatus() {
        // Kiểm tra xem người dùng đã đăng nhập hay chưa
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvStatus.text = "Trạng thái: Đã đăng nhập với ${currentUser.email}"
        } else {
            tvStatus.text = "Trạng thái: Chưa đăng nhập"
        }
    }

    private fun setupButtonListeners() {
        // Xử lý sự kiện khi nhấn nút Đăng ký
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Kiểm tra email và password không được để trống
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Thực hiện đăng ký với Firebase
            registerUser(email, password)
        }

        // Xử lý sự kiện khi nhấn nút Đăng nhập
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Kiểm tra email và password không được để trống
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Thực hiện đăng nhập với Firebase
            loginUser(email, password)
        }

        // Xử lý sự kiện khi nhấn nút Hiển thị dữ liệu
        btnShowData.setOnClickListener {
            // Đọc dữ liệu từ Firebase Realtime Database
            fetchUserData()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtonAnimations() {
        // Thêm hiệu ứng cho nút Đăng ký
        btnRegister.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val anim = AnimationUtils.loadAnimation(this, R.anim.button_click)
                    v.startAnimation(anim)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.clearAnimation()
                }
            }
            false
        }

        // Thêm hiệu ứng cho nút Đăng nhập
        btnLogin.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val anim = AnimationUtils.loadAnimation(this, R.anim.button_click)
                    v.startAnimation(anim)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.clearAnimation()
                }
            }
            false
        }

        // Thêm hiệu ứng cho nút Hiển thị dữ liệu
        btnShowData.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val anim = AnimationUtils.loadAnimation(this, R.anim.button_click)
                    v.startAnimation(anim)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.clearAnimation()
                }
            }
            false
        }
    }

    private fun setupTextWatchers() {
        // Thêm TextWatcher cho trường Email
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.isNotEmpty() && !isValidEmail(s.toString())) {
                    etEmail.error = "Email không đúng định dạng"
                } else {
                    etEmail.error = null
                }
            }
        })

        // Thêm TextWatcher cho trường Password
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.isNotEmpty() && s.length < 6) {
                    etPassword.error = "Mật khẩu phải có ít nhất 6 ký tự"
                } else {
                    etPassword.error = null
                }
            }
        })
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun registerUser(email: String, password: String) {
        // Kiểm tra định dạng email
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Địa chỉ email không đúng định dạng", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra độ dài mật khẩu
        if (password.length < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra kết nối internet
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Không có kết nối Internet. Vui lòng kiểm tra lại.",
                Toast.LENGTH_SHORT).show()
            return
        }

        // Hiển thị ProgressBar
        progressBar.visibility = View.VISIBLE

        // Đăng ký người dùng mới với Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Ẩn ProgressBar
                progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    // Đăng ký thành công
                    val user = auth.currentUser
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()

                    // Cập nhật trạng thái đăng nhập
                    checkLoginStatus()

                    // Lưu thông tin người dùng vào Realtime Database
                    saveUserData(user?.uid, email)

                    // Xóa nội dung các trường nhập liệu
                    clearInputFields()
                } else {
                    // Đăng ký thất bại
                    Log.e("FirebaseAuth", "Đăng ký thất bại", task.exception)

                    // Hiển thị thông báo lỗi cụ thể
                    when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this, "Email không đúng định dạng", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthUserCollisionException -> {
                            Toast.makeText(this, "Email đã được sử dụng bởi tài khoản khác", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthWeakPasswordException -> {
                            Toast.makeText(this, "Mật khẩu quá yếu", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Đăng ký thất bại: ${task.exception?.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        // Kiểm tra định dạng email
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Địa chỉ email không đúng định dạng", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra mật khẩu không được để trống
        if (password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show()
            return
        }

        // Kiểm tra kết nối internet
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Không có kết nối Internet. Vui lòng kiểm tra lại.",
                Toast.LENGTH_SHORT).show()
            return
        }

        // Hiển thị ProgressBar
        progressBar.visibility = View.VISIBLE

        // Đăng nhập với Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Ẩn ProgressBar
                progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    // Đăng nhập thành công
                    val user = auth.currentUser
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                    // Cập nhật trạng thái đăng nhập
                    checkLoginStatus()

                    // Lưu thông tin người dùng vào Realtime Database (cập nhật lần đăng nhập)
                    updateUserLoginTime(user?.uid)

                    // Xóa nội dung các trường nhập liệu
                    clearInputFields()

                    // Tự động hiển thị dữ liệu người dùng
                    fetchUserData()
                } else {
                    // Đăng nhập thất bại
                    Log.e("FirebaseAuth", "Đăng nhập thất bại", task.exception)

                    // Hiển thị thông báo lỗi cụ thể
                    when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this, "Email hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthInvalidUserException -> {
                            Toast.makeText(this, "Tài khoản không tồn tại", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Đăng nhập thất bại: ${task.exception?.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun clearInputFields() {
        // Xóa nội dung các trường nhập liệu
        etEmail.text.clear()
        etPassword.text.clear()
    }

    private fun saveUserData(userId: String?, email: String) {
        // Lưu thông tin người dùng vào Realtime Database
        userId?.let {
            val user = HashMap<String, Any>()
            user["email"] = email
            user["createdAt"] = System.currentTimeMillis()
            user["lastLogin"] = System.currentTimeMillis()

            database.child("users").child(it).setValue(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "Lưu thông tin người dùng thành công",
                        Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseDatabase", "Lỗi khi lưu thông tin", e)
                    Toast.makeText(this, "Lỗi khi lưu thông tin: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateUserLoginTime(userId: String?) {
        // Cập nhật thời gian đăng nhập gần nhất
        userId?.let {
            val updates = HashMap<String, Any>()
            updates["lastLogin"] = System.currentTimeMillis()

            database.child("users").child(it).updateChildren(updates)
                .addOnFailureListener { e ->
                    Log.e("FirebaseDatabase", "Lỗi khi cập nhật thời gian đăng nhập", e)
                }
        }
    }

    private fun fetchUserData() {
        try {
            // Kiểm tra kết nối internet
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "Không có kết nối Internet. Vui lòng kiểm tra lại.",
                    Toast.LENGTH_SHORT).show()
                return
            }

            // Hiển thị ProgressBar
            progressBar.visibility = View.VISIBLE

            // Kiểm tra xem người dùng đã đăng nhập chưa
            val currentUser = auth.currentUser
            if (currentUser == null) {
                // Ẩn ProgressBar
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Vui lòng đăng nhập trước khi xem dữ liệu",
                    Toast.LENGTH_SHORT).show()
                return
            }

            // Thêm timeout để tránh load mãi
            val handler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (progressBar.visibility == View.VISIBLE) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Quá thời gian kết nối đến Firebase. Vui lòng thử lại sau.",
                        Toast.LENGTH_LONG).show()
                }
            }

            // Đặt timeout 10 giây
            handler.postDelayed(timeoutRunnable, 10000)

            // Log đường dẫn để debug
            val userPath = "users/${currentUser.uid}"
            Log.d("FirebaseDebug", "Đang truy cập đường dẫn: $userPath")

            // Đọc dữ liệu từ Firebase Realtime Database
            database.child("users").child(currentUser.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Hủy timeout handler
                        handler.removeCallbacks(timeoutRunnable)

                        // Ẩn ProgressBar
                        progressBar.visibility = View.GONE

                        if (snapshot.exists()) {
                            // Hiển thị dữ liệu người dùng
                            val email = snapshot.child("email").getValue(String::class.java)
                            val createdAt = snapshot.child("createdAt").getValue(Long::class.java)
                            val lastLogin = snapshot.child("lastLogin").getValue(Long::class.java)

                            val userData = "Email: $email\n" +
                                    "Tạo tài khoản: ${formatTimestamp(createdAt)}\n" +
                                    "Đăng nhập gần nhất: ${formatTimestamp(lastLogin)}"

                            tvUserData.text = userData

                            // Thêm hiệu ứng fade in
                            val fadeIn = AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)
                            tvUserData.startAnimation(fadeIn)
                        } else {
                            tvUserData.text = "Không tìm thấy dữ liệu người dùng"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Hủy timeout handler
                        handler.removeCallbacks(timeoutRunnable)

                        // Ẩn ProgressBar
                        progressBar.visibility = View.GONE

                        Log.e("FirebaseDatabase", "Lỗi khi đọc dữ liệu", error.toException())
                        Toast.makeText(this@MainActivity, "Lỗi khi đọc dữ liệu: ${error.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                })
        } catch (e: Exception) {
            // Xử lý ngoại lệ không mong muốn
            progressBar.visibility = View.GONE
            Log.e("FirebaseError", "Lỗi không xác định", e)
            Toast.makeText(this, "Đã xảy ra lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTimestamp(timestamp: Long?): String {
        // Định dạng timestamp thành chuỗi ngày giờ
        return if (timestamp != null) {
            val date = java.util.Date(timestamp)
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
            sdf.format(date)
        } else {
            "Không xác định"
        }
    }

    // Thêm phương thức đăng xuất (tùy chọn)
    private fun logoutUser() {
        auth.signOut()
        checkLoginStatus()
        tvUserData.text = "Dữ liệu người dùng sẽ hiển thị ở đây"
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
    }
}
