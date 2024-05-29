package com.serenitysystems.livable.ui.einkaufsliste

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.databinding.FragmentEinkaufslisteBinding

class EinkaufslisteFragment : Fragment() {

    // Binding-Objekt, das nur zwischen onCreateView und onDestroyView gültig ist
    private var _binding: FragmentEinkaufslisteBinding? = null

    // Dieses Property ist nur zwischen onCreateView und onDestroyView gültig
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,  // LayoutInflater zum Aufblasen von Layouts
        container: ViewGroup?,    // Container, in den das Fragment eingefügt wird
        savedInstanceState: Bundle? // gespeicherter Zustand
    ): View {
        // Initialisieren des ViewModels
        val einkaufslisteViewModel =
            ViewModelProvider(this).get(EinkaufslisteViewModel::class.java)

        // Aufblasen des Layouts für dieses Fragment
        _binding = FragmentEinkaufslisteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Finden der TextView und Beobachten der LiveData des ViewModels
        val textView: TextView = binding.textSlideshow
        einkaufslisteViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it // Aktualisieren des Textes in der TextView, wenn sich die LiveData ändert
        }
        return root // Zurückgeben der Wurzelansicht
    }

    // Aufräumen des Binding-Objekts, um Speicherlecks zu vermeiden
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
