package com.serenitysystems.livable.ui.userprofil

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var wgIdText: TextView
    private lateinit var wgIdCopyIcon: ImageView

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
        observeWgDetails()

        lifecycleScope.launch {
            try {
                sharedViewModel.loadUserEmailAndWgDetails()
            } catch (e: Exception) {
                showError(view, "Fehler beim Laden der Daten: ${e.message}")
            }
        }

        wgIdText = view.findViewById(R.id.wgIdText)
        wgIdCopyIcon = view.findViewById(R.id.wgIdCopyIcon)

        sharedViewModel.wgId.observe(viewLifecycleOwner) { wgId ->
            wgIdText.text = wgId ?: "Nicht verfügbar"
        }

        wgIdCopyIcon.setOnClickListener {
            copyToClipboard(wgIdText)
        }

        editButton.setOnClickListener { navigateToEditFragment() }
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
            wgSizeText.text = "$size m²"
        }

        sharedViewModel.currentUserEmail.observe(viewLifecycleOwner) { email ->
            lifecycleScope.launch {
                if (email != null) {
                    checkAndSetLeiterStatus(email)
                }
            }
        }

        sharedViewModel.bewohnerList.observe(viewLifecycleOwner) { newBewohnerList ->
            sharedViewModel.rankIcons.observe(viewLifecycleOwner) { rankIcons ->
                if (rankIcons.isNotEmpty()) {
                    updateRoommateList(newBewohnerList)
                }
            }
        }
    }

    private fun updateRoommateList(newRoommates: List<Triple<String, String, Int>>) {
        bewohnerContainer.removeAllViews() // Wichtig: Vorher leeren

        val inflater = LayoutInflater.from(context)
        val rankIcons = sharedViewModel.rankIcons.value ?: emptyMap()

        newRoommates.forEachIndexed { index, (name, email, points) ->
            val roommateView = inflater.inflate(R.layout.wgansicht_roommate_item, bewohnerContainer, false)
            val profilePicture = roommateView.findViewById<ImageView>(R.id.profilePicture)
            val profileName = roommateView.findViewById<TextView>(R.id.profileName)
            val lifetimePointsText = roommateView.findViewById<TextView>(R.id.lifetimePoints)
            val rankIcon = roommateView.findViewById<ImageView>(R.id.rankIcon)

            profileName.text = "${index + 1}. $name"
            lifetimePointsText.text = "Punkte: $points"

            fetchUserProfileImage(email) { profileImageUrl ->
                Glide.with(requireContext())
                    .load(profileImageUrl)
                    .placeholder(R.drawable.pp_placeholder)
                    .circleCrop()
                    .into(profilePicture)
            }

            // Dynamisches Laden des Rank-Icons
            val rank = when {
                points < 500 -> "neuling"
                points < 1000 -> "bronze"
                points < 3000 -> "silber"
                points < 5000 -> "gold"
                else -> "champion"
            }

            val rankImageUrl = rankIcons[rank]
            if (!rankImageUrl.isNullOrEmpty()) {
                Glide.with(requireContext()).load(rankImageUrl).into(rankIcon)
            } else {
                rankIcon.setImageResource(R.drawable.einsteiger_ic)
            }

            profilePicture.setOnClickListener {
                sharedViewModel.setSelectedUserEmail(email)
                findNavController().navigate(R.id.nav_profilansicht)
            }

            bewohnerContainer.addView(roommateView)
        }
    }

    private fun fetchUserProfileImage(email: String, callback: (String?) -> Unit) {
        db.collection("users").document(email)
            .get()
            .addOnSuccessListener { callback(it.getString("profileImageUrl")) }
            .addOnFailureListener { callback(null) }
    }

    private suspend fun checkAndSetLeiterStatus(email: String) {
        withContext(Dispatchers.IO) {
            val userDoc = db.collection("users").document(email).get().await()
            val wgRole = userDoc.getString("wgRole")
            isLeiter = (wgRole == "Wg-Leiter")
            withContext(Dispatchers.Main) {
                editButton.visibility = if (isLeiter) View.VISIBLE else View.GONE
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

    private fun copyToClipboard(textView: TextView) {
        val wgId = textView.text.toString().trim()
        if (wgId.isEmpty() || wgId == "Nicht verfügbar") {
            Snackbar.make(textView, "Keine WG-ID vorhanden", Snackbar.LENGTH_SHORT).show()
            return
        }

        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WG ID", wgId)
        clipboard.setPrimaryClip(clip)

        // Feedback: Kurze Meldung oder Änderung des Textes
        textView.text = "✅ WG ID kopiert!"
        textView.postDelayed({
            textView.text = wgId
        }, 1500)
    }

    private fun showError(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }
}
