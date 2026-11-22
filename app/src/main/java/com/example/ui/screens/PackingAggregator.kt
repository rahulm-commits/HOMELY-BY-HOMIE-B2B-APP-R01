package com.example.ui.screens

import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.SnackCatalog
import com.example.data.model.StockItem
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

data class ParsedOrderLineItem(
  val productCode: String,
  val productName: String,
  val packSize: String,
  val category: String,
  val unitPrice: Double,
  val quantity: Int,
)

data class PacketSizeTarget(
  val packSize: String,
  val orderedPacks: Int,
  val loadedPacks: Int,
  val warehouseStockPacks: Int,
  val toPreparePacks: Int,
  val skuCount: Int,
)

data class PackingSkuLine(
  val productCode: String,
  val productName: String,
  val packSize: String,
  val category: String,
  val orderedPacks: Int,
  val loadedPacks: Int,
  val warehouseStockPacks: Int,
  val allocatedDropCount: Int,
) {
  val shortagePacks: Int get() = (orderedPacks - loadedPacks).coerceAtLeast(0)
  val isComplete: Boolean get() = orderedPacks > 0 && loadedPacks >= orderedPacks
  val progressFraction: Float
    get() = if (orderedPacks <= 0) 1f else (loadedPacks.toFloat() / orderedPacks.toFloat()).coerceIn(0f, 1f)
}

data class PackingCategoryGroup(
  val categoryName: String,
  val items: List<PackingSkuLine>,
) {
  val totalOrderedPacks: Int get() = items.sumOf { it.orderedPacks }
  val totalLoadedPacks: Int get() = items.sumOf { it.loadedPacks }
  val completionPercent: Int
    get() = if (totalOrderedPacks <= 0) 100 else ((totalLoadedPacks * 100f) / totalOrderedPacks).roundToInt().coerceIn(0, 100)
}

data class ShopPackingSlip(
  val stopNumber: Int,
  val deliveryStopNumber: Int,
  val totalRouteStops: Int,
  val packOrderNumber: Int,
  val packageLabel: String,
  val order: OrderRecord,
  val shop: Shop?,
  val route: DeliveryRoute?,
  val isAssignedToRoute: Boolean,
  val items: List<ParsedOrderLineItem>,
  val crateLabel: String,
  val isPacked: Boolean,
  val isInProgress: Boolean,
)

data class PackingDashboardSnapshot(
  val selectedRoute: DeliveryRoute?,
  val routeFilteredOrders: List<OrderRecord>,
  val reverseLoadOrder: Boolean,
  val assignedOrdersCount: Int,
  val assignedPacksCount: Int,
  val unassignedOrdersCount: Int,
  val unassignedPacksCount: Int,
  val productionBySize: List<PacketSizeTarget>,
  val categoryGroups: List<PackingCategoryGroup>,
  val shopSlips: List<ShopPackingSlip>,
  val totalOrderedPacks: Int,
  val totalLoadedPacks: Int,
  val payloadWeightKg: Double,
  val maxPayloadKg: Double,
  val payloadPercent: Int,
  val stagedCratesCount: Int,
  val shopsPackedCount: Int,
  val totalShopsCount: Int,
  val shortageItems: List<PackingSkuLine>,
)

private val LINE_ITEM_REGEX = Regex("""([A-Za-z0-9_-]+)\s*[xX×]\s*(\d+)""")
private val PACK_GRAMS_REGEX = Regex("""(\d+)""")
private val CATALOG_BY_CODE: Map<String, CatalogProduct> by lazy {
  SnackCatalog.products.associateBy { it.code.uppercase() }
}
private val CATALOG_ORDER: Map<String, Int> by lazy {
  SnackCatalog.products.mapIndexed { idx, p -> p.code.uppercase() to idx }.toMap()
}

fun parseOrderLineItems(itemsSummary: String): List<ParsedOrderLineItem> {
  if (itemsSummary.isBlank()) return emptyList()
  return itemsSummary.split(",").mapNotNull { rawToken ->
    val token = rawToken.trim()
    if (token.isEmpty()) return@mapNotNull null
    val match = LINE_ITEM_REGEX.find(token)
    if (match != null) {
      val code = match.groupValues[1].trim().uppercase()
      val qty = match.groupValues[2].toIntOrNull() ?: 0
      if (qty <= 0) return@mapNotNull null
      val product = CATALOG_BY_CODE[code]
      if (product != null) {
        ParsedOrderLineItem(
          productCode = product.code,
          productName = product.name,
          packSize = product.packSize,
          category = product.category,
          unitPrice = product.defaultPrice,
          quantity = qty,
        )
      } else {
        ParsedOrderLineItem(
          productCode = code,
          productName = code,
          packSize = "Standard",
          category = "Other Snacks",
          unitPrice = 0.0,
          quantity = qty,
        )
      }
    } else {
      null
    }
  }
}

