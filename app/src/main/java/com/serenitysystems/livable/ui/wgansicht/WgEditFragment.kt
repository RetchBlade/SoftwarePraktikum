package com.serenitysystems.livable.ui.wgansicht

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WgEditFragment : Fragment() {

    private lateinit var wgAddressEdit: EditText
    private lateinit var roomCountEdit: EditText
    private lateinit var wgSizeEdit: EditText
    private lateinit var saveButton: Button
    private lateinit var bewohnerContainer: LinearLayout

    private val sharedViewModel: WgSharedViewModel by activityViewModels()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_wg_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        populateFields()
        populateRoommateList(isEditMode = true)

        saveButton.setOnClickListener {
            saveChanges(view)
        }
    }

    private fun initializeViews(view: View) {
        wgAddressEdit = view.findViewById(R.id.wgAddressEdit)
        roomCountEdit = view.findViewById(R.id.roomCountEdit)
        wgSizeEdit = view.findViewById(R.id.wgSizeEdit)
        saveButton = view.findViewById(R.id.saveButton)
        bewohnerContainer = view.findViewById(R.id.bewohnerContainer)
    }

    private fun populateFields() {
        sharedViewModel.wgAddress.observe(viewLifecycleOwner) {
            wgAddressEdit.setText(it)
        }
        sharedViewModel.roomCount.observe(viewLifecycleOwner) {
            roomCountEdit.setText(it)
        }
        sharedViewModel.wgSize.observe(viewLifecycleOwner) { size ->
            wgSizeEdit.setText(size.replace(" m²", ""))
        }
    }

    private fun populateRoommateList(isEditMode: Boolean) {
        val roommates = sharedViewModel.bewohnerList.value.orEmpty()
        bewohnerContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)
        roommates.forEach { (name, email) ->
            val roommateView = inflater.inflate(R.layout.roommate_item, bewohnerContainer, false)
            val profilePicture = roommateView.findViewById<ImageView>(R.id.profilePicture)
            val profileName = roommateView.findViewById<TextView>(R.id.profileName)
            val removeButton = roommateView.findViewById<ImageView>(R.id.removeRoommateButton)

            profileName.text = name

            fetchUserProfileImage(email) { profileImageUrl ->
                Glide.with(requireContext())
                    .load(profileImageUrl ?: R.drawable.pp_placeholder)
                    .circleCrop()
                    .into(profilePicture)
            }

            // Set visibility of the remove button based on edit mode
            removeButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
            removeButton.setOnClickListener {
                showRemoveRoommateDialog(email)
            }

            bewohnerContainer.addView(roommateView)
        }
    }

    private fun showRemoveRoommateDialog(email: String) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Mitbewohner entfernen")
            .setMessage("Sind Sie sicher, dass Sie diesen Mitbewohner entfernen möchten?")
            .setPositiveButton("Ja") { _, _ ->
                removeRoommate(email) // Call updated remove method
            }
            .setNegativeButton("Abbrechen", null)
            .create()

        dialog.show()
    }

    private fun removeRoommate(email: String) {
        lifecycleScope.launch {
            try {
                val wgId = sharedViewModel.wgId.value ?: throw Exception("WG ID nicht verfügbar!")
                val currentRoommates = sharedViewModel.bewohnerList.value?.map { it.second }?.toMutableList()
                currentRoommates?.remove(email)

                firestore.collection("WGs")
                    .document(wgId)
                    .update("mitgliederEmails", currentRoommates)
                    .await()

                sharedViewModel.loadWgDetails(email) // Reload details to update UI
                Snackbar.make(requireView(), "Mitbewohner entfernt!", Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Fehler beim Entfernen des Mitbewohners: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun saveChanges(view: View) {
        val newAddress = wgAddressEdit.text.toString().trim()
        val newRoomCount = roomCountEdit.text.toString().trim()
        val newWgSize = wgSizeEdit.text.toString().trim()

        if (newAddress.isEmpty() || newRoomCount.isEmpty() || newWgSize.isEmpty()) {
            Snackbar.make(view, "Alle Felder ausfüllen!", Snackbar.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                saveToFirestore(newAddress, newRoomCount, newWgSize)
                sharedViewModel.setWgDetails(newAddress, newRoomCount, newWgSize)
                Snackbar.make(view, "Änderungen gespeichert!", Snackbar.LENGTH_LONG).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Snackbar.make(view, "Fehler beim Speichern der Änderungen!", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun saveToFirestore(address: String, rooms: String, size: String) {
        val wgId = sharedViewModel.wgId.value ?: throw Exception("WG ID nicht verfügbar!")
        val wgData = mapOf(
            "adresse" to address,
            "zimmerAnzahl" to rooms,
            "groesse" to size
        )

        withContext(Dispatchers.IO) {
            firestore.collection("WGs")
                .document(wgId)
                .update(wgData)
                .await()
        }
    }

    private fun fetchUserProfileImage(email: String, callback: (String?) -> Unit) {
        firestore.collection("users").document(email)
            .get()
            .addOnSuccessListener { callback(it.getString("profileImageUrl")) }
            .addOnFailureListener { callback(null) }
    }
}
