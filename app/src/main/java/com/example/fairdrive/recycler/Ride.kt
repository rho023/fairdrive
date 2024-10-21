package com.example.fairdrive.recycler

import com.google.firebase.Timestamp

data class Ride(
    val pickup: LocationData = LocationData(),
    val dropoff: LocationData = LocationData(),
    val timestamp: Timestamp = Timestamp.now()
)

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
