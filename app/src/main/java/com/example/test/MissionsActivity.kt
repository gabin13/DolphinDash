package com.example.test

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MissionsActivity : AppCompatActivity() {

    private val TAG = "MissionsActivity"

    // Firebase
    private val db = Firebase.firestore
    private val missionService = MissionService()

    // Vues
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var emptyStateView: LinearLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var coinsIndicator: TextView

    // Données
    private val dailyMissions = mutableListOf<Mission>()
    private val generalMissions = mutableListOf<Mission>()
    private lateinit var adapter: MissionAdapter
    private var currentMissionType = "Daily" // Par défaut, afficher les missions quotidiennes

    // Préférences utilisateur
    private lateinit var sharedPreferences: SharedPreferences
    private var userCoins = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_missions)

        // Initialiser les préférences partagées
        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        userCoins = sharedPreferences.getInt("user_coins", 0)

        // Initialiser les vues
        initViews()

        // Configurer l'adaptateur et le RecyclerView
        setupRecyclerView()

        // Configurer les onglets
        setupTabs()

        // Configurer le bouton de retour
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Configurer les options développeur
        setupDeveloperOptions()

        // Charger les missions
        loadMissions()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.missionsRecyclerView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        emptyStateView = findViewById(R.id.emptyStateView)
        tabLayout = findViewById(R.id.missionTabs)
        coinsIndicator = findViewById(R.id.coinsIndicator)

        // Afficher le nombre de pièces de l'utilisateur
        coinsIndicator.text = "$userCoins coins"
    }

    private fun setupRecyclerView() {
        // Initialiser avec une liste vide, elle sera mise à jour après le chargement
        adapter = MissionAdapter(this, emptyList()) { mission ->
            claimMissionReward(mission)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MissionsActivity)
            adapter = this@MissionsActivity.adapter
        }
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        currentMissionType = "Daily"
                        updateMissionList()
                    }
                    1 -> {
                        currentMissionType = "General"
                        updateMissionList()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadMissions() {
        showLoading()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Charger toutes les missions
                val dailyResult = missionService.getDailyMissions().await()
                val generalResult = missionService.getGeneralMissions().await()

                // Convertir les documents Firestore en objets Mission
                dailyMissions.clear()
                generalMissions.clear()

                for (document in dailyResult) {
                    val mission = document.toObject(Mission::class.java).copy(id = document.id)
                    dailyMissions.add(mission)
                }

                for (document in generalResult) {
                    val mission = document.toObject(Mission::class.java).copy(id = document.id)
                    generalMissions.add(mission)
                }

                withContext(Dispatchers.Main) {
                    updateMissionList()
                    hideLoading()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du chargement des missions", e)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    showErrorToast("Impossible de charger les missions")
                }
            }
        }
    }

    private fun updateMissionList() {
        val missions = if (currentMissionType == "Daily") dailyMissions else generalMissions

        if (missions.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            adapter = MissionAdapter(this, missions) { mission ->
                claimMissionReward(mission)
            }
            recyclerView.adapter = adapter
        }
    }

    private fun claimMissionReward(mission: Mission) {
        if (!mission.isComplete() || mission.completed) {
            return
        }

        // Ajouter la récompense au solde de l'utilisateur
        val newCoins = userCoins + mission.reward
        userCoins = newCoins

        // Mettre à jour l'interface utilisateur
        coinsIndicator.text = "$userCoins coins"

        // Sauvegarder dans les préférences
        sharedPreferences.edit().putInt("user_coins", newCoins).apply()

        // Marquer la mission comme complétée sur Firebase
        CoroutineScope(Dispatchers.IO).launch {
            try {
                missionService.completeMission(mission.id).await()

                // Mettre à jour localement
                val localMission = if (mission.type == "Daily") {
                    dailyMissions.find { it.id == mission.id }
                } else {
                    generalMissions.find { it.id == mission.id }
                }

                localMission?.let {
                    it.javaClass.getDeclaredField("completed").apply {
                        isAccessible = true
                        set(it, true)
                    }
                }

                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                    Toast.makeText(
                        this@MissionsActivity,
                        "Récompense de ${mission.reward} coins réclamée !",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la réclamation de la récompense", e)
                withContext(Dispatchers.Main) {
                    showErrorToast("Impossible de réclamer la récompense")
                }
            }
        }
    }

    private fun showLoading() {
        loadingIndicator.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateView.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingIndicator.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        recyclerView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Méthode pour initialiser les missions pendant le développement
     * Accessible via un long clic sur le titre "Missions"
     */
    private fun setupDeveloperOptions() {
        val missionsTitle = findViewById<TextView>(R.id.missionsTitle)

        missionsTitle.setOnLongClickListener {
            // Afficher un dialogue pour choisir l'action de développement
            AlertDialog.Builder(this)
                .setTitle("Options développeur")
                .setItems(arrayOf("Initialiser les missions", "Réinitialiser les missions")) { _, which ->
                    val initializer = FirebaseMissionsInitializer(this)
                    when (which) {
                        0 -> initializer.initializeMissions { success ->
                            if (success) loadMissions()
                        }
                        1 -> initializer.resetAllMissions { success ->
                            if (success) loadMissions()
                        }
                    }
                }
                .setNegativeButton("Annuler", null)
                .show()
            true
        }
    }
}