package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Locale

private val isoDateFormatLocal = ThreadLocal.withInitial {
  SimpleDateFormat("yyyy-MM-dd", Locale.US)
}

@IgnoreExtraProperties
data class Shop(
  @DocumentId val id: String = "",
  val userId: String = "",
  val shopCode: String = "",
  val name: String = "",
  val ownerName: String = "",
  val phone: String = "",
  val secondaryPhone: String = "",
  val address: String = "",
  val pinCode: String = "",
  val routeId: String = RoutesCatalog.R01_KANNUR_BANGALORE,
  val photoUri: String = "",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val note: String = "",
  val status: String = ShopStatus.ACTIVE,
  val inquiryStatus: String = InquiryStatus.PENDING_INQUIRY,
  val enquiryDate: String = "",
  val callDate: String = "",
  val lastOrderDate: String = "",
  val lastCallDate: String = "",
  val confirmedPriceNotes: String = "",
  val mapsLink: String = "",
  val addedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  @get:Exclude val isPendingSync: Boolean = false,
)

object ShopStatus {
  const val ACTIVE = "active"
  const val INACTIVE = "inactive"
  const val DELETED = "deleted"
}

object InquiryStatus {
  const val PENDING_INQUIRY = "pending_inquiry"
  const val CONFIRMED = "confirmed"
  const val UNATTENDED = "unattended"
  const val NO_ORDER = "no_order"
}

@IgnoreExtraProperties
data class OrderRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val orderCode: String = "",
  val shopId: String = "",
  val shopCode: String = "",
  val shopName: String = "",
  val routeId: String = "",
  val status: String = OrderStatus.CONFIRMED,
  val totalAmount: Double = 0.0,
  val itemCount: Int = 0,
  val itemsSummary: String = "",
  val orderDate: String = "",
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  @get:Exclude val isPendingSync: Boolean = false,
)

object OrderStatus {
  const val PENDING = "pending"
  const val CONFIRMED = "confirmed"
  const val DELIVERED = "delivered"
  const val REJECTED = "rejected"
}

@IgnoreExtraProperties
data class StockItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productCode: String = "",
  val productName: String = "",
  val packSize: String = "",
  val category: String = "",
  val quantity: Int = 0,
  val minThreshold: Int = 30,
  val unitPrice: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class OperationsMeta(
  @DocumentId val id: String = "",
  val userId: String = "",
  val shopRegTarget: Int = 10,
  val vehiclesToDispatch: Int = 3,
  val vehiclesOnRoad: Int = 2,
  val returnsCollected: Int = 4,
  val toCollectAmount: Double = 46800.0,
  val toCollectShops: Int = 11,
  val toSuppliersAmount: Double = 28500.0,
  val toSuppliersCount: Int = 4,
  val seeded: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class CatalogProduct(
  val code: String,
  val name: String,
  val packSize: String,
  val category: String,
  val defaultPrice: Double,
)

data class DeliveryRoute(
  val id: String,
  val name: String,
  val vehicle: String = "Tata Ace (KL-07-CD-4190)",
  val maxCrates: Int = 35,
  val driverName: String = "Field Driver",
  val driverPhone: String = "+91 98452 10980",
  val dispatchWindow: String = "06:30 AM – 10:30 AM",
  val stagingBay: String = "Bay #03 - Central Dock",
  val colorIndex: Int = 0,
  val isActive: Boolean = true,
  val stopOrderIds: List<String> = emptyList(),
)

object RoutesCatalog {
  const val ALL = "ALL ROUTES"
  const val R01_KANNUR_BANGALORE = "R01-KANNUR-BANGALORE"
  const val R02_KANNUR_KOZHIKODE = "R02-KANNUR-KOZHIKODE"
  const val R03_BANGALORE_PUNE = "R03-BANGALORE-PUNE"
  const val UNALLOCATED = "UNALLOCATED"

  val allRoutes = listOf(
    R01_KANNUR_BANGALORE,
    R02_KANNUR_KOZHIKODE,
    R03_BANGALORE_PUNE,
    UNALLOCATED,
  )

  val filterOptions = listOf(ALL) + allRoutes

  fun sortShopsByRouteSequence(routeShops: List<Shop>, route: DeliveryRoute?): List<Shop> {
    val orderIndex = route?.stopOrderIds
      ?.mapIndexed { idx, id -> id to idx }
      ?.toMap()
      .orEmpty()
    return routeShops.sortedWith(
      compareBy<Shop>(
        { orderIndex[it.id] ?: Int.MAX_VALUE },
        { it.shopCode.uppercase() },
        { it.name.lowercase() },
        { it.id },
      )
    )
  }

  val starterRoutes = listOf(
    DeliveryRoute(
      id = R01_KANNUR_BANGALORE,
      name = "Route 1: Kannur - Bangalore",
      vehicle = "Tata Ace (KL-07-CD-4190)",
      maxCrates = 32,
      driverName = "Rajan Kumar",
      driverPhone = "+91 98452 10980",
      dispatchWindow = "06:30 AM – 10:30 AM",
      stagingBay = "Bay #03 - North Dock",
      colorIndex = 0,
    ),
    DeliveryRoute(
      id = R02_KANNUR_KOZHIKODE,
      name = "Route 2: Kannur - Kozhikode",
      vehicle = "Mahindra Bolero Maxi (KL-07-CB-9081)",
      maxCrates = 40,
      driverName = "Manoj Varma",
      driverPhone = "+91 97455 67890",
      dispatchWindow = "07:15 AM – 11:30 AM",
      stagingBay = "Bay #01 - South Dock",
      colorIndex = 1,
    ),
    DeliveryRoute(
      id = R03_BANGALORE_PUNE,
      name = "Route 3: Bangalore - Pune",
      vehicle = "Ashok Leyland Dost (KL-07-CC-1120)",
      maxCrates = 30,
      driverName = "Shaji Mathew",
      driverPhone = "+91 94470 33445",
      dispatchWindow = "08:00 AM – 12:00 PM",
      stagingBay = "Bay #05 - Central Dock",
      colorIndex = 2,
    ),
  )
}

object SnackCatalog {
  val products = listOf(
    CatalogProduct("BC100", "Banana Chips", "100g", "Banana Chips", 45.0),
    CatalogProduct("BC200", "Banana Chips", "200g", "Banana Chips", 88.0),
    CatalogProduct("BC500", "Banana Chips", "500g", "Banana Chips", 210.0),
    CatalogProduct("JB100", "Jaggery Banana", "100g", "Jaggery & Sweet", 50.0),
    CatalogProduct("JB200", "Jaggery Banana", "200g", "Jaggery & Sweet", 96.0),
    CatalogProduct("JB500", "Jaggery Banana", "500g", "Jaggery & Sweet", 230.0),
    CatalogProduct("MX200", "Kerala Spicy Mixture", "200g", "Mixtures & Savories", 72.0),
    CatalogProduct("MX500", "Kerala Spicy Mixture", "500g", "Mixtures & Savories", 170.0),
    CatalogProduct("TP200", "Tapioca Chips", "200g", "Tapioca & Crackers", 65.0),
    CatalogProduct("TP500", "Tapioca Chips", "500g", "Tapioca & Crackers", 155.0),
    CatalogProduct("MR200", "Butter Murukku", "200g", "Mixtures & Savories", 68.0),
    CatalogProduct("JK200", "Jackfruit Chips", "200g", "Banana Chips", 115.0),
  )

  val categories = listOf(
    "All Snacks",
    "Banana Chips",
    "Jaggery & Sweet",
    "Mixtures & Savories",
    "Tapioca & Crackers",
  )
}

@IgnoreExtraProperties
data class ProductVariant(
  val sku: String = "",
  val packSize: String = "",
  val wholesalePrice: Double = 0.0,
  val mrp: Double = 0.0,
  val isPopular: Boolean = false,
)

object ProductStatus {
  const val ACTIVE = "active"
  const val INACTIVE = "inactive"
  const val DRAFT = "draft"
}

@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productCode: String = "",
  val sku: String = "",
  val barcode: String = "",
  val name: String = "",
  val category: String = "Biscuits & Snacks",
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
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  @get:Exclude val isPendingSync: Boolean = false,
)

