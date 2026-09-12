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
import com.tictactoe.icc.databinding.ActivityGameAiBinding
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameAiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameAiBinding
    private var board = Array(9) { "" }
    private var userScore = 0
    private var aiScore = 0
    private var isUserTurn = true
    private var isGameOver = false

    private lateinit var xBitmap: Bitmap
    private lateinit var oBitmap: Bitmap

    private val PREFS_NAME = "tictactoe_prefs"
    private val PREF_DIFFICULTY = "difficulty_level"
    private val PREF_VIBRATION = "vibration_enabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGameAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
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
                if (isUserTurn && !isGameOver && board[i].isEmpty()) {
                    vibrateIfEnabled(50)
                    makeMove(i, "O")
                    if (!isGameOver) {
                        isUserTurn = false
                        Handler(Looper.getMainLooper()).postDelayed({
                            aiMove()
                        }, 600)
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
        
        if (player == "X") vibrateIfEnabled(50)

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

    private fun aiMove() {
        if (isGameOver) return
        
        val difficulty = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_DIFFICULTY, getString(R.string.easy))

        val position = when (difficulty) {
            getString(R.string.random) -> getRandomMove()
            getString(R.string.easy) -> getEasyMove()
            getString(R.string.medium) -> getMediumMove()
            getString(R.string.hard) -> getHardMove()
            getString(R.string.impossible) -> getBestMove()
            else -> getEasyMove()
        }

        if (position != -1) {
            makeMove(position, "X")
            if (!isGameOver) isUserTurn = true
        }
    }

    private fun getRandomMove(): Int {
        val emptyCells = board.indices.filter { board[it].isEmpty() }
        return if (emptyCells.isNotEmpty()) emptyCells.random() else -1
    }

    private fun getEasyMove(): Int {
        return getRandomMove()
    }

    private fun getMediumMove(): Int {
        return if (Random.nextBoolean()) getHardMove() else getRandomMove()
    }

    private fun getHardMove(): Int {
        val winMove = findWinningMove("X")
        if (winMove != -1) return winMove
        
        val blockMove = findWinningMove("O")
        if (blockMove != -1) return blockMove
        
        if (board[4].isEmpty()) return 4
        
        val corners = intArrayOf(0, 2, 6, 8).filter { board[it].isEmpty() }
        if (corners.isNotEmpty()) return corners.random()
        
        return getRandomMove()
    }

    private fun findWinningMove(player: String): Int {
        for (i in board.indices) {
            if (board[i].isEmpty()) {
                board[i] = player
                if (checkWinner(player)) {
                    board[i] = ""
                    return i
                }
                board[i] = ""
            }
        }
        return -1
    }

    private fun getBestMove(): Int {
        var bestScore = Int.MIN_VALUE
        var move = -1
        for (i in board.indices) {
            if (board[i].isEmpty()) {
                board[i] = "X"
                val score = minimax(board, 0, false)
                board[i] = ""
                if (score > bestScore) {
                    bestScore = score
                    move = i
                }
            }
        }
        return move
    }

    private fun minimax(currentBoard: Array<String>, depth: Int, isMaximizing: Boolean): Int {
        if (checkWinner("X")) return 10 - depth
        if (checkWinner("O")) return depth - 10
        if (currentBoard.none { it.isEmpty() }) return 0

        if (isMaximizing) {
            var bestScore = Int.MIN_VALUE
            for (i in currentBoard.indices) {
                if (currentBoard[i].isEmpty()) {
                    currentBoard[i] = "X"
                    val score = minimax(currentBoard, depth + 1, false)
                    currentBoard[i] = ""
                    bestScore = max(score, bestScore)
                }
            }
            return bestScore
        } else {
            var bestScore = Int.MAX_VALUE
            for (i in currentBoard.indices) {
                if (currentBoard[i].isEmpty()) {
                    currentBoard[i] = "O"
                    val score = minimax(currentBoard, depth + 1, true)
                    currentBoard[i] = ""
                    bestScore = min(score, bestScore)
                }
            }
            return bestScore
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

    private fun checkGameState() {
        if (checkWinner("O")) {
            userScore++
            updateScoreUI()
            vibrateIfEnabled(300)
            showToast("You Win!")
            isGameOver = true
            autoRestart()
        } else if (checkWinner("X")) {
            aiScore++
            updateScoreUI()
            vibrateIfEnabled(300)
            showToast("You Lose!")
            isGameOver = true
            autoRestart()
        } else if (board.none { it.isEmpty() }) {
            vibrateIfEnabled(300)
            showToast("Game Draw!")
            isGameOver = true
            autoRestart()
        }
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
        }, 1200)
    }

    private fun updateScoreUI() {
        val oldPlayerScore = binding.tvPlayerScore.text.toString()
        val oldAiScore = binding.tvAiScore.text.toString()
        
        if (oldPlayerScore != userScore.toString()) {
            binding.tvPlayerScore.text = userScore.toString()
            animateScore(binding.tvPlayerScore)
        }
        
        if (oldAiScore != aiScore.toString()) {
            binding.tvAiScore.text = aiScore.toString()
            animateScore(binding.tvAiScore)
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
        isUserTurn = true
    }
}