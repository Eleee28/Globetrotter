package com.example.globetrotter.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.globetrotter.R
import com.example.globetrotter.activities.ItineraryDetailsActivity
import com.example.globetrotter.database.Itinerary
import com.squareup.picasso.Picasso
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ItineraryAdapter(
    private val context: Context,
    private var itineraries: List<Itinerary>,
    private var layout: Int,
    private val onDeleteClicked: ((Itinerary, Int) -> Unit)? = null // Callback for delete action
) : RecyclerView.Adapter<ItineraryAdapter.ItineraryViewHolder>() {

    private val mutableItineraries = itineraries.toMutableList()
    private var currentDate: LocalDate = LocalDate.now()

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

            // Parse startDate
            val formatter = DateTimeFormatter.ofPattern("d/M/yyyy")
            val itineraryEndDate = LocalDate.parse(itinerary.endDate, formatter)

            // Change background if expired itinerary
            if (itineraryEndDate.isBefore(currentDate)) {
                destination.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            }

            deleteButton?.setOnClickListener {
                onDeleteClicked?.invoke(itinerary, adapterPosition) // Trigger callback
            }

            itemView.setOnClickListener {
                val intent = Intent(context, ItineraryDetailsActivity::class.java)
                intent.putExtra("itinerary", itinerary)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItineraryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ItineraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItineraryViewHolder, position: Int) {
        val itinerary = mutableItineraries[position]
        holder.bind(itinerary)
    }

    override fun getItemCount(): Int {
        return mutableItineraries.size
    }

    fun updateItineraries(newItineraries: List<Itinerary>, currentDate: LocalDate) {
        mutableItineraries.clear()
        mutableItineraries.addAll(newItineraries)
        this.currentDate = currentDate
        notifyDataSetChanged()
    }

    fun deleteItem(position: Int) {
            // Remove from list and update RecycleView
            mutableItineraries.removeAt(position)
            notifyDataSetChanged()
    }
}