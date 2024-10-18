package com.serenitysystems.livable.ui.register

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.EditText
import android.widget.Button
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.serenitysystems.livable.R
import com.serenitysystems.livable.ui.login.LoginActivity
import com.serenitysystems.livable.ui.register.data.User
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.*

class RegistrierungFragment : Fragment() {

    // Initialisierung des ViewModels für die Registrierung
    private val viewModel: RegistrationViewModel by viewModels()

    // Deklaration der UI-Elemente
    private lateinit var editTextBirthdate: TextView
    private lateinit var editTextEmail: EditText
    private lateinit var editTextNickname: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextPasswordConfirm: EditText
    private lateinit var radioGroupGender: RadioGroup
    private lateinit var signUpButton: Button
    private lateinit var backtologin : TextView

    // Methode, die aufgerufen wird, wenn das Fragment erstellt wird
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflating des Layouts für dieses Fragment
        val view = inflater.inflate(R.layout.fragment_registrierung, container, false)

        // Initialisierung der UI-Elemente
        editTextBirthdate = view.findViewById(R.id.editTextBirthdate)
        // Setzt einen OnClickListener, um das Datumsauswahl-Dialogfeld anzuzeigen
        editTextBirthdate.setOnClickListener { showDatePickerDialog() }

        backtologin = view.findViewById(R.id.Login)

        editTextEmail = view.findViewById(R.id.usernameEditText)
        editTextNickname = view.findViewById(R.id.editTextNickname)
        editTextPassword = view.findViewById(R.id.editTextPassword)
        editTextPasswordConfirm = view.findViewById(R.id.PasswortBestätigung)
        radioGroupGender = view.findViewById(R.id.radioGroupGender)
        signUpButton = view.findViewById(R.id.signUpButton)

        // Setzt einen OnClickListener, um die Benutzerdaten zu sammeln und die Registrierung zu starten
        signUpButton.setOnClickListener { collectUserDataAndRegister() }
        backtologin.setOnClickListener {navigateToLoginActivity()}


        // Beobachtung des ViewModels
        observeViewModel()

        return view
    }

    // Methode zum Sammeln der Benutzerdaten und zum Starten der Registrierung
    private fun collectUserDataAndRegister() {
        val email = editTextEmail.text.toString()
        val nickname = editTextNickname.text.toString()
        val password = editTextPassword.text.toString()
        val passwordConfirm = editTextPasswordConfirm.text.toString()
        val birthdate = editTextBirthdate.text.toString()

        // Überprüfen, ob alle Felder ausgefüllt sind und die Passwörter übereinstimmen
        if (email.isNotEmpty() && nickname.isNotEmpty() && password.isNotEmpty() &&
            passwordConfirm.isNotEmpty() && birthdate.isNotEmpty() && password == passwordConfirm) {

            // Bestimmt das ausgewählte Geschlecht
            val selectedGenderId = radioGroupGender.checkedRadioButtonId
            val gender = if (selectedGenderId == R.id.radioMale) "Männlich" else "Weiblich"

            val hashedPassword = hashPassword(password)
            // Erstellt ein User-Objekt mit den eingegebenen Daten
            val user = User(email, nickname, hashedPassword, birthdate, gender)

            // Startet die Registrierung im Hintergrund
            lifecycleScope.launch {
                viewModel.registerUser(user)
            }
        } else {
            // Setzt Fehlermeldungen für leere oder ungültige Felder
            if (email.isEmpty()) {
                editTextEmail.error = "Email darf nicht leer sein"
            }
            if (nickname.isEmpty()) {
                editTextNickname.error = "Benutzername darf nicht leer sein"
            }
            if (password.isEmpty()) {
                editTextPassword.error = "Passwort darf nicht leer sein"
            }
            if (passwordConfirm.isEmpty()) {
                editTextPasswordConfirm.error = "Passwortbestätigung darf nicht leer sein"
            }
            if (birthdate.isEmpty()) {
                editTextBirthdate.error = "Geburtsdatum darf nicht leer sein"
            }
            if (password != passwordConfirm) {
                editTextPasswordConfirm.error = "Passwörter stimmen nicht überein"
            }
        }
    }

    // Methode zur Beobachtung des ViewModels
    private fun observeViewModel() {
        // Beobachtet den Registrierungserfolg
        viewModel.registrationSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                navigateToLoginActivity()
            }
        })

        // Beobachtet Registrierungsfehler
        viewModel.registrationError.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                when {
                    it.contains("email") -> editTextEmail.error = "Diese Email ist bereits vergeben"
                    else -> editTextEmail.error = "Ein unbekannter Fehler ist aufgetreten"
                }
            }
        })
    }

    // Methode zum Anzeigen des Datumsauswahl-Dialogfelds
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Erstellt und zeigt das Datumsauswahl-Dialogfeld an
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Setzt das ausgewählte Datum im TextView
                val selectedDate = "$selectedDay.${selectedMonth + 1}.$selectedYear"
                editTextBirthdate.text = selectedDate
            },
            year, month, day
        )

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