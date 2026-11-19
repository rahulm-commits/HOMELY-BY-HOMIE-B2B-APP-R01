package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.room.Room
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
import com.example.data.model.ShopStatus
import com.example.data.model.StockItem
import com.example.data.model.Supplier
import com.example.data.model.SupplierBill
import com.example.data.remote.LaptopServerClient
import com.example.data.repository.LocalTestRepository
import com.example.data.repository.RemoteCloudAdapter
import com.example.data.repository.SnackRouteDataSource
import com.example.data.repository.SyncCoordinatorRepository
import com.example.ui.screens.ShopListScreen
import com.example.ui.theme.SnackRouteTheme
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w360dp-h780dp")
class ShopListScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun shopListHeader_withPendingBadge_rendersHorizontallyWithoutVerticalWrapping() {
    val shops = listOf(
      Shop(
        id = "shop_1",
        shopCode = "SHOP-000107",
        name = "Malabar Bakery & Mart",
        ownerName = "Abdul",
        phone = "9847011223",
        address = "Station Road, Kannur",
        pinCode = "670001",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        status = ShopStatus.ACTIVE,
        isPendingSync = true,
      ),
      Shop(
        id = "shop_2",
        shopCode = "SHOP-000108",
        name = "Green Valley Grocers",
        ownerName = "Ramesh",
        phone = "9847044556",
        address = "NH Bypass, Kochi",
        pinCode = "682024",
        routeId = RoutesCatalog.R02_KANNUR_KOZHIKODE,
        status = ShopStatus.ACTIVE,
        isPendingSync = false,
      ),
    )

    composeTestRule.setContent {
      SnackRouteTheme(darkTheme = false) {
        ShopListScreen(
          shops = shops,
          pendingQueueCount = 1,
          laptopServerEnabled = false,
          onOpenSyncServers = {},
          onAddShopClick = {},
          onEditShopClick = {},
          onChangeShopStatus = { _, _, _ -> },
        )
      }
    }
    composeTestRule.waitForIdle()

    val countBadge = composeTestRule.onNodeWithTag("shop_count_badge").assertIsDisplayed()
    val pendingBadge = composeTestRule.onNodeWithTag("shop_pending_badge").assertIsDisplayed()
    val syncButton = composeTestRule.onNodeWithTag("shop_sync_servers_button").assertIsDisplayed()

    composeTestRule.onNodeWithText("2 shops").assertIsDisplayed()
    composeTestRule.onNodeWithText("1 pending").assertIsDisplayed()
    composeTestRule.onNodeWithText("Queue (1)").assertIsDisplayed()
    composeTestRule.onNodeWithText("Offline Pending").assertIsDisplayed()

    // Ensure the "1 pending" pill is horizontal (wider than tall and compact single-line height)
    val pendingBounds = pendingBadge.getUnclippedBoundsInRoot()
    val pendingWidth = pendingBounds.right - pendingBounds.left
    val pendingHeight = pendingBounds.bottom - pendingBounds.top
    assertTrue("Expected compact single-line height <= 32dp, got $pendingHeight", pendingHeight <= 32.dp)
    assertTrue("Expected horizontal '1 pending' pill, width=$pendingWidth height=$pendingHeight", pendingWidth > pendingHeight)

    // Ensure countBadge and pendingBadge sit on the same horizontal row below the title
    val countBounds = countBadge.getUnclippedBoundsInRoot()
    assertTrue("Count and pending badges should align on same horizontal row", kotlin.math.abs((countBounds.top - pendingBounds.top).value) < 6f)

