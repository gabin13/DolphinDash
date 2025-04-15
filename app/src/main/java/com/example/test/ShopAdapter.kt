// app/src/main/java/com/example/dolphindash/adapters/ShopAdapter.kt
package com.example.dolphindash.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.dolphindash.model.ShopItem
import com.example.test.R

class ShopAdapter(private val shopItems: List<ShopItem>) :
    RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        val itemName: TextView = itemView.findViewById(R.id.item_name)
        val itemDescription: TextView = itemView.findViewById(R.id.item_description)
        val itemPrice: TextView = itemView.findViewById(R.id.item_price)
        val buyButton: Button = itemView.findViewById(R.id.buy_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val item = shopItems[position]

        holder.itemImage.setImageResource(item.imageResId)
        holder.itemName.text = item.name
        holder.itemDescription.text = item.description
        holder.itemPrice.text = "${item.price} coins"

        holder.buyButton.setOnClickListener {
            // Implémentez ici la logique d'achat
            Toast.makeText(
                holder.itemView.context,
                "Achat de ${item.name} pour ${item.price} coins",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun getItemCount() = shopItems.size
}