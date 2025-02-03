package com.example.globetrotter.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.globetrotter.R
import com.example.globetrotter.activities.PlaceDetailsActivity
import com.example.globetrotter.database.Favorite
import com.squareup.picasso.Picasso

class FavoriteAdapter(
    private val context: Context,
    private val favorites: List<Favorite>,
    private val onDeleteClicked: ((Favorite, Int) -> Unit)? = null // Callback for delete action
) : RecyclerView.Adapter<FavoriteAdapter.PlaceViewHolder>() {

    private val mutableFavorites = favorites.toMutableList()

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.favoriteImage)
        val name: TextView = itemView.findViewById(R.id.favoriteName)
        val address: TextView = itemView.findViewById(R.id.address)
        val deleteButton : ImageView? = itemView.findViewById(R.id.deleteButton)

        fun bind(favorite: Favorite) {
            name.text = favorite.place.name
            address.text = favorite.place.address
            favorite.place.imageUrl?.let {
                if (favorite.place.imageUrl.isNotEmpty())
                    Picasso.get().load(it).into(icon)
                else
                    icon.setImageResource(favorite.place.categoryIcon)
            }

            deleteButton?.setOnClickListener {
                onDeleteClicked?.invoke(favorite, adapterPosition) // Trigger callback
            }

            itemView.setOnClickListener {
                 val intent = Intent(context, PlaceDetailsActivity::class.java)
                intent.putExtra("place", favorite.place)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = mutableFavorites[position]
        holder.bind(place)
    }

    override fun getItemCount(): Int = mutableFavorites.size

    fun updateFavorites(newFavorites: List<Favorite>) {
        mutableFavorites.clear()
        mutableFavorites.addAll(newFavorites)
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int) {
        // Remove from list and update RecycleView
        mutableFavorites.removeAt(position)
        notifyDataSetChanged()
    }
}