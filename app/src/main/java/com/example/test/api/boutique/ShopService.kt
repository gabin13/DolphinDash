package com.example.test.api.boutique

import com.example.test.Item
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot


class ShopService {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("Boutique")

    /**
     * Récupère tous les items de la boutique
     * @return Task contenant le résultat de la requête
     */
    fun getAllItems(): Task<QuerySnapshot> {
        return collection.get()
    }

    /**
     * Récupère un item par son ID
     * @param itemId ID de l'item à récupérer
     * @return Task contenant le résultat de la requête
     */
    fun getItemById(itemId: String): Task<DocumentSnapshot> {
        return collection.document(itemId).get()
    }

    /**
     * Ajoute un nouvel item dans la boutique
     * @param item L'item à ajouter
     * @return Task contenant la référence du document créé
     */
    fun addItem(item: Item): Task<DocumentReference> {
        return collection.add(item)
    }

    /**
     * Met à jour un item existant
     * @param itemId ID de l'item à mettre à jour
     * @param item Nouvelles données de l'item
     * @return Task indiquant le succès ou l'échec de l'opération
     */
    fun updateItem(itemId: String, item: Item): Task<Void> {
        return collection.document(itemId).set(item)
    }

    /**
     * Supprime un item de la boutique
     * @param itemId ID de l'item à supprimer
     * @return Task indiquant le succès ou l'échec de l'opération
     */
    fun deleteItem(itemId: String): Task<Void> {
        return collection.document(itemId).delete()
    }

    /**
     * Recherche des items par nom
     * @param name Nom ou partie du nom à rechercher
     * @return Task contenant le résultat de la requête
     */
    fun searchItemsByName(name: String): Task<QuerySnapshot> {
        val searchName = name.lowercase()
        return collection
            .whereGreaterThanOrEqualTo("nom", searchName)
            .whereLessThanOrEqualTo("nom", searchName + "\uf8ff")
            .get()
    }

    /**
     * Récupère les items dont le prix est inférieur à la valeur spécifiée
     * @param maxPrice Prix maximum
     * @return Task contenant le résultat de la requête
     */
    fun getItemsBelowPrice(maxPrice: Int): Task<QuerySnapshot> {
        return collection
            .whereLessThanOrEqualTo("prix", maxPrice)
            .get()
    }
}