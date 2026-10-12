package com.tictactoe.icc

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tictactoe.icc.databinding.ActivityBarcodeBinding

class BarcodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBarcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.barcodeRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnHeaderBack.setOnClickListener {
            finish()
        }

        binding.btnScanBarcodePage.setOnClickListener {
            startActivity(Intent(this, ScanBarcodeActivity::class.java))
        }

        binding.btnAddProductPage.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
        }
    }
}
