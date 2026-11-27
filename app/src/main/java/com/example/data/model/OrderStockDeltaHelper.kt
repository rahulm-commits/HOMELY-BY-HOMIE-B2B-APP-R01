package com.example.data.model

/**
 * Pure helper functions for computing per-SKU inventory deltas when orders are placed,
 * edited, cancelled (OrderStatus.REJECTED), or deleted, and mapping them onto both
 * [StockItem] alert entries and [Product] catalog quantities.
 */
object OrderStockDeltaHelper {

  private val ITEM_REGEX = Regex("""([A-Za-z0-9_-]+)\s*[xX×]\s*(\d+)""")

  fun parseQuantitiesByCode(itemsSummary: String): Map<String, Int> {
    if (itemsSummary.isBlank()) return emptyMap()
    val counts = LinkedHashMap<String, Int>()
    ITEM_REGEX.findAll(itemsSummary).forEach { match ->
      val code = match.groupValues[1].trim().uppercase()
      val qty = match.groupValues[2].toIntOrNull() ?: 0
      if (code.isNotEmpty() && qty > 0) {
        counts[code] = (counts[code] ?: 0) + qty
      }
    }
    return counts
  }

  fun fromCartItems(items: List<Pair<CatalogProduct, Int>>): Map<String, Int> {
    val counts = LinkedHashMap<String, Int>()
    items.forEach { (prod, qty) ->
      val code = prod.code.trim().uppercase()
      if (code.isNotEmpty() && qty > 0) {
        counts[code] = (counts[code] ?: 0) + qty
      }
    }
    return counts
  }

  fun computeDeltaBetweenOrders(
    oldItemsSummary: String,
    newItems: List<Pair<CatalogProduct, Int>>,
  ): Map<String, Int> {
    val oldMap = parseQuantitiesByCode(oldItemsSummary)
    val newMap = fromCartItems(newItems)
    val allCodes = oldMap.keys + newMap.keys
    val deltas = LinkedHashMap<String, Int>()
    allCodes.forEach { code ->
      val diff = (oldMap[code] ?: 0) - (newMap[code] ?: 0)
      if (diff != 0) {
        deltas[code] = diff
      }
    }
    return deltas
  }

  /**
   * Maps per-SKU deltas (negative = deduct stock, positive = restore stock)
   * onto [StockItem.id] -> new target quantity.
   */
  fun computeStockItemTargets(
    stocks: List<StockItem>,
    qtyDeltaByCode: Map<String, Int>,
  ): Map<String, Int> {
    if (qtyDeltaByCode.isEmpty()) return emptyMap()
    val targets = LinkedHashMap<String, Int>()
    stocks.forEach { stock ->
      if (stock.id.isBlank()) return@forEach
      val delta = qtyDeltaByCode[stock.productCode.trim().uppercase()] ?: 0
      if (delta != 0) {
        val updated = (stock.quantity + delta).coerceIn(0, 1_000_000)
        if (updated != stock.quantity) {
          targets[stock.id] = updated
        }
      }
    }
    return targets
  }

  /**
   * Maps per-SKU deltas (negative = deduct stock, positive = restore stock)
   * onto [Product.id] -> new target stockQuantity.
   */
  fun computeProductStockTargets(
    products: List<Product>,
    qtyDeltaByCode: Map<String, Int>,
  ): Map<String, Int> {
    if (qtyDeltaByCode.isEmpty()) return emptyMap()
    val targets = LinkedHashMap<String, Int>()
    products.forEach { product ->
      if (product.id.isBlank()) return@forEach
      val variantSkus = if (product.hasVariants) {
        ProductCatalogHelper.decodeVariants(product.variantsJson)
          .mapNotNull { it.sku.trim().uppercase().takeIf { s -> s.isNotEmpty() } }
          .distinct()
      } else {
        emptyList()
      }
      val candidateCodes = if (variantSkus.isNotEmpty()) {
        variantSkus
      } else {
        listOf(product.sku.trim().uppercase(), product.productCode.trim().uppercase())
          .filter { it.isNotEmpty() }
          .distinct()
      }
      val totalDelta = candidateCodes.sumOf { code -> qtyDeltaByCode[code] ?: 0 }
      if (totalDelta != 0) {
        val updated = (product.stockQuantity + totalDelta).coerceIn(0, 1_000_000)
        if (updated != product.stockQuantity) {
          targets[product.id] = updated
        }
      }
    }
    return targets
  }
}
