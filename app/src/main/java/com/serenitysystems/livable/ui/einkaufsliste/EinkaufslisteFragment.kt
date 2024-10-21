package com.serenitysystems.livable.ui.einkaufsliste

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
            onItemClicked = { item -> showEditItemDialog(item) },   // Directly edit the product without alert
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) },  // Datum ändern
            onImageClicked = { item -> handleImageClick(item) }, // Bildklick verarbeiten
            onItemDeleted = { item -> deleteItem(item) }         // Produkt löschen
        )
        getrankeAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> showEditItemDialog(item) },
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) },
            onImageClicked = { item -> handleImageClick(item) },
            onItemDeleted = { item -> deleteItem(item) }
        )
        haushaltAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> showEditItemDialog(item) },
            onDateChanged = { item, neuesDatum -> moveItemToNewDate(item, neuesDatum) },
            onImageClicked = { item -> handleImageClick(item) },
            onItemDeleted = { item -> deleteItem(item) }
        )
        sonstigesAdapter = EinkaufsItemAdapter(
            mutableListOf(),
            onItemClicked = { item -> showEditItemDialog(item) },
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

    // Dialog zum Bearbeiten eines Produkts anzeigen (no confirmation alert)
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
                    // Show delete confirmation dialog
                    showDeleteConfirmationDialog(item, adapter, position)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    if (item.isChecked) {
                        // Only allow restoring if the item is marked as bought
                        showRestoreConfirmationDialog(position, adapter)
                    } else {
                        // Optionally notify user that the item cannot be restored
                        adapter.notifyItemChanged(position) // Restore the item to its original state
                    }

            }

        }override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float,
                dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconWidth = 100f // Set a size for your icons
                val iconMargin = 20f // Margin from the edge of the item view

                // Load your icons
                val trashBinIcon = BitmapFactory.decodeResource(recyclerView.context.resources, R.drawable.ic_trash_bin)
                val restoreIcon = BitmapFactory.decodeResource(recyclerView.context.resources, R.drawable.ic_restore1)

                // Get the adapter and the position of the swiped item
                val position = viewHolder.adapterPosition
                val adapter = recyclerView.adapter as EinkaufsItemAdapter
                val item = adapter.getItem(position)

                if (dX < 0) { // Swipe left for delete
                    // Draw trash bin icon
                    val trashBinIconRect = RectF(
                        itemView.right + dX + iconMargin,
                        itemView.top + (itemView.height / 2 - iconWidth / 2),
                        itemView.right + dX + iconMargin + iconWidth,
                        itemView.bottom - (itemView.height / 2 - iconWidth / 2)
                    )
                    c.drawBitmap(trashBinIcon, null, trashBinIconRect, null) // Draw the trash bin icon
                } else if (dX > 0 && item.isChecked) { // Swipe right for restore, only if item is marked as bought
                    // Draw restore icon
                    val restoreIconRect = RectF(
                        itemView.left + dX - iconWidth - iconMargin,
                        itemView.top + (itemView.height / 2 - iconWidth / 2),
                        itemView.left + dX - iconMargin,
                        itemView.bottom - (itemView.height / 2 - iconWidth / 2)
                    )
                    c.drawBitmap(restoreIcon, null, restoreIconRect, null) // Draw the restore icon
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

        }

        // Attach swipe handlers to each RecyclerView
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerLebensmittel)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerGetranke)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerHaushalt)
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerSonstiges)
    }

    // Confirmation dialog for deleting a product
    // Bestätigungsdialog für das Löschen eines Produkts anzeigen
    private fun showDeleteConfirmationDialog(item: Produkt, adapter: EinkaufsItemAdapter, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Bestätigung")
            .setMessage("Möchten Sie dieses Produkt löschen?")
            .setPositiveButton("Ja") { _, _ ->
                // Check if position is valid before deleting
                if (position >= 0 && position < adapter.itemCount) {
                    adapter.markItemForDeletion(position)
                    deleteItem(item)
                } else {
                    // Handle case where position is invalid (optional)
                }
            }
            .setNegativeButton("Nein") { dialog, _ ->
                dialog.dismiss()
                adapter.notifyItemChanged(position)
            }
            .show()
    }


    // Confirmation dialog for restoring a deleted product
    private fun showRestoreConfirmationDialog(position: Int, adapter: EinkaufsItemAdapter) {
        AlertDialog.Builder(requireContext())
            .setTitle("Bestätigung")
            .setMessage("Möchten Sie diesen Artikel wiederherstellen?")
            .setPositiveButton("Ja") { _, _ ->
                val item = adapter.getItem(position)

                // Restore the item and mark it as "not bought"
                item.isChecked = false  // Mark as not bought
                item.statusIcon = null  // Remove the green check icon

                adapter.restoreItem(position) // Restore the item in the adapter
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
