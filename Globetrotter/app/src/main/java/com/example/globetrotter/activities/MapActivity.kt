package com.example.globetrotter.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.globetrotter.R
import com.example.globetrotter.database.AppDatabase
import com.example.globetrotter.network.RetrofitClient
import com.example.globetrotter.utils.NavigationBar
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var searchBox: AutoCompleteTextView
    private lateinit var itineraryBtn: Button
    private lateinit var favoritesBtn: Button

    private lateinit var appDatabase: AppDatabase

    // For city suggestions and dropdown
    private lateinit var autoCompleteAdapter: ArrayAdapter<String>
    private val citySuggestions = mutableListOf<String>()
    private val cityCoordinates = mutableMapOf<String, Pair<Double,Double>>()

    private lateinit var addItineraryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.mapView)
        searchBox = findViewById(R.id.searchBox)
        itineraryBtn = findViewById(R.id.itineraryButton)
        favoritesBtn = findViewById(R.id.favoriteButton)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        autoCompleteAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, citySuggestions)
        searchBox.setAdapter(autoCompleteAdapter)

        appDatabase = AppDatabase.getDatabase(this)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val name = intent.getStringExtra("name") ?: "Selected Location"

        // Check for Location Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }

        if (latitude != 0.0 && longitude != 0.0) {
            loadMap(latitude, longitude, name)
        } else {
            initializeMap()
        }

        setUpListeners()

        // Register ActivityResultLauncher
        addItineraryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                initializeMap() // Reload itineraries
            }
        }

        NavigationBar.setupNavigationBar(this, addItineraryLauncher)
    }

    private fun initializeMap() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                loadMap(latitude, longitude, "You are here!")
            } else {
                loadMap(0.0, 0.0, "Default Location") // Default
            }
        }
    }

    private fun loadMap(latitude: Double, longitude: Double, name: String) {
        val htmlData = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.3/dist/leaflet.css">
                <script src="https://unpkg.com/leaflet@1.9.3/dist/leaflet.js"></script>
                <style>
                    html, body { height: 100%; margin: 0; padding: 0; }
                    #map { height: 100%; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    // Wait for DOM to fully load
                    document.addEventListener("DOMContentLoaded", function () {
                        try {
                            // Initialize the map at a given latitude and longitude
                            var map = L.map('map', {
                                zoomControl: false // Disable soom control buttons
                            }).setView([$latitude, $longitude], 13); // Default to London
                            
                            // Load and display the tile layer
                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                maxZoom: 19,
                                attribution: '© OpenStreetMap contributors'
                            }).addTo(map);
                    
                            // Add a marker at the same location
                            L.marker([$latitude, $longitude]).addTo(map)
                                .bindPopup("$name")
                                .openPopup();
                        } catch (error) {
                            console.error("Error initializing map:", error);
                        }
                     });
                </script>
            </body>
            </html>
        """

        Log.d("HTMLContent", htmlData)

        mapView.settings.javaScriptEnabled = true
        mapView.settings.domStorageEnabled = true
        mapView.webViewClient = WebViewClient()
        WebView.setWebContentsDebuggingEnabled(true)
        mapView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
    }

    private fun setUpListeners() {

        // Get city suggestions
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isNotEmpty() && query.toString().length >= 3) {
                    fetchSuggestions(query)
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        })

        searchBox.imeOptions = EditorInfo.IME_ACTION_SEARCH

        // Listener for search (enter)
        searchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                val selectedPlace = searchBox.text.toString()
                if (selectedPlace.isNotEmpty())
                    searchPlace(selectedPlace)
                true
            }
            false
        }

        itineraryBtn.setOnClickListener {
            loadItineraryMarkers()
        }

        favoritesBtn.setOnClickListener {
            loadFavoriteMarkers()
        }
    }

    private fun loadItineraryMarkers() {
        lifecycleScope.launch {
            val itineraries = appDatabase.itineraryDao().getAllItineraries()  // Fetch all itineraries from the database
            Log.d("itineraries", itineraries.toString())
            val markers = itineraries.map { itinerary ->
                val description = itinerary.description.replace("\n", "<br>")
                """
                    L.marker([${itinerary.latitude}, ${itinerary.longitude}])
                    .addTo(map)
                    .bindPopup("<b>${itinerary.name}</b><br>${description}");
                """
            }.joinToString("\n")

            loadMapWithMarkers(markers)
        }
    }

    private fun loadFavoriteMarkers() {
        lifecycleScope.launch {
            val favorites = appDatabase.favoriteDao().getAllFavorites()  // Fetch all favorite places from the database
            val markers = favorites.map { favorite ->
                val place = favorite.place
                """
                    L.marker([${place.latitude}, ${place.longitude}])
                    .addTo(map)
                    .bindPopup("<b>${place.name}</b><br>${place.address}");
                """
            }.joinToString("\n")

            loadMapWithMarkers(markers)
        }
    }

    private fun loadMapWithMarkers(markers: String) {
        val htmlData = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.3/dist/leaflet.css">
                <script src="https://unpkg.com/leaflet@1.9.3/dist/leaflet.js"></script>
                <style>
                    html, body { height: 100%; margin: 0; padding: 0; }
                    #map { height: 100%; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    // Initialize the map
                    var map = L.map('map', {
                                zoomControl: false // Disable soom control buttons
                    }).setView([51.505, -0.09], 2);  // Default view (adjust as needed)

                    // Load and display the tile layer
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);

                    // Add the markers dynamically
                    $markers
                </script>
            </body>
            </html>
        """

        Log.d("HTMLContent", htmlData.replace("\$markers", markers))

        // Load the map in WebView
        mapView.settings.javaScriptEnabled = true
        mapView.settings.domStorageEnabled = true
        mapView.webViewClient = WebViewClient()
        WebView.setWebContentsDebuggingEnabled(true)
        mapView.loadDataWithBaseURL(null, htmlData.replace("\$markers", markers), "text/html", "UTF-8", null)
    }

    private fun fetchSuggestions(query: String) {
        lifecycleScope.launch {
            try {
                // delay before making request to avoid making too many
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
                        this@MapActivity,
                        "Error fetching cities: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun searchPlace(place: String) {
        val coordinates = cityCoordinates[place]
        if (coordinates != null) {
            // If we found the coordinates for the place, update the map
            loadMap(coordinates.first, coordinates.second, place)
        } else {
            Toast.makeText(this, "Place not found, please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeMap()
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }
}