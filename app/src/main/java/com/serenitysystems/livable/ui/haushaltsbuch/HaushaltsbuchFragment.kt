package com.serenitysystems.livable.ui.haushaltsbuch

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.databinding.FragmentHaushaltsbuchBinding

class HaushaltsbuchFragment : Fragment() {

    private var _binding: FragmentHaushaltsbuchBinding? = null
    private val binding get() = _binding!!
    private lateinit var haushaltsbuchViewModel: HaushaltsbuchViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        haushaltsbuchViewModel =
            ViewModelProvider(this).get(HaushaltsbuchViewModel::class.java)

        _binding = FragmentHaushaltsbuchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textMonat
        haushaltsbuchViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
