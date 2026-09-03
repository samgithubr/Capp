package com.example.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.CoffeeOrder
import com.example.expense.MonthlyExpenseCalculator
import com.example.expense.MonthlyExpenseSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CoffeeExpensePdfGenerator {

    // Standard A4 dimensions in points (72 points = 1 inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun generateBillingPdf(
        context: Context,
        orders: List<CoffeeOrder>,
        billingMonth: String,
        billingCycleStartDay: Int = 1,
        monthlyBudget: Double = 150.0
    ): File? {
        val summary = MonthlyExpenseCalculator.calculateSummary(
            orders,
            billingMonth,
            billingCycleStartDay,
            monthlyBudget
        )

        val pdfDocument = PdfDocument()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Draw Header
        drawHeader(canvas, paint, summary)

        // Draw KPI Cards
        drawKpiCards(canvas, paint, summary)

        // Draw Table Header
        var currentY = 220f
        currentY = drawTableHeader(canvas, paint, currentY)

        // Draw Rows
        val itemsPerPage = 12
        var itemsOnCurrentPage = 0

        for (i in orders.indices) {
            val order = orders[i]

            // Check if page break needed
            if (currentY > 740f || itemsOnCurrentPage >= itemsPerPage) {
                drawFooter(canvas, paint, pageNumber)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                // Continued header on next page
                currentY = 40f
                paint.color = Color.parseColor("#3E2723")
                paint.textSize = 14f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Coffee Expense Statement (Page $pageNumber continued)", 30f, currentY, paint)
                currentY += 20f

                currentY = drawTableHeader(canvas, paint, currentY)
                itemsOnCurrentPage = 0
            }

            currentY = drawOrderRow(canvas, paint, order, i + 1, currentY)
            itemsOnCurrentPage++
        }

        // Draw Financial Totals Box
        if (currentY > 670f) {
            drawFooter(canvas, paint, pageNumber)
            pdfDocument.finishPage(page)

            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = 50f
        }

        drawTotalsAndBreakdown(canvas, paint, summary, currentY + 15f)

        // Draw Footer on last page
        drawFooter(canvas, paint, pageNumber)
        pdfDocument.finishPage(page)

        // Write to file
        return try {
            val fileName = "Coffee_Expense_Statement_${billingMonth.replace("-", "_")}.pdf"
            val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val pdfFile = File(documentsDir, fileName)

            FileOutputStream(pdfFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            // Also copy to MediaStore Downloads if Android Q+ so user finds it immediately
            saveToDownloadsIfPossible(context, pdfFile, fileName)

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawHeader(canvas: Canvas, paint: Paint, summary: MonthlyExpenseSummary) {
        // Top banner background
        paint.color = Color.parseColor("#3E2723") // Rich Espresso
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        // Gold accent stripe
        paint.color = Color.parseColor("#D97706") // Coffee Amber
        canvas.drawRect(0f, 95f, PAGE_WIDTH.toFloat(), 98f, paint)

        // Brand Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("COFFEE EXPENSE STATEMENT", 30f, 40f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#D7CCC8")
        canvas.drawText("Billing Cycle: ${summary.startDateFormatted} - ${summary.endDateFormatted}", 30f, 60f, paint)
        canvas.drawText("Offline & Private Local Record • Zero Cloud Subscription", 30f, 75f, paint)

        // Statement Date & Account on right
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(summary.billingCycleName.uppercase(), (PAGE_WIDTH - 30).toFloat(), 40f, paint)

        val issueDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#D7CCC8")
        canvas.drawText("Issued: $issueDate", (PAGE_WIDTH - 30).toFloat(), 60f, paint)
        canvas.drawText("Status: Finalized", (PAGE_WIDTH - 30).toFloat(), 75f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawKpiCards(canvas: Canvas, paint: Paint, summary: MonthlyExpenseSummary) {
        val cardWidth = 120f
        val cardHeight = 65f
        val startX = 30f
        val startY = 115f
        val gap = 16f

        val kpiList = listOf(
            Triple("TOTAL SPENT", String.format(Locale.US, "$%.2f", summary.totalSpent), "#1B8754"),
            Triple("TOTAL ORDERS", "${summary.totalOrders} calls", "#3E2723"),
            Triple("AVG PER ORDER", String.format(Locale.US, "$%.2f", summary.averageOrderValue), "#4E342E"),
            Triple("TOP CAFE", summary.topShopName.take(13), "#B45309")
        )

        for (i in kpiList.indices) {
            val (title, value, accentColor) = kpiList[i]
            val x = startX + i * (cardWidth + gap)
            val rect = RectF(x, startY, x + cardWidth, startY + cardHeight)

            // Card background
            paint.color = Color.parseColor("#FAF7F5")
            canvas.drawRoundRect(rect, 8f, 8f, paint)

            // Card border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.parseColor("#E0D6CE")
            canvas.drawRoundRect(rect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            // Top accent indicator
            paint.color = Color.parseColor(accentColor)
            canvas.drawRoundRect(RectF(x, startY, x + cardWidth, startY + 4f), 2f, 2f, paint)

            // KPI Title
            paint.color = Color.parseColor("#6D5D55")
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(title, x + 10f, startY + 22f, paint)

            // KPI Value
            paint.color = Color.parseColor(accentColor)
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(value, x + 10f, startY + 46f, paint)
        }
    }

    private fun drawTableHeader(canvas: Canvas, paint: Paint, y: Float): Float {
        val rect = RectF(30f, y, (PAGE_WIDTH - 30).toFloat(), y + 24f)
        paint.color = Color.parseColor("#4E342E") // Coffee Roast
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val headerY = y + 16f
        canvas.drawText("#", 38f, headerY, paint)
        canvas.drawText("DATE & TIME", 60f, headerY, paint)
        canvas.drawText("COFFEE SHOP", 155f, headerY, paint)
        canvas.drawText("ORDER ITEMS", 280f, headerY, paint)
        canvas.drawText("QTY", 450f, headerY, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("TOTAL ($)", (PAGE_WIDTH - 40).toFloat(), headerY, paint)
        paint.textAlign = Paint.Align.LEFT

        return y + 28f
    }

    private fun drawOrderRow(
        canvas: Canvas,
        paint: Paint,
        order: CoffeeOrder,
        index: Int,
        y: Float
    ): Float {
        val rowHeight = 22f
        val rect = RectF(30f, y, (PAGE_WIDTH - 30).toFloat(), y + rowHeight)

        // Alternating row background
        if (index % 2 == 0) {
            paint.color = Color.parseColor("#FAF6F2")
            canvas.drawRect(rect, paint)
        }

        // Bottom border line
        paint.color = Color.parseColor("#EDE3DB")
        canvas.drawLine(30f, y + rowHeight, (PAGE_WIDTH - 30).toFloat(), y + rowHeight, paint)

        val textY = y + 15f
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#2E201C")

        // Index
        canvas.drawText(index.toString(), 38f, textY, paint)

        // Date
        val dateText = "${order.formattedDate} ${order.formattedTime}"
        canvas.drawText(dateText, 60f, textY, paint)

        // Shop Name
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(order.shopName.take(18), 155f, textY, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // Items Summary
        canvas.drawText(order.orderItemsSummary.take(28), 280f, textY, paint)

        // Qty
        canvas.drawText("${order.itemCount}", 455f, textY, paint)

        // Total
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#1B8754")
        canvas.drawText(String.format(Locale.US, "$%.2f", order.totalAmount), (PAGE_WIDTH - 40).toFloat(), textY, paint)
        paint.textAlign = Paint.Align.LEFT

        return y + rowHeight
    }

    private fun drawTotalsAndBreakdown(
        canvas: Canvas,
        paint: Paint,
        summary: MonthlyExpenseSummary,
        y: Float
    ) {
        // Left Box: Shop Breakdown
        val leftRect = RectF(30f, y, 320f, y + 100f)
        paint.color = Color.parseColor("#F7F2EE")
        canvas.drawRoundRect(leftRect, 6f, 6f, paint)

        paint.color = Color.parseColor("#3E2723")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SPENDING BY COFFEE SHOP", 42f, y + 20f, paint)

        var shopY = y + 36f
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        summary.shopBreakdowns.take(3).forEach { shop ->
            paint.color = Color.parseColor("#4A3731")
            canvas.drawText(shop.shopName.take(22), 42f, shopY, paint)

            paint.textAlign = Paint.Align.RIGHT
            val pct = (shop.percentageOfTotal * 100).toInt()
            val text = String.format(Locale.US, "$%.2f (%d%%)", shop.totalSpent, pct)
            canvas.drawText(text, 305f, shopY, paint)
            paint.textAlign = Paint.Align.LEFT
            shopY += 16f
        }

        // Right Box: Financial Totals Table
        val rightRect = RectF(340f, y, (PAGE_WIDTH - 30).toFloat(), y + 100f)
        paint.color = Color.parseColor("#F7F2EE")
        canvas.drawRoundRect(rightRect, 6f, 6f, paint)

        val rightX = 352f
        val rightValX = (PAGE_WIDTH - 42).toFloat()
        var lineY = y + 20f

        val finLines = listOf(
            Pair("Subtotal", String.format(Locale.US, "$%.2f", summary.subtotalSpent)),
            Pair("Estimated Tax (8.5%)", String.format(Locale.US, "$%.2f", summary.taxSpent)),
            Pair("Barista Tips", String.format(Locale.US, "$%.2f", summary.tipSpent))
        )

        finLines.forEach { (label, value) ->
            paint.color = Color.parseColor("#5A4842")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(label, rightX, lineY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(value, rightValX, lineY, paint)
            paint.textAlign = Paint.Align.LEFT
            lineY += 16f
        }

        // Grand Total Line
        paint.color = Color.parseColor("#B45309")
        canvas.drawLine(rightX, lineY - 4f, rightValX, lineY - 4f, paint)

        paint.color = Color.parseColor("#1B8754")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("GRAND TOTAL", rightX, lineY + 12f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(String.format(Locale.US, "$%.2f", summary.totalSpent), rightValX, lineY + 12f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawFooter(canvas: Canvas, paint: Paint, pageNumber: Int) {
        val y = PAGE_HEIGHT - 35f
        paint.color = Color.parseColor("#D7CCC8")
        canvas.drawLine(30f, y, (PAGE_WIDTH - 30).toFloat(), y, paint)

        paint.color = Color.parseColor("#8D6E63")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Coffee Tracker • Completely Free, Private & Offline • Android 10+", 30f, y + 18f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page $pageNumber", (PAGE_WIDTH - 30).toFloat(), y + 18f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun saveToDownloadsIfPossible(context: Context, sourceFile: File, displayName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CoffeeTracker")
                }

                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            } catch (e: Exception) {
                // non-fatal, file is still in app storage
            }
        }
    }

    fun viewPdf(context: Context, pdfFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Coffee Statement PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePdf(context: Context, pdfFile: File, summary: MonthlyExpenseSummary) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareText = """
                ☕ Coffee Expense Statement (${summary.billingCycleName})
                • Total Spent: $${String.format(Locale.US, "%.2f", summary.totalSpent)}
                • Total Orders: ${summary.totalOrders} calls
                • Favorite Spot: ${summary.topShopName}
                • Period: ${summary.startDateFormatted} - ${summary.endDateFormatted}
                
                Attached is the PDF statement generated locally with Coffee Tracker.
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Coffee Expense Statement - ${summary.billingCycleName}")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Send Monthly Coffee Summary")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
