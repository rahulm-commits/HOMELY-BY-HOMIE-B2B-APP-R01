package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DeliveryRoute
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.ProductStatus
import com.example.data.model.Shop
import com.example.data.model.StockItem
import com.example.data.model.Supplier
import com.example.data.model.SupplierBill
import com.example.data.model.SupplierBillStatus
import com.example.data.model.SupplierStatus
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
  val isUpdate: Boolean = false,
  val pendingDelete: Boolean = false,
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

@Entity(tableName = "pending_products")
data class PendingProductEntity(
  @PrimaryKey val localId: String,
  val remoteProductId: String = "",
  val isUpdate: Boolean = false,
  val userId: String = "",
  val productCode: String,
  val sku: String,
  val barcode: String = "",
  val name: String,
  val category: String,
  val brand: String = "",
  val baseUnit: String = "Gram (g)",
  val sellingUnit: String = "Pouch / Packet",
  val casePack: String = "",
  val packSize: String = "",
  val wholesalePrice: Double = 0.0,
  val mrp: Double = 0.0,
  val stockQuantity: Int = 0,
  val minThreshold: Int = 30,
  val photoUri: String = "",
  val notes: String = "",
  val status: String = ProductStatus.ACTIVE,
  val salesVelocity: Int = 0,
  val hasVariants: Boolean = false,
  val variantsJson: String = "",
  val addedDate: String = "",
  val globalSynced: Boolean = false,
  val lastError: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun isFullySynced(requireGlobal: Boolean): Boolean = !requireGlobal || globalSynced

  fun toDomainProduct(pendingSync: Boolean = true): Product = Product(
    id = remoteProductId.ifBlank { localId },
    userId = userId,
    productCode = productCode,
    sku = sku,
    barcode = barcode,
    name = name,
    category = category,
    brand = brand,
    baseUnit = baseUnit,
    sellingUnit = sellingUnit,
    casePack = casePack,
    packSize = packSize,
    wholesalePrice = wholesalePrice,
    mrp = mrp,
    stockQuantity = stockQuantity,
    minThreshold = minThreshold,
    photoUri = photoUri,
    notes = notes,
    status = status,
    salesVelocity = salesVelocity,
    hasVariants = hasVariants,
    variantsJson = variantsJson,
    addedDate = addedDate,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = pendingSync,
  )

  companion object {
    fun fromDomainProduct(
      product: Product,
      localId: String,
      userId: String,
      isUpdate: Boolean,
    ): PendingProductEntity {
      val now = System.currentTimeMillis()
      return PendingProductEntity(
        localId = localId,
        remoteProductId = product.id,
        isUpdate = isUpdate,
        userId = userId,
        productCode = product.productCode,
        sku = product.sku,
        barcode = product.barcode,
        name = product.name,
        category = product.category,
        brand = product.brand,
        baseUnit = product.baseUnit,
        sellingUnit = product.sellingUnit,
        casePack = product.casePack,
        packSize = product.packSize,
        wholesalePrice = product.wholesalePrice,
        mrp = product.mrp,
        stockQuantity = product.stockQuantity,
        minThreshold = product.minThreshold,
        photoUri = product.photoUri,
        notes = product.notes,
        status = product.status,
        salesVelocity = product.salesVelocity,
        hasVariants = product.hasVariants,
        variantsJson = product.variantsJson,
        addedDate = product.addedDate,
        createdAtMillis = product.createdAt?.toDate()?.time ?: now,
        updatedAtMillis = now,
      )
    }
  }
}