    // Sync button must satisfy 48dp accessibility touch target
    val syncBounds = syncButton.getUnclippedBoundsInRoot()
    assertTrue("Sync button touch target height >= 48dp", (syncBounds.bottom - syncBounds.top) >= 47.dp)
  }

  @Test
  fun saveShop_normalizesFieldsAndReconcilesConfirmedCloudStream() = runBlocking {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db = Room.inMemoryDatabaseBuilder(context, SnackRouteDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val configStore = ServerConfigStore(context)
    configStore.updateServerConfig(
      globalSyncEnabled = true,
      laptopServerEnabled = false,
      laptopServerUrl = "",
      laptopApiToken = "",
      autoDeleteSyncedFromPhone = false,
    )

    val shopsStore = ConcurrentHashMap<String, Shop>()
    val shopsFlow = MutableStateFlow<List<Shop>>(emptyList())
    var failInitialUpload = true

    val fakeCloud = object : SnackRouteDataSource, RemoteCloudAdapter {
      override fun observeShops(userId: String): Flow<List<Shop>> = shopsFlow
      override fun observeOrders(userId: String): Flow<List<OrderRecord>> = MutableStateFlow(emptyList())
      override fun observeStocks(userId: String): Flow<List<StockItem>> = MutableStateFlow(emptyList())
      override fun observeProducts(userId: String): Flow<List<Product>> = MutableStateFlow(emptyList())
      override fun observePayments(userId: String): Flow<List<PaymentTransaction>> = MutableStateFlow(emptyList())
      override fun observeSupplierBills(userId: String): Flow<List<SupplierBill>> = MutableStateFlow(emptyList())
      override fun observeSuppliers(userId: String): Flow<List<Supplier>> = MutableStateFlow(emptyList())
      override fun observeMeta(userId: String): Flow<OperationsMeta?> = MutableStateFlow(null)
      override fun observeRoutes(): Flow<List<DeliveryRoute>> = MutableStateFlow(emptyList())
      override suspend fun seedStarterDataIfNeeded() = Result.success(Unit)
      override suspend fun seedRoutesIfNeeded() = Result.success(Unit)
      override suspend fun saveRoute(route: DeliveryRoute) = Result.success(Unit)
      override suspend fun saveShop(shop: Shop) = upsertShop(shop, shop.id.isNotBlank())
      override suspend fun upsertShop(shop: Shop, isUpdate: Boolean): Result<String> {
        // Verify Firestore rules length bounds are always satisfied before hitting Firestore
        assertTrue(shop.name.length <= 120)
        assertTrue(shop.mapsLink.length <= 600)
        assertTrue(shop.latitude.isFinite())
        assertTrue(shop.longitude.isFinite())
        if (failInitialUpload) {
          return Result.failure(IllegalStateException("Temporary server timeout"))
        }
        shopsStore[shop.id] = shop.copy(isPendingSync = false)
        shopsFlow.value = shopsStore.values.toList()
        return Result.success(shop.id)
      }
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
      override suspend fun fetchShopsOnce(userId: String) = Result.success(shopsStore.values.toList())
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
    }

    val coordinator = SyncCoordinatorRepository(
      appContext = context,
      primary = fakeCloud,
      dao = db.snackRouteDao(),
      configStore = configStore,
      laptopClient = LaptopServerClient(),
      currentUserId = "uid_test",
      isLocalTestMode = false,
      cloudAdapter = fakeCloud,
      isOnlineProvider = { true },
    )

    try {
      // 1. Initial upload fails -> shop stays pending with diagnostic error
      val id = coordinator.saveShop(
        Shop(
          shopCode = "SHOP-000999",
          name = "A".repeat(150),
          ownerName = "Tester",
          phone = "9847001122",
          address = "Kalpetta",
          pinCode = "673121",
          routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
          latitude = Double.NaN,
          longitude = Double.POSITIVE_INFINITY,
          mapsLink = "https://maps.google.com/?q=" + "x".repeat(700),
        )
      ).getOrThrow()

      assertTrue(coordinator.isShopPending(id))
      assertEquals("Cloud: Temporary server timeout", coordinator.getPendingShopError(id))

      // 2. When Firestore later confirms the created document on the snapshot stream, pending flag reconciles automatically
      failInitialUpload = false
      val savedShop = coordinator.observeShops("uid_test").first { list -> list.any { it.id == id } }.first { it.id == id }
      shopsStore[id] = savedShop.copy(isPendingSync = false)
      shopsFlow.value = shopsStore.values.toList()

      val reconciledShops = coordinator.observeShops("uid_test").first { list ->
        list.any { it.id == id && !it.isPendingSync }
      }
      assertFalse(reconciledShops.first { it.id == id }.isPendingSync)
      assertFalse(coordinator.isShopPending(id))
    } finally {
      db.close()
    }
  }

  @Test
  fun localTestRepository_saveShopWithPreAssignedId_insertsNewShop() = runBlocking {
    val repo = LocalTestRepository()
    repo.saveShop(
      Shop(
        id = "shop_preassigned_1",
        shopCode = "SHOP-000555",
        name = "Coastal Chips",
        ownerName = "Suresh",
        phone = "9847011111",
        address = "Kannur",
        pinCode = "670001",
      )
    )
    val list = repo.observeShops(LocalTestRepository.LOCAL_UID).first()
    assertTrue(list.any { it.id == "shop_preassigned_1" && it.name == "Coastal Chips" })
  }
}
