package com.smsfinance.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.smsfinance.domain.model.Transaction
import com.smsfinance.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles exporting transactions to:
 * - Excel (.xlsx) via Apache POI
 * - PDF via iText
 *
 * Files are written to the app's cache directory and shared
 * via FileProvider so other apps (Gmail, WhatsApp, etc.) can receive them.
 */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    private val numFormat = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }

    // ── Excel Export ──────────────────────────────────────────────────────────

    fun exportToExcel(
        transactions: List<Transaction>,
        profileName: String = "My Account"
    ): Uri {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Transactions")

        // Styles
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.DARK_GREEN.index
            fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont().apply {
                bold = true
                color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
            }
            setFont(font)
        }
        val depositStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                color = org.apache.poi.ss.usermodel.IndexedColors.GREEN.index
                bold = true
            }
            setFont(font)
        }
        val withdrawalStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                color = org.apache.poi.ss.usermodel.IndexedColors.RED.index
                bold = true
            }
            setFont(font)
        }

        // Header row
        val headers = listOf("Date", "Type", "Source", "Amount (TZS)", "Description", "Reference")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, title ->
            headerRow.createCell(i).apply {
                setCellValue(title)
                cellStyle = headerStyle
            }
        }

        // Data rows
        var totalIncome = 0.0; var totalExpense = 0.0
        transactions.forEachIndexed { index, tx ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(dateFormat.format(Date(tx.date)))
            row.createCell(1).apply {
                setCellValue(tx.type.label)
                cellStyle = if (tx.type == TransactionType.DEPOSIT) depositStyle else withdrawalStyle
            }
            row.createCell(2).setCellValue(tx.source)
            row.createCell(3).apply {
                setCellValue(tx.amount)
                cellStyle = if (tx.type == TransactionType.DEPOSIT) depositStyle else withdrawalStyle
            }
            row.createCell(4).setCellValue(tx.description)
            row.createCell(5).setCellValue(tx.reference)

            if (tx.type == TransactionType.DEPOSIT) totalIncome += tx.amount
            else totalExpense += tx.amount
        }

        // Summary rows
        val summaryStart = transactions.size + 2
        sheet.createRow(summaryStart).apply {
            createCell(2).setCellValue("Total Income")
            createCell(3).setCellValue(totalIncome)
        }
        sheet.createRow(summaryStart + 1).apply {
            createCell(2).setCellValue("Total Expenses")
            createCell(3).setCellValue(totalExpense)
        }
        sheet.createRow(summaryStart + 2).apply {
            createCell(2).setCellValue("Net Balance")
            createCell(3).setCellValue(totalIncome - totalExpense)
        }

        // Auto-size columns
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        // Write to file
        val fileName = "SmartMoney_${profileName.replace(" ", "_")}_${
            SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        }.xlsx"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()

        return getUriForFile(file)
    }

    // ── PDF Export ────────────────────────────────────────────────────────────

    fun exportToPdf(
        transactions: List<Transaction>,
        profileName: String = "My Account"
    ): Uri {
        val fileName = "SmartMoney_${profileName.replace(" ", "_")}_${
            SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        }.pdf"
        val file = File(context.cacheDir, fileName)

        val document = Document(PageSize.A4.rotate())
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()

        // ── Title ─────────────────────────────────────────────────────────────
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, BaseColor(0, 200, 83))
        val subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11f, BaseColor.GRAY)
        val normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9f)
        val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f)
        val greenFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, BaseColor(0, 150, 63))
        val redFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, BaseColor(200, 0, 0))

        document.add(Paragraph("Smart Money", titleFont).apply { spacingAfter = 4f })
        document.add(Paragraph("Transaction Report — $profileName", subtitleFont).apply { spacingAfter = 2f })
        document.add(Paragraph("Generated: ${dateFormat.format(Date())}", subtitleFont).apply { spacingAfter = 16f })

        // ── Summary box ───────────────────────────────────────────────────────
        val totalIncome = transactions.filter { it.type == TransactionType.DEPOSIT }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.WITHDRAWAL }.sumOf { it.amount }
        val net = totalIncome - totalExpense

        val summaryTable = PdfPTable(3).apply {
            widthPercentage = 100f
            spacingAfter = 16f
            setWidths(floatArrayOf(1f, 1f, 1f))
        }
        fun summaryCell(label: String, value: String, valueFont: com.itextpdf.text.Font) = PdfPCell().apply {
            addElement(Paragraph(label, subtitleFont))
            addElement(Paragraph(value, valueFont))
            borderColor = BaseColor.LIGHT_GRAY
            setPadding(10f)
        }
        summaryTable.addCell(summaryCell("Total Income", "TZS ${numFormat.format(totalIncome)}",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BaseColor(0, 150, 63))))
        summaryTable.addCell(summaryCell("Total Expenses", "TZS ${numFormat.format(totalExpense)}",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, BaseColor(200, 0, 0))))
        summaryTable.addCell(summaryCell("Net Balance", "TZS ${numFormat.format(net)}",
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f,
                if (net >= 0) BaseColor(0, 150, 63) else BaseColor(200, 0, 0))))
        document.add(summaryTable)

        // ── Transactions table ────────────────────────────────────────────────
        val table = PdfPTable(5).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(2f, 1.2f, 2f, 1.5f, 2.5f))
        }
        fun headerCell(text: String) = PdfPCell(Phrase(text, FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 9f, BaseColor.WHITE))).apply {
            backgroundColor = BaseColor(0, 150, 63)
            setPadding(6f)
            horizontalAlignment = Element.ALIGN_CENTER
        }
        listOf("Date", "Type", "Source", "Amount (TZS)", "Description")
            .forEach { table.addCell(headerCell(it)) }

        transactions.forEach { tx ->
            val isDeposit = tx.type == TransactionType.DEPOSIT
            val amtFont = if (isDeposit) greenFont else redFont
            val sign = if (isDeposit) "+" else "-"

            fun dataCell(text: String, f: com.itextpdf.text.Font = normalFont, align: Int = Element.ALIGN_LEFT) =
                PdfPCell(Phrase(text, f)).apply { setPadding(5f); horizontalAlignment = align }

            table.addCell(dataCell(dateFormat.format(Date(tx.date))))
            table.addCell(dataCell(tx.type.label, amtFont, Element.ALIGN_CENTER))
            table.addCell(dataCell(tx.source))
            table.addCell(dataCell("$sign ${numFormat.format(tx.amount)}", amtFont, Element.ALIGN_RIGHT))
            table.addCell(dataCell(tx.description.take(40)))
        }
        document.add(table)
        document.add(Paragraph("\n${transactions.size} transactions | Report generated by Smart Money",
            subtitleFont).apply { spacingBefore = 10f })
        document.close()

        return getUriForFile(file)
    }

    // ── Share ──────────────────────────────────────────────────────────────────

    fun shareFile(uri: Uri, mimeType: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun getUriForFile(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.file provider", file)
}