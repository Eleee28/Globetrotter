# Globetrotter
Mobile Software Development Final Project

## Overview
Globetrotter is an Android application designed to help travelers plan, organize, and manage their itineraries. The app allows users to create itineraries, add places to plans, and view weather forecasts for specific destinations. It uses Room Database for data storage and integrates with external services like OpenWeatherMap for weather updates.

## Features
- **Itinerary Management:** Create, modify, and view itineraries with detailed information.

- **Plans Integration:** Associate places with itineraries and manage notes for each plan.

- **Weather Forecasts:** Fetch and display weather forecasts for itinerary destinations.

- **Map Integration:** View your current location or favorite saved places on an interactive map

- **Data Persistence:** Local storage using Room Database ensures itinerary and plan information is saved and accessible offline.

- **Image Handling:** Images are dynamically loaded using the Picasso library.

- **Seamless UI:** Clean, user-friendly interface with support for viewing itineraries and plans.

## Video Demo
![Demo](globetrotter_demo.mp4)

## Installation
1. Clone this repository:
   ```bash
   git clone git@github.com:Eleee28/Globetrotter.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle.
4. Obtain required API keys. E.g.: from OpenWeatherMap and add it to `strings.xml`:
   ```xml
   <string name="OPENWEATHERMAP_API_KEY">your_api_key_here</string>
   ```
5. Run the app on an emulator or a physical device.

## Technologies Used
- **Programming Language:** Kotlin

- **Database:** Room Database

- **Networking:** Retrofit for RESTful API calls

- **Image Loading:** Picasso for handling image requests

- **Asynchronous Tasks:** Coroutines for non-blocking operations

## Architecture
The project follows an MVVM (Model-View-ViewModel) architecture:

- **Model:** Room Entities (Itinerary, Plan)

- **View:** Activities and Adapters for displaying data

- **ViewModel:** Manages UI-related data and communicates with the database

## Database Schema
### Tables:
- **Itinerary Table:** Stores details about travel itineraries.
- **Plan Table:** Stores associated plans for specific itineraries.
- **Favorite Table:** Stores favorite plans details.

### Data Models:
- **Itinerary:**
```kotlin
  @Entity(tableName = "itinerary")
    data class Itinerary(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val name: String,
        val startDate: String,
        val endDate: String,
        val description: String,
        val imageUrl: String? = null,
        val latitude: Double,
        val longitude: Double
    )
```

- **Plan:**
```kotlin
    @Entity(
        tableName = "plans",
        foreignKeys = [
            ForeignKey(
                entity = Itinerary::class,
                parentColumns = ["id"],
                childColumns = ["itineraryId"],
                onDelete = ForeignKey.CASCADE
            )
        ]
    )
    data class Plan (
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val itineraryId: Int,
        val place: Place
    )
```

- **Favorite:**
```kotlin
    @Entity(tableName = "favorite")
    data class Favorite (
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val place: Place
    )
```

## Usage Instructions
1. **Create Itinerary:** Tap on the "Add Itinerary" button, fill in details, and save.
2. **View Itinerary Details:** Select an itinerary to view associated plans and weather forecasts.
3. **Add Plans:** Tap on a place to associate it with an itinerary.
4. **Weather Forecast:** Click the weather icon in the itinerary details to see weather forecasts for your trip.

## Notice
For most of the apis used in this project an api key is required to make the calls. I stored my keys on the file res/values/string.xml, but for the upload I deleted them as these are private keys.