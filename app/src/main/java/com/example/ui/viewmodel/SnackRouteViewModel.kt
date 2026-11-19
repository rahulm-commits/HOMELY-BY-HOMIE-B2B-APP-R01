package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PendingOrderEntity
import com.example.data.local.PendingPaymentEntity
import com.example.data.local.PendingProductEntity
import com.example.data.local.PendingShopEntity
import com.example.data.local.PendingStockUpdateEntity
import com.example.data.local.PendingSupplierBillEntity
import com.example.data.local.PendingSupplierEntity
import com.example.data.local.ServerSettings
import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.StockItem
import com.example.data.model.Supplier
import com.example.data.model.SupplierBill
import com.example.data.remote.LaptopHealthInfo
import com.example.data.repository.SnackRouteDataSource
import com.example.data.repository.SyncCoordinatorRepository
import com.example.ui.components.formatInr
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class SnackRouteViewModel(
  private val repository: SnackRouteDataSource,
  private val currentUserId: String,
) : ViewModel() {

  private val coordinator: SyncCoordinatorRepository?
    get() = repository as? SyncCoordinatorRepository

  private val _feedback = MutableSharedFlow<String>(extraBufferCapacity = 6)
  val feedback = _feedback.asSharedFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

  private val _laptopHealth = MutableStateFlow<LaptopHealthInfo?>(null)
  val laptopHealth: StateFlow<LaptopHealthInfo?> = _laptopHealth.asStateFlow()

  val serverSettings: StateFlow<ServerSettings> =
    (coordinator?.observeServerSettings() ?: flowOf(ServerSettings()))
      .catch { emit(ServerSettings()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ServerSettings(),
      )

  val pendingShops: StateFlow<List<PendingShopEntity>> =
    (coordinator?.observePendingShops() ?: flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val pendingOrders: StateFlow<List<PendingOrderEntity>> =
    (coordinator?.observePendingOrders() ?: flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val pendingStockUpdates: StateFlow<List<PendingStockUpdateEntity>> =
    (coordinator?.observePendingStockUpdates() ?: flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val pendingProducts: StateFlow<List<PendingProductEntity>> =
    (coordinator?.observePendingProducts() ?: flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val pendingPayments: StateFlow<List<PendingPaymentEntity>> =
    (coordinator?.observePendingPayments() ?: flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val pendingSupplierBills: StateFlow<List<PendingSupplierBillEntity>> =
    (coordinator?.observePendingSupplierBills() ?: flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val pendingSuppliers: StateFlow<List<PendingSupplierEntity>> =
    (coordinator?.observePendingSuppliers() ?: flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val shopsState: StateFlow<UiState<List<Shop>>> =
    repository.observeShops(currentUserId)
      .map<List<Shop>, UiState<List<Shop>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(UiState.Error(firestoreError?.message ?: error.message ?: "Failed to load shops"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val ordersState: StateFlow<UiState<List<OrderRecord>>> =
    repository.observeOrders(currentUserId)
      .map<List<OrderRecord>, UiState<List<OrderRecord>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(UiState.Error(firestoreError?.message ?: error.message ?: "Failed to load orders"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val stocksState: StateFlow<UiState<List<StockItem>>> =
    repository.observeStocks(currentUserId)
      .map<List<StockItem>, UiState<List<StockItem>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(UiState.Error(firestoreError?.message ?: error.message ?: "Failed to load stocks"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val productsState: StateFlow<UiState<List<Product>>> =
    repository.observeProducts(currentUserId)
      .map<List<Product>, UiState<List<Product>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(UiState.Error(firestoreError?.message ?: error.message ?: "Failed to load products"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val paymentsState: StateFlow<UiState<List<PaymentTransaction>>> =
    repository.observePayments(currentUserId)
      .map<List<PaymentTransaction>, UiState<List<PaymentTransaction>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(UiState.Error(firestoreError?.message ?: error.message ?: "Failed to load payments"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val supplierBillsState: StateFlow<UiState<List<SupplierBill>>> =
    repository.observeSupplierBills(currentUserId)
      .map<List<SupplierBill>, UiState<List<SupplierBill>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(UiState.Error(firestoreError?.message ?: error.message ?: "Failed to load supplier bills"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val suppliersState: StateFlow<UiState<List<Supplier>>> =
    repository.observeSuppliers(currentUserId)
      .map<List<Supplier>, UiState<List<Supplier>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(UiState.Error(firestoreError?.message ?: error.message ?: "Failed to load suppliers"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val metaState: StateFlow<OperationsMeta?> =
    repository.observeMeta(currentUserId)
      .catch { emit(null) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val routesState: StateFlow<List<DeliveryRoute>> =
    repository.observeRoutes()
      .catch { emit(RoutesCatalog.starterRoutes) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = RoutesCatalog.starterRoutes,
      )

  init {
    viewModelScope.launch {
      repository.seedRoutesIfNeeded()
      repository.seedStarterDataIfNeeded()
      syncNow(silent = true)
    }
  }

  fun saveShop(
    shop: Shop,
    isOnline: Boolean,
    onFinished: () -> Unit = {},
    onSaved: () -> Unit,
  ) {
    viewModelScope.launch {
      try {
        val result = repository.saveShop(shop)
        if (result.isSuccess) {
          val savedId = result.getOrNull().orEmpty()
          val stillPending = coordinator?.isShopPending(savedId) ?: !isOnline
          val lastError = if (stillPending) coordinator?.getPendingShopError(savedId).orEmpty() else ""
          val settings = serverSettings.value
          val unsyncedCount = pendingShops.value.count {
            !it.isFullySynced(
              requireGlobal = settings.globalSyncEnabled,
              requireLocalServer = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank(),
            )
          }
          val msg = when {
            !stillPending -> "Shop ${shop.shopCode} saved & synced"
            lastError.isNotBlank() -> "Saved on phone (${maxOf(unsyncedCount, 1)} pending) • $lastError"
            else -> "Saved safely on phone (${maxOf(unsyncedCount, 1)} in queue) • Tap Sync when server connects"
          }
          _feedback.tryEmit(msg)
          onSaved()
        } else {
          _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to save shop")
        }
      } finally {
        onFinished()
      }
    }
  }

  fun updateShopStatus(shopId: String, newStatus: String, label: String) {
    viewModelScope.launch {
      val result = repository.updateShopStatus(shopId, newStatus)
      if (result.isSuccess) {
        _feedback.tryEmit(label)
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not update shop status")
      }
    }
  }

  fun updateShopInquiryStatus(shopId: String, inquiryStatus: String, label: String) {
    viewModelScope.launch {
      val result = repository.updateShopInquiryStatus(shopId, inquiryStatus)
      if (result.isSuccess) {
        _feedback.tryEmit(label)
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not update inquiry status")
      }
    }
  }

  fun saveRoute(route: DeliveryRoute, feedbackMsg: String = "Route & Driver Details Saved!") {
    viewModelScope.launch {
      val result = repository.saveRoute(route)
      if (result.isSuccess) {
        _feedback.tryEmit(feedbackMsg)
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to save route")
      }
    }
  }

  fun updateShopRoute(shopId: String, newRouteId: String, feedbackMsg: String) {
    viewModelScope.launch {
      val result = repository.updateShopRoute(shopId, newRouteId)
      if (result.isSuccess) {
        _feedback.tryEmit(feedbackMsg)
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not update route allocation")
      }
    }
  }

  fun bulkAssignShopsToRoute(shopIds: List<String>, targetRouteId: String, feedbackMsg: String) {
    if (shopIds.isEmpty()) return
    viewModelScope.launch {
      var anyFailure: Throwable? = null
      shopIds.forEach { shopId ->
        val res = repository.updateShopRoute(shopId, targetRouteId)
        if (res.isFailure) anyFailure = res.exceptionOrNull()
      }
      if (anyFailure == null) {
        _feedback.tryEmit(feedbackMsg)
      } else {
        _feedback.tryEmit(anyFailure?.localizedMessage ?: "Some route updates failed")
      }
    }
  }

  fun placeOrder(
    shop: Shop,
    items: List<Pair<CatalogProduct, Int>>,
    note: String,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      val result = repository.placeOrder(shop, items, note)
      if (result.isSuccess) {
        _feedback.tryEmit("Order confirmed for ${shop.name}")
        onSuccess()
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to place order")
      }
    }
  }

  fun updateOrder(
    existingOrder: com.example.data.model.OrderRecord,
    items: List<Pair<CatalogProduct, Int>>,
    note: String,
    onSuccess: () -> Unit,
  ) {
    viewModelScope.launch {
      val result = repository.updateOrderDetails(existingOrder.id, items, note)
      if (result.isSuccess) {
        _feedback.tryEmit("Order ${existingOrder.orderCode} updated")
        onSuccess()
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to update order")
      }
    }
  }

  fun deleteOrder(order: com.example.data.model.OrderRecord) {
    viewModelScope.launch {
      val result = repository.deleteOrder(order.id)
      if (result.isSuccess) {
        _feedback.tryEmit("Order ${order.orderCode} deleted")
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to delete order")
      }
    }
  }

  fun markOrderDelivered(orderId: String, shopName: String, podNote: String) {
    viewModelScope.launch {
      val result = repository.updateOrderStatus(orderId, com.example.data.model.OrderStatus.DELIVERED, podNote)
      if (result.isSuccess) {
        _feedback.tryEmit("Delivered to $shopName")
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not mark order delivered")
      }
    }
  }

  fun updateStockQuantity(stockId: String, newQuantity: Int, productName: String) {
    viewModelScope.launch {
      val result = repository.updateStockQuantity(stockId, newQuantity)
      if (result.isSuccess) {
        _feedback.tryEmit("$productName stock updated to $newQuantity")
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to update stock")
      }
    }
  }

  fun saveProduct(product: Product, isOnline: Boolean, onSaved: () -> Unit) {
    viewModelScope.launch {
      val result = repository.saveProduct(product)
      if (result.isSuccess) {
        val savedId = result.getOrNull().orEmpty()
        val stillPending = coordinator?.isProductPending(savedId) ?: !isOnline
        val msg = if (product.status == com.example.data.model.ProductStatus.DRAFT) {
          "Draft ${product.productCode} saved on device"
        } else if (stillPending) {
          "Product ${product.name} saved offline • Will sync when online"
        } else {
          "Product ${product.name} registered & synced"
        }
        _feedback.tryEmit(msg)
        onSaved()
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to save product")
      }
    }
  }

  fun updateProductStatus(productId: String, newStatus: String, feedbackLabel: String) {
    viewModelScope.launch {
      val result = repository.updateProductStatus(productId, newStatus)
      if (result.isSuccess) {
        _feedback.tryEmit(feedbackLabel)
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not update product status")
      }
    }
  }

  fun updateProductStock(productId: String, newQuantity: Int, productName: String) {
    viewModelScope.launch {
      val result = repository.updateProductStock(productId, newQuantity)
      if (result.isSuccess) {
        _feedback.tryEmit("$productName stock updated to $newQuantity")
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to update product stock")
      }
    }
  }

  fun recordPayment(
    payment: PaymentTransaction,
    isOnline: Boolean,
    onSaved: (String) -> Unit = {},
  ) {
    viewModelScope.launch {
      val result = repository.recordPayment(payment)
      if (result.isSuccess) {
        val savedId = result.getOrNull().orEmpty()
        val stillPending = coordinator?.isPaymentPending(savedId) ?: !isOnline
        val actionLabel = if (payment.direction == PaymentDirection.PAY_SUPPLIER) {
          "Paid ${formatInr(payment.amount)} to ${payment.partyName}"
        } else {
          "Collected ${formatInr(payment.amount)} from ${payment.partyName}"
        }
        val msg = if (stillPending) {
          "$actionLabel • Saved offline (will sync)"
        } else {
          "$actionLabel • Synced"
        }
        _feedback.tryEmit(msg)
        onSaved(savedId)
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to record payment")
      }
    }
  }

  fun saveSupplierBill(
    bill: SupplierBill,
    isOnline: Boolean,
    onSaved: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val result = repository.saveSupplierBill(bill)
      if (result.isSuccess) {
        val msg = if (isOnline) {
          "Supplier bill ${bill.billNumber} (${bill.supplierName}) saved"
        } else {
          "Supplier bill ${bill.billNumber} saved offline"
        }
        _feedback.tryEmit(msg)
        onSaved()
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to save supplier bill")
      }
    }
  }

  fun saveSupplier(
    supplier: Supplier,
    isOnline: Boolean,
    onSaved: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val result = repository.saveSupplier(supplier)
      if (result.isSuccess) {
        val savedId = result.getOrNull().orEmpty()
        val stillPending = coordinator?.isSupplierPending(savedId) ?: !isOnline
        val msg = if (stillPending) {
          "Supplier ${supplier.name} saved offline • Will sync when online"
        } else {
          "Supplier ${supplier.supplierCode} saved & synced"
        }
        _feedback.tryEmit(msg)
        onSaved()
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Failed to save supplier")
      }
    }
  }

  fun updateSupplierStatus(supplierId: String, newStatus: String, feedbackLabel: String) {
    viewModelScope.launch {
      val result = repository.updateSupplierStatus(supplierId, newStatus)
      if (result.isSuccess) {
        _feedback.tryEmit(feedbackLabel)
      } else {
        _feedback.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not update supplier status")
      }
    }
  }

  fun saveServerSettings(
    globalSyncEnabled: Boolean,
    laptopServerEnabled: Boolean,
    laptopServerUrl: String,
    laptopApiToken: String,
    autoDeleteSyncedFromPhone: Boolean,
  ) {
    val coord = coordinator ?: return
    viewModelScope.launch {
      coord.updateServerSettings(
        globalSyncEnabled = globalSyncEnabled,
        laptopServerEnabled = laptopServerEnabled,
        laptopServerUrl = laptopServerUrl,
        laptopApiToken = laptopApiToken,
        autoDeleteSyncedFromPhone = autoDeleteSyncedFromPhone,
      )
      _feedback.tryEmit("Server settings saved")
    }
  }

  fun testLaptopConnection(url: String, token: String) {
    val coord = coordinator ?: return
    viewModelScope.launch {
      _laptopHealth.value = LaptopHealthInfo(reachable = false, message = "Testing connection...")
      val result = coord.testLaptopConnection(url, token)
      if (result.reachable) {
        val cur = serverSettings.value
        if (!cur.laptopServerEnabled) {
          coord.updateServerSettings(
            globalSyncEnabled = cur.globalSyncEnabled,
            laptopServerEnabled = true,
            laptopServerUrl = url,
            laptopApiToken = token,
            autoDeleteSyncedFromPhone = cur.autoDeleteSyncedFromPhone,
          )
        }
        _laptopHealth.value = result.copy(message = "${result.message} • Laptop sync active")
        _feedback.tryEmit("${result.message} • Laptop sync active")
      } else {
        _laptopHealth.value = result
        _feedback.tryEmit(result.message)
      }
    }
  }

  fun syncNow(silent: Boolean = false) {
    val coord = coordinator ?: return
    if (_isSyncing.value) return
    viewModelScope.launch {
      _isSyncing.value = true
      try {
        val outcome = coord.syncAllPending()
        if (!silent || outcome.syncedCount > 0) {
          _feedback.tryEmit(outcome.message)
        }
      } finally {
        _isSyncing.value = false
      }
    }
  }

  fun pullFromLaptopServer() {
    val coord = coordinator ?: return
    if (_isSyncing.value) return
    viewModelScope.launch {
      _isSyncing.value = true
      try {
        val outcome = coord.pullFromLaptopServer()
        _feedback.tryEmit(outcome.message)
      } finally {
        _isSyncing.value = false
      }
    }
  }

  fun deleteSyncedFromPhone() {
    val coord = coordinator ?: return
    viewModelScope.launch {
      val removed = coord.deleteSyncedFromPhone()
      if (removed > 0) {
        _feedback.tryEmit("Deleted $removed synced record(s) from phone storage")
      } else {
        _feedback.tryEmit("No fully synced records to delete yet")
      }
    }
  }

  fun deletePendingShop(localId: String) {
    val coord = coordinator ?: return
    viewModelScope.launch {
      coord.deletePendingShopById(localId)
      _feedback.tryEmit("Removed record from phone queue")
    }
  }

  fun deletePendingOrder(localId: String) {
    val coord = coordinator ?: return
    viewModelScope.launch {
      coord.deletePendingOrderById(localId)
      _feedback.tryEmit("Removed order from phone queue")
    }
  }

  fun onNetworkAvailable() {
    syncNow(silent = true)
  }

  private companion object {
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
