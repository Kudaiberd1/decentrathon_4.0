package com.example.decentrathon40

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Random

class DriverSideActivity : AppCompatActivity() {
    private lateinit var currentPhotoPath: String
    private var currentPhotoType: String = ""

    // Request code for camera permission
    private val cameraPermissionRequestCode = 101

    // Views
    private lateinit var imageViewFront: ImageView
    private lateinit var imageViewSide: ImageView
    private lateinit var imageViewBack: ImageView
    private lateinit var buttonSubmit: Button
    private lateinit var buttonSkip: Button

    // Track which photos have been taken
    private var frontPhotoTaken = false
    private var sidePhotoTaken = false
    private var backPhotoTaken = false

    // Activity result launcher for camera
    private val takePictureResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Photo was taken successfully
            val photoFile = File(currentPhotoPath)
            if (photoFile.exists()) {
                // Load the image into the appropriate ImageView
                val scaledBitmap = decodeSampledBitmapFromFile(photoFile.absolutePath, 800, 600)
                when (currentPhotoType) {
                    "front" -> {
                        imageViewFront.setImageBitmap(scaledBitmap)
                        frontPhotoTaken = true
                    }
                    "side" -> {
                        imageViewSide.setImageBitmap(scaledBitmap)
                        sidePhotoTaken = true
                    }
                    "back" -> {
                        imageViewBack.setImageBitmap(scaledBitmap)
                        backPhotoTaken = true
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_side)

        // Initialize views
        val buttonTakePhotoFront: Button = findViewById(R.id.button_take_photo_front)
        val buttonTakePhotoSide: Button = findViewById(R.id.button_take_photo_side)
        val buttonTakePhotoBack: Button = findViewById(R.id.button_take_photo_back)
        buttonSubmit = findViewById(R.id.button_submit)
        buttonSkip = findViewById(R.id.button_skip)

        imageViewFront = findViewById(R.id.image_view_front)
        imageViewSide = findViewById(R.id.image_view_side)
        imageViewBack = findViewById(R.id.image_view_back)

        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Clear all existing photos on start
        clearAllPhotos()

        // Set click listeners for buttons
        buttonTakePhotoFront.setOnClickListener {
            if (checkCameraPermission()) {
                currentPhotoType = "front"
                dispatchTakePictureIntent()
            }
        }

        buttonTakePhotoSide.setOnClickListener {
            if (checkCameraPermission()) {
                currentPhotoType = "side"
                dispatchTakePictureIntent()
            }
        }

        buttonTakePhotoBack.setOnClickListener {
            if (checkCameraPermission()) {
                currentPhotoType = "back"
                dispatchTakePictureIntent()
            }
        }

        // Set click listener for Submit button
        buttonSubmit.setOnClickListener {
            checkAndSubmitPhotos()
        }

        // Set click listener for Skip button
        buttonSkip.setOnClickListener {
            showSkipWarningDialog()
        }
    }

    // Helper function to decode and scale down bitmap to avoid memory issues
    private fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun checkAndSubmitPhotos() {
        val missingPhotos = mutableListOf<String>()

        if (!frontPhotoTaken) missingPhotos.add("Front")
        if (!sidePhotoTaken) missingPhotos.add("Side")
        if (!backPhotoTaken) missingPhotos.add("Back")

        if (missingPhotos.isNotEmpty()) {
            // Show warning about missing photos
            val message = "Please take the following photos: ${missingPhotos.joinToString(", ")}"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } else {
            // All photos are taken, proceed to next activity
            proceedToCarStatusActivity(true)
        }
    }

    private fun showSkipWarningDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Skip Daily Check")
        builder.setMessage("If you skip the daily check, your driver rating may decline and you may be less likely to be recommended to clients.")

        builder.setPositiveButton("Continue") { dialog, which ->
            proceedToCarStatusActivity(false)
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun proceedToCarStatusActivity(carStatusChecked: Boolean) {
        val random = Random()

        // Randomly select car cleanness and damage status
        val carCleanness = if (random.nextBoolean()) "clean" else "dirty"
        val carDamage = if (random.nextBoolean()) "intact" else "damaged"

        val intent = Intent(this, CarStatusActivity::class.java).apply {
            putExtra("car_status_checked", carStatusChecked)
            putExtra("car_cleanness", carCleanness)
            putExtra("car_damage", carDamage)
        }

        // Print to console for debugging
        println("Sending to CarStatusActivity:")
        println("car_status_checked: $carStatusChecked")
        println("car_cleanness: $carCleanness")
        println("car_damage: $carDamage")

        startActivity(intent)
    }

    private fun clearAllPhotos() {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.let { dir ->
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()
                files?.forEach { file ->
                    if (file.isFile && (file.name.contains("front") || file.name.contains("side") || file.name.contains("back"))) {
                        file.delete()
                    }
                }
            }
        }

        // Reset ImageViews to default drawable
        imageViewFront.setImageResource(R.drawable.driving_school_rafiki)
        imageViewSide.setImageResource(R.drawable.driving_school_rafiki)
        imageViewBack.setImageResource(R.drawable.driving_school_rafiki)

        // Reset photo tracking
        frontPhotoTaken = false
        sidePhotoTaken = false
        backPhotoTaken = false
    }

    private fun checkCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionRequestCode)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry taking photo
                when (currentPhotoType) {
                    "front", "side", "back" -> dispatchTakePictureIntent()
                }
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(type: String): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${type}_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile(currentPhotoType)
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureResult.launch(takePictureIntent)
                }
            }
        }
    }
}