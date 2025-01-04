package com.serenitysystems.livable.ui.userprofil

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.R
import com.serenitysystems.livable.ui.wgansicht.WgSharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WgAnsichtFragment : Fragment() {
    private val sharedViewModel: WgSharedViewModel by activityViewModels()

    private lateinit var wgAddressText: TextView
    private lateinit var roomCountText: TextView
    private lateinit var wgSizeText: TextView
    private lateinit var editButton: ImageView
    private lateinit var bewohnerContainer: LinearLayout

    private val db = FirebaseFirestore.getInstance()
    private var isLeiter = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_wg_ansicht, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)

        // Observe LiveData from the ViewModel to update UI
        observeWgDetails()

        // Load WG details and check user role
        lifecycleScope.launch {
            val userEmail = fetchUserEmailFromFirestore()
            sharedViewModel.loadWgDetails(userEmail)

            // Check if the user is a Wg-Leiter
            checkAndSetLeiterStatus(userEmail)
        }

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
    private fun observeWgDetails() {
        sharedViewModel.wgAddress.observe(viewLifecycleOwner) { address ->
            wgAddressText.text = address
        }

        sharedViewModel.roomCount.observe(viewLifecycleOwner) { roomCount ->
            roomCountText.text = roomCount
        }

        sharedViewModel.wgSize.observe(viewLifecycleOwner) { size ->
            wgSizeText.text = "$size m²" // Append "m²" to the size value
        }

        sharedViewModel.bewohnerList.observe(viewLifecycleOwner) { bewohnerList ->
            populateRoommateList(bewohnerList)
        }
    }


    private fun populateRoommateList(roommates: List<Pair<String, String>>) {
        bewohnerContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)
        for ((name, email) in roommates) {
            val roommateView = inflater.inflate(R.layout.roommate_item, bewohnerContainer, false)
            val profilePicture: ImageView = roommateView.findViewById(R.id.profilePicture)
            val profileName: TextView = roommateView.findViewById(R.id.profileName)

            profileName.text = name

            // Fetch and load profile image
            fetchUserProfileImage(email) { profileImageUrl ->
                if (!profileImageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(profileImageUrl)
                        .circleCrop()
                        .into(profilePicture)
                } else {
                    profilePicture.setImageResource(R.drawable.pp_placeholder) // Default placeholder
                }
            }

            bewohnerContainer.addView(roommateView)
        }
    }

    private suspend fun fetchUserEmailFromFirestore(): String {
        return withContext(Dispatchers.IO) {
            try {
                val userPreferences = db.collection("users").get().await()
                val currentUser = userPreferences.documents.firstOrNull()
                val email = currentUser?.getString("email")
                Log.d("WgAnsichtFragment", "Fetched user email: $email") // Debug log
                email ?: throw Exception("User email not found.")
            } catch (e: Exception) {
                Log.e("WgAnsichtFragment", "Failed to fetch user email", e)
                throw e
            }
        }
    }


    private fun fetchUserProfileImage(email: String, callback: (String?) -> Unit) {
        db.collection("users").document(email)
            .get()
            .addOnSuccessListener { document ->
                val profileImageUrl = document.getString("profileImageUrl")
                callback(profileImageUrl)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    private suspend fun checkAndSetLeiterStatus(userEmail: String) {
        withContext(Dispatchers.IO) {
            try {
                val userDoc = db.collection("users").document(userEmail).get().await()
                val wgRole = userDoc.getString("wgRole")
                isLeiter = (wgRole == "Wg-Leiter")
                withContext(Dispatchers.Main) {
                    editButton.visibility = if (isLeiter) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    editButton.visibility = View.GONE
                }
            }
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
