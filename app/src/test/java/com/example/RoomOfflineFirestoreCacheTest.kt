package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ServerConfigStore
import com.example.data.local.SnackRouteDatabase
import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.StockItem
import com.example.data.model.Supplier
import com.example.data.model.SupplierBill
import com.example.data.remote.LaptopServerClient
import com.example.data.repository.RemoteCloudAdapter
import com.example.data.repository.SnackRouteDataSource
import com.example.data.repository.SyncCoordinatorRepository
import com.example.ui.viewmodel.SnackRouteViewModel
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import android.os.Looper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomOfflineFirestoreCacheTest {

  private lateinit var context: Context
  private lateinit var roomDb: SnackRouteDatabase
  private lateinit var configStore: ServerConfigStore

  private var networkOnline = true
  private val testUserId = "uid_cloud_test_01"

  private class FakeCloudBackend : SnackRouteDataSource, RemoteCloudAdapter {
    var failUpstreamStream = false
    val shopsStore = ConcurrentHashMap<String, Shop>()
    val ordersStore = ConcurrentHashMap<String, OrderRecord>()
    val stocksStore = ConcurrentHashMap<String, StockItem>()
    val productsStore = ConcurrentHashMap<String, Product>()
    val paymentsStore = ConcurrentHashMap<String, PaymentTransaction>()
    val supplierBillsStore = ConcurrentHashMap<String, SupplierBill>()
    val suppliersStore = ConcurrentHashMap<String, Supplier>()
    val metaFlow = MutableStateFlow<OperationsMeta?>(null)
    val shopsFlow = MutableStateFlow<List<Shop>>(emptyList())
    val ordersFlow = MutableStateFlow<List<OrderRecord>>(emptyList())
    val stocksFlow = MutableStateFlow<List<StockItem>>(emptyList())
    val productsFlow = MutableStateFlow<List<Product>>(emptyList())
    val paymentsFlow = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    val supplierBillsFlow = MutableStateFlow<List<SupplierBill>>(emptyList())
    val suppliersFlow = MutableStateFlow<List<Supplier>>(emptyList())
    val routesFlow = MutableStateFlow<List<DeliveryRoute>>(RoutesCatalog.starterRoutes)

    fun seedRemoteOnline(
      shops: List<Shop>,
      orders: List<OrderRecord>,
      stocks: List<StockItem>,
      meta: OperationsMeta,
    ) {
      shopsStore.clear()
      shops.forEach { shopsStore[it.id] = it }
      ordersStore.clear()
      orders.forEach { ordersStore[it.id] = it }
      stocksStore.clear()
      stocks.forEach { stocksStore[it.id] = it }
      shopsFlow.value = shopsStore.values.toList()
      ordersFlow.value = ordersStore.values.toList()
      stocksFlow.value = stocksStore.values.toList()
      metaFlow.value = meta
    }

    override fun observeShops(userId: String): Flow<List<Shop>> =
      if (failUpstreamStream) {
        flow { throw FirebaseFirestoreException("Offline stream error", FirebaseFirestoreException.Code.UNAVAILABLE) }
      } else {
        shopsFlow
      }

    override fun observeOrders(userId: String): Flow<List<OrderRecord>> =
      if (failUpstreamStream) {
        flow { throw FirebaseFirestoreException("Offline stream error", FirebaseFirestoreException.Code.UNAVAILABLE) }
      } else {
        ordersFlow
      }

    override fun observeStocks(userId: String): Flow<List<StockItem>> =
      if (failUpstreamStream) {
        flow { throw FirebaseFirestoreException("Offline stream error", FirebaseFirestoreException.Code.UNAVAILABLE) }
      } else {
        stocksFlow
      }

    override fun observeProducts(userId: String): Flow<List<Product>> =
      if (failUpstreamStream) {
        flow { throw FirebaseFirestoreException("Offline stream error", FirebaseFirestoreException.Code.UNAVAILABLE) }
      } else {
        productsFlow
      }

    override fun observePayments(userId: String): Flow<List<PaymentTransaction>> =
      if (failUpstreamStream) {
        flow { throw FirebaseFirestoreException("Offline stream error", FirebaseFirestoreException.Code.UNAVAILABLE) }
      } else {
        paymentsFlow
      }

    override fun observeSupplierBills(userId: String): Flow<List<SupplierBill>> =
      if (failUpstreamStream) {
        flow { throw FirebaseFirestoreException("Offline stream error", FirebaseFirestoreException.Code.UNAVAILABLE) }
      } else {
        supplierBillsFlow
      }

    override fun observeSuppliers(userId: String): Flow<List<Supplier>> =
      if (failUpstreamStream) {
        flow { throw FirebaseFirestoreException("Offline stream error", FirebaseFirestoreException.Code.UNAVAILABLE) }
      } else {
        suppliersFlow
      }

    override fun observeMeta(userId: String): Flow<OperationsMeta?> =
      if (failUpstreamStream) {
        flow { throw FirebaseFirestoreException("Offline stream error", FirebaseFirestoreException.Code.UNAVAILABLE) }
      } else {
        metaFlow
      }

    override fun observeRoutes(): Flow<List<DeliveryRoute>> = routesFlow

    override suspend fun seedStarterDataIfNeeded(): Result<Unit> = Result.success(Unit)

    override suspend fun seedRoutesIfNeeded(): Result<Unit> = Result.success(Unit)

    override suspend fun saveRoute(route: DeliveryRoute): Result<Unit> = Result.success(Unit)

    override suspend fun saveShop(shop: Shop): Result<String> = upsertShop(shop, shop.id.isNotBlank())

    override suspend fun upsertShop(shop: Shop, isUpdate: Boolean): Result<String> {
      val id = shop.id.ifBlank { "remote_${UUID.randomUUID()}" }
      val saved = shop.copy(id = id)
      shopsStore[id] = saved
      shopsFlow.value = shopsStore.values.toList()
      return Result.success(id)
    }

    override suspend fun updateShopRoute(shopId: String, newRouteId: String): Result<Unit> {
      val cur = shopsStore[shopId]
        ?: return Result.failure(IllegalStateException("Shop not found on cloud"))
      shopsStore[shopId] = cur.copy(routeId = newRouteId)
      shopsFlow.value = shopsStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun updateShopStatus(shopId: String, newStatus: String): Result<Unit> {
      val cur = shopsStore[shopId] ?: return Result.failure(IllegalStateException("Missing"))
      shopsStore[shopId] = cur.copy(status = newStatus)
      shopsFlow.value = shopsStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun updateShopInquiryStatus(
      shopId: String,
      inquiryStatus: String,
      markCalledToday: Boolean,
    ): Result<Unit> {
      val cur = shopsStore[shopId] ?: return Result.failure(IllegalStateException("Missing"))
      shopsStore[shopId] = cur.copy(inquiryStatus = inquiryStatus)
      shopsFlow.value = shopsStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun placeOrder(
      shop: Shop,
      items: List<Pair<CatalogProduct, Int>>,
      note: String,
    ): Result<String> = Result.failure(UnsupportedOperationException())

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
    ): Result<String> {
      val rec = OrderRecord(
        id = orderDocId,
        userId = shopsStore[shopId]?.userId.orEmpty(),
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
      )
      ordersStore[orderDocId] = rec
      ordersFlow.value = ordersStore.values.toList()
      return Result.success(orderDocId)
    }

    override suspend fun updateOrderStatus(
      orderId: String,
      newStatus: String,
      note: String?,
    ): Result<Unit> {
      val cur = ordersStore[orderId] ?: return Result.failure(IllegalStateException("Order not found"))
      ordersStore[orderId] = cur.copy(status = newStatus, note = note ?: cur.note)
      ordersFlow.value = ordersStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun updateOrderDetails(
      orderId: String,
      items: List<Pair<CatalogProduct, Int>>,
      note: String,
    ): Result<Unit> {
      val cur = ordersStore[orderId] ?: return Result.failure(IllegalStateException("Order not found"))
      val totalAmount = items.sumOf { it.first.defaultPrice * it.second }
      val itemCount = items.sumOf { it.second }
      val itemsSummary = items.joinToString(", ") { "${it.first.code} x${it.second}" }
      ordersStore[orderId] = cur.copy(
        totalAmount = totalAmount,
        itemCount = itemCount,
        itemsSummary = itemsSummary,
        note = note,
      )
      ordersFlow.value = ordersStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun updateOrderDetails(
      orderId: String,
      totalAmount: Double,
      itemCount: Int,
      itemsSummary: String,
      note: String,
    ): Result<Unit> {
      val cur = ordersStore[orderId] ?: return Result.failure(IllegalStateException("Order not found"))
      ordersStore[orderId] = cur.copy(
        totalAmount = totalAmount,
        itemCount = itemCount,
        itemsSummary = itemsSummary,
        note = note,
      )
      ordersFlow.value = ordersStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun deleteOrder(orderId: String): Result<Unit> {
      ordersStore.remove(orderId)
      ordersFlow.value = ordersStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun updateStockQuantity(stockId: String, newQuantity: Int): Result<Unit> {
      val cur = stocksStore[stockId]
        ?: return Result.failure(IllegalStateException("Stock not found"))
      stocksStore[stockId] = cur.copy(quantity = newQuantity)
      stocksFlow.value = stocksStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun fetchShopsOnce(userId: String): Result<List<Shop>> =
      Result.success(shopsStore.values.toList())

    override suspend fun fetchOrdersOnce(userId: String): Result<List<OrderRecord>> =
      Result.success(ordersStore.values.toList())

    override suspend fun saveProduct(product: Product): Result<String> =
      upsertProduct(product, product.id.isNotBlank())

    override suspend fun upsertProduct(product: Product, isUpdate: Boolean): Result<String> {
      val id = product.id.ifBlank { "remote_prd_${UUID.randomUUID()}" }
      val saved = product.copy(id = id)
      productsStore[id] = saved
      productsFlow.value = productsStore.values.toList()
      return Result.success(id)
    }

    override suspend fun updateProductStatus(productId: String, newStatus: String): Result<Unit> {
      val cur = productsStore[productId] ?: return Result.failure(IllegalStateException("Missing product"))
      productsStore[productId] = cur.copy(status = newStatus)
      productsFlow.value = productsStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun updateProductStock(productId: String, newQuantity: Int): Result<Unit> {
      val cur = productsStore[productId] ?: return Result.failure(IllegalStateException("Missing product"))
      productsStore[productId] = cur.copy(stockQuantity = newQuantity)
      productsFlow.value = productsStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun fetchProductsOnce(userId: String): Result<List<Product>> =
      Result.success(productsStore.values.toList())

    override suspend fun recordPayment(payment: PaymentTransaction): Result<String> =
      upsertPayment(payment, payment.id.isNotBlank())

    override suspend fun upsertPayment(payment: PaymentTransaction, isUpdate: Boolean): Result<String> {
      val id = payment.id.ifBlank { "remote_pay_${UUID.randomUUID()}" }
      val saved = payment.copy(id = id)
      paymentsStore[id] = saved
      paymentsFlow.value = paymentsStore.values.toList()
      return Result.success(id)
    }

    override suspend fun saveSupplierBill(bill: SupplierBill): Result<String> =
      upsertSupplierBill(bill, bill.id.isNotBlank())

    override suspend fun upsertSupplierBill(bill: SupplierBill, isUpdate: Boolean): Result<String> {
      val id = bill.id.ifBlank { "remote_sup_${UUID.randomUUID()}" }
      val saved = bill.copy(id = id)
      supplierBillsStore[id] = saved
      supplierBillsFlow.value = supplierBillsStore.values.toList()
      return Result.success(id)
    }

    override suspend fun fetchPaymentsOnce(userId: String): Result<List<PaymentTransaction>> =
      Result.success(paymentsStore.values.toList())

    override suspend fun fetchSupplierBillsOnce(userId: String): Result<List<SupplierBill>> =
      Result.success(supplierBillsStore.values.toList())

    override suspend fun saveSupplier(supplier: Supplier): Result<String> =
      upsertSupplier(supplier, supplier.id.isNotBlank())

    override suspend fun upsertSupplier(supplier: Supplier, isUpdate: Boolean): Result<String> {
      val id = supplier.id.ifBlank { "remote_sup_dir_${UUID.randomUUID()}" }
      val saved = supplier.copy(id = id)
      suppliersStore[id] = saved
      suppliersFlow.value = suppliersStore.values.toList()
      return Result.success(id)
    }

    override suspend fun updateSupplierStatus(supplierId: String, newStatus: String): Result<Unit> {
      val cur = suppliersStore[supplierId] ?: return Result.failure(IllegalStateException("Missing supplier"))
      suppliersStore[supplierId] = cur.copy(status = newStatus)
      suppliersFlow.value = suppliersStore.values.toList()
      return Result.success(Unit)
    }

    override suspend fun fetchSuppliersOnce(userId: String): Result<List<Supplier>> =
      Result.success(suppliersStore.values.toList())
  }

  private lateinit var fakeCloud: FakeCloudBackend
  private lateinit var coordinator: SyncCoordinatorRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    roomDb = Room.inMemoryDatabaseBuilder(context, SnackRouteDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    configStore = ServerConfigStore(context)
    fakeCloud = FakeCloudBackend()
    coordinator = SyncCoordinatorRepository(
      appContext = context,
      primary = fakeCloud,
      dao = roomDb.snackRouteDao(),
      configStore = configStore,
      laptopClient = LaptopServerClient(),
      currentUserId = testUserId,
      isLocalTestMode = false,
      cloudAdapter = fakeCloud,
      isOnlineProvider = { networkOnline },
    )
  }

  @After
  fun tearDown() {
    roomDb.close()
  }

  private suspend fun <T> step(name: String, block: suspend () -> T): T {
    return try {
      withTimeout(5000L) { block() }
    } catch (e: Throwable) {
      throw AssertionError("Failed/timed out at step [$name]: ${e.message}", e)
    }
  }

  @Test
  fun roomCachesFirestoreDataOffline_queuesMutations_andReplaysOnReconnect() = runBlocking {
    step("updateServerSettings") {
      coordinator.updateServerSettings(
        globalSyncEnabled = true,
        laptopServerEnabled = false,
        laptopServerUrl = "",
        laptopApiToken = "",
        autoDeleteSyncedFromPhone = false,
      )
    }

    // 1. Online phase: Cloud emits shops, orders, stocks, and meta -> Room caches everything
    fakeCloud.seedRemoteOnline(
      shops = listOf(
        Shop(
          id = "cloud_shop_1",
          userId = testUserId,
          shopCode = "SHP-301",
          name = "Kannur Central Traders",
          ownerName = "Rafiq",
          phone = "9847123456",
          address = "Bank Road, Kannur",
          pinCode = "670001",
          routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        )
      ),
      orders = listOf(
        OrderRecord(
          id = "cloud_ord_1",
          userId = testUserId,
          orderCode = "ORD-9001",
          shopId = "cloud_shop_1",
          shopCode = "SHP-301",
          shopName = "Kannur Central Traders",
          routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
          totalAmount = 900.0,
          itemCount = 20,
          itemsSummary = "BC100 x20",
          orderDate = "2026-11-17",
        )
      ),
      stocks = listOf(
        StockItem(
          id = "stk_bc100",
          userId = testUserId,
          productCode = "BC100",
          productName = "Banana Chips",
          packSize = "100g",
          category = "Banana Chips",
          quantity = 40,
          minThreshold = 20,
          unitPrice = 45.0,
        )
      ),
      meta = OperationsMeta(
        id = "summary",
        userId = testUserId,
        shopRegTarget = 15,
        toCollectAmount = 52000.0,
      ),
    )

    step("warmOnlineCache") {
      assertEquals(1, coordinator.observeShops(testUserId).first { it.isNotEmpty() }.size)
      assertEquals(1, coordinator.observeOrders(testUserId).first { it.isNotEmpty() }.size)
      assertEquals(1, coordinator.observeStocks(testUserId).first { it.isNotEmpty() }.size)
      assertNotNull(coordinator.observeMeta(testUserId).first { it != null })
    }

    // 2. Disconnect internet AND make Firestore stream fail (simulating cold offline start)
    networkOnline = false
    fakeCloud.failUpstreamStream = true

    val offlineShops = step("offlineObserveShops") {
      coordinator.observeShops(testUserId).first { it.isNotEmpty() }
    }
    val offlineOrders = step("offlineObserveOrders") {
      coordinator.observeOrders(testUserId).first { it.isNotEmpty() }
    }
    val offlineStocks = step("offlineObserveStocks") {
      coordinator.observeStocks(testUserId).first { it.isNotEmpty() }
    }
    val offlineMeta = step("offlineObserveMeta") {
      coordinator.observeMeta(testUserId).first { it != null }
    }

    assertEquals("Kannur Central Traders", offlineShops.first().name)
    assertEquals("ORD-9001", offlineOrders.first().orderCode)
    assertEquals(40, offlineStocks.first().quantity)
    assertEquals(52000.0, offlineMeta!!.toCollectAmount, 0.01)

    // 3. Perform offline mutations: add shop, update route, place order, update stock
    val createdShopId = step("offlineSaveShop") {
      coordinator.saveShop(
        Shop(
          shopCode = "SHP-OFF-9",
          name = "Offline Provision Store",
          ownerName = "Deepak",
          phone = "9847099887",
          address = "Talap, Kannur",
          pinCode = "670002",
          routeId = RoutesCatalog.R02_KANNUR_KOZHIKODE,
        )
      ).getOrThrow()
    }

    step("offlineUpdateShopRoute") {
      coordinator.updateShopRoute("cloud_shop_1", RoutesCatalog.R03_BANGALORE_PUNE)
    }

    val product = CatalogProduct("BC100", "Banana Chips", "100g", "Banana Chips", 45.0)
    val offlineOrderId = step("offlinePlaceOrder") {
      coordinator.placeOrder(
        shop = offlineShops.first(),
        items = listOf(product to 12),
        note = "Deliver evening",
      ).getOrThrow()
    }

    step("offlineUpdateStock") {
      coordinator.updateStockQuantity("stk_bc100", 75)
    }

    // Verify Room immediately exposes all offline changes while offline
    val shopsWithOffline = step("verifyOfflineShopsFlow") {
      coordinator.observeShops(testUserId).first { list -> list.any { it.id == createdShopId } }
    }
    assertTrue(shopsWithOffline.any { it.id == createdShopId && it.isPendingSync })
    assertEquals(RoutesCatalog.R03_BANGALORE_PUNE, shopsWithOffline.first { it.id == "cloud_shop_1" }.routeId)

    val ordersWithOffline = step("verifyOfflineOrdersFlow") {
      coordinator.observeOrders(testUserId).first { list -> list.any { it.id == offlineOrderId } }
    }
    assertTrue(ordersWithOffline.any { it.id == offlineOrderId && it.isPendingSync })

    val stocksWithOffline = step("verifyOfflineStocksFlow") {
      coordinator.observeStocks(testUserId).first { list -> list.any { it.quantity == 75 } }
    }
    assertEquals(75, stocksWithOffline.first().quantity)

    assertEquals(1, step("verifyPendingStockRow") { coordinator.observePendingStockUpdates().first().size })

    // 4. Restore connectivity & trigger sync -> replays everything to Firestore Cloud
    networkOnline = true
    fakeCloud.failUpstreamStream = false

    val syncOutcome = step("syncAllPendingOnReconnect") {
      coordinator.syncAllPending()
    }
    assertEquals(0, syncOutcome.remainingPendingCount)

    val vm = SnackRouteViewModel(coordinator, testUserId)
    vm.onNetworkAvailable()
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    // Confirm Cloud received all offline mutations
    assertTrue(fakeCloud.shopsStore.containsKey(createdShopId))
    assertEquals(RoutesCatalog.R03_BANGALORE_PUNE, fakeCloud.shopsStore["cloud_shop_1"]?.routeId)
    assertTrue(fakeCloud.ordersStore.containsKey(offlineOrderId))
    assertEquals(75, fakeCloud.stocksStore["stk_bc100"]?.quantity)
    assertEquals(0, step("verifyClearedPendingStock") { coordinator.observePendingStockUpdates().first().size })
  }

  @Test
  fun migration3To4AddsCacheTablesOnExistingV3Database() {
    val dbFile = File(context.cacheDir, "migration_v3_v4_${System.currentTimeMillis()}.db")
    try {
      val openHelper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
          .name(dbFile.absolutePath)
          .callback(object : SupportSQLiteOpenHelper.Callback(3) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `delivery_routes` (
                  `id` TEXT NOT NULL,
                  `name` TEXT NOT NULL,
                  `vehicle` TEXT NOT NULL,
                  `maxCrates` INTEGER NOT NULL,
                  `driverName` TEXT NOT NULL,
                  `driverPhone` TEXT NOT NULL,
                  `dispatchWindow` TEXT NOT NULL,
                  `stagingBay` TEXT NOT NULL,
                  `colorIndex` INTEGER NOT NULL,
                  `isActive` INTEGER NOT NULL,
                  `stopOrderIdsCsv` TEXT NOT NULL,
                  `updatedAtMillis` INTEGER NOT NULL,
                  PRIMARY KEY(`id`)
                )
                """.trimIndent()
              )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          })
          .build()
      )
      val sqlite = openHelper.writableDatabase
      SnackRouteDatabase.MIGRATION_3_4.migrate(sqlite)

      val cursor = sqlite.query(
        "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('cached_stocks','pending_stock_updates','cached_meta')"
      )
      val tables = mutableSetOf<String>()
      while (cursor.moveToNext()) {
        tables.add(cursor.getString(0))
      }
      cursor.close()
      sqlite.close()

      assertEquals(setOf("cached_stocks", "pending_stock_updates", "cached_meta"), tables)
    } finally {
      dbFile.delete()
    }
  }

  @Test
  fun migration7To8AddsOrderUpdateAndDeleteColumns() {
    val dbFile = File(context.cacheDir, "migration_v7_v8_${System.currentTimeMillis()}.db")
    try {
      val openHelper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
          .name(dbFile.absolutePath)
          .callback(object : SupportSQLiteOpenHelper.Callback(7) {
            override fun onCreate(db: SupportSQLiteDatabase) {
              db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pending_orders` (
                  `localId` TEXT NOT NULL,
                  `orderCode` TEXT NOT NULL,
                  `userId` TEXT NOT NULL,
                  `shopId` TEXT NOT NULL,
                  `shopCode` TEXT NOT NULL,
                  `shopName` TEXT NOT NULL,
                  `routeId` TEXT NOT NULL,
                  `status` TEXT NOT NULL,
                  `totalAmount` REAL NOT NULL,
                  `itemCount` INTEGER NOT NULL,
                  `itemsSummary` TEXT NOT NULL,
                  `orderDate` TEXT NOT NULL,
                  `note` TEXT NOT NULL,
                  `globalSynced` INTEGER NOT NULL,
                  `localServerSynced` INTEGER NOT NULL,
                  `lastError` TEXT NOT NULL,
                  `createdAtMillis` INTEGER NOT NULL,
                  PRIMARY KEY(`localId`)
                )
                """.trimIndent()
              )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
          })
          .build()
      )
      val sqlite = openHelper.writableDatabase
      SnackRouteDatabase.MIGRATION_7_8.migrate(sqlite)

      val cursor = sqlite.query("PRAGMA table_info(`pending_orders`)")
      val columns = mutableSetOf<String>()
      val nameIndex = cursor.getColumnIndexOrThrow("name")
      while (cursor.moveToNext()) {
        columns.add(cursor.getString(nameIndex))
      }
      cursor.close()
      sqlite.close()

      assertTrue(columns.contains("isUpdate"))
      assertTrue(columns.contains("pendingDelete"))
    } finally {
      dbFile.delete()
    }
  }
}
