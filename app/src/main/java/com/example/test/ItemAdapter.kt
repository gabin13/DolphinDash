// app/src/main/java/com/example/test/ItemAdapter.kt
package com.example.test

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ItemAdapter(private val context: Context, private val items: List<Item>) :
    RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemImage: ImageView = view.findViewById(R.id.itemImage)
        val itemName: TextView = view.findViewById(R.id.itemName)
        val itemDescription: TextView = view.findViewById(R.id.itemDescription)
        val itemPrice: TextView = view.findViewById(R.id.itemPrice)
        val buyButton: Button = view.findViewById(R.id.buyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.itemName.text = item.name
        holder.itemDescription.text = item.description
        holder.itemPrice.text = "${item.price} coins"

        // Charger l'image avec Glide (assurez-vous d'avoir ajouté la dépendance Glide)
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(item.imageUrl)
                .into(holder.itemImage)
        } else {
        }

        holder.buyButton.setOnClickListener {
            // Logique d'achat à implémenter
            Toast.makeText(context, "Achat de ${item.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = items.size
}