object ProductCatalogHelper {
  val defaultCategories = listOf(
    "Biscuits & Snacks",
    "Banana Chips",
    "Jaggery & Sweet",
    "Mixtures & Savories",
    "Cooking Oil",
    "Rice & Grains",
    "Spices & Masala",
    "Flour & Atta",
  )

  val baseUnits = listOf(
    "Gram (g)",
    "Kilogram (kg)",
    "Litre (L)",
    "Millilitre (ml)",
    "Piece (pc)",
  )

  val sellingUnits = listOf(
    "Pouch / Packet",
    "Bag",
    "Bottle",
    "Box",
    "Carton",
  )

  fun encodeVariants(variants: List<ProductVariant>): String {
    if (variants.isEmpty()) return ""
    val arr = org.json.JSONArray()
    variants.forEach { v ->
      arr.put(
        org.json.JSONObject().apply {
          put("sku", v.sku)
          put("packSize", v.packSize)
          put("wholesalePrice", v.wholesalePrice)
          put("mrp", v.mrp)
          put("isPopular", v.isPopular)
        }
      )
    }
    return arr.toString().take(3900)
  }

  fun decodeVariants(json: String): List<ProductVariant> {
    if (json.isBlank()) return emptyList()
    return try {
      val arr = org.json.JSONArray(json)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val pack = obj.optString("packSize", "").trim()
          val sku = obj.optString("sku", "").trim()
          if (pack.isNotEmpty() || sku.isNotEmpty()) {
            add(
              ProductVariant(
                sku = sku,
                packSize = pack,
                wholesalePrice = obj.optDouble("wholesalePrice", 0.0),
                mrp = obj.optDouble("mrp", 0.0),
                isPopular = obj.optBoolean("isPopular", false),
              )
            )
          }
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun computeMarginPercent(wholesalePrice: Double, mrp: Double): Double {
    if (mrp <= 0.0 || wholesalePrice < 0.0) return 0.0
    return ((mrp - wholesalePrice) / mrp * 100.0).coerceIn(0.0, 99.9)
  }

  fun formatPriceDisplay(product: Product): String {
    val variants = if (product.hasVariants) decodeVariants(product.variantsJson) else emptyList()
    if (variants.isNotEmpty()) {
      val prices = variants.map { it.wholesalePrice }.filter { it > 0.0 }
      if (prices.isNotEmpty()) {
        val minP = prices.min()
        val maxP = prices.max()
        return if (kotlin.math.abs(maxP - minP) < 0.01) {
          String.format(java.util.Locale.US, "₹%.2f", minP)
        } else {
          String.format(java.util.Locale.US, "₹%.2f - ₹%.2f", minP, maxP)
        }
      }
    }
    return String.format(java.util.Locale.US, "₹%.2f", product.wholesalePrice)
  }

  fun isLowStock(product: Product): Boolean {
    return product.status == ProductStatus.ACTIVE && product.stockQuantity <= product.minThreshold
  }

  fun isRecentlyAdded(product: Product, nowMillis: Long = System.currentTimeMillis()): Boolean {
    val cutoff = nowMillis - 14L * 24L * 60L * 60L * 1000L
    val tsMillis = product.createdAt?.toDate()?.time
    if (tsMillis != null) {
      return tsMillis >= cutoff
    }
    if (product.addedDate.isNotBlank()) {
      return try {
        val parsed = isoDateFormatLocal.get()!!.parse(product.addedDate)
        parsed != null && parsed.time >= cutoff
      } catch (_: Exception) {
        false
      }
    }
    return false
  }

  fun unitShortLabel(sellingUnit: String): String {
    return when {
      sellingUnit.contains("Bag", ignoreCase = true) -> "bags"
      sellingUnit.contains("Bottle", ignoreCase = true) -> "btl"
      sellingUnit.contains("Box", ignoreCase = true) -> "box"
      sellingUnit.contains("Carton", ignoreCase = true) -> "ctn"
      sellingUnit.contains("Pouch", ignoreCase = true) -> "pk"
      else -> "pk"
    }
  }

  fun toCatalogProducts(
    products: List<Product>,
    fallback: List<CatalogProduct> = SnackCatalog.products,
  ): List<CatalogProduct> {
    val active = products.filter { it.status == ProductStatus.ACTIVE }
    val converted = mutableListOf<CatalogProduct>()
    val seenCodes = mutableSetOf<String>()

    active.forEach { prod ->
      val variants = if (prod.hasVariants) decodeVariants(prod.variantsJson) else emptyList()
      if (variants.isNotEmpty()) {
        variants.forEach { variant ->
          val code = variant.sku.ifBlank { prod.sku.ifBlank { prod.productCode } }.trim()
          if (code.isNotBlank() && seenCodes.add(code.uppercase())) {
            converted.add(
              CatalogProduct(
                code = code,
                name = prod.name,
                packSize = variant.packSize.ifBlank { prod.packSize.ifBlank { "Standard" } },
                category = prod.category.ifBlank { "Snacks" },
                defaultPrice = variant.wholesalePrice,
              )
            )
          }
        }
      } else {
        val code = prod.sku.ifBlank { prod.productCode }.trim()
        if (code.isNotBlank() && seenCodes.add(code.uppercase())) {
          converted.add(
            CatalogProduct(
              code = code,
              name = prod.name,
              packSize = prod.packSize.ifBlank { prod.baseUnit },
              category = prod.category.ifBlank { "Snacks" },
              defaultPrice = prod.wholesalePrice,
            )
          )
        }
      }
    }

    fallback.forEach { item ->
      if (seenCodes.add(item.code.uppercase())) {
        converted.add(item)
      }
    }
    return converted
  }

  fun starterProducts(
    userId: String,
    today: String,
    daysAgo: (Int) -> String,
  ): List<Product> {
    val bananaVariants = encodeVariants(
      listOf(
        ProductVariant("BC50", "50g Individual Pack", 22.0, 25.0, false),
        ProductVariant("BC100", "100g Retail Pack", 42.0, 50.0, true),
        ProductVariant("BC200", "200g Stand-up Pouch", 80.0, 95.0, false),
        ProductVariant("BC500", "500g Family Value Box", 190.0, 220.0, false),
      )
    )
    val mixtureVariants = encodeVariants(
      listOf(
        ProductVariant("MX200", "200g Pouch", 45.0, 55.0, false),
        ProductVariant("MX500", "500g Family Pack", 72.0, 88.0, true),
      )
    )
    return listOf(
      Product(
        id = "seed_prd_1",
        userId = userId,
        productCode = "PRD-00101",
        sku = "SKU-BNC-001",
        barcode = "8901234567890",
        name = "Kerala Special Banana Chips",
        category = "Banana Chips",
        brand = "Homely Gold",
        baseUnit = "Gram (g)",
        sellingUnit = "Pouch / Packet",
        casePack = "24 packets per outer corrugated master carton",
        packSize = "50g - 500g",
        wholesalePrice = 22.0,
        mrp = 50.0,
        stockQuantity = 410,
        minThreshold = 60,
        notes = "Fresh Kerala Nendran banana chips fried in pure wood-pressed coconut oil with mild rock salt",
        status = ProductStatus.ACTIVE,
        salesVelocity = 95,
        hasVariants = true,
        variantsJson = bananaVariants,
        addedDate = daysAgo(3),
      ),
      Product(
        id = "seed_prd_2",
        userId = userId,
        productCode = "PRD-00102",
        sku = "SKU-CNO-002",
        barcode = "8901234567891",
        name = "Pure Coconut Oil 1L Pouch",
        category = "Cooking Oil",
        brand = "Kera Pure",
        baseUnit = "Litre (L)",
        sellingUnit = "Pouch / Packet",
        casePack = "12 pouches per carton",
        packSize = "1L Pouch",
        wholesalePrice = 165.0,
        mrp = 195.0,
        stockQuantity = 15,
        minThreshold = 40,
        notes = "Cold-filtered sulphur-free Kerala coconut cooking oil pouch",
        status = ProductStatus.ACTIVE,
        salesVelocity = 82,
        hasVariants = false,
        addedDate = daysAgo(5),
      ),
      Product(
        id = "seed_prd_3",
        userId = userId,
        productCode = "PRD-00103",
        sku = "SKU-MTR-005",
        barcode = "8901234567892",
        name = "Palakkadan Matta Rice 5kg Bag",
        category = "Rice & Grains",
        brand = "Farm Harvest",
        baseUnit = "Kilogram (kg)",
        sellingUnit = "Bag",
        casePack = "5 bags per woven bale",
        packSize = "5kg Bag",
        wholesalePrice = 280.0,
        mrp = 320.0,
        stockQuantity = 8,
        minThreshold = 30,
        notes = "Single-boiled bold grain Kerala Palakkadan Matta red rice",
        status = ProductStatus.ACTIVE,
        salesVelocity = 76,
        hasVariants = false,
        addedDate = daysAgo(10),
      ),
      Product(
        id = "seed_prd_4",
        userId = userId,
        productCode = "PRD-00104",
        sku = "SKU-MIX-004",
        barcode = "8901234567893",
        name = "Malabar Spicy Mixture",
        category = "Mixtures & Savories",
        brand = "Homely Gold",
        baseUnit = "Gram (g)",
        sellingUnit = "Pouch / Packet",
        casePack = "20 pouches per box",
        packSize = "200g - 500g",
        wholesalePrice = 45.0,
        mrp = 88.0,
        stockQuantity = 140,
        minThreshold = 35,
        notes = "Crispy boondi, curry leaves, roasted peanuts and sev blend",
        status = ProductStatus.ACTIVE,
        salesVelocity = 88,
        hasVariants = true,
        variantsJson = mixtureVariants,
        addedDate = today,
      ),
      Product(
        id = "seed_prd_5",
        userId = userId,
        productCode = "PRD-00105",
        sku = "SKU-PEP-012",
        barcode = "8901234567894",
        name = "Premium Malabar Black Pepper",
        category = "Spices & Masala",
        brand = "Wayanad Choice",
        baseUnit = "Gram (g)",
        sellingUnit = "Pouch / Packet",
        casePack = "10 pouches per display box",
        packSize = "500g",
        wholesalePrice = 380.0,
        mrp = 440.0,
        stockQuantity = 85,
        minThreshold = 20,
        notes = "Bold TGSEB grade whole Wayanad black peppercorns",
        status = ProductStatus.ACTIVE,
        salesVelocity = 64,
        hasVariants = false,
        addedDate = daysAgo(20),
      ),
      Product(
        id = "seed_prd_6",
        userId = userId,
        productCode = "PRD-00106",
        sku = "SKU-FLR-008",
        barcode = "8901234567895",
        name = "Roasted Rice Flour (Puttu Podi)",
        category = "Flour & Atta",
        brand = "Brahmin's Kitchen",
        baseUnit = "Kilogram (kg)",
        sellingUnit = "Pouch / Packet",
        casePack = "20 packets per sack",
        packSize = "1kg",
        wholesalePrice = 58.0,
        mrp = 72.0,
        stockQuantity = 0,
        minThreshold = 30,
        notes = "Steam-roasted traditional white puttu podi flour",
        status = ProductStatus.INACTIVE,
        salesVelocity = 42,
        hasVariants = false,
        addedDate = daysAgo(25),
      ),
    )
  }
}

object PaymentDirection {
  const val COLLECT = "collect"
  const val PAY_SUPPLIER = "pay_supplier"
}

object PaymentMethod {
  const val CASH = "cash"
  const val UPI = "upi"
  const val BANK = "bank"
  const val CHEQUE = "cheque"

  val allMethods = listOf(CASH, UPI, BANK, CHEQUE)

  fun displayLabel(method: String): String = when (method) {
    CASH -> "Cash"
    UPI -> "UPI"
    BANK -> "Bank Trf"
    CHEQUE -> "Cheque"
    else -> method.uppercase()
  }
}

object SupplierBillStatus {
  const val PENDING = "pending"
  const val OVERDUE = "overdue"
  const val SETTLED = "settled"
}

@IgnoreExtraProperties
data class PaymentTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val receiptCode: String = "",
  val direction: String = PaymentDirection.COLLECT,
  val partyId: String = "",
  val partyCode: String = "",
  val partyName: String = "",
  val contactName: String = "",
  val phone: String = "",
  val invoiceRef: String = "",
  val amount: Double = 0.0,
  val paymentMethod: String = PaymentMethod.UPI,
  val referenceNumber: String = "",
  val paymentDate: String = "",
  val recordedBy: String = "",
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  @get:Exclude val isPendingSync: Boolean = false,
)

@IgnoreExtraProperties
data class SupplierBill(
  @DocumentId val id: String = "",
  val userId: String = "",
  val supplierCode: String = "",
  val supplierName: String = "",
  val contactPerson: String = "",
  val phone: String = "",
  val location: String = "",
  val category: String = "Raw Materials",
  val billNumber: String = "",
  val totalBilled: Double = 0.0,
  val paidAmount: Double = 0.0,
  val billDate: String = "",
  val dueDate: String = "",
  val status: String = SupplierBillStatus.PENDING,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  @get:Exclude val isPendingSync: Boolean = false,
) {
  val outstandingAmount: Double
    get() = (totalBilled - paidAmount).coerceAtLeast(0.0)
}

data class ShopLedgerSummary(
  val shop: Shop,
  val invoicedTotal: Double,
  val paidTotal: Double,
  val outstandingBalance: Double,
  val overdueAmount: Double,
  val maxDaysOverdue: Int,
  val pendingOrderCount: Int,
  val unpaidOrders: List<OrderRecord>,
  val recentPayment: PaymentTransaction?,
  val isSettled: Boolean,
  val statusLabel: String,
)

data class AgingBucketSummary(
  val label: String,
  val subtitle: String,
  val amount: Double,
  val accountCount: Int,
)

object PaymentLedgerHelper {

