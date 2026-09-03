package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Coffee Tracker", appName)
  }

  @Test
  fun `verify monthly expense calculation`() {
    val testOrders = listOf(
      com.example.data.CoffeeOrder(
        shopName = "Blue Bottle Coffee",
        orderItemsSummary = "2x Oat Latte",
        itemCount = 2,
        subtotal = 12.50,
        tax = 1.06,
        tip = 2.00,
        totalAmount = 15.56,
        orderTimestamp = 1756789000000L,
        billingCycleMonth = "2026-09"
      )
    )
    val summary = com.example.expense.MonthlyExpenseCalculator.calculateSummary(testOrders, "2026-09")
    assertEquals(15.56, summary.totalSpent, 0.01)
    assertEquals(1, summary.totalOrders)
    assertEquals("Blue Bottle Coffee", summary.topShopName)
  }
}
