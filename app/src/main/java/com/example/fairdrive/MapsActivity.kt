package com.example.fairdrive

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import androidx.fragment.app.Fragment

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.fairdrive.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 101
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var geocoder: Geocoder
    private lateinit var dropoffLocation: LatLng
    private lateinit var pickupLocation: LatLng

    private var locationCircle: Circle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        geocoder = Geocoder(this)

        binding.getCurrentLoc.setOnClickListener {
            requestLocationPermission()
            binding.fromWhereTextview.setText("Current location")
        }

        binding.findDriveButton.setOnClickListener {
            saveRideToFirestore()
        }

        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, RideHistoryActivity::class.java))
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        loadUserData()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnMapClickListener { latLng ->
            getAddressFromLocation(latLng)

            dropoffLocation = latLng
            val selectedLocation = LatLng(latLng.latitude, latLng.longitude)

            mMap.addMarker(MarkerOptions().position(selectedLocation).title("Drop off location"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15f))
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    // Set the username in your TextView
                    binding.userTextview.text = username ?: "User"
                } else {
                    Log.e("TAG", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("TAG", "Failed to retrieve user data: ", exception)
            }
    }

    private fun saveRideToFirestore() {
        if (pickupLocation == null || dropoffLocation == null) {
            Toast.makeText(this, "Please set both pickup and dropoff locations", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: "unknown"

        val ride = hashMapOf(
            "userid" to userId,
            "pickup" to mapOf("latitude" to pickupLocation.latitude, "longitude" to pickupLocation.longitude),
            "dropoff" to mapOf("latitude" to dropoffLocation.latitude, "longitude" to dropoffLocation.longitude),
            "timestamp" to Calendar.getInstance().time
        )

        firestore.collection("rides")
            .add(ride)
            .addOnSuccessListener {
                Toast.makeText(this, "Ride booked", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save ride!", Toast.LENGTH_SHORT).show()
                Log.e("TAG", "saveRideToFirestore: ${e.message}")
            }
    }

    private fun getAddressFromLocation(latLng: LatLng) {
        try {
            // Reverse geocoding to get address from LatLng
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0].getAddressLine(0)

                // Set the address in the TextView
                binding.toWhereTextview.setText(address)
            } else {
                binding.toWhereTextview.setText("No address found")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.toWhereTextview.setText("Unable to get the address")
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted
            getCurrentLocation()
        } else {
            // Request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Define location request
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000 // 5 seconds
            fastestInterval = 2000 // 2 seconds
        }

        // Location callback to handle location updates
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    pickupLocation = currentLatLng
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    addLocationCircle(currentLatLng)
                }
            }
        }

        // Start location updates
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun addLocationCircle(latLng: LatLng) {
        if (locationCircle == null) {
            // Create the circle for the first time
            locationCircle = mMap.addCircle(
                CircleOptions()
                    .center(latLng)
                    .radius(50.0) // 50 meters radius
                    .strokeColor(ContextCompat.getColor(this, R.color.blue)) // Circle outline color
                    .fillColor(ContextCompat.getColor(this, R.color.light_blue)) // Circle fill color
                    .strokeWidth(2f)
            )
        } else {
            // Move the circle to the new location
            locationCircle?.center = latLng
        }
    }
}