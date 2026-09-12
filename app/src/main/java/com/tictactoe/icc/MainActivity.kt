package com.tictactoe.icc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tictactoe.icc.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmation()
            }
        })
    }

    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Exit App?")
            .setMessage("Are you sure you want to close the app?")
            .setNegativeButton("NO") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("YES") { _, _ -> finish() }
            .show()
    }

    private fun setupClickListeners() {
        binding.btnWithAI.setOnClickListener {
            // Navigate to AI game setup screen
            startActivity(Intent(this, GameAiActivity::class.java))
        }

        binding.btnWithFriend.setOnClickListener {
            // Navigate to Two-Player setup screen
            showToast("Navigating to Friend Setup")
        }

        binding.btnSettings.setOnClickListener {
            // Open Settings
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}