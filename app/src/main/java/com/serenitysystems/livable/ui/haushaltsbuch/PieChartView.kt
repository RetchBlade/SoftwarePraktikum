package com.serenitysystems.livable.ui.haushaltsbuch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
    }
    private val linePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private var categories = listOf<String>()
    private var percentages = listOf<Float>()
    private var colors = listOf<Int>()

    fun setData(categories: List<String>, percentages: List<Float>, colors: List<Int>) {
        this.categories = categories
        this.percentages = percentages
        this.colors = colors
        invalidate() // Ekranı yeniden çizer
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val total = percentages.sum()
        var startAngle = -90f
        val radius = min(width, height) / 2f * 0.7f
        val cx = width / 2f
        val cy = height / 2f

        for (i in percentages.indices) {
            val sweepAngle = (percentages[i] / total) * 360f
            paint.color = colors[i % colors.size]
            canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, startAngle, sweepAngle, true, paint)

            val midAngle = startAngle + sweepAngle / 2f
            val lineX = cx + radius * cos(Math.toRadians(midAngle.toDouble())).toFloat()
            val lineY = cy + radius * sin(Math.toRadians(midAngle.toDouble())).toFloat()
            val textX = cx + (radius + 20) * cos(Math.toRadians(midAngle.toDouble())).toFloat()
            val textY = cy + (radius + 20) * sin(Math.toRadians(midAngle.toDouble())).toFloat()

            canvas.drawLine(cx, cy, lineX, lineY, linePaint)
            canvas.drawText("${"%.1f".format(percentages[i] * 100)}%", textX, textY, textPaint)

            startAngle += sweepAngle
        }
    }
}
