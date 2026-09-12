package com.tictactoe.icc

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tictactoe.icc.databinding.ItemDateSeparatorBinding
import com.tictactoe.icc.databinding.ItemMessageReceivedBinding
import com.tictactoe.icc.databinding.ItemMessageSentBinding
import com.tictactoe.icc.model.Message
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class ChatListItem {
    data class MessageItem(val message: Message) : ChatListItem()
    data class DateSeparator(val date: String) : ChatListItem()
}

class ChatAdapter(private val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ChatListItem>()

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_DATE = 3
    }

    fun setMessages(newMessages: List<Message>) {
        val sortedMessages = newMessages.sortedBy { it.timestamp }
        val newItems = mutableListOf<ChatListItem>()
        
        var lastDateStr: String? = null
        
        for (message in sortedMessages) {
            val dateStr = formatDateSeparator(message.timestamp)
            if (dateStr != lastDateStr) {
                newItems.add(ChatListItem.DateSeparator(dateStr))
                lastDateStr = dateStr
            }
            newItems.add(ChatListItem.MessageItem(message))
        }
        
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is ChatListItem.DateSeparator -> VIEW_TYPE_DATE
            is ChatListItem.MessageItem -> {
                if (item.message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SentViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ReceivedViewHolder(binding)
            }
            else -> {
                val binding = ItemDateSeparatorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ChatListItem.MessageItem -> {
                if (holder is SentViewHolder) holder.bind(item.message)
                else if (holder is ReceivedViewHolder) holder.bind(item.message)
            }
            is ChatListItem.DateSeparator -> {
                if (holder is DateViewHolder) holder.bind(item.date)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    inner class SentViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            binding.tvTime.text = formatTime(message.timestamp)

            when (message.getNormalizedDeliveryStatus()) {
                "READ" -> {
                    binding.ivStatus.setImageResource(R.drawable.ic_tick_double)
                    binding.ivStatus.setColorFilter(binding.root.context.getColor(R.color.chat_read_tick))
                }
                "DELIVERED" -> {
                    binding.ivStatus.setImageResource(R.drawable.ic_tick_double)
                    binding.ivStatus.setColorFilter(binding.root.context.getColor(R.color.chat_unread_tick))
                }
                else -> { // "SENT"
                    binding.ivStatus.setImageResource(R.drawable.ic_tick_single)
                    binding.ivStatus.setColorFilter(binding.root.context.getColor(R.color.chat_unread_tick))
                }
            }
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.text
            binding.tvTime.text = formatTime(message.timestamp)
        }
    }

    inner class DateViewHolder(private val binding: ItemDateSeparatorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String) {
            binding.tvDate.text = date
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDateSeparator(timestamp: Long): String {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        
        return when {
            isSameDay(messageDate, now) -> "TODAY"
            isYesterday(messageDate, now) -> "YESTERDAY"
            else -> {
                val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                sdf.format(Date(timestamp)).uppercase()
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        val yesterday = cal2.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(cal1, yesterday)
    }
}
