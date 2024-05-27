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

    private var _binding: FragmentEinkaufslisteBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val einkaufslisteViewModel =
            ViewModelProvider(this).get(EinkaufslisteViewModel::class.java)

        _binding = FragmentEinkaufslisteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSlideshow
        einkaufslisteViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}