private fun estimatePackWeightKg(packSize: String): Double {
  val lower = packSize.lowercase()
  val value = PACK_GRAMS_REGEX.find(lower)?.groupValues?.get(1)?.toDoubleOrNull() ?: 200.0
  return if (lower.contains("kg")) value else value / 1000.0
}

private fun packSizeSortKey(packSize: String): Int {
  val lower = packSize.lowercase()
  val value = PACK_GRAMS_REGEX.find(lower)?.groupValues?.get(1)?.toIntOrNull() ?: 9999
  return if (lower.contains("kg")) value * 1000 else value
}

fun buildPackingSnapshot(
  selectedRouteId: String,
  shops: List<Shop>,
  orders: List<OrderRecord>,
  routes: List<DeliveryRoute>,
  stocks: List<StockItem>,
  loadedOverrides: Map<String, Int> = emptyMap(),
  packedOrderOverrides: Map<String, Boolean> = emptyMap(),
  searchQuery: String = "",
  reverseLoadOrder: Boolean = true,
): PackingDashboardSnapshot {
  val routeMap = routes.associateBy { it.id }
  val routeIndexMap = routes.mapIndexed { idx, r -> r.id to idx }.toMap()
  val shopMap = shops.associateBy { it.id }
  val stockByCode = stocks.associateBy { it.productCode.uppercase() }

  // Pre-compute canonical 1..N delivery stop numbering for every route based on Route stopOrderIds
  val routeStopMetaByRouteAndShop = mutableMapOf<Pair<String, String>, Pair<Int, Int>>()
  val routeTotalStops = mutableMapOf<String, Int>()
  routes.forEach { route ->
    val sortedRouteShops = RoutesCatalog.sortShopsByRouteSequence(
      shops.filter { it.routeId == route.id },
      route,
    )
    routeTotalStops[route.id] = sortedRouteShops.size
    sortedRouteShops.forEachIndexed { idx, shop ->
      routeStopMetaByRouteAndShop[route.id to shop.id] = (idx + 1) to sortedRouteShops.size
    }
  }

  // Exclude rejected orders from dispatch & packing load
  val activeOrders = orders.filter { it.status != OrderStatus.REJECTED }

  val unassignedAllOrders = activeOrders.filter { ord ->
    val effectiveRouteId = ord.routeId.ifBlank { shopMap[ord.shopId]?.routeId.orEmpty() }
    effectiveRouteId.isBlank() || effectiveRouteId == RoutesCatalog.UNALLOCATED || !routeMap.containsKey(effectiveRouteId)
  }
  val assignedAllOrders = activeOrders.filter { ord ->
    val effectiveRouteId = ord.routeId.ifBlank { shopMap[ord.shopId]?.routeId.orEmpty() }
    effectiveRouteId.isNotBlank() && effectiveRouteId != RoutesCatalog.UNALLOCATED && routeMap.containsKey(effectiveRouteId)
  }

  val routeFilteredOrders = when (selectedRouteId) {
    RoutesCatalog.ALL -> activeOrders
    RoutesCatalog.UNALLOCATED -> unassignedAllOrders
    else -> activeOrders.filter { ord ->
      val effectiveRouteId = ord.routeId.ifBlank { shopMap[ord.shopId]?.routeId.orEmpty() }
      effectiveRouteId == selectedRouteId
    }
  }

  val selectedRoute = routeMap[selectedRouteId]

  data class RawSlipSeed(
    val order: OrderRecord,
    val shop: Shop?,
    val assignedRoute: DeliveryRoute?,
    val routeOrderIdx: Int,
    val deliveryStopNumber: Int,
    val totalRouteStops: Int,
  )

  // Group filtered orders to resolve fallback stop numbering if an order references a shop not in the route's shop list
  val fallbackCountsByRoute = mutableMapOf<String, Int>()
  val rawSeeds = routeFilteredOrders.map { order ->
    val shop = shopMap[order.shopId]
    val effectiveRouteId = order.routeId.ifBlank { shop?.routeId.orEmpty() }
    val assignedRoute = routeMap[effectiveRouteId]
    val routeIdx = routeIndexMap[effectiveRouteId] ?: Int.MAX_VALUE
    val mappedMeta = if (assignedRoute != null) {
      routeStopMetaByRouteAndShop[assignedRoute.id to order.shopId]
    } else null

    val deliveryStop = if (assignedRoute != null) {
      mappedMeta?.first ?: run {
        val next = (routeTotalStops[assignedRoute.id] ?: 0) + (fallbackCountsByRoute[assignedRoute.id] ?: 0) + 1
        fallbackCountsByRoute[assignedRoute.id] = (fallbackCountsByRoute[assignedRoute.id] ?: 0) + 1
        next
      }
    } else {
      0
    }
    val totalStops = if (assignedRoute != null) {
      maxOf(mappedMeta?.second ?: 0, deliveryStop)
    } else {
      0
    }
    RawSlipSeed(
      order = order,
      shop = shop,
      assignedRoute = assignedRoute,
      routeOrderIdx = routeIdx,
      deliveryStopNumber = deliveryStop,
      totalRouteStops = totalStops,
    )
  }

  val sortedSeeds = rawSeeds.sortedWith(
    compareBy<RawSlipSeed>(
      { if (it.assignedRoute != null) 0 else 1 },
      { it.routeOrderIdx },
      { if (reverseLoadOrder) -it.deliveryStopNumber else it.deliveryStopNumber },
      { it.order.shopCode.uppercase() },
      { it.order.orderCode.uppercase() },
      { it.order.id },
    )
  )

  val shopSlips = sortedSeeds.mapIndexed { idx, seed ->
    val order = seed.order
    val packOrderNumber = idx + 1
    val parsedItems = parseOrderLineItems(order.itemsSummary)
    val defaultPacked = order.status == OrderStatus.DELIVERED
    val isPacked = packedOrderOverrides[order.id] ?: defaultPacked
    val isInProgress = !isPacked && order.status == OrderStatus.CONFIRMED
    val crateNumber = 100 + packOrderNumber
    ShopPackingSlip(
      stopNumber = if (seed.deliveryStopNumber > 0) seed.deliveryStopNumber else packOrderNumber,
      deliveryStopNumber = seed.deliveryStopNumber,
      totalRouteStops = seed.totalRouteStops,
      packOrderNumber = packOrderNumber,
      packageLabel = "Pkg #$packOrderNumber",
      order = order,
      shop = seed.shop,
      route = seed.assignedRoute,
      isAssignedToRoute = seed.assignedRoute != null,
      items = parsedItems,
      crateLabel = "#CR-$crateNumber",
      isPacked = isPacked,
      isInProgress = isInProgress,
    )
  }

  // Aggregate demanded SKUs across routeFilteredOrders
  data class Accumulator(
    val productCode: String,
    val productName: String,
    val packSize: String,
    val category: String,
    var orderedPacks: Int = 0,
    val orderIds: MutableSet<String> = mutableSetOf(),
  )

  val accMap = linkedMapOf<String, Accumulator>()
  // Pre-seed catalog order cleanly
  val catalogOrder = CATALOG_ORDER

  routeFilteredOrders.forEach { order ->
    val lineItems = parseOrderLineItems(order.itemsSummary)
    if (lineItems.isEmpty() && order.itemCount > 0) {
      val fallbackKey = "MISC"
      val acc = accMap.getOrPut(fallbackKey) {
        Accumulator(
          productCode = fallbackKey,
          productName = "Assorted Snacks",
          packSize = "Standard",
          category = "Other Snacks",
        )
      }
      acc.orderedPacks += order.itemCount
      acc.orderIds += order.id
    } else {
      lineItems.forEach { line ->
        val codeKey = line.productCode.uppercase()
        val acc = accMap.getOrPut(codeKey) {
          Accumulator(
            productCode = line.productCode,
            productName = line.productName,
            packSize = line.packSize,
            category = line.category,
          )
        }
        acc.orderedPacks += line.quantity
        acc.orderIds += order.id
      }
    }
  }

  val allSkuLines = accMap.values
    .sortedBy { catalogOrder[it.productCode.uppercase()] ?: 999 }
    .map { acc ->
      val stockQty = stockByCode[acc.productCode.uppercase()]?.quantity ?: 0
      val defaultLoaded = when {
        routeFilteredOrders.all { it.status == OrderStatus.DELIVERED } -> acc.orderedPacks
        stockQty > 0 -> min(acc.orderedPacks, stockQty)
        else -> 0
      }
      val loaded = (loadedOverrides[acc.productCode] ?: defaultLoaded).coerceAtLeast(0)
      PackingSkuLine(
        productCode = acc.productCode,
        productName = acc.productName,
        packSize = acc.packSize,
        category = acc.category,
        orderedPacks = acc.orderedPacks,
        loadedPacks = loaded,
        warehouseStockPacks = stockQty,
        allocatedDropCount = acc.orderIds.size,
      )
    }

  // Filter SKU lines by searchQuery if present (KPI metrics stay anchored to route total)
  val q = searchQuery.trim().lowercase()
  val visibleSkuLines = if (q.isEmpty()) {
    allSkuLines
  } else {
    allSkuLines.filter {
      it.productName.lowercase().contains(q) ||
        it.productCode.lowercase().contains(q) ||
        it.packSize.lowercase().contains(q) ||
        it.category.lowercase().contains(q)
    }
  }

  val categoryOrder = listOf(
    "Banana Chips",
    "Jaggery & Sweet",
    "Mixtures & Savories",
    "Tapioca & Crackers",
    "Other Snacks",
  )

  val categoryGroups = visibleSkuLines
    .groupBy { it.category }
    .entries
    .sortedBy { (cat, _) -> categoryOrder.indexOf(cat).let { if (it == -1) 999 else it } }
    .map { (category, lines) ->
      PackingCategoryGroup(categoryName = category, items = lines)
    }

  val productionBySize = allSkuLines
    .groupBy { it.packSize }
    .entries
    .sortedBy { (packSize, _) -> packSizeSortKey(packSize) }
    .map { (packSize, lines) ->
      val ordered = lines.sumOf { it.orderedPacks }
      val loaded = lines.sumOf { it.loadedPacks }
      val stock = lines.sumOf { it.warehouseStockPacks }
      PacketSizeTarget(
        packSize = packSize,
        orderedPacks = ordered,
        loadedPacks = loaded,
        warehouseStockPacks = stock,
        toPreparePacks = (ordered - loaded).coerceAtLeast(0),
        skuCount = lines.size,
      )
    }

  val totalOrderedPacks = allSkuLines.sumOf { it.orderedPacks }
  val totalLoadedPacks = allSkuLines.sumOf { it.loadedPacks }
  val payloadWeightKg = allSkuLines.sumOf { it.loadedPacks * estimatePackWeightKg(it.packSize) }
  val totalCratesCapacity = selectedRoute?.maxCrates ?: routes.sumOf { it.maxCrates }.coerceAtLeast(30)
  val maxPayloadKg = (totalCratesCapacity * 18.0).coerceAtLeast(180.0)
  val payloadPercent = ((payloadWeightKg / maxPayloadKg) * 100.0).roundToInt().coerceIn(0, 100)
  val stagedCratesCount = if (totalLoadedPacks <= 0) 0 else ceil(totalLoadedPacks / 30.0).toInt()
  val shopsPackedCount = shopSlips.count { it.isPacked }
  val shortageItems = allSkuLines.filter { it.shortagePacks > 0 }

  return PackingDashboardSnapshot(
    selectedRoute = selectedRoute,
    routeFilteredOrders = routeFilteredOrders,
    reverseLoadOrder = reverseLoadOrder,
    assignedOrdersCount = assignedAllOrders.size,
    assignedPacksCount = assignedAllOrders.sumOf { ord ->
      parseOrderLineItems(ord.itemsSummary).sumOf { it.quantity }.takeIf { it > 0 } ?: ord.itemCount
    },
    unassignedOrdersCount = unassignedAllOrders.size,
    unassignedPacksCount = unassignedAllOrders.sumOf { ord ->
      parseOrderLineItems(ord.itemsSummary).sumOf { it.quantity }.takeIf { it > 0 } ?: ord.itemCount
    },
    productionBySize = productionBySize,
    categoryGroups = categoryGroups,
    shopSlips = shopSlips,
    totalOrderedPacks = totalOrderedPacks,
    totalLoadedPacks = totalLoadedPacks,
    payloadWeightKg = payloadWeightKg,
    maxPayloadKg = maxPayloadKg,
    payloadPercent = payloadPercent,
    stagedCratesCount = stagedCratesCount,
    shopsPackedCount = shopsPackedCount,
    totalShopsCount = shopSlips.size,
    shortageItems = shortageItems,
  )
}

