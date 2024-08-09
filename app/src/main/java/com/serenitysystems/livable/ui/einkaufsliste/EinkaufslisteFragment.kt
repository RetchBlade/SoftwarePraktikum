package com.serenitysystems.livable.ui.einkaufsliste

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentEinkaufslisteBinding

class EinkaufslisteFragment : Fragment() {

    private var _binding: FragmentEinkaufslisteBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EinkaufslisteViewModel

    // Adapter für die verschiedenen Kategorien
    private lateinit var lebensmittelAdapter: EinkaufsItemAdapter
    private lateinit var getrankeAdapter: EinkaufsItemAdapter
    private lateinit var haushaltAdapter: EinkaufsItemAdapter
    private lateinit var sonstigesAdapter: EinkaufsItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(EinkaufslisteViewModel::class.java)
        _binding = FragmentEinkaufslisteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerViews()
        setupFab()
        setupSwipeHandlers()

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

    // Fügt das neue Produkt zur entsprechenden Kategorie hinzu und scrollt die Liste nach oben
    private fun addItemToList(newItem: Produkt) {
        val containerToMove: ViewGroup? = when (newItem.category) {
            "Lebensmittel" -> {
                lebensmittelAdapter.addItem(newItem)
                binding.recyclerLebensmittel.scrollToPosition(0)
                binding.containerLebensmittel
            }
            "Getränke" -> {
                getrankeAdapter.addItem(newItem)
                binding.recyclerGetranke.scrollToPosition(0)
                binding.containerGetranke
            }
            "Haushalt" -> {
                haushaltAdapter.addItem(newItem)
                binding.recyclerHaushalt.scrollToPosition(0)
                binding.containerHaushalt
            }
            "Sonstiges" -> {
                sonstigesAdapter.addItem(newItem)
                binding.recyclerSonstiges.scrollToPosition(0)
                binding.containerSonstiges
            }
            else -> null
        }

        // Wenn der Container existiert, wird er an die erste Position verschoben
        containerToMove?.let {
            val parent = it.parent as ViewGroup
            parent.removeView(it)
            parent.addView(it, 0)  // Container an die erste Position verschieben
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
                    adapter.markItemForDeletion(position)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    showRestoreConfirmationDialog(position, adapter)
                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float,
                dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()
                if (dX < 0) {
                    paint.color = Color.RED
                    val background = RectF(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                    c.drawRect(background, paint)
                } else {
                    paint.color = Color.GREEN
                    val background = RectF(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat())
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
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
