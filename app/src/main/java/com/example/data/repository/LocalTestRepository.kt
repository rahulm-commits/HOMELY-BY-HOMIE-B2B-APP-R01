package com.example.data.repository

import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.InquiryStatus
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentLedgerHelper
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCatalogHelper
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.example.data.model.StockItem
import com.example.data.model.Supplier
import com.example.data.model.SupplierBill
import com.example.data.model.SupplierBillStatus
import com.example.data.model.SupplierCatalogHelper
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory local repository used ONLY when the user selects "Skip Login (Test Mode)".
 * Never touches Firebase Auth or Firestore so zero unauthenticated network calls occur.
 */
class LocalTestRepository : SnackRouteDataSource {

  private val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  private val yesterday = daysAgoString(1)
  private val threeDaysAgo = daysAgoString(3)
  private val sevenDaysAgo = daysAgoString(7)

  private val shopsFlow = MutableStateFlow(
    listOf(
      Shop(
        id = "local_shop_1",
        userId = LOCAL_UID,
        shopCode = "SHP-101",
        name = "Malabar Heritage Bakery",
        ownerName = "Suresh Nair",
        phone = "9847012345",
        secondaryPhone = "9847011223",
        address = "Fort Road, Near Town Hall, Kannur",
        pinCode = "670001",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        latitude = 11.8745,
        longitude = 75.3704,
        note = "Takes weekly Banana Chips & Mixture crates",
        status = ShopStatus.ACTIVE,
        inquiryStatus = InquiryStatus.PENDING_INQUIRY,
        enquiryDate = today,
        callDate = today,
        lastOrderDate = threeDaysAgo,
        lastCallDate = yesterday,
        confirmedPriceNotes = "BC100: ₹42, JB200: ₹92",
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
      Shop(
        id = "local_shop_2",
        userId = LOCAL_UID,
        shopCode = "SHP-102",
        name = "Kaveri Super Mart",
        ownerName = "Prakash Shetty",
        phone = "9448023456",
        address = "12th Cross, Malleshwaram, Bangalore",
        pinCode = "560003",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        latitude = 13.0031,
        longitude = 77.5643,
        note = "Call before 11 AM for weekend stock",
        status = ShopStatus.ACTIVE,
        inquiryStatus = InquiryStatus.PENDING_INQUIRY,
        enquiryDate = today,
        callDate = today,
        lastOrderDate = sevenDaysAgo,
        lastCallDate = threeDaysAgo,
        confirmedPriceNotes = "BC200: ₹85",
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
      Shop(
        id = "local_shop_3",
        userId = LOCAL_UID,
        shopCode = "SHP-103",
        name = "Calicut Spice & Bakes",
        ownerName = "Faisal Rahman",
        phone = "9895034567",
        secondaryPhone = "9895034500",
        address = "SM Street, Palayam, Kozhikode",
        pinCode = "673001",
        routeId = RoutesCatalog.R02_KANNUR_KOZHIKODE,
        latitude = 11.2588,
        longitude = 75.7804,
        note = "Confirmed morning route",
        status = ShopStatus.ACTIVE,
        inquiryStatus = InquiryStatus.CONFIRMED,
        enquiryDate = today,
        callDate = today,
        lastOrderDate = today,
        lastCallDate = today,
        confirmedPriceNotes = "JB200: ₹90, MX500: ₹162",
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
      Shop(
        id = "local_shop_4",
        userId = LOCAL_UID,
        shopCode = "SHP-104",
        name = "Deccan Fresh Corner",
        ownerName = "Anand Kulkarni",
        phone = "9822045678",
        address = "FC Road, Shivajinagar, Pune",
        pinCode = "411004",
        routeId = RoutesCatalog.R03_BANGALORE_PUNE,
        latitude = 18.5204,
        longitude = 73.8567,
        note = "Owner unavailable on Monday mornings",
        status = ShopStatus.ACTIVE,
        inquiryStatus = InquiryStatus.UNATTENDED,
        enquiryDate = yesterday,
        callDate = today,
        lastOrderDate = sevenDaysAgo,
        lastCallDate = yesterday,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
      Shop(
        id = "local_shop_5",
        userId = LOCAL_UID,
        shopCode = "SHP-105",
        name = "Thalassery Chips Centre",
        ownerName = "Meera Krishnan",
        phone = "9745056789",
        address = "Logans Road, Thalassery",
        pinCode = "670101",
        routeId = RoutesCatalog.R02_KANNUR_KOZHIKODE,
        latitude = 11.7491,
        longitude = 75.4890,
        note = "Stock sufficient until Friday",
        status = ShopStatus.ACTIVE,
        inquiryStatus = InquiryStatus.NO_ORDER,
        enquiryDate = yesterday,
        callDate = today,
        lastOrderDate = threeDaysAgo,
        lastCallDate = today,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
      Shop(
        id = "local_shop_6",
        userId = LOCAL_UID,
        shopCode = "SHP-106",
        name = "Green Valley Provision",
        ownerName = "Ramesh Gowda",
        phone = "9900167890",
        address = "Jayanagar 4th Block, Bangalore",
        pinCode = "560011",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        latitude = 12.9250,
        longitude = 77.5938,
        note = "Temporarily closed for renovation",
        status = ShopStatus.INACTIVE,
        inquiryStatus = InquiryStatus.NO_ORDER,
        enquiryDate = sevenDaysAgo,
        callDate = sevenDaysAgo,
        lastOrderDate = sevenDaysAgo,
        lastCallDate = sevenDaysAgo,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
    )
  )

  private val ordersFlow = MutableStateFlow(
    listOf(
      OrderRecord(
        id = "local_ord_1",
        userId = LOCAL_UID,
        orderCode = "ORD-2041",
        shopId = "local_shop_3",
        shopCode = "SHP-103",
        shopName = "Calicut Spice & Bakes",
        routeId = RoutesCatalog.R02_KANNUR_KOZHIKODE,
        status = OrderStatus.CONFIRMED,
        totalAmount = 6420.0,
        itemCount = 60,
        itemsSummary = "JB200 x40, MX500 x20",
        orderDate = today,
        note = "Morning van dispatch",
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
      OrderRecord(
        id = "local_ord_2",
        userId = LOCAL_UID,
        orderCode = "ORD-2040",
        shopId = "local_shop_1",
        shopCode = "SHP-101",
        shopName = "Malabar Heritage Bakery",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        status = OrderStatus.DELIVERED,
        totalAmount = 8950.0,
        itemCount = 95,
        itemsSummary = "BC100 x50, BC200 x30, JB200 x15",
        orderDate = today,
        note = "Delivered via Van #02",
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
      OrderRecord(
        id = "local_ord_3",
        userId = LOCAL_UID,
        orderCode = "ORD-2039",
        shopId = "local_shop_2",
        shopCode = "SHP-102",
        shopName = "Kaveri Super Mart",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        status = OrderStatus.PENDING,
        totalAmount = 4250.0,
        itemCount = 50,
        itemsSummary = "BC200 x50",
        orderDate = today,
        note = "Awaiting packing",
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
      OrderRecord(
        id = "local_ord_4",
        userId = LOCAL_UID,
        orderCode = "ORD-2035",
        shopId = "local_shop_4",
        shopCode = "SHP-104",
        shopName = "Deccan Fresh Corner",
        routeId = RoutesCatalog.R03_BANGALORE_PUNE,
        status = OrderStatus.DELIVERED,
        totalAmount = 12400.0,
        itemCount = 110,
        itemsSummary = "BC500 x30, JB200 x50, TP200 x30",
        orderDate = yesterday,
        note = "Yesterday route",
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
      OrderRecord(
        id = "local_ord_5",
        userId = LOCAL_UID,
        orderCode = "ORD-2032",
        shopId = "local_shop_5",
        shopCode = "SHP-105",
        shopName = "Thalassery Chips Centre",
        routeId = RoutesCatalog.R02_KANNUR_KOZHIKODE,
        status = OrderStatus.REJECTED,
        totalAmount = 1980.0,
        itemCount = 22,
        itemsSummary = "MR200 x22",
        orderDate = threeDaysAgo,
        note = "Shop closed during delivery window",
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      ),
    )
  )

  private val stocksFlow = MutableStateFlow(
    listOf(
      StockItem(
        id = "local_stk_1",
        userId = LOCAL_UID,
        productCode = "BC100",
        productName = "Banana Chips",
        packSize = "100g",
        category = "Banana Chips",
        quantity = 24,
        minThreshold = 50,
        unitPrice = 45.0,
      ),
      StockItem(
        id = "local_stk_2",
        userId = LOCAL_UID,
        productCode = "JB200",
        productName = "Jaggery Banana",
        packSize = "200g",
        category = "Jaggery & Sweet",
        quantity = 18,
        minThreshold = 40,
        unitPrice = 96.0,
      ),
      StockItem(
        id = "local_stk_3",
        userId = LOCAL_UID,
        productCode = "MX500",
        productName = "Kerala Spicy Mixture",
        packSize = "500g",
        category = "Mixtures & Savories",
        quantity = 14,
        minThreshold = 25,
        unitPrice = 170.0,
      ),
      StockItem(
        id = "local_stk_4",
        userId = LOCAL_UID,
        productCode = "TP200",
        productName = "Tapioca Chips",
        packSize = "200g",
        category = "Tapioca & Crackers",
        quantity = 85,
        minThreshold = 30,
        unitPrice = 65.0,
      ),
    )
  )

  private val metaFlow = MutableStateFlow<OperationsMeta?>(
    OperationsMeta(
      id = "summary",
      userId = LOCAL_UID,
      shopRegTarget = 10,
      vehiclesToDispatch = 3,
      vehiclesOnRoad = 2,
      returnsCollected = 4,
      toCollectAmount = 46800.0,
      toCollectShops = 11,
      toSuppliersAmount = 28500.0,
      toSuppliersCount = 4,
      seeded = true,
    )
  )

  private val routesFlow = MutableStateFlow(RoutesCatalog.starterRoutes)

  private val productsFlow = MutableStateFlow(
    ProductCatalogHelper.starterProducts(
      userId = LOCAL_UID,
      today = today,
      daysAgo = ::daysAgoString,
    ).map {
      it.copy(
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      )
    }
  )

  private val paymentsFlow = MutableStateFlow(
    PaymentLedgerHelper.starterPayments(
      userId = LOCAL_UID,
      today = today,
      yesterday = yesterday,
    ).map {
      it.copy(
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      )
    }
  )

  private val supplierBillsFlow = MutableStateFlow(
    PaymentLedgerHelper.starterSupplierBills(
      userId = LOCAL_UID,
      today = today,
      daysOffset = ::daysOffsetString,
    ).map {
      it.copy(
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
      )
    }
  )

  private val suppliersFlow = MutableStateFlow(
    SupplierCatalogHelper.starterSuppliers(
      userId = LOCAL_UID,
      today = today,
      daysAgo = ::daysAgoString,
    ).mapIndexed { index, sup ->
      val offsetMs = System.currentTimeMillis() - (index * 3L * 24L * 60L * 60L * 1000L)
      val ts = Timestamp(Date(offsetMs))
      sup.copy(createdAt = ts, updatedAt = ts)
    }
  )

  override fun observeShops(userId: String): Flow<List<Shop>> = shopsFlow.asStateFlow()

  override fun observeOrders(userId: String): Flow<List<OrderRecord>> = ordersFlow.asStateFlow()

  override fun observeStocks(userId: String): Flow<List<StockItem>> = stocksFlow.asStateFlow()

  override fun observeProducts(userId: String): Flow<List<Product>> = productsFlow.asStateFlow()

  override fun observePayments(userId: String): Flow<List<PaymentTransaction>> = paymentsFlow.asStateFlow()

  override fun observeSupplierBills(userId: String): Flow<List<SupplierBill>> = supplierBillsFlow.asStateFlow()

  override fun observeSuppliers(userId: String): Flow<List<Supplier>> = suppliersFlow.asStateFlow()

  override fun observeMeta(userId: String): Flow<OperationsMeta?> = metaFlow.asStateFlow()

  override fun observeRoutes(): Flow<List<DeliveryRoute>> = routesFlow.asStateFlow()

  override suspend fun seedStarterDataIfNeeded(): Result<Unit> = Result.success(Unit)

  override suspend fun seedRoutesIfNeeded(): Result<Unit> = Result.success(Unit)

  override suspend fun saveRoute(route: DeliveryRoute): Result<Unit> {
    routesFlow.update { current ->
      val idx = current.indexOfFirst { it.id == route.id }
      if (idx >= 0) {
        current.toMutableList().apply { set(idx, route) }
      } else {
        current + route
      }
    }
    return Result.success(Unit)
  }

  override suspend fun updateShopRoute(shopId: String, newRouteId: String): Result<Unit> {
    shopsFlow.update { current ->
      current.map {
        if (it.id == shopId) it.copy(routeId = newRouteId, updatedAt = Timestamp.now()) else it
      }
    }
    return Result.success(Unit)
  }

  override suspend fun saveShop(shop: Shop): Result<String> {
    val targetId = if (shop.id.isNotBlank()) shop.id else "local_shop_${UUID.randomUUID().toString().take(8)}"
    val saved = shop.copy(
      id = targetId,
      userId = LOCAL_UID,
      enquiryDate = shop.enquiryDate.ifBlank { today },
      callDate = shop.callDate.ifBlank { today },
      lastCallDate = shop.lastCallDate.ifBlank { today },
      createdAt = shop.createdAt ?: Timestamp.now(),
      updatedAt = Timestamp.now(),
      isPendingSync = false,
    )
    shopsFlow.update { current ->
      val existingIndex = current.indexOfFirst { it.id == targetId }
      if (existingIndex >= 0) {
        current.toMutableList().apply { set(existingIndex, saved) }
      } else {
        listOf(saved) + current
      }
    }
    return Result.success(targetId)
  }

  override suspend fun updateShopStatus(shopId: String, newStatus: String): Result<Unit> {
    shopsFlow.update { current ->
      current.map {
        if (it.id == shopId) it.copy(status = newStatus, updatedAt = Timestamp.now()) else it
      }
    }
    return Result.success(Unit)
  }

  override suspend fun updateShopInquiryStatus(
    shopId: String,
    inquiryStatus: String,
    markCalledToday: Boolean,
  ): Result<Unit> {
    shopsFlow.update { current ->
      current.map {
        if (it.id == shopId) {
          it.copy(
            inquiryStatus = inquiryStatus,
            lastCallDate = if (markCalledToday) today else it.lastCallDate,
            updatedAt = Timestamp.now(),
          )
        } else {
          it
        }
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
    val orderId = "local_ord_${UUID.randomUUID().toString().take(8)}"
    val orderCode = "ORD-${(System.currentTimeMillis() % 9000 + 1000)}"
    val totalAmount = validItems.sumOf { (prod, qty) -> prod.defaultPrice * qty }
    val itemCount = validItems.sumOf { it.second }
    val itemsSummary = validItems.joinToString(", ") { (prod, qty) -> "${prod.code} x$qty" }

    val newOrder = OrderRecord(
      id = orderId,
      userId = LOCAL_UID,
      orderCode = orderCode,
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
      createdAt = Timestamp.now(),
      updatedAt = Timestamp.now(),
      isPendingSync = true,
    )
    ordersFlow.update { listOf(newOrder) + it }
    shopsFlow.update { current ->
      current.map {
        if (it.id == shop.id) {
          it.copy(
            inquiryStatus = InquiryStatus.CONFIRMED,
            lastOrderDate = today,
            lastCallDate = today,
            updatedAt = Timestamp.now(),
          )
        } else {
          it
        }
      }
    }
    return Result.success(orderId)
  }

  fun upsertOrderExact(order: OrderRecord, markShopConfirmed: Boolean = true) {
    ordersFlow.update { current ->
      val idx = current.indexOfFirst {
        it.id == order.id || (order.orderCode.isNotBlank() && it.orderCode == order.orderCode)
      }
      if (idx >= 0) {
        current.toMutableList().apply { set(idx, order) }
      } else {
        listOf(order) + current
      }
    }
    if (markShopConfirmed) {
      shopsFlow.update { current ->
        current.map {
          if (it.id == order.shopId) {
            it.copy(
              inquiryStatus = InquiryStatus.CONFIRMED,
              lastOrderDate = today,
              lastCallDate = today,
              updatedAt = Timestamp.now(),
            )
          } else {
            it
          }
        }
      }
    }
  }

  override suspend fun updateOrderStatus(
    orderId: String,
    newStatus: String,
    note: String?,
  ): Result<Unit> {
    ordersFlow.update { current ->
      current.map { ord ->
        if (ord.id == orderId || ord.orderCode == orderId) {
          ord.copy(
            status = newStatus,
            note = note?.trim() ?: ord.note,
            updatedAt = Timestamp.now(),
          )
        } else {
          ord
        }
      }
    }
    return Result.success(Unit)
  }

  override suspend fun updateOrderDetails(
    orderId: String,
    items: List<Pair<CatalogProduct, Int>>,
    note: String,
  ): Result<Unit> {
    val validItems = items.filter { it.second > 0 }
    if (validItems.isEmpty()) {
      return Result.failure(IllegalArgumentException("Select at least one product to update the order."))
    }
    val totalAmount = validItems.sumOf { (prod, qty) -> prod.defaultPrice * qty }
    val itemCount = validItems.sumOf { it.second }
    val itemsSummary = validItems.joinToString(", ") { (prod, qty) -> "${prod.code} x$qty" }.take(780)
    ordersFlow.update { current ->
      current.map { ord ->
        if (ord.id == orderId || ord.orderCode == orderId) {
          ord.copy(
            totalAmount = totalAmount,
            itemCount = itemCount,
            itemsSummary = itemsSummary,
            note = note.trim().take(500),
            updatedAt = Timestamp.now(),
          )
        } else {
          ord
        }
      }
    }
    return Result.success(Unit)
  }

  override suspend fun deleteOrder(orderId: String): Result<Unit> {
    ordersFlow.update { current ->
      current.filterNot { it.id == orderId || it.orderCode == orderId }
    }
    return Result.success(Unit)
  }

  override suspend fun updateStockQuantity(stockId: String, newQuantity: Int): Result<Unit> {
    stocksFlow.update { current ->
      current.map {
        if (it.id == stockId) it.copy(quantity = newQuantity.coerceAtLeast(0)) else it
      }.sortedBy { it.quantity - it.minThreshold }
    }
    return Result.success(Unit)
  }

  override suspend fun saveProduct(product: Product): Result<String> {
    val isUpdate = product.id.isNotBlank() && productsFlow.value.any { it.id == product.id }
    val targetId = if (product.id.isNotBlank()) product.id else "local_prd_${UUID.randomUUID().toString().take(8)}"
    val saved = product.copy(
      id = targetId,
      userId = LOCAL_UID,
      addedDate = product.addedDate.ifBlank { today },
      createdAt = product.createdAt ?: Timestamp.now(),
      updatedAt = Timestamp.now(),
      isPendingSync = true,
    )
    productsFlow.update { current ->
      if (isUpdate) {
        current.map { if (it.id == targetId) saved else it }
      } else {
        listOf(saved) + current
      }
    }
    return Result.success(targetId)
  }

  override suspend fun updateProductStatus(productId: String, newStatus: String): Result<Unit> {
    productsFlow.update { current ->
      current.map {
        if (it.id == productId) it.copy(status = newStatus, updatedAt = Timestamp.now()) else it
      }
    }
    return Result.success(Unit)
  }

  override suspend fun updateProductStock(productId: String, newQuantity: Int): Result<Unit> {
    val safeQty = newQuantity.coerceAtLeast(0)
    productsFlow.update { current ->
      current.map {
        if (it.id == productId) it.copy(stockQuantity = safeQty, updatedAt = Timestamp.now()) else it
      }
    }
    return Result.success(Unit)
  }

  override suspend fun recordPayment(payment: PaymentTransaction): Result<String> {
    if (payment.amount <= 0.0) {
      return Result.failure(IllegalArgumentException("Payment amount must be greater than zero."))
    }
    val targetId = payment.id.ifBlank { "local_pay_${UUID.randomUUID().toString().take(8)}" }
    val saved = payment.copy(
      id = targetId,
      userId = LOCAL_UID,
      receiptCode = payment.receiptCode.ifBlank {
        val prefix = if (payment.direction == PaymentDirection.PAY_SUPPLIER) "PAYOUT" else "RCPT"
        "$prefix-2026-${(System.currentTimeMillis() % 9000 + 1000)}"
      },
      paymentDate = payment.paymentDate.ifBlank { today },
      createdAt = payment.createdAt ?: Timestamp.now(),
      updatedAt = Timestamp.now(),
      isPendingSync = true,
    )
    paymentsFlow.update { current ->
      val idx = current.indexOfFirst { it.id == targetId }
      if (idx >= 0) {
        current.toMutableList().apply { set(idx, saved) }
      } else {
        listOf(saved) + current
      }
    }
    if (saved.direction == PaymentDirection.PAY_SUPPLIER) {
      supplierBillsFlow.update { bills ->
        bills.map { bill ->
          if (bill.id == saved.partyId || (saved.partyCode.isNotBlank() && bill.supplierCode == saved.partyCode && bill.billNumber == saved.invoiceRef)) {
            val newPaid = (bill.paidAmount + saved.amount).coerceAtMost(bill.totalBilled)
            val newStatus = if (newPaid >= bill.totalBilled - 0.5) {
              SupplierBillStatus.SETTLED
            } else {
              PaymentLedgerHelper.effectiveSupplierStatus(bill.copy(paidAmount = newPaid))
            }
            bill.copy(
              paidAmount = newPaid,
              status = newStatus,
              updatedAt = Timestamp.now(),
            )
          } else {
            bill
          }
        }
      }
    }
    return Result.success(targetId)
  }

  override suspend fun saveSupplierBill(bill: SupplierBill): Result<String> {
    val isUpdate = bill.id.isNotBlank() && supplierBillsFlow.value.any { it.id == bill.id }
    val targetId = if (bill.id.isNotBlank()) bill.id else "local_sup_${UUID.randomUUID().toString().take(8)}"
    val computedStatus = PaymentLedgerHelper.effectiveSupplierStatus(bill)
    val saved = bill.copy(
      id = targetId,
      userId = LOCAL_UID,
      billDate = bill.billDate.ifBlank { today },
      dueDate = bill.dueDate.ifBlank { daysOffsetString(7) },
      status = computedStatus,
      createdAt = bill.createdAt ?: Timestamp.now(),
      updatedAt = Timestamp.now(),
      isPendingSync = true,
    )
    supplierBillsFlow.update { current ->
      if (isUpdate) {
        current.map { if (it.id == targetId) saved else it }
      } else {
        listOf(saved) + current
      }
    }
    return Result.success(targetId)
  }

  override suspend fun saveSupplier(supplier: Supplier): Result<String> {
    val isUpdate = supplier.id.isNotBlank() && suppliersFlow.value.any { it.id == supplier.id }
    val targetId = if (supplier.id.isNotBlank()) supplier.id else "local_sup_dir_${UUID.randomUUID().toString().take(8)}"
    val saved = supplier.copy(
      id = targetId,
      userId = LOCAL_UID,
      addedDate = supplier.addedDate.ifBlank { today },
      addedBy = supplier.addedBy.ifBlank { "Local Tester" },
      createdAt = supplier.createdAt ?: Timestamp.now(),
      updatedAt = Timestamp.now(),
      isPendingSync = true,
    )
    suppliersFlow.update { current ->
      if (isUpdate) {
        current.map { if (it.id == targetId) saved else it }
      } else {
        listOf(saved) + current
      }
    }
    return Result.success(targetId)
  }

  override suspend fun updateSupplierStatus(supplierId: String, newStatus: String): Result<Unit> {
    suppliersFlow.update { current ->
      current.map {
        if (it.id == supplierId) it.copy(status = newStatus, updatedAt = Timestamp.now()) else it
      }
    }
    return Result.success(Unit)
  }

  private fun daysAgoString(days: Int): String {
    return daysOffsetString(-days)
  }

  private fun daysOffsetString(offsetDays: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, offsetDays)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
  }

  companion object {
    const val LOCAL_UID = "local_test_user"
  }
}
