package com.example.fairdrive.recycler

import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fairdrive.R
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.Locale

class RideAdapter (private val rides: List<Ride>) : RecyclerView.Adapter<RideAdapter.ViewHolder>() {
    private lateinit var geocoder: Geocoder

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pickupLocation: TextView = itemView.findViewById(R.id.pickupLocation)
        val dropoffLocation: TextView = itemView.findViewById(R.id.dropoffLocation)
        val timestampDate: TextView = itemView.findViewById(R.id.timestamp_date)
        val timestampTime: TextView = itemView.findViewById(R.id.timestamp_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        geocoder = Geocoder(parent.context)

        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.ride_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return rides.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ride = rides[position]

        val sdf = SimpleDateFormat("d MMMM, yyyy", Locale.getDefault())
        val time = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedDate = sdf.format(ride.timestamp.toDate())
        val formattedTime = time.format(ride.timestamp.toDate())
        holder.timestampDate.text = "$formattedDate"
        holder.timestampTime.text = "$formattedTime"

        try {
            // Reverse geocoding to get address from LatLng
            val pickupAddresses = geocoder.getFromLocation(ride.pickup.latitude, ride.pickup.longitude, 1)
            val dropoffAddresses = geocoder.getFromLocation(ride.dropoff.latitude, ride.dropoff.longitude, 1)

            if (!pickupAddresses.isNullOrEmpty() && !dropoffAddresses.isNullOrEmpty()) {
                val pickupAddress = pickupAddresses[0].getAddressLine(0)
                val dropoffAddress = dropoffAddresses[0].getAddressLine(0)

                // Set the address in the TextView
                holder.pickupLocation.text = pickupAddress
                holder.dropoffLocation.text = dropoffAddress
            } else {
                holder.pickupLocation.text = "No address found"
                holder.dropoffLocation.text = "No address found"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            holder.pickupLocation.text = "Unable to get the address"
            holder.dropoffLocation.text = "Unable to get the address"
        }
    }
}