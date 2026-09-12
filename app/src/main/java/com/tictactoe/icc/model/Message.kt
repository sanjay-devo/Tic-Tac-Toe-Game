package com.tictactoe.icc.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val deliveryStatus: Any? = "SENT", // Can be String or Boolean in Firebase
    val readStatus: Any? = false       // Can be Boolean or String in Firebase
) {
    fun getNormalizedDeliveryStatus(): String {
        return when (deliveryStatus) {
            is String -> deliveryStatus
            is Boolean -> if (deliveryStatus) "DELIVERED" else "SENT"
            else -> "SENT"
        }
    }

    fun getNormalizedReadStatus(): Boolean {
        return when (readStatus) {
            is Boolean -> readStatus
            is String -> readStatus.toBoolean() || readStatus.equals("READ", true)
            else -> false
        }
    }
}
