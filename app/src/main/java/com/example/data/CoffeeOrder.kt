package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "coffee_orders")
data class CoffeeOrder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shopName: String,
    val phoneNumber: String = "",
    val orderItemsSummary: String,
    val itemCount: Int = 1,
    val unitPriceEstimate: Double = 0.0,
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val tip: Double = 0.0,
    val totalAmount: Double = 0.0,
    val orderTimestamp: Long = System.currentTimeMillis(),
    val billingCycleMonth: String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date()),
    val audioFilePath: String? = null,
    val audioDurationSeconds: Int = 0,
    val transcriptOrNotes: String = "",
    val orderStatus: String = "Confirmed", // "Recorded", "Confirmed", "Draft"
    val isPaid: Boolean = true
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(orderTimestamp))

    val formattedTime: String
        get() = SimpleDateFormat("hh:mm a", Locale.US).format(Date(orderTimestamp))

    val formattedDateTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(orderTimestamp))
}
