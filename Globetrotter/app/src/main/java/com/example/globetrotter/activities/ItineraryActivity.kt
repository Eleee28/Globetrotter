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
import com.example.globetrotter.R
import com.example.globetrotter.adapters.ItineraryAdapter
import com.example.globetrotter.database.AppDatabase
import com.example.globetrotter.database.Itinerary
import com.example.globetrotter.utils.NavigationBar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ItineraryActivity : AppCompatActivity() {

    private lateinit var itineraryRecyclerView: RecyclerView
    private lateinit var itineraryAdapter: ItineraryAdapter

    private lateinit var appDatabase: AppDatabase

    private lateinit var addItineraryLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_itineraries)

        // Set up RecyclerView
        itineraryRecyclerView = findViewById(R.id.itineraryRecyclerView)
        itineraryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        // Initialize database
        appDatabase = AppDatabase.getDatabase(this)

        // Set up itineraryAdapter for RecyclerView
        itineraryAdapter = ItineraryAdapter(
                this,
                emptyList(),
                R.layout.item_itinerary_v,
                onDeleteClicked = ::onItineraryDeleteClicked // Named function
        )

        itineraryRecyclerView.adapter = itineraryAdapter

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

            // Sort itineraries by startDate
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

    private fun onItineraryDeleteClicked(itinerary: Itinerary, position: Int) {
        lifecycleScope.launch {
            appDatabase.itineraryDao().delete(itinerary) // Remove from database
            itineraryAdapter.deleteItem(position) // Remove from adapter
        }
    }
}