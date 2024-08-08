package com.serenitysystems.livable.ui.einkaufsliste

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.databinding.ItemEinkaufsBinding

class EinkaufsItemAdapter(
    val items: MutableList<Produkt>,  // 'items' public yapıldı
    private val onItemClicked: (Produkt) -> Unit
) : RecyclerView.Adapter<EinkaufsItemAdapter.EinkaufsItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EinkaufsItemViewHolder {
        val binding = ItemEinkaufsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EinkaufsItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EinkaufsItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class EinkaufsItemViewHolder(private val binding: ItemEinkaufsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Produkt) {
            binding.apply {
                itemImage.setImageResource(item.imageResId)
                itemName.text = item.name.replaceFirstChar { it.uppercase() }
                itemQuantity.text = item.quantity
                itemUnit.text = item.unit

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

                root.setOnClickListener { onItemClicked(item) }
            }
        }
    }

    fun addItem(item: Produkt) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    fun markItemForDeletion(position: Int) {
        items[position].isChecked = true
        notifyItemChanged(position)
    }

    fun restoreItem(position: Int) {
        items[position].isChecked = false
        notifyItemChanged(position)
    }

    fun getItem(position: Int): Produkt {
        return items[position]
    }
}