  fun computeShopLedgers(
    shops: List<Shop>,
    orders: List<OrderRecord>,
    payments: List<PaymentTransaction>,
    todayMillis: Long = System.currentTimeMillis(),
  ): List<ShopLedgerSummary> {
    return shops
      .filter { it.status != ShopStatus.DELETED }
      .map { shop ->
        val shopOrders = orders
          .filter { ord ->
            ord.status != OrderStatus.REJECTED &&
              (ord.shopId == shop.id || (shop.shopCode.isNotBlank() && ord.shopCode == shop.shopCode))
          }
          .sortedBy { it.orderDate }

        val invoiced = shopOrders.sumOf { it.totalAmount.coerceAtLeast(0.0) }
        val shopPayments = payments
          .filter { pay ->
            pay.direction == PaymentDirection.COLLECT &&
              (pay.partyId == shop.id || (shop.shopCode.isNotBlank() && pay.partyCode == shop.shopCode))
          }
          .sortedWith(
            compareByDescending<PaymentTransaction> { it.paymentDate }
              .thenByDescending { it.createdAt?.seconds ?: 0L }
          )

        val paid = shopPayments.sumOf { it.amount.coerceAtLeast(0.0) }
        val outstanding = (invoiced - paid).coerceAtLeast(0.0)

        var remainingCredit = paid
        val unpaidList = mutableListOf<OrderRecord>()
        var overdueSum = 0.0
        var maxOverdueDays = 0

        for (ord in shopOrders) {
          val orderAmt = ord.totalAmount.coerceAtLeast(0.0)
          if (remainingCredit >= orderAmt - 0.01) {
            remainingCredit = (remainingCredit - orderAmt).coerceAtLeast(0.0)
          } else {
            val unpaidPart = (orderAmt - remainingCredit).coerceAtLeast(0.0)
            remainingCredit = 0.0
            unpaidList.add(ord)
            val ageDays = daysBetweenIsoDate(ord.orderDate, todayMillis)
            val isDeliveredOverdue = ord.status == OrderStatus.DELIVERED && ageDays >= 1
            val isAgedOverdue = ageDays >= 3
            if (isDeliveredOverdue || isAgedOverdue) {
              overdueSum += unpaidPart
              val overdueDays = if (ord.status == OrderStatus.DELIVERED) ageDays.coerceAtLeast(1) else (ageDays - 2).coerceAtLeast(1)
              if (overdueDays > maxOverdueDays) maxOverdueDays = overdueDays
            }
          }
        }

        val settled = invoiced > 0.0 && outstanding <= 0.5
        val statusLabel = when {
          invoiced <= 0.0 -> "No Orders Yet"
          settled -> "Fully Settled"
          maxOverdueDays > 0 -> "Overdue ($maxOverdueDays ${if (maxOverdueDays == 1) "Day" else "Days"})"
          else -> "Payment Pending"
        }

        ShopLedgerSummary(
          shop = shop,
          invoicedTotal = invoiced,
          paidTotal = paid,
          outstandingBalance = outstanding,
          overdueAmount = overdueSum,
          maxDaysOverdue = maxOverdueDays,
          pendingOrderCount = unpaidList.size,
          unpaidOrders = unpaidList,
          recentPayment = shopPayments.firstOrNull(),
          isSettled = settled,
          statusLabel = statusLabel,
        )
      }
  }

