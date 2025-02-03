package com.example.globetrotter.database

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

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
    // Reference: From Chat-GPT
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(startDate)
        parcel.writeString(endDate)
        parcel.writeString(description)
        parcel.writeString(imageUrl)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Itinerary> {
        override fun createFromParcel(parcel: Parcel): Itinerary {
            return Itinerary(parcel)
        }

        override fun newArray(size: Int): Array<Itinerary?> {
            return arrayOfNulls(size)
        }
    }
}
// Reference complete