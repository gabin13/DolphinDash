package com.example.test
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import android.content.Intent
import android.util.Log
import android.widget.TextView
import com.example.test.database.DatabaseHelper


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dbHelper = DatabaseHelper(this)

        val currentScore = dbHelper.getLastScoreAsInt()
        val scoreTextView = findViewById<TextView>(R.id.scoreText)
        scoreTextView.text = "Last: $currentScore"
        Log.d("MainActivity", "Current score: $currentScore")

        val highestScore = dbHelper.getHighestScoreAsInt()
        val highScoreTextView = findViewById<TextView>(R.id.highScoreText)
        highScoreTextView.text = "Best : $highestScore"

        // Configurer la vidéo d'arrière-plan
        //val videoView = findViewById<VideoView>(R.id.videoBackground)
        //val videoUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.background_ocean)
        //videoView.setVideoURI(videoUri)

        // Lancer la vidéo en boucle
        //videoView.setOnPreparedListener { mediaPlayer ->
            //mediaPlayer.isLooping = true
            //mediaPlayer.start()
        //}

        // Gérer le bouton PLAY
        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // Gérer les icônes du menu inférieur
        val homeButton = findViewById<ImageButton>(R.id.homeButton)
        homeButton.setOnClickListener {
            Toast.makeText(this, "Inventory clicked!", Toast.LENGTH_SHORT).show()
        }

        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            Toast.makeText(this, "Settings clicked!", Toast.LENGTH_SHORT).show()
        }

        val scoresButton = findViewById<ImageButton>(R.id.scoresButton)
        scoresButton.setOnClickListener {
            Toast.makeText(this, "Missions clicked!", Toast.LENGTH_SHORT).show()
        }

        val shopButton = findViewById<ImageButton>(R.id.shopButton)
        shopButton.setOnClickListener {
            Toast.makeText(this, "Shop clicked!", Toast.LENGTH_SHORT).show()
        }

    }
}
