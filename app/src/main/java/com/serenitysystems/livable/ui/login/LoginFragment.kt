package com.serenitysystems.livable.ui.login

import androidx.fragment.app.Fragment
import com.serenitysystems.livable.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    // Späte Initialisierung des LoginViewModel
    private lateinit var loginViewModel: LoginViewModel
    // Binding-Objekt für das Login-Fragment, nur zwischen onCreateView und onDestroyView gültig
    private var _binding: FragmentLoginBinding? = null

    // Dieses Property ist nur zwischen onCreateView und onDestroyView gültig
    private val binding get() = _binding!!

    // Aufräumen des Binding-Objekts, um Speicherlecks zu vermeiden
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
