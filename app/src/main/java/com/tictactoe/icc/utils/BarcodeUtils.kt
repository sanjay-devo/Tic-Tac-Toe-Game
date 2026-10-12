package com.tictactoe.icc.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream

object BarcodeUtils {

    const val DB_URL = "https://dukans-flutter-app-default-rtdb.firebaseio.com/"

    fun getProductsRef(): DatabaseReference {
        return FirebaseDatabase.getInstance(DB_URL).getReference("barcode/products")
    }

    fun generateBarcodeBitmap(
        contents: String,
        width: Int = 600,
        height: Int = 200
    ): Bitmap? {
        if (contents.isEmpty()) return null
        return try {
            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(contents, BarcodeFormat.CODE_128, width, height)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val pixels = IntArray(matrixWidth * matrixHeight)
            for (y in 0 until matrixHeight) {
                val offset = y * matrixWidth
                for (x in 0 until matrixWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createPrintableBarcodeImage(
        barcodeBitmap: Bitmap,
        productName: String,
        barcodeNumber: String,
        mrp: String
    ): Bitmap {
        val cardWidth = 800
        val cardHeight = 460
        val result = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // White background
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Outer border
        paint.color = Color.parseColor("#E2E8F0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawRect(20f, 20f, (cardWidth - 20).toFloat(), (cardHeight - 20).toFloat(), paint)

        // Product Name Title
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#0F172A") // Dark Navy
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER

        val displayName = if (productName.length > 26) productName.take(23) + "..." else productName
        canvas.drawText(displayName, (cardWidth / 2).toFloat(), 75f, paint)

        // Draw Barcode Image
        val barcodeWidth = 660
        val barcodeHeight = 190
        val scaledBarcode = Bitmap.createScaledBitmap(barcodeBitmap, barcodeWidth, barcodeHeight, true)
        canvas.drawBitmap(scaledBarcode, ((cardWidth - barcodeWidth) / 2).toFloat(), 110f, null)

        // Barcode Number
        paint.textSize = 28f
        paint.color = Color.parseColor("#334155")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText(barcodeNumber, (cardWidth / 2).toFloat(), 340f, paint)

        // MRP
        paint.textSize = 32f
        paint.color = Color.parseColor("#1D4ED8") // Blue Accent
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val formattedMrp = if (mrp.startsWith("₹")) mrp else "MRP: ₹$mrp"
        canvas.drawText(formattedMrp, (cardWidth / 2).toFloat(), 395f, paint)

        return result
    }

    fun saveBitmapToDownloads(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$safeFileName.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    }
                    true
                } else false
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, "$safeFileName.png")
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
