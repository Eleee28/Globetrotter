package com.example.globetrotter.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.globetrotter.adapters.ItineraryAdapter
import com.example.globetrotter.utils.NavigationBar
import com.example.globetrotter.R
import com.example.globetrotter.adapters.CategoryAdapter
import com.example.globetrotter.database.AppDatabase
import com.example.globetrotter.database.Category
import com.example.globetrotter.database.Itinerary
import com.example.globetrotter.utils.HorizontalSpacingItemDecoration
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter

    private lateinit var itineraryRecyclerView: RecyclerView
    private lateinit var itineraryAdapter: ItineraryAdapter

    private lateinit var appDatabase: AppDatabase

    private lateinit var addItineraryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
        categoryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // category codes from: https://docs.foursquare.com/data-products/docs/categories
        val categories = listOf(
            Category("Sightseeing", R.drawable.ic_attraction, "16000"),
            Category("Accommodation", R.drawable.ic_hotel, "19009"),
            Category("Food", R.drawable.ic_food, "13065"),
            Category("Shopping", R.drawable.ic_shopping, "17000"),
            Category("Entertainment", R.drawable.ic_museum, "10000")
        )

        categoryAdapter = CategoryAdapter(this, categories)
        categoryRecyclerView.adapter = categoryAdapter

        // Set up RecyclerView
        itineraryRecyclerView = findViewById(R.id.itineraryRecyclerView)

        itineraryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // Add spacing between items
        itineraryRecyclerView.addItemDecoration(HorizontalSpacingItemDecoration(16))

        // Initialize database
        appDatabase = AppDatabase.getDatabase(this)

        // Set up itineraryAdapter for RecyclerView
        itineraryAdapter = ItineraryAdapter(this, emptyList(), R.layout.item_itinerary_h)
        itineraryRecyclerView.adapter = itineraryAdapter

        // free database
        //clearItineraries()
        //clearPlans()

        // Fetch itineraries and add data to RecyclerView
        loadItineraries()

        searchBarFunctionality()

        // Register ActivityResultLauncher
        addItineraryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                loadItineraries() // Reload itineraries
            }
        }

        NavigationBar.setupNavigationBar(this, addItineraryLauncher)
    }

    private fun loadItineraries() {
        lifecycleScope.launch {
            val itineraries = appDatabase.itineraryDao().getAllItineraries()

            // Current date
            val currDate = LocalDate.now()

            // Sort itineraries
            val sortedItineraries = sortItineraries(itineraries)

            itineraryAdapter.updateItineraries(sortedItineraries, currDate)
        }
    }

    private fun searchBarFunctionality() {
        val searchBar: EditText = findViewById(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                filterItineraries(query)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        })
    }

    private fun filterItineraries(query: String) {
        lifecycleScope.launch {
            val itineraries = appDatabase.itineraryDao().getAllItineraries()
            val filteredItineraries = itineraries.filter {
                it.name.contains(query, ignoreCase = true)
            }

            // Current date
            val currDate = LocalDate.now()

            // Sort itineraries
            val sortedItineraries = sortItineraries(filteredItineraries)

            itineraryAdapter.updateItineraries(sortedItineraries, currDate)
        }
    }

    private fun sortItineraries(itineraries: List<Itinerary>) : List<Itinerary> {
        // Format for parsing date
        val formatter = DateTimeFormatter.ofPattern("d/M/yyyy")

        // Current date
        val currDate = LocalDate.now()

        // Sort itineraries by expiration and by startDate
        val sortedItineraries = itineraries.sortedWith(compareBy({
            LocalDate.parse(it.startDate, formatter).isBefore(currDate)
        }, {
            LocalDate.parse(it.startDate, formatter)
        }))

        return sortedItineraries
    }

    private fun clearItineraries() {
        lifecycleScope.launch {
            appDatabase.itineraryDao().deleteAll()
        }
    }

    private fun clearPlans() {
        lifecycleScope.launch {
            appDatabase.planDao().deleteAll()
        }
    }
}