package com.serenitysystems.livable.ui.wgregister

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.R
import com.google.android.material.button.MaterialButton
import com.serenitysystems.livable.ui.wohngesellschaft.data.Wg

class WgRegistrierungFragment : Fragment() {

    private lateinit var viewModel: WgRegistrierungViewModel
    private lateinit var adresseInput: EditText
    private lateinit var groesseInput: EditText
    private lateinit var zimmerInput: EditText
    private lateinit var bewohnerInput: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wg_registrierung, container, false)

        viewModel = ViewModelProvider(this).get(WgRegistrierungViewModel::class.java)

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
            // Hier plane ich noch die fehlermeldungen
            return
        }

        val wg = Wg(adresse, groesse, zimmer, bewohner)

        viewModel.registerWg(wg, {
            // Erfolgreiche Registrierung ->
        }, { exception ->
            // Fehlerfall ->
        })
    }
}
