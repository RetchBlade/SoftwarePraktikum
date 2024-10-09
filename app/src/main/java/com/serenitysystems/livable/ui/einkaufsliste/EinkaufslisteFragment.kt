package com.serenitysystems.livable.ui.einkaufsliste

import android.app.DatePickerDialog
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.DatePicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentEinkaufslisteBinding
import java.text.SimpleDateFormat
import java.util.*

class EinkaufslisteFragment : Fragment() {

    private var _binding: FragmentEinkaufslisteBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EinkaufslisteViewModel

    // Adapter für die verschiedenen Kategorien
    private lateinit var lebensmittelAdapter: EinkaufsItemAdapter
    private lateinit var getrankeAdapter: EinkaufsItemAdapter
    private lateinit var haushaltAdapter: EinkaufsItemAdapter
    private lateinit var sonstigesAdapter: EinkaufsItemAdapter

    // Aktuelles Datum
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private var selectedDate = Calendar.getInstance()

    // Datenhaltung für Artikel pro Datum
    private val itemsByDate = mutableMapOf<String, List<Produkt>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(EinkaufslisteViewModel::class.java)
        _binding = FragmentEinkaufslisteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerViews()
        setupFab()
        setupSwipeHandlers()
        setupDateSelector()
        loadItemsForDate()

