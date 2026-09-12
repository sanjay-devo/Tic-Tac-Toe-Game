package com.tictactoe.icc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tictactoe.icc.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val PREFS_NAME = "tictactoe_prefs"
    private val PREF_VIBRATION = "vibration_enabled"
    private val PREF_DIFFICULTY = "difficulty_level"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupSettings()
    }

    private fun setupSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Difficulty
        val currentDifficulty = prefs.getString(PREF_DIFFICULTY, getString(R.string.easy))
        binding.tvDifficultyValue.text = currentDifficulty
        binding.rowDifficulty.setOnClickListener { showDifficultyDropdown() }

        // Vibration
        val vibrationEnabled = prefs.getBoolean(PREF_VIBRATION, true)
        binding.switchVibration.isChecked = vibrationEnabled
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_VIBRATION, isChecked).apply()
        }

        // No Ads
        binding.rowNoAds.setOnClickListener {
            showToast("No Ads feature coming soon!")
        }

        // Rating App
        binding.rowRating.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                showToast("Play Store not found")
            }
        }

        // Feedback
        binding.rowFeedback.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("indiacybercafe.com@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Tic Tac Toe Feedback")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                showToast("Email app not found")
            }
        }
    }

    private fun showDifficultyDropdown() {
        val levels = arrayOf(
            getString(R.string.easy),
            getString(R.string.random),
            getString(R.string.medium),
            getString(R.string.hard),
            getString(R.string.impossible)
        )
        
        val listPopupWindow = ListPopupWindow(this)
        listPopupWindow.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, levels))
        listPopupWindow.anchorView = binding.tvDifficultyValue
        listPopupWindow.width = 550
        listPopupWindow.height = ListPopupWindow.WRAP_CONTENT
        listPopupWindow.verticalOffset = 10
        listPopupWindow.horizontalOffset = -400
        listPopupWindow.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_dropdown))
        listPopupWindow.isModal = true
        
        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            val selectedLevel = levels[position]
            binding.tvDifficultyValue.text = selectedLevel
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_DIFFICULTY, selectedLevel)
                .apply()
            listPopupWindow.dismiss()
        }
        
        listPopupWindow.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}