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
 * Fragment für die monatliche Übersicht der Ausgaben (istEinnahme = false).
 * - Zeigt eine Tortengrafik (PieChart) zur Verteilung der Kategorien.
 * - Berechnet eine monatliche Gesamtsumme.
 * - Ermöglicht das Wechseln des Monats per Pfeil-Buttons oder DatePickerDialog.
 */
class AusgabenÜbersichtFragment : Fragment() {

    // Binding-Objekt für das Layout fragment_ausgaben_uebersicht.xml
    private var _binding: FragmentAusgabenUebersichtBinding? = null
    private val binding get() = _binding!!

    // ViewModel enthält alle Funktionen zum Laden der Daten aus Firestore
    private val haushaltsbuchViewModel: HaushaltsbuchViewModel by activityViewModels()

    // In diesem Calendar-Objekt wird das aktuell ausgewählte Monat/Jahr gespeichert
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAusgabenUebersichtBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Nachdem die View erstellt wurde, initialisieren wir UI-Elemente und LiveData-Observer.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    /**
     * Initialisiert das Fragment, setzt Listener und Observer.
     */
    private fun initView() {
        // 1) Monat/Jahr in der TextView anzeigen (z. B. "März 2025")
        updateDateDisplay()

        // 2) Beim ersten Aufruf: Lade gleich den aktuellen Monat
        haushaltsbuchViewModel.loadExpensesForMonth(formatMonth(selectedDate))

        // 3) Beobachte allExpenses, damit alle Änderungen direkt übernommen werden
        haushaltsbuchViewModel.allExpenses.observe(viewLifecycleOwner) {
            // Monatliche Filterung erneut aufrufen
            haushaltsbuchViewModel.loadExpensesForMonth(formatMonth(selectedDate))
        }

        // 4) Beobachte die bereits gefilterten Daten (selectedDateExpenses),
        //    jedoch filtern wir hier zusätzlich nur Ausgaben (istEinnahme = false).
        haushaltsbuchViewModel.selectedDateExpenses.observe(viewLifecycleOwner) { expenses ->
            val ausgaben = expenses.filter { !it.istEinnahme }
            updateKontostand(ausgaben)
            updateDiagramAndPanel(ausgaben)
        }

        // 5) Navigationspfeile für das Wechseln des Monats
        binding.leftArrow.setOnClickListener {
            changeMonth(-1) // Einen Monat zurück
        }
        binding.rightArrow.setOnClickListener {
            changeMonth(1)  // Einen Monat vor
        }

        // 6) Klick auf das Datum => öffnet DatePickerDialog für Monat/Jahr
        binding.dateTextView.setOnClickListener {
            showMonthPickerDialog()
        }
    }

    /**
     * Berechnet die Summe aller Ausgaben in der Liste und zeigt sie im TextView "textViewKontostand" an.
     */
    private fun updateKontostand(ausgaben: List<Expense>) {
        val totalExpense = ausgaben.sumOf { it.betrag.toDouble() }.toFloat()
        binding.textViewKontostand.text = "Ausgaben im Monat: %.2f EUR".format(totalExpense)
    }

    /**
     * Aktualisiert das Tortendiagramm (PieChart) und die Kategorienliste (Panel).
     */
    private fun updateDiagramAndPanel(ausgaben: List<Expense>) {
        // Kategoriedaten werden in einem Hintergrund-Thread berechnet (Dispatchers.Default),
        // um die Haupt-UI nicht zu blockieren.
        viewLifecycleOwner.lifecycleScope.launch {
            val categoryDataList = withContext(Dispatchers.Default) {
                val categories = haushaltsbuchViewModel.categories
                categories.mapNotNull { category ->
                    val amount = ausgaben.filter { it.kategorie == category }.sumOf { it.betrag.toDouble() }
                    if (amount > 0) {
                        val totalAmount = ausgaben.sumOf { it.betrag.toDouble() }
                        val percentage = (amount / totalAmount * 100).toFloat()
                        val color = haushaltsbuchViewModel.getCategoryColor(category)
                        CategoryData(category, amount, percentage, color)
                    } else null
                }
            }

            // Sicherstellen, dass das Fragment noch "angebunden" ist
            if (isAdded && _binding != null) {
                // PieChart-Daten setzen
                binding.pieChartView.setData(
                    categoryDataList.map { it.category },
                    categoryDataList.map { it.amount },
                    categoryDataList.map { it.color }
                )

                // Kategorien-Panel leeren
                binding.categoriesListPanel.removeAllViews()

                // Neue Einträge für jede Kategorie erstellen
                val inflater = LayoutInflater.from(requireContext())
                categoryDataList.forEach { data ->
                    val itemBinding = ItemCategoryDetailBinding.inflate(inflater, binding.categoriesListPanel, false)

                    // Anfangsbuchstabe, z. B. "H" für "Haushalt"
                    itemBinding.textViewCategoryLetter.text = data.category.first().toString().uppercase()
                    itemBinding.textViewCategoryName.text = data.category
                    itemBinding.textViewCategoryAmount.text = "%.2f EUR".format(data.amount)
                    itemBinding.textViewCategoryPercentage.text = "%.0f%%".format(data.percentage)

                    // Hintergrundfarbe für den Buchstaben (z. B. Orange, Grün etc.)
                    val drawable = itemBinding.textViewCategoryLetter.background as? GradientDrawable
                    drawable?.setColor(data.color)

                    // Füge das Item dem Panel hinzu
                    binding.categoriesListPanel.addView(itemBinding.root)
                }

                // ScrollView ein-/ausblenden je nachdem, ob Kategorien vorhanden sind
                binding.categoriesScrollView.visibility =
                    if (categoryDataList.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * Ändert den Monat um +1 oder -1 und lädt anschließend die Daten neu.
     */
    private fun changeMonth(monthDiff: Int) {
        selectedDate.add(Calendar.MONTH, monthDiff)
        updateDateDisplay()
        haushaltsbuchViewModel.loadExpensesForMonth(formatMonth(selectedDate))
    }

    /**
     * Aktualisiert das Datumstextfeld, z. B. "März 2025".
     */
    private fun updateDateDisplay() {
        binding.dateTextView.text = formatMonth(selectedDate)
    }

    /**
     * Öffnet einen DatePickerDialog, bei dem nur Monat und Jahr gewählt werden.
     * Der Tag (dayOfMonth) ist hier nicht relevant.
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
     * Wandelt das Calendar-Datum in "MMMM yyyy" um, z. B. "März 2025".
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
     * Datenklasse für die Darstellung pro Kategorie im PieChart.
     */
    data class CategoryData(
        val category: String,
        val amount: Double,
        val percentage: Float,
        val color: Int
    )
}
