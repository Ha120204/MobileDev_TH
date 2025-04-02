package com.example.accelerometerapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    // Khai báo SensorManager và Sensor
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Khai báo các TextView để hiển thị giá trị gia tốc
    private lateinit var tvXAxis: TextView
    private lateinit var tvYAxis: TextView
    private lateinit var tvZAxis: TextView
    private lateinit var tvTotalAcceleration: TextView

    // Khai báo các thành phần UI để hiển thị quả bóng
    private lateinit var ballContainer: FrameLayout
    private lateinit var ivBall: ImageView

    // Các biến để tính toán vị trí của quả bóng
    private var xPos = 0f
    private var yPos = 0f
    private var containerWidth = 0
    private var containerHeight = 0
    private var ballWidth = 0
    private var ballHeight = 0

    // Hệ số giảm chấn và độ nhạy
    private val dampingFactor = 0.8f
    private val sensitivity = 5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo các TextView
        tvXAxis = findViewById(R.id.tvXAxis)
        tvYAxis = findViewById(R.id.tvYAxis)
        tvZAxis = findViewById(R.id.tvZAxis)
        tvTotalAcceleration = findViewById(R.id.tvTotalAcceleration)

        // Khởi tạo các thành phần UI cho quả bóng
        ballContainer = findViewById(R.id.ballContainer)
        ivBall = findViewById(R.id.ivBall)

        // Lấy SensorManager và cảm biến gia tốc
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Kiểm tra xem thiết bị có cảm biến gia tốc không
        if (accelerometer == null) {
            Toast.makeText(this, "Thiết bị không có cảm biến gia tốc!", Toast.LENGTH_LONG).show()
            finish()
        }

        // Thiết lập kích thước ban đầu cho quả bóng
        ivBall.post {
            ballWidth = ivBall.width
            ballHeight = ivBall.height
        }

        // Thiết lập kích thước container
        ballContainer.post {
            containerWidth = ballContainer.width
            containerHeight = ballContainer.height

            // Đặt quả bóng ở giữa container
            xPos = (containerWidth - ballWidth) / 2f
            yPos = (containerHeight - ballHeight) / 2f
            updateBallPosition()
        }
    }

    override fun onResume() {
        super.onResume()
        // Đăng ký lắng nghe sự kiện cảm biến khi activity được resume
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        // Hủy đăng ký lắng nghe sự kiện cảm biến khi activity bị pause
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Lấy giá trị gia tốc từ sự kiện cảm biến
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Tính gia tốc tổng
            val totalAcceleration = sqrt(x*x + y*y + z*z)

            // Cập nhật TextView để hiển thị giá trị gia tốc
            tvXAxis.text = String.format("Trục X: %.2f m/s²", x)
            tvYAxis.text = String.format("Trục Y: %.2f m/s²", y)
            tvZAxis.text = String.format("Trục Z: %.2f m/s²", z)
            tvTotalAcceleration.text = String.format("Gia tốc tổng: %.2f m/s²", totalAcceleration)

            // Cập nhật vị trí của quả bóng dựa trên giá trị gia tốc
            // Lưu ý: Chúng ta đảo ngược giá trị x và y để quả bóng di chuyển theo hướng nghiêng của thiết bị
            updateBallPosition(x, y)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Xử lý khi độ chính xác của cảm biến thay đổi (không cần thiết cho ứng dụng này)
    }

    private fun updateBallPosition(accelerationX: Float = 0f, accelerationY: Float = 0f) {
        // Tính toán vị trí mới của quả bóng dựa trên gia tốc
        // Nhân với sensitivity để điều chỉnh độ nhạy của chuyển động
        // Nhân với dampingFactor để tạo hiệu ứng giảm chấn (làm cho chuyển động mượt mà hơn)
        xPos -= accelerationX * sensitivity * dampingFactor
        yPos += accelerationY * sensitivity * dampingFactor

        // Giới hạn vị trí của quả bóng trong container
        xPos = xPos.coerceIn(0f, (containerWidth - ballWidth).toFloat())
        yPos = yPos.coerceIn(0f, (containerHeight - ballHeight).toFloat())

        // Cập nhật vị trí của quả bóng trên giao diện
        ivBall.translationX = xPos
        ivBall.translationY = yPos
    }
}