@Entity(tableName = "cached_products")
data class CachedProductEntity(
  @PrimaryKey val id: String,
  val userId: String = "",
  val productCode: String,
  val sku: String,
  val barcode: String = "",
  val name: String,
  val category: String,
  val brand: String = "",
  val baseUnit: String = "Gram (g)",
  val sellingUnit: String = "Pouch / Packet",
  val casePack: String = "",
  val packSize: String = "",
  val wholesalePrice: Double = 0.0,
  val mrp: Double = 0.0,
  val stockQuantity: Int = 0,
  val minThreshold: Int = 30,
  val photoUri: String = "",
  val notes: String = "",
  val status: String = ProductStatus.ACTIVE,
  val salesVelocity: Int = 0,
  val hasVariants: Boolean = false,
  val variantsJson: String = "",
  val addedDate: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun toDomainProduct(): Product = Product(
    id = id,
    userId = userId,
    productCode = productCode,
    sku = sku,
    barcode = barcode,
    name = name,
    category = category,
    brand = brand,
    baseUnit = baseUnit,
    sellingUnit = sellingUnit,
    casePack = casePack,
    packSize = packSize,
    wholesalePrice = wholesalePrice,
    mrp = mrp,
    stockQuantity = stockQuantity,
    minThreshold = minThreshold,
    photoUri = photoUri,
    notes = notes,
    status = status,
    salesVelocity = salesVelocity,
    hasVariants = hasVariants,
    variantsJson = variantsJson,
    addedDate = addedDate,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = false,
  )

  companion object {
    fun fromDomainProduct(product: Product, fallbackUserId: String = ""): CachedProductEntity {
      val now = System.currentTimeMillis()
      return CachedProductEntity(
        id = product.id.ifBlank { product.productCode },
        userId = product.userId.ifBlank { fallbackUserId },
        productCode = product.productCode,
        sku = product.sku,
        barcode = product.barcode,
        name = product.name,
        category = product.category,
        brand = product.brand,
        baseUnit = product.baseUnit,
        sellingUnit = product.sellingUnit,
        casePack = product.casePack,
        packSize = product.packSize,
        wholesalePrice = product.wholesalePrice,
        mrp = product.mrp,
        stockQuantity = product.stockQuantity,
        minThreshold = product.minThreshold,
        photoUri = product.photoUri,
        notes = product.notes,
        status = product.status,
        salesVelocity = product.salesVelocity,
        hasVariants = product.hasVariants,
        variantsJson = product.variantsJson,
        addedDate = product.addedDate,
        createdAtMillis = product.createdAt?.toDate()?.time ?: now,
        updatedAtMillis = product.updatedAt?.toDate()?.time ?: now,
      )
    }
  }
}

@Entity(tableName = "pending_payments")
data class PendingPaymentEntity(
  @PrimaryKey val localId: String,
  val remotePaymentId: String = "",
  val isUpdate: Boolean = false,
  val userId: String = "",
  val receiptCode: String,
  val direction: String = PaymentDirection.COLLECT,
  val partyId: String,
  val partyCode: String = "",
  val partyName: String,
  val contactName: String = "",
  val phone: String = "",
  val invoiceRef: String = "",
  val amount: Double = 0.0,
  val paymentMethod: String = PaymentMethod.UPI,
  val referenceNumber: String = "",
  val paymentDate: String = "",
  val recordedBy: String = "",
  val note: String = "",
  val globalSynced: Boolean = false,
  val lastError: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun isFullySynced(requireGlobal: Boolean): Boolean = !requireGlobal || globalSynced

  fun toDomainPayment(pendingSync: Boolean = true): PaymentTransaction = PaymentTransaction(
    id = remotePaymentId.ifBlank { localId },
    userId = userId,
    receiptCode = receiptCode,
    direction = direction,
    partyId = partyId,
    partyCode = partyCode,
    partyName = partyName,
    contactName = contactName,
    phone = phone,
    invoiceRef = invoiceRef,
    amount = amount,
    paymentMethod = paymentMethod,
    referenceNumber = referenceNumber,
    paymentDate = paymentDate,
    recordedBy = recordedBy,
    note = note,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = pendingSync,
  )

  companion object {
    fun fromDomainPayment(
      payment: PaymentTransaction,
      localId: String,
      userId: String,
      isUpdate: Boolean,
    ): PendingPaymentEntity {
      val now = System.currentTimeMillis()
      return PendingPaymentEntity(
        localId = localId,
        remotePaymentId = payment.id,
        isUpdate = isUpdate,
        userId = userId,
        receiptCode = payment.receiptCode,
        direction = payment.direction,
        partyId = payment.partyId,
        partyCode = payment.partyCode,
        partyName = payment.partyName,
        contactName = payment.contactName,
        phone = payment.phone,
        invoiceRef = payment.invoiceRef,
        amount = payment.amount,
        paymentMethod = payment.paymentMethod,
        referenceNumber = payment.referenceNumber,
        paymentDate = payment.paymentDate,
        recordedBy = payment.recordedBy,
        note = payment.note,
        createdAtMillis = payment.createdAt?.toDate()?.time ?: now,
        updatedAtMillis = now,
      )
    }
  }
}

