package com.tictactoe.icc

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.tictactoe.icc.databinding.ActivityAddProductBinding
import com.tictactoe.icc.model.BarcodeProduct
import com.tictactoe.icc.utils.BarcodeUtils

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding

    private var generatedProductId = ""
    private var generatedSku = ""
    private var generatedBarcodeNumber = ""
    private var barcodeBitmap: Bitmap? = null

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            downloadBarcodeImage()
        } else {
            Toast.makeText(this, "Storage permission required to download barcode", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.addProductRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        generateProductIdentifiers()

        binding.btnHeaderBack.setOnClickListener { finish() }

        binding.btnDownloadBarcode.setOnClickListener {
            checkPermissionAndDownload()
        }

        binding.btnSaveProduct.setOnClickListener {
            validateAndSaveProduct()
        }
    }

    private fun generateProductIdentifiers() {
        val timestamp = System.currentTimeMillis()
        val randomNum = (100000..999999).random()
        val random9Digits = (100000000..999999999).random()

        generatedProductId = "PRD-$timestamp"
        generatedSku = "SKU-$randomNum"
        generatedBarcodeNumber = "890$random9Digits"

        binding.etProductId.setText(generatedProductId)
        binding.etSku.setText(generatedSku)
        binding.etBarcodeNumber.setText(generatedBarcodeNumber)
        binding.tvBarcodeNumberPreview.text = generatedBarcodeNumber

        barcodeBitmap = BarcodeUtils.generateBarcodeBitmap(generatedBarcodeNumber, width = 600, height = 200)
        if (barcodeBitmap != null) {
            binding.ivBarcodePreview.setImageBitmap(barcodeBitmap)
        }
    }

    private fun checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                downloadBarcodeImage()
            } else {
                storagePermissionLauncher.launch(permission)
            }
        } else {
            downloadBarcodeImage()
        }
    }

    private fun downloadBarcodeImage() {
        val bitmap = barcodeBitmap ?: run {
            Toast.makeText(this, "Barcode preview not available", Toast.LENGTH_SHORT).show()
            return
        }

        val productName = binding.etProductName.text.toString().trim().ifEmpty { "Product Barcode" }
        val mrp = binding.etMrp.text.toString().trim().ifEmpty { "0.00" }

        val printableCard = BarcodeUtils.createPrintableBarcodeImage(
            barcodeBitmap = bitmap,
            productName = productName,
            barcodeNumber = generatedBarcodeNumber,
            mrp = mrp
        )

        val fileName = "Barcode_${generatedBarcodeNumber}"
        val success = BarcodeUtils.saveBitmapToDownloads(this, printableCard, fileName)

        if (success) {
            Toast.makeText(this, "Barcode downloaded to Downloads folder ($fileName.png)", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Failed to download barcode image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateAndSaveProduct() {
        val productName = binding.etProductName.text.toString().trim()
        val shortDesc = binding.etShortDescription.text.toString().trim()
        val mrpStr = binding.etMrp.text.toString().trim()
        val imageUrl = binding.etProductImage.text.toString().trim()

        if (productName.isEmpty()) {
            binding.etProductName.error = "Product name is required"
            binding.etProductName.requestFocus()
            return
        }

        if (mrpStr.isEmpty()) {
            binding.etMrp.error = "MRP is required"
            binding.etMrp.requestFocus()
            return
        }

        val mrpVal = mrpStr.toDoubleOrNull()
        if (mrpVal == null || mrpVal <= 0) {
            binding.etMrp.error = "Enter a valid MRP amount"
            binding.etMrp.requestFocus()
            return
        }

        setLoading(true)

        // Check Firebase for duplicate barcode number before saving
        val productsRef = BarcodeUtils.getProductsRef()
        productsRef.orderByChild("barcodeNumber").equalTo(generatedBarcodeNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        setLoading(false)
                        Toast.makeText(
                            this@AddProductActivity,
                            "Duplicate barcode detected! Regenerating code...",
                            Toast.LENGTH_SHORT
                        ).show()
                        generateProductIdentifiers()
                    } else {
                        saveToFirebase(productName, shortDesc, mrpStr, imageUrl)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    setLoading(false)
                    Toast.makeText(
                        this@AddProductActivity,
                        "Firebase error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun saveToFirebase(
        productName: String,
        shortDesc: String,
        mrpStr: String,
        imageUrl: String
    ) {
        val product = BarcodeProduct(
            productId = generatedProductId,
            sku = generatedSku,
            barcodeNumber = generatedBarcodeNumber,
            productName = productName,
            shortDescription = shortDesc,
            mrp = mrpStr,
            productImage = imageUrl,
            createdAt = System.currentTimeMillis()
        )

        val productsRef = BarcodeUtils.getProductsRef()
        productsRef.child(generatedProductId).setValue(product)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Product saved successfully to Firebase!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnSaveProduct.isEnabled = false
            binding.progressBarSave.visibility = View.VISIBLE
        } else {
            binding.btnSaveProduct.isEnabled = true
            binding.progressBarSave.visibility = View.GONE
        }
    }
}
