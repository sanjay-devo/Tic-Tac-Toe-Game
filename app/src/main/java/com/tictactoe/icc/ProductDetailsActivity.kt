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
import com.bumptech.glide.Glide
import com.tictactoe.icc.databinding.ActivityProductDetailsBinding
import com.tictactoe.icc.utils.BarcodeUtils

class ProductDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailsBinding

    private var productId = ""
    private var sku = ""
    private var barcodeNumber = ""
    private var productName = ""
    private var shortDesc = ""
    private var mrp = ""
    private var imageUrl = ""
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
        binding = ActivityProductDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.productDetailsRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        extractIntentData()
        populateUI()

        binding.btnHeaderBack.setOnClickListener { finish() }
        binding.btnScanAnother.setOnClickListener { finish() }

        binding.btnDownloadBarcodeDetails.setOnClickListener {
            checkPermissionAndDownload()
        }
    }

    private fun extractIntentData() {
        productId = intent.getStringExtra("EXTRA_PRODUCT_ID") ?: ""
        sku = intent.getStringExtra("EXTRA_SKU") ?: ""
        barcodeNumber = intent.getStringExtra("EXTRA_BARCODE_NUMBER") ?: ""
        productName = intent.getStringExtra("EXTRA_PRODUCT_NAME") ?: "Product"
        shortDesc = intent.getStringExtra("EXTRA_SHORT_DESC") ?: ""
        mrp = intent.getStringExtra("EXTRA_MRP") ?: "0.00"
        imageUrl = intent.getStringExtra("EXTRA_IMAGE_URL") ?: ""
    }

    private fun populateUI() {
        binding.tvProductName.text = productName
        binding.tvProductId.text = productId.ifEmpty { "N/A" }
        binding.tvProductSku.text = sku.ifEmpty { "N/A" }
        binding.tvProductBarcodeNumber.text = barcodeNumber.ifEmpty { "N/A" }

        binding.tvProductMrp.text = if (mrp.startsWith("₹")) mrp else "MRP: ₹$mrp"

        if (shortDesc.isNotBlank()) {
            binding.tvProductDescription.visibility = View.VISIBLE
            binding.tvProductDescription.text = shortDesc
        } else {
            binding.tvProductDescription.visibility = View.GONE
        }

        if (imageUrl.isNotBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(binding.ivProductImage)
        } else {
            binding.ivProductImage.setImageResource(R.mipmap.ic_launcher)
        }

        barcodeBitmap = BarcodeUtils.generateBarcodeBitmap(barcodeNumber, width = 600, height = 200)
        if (barcodeBitmap != null) {
            binding.ivBarcodeDetails.setImageBitmap(barcodeBitmap)
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

        val printableCard = BarcodeUtils.createPrintableBarcodeImage(
            barcodeBitmap = bitmap,
            productName = productName,
            barcodeNumber = barcodeNumber,
            mrp = mrp
        )

        val fileName = "Barcode_${barcodeNumber}"
        val success = BarcodeUtils.saveBitmapToDownloads(this, printableCard, fileName)

        if (success) {
            Toast.makeText(this, "Barcode downloaded to Downloads folder ($fileName.png)", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Failed to download barcode image", Toast.LENGTH_SHORT).show()
        }
    }
}
