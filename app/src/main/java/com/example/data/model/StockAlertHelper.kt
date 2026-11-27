package com.example.data.model

enum class UnifiedStockSource {
  PRODUCT,
  STOCK_ITEM,
}

data class UnifiedStockAlert(
  val key: String,
  val source: UnifiedStockSource,
  val targetId: String,
  val code: String,
  val secondaryCode: String = "",
  val name: String,
  val packSize: String,
  val category: String,
  val unitLabel: String,
  val quantity: Int,
  val minThreshold: Int,
  val isLow: Boolean,
  val isCritical: Boolean,
  val status: String,
  val isRecentlyAdded: Boolean = false,
)

/**
 * Pure helper for unifying [Product] catalog items (including newly created products)
 * and legacy/variant [StockItem] records into a single alert & quick-stock-update list.
 */
object StockAlertHelper {

  fun buildUnifiedAlerts(
    products: List<Product>,
    stocks: List<StockItem>,
    nowMillis: Long = System.currentTimeMillis(),
  ): List<UnifiedStockAlert> {
    val entries = mutableListOf<UnifiedStockAlert>()
    val directProductCodes = mutableSetOf<String>()

    products.forEach { product ->
      val primaryCode = product.productCode.trim().ifBlank { product.sku.trim() }
      val skuCode = product.sku.trim()
      if (primaryCode.isNotEmpty()) {
        directProductCodes.add(primaryCode.uppercase())
      }
      if (!product.hasVariants && skuCode.isNotEmpty()) {
        directProductCodes.add(skuCode.uppercase())
      }

      val threshold = product.minThreshold.coerceAtLeast(1)
      val isInactive = product.status == ProductStatus.INACTIVE
      val isLow = !isInactive && product.stockQuantity <= threshold
      val isCritical = isLow && (product.stockQuantity == 0 || product.stockQuantity * 2 <= threshold)
      val unitShort = ProductCatalogHelper.unitShortLabel(product.sellingUnit)
      val packDisplay = product.packSize.ifBlank { product.baseUnit }.ifBlank { "Standard" }

      entries.add(
        UnifiedStockAlert(
          key = "prd_${product.id.ifBlank { primaryCode }}",
          source = UnifiedStockSource.PRODUCT,
          targetId = product.id,
          code = primaryCode,
          secondaryCode = if (skuCode.equals(primaryCode, ignoreCase = true)) "" else skuCode,
          name = product.name,
          packSize = packDisplay,
          category = product.category,
          unitLabel = unitShort,
          quantity = product.stockQuantity.coerceAtLeast(0),
          minThreshold = threshold,
          isLow = isLow,
          isCritical = isCritical,
          status = product.status,
          isRecentlyAdded = ProductCatalogHelper.isRecentlyAdded(product, nowMillis),
        )
      )
    }

    stocks.forEach { stock ->
      val code = stock.productCode.trim()
      if (code.uppercase() in directProductCodes) {
        return@forEach
      }
      val threshold = stock.minThreshold.coerceAtLeast(1)
      val qty = stock.quantity.coerceAtLeast(0)
      val isLow = qty <= threshold
      val isCritical = isLow && (qty == 0 || qty * 2 <= threshold)

      entries.add(
        UnifiedStockAlert(
          key = "stk_${stock.id.ifBlank { code }}",
          source = UnifiedStockSource.STOCK_ITEM,
          targetId = stock.id,
          code = code,
          secondaryCode = "",
          name = stock.productName,
          packSize = stock.packSize.ifBlank { "Standard" },
          category = stock.category,
          unitLabel = "pkts",
          quantity = qty,
          minThreshold = threshold,
          isLow = isLow,
          isCritical = isCritical,
          status = ProductStatus.ACTIVE,
          isRecentlyAdded = false,
        )
      )
    }

    return entries.sortedWith(
      compareBy<UnifiedStockAlert>(
        { if (it.isLow) 0 else 1 },
        { if (it.isCritical) 0 else 1 },
        { if (it.status == ProductStatus.INACTIVE) 1 else 0 },
        { it.quantity.toDouble() / it.minThreshold.coerceAtLeast(1).toDouble() },
        { if (it.isRecentlyAdded) 0 else 1 },
        { it.name.lowercase() },
        { it.code.lowercase() },
      )
    )
  }

  /**
   * Resolves current warehouse stock quantities by uppercase SKU / product code
   * across both [StockItem] records and [Product] catalog items (including variants).
   */
  fun resolveWarehouseStockMap(
    products: List<Product>,
    stocks: List<StockItem>,
  ): Map<String, Int> {
    val map = LinkedHashMap<String, Int>()
    products.forEach { product ->
      val qty = product.stockQuantity.coerceAtLeast(0)
      val pCode = product.productCode.trim().uppercase()
      val sku = product.sku.trim().uppercase()
      if (pCode.isNotEmpty()) map[pCode] = qty
      if (sku.isNotEmpty()) map[sku] = qty
      if (product.hasVariants) {
        ProductCatalogHelper.decodeVariants(product.variantsJson).forEach { variant ->
          val vSku = variant.sku.trim().uppercase()
          if (vSku.isNotEmpty() && !map.containsKey(vSku)) {
            map[vSku] = qty
          }
        }
      }
    }
    stocks.forEach { stock ->
      val code = stock.productCode.trim().uppercase()
      if (code.isNotEmpty()) {
        map[code] = stock.quantity.coerceAtLeast(0)
      }
    }
    return map
  }
}
