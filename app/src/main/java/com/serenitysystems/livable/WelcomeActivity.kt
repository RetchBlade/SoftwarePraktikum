package com.serenitysystems.livable

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.serenitysystems.livable.ui.login.LoginActivity  // Importiere die LoginActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_page)

        // Get references to the views
        val logoImage: ImageView = findViewById(R.id.logoImage)
        val motivText: TextView = findViewById(R.id.motivText)

        // Load animations
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        val textFadeIn = AnimationUtils.loadAnimation(this, R.anim.text_fade_in)

        // Set visibility to VISIBLE before starting animations
        motivText.visibility = TextView.INVISIBLE

        // Start both animations simultaneously
        logoImage.startAnimation(logoAnim)
        motivText.startAnimation(textFadeIn)

        // Navigate to the login activity after a 3-second delay
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@WelcomeActivity, LoginActivity::class.java)
            startActivity(intent)
            finish() // Finish WelcomeActivity so users can't go back to it
        }, 3000) // seconds kann man einstellen
    }
}
