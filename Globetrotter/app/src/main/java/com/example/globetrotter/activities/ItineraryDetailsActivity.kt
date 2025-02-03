package com.example.globetrotter.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.globetrotter.R
import com.example.globetrotter.adapters.PlansAdapter
import com.example.globetrotter.database.AppDatabase
import com.example.globetrotter.database.Itinerary
import com.example.globetrotter.network.RetrofitClient
import com.example.globetrotter.network.WeatherItem
import com.example.globetrotter.network.WeatherResponse
import com.example.globetrotter.utils.NavigationBar
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ItineraryDetailsActivity : AppCompatActivity() {

    private lateinit var itineraryImage: ImageView
    private lateinit var itineraryName: TextView
    private lateinit var itineraryDates: TextView
    private lateinit var itineraryDescription: TextView
    private lateinit var modifyButton: ImageButton
    private lateinit var weatherButton: ImageButton

    private lateinit var plansRecyclerView: RecyclerView

    private lateinit var appDatabase: AppDatabase

    private lateinit var modifyItineraryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_itinerary_details)

        itineraryImage = findViewById(R.id.placeImageView)
        itineraryName = findViewById(R.id.itineraryNameTextView)
        itineraryDates = findViewById(R.id.datesTextView)
        itineraryDescription = findViewById(R.id.descriptionTextView)
        modifyButton = findViewById(R.id.modifyButton)
        weatherButton = findViewById(R.id.weatherButton)

        appDatabase = AppDatabase.getDatabase(this)

        // Reference: From Chat-GPT
        val itinerary: Itinerary? = intent.getParcelableExtra("itinerary")
        // Reference complete

        if (itinerary != null) {
            // Populate data
            itineraryName.text = itinerary.name
            itineraryDates.text = "${itinerary.startDate}  -  ${itinerary.endDate}"
            itineraryDescription.text = itinerary.description

            // Load image
            if (!itinerary.imageUrl.isNullOrEmpty())
                Picasso.get().load(itinerary.imageUrl).into(itineraryImage)
            else
                itineraryImage.setImageResource(R.drawable.rounded_image_border)

            plansRecyclerView = findViewById(R.id.plansRecyclerView)
            plansRecyclerView.layoutManager = LinearLayoutManager(this)


            lifecycleScope.launch {
                val plans = appDatabase.planDao().getPlansByItinerary(itinerary.id)

                val plansAdapter = PlansAdapter(this@ItineraryDetailsActivity, plans)
                plansRecyclerView.adapter = plansAdapter
            }
        }

        modifyItineraryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val updatedItinerary: Itinerary? = result.data?.getParcelableExtra("updatedItinerary")
                if (updatedItinerary != null) {
                    itineraryName.text = updatedItinerary.name
                    itineraryDates.text = "${updatedItinerary.startDate} - ${updatedItinerary.endDate}"
                    itineraryDescription.text = updatedItinerary.description
                    if (!updatedItinerary.imageUrl.isNullOrEmpty())
                        Picasso.get().load(updatedItinerary.imageUrl).into(itineraryImage)
                    else
                        itineraryImage.setImageResource(R.drawable.rounded_image_border)
                }
            }
        }

        modifyButton.setOnClickListener {
            val intent = Intent(this, AddItineraryActivity::class.java)
            intent.putExtra("itinerary", itinerary)
            modifyItineraryLauncher.launch(intent)
        }

        weatherButton.setOnClickListener {
            showWeatherPopup(itinerary!!)
        }

        NavigationBar.setupNavigationBar(this)
    }

    private fun showWeatherPopup(itinerary: Itinerary) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.weather_popup, null)

        // Alert dialog
        val dialog = AlertDialog.Builder(this).setView(popupView).create()

        // Set transparent background for dialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        // Initialize views
        val weatherDate = popupView.findViewById<TextView>(R.id.weatherDate)
        val weatherIcon = popupView.findViewById<ImageView>(R.id.weatherIcon)
        val weatherDescription = popupView.findViewById<TextView>(R.id.weatherDescription)
        val temperature = popupView.findViewById<TextView>(R.id.weatherTemperature)
        val prevBtn = popupView.findViewById<ImageButton>(R.id.arrowLeft)
        val nextBtn = popupView.findViewById<ImageView>(R.id.arrowRight)
        val closeBtn = popupView.findViewById<ImageView>(R.id.closeButton)

        // Parse dates
        val formatter = DateTimeFormatter.ofPattern("d/M/yyyy")
        val startDate = LocalDate.parse(itinerary.startDate, formatter)
        val endDate = LocalDate.parse(itinerary.endDate, formatter)
        val totalDays = (endDate.toEpochDay() - startDate.toEpochDay()).toInt() + 1

        var currDayIndex = 0
        var weatherList: List<WeatherItem> = emptyList()

        // OpenWeatherMap iconCodes from: https://openweathermap.org/weather-conditions
        fun getCustomeIconResource(iconCode: String): Int {
            return when (iconCode) {
                "01d" -> R.drawable.clear_day
                "01n" -> R.drawable.clear_night
                "02d" -> R.drawable.few_clouds_day
                "02n" -> R.drawable.few_clouds_night
                "03d", "03n" -> R.drawable.scattered_clouds
                "04d", "04n" -> R.drawable.broken_clouds
                "09d", "09n" -> R.drawable.shower_rain
                "10d" -> R.drawable.rain_day
                "10n" -> R.drawable.rain_night
                "11d" -> R.drawable.thunder_day
                "11n" -> R.drawable.thunder_night
                "13d", "13n" -> R.drawable.snow
                "50d", "50n" -> R.drawable.mist
                else -> R.drawable.rounded_image_border
            }
        }

        fun updateWeatherDisplay() {
            val currWeather = weatherList[currDayIndex]

            val currDate = startDate.plusDays(currDayIndex.toLong())
            weatherDate.text = currDate.format(formatter)

            temperature.text = "${currWeather.main.temp.toInt()}ºC"
            weatherDescription.text = currWeather.weather[0].description
            weatherIcon.setImageResource(getCustomeIconResource(currWeather.weather[0].icon))
        }

        lifecycleScope.launch {
            try {
                val weatherResponse = fetchWeather(itinerary.latitude, itinerary.longitude)
                if (weatherResponse != null) {
                    weatherList = weatherResponse.list
                    updateWeatherDisplay()
                } else {
                    Toast.makeText(this@ItineraryDetailsActivity, "Weather data unavailable", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                Log.e("WeatherPopup", "Error fetching weather: ${e.message}")
                Toast.makeText(this@ItineraryDetailsActivity, "Error fetching weather", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        prevBtn.setOnClickListener {
            if (currDayIndex > 0) {
                currDayIndex--
                updateWeatherDisplay()
            }
        }

        nextBtn.setOnClickListener {
            if (currDayIndex < totalDays - 1) {
                currDayIndex++
                updateWeatherDisplay()
            }
        }

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherResponse? {
        return try {
            RetrofitClient.weatherService.getWeatherForecast(latitude, longitude, getString(R.string.OPENWEATHERMAP_API_KEY))
        } catch (e: Exception) {
            Log.e("WeatherFetch", "Error fetching weather data: ${e.message}")
            null
        }
    }
}