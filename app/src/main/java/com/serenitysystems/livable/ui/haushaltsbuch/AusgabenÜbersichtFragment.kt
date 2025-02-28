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
import com.serenitysystems.livable.databinding.FragmentAusgabenUebersichtBinding
import com.serenitysystems.livable.databinding.ItemCategoryDetailBinding
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Fragment für die **monatliche** Übersicht der Ausgaben (istEinnahme = false).
 * - Aktualisiert sich sofort bei Änderungen in allExpenses,
 * - onResume => neu laden des Monats,
 * - Pfeile links/rechts => Monat +/- => sofortiger Reload.
 */
class AusgabenÜbersichtFragment : Fragment() {

    private var _binding: FragmentAusgabenUebersichtBinding? = null
    private val binding get() = _binding!!
    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()

    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAusgabenUebersichtBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Nach Erzeugung der View.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    /**
     * Wenn das Fragment wieder sichtbar wird, aktualisieren wir den aktuellen Monat.
     */
    override fun onResume() {
        super.onResume()
        loadCurrentMonth()
    }

    private fun initView() {
        // Direkt den aktuellen Monat anzeigen
        updateDateDisplay()

        // allExpenses => wenn neue Firestore-Daten => neu filtern
        haushaltsbuchViewModel.allExpenses.observe(viewLifecycleOwner) {
            loadCurrentMonth()
        }

        // selectedDateExpenses => nur Ausgaben (istEinnahme = false)
        haushaltsbuchViewModel.selectedDateExpenses.observe(viewLifecycleOwner) { expenses ->
            val ausgaben = expenses.filter { !it.istEinnahme }
            updateKontostand(ausgaben)
            updateDiagramAndPanel(ausgaben)
        }

        // Pfeile -1 / +1 Monat
        binding.leftArrow.setOnClickListener {
            changeMonth(-1)
        }
        binding.rightArrow.setOnClickListener {
            changeMonth(1)
        }

        // Klick auf Datum => MonthPickerDialog
        binding.dateTextView.setOnClickListener {
            showMonthPickerDialog()
        }
    }

    /**
     * Zeigt "Ausgaben: XX.XX EUR" als Summe im Monat.
     */
    private fun updateKontostand(ausgaben: List<Expense>) {
        val totalExpense = ausgaben.sumOf { it.betrag.toDouble() }.toFloat()
        binding.textViewKontostand.text = "Ausgaben: %.2f EUR".format(totalExpense)
    }

    /**
     * Aktualisiert das Diagramm (PieChart) und das Kategorien-Panel.
     */
    private fun updateDiagramAndPanel(ausgaben: List<Expense>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val categoryDataList = withContext(Dispatchers.Default) {
                val categories = haushaltsbuchViewModel.categories
                categories.mapNotNull { cat ->
                    val amount = ausgaben.filter { it.kategorie == cat }.sumOf { it.betrag.toDouble() }
                    if (amount > 0) {
                        val totalAmount = ausgaben.sumOf { it.betrag.toDouble() }
                        val percentage = (amount / totalAmount * 100).toFloat()
                        val color = haushaltsbuchViewModel.getCategoryColor(cat)
                        CategoryData(
                            category = cat,
                            amount = amount,
                            percentage = percentage,
                            color = color
                        )
                    } else null
                }
            }

            if (isAdded && _binding != null) {
                // PieChart-Daten setzen
                binding.pieChartView.setData(
                    categoryDataList.map { it.category },
                    categoryDataList.map { it.amount },
                    categoryDataList.map { it.color }
                )

                // Panel leeren
                binding.categoriesListPanel.removeAllViews()

                // Neue Items hinzufügen
                val inflater = LayoutInflater.from(requireContext())
                categoryDataList.forEach { data ->
                    val itemBinding = ItemCategoryDetailBinding.inflate(inflater, binding.categoriesListPanel, false)

                    itemBinding.textViewCategoryLetter.text = data.category.first().toString().uppercase()
                    itemBinding.textViewCategoryName.text = data.category
                    itemBinding.textViewCategoryAmount.text = "%.2f EUR".format(data.amount)
                    itemBinding.textViewCategoryPercentage.text = "%.0f%%".format(data.percentage)

                    val drawable = itemBinding.textViewCategoryLetter.background as? GradientDrawable
                    drawable?.setColor(data.color)

                    binding.categoriesListPanel.addView(itemBinding.root)
                }

                binding.categoriesScrollView.visibility =
                    if (categoryDataList.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * Ändert den Monat um +/- 1
     */
    private fun changeMonth(monthDiff: Int) {
        selectedDate.add(Calendar.MONTH, monthDiff)
        updateDateDisplay()
    }

    /**
     * Datumstext aktualisieren und Daten für diesen Monat laden
     */
    private fun updateDateDisplay() {
        binding.dateTextView.text = formatMonth(selectedDate)
        loadCurrentMonth()
    }

    private fun showMonthPickerDialog() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val dialog = DatePickerDialog(
            requireContext(),
            { _, selYear, selMonth, _ ->
                selectedDate.set(Calendar.YEAR, selYear)
                selectedDate.set(Calendar.MONTH, selMonth)
                selectedDate.set(Calendar.DAY_OF_MONTH, 1)
                updateDateDisplay()
            },
            year,
            month,
            1
        )
        dialog.show()
    }

    /**
     * Lädt die Daten für den aktuellen Monat (selectedDate)
     */
    private fun loadCurrentMonth() {
        val queryFormat = formatMonthForQuery(selectedDate)
        haushaltsbuchViewModel.loadExpensesForMonth(queryFormat)
    }

    /**
     * "MMMM yyyy", z. B. "März 2025" .
     */
    private fun formatMonth(cal: Calendar): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.GERMAN)
        return sdf.format(cal.time)
    }

    /**
     * "MM.yyyy", z. B. "03.2025" .
     */
    private fun formatMonthForQuery(cal: Calendar): String {
        val sdf = SimpleDateFormat("MM.yyyy", Locale.GERMAN)
        return sdf.format(cal.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Hilfsklasse für die Kategorien im Diagramm.
     */
    data class CategoryData(
        val category: String,
        val amount: Double,
        val percentage: Float,
        val color: Int
    )
}