@Entity(tableName = "cached_payments")
data class CachedPaymentEntity(
  @PrimaryKey val id: String,
  val userId: String = "",
  val receiptCode: String,
  val direction: String = PaymentDirection.COLLECT,
  val partyId: String,
  val partyCode: String = "",
  val partyName: String,
  val contactName: String = "",
  val phone: String = "",
  val invoiceRef: String = "",
  val amount: Double = 0.0,
  val paymentMethod: String = PaymentMethod.UPI,
  val referenceNumber: String = "",
  val paymentDate: String = "",
  val recordedBy: String = "",
  val note: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun toDomainPayment(): PaymentTransaction = PaymentTransaction(
    id = id,
    userId = userId,
    receiptCode = receiptCode,
    direction = direction,
    partyId = partyId,
    partyCode = partyCode,
    partyName = partyName,
    contactName = contactName,
    phone = phone,
    invoiceRef = invoiceRef,
    amount = amount,
    paymentMethod = paymentMethod,
    referenceNumber = referenceNumber,
    paymentDate = paymentDate,
    recordedBy = recordedBy,
    note = note,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = false,
  )

  companion object {
    fun fromDomainPayment(
      payment: PaymentTransaction,
      fallbackUserId: String = "",
    ): CachedPaymentEntity {
      val now = System.currentTimeMillis()
      return CachedPaymentEntity(
        id = payment.id.ifBlank { payment.receiptCode },
        userId = payment.userId.ifBlank { fallbackUserId },
        receiptCode = payment.receiptCode,
        direction = payment.direction,
        partyId = payment.partyId,
        partyCode = payment.partyCode,
        partyName = payment.partyName,
        contactName = payment.contactName,
        phone = payment.phone,
        invoiceRef = payment.invoiceRef,
        amount = payment.amount,
        paymentMethod = payment.paymentMethod,
        referenceNumber = payment.referenceNumber,
        paymentDate = payment.paymentDate,
        recordedBy = payment.recordedBy,
        note = payment.note,
        createdAtMillis = payment.createdAt?.toDate()?.time ?: now,
        updatedAtMillis = payment.updatedAt?.toDate()?.time ?: now,
      )
    }
  }
}

@Entity(tableName = "pending_supplier_bills")
data class PendingSupplierBillEntity(
  @PrimaryKey val localId: String,
  val remoteBillId: String = "",
  val isUpdate: Boolean = false,
  val userId: String = "",
  val supplierCode: String,
  val supplierName: String,
  val contactPerson: String = "",
  val phone: String = "",
  val location: String = "",
  val category: String = "Raw Materials",
  val billNumber: String,
  val totalBilled: Double = 0.0,
  val paidAmount: Double = 0.0,
  val billDate: String = "",
  val dueDate: String = "",
  val status: String = SupplierBillStatus.PENDING,
  val notes: String = "",
  val globalSynced: Boolean = false,
  val lastError: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun isFullySynced(requireGlobal: Boolean): Boolean = !requireGlobal || globalSynced

  fun toDomainBill(pendingSync: Boolean = true): SupplierBill = SupplierBill(
    id = remoteBillId.ifBlank { localId },
    userId = userId,
    supplierCode = supplierCode,
    supplierName = supplierName,
    contactPerson = contactPerson,
    phone = phone,
    location = location,
    category = category,
    billNumber = billNumber,
    totalBilled = totalBilled,
    paidAmount = paidAmount,
    billDate = billDate,
    dueDate = dueDate,
    status = status,
    notes = notes,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = pendingSync,
  )

  companion object {
    fun fromDomainBill(
      bill: SupplierBill,
      localId: String,
      userId: String,
      isUpdate: Boolean,
    ): PendingSupplierBillEntity {
      val now = System.currentTimeMillis()
      return PendingSupplierBillEntity(
        localId = localId,
        remoteBillId = bill.id,
        isUpdate = isUpdate,
        userId = userId,
        supplierCode = bill.supplierCode,
        supplierName = bill.supplierName,
        contactPerson = bill.contactPerson,
        phone = bill.phone,
        location = bill.location,
        category = bill.category,
        billNumber = bill.billNumber,
        totalBilled = bill.totalBilled,
        paidAmount = bill.paidAmount,
        billDate = bill.billDate,
        dueDate = bill.dueDate,
        status = bill.status,
        notes = bill.notes,
        createdAtMillis = bill.createdAt?.toDate()?.time ?: now,
        updatedAtMillis = now,
      )
    }
  }
}

