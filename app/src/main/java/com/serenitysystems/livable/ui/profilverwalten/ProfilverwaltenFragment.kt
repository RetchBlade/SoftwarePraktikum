package com.serenitysystems.livable.ui.profilverwalten

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentProfilverwaltenBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class ProfilverwaltenFragment : Fragment() {

    private var _binding: FragmentProfilverwaltenBinding? = null
    private val binding get() = _binding!!

    private val profilverwaltenViewModel: ProfilverwaltenViewModel by activityViewModels()

    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilverwaltenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()

        binding.uploadImageButton.setOnClickListener {
            openGalleryForImage()
        }

        binding.saveProfileButton.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun observeViewModel() {
        profilverwaltenViewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.benutzerName.setText(it.nickname)
                binding.emailInput.setText(it.email)
                binding.wgId.text = "WG-ID: ${it.wgId}"
                binding.roleTextView.text = "WG-Rolle: ${it.wgRole}"
                Glide.with(this)
                    .load(it.profileImageUrl)
                    .placeholder(R.drawable.pp_placeholder)
                    .into(binding.profileImageView)
            }
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Bild auswählen"), IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.profileImageView.setImageURI(selectedImageUri) // Zeige Vorschau des Bildes
        }
    }

    private fun saveProfileChanges() {
        val nickname = binding.benutzerName.text.toString()
        val email = binding.emailInput.text.toString()
        val emailConfirm = binding.emailconfirm.text.toString()
        val oldPassword = binding.oldPasswordInput.text.toString()
        val password = binding.passwordInput.text.toString()
        val passwordConfirm = binding.passworconfirm.text.toString()

        if (nickname.isBlank() || email.isBlank()) {
            binding.benutzerName.error = "Benutzername darf nicht leer sein"
            binding.emailInput.error = "E-Mail darf nicht leer sein"
            return
        }

        if (emailConfirm.isNotBlank() && email != emailConfirm) {
            binding.emailInput.error = "E-Mails stimmen nicht überein"
            binding.emailconfirm.error = "E-Mails stimmen nicht überein"
            return
        }

        if (password.isNotBlank()) {
            if (password != passwordConfirm) {
                binding.passwordInput.error = "Passwörter stimmen nicht überein"
                binding.passworconfirm.error = "Passwörter stimmen nicht überein"
                return
            }

            if (!verifyOldPassword(profilverwaltenViewModel.userData.value?.password ?: "", oldPassword)) {
                binding.oldPasswordInput.error = "Altes Passwort ist falsch"
                return
            }
        }

        val hashedPassword = if (password.isNotBlank()) hashPassword(password) else profilverwaltenViewModel.userData.value?.password

        val updatedUser = hashedPassword?.let {
            profilverwaltenViewModel.userData.value?.copy(
                nickname = nickname,
                email = email,
                password = it
            )
        } ?: return

        lifecycleScope.launch {
            try {
                if (selectedImageUri != null) {
                    val storageReference = FirebaseStorage.getInstance().reference
                    val fileReference = storageReference.child("User/${UUID.randomUUID()}.jpg")

                    // Lösche das alte Bild, falls vorhanden
                    updatedUser.profileImageUrl?.let { oldImageUrl ->
                        if (oldImageUrl.isNotEmpty()) {
                            FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl).delete().await()
                        }
                    }

                    // Lade das neue Bild hoch
                    fileReference.putFile(selectedImageUri!!).await()
                    val downloadUrl = fileReference.downloadUrl.await().toString()

                    profilverwaltenViewModel.updateUserData(updatedUser.copy(profileImageUrl = downloadUrl), null)
                } else {
                    profilverwaltenViewModel.updateUserData(updatedUser, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun verifyOldPassword(storedPassword: String, enteredPassword: String): Boolean {
        val hashedEnteredPassword = hashPassword(enteredPassword)
        return storedPassword == hashedEnteredPassword
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1001
    }
}
