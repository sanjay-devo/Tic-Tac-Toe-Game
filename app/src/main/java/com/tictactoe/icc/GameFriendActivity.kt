package com.tictactoe.icc

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tictactoe.icc.databinding.ActivityGameFriendBinding
import java.io.InputStream

class GameFriendActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameFriendBinding
    private var board = Array(9) { "" }
    private var userScore = 0
    private var friendScore = 0
    private var isUserTurn = true // User (O) always starts the first round
    private var isGameOver = false
    private var startingPlayerIsUser = true

    private lateinit var xBitmap: Bitmap
    private lateinit var oBitmap: Bitmap

    private val PREFS_NAME = "tictactoe_prefs"
    private val PREF_VIBRATION = "vibration_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGameFriendBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadAssets()
        setupBoard()
        setupClickListeners()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmation()
            }
        })
    }

    private fun loadAssets() {
        try {
            val xInput: InputStream = assets.open("x.png")
            xBitmap = BitmapFactory.decodeStream(xInput)
            val oInput: InputStream = assets.open("o.png")
            oBitmap = BitmapFactory.decodeStream(oInput)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBoard() {
        val cells = arrayOf(
            binding.cell0, binding.cell1, binding.cell2,
            binding.cell3, binding.cell4, binding.cell5,
            binding.cell6, binding.cell7, binding.cell8
        )

        for (i in cells.indices) {
            cells[i].setOnClickListener {
                if (!isGameOver && board[i].isEmpty()) {
                    vibrateIfEnabled(50)
                    val currentPlayerSymbol = if (isUserTurn) "O" else "X"
                    makeMove(i, currentPlayerSymbol)
                    
                    if (!isGameOver) {
                        isUserTurn = !isUserTurn
                    }
                }
            }
        }
    }

    private fun vibrateIfEnabled(duration: Long) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_VIBRATION, true)) {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnHeaderHome.setOnClickListener {
            showExitConfirmation()
        }

        binding.btnHeaderSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showExitConfirmation() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.tvDialogTitle).setText(R.string.exit_game_question)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNo).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnYes).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun makeMove(position: Int, player: String) {
        board[position] = player
        val imageView = getImageViewAt(position)
        imageView.setImageBitmap(if (player == "X") xBitmap else oBitmap)
        imageView.alpha = 0f
        imageView.animate().alpha(1f).scaleX(1.1f).scaleY(1.1f).setDuration(200).withEndAction {
            imageView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()

        checkGameState()
    }

    private fun getImageViewAt(position: Int): ImageView {
        return when (position) {
            0 -> binding.ivCell0
            1 -> binding.ivCell1
            2 -> binding.ivCell2
            3 -> binding.ivCell3
            4 -> binding.ivCell4
            5 -> binding.ivCell5
            6 -> binding.ivCell6
            7 -> binding.ivCell7
            8 -> binding.ivCell8
            else -> binding.ivCell0
        }
    }

    private fun checkGameState() {
        if (checkWinner("O")) {
            userScore++
            updateScoreUI()
            vibrateIfEnabled(300)
            showToast("You Win!")
            isGameOver = true
            autoRestart()
        } else if (checkWinner("X")) {
            friendScore++
            updateScoreUI()
            vibrateIfEnabled(300)
            showToast("Friend Wins!")
            isGameOver = true
            autoRestart()
        } else if (board.none { it.isEmpty() }) {
            vibrateIfEnabled(300)
            showToast("Game Draw!")
            isGameOver = true
            autoRestart()
        }
    }

    private fun checkWinner(player: String): Boolean {
        val winPatterns = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
        for (pattern in winPatterns) {
            if (board[pattern[0]] == player && board[pattern[1]] == player && board[pattern[2]] == player) {
                return true
            }
        }
        return false
    }

    private fun showToast(message: String) {
        val layout = LayoutInflater.from(this).inflate(R.layout.layout_custom_toast, null)
        val text = layout.findViewById<TextView>(R.id.tvToastText)
        text.text = message

        val toast = Toast(applicationContext)
        toast.setGravity(Gravity.BOTTOM, 0, 450)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }

    private fun autoRestart() {
        Handler(Looper.getMainLooper()).postDelayed({
            resetBoard()
            // Alternate starting player
            startingPlayerIsUser = !startingPlayerIsUser
            isUserTurn = startingPlayerIsUser
        }, 1200)
    }

    private fun updateScoreUI() {
        val oldPlayerScore = binding.tvPlayerScore.text.toString()
        val oldFriendScore = binding.tvFriendScore.text.toString()
        
        if (oldPlayerScore != userScore.toString()) {
            binding.tvPlayerScore.text = userScore.toString()
            animateScore(binding.tvPlayerScore)
        }
        
        if (oldFriendScore != friendScore.toString()) {
            binding.tvFriendScore.text = friendScore.toString()
            animateScore(binding.tvFriendScore)
        }
    }

    private fun animateScore(view: View) {
        view.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(200)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun resetBoard() {
        board = Array(9) { "" }
        for (i in 0..8) {
            getImageViewAt(i).setImageDrawable(null)
        }
        isGameOver = false
    }
}