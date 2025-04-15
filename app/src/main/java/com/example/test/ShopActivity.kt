// app/src/main/java/com/example/test/ShopActivity.kt
package com.example.test

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ShopActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private val itemList = mutableListOf<Item>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)

        // Configurer la Toolbar avec un bouton de retour
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.shopToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Boutique"

        // Configurer la RecyclerView
        recyclerView = findViewById(R.id.shopRecyclerView)
        val spanCount = 2 // nombre de colonnes
        recyclerView.layoutManager = GridLayoutManager(this, spanCount)

        // Initialiser l'adaptateur
        adapter = ItemAdapter(this, itemList)
        recyclerView.adapter = adapter

        // Charger les données depuis Firestore
        loadShopItems()
    }

    private fun loadShopItems() {
        val db = Firebase.firestore
        db.collection("Boutique")
            .get()
            .addOnSuccessListener { result ->
                itemList.clear()
                for (document in result) {
                    val item = document.toObject(Item::class.java)
                    itemList.add(item)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error fetching documents", exception)
                Toast.makeText(this, "Erreur: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}