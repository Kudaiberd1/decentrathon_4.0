package com.example.decentrathon40

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EnterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_enter)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val driverSideButton: Button = findViewById<Button>(R.id.button_demo_driver)
        val clientSideButton: Button = findViewById<Button>(R.id.button_demo_client)

        driverSideButton.setOnClickListener {
            val intent = Intent(this, DriverSideActivity::class.java)
            startActivity(intent)
        }

        clientSideButton.setOnClickListener {
            val intent = Intent(this, ClientSideActivity::class.java)
            startActivity(intent)
        }
    }
}