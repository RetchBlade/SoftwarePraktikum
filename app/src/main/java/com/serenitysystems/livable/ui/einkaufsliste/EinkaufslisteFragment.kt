package com.serenitysystems.livable.ui.einkaufsliste

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentEinkaufslisteBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // ViewModel initialisieren
        viewModel = ViewModelProvider(this).get(EinkaufslisteViewModel::class.java)
        _binding = FragmentEinkaufslisteBinding.inflate(inflater, container, false)

        // RecyclerViews einrichten
        setupRecyclerViews()
        setupFab()
        setupDateSelector()

        // Datum im View aktualisieren
        updateDateInView()

        // Beobachte Änderungen im ViewModel
        observeViewModel()

        return binding.root
    }

    // Beobachte das ViewModel für Datenänderungen
    private fun observeViewModel() {
        viewModel.itemsByDate.observe(viewLifecycleOwner, Observer {
            loadItemsForDate()
        })
    }

    // RecyclerViews einrichten
    private fun setupRecyclerViews() {
        lebensmittelAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> handleItemClick(item) },
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) }
        )
        getrankeAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> handleItemClick(item) },
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) }
        )
        haushaltAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> handleItemClick(item) },
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) }
        )
        sonstigesAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> handleItemClick(item) },
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) }
        )

        binding.recyclerLebensmittel.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLebensmittel.adapter = lebensmittelAdapter

        binding.recyclerGetranke.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGetranke.adapter = getrankeAdapter

        binding.recyclerHaushalt.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHaushalt.adapter = haushaltAdapter

        binding.recyclerSonstiges.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSonstiges.adapter = sonstigesAdapter

        // SwipeHandler einrichten
        setupSwipeHandlers()
    }

    // Produktklick behandeln
    private fun handleItemClick(item: Produkt) {
        val today = dateFormat.format(Date())

        if (item.date == today) {
            AlertDialog.Builder(requireContext())
                .setTitle("Wurde das heute gekauft?")
                .setPositiveButton("Ja") { _, _ ->
                    // Produkt als gekauft markieren
                    item.statusIcon = R.drawable.ic_check
                    item.isChecked = true
                    val dateKey = dateFormat.format(selectedDate.time)
                    viewModel.updateItemStatus(dateKey, item, true)
                }
                .setNegativeButton("Nein") { _, _ ->
                    // Produkt zu einem neuen Datum verschieben
                    showDatePickerForItem(item)
                }
                .show()
        }
    }

    // Produkt zu einem neuen Datum verschieben
    private fun moveItemToNewDate(item: Produkt, neuesDatum: String) {
        val currentDateKey = dateFormat.format(selectedDate.time)

        // Statusicon und isChecked aktualisieren
        item.statusIcon = R.drawable.ic_warning
        item.isChecked = false

        viewModel.moveItemToNewDate(currentDateKey, neuesDatum, item)
    }

    // Datumsauswahl für ein Produkt anzeigen
    private fun showDatePickerForItem(item: Produkt) {
        val datePickerDialog = DatePickerDialog(
            requireContext(), { _, year, month, day ->
                val newDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    .format(GregorianCalendar(year, month, day).time)
                moveItemToNewDate(item, newDate)
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // FloatingActionButton einrichten
    private fun setupFab() {
        binding.fab.setOnClickListener {
            val dialog = AddItemDialogFragment()
            dialog.onAddItem = { newItem ->
                val dateKey = dateFormat.format(selectedDate.time)
                viewModel.addItem(dateKey, newItem)
                true
            }
            dialog.show(childFragmentManager, "AddItemDialog")
        }
    }

    // Datumsauswahl einrichten
    private fun setupDateSelector() {
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

    // Datum im View aktualisieren
    private fun updateDateInView() {
        binding.etSelectedDate.setText(dateFormat.format(selectedDate.time))
    }

    // Datumsauswahl Dialog anzeigen
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

    // Produkte für das ausgewählte Datum laden
    private fun loadItemsForDate() {
        val dateKey = dateFormat.format(selectedDate.time)
        val itemsForDate = viewModel.getItemsForDate(dateKey)

        lebensmittelAdapter.setItems(emptyList())
        getrankeAdapter.setItems(emptyList())
        haushaltAdapter.setItems(emptyList())
        sonstigesAdapter.setItems(emptyList())

        itemsForDate.forEach { item ->
            when (item.category) {
                "Lebensmittel" -> lebensmittelAdapter.addItem(item)
                "Getränke" -> getrankeAdapter.addItem(item)
                "Haushalt" -> haushaltAdapter.addItem(item)
                "Sonstiges" -> sonstigesAdapter.addItem(item)
            }
        }
    }

    // SwipeHandler einrichten
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
                    // Produkt als gelöscht markieren
                    adapter.markItemForDeletion(position)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    if (item.isChecked) {
                        // Gelöschtes Produkt wiederherstellen
                        showRestoreConfirmationDialog(position, adapter)
                    } else {
                        // Nichts tun, wenn das Produkt nicht gelöscht ist
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

                if (dX < 0) {
                    // Nach links wischen - Rot
                    paint.color = Color.RED
                    val background = RectF(
                        itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)
                } else if (dX > 0) {
                    // Nach rechts wischen - Grün
                    paint.color = Color.GREEN
                    val background = RectF(
                        itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerLebensmittel)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerGetranke)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerHaushalt)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerSonstiges)
    }

    // Bestätigungsdialog zum Wiederherstellen eines gelöschten Produkts
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
