package com.example.tcpclient

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val TAG = "TCPClient"
    private lateinit var etServerAddress: EditText
    private lateinit var etServerPort: EditText
    private lateinit var btnConnect: Button
    private lateinit var tvMessages: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var scrollView: ScrollView

    private var clientSocket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var isConnected = false

    private val executorService = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo các thành phần UI
        etServerAddress = findViewById(R.id.etServerAddress)
        etServerPort = findViewById(R.id.etServerPort)
        btnConnect = findViewById(R.id.btnConnect)
        tvMessages = findViewById(R.id.tvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        scrollView = findViewById(R.id.scrollView)

        // Thiết lập sự kiện cho nút Kết nối
        btnConnect.setOnClickListener {
            val serverAddress = etServerAddress.text.toString().trim()
            val serverPortStr = etServerPort.text.toString().trim()

            if (serverAddress.isEmpty() || serverPortStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập địa chỉ và cổng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val serverPort = serverPortStr.toIntOrNull()
            if (serverPort == null || serverPort <= 0 || serverPort > 65535) {
                Toast.makeText(this, "Cổng không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isConnected) {
                disconnectFromServer()
                btnConnect.text = "Kết nối"
                etMessage.isEnabled = false
                btnSend.isEnabled = false
            } else {
                connectToServer(serverAddress, serverPort)
            }
        }

        // Thiết lập sự kiện cho nút Gửi
        btnSend.setOnClickListener {
            sendMessageFromUI()
        }

        // Thêm sự kiện khi nhấn Enter trên bàn phím
        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessageFromUI()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun sendMessageFromUI() {
        val message = etMessage.text.toString().trim()
        if (message.isNotEmpty()) {
            Log.d(TAG, "Đang gửi tin nhắn từ UI: \"$message\"")

            // Kiểm tra kết nối trước khi gửi
            synchronized(this) {
                if (!isConnected || clientSocket == null || clientSocket!!.isClosed) {
                    Log.e(TAG, "Không thể gửi tin nhắn: Kết nối không hợp lệ")
                    Toast.makeText(this, "Không thể gửi tin nhắn: Kết nối đã mất", Toast.LENGTH_SHORT).show()

                    // Đảm bảo UI được cập nhật
                    handler.post {
                        disconnectFromServer()
                        btnConnect.text = "Kết nối"
                        etMessage.isEnabled = false
                        btnSend.isEnabled = false
                    }
                    return
                }

                // Nếu kết nối vẫn ổn, gửi tin nhắn
                sendMessage(message)
                etMessage.text.clear()
            }
        } else {
            Toast.makeText(this, "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show()
        }
    }



    private fun connectToServer(serverAddress: String, serverPort: Int) {
        addMessage("Đang kết nối đến $serverAddress:$serverPort...")

        executorService.execute {
            try {
                Log.d(TAG, "Đang thử kết nối đến $serverAddress:$serverPort")

                // Tạo socket với timeout
                clientSocket = Socket()
                Log.d(TAG, "Đã tạo đối tượng socket")

                // Thiết lập timeout kết nối
                clientSocket!!.connect(InetSocketAddress(serverAddress, serverPort), 5000)
                Log.d(TAG, "Đã kết nối socket thành công")

                // Thiết lập timeout đọc
                clientSocket!!.soTimeout = 10000
                Log.d(TAG, "Đã thiết lập timeout đọc: 10000ms")

                // Tạo reader và writer với UTF-8
                reader = BufferedReader(InputStreamReader(clientSocket!!.getInputStream(), StandardCharsets.UTF_8))
                writer = BufferedWriter(OutputStreamWriter(clientSocket!!.getOutputStream(), StandardCharsets.UTF_8))
                Log.d(TAG, "Đã tạo reader và writer thành công")

                isConnected = true

                // Cập nhật UI khi kết nối thành công
                handler.post {
                    addMessage("Đã kết nối thành công!")
                    btnConnect.text = "Ngắt kết nối"
                    etMessage.isEnabled = true
                    btnSend.isEnabled = true
                }

                // Bắt đầu nhận tin nhắn từ server
                receiveMessages()

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi kết nối: ${e.message}", e)
                handler.post {
                    addMessage("Lỗi kết nối: ${e.message}")
                    disconnectFromServer()
                }
            }
        }
    }

    private fun receiveMessages() {
        try {
            Log.d(TAG, "Bắt đầu lắng nghe tin nhắn từ server")
            while (isConnected && clientSocket != null && !clientSocket!!.isClosed) {
                try {
                    Log.d(TAG, "Đang đợi tin nhắn...")
                    val message = reader?.readLine()
                    Log.d(TAG, "Đã nhận tin nhắn: \"$message\"")

                    if (message != null) {
                        handler.post {
                            addMessage("Server: $message")
                        }
                    } else {
                        // Nếu message là null, server đã đóng kết nối
                        Log.d(TAG, "Nhận được null, server có thể đã đóng kết nối")
                        break
                    }
                } catch (e: SocketTimeoutException) {
                    // Timeout là bình thường, chỉ log ở mức debug
                    Log.d(TAG, "Timeout khi đọc, tiếp tục chờ")

                    // Gửi một gói "ping" đến server để kiểm tra kết nối
                    // Điều này có thể ngăn timeout nếu server vẫn hoạt động
                    try {
                        if (isConnected && writer != null) {
                            writer?.write("PING")
                            writer?.newLine()
                            writer?.flush()
                            Log.d(TAG, "Đã gửi PING đến server")
                        }
                    } catch (pingEx: Exception) {
                        Log.e(TAG, "Lỗi gửi PING: ${pingEx.message}")
                        break  // Kết thúc vòng lặp nếu không thể gửi PING
                    }

                    continue  // Tiếp tục vòng lặp
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi đọc tin nhắn: ${e.message}", e)
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi nhận tin nhắn: ${e.message}", e)
        } finally {
            Log.d(TAG, "Kết thúc nhận tin nhắn")

            // Đảm bảo đánh dấu kết nối đã mất
            val wasConnected = isConnected
            isConnected = false

            // Chỉ ngắt kết nối từ main thread
            handler.post {
                if (wasConnected) {
                    disconnectFromServer()
                    btnConnect.text = "Kết nối"
                    etMessage.isEnabled = false
                    btnSend.isEnabled = false
                }
            }
        }
    }


    private fun sendMessage(message: String) {
        executorService.execute {
            try {
                if (writer == null) {
                    Log.e(TAG, "Lỗi: writer là null")
                    handler.post {
                        Toast.makeText(this, "Lỗi: Không thể gửi tin nhắn - writer null", Toast.LENGTH_SHORT).show()
                    }
                    return@execute
                }

                if (clientSocket == null || clientSocket!!.isClosed) {
                    Log.e(TAG, "Lỗi: Socket đã đóng hoặc null")
                    handler.post {
                        Toast.makeText(this, "Lỗi: Socket đã đóng", Toast.LENGTH_SHORT).show()
                        disconnectFromServer()
                        btnConnect.text = "Kết nối"
                        etMessage.isEnabled = false
                        btnSend.isEnabled = false
                        isConnected = false
                    }
                    return@execute
                }

                Log.d(TAG, "Chuẩn bị gửi tin nhắn: \"$message\"")
                writer?.write(message)
                Log.d(TAG, "Đã gọi writer.write()")
                writer?.newLine()
                Log.d(TAG, "Đã gọi writer.newLine()")
                writer?.flush()
                Log.d(TAG, "Đã gọi writer.flush()")

                handler.post {
                    addMessage("Bạn: $message")
                    Log.d(TAG, "Đã cập nhật UI với tin nhắn đã gửi")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi gửi tin nhắn: ${e.message}", e)

                handler.post {
                    addMessage("Lỗi gửi tin nhắn: ${e.message}")
                    Toast.makeText(this, "Lỗi gửi tin nhắn: ${e.message}", Toast.LENGTH_SHORT).show()

                    // Nếu có lỗi gửi, giả định kết nối đã mất
                    disconnectFromServer()
                    btnConnect.text = "Kết nối"
                    etMessage.isEnabled = false
                    btnSend.isEnabled = false
                    isConnected = false
                }
            }
        }
    }


    private fun disconnectFromServer() {
        try {
            Log.d(TAG, "Đang ngắt kết nối từ server")
            isConnected = false
            reader?.close()
            writer?.close()
            clientSocket?.close()
            Log.d(TAG, "Đã ngắt kết nối thành công")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi ngắt kết nối: ${e.message}", e)
            addMessage("Lỗi khi ngắt kết nối: ${e.message}")
        } finally {
            reader = null
            writer = null
            clientSocket = null
        }
    }

    private fun addMessage(message: String) {
        Log.d(TAG, "Thêm tin nhắn: $message")
        tvMessages.append("$message\n\n")

        // Cuộn xuống để hiển thị tin nhắn mới nhất
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy được gọi, đang dọn dẹp tài nguyên")
        disconnectFromServer()
        executorService.shutdown()
    }
}
