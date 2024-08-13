package com.serenitysystems.livable.ui.haushaltsbuch

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BalkendiagrammView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    private var kategorien: Map<String, Float> = emptyMap()

    fun setKategorien(kategorien: Map<String, Float>) {
        this.kategorien = kategorien
        invalidate() // Aktualisiere die View
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val maxBetrag = kategorien.values.maxOrNull() ?: 0f
        val barWidth = width.toFloat() * 0.6f
        val barHeightFactor = height.toFloat() / (maxBetrag + 10) // Skaliert die Balkenhöhe

        var yPos = 0f

        kategorien.forEach { (kategorie, betrag) ->
            paint.color = when (kategorie) {
                "Zuhause" -> Color.RED
                "Kultur" -> Color.YELLOW
                "Gesundheit & Beauty" -> Color.MAGENTA
                "Auto" -> Color.BLUE
                "Andere" -> Color.GRAY
                else -> Color.LTGRAY
            }

            val barHeight = betrag * barHeightFactor
            canvas.drawRect(0f, yPos, barWidth, yPos + barHeight, paint)
            canvas.drawText("$kategorie: ${betrag}€", barWidth + 10, yPos + barHeight / 2, textPaint)

            yPos += barHeight + 20
        }
    }
}
