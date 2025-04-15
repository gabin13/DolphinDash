package com.example.test
import MarginItemDecoration
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.api.ApiServer
import android.content.Intent
import android.os.Vibrator
import android.util.Log
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.test.database.DatabaseHelper
import android.media.MediaPlayer
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private val itemList = mutableListOf<Item>()
    private var apiServer: ApiServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiServer = ApiServer(this)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ItemAdapter(this, itemList)
        recyclerView.adapter = adapter
        val spanCount = 4 // number of columns
        val spacing = 32  // spacing in px (you can convert dp to px)
        val includeEdge = true

        recyclerView.layoutManager = GridLayoutManager(this, spanCount)
        recyclerView.addItemDecoration(MarginItemDecoration(spanCount, spacing, includeEdge))

        var isShopVisible = false

        val db = Firebase.firestore
        db.collection("Boutique")
            .get()
            .addOnSuccessListener { result ->
                itemList.clear()
                for (document in result) {
                    val item = document.toObject(Item::class.java)
                    itemList.add(item)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error fetching documents", exception)
                Toast.makeText(this, "Erreur: ${exception.message}", Toast.LENGTH_LONG).show()
            }

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
            // Modification ici pour lancer l'activité des missions
            val intent = Intent(this, MissionsActivity::class.java)
            startActivity(intent)
            // Toast optionnel
            Toast.makeText(this, "Missions", Toast.LENGTH_SHORT).show()
        }

        val shopBackgroundView = findViewById<View>(R.id.shopBackground)
        val shopButton = findViewById<ImageButton>(R.id.shopButton)
        shopButton.setOnClickListener {
            Toast.makeText(this, "Shop clicked!", Toast.LENGTH_SHORT).show()
            isShopVisible = !isShopVisible
            recyclerView.visibility = if (isShopVisible) View.VISIBLE else View.GONE
            shopBackgroundView.visibility = if (isShopVisible) View.VISIBLE else View.GONE
        }

        db.collection("Boutique").get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("FirestoreTest", "Nombre d'items dans Boutique: ${querySnapshot.size()}")
                for (document in querySnapshot.documents) {
                    Log.d("FirestoreTest", "Item: ${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreTest", "Erreur lors de la récupération des items", e)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Arrêter le serveur API lorsque l'activité est détruite
        apiServer?.stop()
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