  fun computeSupplierDaysOverdue(
    bill: SupplierBill,
    todayMillis: Long = System.currentTimeMillis(),
  ): Int {
    if (bill.outstandingAmount <= 0.5 || bill.status == SupplierBillStatus.SETTLED) return 0
    val dueMillis = parseIsoDateMillis(bill.dueDate) ?: return if (bill.status == SupplierBillStatus.OVERDUE) 1 else 0
    val diffDays = ((todayMillis - dueMillis) / DAY_MILLIS).toInt()
    return if (diffDays > 0) diffDays else 0
  }

  fun effectiveSupplierStatus(
    bill: SupplierBill,
    todayMillis: Long = System.currentTimeMillis(),
  ): String {
    if (bill.outstandingAmount <= 0.5) return SupplierBillStatus.SETTLED
    return if (computeSupplierDaysOverdue(bill, todayMillis) > 0 || bill.status == SupplierBillStatus.OVERDUE) {
      SupplierBillStatus.OVERDUE
    } else {
      SupplierBillStatus.PENDING
    }
  }

  fun computeShopAgingBuckets(
    ledgers: List<ShopLedgerSummary>,
    todayMillis: Long = System.currentTimeMillis(),
  ): List<AgingBucketSummary> {
    var b0to7 = 0.0
    var c0to7 = 0
    var b8to15 = 0.0
    var c8to15 = 0
    var b16to30 = 0.0
    var c16to30 = 0
    var b30plus = 0.0
    var c30plus = 0

    ledgers.filter { it.outstandingBalance > 0.5 }.forEach { entry ->
      val oldestAge = entry.unpaidOrders.firstOrNull()?.let { daysBetweenIsoDate(it.orderDate, todayMillis) } ?: 0
      when {
        oldestAge <= 7 -> {
          b0to7 += entry.outstandingBalance
          c0to7++
        }
        oldestAge <= 15 -> {
          b8to15 += entry.outstandingBalance
          c8to15++
        }
        oldestAge <= 30 -> {
          b16to30 += entry.outstandingBalance
          c16to30++
        }
        else -> {
          b30plus += entry.outstandingBalance
          c30plus++
        }
      }
    }
    return listOf(
      AgingBucketSummary("0 – 7 Days", "Current cycle dues", b0to7, c0to7),
      AgingBucketSummary("8 – 15 Days", "Follow-up recommended", b8to15, c8to15),
      AgingBucketSummary("16 – 30 Days", "Escalate reminder", b16to30, c16to30),
      AgingBucketSummary("30+ Days", "Critical recovery", b30plus, c30plus),
    )
  }