fun buildSingleSlipShareText(slip: ShopPackingSlip): String = buildString {
  appendLine("HOMELY BY HOMIE SHOP PACKING SLIP")
  val stopText = if (slip.deliveryStopNumber > 0) {
    "Delivery Stop #${slip.deliveryStopNumber} of ${slip.totalRouteStops}"
  } else {
    "Unassigned Queue"
  }
  appendLine("PACK #${slip.packOrderNumber} (${slip.packageLabel}) • $stopText • ${slip.crateLabel}")
  appendLine("Shop: ${slip.order.shopName} (${slip.order.shopCode})")
  slip.shop?.let {
    if (it.address.isNotBlank()) appendLine("Address: ${it.address}")
    if (it.phone.isNotBlank()) appendLine("Phone: ${it.phone}")
  }
  appendLine("Route: ${slip.route?.name ?: "Unassigned Queue"}")
  appendLine("Order: ${slip.order.orderCode} • Date: ${slip.order.orderDate}")
  appendLine("Items:")
  if (slip.items.isNotEmpty()) {
    slip.items.forEach { item ->
      appendLine(" - ${item.productName} (${item.packSize}) x${item.quantity}")
    }
  } else {
    appendLine(" - ${slip.order.itemsSummary} (${slip.order.itemCount} pk)")
  }
  if (slip.order.note.isNotBlank()) {
    appendLine("Note: ${slip.order.note}")
  }
  appendLine("Status: ${if (slip.isPacked) "PACKED" else "PENDING LOAD"}")
}

