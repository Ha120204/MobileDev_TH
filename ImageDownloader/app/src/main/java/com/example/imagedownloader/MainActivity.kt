package com.example.imagedownloader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class MainActivity : ComponentActivity() {

    private lateinit var editTextImageUrl: EditText
    private lateinit var buttonDownload: Button
    private lateinit var imageView: ImageView
    private lateinit var progressBar: ProgressBar
    private val TAG = "ImageDownloader"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ánh xạ các thành phần UI
        editTextImageUrl = findViewById(R.id.editTextImageUrl)
        buttonDownload = findViewById(R.id.buttonDownload)
        imageView = findViewById(R.id.imageView)
        progressBar = findViewById(R.id.progressBar)

        // Thiết lập sự kiện click cho button
        buttonDownload.setOnClickListener {
            val imageUrl = editTextImageUrl.text.toString().trim()
            if (imageUrl.isNotEmpty()) {
                // Tạo và thực thi AsyncTask để tải ảnh
                ImageDownloader().execute(imageUrl)
            } else {
                Toast.makeText(this, "Vui lòng nhập URL ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // AsyncTask để tải ảnh trong background
    private inner class ImageDownloader : AsyncTask<String, Int, Bitmap?>() {
        private var errorMessage: String? = null

        override fun onPreExecute() {
            // Hiển thị progress bar trước khi bắt đầu tải
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 0
            // Xóa ảnh hiện tại (nếu có)
            imageView.setImageBitmap(null)
        }

        override fun doInBackground(vararg urls: String): Bitmap? {
            val imageUrl = urls[0]
            var bitmap: Bitmap? = null
            var connection: HttpURLConnection? = null
            var input: InputStream? = null

            try {
                // Log để debug
                Log.d(TAG, "Attempting to download image from: $imageUrl")

                // Tạo kết nối HTTP
                val url = URL(imageUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 15000 // 15 seconds timeout
                connection.readTimeout = 15000
                connection.connect()

                // Kiểm tra response code
                val responseCode = connection.responseCode
                Log.d(TAG, "HTTP Response Code: $responseCode")

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    errorMessage = "Server returned HTTP response code: $responseCode"
                    Log.e(TAG, errorMessage!!)
                    return null
                }

                // Lấy thông tin về kích thước file để tính toán tiến trình
                val fileLength = connection.contentLength
                Log.d(TAG, "File length: $fileLength")

                // Tạo input stream để đọc dữ liệu
                input = connection.inputStream

                if (fileLength > 0) {
                    val data = ByteArray(4096)
                    var total = 0
                    var count: Int

                    // Tạo một output stream tạm thời để lưu dữ liệu
                    val output = ByteArrayOutputStream()

                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        // Cập nhật tiến trình
                        publishProgress((total * 100 / fileLength))
                        output.write(data, 0, count)
                    }

                    // Chuyển đổi dữ liệu thành bitmap
                    val imageData = output.toByteArray()
                    bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

                    if (bitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap from byte array")
                        errorMessage = "Không thể chuyển đổi dữ liệu thành ảnh"
                    } else {
                        Log.d(TAG, "Bitmap created successfully: ${bitmap.width}x${bitmap.height}")
                    }

                    output.close()
                } else {
                    // Nếu không biết kích thước file, đọc trực tiếp
                    Log.d(TAG, "Using BitmapFactory.decodeStream directly")
                    bitmap = BitmapFactory.decodeStream(input)

                    if (bitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap from stream")
                        errorMessage = "Không thể chuyển đổi dữ liệu thành ảnh"
                    } else {
                        Log.d(TAG, "Bitmap created successfully: ${bitmap.width}x${bitmap.height}")
                    }

                    // Giả lập tiến trình
                    for (i in 0..100 step 10) {
                        publishProgress(i)
                        Thread.sleep(50) // Giảm thời gian chờ
                    }
                }
            } catch (e: MalformedURLException) {
                Log.e(TAG, "Invalid URL", e)
                errorMessage = "URL không hợp lệ: ${e.message}"
            } catch (e: IOException) {
                Log.e(TAG, "IO Error", e)
                errorMessage = "Lỗi kết nối: ${e.message}"
            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
                errorMessage = "Lỗi: ${e.message}"
            } finally {
                // Đóng các kết nối
                input?.close()
                connection?.disconnect()
            }

            return bitmap
        }

        override fun onProgressUpdate(vararg values: Int?) {
            // Cập nhật tiến trình trên UI
            values[0]?.let { progressBar.progress = it }
        }

        override fun onPostExecute(result: Bitmap?) {
            // Ẩn progress bar
            progressBar.visibility = View.GONE

            // Hiển thị ảnh nếu tải thành công
            if (result != null) {
                imageView.setImageBitmap(result)
                Toast.makeText(this@MainActivity, "Tải ảnh thành công", Toast.LENGTH_SHORT).show()
            } else {
                val message = errorMessage ?: "Lỗi không xác định khi tải ảnh"
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Download failed: $message")
            }
        }
    }
}
