package com.example.bottlenex

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DirectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direction)

        // Update time
        val timeTextView = findViewById<TextView>(R.id.tvTime)
        timeTextView.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.rvLocations)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = LocationAdapter(getSampleLocations())
        recyclerView.adapter = adapter

        // Set item click listener
        adapter.setOnItemClickListener { location ->
            val intent = Intent(this, LocationDetailsActivity::class.java).apply {
                putExtra("location_name", location.name)
            }
            startActivity(intent)
        }
    }

    private fun getSampleLocations(): List<Location> {
        return listOf(
            Location("CLEMENTI", "CIF", ""),
            Location("Commenti'Ra", "Urban Care Employ", "8 mins - 2.6km"),
            Location("Sinopec", "10 min", "8 mins - 2.6km")
        )
    }
}

data class Location(
    val name: String,
    val description: String,
    val distance: String
)

class LocationAdapter(private val locations: List<Location>) : 
    RecyclerView.Adapter<LocationAdapter.LocationViewHolder>() {

    private var onItemClickListener: ((Location) -> Unit)? = null

    fun setOnItemClickListener(listener: (Location) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locations[position]
        holder.bind(location)
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(location)
        }
    }

    override fun getItemCount(): Int = locations.size

    class LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(location: Location) {
            text1.text = location.name
            text2.text = "${location.description} ${location.distance}".trim()
        }
    }
}
