package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ServerConfigStore
import com.example.data.local.HomelyDatabase
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.Supplier
import com.example.data.model.SupplierBill
import com.example.data.remote.LaptopServerClient
import com.example.data.repository.LocalTestRepository
import com.example.data.repository.SyncCoordinatorRepository
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineQueueAndLaptopSyncTest {

  private lateinit var context: Context
  private lateinit var roomDb: HomelyDatabase
  private lateinit var configStore: ServerConfigStore
  private lateinit var coordinator: SyncCoordinatorRepository
  private lateinit var mockLaptopServer: HttpServer
  private val laptopShopsStore = ConcurrentHashMap<String, JSONObject>()
  private val laptopProductsStore = ConcurrentHashMap<String, JSONObject>()
  private val laptopPaymentsStore = ConcurrentHashMap<String, JSONObject>()
  private val laptopBillsStore = ConcurrentHashMap<String, JSONObject>()
  private val laptopSuppliersStore = ConcurrentHashMap<String, JSONObject>()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    roomDb = Room.inMemoryDatabaseBuilder(context, HomelyDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    configStore = ServerConfigStore(context)

    mockLaptopServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    mockLaptopServer.createContext("/api/health") { exchange ->
      val body = JSONObject(mapOf("ok" to true, "shopCount" to laptopShopsStore.size, "orderCount" to 0)).toString()
      val bytes = body.toByteArray()
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    mockLaptopServer.createContext("/api/shops") { exchange ->
      when (exchange.requestMethod) {
        "GET" -> {
          val arr = JSONArray(laptopShopsStore.values.toList())
          val bytes = arr.toString().toByteArray()
          exchange.sendResponseHeaders(200, bytes.size.toLong())
          exchange.responseBody.use { it.write(bytes) }
        }
        "POST" -> {
          val raw = exchange.requestBody.bufferedReader().readText()
          val obj = JSONObject(raw)
          laptopShopsStore[obj.getString("id")] = obj
          val bytes = """{"ok":true}""".toByteArray()
          exchange.sendResponseHeaders(200, bytes.size.toLong())
          exchange.responseBody.use { it.write(bytes) }
        }
        else -> exchange.sendResponseHeaders(405, -1)
      }
    }
    mockLaptopServer.createContext("/api/orders") { exchange ->
      val bytes = "[]".toByteArray()
      exchange.sendResponseHeaders(200, bytes.size.toLong())
      exchange.responseBody.use { it.write(bytes) }
    }
    mockLaptopServer.createContext("/api/routes") { exchange ->
      when (exchange.requestMethod) {
        "GET" -> {
          val bytes = "[]".toByteArray()
          exchange.sendResponseHeaders(200, bytes.size.toLong())
          exchange.responseBody.use { it.write(bytes) }
        }
        "POST" -> {
          val bytes = """{"ok":true}""".toByteArray()
          exchange.sendResponseHeaders(200, bytes.size.toLong())
          exchange.responseBody.use { it.write(bytes) }
        }
        else -> exchange.sendResponseHeaders(405, -1)
      }
    }
    fun registerEntityEndpoint(path: String, store: ConcurrentHashMap<String, JSONObject>) {
      mockLaptopServer.createContext(path) { exchange ->
        when (exchange.requestMethod) {
          "GET" -> {
            val bytes = JSONArray(store.values.toList()).toString().toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
          }
          "POST" -> {
            val raw = exchange.requestBody.bufferedReader().readText()
            val obj = JSONObject(raw)
            store[obj.getString("id")] = obj
            val bytes = """{"ok":true}""".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
          }
          else -> exchange.sendResponseHeaders(405, -1)
        }
      }
    }
    registerEntityEndpoint("/api/products", laptopProductsStore)
    registerEntityEndpoint("/api/payments", laptopPaymentsStore)
    registerEntityEndpoint("/api/supplier_bills", laptopBillsStore)
    registerEntityEndpoint("/api/suppliers", laptopSuppliersStore)
    mockLaptopServer.start()

    coordinator = SyncCoordinatorRepository(
      appContext = context,
      primary = LocalTestRepository(),
      dao = roomDb.homelyDao(),
      configStore = configStore,
      laptopClient = LaptopServerClient(),
      currentUserId = LocalTestRepository.LOCAL_UID,
      isLocalTestMode = true,
    )
  }

  @After
  fun tearDown() {
    mockLaptopServer.stop(0)
    roomDb.close()
  }

  @Test
  fun saveManyShopsOffline_thenSyncToLaptop_andDeleteSyncedFromPhone() = runBlocking {
    // 1. Offline / Laptop disabled: save 3 shops into durable phone Room storage
    coordinator.updateServerSettings(
      globalSyncEnabled = false,
      laptopServerEnabled = false,
      laptopServerUrl = "",
      laptopApiToken = "",
      autoDeleteSyncedFromPhone = false,
    )

    repeat(3) { idx ->
      coordinator.saveShop(
        Shop(
          shopCode = "SHOP-OFF-${idx + 1}",
          name = "Offline Shop ${idx + 1}",
          ownerName = "Owner ${idx + 1}",
          phone = "984700000${idx + 1}",
          address = "Road ${idx + 1}, Kannur",
          pinCode = "670001",
          routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        )
      )
    }

    val queuedOffline = coordinator.observePendingShops().first()
    assertEquals(3, queuedOffline.size)

    // 2. Enable Laptop Local Server & run Sync Now
    val laptopUrl = "http://127.0.0.1:${mockLaptopServer.address.port}"
    coordinator.updateServerSettings(
      globalSyncEnabled = false,
      laptopServerEnabled = true,
      laptopServerUrl = laptopUrl,
      laptopApiToken = "",
      autoDeleteSyncedFromPhone = false,
    )

    val syncResult = coordinator.syncAllPending()
    assertEquals(3, syncResult.syncedCount)
    assertEquals(0, syncResult.remainingPendingCount)
    assertTrue(laptopShopsStore.size >= 3)

    // 3. Delete synced records from phone temporary queue
    val deleted = coordinator.deleteSyncedFromPhone()
    assertEquals(3, deleted)
    assertEquals(0, coordinator.observePendingShops().first().size)

    // 4. Add a shop directly on laptop server and pull it onto phone
    laptopShopsStore["laptop_created_1"] = JSONObject(
      mapOf(
        "id" to "laptop_created_1",
        "shopCode" to "SHOP-LAP-99",
        "name" to "Laptop Added Mart",
        "ownerName" to "Vikram",
        "phone" to "9895123456",
        "address" to "MG Road",
        "pinCode" to "670002",
        "routeId" to RoutesCatalog.R02_KANNUR_KOZHIKODE,
        "status" to "active",
        "inquiryStatus" to "pending_inquiry",
        "updatedAtMillis" to System.currentTimeMillis(),
      )
    )

    val pullResult = coordinator.pullFromLaptopServer()
    assertTrue(pullResult.shopsPulled >= 4)

    val allVisibleShops = coordinator.observeShops(LocalTestRepository.LOCAL_UID).first()
    assertTrue(allVisibleShops.any { it.shopCode == "SHOP-LAP-99" && it.name == "Laptop Added Mart" })
  }

  @Test
  fun updateExistingShopStatusAndRouteOffline_queuesAndSyncsToLaptop() = runBlocking {
    val laptopUrl = "http://127.0.0.1:${mockLaptopServer.address.port}"
    // Enable laptop server pointing to an unreachable port initially to simulate offline laptop edit
    coordinator.updateServerSettings(
      globalSyncEnabled = false,
      laptopServerEnabled = true,
      laptopServerUrl = "http://127.0.0.1:1",
      laptopApiToken = "",
      autoDeleteSyncedFromPhone = false,
    )

    // Edit an existing starter shop's route & inquiry status while laptop is unreachable
    coordinator.updateShopRoute("local_shop_1", RoutesCatalog.R03_BANGALORE_PUNE)
    coordinator.updateShopInquiryStatus("local_shop_1", "unattended", markCalledToday = true)

    val queuedAfterEdit = coordinator.observePendingShops().first()
    assertEquals(1, queuedAfterEdit.size)
    assertEquals(RoutesCatalog.R03_BANGALORE_PUNE, queuedAfterEdit.first().routeId)
    assertEquals("unattended", queuedAfterEdit.first().inquiryStatus)

    // Now point to live mock laptop server & sync
    coordinator.updateServerSettings(
      globalSyncEnabled = false,
      laptopServerEnabled = true,
      laptopServerUrl = laptopUrl,
      laptopApiToken = "",
      autoDeleteSyncedFromPhone = false,
    )

    val syncResult = coordinator.syncAllPending()
    assertEquals(1, syncResult.syncedCount)
    assertEquals(0, syncResult.remainingPendingCount)
    val syncedOnLaptop = laptopShopsStore["local_shop_1"]!!
    assertEquals(RoutesCatalog.R03_BANGALORE_PUNE, syncedOnLaptop.getString("routeId"))
    assertEquals("unattended", syncedOnLaptop.getString("inquiryStatus"))
  }

  @Test
  fun saveExtendedEntitiesOffline_andSignedInUidMigration_syncsAllToLaptop() = runBlocking {
    // 1. Offline authoring under LOCAL_UID before user signs in
    coordinator.updateServerSettings(
      globalSyncEnabled = false,
      laptopServerEnabled = false,
      laptopServerUrl = "",
      laptopApiToken = "",
      autoDeleteSyncedFromPhone = false,
    )

    coordinator.saveShop(
      Shop(
        id = "offline_guest_shop_1",
        shopCode = "SHOP-GUEST-1",
        name = "Guest Created Shop",
        ownerName = "Meera",
        phone = "9847123456",
        address = "Marine Drive, Kochi",
        pinCode = "682031",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
      )
    )
    coordinator.saveProduct(
      Product(
        id = "offline_prod_1",
        sku = "SKU-TST-1",
        name = "Homely Special Halwa",
        category = "Sweets",
        brand = "Homely Gold",
        wholesalePrice = 140.0,
        mrp = 175.0,
        stockQuantity = 45,
      )
    )
    coordinator.recordPayment(
      PaymentTransaction(
        id = "offline_pay_1",
        receiptCode = "RCP-001",
        partyId = "offline_guest_shop_1",
        partyName = "Guest Created Shop",
        amount = 1400.0,
        paymentMethod = "UPI",
      )
    )
    coordinator.saveSupplier(
      Supplier(
        id = "offline_sup_1",
        supplierCode = "SUP-01",
        name = "Kera Agro Mills",
        phone = "9845009988",
        category = "Raw Materials",
      )
    )
    coordinator.saveSupplierBill(
      SupplierBill(
        id = "offline_bill_1",
        supplierCode = "SUP-01",
        supplierName = "Kera Agro Mills",
        billNumber = "INV-778",
        totalBilled = 5000.0,
        paidAmount = 2000.0,
      )
    )

    assertEquals(1, coordinator.observePendingProducts().first().size)
    assertEquals(1, coordinator.observePendingPayments().first().size)
    assertEquals(1, coordinator.observePendingSuppliers().first().size)
    assertEquals(1, coordinator.observePendingSupplierBills().first().size)

    // 2. Simulate signing into Google account (different UID) with live laptop server
    val signedInCoordinator = SyncCoordinatorRepository(
      appContext = context,
      primary = LocalTestRepository(),
      dao = roomDb.homelyDao(),
      configStore = configStore,
      laptopClient = LaptopServerClient(),
      currentUserId = "google_uid_42",
      isLocalTestMode = true,
    )
    val laptopUrl = "http://127.0.0.1:${mockLaptopServer.address.port}"
    signedInCoordinator.updateServerSettings(
      globalSyncEnabled = false,
      laptopServerEnabled = true,
      laptopServerUrl = laptopUrl,
      laptopApiToken = "",
      autoDeleteSyncedFromPhone = false,
    )

    val syncResult = signedInCoordinator.syncAllPending()
    assertTrue(syncResult.syncedCount >= 5)
    assertEquals(0, syncResult.remainingPendingCount)
    assertTrue(laptopShopsStore.containsKey("offline_guest_shop_1"))
    assertTrue(laptopProductsStore.containsKey("offline_prod_1"))
    assertTrue(laptopPaymentsStore.containsKey("offline_pay_1"))
    assertTrue(laptopSuppliersStore.containsKey("offline_sup_1"))
    assertTrue(laptopBillsStore.containsKey("offline_bill_1"))
  }
}
