package com.serenitysystems.livable.ui.haushaltsbuch

import android.app.DatePickerDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.serenitysystems.livable.databinding.FragmentEinnahmenUebersichtBinding
import com.serenitysystems.livable.databinding.ItemCategoryDetailBinding
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EinnahmenÜbersichtFragment : Fragment() {

    private var _binding: FragmentEinnahmenUebersichtBinding? = null
    private val binding get() = _binding!!
    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()

    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEinnahmenUebersichtBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    private fun initView() {
        updateDateDisplay()
        haushaltsbuchViewModel.loadExpensesForDate(formatDate(selectedDate))

        binding.leftArrow.setOnClickListener {
            changeDate(-1)
        }

        binding.rightArrow.setOnClickListener {
            changeDate(1)
        }

        binding.dateTextView.setOnClickListener {
            showDatePickerDialog()
        }

        haushaltsbuchViewModel.selectedDateExpenses.observe(viewLifecycleOwner) { expenses ->
            val einnahmen = expenses.filter { it.istEinnahme }
            updateKontostand(einnahmen)
            updateDiagramAndPanel(einnahmen)
        }
    }

    private fun updateKontostand(einnahmen: List<Expense>) {
        val totalIncome = einnahmen.sumOf { it.betrag.toDouble() }.toFloat()
        binding.textViewKontostand.text = "Einnahmen: ${"%.2f".format(totalIncome)} EUR"
    }

    private fun updateDiagramAndPanel(einnahmen: List<Expense>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val categoryDataList = withContext(Dispatchers.Default) {
                val categories = haushaltsbuchViewModel.categories
                categories.mapNotNull { category ->
                    val amount = einnahmen.filter { it.kategorie == category }.sumOf { it.betrag.toDouble() }
                    if (amount > 0.0) {
                        val totalAmount = einnahmen.sumOf { it.betrag.toDouble() }
                        val percentage = (amount / totalAmount * 100).toFloat()
                        val color = haushaltsbuchViewModel.getCategoryColor(category)
                        CategoryData(
                            category = category,
                            amount = amount,
                            percentage = percentage,
                            color = color
                        )
                    } else {
                        null
                    }
                }
            }

            // Überprüfen, ob die View noch existiert
            if (isAdded && _binding != null) {
                // Aktualisiere die PieChartView
                binding.pieChartView.setData(
                    categoryDataList.map { it.category },
                    categoryDataList.map { it.amount },
                    categoryDataList.map { it.color }
                )

                // Entferne bestehende Elemente im categoriesListPanel
                binding.categoriesListPanel.removeAllViews()

                // Füge neue Elemente hinzu
                val inflater = LayoutInflater.from(requireContext())
                categoryDataList.forEach { data ->
                    val itemBinding = ItemCategoryDetailBinding.inflate(inflater, binding.categoriesListPanel, false)

                    itemBinding.textViewCategoryLetter.text = data.category.first().toString().uppercase()
                    itemBinding.textViewCategoryName.text = data.category
                    itemBinding.textViewCategoryAmount.text = "${"%.2f".format(data.amount)} EUR"
                    itemBinding.textViewCategoryPercentage.text = "${"%.0f".format(data.percentage)}%"

                    // Setze die Hintergrundfarbe
                    val drawable = itemBinding.textViewCategoryLetter.background as? GradientDrawable
                    drawable?.setColor(data.color)

                    // Füge das Element dem Panel hinzu
                    binding.categoriesListPanel.addView(itemBinding.root)
                }

                // Kontrolliere die Sichtbarkeit des Panels
                binding.categoriesScrollView.visibility = if (categoryDataList.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun changeDate(days: Int) {
        selectedDate.add(Calendar.DAY_OF_MONTH, days)
        updateDateDisplay()
        haushaltsbuchViewModel.loadExpensesForDate(formatDate(selectedDate))
    }

    private fun updateDateDisplay() {
        binding.dateTextView.text = formatDate(selectedDate)
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, monthOfYear, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, monthOfYear)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateDisplay()
                haushaltsbuchViewModel.loadExpensesForDate(formatDate(selectedDate))
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun formatDate(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class CategoryData(
        val category: String,
        val amount: Double,
        val percentage: Float,
        val color: Int
    )
}
