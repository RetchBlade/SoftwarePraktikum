package com.serenitysystems.livable.ui.haushaltsbuch

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToDeleteCallback(
    private val context: Context,
    private val adapter: ExpenseAdapter
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

    private val deleteColor = Color.parseColor("#f44336") //
    private val deletePaint = Paint().apply {
        color = deleteColor
        isAntiAlias = true
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        val expense = adapter.getExpenseAtPosition(position)

        AlertDialog.Builder(context)
            .setTitle("Bestätigung")
            .setMessage("Möchten Sie diesen Eintrag wirklich löschen?")
            .setPositiveButton("Ja") { _, _ ->
                adapter.removeItem(position)
            }
            .setNegativeButton("Nein") { dialog, _ ->
                adapter.notifyItemChanged(position)
                dialog.dismiss()
            }
            .show()
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
        val itemView = viewHolder.itemView

        c.drawRect(
            itemView.left.toFloat(), itemView.top.toFloat(),
            itemView.left + dX, itemView.bottom.toFloat(), deletePaint
        )

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
