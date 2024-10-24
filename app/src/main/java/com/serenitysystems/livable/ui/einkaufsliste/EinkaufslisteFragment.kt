// EinkaufslisteFragment.kt
package com.serenitysystems.livable.ui.einkaufsliste

// Notwendige Importe
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentEinkaufslisteBinding
import com.serenitysystems.livable.ui.AddItemDialogFragment
import com.serenitysystems.livable.ui.einkaufsliste.adapter.EinkaufsItemAdapter
import com.serenitysystems.livable.ui.einkaufsliste.data.Produkt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
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

    // Temporär ausgewähltes Produkt für Bildänderung
    private var selectedItemForImageChange: Produkt? = null
    private val selectedDateKey get() = dateFormat.format(selectedDate.time)

    // ActivityResultLauncher für die Bildauswahl
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && selectedItemForImageChange != null) {
            // Bildverarbeitung im Hintergrund
            CoroutineScope(Dispatchers.IO).launch {
                val imageUriString = uri.toString()
                selectedItemForImageChange?.imageUri = imageUriString
                viewModel.updateItemImage(selectedDateKey, selectedItemForImageChange!!)

                withContext(Dispatchers.Main) {
                    loadItemsForDate()  // Liste nach Bildänderung neu laden
                }
            }
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
            onItemClicked = { item -> showEditItemDialog(item) },   // Direktes Bearbeiten des Produkts
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
        lebensmittelAdapter.attachSwipeToDelete(binding.recyclerLebensmittel)
        getrankeAdapter.attachSwipeToDelete(binding.recyclerGetranke)
        haushaltAdapter.attachSwipeToDelete(binding.recyclerHaushalt)
        sonstigesAdapter.attachSwipeToDelete(binding.recyclerSonstiges)
    }

    // Dialog zum Bearbeiten eines Produkts anzeigen
    private fun showEditItemDialog(item: Produkt) {
        val dialog = AddItemDialogFragment()
        dialog.setCurrentItem(item)
        dialog.onAddItem = { updatedItem, oldItem ->
            val currentDateKey = dateFormat.format(selectedDate.time)

            // Wenn das Datum geändert wurde oder die Kategorie geändert wurde
            if (updatedItem.date != oldItem?.date || updatedItem.category != oldItem?.category) {
                oldItem?.let { viewModel.deleteItem(currentDateKey, it) }
                updatedItem.date?.let { newDate ->
                    viewModel.addItem(newDate, updatedItem)
                }
            } else {
                // Falls nur andere Werte geändert wurden
                viewModel.updateItem(currentDateKey, updatedItem, currentDateKey)
            }

            // Liste neu laden
            loadItemsForDate()
            true
        }
        dialog.show(childFragmentManager, "EditItemDialog")
    }


    // Funktion zum Verschieben des Produkts zu einem neuen Datum
    private fun moveItemToNewDate(item: Produkt, neuesDatum: String) {
        val currentDateKey = dateFormat.format(selectedDate.time)
        item.date = neuesDatum // Datum des Produkts aktualisieren
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

    // FloatingActionButton einrichten - Neues Produkt hinzufügen
    private fun setupFab() {
        binding.fab.setOnClickListener {
            val dialog = AddItemDialogFragment()
            dialog.onAddItem = { newItem, _ ->
                val dateKey = newItem.date ?: dateFormat.format(selectedDate.time)
                viewModel.addItem(dateKey, newItem)  // Neues Produkt zum ViewModel hinzufügen

                // Wenn das Datum des neuen Produkts mit dem ausgewählten Datum übereinstimmt, Liste aktualisieren
                if (dateKey == selectedDateKey) {
                    loadItemsForDate()
                }
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
                else -> sonstigesAdapter.addItem(item)  // Für unbekannte Kategorien
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
