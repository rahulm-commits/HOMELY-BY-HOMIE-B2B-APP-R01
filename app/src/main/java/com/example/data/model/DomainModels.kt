package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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
