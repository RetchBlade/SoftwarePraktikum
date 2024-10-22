package com.serenitysystems.livable.ui.wochenplan.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R

class DayPagerAdapter(
    private val days: List<String>,
    private val onDaySelected: (String) -> Unit
) : RecyclerView.Adapter<DayPagerAdapter.DayViewHolder>() {

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTitle: TextView = itemView.findViewById(R.id.dayName)

        init {
            itemView.setOnClickListener {
                val bindingAdapterPosition = 0
                val day = days[bindingAdapterPosition] // Updated to use `bindingAdapterPosition` to avoid potential issues
                onDaySelected(day)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.wochenplan_day_header, parent, false)

        // Ensure the width and height are set to match_parent to avoid layout issues in ViewPager2
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.dayTitle.text = days[position]
    }

    override fun getItemCount(): Int = days.size
}
