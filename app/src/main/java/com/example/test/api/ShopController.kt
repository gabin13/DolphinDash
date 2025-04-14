package com.example.test.api

import android.content.Context
import android.util.Log
import com.example.test.Item
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

/**
 * Contrôleur pour exposer les fonctionnalités de la boutique via une API
 */
class ShopController(private val context: Context) {
    private val shopService = ShopService()
    private val TAG = "ShopController"

    /**
     * Méthode principale pour traiter les requêtes API
     * @param path Chemin de la requête (ex: "items", "items/123")
     * @param method Méthode HTTP (GET, POST, PUT, DELETE)
     * @param body Corps de la requête (pour POST et PUT)
     * @return Réponse au format JSONObject ou JSONArray
     */
    suspend fun handleRequest(path: String, method: String, body: String? = null): String {
        return withContext(Dispatchers.IO) {
            try {
                val segments = path.split("/").filter { it.isNotEmpty() }

                when {
                    // Liste des items
                    segments.isEmpty() && method == "GET" -> {
                        getAllItems()
                    }

                    // Récupérer un item spécifique
                    segments.size == 1 && method == "GET" -> {
                        val itemId = segments[0]
                        getItemById(itemId)
                    }

                    // Recherche d'items par nom
                    segments.size == 2 && segments[0] == "search" && method == "GET" -> {
                        val query = segments[1]
                        searchItems(query)
                    }

                    // Recherche d'items par prix max
                    segments.size == 2 && segments[0] == "price" && method == "GET" -> {
                        val maxPrice = segments[1].toIntOrNull() ?: 0
                        getItemsBelowPrice(maxPrice)
                    }

                    // Ajouter un item
                    segments.isEmpty() && method == "POST" && body != null -> {
                        addItem(body)
                    }

                    // Mettre à jour un item
                    segments.size == 1 && method == "PUT" && body != null -> {
                        val itemId = segments[0]
                        updateItem(itemId, body)
                    }

                    // Supprimer un item
                    segments.size == 1 && method == "DELETE" -> {
                        val itemId = segments[0]
                        deleteItem(itemId)
                    }

                    else -> {
                        createErrorResponse("Route non valide ou méthode non supportée")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur dans le traitement de la requête", e)
                createErrorResponse("Erreur interne: ${e.message}")
            }
        }
    }

    private suspend fun getAllItems(): String {
        val items = shopService.getAllItems().await()
        val jsonArray = JSONArray()

        Log.d("ShopController", "Nombre d'items récupérés: ${items.documents.size}")

        for (document in items.documents) {
            val item = document.toObject(Item::class.java)
            item?.let {
                val itemJson = JSONObject().apply {
                    put("id", document.id)
                    put("nom", it.nom)
                    put("prix", it.prix)
                    put("imageURL", it.imageURL)
                }
                jsonArray.put(itemJson)
                Log.d("ShopController", "Item ajouté à la réponse: ${document.id} - ${it.nom}")
            }
        }

        val responseStr = jsonArray.toString()
        Log.d("ShopController", "Réponse complète: $responseStr")
        return responseStr
    }

    private suspend fun getItemById(itemId: String): String {
        val document = shopService.getItemById(itemId).await()

        return if (document.exists()) {
            val item = document.toObject(Item::class.java)
            item?.let {
                JSONObject().apply {
                    put("id", document.id)
                    put("nom", it.nom)
                    put("prix", it.prix)
                    put("imageURL", it.imageURL)
                }
            }?.toString() ?: createErrorResponse("Erreur lors de la conversion de l'item")
        } else {
            createErrorResponse("Item non trouvé", 404)
        }
    }

    private suspend fun searchItems(query: String): String {
        val items = shopService.searchItemsByName(query).await()
        val jsonArray = JSONArray()

        for (document in items.documents) {
            val item = document.toObject(Item::class.java)
            item?.let {
                val itemJson = JSONObject().apply {
                    put("id", document.id)
                    put("nom", it.nom)
                    put("prix", it.prix)
                    put("imageURL", it.imageURL)
                }
                jsonArray.put(itemJson)
            }
        }
        Log.d("ShopController", "Réponse renvoyée: $jsonArray")
        return jsonArray.toString()
    }

    private suspend fun getItemsBelowPrice(maxPrice: Int): String {
        val items = shopService.getItemsBelowPrice(maxPrice).await()
        val jsonArray = JSONArray()

        for (document in items.documents) {
            val item = document.toObject(Item::class.java)
            item?.let {
                val itemJson = JSONObject().apply {
                    put("id", document.id)
                    put("nom", it.nom)
                    put("prix", it.prix)
                    put("imageURL", it.imageURL)
                }
                jsonArray.put(itemJson)
            }
        }
        Log.d("ShopController", "Réponse renvoyée: $jsonArray")
        return jsonArray.toString()
    }

    private suspend fun addItem(body: String): String {
        try {
            val json = JSONObject(body)
            val item = Item(
                nom = json.optString("nom", ""),
                prix = json.optInt("prix", 0),
                imageURL = json.optString("imageURL", "")
            )

            val documentRef = shopService.addItem(item).await()

            return JSONObject().apply {
                put("id", documentRef.id)
                put("message", "Item ajouté avec succès")
            }.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'ajout d'un item", e)
            return createErrorResponse("Erreur lors de l'ajout: ${e.message}")
        }
    }

    private suspend fun updateItem(itemId: String, body: String): String {
        try {
            val json = JSONObject(body)

            // Vérifier si l'item existe
            val document = shopService.getItemById(itemId).await()
            if (!document.exists()) {
                return createErrorResponse("Item non trouvé", 404)
            }

            val item = Item(
                id = itemId,
                nom = json.optString("nom", ""),
                prix = json.optInt("prix", 0),
                imageURL = json.optString("imageURL", "")
            )

            shopService.updateItem(itemId, item).await()

            return JSONObject().apply {
                put("id", itemId)
                put("message", "Item mis à jour avec succès")
            }.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise à jour d'un item", e)
            return createErrorResponse("Erreur lors de la mise à jour: ${e.message}")
        }
    }

    private suspend fun deleteItem(itemId: String): String {
        try {
            // Vérifier si l'item existe
            val document = shopService.getItemById(itemId).await()
            if (!document.exists()) {
                return createErrorResponse("Item non trouvé", 404)
            }

            shopService.deleteItem(itemId).await()

            return JSONObject().apply {
                put("message", "Item supprimé avec succès")
            }.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression d'un item", e)
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