  fun computeSupplierAgingBuckets(
    bills: List<SupplierBill>,
    todayMillis: Long = System.currentTimeMillis(),
  ): List<AgingBucketSummary> {
    var currentDue = 0.0
    var currentCount = 0
    var overdue1to7 = 0.0
    var overdue1to7Count = 0
    var overdue8to15 = 0.0
    var overdue8to15Count = 0
    var overdue15Plus = 0.0
    var overdue15PlusCount = 0

    bills.filter { it.outstandingAmount > 0.5 }.forEach { bill ->
      val overdueDays = computeSupplierDaysOverdue(bill, todayMillis)
      when {
        overdueDays <= 0 -> {
          currentDue += bill.outstandingAmount
          currentCount++
        }
        overdueDays <= 7 -> {
          overdue1to7 += bill.outstandingAmount
          overdue1to7Count++
        }
        overdueDays <= 15 -> {
          overdue8to15 += bill.outstandingAmount
          overdue8to15Count++
        }
        else -> {
          overdue15Plus += bill.outstandingAmount
          overdue15PlusCount++
        }
      }
    }
    return listOf(
      AgingBucketSummary("Upcoming / Current", "Not yet past due date", currentDue, currentCount),
      AgingBucketSummary("1 – 7 Days Overdue", "Schedule supplier transfer", overdue1to7, overdue1to7Count),
      AgingBucketSummary("8 – 15 Days Overdue", "Priority settlement", overdue8to15, overdue8to15Count),
      AgingBucketSummary("15+ Days Overdue", "Immediate payout required", overdue15Plus, overdue15PlusCount),
    )
  }

