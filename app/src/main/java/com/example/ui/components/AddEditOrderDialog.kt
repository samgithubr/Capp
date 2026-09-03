package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CoffeeOrder
import com.example.ui.theme.SheetGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddEditOrderDialog(
    order: CoffeeOrder?,
    currentBillingMonth: String,
    onDismiss: () -> Unit,
    onSave: (CoffeeOrder) -> Unit,
    onDelete: ((CoffeeOrder) -> Unit)? = null
) {
    var shopName by remember { mutableStateOf(order?.shopName ?: "") }
    var phoneNumber by remember { mutableStateOf(order?.phoneNumber ?: "") }
    var orderItems by remember { mutableStateOf(order?.orderItemsSummary ?: "") }
    var subtotalStr by remember { mutableStateOf(order?.let { String.format(Locale.US, "%.2f", it.subtotal) } ?: "12.50") }
    var taxStr by remember { mutableStateOf(order?.let { String.format(Locale.US, "%.2f", it.tax) } ?: "1.06") }
    var tipStr by remember { mutableStateOf(order?.let { String.format(Locale.US, "%.2f", it.tip) } ?: "2.00") }
    var notes by remember { mutableStateOf(order?.transcriptOrNotes ?: "") }

    val subtotal = subtotalStr.toDoubleOrNull() ?: 0.0
    val tax = taxStr.toDoubleOrNull() ?: 0.0
    val tip = tipStr.toDoubleOrNull() ?: 0.0
    val calculatedTotal = Math.round((subtotal + tax + tip) * 100.0) / 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (order == null) "New Coffee Order" else "Edit Order #${order.id}",
                    fontSize = 18.sp
                )
                if (order != null && onDelete != null) {
                    IconButton(onClick = { onDelete(order) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete order",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Coffee Shop Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_shop_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = orderItems,
                    onValueChange = { orderItems = it },
                    label = { Text("Items Ordered") },
                    placeholder = { Text("e.g. 2x Oat Latte, 1x Croissant") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_order_items_input"),
                    singleLine = false,
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = subtotalStr,
                        onValueChange = { subtotalStr = it },
                        label = { Text("Subtotal ($)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = taxStr,
                        onValueChange = { taxStr = it },
                        label = { Text("Tax ($)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tipStr,
                        onValueChange = { tipStr = it },
                        label = { Text("Tip ($)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Calculated Grand Total:", fontSize = 13.sp)
                    Text(
                        text = String.format(Locale.US, "$%.2f", calculatedTotal),
                        fontSize = 17.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = SheetGreen
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Call Recording Transcript") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (shopName.isNotBlank() && orderItems.isNotBlank()) {
                        val savedOrder = order?.copy(
                            shopName = shopName,
                            phoneNumber = phoneNumber,
                            orderItemsSummary = orderItems,
                            subtotal = subtotal,
                            tax = tax,
                            tip = tip,
                            totalAmount = calculatedTotal,
                            transcriptOrNotes = notes
                        ) ?: CoffeeOrder(
                            shopName = shopName,
                            phoneNumber = phoneNumber,
                            orderItemsSummary = orderItems,
                            subtotal = subtotal,
                            tax = tax,
                            tip = tip,
                            totalAmount = calculatedTotal,
                            orderTimestamp = System.currentTimeMillis(),
                            billingCycleMonth = currentBillingMonth,
                            transcriptOrNotes = notes
                        )
                        onSave(savedOrder)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SheetGreen),
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("Save to Sheet")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
