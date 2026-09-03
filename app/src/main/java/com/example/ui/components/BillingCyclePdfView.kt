package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CoffeeOrder
import com.example.expense.MonthlyExpenseSummary
import com.example.ui.theme.AmberRoast
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.SheetGreen
import java.io.File
import java.util.Locale

@Composable
fun BillingCyclePdfView(
    summary: MonthlyExpenseSummary,
    orders: List<CoffeeOrder>,
    lastPdfFile: File?,
    billingCycleStartDay: Int,
    monthlyBudget: Double,
    onDownloadPdf: () -> Unit,
    onViewPdf: () -> Unit,
    onSendSummary: () -> Unit,
    onUpdateCycleSettings: (Int, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PDF Summary Executive Header & Action Buttons
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F), // PDF Red
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Billing Cycle Statement",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "${summary.billingCycleName} • End of Cycle Summary",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showSettingsDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cycle Day $billingCycleStartDay", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Download and Send Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDownloadPdf,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("download_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EspressoBrown),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download PDF", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onSendSummary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_summary_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SheetGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Summary", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (lastPdfFile != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SheetGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SheetGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Saved: ${lastPdfFile.name}", fontSize = 11.sp, color = SheetGreen)
                            }
                            OutlinedButton(
                                onClick = onViewPdf,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Live Mockup Preview of the Actual PDF Document
        item {
            Text(
                text = "Document Layout Preview",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // White Paper Sheet Simulation
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // PDF Top Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3E2723))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "COFFEE EXPENSE STATEMENT",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Period: ${summary.startDateFormatted} - ${summary.endDateFormatted}",
                                    color = Color(0xFFD7CCC8),
                                    fontSize = 10.sp
                                )
                            }

                            Text(
                                text = summary.billingCycleName.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // PDF Gold Stripe
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color(0xFFD97706))
                    )

                    Column(modifier = Modifier.padding(14.dp)) {
                        // KPI Preview Boxes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PdfMiniCard("TOTAL SPENT", String.format(Locale.US, "$%.2f", summary.totalSpent), Color(0xFF1B8754), Modifier.weight(1f))
                            PdfMiniCard("ORDERS", "${summary.totalOrders} calls", Color(0xFF3E2723), Modifier.weight(1f))
                            PdfMiniCard("AVG / ORDER", String.format(Locale.US, "$%.2f", summary.averageOrderValue), Color(0xFF4E342E), Modifier.weight(1f))
                            PdfMiniCard("FAVORITE SPOT", summary.topShopName.take(10), Color(0xFFB45309), Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Table Preview Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF4E342E), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DATE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("COFFEE SHOP", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("ITEMS", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("TOTAL", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Orders Preview Rows
                        orders.take(5).forEachIndexed { idx, order ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) Color(0xFFFAF7F5) else Color.White)
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(order.formattedDate, fontSize = 9.sp, color = Color.Black)
                                Text(order.shopName.take(14), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                Text(order.orderItemsSummary.take(18), fontSize = 9.sp, color = Color.DarkGray)
                                Text(String.format(Locale.US, "$%.2f", order.totalAmount), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B8754))
                            }
                        }

                        if (orders.size > 5) {
                            Text(
                                text = "+ ${orders.size - 5} additional itemized calls in full PDF export...",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Grand Total Box in PDF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "100% Offline & Private Local Document",
                                fontSize = 8.sp,
                                color = Color.Gray
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GRAND TOTAL: ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3E2723)
                                )
                                Text(
                                    text = String.format(Locale.US, "$%.2f", summary.totalSpent),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B8754)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        BillingSettingsDialog(
            currentDay = billingCycleStartDay,
            currentBudget = monthlyBudget,
            onDismiss = { showSettingsDialog = false },
            onSave = { day, budget ->
                onUpdateCycleSettings(day, budget)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
private fun PdfMiniCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFFAF7F5), RoundedCornerShape(4.dp))
            .border(0.5.dp, Color(0xFFE0D6CE), RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        Column {
            Text(title, fontSize = 7.sp, color = Color(0xFF6D5D55), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 10.sp, color = accent, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun BillingSettingsDialog(
    currentDay: Int,
    currentBudget: Double,
    onDismiss: () -> Unit,
    onSave: (Int, Double) -> Unit
) {
    var dayInput by remember { mutableStateOf(currentDay.toString()) }
    var budgetInput by remember { mutableStateOf(String.format(Locale.US, "%.0f", currentBudget)) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Billing Cycle Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Configure your coffee expense statement cycle day and budget limit:",
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = dayInput,
                    onValueChange = { dayInput = it },
                    label = { Text("Cycle Start Day (1-28)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Monthly Coffee Budget ($)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val day = dayInput.toIntOrNull() ?: currentDay
                    val budget = budgetInput.toDoubleOrNull() ?: currentBudget
                    onSave(day, budget)
                }
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
