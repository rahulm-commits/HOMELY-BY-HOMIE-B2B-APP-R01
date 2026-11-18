package com.example.data.repository

import android.content.Context
import com.example.data.local.CachedMetaEntity
import com.example.data.local.CachedOrderEntity
import com.example.data.local.CachedPaymentEntity
import com.example.data.local.CachedProductEntity
import com.example.data.local.CachedShopEntity
import com.example.data.local.CachedStockEntity
import com.example.data.local.CachedSupplierBillEntity
import com.example.data.local.CachedSupplierEntity
import com.example.data.local.DeliveryRouteEntity
import com.example.data.local.PendingOrderEntity
import com.example.data.local.PendingPaymentEntity
import com.example.data.local.PendingProductEntity
import com.example.data.local.PendingShopEntity
import com.example.data.local.PendingStockUpdateEntity
import com.example.data.local.PendingSupplierBillEntity
import com.example.data.local.PendingSupplierEntity
import com.example.data.local.ServerConfigStore
import com.example.data.local.ServerSettings
import com.example.data.local.SnackRouteDao
import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentLedgerHelper
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.ProductStatus
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.StockItem
import com.example.data.model.Supplier
import com.example.data.model.SupplierBill
import com.example.data.model.SupplierBillStatus
import com.example.data.model.SupplierStatus
import com.example.data.remote.LaptopHealthInfo
import com.example.data.remote.LaptopServerClient
import com.example.ui.components.isNetworkAvailable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

data class SyncRunResult(
  val syncedCount: Int,
  val remainingPendingCount: Int,
  val autoDeletedCount: Int,
  val message: String,
)

data class PullRunResult(
  val shopsPulled: Int,
  val ordersPulled: Int,
  val pushedToGlobal: Int,
  val message: String,
)

interface RemoteCloudAdapter {
  suspend fun upsertShop(shop: Shop, isUpdate: Boolean): Result<String>
  suspend fun updateShopRoute(shopId: String, newRouteId: String): Result<Unit>
  suspend fun updateShopStatus(shopId: String, newStatus: String): Result<Unit>
  suspend fun updateShopInquiryStatus(
    shopId: String,
    inquiryStatus: String,
    markCalledToday: Boolean,
  ): Result<Unit>
  suspend fun placeRawOrder(
    orderDocId: String,
    orderCode: String,
    shopId: String,
    shopCode: String,
    shopName: String,
    routeId: String,
    status: String,
    totalAmount: Double,
    itemCount: Int,
    itemsSummary: String,
    orderDate: String,
    note: String,
  ): Result<String>
  suspend fun updateOrderStatus(orderId: String, newStatus: String, note: String?): Result<Unit>
  suspend fun updateStockQuantity(stockId: String, newQuantity: Int): Result<Unit>
  suspend fun upsertProduct(product: Product, isUpdate: Boolean): Result<String>
  suspend fun updateProductStatus(productId: String, newStatus: String): Result<Unit>
  suspend fun updateProductStock(productId: String, newQuantity: Int): Result<Unit>
  suspend fun fetchShopsOnce(userId: String): Result<List<Shop>>
  suspend fun fetchOrdersOnce(userId: String): Result<List<OrderRecord>>
  suspend fun fetchProductsOnce(userId: String): Result<List<Product>>
  suspend fun upsertPayment(payment: PaymentTransaction, isUpdate: Boolean): Result<String>
  suspend fun upsertSupplierBill(bill: SupplierBill, isUpdate: Boolean): Result<String>
  suspend fun upsertSupplier(supplier: Supplier, isUpdate: Boolean): Result<String>
  suspend fun updateSupplierStatus(supplierId: String, newStatus: String): Result<Unit>
  suspend fun fetchPaymentsOnce(userId: String): Result<List<PaymentTransaction>>
  suspend fun fetchSupplierBillsOnce(userId: String): Result<List<SupplierBill>>
  suspend fun fetchSuppliersOnce(userId: String): Result<List<Supplier>>
  suspend fun seedStarterDataIfNeeded(): Result<Unit>
}

private class FirestoreCloudAdapter(
  private val repo: SnackRouteRepository,
) : RemoteCloudAdapter {
  override suspend fun upsertShop(shop: Shop, isUpdate: Boolean): Result<String> =
    repo.saveShopWithId(shop, isUpdate = isUpdate, awaitServerAck = true)

  override suspend fun updateShopRoute(shopId: String, newRouteId: String): Result<Unit> =
    repo.updateShopRouteWithAck(shopId, newRouteId, awaitServerAck = true)

  override suspend fun updateShopStatus(shopId: String, newStatus: String): Result<Unit> =
    repo.updateShopStatusWithAck(shopId, newStatus, awaitServerAck = true)

  override suspend fun updateShopInquiryStatus(
    shopId: String,
    inquiryStatus: String,
    markCalledToday: Boolean,
  ): Result<Unit> =
    repo.updateShopInquiryStatusWithAck(shopId, inquiryStatus, markCalledToday, awaitServerAck = true)

  override suspend fun placeRawOrder(
    orderDocId: String,
    orderCode: String,
    shopId: String,
    shopCode: String,
    shopName: String,
    routeId: String,
    status: String,
    totalAmount: Double,
    itemCount: Int,
    itemsSummary: String,
    orderDate: String,
    note: String,
  ): Result<String> = repo.placeRawOrder(
    orderDocId = orderDocId,
    orderCode = orderCode,
    shopId = shopId,
    shopCode = shopCode,
    shopName = shopName,
    routeId = routeId,
    status = status,
    totalAmount = totalAmount,
    itemCount = itemCount,
    itemsSummary = itemsSummary,
    orderDate = orderDate,
    note = note,
    awaitServerAck = true,
  )

  override suspend fun updateOrderStatus(
    orderId: String,
    newStatus: String,
    note: String?,
  ): Result<Unit> = repo.updateOrderStatusWithAck(orderId, newStatus, note, awaitServerAck = true)

  override suspend fun updateStockQuantity(stockId: String, newQuantity: Int): Result<Unit> =
    repo.updateStockQuantityWithAck(stockId, newQuantity, awaitServerAck = true)

  override suspend fun upsertProduct(product: Product, isUpdate: Boolean): Result<String> =
    repo.saveProductWithId(product, isUpdate = isUpdate, awaitServerAck = true)

  override suspend fun updateProductStatus(productId: String, newStatus: String): Result<Unit> =
    repo.updateProductStatusWithAck(productId, newStatus, awaitServerAck = true)

  override suspend fun updateProductStock(productId: String, newQuantity: Int): Result<Unit> =
    repo.updateProductStockWithAck(productId, newQuantity, awaitServerAck = true)

  override suspend fun fetchShopsOnce(userId: String): Result<List<Shop>> =
    repo.getShopsOnce(userId)

  override suspend fun fetchOrdersOnce(userId: String): Result<List<OrderRecord>> =
    repo.getOrdersOnce(userId)

  override suspend fun fetchProductsOnce(userId: String): Result<List<Product>> =
    repo.getProductsOnce(userId)

  override suspend fun upsertPayment(payment: PaymentTransaction, isUpdate: Boolean): Result<String> =
    repo.savePaymentWithId(payment, isUpdate = isUpdate, awaitServerAck = true)

  override suspend fun upsertSupplierBill(bill: SupplierBill, isUpdate: Boolean): Result<String> =
    repo.saveSupplierBillWithId(bill, isUpdate = isUpdate, awaitServerAck = true)

  override suspend fun upsertSupplier(supplier: Supplier, isUpdate: Boolean): Result<String> =
    repo.saveSupplierWithId(supplier, isUpdate = isUpdate, awaitServerAck = true)

  override suspend fun updateSupplierStatus(supplierId: String, newStatus: String): Result<Unit> =
    repo.updateSupplierStatusWithAck(supplierId, newStatus, awaitServerAck = true)

  override suspend fun fetchPaymentsOnce(userId: String): Result<List<PaymentTransaction>> =
    repo.getPaymentsOnce(userId)

  override suspend fun fetchSupplierBillsOnce(userId: String): Result<List<SupplierBill>> =
    repo.getSupplierBillsOnce(userId)

  override suspend fun fetchSuppliersOnce(userId: String): Result<List<Supplier>> =
    repo.getSuppliersOnce(userId)

  override suspend fun seedStarterDataIfNeeded(): Result<Unit> =
    repo.seedStarterDataIfNeeded()
}

