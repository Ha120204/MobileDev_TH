package com.example.audiorecorderapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Recording(
    val uri: Uri,
    var name: String,
    val date: String,
    val duration: String,
    var isPlaying: Boolean = false
)

class RecordingsAdapter(
    private val recordings: MutableList<Recording>,
    private val onPlayClick: (Recording) -> Unit,
    private val onDeleteClick: (Recording, Int) -> Unit,
    private val onShareClick: (Recording) -> Unit,
    private val onRenameClick: (Recording, Int) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.recordingNameTextView)
        val dateTextView: TextView = view.findViewById(R.id.recordingDateTextView)
        val durationTextView: TextView = view.findViewById(R.id.recordingDurationTextView)
        val playButton: ImageButton = view.findViewById(R.id.playButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
        val shareButton: ImageButton = view.findViewById(R.id.shareButton)
        val renameButton: ImageButton = view.findViewById(R.id.renameButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recording = recordings[position]
        holder.nameTextView.text = recording.name
        holder.dateTextView.text = recording.date
        holder.durationTextView.text = recording.duration

        // Thêm contentDescription cho trình đọc màn hình
        holder.itemView.contentDescription = "Bản ghi âm ${recording.name}, ngày ${recording.date}, thời lượng ${recording.duration}"

        // Cập nhật icon dựa trên trạng thái phát
        if (recording.isPlaying) {
            holder.playButton.setImageResource(android.R.drawable.ic_media_pause)
            holder.playButton.contentDescription = "Tạm dừng"
        } else {
            holder.playButton.setImageResource(android.R.drawable.ic_media_play)
            holder.playButton.contentDescription = "Phát"
        }

        holder.deleteButton.contentDescription = "Xóa bản ghi ${recording.name}"
        holder.shareButton.contentDescription = "Chia sẻ bản ghi ${recording.name}"
        holder.renameButton.contentDescription = "Đổi tên bản ghi ${recording.name}"

        holder.playButton.setOnClickListener {
            onPlayClick(recording)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(recording, position)
        }

        holder.shareButton.setOnClickListener {
            onShareClick(recording)
        }

        holder.renameButton.setOnClickListener {
            onRenameClick(recording, position)
        }
    }

    override fun getItemCount() = recordings.size
}