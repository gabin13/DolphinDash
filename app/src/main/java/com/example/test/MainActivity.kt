package com.example.test
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Vibrator
import android.util.Log
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.test.database.DatabaseHelper
import android.media.MediaPlayer


class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music)


        val dbHelper = DatabaseHelper(this)

        val currentScore = dbHelper.getLastScoreAsInt()
        val scoreTextView = findViewById<TextView>(R.id.scoreText)
        scoreTextView.text = "Last: $currentScore"
        Log.d("MainActivity", "Current score: $currentScore")

        val highestScore = dbHelper.getHighestScoreAsInt()
        val highScoreTextView = findViewById<TextView>(R.id.highScoreText)
        highScoreTextView.text = "Best : $highestScore"



        // Gérer le bouton PLAY
        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        // Gérer les icônes du menu inférieur
        val inventaireButton = findViewById<ImageButton>(R.id.inventaireButton)
        inventaireButton.setOnClickListener {
            Toast.makeText(this, "Inventory clicked!", Toast.LENGTH_SHORT).show()

        }

        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            Toast.makeText(this, "Settings clicked!", Toast.LENGTH_SHORT).show()
            showSettingsDialog()
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
    // Load vibration setting from SharedPreferences
    private fun loadVibrationSetting(): Boolean {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("vibration_enabled", true)  // Default to true if not set
    }
    // Load vibration setting from SharedPreferences
    private fun loadSoundSetting(): Boolean {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("SOUND_PREF", true)  // Default to true if not set
    }

    private fun showSettingsDialog() {
        // Initialize dialog view
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)

        // Find the checkbox for vibration in the dialog
        val vibrationCheckBox: CheckBox = dialogView.findViewById(R.id.vibrationCheckBox)

        // Find the checkbox for sound  in the dialog
        val soundCheckBox: CheckBox = dialogView.findViewById(R.id.soundCheckBox)

        // Load current vibration setting from SharedPreferences
        vibrationCheckBox.isChecked = loadVibrationSetting()

        // Load current sound setting from SharedPreferences
        soundCheckBox.isChecked = loadSoundSetting()

        // Set listener for checkbox change
        vibrationCheckBox.setOnCheckedChangeListener { _, isChecked ->
            // Save the new vibration setting when the checkbox is toggled
            saveVibrationSetting(isChecked)
            // Optionally, you can perform any immediate action here, like showing a Toast.
            Toast.makeText(this, "Vibration ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        // Set listener for checkbox change
        soundCheckBox.setOnCheckedChangeListener { _, isChecked ->
            // Save the new sound setting when the checkbox is toggled
            saveSoundSetting(isChecked)

            // Optionally, you can perform any immediate action here, like showing a Toast.
            Toast.makeText(this, "Sound ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }


        // Create the AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Settings")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }

    // Save vibration setting to SharedPreferences
    private fun saveVibrationSetting(isEnabled: Boolean) {
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("vibration_enabled", isEnabled)  // Save the vibration setting
        editor.apply()
    }

    // Save sound setting to SharedPreferences
    private fun saveSoundSetting(isEnabled: Boolean) {
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("SOUND_PREF", isEnabled)  // Save the vibration setting
        editor.apply()

    }
}
