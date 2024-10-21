package com.serenitysystems.livable.ui.einkaufsliste

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentEinkaufslisteBinding
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.contract.ActivityResultContracts
import com.serenitysystems.livable.ui.einkaufsliste.adapter.EinkaufsItemAdapter
import com.serenitysystems.livable.ui.einkaufsliste.data.Produkt

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

    // Temporär ausgewähltes Produkt für Bildänderung
    private var selectedItemForImageChange: Produkt? = null
    private val selectedDateKey get() = dateFormat.format(selectedDate.time)

    // ActivityResultLauncher für die Bildauswahl
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && selectedItemForImageChange != null) {
            selectedItemForImageChange?.imageUri = uri.toString()
            viewModel.updateItemImage(selectedDateKey, selectedItemForImageChange!!)
            loadItemsForDate()  // Liste nach Bildänderung neu laden
        }
    }

    // Fragment-Ansicht erstellen
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
            loadItemsForDate()  // Lädt die Artikel für das ausgewählte Datum neu
        })
    }

    // RecyclerViews und Adapter für die verschiedenen Kategorien einrichten
    private fun setupRecyclerViews() {
        lebensmittelAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> handleItemClick(item) },   // Produktklick verarbeiten
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) },  // Datum ändern
            onImageClicked = { item -> handleImageClick(item) }, // Bildklick verarbeiten
            onItemDeleted = { item -> deleteItem(item) }         // Produkt löschen
        )
        getrankeAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> handleItemClick(item) },
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) },
            onImageClicked = { item -> handleImageClick(item) },
            onItemDeleted = { item -> deleteItem(item) }
        )
        haushaltAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> handleItemClick(item) },
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) },
            onImageClicked = { item -> handleImageClick(item) },
            onItemDeleted = { item -> deleteItem(item) }
        )
        sonstigesAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> handleItemClick(item) },
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) },
            onImageClicked = { item -> handleImageClick(item) },
            onItemDeleted = { item -> deleteItem(item) }
        )

        // RecyclerViews für die verschiedenen Kategorien verbinden
        binding.recyclerLebensmittel.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLebensmittel.adapter = lebensmittelAdapter

        binding.recyclerGetranke.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGetranke.adapter = getrankeAdapter

        binding.recyclerHaushalt.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHaushalt.adapter = haushaltAdapter

        binding.recyclerSonstiges.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSonstiges.adapter = sonstigesAdapter

        // Swipe-to-Delete Funktionalität einrichten
        setupSwipeHandlers()
    }

    // Funktion zur Behandlung des Klicks auf ein Produkt
    private fun handleItemClick(item: Produkt) {
        AlertDialog.Builder(requireContext())
            .setTitle("Aktion wählen")
            .setMessage("Möchten Sie das Produkt bearbeiten oder prüfen, ob es heute gekauft wurde?")
            .setPositiveButton("Bearbeiten") { _, _ ->
                // Produkt bearbeiten
                showEditItemDialog(item)
            }
            .setNegativeButton("Heute gekauft?") { _, _ ->
                checkIfPurchasedToday(item)  // Funktion zum Überprüfen, ob es heute gekauft wurde
            }
            .show()
    }

    // Dialog zum Bearbeiten eines Produkts anzeigen
    private fun showEditItemDialog(item: Produkt) {
        val dialog = AddItemDialogFragment()
        dialog.setCurrentItem(item)
        dialog.onAddItem = { updatedItem, oldItem ->
            val currentDateKey = dateFormat.format(selectedDate.time)

            // Altes Produkt entfernen, neues hinzufügen
            oldItem?.let { viewModel.deleteItem(currentDateKey, it) }
            viewModel.addItem(currentDateKey, updatedItem)
            loadItemsForDate()  // Liste nach dem Bearbeiten neu laden
            true
        }
        dialog.show(childFragmentManager, "EditItemDialog")
    }

    // Funktion für den "Heute gekauft?" Dialog
    private fun checkIfPurchasedToday(item: Produkt) {
        val today = dateFormat.format(Date())

        AlertDialog.Builder(requireContext())
            .setTitle("Wurde das heute gekauft?")
            .setPositiveButton("Ja") { _, _ ->
                // Produkt als gekauft markieren
                item.statusIcon = R.drawable.ic_check
                item.isChecked = true
                viewModel.updateItemStatus(today, item, true)  // Status des Produkts aktualisieren
                loadItemsForDate()  // Liste neu laden
            }
            .setNegativeButton("Nein") { _, _ ->
                // Produkt zu einem neuen Datum verschieben
                showDatePickerForItem(item)
            }
            .show()
    }

    // Funktion zum Verschieben des Produkts zu einem neuen Datum
    private fun moveItemToNewDate(item: Produkt, neuesDatum: String) {
        val currentDateKey = dateFormat.format(selectedDate.time)
        item.statusIcon = R.drawable.ic_warning  // Warnsymbol setzen
        item.isChecked = false  // Produkt als nicht erledigt markieren
        viewModel.moveItemToNewDate(currentDateKey, neuesDatum, item)
        loadItemsForDate()  // Liste neu laden
    }

    // Funktion zum Löschen eines Produkts
    private fun deleteItem(item: Produkt) {
        val currentDateKey = dateFormat.format(selectedDate.time)
        viewModel.deleteItem(currentDateKey, item)
        loadItemsForDate()  // Liste nach dem Löschen neu laden
    }

    // Funktion zum Verarbeiten des Bildklicks eines Produkts
    private fun handleImageClick(item: Produkt) {
        selectedItemForImageChange = item
        selectImage()  // Bildauswahl starten
    }

    // Bildauswahl für das Produkt
    private fun selectImage() {
        imagePickerLauncher.launch("image/*")
    }

    // Datumsauswahl für ein Produkt anzeigen
    private fun showDatePickerForItem(item: Produkt) {
        val datePickerDialog = DatePickerDialog(
            requireContext(), { _, year, month, day ->
                val newDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    .format(GregorianCalendar(year, month, day).time)
                moveItemToNewDate(item, newDate)  // Produkt zu einem neuen Datum verschieben
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // FloatingActionButton einrichten - Neues Produkt hinzufügen
    private fun setupFab() {
        binding.fab.setOnClickListener {
            val dialog = AddItemDialogFragment()
            dialog.onAddItem = { newItem, _ ->
                val dateKey = dateFormat.format(selectedDate.time)
                viewModel.addItem(dateKey, newItem)  // Neues Produkt zum ViewModel hinzufügen
                true
            }
            dialog.show(childFragmentManager, "AddItemDialog")
        }
    }

    // Datumsauswahl einrichten (vorher/nachher navigieren)
    private fun setupDateSelector() {
        binding.btnDatePrev.setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1)
            updateDateInView()
            loadItemsForDate()  // Liste neu laden
        }

        binding.btnDateNext.setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1)
            updateDateInView()
            loadItemsForDate()  // Liste neu laden
        }

        binding.etSelectedDate.setOnClickListener {
            showDatePickerDialog()  // Datumsauswahl-Dialog anzeigen
        }
    }

    // Datum im View aktualisieren
    private fun updateDateInView() {
        binding.etSelectedDate.setText(dateFormat.format(selectedDate.time))
    }

    // Datumsauswahl-Dialog anzeigen
    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
                loadItemsForDate()  // Produkte für das neue Datum laden
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

        // Listen der Kategorien leeren
        lebensmittelAdapter.setItems(emptyList())
        getrankeAdapter.setItems(emptyList())
        haushaltAdapter.setItems(emptyList())
        sonstigesAdapter.setItems(emptyList())

        // Produkte den jeweiligen Kategorien hinzufügen
        itemsForDate.forEach { item ->
            when (item.category) {
                "Lebensmittel" -> lebensmittelAdapter.addItem(item)
                "Getränke" -> getrankeAdapter.addItem(item)
                "Haushalt" -> haushaltAdapter.addItem(item)
                "Sonstiges" -> sonstigesAdapter.addItem(item)
            }
        }
    }

    // Swipe-to-Delete Funktionalität einrichten
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
                    // Bestätigungsdialog für das Löschen anzeigen
                    showDeleteConfirmationDialog(item, adapter, position)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    if (item.isChecked) {
                        // Gelöschtes Produkt wiederherstellen
                        showRestoreConfirmationDialog(position, adapter)
                    } else {
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
                    // Zeichnet nur roten Hintergrund, ohne Symbol
                    paint.color = Color.RED
                    val background = RectF(
                        itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)
                } else if (dX > 0) {
                    // Zeichnet grünen Hintergrund für Swipe nach rechts
                    paint.color = Color.GREEN
                    val background = RectF(
                        itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        // Swipe-to-Delete Funktion für die RecyclerViews der Kategorien verbinden
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerLebensmittel)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerGetranke)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerHaushalt)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerSonstiges)
    }

    // Bestätigungsdialog für das Löschen eines Produkts anzeigen
    private fun showDeleteConfirmationDialog(item: Produkt, adapter: EinkaufsItemAdapter, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Bestätigung")
            .setMessage("Möchten Sie dieses Produkt wirklich löschen?")
            .setPositiveButton("Ja") { _, _ ->
                adapter.markItemForDeletion(position)
                deleteItem(item)
            }
            .setNegativeButton("Nein") { dialog, _ ->
                dialog.dismiss()
                adapter.notifyItemChanged(position)
            }
            .show()
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
