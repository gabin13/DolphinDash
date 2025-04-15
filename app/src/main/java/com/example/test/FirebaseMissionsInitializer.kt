package com.example.test

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Classe utilitaire pour initialiser les données de missions dans Firebase
 * À utiliser uniquement pendant le développement
 */
class FirebaseMissionsInitializer(private val context: Context) {
    private val TAG = "MissionsInitializer"
    private val db = FirebaseFirestore.getInstance()
    private val missionsCollection = db.collection("Missions")

    /**
     * Initialise les missions dans Firebase avec des exemples
     */
    fun initializeMissions(onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Vérifier si la collection existe déjà et contient des données
                val existingMissions = missionsCollection.get().await()

                if (!existingMissions.isEmpty) {
                    Log.d(TAG, "Des missions existent déjà, initialisation ignorée.")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Des missions existent déjà", Toast.LENGTH_SHORT).show()
                        onComplete(false)
                    }
                    return@launch
                }

                // Ajouter les missions quotidiennes d'exemple
                val dailyMissions = createSampleDailyMissions()
                for (mission in dailyMissions) {
                    missionsCollection.add(mission).await()
                }

                // Ajouter les missions générales d'exemple
                val generalMissions = createSampleGeneralMissions()
                for (mission in generalMissions) {
                    missionsCollection.add(mission).await()
                }

                Log.d(TAG, "Missions initialisées avec succès!")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Missions initialisées avec succès!", Toast.LENGTH_SHORT).show()
                    onComplete(true)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'initialisation des missions", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                    onComplete(false)
                }
            }
        }
    }

    /**
     * Crée des exemples de missions quotidiennes
     */
    private fun createSampleDailyMissions(): List<Mission> {
        return listOf(
            Mission(
                title = "Jouer 3 parties",
                description = "Jouez à 3 parties de Dolphin Dash aujourd'hui",
                reward = 50,
                type = "Daily",
                progressRequired = 3,
                currentProgress = 0,
                imageURL = "https://firebasestorage.googleapis.com/v0/b/dolphindash-8e132.appspot.com/o/missions%2Fplay3.png?alt=media"
            ),
            Mission(
                title = "Atteindre 500 points",
                description = "Faites un score d'au moins 500 points en une partie",
                reward = 75,
                type = "Daily",
                progressRequired = 1,
                currentProgress = 0,
                imageURL = "https://firebasestorage.googleapis.com/v0/b/dolphindash-8e132.appspot.com/o/missions%2Fscore500.png?alt=media"
            ),
            Mission(
                title = "Éviter 10 obstacles",
                description = "Évitez 10 obstacles sans collision",
                reward = 40,
                type = "Daily",
                progressRequired = 10,
                currentProgress = 0,
                imageURL = "https://firebasestorage.googleapis.com/v0/b/dolphindash-8e132.appspot.com/o/missions%2Fobstacles.png?alt=media"
            ),
            Mission(
                title = "Connectez-vous",
                description = "Connectez-vous à l'application aujourd'hui",
                reward = 25,
                type = "Daily",
                progressRequired = 1,
                currentProgress = 1, // Déjà complété automatiquement
                imageURL = "https://firebasestorage.googleapis.com/v0/b/dolphindash-8e132.appspot.com/o/missions%2Flogin.png?alt=media"
            )
        )
    }

    /**
     * Crée des exemples de missions générales (de progression)
     */
    private fun createSampleGeneralMissions(): List<Mission> {
        return listOf(
            Mission(
                title = "Nageur débutant",
                description = "Atteignez un score cumulé de 2 000 points",
                reward = 100,
                type = "General",
                progressRequired = 2000,
                currentProgress = 0,
                imageURL = "https://firebasestorage.googleapis.com/v0/b/dolphindash-8e132.appspot.com/o/missions%2Fbeginner.png?alt=media"
            ),
            Mission(
                title = "Nageur intermédiaire",
                description = "Atteignez un score cumulé de 5 000 points",
                reward = 200,
                type = "General",
                progressRequired = 5000,
                currentProgress = 0,
                imageURL = "https://firebasestorage.googleapis.com/v0/b/dolphindash-8e132.appspot.com/o/missions%2Fintermediate.png?alt=media"
            ),
            Mission(
                title = "Nageur expert",
                description = "Atteignez un score cumulé de 10 000 points",
                reward = 500,
                type = "General",
                progressRequired = 10000,
                currentProgress = 0,
                imageURL = "https://firebasestorage.googleapis.com/v0/b/dolphindash-8e132.appspot.com/o/missions%2Fexpert.png?alt=media"
            ),
            Mission(
                title = "Joueur dévoué",
                description = "Jouez 50 parties au total",
                reward = 300,
                type = "General",
                progressRequired = 50,
                currentProgress = 0,
                imageURL = "https://firebasestorage.googleapis.com/v0/b/dolphindash-8e132.appspot.com/o/missions%2Fdedicated.png?alt=media"
            ),
            Mission(
                title = "Collectionneur",
                description = "Achetez 5 objets dans la boutique",
                reward = 250,
                type = "General",
                progressRequired = 5,
                currentProgress = 0,
                imageURL = "https://firebasestorage.googleapis.com/v0/b/dolphindash-8e132.appspot.com/o/missions%2Fcollector.png?alt=media"
            )
        )
    }

    /**
     * Réinitialise toutes les missions dans Firebase (en supprime l'ensemble)
     * Utilisé uniquement pour le développement/test
     */
    fun resetAllMissions(onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Récupérer toutes les missions
                val missions = missionsCollection.get().await()

                // Supprimer chaque mission
                for (mission in missions) {
                    missionsCollection.document(mission.id).delete().await()
                }

                Log.d(TAG, "Toutes les missions ont été supprimées avec succès")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Missions réinitialisées", Toast.LENGTH_SHORT).show()
                    onComplete(true)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la réinitialisation des missions", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            }
        }
    }
}