package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CoffeeOrder
import com.example.ui.theme.SheetGreen
import java.util.Locale

@Composable
fun SpreadsheetGridView(
    orders: List<CoffeeOrder>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOrderClick: (CoffeeOrder) -> Unit,
    onAddOrderClick: () -> Unit,
    onExportCsv: () -> Unit,
    onCopyTable: () -> Unit,
    onPlayAudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isGridView by remember { mutableStateOf(true) }
    val totalAmount = orders.sumOf { it.totalAmount }
    val formulaText = remember(orders) {
        if (orders.isEmpty()) "=SUM(0)" else "=SUM(I2:I${orders.size + 1}) → \$${String.format(Locale.US, "%.2f", totalAmount)}"
    }

    Column(modifier = modifier) {
        // Search & Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_orders_input"),
                placeholder = { Text("Search coffee shop, item...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SheetGreen,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Toggle Grid / List View
            IconButton(
                onClick = { isGridView = !isGridView },
                modifier = Modifier.testTag("toggle_view_button")
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Default.FormatListBulleted else Icons.Default.GridOn,
                    contentDescription = "Toggle View",
                    tint = SheetGreen
                )
            }

            // Export to CSV / Google Sheets
            IconButton(
                onClick = onExportCsv,
                modifier = Modifier.testTag("export_csv_button")
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Open in Google Sheets",
                    tint = SheetGreen
                )
            }
        }

        // Google Sheets Formula Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(SheetGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = "fx",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "fx",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = formulaText,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action Pill Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCopyTable,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("copy_sheet_table_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy for Sheets", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onExportCsv,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("download_csv_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export .CSV", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onAddOrderClick,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_order_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = null,
                        tint = SheetGreen.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Coffee Orders in this Sheet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Call a coffee shop or record an order to populate the spreadsheet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (isGridView) {
            // Interactive Horizontal Scrolling Google Sheets Table
            val horizontalScrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Spreadsheet Header
                Row(
                    modifier = Modifier
                        .background(SheetGreen, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SheetHeaderCell("A", "ID", 45.dp)
                    SheetHeaderCell("B", "Date & Time", 125.dp)
                    SheetHeaderCell("C", "Coffee Shop", 140.dp)
                    SheetHeaderCell("D", "Items Ordered", 180.dp)
                    SheetHeaderCell("E", "Qty", 45.dp)
                    SheetHeaderCell("F", "Subtotal", 75.dp)
                    SheetHeaderCell("G", "Tax", 65.dp)
                    SheetHeaderCell("H", "Tip", 65.dp)
                    SheetHeaderCell("I", "Total ($)", 85.dp)
                    SheetHeaderCell("J", "Audio", 65.dp)
                    SheetHeaderCell("K", "Status", 80.dp)
                }

                // Table Rows
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                ) {
                    itemsIndexed(orders) { index, order ->
                        val isEven = index % 2 == 0
                        val rowBg = if (isEven) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .clickable { onOrderClick(order) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SheetDataCell("${order.id}", 45.dp, isMonospace = true)
                            SheetDataCell("${order.formattedDate} ${order.formattedTime}", 125.dp)
                            SheetDataCell(order.shopName, 140.dp, isBold = true)
                            SheetDataCell(order.orderItemsSummary, 180.dp)
                            SheetDataCell("${order.itemCount}", 45.dp, alignCenter = true)
                            SheetDataCell(String.format(Locale.US, "$%.2f", order.subtotal), 75.dp, isMonospace = true)
                            SheetDataCell(String.format(Locale.US, "$%.2f", order.tax), 65.dp, isMonospace = true)
                            SheetDataCell(String.format(Locale.US, "$%.2f", order.tip), 65.dp, isMonospace = true)
                            SheetDataCell(
                                text = String.format(Locale.US, "$%.2f", order.totalAmount),
                                width = 85.dp,
                                isBold = true,
                                isMonospace = true,
                                textColor = SheetGreen
                            )

                            // Audio cell with play button if recording exists
                            Box(
                                modifier = Modifier
                                    .width(65.dp)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (order.audioFilePath != null) {
                                    IconButton(
                                        onClick = { onPlayAudio(order.audioFilePath) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play recording",
                                            tint = SheetGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    Text("—", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            SheetDataCell(order.orderStatus, 80.dp, isTag = true)
                        }
                    }

                    // Total Bottom Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SheetGreen.copy(alpha = 0.12f))
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SheetDataCell("∑", 45.dp, isBold = true)
                            SheetDataCell("TOTALS (${orders.size} Orders)", 265.dp, isBold = true)
                            SheetDataCell("${orders.sumOf { it.itemCount }}", 45.dp, isBold = true, alignCenter = true)
                            SheetDataCell(String.format(Locale.US, "$%.2f", orders.sumOf { it.subtotal }), 75.dp, isBold = true, isMonospace = true)
                            SheetDataCell(String.format(Locale.US, "$%.2f", orders.sumOf { it.tax }), 65.dp, isBold = true, isMonospace = true)
                            SheetDataCell(String.format(Locale.US, "$%.2f", orders.sumOf { it.tip }), 65.dp, isBold = true, isMonospace = true)
                            SheetDataCell(
                                text = String.format(Locale.US, "$%.2f", totalAmount),
                                width = 85.dp,
                                isBold = true,
                                isMonospace = true,
                                textColor = SheetGreen
                            )
                            SheetDataCell("", 65.dp)
                            SheetDataCell("", 80.dp)
                        }
                    }
                }
            }
        } else {
            // Card List View
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(orders) { _, order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOrderClick(order) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = order.shopName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${order.formattedDate} • ${order.formattedTime}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = String.format(Locale.US, "$%.2f", order.totalAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = SheetGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = order.orderItemsSummary,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (order.transcriptOrNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Notes: ${order.transcriptOrNotes}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeaderCell(columnLetter: String, title: String, width: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = columnLetter,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SheetDataCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isBold: Boolean = false,
    isMonospace: Boolean = false,
    alignCenter: Boolean = false,
    textColor: Color = Color.Unspecified,
    isTag: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart
    ) {
        if (isTag) {
            Box(
                modifier = Modifier
                    .background(SheetGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = text,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SheetGreen
                )
            }
        } else {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
