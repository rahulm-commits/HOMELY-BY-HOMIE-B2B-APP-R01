package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HomelyDatabase
import com.example.data.local.ServerConfigStore
import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.OrderStockDeltaHelper
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCatalogHelper
import com.example.data.model.ProductVariant
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.SnackCatalog
import com.example.data.model.StockItem
import com.example.data.model.Supplier
import com.example.data.model.SupplierBill
import com.example.data.remote.LaptopServerClient
import com.example.data.repository.HomelyDataSource
import com.example.data.repository.LocalTestRepository
import com.example.data.repository.RemoteCloudAdapter
import com.example.data.repository.SyncCoordinatorRepository
import com.example.ui.screens.OrderScreen
import com.example.ui.theme.HomelyTheme
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OrderStockAndPersistenceUnitTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun orderStockDeltaHelper_parsesComputesDeltasAndMapsTargets() {
    val parsed = OrderStockDeltaHelper.parseQuantitiesByCode("BC100 x10, JB200 x5, BC100 × 2")
    assertEquals(12, parsed["BC100"])
    assertEquals(5, parsed["JB200"])

    val bc100 = SnackCatalog.products.first { it.code == "BC100" }
    val jb200 = SnackCatalog.products.first { it.code == "JB200" }
    val tp200 = SnackCatalog.products.first { it.code == "TP200" }

    val deltas = OrderStockDeltaHelper.computeDeltaBetweenOrders(
      oldItemsSummary = "BC100 x10, JB200 x5",
      newItems = listOf(bc100 to 12, jb200 to 2, tp200 to 4),
    )
    assertEquals(-2, deltas["BC100"])
    assertEquals(3, deltas["JB200"])
    assertEquals(-4, deltas["TP200"])

    val stockTargets = OrderStockDeltaHelper.computeStockItemTargets(
      stocks = listOf(
        StockItem(id = "stk_bc", productCode = "BC100", productName = "Banana Chips", quantity = 10),
        StockItem(id = "stk_jb", productCode = "JB200", productName = "Jaggery Banana", quantity = 1),
      ),
      qtyDeltaByCode = mapOf("BC100" to -15, "JB200" to 3),
    )
    assertEquals(0, stockTargets["stk_bc"])
    assertEquals(4, stockTargets["stk_jb"])

    val variantProduct = Product(
      id = "prd_1",
      productCode = "PRD-00101",
      sku = "BC-VAR",
      name = "Banana Chips",
      stockQuantity = 100,
      hasVariants = true,
      variantsJson = ProductCatalogHelper.encodeVariants(
        listOf(ProductVariant("BC100", "100g", 42.0, 50.0, true))
      ),
    )
    val prodTargets = OrderStockDeltaHelper.computeProductStockTargets(
      products = listOf(variantProduct),
      qtyDeltaByCode = mapOf("BC100" to -10),
    )
    assertEquals(90, prodTargets["prd_1"])
  }

  @Test
  fun placeEditCancelAndDeleteOrder_automaticallyAdjustStockWithoutDoubleRestore() = runBlocking {
    val context: Context = ApplicationProvider.getApplicationContext()
    val roomDb = Room.inMemoryDatabaseBuilder(context, HomelyDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val configStore = ServerConfigStore(context)
    configStore.updateServerConfig(
      globalSyncEnabled = false,
      laptopServerEnabled = false,
      laptopServerUrl = "",
      laptopApiToken = "",
      autoDeleteSyncedFromPhone = false,
    )
    val coordinator = SyncCoordinatorRepository(
      appContext = context,
      primary = LocalTestRepository(),
      dao = roomDb.homelyDao(),
      configStore = configStore,
      laptopClient = LaptopServerClient(),
      currentUserId = LocalTestRepository.LOCAL_UID,
      isLocalTestMode = true,
    )

    try {
      val initialStocks = coordinator.observeStocks(LocalTestRepository.LOCAL_UID).first()
      val initialBcStock = initialStocks.first { it.productCode == "BC100" }.quantity
      val initialJbStock = initialStocks.first { it.productCode == "JB200" }.quantity

      val initialProducts = coordinator.observeProducts(LocalTestRepository.LOCAL_UID).first()
      val bcProduct = initialProducts.first { it.productCode == "PRD-00101" }
      val initialBcProductQty = bcProduct.stockQuantity

      val shop = coordinator.observeShops(LocalTestRepository.LOCAL_UID).first().first()
      val bc100 = SnackCatalog.products.first { it.code == "BC100" }
      val jb200 = SnackCatalog.products.first { it.code == "JB200" }

      // 1. Place order -> deducts stock
      val orderId = coordinator.placeOrder(shop, listOf(bc100 to 5, jb200 to 3), "Fresh lot").getOrThrow()
      val afterPlaceStocks = coordinator.observeStocks(LocalTestRepository.LOCAL_UID).first()
      assertEquals(initialBcStock - 5, afterPlaceStocks.first { it.productCode == "BC100" }.quantity)
      assertEquals(initialJbStock - 3, afterPlaceStocks.first { it.productCode == "JB200" }.quantity)
      val afterPlaceProducts = coordinator.observeProducts(LocalTestRepository.LOCAL_UID).first()
      assertEquals(initialBcProductQty - 5, afterPlaceProducts.first { it.id == bcProduct.id }.stockQuantity)

      // 2. Edit order (BC100: 5 -> 8, JB200: 3 -> 1) -> net -3 BC100, +2 JB200
      coordinator.updateOrderDetails(orderId, listOf(bc100 to 8, jb200 to 1), "Edited qty").getOrThrow()
      val afterEditStocks = coordinator.observeStocks(LocalTestRepository.LOCAL_UID).first()
      assertEquals(initialBcStock - 8, afterEditStocks.first { it.productCode == "BC100" }.quantity)
      assertEquals(initialJbStock - 1, afterEditStocks.first { it.productCode == "JB200" }.quantity)
      val afterEditProducts = coordinator.observeProducts(LocalTestRepository.LOCAL_UID).first()
      assertEquals(initialBcProductQty - 8, afterEditProducts.first { it.id == bcProduct.id }.stockQuantity)

      // 3. Cancel order (REJECTED) -> restores stock back to starter counts
      coordinator.updateOrderStatus(orderId, OrderStatus.REJECTED, "Customer cancelled").getOrThrow()
      val afterCancelStocks = coordinator.observeStocks(LocalTestRepository.LOCAL_UID).first()
      assertEquals(initialBcStock, afterCancelStocks.first { it.productCode == "BC100" }.quantity)
      assertEquals(initialJbStock, afterCancelStocks.first { it.productCode == "JB200" }.quantity)
      val afterCancelProducts = coordinator.observeProducts(LocalTestRepository.LOCAL_UID).first()
      assertEquals(initialBcProductQty, afterCancelProducts.first { it.id == bcProduct.id }.stockQuantity)

      // 4. Repeated cancel or deleting already-cancelled order must NOT double-restore
      coordinator.updateOrderStatus(orderId, OrderStatus.REJECTED, "Cancelled again").getOrThrow()
      coordinator.deleteOrder(orderId).getOrThrow()
      val afterDeleteCancelledStocks = coordinator.observeStocks(LocalTestRepository.LOCAL_UID).first()
      assertEquals(initialBcStock, afterDeleteCancelledStocks.first { it.productCode == "BC100" }.quantity)
      assertEquals(initialJbStock, afterDeleteCancelledStocks.first { it.productCode == "JB200" }.quantity)

      // 5. Place another active order then delete directly -> deducts 4 then restores 4
      val activeOrderId = coordinator.placeOrder(shop, listOf(bc100 to 4), "Direct delete test").getOrThrow()
      assertEquals(
        initialBcStock - 4,
        coordinator.observeStocks(LocalTestRepository.LOCAL_UID).first().first { it.productCode == "BC100" }.quantity,
      )
      coordinator.deleteOrder(activeOrderId).getOrThrow()
      assertEquals(
        initialBcStock,
        coordinator.observeStocks(LocalTestRepository.LOCAL_UID).first().first { it.productCode == "BC100" }.quantity,
      )
    } finally {
      roomDb.close()
    }
  }

  @Test
  fun routeAndShopUpdates_persistAcrossRestartInTestModeAndSyncToCloud() = runBlocking {
    val context: Context = ApplicationProvider.getApplicationContext()
    val roomDb = Room.inMemoryDatabaseBuilder(context, HomelyDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val configStore = ServerConfigStore(context)
    configStore.updateServerConfig(
      globalSyncEnabled = false,
      laptopServerEnabled = false,
      laptopServerUrl = "",
      laptopApiToken = "",
      autoDeleteSyncedFromPhone = false,
    )

    try {
      // Session 1 (Local Test Mode)
      val session1 = SyncCoordinatorRepository(
        appContext = context,
        primary = LocalTestRepository(),
        dao = roomDb.homelyDao(),
        configStore = configStore,
        laptopClient = LaptopServerClient(),
        currentUserId = LocalTestRepository.LOCAL_UID,
        isLocalTestMode = true,
      )
      session1.seedRoutesIfNeeded()

      val customRoute = DeliveryRoute(
        id = "R04-KANNUR-THALASSERY",
        name = "Kannur -> Thalassery Express",
        vehicle = "KL-13-Z-9900",
        maxCrates = 48,
        driverName = "Arjun Rao",
        driverPhone = "9847099887",
        dispatchWindow = "07:00 AM",
        stagingBay = "Bay 04",
        colorIndex = 3,
      )
      session1.saveRoute(customRoute).getOrThrow()

      val newShopId = session1.saveShop(
        Shop(
          shopCode = "SHOP-000777",
          name = "Fort Bakery Persistent",
          ownerName = "Nikhil",
          phone = "9847007777",
          address = "Thalassery Market",
          pinCode = "670101",
          routeId = customRoute.id,
        )
      ).getOrThrow()

      // Reassign starter shop seed_shop_1 to the custom route as well
      session1.updateShopRoute("seed_shop_1", customRoute.id).getOrThrow()

      // Session 2 (Simulate reopening the app with a fresh LocalTestRepository on the same Room DB)
      val session2 = SyncCoordinatorRepository(
        appContext = context,
        primary = LocalTestRepository(),
        dao = roomDb.homelyDao(),
        configStore = configStore,
        laptopClient = LaptopServerClient(),
        currentUserId = LocalTestRepository.LOCAL_UID,
        isLocalTestMode = true,
      )
      val reopenedRoutes = session2.observeRoutes().first()
      val restoredCustomRoute = reopenedRoutes.firstOrNull { it.id == customRoute.id }
      assertEquals("Kannur -> Thalassery Express", restoredCustomRoute?.name)
      assertEquals("Arjun Rao", restoredCustomRoute?.driverName)

      val reopenedShops = session2.observeShops(LocalTestRepository.LOCAL_UID).first()
      assertEquals(customRoute.id, reopenedShops.firstOrNull { it.id == newShopId }?.routeId)
      assertEquals("Fort Bakery Persistent", reopenedShops.firstOrNull { it.id == newShopId }?.name)
      assertEquals(customRoute.id, reopenedShops.firstOrNull { it.id == "seed_shop_1" }?.routeId)

      // Verify Cloud sync queue for routes (offline -> online)
      var isOnline = false
      val cloudRoutes = ConcurrentHashMap<String, DeliveryRoute>()
      val cloudRoutesFlow = MutableStateFlow<List<DeliveryRoute>>(emptyList())
      val fakeCloud = object : HomelyDataSource, RemoteCloudAdapter {
        override fun observeShops(userId: String): Flow<List<Shop>> = MutableStateFlow(emptyList())
        override fun observeOrders(userId: String): Flow<List<OrderRecord>> = MutableStateFlow(emptyList())
        override fun observeStocks(userId: String): Flow<List<StockItem>> = MutableStateFlow(emptyList())
        override fun observeProducts(userId: String): Flow<List<Product>> = MutableStateFlow(emptyList())
        override fun observePayments(userId: String): Flow<List<PaymentTransaction>> = MutableStateFlow(emptyList())
        override fun observeSupplierBills(userId: String): Flow<List<SupplierBill>> = MutableStateFlow(emptyList())
        override fun observeSuppliers(userId: String): Flow<List<Supplier>> = MutableStateFlow(emptyList())
        override fun observeMeta(userId: String): Flow<OperationsMeta?> = MutableStateFlow(null)
        override fun observeRoutes(): Flow<List<DeliveryRoute>> = cloudRoutesFlow
        override suspend fun seedStarterDataIfNeeded() = Result.success(Unit)
        override suspend fun seedRoutesIfNeeded() = Result.success(Unit)
        override suspend fun saveRoute(route: DeliveryRoute) = upsertRoute(route, false).map { Unit }
        override suspend fun saveShop(shop: Shop) = Result.success(shop.id)
        override suspend fun upsertShop(shop: Shop, isUpdate: Boolean) = Result.success(shop.id)
        override suspend fun updateShopRoute(shopId: String, newRouteId: String) = Result.success(Unit)
        override suspend fun updateShopStatus(shopId: String, newStatus: String) = Result.success(Unit)
        override suspend fun updateShopInquiryStatus(shopId: String, inquiryStatus: String, markCalledToday: Boolean) = Result.success(Unit)
        override suspend fun placeOrder(shop: Shop, items: List<Pair<CatalogProduct, Int>>, note: String) = Result.success("")
        override suspend fun placeRawOrder(orderDocId: String, orderCode: String, shopId: String, shopCode: String, shopName: String, routeId: String, status: String, totalAmount: Double, itemCount: Int, itemsSummary: String, orderDate: String, note: String) = Result.success(orderDocId)
        override suspend fun updateOrderStatus(orderId: String, newStatus: String, note: String?) = Result.success(Unit)
        override suspend fun updateOrderDetails(orderId: String, items: List<Pair<CatalogProduct, Int>>, note: String) = Result.success(Unit)
        override suspend fun updateOrderDetails(orderId: String, totalAmount: Double, itemCount: Int, itemsSummary: String, note: String) = Result.success(Unit)
        override suspend fun deleteOrder(orderId: String) = Result.success(Unit)
        override suspend fun updateStockQuantity(stockId: String, newQuantity: Int) = Result.success(Unit)
        override suspend fun saveProduct(product: Product) = Result.success("")
        override suspend fun upsertProduct(product: Product, isUpdate: Boolean) = Result.success("")
        override suspend fun updateProductStatus(productId: String, newStatus: String) = Result.success(Unit)
        override suspend fun updateProductStock(productId: String, newQuantity: Int) = Result.success(Unit)
        override suspend fun fetchShopsOnce(userId: String) = Result.success(emptyList<Shop>())
        override suspend fun fetchOrdersOnce(userId: String) = Result.success(emptyList<OrderRecord>())
        override suspend fun fetchProductsOnce(userId: String) = Result.success(emptyList<Product>())
        override suspend fun recordPayment(payment: PaymentTransaction) = Result.success("")
        override suspend fun saveSupplierBill(bill: SupplierBill) = Result.success("")
        override suspend fun saveSupplier(supplier: Supplier) = Result.success("")
        override suspend fun upsertPayment(payment: PaymentTransaction, isUpdate: Boolean) = Result.success("")
        override suspend fun upsertSupplierBill(bill: SupplierBill, isUpdate: Boolean) = Result.success("")
        override suspend fun upsertSupplier(supplier: Supplier, isUpdate: Boolean) = Result.success("")
        override suspend fun updateSupplierStatus(supplierId: String, newStatus: String) = Result.success(Unit)
        override suspend fun fetchPaymentsOnce(userId: String) = Result.success(emptyList<PaymentTransaction>())
        override suspend fun fetchSupplierBillsOnce(userId: String) = Result.success(emptyList<SupplierBill>())
        override suspend fun fetchSuppliersOnce(userId: String) = Result.success(emptyList<Supplier>())
        override suspend fun upsertRoute(route: DeliveryRoute, isUpdate: Boolean): Result<String> {
          cloudRoutes[route.id] = route
          cloudRoutesFlow.value = cloudRoutes.values.toList()
          return Result.success(route.id)
        }
        override suspend fun fetchRoutesOnce(userId: String) = Result.success(cloudRoutes.values.toList())
      }

      configStore.updateServerConfig(
        globalSyncEnabled = true,
        laptopServerEnabled = false,
        laptopServerUrl = "",
        laptopApiToken = "",
        autoDeleteSyncedFromPhone = false,
      )
      val cloudDb = Room.inMemoryDatabaseBuilder(context, HomelyDatabase::class.java)
        .allowMainThreadQueries()
        .build()
      try {
        val cloudCoordinator = SyncCoordinatorRepository(
          appContext = context,
          primary = fakeCloud,
          dao = cloudDb.homelyDao(),
          configStore = configStore,
          laptopClient = LaptopServerClient(),
          currentUserId = "uid_cloud_route",
          isLocalTestMode = false,
          cloudAdapter = fakeCloud,
          isOnlineProvider = { isOnline },
        )
        cloudCoordinator.saveRoute(customRoute).getOrThrow()
        assertTrue(cloudCoordinator.observePendingRoutes().first().any { it.routeId == customRoute.id && !it.globalSynced })
        assertTrue(cloudRoutes[customRoute.id] == null)

        isOnline = true
        cloudCoordinator.syncAllPending()
        assertEquals("Kannur -> Thalassery Express", cloudRoutes[customRoute.id]?.name)
        assertTrue(cloudCoordinator.observePendingRoutes().first().first { it.routeId == customRoute.id }.globalSynced)
      } finally {
        cloudDb.close()
      }
    } finally {
      roomDb.close()
    }
  }

  @Test
  fun orderScreen_cancelOrderButtonShowsConfirmationAndCancelsActiveOrderOnly() {
    val shop = Shop(
      id = "shop_1",
      shopCode = "SHP-101",
      name = "Malabar Heritage Bakery",
      routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
    )
    val confirmedOrder = OrderRecord(
      id = "ord_conf_1",
      orderCode = "ORD-7701",
      shopId = shop.id,
      shopCode = shop.shopCode,
      shopName = shop.name,
      routeId = shop.routeId,
      status = OrderStatus.CONFIRMED,
      totalAmount = 450.0,
      itemCount = 10,
      itemsSummary = "BC100 x10",
      orderDate = "2026-11-19",
    )
    val cancelledOrder = confirmedOrder.copy(
      id = "ord_rej_2",
      orderCode = "ORD-7702",
      status = OrderStatus.REJECTED,
    )

    var cancelledTarget: OrderRecord? = null
    composeTestRule.setContent {
      HomelyTheme(darkTheme = false) {
        OrderScreen(
          shops = listOf(shop),
          orders = listOf(confirmedOrder, cancelledOrder),
          onPlaceOrderClick = {},
          onMarkInquiryStatus = { _, _, _ -> },
          onCancelOrderClick = { cancelledTarget = it },
        )
      }
    }

    composeTestRule.onNodeWithTag("order_tab_confirmed_orders").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Cancelled").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag("cancel_order_ORD-7702")).fetchSemanticsNodes().size)

    composeTestRule.onNodeWithTag("cancel_order_ORD-7701").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Cancel Order?").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_cancel_order_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals("ord_conf_1", cancelledTarget?.id)
  }
}
