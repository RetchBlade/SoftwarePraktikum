// EinkaufsItemAdapter.kt
package com.serenitysystems.livable.ui.einkaufsliste.adapter

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.ItemEinkaufsBinding
import com.serenitysystems.livable.ui.FullScreenImageDialogFragment
import com.serenitysystems.livable.ui.einkaufsliste.EinkaufslisteViewModel
import com.serenitysystems.livable.ui.einkaufsliste.ItemOptionsDialogFragment
import com.serenitysystems.livable.ui.einkaufsliste.data.Produkt
import java.text.SimpleDateFormat
import java.util.*

class EinkaufsItemAdapter(
    private var items: MutableList<Produkt>,
    private val onItemClicked: (Produkt) -> Unit,             // Callback für Klick auf ein Produkt
    private val onDateChanged: (Produkt, String) -> Unit,     // Callback für das Ändern des Datums
    private val onImageClicked: (Produkt) -> Unit,            // Callback für Klick auf das Produktbild
    private val onItemDeleted: (Produkt) -> Unit,             // Callback zum Löschen eines Produkts
    private val onItemStatusChanged: (Produkt) -> Unit,       // Callback zum Zurücksetzen des Status
    private val viewModel: EinkaufslisteViewModel             // Hier ViewModel als Parameter
) : RecyclerView.Adapter<EinkaufsItemAdapter.EinkaufsItemViewHolder>() {
    // Datumsformat definieren
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // Kontext speichern
    private lateinit var context: Context


    // ViewHolder-Klasse für die Listenelemente
    inner class EinkaufsItemViewHolder(val binding: ItemEinkaufsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        // Daten an die Views binden
        fun bind(item: Produkt) {
            binding.apply {
                // Produktbild laden
                if (!item.imageUri.isNullOrEmpty()) {
                    // Bild mit Glide laden
                    Glide.with(itemView.context)
                        .load(Uri.parse(item.imageUri))
                        .override(100, 100)
                        .into(itemImage)
                } else {
                    // Platzhalterbild setzen
                    itemImage.setImageResource(R.drawable.ic_add_image)
                }

                // Klick-Listener für das Produktbild
                itemImage.setOnClickListener {
                    if (!item.imageUri.isNullOrEmpty()) {
                        // Bild im Vollbildmodus anzeigen
                        showFullScreenImageDialog(item.imageUri!!)
                    } else {
                        // Bildauswahl starten
                        onImageClicked(item)
                    }
                }

                // Produktname, Menge und Einheit setzen
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

                // Anpassungen je nach Status (erledigt oder nicht)
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
                    itemQuantity.paintFlags =
                        itemQuantity.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    itemUnit.paintFlags = itemUnit.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

                // Lange Klick-Listener zum Ändern des Datums
                root.setOnLongClickListener {
                    showDatePickerForItem(item)
                    true
                }
            }
        }

        // Methode zum Anzeigen des Vollbild-Bilddialogs
        private fun showFullScreenImageDialog(imageUri: String) {
            val fragmentManager =
                (binding.root.context as androidx.fragment.app.FragmentActivity).supportFragmentManager
            val dialogFragment = FullScreenImageDialogFragment.newInstance(imageUri)
            dialogFragment.show(fragmentManager, "fullscreen_image")
        }

        // Methode zum Anzeigen des DatePickers für ein Item
        private fun showDatePickerForItem(item: Produkt) {
            val calendar = Calendar.getInstance()

            // Falls das Produkt bereits ein Datum hat, dieses verwenden
            item.date?.let {
                val date = dateFormat.parse(it)
                if (date != null) {
                    calendar.time = date
                }
            }

            // DatePickerDialog erstellen
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, dayOfMonth)

                    // Überprüfen, ob das ausgewählte Datum in der Vergangenheit liegt
                    if (selectedCalendar.before(Calendar.getInstance().apply {
                            // Zeit auf Mitternacht setzen
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        })
                    ) {
                        Toast.makeText(
                            context,
                            "Sie können kein Datum in der Vergangenheit wählen.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val newDate = dateFormat.format(selectedCalendar.time)
                        item.date = newDate
                        item.statusIcon = R.drawable.ic_warning  // Warnungsicon setzen
                        item.isChecked = false
                        item.isPurchasedToday = false
                        onDateChanged(item, newDate)  // Callback aufrufen
                        viewModel.updateItem(item.date.toString(), item)
                        notifyItemChanged(adapterPosition)
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Minimales Datum auf heute setzen, um vergangene Daten zu verhindern
            datePickerDialog.datePicker.minDate = Calendar.getInstance().apply {
                // Zeit auf Mitternacht setzen
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            datePickerDialog.show()
        }
    }

    // ViewHolder erstellen
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EinkaufsItemViewHolder {
        context = parent.context
        val binding = ItemEinkaufsBinding.inflate(LayoutInflater.from(context), parent, false)
        return EinkaufsItemViewHolder(binding)
    }

    // ViewHolder mit Daten verbinden
    override fun onBindViewHolder(holder: EinkaufsItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        // Klick-Listener für das gesamte Item
        holder.itemView.setOnClickListener {
            val fragmentManager =
                (context as androidx.fragment.app.FragmentActivity).supportFragmentManager

            val dialogFragment = ItemOptionsDialogFragment(
                produkt = item,
                onEditItem = { produkt ->
                    onItemClicked(produkt) // Bearbeitungsdialog öffnen
                },
                onMarkAsBought = { produkt ->
                    // Status aktualisieren
                    produkt.isChecked = true
                    produkt.statusIcon = R.drawable.ic_check // Häkchen setzen
                    produkt.isPurchasedToday = true
                    viewModel.updateItem(produkt.date.toString(), produkt)
                    notifyItemChanged(position)

                },
                onMoveToAnotherDay = { produkt ->
                    // DatePicker zum Verschieben auf einen anderen Tag anzeigen
                    showDatePickerDialog(produkt, position)
                }
            )

            dialogFragment.show(fragmentManager, "item_options_dialog")
        }
    }

    // Anzahl der Elemente zurückgeben
    override fun getItemCount(): Int = items.size


    // Neues Element hinzufügen
    fun addItem(item: Produkt) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    // Liste der Elemente setzen
    fun setItems(newItems: List<Produkt>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged() // RecyclerView aktualisieren
    }

    // Element entfernen mit Indexüberprüfung
    private fun removeItemFromCurrentDate(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
        } else {
            // Loggen oder Meldung ausgeben, falls der Index ungültig ist
            Log.e("EinkaufsItemAdapter", "Ungültiger Index beim Entfernen: $position")
        }
    }

    // Swipe-to-Delete Funktionalität hinzufügen
    fun attachSwipeToDelete(recyclerView: RecyclerView) {
        val swipeHandler = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            // Bewegt die Elemente nicht
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            // Verarbeitet den Swipe
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position >= 0 && position < items.size) {
                    val item = items[position]

                    if (direction == ItemTouchHelper.LEFT) {
                        // Swipe nach links (Löschen)
                        AlertDialog.Builder(context)
                            .setTitle("Löschen bestätigen")
                            .setMessage("Sind Sie sicher, dass Sie dieses Produkt löschen möchten?")
                            .setPositiveButton("Ja") { _, _ ->
                                onItemDeleted(item)
                                removeItemFromCurrentDate(position)
                            }
                            .setNegativeButton("Nein") { dialog, _ ->
                                notifyItemChanged(position)
                                dialog.dismiss()
                            }
                            .show()
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        // Swipe nach rechts (Status zurücksetzen)
                        AlertDialog.Builder(context)
                            .setTitle("Status zurücksetzen")
                            .setMessage("Möchten Sie den Status dieses Produkts zurücksetzen?")
                            .setPositiveButton("Ja") { _, _ ->
                                item.isPurchasedToday = false
                                item.isChecked = false
                                item.statusIcon = null
                                onItemStatusChanged(item)
                                notifyItemChanged(position)
                            }
                            .setNegativeButton("Nein") { dialog, _ ->
                                notifyItemChanged(position)
                                dialog.dismiss()
                            }
                            .show()
                    }
                } else {
                    // Ungültiger Index, Swipe rückgängig machen
                    notifyItemChanged(position)
                }
            }

            // Zeichnet den Hintergrund und das Icon beim Wischen
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, // Verschiebung in X-Richtung
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()

                if (dX > 0) {
                    // Swipe nach rechts (Status zurücksetzen)
                    paint.color = Color.YELLOW

                    // Hintergrund zeichnen
                    val background = RectF(
                        itemView.left.toFloat(), itemView.top.toFloat(),
                        itemView.left + dX, itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)

                    // Undo-Icon laden
                    val icon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_undo)

                    if (icon != null) {
                        // Größe des Icons anpassen
                        val iconWidth = icon.width.toFloat()
                        val iconHeight = icon.height.toFloat()
                        val scaleFactor = itemView.height / iconHeight * 0.5f
                        val scaledWidth = iconWidth * scaleFactor
                        val scaledHeight = iconHeight * scaleFactor

                        // Position des Icons berechnen
                        val iconMargin = (itemView.height - scaledHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + scaledWidth
                        val iconBottom = iconTop + scaledHeight

                        // Icon zeichnen
                        val iconDest = RectF(iconLeft, iconTop, iconRight, iconBottom)
                        c.drawBitmap(icon, null, iconDest, null)
                    } else {
                        // Loggen oder alternative Aktion, falls das Icon nicht geladen werden kann
                        Log.e("EinkaufsItemAdapter", "Konnte ic_undo nicht laden")
                    }
                } else if (dX < 0) {
                    // Swipe nach links (Löschen)
                    paint.color = Color.RED

                    // Hintergrund zeichnen
                    val background = RectF(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat()
                    )
                    c.drawRect(background, paint)

                    // Papierkorb-Icon laden
                    val icon =
                        BitmapFactory.decodeResource(context.resources, R.drawable.ic_trash_bin)

                    if (icon != null) {
                        // Größe des Icons anpassen
                        val iconWidth = icon.width.toFloat()
                        val iconHeight = icon.height.toFloat()
                        val scaleFactor = itemView.height / iconHeight * 0.5f
                        val scaledWidth = iconWidth * scaleFactor
                        val scaledHeight = iconHeight * scaleFactor

                        // Position des Icons berechnen
                        val iconMargin = (itemView.height - scaledHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconLeft = itemView.right - iconMargin - scaledWidth
                        val iconRight = itemView.right - iconMargin
                        val iconBottom = iconTop + scaledHeight

                        // Icon zeichnen
                        val iconDest = RectF(iconLeft, iconTop, iconRight, iconBottom)
                        c.drawBitmap(icon, null, iconDest, null)
                    } else {
                        // Loggen oder alternative Aktion, falls das Icon nicht geladen werden kann
                        Log.e("EinkaufsItemAdapter", "Konnte ic_trash_bin nicht laden")
                    }
                }

                super.onChildDraw(
                    c, recyclerView, viewHolder,
                    dX, dY, actionState, isCurrentlyActive
                )
            }
        }

        // ItemTouchHelper an RecyclerView anhängen
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    // Methode zum Anzeigen des DatePickers beim Verschieben auf einen anderen Tag
    private fun showDatePickerDialog(produkt: Produkt, position: Int) {
        val calendar = Calendar.getInstance()

        // Aktuelles Datum des Produkts verwenden, falls vorhanden
        produkt.date?.let {
            val date = dateFormat.parse(it)
            if (date != null) {
                calendar.time = date
            }
        }

        // DatePickerDialog erstellen
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)

                // Überprüfen, ob das ausgewählte Datum in der Vergangenheit liegt
                if (selectedCalendar.before(Calendar.getInstance().apply {
                        // Zeit auf Mitternacht setzen
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    })
                ) {
                    Toast.makeText(
                        context,
                        "Sie können kein Datum in der Vergangenheit wählen.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val newDate = dateFormat.format(selectedCalendar.time)
                    produkt.date = newDate
                    produkt.statusIcon = R.drawable.ic_warning  // Warnungsicon setzen
                    produkt.isChecked = false
                    onDateChanged(produkt, newDate)  // Callback aufrufen
                    notifyItemChanged(position)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Minimales Datum auf heute setzen, um vergangene Daten zu verhindern
        datePickerDialog.datePicker.minDate = Calendar.getInstance().apply {
            // Zeit auf Mitternacht setzen
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        datePickerDialog.show()
    }
}