        return root
    }

    // Einrichten der RecyclerViews für jede Kategorie
    private fun setupRecyclerViews() {
        lebensmittelAdapter = EinkaufsItemAdapter(mutableListOf()) { item -> }
        getrankeAdapter = EinkaufsItemAdapter(mutableListOf()) { item -> }
        haushaltAdapter = EinkaufsItemAdapter(mutableListOf()) { item -> }
        sonstigesAdapter = EinkaufsItemAdapter(mutableListOf()) { item -> }

        binding.recyclerLebensmittel.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLebensmittel.adapter = lebensmittelAdapter

        binding.recyclerGetranke.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGetranke.adapter = getrankeAdapter

        binding.recyclerHaushalt.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHaushalt.adapter = haushaltAdapter

        binding.recyclerSonstiges.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSonstiges.adapter = sonstigesAdapter
    }

    // Einrichten des Floating Action Buttons (FAB) zum Hinzufügen neuer Produkte
    private fun setupFab() {
        binding.fab.setOnClickListener {
            val dialog = AddItemDialogFragment()
            dialog.onAddItem = { newItem ->
                addItemToList(newItem)
                true
            }
            dialog.show(childFragmentManager, "AddItemDialog")
        }
    }

    // Fügt das neue Produkt zur entsprechenden Kategorie hinzu und aktualisiert die Anzeige
    private fun addItemToList(newItem: Produkt) {
        val itemDate = newItem.date ?: dateFormat.format(selectedDate.time)
        val dateKey = itemDate

        // Speichern des neuen Artikels
        val currentItems = itemsByDate[dateKey]?.toMutableList() ?: mutableListOf()
        currentItems.add(0, newItem)
        itemsByDate[dateKey] = currentItems

        // Falls der Artikel für das aktuell ausgewählte Datum ist, zur Liste hinzufügen
        if (dateKey == dateFormat.format(selectedDate.time)) {
            when (newItem.category) {
                "Lebensmittel" -> {
                    lebensmittelAdapter.addItem(newItem)
                    binding.recyclerLebensmittel.scrollToPosition(0)
                }
                "Getränke" -> {
                    getrankeAdapter.addItem(newItem)
                    binding.recyclerGetranke.scrollToPosition(0)
                }
                "Haushalt" -> {
                    haushaltAdapter.addItem(newItem)
                    binding.recyclerHaushalt.scrollToPosition(0)
                }
                "Sonstiges" -> {
                    sonstigesAdapter.addItem(newItem)
                    binding.recyclerSonstiges.scrollToPosition(0)
                }
                else -> {
                    sonstigesAdapter.addItem(newItem)
                    binding.recyclerSonstiges.scrollToPosition(0)
                }
            }
        }
    }

    // Einrichten der Swipe-Handler für jede Kategorie
    private fun setupSwipeHandlers() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val recyclerView = viewHolder.itemView.parent as RecyclerView
                val adapter = when (recyclerView.id) {
                    R.id.recycler_lebensmittel -> lebensmittelAdapter
                    R.id.recycler_getranke -> getrankeAdapter
                    R.id.recycler_haushalt -> haushaltAdapter
                    R.id.recycler_sonstiges -> sonstigesAdapter
                    else -> return
                }

                val position = viewHolder.adapterPosition
                val item = adapter.getItem(position)

                if (direction == ItemTouchHelper.LEFT) {
                    // Element als gelöscht markieren
                    adapter.markItemForDeletion(position)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    if (item.isChecked) {
                        // Gelöschtes Element wiederherstellen
                        showRestoreConfirmationDialog(position, adapter)
                    } else {
                        // Nicht gelöschtes Element kann nicht wiederhergestellt werden
                        adapter.notifyItemChanged(position)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float,
                dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()
                val background: RectF

                if (dX < 0) {
                    // Linkswisch - Rot
                    paint.color = Color.RED
                    background = RectF(
                        itemView.right + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat()
                    )
                } else if (dX > 0) {
                    // Rechtswisch - Grün
                    paint.color = Color.GREEN
                    background = RectF(
                        itemView.left.toFloat(),
                        itemView.top.toFloat(),
                        itemView.left + dX,
                        itemView.bottom.toFloat()
                    )
                } else {
                    // Kein Wischen - kein Hintergrund zeichnen
                    background = RectF(0f, 0f, 0f, 0f)
                }

                // Hintergrund zeichnen
                c.drawRect(background, paint)

                // Vordere Ansicht zeichnen
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerLebensmittel)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerGetranke)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerHaushalt)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerSonstiges)
    }

    // Datumsauswahl einrichten
    private fun setupDateSelector() {
        updateDateInView()

        binding.btnDatePrev.setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1)
            updateDateInView()
            loadItemsForDate()
        }

        binding.btnDateNext.setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1)
            updateDateInView()
            loadItemsForDate()
        }

        binding.etSelectedDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun updateDateInView() {
        binding.etSelectedDate.setText(dateFormat.format(selectedDate.time))
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
                loadItemsForDate()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // Lädt die Artikel für das ausgewählte Datum
    private fun loadItemsForDate() {
        val dateKey = dateFormat.format(selectedDate.time)
        val itemsForDate = itemsByDate[dateKey] ?: emptyList()

        // Listen leeren
        lebensmittelAdapter.setItems(emptyList())
        getrankeAdapter.setItems(emptyList())
        haushaltAdapter.setItems(emptyList())
        sonstigesAdapter.setItems(emptyList())

        // Artikel hinzufügen
        itemsForDate.forEach { item ->
            when (item.category) {
                "Lebensmittel" -> lebensmittelAdapter.addItem(item)
                "Getränke" -> getrankeAdapter.addItem(item)
                "Haushalt" -> haushaltAdapter.addItem(item)
                "Sonstiges" -> sonstigesAdapter.addItem(item)
            }
        }
    }

    // Zeigt einen Bestätigungsdialog an, um ein gelöschtes Produkt wiederherzustellen
    private fun showRestoreConfirmationDialog(position: Int, adapter: EinkaufsItemAdapter) {
        AlertDialog.Builder(requireContext())
            .setTitle("Bestätigung")
            .setMessage("Möchten Sie diesen Artikel wiederherstellen?")
            .setPositiveButton("Ja") { _, _ ->
                adapter.restoreItem(position)
            }
            .setNegativeButton("Nein") { dialog, _ ->
                dialog.dismiss()
                adapter.notifyItemChanged(position)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
