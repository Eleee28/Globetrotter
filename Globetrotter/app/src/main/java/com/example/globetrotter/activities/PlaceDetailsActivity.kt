package com.example.globetrotter.activities

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.globetrotter.R
import com.example.globetrotter.adapters.ItineraryPopupAdapter
import com.example.globetrotter.database.AppDatabase
import com.example.globetrotter.database.Favorite
import com.example.globetrotter.database.Itinerary
import com.example.globetrotter.database.Place
import com.example.globetrotter.utils.NavigationBar
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class PlaceDetailsActivity : AppCompatActivity() {

    private lateinit var placeImage: ImageView
    private lateinit var placeName: TextView
    private lateinit var placeCategory: TextView
    private lateinit var placeAddress: TextView
    private lateinit var placePrice: TextView
    private lateinit var placeContact: TextView
    private lateinit var placeStatus: TextView
    private lateinit var placeHours: TextView
    private lateinit var addBtn: Button
    private lateinit var mapBtn: Button

    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_place_details)

        placeImage = findViewById(R.id.placeImage)
        placeName = findViewById(R.id.placeName)
        placeCategory = findViewById(R.id.categoryName)
        placeAddress = findViewById(R.id.address)
        placePrice = findViewById(R.id.price)
        placeContact = findViewById(R.id.contactInfo)
        placeStatus = findViewById(R.id.status)
        placeHours = findViewById(R.id.openingHours)
        addBtn = findViewById(R.id.addButton)
        mapBtn = findViewById(R.id.mapButton)

        appDatabase = AppDatabase.getDatabase(this)

        val place: Place? = intent.getParcelableExtra("place")

        if (place != null) {
            // Place image
            if (!place.imageUrl.isNullOrEmpty())
                Picasso.get().load(place.imageUrl).into(placeImage)
            else
                placeImage.setImageResource(R.drawable.rounded_image_border)

            // Text Views
            placeName.text = place.name
            placeCategory.text = place.category
            placeAddress.text = place.address
            placeContact.text = "Email: ${place.contactInfo}"
            placeStatus.text = place.status
            placeHours.text = "Opening Hours:\n\n${place.openHours}"

            // Price
            if (place.price.toInt() != -1)
                placePrice.text = "Price: ${place.price}"
            else
                placePrice.text = "Price: Unknown"

            // Category Icon
            val categoryIconDrawable = ContextCompat.getDrawable(this, place.categoryIcon) // From: https://stackoverflow.com/questions/11376516/change-drawable-color-programmatically
            placeCategory.setCompoundDrawablesRelativeWithIntrinsicBounds(categoryIconDrawable, null, null, null)

            // Status dot color
            val dot = ContextCompat.getDrawable(this, R.drawable.ic_dot)
            val statusColor: Int
            if (place.status.contains("open", ignoreCase = true))
                statusColor = ContextCompat.getColor(this, R.color.colorOk)
            else
                statusColor = ContextCompat.getColor(this, R.color.colorError)

            // Reference from: https://stackoverflow.com/questions/11376516/change-drawable-color-programmatically
            dot?.setColorFilter(statusColor, PorterDuff.Mode.SRC_IN)
            placeStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
            // End reference

            setButtonActions(place)
        }

        NavigationBar.setupNavigationBar(this)
    }

    private fun setButtonActions(place: Place) {

        addBtn.setOnClickListener {
            showItineraryPopup(place)
        }

        mapBtn.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("latitude", place.latitude)
            intent.putExtra("longitude", place.longitude)
            intent.putExtra("name", place.name)
            startActivity(intent)
        }
    }

    private fun showItineraryPopup(place: Place) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.itinerary_popup, null)

        val dialog = AlertDialog.Builder(this).setView(popupView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // For adding to favorites
        val messageTextView = popupView.findViewById<TextView>(R.id.message)
        val addFavBtn = popupView.findViewById<Button>(R.id.addFavBtn)

        // Recycler View
        val itineraryRecyclerView = popupView.findViewById<RecyclerView>(R.id.itineraryRecyclerView)
        itineraryRecyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            // Fetch itineraries from database
            val itineraries = getItinerariesWithinRange(appDatabase.itineraryDao().getAllItineraries(), place.latitude, place.longitude, 50.0)

            if (itineraries.size > 0) {
                messageTextView.visibility = View.GONE
                addFavBtn.visibility = View.GONE
                itineraryRecyclerView.visibility = View.VISIBLE

                val itineraryAdapter = ItineraryPopupAdapter(itineraries, place) { newPlan ->
                    lifecycleScope.launch {
                        appDatabase.planDao().insertPlan(newPlan)
                        runOnUiThread { // Feedback for the user
                            Toast.makeText(this@PlaceDetailsActivity, "Plan added!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    dialog.hide()
                }
                itineraryRecyclerView.adapter = itineraryAdapter
            } else {
                messageTextView.visibility = View.VISIBLE
                addFavBtn.visibility = View.VISIBLE
                itineraryRecyclerView.visibility = View.GONE

                addFavBtn.setOnClickListener {
                    val favorite = Favorite(place = place)
                    lifecycleScope.launch {
                        appDatabase.favoriteDao().insertFavorite(favorite)
                        runOnUiThread { // Feedback for the user
                            Toast.makeText(this@PlaceDetailsActivity, "Plan added to Favorites!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    dialog.hide()
                }
            }
        }
        dialog.show()
    }

    // Formula to filter from:
    // https://www.sisense.com/blog/latitude-longitude-distance-calculation-explained/
    // https://stackoverflow.com/questions/21084886/how-to-calculate-distance-using-latitude-and-longitude
    private fun getItinerariesWithinRange(itineraries: List<Itinerary>, lat: Double, lng: Double, rangeKm: Double): List<Itinerary> {
        val earthRadius = 6371.0

        return itineraries.filter { itinerary ->
            val dLat = Math.toRadians(itinerary.latitude - lat)
            val dLng = Math.toRadians(itinerary.longitude - lng)

            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(itinerary.latitude)) *
                    Math.sin(dLng / 2) * Math.sin(dLng / 2)

            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

            val distance = earthRadius * c
            distance <= rangeKm
        }
    }
}