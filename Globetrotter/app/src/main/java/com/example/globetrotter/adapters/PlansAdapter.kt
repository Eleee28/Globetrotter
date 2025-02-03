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
import com.example.globetrotter.database.Plan
import com.squareup.picasso.Picasso

class PlansAdapter(
    private val context: Context,
    private val plans: List<Plan>,
) : RecyclerView.Adapter<PlansAdapter.PlanViewHolder>() {

    private val mutablePlans = plans.toMutableList()

    inner class PlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.placeIcon)
        val name: TextView = itemView.findViewById(R.id.placeName)
        val address: TextView = itemView.findViewById(R.id.address)

        fun bind(plan: Plan) {
            name.text = plan.place.name
            address.text = plan.place.address
            plan.place.imageUrl?.let {
                if (plan.place.imageUrl.isNotEmpty())
                    Picasso.get().load(it).into(icon)
                else
                    icon.setImageResource(plan.place.categoryIcon)
            }

            itemView.setOnClickListener {
                 val intent = Intent(context, PlaceDetailsActivity::class.java)
                intent.putExtra("place", plan.place)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = mutablePlans[position]
        holder.bind(plan)
    }

    override fun getItemCount(): Int = mutablePlans.size

}