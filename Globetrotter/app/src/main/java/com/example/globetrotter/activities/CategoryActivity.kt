package com.example.globetrotter.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.globetrotter.R
import com.example.globetrotter.adapters.PlacesAdapter
import com.example.globetrotter.database.Category
import com.example.globetrotter.database.Place
import com.example.globetrotter.network.RetrofitClient
import com.example.globetrotter.utils.NavigationBar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class CategoryActivity : AppCompatActivity() {

    private lateinit var categoryTitle: TextView
    private lateinit var citySearchBox: AutoCompleteTextView
    private lateinit var filterSearchBox: EditText
    private lateinit var searchBtn: Button

    private lateinit var placeRecyclerView: RecyclerView
    private lateinit var placesAdapter: PlacesAdapter

    // For city suggestions
    private lateinit var autoCompleteAdapter: ArrayAdapter<String>
    private val citySuggestions = mutableListOf<String>()
    private val cityCoordinates = mutableMapOf<String, Pair<Double,Double>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category)

        categoryTitle = findViewById(R.id.categoryTitle)
        citySearchBox = findViewById(R.id.citySearchBox)
        filterSearchBox = findViewById(R.id.filterSearchBox)
        searchBtn = findViewById(R.id.searchBtn)

        autoCompleteAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, citySuggestions)
        citySearchBox.setAdapter(autoCompleteAdapter)

        placeRecyclerView = findViewById(R.id.placesRecyclerView)
        placeRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val category: Category? = intent.getParcelableExtra("category")
        categoryTitle.text = category?.title ?: "Category"

        placesAdapter = PlacesAdapter(this, emptyList())
        placeRecyclerView.adapter = placesAdapter

        citySearchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isNotEmpty() && query.toString().length >= 3) {
                    fetchSuggestions(query)
                }
            }
        })

        searchBtn.setOnClickListener {
            fetchPlaces(citySearchBox.text.toString(), filterSearchBox.text.toString(), category!!)
        }

        NavigationBar.setupNavigationBar(this)
    }

    private fun fetchSuggestions(query: String) {
        lifecycleScope.launch {
            try {
                delay(500)
                val response = RetrofitClient.geoService.getCities(
                    namePrefix = query,
                    apiKey = getString(R.string.GEODB_API_KEY)
                )
                if (response.data != null && response.data.isNotEmpty()) {
                    citySuggestions.clear()
                    cityCoordinates.clear()

                    response.data.forEach { city ->
                        val cityName = "${city.name} (${city.country})"
                        citySuggestions.add(cityName)
                        cityCoordinates[cityName] = Pair(city.latitude, city.longitude)
                    }
                }
                autoCompleteAdapter.addAll(citySuggestions)
                autoCompleteAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                if (e.message?.contains("HTTP 429") == false) { // Avoid api too many requests exception
                    Log.e("Error", "Exception occurred: ${e.message}")
                    Toast.makeText(
                        this@CategoryActivity,
                        "Error fetching cities: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun fetchPlaces(city: String, query: String, category: Category) {
        lifecycleScope.launch {
            try {

                withContext(Dispatchers.IO) {
                    // Reference from: https://docs.foursquare.com/developer/reference/place-search?example=kotlin
                    val client = OkHttpClient()

                    val request = Request.Builder()
                        .url("https://api.foursquare.com/v3/places/search?query=${query}&categories=${category.categoryId}&near=${city}&sort=RELEVANCE&limit=10")
                        .get()
                        .addHeader("accept", "application/json")
                        .addHeader("Authorization", getString(R.string.FOURSQUARE_API_KEY))
                        .build()

                    val response = client.newCall(request).execute()
                    // End reference

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        // Parse JSON - structure to see how to parse from: https://docs.foursquare.com/developer/reference/place-search?example=kotlin
                        val jsonResponse = JSONObject(responseBody)
                        val results = jsonResponse.getJSONArray("results")

                        val places = mutableListOf<Place>()
                        for (i in 0 until results.length()) {
                            val jsonPlace = results.getJSONObject(i)

                            // Basic information
                            val name = jsonPlace.getString("name")
                            val address = jsonPlace.getJSONObject("location").getString("formatted_address")
                            val latitude = jsonPlace.getJSONObject("geocodes").getJSONObject("main").getDouble("latitude")
                            val longitude = jsonPlace.getJSONObject("geocodes").getJSONObject("main").getDouble("longitude")

                            // Status and Open hours
                            val status = jsonPlace.optString("closed_bucket", "Unknown") // maybe inside of hours: open_now
                            val openHours = jsonPlace.optJSONObject("hours")?.optString("display") ?: "Not Available"

                            // Category and Icon
                            val cat = category.title
                            val catIcon = category.iconResId

                            // Price
                            val price = jsonPlace.optInt("price", -1).toDouble()

                            // Contact Information
                            val email = jsonPlace.optString("email", "Unknown")

                            // Image
                            val imageUrl = getImageUrl(name).await()

                            val place = Place(
                                name,
                                address,
                                latitude,
                                longitude,
                                imageUrl,
                                status,
                                openHours,
                                cat,
                                catIcon,
                                price,
                                email)

                            places.add(place)
                        }
                        withContext(Dispatchers.Main) {
                            placesAdapter.updatePlaces(places)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("API_EXCEPTION", "Exception occurred: ${e.message}")
                e.printStackTrace()  // Log the full stack trace
                Toast.makeText(this@CategoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getImageUrl(name: String) : Deferred<String> {
        val deferred = CompletableDeferred<String>()
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.unsplashService.searchImages(name, getString(R.string.UNSPLASH_API_KEY))
                val imageUrl = response.results.firstOrNull()?.urls?.regular ?: ""
                deferred.complete(imageUrl)

            } catch (e: Exception) {
                Log.e("API_EXCEPTION", "Exception occurred: ${e.message}")
                e.printStackTrace()  // Log the full stack trace
                Toast.makeText(this@CategoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        return deferred
    }
}