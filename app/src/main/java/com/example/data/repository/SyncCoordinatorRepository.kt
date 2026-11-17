package com.example.data.repository

import android.content.Context
import com.example.data.local.CachedMetaEntity
import com.example.data.local.CachedOrderEntity
import com.example.data.local.CachedShopEntity
import com.example.data.local.CachedStockEntity
import com.example.data.local.DeliveryRouteEntity
import com.example.data.local.PendingOrderEntity
import com.example.data.local.PendingShopEntity
import com.example.data.local.PendingStockUpdateEntity
import com.example.data.local.ServerConfigStore
import com.example.data.local.ServerSettings
import com.example.data.local.SnackRouteDao
import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.StockItem
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
  suspend fun fetchShopsOnce(userId: String): Result<List<Shop>>
  suspend fun fetchOrdersOnce(userId: String): Result<List<OrderRecord>>
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

  override suspend fun fetchShopsOnce(userId: String): Result<List<Shop>> =
    repo.getShopsOnce(userId)

  override suspend fun fetchOrdersOnce(userId: String): Result<List<OrderRecord>> =
    repo.getOrdersOnce(userId)

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

  suspend fun isShopPending(shopId: String): Boolean {
    val settings = configStore.getCurrentSettings()
    val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
    val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()
    val pending = dao.getPendingShopsOnce().firstOrNull { it.localId == shopId || it.remoteShopId == shopId }
      ?: return false
    return !pending.isFullySynced(requireGlobal, requireLaptop)
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
        dao.getPendingStockUpdatesOnce().size
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
    return removed
  }

  suspend fun deletePendingShopById(localId: String) {
    dao.deletePendingShop(localId)
  }

  suspend fun deletePendingOrderById(localId: String) {
    dao.deletePendingOrder(localId)
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

  private fun isPureCancellation(error: Throwable): Boolean {
    val hasFirestore = generateSequence(error) { it.cause }
      .any { it is FirebaseFirestoreException }
    return error is CancellationException && !hasFirestore
  }

  private fun Shop.updatedAtMillis(): Long = updatedAt?.toDate()?.time ?: 0L

  private fun OrderRecord.updatedAtMillis(): Long = updatedAt?.toDate()?.time ?: createdAt?.toDate()?.time ?: 0L

  private fun StockItem.updatedAtMillis(): Long = updatedAt?.toDate()?.time ?: createdAt?.toDate()?.time ?: 0L

  private fun todayString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }
}
