package com.talkie.app.domain

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import com.talkie.app.data.local.TransmissionLogEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {
    fun generateLogReport(logs: List<TransmissionLogEntity>): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(792, 612, 1).create() // Landscape for wide table
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 20f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowPaint = Paint().apply {
            textSize = 12f
            color = Color.BLACK
        }
        val incidentPaint = Paint().apply {
            textSize = 12f
            color = Color.RED
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Talkie P2P Network Traffic & Fleet Incident Report", 396f, 40f, titlePaint)
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val generatedTime = dateFormat.format(Date())
        
        paint.textSize = 10f
        canvas.drawText("Generated: $generatedTime", 40f, 65f, paint)

        var yPosition = 90f
        
        // Draw Header Background
        paint.color = Color.DKGRAY
        canvas.drawRect(40f, yPosition - 15f, 752f, yPosition + 5f, paint)
        
        // Header Text
        canvas.drawText("Time", 50f, yPosition, headerPaint)
        canvas.drawText("Operator Name", 200f, yPosition, headerPaint)
        canvas.drawText("Channel", 350f, yPosition, headerPaint)
        canvas.drawText("Duration (s)", 480f, yPosition, headerPaint)
        canvas.drawText("Compliance Status", 580f, yPosition, headerPaint)
        
        yPosition += 25f

        for (log in logs) {
            if (yPosition > 560f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            val dateStr = dateFormat.format(Date(log.timestamp))
            val complianceStatus = if (log.isIncidentFlagged) "Flagged Incident" else "Normal"
            val currentPaint = if (log.isIncidentFlagged) incidentPaint else rowPaint

            canvas.drawText(dateStr, 50f, yPosition, rowPaint)
            canvas.drawText(log.workerName, 200f, yPosition, rowPaint)
            canvas.drawText(log.channelName, 350f, yPosition, rowPaint)
            canvas.drawText(log.durationSeconds.toString(), 480f, yPosition, rowPaint)
            canvas.drawText(complianceStatus, 580f, yPosition, currentPaint)

            // Row separator line
            paint.color = Color.LTGRAY
            paint.strokeWidth = 1f
            canvas.drawLine(40f, yPosition + 10f, 752f, yPosition + 10f, paint)

            yPosition += 25f
        }

        pdfDocument.finishPage(page)

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, "Talkie_Fleet_Incident_Report_${System.currentTimeMillis()}.pdf")

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            file
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Error writing PDF", e)
            null
        } finally {
            pdfDocument.close()
        }
    }
}