  fun starterSupplierBills(
    userId: String,
    today: String,
    daysOffset: (Int) -> String,
  ): List<SupplierBill> {
    return listOf(
      SupplierBill(
        id = "seed_sup_1",
        userId = userId,
        supplierCode = "SUP-001",
        supplierName = "Kera Pure Coconut Mills",
        contactPerson = "Ramesh Menon",
        phone = "9846077123",
        location = "Edappally, Kochi",
        category = "Cooking Oil",
        billNumber = "BILL-402",
        totalBilled = 18500.0,
        paidAmount = 6500.0,
        billDate = daysOffset(-10),
        dueDate = daysOffset(-3),
        status = SupplierBillStatus.OVERDUE,
        notes = "Wood-pressed coconut oil 200L batch",
      ),
      SupplierBill(
        id = "seed_sup_2",
        userId = userId,
        supplierCode = "SUP-002",
        supplierName = "Palakkad Nendran Agro Farms",
        contactPerson = "Vijayan K.",
        phone = "9447188234",
        location = "Alathur, Palakkad",
        category = "Raw Materials",
        billNumber = "BILL-409",
        totalBilled = 14200.0,
        paidAmount = 4200.0,
        billDate = daysOffset(-4),
        dueDate = daysOffset(4),
        status = SupplierBillStatus.PENDING,
        notes = "Grade-A green Nendran banana crates (450 kg)",
      ),
      SupplierBill(
        id = "seed_sup_3",
        userId = userId,
        supplierCode = "SUP-003",
        supplierName = "Cochin Corrugated Packagers",
        contactPerson = "Priya Thomas",
        phone = "9895244110",
        location = "Kalamassery Industrial Estate",
        category = "Packaging",
        billNumber = "BILL-415",
        totalBilled = 8500.0,
        paidAmount = 2000.0,
        billDate = daysOffset(-2),
        dueDate = daysOffset(6),
        status = SupplierBillStatus.PENDING,
        notes = "Laminated foil pouches & 5-ply master cartons",
      ),
      SupplierBill(
        id = "seed_sup_4",
        userId = userId,
        supplierCode = "SUP-004",
        supplierName = "Wayanad Spice Growers Co-op",
        contactPerson = "Joseph P.",
        phone = "9745019283",
        location = "Kalpetta, Wayanad",
        category = "Spices & Masala",
        billNumber = "BILL-398",
        totalBilled = 11800.0,
        paidAmount = 11800.0,
        billDate = daysOffset(-14),
        dueDate = daysOffset(-4),
        status = SupplierBillStatus.SETTLED,
        notes = "Black pepper, chilli & turmeric bulk supply",
      ),
    )
  }

