package com.example.test.api

import android.content.Context
import android.util.Log
import com.example.test.api.boutique.ShopController
import com.example.test.api.missions.MissionController
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.*

/**
 * Serveur HTTP léger pour exposer les API de l'application
 * Utilise NanoHTTPD pour fournir un serveur web simple
 */
class ApiServer(private val context: Context) : NanoHTTPD(API_PORT) {

    companion object {
        private const val TAG = "ApiServer"
        private const val API_PORT = 8080

        // Préfixes d'API
        private const val SHOP_API_PREFIX = "shop"
        private const val MISSIONS_API_PREFIX = "missions"
    }

    private val shopController = ShopController(context)
    private val missionController = MissionController(context)

    init {
        // Configuration du serveur
        try {
            start(SOCKET_READ_TIMEOUT, false)
            Log.i(TAG, "API server running on port $API_PORT")
            Log.i(TAG, "Shop API available at: /api/$SHOP_API_PREFIX/")
            Log.i(TAG, "Missions API available at: /api/$MISSIONS_API_PREFIX/")
        } catch (e: IOException) {
            Log.e(TAG, "Impossible de démarrer le serveur API", e)
        }
    }

    /**
     * Méthode appelée à chaque requête HTTP reçue
     */
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method.name
        val headers = session.headers
        val clientAddress = session.remoteHostName

        Log.d(TAG, "======== REQUÊTE REÇUE ========")
        Log.d(TAG, "Méthode: $method")
        Log.d(TAG, "URI: $uri")
        Log.d(TAG, "Client: $clientAddress")
        Log.d(TAG, "Headers: $headers")

        var body = ""

        // Lecture du corps pour les méthodes POST et PUT
        if (method == "POST" || method == "PUT") {
            try {
                // Récupérer la taille du contenu
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0

                if (contentLength > 0) {
                    // Créer une map pour stocker les paramètres de formulaire et les fichiers
                    val files = HashMap<String, String>()

                    // Parse les paramètres de la session
                    // Cette étape est nécessaire pour que NanoHTTPD traite le corps de la requête
                    session.parseBody(files)

                    // Le corps de la requête JSON est stocké sous la clé "postData"
                    body = files["postData"] ?: ""

                    Log.d(TAG, "Corps reçu: $body")
                } else {
                    Log.w(TAG, "Corps vide reçu (content-length: $contentLength)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la lecture du corps de la requête", e)
                return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "application/json",
                    "{\"error\":true,\"message\":\"Erreur lors de la lecture du corps de la requête: ${e.message}\"}"
                )
            }
        }

        // Nettoyage du chemin (enlever /api et normaliser)
        val cleanPath = uri.replace("/api", "").replace(Regex("^/+"), "")
        Log.d(TAG, "Chemin nettoyé: $cleanPath")

        // Déterminer le contrôleur à utiliser en fonction du préfixe de la route
        return try {
            // Utiliser runBlocking pour attendre le résultat de façon synchrone
            val result = runBlocking {
                // Extraire le préfixe et le reste du chemin
                val pathParts = cleanPath.split("/", limit = 2)
                val prefix = pathParts[0]
                val remainingPath = if (pathParts.size > 1) pathParts[1] else ""

                when (prefix) {
                    SHOP_API_PREFIX -> {
                        Log.d(TAG, "Routage vers l'API Boutique: /$remainingPath")
                        shopController.handleRequest(remainingPath, method, body)
                    }
                    MISSIONS_API_PREFIX -> {
                        Log.d(TAG, "Routage vers l'API Missions: /$remainingPath")
                        missionController.handleRequest(remainingPath, method, body)
                    }
                    else -> {
                        Log.d(TAG, "Route inconnue: /$cleanPath")
                        createErrorResponse("Route inconnue. Les préfixes d'API valides sont: /$SHOP_API_PREFIX/ ou /$MISSIONS_API_PREFIX/")
                    }
                }
            }

            Log.d(TAG, "Résultat obtenu: $result")
            newFixedLengthResponse(Response.Status.OK, "application/json", result)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du traitement de la requête", e)
            e.printStackTrace()
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                "{\"error\":true,\"message\":\"Erreur serveur interne: ${e.message}\"}"
            )
        }
    }

    private fun createErrorResponse(message: String, code: Int = 400): String {
        return "{\"error\":true,\"code\":$code,\"message\":\"$message\"}"
    }

    /**
     * Arrête le serveur API
     */
    override fun stop() {
        try {
            Log.i(TAG, "Arrêt du serveur API")
            super.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'arrêt du serveur API", e)
        }
    }
}