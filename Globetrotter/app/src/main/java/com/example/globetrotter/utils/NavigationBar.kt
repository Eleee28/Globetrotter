package com.example.globetrotter.utils

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import com.example.globetrotter.R
import com.example.globetrotter.activities.AddItineraryActivity
import com.example.globetrotter.activities.FavoriteActivity
import com.example.globetrotter.activities.ItineraryActivity
import com.example.globetrotter.activities.MainActivity
import com.example.globetrotter.activities.MapActivity

object NavigationBar {
    fun setupNavigationBar(
        activity: Activity,
        addItineraryLauncher: ActivityResultLauncher<Intent> ?= null
    ) {

        // Navigate to Main Page
        activity.findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
            if (activity !is MainActivity) {
                val intent = Intent(activity, MainActivity::class.java)
                activity.startActivity(intent)
            }
        }

        // Navigate to Itineraries Page
        activity.findViewById<ImageButton>(R.id.nav_itinerary).setOnClickListener {
            if (activity !is ItineraryActivity) {
                val intent = Intent(activity, ItineraryActivity::class.java)
                activity.startActivity(intent)
            }
        }

        // Add Itinerary Page
        activity.findViewById<ImageButton>(R.id.add_itinerary).setOnClickListener {
            if (activity !is AddItineraryActivity) {
                val intent = Intent(activity, AddItineraryActivity::class.java)
                if (addItineraryLauncher == null)
                    activity.startActivity(intent)
                else
                    addItineraryLauncher.launch(intent)
            }
        }

        // Favorites Page
        activity.findViewById<ImageButton>(R.id.nav_favorite).setOnClickListener {
            if (activity !is FavoriteActivity)
                activity.startActivity(Intent(activity, FavoriteActivity::class.java))
        }

        // Map Page
        activity.findViewById<ImageButton>(R.id.nav_map).setOnClickListener {
            if (activity !is MapActivity)
                activity.startActivity(Intent(activity, MapActivity::class.java))
        }

    }
}