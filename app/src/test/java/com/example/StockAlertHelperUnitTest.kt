package com.example

import com.example.data.model.Product
import com.example.data.model.ProductCatalogHelper
import com.example.data.model.ProductStatus
import com.example.data.model.StockAlertHelper
import com.example.data.model.StockItem
import com.example.data.model.UnifiedStockSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StockAlertHelperUnitTest {

  @Test
  fun buildUnifiedAlerts_includesNewlyAddedProductsAndFlagsLowStock() {
    val today = "2026-11-26"
    val existingProduct = Product(
      id = "seed_prd_2",
      userId = "u1",
      productCode = "PRD-00102",
      sku = "SKU-CNO-002",
      name = "Pure Coconut Oil 1L Pouch",
      category = "Cooking Oil",
      packSize = "1L Pouch",
      wholesalePrice = 165.0,
      stockQuantity = 15,
      minThreshold = 40,
      status = ProductStatus.ACTIVE,
      addedDate = "2026-11-10",
    )
    val newlyAddedLow = Product(
      id = "prd_new_low",
      userId = "u1",
      productCode = "PRD-00107",
      sku = "SKU-PRD-107",
      name = "Kochi Ginger Candy",
      category = "Biscuits & Snacks",
      packSize = "150g Pouch",
      wholesalePrice = 65.0,
      stockQuantity = 12,
      minThreshold = 25,
      status = ProductStatus.ACTIVE,
      addedDate = today,
    )
    val newlyAddedHealthy = Product(
      id = "prd_new_ok",
      userId = "u1",
      productCode = "PRD-00108",
      sku = "SKU-PRD-108",
      name = "Wayanad Honey Jar",
      category = "Spices & Masala",
      packSize = "500g",
      wholesalePrice = 240.0,
      stockQuantity = 80,
      minThreshold = 20,
      status = ProductStatus.ACTIVE,
      addedDate = today,
    )

    val stocks = listOf(
      StockItem(id = "stk_1", productCode = "BC100", productName = "Banana Chips", packSize = "100g", quantity = 24, minThreshold = 50),
      StockItem(id = "stk_2", productCode = "TP200", productName = "Tapioca Chips", packSize = "200g", quantity = 85, minThreshold = 30),
    )

    val alerts = StockAlertHelper.buildUnifiedAlerts(
      products = listOf(existingProduct, newlyAddedLow, newlyAddedHealthy),
      stocks = stocks,
    )

    val newLowAlert = alerts.firstOrNull { it.code == "PRD-00107" }
    val newHealthyAlert = alerts.firstOrNull { it.code == "PRD-00108" }
    val starterStockAlert = alerts.firstOrNull { it.code == "BC100" }

    assertTrue(newLowAlert != null)
    assertEquals(UnifiedStockSource.PRODUCT, newLowAlert!!.source)
    assertTrue(newLowAlert.isLow)
    assertTrue(newLowAlert.isCritical)

    assertTrue(newHealthyAlert != null)
    assertEquals(UnifiedStockSource.PRODUCT, newHealthyAlert!!.source)
    assertFalse(newHealthyAlert.isLow)

    assertTrue(starterStockAlert != null)
    assertEquals(UnifiedStockSource.STOCK_ITEM, starterStockAlert!!.source)
    assertTrue(starterStockAlert.isLow)

    // Low stock alerts must sort ahead of healthy items
    val firstHealthyIdx = alerts.indexOfFirst { !it.isLow }
    val lastLowIdx = alerts.indexOfLast { it.isLow }
    assertTrue(lastLowIdx < firstHealthyIdx)
  }

  @Test
  fun buildUnifiedAlerts_deduplicatesMatchingProductAndStockCodes() {
    val product = Product(
      id = "prd_custom",
      productCode = "PRD-00200",
      sku = "CUST-200",
      name = "Custom Jackfruit Chips",
      stockQuantity = 5,
      minThreshold = 20,
      status = ProductStatus.ACTIVE,
    )
    val matchingStock = StockItem(
      id = "stk_dup",
      productCode = "CUST-200",
      productName = "Custom Jackfruit Chips",
      quantity = 5,
      minThreshold = 20,
    )

    val alerts = StockAlertHelper.buildUnifiedAlerts(listOf(product), listOf(matchingStock))
    assertEquals(1, alerts.size)
    assertEquals("PRD-00200", alerts.first().code)
  }

  @Test
  fun resolveWarehouseStockMap_includesNewProductCodesAndSkus() {
    val product = Product(
      id = "prd_new",
      productCode = "PRD-00109",
      sku = "SKU-JACK-250",
      name = "Jackfruit Chips",
      stockQuantity = 42,
      minThreshold = 15,
      status = ProductStatus.ACTIVE,
    )
    val stock = StockItem(
      id = "stk_1",
      productCode = "BC100",
      productName = "Banana Chips",
      quantity = 24,
      minThreshold = 50,
    )

    val map = StockAlertHelper.resolveWarehouseStockMap(listOf(product), listOf(stock))
    assertEquals(42, map["PRD-00109"])
    assertEquals(42, map["SKU-JACK-250"])
    assertEquals(24, map["BC100"])
  }
}
