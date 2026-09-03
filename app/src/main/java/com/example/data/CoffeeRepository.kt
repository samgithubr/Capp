package com.example.data

import kotlinx.coroutines.flow.Flow

class CoffeeRepository(private val dao: CoffeeOrderDao) {
    val allOrders: Flow<List<CoffeeOrder>> = dao.getAllOrders()

    fun getOrdersForMonth(billingMonth: String): Flow<List<CoffeeOrder>> =
        dao.getOrdersForMonth(billingMonth)

    fun getOrdersInRange(startMillis: Long, endMillis: Long): Flow<List<CoffeeOrder>> =
        dao.getOrdersInRange(startMillis, endMillis)

    fun getOrderById(id: Long): Flow<CoffeeOrder?> =
        dao.getOrderById(id)

    suspend fun insertOrder(order: CoffeeOrder): Long =
        dao.insertOrder(order)

    suspend fun updateOrder(order: CoffeeOrder) =
        dao.updateOrder(order)

    suspend fun deleteOrder(order: CoffeeOrder) =
        dao.deleteOrder(order)

    suspend fun deleteOrderById(id: Long) =
        dao.deleteOrderById(id)

    suspend fun getOrderCount(): Int =
        dao.getOrderCount()
}
