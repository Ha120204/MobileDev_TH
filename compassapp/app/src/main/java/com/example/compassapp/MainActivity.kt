package com.example.compassapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.lang.Math.toDegrees

class MainActivity : AppCompatActivity(), SensorEventListener {

    // Khai báo SensorManager và các Sensor
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    // Khai báo các TextView để hiển thị thông tin
    private lateinit var tvDirection: TextView
    private lateinit var tvDegree: TextView
    private lateinit var tvCalibration: TextView

    // Khai báo ImageView để hiển thị la bàn
    private lateinit var ivCompassNeedle: ImageView

    // Khai báo Button hiệu chuẩn
    private lateinit var btnCalibrate: Button

    // Mảng lưu trữ giá trị cảm biến
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    // Mảng ma trận xoay và ma trận định hướng
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Biến lưu góc hiện tại
    private var currentDegree = 0f

    // Biến kiểm tra trạng thái cảm biến
    private var isSensorAvailable = true
    private var isCalibrating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo các TextView
        tvDirection = findViewById(R.id.tvDirection)
        tvDegree = findViewById(R.id.tvDegree)
        tvCalibration = findViewById(R.id.tvCalibration)

        // Khởi tạo ImageView
        ivCompassNeedle = findViewById(R.id.ivCompassNeedle)

        // Khởi tạo Button
        btnCalibrate = findViewById(R.id.btnCalibrate)

        // Lấy SensorManager và các cảm biến
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Kiểm tra xem thiết bị có cảm biến cần thiết không
        if (accelerometer == null || magnetometer == null) {
            isSensorAvailable = false
            Toast.makeText(this, "Thiết bị không có cảm biến cần thiết cho la bàn!", Toast.LENGTH_LONG).show()
            tvCalibration.text = "Thiết bị không hỗ trợ la bàn"
            btnCalibrate.isEnabled = false
        }

        // Thiết lập sự kiện cho nút hiệu chuẩn
        btnCalibrate.setOnClickListener {
            if (isSensorAvailable) {
                isCalibrating = true
                tvCalibration.text = "Đang hiệu chuẩn... Xoay thiết bị theo hình số 8"
                Toast.makeText(this, "Đang hiệu chuẩn la bàn...", Toast.LENGTH_SHORT).show()

                // Sau 5 giây, kết thúc hiệu chuẩn
                ivCompassNeedle.postDelayed({
                    isCalibrating = false
                    tvCalibration.text = "Hiệu chuẩn hoàn tất"
                    Toast.makeText(this, "Hiệu chuẩn hoàn tất!", Toast.LENGTH_SHORT).show()
                }, 5000)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Đăng ký lắng nghe sự kiện cảm biến khi activity được resume
        if (isSensorAvailable) {
            accelerometer?.let {
                sensorManager.registerListener(
                    this,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }

            magnetometer?.let {
                sensorManager.registerListener(
                    this,
                    it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Hủy đăng ký lắng nghe sự kiện cảm biến khi activity bị pause
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Xử lý dữ liệu từ cảm biến
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }

        // Cập nhật hướng la bàn
        updateOrientationAngles()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Xử lý khi độ chính xác của cảm biến thay đổi
        if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            when (accuracy) {
                SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                    tvCalibration.text = "Cảm biến không đáng tin cậy, cần hiệu chuẩn"
                }
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                    tvCalibration.text = "Độ chính xác cảm biến thấp, cần hiệu chuẩn"
                }
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                    tvCalibration.text = "Độ chính xác cảm biến trung bình"
                }
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                    tvCalibration.text = "Độ chính xác cảm biến cao"
                }
            }
        }
    }

    private fun updateOrientationAngles() {
        // Cập nhật ma trận xoay từ dữ liệu cảm biến
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // Lấy góc định hướng từ ma trận xoay
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // Chuyển đổi góc từ radian sang độ và lấy góc azimuth (hướng bắc)
        // Azimuth: góc giữa hướng bắc từ tính và hướng thiết bị
        val azimuthInRadians = orientationAngles[0]
        val azimuthInDegrees = (toDegrees(azimuthInRadians.toDouble()) + 360) % 360

        // Cập nhật TextView hiển thị góc
        tvDegree.text = String.format("Góc: %.1f°", azimuthInDegrees)

        // Cập nhật TextView hiển thị hướng
        val direction = getDirectionFromDegree(azimuthInDegrees.toFloat())
        tvDirection.text = "Hướng: $direction"

        // Tạo hiệu ứng xoay cho kim la bàn
        val rotateAnimation = RotateAnimation(
            currentDegree,
            -azimuthInDegrees.toFloat(),
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )

        // Thiết lập thời gian và kiểu nội suy cho hiệu ứng
        rotateAnimation.duration = 250
        rotateAnimation.fillAfter = true

        // Áp dụng hiệu ứng xoay cho kim la bàn
        ivCompassNeedle.startAnimation(rotateAnimation)

        // Cập nhật góc hiện tại
        currentDegree = -azimuthInDegrees.toFloat()
    }

    private fun getDirectionFromDegree(degree: Float): String {
        return when {
            degree >= 337.5 || degree < 22.5 -> "Bắc"
            degree >= 22.5 && degree < 67.5 -> "Đông Bắc"
            degree >= 67.5 && degree < 112.5 -> "Đông"
            degree >= 112.5 && degree < 157.5 -> "Đông Nam"
            degree >= 157.5 && degree < 202.5 -> "Nam"
            degree >= 202.5 && degree < 247.5 -> "Tây Nam"
            degree >= 247.5 && degree < 292.5 -> "Tây"
            degree >= 292.5 && degree < 337.5 -> "Tây Bắc"
            else -> "Không xác định"
        }
    }
}
