package com.example.decentrathon40

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginTop
import java.io.File

class CarStatusActivity : AppCompatActivity() {

    companion object {
        private const val IMAGE_CYCLE_DELAY_MS = 2000L // 2.5 seconds
    }

    private lateinit var imageViewCar: ImageView
    private lateinit var imageCycleHandler: Handler
    private var imageCycleRunnable: Runnable? = null
    private var currentImageIndex = 0
    private val carImages = mutableListOf<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_car_status)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get the data sent from DriverSideActivity
        val carStatusChecked = intent.getBooleanExtra("car_status_checked", false)
        val carCleanness = intent.getStringExtra("car_cleanness") ?: "unknown"
        val carDamage = intent.getStringExtra("car_damage") ?: "unknown"

        // Print to console for debugging
        Log.d("CarStatusActivity", "Car status checked: $carStatusChecked")
        Log.d("CarStatusActivity", "Car cleanness: $carCleanness")
        Log.d("CarStatusActivity", "Car damage: $carDamage")

        // Initialize views
        val titleTextView = findViewById<TextView>(R.id.text_view_title)
        val subtitleTextView = findViewById<TextView>(R.id.text_view_subtitle)
        val cleanlinessLabel = findViewById<TextView>(R.id.text_view_cleanliness_label)
        val cleanlinessValue = findViewById<TextView>(R.id.text_view_cleanliness_value)
        val damageLabel = findViewById<TextView>(R.id.text_view_damage_label)
        val damageValue = findViewById<TextView>(R.id.text_view_damage_value)
        val inspectionLabel = findViewById<TextView>(R.id.text_view_inspection_label)
        val inspectionValue = findViewById<TextView>(R.id.text_view_inspection_value)
        val inspectionLabelWhenSkipped = findViewById<TextView>(R.id.text_view_inspection_label_when_skipped)
        val inspectionValueWhenSkipped = findViewById<TextView>(R.id.text_view_inspection_value_when_skipped)
        val additionalInfo = findViewById<TextView>(R.id.text_view_additional_info)
        val backButton = findViewById<Button>(R.id.button_back_to_menu)
        imageViewCar = findViewById(R.id.image_view_car)

        // Initialize handler for image cycling
        imageCycleHandler = Handler(Looper.getMainLooper())

        // Update UI based on car status
        if (carStatusChecked) {
            // Car was properly checked - show all status information
            titleTextView.text = "Car Status Report"
            subtitleTextView.text = "Daily inspection completed successfully"
            inspectionValue.text = "Completed"
            inspectionValue.setTextColor(getColor(android.R.color.holo_green_dark))
            additionalInfo.text = "Your vehicle has been verified and is ready for service. Thank you for maintaining high standards!"

            // Show cleanliness and damage status
            cleanlinessLabel.visibility = View.VISIBLE
            cleanlinessValue.visibility = View.VISIBLE
            damageLabel.visibility = View.VISIBLE
            damageValue.visibility = View.VISIBLE

            // Update cleanliness and damage status
            cleanlinessValue.text = carCleanness.replaceFirstChar { it.uppercase() }
            damageValue.text = carDamage.replaceFirstChar { it.uppercase() }

            // Set color based on status
            when (carCleanness) {
                "clean" -> cleanlinessValue.setTextColor(getColor(android.R.color.holo_green_dark))
                "dirty" -> cleanlinessValue.setTextColor(getColor(android.R.color.holo_red_dark))
                else -> cleanlinessValue.setTextColor(getColor(android.R.color.darker_gray))
            }

            when (carDamage) {
                "intact" -> damageValue.setTextColor(getColor(android.R.color.holo_green_dark))
                "damaged" -> damageValue.setTextColor(getColor(android.R.color.holo_red_dark))
                else -> damageValue.setTextColor(getColor(android.R.color.darker_gray))
            }

            // Load and cycle through the submitted photos
            loadAndCycleCarImages()

        } else {
            // Car check was skipped - hide cleanliness and damage status
            titleTextView.text = "Inspection Skipped"
            subtitleTextView.text = "Daily inspection was not completed"
            inspectionValue.text = "Skipped"
            inspectionValue.setTextColor(getColor(android.R.color.holo_red_dark))
            additionalInfo.text = "Your driver rating may be affected. Completing daily inspections helps maintain your reputation and increases client recommendations."

            // Hide cleanliness and damage status since inspection was skipped
            cleanlinessLabel.visibility = View.GONE
            cleanlinessValue.visibility = View.GONE
            damageLabel.visibility = View.GONE
            damageValue.visibility = View.GONE
            inspectionLabel.visibility = View.GONE
            inspectionValue.visibility = View.GONE

            inspectionLabelWhenSkipped.visibility = View.VISIBLE
            inspectionValueWhenSkipped.visibility = View.VISIBLE
        }

        // Set up back button to return to EnterActivity
        backButton.setOnClickListener {
            // Stop image cycling when leaving the activity
            stopImageCycling()
            val intent = Intent(this, EnterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun loadAndCycleCarImages() {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.let { dir ->
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()
                files?.forEach { file ->
                    if (file.isFile && (file.name.contains("front") || file.name.contains("side") || file.name.contains("back"))) {
                        try {
                            val bitmap = decodeSampledBitmapFromFile(file.absolutePath, 800, 600)
                            carImages.add(bitmap)
                        } catch (e: Exception) {
                            Log.e("CarStatusActivity", "Error loading image: ${file.name}", e)
                        }
                    }
                }
            }
        }

        // If we have images to cycle through, start the cycle
        if (carImages.isNotEmpty()) {
            startImageCycling()
        }
    }

    private fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun startImageCycling() {
        imageCycleRunnable = object : Runnable {
            override fun run() {
                if (carImages.isNotEmpty()) {
                    // Display the current image
                    imageViewCar.setImageBitmap(carImages[currentImageIndex])

                    // Move to the next image (cycle back to start if at end)
                    currentImageIndex = (currentImageIndex + 1) % carImages.size

                    // Schedule the next image change
                    imageCycleHandler.postDelayed(this, IMAGE_CYCLE_DELAY_MS)
                }
            }
        }

        // Start the image cycling
        imageCycleRunnable?.run()
    }

    private fun stopImageCycling() {
        imageCycleRunnable?.let {
            imageCycleHandler.removeCallbacks(it)
        }
        imageCycleRunnable = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up to prevent memory leaks
        stopImageCycling()
        carImages.forEach { it.recycle() }
        carImages.clear()
    }
}