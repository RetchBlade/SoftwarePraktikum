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
 * Fragment für die monatliche Übersicht der Einnahmen (istEinnahme = true).
 * - Zeigt eine Tortengrafik (PieChart) zur Verteilung der Kategorien.
 * - Berechnet eine monatliche Gesamtsumme der Einnahmen.
 * - Ermöglicht das Wechseln des Monats.
 */
class EinnahmenÜbersichtFragment : Fragment() {

    // Binding-Objekt für das Layout fragment_einnahmen_uebersicht.xml
    private var _binding: FragmentEinnahmenUebersichtBinding? = null
    private val binding get() = _binding!!

    // ViewModel für Firestore-Operationen und Datenhaltung
    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()

    // Aktuell ausgewähltes Monat/Jahr
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEinnahmenUebersichtBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Nachdem die View erstellt wurde, Observer registrieren und UI initialisieren.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    /**
     * Richtet das Fragment ein und setzt alle nötigen Listener.
     */
    private fun initView() {
        // 1) Aktuellen Monat/Jahr anzeigen
        updateDateDisplay()

        // 2) Direkt beim Start die Daten für diesen Monat laden
        haushaltsbuchViewModel.loadExpensesForMonth(formatMonth(selectedDate))

        // 3) allExpenses beobachten => neue/gelöschte/aktualisierte Datensätze sofort übernehmen
        haushaltsbuchViewModel.allExpenses.observe(viewLifecycleOwner) {
            haushaltsbuchViewModel.loadExpensesForMonth(formatMonth(selectedDate))
        }

        // 4) selectedDateExpenses beobachten => nur istEinnahme = true anzeigen
        haushaltsbuchViewModel.selectedDateExpenses.observe(viewLifecycleOwner) { expenses ->
            val einnahmen = expenses.filter { it.istEinnahme }
            updateKontostand(einnahmen)
            updateDiagramAndPanel(einnahmen)
        }

        // 5) Pfeile zur Monatsnavigation
        binding.leftArrow.setOnClickListener {
            changeMonth(-1)
        }
        binding.rightArrow.setOnClickListener {
            changeMonth(1)
        }

        // 6) Klick auf das Datum öffnet einen DatePickerDialog (Monat/Jahr)
        binding.dateTextView.setOnClickListener {
            showMonthPickerDialog()
        }
    }

    /**
     * Berechnet die Summe aller Einnahmen in der Liste und zeigt sie an.
     */
    private fun updateKontostand(einnahmen: List<Expense>) {
        val totalIncome = einnahmen.sumOf { it.betrag.toDouble() }.toFloat()
        binding.textViewKontostand.text = "Einnahmen im Monat: %.2f EUR".format(totalIncome)
    }

    /**
     * Aktualisiert das Tortendiagramm (PieChart) und die Kategorieliste.
     */
    private fun updateDiagramAndPanel(einnahmen: List<Expense>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val categoryDataList = withContext(Dispatchers.Default) {
                val categories = haushaltsbuchViewModel.categories
                categories.mapNotNull { category ->
                    val amount = einnahmen.filter { it.kategorie == category }.sumOf { it.betrag.toDouble() }
                    if (amount > 0) {
                        val totalAmount = einnahmen.sumOf { it.betrag.toDouble() }
                        val percentage = (amount / totalAmount * 100).toFloat()
                        val color = haushaltsbuchViewModel.getCategoryColor(category)
                        CategoryData(category, amount, percentage, color)
                    } else null
                }
            }

            // Nur updaten, wenn Fragment noch existiert
            if (isAdded && _binding != null) {
                // PieChart befüllen
                binding.pieChartView.setData(
                    categoryDataList.map { it.category },
                    categoryDataList.map { it.amount },
                    categoryDataList.map { it.color }
                )

                // Panel leeren
                binding.categoriesListPanel.removeAllViews()

                // Liste der Kategorien hinzufügen
                val inflater = LayoutInflater.from(requireContext())
                categoryDataList.forEach { data ->
                    val itemBinding = ItemCategoryDetailBinding.inflate(inflater, binding.categoriesListPanel, false)

                    // Anfangsbuchstabe
                    itemBinding.textViewCategoryLetter.text = data.category.first().toString().uppercase()
                    itemBinding.textViewCategoryName.text = data.category
                    itemBinding.textViewCategoryAmount.text = "%.2f EUR".format(data.amount)
                    itemBinding.textViewCategoryPercentage.text = "%.0f%%".format(data.percentage)

                    val drawable = itemBinding.textViewCategoryLetter.background as? GradientDrawable
                    drawable?.setColor(data.color)

                    binding.categoriesListPanel.addView(itemBinding.root)
                }

                // Sichtbarkeit ScrollView
                binding.categoriesScrollView.visibility =
                    if (categoryDataList.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * Verschiebt den Monat um +1 oder -1 und lädt anschließend neu.
     */
    private fun changeMonth(monthDiff: Int) {
        selectedDate.add(Calendar.MONTH, monthDiff)
        updateDateDisplay()
        haushaltsbuchViewModel.loadExpensesForMonth(formatMonth(selectedDate))
    }

    /**
     * Aktualisiert die dateTextView, z. B. "März 2025".
     */
    private fun updateDateDisplay() {
        binding.dateTextView.text = formatMonth(selectedDate)
    }

    /**
     * Zeigt einen DatePickerDialog an, in dem wir das gewünschte Monat/Jahr auswählen.
     * Der Tag (dayOfMonth) ist irrelevant.
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
                haushaltsbuchViewModel.loadExpensesForMonth(formatMonth(selectedDate))
            },
            year,
            month,
            1
        )
        dialog.show()
    }

    /**
     * Formatierung für Monat/Jahr, z. B. "MMMM yyyy" => "März 2025".
     */
    private fun formatMonth(calendar: Calendar): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Datenklasse für die Anzeige der Kategorien im PieChart.
     */
    data class CategoryData(
        val category: String,
        val amount: Double,
        val percentage: Float,
        val color: Int
    )
}
