package com.example.globetrotter.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.globetrotter.R
import com.example.globetrotter.database.Itinerary
import com.example.globetrotter.database.Place
import com.example.globetrotter.database.Plan
import com.squareup.picasso.Picasso

class ItineraryPopupAdapter(
    private val itineraries: List<Itinerary>,
    private val place: Place,
    private val addPlanCallback: (Plan) -> Unit
) : RecyclerView.Adapter<ItineraryPopupAdapter.ItineraryViewHolder>() {

    inner class ItineraryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.itineraryImage)
        val destination: TextView = itemView.findViewById(R.id.itineraryName)
        val dates: TextView = itemView.findViewById(R.id.dates)
        val deleteButton : ImageView? = itemView.findViewById(R.id.deleteButton)

        fun bind(itinerary: Itinerary) {
            destination.text = itinerary.name
            dates.text = "${itinerary.startDate} - ${itinerary.endDate}"
            itinerary.imageUrl?.let {
                Picasso.get().load(it).into(imageView)
            }
            deleteButton?.visibility = View.GONE

            itemView.setOnClickListener {
                val newPlan = Plan(itineraryId = itinerary.id, place = place)
                addPlanCallback(newPlan)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItineraryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_itinerary_v, parent, false)
        return ItineraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItineraryViewHolder, position: Int) {
        val itinerary = itineraries[position]
        holder.bind(itinerary)
    }

    override fun getItemCount(): Int {
        return itineraries.size
    }

}