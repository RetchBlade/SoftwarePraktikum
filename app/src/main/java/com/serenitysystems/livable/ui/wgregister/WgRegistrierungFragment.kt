package com.serenitysystems.livable.ui.wgregister

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.serenitysystems.livable.R
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.wgregister.data.Wg
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WgRegistrierungFragment : Fragment() {

    private lateinit var viewModel: WgRegistrierungViewModel
    private lateinit var adresseInput: EditText
    private lateinit var groesseInput: EditText
    private lateinit var zimmerInput: EditText
    private lateinit var bewohnerInput: EditText
    private lateinit var userPreferences: UserPreferences



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wg_registrierung, container, false)

        viewModel = ViewModelProvider(this).get(WgRegistrierungViewModel::class.java)
        userPreferences = UserPreferences(requireContext())

        adresseInput = view.findViewById(R.id.adresseInput)
        groesseInput = view.findViewById(R.id.groesseInput)
        zimmerInput = view.findViewById(R.id.zimmerInput)
        bewohnerInput = view.findViewById(R.id.bewohnerInput)

        val registerButton = view.findViewById<MaterialButton>(R.id.registerButton)
        registerButton.setOnClickListener {
            registerWg()
        }
        return view
    }

    private fun registerWg() {
        val adresse = adresseInput.text.toString().trim()
        val groesse = groesseInput.text.toString().trim()
        val zimmer = zimmerInput.text.toString().trim()
        val bewohner = bewohnerInput.text.toString().trim()

        if (adresse.isEmpty() || groesse.isEmpty() || zimmer.isEmpty() || bewohner.isEmpty()) {
            // Error handling for empty fields
            adresseInput.error = "Alle Felder müssen ausgefüllt sein"
            return
        }

        val wg = Wg(adresse, groesse, zimmer, bewohner)

        lifecycleScope.launch {
            val userToken = userPreferences.userToken.first()
            val userEmail = userToken?.email ?: ""

            viewModel.registerWg(wg, userEmail, { wgId ->
                userToken?.let {
                    viewModel.updateUserInFirestore(it.email, wgId, "Wg-Leiter", {
                        Log.e("WgRegistrierungFragment", "Erfolgreich die WG erstellt und den User zugewiesen.")
                        // Navigiere zur Homepage
                        findNavController().navigate(R.id.nav_homepage)
                    }, { exception ->
                        Log.e("WgRegistrierungFragment", "Error updating user: ${exception.message}")
                        adresseInput.error = "Fehler beim Aktualisieren des Benutzers"
                    })
                } ?: run {
                    Log.e("WgRegistrierungFragment", "User token is null")
                }
            }, { exception ->
                Log.e("WgRegistrierungFragment", "Error registering WG: ${exception.message}")
                adresseInput.error = "Fehler beim Erstellen der WG"
            })
        }
    }

}
