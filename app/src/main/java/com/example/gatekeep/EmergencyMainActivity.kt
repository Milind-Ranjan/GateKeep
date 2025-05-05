package com.example.gatekeep

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Emergency fallback activity in case other activities are crashing.
 * This is a very simple activity with minimal layout to ensure it loads.
 */
class EmergencyMainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple TextView
        val textView = TextView(this).apply {
            text = "GateKeep is running. Please wait while we set up your app."
            textSize = 18f
            setPadding(40, 100, 40, 100)
        }
        
        // Set it as the content view
        setContentView(textView)
        
        // Start MainActivity after a short delay
        android.os.Handler(mainLooper).postDelayed({
            try {
                // Try to start MainActivity
                startActivity(android.content.Intent(this, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                // Update text to show error
                textView.text = "Error: ${e.message}. Please reinstall the app."
            }
        }, 2000)
    }
} 