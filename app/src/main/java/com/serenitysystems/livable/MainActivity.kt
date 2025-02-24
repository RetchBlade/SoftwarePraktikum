package com.serenitysystems.livable

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.databinding.ActivityMainBinding
import com.serenitysystems.livable.ui.login.LoginActivity
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var userPreferences: UserPreferences
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dark Mode persistent setting: Lade den gespeicherten Wert und setze den Modus
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Initialize UserPreferences
        userPreferences = UserPreferences(this)

        // Set up view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if a user token is present
        lifecycleScope.launch {
            userPreferences.userToken.collect { userToken ->
                if (userToken != null) {
                    setupMainActivity(userToken)
                }
            }
        }
    }

    private fun setupMainActivity(userToken: UserToken) {
        // Enable edge-to-edge design
        enableEdgeToEdge()

        // Set the toolbar as the support action bar
        setSupportActionBar(binding.appBarMain.toolbar)

        // Initialize DrawerLayout and NavigationView
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Define top-level destinations for navigation
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_homepage,
                R.id.nav_wochenplan,
                R.id.nav_todo,
                R.id.nav_einkaufsliste,
                R.id.nav_haushaltsbuch
            ), drawerLayout
        )

        // Set up ActionBar and NavigationView with NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Customize Sidebar (NavigationView) to display user information
        val headerView: View = navView.getHeaderView(0)
        val userNicknameTextView = headerView.findViewById<TextView>(R.id.user_nickname_text_view)
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)

        // Set user nickname in the sidebar header
        userNicknameTextView.text = userToken.nickname

        // Listen for Firestore updates to WG info
        firestore.collection("users")
            .document(userToken.email)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Log.w("MainActivity", "Error listening to Firestore updates", e)
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val wgId = documentSnapshot.getString("wgId") ?: ""
                    updateNavigationMenu(wgId, navView)
                    val profileImageUrl = documentSnapshot.getString("profileImageUrl") ?: ""
                    // Überprüfe, ob die Activity noch nicht zerstört ist
                    if (!isFinishing && !isDestroyed) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.pp)
                            .into(profileImageView)
                    }
                }
            }

        // Set a listener for navigation
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    performLogout()
                    true
                }
                else -> {
                    navController.navigate(item.itemId)
                    drawerLayout.closeDrawers()
                    true
                }
            }
        }

    }

    private fun updateNavigationMenu(wgId: String, navView: NavigationView) {
        val menu = navView.menu
        if (wgId.isEmpty()) {
            menu.findItem(R.id.nav_wochenplan).isVisible = false
            menu.findItem(R.id.nav_einkaufsliste).isVisible = false
            menu.findItem(R.id.nav_haushaltsbuch).isVisible = false
        } else {
            menu.findItem(R.id.nav_wochenplan).isVisible = true
            menu.findItem(R.id.nav_einkaufsliste).isVisible = true
            menu.findItem(R.id.nav_haushaltsbuch).isVisible = true
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            userPreferences.clearUserToken()
            navigateToLoginActivity()
        }
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
