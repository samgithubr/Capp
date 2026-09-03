package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeOrderDao {
    @Query("SELECT * FROM coffee_orders ORDER BY orderTimestamp DESC")
    fun getAllOrders(): Flow<List<CoffeeOrder>>

    @Query("SELECT * FROM coffee_orders WHERE billingCycleMonth = :billingMonth ORDER BY orderTimestamp DESC")
    fun getOrdersForMonth(billingMonth: String): Flow<List<CoffeeOrder>>

    @Query("SELECT * FROM coffee_orders WHERE orderTimestamp BETWEEN :startMillis AND :endMillis ORDER BY orderTimestamp DESC")
    fun getOrdersInRange(startMillis: Long, endMillis: Long): Flow<List<CoffeeOrder>>

    @Query("SELECT * FROM coffee_orders WHERE id = :id LIMIT 1")
    fun getOrderById(id: Long): Flow<CoffeeOrder?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: CoffeeOrder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<CoffeeOrder>)

    @Update
    suspend fun updateOrder(order: CoffeeOrder)

    @Delete
    suspend fun deleteOrder(order: CoffeeOrder)

    @Query("DELETE FROM coffee_orders WHERE id = :id")
    suspend fun deleteOrderById(id: Long)

    @Query("SELECT COUNT(*) FROM coffee_orders")
    suspend fun getOrderCount(): Int
}
