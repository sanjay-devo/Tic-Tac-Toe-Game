package com.tictactoe.icc

import android.os.Bundle
import android.provider.Settings
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
    private lateinit var senderId: String
    private var isFirstLoad = true
    private val receiverId = "support"
    private val databaseUrl = "https://rs-chat-sanjay-default-rtdb.firebaseio.com/"
    
    private val chatRef by lazy {
        FirebaseDatabase.getInstance(databaseUrl).reference
            .child("ttt").child("chats").child(senderId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySupportChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        senderId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        setupRecyclerView()
        setupClickListeners()
        handleInsets()
        listenForMessages()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(senderId)
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = false // Messages start from the top
        }
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
        
        binding.btnVideoCall.setOnClickListener { }
        binding.btnPhoneCall.setOnClickListener { }
        binding.btnMore.setOnClickListener { }
        binding.btnAttachment.setOnClickListener { }
    }

    private fun handleInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.supportChatRoot) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            
            binding.chatHeader.updatePadding(top = systemBars.top)
            
            val bottomInset = if (ime.bottom > systemBars.bottom) ime.bottom else systemBars.bottom
            view.updatePadding(bottom = bottomInset)
            
            // If keyboard opens and we are at the bottom, stay at the bottom
            if (ime.bottom > 0) {
                val layoutManager = binding.rvChat.layoutManager as LinearLayoutManager
                if (layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 2) {
                    binding.rvChat.postDelayed({
                        binding.rvChat.smoothScrollToPosition(adapter.itemCount - 1)
                    }, 100)
                }
            }
            
            windowInsets
        }
    }

    private fun sendMessage(text: String) {
        val messageId = chatRef.push().key ?: return
        val message = Message(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            timestamp = System.currentTimeMillis(),
            delivered = false,
            read = false
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
                        // Mark received messages as read
                        if (msg.senderId == receiverId && !msg.read) {
                            child.ref.child("read").setValue(true)
                        }
                        // Mark sent messages as delivered if they are not
                        // (Usually delivery is handled by the receiver app, but here we can simulate/ensure it)
                        if (msg.senderId == senderId && !msg.delivered) {
                            child.ref.child("delivered").setValue(true)
                        }
                    }
                }
                
                val previousCount = adapter.itemCount
                adapter.setMessages(messages)
                val newCount = adapter.itemCount

                if (newCount > 0) {
                    if (isFirstLoad) {
                        // On first load, jump to bottom
                        binding.rvChat.scrollToPosition(newCount - 1)
                        isFirstLoad = false
                    } else if (newCount > previousCount) {
                        // On new message, scroll only if user is near the bottom
                        val layoutManager = binding.rvChat.layoutManager as LinearLayoutManager
                        val lastVisible = layoutManager.findLastVisibleItemPosition()
                        if (lastVisible >= previousCount - 1) {
                            binding.rvChat.smoothScrollToPosition(newCount - 1)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
