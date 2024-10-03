import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.ui.haushaltsbuch.ExpenseAdapter

class SwipeToDeleteCallback(
    private val context: Context,
    private val adapter: ExpenseAdapter
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteColor = Color.parseColor("#f44336")
    private val restoreColor = Color.parseColor("#4CAF50")
    private val deletePaint = Paint().apply {
        color = deleteColor
        isAntiAlias = true
    }
    private val restorePaint = Paint().apply {
        color = restoreColor
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

        when (direction) {
            ItemTouchHelper.RIGHT -> {
                // Sağ kaydırma (silme) için onRequestDelete kullanılır
                adapter.onRequestDelete(expense, position)
            }
            ItemTouchHelper.LEFT -> {
                // Sol kaydırma (geri yükleme) için restoreExpense kullanılır
                adapter.restoreExpense(expense)
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
        val itemView = viewHolder.itemView
        val paint = if (dX > 0) deletePaint else restorePaint

        c.drawRect(
            itemView.left.toFloat(), itemView.top.toFloat(),
            itemView.left + dX, itemView.bottom.toFloat(), paint
        )

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
