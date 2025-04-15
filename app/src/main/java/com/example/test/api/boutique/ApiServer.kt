package com.example.test.api.boutique

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * Serveur HTTP léger pour exposer l'API de la boutique
 * Utilise NanoHTTPD pour fournir un serveur web simple
 */
class ApiServer(private val context: Context) : NanoHTTPD(API_PORT) {

    companion object {
        private const val TAG = "ApiServer"
        private const val API_PORT = 8080
    }

    private val shopController = ShopController(context)

    init {
        // Configuration du serveur
        try {
            start(SOCKET_READ_TIMEOUT, false)
            Log.i(TAG, "API server running on port $API_PORT")
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
                if (headers.containsKey("content-type") && headers["content-type"]?.contains("application/json") == true) {
                    // Pour les requêtes JSON, utiliser le contenu du bodyMap ou tenter une lecture limitée
                    val bodyMap = HashMap<String, String>()
                    session.parseBody(bodyMap)

                    // Essayer d'obtenir d'abord le corps via la méthode standard
                    body = bodyMap["postData"] ?: ""

                    // Si le corps est vide mais qu'il y a un content-length, essayer de lire quelques octets
                    if (body.isBlank() && headers.containsKey("content-length") && headers["content-length"]?.toIntOrNull() ?: 0 > 0) {
                        try {
                            // Utiliser une lecture avec timeout pour éviter le blocage
                            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                            val buffer = ByteArray(contentLength.coerceAtMost(8192))  // Limiter à 8Ko max

                            // Configurer un timeout de lecture
                            session.inputStream.readNBytes(buffer, 0, buffer.size)
                            body = String(buffer, Charsets.UTF_8).trim { it <= ' ' }
                        } catch (e: Exception) {
                            Log.w(TAG, "Échec de la lecture directe du flux: ${e.message}")
                            // Continuer avec le body tel qu'il est (potentiellement vide)
                        }
                    }
                } else {
                    // Méthode standard pour les autres types de contenu
                    val bodyMap = HashMap<String, String>()
                    session.parseBody(bodyMap)
                    body = bodyMap["postData"] ?: ""
                }

                Log.d(TAG, "Corps reçu: $body")
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

        // Traitement synchrone de la requête
        return try {
            // Utiliser runBlocking pour attendre le résultat de façon synchrone
            val result = runBlocking {
                shopController.handleRequest(cleanPath, method, body)
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