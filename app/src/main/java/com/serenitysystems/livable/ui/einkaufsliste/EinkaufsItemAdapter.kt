// EinkaufsItemAdapter.kt

package com.serenitysystems.livable.ui.einkaufsliste

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.ItemEinkaufsBinding
import java.text.SimpleDateFormat
import java.util.*

class EinkaufsItemAdapter(
    private var items: MutableList<Produkt>,
    private val onItemClicked: (Produkt) -> Unit,           // Callback beim Klick auf ein Produkt
    private val onDateChanged: (Produkt, String) -> Unit,   // Callback beim Ändern des Datums
    private val onImageClicked: (Produkt) -> Unit           // Callback beim Klick auf das Produktbild
) : RecyclerView.Adapter<EinkaufsItemAdapter.EinkaufsItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EinkaufsItemViewHolder {
        val binding = ItemEinkaufsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EinkaufsItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EinkaufsItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        if (!item.isChecked) {
            holder.itemView.setOnClickListener {
                onItemClicked(item)
            }
        } else {
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = items.size

    fun getItem(position: Int): Produkt = items[position]

    fun addItem(item: Produkt) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    fun markItemForDeletion(position: Int) {
        items[position].isChecked = true
        notifyItemChanged(position)
    }

    fun setItems(newItems: List<Produkt>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun restoreItem(position: Int) {
        items[position].isChecked = false
        notifyItemChanged(position)
    }

    inner class EinkaufsItemViewHolder(private val binding: ItemEinkaufsBinding) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        fun bind(item: Produkt) {
            binding.apply {
                // Produktbild setzen
                if (item.imageUri != null) {
                    itemImage.setImageURI(Uri.parse(item.imageUri))
                } else {
                    itemImage.setImageResource(R.drawable.ic_placeholder_image)
                }

                // Klick-Listener für das Produktbild
                itemImage.setOnClickListener {
                    onImageClicked(item)
                }

                // Produktname setzen
                itemName.text = item.name.replaceFirstChar { it.uppercase() }

                // Menge und Einheit setzen
                itemQuantity.text = item.quantity
                itemUnit.text = item.unit

                // Hintergrundfarbe setzen
                root.setBackgroundColor(Color.parseColor("#E3F2FD"))

                // Statusicon setzen
                if (item.statusIcon != null) {
                    itemIcon.setImageResource(item.statusIcon!!)
                    itemIcon.visibility = View.VISIBLE
                } else {
                    itemIcon.visibility = View.GONE
                }

                // Aussehen abhängig vom Status anpassen
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

        // Datumsauswahl für ein Produkt anzeigen
        private fun showDatePickerForItem(item: Produkt) {
            val context = binding.root.context
            val calendar = Calendar.getInstance()

            item.date?.let {
                val date = dateFormat.parse(it)
                if (date != null) {
                    calendar.time = date
                }
            }

            val datePickerDialog = DatePickerDialog(
                context, { _, year, month, dayOfMonth ->
                    val newDate = dateFormat.format(GregorianCalendar(year, month, dayOfMonth).time)
                    item.date = newDate
                    item.statusIcon = R.drawable.ic_warning
                    item.isChecked = false
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
