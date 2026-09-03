package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(entities = [CoffeeOrder::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coffeeOrderDao(): CoffeeOrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coffee_tracker_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialOrders(database.coffeeOrderDao())
                    }
                }
            }

            private suspend fun populateInitialOrders(dao: CoffeeOrderDao) {
                val cal = Calendar.getInstance()
                val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
                val currentMonth = monthFormat.format(cal.time)

                // Current month orders
                cal.set(Calendar.HOUR_OF_DAY, 8)
                cal.set(Calendar.MINUTE, 30)

                val sampleOrders = listOf(
                    CoffeeOrder(
                        shopName = "Blue Bottle Coffee",
                        phoneNumber = "+1 (555) 432-8765",
                        orderItemsSummary = "2x Iced New Orleans Cold Brew, 1x Almond Croissant",
                        itemCount = 3,
                        unitPriceEstimate = 6.25,
                        subtotal = 17.50,
                        tax = 1.62,
                        tip = 3.00,
                        totalAmount = 22.12,
                        orderTimestamp = cal.timeInMillis - (1L * 24 * 60 * 60 * 1000), // Yesterday
                        billingCycleMonth = currentMonth,
                        audioFilePath = null,
                        audioDurationSeconds = 48,
                        transcriptOrNotes = "Pickup at side counter. Asked for extra chicory & oat milk.",
                        orderStatus = "Confirmed",
                        isPaid = true
                    ),
                    CoffeeOrder(
                        shopName = "Philz Coffee",
                        phoneNumber = "+1 (555) 789-0123",
                        orderItemsSummary = "1x Mint Mojito Iced Coffee (Sweet & Creamy)",
                        itemCount = 1,
                        unitPriceEstimate = 6.75,
                        subtotal = 6.75,
                        tax = 0.62,
                        tip = 1.50,
                        totalAmount = 8.87,
                        orderTimestamp = cal.timeInMillis - (3L * 24 * 60 * 60 * 1000),
                        billingCycleMonth = currentMonth,
                        audioFilePath = null,
                        audioDurationSeconds = 34,
                        transcriptOrNotes = "Pre-paid over the phone. Extra mint sprigs requested.",
                        orderStatus = "Confirmed",
                        isPaid = true
                    ),
                    CoffeeOrder(
                        shopName = "Artisan Roast & Co.",
                        phoneNumber = "+1 (555) 321-9988",
                        orderItemsSummary = "1x Oat Flat White, 1x Double Espresso, 1x Blueberry Scone",
                        itemCount = 3,
                        unitPriceEstimate = 5.50,
                        subtotal = 16.00,
                        tax = 1.48,
                        tip = 2.50,
                        totalAmount = 19.98,
                        orderTimestamp = cal.timeInMillis - (5L * 24 * 60 * 60 * 1000),
                        billingCycleMonth = currentMonth,
                        audioFilePath = null,
                        audioDurationSeconds = 52,
                        transcriptOrNotes = "Called barista directly. Ethiopian single-origin beans.",
                        orderStatus = "Confirmed",
                        isPaid = true
                    ),
                    CoffeeOrder(
                        shopName = "Starbucks Downtown",
                        phoneNumber = "+1 (555) 654-3210",
                        orderItemsSummary = "1x Venti Iced Caramel Macchiato, 1x Bacon Gouda Sandwich",
                        itemCount = 2,
                        unitPriceEstimate = 6.45,
                        subtotal = 12.45,
                        tax = 1.15,
                        tip = 2.00,
                        totalAmount = 15.60,
                        orderTimestamp = cal.timeInMillis - (7L * 24 * 60 * 60 * 1000),
                        billingCycleMonth = currentMonth,
                        audioFilePath = null,
                        audioDurationSeconds = 39,
                        transcriptOrNotes = "Drive-thru phone order ready for immediate curb pickup.",
                        orderStatus = "Confirmed",
                        isPaid = true
                    ),
                    CoffeeOrder(
                        shopName = "Corner Espresso Bar",
                        phoneNumber = "+1 (555) 901-2345",
                        orderItemsSummary = "2x Cappuccino with Cinnamon, 1x Pain au Chocolat",
                        itemCount = 3,
                        unitPriceEstimate = 5.25,
                        subtotal = 15.00,
                        tax = 1.38,
                        tip = 2.50,
                        totalAmount = 18.88,
                        orderTimestamp = cal.timeInMillis - (10L * 24 * 60 * 60 * 1000),
                        billingCycleMonth = currentMonth,
                        audioFilePath = null,
                        audioDurationSeconds = 41,
                        transcriptOrNotes = "Called to ensure whole milk was available.",
                        orderStatus = "Confirmed",
                        isPaid = true
                    )
                )

                dao.insertOrders(sampleOrders)
            }
        }
    }
}
