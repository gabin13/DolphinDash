// app/src/main/java/com/example/dolphindash/ui/shop/ShopFragment.kt
package com.example.dolphindash.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dolphindash.R
import com.example.dolphindash.adapters.ShopAdapter
import com.example.dolphindash.model.ShopItem

class ShopFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var shopAdapter: ShopAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_shop, container, false)
        recyclerView = root.findViewById(R.id.shop_recycler_view)
        setupRecyclerView()
        return root
    }

    private fun setupRecyclerView() {
        // Liste d'exemple d'items (à remplacer par vos propres données)
        val shopItems = listOf(
            ShopItem(
                1,
                "Super Vitesse",
                "Augmente la vitesse de votre dauphin de 20%",
                100,
                R.drawable.shop_speed
            ),
            ShopItem(
                2,
                "Bouclier Protecteur",
                "Protège contre les obstacles pendant 10 secondes",
                150,
                R.drawable.shop_shield
            ),
            ShopItem(
                3,
                "Multiplicateur de Pièces",
                "Double les pièces collectées pendant 30 secondes",
                200,
                R.drawable.shop_coin_multiplier
            ),
            ShopItem(
                4,
                "Dauphin Doré",
                "Skin exclusif pour votre dauphin",
                500,
                R.drawable.shop_golden_dolphin
            )
            // Ajoutez d'autres items selon vos besoins
        )

        shopAdapter = ShopAdapter(shopItems)
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, 1) // Utiliser 2 pour une grille à deux colonnes
            adapter = shopAdapter
        }
    }
}