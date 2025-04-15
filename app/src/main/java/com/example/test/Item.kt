package com.example.test


data class Item(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0,
    val imageUrl: String = "",
    val type: String = "",
    val purchased: Boolean = false
) {
    // Constructeur sans arguments pour Firestore
    constructor() : this("", "", "", 0)
}