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

/**
 * Fragment für die **monatliche** Übersicht der Einnahmen (istEinnahme = true).
 * - Beobachtet allExpenses, um bei jeder Änderung sofort den Monat neu zu filtern.
 * - onResume: lädt den Monat erneut.
 * - Klick auf Pfeile => Monat +/- => neu laden.
 */
class EinnahmenÜbersichtFragment : Fragment() {

    private var _binding: FragmentEinnahmenUebersichtBinding? = null
    private val binding get() = _binding!!
    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()

    // Speichert den aktuell ausgewählten Monat/Jahr
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEinnahmenUebersichtBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Wird aufgerufen, wenn die View erstellt wurde.
     * Hier registrieren wir Observer und richten UI-Elemente ein.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    /**
     * Wenn das Fragment erneut sichtbar wird (Tab-Wechsel oder Ähnliches),
     * laden wir zur Sicherheit erneut die Daten des Monats.
     */
    override fun onResume() {
        super.onResume()
        loadCurrentMonth()
    }

    private fun initView() {
        // Beim ersten Mal direkt den aktuellen Monat laden
        updateDateDisplay()

        // Beobachte allExpenses => sobald Firestore-Daten sich ändern,
        // rufen wir loadExpensesForMonth auf.
        haushaltsbuchViewModel.allExpenses.observe(viewLifecycleOwner) {
            loadCurrentMonth()
        }

        // selectedDateExpenses => filtern wir in diesem Fragment nur istEinnahme = true
        haushaltsbuchViewModel.selectedDateExpenses.observe(viewLifecycleOwner) { expenses ->
            val einnahmen = expenses.filter { it.istEinnahme }
            updateKontostand(einnahmen)
            updateDiagramAndPanel(einnahmen)
        }

        // Pfeile => Monat -1 / +1
        binding.leftArrow.setOnClickListener {
            changeDate(-1)
        }
        binding.rightArrow.setOnClickListener {
            changeDate(1)
        }

        // Klick auf Datum => DatePickerDialog nur Monat/Jahr
        binding.dateTextView.setOnClickListener {
            showMonthPickerDialog()
        }
    }

    /**
     * Aktualisiert die TextView, z. B. "März 2025", und lädt diesen Monat.
     */
    private fun updateDateDisplay() {
        binding.dateTextView.text = formatMonth(selectedDate)
        loadCurrentMonth()
    }

    /**
     * Ändert den Monat um +/- 1 und ruft updateDateDisplay auf.
     */
    private fun changeDate(monthDiff: Int) {
        selectedDate.add(Calendar.MONTH, monthDiff)
        updateDateDisplay()
    }

    /**
     * Öffnet einen DatePickerDialog, bei dem nur Monat/Jahr gewählt wird (Tag ignorieren).
     */
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
     * Lädt die Daten für den aktuell in selectedDate gespeicherten Monat.
     */
    private fun loadCurrentMonth() {
        val monthYearFormat = formatMonthForQuery(selectedDate)
        haushaltsbuchViewModel.loadExpensesForMonth(monthYearFormat)
    }

    /**
     * Berechnet die Summe der Einnahmen im Monat und zeigt sie oben an.
     */
    private fun updateKontostand(einnahmen: List<Expense>) {
        val totalIncome = einnahmen.sumOf { it.betrag.toDouble() }.toFloat()
        binding.textViewKontostand.text = "Einnahmen: %.2f EUR".format(totalIncome)
    }

    /**
     * Aktualisiert das Diagramm und die Kategorien-Liste (Panel).
     */
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
                    } else null
                }
            }

            if (isAdded && _binding != null) {
                // PieChart befüllen
                binding.pieChartView.setData(
                    categoryDataList.map { it.category },
                    categoryDataList.map { it.amount },
                    categoryDataList.map { it.color }
                )

                // Panel leeren
                binding.categoriesListPanel.removeAllViews()

                // Neue Einträge hinzufügen
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
     * Formatiert den Monat als "MMMM yyyy" für die Anzeige, z. B. "März 2025".
     */
    private fun formatMonth(cal: Calendar): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }

    /**
     * Formatiert den Monat als "MM.yyyy" (z. B. "03.2025") für loadExpensesForMonth.
     */
    private fun formatMonthForQuery(cal: Calendar): String {
        val sdf = SimpleDateFormat("MM.yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Hilfsklasse für die Diagrammdaten.
     */
    data class CategoryData(
        val category: String,
        val amount: Double,
        val percentage: Float,
        val color: Int
    )
}
