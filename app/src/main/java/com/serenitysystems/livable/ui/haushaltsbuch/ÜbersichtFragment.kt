package com.serenitysystems.livable.ui.haushaltsbuch

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentUebersichtBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ÜbersichtFragment : Fragment() {

    private var _binding: FragmentUebersichtBinding? = null
    private val binding get() = _binding!!
    private lateinit var haushaltsbuchViewModel: HaushaltsbuchViewModel
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        haushaltsbuchViewModel = ViewModelProvider(requireActivity()).get(HaushaltsbuchViewModel::class.java)

        _binding = FragmentUebersichtBinding.inflate(inflater, container, false)
        val root: View = binding.root

        haushaltsbuchViewModel.kontostand.observe(viewLifecycleOwner) { kontostand ->
            binding.textViewKontostand.text = "Kontostand: ${"%.2f".format(kontostand)} EUR"
        }

        binding.leftArrow.setOnClickListener {
            changeDate(-1)
        }

        binding.rightArrow.setOnClickListener {
            changeDate(1)
        }

        updateDateDisplay()
        haushaltsbuchViewModel.loadExpensesForDate(selectedDate)

        haushaltsbuchViewModel.allExpenses.observe(viewLifecycleOwner) {
            updateDiagramAndPanel()
        }

        return root
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        binding.dateTextView.text = dateFormat.format(selectedDate.time)
    }

    private fun changeDate(days: Int) {
        selectedDate.add(Calendar.DAY_OF_MONTH, days)
        updateDateDisplay()
        haushaltsbuchViewModel.loadExpensesForDate(selectedDate)
    }

    private fun updateDiagramAndPanel() {
        val categories = haushaltsbuchViewModel.getCategories()
        val colors = categories.map { category -> Color.parseColor(haushaltsbuchViewModel.getCategoryColor(category)) }
        val percentages = categories.map { category -> haushaltsbuchViewModel.getCategoryPercentage(category) }

        binding.pieChartView.setData(categories, percentages.map { it / 100 }, colors)

        binding.categoriesListPanel.removeAllViews()
        for (i in categories.indices) {
            if (percentages[i] > 0) {
                val containerView = LayoutInflater.from(context).inflate(R.layout.item_category_percentage, binding.categoriesListPanel, false)
                val categoryTextView = containerView.findViewById<TextView>(R.id.textViewCategory)
                categoryTextView.text = "${categories[i]}: ${"%.2f".format(percentages[i])}%"
                binding.categoriesListPanel.addView(containerView)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