@Entity(tableName = "cached_supplier_bills")
data class CachedSupplierBillEntity(
  @PrimaryKey val id: String,
  val userId: String = "",
  val supplierCode: String,
  val supplierName: String,
  val contactPerson: String = "",
  val phone: String = "",
  val location: String = "",
  val category: String = "Raw Materials",
  val billNumber: String,
  val totalBilled: Double = 0.0,
  val paidAmount: Double = 0.0,
  val billDate: String = "",
  val dueDate: String = "",
  val status: String = SupplierBillStatus.PENDING,
  val notes: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun toDomainBill(): SupplierBill = SupplierBill(
    id = id,
    userId = userId,
    supplierCode = supplierCode,
    supplierName = supplierName,
    contactPerson = contactPerson,
    phone = phone,
    location = location,
    category = category,
    billNumber = billNumber,
    totalBilled = totalBilled,
    paidAmount = paidAmount,
    billDate = billDate,
    dueDate = dueDate,
    status = status,
    notes = notes,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = false,
  )

  companion object {
    fun fromDomainBill(
      bill: SupplierBill,
      fallbackUserId: String = "",
    ): CachedSupplierBillEntity {
      val now = System.currentTimeMillis()
      return CachedSupplierBillEntity(
        id = bill.id.ifBlank { bill.billNumber },
        userId = bill.userId.ifBlank { fallbackUserId },
        supplierCode = bill.supplierCode,
        supplierName = bill.supplierName,
        contactPerson = bill.contactPerson,
        phone = bill.phone,
        location = bill.location,
        category = bill.category,
        billNumber = bill.billNumber,
        totalBilled = bill.totalBilled,
        paidAmount = bill.paidAmount,
        billDate = bill.billDate,
        dueDate = bill.dueDate,
        status = bill.status,
        notes = bill.notes,
        createdAtMillis = bill.createdAt?.toDate()?.time ?: now,
        updatedAtMillis = bill.updatedAt?.toDate()?.time ?: now,
      )
    }
  }
}

@Entity(tableName = "pending_suppliers")
data class PendingSupplierEntity(
  @PrimaryKey val localId: String,
  val remoteSupplierId: String = "",
  val isUpdate: Boolean = false,
  val userId: String = "",
  val supplierCode: String,
  val name: String,
  val category: String = "Wholesale Staples & Grains",
  val contactPerson: String = "",
  val contactRole: String = "",
  val altContactPerson: String = "",
  val phone: String = "",
  val altPhone: String = "",
  val email: String = "",
  val address: String = "",
  val pinCode: String = "",
  val mapsLink: String = "",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val photoUri: String = "",
  val gstin: String = "",
  val kycVerified: Boolean = true,
  val creditTerms: String = "Net 15 Days",
  val confirmedPriceNotes: String = "",
  val notes: String = "",
  val status: String = SupplierStatus.ACTIVE,
  val addedDate: String = "",
  val addedBy: String = "",
  val globalSynced: Boolean = false,
  val lastError: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun isFullySynced(requireGlobal: Boolean): Boolean = !requireGlobal || globalSynced

  fun toDomainSupplier(pendingSync: Boolean = true): Supplier = Supplier(
    id = remoteSupplierId.ifBlank { localId },
    userId = userId,
    supplierCode = supplierCode,
    name = name,
    category = category,
    contactPerson = contactPerson,
    contactRole = contactRole,
    altContactPerson = altContactPerson,
    phone = phone,
    altPhone = altPhone,
    email = email,
    address = address,
    pinCode = pinCode,
    mapsLink = mapsLink,
    latitude = latitude,
    longitude = longitude,
    photoUri = photoUri,
    gstin = gstin,
    kycVerified = kycVerified,
    creditTerms = creditTerms,
    confirmedPriceNotes = confirmedPriceNotes,
    notes = notes,
    status = status,
    addedDate = addedDate,
    addedBy = addedBy,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = pendingSync,
  )

  companion object {
    fun fromDomainSupplier(
      supplier: Supplier,
      localId: String,
      userId: String,
      isUpdate: Boolean,
    ): PendingSupplierEntity {
      val now = System.currentTimeMillis()
      return PendingSupplierEntity(
        localId = localId,
        remoteSupplierId = supplier.id,
        isUpdate = isUpdate,
        userId = userId,
        supplierCode = supplier.supplierCode,
        name = supplier.name,
        category = supplier.category,
        contactPerson = supplier.contactPerson,
        contactRole = supplier.contactRole,
        altContactPerson = supplier.altContactPerson,
        phone = supplier.phone,
        altPhone = supplier.altPhone,
        email = supplier.email,
        address = supplier.address,
        pinCode = supplier.pinCode,
        mapsLink = supplier.mapsLink,
        latitude = supplier.latitude,
        longitude = supplier.longitude,
        photoUri = supplier.photoUri,
        gstin = supplier.gstin,
        kycVerified = supplier.kycVerified,
        creditTerms = supplier.creditTerms,
        confirmedPriceNotes = supplier.confirmedPriceNotes,
        notes = supplier.notes,
        status = supplier.status,
        addedDate = supplier.addedDate,
        addedBy = supplier.addedBy,
        createdAtMillis = supplier.createdAt?.toDate()?.time ?: now,
        updatedAtMillis = now,
      )
    }
  }
}

