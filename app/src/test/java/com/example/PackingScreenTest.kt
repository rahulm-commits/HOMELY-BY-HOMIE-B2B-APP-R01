package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.StockItem
import com.example.ui.screens.PackingScreen
import com.example.ui.screens.buildPackingExcelBytes
import com.example.ui.screens.buildPackingSnapshot
import com.example.ui.screens.parseOrderLineItems
import com.example.ui.theme.HomelyTheme
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class PackingScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val sampleRoutes = listOf(
    DeliveryRoute(
      id = RoutesCatalog.R01_KANNUR_BANGALORE,
      name = "Route 1: Kannur - Bangalore",
      vehicle = "Tata Ace (KL-13-AB-1122)",
      maxCrates = 40,
      driverName = "Rajan Kumar",
      driverPhone = "+91 98450 11223",
      dispatchWindow = "06:30 AM – 04:00 PM",
      stagingBay = "Staging Bay #03 • Kochi Central Hub",
      colorIndex = 0,
    ),
    DeliveryRoute(
      id = RoutesCatalog.R02_KANNUR_KOZHIKODE,
      name = "Route 2: Kannur - Kozhikode",
      vehicle = "Mahindra Bolero (KL-07-CB-9081)",
      maxCrates = 40,
      driverName = "Suresh Babu",
      driverPhone = "+91 98451 22334",
      dispatchWindow = "07:00 AM – 03:30 PM",
      stagingBay = "Staging Bay #02 • Kochi Central Hub",
      colorIndex = 1,
    ),
  )

  private val sampleShops = listOf(
    Shop(
      id = "shop_r1",
      shopCode = "SHP-101",
      name = "Lakshmi General Store",
      ownerName = "Lakshmi",
      phone = "9845011111",
      address = "MG Road, Kannur",
      pinCode = "670001",
      routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
    ),
    Shop(
      id = "shop_r2",
      shopCode = "SHP-102",
      name = "Malabar Supermart",
      ownerName = "Hameed",
      phone = "9845022222",
      address = "Beach Rd, Kozhikode",
      pinCode = "673001",
      routeId = RoutesCatalog.R02_KANNUR_KOZHIKODE,
    ),
    Shop(
      id = "shop_unalloc",
      shopCode = "SHP-103",
      name = "Priya Corner Shop",
      ownerName = "Priya",
      phone = "9845033333",
      address = "Edappally Toll",
      pinCode = "682024",
      routeId = RoutesCatalog.UNALLOCATED,
    ),
  )

  private val sampleOrders = listOf(
    OrderRecord(
      id = "ord_r1",
      orderCode = "ORD-101",
      shopId = "shop_r1",
      shopCode = "SHP-101",
      shopName = "Lakshmi General Store",
      routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
      itemsSummary = "BC100 x200, BC200 x75, JB200 x30",
      itemCount = 305,
      totalAmount = 14500.0,
      status = OrderStatus.CONFIRMED,
      orderDate = "2026-10-26",
    ),
    OrderRecord(
      id = "ord_r2",
      orderCode = "ORD-102",
      shopId = "shop_r2",
      shopCode = "SHP-102",
      shopName = "Malabar Supermart",
      routeId = RoutesCatalog.R02_KANNUR_KOZHIKODE,
      itemsSummary = "BC500 x15, MX200 x40",
      itemCount = 55,
      totalAmount = 4200.0,
      status = OrderStatus.CONFIRMED,
      orderDate = "2026-10-26",
    ),
    OrderRecord(
      id = "ord_unalloc",
      orderCode = "ORD-103",
      shopId = "shop_unalloc",
      shopCode = "SHP-103",
      shopName = "Priya Corner Shop",
      routeId = RoutesCatalog.UNALLOCATED,
      itemsSummary = "TP200 x45",
      itemCount = 45,
      totalAmount = 1800.0,
      status = OrderStatus.CONFIRMED,
      orderDate = "2026-10-26",
    ),
  )

  private val sampleStocks = listOf(
    StockItem(id = "st1", productCode = "BC100", productName = "Banana Chips", packSize = "100g", quantity = 180, unitPrice = 45.0),
    StockItem(id = "st2", productCode = "BC200", productName = "Banana Chips", packSize = "200g", quantity = 100, unitPrice = 85.0),
    StockItem(id = "st3", productCode = "BC500", productName = "Banana Chips", packSize = "500g", quantity = 25, unitPrice = 200.0),
    StockItem(id = "st4", productCode = "JB200", productName = "Jaggery Banana", packSize = "200g", quantity = 50, unitPrice = 95.0),
    StockItem(id = "st5", productCode = "MX200", productName = "Kerala Spicy Mixture", packSize = "200g", quantity = 60, unitPrice = 75.0),
    StockItem(id = "st6", productCode = "TP200", productName = "Tapioca Chips", packSize = "200g", quantity = 30, unitPrice = 65.0),
  )

  @Test
  fun parseOrderLineItems_andBuildPackingSnapshot_filtersByRouteAndComputesProductionTargets() {
    val parsed = parseOrderLineItems("BC100 x200, BC200 x75, JB200 x30")
    assertEquals(3, parsed.size)
    assertEquals("BC100", parsed[0].productCode)
    assertEquals(200, parsed[0].quantity)
    assertEquals("100g", parsed[0].packSize)

    // Selected Route 1 should ONLY include Route 1's order (305 packs total)
    val r1Snapshot = buildPackingSnapshot(
      selectedRouteId = RoutesCatalog.R01_KANNUR_BANGALORE,
      shops = sampleShops,
      orders = sampleOrders,
      routes = sampleRoutes,
      stocks = sampleStocks,
    )
    assertEquals(1, r1Snapshot.routeFilteredOrders.size)
    assertEquals(305, r1Snapshot.totalOrderedPacks)
    assertEquals(285, r1Snapshot.totalLoadedPacks) // 180 + 75 + 30
    assertEquals(2, r1Snapshot.assignedOrdersCount)
    assertEquals(1, r1Snapshot.unassignedOrdersCount)

    // Verify today's production target grouped by packet size (100g and 200g)
    val target100g = r1Snapshot.productionBySize.first { it.packSize == "100g" }
    assertEquals(200, target100g.orderedPacks)
    assertEquals(180, target100g.loadedPacks)
    assertEquals(20, target100g.toPreparePacks)

    val target200g = r1Snapshot.productionBySize.first { it.packSize == "200g" }
    assertEquals(105, target200g.orderedPacks) // 75 BC200 + 30 JB200
    assertEquals(0, target200g.toPreparePacks)

    // Verify section-by-section categories (Banana Chips & Jaggery & Sweet)
    val categoryNames = r1Snapshot.categoryGroups.map { it.categoryName }
    assertTrue(categoryNames.contains("Banana Chips"))
    assertTrue(categoryNames.contains("Jaggery & Sweet"))
    assertEquals(1, r1Snapshot.shortageItems.size)
    assertEquals("BC100", r1Snapshot.shortageItems.first().productCode)
  }

  @Test
  fun packingScreen_rendersVehicleDetailsOverallCountsAndShopOrders() {
    composeTestRule.setContent {
      HomelyTheme {
        PackingScreen(
          shops = sampleShops,
          orders = sampleOrders,
          routes = sampleRoutes,
          stocks = sampleStocks,
          onUpdateStock = { _, _, _ -> },
          onOpenSyncServers = {},
          onNavigateToRoutes = {},
          onNavigateToOrders = {},
          onShowFeedback = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    val list = composeTestRule.onNodeWithTag("packing_lazy_list")
    composeTestRule.onNodeWithTag("packing_route_filter").assertIsDisplayed()
    composeTestRule.onNodeWithTag("packing_operational_banner").assertIsDisplayed()

    // Overall count of different packets card
    list.performScrollToNode(hasTestTag("packing_overall_counts_card"))
    composeTestRule.onNodeWithTag("packing_overall_counts_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Overall Packet Count").assertIsDisplayed()

    // Shop-by-shop order details below overall counts
    list.performScrollToNode(hasTestTag("packing_shop_card_ord_r1"))
    composeTestRule.onNodeWithTag("packing_shop_card_ord_r1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Lakshmi General Store").assertIsDisplayed()

    list.performScrollToNode(hasTestTag("packing_shop_card_ord_r2"))
    composeTestRule.onNodeWithTag("packing_shop_card_ord_r2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Malabar Supermart").assertIsDisplayed()

    // Toggle packed in sequence (ord_r1 then ord_r2) and filter by Packed
    list.performScrollToNode(hasTestTag("packing_toggle_packed_ord_r1"))
    composeTestRule.onNodeWithTag("packing_toggle_packed_ord_r1").performClick()
    composeTestRule.waitForIdle()
    list.performScrollToNode(hasTestTag("packing_toggle_packed_ord_r2"))
    composeTestRule.onNodeWithTag("packing_toggle_packed_ord_r2").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("packing_filter_packed").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("packing_shop_card_ord_r2").assertIsDisplayed()

    // Bottom Excel export buttons remain visible
    composeTestRule.onNodeWithTag("packing_primary_print_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("packing_save_excel_button").assertIsDisplayed()
  }

  @Test
  fun packingExcelExporter_generatesValidXlsxWithVehicleOverallCountsAndShopOrders() {
    val snapshot = buildPackingSnapshot(
      selectedRouteId = RoutesCatalog.R01_KANNUR_BANGALORE,
      shops = sampleShops,
      orders = sampleOrders,
      routes = sampleRoutes,
      stocks = sampleStocks,
      reverseLoadOrder = false,
    )
    val bytes = buildPackingExcelBytes(snapshot, RoutesCatalog.R01_KANNUR_BANGALORE)
    assertTrue(bytes.isNotEmpty())

    val entries = mutableMapOf<String, String>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    assertTrue(entries.containsKey("[Content_Types].xml"))
    assertTrue(entries.containsKey("_rels/.rels"))
    assertTrue(entries.containsKey("xl/workbook.xml"))
    assertTrue(entries.containsKey("xl/_rels/workbook.xml.rels"))
    assertTrue(entries.containsKey("xl/styles.xml"))
    assertTrue(entries.containsKey("xl/sharedStrings.xml"))
    assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
    assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))

    val xmlFactory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
    entries.forEach { (name, xml) ->
      assertTrue("$name must start with <?xml declaration", xml.startsWith("<?xml"))
      xmlFactory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }

    val sharedStrings = entries.getValue("xl/sharedStrings.xml")
    assertTrue(sharedStrings.contains("VEHICLE DETAILS"))
    assertTrue(sharedStrings.contains("Tata Ace (KL-13-AB-1122)"))
    assertTrue(sharedStrings.contains("OVERALL PACKET COUNT"))
    assertTrue(sharedStrings.contains("Banana Chips"))
    assertTrue(sharedStrings.contains("Lakshmi General Store"))
    assertTrue(sharedStrings.contains("ORD-101"))

    val sheet1 = entries.getValue("xl/worksheets/sheet1.xml")
    assertTrue(sheet1.contains("SUM("))
    assertTrue(sheet1.contains("<mergeCells"))
    assertTrue(sheet1.contains("<v>305</v>"))

    val sheet2 = entries.getValue("xl/worksheets/sheet2.xml")
    assertTrue(sheet2.contains("<v>200</v>"))
  }

  @Test
  fun buildPackingSnapshot_ordersSixRouteStopsLastToFirstWithPackageNumbers() {
    val orderedIds = listOf("s1", "s2", "s3", "s4", "s5", "s6")
    val route = DeliveryRoute(
      id = "R-SIX-STOPS",
      name = "Route 1: Six Stop Corridor",
      stopOrderIds = orderedIds,
    )
    val shops = orderedIds.mapIndexed { idx, id ->
      Shop(
        id = id,
        shopCode = "SHP-${idx + 1}",
        name = "Stop Shop #${idx + 1}",
        routeId = route.id,
      )
    }
    val orders = orderedIds.mapIndexed { idx, shopId ->
      OrderRecord(
        id = "ord_$shopId",
        orderCode = "ORD-${idx + 1}",
        shopId = shopId,
        shopCode = "SHP-${idx + 1}",
        shopName = "Stop Shop #${idx + 1}",
        routeId = route.id,
        itemsSummary = "BC100 x${(idx + 1) * 5}",
        itemCount = (idx + 1) * 5,
        status = OrderStatus.CONFIRMED,
      )
    }

    val reverseSnapshot = buildPackingSnapshot(
      selectedRouteId = route.id,
      shops = shops,
      orders = orders,
      routes = listOf(route),
      stocks = sampleStocks,
      reverseLoadOrder = true,
    )

    assertEquals(6, reverseSnapshot.shopSlips.size)
    // Last delivery stop (#6) must be packed FIRST as Pkg #1 at index 0
    assertEquals("s6", reverseSnapshot.shopSlips[0].order.shopId)
    assertEquals(6, reverseSnapshot.shopSlips[0].deliveryStopNumber)
    assertEquals(6, reverseSnapshot.shopSlips[0].totalRouteStops)
    assertEquals(1, reverseSnapshot.shopSlips[0].packOrderNumber)
    assertEquals("Pkg #1", reverseSnapshot.shopSlips[0].packageLabel)

    // Second-last stop (#5) packed second as Pkg #2
    assertEquals("s5", reverseSnapshot.shopSlips[1].order.shopId)
    assertEquals(5, reverseSnapshot.shopSlips[1].deliveryStopNumber)
    assertEquals(2, reverseSnapshot.shopSlips[1].packOrderNumber)
    assertEquals("Pkg #2", reverseSnapshot.shopSlips[1].packageLabel)

    // First delivery stop (#1) is packed LAST as Pkg #6 (loaded near truck door)
    assertEquals("s1", reverseSnapshot.shopSlips[5].order.shopId)
    assertEquals(1, reverseSnapshot.shopSlips[5].deliveryStopNumber)
    assertEquals(6, reverseSnapshot.shopSlips[5].packOrderNumber)
    assertEquals("Pkg #6", reverseSnapshot.shopSlips[5].packageLabel)

    // Delivery mode (reverseLoadOrder = false) restores road order 1..6
    val roadSnapshot = buildPackingSnapshot(
      selectedRouteId = route.id,
      shops = shops,
      orders = orders,
      routes = listOf(route),
      stocks = sampleStocks,
      reverseLoadOrder = false,
    )
    assertEquals(listOf(1, 2, 3, 4, 5, 6), roadSnapshot.shopSlips.map { it.deliveryStopNumber })
  }

  @Test
  fun packingScreen_markPackingComplete_andRemoveFromPackedInPackedSection() {
    composeTestRule.setContent {
      HomelyTheme {
        val packedOverrides = remember { mutableStateMapOf<String, Boolean>() }
        PackingScreen(
          shops = sampleShops,
          orders = sampleOrders,
          routes = sampleRoutes,
          stocks = sampleStocks,
          onUpdateStock = { _, _, _ -> },
          onOpenSyncServers = {},
          onNavigateToRoutes = {},
          onNavigateToOrders = {},
          onShowFeedback = {},
          packedOrderOverrides = packedOverrides.toMap(),
          onUpdatePackedStatus = { orderId, _, isPacked ->
            packedOverrides[orderId] = isPacked
          },
        )
      }
    }

    composeTestRule.waitForIdle()
    val list = composeTestRule.onNodeWithTag("packing_lazy_list")
    list.performScrollToNode(hasTestTag("packing_complete_button_ord_r1"))
    composeTestRule.onNodeWithTag("packing_complete_button_ord_r1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("packing_complete_button_ord_r1").performClick()
    composeTestRule.waitForIdle()

    // Switch to Packed section and verify "Remove from Packed" asks for confirmation
    composeTestRule.onNodeWithTag("packing_filter_packed").performClick()
    composeTestRule.waitForIdle()
    list.performScrollToNode(hasTestTag("packing_remove_packed_ord_r1"))
    composeTestRule.onNodeWithTag("packing_remove_packed_ord_r1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("packing_remove_packed_ord_r1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("packing_remove_confirm_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("packing_cancel_remove_packed_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Packed (1)").assertIsDisplayed()

    composeTestRule.onNodeWithTag("packing_remove_packed_ord_r1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("packing_confirm_remove_packed_button").performClick()
    composeTestRule.waitForIdle()

    // After confirming removal from Packed, Packed filter has 0 items and Pending filter shows ord_r1 again
    composeTestRule.onNodeWithTag("packing_filter_packed").assertIsDisplayed()
    composeTestRule.onNodeWithText("Packed (0)").assertIsDisplayed()
    composeTestRule.onNodeWithTag("packing_filter_pending").performClick()
    composeTestRule.waitForIdle()
    list.performScrollToNode(hasTestTag("packing_shop_card_ord_r1"))
    composeTestRule.onNodeWithTag("packing_shop_card_ord_r1").assertIsDisplayed()
  }

  @Test
  fun packingScreen_sequentialOrder_andMarkPendingStockHoldAllowsNextOrderToProceed() {
    composeTestRule.setContent {
      HomelyTheme {
        val packedOverrides = remember { mutableStateMapOf<String, Boolean>() }
        val stockPendingReasons = remember { mutableStateMapOf<String, String>() }
        PackingScreen(
          shops = sampleShops,
          orders = sampleOrders,
          routes = sampleRoutes,
          stocks = sampleStocks,
          onUpdateStock = { _, _, _ -> },
          onOpenSyncServers = {},
          onNavigateToRoutes = {},
          onNavigateToOrders = {},
          onShowFeedback = {},
          packedOrderOverrides = packedOverrides.toMap(),
          onUpdatePackedStatus = { orderId, _, isPacked ->
            packedOverrides[orderId] = isPacked
            stockPendingReasons.remove(orderId)
          },
          stockPendingReasons = stockPendingReasons.toMap(),
          onMarkOrderStockPending = { orderId, _, reason ->
            packedOverrides[orderId] = false
            stockPendingReasons[orderId] = reason
          },
          onClearOrderStockPending = { orderId, _ ->
            stockPendingReasons.remove(orderId)
          },
        )
      }
    }

    composeTestRule.waitForIdle()
    val list = composeTestRule.onNodeWithTag("packing_lazy_list")

    // Second order must be locked until first order is packed or marked pending for stock
    list.performScrollToNode(hasTestTag("packing_complete_button_ord_r2"))
    composeTestRule.onNodeWithTag("packing_complete_button_ord_r2").assertIsNotEnabled()

    // First order is active in sequence: mark pending due to unavailable BC100 stock and proceed next
    list.performScrollToNode(hasTestTag("packing_hold_next_button_ord_r1"))
    composeTestRule.onNodeWithTag("packing_hold_next_button_ord_r1").assertIsEnabled()
    composeTestRule.onNodeWithTag("packing_hold_next_button_ord_r1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("packing_stock_pending_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("packing_stock_chip_BC100").performClick()
    composeTestRule.onNodeWithTag("packing_stock_pending_reason_input").performTextReplacement("Waiting 100g batch")
    composeTestRule.onNodeWithTag("packing_confirm_stock_pending_button").performClick()
    composeTestRule.waitForIdle()

    // Second order is now unlocked and can be marked Packing Complete
    list.performScrollToNode(hasTestTag("packing_complete_button_ord_r2"))
    composeTestRule.onNodeWithTag("packing_complete_button_ord_r2").assertIsEnabled()
    composeTestRule.onNodeWithTag("packing_complete_button_ord_r2").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Packed (1)").assertIsDisplayed()
  }

  @Test
  fun buildPackingSnapshot_enforcesSequenceAndUnlocksNextWhenMarkedStockPending() {
    val initial = buildPackingSnapshot(
      selectedRouteId = RoutesCatalog.ALL,
      shops = sampleShops,
      orders = sampleOrders,
      routes = sampleRoutes,
      stocks = sampleStocks,
      reverseLoadOrder = false,
    )
    assertEquals(true, initial.shopSlips[0].isNextInSequence)
    assertEquals(false, initial.shopSlips[0].isSequenceLocked)
    assertEquals(false, initial.shopSlips[1].isNextInSequence)
    assertEquals(true, initial.shopSlips[1].isSequenceLocked)
    assertEquals(1, initial.shopSlips[1].blockingPackOrderNumber)

    val afterHold = buildPackingSnapshot(
      selectedRouteId = RoutesCatalog.ALL,
      shops = sampleShops,
      orders = sampleOrders,
      routes = sampleRoutes,
      stocks = sampleStocks,
      reverseLoadOrder = false,
      stockPendingReasons = mapOf(initial.shopSlips[0].order.id to "BC100 unavailable"),
    )
    assertEquals(true, afterHold.shopSlips[0].isStockPending)
    assertEquals(false, afterHold.shopSlips[0].isSequenceLocked)
    assertEquals(true, afterHold.shopSlips[1].isNextInSequence)
    assertEquals(false, afterHold.shopSlips[1].isSequenceLocked)
    assertEquals(true, afterHold.shopSlips[2].isSequenceLocked)
    assertEquals(1, afterHold.stockPendingCount)
  }

  @Test
  fun packingScreen_quickEditOrderSheet_updatesQuantitiesNoteAndPackingComplete() {
    var updatedSummary = ""
    var updatedNote = ""

    composeTestRule.setContent {
      HomelyTheme {
        var ordersState by remember { mutableStateOf(sampleOrders) }
        val packedOverrides = remember { mutableStateMapOf<String, Boolean>() }
        PackingScreen(
          shops = sampleShops,
          orders = ordersState,
          routes = sampleRoutes,
          stocks = sampleStocks,
          onUpdateStock = { _, _, _ -> },
          onOpenSyncServers = {},
          onNavigateToRoutes = {},
          onNavigateToOrders = {},
          onShowFeedback = {},
          packedOrderOverrides = packedOverrides.toMap(),
          onUpdatePackedStatus = { orderId, _, isPacked ->
            packedOverrides[orderId] = isPacked
          },
          onUpdateOrder = { order, items, note, onSaved ->
            val summary = items.joinToString(", ") { (prod, qty) -> "${prod.code} x$qty" }
            val count = items.sumOf { it.second }
            val total = items.sumOf { (prod, qty) -> prod.defaultPrice * qty }
            updatedSummary = summary
            updatedNote = note
            ordersState = ordersState.map {
              if (it.id == order.id) {
                it.copy(itemsSummary = summary, itemCount = count, totalAmount = total, note = note)
              } else {
                it
              }
            }
            onSaved()
          },
        )
      }
    }

    composeTestRule.waitForIdle()
    val list = composeTestRule.onNodeWithTag("packing_lazy_list")
    list.performScrollToNode(hasTestTag("packing_edit_order_ord_r1"))
    composeTestRule.onNodeWithTag("packing_edit_order_ord_r1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("packing_quick_edit_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("packing_edit_plus5_BC100").performClick()
    composeTestRule.onNodeWithTag("packing_edit_note_input").performTextReplacement("Load in top crate")
    composeTestRule.onNodeWithTag("packing_edit_status_complete").performClick()
    composeTestRule.onNodeWithTag("packing_edit_save_button").performClick()
    composeTestRule.waitForIdle()

    assertTrue(updatedSummary.contains("BC100 x205"))
    assertEquals("Load in top crate", updatedNote)
    composeTestRule.onNodeWithText("Packed (1)").assertIsDisplayed()
  }
}
