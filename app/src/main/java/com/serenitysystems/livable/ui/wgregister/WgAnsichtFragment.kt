package com.serenitysystems.livable.ui.userprofil

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.serenitysystems.livable.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WgAnsichtFragment : Fragment() {
    private val sharedViewModel: WgSharedViewModel by activityViewModels()
    private lateinit var wgAddressText: TextView
    private lateinit var roomCountText: TextView
    private lateinit var wgSizeText: TextView
    private lateinit var editButton: ImageView
    private lateinit var bewohnerContainer: LinearLayout

    private var isLeiter = false

    private val roommates = listOf(
        Pair("Haneen", R.drawable.pp_placeholder),
        Pair("Safak", R.drawable.pp_placeholder),
        Pair("Lorenz", R.drawable.pp_placeholder),
        Pair("Eray", R.drawable.pp_placeholder)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_wg_ansicht, container, false)
    }

    private fun loadSavedData() {
        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val wgAddress = preferences.getString("wgAddress", "Beispielstraße 123")
        val roomCount = preferences.getString("roomCount", "3")
        val wgSize = preferences.getString("wgSize", "85 m²")

        wgAddressText.text = wgAddress
        roomCountText.text = roomCount
        wgSizeText.text = wgSize
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupInitialVisibility()
        populateRoommateList()

        // Daten laden und anzeigen
        loadSavedData()

        editButton.setOnClickListener {
            navigateToEditFragment()
        }
    }

    private fun initializeViews(view: View) {
        wgAddressText = view.findViewById(R.id.wgAddressText)
        roomCountText = view.findViewById(R.id.roomCountText)
        wgSizeText = view.findViewById(R.id.wgSizeText)
        editButton = view.findViewById(R.id.editButton)
        bewohnerContainer = view.findViewById(R.id.bewohnerContainer)
    }

    private fun setupInitialVisibility() {
        lifecycleScope.launch {
            isLeiter = checkUserRole()
            withContext(Dispatchers.Main) {
                editButton.visibility = if (isLeiter) View.VISIBLE else View.GONE
            }
        }
    }

    private suspend fun checkUserRole(): Boolean {
        return withContext(Dispatchers.IO) {
            // Simulierte Rollenprüfung
            true // Angenommen, der Benutzer ist "Leiter"
        }
    }

    private fun populateRoommateList() {
        val inflater = LayoutInflater.from(context)

        for ((name, imageRes) in roommates) {
            val roommateView = inflater.inflate(R.layout.roommate_item, bewohnerContainer, false)
            val profilePicture: ImageView = roommateView.findViewById(R.id.profilePicture)
            val profileName: TextView = roommateView.findViewById(R.id.profileName)

            profilePicture.setImageResource(imageRes)
            profileName.text = name

            bewohnerContainer.addView(roommateView)
        }
    }

    private fun navigateToEditFragment() {
        sharedViewModel.setWgDetails(
            wgAddressText.text.toString(),
            roomCountText.text.toString(),
            wgSizeText.text.toString()
        )
        findNavController().navigate(R.id.action_wgAnsichtFragment_to_wgEditFragment)
    }
}