package com.example.udpmessenger

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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val TAG = "UDPMessenger"

    // UI components
    private lateinit var etTargetIP: EditText
    private lateinit var etTargetPort: EditText
    private lateinit var etListenPort: EditText
    private lateinit var btnStartStopListener: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvMessages: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    // Network components
    private var listenerSocket: DatagramSocket? = null
    private var isListening = false
    private var listenerThread: Thread? = null

    // Thread management
    private val executorService: ExecutorService = Executors.newCachedThreadPool()
    private val handler = Handler(Looper.getMainLooper())

    // Date formatter for timestamps
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        etTargetIP = findViewById(R.id.etTargetIP)
        etTargetPort = findViewById(R.id.etTargetPort)
        etListenPort = findViewById(R.id.etListenPort)
        btnStartStopListener = findViewById(R.id.btnStartStopListener)
        tvStatus = findViewById(R.id.tvStatus)
        tvMessages = findViewById(R.id.tvMessages)
        scrollView = findViewById(R.id.scrollView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        // Set default values
        etTargetPort.setText("9876")
        etListenPort.setText("9877")

        // Set up button click listeners
        btnStartStopListener.setOnClickListener {
            if (isListening) {
                stopListener()
            } else {
                startListener()
            }
        }

        btnSend.setOnClickListener {
            sendMessage()
        }

        // Set up enter key listener for message input
        etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun startListener() {
        val portStr = etListenPort.text.toString().trim()
        if (portStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập cổng lắng nghe", Toast.LENGTH_SHORT).show()
            return
        }

        val port = portStr.toIntOrNull()
        if (port == null || port <= 0 || port > 65535) {
            Toast.makeText(this, "Cổng không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        executorService.execute {
            try {
                // Create a new DatagramSocket to listen on the specified port
                listenerSocket = DatagramSocket(port)
                isListening = true

                handler.post {
                    btnStartStopListener.text = "Dừng lắng nghe"
                    tvStatus.text = "Trạng thái: Đang lắng nghe trên cổng $port"
                    addMessage("Bắt đầu lắng nghe trên cổng $port")
                }

                // Start a new thread to listen for incoming packets
                listenerThread = Thread {
                    listenForMessages()
                }
                listenerThread?.start()

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi bắt đầu lắng nghe: ${e.message}", e)
                handler.post {
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    tvStatus.text = "Trạng thái: Lỗi khi lắng nghe"
                    addMessage("Lỗi khi bắt đầu lắng nghe: ${e.message}")
                    isListening = false
                    btnStartStopListener.text = "Bắt đầu lắng nghe"
                }
            }
        }
    }

    private fun listenForMessages() {
        try {
            // Create a buffer for incoming packets
            val buffer = ByteArray(1024)

            while (isListening && listenerSocket != null && !listenerSocket!!.isClosed) {
                // Create a DatagramPacket to receive data
                val packet = DatagramPacket(buffer, buffer.size)

                try {
                    // Wait for a packet to arrive
                    listenerSocket?.receive(packet)

                    // Extract the message from the packet
                    val message = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                    val sender = packet.address.hostAddress

                    Log.d(TAG, "Nhận tin nhắn từ $sender: $message")

                    // Update UI on the main thread
                    handler.post {
                        addMessage("Nhận từ $sender: $message")
                    }
                } catch (e: Exception) {
                    if (isListening) {
                        Log.e(TAG, "Lỗi khi nhận tin nhắn: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi trong luồng lắng nghe: ${e.message}", e)
        } finally {
            // Make sure to close the socket when done
            try {
                listenerSocket?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi đóng socket: ${e.message}", e)
            }

            handler.post {
                if (isListening) {
                    tvStatus.text = "Trạng thái: Đã dừng lắng nghe"
                    addMessage("Đã dừng lắng nghe")
                    isListening = false
                    btnStartStopListener.text = "Bắt đầu lắng nghe"
                }
            }
        }
    }

    private fun stopListener() {
        if (isListening) {
            isListening = false

            executorService.execute {
                try {
                    listenerSocket?.close()
                    listenerThread?.join(1000)  // Wait for the listener thread to finish

                    handler.post {
                        tvStatus.text = "Trạng thái: Đã dừng lắng nghe"
                        addMessage("Đã dừng lắng nghe")
                        btnStartStopListener.text = "Bắt đầu lắng nghe"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi dừng lắng nghe: ${e.message}", e)

                    handler.post {
                        Toast.makeText(this, "Lỗi khi dừng lắng nghe: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val message = etMessage.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show()
            return
        }

        val targetIP = etTargetIP.text.toString().trim()
        if (targetIP.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ IP đích", Toast.LENGTH_SHORT).show()
            return
        }

        val targetPortStr = etTargetPort.text.toString().trim()
        if (targetPortStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập cổng đích", Toast.LENGTH_SHORT).show()
            return
        }

        val targetPort = targetPortStr.toIntOrNull()
        if (targetPort == null || targetPort <= 0 || targetPort > 65535) {
            Toast.makeText(this, "Cổng đích không hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }

        executorService.execute {
            var socket: DatagramSocket? = null

            try {
                // Create a DatagramSocket for sending
                socket = DatagramSocket()

                // Convert the message to bytes
                val messageBytes = message.toByteArray(StandardCharsets.UTF_8)

                // Create the destination address
                val address = InetAddress.getByName(targetIP)

                // Create a DatagramPacket with the message data
                val packet = DatagramPacket(messageBytes, messageBytes.size, address, targetPort)

                // Send the packet
                socket.send(packet)

                Log.d(TAG, "Đã gửi tin nhắn đến $targetIP:$targetPort: $message")

                handler.post {
                    addMessage("Gửi đến $targetIP:$targetPort: $message")
                    etMessage.text.clear()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi gửi tin nhắn: ${e.message}", e)

                handler.post {
                    Toast.makeText(this, "Lỗi khi gửi tin nhắn: ${e.message}", Toast.LENGTH_SHORT).show()
                    addMessage("Lỗi khi gửi tin nhắn: ${e.message}")
                }
            } finally {
                // Close the socket when done
                socket?.close()
            }
        }
    }

    private fun addMessage(message: String) {
        val time = dateFormat.format(Date())
        tvMessages.append("[$time] $message\n\n")

        // Scroll to the bottom to show the latest message
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up resources
        stopListener()
        executorService.shutdown()
    }
}
