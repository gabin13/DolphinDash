package com.example.test.api.missions

import android.content.Context
import android.util.Log
import com.example.test.Mission
import com.example.test.MissionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

/**
 * Contrôleur pour exposer les fonctionnalités des missions via une API
 */
class MissionController(private val context: Context) {
    private val missionService = MissionService()
    private val TAG = "MissionController"

    /**
     * Méthode principale pour traiter les requêtes API
     * @param path Chemin de la requête (ex: "missions", "missions/123")
     * @param method Méthode HTTP (GET, POST, PUT, DELETE)
     * @param body Corps de la requête (pour POST et PUT)
     * @return Réponse au format JSON
     */
    suspend fun handleRequest(path: String, method: String, body: String? = null): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Traitement de la requête: $method $path")
                val segments = path.split("/").filter { it.isNotEmpty() }

                when {
                    // Liste de toutes les missions
                    path.isEmpty() && method == "GET" -> {
                        Log.d(TAG, "Route: GET /")
                        getAllMissions()
                    }

                    // Récupérer une mission spécifique
                    segments.size == 1 && !listOf("type", "active", "progress", "complete").contains(segments[0]) && method == "GET" -> {
                        val missionId = segments[0]
                        Log.d(TAG, "Route: GET /$missionId")
                        getMissionById(missionId)
                    }

                    // Filtrer les missions par type (Daily/General)
                    segments.size == 2 && segments[0] == "type" && method == "GET" -> {
                        val type = segments[1]
                        Log.d(TAG, "Route: GET /type/$type")
                        getMissionsByType(type)
                    }

                    // Récupérer les missions actives
                    segments.size == 1 && segments[0] == "active" && method == "GET" -> {
                        Log.d(TAG, "Route: GET /active")
                        getActiveMissions()
                    }

                    // Ajouter une mission
                    path.isEmpty() && method == "POST" && body != null -> {
                        Log.d(TAG, "Route: POST /")
                        addMission(body)
                    }

                    // Mettre à jour une mission
                    segments.size == 1 && !listOf("type", "active", "progress", "complete").contains(segments[0]) && method == "PUT" && body != null -> {
                        val missionId = segments[0]
                        Log.d(TAG, "Route: PUT /$missionId")
                        updateMission(missionId, body)
                    }

                    // Pour compatibilité avec l'ancien code qui utilisait POST pour mise à jour
                    segments.size == 1 && !listOf("type", "active", "progress", "complete").contains(segments[0]) && method == "POST" && body != null -> {
                        val missionId = segments[0]
                        Log.d(TAG, "Route: POST /$missionId (considéré comme PUT)")
                        updateMission(missionId, body)
                    }

                    // Mettre à jour la progression d'une mission
                    segments.size == 2 && segments[0] == "progress" && method == "PUT" && body != null -> {
                        val missionId = segments[1]
                        Log.d(TAG, "Route: PUT /progress/$missionId")
                        updateMissionProgress(missionId, body)
                    }

                    // Pour compatibilité avec l'ancien code qui utilisait POST pour mise à jour
                    segments.size == 2 && segments[0] == "progress" && method == "POST" && body != null -> {
                        val missionId = segments[1]
                        Log.d(TAG, "Route: POST /progress/$missionId (considéré comme PUT)")
                        updateMissionProgress(missionId, body)
                    }

                    // Marquer une mission comme complétée
                    segments.size == 2 && segments[0] == "complete" && (method == "PUT" || method == "POST") -> {
                        val missionId = segments[1]
                        Log.d(TAG, "Route: ${method} /complete/$missionId")
                        completeMission(missionId)
                    }

                    // Supprimer une mission
                    segments.size == 1 && !listOf("type", "active", "progress", "complete").contains(segments[0]) && method == "DELETE" -> {
                        val missionId = segments[0]
                        Log.d(TAG, "Route: DELETE /$missionId")
                        deleteMission(missionId)
                    }

                    else -> {
                        Log.w(TAG, "Route non reconnue: $method $path")
                        createErrorResponse("Route non valide ou méthode non supportée")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur dans le traitement de la requête", e)
                createErrorResponse("Erreur interne: ${e.message}")
            }
        }
    }

    private suspend fun getAllMissions(): String {
        val missions = missionService.getAllMissions().await()
        val jsonArray = JSONArray()

        Log.d(TAG, "Nombre de missions récupérées: ${missions.documents.size}")

        for (document in missions.documents) {
            val mission = document.toObject(Mission::class.java)
            mission?.let {
                val missionJson = JSONObject().apply {
                    put("id", document.id)
                    put("title", it.title)
                    put("description", it.description)
                    put("reward", it.reward)
                    put("type", it.type)
                    put("completed", it.completed)
                    put("progressRequired", it.progressRequired)
                    put("currentProgress", it.currentProgress)
                    put("imageURL", it.imageURL)
                    // Ajouter le pourcentage de progression pour faciliter l'affichage
                    put("progressPercentage", it.getProgressPercentage())
                }
                jsonArray.put(missionJson)
            }
        }

        return jsonArray.toString()
    }

    private suspend fun getMissionById(missionId: String): String {
        val document = missionService.getMissionById(missionId).await()

        return if (document.exists()) {
            val mission = document.toObject(Mission::class.java)
            mission?.let {
                JSONObject().apply {
                    put("id", document.id)
                    put("title", it.title)
                    put("description", it.description)
                    put("reward", it.reward)
                    put("type", it.type)
                    put("completed", it.completed)
                    put("progressRequired", it.progressRequired)
                    put("currentProgress", it.currentProgress)
                    put("imageURL", it.imageURL)
                    put("progressPercentage", it.getProgressPercentage())
                }
            }?.toString() ?: createErrorResponse("Erreur lors de la conversion de la mission")
        } else {
            createErrorResponse("Mission non trouvée", 404)
        }
    }

    private suspend fun getMissionsByType(type: String): String {
        val missions = if (type.equals("Daily", ignoreCase = true)) {
            missionService.getDailyMissions().await()
        } else {
            missionService.getGeneralMissions().await()
        }

        val jsonArray = JSONArray()

        for (document in missions.documents) {
            val mission = document.toObject(Mission::class.java)
            mission?.let {
                val missionJson = JSONObject().apply {
                    put("id", document.id)
                    put("title", it.title)
                    put("description", it.description)
                    put("reward", it.reward)
                    put("type", it.type)
                    put("completed", it.completed)
                    put("progressRequired", it.progressRequired)
                    put("currentProgress", it.currentProgress)
                    put("imageURL", it.imageURL)
                    put("progressPercentage", it.getProgressPercentage())
                }
                jsonArray.put(missionJson)
            }
        }

        return jsonArray.toString()
    }

    private suspend fun getActiveMissions(): String {
        val missions = missionService.getActiveMissions().await()
        val jsonArray = JSONArray()

        for (document in missions.documents) {
            val mission = document.toObject(Mission::class.java)
            mission?.let {
                val missionJson = JSONObject().apply {
                    put("id", document.id)
                    put("title", it.title)
                    put("description", it.description)
                    put("reward", it.reward)
                    put("type", it.type)
                    put("completed", it.completed)
                    put("progressRequired", it.progressRequired)
                    put("currentProgress", it.currentProgress)
                    put("imageURL", it.imageURL)
                    put("progressPercentage", it.getProgressPercentage())
                }
                jsonArray.put(missionJson)
            }
        }

        return jsonArray.toString()
    }

    private suspend fun addMission(body: String): String {
        try {
            val json = JSONObject(body)
            val mission = Mission(
                title = json.optString("title", ""),
                description = json.optString("description", ""),
                reward = json.optInt("reward", 0),
                type = json.optString("type", ""),
                completed = json.optBoolean("completed", false),
                progressRequired = json.optInt("progressRequired", 1),
                currentProgress = json.optInt("currentProgress", 0),
                imageURL = json.optString("imageURL", "")
            )

            val documentRef = missionService.addMission(mission).await()

            return JSONObject().apply {
                put("id", documentRef.id)
                put("message", "Mission ajoutée avec succès")
            }.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'ajout d'une mission", e)
            return createErrorResponse("Erreur lors de l'ajout: ${e.message}")
        }
    }

    private suspend fun updateMission(missionId: String, body: String): String {
        try {
            val json = JSONObject(body)

            // Vérifier si la mission existe
            val document = missionService.getMissionById(missionId).await()
            if (!document.exists()) {
                return createErrorResponse("Mission non trouvée", 404)
            }

            val mission = Mission(
                title = json.optString("title", ""),
                description = json.optString("description", ""),
                reward = json.optInt("reward", 0),
                type = json.optString("type", ""),
                completed = json.optBoolean("completed", false),
                progressRequired = json.optInt("progressRequired", 1),
                currentProgress = json.optInt("currentProgress", 0),
                imageURL = json.optString("imageURL", "")
            )

            missionService.updateMission(missionId, mission).await()

            return JSONObject().apply {
                put("id", missionId)
                put("message", "Mission mise à jour avec succès")
            }.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise à jour d'une mission", e)
            return createErrorResponse("Erreur lors de la mise à jour: ${e.message}")
        }
    }

    private suspend fun updateMissionProgress(missionId: String, body: String): String {
        try {
            val json = JSONObject(body)
            val progress = json.optInt("progress", 0)

            // Vérifier si la mission existe
            val document = missionService.getMissionById(missionId).await()
            if (!document.exists()) {
                return createErrorResponse("Mission non trouvée", 404)
            }

            missionService.updateMissionProgress(missionId, progress).await()

            // Vérifier si la mission est maintenant complétée
            val updatedDocument = missionService.getMissionById(missionId).await()
            val mission = updatedDocument.toObject(Mission::class.java)

            var isNowCompleted = false
            if (mission != null && mission.currentProgress >= mission.progressRequired && !mission.completed) {
                missionService.completeMission(missionId).await()
                isNowCompleted = true
            }

            return JSONObject().apply {
                put("id", missionId)
                put("progress", progress)
                put("completed", isNowCompleted)
                put("message", "Progression mise à jour avec succès")
            }.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise à jour de la progression", e)
            return createErrorResponse("Erreur lors de la mise à jour: ${e.message}")
        }
    }

    private suspend fun completeMission(missionId: String): String {
        try {
            // Vérifier si la mission existe
            val document = missionService.getMissionById(missionId).await()
            if (!document.exists()) {
                return createErrorResponse("Mission non trouvée", 404)
            }

            missionService.completeMission(missionId).await()

            return JSONObject().apply {
                put("id", missionId)
                put("message", "Mission marquée comme complétée")
            }.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la complétion de la mission", e)
            return createErrorResponse("Erreur lors de la complétion: ${e.message}")
        }
    }

    private suspend fun deleteMission(missionId: String): String {
        try {
            // Vérifier si la mission existe
            val document = missionService.getMissionById(missionId).await()
            if (!document.exists()) {
                return createErrorResponse("Mission non trouvée", 404)
            }

            // Supprimer la mission
            missionService.deleteMission(missionId).await()

            return JSONObject().apply {
                put("message", "Mission supprimée avec succès")
            }.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression de la mission", e)
            return createErrorResponse("Erreur lors de la suppression: ${e.message}")
        }
    }

    private fun createErrorResponse(message: String, code: Int = 400): String {
        return JSONObject().apply {
            put("error", true)
            put("code", code)
            put("message", message)
        }.toString()
    }
}