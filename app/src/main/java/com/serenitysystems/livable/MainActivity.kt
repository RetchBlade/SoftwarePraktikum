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


class MainActivity : AppCompatActivity() {

    // Konfiguration der AppBar
    private lateinit var appBarConfiguration: AppBarConfiguration
    // Binding für die Hauptaktivität
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aktivieren des randlosen Designs
        enableEdgeToEdge()

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
                R.id.nav_wochenplan, R.id.nav_todo, R.id.nav_einkaufsliste, R.id.nav_haushaltsbuch
            ), drawerLayout
        )
        // Einrichten der ActionBar mit dem NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        // Einrichten der NavigationView mit dem NavController
        navView.setupWithNavController(navController)
    }

    // Handhaben der Navigation, wenn die Zurück-Taste in der ActionBar gedrückt wird
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
