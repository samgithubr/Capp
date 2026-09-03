package com.example.sheet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.CoffeeOrder
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SpreadsheetExporter {

    fun exportToCsvFile(context: Context, orders: List<CoffeeOrder>, billingMonth: String): File? {
        return try {
            val fileName = "Coffee_Orders_Google_Sheet_${billingMonth.replace("-", "_")}.csv"
            
            // Prefer public Downloads folder if accessible or external files dir
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val file = File(baseDir, fileName)

            FileWriter(file).use { writer ->
                // Google Sheets CSV Header Row
                writer.append("Order ID,Date,Time,Coffee Shop,Phone Number,Order Items,Items Count,Subtotal ($),Tax ($),Tip ($),Total Amount ($),Audio Duration (s),Audio File,Notes,Status\n")

                var subtotalSum = 0.0
                var taxSum = 0.0
                var tipSum = 0.0
                var grandTotalSum = 0.0

                orders.forEach { order ->
                    subtotalSum += order.subtotal
                    taxSum += order.tax
                    tipSum += order.tip
                    grandTotalSum += order.totalAmount

                    val escapedShop = escapeCsv(order.shopName)
                    val escapedPhone = escapeCsv(order.phoneNumber)
                    val escapedItems = escapeCsv(order.orderItemsSummary)
                    val escapedNotes = escapeCsv(order.transcriptOrNotes)
                    val audioFileName = order.audioFilePath?.let { File(it).name } ?: "None"

                    writer.append("${order.id},")
                    writer.append("${order.formattedDate},")
                    writer.append("${order.formattedTime},")
                    writer.append("$escapedShop,")
                    writer.append("$escapedPhone,")
                    writer.append("$escapedItems,")
                    writer.append("${order.itemCount},")
                    writer.append(String.format(Locale.US, "%.2f", order.subtotal) + ",")
                    writer.append(String.format(Locale.US, "%.2f", order.tax) + ",")
                    writer.append(String.format(Locale.US, "%.2f", order.tip) + ",")
                    writer.append(String.format(Locale.US, "%.2f", order.totalAmount) + ",")
                    writer.append("${order.audioDurationSeconds},")
                    writer.append("$audioFileName,")
                    writer.append("$escapedNotes,")
                    writer.append("${order.orderStatus}\n")
                }

                // Summary Formulas / Totals row for Google Sheets compatibility
                val nextRow = orders.size + 1
                writer.append("TOTALS,,,,,TOTAL ORDERS: ${orders.size},,")
                writer.append(String.format(Locale.US, "%.2f", subtotalSum) + ",")
                writer.append(String.format(Locale.US, "%.2f", taxSum) + ",")
                writer.append(String.format(Locale.US, "%.2f", tipSum) + ",")
                writer.append(String.format(Locale.US, "%.2f", grandTotalSum) + ",,,\n")
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openInGoogleSheets(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open in Google Sheets / Spreadsheet App")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open spreadsheet: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareCsvFile(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Coffee Order Google Sheet (${file.name})")
                putExtra(Intent.EXTRA_TEXT, "Attached is the offline coffee orders spreadsheet log for Google Sheets.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share Coffee Sheet CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun copySheetToClipboard(context: Context, orders: List<CoffeeOrder>) {
        val sb = StringBuilder()
        sb.append("Order ID\tDate\tTime\tCoffee Shop\tItems\tSubtotal\tTax\tTip\tTotal\tStatus\n")
        orders.forEach { order ->
            sb.append("${order.id}\t")
            sb.append("${order.formattedDate}\t")
            sb.append("${order.formattedTime}\t")
            sb.append("${order.shopName}\t")
            sb.append("${order.orderItemsSummary}\t")
            sb.append("$${String.format(Locale.US, "%.2f", order.subtotal)}\t")
            sb.append("$${String.format(Locale.US, "%.2f", order.tax)}\t")
            sb.append("$${String.format(Locale.US, "%.2f", order.tip)}\t")
            sb.append("$${String.format(Locale.US, "%.2f", order.totalAmount)}\t")
            sb.append("${order.orderStatus}\n")
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Coffee Orders Sheet", sb.toString())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied table! Ready to paste into Google Sheets", Toast.LENGTH_SHORT).show()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
