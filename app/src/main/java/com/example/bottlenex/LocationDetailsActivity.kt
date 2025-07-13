package com.example.bottlenex

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class LocationDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_details)

        // Set up CIF keyboard
        setupKeyboard()

        // Get data from intent
        val locationName = intent.getStringExtra("location_name") ?: "CLEMENTI"
        findViewById<TextView>(R.id.tvLocationTitle).text = locationName
    }

    private fun setupKeyboard() {
        val rows = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("⭕", "z", "x", "c", "v", "b", "n", "m", "💶")
        )

        val keyboardContainer = findViewById<LinearLayout>(R.id.keyboardContainer)

        rows.forEach { rowKeys ->
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
            }

            rowKeys.forEach { key ->
                val button = Button(this).apply {
                    text = key
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                    }
                    background = ContextCompat.getDrawable(this@LocationDetailsActivity, R.drawable.keyboard_key_bg)
                    setOnClickListener { onKeyPressed(key) }
                }
                rowLayout.addView(button)
            }

            keyboardContainer.addView(rowLayout)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun onKeyPressed(key: String) {
        // Handle keyboard presses
        Toast.makeText(this, "Pressed: $key", Toast.LENGTH_SHORT).show()
    }
}