fun buildA4ManifestShareText(
  snapshot: PackingDashboardSnapshot,
  selectedRouteId: String,
): String = buildString {
  appendLine("HOMELY BY HOMIE DISPATCH & PACKING MANIFEST")
  appendLine("Route: ${snapshot.selectedRoute?.name ?: if (selectedRouteId == RoutesCatalog.UNALLOCATED) "Unassigned Queue" else "All Active Routes"}")
  snapshot.selectedRoute?.let { r ->
    appendLine("Vehicle: ${r.vehicle} | Bay: ${r.stagingBay}")
    appendLine("Driver: ${r.driverName} (${r.driverPhone}) | Window: ${r.dispatchWindow}")
  }
  appendLine("Summary: ${snapshot.shopsPackedCount}/${snapshot.totalShopsCount} stops packed • ${snapshot.totalLoadedPacks}/${snapshot.totalOrderedPacks} pk • ${snapshot.stagedCratesCount} Crates")
  appendLine()
  if (snapshot.shopSlips.isNotEmpty()) {
    appendLine("VEHICLE LOADING SEQUENCE (${if (snapshot.reverseLoadOrder) "LAST STOP -> STOP #1 (LIFO)" else "STOP #1 -> LAST STOP"}):")
    snapshot.shopSlips.forEach { slip ->
      val qty = slip.items.sumOf { it.quantity }.takeIf { it > 0 } ?: slip.order.itemCount
      val stopLabel = if (slip.deliveryStopNumber > 0) "Stop #${slip.deliveryStopNumber}" else "Unassigned"
      appendLine(" ${slip.packageLabel} | $stopLabel | ${slip.order.shopName} (${qty} pk)")
    }
    appendLine()
  }
  appendLine("TODAY'S PRODUCTION TARGET BY SIZE:")
  snapshot.productionBySize.forEach { target ->
    appendLine(" - ${target.packSize}: Ordered ${target.orderedPacks} pk | Loaded ${target.loadedPacks} pk | To Prepare ${target.toPreparePacks} pk")
  }
  appendLine()
  appendLine("CONSOLIDATED SKU LOAD:")
  snapshot.categoryGroups.forEach { group ->
    appendLine("[${group.categoryName}] (${group.totalLoadedPacks}/${group.totalOrderedPacks} pk)")
    group.items.forEach { sku ->
      val statusTag = if (sku.shortagePacks > 0) "SHORT -${sku.shortagePacks}" else "OK"
      appendLine(" * ${sku.productCode} ${sku.productName} (${sku.packSize}): ${sku.loadedPacks}/${sku.orderedPacks} pk [$statusTag]")
    }
  }
}
