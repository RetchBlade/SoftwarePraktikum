package com.serenitysystems.livable

import android.os.Bundle
import android.widget.Button
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.serenitysystems.livable.databinding.ActivityMainBinding

import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // Konfiguration der AppBar
    private lateinit var appBarConfiguration: AppBarConfiguration
    // Binding für die Hauptaktivität
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aktivieren des randlosen Designs
        enableEdgeToEdge()
        // Setzen des Inhaltslayouts auf das Login-Fragment
        setContentView(R.layout.fragment_login)

        // Anwenden von Fenster-Insets auf ein Hintergrundbild
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.backgroundImage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Finden und Setzen des Klick-Listeners für den Login-Button
        val btnlogin = findViewById<Button>(R.id.btnLogin)
        btnlogin.setOnClickListener{
            // Initialisieren des Bindings mit dem Hauptlayout
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Setzen der Toolbar als Support-ActionBar
            setSupportActionBar(binding.appBarMain.toolbar)

            // Initialisieren der Drawer-Layout- und Navigations-View-Komponenten
            val drawerLayout: DrawerLayout = binding.drawerLayout
            val navView: NavigationView = binding.navView
            val navController = findNavController(R.id.nav_host_fragment_content_main)

            // Festlegen der Top-Level-Ziele für die Navigation
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_wochenplan, R.id.nav_todo, R.id.nav_einkaufsliste
                ), drawerLayout
            )
            // Einrichten der ActionBar mit dem NavController
            setupActionBarWithNavController(navController, appBarConfiguration)
            // Einrichten der NavigationView mit dem NavController
            navView.setupWithNavController(navController)
        }
    }

    // Handhaben der Navigation, wenn die Zurück-Taste in der ActionBar gedrückt wird
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
