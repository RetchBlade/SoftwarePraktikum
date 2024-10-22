package com.serenitysystems.livable.ui.einkaufsliste.adapter

import android.app.DatePickerDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.ItemEinkaufsBinding
import com.serenitysystems.livable.ui.FullScreenImageDialogFragment
import com.serenitysystems.livable.ui.einkaufsliste.ItemOptionsDialogFragment
import com.serenitysystems.livable.ui.einkaufsliste.data.Produkt
import java.text.SimpleDateFormat
import java.util.*

class EinkaufsItemAdapter(
    private var items: MutableList<Produkt>,
    private val onItemClicked: (Produkt) -> Unit,             // Callback für Klick auf ein Produkt
    private val onDateChanged: (Produkt, String) -> Unit,     // Callback für das Ändern des Datums
    private val onImageClicked: (Produkt) -> Unit,            // Callback für Klick auf das Produktbild
    private val onItemDeleted: (Produkt) -> Unit              // Callback zum Löschen eines Produkts
) : RecyclerView.Adapter<EinkaufsItemAdapter.EinkaufsItemViewHolder>() {

    // Erstellt einen neuen ViewHolder für ein Listenelement
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EinkaufsItemViewHolder {
        val binding = ItemEinkaufsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EinkaufsItemViewHolder(binding)
    }

    // Verbindet den ViewHolder mit den Daten an der angegebenen Position
    override fun onBindViewHolder(holder: EinkaufsItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        // Click listener to open the options dialog for this item
        holder.itemView.setOnClickListener {
            val fragmentManager = (holder.itemView.context as androidx.fragment.app.FragmentActivity).supportFragmentManager

            val dialogFragment = ItemOptionsDialogFragment(
                produkt = item,
                onEditItem = { produkt ->
                    onItemClicked(produkt) // Directly open the edit screen (Bearbeiten)
                },
                onMarkAsBought = { produkt ->
                    // Update item status and UI
                    produkt.isChecked = true
                    produkt.statusIcon = R.drawable.ic_check // Assuming this is the green check icon
                    notifyItemChanged(position) // Refresh the item in the RecyclerView
                },

                onMoveToAnotherDay = { produkt ->
                    // Handle moving the item to another day
                    val context = holder.itemView.context
                    val calendar = Calendar.getInstance()

                    produkt.date?.let {
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        val date = dateFormat.parse(it)
                        if (date != null) {
                            calendar.time = date
                        }
                    }

                    val datePickerDialog = DatePickerDialog(
                        context, { _, year, month, dayOfMonth ->
                            val newDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                                .format(GregorianCalendar(year, month, dayOfMonth).time)
                            produkt.date = newDate
                            produkt.statusIcon = R.drawable.ic_warning  // Set warning icon if date is changed
                            produkt.isChecked = false
                            onDateChanged(produkt, newDate)  // Call the date changed callback
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )
                    datePickerDialog.show()
                }
            )

            dialogFragment.show(fragmentManager, "item_options_dialog")
        }
    }





    // Gibt die Anzahl der Elemente in der Liste zurück
    override fun getItemCount(): Int = items.size

    // Gibt das Produkt an der gegebenen Position zurück
    fun getItem(position: Int): Produkt = items[position]

    // Fügt ein neues Produkt zur Liste hinzu
    fun addItem(item: Produkt) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    // Markiert ein Produkt für das Löschen
    fun markItemForDeletion(position: Int) {
        if (position >= 0 && position < items.size) {
            items[position].isChecked = true
            notifyItemChanged(position)
        }
    }


    // Setzt die Liste der Produkte
    fun setItems(newItems: List<Produkt>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Stellt ein zuvor gelöschtes Produkt wieder her
    fun restoreItem(position: Int) {
        // Ensure the item is marked as not bought
        items[position].isChecked = false  // Mark as not checked
        items[position].statusIcon = null  // Remove the status icon (e.g., green check)

        notifyItemChanged(position)
    }


    // Entfernt ein Produkt aus der Liste
    private fun removeItemFromCurrentDate(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    // ViewHolder für jedes Listenelement
    inner class EinkaufsItemViewHolder(private val binding: ItemEinkaufsBinding) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // Verbindet das Produkt mit den Views im Layout
        fun bind(item: Produkt) {
            binding.apply {
                // Produktbild anzeigen oder Platzhalter setzen
                if (!item.imageUri.isNullOrEmpty()) {
                    itemImage.setImageURI(Uri.parse(item.imageUri))  // Bild anzeigen
                } else {
                    itemImage.setImageResource(R.drawable.ic_add_image)  // Platzhalterbild anzeigen
                }

                // Klick-Listener für das Produktbild
                itemImage.setOnClickListener {
                    if (!item.imageUri.isNullOrEmpty()) {
                        // Vollbild-Dialog anzeigen, wenn ein Bild vorhanden ist
                        showFullScreenImageDialog(item.imageUri!!)
                    } else {
                        // Bild auswählen, wenn kein Bild gesetzt ist
                        onImageClicked(item)
                    }
                }

                // Produktname, Menge und Einheit anzeigen
                itemName.text = item.name.replaceFirstChar { it.uppercase() }
                itemQuantity.text = item.quantity
                itemUnit.text = item.unit

                // Hintergrundfarbe setzen
                root.setBackgroundColor(Color.parseColor("#E3F2FD"))

                // Statusicon anzeigen, falls vorhanden
                if (item.statusIcon != null) {
                    itemIcon.setImageResource(item.statusIcon!!)
                    itemIcon.visibility = View.VISIBLE
                } else {
                    itemIcon.visibility = View.GONE
                }

                // Anpassung je nach Status (abgeschlossen oder nicht)
                if (item.isChecked) {
                    itemName.alpha = 0.5f
                    itemQuantity.alpha = 0.5f
                    itemUnit.alpha = 0.5f
                    itemName.paintFlags = itemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    itemQuantity.paintFlags = itemQuantity.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    itemUnit.paintFlags = itemUnit.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    itemName.alpha = 1.0f
                    itemQuantity.alpha = 1.0f
                    itemUnit.alpha = 1.0f
                    itemName.paintFlags = itemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    itemQuantity.paintFlags = itemQuantity.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    itemUnit.paintFlags = itemUnit.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

                // Lange Klick-Listener zum Ändern des Datums
                root.setOnLongClickListener {
                    showDatePickerForItem(item)
                    true
                }
            }
        }

        // Zeigt den Dialog zur Auswahl eines neuen Datums
        private fun showDatePickerForItem(item: Produkt) {
            val context = binding.root.context
            val calendar = Calendar.getInstance()

            // Aktuelles Datum des Produkts verwenden, falls vorhanden
            item.date?.let {
                val date = dateFormat.parse(it)
                if (date != null) {
                    calendar.time = date
                }
            }

            // DatePickerDialog anzeigen und Datum aktualisieren
            val datePickerDialog = DatePickerDialog(
                context, { _, year, month, dayOfMonth ->
                    val newDate = dateFormat.format(GregorianCalendar(year, month, dayOfMonth).time)
                    item.date = newDate
                    item.statusIcon = R.drawable.ic_warning  // Warnung setzen, wenn Datum geändert wurde
                    item.isChecked = false
                    onDateChanged(item, newDate)  // Callback aufrufen
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // Zeigt den Vollbild-Dialog für das Produktbild
        private fun showFullScreenImageDialog(imageUri: String) {
            val fragmentManager = (binding.root.context as androidx.fragment.app.FragmentActivity).supportFragmentManager
            val dialogFragment = FullScreenImageDialogFragment.newInstance(imageUri)
            dialogFragment.show(fragmentManager, "fullscreen_image")
        }
    }

    // Fügt Swipe-to-Delete-Funktionalität hinzu (mit Löschdialog und ohne Papierkorbsymbol)
    fun attachSwipeToDelete(recyclerView: RecyclerView) {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            // Ermöglicht keine Verschiebung der Listenelemente
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            // Verarbeitet den Links-Swipe und zeigt einen Bestätigungsdialog zum Löschen
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = items[position]

                // Bestätigungsdialog auf Deutsch anzeigen
                android.app.AlertDialog.Builder(recyclerView.context)
                    .setTitle("Löschen bestätigen")
                    .setMessage("Sind Sie sicher, dass Sie diesen Artikel löschen möchten?")
                    .setPositiveButton("Ja") { _, _ ->
                        // Produkt löschen
                        onItemDeleted(item)
                        removeItemFromCurrentDate(position)
                    }
                    .setNegativeButton("Nein") { dialog, _ ->
                        // Swipe rückgängig machen
                        notifyItemChanged(position)
                        dialog.dismiss()
                    }
                    .show()
            }

            // Zeichnet den Hintergrund beim Wischen (ohne Symbole)
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint().apply { color = Color.RED }
                val background = RectF(
                    itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat()
                )
                c.drawRect(background, paint)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        // Swipe-Handler mit RecyclerView verbinden
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }
}
