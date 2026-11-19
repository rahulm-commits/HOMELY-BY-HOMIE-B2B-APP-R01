package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnackRouteDao {

  @Query("SELECT * FROM pending_shops ORDER BY updatedAtMillis DESC")
  fun observePendingShops(): Flow<List<PendingShopEntity>>

  @Query("SELECT * FROM pending_shops ORDER BY createdAtMillis ASC")
  suspend fun getPendingShopsOnce(): List<PendingShopEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPendingShop(entity: PendingShopEntity)

  @Query("DELETE FROM pending_shops WHERE localId = :localId")
  suspend fun deletePendingShop(localId: String)

  @Query("SELECT * FROM cached_shops ORDER BY updatedAtMillis DESC")
  fun observeCachedShops(): Flow<List<CachedShopEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedShops(items: List<CachedShopEntity>)

  @Query("DELETE FROM cached_shops WHERE source = :source")
  suspend fun deleteCachedShopsBySource(source: String)

  @Query("SELECT * FROM pending_orders ORDER BY createdAtMillis DESC")
  fun observePendingOrders(): Flow<List<PendingOrderEntity>>

  @Query("SELECT * FROM pending_orders ORDER BY createdAtMillis ASC")
  suspend fun getPendingOrdersOnce(): List<PendingOrderEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPendingOrder(entity: PendingOrderEntity)

  @Query("DELETE FROM pending_orders WHERE localId = :localId")
  suspend fun deletePendingOrder(localId: String)

  @Query("SELECT * FROM cached_orders ORDER BY updatedAtMillis DESC")
  fun observeCachedOrders(): Flow<List<CachedOrderEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedOrders(items: List<CachedOrderEntity>)

  @Query("DELETE FROM cached_orders WHERE id = :orderId")
  suspend fun deleteCachedOrderById(orderId: String)

  @Query("DELETE FROM cached_orders WHERE id = :id OR orderCode = :code")
  suspend fun deleteCachedOrderByIdOrCode(id: String, code: String)

  @Query("SELECT * FROM delivery_routes ORDER BY colorIndex ASC, id ASC")
  fun observeRoutes(): Flow<List<DeliveryRouteEntity>>

  @Query("SELECT * FROM delivery_routes ORDER BY colorIndex ASC, id ASC")
  suspend fun getRoutesOnce(): List<DeliveryRouteEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRoute(route: DeliveryRouteEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRoutes(routes: List<DeliveryRouteEntity>)

  @Query("SELECT * FROM cached_stocks WHERE userId = :userId OR userId = '' ORDER BY (quantity - minThreshold) ASC")
  fun observeCachedStocks(userId: String): Flow<List<CachedStockEntity>>

  @Query("SELECT * FROM cached_stocks WHERE userId = :userId OR userId = '' ORDER BY (quantity - minThreshold) ASC")
  suspend fun getCachedStocksOnce(userId: String): List<CachedStockEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedStocks(items: List<CachedStockEntity>)

  @Query("UPDATE cached_stocks SET quantity = :newQuantity, updatedAtMillis = :updatedAtMillis WHERE id = :stockId")
  suspend fun updateCachedStockQuantity(stockId: String, newQuantity: Int, updatedAtMillis: Long)

  @Query("SELECT * FROM pending_stock_updates ORDER BY updatedAtMillis DESC")
  fun observePendingStockUpdates(): Flow<List<PendingStockUpdateEntity>>

  @Query("SELECT * FROM pending_stock_updates ORDER BY updatedAtMillis ASC")
  suspend fun getPendingStockUpdatesOnce(): List<PendingStockUpdateEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPendingStockUpdate(entity: PendingStockUpdateEntity)

  @Query("DELETE FROM pending_stock_updates WHERE stockId = :stockId")
  suspend fun deletePendingStockUpdate(stockId: String)

  @Query("SELECT * FROM cached_meta WHERE userId = :userId LIMIT 1")
  fun observeCachedMeta(userId: String): Flow<CachedMetaEntity?>

  @Query("SELECT * FROM cached_meta WHERE userId = :userId LIMIT 1")
  suspend fun getCachedMetaOnce(userId: String): CachedMetaEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedMeta(entity: CachedMetaEntity)

  @Query("SELECT * FROM cached_products ORDER BY updatedAtMillis DESC")
  fun observeCachedProducts(): Flow<List<CachedProductEntity>>

  @Query("SELECT * FROM cached_products ORDER BY updatedAtMillis DESC")
  suspend fun getCachedProductsOnce(): List<CachedProductEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedProducts(items: List<CachedProductEntity>)

  @Query("SELECT * FROM pending_products ORDER BY updatedAtMillis DESC")
  fun observePendingProducts(): Flow<List<PendingProductEntity>>

  @Query("SELECT * FROM pending_products ORDER BY createdAtMillis ASC")
  suspend fun getPendingProductsOnce(): List<PendingProductEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPendingProduct(entity: PendingProductEntity)

  @Query("DELETE FROM pending_products WHERE localId = :localId")
  suspend fun deletePendingProduct(localId: String)

  @Query("SELECT * FROM cached_payments ORDER BY paymentDate DESC, createdAtMillis DESC")
  fun observeCachedPayments(): Flow<List<CachedPaymentEntity>>

  @Query("SELECT * FROM cached_payments ORDER BY paymentDate DESC, createdAtMillis DESC")
  suspend fun getCachedPaymentsOnce(): List<CachedPaymentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedPayments(items: List<CachedPaymentEntity>)

  @Query("SELECT * FROM pending_payments ORDER BY updatedAtMillis DESC")
  fun observePendingPayments(): Flow<List<PendingPaymentEntity>>

  @Query("SELECT * FROM pending_payments ORDER BY createdAtMillis ASC")
  suspend fun getPendingPaymentsOnce(): List<PendingPaymentEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPendingPayment(entity: PendingPaymentEntity)

  @Query("DELETE FROM pending_payments WHERE localId = :localId")
  suspend fun deletePendingPayment(localId: String)

  @Query("SELECT * FROM cached_supplier_bills ORDER BY updatedAtMillis DESC")
  fun observeCachedSupplierBills(): Flow<List<CachedSupplierBillEntity>>

  @Query("SELECT * FROM cached_supplier_bills ORDER BY updatedAtMillis DESC")
  suspend fun getCachedSupplierBillsOnce(): List<CachedSupplierBillEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedSupplierBills(items: List<CachedSupplierBillEntity>)

  @Query("SELECT * FROM pending_supplier_bills ORDER BY updatedAtMillis DESC")
  fun observePendingSupplierBills(): Flow<List<PendingSupplierBillEntity>>

  @Query("SELECT * FROM pending_supplier_bills ORDER BY createdAtMillis ASC")
  suspend fun getPendingSupplierBillsOnce(): List<PendingSupplierBillEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPendingSupplierBill(entity: PendingSupplierBillEntity)

  @Query("DELETE FROM pending_supplier_bills WHERE localId = :localId")
  suspend fun deletePendingSupplierBill(localId: String)

  @Query("SELECT * FROM cached_suppliers ORDER BY updatedAtMillis DESC")
  fun observeCachedSuppliers(): Flow<List<CachedSupplierEntity>>

  @Query("SELECT * FROM cached_suppliers ORDER BY updatedAtMillis DESC")
  suspend fun getCachedSuppliersOnce(): List<CachedSupplierEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedSuppliers(items: List<CachedSupplierEntity>)

  @Query("SELECT * FROM pending_suppliers ORDER BY updatedAtMillis DESC")
  fun observePendingSuppliers(): Flow<List<PendingSupplierEntity>>

  @Query("SELECT * FROM pending_suppliers ORDER BY createdAtMillis ASC")
  suspend fun getPendingSuppliersOnce(): List<PendingSupplierEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPendingSupplier(entity: PendingSupplierEntity)

  @Query("DELETE FROM pending_suppliers WHERE localId = :localId")
  suspend fun deletePendingSupplier(localId: String)
}
