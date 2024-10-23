package com.serenitysystems.livable.ui.haushaltsbuch

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense

class PDFHelper(private val context: Context) {

    fun createPDF(title: String, expenses: List<Expense>) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = document.startPage(pageInfo)

        val canvas = page.canvas
        canvas.drawText(title, 10f, 10f, Paint())

        expenses.forEachIndexed { index, expense ->
            canvas.drawText("${expense.kategorie}: ${expense.betrag} EUR", 10f, (30 + index * 20).toFloat(), Paint())
        }

        document.finishPage(page)

        val directoryPath = "${Environment.getExternalStorageDirectory()}/Haushaltsbuch/"
        val file = File(directoryPath)
        if (!file.exists()) {
            file.mkdirs()
        }

        val targetPDF = "$directoryPath$title.pdf"
        val filePath = File(targetPDF)

        try {
            document.writeTo(FileOutputStream(filePath))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        document.close()
    }
}