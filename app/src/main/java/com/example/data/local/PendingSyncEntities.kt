package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DeliveryRoute
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.Shop
import com.example.data.model.StockItem
import com.google.firebase.Timestamp
import java.util.Date

@Entity(tableName = "delivery_routes")
data class DeliveryRouteEntity(
  @PrimaryKey val id: String,
  val name: String,
  val vehicle: String,
  val maxCrates: Int,
  val driverName: String,
  val driverPhone: String,
  val dispatchWindow: String,
  val stagingBay: String,
  val colorIndex: Int,
  val isActive: Boolean = true,
  val stopOrderIdsCsv: String = "",
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun toDomainRoute(): DeliveryRoute = DeliveryRoute(
    id = id,
    name = name,
    vehicle = vehicle,
    maxCrates = maxCrates,
    driverName = driverName,
    driverPhone = driverPhone,
    dispatchWindow = dispatchWindow,
    stagingBay = stagingBay,
    colorIndex = colorIndex,
    isActive = isActive,
    stopOrderIds = stopOrderIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
  )

  companion object {
    fun fromDomainRoute(route: DeliveryRoute): DeliveryRouteEntity = DeliveryRouteEntity(
      id = route.id,
      name = route.name,
      vehicle = route.vehicle,
      maxCrates = route.maxCrates,
      driverName = route.driverName,
      driverPhone = route.driverPhone,
      dispatchWindow = route.dispatchWindow,
      stagingBay = route.stagingBay,
      colorIndex = route.colorIndex,
      isActive = route.isActive,
      stopOrderIdsCsv = route.stopOrderIds.joinToString(","),
      updatedAtMillis = System.currentTimeMillis(),
    )
  }
}

@Entity(tableName = "pending_shops")
data class PendingShopEntity(
  @PrimaryKey val localId: String,
  val remoteShopId: String = "",
  val isUpdate: Boolean = false,
  val userId: String = "",
  val shopCode: String,
  val name: String,
  val ownerName: String,
  val phone: String,
  val secondaryPhone: String = "",
  val address: String,
  val pinCode: String,
  val routeId: String,
  val photoUri: String = "",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val note: String = "",
  val status: String,
  val inquiryStatus: String,
  val enquiryDate: String = "",
  val callDate: String = "",
  val lastOrderDate: String = "",
  val lastCallDate: String = "",
  val confirmedPriceNotes: String = "",
  val mapsLink: String = "",
  val addedBy: String = "",
  val globalSynced: Boolean = false,
  val localServerSynced: Boolean = false,
  val lastError: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun isFullySynced(requireGlobal: Boolean, requireLocalServer: Boolean): Boolean {
    val hasAtLeastOneSync = globalSynced || localServerSynced
    if (!hasAtLeastOneSync) return false
    val globalOk = !requireGlobal || globalSynced
    val localOk = !requireLocalServer || localServerSynced
    return globalOk && localOk
  }

  fun toDomainShop(pendingSync: Boolean = true): Shop = Shop(
    id = remoteShopId.ifBlank { localId },
    userId = userId,
    shopCode = shopCode,
    name = name,
    ownerName = ownerName,
    phone = phone,
    secondaryPhone = secondaryPhone,
    address = address,
    pinCode = pinCode,
    routeId = routeId,
    photoUri = photoUri,
    latitude = latitude,
    longitude = longitude,
    note = note,
    status = status,
    inquiryStatus = inquiryStatus,
    enquiryDate = enquiryDate,
    callDate = callDate,
    lastOrderDate = lastOrderDate,
    lastCallDate = lastCallDate,
    confirmedPriceNotes = confirmedPriceNotes,
    mapsLink = mapsLink,
    addedBy = addedBy,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = pendingSync,
  )

  companion object {
    fun fromDomainShop(
      shop: Shop,
      localId: String,
      userId: String,
      isUpdate: Boolean,
    ): PendingShopEntity {
      val now = System.currentTimeMillis()
      return PendingShopEntity(
        localId = localId,
        remoteShopId = shop.id,
        isUpdate = isUpdate,
        userId = userId,
        shopCode = shop.shopCode,
        name = shop.name,
        ownerName = shop.ownerName,
        phone = shop.phone,
        secondaryPhone = shop.secondaryPhone,
        address = shop.address,
        pinCode = shop.pinCode,
        routeId = shop.routeId,
        photoUri = shop.photoUri,
        latitude = shop.latitude,
        longitude = shop.longitude,
        note = shop.note,
        status = shop.status,
        inquiryStatus = shop.inquiryStatus,
        enquiryDate = shop.enquiryDate,
        callDate = shop.callDate,
        lastOrderDate = shop.lastOrderDate,
        lastCallDate = shop.lastCallDate,
        confirmedPriceNotes = shop.confirmedPriceNotes,
        mapsLink = shop.mapsLink,
        addedBy = shop.addedBy,
        createdAtMillis = shop.createdAt?.toDate()?.time ?: now,
        updatedAtMillis = now,
      )
    }
  }
}