  fun starterPayments(
    userId: String,
    today: String,
    yesterday: String,
  ): List<PaymentTransaction> {
    return listOf(
      PaymentTransaction(
        id = "seed_pay_1",
        userId = userId,
        receiptCode = "RCPT-2026-9901",
        direction = PaymentDirection.COLLECT,
        partyId = "local_shop_1",
        partyCode = "SHP-101",
        partyName = "Malabar Heritage Bakery",
        contactName = "Suresh Nair",
        phone = "9847012345",
        invoiceRef = "ORD-2040",
        amount = 3500.0,
        paymentMethod = PaymentMethod.UPI,
        referenceNumber = "UPI/AXIS/4092188492",
        paymentDate = today,
        recordedBy = "Field Officer",
        note = "Partial UPI collection at counter",
      ),
      PaymentTransaction(
        id = "seed_pay_2",
        userId = userId,
        receiptCode = "RCPT-2026-9902",
        direction = PaymentDirection.COLLECT,
        partyId = "local_shop_4",
        partyCode = "SHP-104",
        partyName = "Deccan Fresh Corner",
        contactName = "Anand Kulkarni",
        phone = "9822045678",
        invoiceRef = "ORD-2035",
        amount = 4000.0,
        paymentMethod = PaymentMethod.CASH,
        referenceNumber = "CASH-VAN-03",
        paymentDate = yesterday,
        recordedBy = "Field Officer",
        note = "Initial cash advance on delivery",
      ),
      PaymentTransaction(
        id = "seed_pay_3",
        userId = userId,
        receiptCode = "PAYOUT-2026-4401",
        direction = PaymentDirection.PAY_SUPPLIER,
        partyId = "seed_sup_1",
        partyCode = "SUP-001",
        partyName = "Kera Pure Coconut Mills",
        contactName = "Ramesh Menon",
        phone = "9846077123",
        invoiceRef = "BILL-402",
        amount = 6500.0,
        paymentMethod = PaymentMethod.BANK,
        referenceNumber = "NEFT/SBI/88310294",
        paymentDate = today,
        recordedBy = "Hub Accounts",
        note = "Part settlement for coconut oil tanker batch",
      ),
    )
  }

  private fun parseIsoDateMillis(dateStr: String): Long? {
    val clean = dateStr.trim()
    if (clean.isBlank()) return null
    return try {
      isoDateFormatLocal.get()!!.parse(clean)?.time
    } catch (_: Exception) {
      null
    }
  }

  fun daysBetweenIsoDate(dateStr: String, todayMillis: Long = System.currentTimeMillis()): Int {
    val parsed = parseIsoDateMillis(dateStr) ?: return 0
    val diff = ((todayMillis - parsed) / DAY_MILLIS).toInt()
    return diff.coerceAtLeast(0)
  }

  private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
}

object SupplierStatus {
  const val ACTIVE = "active"
  const val INACTIVE = "inactive"
  const val DELETED = "deleted"
}

@IgnoreExtraProperties
data class Supplier(
  @DocumentId val id: String = "",
  val userId: String = "",
  val supplierCode: String = "",
  val name: String = "",
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
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
  @get:Exclude val isPendingSync: Boolean = false,
)

object SupplierCatalogHelper {
  val defaultCategories = listOf(
    "Wholesale Staples & Grains",
    "Fresh Produce & Dairy",
    "FMCG & Packaged Goods",
    "Edible Oils & Spices",
    "Raw Materials",
    "Packaging",
  )

  val creditTermOptions = listOf(
    "Immediate / Cash",
    "Net 7 Days",
    "Net 15 Days",
    "Net 30 Days",
    "Net 45 Days",
  )

  fun isRecentlyAdded(
    supplier: Supplier,
    nowMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    val cutoff = nowMillis - 14L * 24L * 60L * 60L * 1000L
    val tsMillis = supplier.createdAt?.toDate()?.time
    if (tsMillis != null && supplier.addedDate.isBlank()) {
      return tsMillis >= cutoff
    }
    if (supplier.addedDate.isNotBlank()) {
      return try {
        val parsed = isoDateFormatLocal.get()!!.parse(supplier.addedDate)
        parsed != null && parsed.time >= cutoff
      } catch (_: Exception) {
        false
      }
    }
    return tsMillis != null && tsMillis >= cutoff
  }

