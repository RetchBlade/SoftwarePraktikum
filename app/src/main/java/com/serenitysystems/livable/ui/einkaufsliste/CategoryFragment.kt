package com.serenitysystems.livable.ui.einkaufsliste

import android.graphics.*
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.serenitysystems.livable.databinding.FragmentCategoryBinding

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: EinkaufsItemAdapter
    private lateinit var category: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            category = it.getString(ARG_CATEGORY) ?: "Sonstiges"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupSwipeHandler()

        return root
    }

    // RecyclerView einrichten
    private fun setupRecyclerView() {
        adapter = EinkaufsItemAdapter(mutableListOf()) { item -> }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    // Swipe Handler einrichten
    private fun setupSwipeHandler() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.getItem(position)

                if (direction == ItemTouchHelper.LEFT) {
                    // Element als gelöscht markieren
                    adapter.markItemForDeletion(position)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    if (item.isChecked) {
                        // Gelöschtes Element wiederherstellen
                        showRestoreConfirmationDialog(position)
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
                if (dX < 0) {
                    // Linkswisch - Rot
                    paint.color = Color.RED
                    val background = RectF(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                    c.drawRect(background, paint)
                } else if (dX > 0) {
                    // Rechtswisch - Grün
                    if (adapter.getItem(viewHolder.adapterPosition).isChecked) {
                        paint.color = Color.GREEN
                        val background = RectF(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat())
                        c.drawRect(background, paint)
                    } else {
                        // Nichts zeichnen
                        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    }
                } else {
                    // Nichts zeichnen
                    c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerView)
    }

    // Bestätigungsdialog zum Wiederherstellen eines gelöschten Artikels
    private fun showRestoreConfirmationDialog(position: Int) {
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

    // Artikel zur Kategorie hinzufügen
    fun addItem(item: Produkt) {
        adapter.addItem(item)
        binding.recyclerView.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String) = CategoryFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CATEGORY, category)
            }
        }
    }
}
