package com.example.test

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class MissionService {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("Missions")

    /**
     * Récupère toutes les missions
     */
    fun getAllMissions(): Task<QuerySnapshot> {
        return collection.get()
    }

    /**
     * Récupère toutes les missions quotidiennes
     */
    fun getDailyMissions(): Task<QuerySnapshot> {
        return collection
            .whereEqualTo("type", "Daily")
            .get()
    }

    /**
     * Récupère toutes les missions générales
     */
    fun getGeneralMissions(): Task<QuerySnapshot> {
        return collection
            .whereEqualTo("type", "General")
            .get()
    }

    /**
     * Récupère une mission par son ID
     */
    fun getMissionById(missionId: String): Task<DocumentSnapshot> {
        return collection.document(missionId).get()
    }

    /**
     * Ajoute une nouvelle mission
     */
    fun addMission(mission: Mission): Task<DocumentReference> {
        return collection.add(mission)
    }

    /**
     * Met à jour une mission existante
     */
    fun updateMission(missionId: String, mission: Mission): Task<Void> {
        return collection.document(missionId).set(mission)
    }

    /**
     * Met à jour la progression d'une mission
     */
    fun updateMissionProgress(missionId: String, currentProgress: Int): Task<Void> {
        return collection.document(missionId).update("currentProgress", currentProgress)
    }

    /**
     * Marque une mission comme complétée
     */
    fun completeMission(missionId: String): Task<Void> {
        return collection.document(missionId).update("completed", true)
    }

    /**
     * Récupère toutes les missions non-complétées
     */
    fun getActiveMissions(): Task<QuerySnapshot> {
        return collection
            .whereEqualTo("completed", false)
            .get()
    }

    // Assurez-vous également d'ajouter cette méthode au MissionService si elle n'existe pas déjà
// Dans MissionService.kt :
    /**
     * Supprime une mission
     * @param missionId Identifiant de la mission à supprimer
     * @return Task représentant l'opération
     */
    fun deleteMission(missionId: String): Task<Void> {
        return collection.document(missionId).delete()
    }
}