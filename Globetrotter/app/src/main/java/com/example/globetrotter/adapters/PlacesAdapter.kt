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
import com.example.globetrotter.database.Place
import com.squareup.picasso.Picasso

class PlacesAdapter(
    private val context: Context,
    private val places: List<Place>,
) : RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

    private val mutablePlaces = places.toMutableList()

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.placeIcon)
        val name: TextView = itemView.findViewById(R.id.placeName)
        val address: TextView = itemView.findViewById(R.id.address)

        fun bind(place: Place) {
            name.text = place.name
            address.text = place.address
            place.imageUrl?.let {
                if (place.imageUrl.isNotEmpty())
                    Picasso.get().load(it).into(icon)
                else
                    icon.setImageResource(place.categoryIcon)
            }

            itemView.setOnClickListener {
                 val intent = Intent(context, PlaceDetailsActivity::class.java)
                intent.putExtra("place", place)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = mutablePlaces[position]
        holder.bind(place)
    }

    override fun getItemCount(): Int = mutablePlaces.size

    fun updatePlaces(newPlaces: List<Place>) {
        mutablePlaces.clear()
        mutablePlaces.addAll(newPlaces)
        notifyDataSetChanged()
    }

}