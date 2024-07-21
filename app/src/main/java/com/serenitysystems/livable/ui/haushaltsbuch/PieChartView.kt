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

class CustomPieChartView @JvmOverloads constructor(
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
    private val labelTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }
    private val colors = listOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.LTGRAY
    )
    private val data = listOf(
        28f, 26f, 17f, 14f, 7f, 5f, 20f
    )
    private val labels = listOf(
        "Kultur", "Gesundheit", "Beauty", "Kleidung", "Transportation", "Lebensmittel", "Haushalt"
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = data.sum()
        var startAngle = -90f
        val radius = min(width, height) / 2f * 0.7f
        val innerRadius = radius * 0.6f
        val cx = width / 2f
        val cy = height / 2f

        // Draw the pie segments
        for (i in data.indices) {
            val sweepAngle = data[i] / total * 360f
            paint.color = colors[i % colors.size]
            canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }

        // Draw the inner circle to create the donut effect
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, innerRadius, paint)

        // Draw the text inside the donut
        textPaint.color = Color.GREEN
        canvas.drawText("20.00 €", cx, cy - 10, textPaint)
        textPaint.color = Color.RED
        canvas.drawText("-265.78 €", cx, cy + 30, textPaint)

        // Draw labels with lines and percentages
        startAngle = -90f
        for (i in data.indices) {
            val sweepAngle = data[i] / total * 360f
            val angle = startAngle + sweepAngle / 2f

            val labelRadius = radius + 40f
            val x = cx + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = cy + radius * sin(Math.toRadians(angle.toDouble())).toFloat()
            val endX = cx + labelRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val endY = cy + labelRadius * sin(Math.toRadians(angle.toDouble())).toFloat()

            // Draw line from segment to label
            canvas.drawLine(x, y, endX, endY, linePaint)

            // Adjust text alignment based on position
            if (angle > 90 && angle < 270) {
                labelTextPaint.textAlign = Paint.Align.RIGHT
            } else {
                labelTextPaint.textAlign = Paint.Align.LEFT
            }

            // Draw the label text
            canvas.drawText(labels[i], endX, endY, labelTextPaint)

            // Draw the percentage text
            val percentageText = "${data[i].toInt()}%"
            val percentageY = endY + 30
            canvas.drawText(percentageText, endX, percentageY, labelTextPaint)

            startAngle += sweepAngle
        }
    }
}
