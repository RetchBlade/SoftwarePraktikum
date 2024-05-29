package com.serenitysystems.livable.ui.login

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.serenitysystems.livable.databinding.FragmentLoginBinding
import com.serenitysystems.livable.R

class LoginFragment : Fragment() {

    // Späte Initialisierung des LoginViewModel
    private lateinit var loginViewModel: LoginViewModel
    // Binding-Objekt für das Login-Fragment, nur zwischen onCreateView und onDestroyView gültig
    private var _binding: FragmentLoginBinding? = null

    // Dieses Property ist nur zwischen onCreateView und onDestroyView gültig
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,  // LayoutInflater zum Aufblasen von Layouts
        container: ViewGroup?,    // Container, in den das Fragment eingefügt wird
        savedInstanceState: Bundle? // gespeicherter Zustand
    ): View? {
        // Aufblasen des Layouts für dieses Fragment und Setzen des Binding-Objekts
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root // Zurückgeben der Wurzelansicht
    }

    // Aufräumen des Binding-Objekts, um Speicherlecks zu vermeiden
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