  fun isRecentlyUpdated(
    supplier: Supplier,
    nowMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    val cutoff = nowMillis - 7L * 24L * 60L * 60L * 1000L
    val tsMillis = supplier.updatedAt?.toDate()?.time ?: return false
    return tsMillis >= cutoff
  }

  fun hasMissingDetails(supplier: Supplier): Boolean {
    return supplier.email.isBlank() ||
      supplier.gstin.isBlank() ||
      supplier.address.isBlank() ||
      supplier.pinCode.isBlank() ||
      supplier.phone.filter { it.isDigit() }.length < 10
  }

  fun formatPhoneDisplay(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    return when {
      digits.length == 10 -> "+91 ${digits.substring(0, 5)} ${digits.substring(5)}"
      digits.length == 12 && digits.startsWith("91") -> "+91 ${digits.substring(2, 7)} ${digits.substring(7)}"
      rawPhone.isNotBlank() -> rawPhone
      else -> "No phone"
    }
  }

  fun starterSuppliers(
    userId: String,
    today: String,
    daysAgo: (Int) -> String,
  ): List<Supplier> {
    return listOf(
      Supplier(
        id = "seed_sup_dir_1",
        userId = userId,
        supplierCode = "SUP-000044",
        name = "Malabar Wholesale Distributors",
        category = "Wholesale Staples & Grains",
        contactPerson = "Mohammed Shafi",
        contactRole = "Key Account Mgr",
        altContactPerson = "Faizal Rahman (Logistics)",
        phone = "9876554321",
        altPhone = "9847054321",
        email = "sales@malabargrocery.com",
        address = "Plot 45, Industrial Estate, Kalamassery, Ernakulam, Kerala",
        pinCode = "683104",
        mapsLink = "https://maps.google.com/?q=10.0518,76.3197",
        latitude = 10.0518,
        longitude = 76.3197,
        gstin = "32AABCM4421K1Z5",
        kycVerified = true,
        creditTerms = "Net 15 Days",
        confirmedPriceNotes = "Matta Rice 50kg: ₹2,450 • Whole Wheat: ₹1,820",
        notes = "Primary central hub wholesale staples & bulk rice partner",
        status = SupplierStatus.ACTIVE,
        addedDate = daysAgo(20),
        addedBy = "Arun Sharma",
      ),
      Supplier(
        id = "seed_sup_dir_2",
        userId = userId,
        supplierCode = "SUP-000045",
        name = "Kerala Fresh Foods",
        category = "Fresh Produce & Dairy",
        contactPerson = "Arun Menon",
        contactRole = "Partner",
        phone = "9876554322",
        email = "contact@keralafreshfoods.in",
        address = "Aluva, Ernakulam, Kerala",
        pinCode = "683101",
        mapsLink = "https://maps.google.com/?q=10.1004,76.3570",
        latitude = 10.1004,
        longitude = 76.3570,
        gstin = "32AABCK8832M1Z2",
        kycVerified = true,
        creditTerms = "Net 7 Days",
        confirmedPriceNotes = "Green Nendran Grade-A: ₹58/kg",
        notes = "Daily early-morning farm fresh dispatch to Kalamassery hub",
        status = SupplierStatus.ACTIVE,
        addedDate = daysAgo(18),
        addedBy = "Arun Sharma",
      ),
      Supplier(
        id = "seed_sup_dir_3",
        userId = userId,
        supplierCode = "SUP-000046",
        name = "South India Grocery Supplies",
        category = "FMCG & Packaged Goods",
        contactPerson = "Suresh Kumar",
        contactRole = "Sales Director",
        phone = "9876554323",
        email = "info@southindiagrocery.in",
        address = "Thrissur, Kerala",
        pinCode = "680001",
        mapsLink = "https://maps.google.com/?q=10.5276,76.2144",
        latitude = 10.5276,
        longitude = 76.2144,
        gstin = "32AABCS9910R1Z9",
        kycVerified = true,
        creditTerms = "Net 30 Days",
        confirmedPriceNotes = "Carton Packaging & FMCG assorted bulk slab 6% discount",
        notes = "Weekly Thrissur consolidated lorry load",
        status = SupplierStatus.ACTIVE,
        addedDate = daysAgo(12),
        addedBy = "Hub Procurement",
      ),
      Supplier(
        id = "seed_sup_dir_4",
        userId = userId,
        supplierCode = "SUP-000047",
        name = "Cochin Spices & Oil Mill",
        category = "Edible Oils & Spices",
        contactPerson = "K. R. Jayesh",
        contactRole = "Managing Partner",
        phone = "9847033411",
        email = "orders@cochinspices.com",
        address = "Mattancherry, Kochi, Kerala",
        pinCode = "682002",
        mapsLink = "https://maps.google.com/?q=9.9579,76.2597",
        latitude = 9.9579,
        longitude = 76.2597,
        gstin = "32AABCC2210P1Z8",
        kycVerified = true,
        creditTerms = "Net 15 Days",
        confirmedPriceNotes = "Sulphur-Free Coconut Oil: ₹162/L • Wayanad Pepper: ₹540/kg",
        notes = "Cold-filtered coconut oil drums and whole spice sacks",
        status = SupplierStatus.ACTIVE,
        addedDate = today,
        addedBy = "Arun Sharma",
      ),
    )
  }
}