@Entity(tableName = "cached_suppliers")
data class CachedSupplierEntity(
  @PrimaryKey val id: String,
  val userId: String = "",
  val supplierCode: String,
  val name: String,
  val category: String = "Wholesale Staples & Grains",
  val contactPerson: String = "",
  val contactRole: String = "",
  val altContactPerson: String = "",
  val phone: String = "",
  val altPhone: String = "",
  val email: String = "",
  val address: String = "",
  val pinCode: String = "",
  val mapsLink: String = "",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val photoUri: String = "",
  val gstin: String = "",
  val kycVerified: Boolean = true,
  val creditTerms: String = "Net 15 Days",
  val confirmedPriceNotes: String = "",
  val notes: String = "",
  val status: String = SupplierStatus.ACTIVE,
  val addedDate: String = "",
  val addedBy: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
) {
  fun toDomainSupplier(): Supplier = Supplier(
    id = id,
    userId = userId,
    supplierCode = supplierCode,
    name = name,
    category = category,
    contactPerson = contactPerson,
    contactRole = contactRole,
    altContactPerson = altContactPerson,
    phone = phone,
    altPhone = altPhone,
    email = email,
    address = address,
    pinCode = pinCode,
    mapsLink = mapsLink,
    latitude = latitude,
    longitude = longitude,
    photoUri = photoUri,
    gstin = gstin,
    kycVerified = kycVerified,
    creditTerms = creditTerms,
    confirmedPriceNotes = confirmedPriceNotes,
    notes = notes,
    status = status,
    addedDate = addedDate,
    addedBy = addedBy,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(updatedAtMillis)),
    isPendingSync = false,
  )

  companion object {
    fun fromDomainSupplier(
      supplier: Supplier,
      fallbackUserId: String = "",
    ): CachedSupplierEntity {
      val now = System.currentTimeMillis()
      return CachedSupplierEntity(
        id = supplier.id.ifBlank { supplier.supplierCode },
        userId = supplier.userId.ifBlank { fallbackUserId },
        supplierCode = supplier.supplierCode,
        name = supplier.name,
        category = supplier.category,
        contactPerson = supplier.contactPerson,
        contactRole = supplier.contactRole,
        altContactPerson = supplier.altContactPerson,
        phone = supplier.phone,
        altPhone = supplier.altPhone,
        email = supplier.email,
        address = supplier.address,
        pinCode = supplier.pinCode,
        mapsLink = supplier.mapsLink,
        latitude = supplier.latitude,
        longitude = supplier.longitude,
        photoUri = supplier.photoUri,
        gstin = supplier.gstin,
        kycVerified = supplier.kycVerified,
        creditTerms = supplier.creditTerms,
        confirmedPriceNotes = supplier.confirmedPriceNotes,
        notes = supplier.notes,
        status = supplier.status,
        addedDate = supplier.addedDate,
        addedBy = supplier.addedBy,
        createdAtMillis = supplier.createdAt?.toDate()?.time ?: now,
        updatedAtMillis = supplier.updatedAt?.toDate()?.time ?: now,
      )
    }
  }
}

@Entity(tableName = "pending_routes")
data class PendingRouteEntity(
  @PrimaryKey val routeId: String,
  val isUpdate: Boolean = true,
  val userId: String = "",
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

  fun toDomainRoute(): DeliveryRoute = DeliveryRoute(
    id = routeId,
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
    fun fromDomainRoute(
      route: DeliveryRoute,
      userId: String,
      isUpdate: Boolean,
    ): PendingRouteEntity {
      val now = System.currentTimeMillis()
      return PendingRouteEntity(
        routeId = route.id,
        isUpdate = isUpdate,
        userId = userId,
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
        createdAtMillis = now,
        updatedAtMillis = now,
      )
    }
  }
}