class SyncCoordinatorRepository(
  private val appContext: Context,
  private val primary: SnackRouteDataSource,
  private val dao: SnackRouteDao,
  private val configStore: ServerConfigStore,
  private val laptopClient: LaptopServerClient,
  private val currentUserId: String,
  private val isLocalTestMode: Boolean,
  private val cloudAdapter: RemoteCloudAdapter? =
    (primary as? SnackRouteRepository)?.let(::FirestoreCloudAdapter),
  private val isOnlineProvider: () -> Boolean = { isNetworkAvailable(appContext) },
) : SnackRouteDataSource {

  private val syncMutex = Mutex()

  fun observeServerSettings(): Flow<ServerSettings> = configStore.settingsFlow

  fun observePendingShops(): Flow<List<PendingShopEntity>> = dao.observePendingShops()

  fun observePendingOrders(): Flow<List<PendingOrderEntity>> = dao.observePendingOrders()

  fun observePendingStockUpdates(): Flow<List<PendingStockUpdateEntity>> =
    dao.observePendingStockUpdates()

  fun observePendingProducts(): Flow<List<PendingProductEntity>> = dao.observePendingProducts()

  fun observePendingPayments(): Flow<List<PendingPaymentEntity>> = dao.observePendingPayments()

  fun observePendingSupplierBills(): Flow<List<PendingSupplierBillEntity>> = dao.observePendingSupplierBills()

  fun observePendingSuppliers(): Flow<List<PendingSupplierEntity>> = dao.observePendingSuppliers()

  suspend fun isShopPending(shopId: String): Boolean {
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()
    val pending = dao.getPendingShopsOnce().firstOrNull { it.localId == shopId || it.remoteShopId == shopId }
      ?: return false
    return !pending.isFullySynced(requireGlobal, requireLaptop)
  }

  suspend fun isProductPending(productId: String): Boolean {
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val pending = dao.getPendingProductsOnce().firstOrNull {
      it.localId == productId || it.remoteProductId == productId
    } ?: return false
    return !pending.isFullySynced(requireGlobal)
  }

  suspend fun isPaymentPending(paymentId: String): Boolean {
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val pending = dao.getPendingPaymentsOnce().firstOrNull {
      it.localId == paymentId || it.remotePaymentId == paymentId
    } ?: return false
    return !pending.isFullySynced(requireGlobal)
  }

  suspend fun isSupplierPending(supplierId: String): Boolean {
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val pending = dao.getPendingSuppliersOnce().firstOrNull {
      it.localId == supplierId || it.remoteSupplierId == supplierId
    } ?: return false
    return !pending.isFullySynced(requireGlobal)
  }

  override fun observeShops(userId: String): Flow<List<Shop>> {
    val primaryFlow = primary.observeShops(userId)
      .onEach { remoteShops ->
        if (remoteShops.isNotEmpty() && !isLocalTestMode) {
          val entities = remoteShops
            .filter { !it.isPendingSync }
            .map {
              CachedShopEntity.fromDomainShop(
                it.copy(userId = it.userId.ifBlank { userId }),
                source = "global",
              )
            }
          if (entities.isNotEmpty()) {
            dao.upsertCachedShops(entities)
          }
        }
      }
      .catch { error ->
        if (isPureCancellation(error)) throw error
        emit(emptyList())
      }
      .onStart { emit(emptyList()) }

    return combine(
      primaryFlow,
      dao.observeCachedShops(),
      dao.observePendingShops(),
      configStore.settingsFlow,
    ) { remoteShops, cachedEntities, pendingEntities, settings ->
      val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
      val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()

      val byId = LinkedHashMap<String, Shop>()

      // 1. Cached shops from Room (global snapshots, laptop-pulled, and offline-saved)
      cachedEntities
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { entity ->
          val shop = entity.toDomainShop()
          val key = shop.id.ifBlank { shop.shopCode }
          byId[key] = shop
        }

      // 2. Live primary (Firestore or Test Mode in-memory) — overwrite cache if primary is newer or equal
      remoteShops.forEach { shop ->
        val key = shop.id.ifBlank { shop.shopCode }
        val existing = byId[key]
        if (existing == null || shop.updatedAtMillis() >= existing.updatedAtMillis()) {
          byId[key] = shop
        }
      }

      // 3. Highest priority: local Room phone queue (unsynced or pending cleanup edits)
      pendingEntities
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { pending ->
          val stillPending = !pending.isFullySynced(requireGlobal, requireLaptop)
          val domainShop = pending.toDomainShop(pendingSync = stillPending)
          val key = domainShop.id.ifBlank { domainShop.shopCode }
          val existing = byId[key]
          if (existing == null || stillPending || pending.updatedAtMillis >= existing.updatedAtMillis()) {
            byId[key] = domainShop
          }
        }

      byId.values.sortedByDescending { it.updatedAt?.seconds ?: 0L }
    }
  }

  override fun observeOrders(userId: String): Flow<List<OrderRecord>> {
    val primaryFlow = primary.observeOrders(userId)
      .onEach { remoteOrders ->
        if (remoteOrders.isNotEmpty() && !isLocalTestMode) {
          val entities = remoteOrders
            .filter { !it.isPendingSync }
            .map {
              CachedOrderEntity.fromDomainOrder(
                it.copy(userId = it.userId.ifBlank { userId }),
                source = "global",
              )
            }
          if (entities.isNotEmpty()) {
            dao.upsertCachedOrders(entities)
          }
        }
      }
      .catch { error ->
        if (isPureCancellation(error)) throw error
        emit(emptyList())
      }
      .onStart { emit(emptyList()) }

    return combine(
      primaryFlow,
      dao.observeCachedOrders(),
      dao.observePendingOrders(),
      configStore.settingsFlow,
    ) { remoteOrders, cachedOrders, pendingOrders, settings ->
      val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
      val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()

      val byId = LinkedHashMap<String, OrderRecord>()
      cachedOrders
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { entity ->
          val ord = entity.toDomainOrder()
          byId[ord.id.ifBlank { ord.orderCode }] = ord
        }
      remoteOrders.forEach { ord ->
        val key = ord.id.ifBlank { ord.orderCode }
        val existing = byId[key]
        if (existing == null || ord.updatedAtMillis() >= existing.updatedAtMillis()) {
          byId[key] = ord
        }
      }
      pendingOrders
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { pending ->
          val stillPending = !pending.isFullySynced(requireGlobal, requireLaptop)
          val ord = pending.toDomainOrder(pendingSync = stillPending)
          val key = ord.id.ifBlank { ord.orderCode }
          val existing = byId[key]
          if (existing == null || stillPending || pending.createdAtMillis >= existing.updatedAtMillis()) {
            byId[key] = ord
          }
        }

      byId.values.sortedByDescending { it.createdAt?.seconds ?: 0L }
    }
  }

  override fun observeStocks(userId: String): Flow<List<StockItem>> {
    val primaryFlow = primary.observeStocks(userId)
      .onEach { remoteStocks ->
        if (remoteStocks.isNotEmpty() && !isLocalTestMode) {
          dao.upsertCachedStocks(
            remoteStocks.map { CachedStockEntity.fromDomainStock(it, fallbackUserId = userId) }
          )
        }
      }
      .catch { error ->
        if (isPureCancellation(error)) throw error
        emit(emptyList())
      }
      .onStart { emit(emptyList()) }

    return combine(
      primaryFlow,
      dao.observeCachedStocks(userId),
      dao.observePendingStockUpdates(),
    ) { remoteStocks, cachedEntities, pendingUpdates ->
      val pendingById = pendingUpdates
        .filter { it.userId == userId || it.userId.isBlank() }
        .associateBy { it.stockId }

      val byId = LinkedHashMap<String, StockItem>()
      cachedEntities.forEach { entity ->
        val item = entity.toDomainStock()
        byId[item.id.ifBlank { item.productCode }] = item
      }
      remoteStocks.forEach { stock ->
        val key = stock.id.ifBlank { stock.productCode }
        val existing = byId[key]
        if (existing == null || stock.updatedAtMillis() >= existing.updatedAtMillis()) {
          byId[key] = stock
        }
      }
      pendingById.forEach { (stockId, pending) ->
        val existing = byId[stockId]
        if (existing != null && (!pending.globalSynced || pending.updatedAtMillis >= existing.updatedAtMillis())) {
          byId[stockId] = existing.copy(
            quantity = pending.newQuantity,
            updatedAt = Timestamp(Date(pending.updatedAtMillis)),
          )
        }
      }

      byId.values.sortedBy { it.quantity - it.minThreshold }
    }
  }

  override fun observeProducts(userId: String): Flow<List<Product>> {
    val primaryFlow = primary.observeProducts(userId)
      .onEach { remoteProducts ->
        if (remoteProducts.isNotEmpty() && !isLocalTestMode) {
          val entities = remoteProducts
            .filter { !it.isPendingSync }
            .map {
              CachedProductEntity.fromDomainProduct(it, fallbackUserId = userId)
            }
          if (entities.isNotEmpty()) {
            dao.upsertCachedProducts(entities)
          }
        }
      }
      .catch { error ->
        if (isPureCancellation(error)) throw error
        emit(emptyList())
      }
      .onStart { emit(emptyList()) }

    return combine(
      primaryFlow,
      dao.observeCachedProducts(),
      dao.observePendingProducts(),
      configStore.settingsFlow,
    ) { remoteProducts, cachedEntities, pendingEntities, settings ->
      val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
      val byId = LinkedHashMap<String, Product>()

      cachedEntities
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { entity ->
          val prod = entity.toDomainProduct()
          byId[prod.id.ifBlank { prod.productCode }] = prod
        }

      remoteProducts.forEach { prod ->
        val key = prod.id.ifBlank { prod.productCode }
        val existing = byId[key]
        if (existing == null || prod.updatedAtMillis() >= existing.updatedAtMillis()) {
          byId[key] = prod
        }
      }

      pendingEntities
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { pending ->
          val stillPending = !pending.isFullySynced(requireGlobal)
          val domainProduct = pending.toDomainProduct(pendingSync = stillPending)
          val key = domainProduct.id.ifBlank { domainProduct.productCode }
          val existing = byId[key]
          if (existing == null || stillPending || pending.updatedAtMillis >= existing.updatedAtMillis()) {
            byId[key] = domainProduct
          }
        }

      byId.values.sortedByDescending { it.updatedAt?.seconds ?: 0L }
    }
  }

  override fun observePayments(userId: String): Flow<List<PaymentTransaction>> {
    val primaryFlow = primary.observePayments(userId)
      .onEach { remotePayments ->
        if (remotePayments.isNotEmpty() && !isLocalTestMode) {
          val entities = remotePayments
            .filter { !it.isPendingSync }
            .map { CachedPaymentEntity.fromDomainPayment(it, fallbackUserId = userId) }
          if (entities.isNotEmpty()) {
            dao.upsertCachedPayments(entities)
          }
        }
      }
      .catch { error ->
        if (isPureCancellation(error)) throw error
        emit(emptyList())
      }
      .onStart { emit(emptyList()) }

    return combine(
      primaryFlow,
      dao.observeCachedPayments(),
      dao.observePendingPayments(),
      configStore.settingsFlow,
    ) { remotePayments, cachedEntities, pendingEntities, settings ->
      val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
      val byId = LinkedHashMap<String, PaymentTransaction>()

      cachedEntities
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { entity ->
          val payment = entity.toDomainPayment()
          byId[payment.id.ifBlank { payment.receiptCode }] = payment
        }

      remotePayments.forEach { payment ->
        val key = payment.id.ifBlank { payment.receiptCode }
        val existing = byId[key]
        if (existing == null || payment.updatedAtMillis() >= existing.updatedAtMillis()) {
          byId[key] = payment
        }
      }

      pendingEntities
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { pending ->
          val stillPending = !pending.isFullySynced(requireGlobal)
          val domainPayment = pending.toDomainPayment(pendingSync = stillPending)
          val key = domainPayment.id.ifBlank { domainPayment.receiptCode }
          val existing = byId[key]
          if (existing == null || stillPending || pending.updatedAtMillis >= existing.updatedAtMillis()) {
            byId[key] = domainPayment
          }
        }

      byId.values.sortedWith(
        compareByDescending<PaymentTransaction> { it.paymentDate }
          .thenByDescending { it.createdAt?.seconds ?: 0L }
      )
    }
  }

  override fun observeSupplierBills(userId: String): Flow<List<SupplierBill>> {
    val primaryFlow = primary.observeSupplierBills(userId)
      .onEach { remoteBills ->
        if (remoteBills.isNotEmpty() && !isLocalTestMode) {
          val entities = remoteBills
            .filter { !it.isPendingSync }
            .map { CachedSupplierBillEntity.fromDomainBill(it, fallbackUserId = userId) }
          if (entities.isNotEmpty()) {
            dao.upsertCachedSupplierBills(entities)
          }
        }
      }
      .catch { error ->
        if (isPureCancellation(error)) throw error
        emit(emptyList())
      }
      .onStart { emit(emptyList()) }

    return combine(
      primaryFlow,
      dao.observeCachedSupplierBills(),
      dao.observePendingSupplierBills(),
      configStore.settingsFlow,
    ) { remoteBills, cachedEntities, pendingEntities, settings ->
      val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
      val byId = LinkedHashMap<String, SupplierBill>()

      cachedEntities
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { entity ->
          val bill = entity.toDomainBill()
          byId[bill.id.ifBlank { bill.billNumber }] = bill
        }

      remoteBills.forEach { bill ->
        val key = bill.id.ifBlank { bill.billNumber }
        val existing = byId[key]
        if (existing == null || bill.updatedAtMillis() >= existing.updatedAtMillis()) {
          byId[key] = bill
        }
      }

      pendingEntities
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { pending ->
          val stillPending = !pending.isFullySynced(requireGlobal)
          val domainBill = pending.toDomainBill(pendingSync = stillPending)
          val key = domainBill.id.ifBlank { domainBill.billNumber }
          val existing = byId[key]
          if (existing == null || stillPending || pending.updatedAtMillis >= existing.updatedAtMillis()) {
            byId[key] = domainBill
          }
        }

      byId.values.sortedByDescending { it.updatedAt?.seconds ?: 0L }
    }
  }

  override fun observeSuppliers(userId: String): Flow<List<Supplier>> {
    val primaryFlow = primary.observeSuppliers(userId)
      .onEach { remoteSuppliers ->
        if (remoteSuppliers.isNotEmpty() && !isLocalTestMode) {
          val entities = remoteSuppliers
            .filter { !it.isPendingSync }
            .map { CachedSupplierEntity.fromDomainSupplier(it, fallbackUserId = userId) }
          if (entities.isNotEmpty()) {
            dao.upsertCachedSuppliers(entities)
          }
        }
      }
      .catch { error ->
        if (isPureCancellation(error)) throw error
        emit(emptyList())
      }
      .onStart { emit(emptyList()) }

    return combine(
      primaryFlow,
      dao.observeCachedSuppliers(),
      dao.observePendingSuppliers(),
      configStore.settingsFlow,
    ) { remoteSuppliers, cachedEntities, pendingEntities, settings ->
      val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
      val byId = LinkedHashMap<String, Supplier>()

      cachedEntities
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { entity ->
          val sup = entity.toDomainSupplier()
          byId[sup.id.ifBlank { sup.supplierCode }] = sup
        }

      remoteSuppliers.forEach { sup ->
        val key = sup.id.ifBlank { sup.supplierCode }
        val existing = byId[key]
        if (existing == null || sup.updatedAtMillis() >= existing.updatedAtMillis()) {
          byId[key] = sup
        }
      }

      pendingEntities
        .filter { it.userId == userId || it.userId.isBlank() }
        .forEach { pending ->
          val stillPending = !pending.isFullySynced(requireGlobal)
          val domainSup = pending.toDomainSupplier(pendingSync = stillPending)
          val key = domainSup.id.ifBlank { domainSup.supplierCode }
          val existing = byId[key]
          if (existing == null || stillPending || pending.updatedAtMillis >= existing.updatedAtMillis()) {
            byId[key] = domainSup
          }
        }

      byId.values.sortedByDescending { it.updatedAt?.seconds ?: 0L }
    }
  }

  override fun observeMeta(userId: String): Flow<OperationsMeta?> {
    val primaryFlow = primary.observeMeta(userId)
      .onEach { remoteMeta ->
        if (remoteMeta != null && !isLocalTestMode) {
          dao.upsertCachedMeta(
            CachedMetaEntity.fromDomainMeta(remoteMeta, fallbackUserId = userId)
          )
        }
      }
      .catch { error ->
        if (isPureCancellation(error)) throw error
        emit(null)
      }
      .onStart { emit(null) }

    return combine(
      primaryFlow,
      dao.observeCachedMeta(userId),
    ) { remoteMeta, cachedMeta ->
      remoteMeta ?: cachedMeta?.toDomainMeta()
    }
  }

  override fun observeRoutes(): Flow<List<DeliveryRoute>> =
    dao.observeRoutes().map { entities ->
      if (entities.isEmpty()) RoutesCatalog.starterRoutes else entities.map { it.toDomainRoute() }
    }

  override suspend fun seedStarterDataIfNeeded(): Result<Unit> {
    seedRoutesIfNeeded()
    if (!isLocalTestMode && !isOnlineProvider()) {
      return Result.success(Unit)
    }
    return cloudAdapter?.seedStarterDataIfNeeded() ?: primary.seedStarterDataIfNeeded()
  }

  override suspend fun seedRoutesIfNeeded(): Result<Unit> {
    if (dao.getRoutesOnce().isEmpty()) {
      dao.upsertRoutes(RoutesCatalog.starterRoutes.map { DeliveryRouteEntity.fromDomainRoute(it) })
    }
    return Result.success(Unit)
  }

  override suspend fun saveRoute(route: DeliveryRoute): Result<Unit> {
    if (dao.getRoutesOnce().isEmpty()) {
      dao.upsertRoutes(RoutesCatalog.starterRoutes.map { DeliveryRouteEntity.fromDomainRoute(it) })
    }
    dao.upsertRoute(DeliveryRouteEntity.fromDomainRoute(route))
    if (isLocalTestMode) {
      primary.saveRoute(route)
    }
    val settings = configStore.getCurrentSettings()
    if (settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()) {
      laptopClient.pushRoute(settings.laptopServerUrl, settings.laptopApiToken, route)
    }
    return Result.success(Unit)
  }

  override suspend fun saveShop(shop: Shop): Result<String> {
    val isUpdate = shop.id.isNotBlank()
    val targetId = if (isUpdate) {
      shop.id
    } else {
      "shop_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }
    val today = todayString()
    val normalizedShop = shop.copy(
      id = targetId,
      userId = currentUserId,
      enquiryDate = shop.enquiryDate.ifBlank { today },
      callDate = shop.callDate.ifBlank { today },
      lastCallDate = shop.lastCallDate.ifBlank { today },
    )

    // 1. Always save durably to the phone's Room database first so zero data is ever lost.
    val initialPending = PendingShopEntity.fromDomainShop(
      shop = normalizedShop,
      localId = targetId,
      userId = currentUserId,
      isUpdate = isUpdate,
    )
    dao.upsertPendingShop(initialPending)

    if (isLocalTestMode) {
      primary.saveShop(normalizedShop)
    }

    // 2. Attempt immediate sync to active servers (Global + Laptop).
    val settings = configStore.getCurrentSettings()
    val afterSync = pushPendingShopToServers(initialPending, settings)

    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()
    val fullySynced = afterSync.isFullySynced(requireGlobal, requireLaptop)

    if (fullySynced) {
      dao.upsertCachedShops(
        listOf(CachedShopEntity.fromDomainShop(afterSync.toDomainShop(pendingSync = false), source = "synced"))
      )
      if (settings.autoDeleteSyncedFromPhone) {
        dao.deletePendingShop(afterSync.localId)
      } else {
        dao.upsertPendingShop(afterSync)
      }
    } else {
      dao.upsertPendingShop(afterSync)
    }

    return Result.success(targetId)
  }

  override suspend fun updateShopRoute(shopId: String, newRouteId: String): Result<Unit> {
    return applyShopMutation(
      shopId = shopId,
      mutateShop = { shop, now ->
        shop.copy(
          routeId = newRouteId,
          updatedAt = Timestamp(Date(now)),
        )
      },
      pushPrimaryInTestMode = {
        primary.updateShopRoute(shopId, newRouteId)
      },
      pushGlobalDirect = { cloud ->
        cloud.updateShopRoute(shopId, newRouteId)
      },
    )
  }

  override suspend fun updateShopStatus(shopId: String, newStatus: String): Result<Unit> {
    return applyShopMutation(
      shopId = shopId,
      mutateShop = { shop, now ->
        shop.copy(
          status = newStatus,
          updatedAt = Timestamp(Date(now)),
        )
      },
      pushPrimaryInTestMode = {
        primary.updateShopStatus(shopId, newStatus)
      },
      pushGlobalDirect = { cloud ->
        cloud.updateShopStatus(shopId, newStatus)
      },
    )
  }

  override suspend fun updateShopInquiryStatus(
    shopId: String,
    inquiryStatus: String,
    markCalledToday: Boolean,
  ): Result<Unit> {
    val today = todayString()
    return applyShopMutation(
      shopId = shopId,
      mutateShop = { shop, now ->
        shop.copy(
          inquiryStatus = inquiryStatus,
          lastCallDate = if (markCalledToday) today else shop.lastCallDate,
          updatedAt = Timestamp(Date(now)),
        )
      },
      pushPrimaryInTestMode = {
        primary.updateShopInquiryStatus(shopId, inquiryStatus, markCalledToday)
      },
      pushGlobalDirect = { cloud ->
        cloud.updateShopInquiryStatus(
          shopId = shopId,
          inquiryStatus = inquiryStatus,
          markCalledToday = markCalledToday,
        )
      },
    )
  }

  private suspend fun applyShopMutation(
    shopId: String,
    mutateShop: (Shop, Long) -> Shop,
    pushPrimaryInTestMode: suspend () -> Result<Unit>,
    pushGlobalDirect: suspend (RemoteCloudAdapter) -> Result<Unit>,
  ): Result<Unit> {
    val now = System.currentTimeMillis()
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()

    // 1. Update in-memory test repository if active
    if (isLocalTestMode) {
      pushPrimaryInTestMode()
    }

    // 2. Find latest snapshot of the shop across pending_shops, cached_shops, and primary
    val pending = dao.getPendingShopsOnce().firstOrNull { it.localId == shopId || it.remoteShopId == shopId }
    val cached = dao.observeCachedShops().first().firstOrNull { it.id == shopId }
    val baseShop = pending?.toDomainShop(pendingSync = true)
      ?: cached?.toDomainShop()
      ?: runCatching {
        withTimeoutOrNull(800L) {
          primary.observeShops(currentUserId).first().firstOrNull { it.id == shopId }
        }
      }.getOrNull()

    if (baseShop == null) {
      val cloud = cloudAdapter
      return if (!isLocalTestMode && cloud != null && isOnlineProvider()) {
        pushGlobalDirect(cloud)
      } else if (isLocalTestMode) {
        pushPrimaryInTestMode()
      } else {
        Result.failure(IllegalStateException("Shop not cached locally yet"))
      }
    }

    val updatedShop = mutateShop(baseShop, now)
      .copy(userId = currentUserId, updatedAt = Timestamp(Date(now)))

    // 3. Persist locally in cached_shops immediately so UI & offline state stay accurate
    dao.upsertCachedShops(
      listOf(CachedShopEntity.fromDomainShop(updatedShop, source = cached?.source ?: "local").copy(updatedAtMillis = now))
    )

    // 4. Push to Global Cloud if enabled & authenticated
    var globalOk = pending?.globalSynced ?: false
    val errors = mutableListOf<String>()
    val cloud = cloudAdapter
    if (requireGlobal && cloud != null) {
      globalOk = false
      if (isOnlineProvider()) {
        val directRes = pushGlobalDirect(cloud)
        if (directRes.isSuccess) {
          globalOk = true
        } else {
          val err = directRes.exceptionOrNull()
          val isNotFound = err is FirebaseFirestoreException && err.code == FirebaseFirestoreException.Code.NOT_FOUND
          if (isNotFound) {
            val createRes = cloud.upsertShop(
              normalizeForFirestore(updatedShop),
              isUpdate = false,
            )
            if (createRes.isSuccess) {
              globalOk = true
            } else {
              errors += "Cloud: ${createRes.exceptionOrNull()?.localizedMessage ?: "failed"}"
            }
          } else {
            errors += "Cloud: ${err?.localizedMessage ?: "failed"}"
          }
        }
      } else {
        errors += "Cloud: No internet"
      }
    }

    // 5. Push to Laptop Local Server if enabled
    var localOk = false
    if (requireLaptop) {
      val push = laptopClient.pushShop(
        settings.laptopServerUrl,
        settings.laptopApiToken,
        updatedShop.copy(isPendingSync = false),
      )
      if (push.isSuccess) {
        localOk = true
      } else {
        errors += "Laptop: ${push.exceptionOrNull()?.localizedMessage ?: "unreachable"}"
      }
    }

    // 6. Queue into pending_shops whenever any active target is not yet synced (or preserve existing queue entry)
    val needsRetryQueue = (requireGlobal && !globalOk) || (requireLaptop && !localOk)
    if (needsRetryQueue || pending != null) {
      val entity = PendingShopEntity.fromDomainShop(
        shop = updatedShop,
        localId = pending?.localId ?: shopId,
        userId = currentUserId,
        isUpdate = pending?.isUpdate ?: true,
      ).copy(
        remoteShopId = pending?.remoteShopId?.ifBlank { shopId } ?: shopId,
        globalSynced = globalOk,
        localServerSynced = localOk,
        lastError = errors.joinToString(" | "),
        createdAtMillis = pending?.createdAtMillis ?: now,
        updatedAtMillis = now,
      )
      val done = entity.isFullySynced(requireGlobal, requireLaptop)
      if (done && settings.autoDeleteSyncedFromPhone && pending != null) {
        dao.deletePendingShop(entity.localId)
      } else if (!done || pending != null) {
        dao.upsertPendingShop(entity)
      }
    }

    return Result.success(Unit)
  }

  override suspend fun placeOrder(
    shop: Shop,
    items: List<Pair<CatalogProduct, Int>>,
    note: String,
  ): Result<String> {
    val validItems = items.filter { it.second > 0 }
    if (validItems.isEmpty()) {
      return Result.failure(IllegalArgumentException("Select at least one product to place an order."))
    }

    val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val orderCode = "ORD-${(System.currentTimeMillis() % 9000 + 1000)}"
    val today = todayString()
    val totalAmount = validItems.sumOf { (prod, qty) -> prod.defaultPrice * qty }
    val itemCount = validItems.sumOf { it.second }
    val itemsSummary = validItems.joinToString(", ") { (prod, qty) -> "${prod.code} x$qty" }.take(780)

    val entity = PendingOrderEntity(
      localId = orderId,
      orderCode = orderCode,
      userId = currentUserId,
      shopId = shop.id,
      shopCode = shop.shopCode,
      shopName = shop.name,
      routeId = shop.routeId,
      status = OrderStatus.CONFIRMED,
      totalAmount = totalAmount,
      itemCount = itemCount,
      itemsSummary = itemsSummary,
      orderDate = today,
      note = note.trim(),
    )

    // Save durably to Room first
    dao.upsertPendingOrder(entity)
    // Also update cached shop metadata immediately so offline UI reflects latest call/order date
    val cachedShop = dao.observeCachedShops().first().firstOrNull { it.id == shop.id }
    if (cachedShop != null) {
      dao.upsertCachedShops(
        listOf(
          cachedShop.copy(
            inquiryStatus = "confirmed",
            lastOrderDate = today,
            lastCallDate = today,
            updatedAtMillis = System.currentTimeMillis(),
          )
        )
      )
    }

    if (isLocalTestMode) {
      primary.placeOrder(shop, items, note)
    }

    val settings = configStore.getCurrentSettings()
    val afterSync = pushPendingOrderToServers(entity, settings)

    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()
    val fullySynced = afterSync.isFullySynced(requireGlobal, requireLaptop)

    if (fullySynced) {
      dao.upsertCachedOrders(
        listOf(CachedOrderEntity.fromDomainOrder(afterSync.toDomainOrder(pendingSync = false), source = "synced"))
      )
      if (settings.autoDeleteSyncedFromPhone) {
        dao.deletePendingOrder(afterSync.localId)
      } else {
        dao.upsertPendingOrder(afterSync)
      }
    } else {
      dao.upsertPendingOrder(afterSync)
    }

    return Result.success(orderId)
  }

  override suspend fun updateOrderStatus(
    orderId: String,
    newStatus: String,
    note: String?,
  ): Result<Unit> {
    val now = System.currentTimeMillis()
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()

    if (isLocalTestMode) {
      primary.updateOrderStatus(orderId, newStatus, note)
    }

    val pending = dao.getPendingOrdersOnce().firstOrNull { it.localId == orderId || it.orderCode == orderId }
    val cached = dao.observeCachedOrders().first().firstOrNull { it.id == orderId || it.orderCode == orderId }
    val baseOrder = pending?.toDomainOrder(pendingSync = true)
      ?: cached?.toDomainOrder()
      ?: runCatching {
        withTimeoutOrNull(800L) {
          primary.observeOrders(currentUserId).first().firstOrNull { it.id == orderId || it.orderCode == orderId }
        }
      }.getOrNull()

    val updatedOrder = baseOrder?.copy(
      status = newStatus,
      note = note?.trim() ?: baseOrder.note,
      updatedAt = Timestamp(Date(now)),
    )

    if (updatedOrder != null) {
      dao.upsertCachedOrders(
        listOf(
          CachedOrderEntity.fromDomainOrder(updatedOrder, source = cached?.source ?: "local")
            .copy(updatedAtMillis = now)
        )
      )
    }

    var globalOk = false
    val cloud = cloudAdapter
    if (requireGlobal && cloud != null && isOnlineProvider()) {
      val res = cloud.updateOrderStatus(orderId, newStatus, note)
      if (res.isSuccess) {
        globalOk = true
      }
    }

    var localOk = false
    if (requireLaptop && updatedOrder != null) {
      val push = laptopClient.pushOrder(
        settings.laptopServerUrl,
        settings.laptopApiToken,
        updatedOrder.copy(isPendingSync = false),
      )
      if (push.isSuccess) {
        localOk = true
      }
    }

    if (pending != null) {
      val updatedPending = pending.copy(
        status = newStatus,
        note = note?.trim() ?: pending.note,
        globalSynced = if (requireGlobal) globalOk else pending.globalSynced,
        localServerSynced = if (requireLaptop) localOk else pending.localServerSynced,
      )
      dao.upsertPendingOrder(updatedPending)
    } else if (updatedOrder != null && ((requireGlobal && !globalOk) || (requireLaptop && !localOk))) {
      dao.upsertPendingOrder(
        PendingOrderEntity(
          localId = updatedOrder.id.ifBlank { orderId },
          orderCode = updatedOrder.orderCode,
          userId = currentUserId,
          shopId = updatedOrder.shopId,
          shopCode = updatedOrder.shopCode,
          shopName = updatedOrder.shopName,
          routeId = updatedOrder.routeId,
          status = newStatus,
          totalAmount = updatedOrder.totalAmount,
          itemCount = updatedOrder.itemCount,
          itemsSummary = updatedOrder.itemsSummary,
          orderDate = updatedOrder.orderDate,
          note = updatedOrder.note,
          globalSynced = globalOk,
          localServerSynced = localOk,
          createdAtMillis = now,
        )
      )
    }

    return Result.success(Unit)
  }

  override suspend fun updateStockQuantity(stockId: String, newQuantity: Int): Result<Unit> {
    val safeQty = newQuantity.coerceAtLeast(0)
    val now = System.currentTimeMillis()

    // 1. Update Room cached_stocks immediately so offline UI & restarts stay accurate
    val cachedStock = dao.getCachedStocksOnce(currentUserId).firstOrNull { it.id == stockId }
    var productName = cachedStock?.productName.orEmpty()
    if (cachedStock != null) {
      dao.updateCachedStockQuantity(stockId, safeQty, now)
    } else {
      val fromPrimary = runCatching {
        withTimeoutOrNull(800L) {
          primary.observeStocks(currentUserId).first().firstOrNull { it.id == stockId }
        }
      }.getOrNull()
      if (fromPrimary != null) {
        productName = fromPrimary.productName
        dao.upsertCachedStocks(
          listOf(
            CachedStockEntity.fromDomainStock(
              fromPrimary.copy(quantity = safeQty, updatedAt = Timestamp(Date(now))),
              fallbackUserId = currentUserId,
            )
          )
        )
      }
    }

    if (isLocalTestMode) {
      return primary.updateStockQuantity(stockId, safeQty)
    }

    val settings = configStore.getCurrentSettings()
    val cloud = cloudAdapter
    val shouldPushCloud = settings.globalSyncEnabled && cloud != null
    if (shouldPushCloud && isOnlineProvider()) {
      val res = cloud!!.updateStockQuantity(stockId, safeQty)
      if (res.isSuccess) {
        dao.deletePendingStockUpdate(stockId)
        return Result.success(Unit)
      }
      dao.upsertPendingStockUpdate(
        PendingStockUpdateEntity(
          stockId = stockId,
          userId = currentUserId,
          productName = productName.ifBlank { stockId },
          newQuantity = safeQty,
          globalSynced = false,
          lastError = "Cloud: ${res.exceptionOrNull()?.localizedMessage ?: "failed"}",
          updatedAtMillis = now,
        )
      )
      return Result.success(Unit)
    }

    // Queue stock change durably in Room so it syncs automatically when connectivity returns
    dao.upsertPendingStockUpdate(
      PendingStockUpdateEntity(
        stockId = stockId,
        userId = currentUserId,
        productName = productName.ifBlank { stockId },
        newQuantity = safeQty,
        globalSynced = false,
        lastError = if (settings.globalSyncEnabled) "Cloud: No internet" else "Cloud sync paused",
        updatedAtMillis = now,
      )
    )
    return Result.success(Unit)
  }

  override suspend fun saveProduct(product: Product): Result<String> {
    val isUpdate = product.id.isNotBlank()
    val targetId = if (isUpdate) {
      product.id
    } else {
      "prd_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }
    val today = todayString()
    val normalizedProduct = normalizeProductForFirestore(
      product.copy(
        id = targetId,
        userId = currentUserId,
        addedDate = product.addedDate.ifBlank { today },
      )
    )

    val initialPending = PendingProductEntity.fromDomainProduct(
      product = normalizedProduct,
      localId = targetId,
      userId = currentUserId,
      isUpdate = isUpdate,
    )
    dao.upsertPendingProduct(initialPending)
    dao.upsertCachedProducts(
      listOf(CachedProductEntity.fromDomainProduct(normalizedProduct, fallbackUserId = currentUserId))
    )

    if (isLocalTestMode) {
      primary.saveProduct(normalizedProduct)
    }

    val settings = configStore.getCurrentSettings()
    val afterSync = pushPendingProductToServer(initialPending, settings)
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    if (afterSync.isFullySynced(requireGlobal)) {
      dao.upsertCachedProducts(
        listOf(CachedProductEntity.fromDomainProduct(afterSync.toDomainProduct(pendingSync = false), fallbackUserId = currentUserId))
      )
      if (settings.autoDeleteSyncedFromPhone) {
        dao.deletePendingProduct(afterSync.localId)
      } else {
        dao.upsertPendingProduct(afterSync)
      }
    } else {
      dao.upsertPendingProduct(afterSync)
    }
    return Result.success(targetId)
  }

  override suspend fun updateProductStatus(productId: String, newStatus: String): Result<Unit> {
    return applyProductMutation(
      productId = productId,
      mutateProduct = { prod, now ->
        prod.copy(status = newStatus, updatedAt = Timestamp(Date(now)))
      },
      pushPrimaryInTestMode = { primary.updateProductStatus(productId, newStatus) },
      pushGlobalDirect = { cloud -> cloud.updateProductStatus(productId, newStatus) },
    )
  }

  override suspend fun updateProductStock(productId: String, newQuantity: Int): Result<Unit> {
    val safeQty = newQuantity.coerceAtLeast(0)
    return applyProductMutation(
      productId = productId,
      mutateProduct = { prod, now ->
        prod.copy(stockQuantity = safeQty, updatedAt = Timestamp(Date(now)))
      },
      pushPrimaryInTestMode = { primary.updateProductStock(productId, safeQty) },
      pushGlobalDirect = { cloud -> cloud.updateProductStock(productId, safeQty) },
    )
  }

  private suspend fun applyProductMutation(
    productId: String,
    mutateProduct: (Product, Long) -> Product,
    pushPrimaryInTestMode: suspend () -> Result<Unit>,
    pushGlobalDirect: suspend (RemoteCloudAdapter) -> Result<Unit>,
  ): Result<Unit> {
    val now = System.currentTimeMillis()
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode

    if (isLocalTestMode) {
      pushPrimaryInTestMode()
    }

    val pending = dao.getPendingProductsOnce().firstOrNull {
      it.localId == productId || it.remoteProductId == productId
    }
    val cached = dao.getCachedProductsOnce().firstOrNull { it.id == productId }
    val baseProduct = pending?.toDomainProduct(pendingSync = true)
      ?: cached?.toDomainProduct()
      ?: runCatching {
        withTimeoutOrNull(800L) {
          primary.observeProducts(currentUserId).first().firstOrNull { it.id == productId }
        }
      }.getOrNull()

    if (baseProduct == null) {
      val cloud = cloudAdapter
      return if (requireGlobal && cloud != null && isOnlineProvider()) {
        pushGlobalDirect(cloud)
      } else if (isLocalTestMode) {
        pushPrimaryInTestMode()
      } else {
        Result.failure(IllegalStateException("Product not cached locally yet"))
      }
    }

    val updatedProduct = normalizeProductForFirestore(
      mutateProduct(baseProduct, now).copy(
        userId = currentUserId,
        updatedAt = Timestamp(Date(now)),
      )
    )

    dao.upsertCachedProducts(
      listOf(CachedProductEntity.fromDomainProduct(updatedProduct, fallbackUserId = currentUserId).copy(updatedAtMillis = now))
    )

    var globalOk = pending?.globalSynced ?: false
    var errorMsg = ""
    val cloud = cloudAdapter
    if (requireGlobal && cloud != null) {
      globalOk = false
      if (isOnlineProvider()) {
        val directRes = pushGlobalDirect(cloud)
        if (directRes.isSuccess) {
          globalOk = true
        } else {
          val err = directRes.exceptionOrNull()
          val isNotFound = err is FirebaseFirestoreException && err.code == FirebaseFirestoreException.Code.NOT_FOUND
          if (isNotFound) {
            val createRes = cloud.upsertProduct(updatedProduct, isUpdate = false)
            if (createRes.isSuccess) {
              globalOk = true
            } else {
              errorMsg = "Cloud: ${createRes.exceptionOrNull()?.localizedMessage ?: "failed"}"
            }
          } else {
            errorMsg = "Cloud: ${err?.localizedMessage ?: "failed"}"
          }
        }
      } else {
        errorMsg = "Cloud: No internet"
      }
    }

    val needsRetryQueue = requireGlobal && !globalOk
    if (needsRetryQueue || pending != null) {
      val entity = PendingProductEntity.fromDomainProduct(
        product = updatedProduct,
        localId = pending?.localId ?: productId,
        userId = currentUserId,
        isUpdate = pending?.isUpdate ?: true,
      ).copy(
        remoteProductId = pending?.remoteProductId?.ifBlank { productId } ?: productId,
        globalSynced = globalOk,
        lastError = errorMsg,
        createdAtMillis = pending?.createdAtMillis ?: now,
        updatedAtMillis = now,
      )
      val done = entity.isFullySynced(requireGlobal)
      if (done && settings.autoDeleteSyncedFromPhone && pending != null) {
        dao.deletePendingProduct(entity.localId)
      } else if (!done || pending != null) {
        dao.upsertPendingProduct(entity)
      }
    }

    return Result.success(Unit)
  }

  suspend fun updateServerSettings(
    globalSyncEnabled: Boolean,
    laptopServerEnabled: Boolean,
    laptopServerUrl: String,
    laptopApiToken: String,
    autoDeleteSyncedFromPhone: Boolean,
  ) {
    configStore.updateServerConfig(
      globalSyncEnabled = globalSyncEnabled,
      laptopServerEnabled = laptopServerEnabled,
      laptopServerUrl = laptopServerUrl,
      laptopApiToken = laptopApiToken,
      autoDeleteSyncedFromPhone = autoDeleteSyncedFromPhone,
    )
  }

  suspend fun testLaptopConnection(url: String, token: String): LaptopHealthInfo {
    return laptopClient.checkHealth(url, token)
  }

  suspend fun syncAllPending(): SyncRunResult = syncMutex.withLock {
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()

    if (!requireGlobal && !requireLaptop) {
      val msg = if (isLocalTestMode) {
        "Enable Laptop Server or Sign in with Google for Cloud sync"
      } else {
        "Enable Global Cloud or Laptop Local Server to sync"
      }
      configStore.recordSyncOutcome(msg)
      val remaining = dao.getPendingShopsOnce().size +
        dao.getPendingOrdersOnce().size +
        dao.getPendingStockUpdatesOnce().size +
        dao.getPendingProductsOnce().size +
        dao.getPendingPaymentsOnce().size +
        dao.getPendingSupplierBillsOnce().size +
        dao.getPendingSuppliersOnce().size
      return@withLock SyncRunResult(0, remaining, 0, msg)
    }

    val cloud = cloudAdapter
    if (requireGlobal && cloud != null && isOnlineProvider()) {
      runCatching { cloud.seedStarterDataIfNeeded() }
    }

    var syncedItems = 0
    var autoDeleted = 0

    // 1. Sync pending shops
    val pendingShops = dao.getPendingShopsOnce()
    for (shopEntity in pendingShops) {
      val needsSync = (requireGlobal && !shopEntity.globalSynced) ||
        (requireLaptop && !shopEntity.localServerSynced)
      if (!needsSync && shopEntity.isFullySynced(requireGlobal, requireLaptop)) {
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingShop(shopEntity.localId)
          autoDeleted++
        }
        continue
      }
      val updated = pushPendingShopToServers(shopEntity, settings)
      val done = updated.isFullySynced(requireGlobal, requireLaptop)
      if (done) {
        syncedItems++
        dao.upsertCachedShops(
          listOf(CachedShopEntity.fromDomainShop(updated.toDomainShop(pendingSync = false), source = "synced"))
        )
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingShop(updated.localId)
          autoDeleted++
        } else {
          dao.upsertPendingShop(updated)
        }
      } else {
        dao.upsertPendingShop(updated)
      }
    }

    // 2. Sync pending orders
    val pendingOrders = dao.getPendingOrdersOnce()
    for (orderEntity in pendingOrders) {
      val needsSync = (requireGlobal && !orderEntity.globalSynced) ||
        (requireLaptop && !orderEntity.localServerSynced)
      if (!needsSync && orderEntity.isFullySynced(requireGlobal, requireLaptop)) {
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingOrder(orderEntity.localId)
          autoDeleted++
        }
        continue
      }
      val updated = pushPendingOrderToServers(orderEntity, settings)
      val done = updated.isFullySynced(requireGlobal, requireLaptop)
      if (done) {
        syncedItems++
        dao.upsertCachedOrders(
          listOf(CachedOrderEntity.fromDomainOrder(updated.toDomainOrder(pendingSync = false), source = "synced"))
        )
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingOrder(updated.localId)
          autoDeleted++
        } else {
          dao.upsertPendingOrder(updated)
        }
      } else {
        dao.upsertPendingOrder(updated)
      }
    }

    // 3. Sync pending stock updates to Cloud Firestore
    val pendingStocks = dao.getPendingStockUpdatesOnce()
    if (requireGlobal && cloud != null && pendingStocks.isNotEmpty()) {
      if (isOnlineProvider()) {
        for (stockEntity in pendingStocks) {
          val res = cloud.updateStockQuantity(stockEntity.stockId, stockEntity.newQuantity)
          if (res.isSuccess) {
            dao.deletePendingStockUpdate(stockEntity.stockId)
            syncedItems++
          } else {
            dao.upsertPendingStockUpdate(
              stockEntity.copy(
                lastError = "Cloud: ${res.exceptionOrNull()?.localizedMessage ?: "failed"}",
                updatedAtMillis = System.currentTimeMillis(),
              )
            )
          }
        }
      } else {
        for (stockEntity in pendingStocks) {
          dao.upsertPendingStockUpdate(stockEntity.copy(lastError = "Cloud: No internet"))
        }
      }
    }

    // 3b. Sync pending catalog products to Cloud Firestore
    val pendingProducts = dao.getPendingProductsOnce()
    for (prodEntity in pendingProducts) {
      val needsSync = requireGlobal && !prodEntity.globalSynced
      if (!needsSync && prodEntity.isFullySynced(requireGlobal)) {
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingProduct(prodEntity.localId)
          autoDeleted++
        }
        continue
      }
      val updated = pushPendingProductToServer(prodEntity, settings)
      val done = updated.isFullySynced(requireGlobal)
      if (done) {
        syncedItems++
        dao.upsertCachedProducts(
          listOf(CachedProductEntity.fromDomainProduct(updated.toDomainProduct(pendingSync = false), fallbackUserId = currentUserId))
        )
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingProduct(updated.localId)
          autoDeleted++
        } else {
          dao.upsertPendingProduct(updated)
        }
      } else {
        dao.upsertPendingProduct(updated)
      }
    }

    // 3c. Sync pending supplier bills to Cloud Firestore
    val pendingSupplierBills = dao.getPendingSupplierBillsOnce()
    for (billEntity in pendingSupplierBills) {
      val needsSync = requireGlobal && !billEntity.globalSynced
      if (!needsSync && billEntity.isFullySynced(requireGlobal)) {
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingSupplierBill(billEntity.localId)
          autoDeleted++
        }
        continue
      }
      val updated = pushPendingSupplierBillToServer(billEntity, settings)
      val done = updated.isFullySynced(requireGlobal)
      if (done) {
        syncedItems++
        dao.upsertCachedSupplierBills(
          listOf(CachedSupplierBillEntity.fromDomainBill(updated.toDomainBill(pendingSync = false), fallbackUserId = currentUserId))
        )
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingSupplierBill(updated.localId)
          autoDeleted++
        } else {
          dao.upsertPendingSupplierBill(updated)
        }
      } else {
        dao.upsertPendingSupplierBill(updated)
      }
    }

    // 3d. Sync pending payment records to Cloud Firestore
    val pendingPayments = dao.getPendingPaymentsOnce()
    for (payEntity in pendingPayments) {
      val needsSync = requireGlobal && !payEntity.globalSynced
      if (!needsSync && payEntity.isFullySynced(requireGlobal)) {
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingPayment(payEntity.localId)
          autoDeleted++
        }
        continue
      }
      val updated = pushPendingPaymentToServer(payEntity, settings)
      val done = updated.isFullySynced(requireGlobal)
      if (done) {
        syncedItems++
        dao.upsertCachedPayments(
          listOf(CachedPaymentEntity.fromDomainPayment(updated.toDomainPayment(pendingSync = false), fallbackUserId = currentUserId))
        )
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingPayment(updated.localId)
          autoDeleted++
        } else {
          dao.upsertPendingPayment(updated)
        }
      } else {
        dao.upsertPendingPayment(updated)
      }
    }

    // 3e. Sync pending suppliers directory to Cloud Firestore
    val pendingSuppliers = dao.getPendingSuppliersOnce()
    for (supEntity in pendingSuppliers) {
      val needsSync = requireGlobal && !supEntity.globalSynced
      if (!needsSync && supEntity.isFullySynced(requireGlobal)) {
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingSupplier(supEntity.localId)
          autoDeleted++
        }
        continue
      }
      val updated = pushPendingSupplierToServer(supEntity, settings)
      val done = updated.isFullySynced(requireGlobal)
      if (done) {
        syncedItems++
        dao.upsertCachedSuppliers(
          listOf(CachedSupplierEntity.fromDomainSupplier(updated.toDomainSupplier(pendingSync = false), fallbackUserId = currentUserId))
        )
        if (settings.autoDeleteSyncedFromPhone) {
          dao.deletePendingSupplier(updated.localId)
          autoDeleted++
        } else {
          dao.upsertPendingSupplier(updated)
        }
      } else {
        dao.upsertPendingSupplier(updated)
      }
    }

    // 4. Mirror existing primary & cached records across Laptop Server and Global Cloud
    var laptopMirrorCount = 0
    var laptopMirrorError: String? = null
    if (requireLaptop) {
      val allShops = LinkedHashMap<String, Shop>()
      dao.observeCachedShops().first().forEach { entity ->
        val s = entity.toDomainShop()
        allShops[s.id.ifBlank { s.shopCode }] = s
      }
      runCatching {
        withTimeoutOrNull(1200L) { primary.observeShops(currentUserId).first() }
      }.getOrNull().orEmpty().forEach { s ->
        val key = s.id.ifBlank { s.shopCode }
        val existing = allShops[key]
        if (existing == null || s.updatedAtMillis() >= existing.updatedAtMillis()) {
          allShops[key] = s
        }
      }
      for (s in allShops.values) {
        val pushRes = laptopClient.pushShop(settings.laptopServerUrl, settings.laptopApiToken, s)
        if (pushRes.isSuccess) {
          laptopMirrorCount++
        } else if (laptopMirrorError == null) {
          laptopMirrorError = pushRes.exceptionOrNull()?.localizedMessage ?: "Laptop unreachable"
        }
      }

      val allOrders = LinkedHashMap<String, OrderRecord>()
      dao.observeCachedOrders().first().forEach { entity ->
        val o = entity.toDomainOrder()
        allOrders[o.id.ifBlank { o.orderCode }] = o
      }
      runCatching {
        withTimeoutOrNull(1200L) { primary.observeOrders(currentUserId).first() }
      }.getOrNull().orEmpty().forEach { o ->
        val key = o.id.ifBlank { o.orderCode }
        val existing = allOrders[key]
        if (existing == null || o.updatedAtMillis() >= existing.updatedAtMillis()) {
          allOrders[key] = o
        }
      }
      for (o in allOrders.values) {
        val pushRes = laptopClient.pushOrder(settings.laptopServerUrl, settings.laptopApiToken, o)
        if (pushRes.isSuccess) {
          laptopMirrorCount++
        } else if (laptopMirrorError == null) {
          laptopMirrorError = pushRes.exceptionOrNull()?.localizedMessage ?: "Laptop unreachable"
        }
      }

      val routesToPush = dao.getRoutesOnce().ifEmpty {
        RoutesCatalog.starterRoutes.map { DeliveryRouteEntity.fromDomainRoute(it) }
      }
      for (routeEntity in routesToPush) {
        laptopClient.pushRoute(
          settings.laptopServerUrl,
          settings.laptopApiToken,
          routeEntity.toDomainRoute(),
        )
      }
    }

    var globalMirrorCount = 0
    var globalMirrorError: String? = null
    if (requireGlobal && cloud != null) {
      if (isOnlineProvider()) {
        val existingGlobalShops = cloud.fetchShopsOnce(currentUserId).getOrNull().orEmpty()
          .associateBy { it.id }
        val cachedShops = dao.observeCachedShops().first()
          .filter { it.userId == currentUserId || it.userId.isBlank() }
        for (entity in cachedShops) {
          val shop = normalizeForFirestore(entity.toDomainShop().copy(userId = currentUserId))
          val targetId = shop.id.ifBlank { shop.shopCode }
          val remote = existingGlobalShops[targetId]
          if (remote == null || entity.updatedAtMillis > remote.updatedAtMillis()) {
            val res = cloud.upsertShop(
              shop.copy(id = targetId),
              isUpdate = remote != null,
            )
            if (res.isSuccess) {
              globalMirrorCount++
            } else if (globalMirrorError == null) {
              globalMirrorError = res.exceptionOrNull()?.localizedMessage
            }
          }
        }

        val existingGlobalOrderIds = cloud.fetchOrdersOnce(currentUserId).getOrNull().orEmpty()
          .map { it.id }.toSet()
        val cachedOrders = dao.observeCachedOrders().first()
          .filter { it.userId == currentUserId || it.userId.isBlank() }
        for (entity in cachedOrders) {
          val orderId = entity.id.ifBlank { entity.orderCode }
          if (!existingGlobalOrderIds.contains(orderId)) {
            val res = cloud.placeRawOrder(
              orderDocId = orderId,
              orderCode = entity.orderCode.ifBlank { "ORD-${System.currentTimeMillis() % 9000 + 1000}" },
              shopId = entity.shopId.ifBlank { entity.shopCode.ifBlank { "shop_unknown" } },
              shopCode = entity.shopCode.ifBlank { "SHP-000" },
              shopName = entity.shopName.ifBlank { "Retail Shop" },
              routeId = entity.routeId.ifBlank { RoutesCatalog.UNALLOCATED },
              status = entity.status.ifBlank { OrderStatus.CONFIRMED },
              totalAmount = entity.totalAmount.coerceAtLeast(0.0),
              itemCount = entity.itemCount.coerceAtLeast(1),
              itemsSummary = entity.itemsSummary.ifBlank { "Order items" },
              orderDate = entity.orderDate.ifBlank { todayString() },
              note = entity.note,
            )
            if (res.isSuccess) {
              globalMirrorCount++
            } else if (globalMirrorError == null) {
              globalMirrorError = res.exceptionOrNull()?.localizedMessage
            }
          }
        }
      } else {
        globalMirrorError = "No internet"
      }
    }

    val remainingPending = dao.getPendingShopsOnce().count {
      !it.isFullySynced(requireGlobal, requireLaptop)
    } + dao.getPendingOrdersOnce().count {
      !it.isFullySynced(requireGlobal, requireLaptop)
    } + dao.getPendingStockUpdatesOnce().count {
      requireGlobal && !it.globalSynced
    } + dao.getPendingProductsOnce().count {
      !it.isFullySynced(requireGlobal)
    } + dao.getPendingPaymentsOnce().count {
      !it.isFullySynced(requireGlobal)
    } + dao.getPendingSupplierBillsOnce().count {
      !it.isFullySynced(requireGlobal)
    } + dao.getPendingSuppliersOnce().count {
      !it.isFullySynced(requireGlobal)
    }

    val totalSynced = syncedItems + laptopMirrorCount + globalMirrorCount
    val summary = when {
      syncedItems > 0 && autoDeleted > 0 ->
        "Synced $syncedItems queued & deleted $autoDeleted from phone"
      syncedItems > 0 && remainingPending == 0 ->
        "Synced $syncedItems queued + $laptopMirrorCount laptop / $globalMirrorCount cloud updates"
      syncedItems > 0 ->
        "Synced $syncedItems records • Remaining pending: $remainingPending"
      laptopMirrorError != null && remainingPending > 0 ->
        "Pending: $remainingPending • Laptop error: $laptopMirrorError"
      laptopMirrorError != null ->
        "Laptop sync error: $laptopMirrorError"
      globalMirrorError != null && remainingPending > 0 ->
        "Pending: $remainingPending • Cloud error: $globalMirrorError"
      remainingPending > 0 ->
        "Waiting for server • $remainingPending pending on phone"
      totalSynced > 0 ->
        "Synced $totalSynced record(s) across active servers"
      else ->
        "All records are up to date"
    }
    configStore.recordSyncOutcome(summary)

    SyncRunResult(
      syncedCount = syncedItems,
      remainingPendingCount = remainingPending,
      autoDeletedCount = autoDeleted,
      message = summary,
    )
  }

  suspend fun pullFromLaptopServer(): PullRunResult = syncMutex.withLock {
    val settings = configStore.getCurrentSettings()
    if (!settings.laptopServerEnabled || settings.laptopServerUrl.isBlank()) {
      return@withLock PullRunResult(0, 0, 0, "Enable Laptop Local Server first")
    }

    val shopsRes = laptopClient.fetchShops(settings.laptopServerUrl, settings.laptopApiToken)
    if (shopsRes.isFailure) {
      val err = shopsRes.exceptionOrNull()?.localizedMessage ?: "Failed to pull from laptop server"
      configStore.recordSyncOutcome(err)
      return@withLock PullRunResult(0, 0, 0, err)
    }

    val laptopShops = shopsRes.getOrNull().orEmpty()
    if (laptopShops.isNotEmpty()) {
      dao.upsertCachedShops(
        laptopShops.map {
          CachedShopEntity.fromDomainShop(it.copy(userId = currentUserId), source = "laptop")
        }
      )
    }

    val laptopOrders = laptopClient.fetchOrders(settings.laptopServerUrl, settings.laptopApiToken)
      .getOrNull().orEmpty()
    if (laptopOrders.isNotEmpty()) {
      dao.upsertCachedOrders(
        laptopOrders.map {
          CachedOrderEntity.fromDomainOrder(it.copy(userId = currentUserId), source = "laptop")
        }
      )
    }

    val laptopRoutes = laptopClient.fetchRoutes(settings.laptopServerUrl, settings.laptopApiToken)
      .getOrNull().orEmpty()
    if (laptopRoutes.isNotEmpty()) {
      dao.upsertRoutes(laptopRoutes.map { DeliveryRouteEntity.fromDomainRoute(it) })
      if (isLocalTestMode) {
        laptopRoutes.forEach { primary.saveRoute(it) }
      }
    }

    // Mirror pulled laptop shops & orders up to Global Server if authenticated & online
    var pushedToGlobal = 0
    val cloud = cloudAdapter
    if (settings.globalSyncEnabled && !isLocalTestMode && cloud != null && isOnlineProvider()) {
      val existingGlobalIds = cloud.fetchShopsOnce(currentUserId).getOrNull()
        ?.map { it.id }?.toSet().orEmpty()
      for (shop in laptopShops) {
        val targetId = shop.id.ifBlank { shop.shopCode }
        val isUpdate = existingGlobalIds.contains(targetId)
        val normalized = normalizeForFirestore(shop.copy(id = targetId, userId = currentUserId))
        val res = cloud.upsertShop(normalized, isUpdate = isUpdate)
        if (res.isSuccess) pushedToGlobal++
      }

      val existingOrderIds = cloud.fetchOrdersOnce(currentUserId).getOrNull()
        ?.map { it.id }?.toSet().orEmpty()
      for (order in laptopOrders) {
        val targetOrderId = order.id.ifBlank { order.orderCode }
        if (!existingOrderIds.contains(targetOrderId)) {
          val res = cloud.placeRawOrder(
            orderDocId = targetOrderId,
            orderCode = order.orderCode.ifBlank { "ORD-${System.currentTimeMillis() % 9000 + 1000}" },
            shopId = order.shopId.ifBlank { order.shopCode.ifBlank { "shop_unknown" } },
            shopCode = order.shopCode.ifBlank { "SHP-000" },
            shopName = order.shopName.ifBlank { "Retail Shop" },
            routeId = order.routeId.ifBlank { RoutesCatalog.UNALLOCATED },
            status = order.status.ifBlank { OrderStatus.CONFIRMED },
            totalAmount = order.totalAmount.coerceAtLeast(0.0),
            itemCount = order.itemCount.coerceAtLeast(1),
            itemsSummary = order.itemsSummary.ifBlank { "Order items" },
            orderDate = order.orderDate.ifBlank { todayString() },
            note = order.note,
          )
          if (res.isSuccess) pushedToGlobal++
        }
      }
    }

    val msg = buildString {
      append("Pulled ${laptopShops.size} shops & ${laptopOrders.size} orders from laptop")
      if (pushedToGlobal > 0) append(" • Synced $pushedToGlobal to Global Cloud")
    }
    configStore.recordSyncOutcome(msg)

    PullRunResult(
      shopsPulled = laptopShops.size,
      ordersPulled = laptopOrders.size,
      pushedToGlobal = pushedToGlobal,
      message = msg,
    )
  }

  suspend fun deleteSyncedFromPhone(): Int {
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()

    var removed = 0
    for (shop in dao.getPendingShopsOnce()) {
      if (shop.isFullySynced(requireGlobal, requireLaptop)) {
        dao.deletePendingShop(shop.localId)
        removed++
      }
    }
    for (order in dao.getPendingOrdersOnce()) {
      if (order.isFullySynced(requireGlobal, requireLaptop)) {
        dao.deletePendingOrder(order.localId)
        removed++
      }
    }
    for (stock in dao.getPendingStockUpdatesOnce()) {
      if (stock.globalSynced || !requireGlobal) {
        dao.deletePendingStockUpdate(stock.stockId)
        removed++
      }
    }
    for (product in dao.getPendingProductsOnce()) {
      if (product.isFullySynced(requireGlobal)) {
        dao.deletePendingProduct(product.localId)
        removed++
      }
    }
    for (payment in dao.getPendingPaymentsOnce()) {
      if (payment.isFullySynced(requireGlobal)) {
        dao.deletePendingPayment(payment.localId)
        removed++
      }
    }
    for (bill in dao.getPendingSupplierBillsOnce()) {
      if (bill.isFullySynced(requireGlobal)) {
        dao.deletePendingSupplierBill(bill.localId)
        removed++
      }
    }
    for (sup in dao.getPendingSuppliersOnce()) {
      if (sup.isFullySynced(requireGlobal)) {
        dao.deletePendingSupplier(sup.localId)
        removed++
      }
    }
    return removed
  }

  suspend fun deletePendingShopById(localId: String) {
    dao.deletePendingShop(localId)
  }

  suspend fun deletePendingOrderById(localId: String) {
    dao.deletePendingOrder(localId)
  }

  suspend fun deletePendingProductById(localId: String) {
    dao.deletePendingProduct(localId)
  }

  override suspend fun recordPayment(payment: PaymentTransaction): Result<String> {
    if (payment.amount <= 0.0) {
      return Result.failure(IllegalArgumentException("Payment amount must be greater than zero."))
    }
    val isUpdate = payment.id.isNotBlank()
    val targetId = if (isUpdate) {
      payment.id
    } else {
      "pay_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }
    val today = todayString()
    val prefix = if (payment.direction == PaymentDirection.PAY_SUPPLIER) "PAYOUT" else "RCPT"
    val normalizedPayment = normalizePaymentForFirestore(
      payment.copy(
        id = targetId,
        userId = currentUserId,
        receiptCode = payment.receiptCode.ifBlank {
          "$prefix-2026-${(System.currentTimeMillis() % 9000 + 1000)}"
        },
        paymentDate = payment.paymentDate.ifBlank { today },
      )
    )

    val initialPending = PendingPaymentEntity.fromDomainPayment(
      payment = normalizedPayment,
      localId = targetId,
      userId = currentUserId,
      isUpdate = isUpdate,
    )
    dao.upsertPendingPayment(initialPending)
    dao.upsertCachedPayments(
      listOf(CachedPaymentEntity.fromDomainPayment(normalizedPayment, fallbackUserId = currentUserId))
    )

    if (isLocalTestMode) {
      primary.recordPayment(normalizedPayment)
    }

    // If paying a supplier bill, update the matching SupplierBill's paidAmount & status as well
    if (!isUpdate && normalizedPayment.direction == PaymentDirection.PAY_SUPPLIER) {
      val existingBill = findSupplierBillForPayment(normalizedPayment)
      if (existingBill != null) {
        val newPaid = (existingBill.paidAmount + normalizedPayment.amount).coerceAtMost(existingBill.totalBilled)
        val updatedBill = existingBill.copy(
          paidAmount = newPaid,
          status = if (newPaid >= existingBill.totalBilled - 0.5) {
            SupplierBillStatus.SETTLED
          } else {
            PaymentLedgerHelper.effectiveSupplierStatus(existingBill.copy(paidAmount = newPaid))
          },
          updatedAt = Timestamp.now(),
        )
        saveSupplierBill(updatedBill)
      }
    }

    val settings = configStore.getCurrentSettings()
    val afterSync = pushPendingPaymentToServer(initialPending, settings)
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    if (afterSync.isFullySynced(requireGlobal)) {
      dao.upsertCachedPayments(
        listOf(CachedPaymentEntity.fromDomainPayment(afterSync.toDomainPayment(pendingSync = false), fallbackUserId = currentUserId))
      )
      if (settings.autoDeleteSyncedFromPhone) {
        dao.deletePendingPayment(afterSync.localId)
      } else {
        dao.upsertPendingPayment(afterSync)
      }
    } else {
      dao.upsertPendingPayment(afterSync)
    }
    return Result.success(targetId)
  }

  private suspend fun findSupplierBillForPayment(payment: PaymentTransaction): SupplierBill? {
    val fromPending = dao.getPendingSupplierBillsOnce().firstOrNull {
      it.localId == payment.partyId || it.remoteBillId == payment.partyId
    }?.toDomainBill(pendingSync = true)
    if (fromPending != null) return fromPending
    val fromCached = dao.getCachedSupplierBillsOnce().firstOrNull {
      it.id == payment.partyId || (payment.partyCode.isNotBlank() && it.supplierCode == payment.partyCode && it.billNumber == payment.invoiceRef)
    }?.toDomainBill()
    if (fromCached != null) return fromCached
    return runCatching {
      withTimeoutOrNull(800L) {
        primary.observeSupplierBills(currentUserId).first().firstOrNull {
          it.id == payment.partyId || (payment.partyCode.isNotBlank() && it.supplierCode == payment.partyCode)
        }
      }
    }.getOrNull()
  }

  override suspend fun saveSupplierBill(bill: SupplierBill): Result<String> {
    val isUpdate = bill.id.isNotBlank()
    val targetId = if (isUpdate) {
      bill.id
    } else {
      "sup_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }
    val today = todayString()
    val computedStatus = PaymentLedgerHelper.effectiveSupplierStatus(bill)
    val normalizedBill = normalizeSupplierBillForFirestore(
      bill.copy(
        id = targetId,
        userId = currentUserId,
        billDate = bill.billDate.ifBlank { today },
        dueDate = bill.dueDate.ifBlank { today },
        status = computedStatus,
      )
    )

    val initialPending = PendingSupplierBillEntity.fromDomainBill(
      bill = normalizedBill,
      localId = targetId,
      userId = currentUserId,
      isUpdate = isUpdate,
    )
    dao.upsertPendingSupplierBill(initialPending)
    dao.upsertCachedSupplierBills(
      listOf(CachedSupplierBillEntity.fromDomainBill(normalizedBill, fallbackUserId = currentUserId))
    )

    if (isLocalTestMode) {
      primary.saveSupplierBill(normalizedBill)
    }

    val settings = configStore.getCurrentSettings()
    val afterSync = pushPendingSupplierBillToServer(initialPending, settings)
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    if (afterSync.isFullySynced(requireGlobal)) {
      dao.upsertCachedSupplierBills(
        listOf(CachedSupplierBillEntity.fromDomainBill(afterSync.toDomainBill(pendingSync = false), fallbackUserId = currentUserId))
      )
      if (settings.autoDeleteSyncedFromPhone) {
        dao.deletePendingSupplierBill(afterSync.localId)
      } else {
        dao.upsertPendingSupplierBill(afterSync)
      }
    } else {
      dao.upsertPendingSupplierBill(afterSync)
    }
    return Result.success(targetId)
  }

  private suspend fun pushPendingPaymentToServer(
    entity: PendingPaymentEntity,
    settings: ServerSettings,
  ): PendingPaymentEntity {
    var globalOk = entity.globalSynced
    var errorMsg = ""
    val domainPayment = entity.toDomainPayment(pendingSync = false)
    val cloud = cloudAdapter
    if (settings.globalSyncEnabled && !isLocalTestMode && cloud != null && !globalOk) {
      if (isOnlineProvider()) {
        val res = cloud.upsertPayment(
          normalizePaymentForFirestore(domainPayment),
          isUpdate = entity.isUpdate,
        )
        if (res.isSuccess) {
          globalOk = true
        } else {
          errorMsg = "Cloud: ${res.exceptionOrNull()?.localizedMessage ?: "failed"}"
        }
      } else {
        errorMsg = "Cloud: No internet"
      }
    }
    return entity.copy(
      globalSynced = globalOk,
      lastError = errorMsg,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  private suspend fun pushPendingSupplierBillToServer(
    entity: PendingSupplierBillEntity,
    settings: ServerSettings,
  ): PendingSupplierBillEntity {
    var globalOk = entity.globalSynced
    var errorMsg = ""
    val domainBill = entity.toDomainBill(pendingSync = false)
    val cloud = cloudAdapter
    if (settings.globalSyncEnabled && !isLocalTestMode && cloud != null && !globalOk) {
      if (isOnlineProvider()) {
        val res = cloud.upsertSupplierBill(
          normalizeSupplierBillForFirestore(domainBill),
          isUpdate = entity.isUpdate,
        )
        if (res.isSuccess) {
          globalOk = true
        } else {
          errorMsg = "Cloud: ${res.exceptionOrNull()?.localizedMessage ?: "failed"}"
        }
      } else {
        errorMsg = "Cloud: No internet"
      }
    }
    return entity.copy(
      globalSynced = globalOk,
      lastError = errorMsg,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  private suspend fun pushPendingProductToServer(
    entity: PendingProductEntity,
    settings: ServerSettings,
  ): PendingProductEntity {
    var globalOk = entity.globalSynced
    var errorMsg = ""
    val domainProduct = entity.toDomainProduct(pendingSync = false)
    val cloud = cloudAdapter
    if (settings.globalSyncEnabled && !isLocalTestMode && cloud != null && !globalOk) {
      if (isOnlineProvider()) {
        val res = cloud.upsertProduct(
          normalizeProductForFirestore(domainProduct),
          isUpdate = entity.isUpdate,
        )
        if (res.isSuccess) {
          globalOk = true
        } else {
          errorMsg = "Cloud: ${res.exceptionOrNull()?.localizedMessage ?: "failed"}"
        }
      } else {
        errorMsg = "Cloud: No internet"
      }
    }
    return entity.copy(
      globalSynced = globalOk,
      lastError = errorMsg,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  private suspend fun pushPendingShopToServers(
    entity: PendingShopEntity,
    settings: ServerSettings,
  ): PendingShopEntity {
    var globalOk = entity.globalSynced
    var localOk = entity.localServerSynced
    val errors = mutableListOf<String>()
    val domainShop = entity.toDomainShop(pendingSync = false)

    val cloud = cloudAdapter
    if (settings.globalSyncEnabled && !isLocalTestMode && cloud != null && !globalOk) {
      if (isOnlineProvider()) {
        val res = cloud.upsertShop(
          normalizeForFirestore(domainShop),
          isUpdate = entity.isUpdate,
        )
        if (res.isSuccess) {
          globalOk = true
        } else {
          errors += "Cloud: ${res.exceptionOrNull()?.localizedMessage ?: "failed"}"
        }
      } else {
        errors += "Cloud: No internet"
      }
    }

    if (settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank() && !localOk) {
      val res = laptopClient.pushShop(settings.laptopServerUrl, settings.laptopApiToken, domainShop)
      if (res.isSuccess) {
        localOk = true
      } else {
        errors += "Laptop: ${res.exceptionOrNull()?.localizedMessage ?: "unreachable"}"
      }
    }

    return entity.copy(
      globalSynced = globalOk,
      localServerSynced = localOk,
      lastError = errors.joinToString(" | "),
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  private suspend fun pushPendingOrderToServers(
    entity: PendingOrderEntity,
    settings: ServerSettings,
  ): PendingOrderEntity {
    var globalOk = entity.globalSynced
    var localOk = entity.localServerSynced
    val errors = mutableListOf<String>()

    val cloud = cloudAdapter
    if (settings.globalSyncEnabled && !isLocalTestMode && cloud != null && !globalOk) {
      if (isOnlineProvider()) {
        val res = cloud.placeRawOrder(
          orderDocId = entity.localId,
          orderCode = entity.orderCode,
          shopId = entity.shopId,
          shopCode = entity.shopCode,
          shopName = entity.shopName,
          routeId = entity.routeId,
          status = entity.status,
          totalAmount = entity.totalAmount,
          itemCount = entity.itemCount,
          itemsSummary = entity.itemsSummary,
          orderDate = entity.orderDate,
          note = entity.note,
        )
        if (res.isSuccess) {
          globalOk = true
        } else {
          errors += "Cloud: ${res.exceptionOrNull()?.localizedMessage ?: "failed"}"
        }
      } else {
        errors += "Cloud: No internet"
      }
    }

    if (settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank() && !localOk) {
      val res = laptopClient.pushOrder(
        settings.laptopServerUrl,
        settings.laptopApiToken,
        entity.toDomainOrder(pendingSync = false),
      )
      if (res.isSuccess) {
        localOk = true
      } else {
        errors += "Laptop: ${res.exceptionOrNull()?.localizedMessage ?: "unreachable"}"
      }
    }

    return entity.copy(
      globalSynced = globalOk,
      localServerSynced = localOk,
      lastError = errors.joinToString(" | "),
    )
  }

  private fun normalizeForFirestore(shop: Shop): Shop {
    return shop.copy(
      shopCode = shop.shopCode.trim().ifBlank { "SHP-000" },
      name = shop.name.trim().ifBlank { "Unnamed Shop" },
      ownerName = shop.ownerName.trim().ifBlank { "Owner" },
      phone = shop.phone.trim().let { if (it.length >= 5) it else "00000" },
      address = shop.address.trim().ifBlank { "Address pending" },
      pinCode = shop.pinCode.trim().let { if (it.length >= 3) it else "000000" },
      routeId = shop.routeId.trim().let { if (it.length >= 2) it else RoutesCatalog.UNALLOCATED },
    )
  }

  private fun normalizeProductForFirestore(product: Product): Product {
    return product.copy(
      productCode = product.productCode.trim().ifBlank { "PRD-00100" }.take(32),
      sku = product.sku.trim().ifBlank { "SKU-001" }.take(40),
      name = product.name.trim().ifBlank { "Unnamed Product" }.take(140),
      category = product.category.trim().ifBlank { "Biscuits & Snacks" }.take(80),
      wholesalePrice = product.wholesalePrice.coerceAtLeast(0.0),
      mrp = product.mrp.coerceAtLeast(0.0),
      stockQuantity = product.stockQuantity.coerceIn(0, 1000000),
      minThreshold = product.minThreshold.coerceIn(0, 100000),
      status = if (product.status in listOf(ProductStatus.ACTIVE, ProductStatus.INACTIVE, ProductStatus.DRAFT)) {
        product.status
      } else {
        ProductStatus.ACTIVE
      },
      variantsJson = product.variantsJson.take(4000),
    )
  }

  private fun normalizePaymentForFirestore(payment: PaymentTransaction): PaymentTransaction {
    return payment.copy(
      receiptCode = payment.receiptCode.trim().ifBlank { "RCPT-2026-0001" }.take(40),
      direction = if (payment.direction in listOf(PaymentDirection.COLLECT, PaymentDirection.PAY_SUPPLIER)) {
        payment.direction
      } else {
        PaymentDirection.COLLECT
      },
      partyId = payment.partyId.trim().ifBlank { "party_unknown" }.take(128),
      partyName = payment.partyName.trim().ifBlank { "Party" }.take(140),
      amount = payment.amount.coerceAtLeast(0.01),
      paymentMethod = if (payment.paymentMethod in PaymentMethod.allMethods) payment.paymentMethod else PaymentMethod.UPI,
      paymentDate = payment.paymentDate.trim().ifBlank { todayString() }.take(32),
      note = payment.note.trim().take(500),
    )
  }

  private fun normalizeSupplierBillForFirestore(bill: SupplierBill): SupplierBill {
    return bill.copy(
      supplierCode = bill.supplierCode.trim().ifBlank { "SUP-001" }.take(32),
      supplierName = bill.supplierName.trim().ifBlank { "Supplier" }.take(140),
      billNumber = bill.billNumber.trim().ifBlank { "BILL-001" }.take(40),
      totalBilled = bill.totalBilled.coerceAtLeast(0.0),
      paidAmount = bill.paidAmount.coerceAtLeast(0.0),
      dueDate = bill.dueDate.trim().ifBlank { todayString() }.take(32),
      status = if (bill.status in listOf(SupplierBillStatus.PENDING, SupplierBillStatus.OVERDUE, SupplierBillStatus.SETTLED)) {
        bill.status
      } else {
        SupplierBillStatus.PENDING
      },
      notes = bill.notes.trim().take(500),
    )
  }

  override suspend fun saveSupplier(supplier: Supplier): Result<String> {
    val isUpdate = supplier.id.isNotBlank()
    val targetId = if (isUpdate) {
      supplier.id
    } else {
      "supdir_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }
    val today = todayString()
    val normalizedSupplier = normalizeSupplierForFirestore(
      supplier.copy(
        id = targetId,
        userId = currentUserId,
        addedDate = supplier.addedDate.ifBlank { today },
      )
    )

    val initialPending = PendingSupplierEntity.fromDomainSupplier(
      supplier = normalizedSupplier,
      localId = targetId,
      userId = currentUserId,
      isUpdate = isUpdate,
    )
    dao.upsertPendingSupplier(initialPending)
    dao.upsertCachedSuppliers(
      listOf(CachedSupplierEntity.fromDomainSupplier(normalizedSupplier, fallbackUserId = currentUserId))
    )

    if (isLocalTestMode) {
      primary.saveSupplier(normalizedSupplier)
    }

    val settings = configStore.getCurrentSettings()
    val afterSync = pushPendingSupplierToServer(initialPending, settings)
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    if (afterSync.isFullySynced(requireGlobal)) {
      dao.upsertCachedSuppliers(
        listOf(CachedSupplierEntity.fromDomainSupplier(afterSync.toDomainSupplier(pendingSync = false), fallbackUserId = currentUserId))
      )
      if (settings.autoDeleteSyncedFromPhone) {
        dao.deletePendingSupplier(afterSync.localId)
      } else {
        dao.upsertPendingSupplier(afterSync)
      }
    } else {
      dao.upsertPendingSupplier(afterSync)
    }
    return Result.success(targetId)
  }

  override suspend fun updateSupplierStatus(supplierId: String, newStatus: String): Result<Unit> {
    val now = System.currentTimeMillis()
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode

    if (isLocalTestMode) {
      primary.updateSupplierStatus(supplierId, newStatus)
    }

    val pending = dao.getPendingSuppliersOnce().firstOrNull {
      it.localId == supplierId || it.remoteSupplierId == supplierId
    }
    val cached = dao.getCachedSuppliersOnce().firstOrNull { it.id == supplierId }
    val baseSupplier = pending?.toDomainSupplier(pendingSync = true)
      ?: cached?.toDomainSupplier()
      ?: runCatching {
        withTimeoutOrNull(800L) {
          primary.observeSuppliers(currentUserId).first().firstOrNull { it.id == supplierId }
        }
      }.getOrNull()

    if (baseSupplier == null) {
      val cloud = cloudAdapter
      return if (requireGlobal && cloud != null && isOnlineProvider()) {
        cloud.updateSupplierStatus(supplierId, newStatus)
      } else if (isLocalTestMode) {
        primary.updateSupplierStatus(supplierId, newStatus)
      } else {
        Result.failure(IllegalStateException("Supplier not cached locally yet"))
      }
    }

    val updatedSupplier = normalizeSupplierForFirestore(
      baseSupplier.copy(
        userId = currentUserId,
        status = newStatus,
        updatedAt = Timestamp(Date(now)),
      )
    )

    dao.upsertCachedSuppliers(
      listOf(CachedSupplierEntity.fromDomainSupplier(updatedSupplier, fallbackUserId = currentUserId).copy(updatedAtMillis = now))
    )

    var globalOk = pending?.globalSynced ?: false
    var errorMsg = ""
    val cloud = cloudAdapter
    if (requireGlobal && cloud != null) {
      globalOk = false
      if (isOnlineProvider()) {
        val directRes = cloud.updateSupplierStatus(supplierId, newStatus)
        if (directRes.isSuccess) {
          globalOk = true
        } else {
          val err = directRes.exceptionOrNull()
          val isNotFound = err is FirebaseFirestoreException && err.code == FirebaseFirestoreException.Code.NOT_FOUND
          if (isNotFound) {
            val createRes = cloud.upsertSupplier(updatedSupplier, isUpdate = false)
            if (createRes.isSuccess) {
              globalOk = true
            } else {
              errorMsg = "Cloud: ${createRes.exceptionOrNull()?.localizedMessage ?: "failed"}"
            }
          } else {
            errorMsg = "Cloud: ${err?.localizedMessage ?: "failed"}"
          }
        }
      } else {
        errorMsg = "Cloud: No internet"
      }
    }

    val needsRetryQueue = requireGlobal && !globalOk
    if (needsRetryQueue || pending != null) {
      val entity = PendingSupplierEntity.fromDomainSupplier(
        supplier = updatedSupplier,
        localId = pending?.localId ?: supplierId,
        userId = currentUserId,
        isUpdate = pending?.isUpdate ?: true,
      ).copy(
        remoteSupplierId = pending?.remoteSupplierId?.ifBlank { supplierId } ?: supplierId,
        globalSynced = globalOk,
        lastError = errorMsg,
        createdAtMillis = pending?.createdAtMillis ?: now,
        updatedAtMillis = now,
      )
      val done = entity.isFullySynced(requireGlobal)
      if (done && settings.autoDeleteSyncedFromPhone && pending != null) {
        dao.deletePendingSupplier(entity.localId)
      } else if (!done || pending != null) {
        dao.upsertPendingSupplier(entity)
      }
    }
    return Result.success(Unit)
  }

  private suspend fun pushPendingSupplierToServer(
    entity: PendingSupplierEntity,
    settings: ServerSettings,
  ): PendingSupplierEntity {
    var globalOk = entity.globalSynced
    var errorMsg = ""
    val domainSupplier = entity.toDomainSupplier(pendingSync = false)
    val cloud = cloudAdapter
    if (settings.globalSyncEnabled && !isLocalTestMode && cloud != null && !globalOk) {
      if (isOnlineProvider()) {
        val res = cloud.upsertSupplier(
          normalizeSupplierForFirestore(domainSupplier),
          isUpdate = entity.isUpdate,
        )
        if (res.isSuccess) {
          globalOk = true
        } else {
          errorMsg = "Cloud: ${res.exceptionOrNull()?.localizedMessage ?: "failed"}"
        }
      } else {
        errorMsg = "Cloud: No internet"
      }
    }
    return entity.copy(
      globalSynced = globalOk,
      lastError = errorMsg,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  private fun normalizeSupplierForFirestore(supplier: Supplier): Supplier {
    return supplier.copy(
      supplierCode = supplier.supplierCode.trim().ifBlank { "SUP-000001" }.take(32),
      name = supplier.name.trim().ifBlank { "Unnamed Supplier" }.take(140),
      category = supplier.category.trim().ifBlank { "Wholesale Staples & Grains" }.take(80),
      contactPerson = supplier.contactPerson.trim().ifBlank { "Contact Person" }.take(100),
      contactRole = supplier.contactRole.trim().take(64),
      altContactPerson = supplier.altContactPerson.trim().take(100),
      phone = supplier.phone.trim().let { if (it.length >= 5) it else "00000" }.take(24),
      altPhone = supplier.altPhone.trim().take(24),
      email = supplier.email.trim().take(120),
      address = supplier.address.trim().ifBlank { "Address pending" }.take(300),
      pinCode = supplier.pinCode.trim().let { if (it.length >= 3) it else "000000" }.take(16),
      mapsLink = supplier.mapsLink.trim().take(600),
      photoUri = supplier.photoUri.take(600),
      gstin = supplier.gstin.trim().take(32),
      creditTerms = supplier.creditTerms.trim().take(64),
      confirmedPriceNotes = supplier.confirmedPriceNotes.trim().take(600),
      notes = supplier.notes.trim().take(600),
      status = if (supplier.status in listOf(SupplierStatus.ACTIVE, SupplierStatus.INACTIVE, SupplierStatus.DELETED)) {
        supplier.status
      } else {
        SupplierStatus.ACTIVE
      },
      addedDate = supplier.addedDate.trim().ifBlank { todayString() }.take(32),
      addedBy = supplier.addedBy.trim().take(120),
    )
  }

  private fun isPureCancellation(error: Throwable): Boolean {
    val hasFirestore = generateSequence(error) { it.cause }
      .any { it is FirebaseFirestoreException }
    return error is CancellationException && !hasFirestore
  }

  private fun Shop.updatedAtMillis(): Long = updatedAt?.toDate()?.time ?: 0L

  private fun OrderRecord.updatedAtMillis(): Long = updatedAt?.toDate()?.time ?: createdAt?.toDate()?.time ?: 0L

  private fun StockItem.updatedAtMillis(): Long = updatedAt?.toDate()?.time ?: createdAt?.toDate()?.time ?: 0L

  private fun Product.updatedAtMillis(): Long = updatedAt?.toDate()?.time ?: createdAt?.toDate()?.time ?: 0L

  private fun PaymentTransaction.updatedAtMillis(): Long = updatedAt?.toDate()?.time ?: createdAt?.toDate()?.time ?: 0L

  private fun SupplierBill.updatedAtMillis(): Long = updatedAt?.toDate()?.time ?: createdAt?.toDate()?.time ?: 0L

  private fun Supplier.updatedAtMillis(): Long = updatedAt?.toDate()?.time ?: createdAt?.toDate()?.time ?: 0L

  private fun todayString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }
}
