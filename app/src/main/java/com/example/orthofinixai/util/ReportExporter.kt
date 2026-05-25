package com.example.orthofinixai.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.orthofinixai.data.model.AIReport
import com.example.orthofinixai.data.model.Patient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ReportExporter {

    fun generateAndSharePdf(context: Context, patient: Patient?, report: AIReport?) {
        val safePatient = patient ?: Patient("MOCK-001", "Anonymous Patient", "01/01/1990", "Male", "N/A")
        val safeReport = report ?: AIReport("MOCK-REP", "MOCK-001", 85f, 90f, 88f, 92f, listOf("Patient is ready for detailing phase."), "2026-05-18")

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 Size: 595 x 842 pt
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint()

        // 1. Header background (Clinical Deep Navy)
        paint.color = Color.parseColor("#0C1B33") // ClinicalDeepNavy
        canvas.drawRect(0f, 0f, 595f, 130f, paint)

        // 2. Title
        textPaint.color = Color.WHITE
        textPaint.textSize = 20f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ORTHOFINIX.AI CLINICAL REPORT", 40f, 55f, textPaint)

        textPaint.textSize = 10f
        textPaint.color = Color.parseColor("#8E9AAF")
        canvas.drawText("Orthodontic Finishing Assessment Report", 40f, 75f, textPaint)
        canvas.drawText("Date: ${safeReport.created_at ?: "2026-05-18"}", 40f, 95f, textPaint)

        // 3. Patient Details Card
        paint.color = Color.parseColor("#F4F6F9") // BackgroundClinical
        canvas.drawRect(40f, 150f, 555f, 250f, paint)

        textPaint.color = Color.parseColor("#0C1B33")
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PATIENT ASSESSMENT CARD", 55f, 175f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 10f
        textPaint.color = Color.BLACK
        canvas.drawText("Patient Name: ${safePatient.name}", 55f, 195f, textPaint)
        canvas.drawText("Patient ID: ${safePatient.id}", 55f, 210f, textPaint)
        canvas.drawText("Gender: ${safePatient.gender}  |  DOB: ${safePatient.date_of_birth}", 55f, 225f, textPaint)

        // 4. Clinical Metrics
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 14f
        textPaint.color = Color.parseColor("#0C1B33")
        canvas.drawText("CORE CLINICAL METRICS", 40f, 290f, textPaint)

        // Line separator
        paint.color = Color.parseColor("#E5E9F0")
        canvas.drawRect(40f, 300f, 555f, 302f, paint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 11f
        textPaint.color = Color.BLACK
        canvas.drawText("Metric Category", 45f, 325f, textPaint)
        canvas.drawText("Clinical Score", 400f, 325f, textPaint)
        canvas.drawText("Status", 490f, 325f, textPaint)

        paint.color = Color.parseColor("#D8DEE9")
        canvas.drawRect(40f, 335f, 555f, 336f, paint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        // Rows
        val overallScore = (safeReport.abo_score + safeReport.arch_symmetry_score + safeReport.root_angulation_score) / 3f
        
        canvas.drawText("ABO OGS Scoring", 45f, 355f, textPaint)
        canvas.drawText("${safeReport.abo_score.toInt()}%", 400f, 355f, textPaint)
        canvas.drawText(if (safeReport.abo_score > 85) "Pass" else "Review", 490f, 355f, textPaint)

        canvas.drawText("Andrews Six Keys Evaluation", 45f, 375f, textPaint)
        canvas.drawText("${safeReport.andrews_score.toInt()}%", 400f, 375f, textPaint)
        canvas.drawText("Optimal", 490f, 375f, textPaint)

        canvas.drawText("Arch Symmetry Scoring", 45f, 395f, textPaint)
        canvas.drawText("${safeReport.arch_symmetry_score.toInt()}%", 400f, 395f, textPaint)
        canvas.drawText(if (safeReport.arch_symmetry_score > 80) "Optimal" else "Asymmetric", 490f, 395f, textPaint)

        canvas.drawText("Root Angulation OPG Analysis", 45f, 415f, textPaint)
        canvas.drawText("${safeReport.root_angulation_score.toInt()}%", 400f, 415f, textPaint)
        canvas.drawText(if (safeReport.root_angulation_score > 75) "Parallel" else "Tipped", 490f, 415f, textPaint)

        paint.color = Color.parseColor("#D8DEE9")
        canvas.drawRect(40f, 430f, 555f, 431f, paint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OVERALL FINISHING SCORE:", 45f, 455f, textPaint)
        canvas.drawText("${overallScore.toInt()} / 100", 400f, 455f, textPaint)
        canvas.drawText(if (overallScore > 80) "READY TO DEBOND" else "ADDITIONAL DETAILING", 490f, 455f, textPaint)

        // 5. Clinical Recommendations
        textPaint.textSize = 14f
        textPaint.color = Color.parseColor("#0C1B33")
        canvas.drawText("CLINICAL RECOMMENDATIONS", 40f, 500f, textPaint)

        paint.color = Color.parseColor("#E5E9F0")
        canvas.drawRect(40f, 510f, 555f, 512f, paint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 10f
        textPaint.color = Color.BLACK

        var startY = 535f
        safeReport.recommendations.forEachIndexed { index, rec ->
            if (startY < 780f) {
                val formattedRec = if (rec.length > 80) rec.take(80) + "..." else rec
                canvas.drawText("${index + 1}. $formattedRec", 45f, startY, textPaint)
                startY += 20f
            }
        }

        // 6. Signatures and Disclaimers
        textPaint.textSize = 8f
        textPaint.color = Color.parseColor("#4C566A")
        canvas.drawText("Disclaimer: This orthodontic finishing assessment is generated with AI metrics assistance.", 40f, 800f, textPaint)
        canvas.drawText("Please cross-examine clinical records and adjust mechanics according to Andrews OGS criteria.", 40f, 812f, textPaint)

        pdfDocument.finishPage(page)

        // Write to Cache Dir
        val fileName = "OrthofinixAI_Report_${safePatient.name.replace(" ", "_")}.pdf"
        val pdfFile = File(context.cacheDir, fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()

            // Trigger standard Share Intent
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.example.orthofinixai.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "OrthofinixAI Clinical Report: ${safePatient.name}")
                putExtra(Intent.EXTRA_TEXT, "Here is the Orthodontic Finishing Assessment Report for patient ${safePatient.name} generated by OrthofinixAI.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Clinical Report"))
            Toast.makeText(context, "Report exported successfully!", Toast.LENGTH_LONG).show()

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error writing PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
