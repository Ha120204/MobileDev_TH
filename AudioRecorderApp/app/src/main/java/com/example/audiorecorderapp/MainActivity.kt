package com.example.audiorecorderapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var recordingTimer: Chronometer
    private lateinit var recordButton: Button
    private lateinit var stopButton: Button
    private lateinit var recordingsRecyclerView: RecyclerView

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var outputFile: String? = null
    private var recordingUri: Uri? = null

    private lateinit var adapter: RecordingsAdapter
    private val recordings = mutableListOf<Recording>()
    private val allRecordings = mutableListOf<Recording>()

    private var notificationManager: NotificationManager? = null
    private val NOTIFICATION_ID = 1

    // Khai báo RequestPermission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Tất cả quyền đã được cấp", Toast.LENGTH_SHORT).show()
            loadRecordings()
        } else {
            Toast.makeText(this, "Một số quyền bị từ chối", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ánh xạ các thành phần UI
        statusTextView = findViewById(R.id.statusTextView)
        searchEditText = findViewById(R.id.searchEditText)
        recordingTimer = findViewById(R.id.recordingTimer)
        recordButton = findViewById(R.id.recordButton)
        stopButton = findViewById(R.id.stopButton)
        recordingsRecyclerView = findViewById(R.id.recordingsRecyclerView)

        // Thêm mô tả truy cập
        recordButton.contentDescription = "Bắt đầu ghi âm"
        stopButton.contentDescription = "Dừng ghi âm"
        recordingTimer.contentDescription = "Thời gian ghi âm"
        recordingsRecyclerView.contentDescription = "Danh sách các bản ghi âm"

        // Khởi tạo NotificationManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Thiết lập tìm kiếm
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterRecordings(s.toString())
            }
        })

        // Cài đặt RecyclerView
        adapter = RecordingsAdapter(
            recordings,
            { recording -> playRecording(recording.uri) },
            { recording, position -> deleteRecording(recording, position) },
            { recording -> shareRecording(recording) },
            { recording, position -> renameRecording(recording, position) }
        )
        recordingsRecyclerView.layoutManager = LinearLayoutManager(this)
        recordingsRecyclerView.adapter = adapter

        // Thiết lập sự kiện click cho nút ghi âm
        recordButton.setOnClickListener {
            if (checkPermissions()) {
                startRecording()
            } else {
                requestPermissions()
            }
        }

        // Thiết lập sự kiện click cho nút dừng
        stopButton.setOnClickListener {
            stopRecording()
        }

        // Tải danh sách bản ghi âm
        if (checkPermissions()) {
            loadRecordings()
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            // Cho Android 12 (API 32) trở xuống
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            // Cho Android 13 (API 33) trở lên
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }

        // Thêm quyền thông báo cho Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            // Cho Android 12 (API 32) trở xuống
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            // Cho Android 13 (API 33) trở lên
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }

        // Thêm quyền thông báo cho Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startRecording() {
        try {
            // Tạo MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            // Cấu hình MediaRecorder
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)

                // Tạo tên file dễ đọc hơn
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                val displayName = "Bản ghi âm $timestamp"
                val fileName = "recording_$timestamp.mp3"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Sử dụng MediaStore cho Android 10+
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Audio.Media.TITLE, displayName)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3")
                        put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                    }

                    contentResolver.let { resolver ->
                        recordingUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                        recordingUri?.let { uri ->
                            outputFile = uri.toString()
                            setOutputFile(resolver.openFileDescriptor(uri, "w")?.fileDescriptor)
                        }
                    }
                } else {
                    // Sử dụng file trực tiếp cho Android 9-
                    val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    val file = File(storageDir, fileName)
                    outputFile = file.absolutePath
                    setOutputFile(outputFile)

                    // Thêm file vào MediaStore
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DATA, outputFile)
                        put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Audio.Media.TITLE, displayName)
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3")
                    }
                    recordingUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                }

                // Chuẩn bị và bắt đầu ghi âm
                prepare()
                start()

                isRecording = true
                recordButton.isEnabled = false
                stopButton.isEnabled = true
                statusTextView.text = "Đang ghi âm..."
                statusTextView.contentDescription = "Trạng thái: Đang ghi âm"
                recordingTimer.visibility = View.VISIBLE
                recordingTimer.base = android.os.SystemClock.elapsedRealtime()
                recordingTimer.start()

                // Hiển thị thông báo
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }
                showRecordingNotification()
            }
        } catch (e: IOException) {
            Log.e("AudioRecorder", "Lỗi khi ghi âm: ${e.message}")
            Toast.makeText(this, "Không thể bắt đầu ghi âm", Toast.LENGTH_SHORT).show()
            resetRecorder()
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null

                recordingTimer.stop()
                val duration = android.os.SystemClock.elapsedRealtime() - recordingTimer.base

                // Hủy thông báo
                cancelRecordingNotification()

                // Lấy thông tin về file đã ghi
                recordingUri?.let { uri ->
                    // Tạo tên hiển thị
                    val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    val displayName = getFileNameFromUri(uri)

                    // Cập nhật tên trong MediaStore nếu cần
                    if (displayName.startsWith("recording_") || displayName == "Unknown") {
                        val values = ContentValues().apply {
                            put(MediaStore.Audio.Media.TITLE, "Bản ghi âm $timestamp")
                            put(MediaStore.Audio.Media.DISPLAY_NAME, "Bản ghi âm $timestamp")
                        }
                        contentResolver.update(uri, values, null, null)
                    }

                    // Lấy tên đã cập nhật
                    val finalName = getFileNameFromUri(uri)

                    val recording = Recording(
                        uri = uri,
                        name = finalName,
                        date = timestamp,
                        duration = formatDuration(duration),
                        isPlaying = false
                    )
                    recordings.add(0, recording)
                    allRecordings.add(0, recording)
                    adapter.notifyItemInserted(0)
                }

                Toast.makeText(this, "Đã lưu bản ghi âm", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Lỗi khi dừng ghi âm: ${e.message}")
                Toast.makeText(this, "Lỗi khi dừng ghi âm", Toast.LENGTH_SHORT).show()
            } finally {
                resetRecorder()
            }
        }
    }

    private fun resetRecorder() {
        isRecording = false
        recordButton.isEnabled = true
        stopButton.isEnabled = false
        statusTextView.text = "Sẵn sàng ghi âm"
        statusTextView.contentDescription = "Trạng thái: Sẵn sàng ghi âm"
        recordingTimer.visibility = View.INVISIBLE
        recordingTimer.stop()

        // Hủy thông báo nếu còn
        cancelRecordingNotification()
    }

    private fun loadRecordings() {
        allRecordings.clear()
        recordings.clear()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Audio.Media.TITLE} LIKE ?"
        val selectionArgs = arrayOf("%recording_%", "%Bản ghi âm%")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val nameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)

                // Ưu tiên lấy TITLE, nếu không có thì lấy DISPLAY_NAME
                var name = "Bản ghi âm"
                if (titleColumn != -1 && !cursor.isNull(titleColumn)) {
                    name = cursor.getString(titleColumn)
                } else if (nameColumn != -1 && !cursor.isNull(nameColumn)) {
                    val fileName = cursor.getString(nameColumn)
                    name = fileName.substringBeforeLast(".") // Loại bỏ phần mở rộng

                    // Nếu tên bắt đầu bằng "recording_", thay thế bằng "Bản ghi âm"
                    if (name.startsWith("recording_")) {
                        val timestamp = name.substringAfter("recording_").replace("_", " ")
                        name = "Bản ghi âm $timestamp"
                    }
                }

                val dateAdded = cursor.getLong(dateColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = Date(dateAdded * 1000) // Convert seconds to milliseconds

                // Lấy thời lượng nếu có
                var duration = "Unknown"
                try {
                    val metaRetriever = MediaMetadataRetriever()
                    metaRetriever.setDataSource(this, contentUri)
                    val durationStr = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    if (durationStr != null) {
                        duration = formatDuration(durationStr.toLong())
                    }
                    metaRetriever.release()
                } catch (e: Exception) {
                    Log.e("AudioRecorder", "Lỗi khi lấy thời lượng: ${e.message}")
                }

                val recording = Recording(
                    uri = contentUri,
                    name = name,
                    date = dateFormat.format(date),
                    duration = duration,
                    isPlaying = false
                )
                recordings.add(recording)
                allRecordings.add(recording)
            }
        }

        adapter.notifyDataSetChanged()
    }

    private fun playRecording(uri: Uri) {
        // Tìm bản ghi đang được phát
        val recordingToPlay = recordings.find { it.uri == uri }

        // Nếu đang phát bản ghi này, dừng lại
        if (recordingToPlay?.isPlaying == true) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            // Đặt lại trạng thái phát
            recordings.forEach { it.isPlaying = false }
            adapter.notifyDataSetChanged()
            return
        }

        // Dừng phát hiện tại nếu có
        mediaPlayer?.release()
        mediaPlayer = null

        // Đặt lại trạng thái phát cho tất cả các bản ghi
        recordings.forEach { it.isPlaying = false }

        // Đặt trạng thái phát cho bản ghi hiện tại
        recordingToPlay?.isPlaying = true
        adapter.notifyDataSetChanged()

        // Tạo MediaPlayer mới
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(this@MainActivity, uri)
                setOnCompletionListener {
                    // Khi phát xong, đặt lại trạng thái
                    recordingToPlay?.isPlaying = false
                    adapter.notifyDataSetChanged()
                    release()
                    mediaPlayer = null
                }
                prepare()
                start()
                Toast.makeText(this@MainActivity, "Đang phát...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Lỗi khi phát: ${e.message}")
                Toast.makeText(this@MainActivity, "Không thể phát bản ghi âm", Toast.LENGTH_SHORT).show()
                recordingToPlay?.isPlaying = false
                adapter.notifyDataSetChanged()
                release()
                mediaPlayer = null
            }
        }
    }

    private fun deleteRecording(recording: Recording, position: Int) {
        // Hiển thị hộp thoại xác nhận
        AlertDialog.Builder(this)
            .setTitle("Xóa bản ghi")
            .setMessage("Bạn có chắc chắn muốn xóa bản ghi này?")
            .setPositiveButton("Xóa") { _, _ ->
                try {
                    // Nếu đang phát bản ghi này, dừng lại
                    if (recording.isPlaying) {
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }

                    // Xóa file từ MediaStore
                    contentResolver.delete(recording.uri, null, null)

                    // Xóa khỏi danh sách và cập nhật adapter
                    recordings.removeAt(position)
                    allRecordings.remove(recording)
                    adapter.notifyItemRemoved(position)

                    Toast.makeText(this, "Đã xóa bản ghi", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("AudioRecorder", "Lỗi khi xóa: ${e.message}")
                    Toast.makeText(this, "Không thể xóa bản ghi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun renameRecording(recording: Recording, position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename, null)
        val editText = dialogView.findViewById<EditText>(R.id.renameEditText)
        editText.setText(recording.name)

        AlertDialog.Builder(this)
            .setTitle("Đổi tên bản ghi")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // Cập nhật tên trong MediaStore
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE, newName)
                        put(MediaStore.Audio.Media.DISPLAY_NAME, "$newName.mp3")
                    }
                    contentResolver.update(recording.uri, values, null, null)

                    // Cập nhật danh sách và adapter
                    recording.name = newName
                    adapter.notifyItemChanged(position)

                    Toast.makeText(this, "Đã đổi tên bản ghi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun shareRecording(recording: Recording) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, recording.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ bản ghi âm"))
    }

    private fun filterRecordings(query: String) {
        recordings.clear()

        if (query.isEmpty()) {
            recordings.addAll(allRecordings)
        } else {
            val filteredList = allRecordings.filter {
                it.name.contains(query, ignoreCase = true)
            }
            recordings.addAll(filteredList)
        }

        adapter.notifyDataSetChanged()
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result = "Bản ghi âm"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                // Ưu tiên lấy TITLE trước, sau đó mới đến DISPLAY_NAME
                val titleIndex = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val nameIndex = it.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)

                if (titleIndex >= 0 && !it.isNull(titleIndex)) {
                    result = it.getString(titleIndex)
                } else if (nameIndex >= 0 && !it.isNull(nameIndex)) {
                    val fileName = it.getString(nameIndex)
                    // Loại bỏ phần mở rộng file nếu có
                    result = fileName.substringBeforeLast(".")
                }
            }
        }

        // Nếu tên bắt đầu bằng "recording_", thay thế bằng "Bản ghi âm"
        if (result.startsWith("recording_")) {
            val timestamp = result.substringAfter("recording_").replace("_", " ")
            result = "Bản ghi âm $timestamp"
        }

        return result
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "recording_channel",
            "Recording Channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Thông báo khi đang ghi âm"
        }
        notificationManager?.createNotificationChannel(channel)
    }

    private fun showRecordingNotification() {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, "recording_channel")
        } else {
            NotificationCompat.Builder(this)
        }

        val notification = builder
            .setContentTitle("Đang ghi âm")
            .setContentText("Ứng dụng đang ghi âm...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelRecordingNotification() {
        notificationManager?.cancel(NOTIFICATION_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
        cancelRecordingNotification()
    }
}