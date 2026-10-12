package com.tictactoe.icc.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class BarcodeProduct(
    val productId: String = "",
    val sku: String = "",
    val barcodeNumber: String = "",
    val productName: String = "",
    val shortDescription: String = "",
    val mrp: String = "",
    val productImage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
