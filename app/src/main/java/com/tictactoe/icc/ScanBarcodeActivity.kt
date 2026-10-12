package com.tictactoe.icc

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.tictactoe.icc.databinding.ActivityScanBarcodeBinding
import com.tictactoe.icc.model.BarcodeProduct
import com.tictactoe.icc.utils.BarcodeUtils

class ScanBarcodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBarcodeBinding

    private var isTorchOn = false
    private var isSearching = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            binding.layoutPermissionRequired.visibility = View.GONE
            startCameraScanner()
        } else {
            binding.layoutPermissionRequired.visibility = View.VISIBLE
            Toast.makeText(this, "Camera permission is required to scan barcodes", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScanBarcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.scanBarcodeRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnHeaderBack.setOnClickListener { finish() }

        binding.btnGrantPermission.setOnClickListener {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnFlashToggle.setOnClickListener {
            toggleFlash()
        }

        binding.btnManualEntry.setOnClickListener {
            showManualEntryDialog()
        }

        checkCameraPermissionAndStart()
    }

    private fun checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            binding.layoutPermissionRequired.visibility = View.GONE
            startCameraScanner()
        } else {
            binding.layoutPermissionRequired.visibility = View.VISIBLE
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCameraScanner() {
        binding.barcodeScannerView.setStatusText("") // Clear default text
        resumeScannerListener()
    }

    private fun resumeScannerListener() {
        isSearching = false
        binding.barcodeScannerView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.text?.let { code ->
                    if (!isSearching && code.isNotBlank()) {
                        processScannedBarcode(code.trim())
                    }
                }
            }
        })
    }

    private fun toggleFlash() {
        if (isTorchOn) {
            binding.barcodeScannerView.setTorchOff()
            binding.ivFlashIcon.setImageResource(R.drawable.ic_flash_off)
            isTorchOn = false
        } else {
            binding.barcodeScannerView.setTorchOn()
            binding.ivFlashIcon.setImageResource(R.drawable.ic_flash_on)
            isTorchOn = true
        }
    }

    private fun showManualEntryDialog() {
        val input = EditText(this).apply {
            hint = "Enter Barcode Number"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setPadding(50, 40, 50, 40)
        }

        val container = FrameLayout(this).apply {
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Manual Barcode Search")
            .setMessage("Enter the product barcode number manually:")
            .setView(container)
            .setPositiveButton("Search") { dialog, _ ->
                val code = input.text.toString().trim()
                if (code.isNotEmpty()) {
                    processScannedBarcode(code)
                } else {
                    Toast.makeText(this, "Please enter a barcode number", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun processScannedBarcode(code: String) {
        if (isSearching) return
        isSearching = true
        showLoading(true)

        val productsRef = BarcodeUtils.getProductsRef()
        productsRef.orderByChild("barcodeNumber").equalTo(code)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    showLoading(false)
                    if (snapshot.exists()) {
                        val productSnapshot = snapshot.children.firstOrNull()
                        val product = productSnapshot?.getValue(BarcodeProduct::class.java)
                        if (product != null) {
                            openProductDetails(product)
                        } else {
                            showNotFoundDialog(code)
                        }
                    } else {
                        showNotFoundDialog(code)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showLoading(false)
                    Toast.makeText(this@ScanBarcodeActivity, "Firebase error: ${error.message}", Toast.LENGTH_SHORT).show()
                    resumeScannerListener()
                }
            })
    }

    private fun openProductDetails(product: BarcodeProduct) {
        val intent = Intent(this, ProductDetailsActivity::class.java).apply {
            putExtra("EXTRA_PRODUCT_ID", product.productId)
            putExtra("EXTRA_SKU", product.sku)
            putExtra("EXTRA_BARCODE_NUMBER", product.barcodeNumber)
            putExtra("EXTRA_PRODUCT_NAME", product.productName)
            putExtra("EXTRA_SHORT_DESC", product.shortDescription)
            putExtra("EXTRA_MRP", product.mrp)
            putExtra("EXTRA_IMAGE_URL", product.productImage)
        }
        startActivity(intent)
    }

    private fun showNotFoundDialog(scannedCode: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Product Not Found")
            .setMessage("No product found in Firebase for barcode number:\n$scannedCode")
            .setPositiveButton("Scan Again") { dialog, _ ->
                dialog.dismiss()
                resumeScannerListener()
            }
            .setNegativeButton("Add Product") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, AddProductActivity::class.java)
                startActivity(intent)
            }
            .setCancelable(false)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.layoutLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            binding.barcodeScannerView.resume()
            resumeScannerListener()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScannerView.pause()
    }
}
