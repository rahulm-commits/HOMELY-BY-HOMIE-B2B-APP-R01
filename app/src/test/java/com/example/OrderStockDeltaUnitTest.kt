package com.example

import com.example.data.model.CatalogProduct
import com.example.data.model.OrderStatus
import com.example.data.model.OrderStockDeltaHelper
import com.example.data.model.Product
import com.example.data.model.SnackCatalog
import com.example.data.model.StockItem
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderStockDeltaUnitTest {

  @Test
  fun orderStockDeltaHelper_parsesComputesDeltasAndMapsTargets() {
    val parsed = OrderStockDeltaHelper.parseQuantitiesByCode("BC100 x10, JB200 x5, BC100 × 2")
    assertEquals(12, parsed["BC100"])
    assertEquals(5, parsed["JB200"])

    val bc100 = SnackCatalog.products.first { it.code == "BC100" }
    val jb200 = SnackCatalog.products.first { it.code == "JB200" }
    val tp200 = SnackCatalog.products.first { it.code == "TP200" }

    val deltas = OrderStockDeltaHelper.computeDeltaBetweenOrders(
      oldItemsSummary = "BC100 x10, JB200 x5",
      newItems = listOf(bc100 to 12, jb200 to 2, tp200 to 4),
    )
    assertEquals(-2, deltas["BC100"])
    assertEquals(3, deltas["JB200"])
    assertEquals(-4, deltas["TP200"])

    val stockTargets = OrderStockDeltaHelper.computeStockItemTargets(
      stocks = listOf(
        StockItem(id = "stk_bc", productCode = "BC100", productName = "Banana Chips", quantity = 10),
        StockItem(id = "stk_jb", productCode = "JB200", productName = "Jaggery Banana", quantity = 1),
      ),
      qtyDeltaByCode = mapOf("BC100" to -15, "JB200" to 3),
    )
    assertEquals(0, stockTargets["stk_bc"])
    assertEquals(4, stockTargets["stk_jb"])

    val singleProduct = Product(
      id = "prd_1",
      productCode = "PRD-00101",
      sku = "BC100",
      name = "Banana Chips",
      stockQuantity = 100,
      hasVariants = false,
    )
    val prodTargets = OrderStockDeltaHelper.computeProductStockTargets(
      products = listOf(singleProduct),
      qtyDeltaByCode = mapOf("BC100" to -10),
    )
    assertEquals(90, prodTargets["prd_1"])
  }

  @Test
  fun orderStockDeltaHelper_unchangedItemsAndCancelRestoreQuantitiesMatch() {
    val bc100 = CatalogProduct("BC100", "Banana Chips", "100g", "Chips", 45.0)
    assertEquals(
      emptyMap<String, Int>(),
      OrderStockDeltaHelper.computeDeltaBetweenOrders("BC100 x6", listOf(bc100 to 6)),
    )

    val placeDeltas = OrderStockDeltaHelper.fromCartItems(listOf(bc100 to 6)).mapValues { -it.value }
    val cancelRestoreDeltas = OrderStockDeltaHelper.parseQuantitiesByCode("BC100 x6")
    assertEquals(-6, placeDeltas["BC100"])
    assertEquals(6, cancelRestoreDeltas["BC100"])
    assertEquals("rejected", OrderStatus.REJECTED)
  }
}
