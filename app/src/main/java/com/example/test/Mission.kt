package com.example.test

data class Mission(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val reward: Int = 0,
    val type: String = "", // Daily ou General
    val completed: Boolean = false,
    val progressRequired: Int = 1,
    val currentProgress: Int = 0,
    val imageURL: String = ""
) {
    // Constructeur sans arguments pour Firestore
    constructor() : this("", "", "", 0, "", false, 1, 0, "")

    // Calculer le pourcentage de progression
    fun getProgressPercentage(): Int {
        if (progressRequired <= 0) return 100
        val percentage = (currentProgress.toFloat() / progressRequired.toFloat()) * 100
        return percentage.toInt().coerceIn(0, 100)
    }

    // Vérifier si la mission est complète
    fun isComplete(): Boolean {
        return completed || currentProgress >= progressRequired
    }
}