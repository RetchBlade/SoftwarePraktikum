package com.serenitysystems.livable.ui.haushaltsbuch.view

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.serenitysystems.livable.databinding.FragmentUebersichtBinding
import com.serenitysystems.livable.databinding.ItemCategoryDetailBinding
import com.serenitysystems.livable.ui.haushaltsbuch.viewmodel.HaushaltsbuchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ÜbersichtFragment : Fragment() {

    private var _binding: FragmentUebersichtBinding? = null
    private val binding get() = _binding!!
    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUebersichtBinding.inflate(inflater, container, false)

        // Beobachten des Kontostands
        haushaltsbuchViewModel.kontostand.observe(viewLifecycleOwner) { kontostand ->
            binding.textViewKontostand.text = "Kontostand: ${"%.2f".format(kontostand)} EUR"
        }

        // Beobachten der Ausgaben/Einnahmen für das ausgewählte Datum
        haushaltsbuchViewModel.selectedDateExpenses.observe(viewLifecycleOwner) {
            updateDiagramAndPanel()
        }

        // Pfeil-Buttons konfigurieren
        binding.leftArrow.setOnClickListener {
            haushaltsbuchViewModel.changeDateByDays(-1)
            updateDateDisplay()
        }
        binding.rightArrow.setOnClickListener {
            haushaltsbuchViewModel.changeDateByDays(1)
            updateDateDisplay()
        }

        updateDateDisplay()

        return binding.root
    }

    private fun updateDiagramAndPanel() {
        // Ausführung im Hintergrundthread
        CoroutineScope(Dispatchers.Default).launch {
            val categories = haushaltsbuchViewModel.categories

            // Erstellen der Liste von CategoryData
            val categoryDataList = categories.mapNotNull { category ->
                val amount = haushaltsbuchViewModel.getCategoryAmount(category)
                if (amount > 0f) {
                    val percentage = haushaltsbuchViewModel.getCategoryPercentage(category)
                    val color = haushaltsbuchViewModel.getCategoryColor(category)
                    CategoryData(
                        category = category,
                        amount = amount.toDouble(),
                        percentage = percentage,
                        color = color
                    )
                } else {
                    null
                }
            }

            Log.d("ÜbersichtFragment", "Anzahl der Kategorien: ${categoryDataList.size}")
            categoryDataList.forEach { data ->
                Log.d("ÜbersichtFragment", "Kategorie: ${data.category}, Betrag: ${data.amount}, Prozent: ${data.percentage}")
            }

            val categoriesForChart = categoryDataList.map { it.category }
            val valuesForChart = categoryDataList.map { it.amount }
            val colors = categoryDataList.map { it.color }

            withContext(Dispatchers.Main) {
                // Aktualisierung des PieChartView im Hauptthread
                binding.pieChartView.setData(categoriesForChart, valuesForChart, colors)

                // Entfernen aller bisherigen Views im Container
                binding.categoriesListPanel.removeAllViews()

                // Hinzufügen der neuen Items
                val inflater = LayoutInflater.from(requireContext())
                categoryDataList.forEach { data ->
                    val itemBinding = ItemCategoryDetailBinding.inflate(inflater, binding.categoriesListPanel, false)

                    itemBinding.textViewCategoryLetter.text = data.category.first().toString().uppercase()
                    itemBinding.textViewCategoryName.text = data.category
                    itemBinding.textViewCategoryAmount.text = "${"%.2f".format(data.amount)} EUR"
                    itemBinding.textViewCategoryPercentage.text = "${"%.0f".format(data.percentage)}%"

                    // Hintergrundfarbe des Kreises anpassen
                    val drawable = itemBinding.textViewCategoryLetter.background as? GradientDrawable
                    drawable?.setColor(data.color)

                    // Hinzufügen des Items zum Container
                    binding.categoriesListPanel.addView(itemBinding.root)
                }
            }
        }
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val selectedDateStr = dateFormat.format(haushaltsbuchViewModel.selectedDate.time)
        binding.dateTextView.text = selectedDateStr
        haushaltsbuchViewModel.loadExpensesForDate(selectedDateStr)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Datenklasse für CategoryData
    data class CategoryData(
        val category: String,
        val amount: Double,
        val percentage: Float,
        val color: Int
    )
}
