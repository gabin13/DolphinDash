package com.example.test

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class ItemAdapter(private val context: Context, private val itemList: List<Item>) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nom: TextView = view.findViewById(R.id.nomItem)
        val prix: TextView = view.findViewById(R.id.prixItem)
        val image: ImageView = view.findViewById(R.id.imageItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itemList[position]
        holder.nom.text = item.nom
        holder.prix.text = "${item.prix} €"
        Glide.with(context).load(item.imageURL).into(holder.image)
    }

    override fun getItemCount(): Int = itemList.size
}
