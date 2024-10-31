package com.serenitysystems.livable.ui.haushaltsbuch

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class PieSegment(
        val startAngle: Float,
        val sweepAngle: Float,
        val color: Int,
        val letter: String
    )

    private val segments = mutableListOf<PieSegment>()
    private val piePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }

    // Daten setzen und vorbereiten
    fun setData(categories: List<String>, values: List<Double>, colors: List<Int>) {
        prepareChartData(categories, values, colors)
        invalidate() // Neu zeichnen
    }

    // Vorbereitung der Daten außerhalb von onDraw()
    private fun prepareChartData(categories: List<String>, values: List<Double>, colors: List<Int>) {
        segments.clear()
        if (categories.isEmpty() || values.isEmpty()) return

        val total = values.sum()
        var startAngle = -90f

        for (i in categories.indices) {
            val value = values[i]
            val sweepAngle = (value / total * 360).toFloat()
            val letter = categories[i].first().toString().uppercase()

            segments.add(
                PieSegment(
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    color = colors[i % colors.size],
                    letter = letter
                )
            )
            startAngle += sweepAngle
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (segments.isEmpty()) return

        val diameter = min(width, height) * 0.8f
        val radius = diameter / 2
        val cx = width / 2f
        val cy = height / 2f
        val rectF = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        segments.forEach { segment ->
            // Zeichnen des Segmentes
            piePaint.color = segment.color
            canvas.drawArc(rectF, segment.startAngle, segment.sweepAngle, true, piePaint)

            // Berechnen der Position für den Buchstaben
            val angle = Math.toRadians((segment.startAngle + segment.sweepAngle / 2).toDouble())
            val textRadius = radius * 0.7f
            val textX = cx + (textRadius * Math.cos(angle)).toFloat()
            val textY = cy + (textRadius * Math.sin(angle)).toFloat() + textPaint.textSize / 2

            // Zeichnen des Buchstabens
            canvas.drawText(segment.letter, textX, textY, textPaint)
        }

        // Zeichnen des inneren Kreises für Donut-Effekt
        val holePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }
        val holeRadius = radius * 0.5f
        canvas.drawCircle(cx, cy, holeRadius, holePaint)
    }
}
