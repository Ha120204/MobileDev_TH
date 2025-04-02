package com.example.videoplayer

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var mediaController: MediaController
    private lateinit var btnChooseVideo: Button
    private lateinit var btnPlayUrl: Button
    private lateinit var etVideoUrl: EditText
    private lateinit var tvCurrentVideo: TextView

    private val PERMISSION_REQUEST_CODE = 100
    private val VIDEO_PICK_CODE = 1000

    private var currentVideoUri: Uri? = null
    private var currentVideoTitle: String = "Chưa có video nào được chọn"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo views
        videoView = findViewById(R.id.videoView)
        btnChooseVideo = findViewById(R.id.btnChooseVideo)
        btnPlayUrl = findViewById(R.id.btnPlayUrl)
        etVideoUrl = findViewById(R.id.etVideoUrl)
        tvCurrentVideo = findViewById(R.id.tvCurrentVideo)

        // Thiết lập MediaController
        mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        // Xử lý sự kiện khi video hoàn thành
        videoView.setOnCompletionListener {
            Toast.makeText(this, "Video đã phát xong", Toast.LENGTH_SHORT).show()
        }

        // Xử lý lỗi khi phát video
        videoView.setOnErrorListener { _, what, extra ->
            Toast.makeText(
                this,
                "Lỗi khi phát video: $what, $extra",
                Toast.LENGTH_SHORT
            ).show()
            true
        }

        // Xử lý sự kiện nút Chọn Video
        btnChooseVideo.setOnClickListener {
            if (checkPermissions()) {
                pickVideoFromGallery()
            } else {
                requestPermissions()
            }
        }

        // Xử lý sự kiện nút Phát từ URL
        btnPlayUrl.setOnClickListener {
            showUrlInputDialog()
        }

        // Khôi phục trạng thái nếu có
        if (savedInstanceState != null) {
            val videoPath = savedInstanceState.getString("videoPath")
            val videoPosition = savedInstanceState.getInt("videoPosition", 0)
            val videoTitle = savedInstanceState.getString("videoTitle")

            if (videoPath != null) {
                currentVideoUri = Uri.parse(videoPath)
                currentVideoTitle = videoTitle ?: "Video từ thư viện"
                playVideo(currentVideoUri!!, videoPosition)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    private fun pickVideoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, VIDEO_PICK_CODE)
    }

    private fun showUrlInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_url_input, null)
        val etDialogUrl = dialogView.findViewById<EditText>(R.id.etDialogUrl)

        // Thiết lập giá trị mặc định từ EditText chính
        etDialogUrl.setText(etVideoUrl.text.toString())

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Phát") { _, _ ->
                val url = etDialogUrl.text.toString().trim()
                if (url.isNotEmpty()) {
                    etVideoUrl.setText(url)
                    playVideoFromUrl(url)
                } else {
                    Toast.makeText(this, "Vui lòng nhập URL", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun playVideoFromUrl(url: String) {
        try {
            currentVideoUri = Uri.parse(url)
            currentVideoTitle = "Video từ URL"
            playVideo(currentVideoUri!!, 0)
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playVideo(uri: Uri, position: Int = 0) {
        try {
            videoView.setVideoURI(uri)
            videoView.requestFocus()

            // Thiết lập lại controller
            videoView.setMediaController(mediaController)
            mediaController.show()

            // Cập nhật UI
            tvCurrentVideo.text = currentVideoTitle

            // Bắt đầu phát video
            videoView.start()

            // Đặt vị trí phát (nếu có)
            if (position > 0) {
                videoView.seekTo(position)
            }

            // Hiển thị thông báo
            Toast.makeText(this, "Đang phát: $currentVideoTitle", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi phát video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VIDEO_PICK_CODE && resultCode == RESULT_OK && data != null) {
            val selectedVideoUri = data.data
            if (selectedVideoUri != null) {
                // Lấy tên video
                val cursor = contentResolver.query(
                    selectedVideoUri,
                    arrayOf(MediaStore.Video.Media.DISPLAY_NAME),
                    null,
                    null,
                    null
                )

                currentVideoTitle = if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Video từ thư viện"
                    cursor.close()
                    name
                } else {
                    "Video từ thư viện"
                }

                currentVideoUri = selectedVideoUri
                playVideo(currentVideoUri!!)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickVideoFromGallery()
            } else {
                Toast.makeText(
                    this,
                    "Cần cấp quyền để truy cập thư viện video",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Lưu trạng thái video hiện tại
        if (currentVideoUri != null) {
            outState.putString("videoPath", currentVideoUri.toString())
            outState.putInt("videoPosition", videoView.currentPosition)
            outState.putString("videoTitle", currentVideoTitle)
        }
    }

    override fun onPause() {
        super.onPause()
        // Lưu vị trí phát hiện tại
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        // Có thể tiếp tục phát nếu cần
    }

    override fun onDestroy() {
        super.onDestroy()
        // Giải phóng tài nguyên
        videoView.stopPlayback()
    }
}
