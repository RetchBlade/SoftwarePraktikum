import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.storage.FirebaseStorage
import com.serenitysystems.livable.R
import com.serenitysystems.livable.ui.login.LoginActivity
import com.serenitysystems.livable.ui.register.RegistrierungViewModel
import com.serenitysystems.livable.ui.register.data.User
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.*

class RegistrierungFragment : Fragment() {

    private val viewModel: RegistrierungViewModel by viewModels()

    // UI-Elemente deklarieren
    private lateinit var editTextBirthdate: TextView
    private lateinit var editTextEmail: EditText
    private lateinit var editTextNickname: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextPasswordConfirm: EditText
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var imageView : ImageView
    private lateinit var signUpButton: Button
    private lateinit var backtologin: TextView
    private lateinit var uploadButton: ImageButton
    private var imageUri: Uri? = null
    private val storageReference = FirebaseStorage.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registrierung, container, false)

        // UI-Elemente initialisieren
        editTextBirthdate = view.findViewById(R.id.editTextBirthdate)
        editTextBirthdate.setOnClickListener { showDatePickerDialog() }

        backtologin = view.findViewById(R.id.Login)
        uploadButton = view.findViewById(R.id.uploadButton)

        editTextEmail = view.findViewById(R.id.usernameEditText)
        editTextNickname = view.findViewById(R.id.editTextNickname)
        editTextPassword = view.findViewById(R.id.editTextPassword)
        editTextPasswordConfirm = view.findViewById(R.id.PasswortBestätigung)
        radioGroupGender = view.findViewById(R.id.radioGroupGender)
        signUpButton = view.findViewById(R.id.signUpButton)
        imageView = view.findViewById(R.id.imageView)

        uploadButton.setOnClickListener {
            openGalleryForImage()
        }

        signUpButton.setOnClickListener { collectUserDataAndRegister() }
        backtologin.setOnClickListener { navigateToLoginActivity() }

        // Beobachte ViewModel
        observeViewModel()

        return view
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Bild auswählen"), 1000) // Image request code
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            // Bild in der ImageView anzeigen (optional)
            view?.findViewById<ImageView>(R.id.imageView)?.setImageURI(imageUri)
        }
    }

    private fun collectUserDataAndRegister() {
        val email = editTextEmail.text.toString()
        val nickname = editTextNickname.text.toString()
        val password = editTextPassword.text.toString()
        val passwordConfirm = editTextPasswordConfirm.text.toString()
        val birthdate = editTextBirthdate.text.toString()
        val profileImageUrl = imageView.toString()
        if (email.isNotEmpty() && nickname.isNotEmpty() && password.isNotEmpty() &&
            passwordConfirm.isNotEmpty() && birthdate.isNotEmpty() && password == passwordConfirm
        ) {

            val selectedGenderId = radioGroupGender.checkedRadioButtonId
            val gender = if (selectedGenderId == R.id.radioMale) "Männlich" else "Weiblich"
            val hashedPassword = hashPassword(password)

            // Wenn ein Bild vorhanden ist, lade es hoch
            if (imageUri != null) {
                uploadImageToFirebase(imageUri!!) { imageUrl ->
                    val user = User(email, nickname, hashedPassword, birthdate, gender, "", "", imageUrl)
                    lifecycleScope.launch {
                        viewModel.registerUser(user)
                    }
                }
            } else {
                // Registrierung ohne Bild
                val user = User(email, nickname, hashedPassword, birthdate, gender)
                lifecycleScope.launch {
                    viewModel.registerUser(user)
                }
            }
        } else {
            // Fehlermeldungen für leere oder ungültige Felder setzen
            if (email.isEmpty()) editTextEmail.error = "Email darf nicht leer sein"
            if (nickname.isEmpty()) editTextNickname.error = "Benutzername darf nicht leer sein"
            if (password.isEmpty()) editTextPassword.error = "Passwort darf nicht leer sein"
            if (passwordConfirm.isEmpty()) editTextPasswordConfirm.error = "Passwortbestätigung darf nicht leer sein"
            if (birthdate.isEmpty()) editTextBirthdate.error = "Geburtsdatum darf nicht leer sein"
            if (password != passwordConfirm) editTextPasswordConfirm.error = "Passwörter stimmen nicht überein"
        }
    }

    // ViewModel-Beobachtung
    private fun observeViewModel() {
        viewModel.registrationSuccess.observe(viewLifecycleOwner, androidx.lifecycle.Observer { success ->
            if (success) {
                navigateToLoginActivity()
            }
        })

        viewModel.registrationError.observe(viewLifecycleOwner, androidx.lifecycle.Observer { error ->
            error?.let {
                if (it.contains("email")) {
                    editTextEmail.error = "Diese Email ist bereits vergeben"
                } else {
                    Toast.makeText(requireContext(), "Ein unbekannter Fehler ist aufgetreten", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun uploadImageToFirebase(imageUri: Uri, onSuccess: (String) -> Unit) {
        Log.d("Bildupload", "Starting image upload") // Logcat message

        val fileReference = storageReference.child("User/${UUID.randomUUID()}.jpg")
        fileReference.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                Log.d("Bildupload", "Image upload successful") // Logcat message
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString()) // Gibt die Bild-URL zurück
                    signUpButton.isEnabled = true // Re-enable the button
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Bildupload", "Image upload failed: ${exception.message}", exception) // Logcat error message
                signUpButton.isEnabled = true // Re-enable the button
            }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay.${selectedMonth + 1}.$selectedYear"
                editTextBirthdate.text = selectedDate
            }, year, month, day)

        datePickerDialog.show()
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
