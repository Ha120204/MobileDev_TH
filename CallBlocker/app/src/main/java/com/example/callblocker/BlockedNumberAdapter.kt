package com.example.callblocker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BlockedNumberAdapter(
    private val blockedNumbers: MutableList<String>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<BlockedNumberAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val phoneNumberTextView: TextView = view.findViewById(R.id.phoneNumberTextView)
        val deleteButton: TextView = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blocked_number, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val phoneNumber = blockedNumbers[position]
        holder.phoneNumberTextView.text = phoneNumber

        // Thêm contentDescription động cho khả năng truy cập tốt hơn
        holder.phoneNumberTextView.contentDescription = "Số điện thoại bị chặn: $phoneNumber"
        holder.deleteButton.contentDescription = "Xóa số $phoneNumber khỏi danh sách chặn"

        holder.deleteButton.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount() = blockedNumbers.size
}
