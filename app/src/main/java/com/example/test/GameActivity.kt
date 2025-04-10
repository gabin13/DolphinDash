package com.example.test

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.database.DatabaseHelper

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var scoreText: TextView
    private lateinit var pauseButton: ImageButton
    private var isGamePaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Initialisez la zone de jeu (GameView) et ajoutez-la dynamiquement
        gameView = GameView(this)
        val gameContainer = findViewById<FrameLayout>(R.id.gameSurfaceContainer)
        gameContainer.addView(gameView)

        // Configuration des éléments de l'interface utilisateur
        scoreText = findViewById(R.id.scoreText)
        pauseButton = findViewById(R.id.pauseButton)



        // Configuration du listener de score
        gameView.setScoreUpdateListener { score ->
            runOnUiThread {
                val roundedScore = score.toInt()
                scoreText.text = "Score : $roundedScore"
            }
        }

        // Gestion du bouton pause
        pauseButton.setOnClickListener {
            if (isGamePaused) {
                gameView.resumeGame()
                isGamePaused = false
                pauseButton.setImageResource(R.drawable.ic_pause)
            } else {
                gameView.pauseGame()
                isGamePaused = true
                pauseButton.setImageResource(R.drawable.ic_play)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseGame()
        isGamePaused = true
        pauseButton.setImageResource(R.drawable.ic_play)
    }

    override fun onResume() {
        super.onResume()
        if (!isGamePaused) {
            gameView.resumeGame()
            pauseButton.setImageResource(R.drawable.ic_pause)
        }
    }

}