@Entity(tableName = "cached_shops")
data class CachedShopEntity(
  @PrimaryKey val id: String,
  val source: String = "laptop",
  val userId: String = "",
  val shopCode: String,
  val name: String,
  val ownerName: String,
  val phone: String,
  val secondaryPhone: String = "",
  val address: String,
  val pinCode: String,
  val routeId: String,
  val photoUri: String = "",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val note: String = "",
  val status: String,
  val inquiryStatus: String,
  val enquiryDate: String = "",
  val callDate: String = "",
  val lastOrderDate: String = "",
  val lastCallDate: String = "",
  val confirmedPriceNotes: String = "",
  val mapsLink: String = "",
  val addedBy: String = "",
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun toDomainShop(): Shop = Shop(
    id = id,
    userId = userId,
    shopCode = shopCode,
    name = name,
    ownerName = ownerName,
    phone = phone,
    secondaryPhone = secondaryPhone,
    address = address,
    pinCode = pinCode,
    routeId = routeId,
    photoUri = photoUri,
    latitude = latitude,
    longitude = longitude,
    note = note,
    status = status,
    inquiryStatus = inquiryStatus,
    enquiryDate = enquiryDate,
    callDate = callDate,
    lastOrderDate = lastOrderDate,
    lastCallDate = lastCallDate,
    confirmedPriceNotes = confirmedPriceNotes,
    mapsLink = mapsLink,
    addedBy = addedBy,
    createdAt = Timestamp(Date(updatedAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = false,
  )

  companion object {
    fun fromDomainShop(shop: Shop, source: String): CachedShopEntity = CachedShopEntity(
      id = shop.id.ifBlank { shop.shopCode },
      source = source,
      userId = shop.userId,
      shopCode = shop.shopCode,
      name = shop.name,
      ownerName = shop.ownerName,
      phone = shop.phone,
      secondaryPhone = shop.secondaryPhone,
      address = shop.address,
      pinCode = shop.pinCode,
      routeId = shop.routeId,
      photoUri = shop.photoUri,
      latitude = shop.latitude,
      longitude = shop.longitude,
      note = shop.note,
      status = shop.status,
      inquiryStatus = shop.inquiryStatus,
      enquiryDate = shop.enquiryDate,
      callDate = shop.callDate,
      lastOrderDate = shop.lastOrderDate,
      lastCallDate = shop.lastCallDate,
      confirmedPriceNotes = shop.confirmedPriceNotes,
      mapsLink = shop.mapsLink,
      addedBy = shop.addedBy,
      updatedAtMillis = shop.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
    )
  }
}

@Entity(tableName = "pending_orders")
data class PendingOrderEntity(
  @PrimaryKey val localId: String,
  val orderCode: String,
  val userId: String = "",
  val shopId: String,
  val shopCode: String,
  val shopName: String,
  val routeId: String,
  val status: String,
  val totalAmount: Double,
  val itemCount: Int,
  val itemsSummary: String,
  val orderDate: String,
  val note: String = "",
  val globalSynced: Boolean = false,
  val localServerSynced: Boolean = false,
  val lastError: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
) {
  fun isFullySynced(requireGlobal: Boolean, requireLocalServer: Boolean): Boolean {
    val hasAtLeastOneSync = globalSynced || localServerSynced
    if (!hasAtLeastOneSync) return false
    val globalOk = !requireGlobal || globalSynced
    val localOk = !requireLocalServer || localServerSynced
    return globalOk && localOk
  }

  fun toDomainOrder(pendingSync: Boolean = true): OrderRecord = OrderRecord(
    id = localId,
    userId = userId,
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
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(createdAtMillis)),
    isPendingSync = pendingSync,
  )
}

@Entity(tableName = "cached_orders")
data class CachedOrderEntity(
  @PrimaryKey val id: String,
  val source: String = "laptop",
  val userId: String = "",
  val orderCode: String,
  val shopId: String,
  val shopCode: String,
  val shopName: String,
  val routeId: String,
  val status: String,
  val totalAmount: Double,
  val itemCount: Int,
  val itemsSummary: String,
  val orderDate: String,
  val note: String = "",
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun toDomainOrder(): OrderRecord = OrderRecord(
    id = id,
    userId = userId,
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
    createdAt = Timestamp(Date(updatedAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = false,
  )

  companion object {
    fun fromDomainOrder(order: OrderRecord, source: String): CachedOrderEntity = CachedOrderEntity(
      id = order.id.ifBlank { order.orderCode },
      source = source,
      userId = order.userId,
      orderCode = order.orderCode,
      shopId = order.shopId,
      shopCode = order.shopCode,
      shopName = order.shopName,
      routeId = order.routeId,
      status = order.status,
      totalAmount = order.totalAmount,
      itemCount = order.itemCount,
      itemsSummary = order.itemsSummary,
      orderDate = order.orderDate,
      note = order.note,
      updatedAtMillis = order.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
    )
  }
}

@Entity(tableName = "cached_stocks")
data class CachedStockEntity(
  @PrimaryKey val id: String,
  val userId: String = "",
  val productCode: String,
  val productName: String,
  val packSize: String,
  val category: String,
  val quantity: Int,
  val minThreshold: Int,
  val unitPrice: Double,
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun toDomainStock(): StockItem = StockItem(
    id = id,
    userId = userId,
    productCode = productCode,
    productName = productName,
    packSize = packSize,
    category = category,
    quantity = quantity,
    minThreshold = minThreshold,
    unitPrice = unitPrice,
    updatedAt = Timestamp(Date(updatedAtMillis)),
  )

  companion object {
    fun fromDomainStock(stock: StockItem, fallbackUserId: String = ""): CachedStockEntity = CachedStockEntity(
      id = stock.id.ifBlank { stock.productCode },
      userId = stock.userId.ifBlank { fallbackUserId },
      productCode = stock.productCode,
      productName = stock.productName,
      packSize = stock.packSize,
      category = stock.category,
      quantity = stock.quantity,
      minThreshold = stock.minThreshold,
      unitPrice = stock.unitPrice,
      updatedAtMillis = stock.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
    )
  }
}

@Entity(tableName = "pending_stock_updates")
data class PendingStockUpdateEntity(
  @PrimaryKey val stockId: String,
  val userId: String = "",
  val productName: String,
  val newQuantity: Int,
  val globalSynced: Boolean = false,
  val lastError: String = "",
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_meta")
data class CachedMetaEntity(
  @PrimaryKey val userId: String,
  val shopRegTarget: Int,
  val vehiclesToDispatch: Int,
  val vehiclesOnRoad: Int,
  val returnsCollected: Int,
  val toCollectAmount: Double,
  val toCollectShops: Int,
  val toSuppliersAmount: Double,
  val toSuppliersCount: Int,
  val seeded: Boolean,
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun toDomainMeta(): OperationsMeta = OperationsMeta(
    id = "summary",
    userId = userId,
    shopRegTarget = shopRegTarget,
    vehiclesToDispatch = vehiclesToDispatch,
    vehiclesOnRoad = vehiclesOnRoad,
    returnsCollected = returnsCollected,
    toCollectAmount = toCollectAmount,
    toCollectShops = toCollectShops,
    toSuppliersAmount = toSuppliersAmount,
    toSuppliersCount = toSuppliersCount,
    seeded = seeded,
    updatedAt = Timestamp(Date(updatedAtMillis)),
  )

  companion object {
    fun fromDomainMeta(meta: OperationsMeta, fallbackUserId: String = ""): CachedMetaEntity = CachedMetaEntity(
      userId = meta.userId.ifBlank { fallbackUserId },
      shopRegTarget = meta.shopRegTarget,
      vehiclesToDispatch = meta.vehiclesToDispatch,
      vehiclesOnRoad = meta.vehiclesOnRoad,
      returnsCollected = meta.returnsCollected,
      toCollectAmount = meta.toCollectAmount,
      toCollectShops = meta.toCollectShops,
      toSuppliersAmount = meta.toSuppliersAmount,
      toSuppliersCount = meta.toSuppliersCount,
      seeded = meta.seeded,
      updatedAtMillis = meta.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
    )
  }
}
