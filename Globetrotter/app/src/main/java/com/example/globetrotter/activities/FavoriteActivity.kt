package com.example.globetrotter.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.globetrotter.R
import com.example.globetrotter.adapters.FavoriteAdapter
import com.example.globetrotter.database.AppDatabase
import com.example.globetrotter.database.Favorite
import com.example.globetrotter.utils.NavigationBar
import kotlinx.coroutines.launch

class FavoriteActivity : AppCompatActivity() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var favoritesAdapter: FavoriteAdapter

    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_itineraries)

        // Change page title
        val pageTitle = findViewById<TextView>(R.id.itineraryTitleTextView)
        pageTitle.text = "My Favorites"

        // Set up RecyclerView
        favoritesRecyclerView = findViewById(R.id.itineraryRecyclerView)
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        // Initialize database
        appDatabase = AppDatabase.getDatabase(this)

        // Set up itineraryAdapter for RecyclerView
        favoritesAdapter = FavoriteAdapter(
            this, emptyList(),
            onDeleteClicked = ::onFavoriteDeleteClicked // Named function
        )

        favoritesRecyclerView.adapter = favoritesAdapter

        // Fetch favorites and add data to RecyclerView
        loadFavorites()

        searchBarFunctionality()

        NavigationBar.setupNavigationBar(this)

    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            val favorites = appDatabase.favoriteDao().getAllFavorites()

            favoritesAdapter.updateFavorites(favorites)
        }
    }

    private fun searchBarFunctionality() {
        val searchBar: EditText = findViewById(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                filterFavorites(query)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        })
    }

    private fun filterFavorites(query: String) {
        lifecycleScope.launch {
            val favorites = appDatabase.favoriteDao().getAllFavorites()
            val filteredFavorites = favorites.filter {
                it.place.name.contains(query, ignoreCase = true)
            }
            favoritesAdapter.updateFavorites(filteredFavorites)
        }
    }

    private fun onFavoriteDeleteClicked(favorite: Favorite, position: Int) {
        lifecycleScope.launch {
            appDatabase.favoriteDao().deleteFavorite(favorite) // Remove from database
            favoritesAdapter.deleteItem(position) // Remove from adapter
        }
    }
}