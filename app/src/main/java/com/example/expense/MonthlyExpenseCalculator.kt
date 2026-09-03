package com.example.expense

import com.example.data.CoffeeOrder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ShopExpenseBreakdown(
    val shopName: String,
    val totalSpent: Double,
    val orderCount: Int,
    val percentageOfTotal: Float
)

data class MonthlyExpenseSummary(
    val billingCycleName: String,
    val startDateFormatted: String,
    val endDateFormatted: String,
    val totalSpent: Double,
    val subtotalSpent: Double,
    val taxSpent: Double,
    val tipSpent: Double,
    val totalOrders: Int,
    val totalItems: Int,
    val averageOrderValue: Double,
    val dailyAverage: Double,
    val topShopName: String,
    val shopBreakdowns: List<ShopExpenseBreakdown>,
    val budgetAmount: Double = 150.0,
    val budgetUsedPercentage: Float = 0f
)

object MonthlyExpenseCalculator {

    fun calculateSummary(
        orders: List<CoffeeOrder>,
        billingCycleMonth: String,
        billingCycleStartDay: Int = 1,
        monthlyBudget: Double = 150.0
    ): MonthlyExpenseSummary {
        val totalSpent = orders.sumOf { it.totalAmount }
        val subtotalSpent = orders.sumOf { it.subtotal }
        val taxSpent = orders.sumOf { it.tax }
        val tipSpent = orders.sumOf { it.tip }
        val totalOrders = orders.size
        val totalItems = orders.sumOf { it.itemCount }
        val avgOrderValue = if (totalOrders > 0) totalSpent / totalOrders else 0.0

        // Shop breakdowns
        val groupedByShop = orders.groupBy { it.shopName }
        val breakdowns = groupedByShop.map { (shop, shopOrders) ->
            val spent = shopOrders.sumOf { it.totalAmount }
            val pct = if (totalSpent > 0) (spent / totalSpent).toFloat() else 0f
            ShopExpenseBreakdown(
                shopName = shop,
                totalSpent = spent,
                orderCount = shopOrders.size,
                percentageOfTotal = pct
            )
        }.sortedByDescending { it.totalSpent }

        val topShop = breakdowns.firstOrNull()?.shopName ?: "None"

        // Dates for billing cycle
        val cal = Calendar.getInstance()
        val parts = billingCycleMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1

        cal.set(year, month, billingCycleStartDay, 0, 0, 0)
        val startDate = cal.time

        // End date (1 month later minus 1 day)
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val endDate = cal.time

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val monthTitleFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        val cycleTitle = monthTitleFormat.format(startDate)

        val daysInCycle = ((endDate.time - startDate.time) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        val dailyAvg = totalSpent / daysInCycle

        val budgetPct = if (monthlyBudget > 0) ((totalSpent / monthlyBudget) * 100f).toFloat().coerceIn(0f, 200f) else 0f

        return MonthlyExpenseSummary(
            billingCycleName = cycleTitle,
            startDateFormatted = dateFormat.format(startDate),
            endDateFormatted = dateFormat.format(endDate),
            totalSpent = totalSpent,
            subtotalSpent = subtotalSpent,
            taxSpent = taxSpent,
            tipSpent = tipSpent,
            totalOrders = totalOrders,
            totalItems = totalItems,
            averageOrderValue = avgOrderValue,
            dailyAverage = dailyAvg,
            topShopName = topShop,
            shopBreakdowns = breakdowns,
            budgetAmount = monthlyBudget,
            budgetUsedPercentage = budgetPct
        )
    }
}
