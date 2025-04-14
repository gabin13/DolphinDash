package com.example.test.api

import android.content.Context
import android.util.Log
import com.example.test.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import fi.iki.elonen.NanoHTTPD
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
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

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
            val bodyMap = HashMap<String, String>()
            try {
                session.parseBody(bodyMap)
                body = bodyMap["postData"] ?: ""
            } catch (e: IOException) {
                Log.e(TAG, "Erreur lors de la lecture du corps de la requête", e)
                return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "application/json",
                    "{\"error\":true,\"message\":\"Erreur lors de la lecture du corps de la requête\"}"
                )
            } catch (e: ResponseException) {
                Log.e(TAG, "Erreur lors de la lecture du corps de la requête", e)
                return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "application/json",
                    "{\"error\":true,\"message\":\"${e.message}\"}"
                )
            }
        }

        // Préparation de la réponse différée
        val r = newFixedLengthResponse("")

        // Traitement asynchrone de la requête
        coroutineScope.launch {
            try {
                // Nettoyage du chemin (enlever /api et normaliser)
                val cleanPath = uri.replace("/api", "").replace(Regex("^/+"), "")

                // Traitement de la requête par le contrôleur
                val result = shopController.handleRequest(cleanPath, method, body)

                // Envoi de la réponse
                r.status = Response.Status.OK
                r.mimeType = "application/json"
                r.data = result.byteInputStream()

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du traitement de la requête", e)
                r.status = Response.Status.INTERNAL_ERROR
                r.mimeType = "application/json"
                r.data = "{\"error\":true,\"message\":\"Erreur serveur interne\"}".byteInputStream()
            }
        }

        return r
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