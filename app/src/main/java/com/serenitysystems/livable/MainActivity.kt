package com.serenitysystems.livable

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.bumptech.glide.Glide
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.databinding.ActivityMainBinding
import com.serenitysystems.livable.interfaces.TokenRefreshListener
import com.serenitysystems.livable.ui.login.LoginActivity
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity(), TokenRefreshListener {

    // Konfiguration der AppBar
    private lateinit var appBarConfiguration: AppBarConfiguration
    // Binding für die Hauptaktivität
    private lateinit var binding: ActivityMainBinding
    private lateinit var userPreferences: UserPreferences
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisiere UserPreferences
        userPreferences = UserPreferences(this)

        // Set up the binding and SwipeRefreshLayout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        swipeRefreshLayout = binding.swipeRefreshLayout

        // Prüfe, ob UserToken vorhanden ist
        lifecycleScope.launch {
            userPreferences.userToken.collect { userToken ->
                if (userToken != null) {
                    // Benutzer ist eingeloggt, fahre mit MainActivity fort
                    setupMainActivity(userToken) // Übergebe usertoken für die Sidebar
                } else {
                    // Kein Benutzer eingeloggt, navigiere zur LoginActivity
                    navigateToLoginActivity()
                }
            }
        }
    }

    private fun setupMainActivity(userToken: UserToken) {
        // Aktivieren des randlosen Designs
        enableEdgeToEdge()

        // Setzen der Toolbar als Support-ActionBar
        setSupportActionBar(binding.appBarMain.toolbar)

        // Initialisieren der Drawer-Layout- und Navigations-View-Komponenten
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Festlegen der Top-Level-Ziele für die Navigation
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_homepage,
                R.id.nav_wochenplan,
                R.id.nav_todo,
                R.id.nav_einkaufsliste,
                R.id.nav_haushaltsbuch
            ), drawerLayout
        )

        // Einrichten der ActionBar mit dem NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        // Einrichten der NavigationView mit dem NavController
        navView.setupWithNavController(navController)

        // Sidebar (NavigationView) anpassen, um WgId und E-Mail anzuzeigen
        val headerView: View = navView.getHeaderView(0)
        val userNicknameTextView = headerView.findViewById<TextView>(R.id.user_nickname_text_view)
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)

        // Setze die E-Mail im Sidebar-Header
        userNicknameTextView.text = userToken.nickname

        // Lade das Profilbild in die ImageView mit Glide
        Glide.with(this)
            .load(userToken.profileImageUrl)
            .placeholder(R.drawable.pp) // Fallback-Bild, wenn das Bild nicht geladen werden kann
            .into(profileImageView)

        // Update navigation menu based on wgId
        updateNavigationMenu(userToken.wgId, navView)

        // Setze einen Listener für die Navigation
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> {
                    // Führe den Logout durch
                    performLogout()
                    true
                }
                else -> {
                    // Navigiere zu den Fragmenten
                    navController.navigate(item.itemId)
                    drawerLayout.closeDrawers() // Schließe das DrawerLayout
                    true // Gebe true zurück, um anzuzeigen, dass das Element verarbeitet wurde
                }
            }
        }

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            refreshUserToken(userToken) // Refresh the user token
        }
    }

    private fun updateNavigationMenu(wgId: String, navView: NavigationView) {
        val menu = navView.menu

        // Show or hide menu items based on wgId
        if (wgId.isEmpty()) {
            // Hide items if wgId is empty
            menu.findItem(R.id.nav_wochenplan).isVisible = false
            menu.findItem(R.id.nav_einkaufsliste).isVisible = false
            menu.findItem(R.id.nav_haushaltsbuch).isVisible = false
        } else {
            // Show items if wgId is present
            menu.findItem(R.id.nav_wochenplan).isVisible = true
            menu.findItem(R.id.nav_einkaufsliste).isVisible = true
            menu.findItem(R.id.nav_haushaltsbuch).isVisible = true
        }
    }



        override fun refreshUserToken(currentToken: UserToken) {
        swipeRefreshLayout.isRefreshing = true // Starte die Refresh-Animation

        lifecycleScope.launch {
            try {
                // Holen der aktuellen Benutzerdaten aus Firestore
                val newToken = retrieveUserDataFromFirestore(currentToken.email)

                // Aktualisiere das UI und die Präferenzen nur, wenn der neue Token anders ist
                if (newToken != currentToken) {
                    // Speichere den neuen Token nur, wenn er sich geändert hat
                    userPreferences.saveUserToken(newToken) // Speichere den neuen Token

                    // Aktualisiere das UI mit dem neuen Benutzertoken
                    updateUIWithNewToken(newToken)

                    // Update navigation menu based on new wgId
                    updateNavigationMenu(newToken.wgId, binding.navView)
                } else {
                    Log.d("MainActivity", "Benutzertoken hat sich nicht geändert.")
                }

            } catch (e: Exception) {
                // Behandle Fehler, die während des Abrufs des Tokens auftreten
                Log.e("MainActivity", "Fehler beim Aktualisieren des Benutzertokens: ${e.message}", e)
            } finally {
                // Stoppe die Refresh-Animation
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private suspend fun retrieveUserDataFromFirestore(email: String): UserToken {
        return suspendCoroutine { continuation ->
            firestore.collection("users").document(email)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        // Benutzerdaten wurden gefunden, erstelle einen neuen UserToken
                        val newToken = UserToken(
                            email = document.getString("email") ?: email,
                            nickname = document.getString("nickname") ?: "Unknown",
                            password = document.getString("password") ?: "", // Beachte, dass du das Passwort nicht speichern solltest
                            birthdate = document.getString("birthdate") ?: "",
                            gender = document.getString("gender") ?: "",
                            wgId = document.getString("wgId") ?: "",
                            wgRole = document.getString("wgRole") ?: "",
                            profileImageUrl = document.getString("profileImageUrl") ?: ""
                        )
                        continuation.resume(newToken)
                    } else {
                        continuation.resumeWithException(Exception("No such document"))
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    private fun updateUIWithNewToken(newToken: UserToken) {
        // Update the UI elements as needed with the new user token data
        val headerView: View = binding.navView.getHeaderView(0)
        val userNicknameTextView = headerView.findViewById<TextView>(R.id.user_nickname_text_view)
        val profileImageView = headerView.findViewById<ImageView>(R.id.imageView)

        // Update the UI with the new data
        userNicknameTextView.text = newToken.nickname

        Glide.with(this)
            .load(newToken.profileImageUrl)
            .placeholder(R.drawable.pp) // Fallback image
            .into(profileImageView)
    }

    private fun performLogout() {
        // Lösche den UserToken
        lifecycleScope.launch {
            userPreferences.clearUserToken()

            // Navigiere zur LoginActivity und beende die MainActivity
            navigateToLoginActivity()
        }
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Beendet die MainActivity, damit der Benutzer nicht zurückkehren kann
    }

    // Handhaben der Navigation, wenn die Zurück-Taste in der ActionBar gedrückt wird
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
