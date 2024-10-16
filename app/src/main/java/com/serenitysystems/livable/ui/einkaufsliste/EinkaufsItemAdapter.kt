// EinkaufslisteItemAdapter.kt

package com.serenitysystems.livable.ui.einkaufsliste

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.ItemEinkaufsBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

// Adapter-Klasse zur Verwaltung der Produkte in der Einkaufsliste
class EinkaufsItemAdapter(
    private var items: MutableList<Produkt>,
    private val onItemClicked: (Produkt) -> Unit,       // Callback-Funktion beim Klick auf ein Produkt
    private val onDateChanged: (Produkt, String) -> Unit // Callback-Funktion, wenn das Datum geändert wird
) : RecyclerView.Adapter<EinkaufsItemAdapter.EinkaufsItemViewHolder>() {

    // ViewHolder-Klasse für die Darstellung eines einzelnen Produkts
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EinkaufsItemViewHolder {
        val binding = ItemEinkaufsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EinkaufsItemViewHolder(binding)
    }

    // Bindet die Daten eines Produkts an die View des ViewHolders
    override fun onBindViewHolder(holder: EinkaufsItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        // Klick-Listener für das Produkt, wenn es nicht abgehakt ist
        if (!item.isChecked) {
            holder.itemView.setOnClickListener {
                onItemClicked(item)
            }
        } else {
            // Wenn das Produkt abgehakt ist, kein Klick-Listener setzen
            holder.itemView.setOnClickListener(null)
        }
    }

    // Gibt die Anzahl der Produkte in der Liste zurück
    override fun getItemCount(): Int = items.size

    // Gibt das Produkt an einer bestimmten Position zurück
    fun getItem(position: Int): Produkt = items[position]

    // Fügt ein neues Produkt an den Anfang der Liste hinzu
    fun addItem(item: Produkt) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    // Markiert ein Produkt als gelöscht (abgehakt)
    fun markItemForDeletion(position: Int) {
        items[position].isChecked = true
        notifyItemChanged(position)
    }

    // Setzt die Liste der Produkte und aktualisiert die Ansicht
    fun setItems(newItems: List<Produkt>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Stellt ein gelöschtes Produkt wieder her
    fun restoreItem(position: Int) {
        items[position].isChecked = false
        notifyItemChanged(position)
    }

    // ViewHolder-Klasse zur Verwaltung der Produktansicht
    inner class EinkaufsItemViewHolder(private val binding: ItemEinkaufsBinding) : RecyclerView.ViewHolder(binding.root) {

        // Format für das Datum
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // Bindet die Produktdaten an die View
        fun bind(item: Produkt) {
            binding.apply {
                // Setzt das Produktbild
                itemImage.setImageResource(item.imageResId)

                // Setzt den Produktnamen mit erstem Buchstaben groß
                itemName.text = item.name.replaceFirstChar { it.uppercase() }

                // Setzt die Menge und Einheit
                itemQuantity.text = item.quantity
                itemUnit.text = item.unit

                // Setzt die Hintergrundfarbe
                root.setBackgroundColor(Color.parseColor("#E3F2FD"))

                // Setzt das Status-Icon, falls vorhanden
                if (item.statusIcon != null) {
                    itemIcon.setImageResource(item.statusIcon!!)
                    itemIcon.visibility = View.VISIBLE
                } else {
                    itemIcon.visibility = View.GONE
                }

                // Wenn das Produkt gelöscht ist, Text durchstreichen und Transparenz reduzieren
                if (item.isChecked) {
                    itemName.alpha = 0.5f
                    itemQuantity.alpha = 0.5f
                    itemUnit.alpha = 0.5f

                    itemName.paintFlags = itemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    itemQuantity.paintFlags = itemQuantity.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    itemUnit.paintFlags = itemUnit.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    // Normalzustand wiederherstellen
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

        // Zeigt einen DatePickerDialog an, um das Datum des Produkts zu ändern
        private fun showDatePickerForItem(item: Produkt) {
            val context = binding.root.context
            val calendar = Calendar.getInstance()

            // Aktuelles Datum des Produkts setzen, falls vorhanden
            item.date?.let {
                val date = dateFormat.parse(it)
                if (date != null) {
                    calendar.time = date
                }
            }

            val datePickerDialog = DatePickerDialog(
                context, { _, year, month, dayOfMonth ->
                    val newDate = dateFormat.format(GregorianCalendar(year, month, dayOfMonth).time)
                    // Aktualisiere das Datum des Produkts
                    item.date = newDate
                    // Setze das Status-Icon auf Warnung und isChecked auf false
                    item.statusIcon = R.drawable.ic_warning
                    item.isChecked = false
                    // Rufe den Callback auf, um das Produkt im ViewModel zu aktualisieren
                    onDateChanged(item, newDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }
}
