package com.example.test

data class Item(
    val id: String = "",
    val nom: String = "",
    val imageURL: String = "",
    val prix: Int = 0
) {
    // Constructeur sans arguments pour Firestore
    constructor() : this("", "", "", 0)
}