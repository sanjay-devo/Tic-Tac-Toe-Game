package com.tictactoe.icc.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val delivered: Boolean = false,
    val read: Boolean = false
)
