package com.tictactoe.icc

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tictactoe.icc.databinding.ActivitySupportChatBinding
import com.tictactoe.icc.model.Message

class SupportChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupportChatBinding
    private lateinit var adapter: ChatAdapter
    private var role: String = "4838" // Default to User
    private var isFirstLoad = true
    private var isChatOpen = false

    private val databaseUrl = "https://rs-chat-sanjay-default-rtdb.firebaseio.com/"
    private val chatsRootRef by lazy {
        FirebaseDatabase.getInstance(databaseUrl).reference.child("ttt").child("chats")
    }
    private val myMessagesRef by lazy {
        chatsRootRef.child(role).child("messages")
    }
    private val otherRole by lazy {
        if (role == "1226") "4838" else "1226"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySupportChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        role = intent.getStringExtra("CHAT_CODE") ?: "4838"
        binding.tvSupportTitle.text = role
        binding.btnMore.visibility = if (role == "1226") View.VISIBLE else View.GONE

        setupRecyclerView()
        setupClickListeners()
        handleInsets()
        listenForMessages()
    }

    override fun onResume() {
        super.onResume()
        isChatOpen = true
        markUnreadAsRead()
    }

    override fun onPause() {
        super.onPause()
        isChatOpen = false
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(role)
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnMore.setOnClickListener { showMoreMenu() }
        
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun handleInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.supportChatRoot) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            
            binding.chatHeader.updatePadding(top = systemBars.top)
            
            val bottomInset = if (ime.bottom > systemBars.bottom) ime.bottom else systemBars.bottom
            view.updatePadding(bottom = bottomInset)
            
            // Safe scroll when keyboard opens
            if (ime.bottom > 0 && adapter.itemCount > 0) {
                val lastVisible = (binding.rvChat.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                if (lastVisible >= adapter.itemCount - 3) {
                    binding.rvChat.postDelayed({ safeSmoothScrollToBottom() }, 100)
                }
            }
            
            windowInsets
        }
    }

    private fun sendMessage(text: String) {
        val messageId = myMessagesRef.push().key ?: return
        val message = Message(
            messageId = messageId,
            senderId = role,
            receiverId = otherRole,
            text = text,
            timestamp = System.currentTimeMillis(),
            deliveryStatus = "SENT",
            readStatus = false
        )
        
        val updates = hashMapOf<String, Any>()
        updates["/$role/messages/$messageId"] = message
        updates["/$otherRole/messages/$messageId"] = message
        
        chatsRootRef.updateChildren(updates)
    }

    private fun listenForMessages() {
        myMessagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                for (child in snapshot.children) {
                    val msg = child.getValue(Message::class.java)
                    if (msg != null) {
                        messages.add(msg)
                        
                        // Receiver side status updates
                        if (msg.receiverId == role) {
                            val statusUpdates = mutableMapOf<String, Any>()
                            val currentStatus = msg.getNormalizedDeliveryStatus()
                            
                            if (currentStatus == "SENT") {
                                statusUpdates["deliveryStatus"] = "DELIVERED"
                            }
                            
                            if (isChatOpen && currentStatus != "READ") {
                                statusUpdates["deliveryStatus"] = "READ"
                                statusUpdates["readStatus"] = true
                            }
                            
                            if (statusUpdates.isNotEmpty()) {
                                val multiUpdates = hashMapOf<String, Any>()
                                statusUpdates.forEach { (key, value) ->
                                    multiUpdates["/$role/messages/${msg.messageId}/$key"] = value
                                    multiUpdates["/$otherRole/messages/${msg.messageId}/$key"] = value
                                }
                                chatsRootRef.updateChildren(multiUpdates)
                            }
                        }
                    }
                }
                
                val previousCount = adapter.itemCount
                adapter.setMessages(messages)
                val newCount = adapter.itemCount

                if (newCount > 0) {
                    if (isFirstLoad) {
                        binding.rvChat.scrollToPosition(newCount - 1)
                        isFirstLoad = false
                    } else if (newCount > previousCount) {
                        val lastVisible = (binding.rvChat.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                        if (lastVisible >= previousCount - 1) {
                            safeSmoothScrollToBottom()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun markUnreadAsRead() {
        myMessagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val multiUpdates = hashMapOf<String, Any>()
                for (child in snapshot.children) {
                    val msg = child.getValue(Message::class.java)
                    if (msg != null && msg.receiverId == role && msg.getNormalizedDeliveryStatus() != "READ") {
                        multiUpdates["/$role/messages/${msg.messageId}/deliveryStatus"] = "READ"
                        multiUpdates["/$role/messages/${msg.messageId}/readStatus"] = true
                        multiUpdates["/$otherRole/messages/${msg.messageId}/deliveryStatus"] = "READ"
                        multiUpdates["/$otherRole/messages/${msg.messageId}/readStatus"] = true
                    }
                }
                if (multiUpdates.isNotEmpty()) {
                    chatsRootRef.updateChildren(multiUpdates)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun safeSmoothScrollToBottom() {
        val count = adapter.itemCount
        if (count > 0) {
            binding.rvChat.smoothScrollToPosition(count - 1)
        }
    }

    private fun showMoreMenu() {
        val popup = PopupMenu(this, binding.btnMore)
        if (role == "1226") {
            popup.menu.add("Delete All Chat")
        }

        popup.setOnMenuItemClickListener { item ->
            if (item.title == "Delete All Chat") {
                showDeleteConfirmationDialog()
                true
            } else {
                false
            }
        }

        if (popup.menu.size() > 0) {
            popup.show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete all chat?")
            .setMessage("This will permanently delete the complete conversation.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteAllChat()
            }
            .show()
    }

    private fun deleteAllChat() {
        val updates = hashMapOf<String, Any?>()
        updates["/1226/messages"] = null
        updates["/4838/messages"] = null
        chatsRootRef.updateChildren(updates)
    }
}
