package com.example

import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.StockItem
import com.example.ui.screens.buildPackingSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackingSequenceUnitTest {

  private val route = DeliveryRoute(
    id = RoutesCatalog.R01_KANNUR_BANGALORE,
    name = "Route 1: Kannur - Bangalore",
    stopOrderIds = listOf("s1", "s2", "s3"),
  )

  private val shops = listOf(
    Shop(id = "s1", shopCode = "SHP-1", name = "Shop One", routeId = route.id),
    Shop(id = "s2", shopCode = "SHP-2", name = "Shop Two", routeId = route.id),
    Shop(id = "s3", shopCode = "SHP-3", name = "Shop Three", routeId = route.id),
  )

  private val orders = listOf(
    OrderRecord(
      id = "ord_1",
      orderCode = "ORD-1",
      shopId = "s1",
      shopCode = "SHP-1",
      shopName = "Shop One",
      routeId = route.id,
      itemsSummary = "BC100 x50",
      itemCount = 50,
      status = OrderStatus.CONFIRMED,
    ),
    OrderRecord(
      id = "ord_2",
      orderCode = "ORD-2",
      shopId = "s2",
      shopCode = "SHP-2",
      shopName = "Shop Two",
      routeId = route.id,
      itemsSummary = "BC200 x20",
      itemCount = 20,
      status = OrderStatus.CONFIRMED,
    ),
    OrderRecord(
      id = "ord_3",
      orderCode = "ORD-3",
      shopId = "s3",
      shopCode = "SHP-3",
      shopName = "Shop Three",
      routeId = route.id,
      itemsSummary = "JB200 x15",
      itemCount = 15,
      status = OrderStatus.CONFIRMED,
    ),
  )

  private val stocks = listOf(
    StockItem(id = "st1", productCode = "BC100", productName = "Banana Chips", packSize = "100g", quantity = 10),
    StockItem(id = "st2", productCode = "BC200", productName = "Banana Chips", packSize = "200g", quantity = 50),
    StockItem(id = "st3", productCode = "JB200", productName = "Jaggery Banana", packSize = "200g", quantity = 50),
  )

  @Test
  fun sequentialPacking_unlocksOnlyOneOrderAtATimeInSequence() {
    val step1 = buildPackingSnapshot(
      selectedRouteId = route.id,
      shops = shops,
      orders = orders,
      routes = listOf(route),
      stocks = stocks,
      reverseLoadOrder = false,
    )

    assertEquals(3, step1.shopSlips.size)
    assertTrue(step1.shopSlips[0].isNextInSequence)
    assertFalse(step1.shopSlips[0].isSequenceLocked)

    assertFalse(step1.shopSlips[1].isNextInSequence)
    assertTrue(step1.shopSlips[1].isSequenceLocked)
    assertEquals(1, step1.shopSlips[1].blockingPackOrderNumber)

    assertTrue(step1.shopSlips[2].isSequenceLocked)
    assertEquals(1, step1.shopSlips[2].blockingPackOrderNumber)

    // Complete Pkg #1 -> Pkg #2 unlocks, Pkg #3 waits for Pkg #2
    val step2 = buildPackingSnapshot(
      selectedRouteId = route.id,
      shops = shops,
      orders = orders,
      routes = listOf(route),
      stocks = stocks,
      packedOrderOverrides = mapOf("ord_1" to true),
      reverseLoadOrder = false,
    )

    assertTrue(step2.shopSlips[0].isPacked)
    assertFalse(step2.shopSlips[0].isSequenceLocked)
    assertTrue(step2.shopSlips[1].isNextInSequence)
    assertFalse(step2.shopSlips[1].isSequenceLocked)
    assertTrue(step2.shopSlips[2].isSequenceLocked)
    assertEquals(2, step2.shopSlips[2].blockingPackOrderNumber)
  }

  @Test
  fun stockUnavailableHold_marksOrderPendingAndUnlocksNextOrdersOnTime() {
    val held = buildPackingSnapshot(
      selectedRouteId = route.id,
      shops = shops,
      orders = orders,
      routes = listOf(route),
      stocks = stocks,
      stockPendingReasons = mapOf("ord_1" to "BC100 out of stock"),
      reverseLoadOrder = false,
    )

    assertFalse(held.shopSlips[0].isPacked)
    assertTrue(held.shopSlips[0].isStockPending)
    assertEquals("BC100 out of stock", held.shopSlips[0].stockPendingReason)
    assertFalse(held.shopSlips[0].isSequenceLocked) // Can be packed once stock arrives
    assertEquals(1, held.stockPendingCount)

    // Pkg #2 is immediately unlocked while Pkg #3 waits for Pkg #2
    assertTrue(held.shopSlips[1].isNextInSequence)
    assertFalse(held.shopSlips[1].isSequenceLocked)
    assertNull(held.shopSlips[1].blockingPackOrderNumber)

    assertTrue(held.shopSlips[2].isSequenceLocked)
    assertEquals(2, held.shopSlips[2].blockingPackOrderNumber)

    // Complete Pkg #2 and Pkg #3 while Pkg #1 remains on stock hold
    val laterDispatched = buildPackingSnapshot(
      selectedRouteId = route.id,
      shops = shops,
      orders = orders,
      routes = listOf(route),
      stocks = stocks,
      packedOrderOverrides = mapOf("ord_2" to true, "ord_3" to true),
      stockPendingReasons = mapOf("ord_1" to "BC100 out of stock"),
      reverseLoadOrder = false,
    )
    assertEquals(2, laterDispatched.shopsPackedCount)
    assertEquals(1, laterDispatched.stockPendingCount)
    assertFalse(laterDispatched.shopSlips[0].isSequenceLocked)
  }
}
