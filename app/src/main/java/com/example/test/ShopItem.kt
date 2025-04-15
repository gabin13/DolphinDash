// app/src/main/java/com/example/dolphindash/model/ShopItem.kt
package com.example.dolphindash.model

data class ShopItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Int,
    val imageResId: Int
)