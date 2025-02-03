package com.example.globetrotter.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.icu.util.Calendar

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.globetrotter.R
import com.example.globetrotter.database.AppDatabase
import com.example.globetrotter.database.Itinerary
import com.example.globetrotter.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddItineraryActivity : AppCompatActivity() {

    private lateinit var nameEditText: AutoCompleteTextView
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var descriptionEditText: EditText

    private lateinit var errorMessageTextView: TextView

    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button

    private lateinit var appDatabase: AppDatabase

    private var existingItinerary: Itinerary? = null

    // For city suggestions and dropdown
    private lateinit var autoCompleteAdapter: ArrayAdapter<String>
    private val citySuggestions = mutableListOf<String>()
    private val cityCoordinates = mutableMapOf<String, Pair<Double,Double>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_itinerary)

        // Init views
        nameEditText = findViewById(R.id.itineraryNameEditText)
        startDateEditText = findViewById(R.id.startDateEditText)
        endDateEditText = findViewById(R.id.endDateEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        errorMessageTextView = findViewById(R.id.errorMessageText)
        cancelButton = findViewById(R.id.cancelButton)
        saveButton = findViewById(R.id.saveButton)

        errorMessageTextView.visibility = View.GONE

        // Init database
        appDatabase = AppDatabase.getDatabase(this)

        autoCompleteAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, citySuggestions)
        nameEditText.setAdapter(autoCompleteAdapter)

        existingItinerary = intent.getParcelableExtra("itinerary")

        if (existingItinerary != null) {
            nameEditText.setText(existingItinerary?.name)
            startDateEditText.setText(existingItinerary?.startDate)
            endDateEditText.setText(existingItinerary?.endDate)
            descriptionEditText.setText(existingItinerary?.description)
        }

        // Get city suggestions
        nameEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isNotEmpty() && query.toString().length >= 3) {
                    fetchCitySuggestions(query)
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        })

        initButtonActions()
        setDatePicker(startDateEditText)
        setDatePicker(endDateEditText)
    }

    private fun fetchCitySuggestions(query: String) {
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
                        this@AddItineraryActivity,
                        "Error fetching cities: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun initButtonActions() {

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val startDate = startDateEditText.text.toString()
            val endDate = endDateEditText.text.toString()
            val description = descriptionEditText.text.toString()

            if (name.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank() && description.isNotBlank()) {
                if (existingItinerary != null)
                    updateItinerary(existingItinerary!!, name, startDate, endDate, description)
                else
                    saveItinerary(name, startDate, endDate, description)
            }
            else {
                errorMessageTextView.text = "All fields must be filled in before saving"
                errorMessageTextView.visibility = View.VISIBLE
            }
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun saveItinerary(name: String, startDate: String, endDate: String, description: String) {
        lifecycleScope.launch {
            try {
                val coordinates = cityCoordinates[name] ?: throw IllegalArgumentException("Coordinates not found for $name")
                val (latitude, longitude) = coordinates

                // From Chat-GPT
                    val response = RetrofitClient.unsplashService.searchImages(name, getString(R.string.UNSPLASH_API_KEY))
                    val imageUrl = response.results.firstOrNull()?.urls?.regular
                // End of Chat-GPT code

                if (imageUrl != null) {
                    val newItinerary = Itinerary(
                        name = name,
                        startDate = startDate,
                        endDate = endDate,
                        description = description,
                        imageUrl = imageUrl,
                        latitude = latitude,
                        longitude = longitude
                    )
                    appDatabase.itineraryDao().insert(newItinerary)

                    // Notify MainActivity to reload data
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddItineraryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateItinerary(existingItinerary: Itinerary, name:String, startDate: String, endDate: String, description: String) {
        lifecycleScope.launch {
            try {
                // Fetch image
                val response = RetrofitClient.unsplashService.searchImages(name, getString(R.string.UNSPLASH_API_KEY))//BuildConfig.UNSPLASH_API_KEY)
                val imageUrl = response.results.firstOrNull()?.urls?.regular

                val updatedItinerary = existingItinerary.copy(
                    name = name,
                    startDate = startDate,
                    endDate = endDate,
                    description = description,
                    imageUrl = imageUrl
                )

                appDatabase.itineraryDao().update(updatedItinerary)

                // Notify previous activity
                val resultIntent = Intent()
                resultIntent.putExtra("updatedItinerary", updatedItinerary)
                setResult(RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddItineraryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setDatePicker(editText: EditText) {
        editText.isFocusable = false // disable keyboard input
        editText.setOnClickListener {
            showDatePicker(editText)
        }
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Show date picker
        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                editText.setText(date)
            },
            year,
            month,
            day
        )
        datePicker.show()
    }
}