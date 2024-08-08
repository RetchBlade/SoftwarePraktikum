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
import com.serenitysystems.livable.databinding.FragmentEinkaufslisteBinding

class EinkaufslisteFragment : Fragment() {

    private var _binding: FragmentEinkaufslisteBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: EinkaufslisteViewModel

    private lateinit var lebensmittelAdapter: EinkaufsItemAdapter
    private lateinit var getrankeAdapter: EinkaufsItemAdapter
    private lateinit var haushaltAdapter: EinkaufsItemAdapter
    private lateinit var sonstigesAdapter: EinkaufsItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(EinkaufslisteViewModel::class.java)
        _binding = FragmentEinkaufslisteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerViews()
        setupFab()
        setupSwipeHandlers()

        return root
    }

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

    private fun setupFab() {
        binding.fab.setOnClickListener {
            val dialog = AddItemDialogFragment()
            dialog.onAddItem = { newItem ->
                val existingItem = findExistingItem(newItem)
                if (existingItem != null) {
                    showAddConfirmationDialog(existingItem, newItem)
                } else {
                    addItemToList(newItem)
                }
                true
            }
            dialog.show(childFragmentManager, "AddItemDialog")
        }
    }

    private fun findExistingItem(newItem: Produkt): Produkt? {
        return (lebensmittelAdapter.items + getrankeAdapter.items + haushaltAdapter.items + sonstigesAdapter.items)
            .find { it.name.equals(newItem.name, ignoreCase = true) }
    }

    private fun addItemToList(newItem: Produkt) {
        when (newItem.category) {
            "Lebensmittel" -> {
                lebensmittelAdapter.addItem(newItem)
            }
            "Getränke" -> {
                getrankeAdapter.addItem(newItem)
            }
            "Haushalt" -> {
                haushaltAdapter.addItem(newItem)
            }
            "Sonstiges" -> {
                sonstigesAdapter.addItem(newItem)
            }
        }
    }

    private fun showAddConfirmationDialog(existingItem: Produkt, newItem: Produkt) {
        AlertDialog.Builder(requireContext())
            .setTitle("Produkt Bereits Vorhanden")
            .setMessage("Dieses Produkt ist bereits vorhanden. Möchten Sie es erneut hinzufügen?")
            .setPositiveButton("Ja") { dialog, which ->
                if (existingItem.unit == newItem.unit) {
                    android.widget.Toast.makeText(requireContext(), "Dieses Produkt ist bereits vorhanden", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    addItemToList(newItem)
                }
            }
            .setNegativeButton("Nein") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupSwipeHandlers() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = viewHolder.itemView.context as? EinkaufsItemAdapter
                if (adapter != null) {
                    val position = viewHolder.adapterPosition
                    val item = adapter.getItem(position)

                    if (direction == ItemTouchHelper.LEFT) {
                        adapter.markItemForDeletion(position)
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        showRestoreConfirmationDialog(position, adapter)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val paint = Paint()
                    if (dX < 0) { // Swipe to left
                        paint.color = Color.RED
                        val background = RectF(
                            itemView.right + dX, itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat()
                        )
                        c.drawRect(background, paint)
                    } else { // Swipe to right
                        paint.color = Color.GREEN
                        val background = RectF(
                            itemView.left.toFloat(), itemView.top.toFloat(),
                            itemView.left + dX, itemView.bottom.toFloat()
                        )
                        c.drawRect(background, paint)
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            }
        }

        val itemTouchHelperLebensmittel = ItemTouchHelper(swipeHandler)
        itemTouchHelperLebensmittel.attachToRecyclerView(binding.recyclerLebensmittel)

        val itemTouchHelperGetranke = ItemTouchHelper(swipeHandler)
        itemTouchHelperGetranke.attachToRecyclerView(binding.recyclerGetranke)

        val itemTouchHelperHaushalt = ItemTouchHelper(swipeHandler)
        itemTouchHelperHaushalt.attachToRecyclerView(binding.recyclerHaushalt)

        val itemTouchHelperSonstiges = ItemTouchHelper(swipeHandler)
        itemTouchHelperSonstiges.attachToRecyclerView(binding.recyclerSonstiges)
    }

    private fun showRestoreConfirmationDialog(position: Int, adapter: EinkaufsItemAdapter) {
        AlertDialog.Builder(requireContext())
            .setTitle("Bestätigung")
            .setMessage("Möchten Sie diesen Artikel wiederherstellen?")
            .setPositiveButton("Ja") { dialog, which ->
                adapter.restoreItem(position)
            }
            .setNegativeButton("Nein") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
