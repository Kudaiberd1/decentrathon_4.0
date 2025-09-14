package com.example.decentrathon40

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class ClientSideActivity : AppCompatActivity() {// Sample data for drivers
private val firstNames = listOf(
        "Nurbolat", "Bekzhan", "Nurlan", "Dastan", "Ayat",
        "Nazer", "Fauske", "Askar", "Yermek", "Alibi"
    )

    private val lastNames = listOf(
        "Narimanev", "Dauletev", "Rasulev", "Baltabekev", "Ardagerev",
        "Erzhanev", "Alibiev", "Dastanev", "Abzalev", "Baurzhanov"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_client_side)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton = findViewById<Button>(R.id.button_back_to_menu)
        val driversContainer = findViewById<LinearLayout>(R.id.drivers_container)

        // Configure LinearLayout for dividers
        driversContainer.orientation = LinearLayout.VERTICAL // Ensure it's vertical
        driversContainer.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE // Show dividers between items

        // Convert 10dp to pixels for dividerPadding
        val density = resources.displayMetrics.density
        val dividerPaddingPx = (10 * density).toInt()
        driversContainer.dividerPadding = dividerPaddingPx


        // Add top margin to the first driver item (you might adjust this or remove if dividerPadding is enough)
        driversContainer.setPadding(20, 8, 20, 8) // Existing padding

        // Generate and display fake drivers
        generateFakeDrivers().forEach { driver ->
            val driverView = createDriverView(driver)
            driversContainer.addView(driverView)
        }

        // Set up back button
        backButton.setOnClickListener {
            val intent = Intent(this, EnterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private data class Driver(
        val firstName: String,
        val lastName: String,
        val hasCarStatusChecked: Boolean,
        val carCleanness: String,
        val carDamage: String,
        val price: Int // Price in Tenge
    )

    private fun generateFakeDrivers(): List<Driver> {
        val drivers = mutableListOf<Driver>()
        val random = Random.Default

        // Generate 8-10 random drivers
        val driverCount = random.nextInt(8, 11)

        repeat(driverCount) {
            val firstName = firstNames.random()
            val lastName = lastNames.random()
            val hasCarStatusChecked = random.nextFloat() > 0.3f // 70% chance of verified status

            val carCleanness = if (hasCarStatusChecked) {
                if (random.nextBoolean()) "clean" else "dirty"
            } else {
                "unknown"
            }

            val carDamage = if (hasCarStatusChecked) {
                if (random.nextBoolean()) "intact" else "damaged"
            } else {
                "unknown"
            }

            // Base price in Tenge (1$ = 500₸, so base ~8$ = 4000₸)
            var basePrice = 4000

            // Adjust price based on status
            if (hasCarStatusChecked) {
                if (carCleanness == "clean") basePrice += 500
                if (carDamage == "intact") basePrice += 500
                // Premium for verified status
                basePrice += 300
            } else {
                // Discount for unverified status
                basePrice -= 500
            }

            // Add some random variation (round to nearest 100)
            val variation = random.nextInt(-300, 301)
            val finalPrice = ((basePrice + variation) / 100) * 100

            drivers.add(Driver(firstName, lastName, hasCarStatusChecked, carCleanness, carDamage, finalPrice))
        }

        // Sort by price (highest first)
        return drivers.sortedByDescending { it.price }
    }

    private fun createDriverView(driver: Driver): View {
        val inflater = LayoutInflater.from(this)
        val driverView = inflater.inflate(R.layout.item_driver, null, false)

        val driverPhoto = driverView.findViewById<ImageView>(R.id.image_view_driver)
        val driverName = driverView.findViewById<TextView>(R.id.text_view_driver_name)
        val carStatus = driverView.findViewById<TextView>(R.id.text_view_car_status)
        val price = driverView.findViewById<TextView>(R.id.text_view_price)

        // Set driver photo (using a placeholder icon)
        driverPhoto.setImageResource(R.drawable.ic_person)

        // Set driver name
        driverName.text = "${driver.firstName} ${driver.lastName}"

        // Set car status information with colors
        if (driver.hasCarStatusChecked) {
            val cleanColor = if (driver.carCleanness == "clean") Color.GREEN else Color.RED
            val damageColor = if (driver.carDamage == "intact") Color.GREEN else Color.RED

            val statusText = "✓ Verified • " +
                    "${getColoredText(driver.carCleanness.replaceFirstChar { it.uppercase() }, cleanColor)} • " +
                    "${getColoredText(driver.carDamage.replaceFirstChar { it.uppercase() }, damageColor)}"

            carStatus.text = android.text.Html.fromHtml(statusText, android.text.Html.FROM_HTML_MODE_LEGACY)
            carStatus.setTextColor(Color.GRAY)
        } else {
            carStatus.text = "⚠ Unverified • Status unknown"
            carStatus.setTextColor(Color.GRAY)
        }

        // Set price in Tenge
        price.text = "${driver.price}₸"

        price.setTextColor(getColor(android.R.color.holo_blue_dark))

        // Set click listener
        driverView.setOnClickListener {
            val message = if (driver.hasCarStatusChecked) {
                "Selected ${driver.firstName} ${driver.lastName} - Verified driver with ${driver.carCleanness}, ${driver.carDamage} car for ${driver.price}₸"
            } else {
                "Selected ${driver.firstName} ${driver.lastName} - Unverified driver for ${driver.price}₸"
            }
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        }

        return driverView
    }

    private fun getColoredText(text: String, color: Int): String {
        val hexColor = String.format("#%06X", 0xFFFFFF and color)
        return "<font color='$hexColor'>$text</font>"
    }
}