package com.tictactoe.icc

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
    private val chatRef by lazy {
        FirebaseDatabase.getInstance(databaseUrl).reference
            .child("ttt").child("chats").child("1226").child("4838").child("messages")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySupportChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        role = intent.getStringExtra("CHAT_CODE") ?: "4838"
        binding.tvSupportTitle.text = role

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
        val messageId = chatRef.push().key ?: return
        val message = Message(
            messageId = messageId,
            senderId = role,
            receiverId = if (role == "1226") "4838" else "1226",
            text = text,
            timestamp = System.currentTimeMillis(),
            deliveryStatus = "SENT",
            readStatus = false
        )
        chatRef.child(messageId).setValue(message)
    }

    private fun listenForMessages() {
        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                for (child in snapshot.children) {
                    val msg = child.getValue(Message::class.java)
                    if (msg != null) {
                        messages.add(msg)
                        
                        // Receiver side status updates
                        if (msg.receiverId == role) {
                            val updates = mutableMapOf<String, Any>()
                            val currentStatus = msg.getNormalizedDeliveryStatus()
                            
                            if (currentStatus == "SENT") {
                                updates["deliveryStatus"] = "DELIVERED"
                            }
                            
                            if (isChatOpen && currentStatus != "READ") {
                                updates["deliveryStatus"] = "READ"
                                updates["readStatus"] = true
                            }
                            
                            if (updates.isNotEmpty()) {
                                child.ref.updateChildren(updates)
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
        chatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val msg = child.getValue(Message::class.java)
                    if (msg != null && msg.receiverId == role && msg.getNormalizedDeliveryStatus() != "READ") {
                        val updates = mapOf(
                            "deliveryStatus" to "READ",
                            "readStatus" to true
                        )
                        child.ref.updateChildren(updates)
                    }
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
}
