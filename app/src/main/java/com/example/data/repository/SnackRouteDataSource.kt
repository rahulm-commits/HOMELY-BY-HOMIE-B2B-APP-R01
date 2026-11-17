package com.example.data.repository

import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.Shop
import com.example.data.model.StockItem
import kotlinx.coroutines.flow.Flow

interface SnackRouteDataSource {
  fun observeShops(userId: String): Flow<List<Shop>>
  fun observeOrders(userId: String): Flow<List<OrderRecord>>
  fun observeStocks(userId: String): Flow<List<StockItem>>
  fun observeMeta(userId: String): Flow<OperationsMeta?>
  fun observeRoutes(): Flow<List<DeliveryRoute>>
  suspend fun seedStarterDataIfNeeded(): Result<Unit>
  suspend fun seedRoutesIfNeeded(): Result<Unit>
  suspend fun saveRoute(route: DeliveryRoute): Result<Unit>
  suspend fun saveShop(shop: Shop): Result<String>
  suspend fun updateShopRoute(shopId: String, newRouteId: String): Result<Unit>
  suspend fun updateShopStatus(shopId: String, newStatus: String): Result<Unit>
  suspend fun updateShopInquiryStatus(
    shopId: String,
    inquiryStatus: String,
    markCalledToday: Boolean = true,
  ): Result<Unit>
  suspend fun placeOrder(
    shop: Shop,
    items: List<Pair<CatalogProduct, Int>>,
    note: String,
  ): Result<String>
  suspend fun updateOrderStatus(
    orderId: String,
    newStatus: String,
    note: String? = null,
  ): Result<Unit>
  suspend fun updateStockQuantity(stockId: String, newQuantity: Int): Result<Unit>
}
