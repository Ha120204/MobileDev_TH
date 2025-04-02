package com.example.timerapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button
    private lateinit var resetButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private var seconds = 0
    private var minutes = 0
    private var hours = 0

    // Sử dụng AtomicBoolean để đảm bảo thread-safety
    private val isRunning = AtomicBoolean(false)
    private var timerThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ánh xạ các thành phần UI
        timerTextView = findViewById(R.id.timerTextView)
        startButton = findViewById(R.id.startButton)
        resetButton = findViewById(R.id.resetButton)

        // Thiết lập sự kiện click cho nút Bắt đầu
        startButton.setOnClickListener {
            if (isRunning.get()) {
                // Nếu đang chạy, dừng lại
                pauseTimer()
                startButton.text = "Bắt đầu"
            } else {
                // Nếu đang dừng, bắt đầu chạy
                startTimer()
                startButton.text = "Tạm dừng"
            }
        }

        // Thiết lập sự kiện click cho nút Đặt lại
        resetButton.setOnClickListener {
            resetTimer()
        }

        // Hiển thị thời gian ban đầu
        updateTimerText()
    }

    private fun startTimer() {
        if (isRunning.compareAndSet(false, true)) {
            // Tạo và khởi động thread mới
            timerThread = Thread {
                try {
                    while (isRunning.get()) {
                        // Cập nhật UI thông qua Handler
                        handler.post {
                            seconds++

                            if (seconds >= 60) {
                                seconds = 0
                                minutes++
                            }

                            if (minutes >= 60) {
                                minutes = 0
                                hours++
                            }

                            updateTimerText()
                        }

                        // Tạm dừng thread trong 1 giây
                        Thread.sleep(1000)
                    }
                } catch (e: InterruptedException) {
                    // Thread bị gián đoạn, không cần xử lý gì thêm
                }
            }

            // Đặt là daemon thread để không ngăn ứng dụng thoát
            timerThread?.isDaemon = true
            timerThread?.start()
        }
    }

    private fun pauseTimer() {
        isRunning.set(false)
        timerThread?.interrupt()
        timerThread = null
    }

    private fun resetTimer() {
        pauseTimer()
        seconds = 0
        minutes = 0
        hours = 0
        updateTimerText()
        startButton.text = "Bắt đầu"
    }

    private fun updateTimerText() {
        // Định dạng thời gian: HH:MM:SS
        val timeString = formatTime(hours, minutes, seconds)
        timerTextView.text = timeString
    }

    private fun formatTime(hours: Int, minutes: Int, seconds: Int): String {
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("seconds", seconds)
        outState.putInt("minutes", minutes)
        outState.putInt("hours", hours)
        outState.putBoolean("isRunning", isRunning.get())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        seconds = savedInstanceState.getInt("seconds")
        minutes = savedInstanceState.getInt("minutes")
        hours = savedInstanceState.getInt("hours")

        updateTimerText()

        if (savedInstanceState.getBoolean("isRunning")) {
            startTimer()
            startButton.text = "Tạm dừng"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Đảm bảo dừng thread khi activity bị hủy
        pauseTimer()
    }
}
