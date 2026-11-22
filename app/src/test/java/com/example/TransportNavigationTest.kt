package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ServerConfigStore
import com.example.data.local.HomelyDatabase
import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.SnackCatalog
import com.example.data.remote.LaptopServerClient
import com.example.data.remote.OsrmRoutingClient
import com.example.data.repository.LocalTestRepository
import com.example.data.repository.SyncCoordinatorRepository
import com.example.ui.screens.TransportScreen
import com.example.ui.screens.TransportStopFilter
import com.example.ui.screens.buildTransportRoutePlanState
import com.example.ui.theme.HomelyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class TransportNavigationTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var roomDb: HomelyDatabase
  private lateinit var coordinator: SyncCoordinatorRepository

  private val sampleRoute = DeliveryRoute(
    id = "KL-07-KC04",
    name = "Route #KC-04: Kochi North",
    vehicle = "Tata Ace (KL-07-AB-1234)",
    maxCrates = 50,
    driverName = "Rajan K.",
    driverPhone = "+91 98450 11223",
    stagingBay = "Bay #03 Departed",
    dispatchWindow = "06:30 AM - 01:30 PM",
    isActive = true,
    stopOrderIds = listOf("s_edappally", "s_toll", "s_pathadipalam", "s_priya"),
  )

  private val sampleShops = listOf(
    Shop(
      id = "s_priya",
      shopCode = "SHOP-000129",
      name = "Priya Supermarket",
      ownerName = "K. Varghese",
      phone = "9845210982",
      address = "Premier Junction, Kalamassery",
      pinCode = "683104",
      routeId = "KL-07-KC04",
      latitude = 10.0561,
      longitude = 76.3225,
    ),
    Shop(
      id = "s_edappally",
      shopCode = "SHOP-000126",
      name = "Edappally Green Valley Stores",
      ownerName = "Rajesh Kumar",
      phone = "9845001122",
      address = "Edappally Bypass",
      pinCode = "682024",
      routeId = "KL-07-KC04",
      latitude = 10.0261,
      longitude = 76.3125,
    ),
    Shop(
      id = "s_toll",
      shopCode = "SHOP-000127",
      name = "Toll Fresh Mart",
      ownerName = "Anil Menon",
      phone = "9845002233",
      address = "Edappally Toll",
      pinCode = "682024",
      routeId = "KL-07-KC04",
      latitude = 10.0325,
      longitude = 76.3150,
    ),
    Shop(
      id = "s_pathadipalam",
      shopCode = "SHOP-000128",
      name = "Pathadipalam Daily Needs",
      ownerName = "Suresh Nair",
      phone = "9845003344",
      address = "Pathadipalam",
      pinCode = "682024",
      routeId = "KL-07-KC04",
      latitude = 10.0442,
      longitude = 76.3189,
    ),
  )

  private val sampleOrders = listOf(
    OrderRecord(
      id = "ord_1",
      orderCode = "ORD-2026-00126",
      shopId = "s_edappally",
      shopCode = "SHOP-000126",
      shopName = "Edappally Green Valley Stores",
      routeId = "KL-07-KC04",
      itemsSummary = "20 × Banana Chips",
      totalAmount = 2400.0,
      itemCount = 20,
      status = OrderStatus.PENDING,
      note = "COD",
    ),
    OrderRecord(
      id = "ord_2",
      orderCode = "ORD-2026-00127",
      shopId = "s_toll",
      shopCode = "SHOP-000127",
      shopName = "Toll Fresh Mart",
      routeId = "KL-07-KC04",
      itemsSummary = "15 × Murukku",
      totalAmount = 1500.0,
      itemCount = 15,
      status = OrderStatus.CONFIRMED,
      note = "Prepaid (UPI Verified)",
    ),
    OrderRecord(
      id = "ord_3",
      orderCode = "ORD-2026-00128",
      shopId = "s_pathadipalam",
      shopCode = "SHOP-000128",
      shopName = "Pathadipalam Daily Needs",
      routeId = "KL-07-KC04",
      itemsSummary = "25 × Banana Chips",
      totalAmount = 3000.0,
      itemCount = 25,
      status = OrderStatus.PENDING,
      note = "Cheque",
    ),
    OrderRecord(
      id = "ord_4",
      orderCode = "ORD-2026-00129",
      shopId = "s_priya",
      shopCode = "SHOP-000129",
      shopName = "Priya Supermarket",
      routeId = "KL-07-KC04",
      itemsSummary = "30 × Banana Chips",
      totalAmount = 4200.0,
      itemCount = 30,
      status = OrderStatus.PENDING,
      note = "Cash COD",
    ),
  )

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    roomDb = Room.inMemoryDatabaseBuilder(context, HomelyDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    coordinator = SyncCoordinatorRepository(
      appContext = context,
      primary = LocalTestRepository(),
      dao = roomDb.homelyDao(),
      configStore = ServerConfigStore(context),
      laptopClient = LaptopServerClient(),
      currentUserId = LocalTestRepository.LOCAL_UID,
      isLocalTestMode = true,
    )
  }

  @After
  fun tearDown() {
    roomDb.close()
  }

  @Test
  fun sequentialStopPlan_ordersShopsByRouteSequenceAndPrioritizesUserToStop1() {
    val userLat = 10.0150
    val userLng = 76.3050

    val plan = buildTransportRoutePlanState(
      route = sampleRoute,
      allShops = sampleShops,
      allOrders = sampleOrders,
      userLatitude = userLat,
      userLongitude = userLng,
    )

    // Must respect Route Planning orderIds 1, 2, 3, 4 rather than input shop list order
    assertEquals(
      listOf("s_edappally", "s_toll", "s_pathadipalam", "s_priya"),
      plan.stops.map { it.shop.id },
    )
    assertEquals(listOf(1, 2, 3, 4), plan.stops.map { it.stopNumber })

    // Priority target is Stop 01
    assertEquals("s_edappally", plan.activeTargetStop?.shop?.id)
    assertEquals(4, plan.totalStops)
    assertEquals(0, plan.completedCount)
    assertEquals(4, plan.pendingCount)

    // Waypoints start at User GPS -> Stop 1 -> Stop 2 -> Stop 3 -> Stop 4
    assertTrue(plan.hasUserOriginForRouting)
    assertEquals(5, plan.orderedWaypointsForMap.size)
    assertEquals(userLat to userLng, plan.orderedWaypointsForMap[0])
    assertEquals(10.0261 to 76.3125, plan.orderedWaypointsForMap[1])

    // Completing Stop 01 shifts priority target cleanly to Stop 02 while preserving numbers 1..4
    val afterStop1 = buildTransportRoutePlanState(
      route = sampleRoute,
      allShops = sampleShops,
      allOrders = sampleOrders,
      locallyCompletedShopIds = setOf("s_edappally"),
      localPodNotesByShopId = mapOf("s_edappally" to "POD Signed by Rajesh"),
      userLatitude = userLat,
      userLongitude = userLng,
    )
    assertEquals("s_toll", afterStop1.activeTargetStop?.shop?.id)
    assertEquals(2, afterStop1.activeTargetStop?.stopNumber)
    assertEquals(1, afterStop1.completedCount)
    assertEquals(3, afterStop1.pendingCount)
    assertEquals(4, afterStop1.orderedWaypointsForMap.size)
  }

  @Test
  fun updateOrderStatus_persistsDeliveredToRoomAndAdvancesRouteProgress() = runBlocking {
    val shop = sampleShops.first { it.id == "s_edappally" }
    coordinator.saveShop(shop)
    coordinator.placeOrder(
      shop = shop,
      items = listOf(SnackCatalog.products.first() to 10),
      note = "COD",
    )

    val initialOrders = coordinator.observeOrders(LocalTestRepository.LOCAL_UID).first()
    val createdOrder = initialOrders.first { it.shopId == "s_edappally" }
    assertEquals(OrderStatus.CONFIRMED, createdOrder.status)

    val res = coordinator.updateOrderStatus(
      orderId = createdOrder.id,
      newStatus = OrderStatus.DELIVERED,
      note = "POD Signed by Rajesh Kumar • Cash Collected",
    )
    assertTrue(res.isSuccess)

    val updatedOrders = coordinator.observeOrders(LocalTestRepository.LOCAL_UID).first()
    val deliveredOrder = updatedOrders.first { it.id == createdOrder.id }
    assertEquals(OrderStatus.DELIVERED, deliveredOrder.status)
    assertEquals("POD Signed by Rajesh Kumar • Cash Collected", deliveredOrder.note)
  }

  @Test
  fun osrmRoutingClient_parsesTurnByTurnGeoJsonAndComputesOfflineFallback() {
    val mockOsrmJson = """
      {
        "code": "Ok",
        "routes": [{
          "distance": 4520.0,
          "duration": 540.0,
          "geometry": {
            "type": "LineString",
            "coordinates": [[76.3050, 10.0150], [76.3125, 10.0261], [76.3150, 10.0325]]
          },
          "legs": [
            {
              "distance": 1820.0,
              "duration": 210.0,
              "steps": [
                {
                  "distance": 450.0,
                  "duration": 60.0,
                  "name": "Pukkattupady Rd",
                  "geometry": { "type": "LineString", "coordinates": [[76.3050, 10.0150], [76.3090, 10.0200]] },
                  "maneuver": { "type": "turn", "modifier": "right", "location": [76.3050, 10.0150] }
                },
                {
                  "distance": 1370.0,
                  "duration": 150.0,
                  "name": "NH 47 Bypass",
                  "geometry": { "type": "LineString", "coordinates": [[76.3090, 10.0200], [76.3125, 10.0261]] },
                  "maneuver": { "type": "arrive", "modifier": "straight", "location": [76.3125, 10.0261] }
                }
              ]
            },
            {
              "distance": 2700.0,
              "duration": 330.0,
              "steps": [
                {
                  "distance": 2700.0,
                  "duration": 330.0,
                  "name": "Edappally Toll Rd",
                  "geometry": { "type": "LineString", "coordinates": [[76.3125, 10.0261], [76.3150, 10.0325]] },
                  "maneuver": { "type": "continue", "modifier": "straight", "location": [76.3125, 10.0261] }
                }
              ]
            }
          ]
        }]
      }
    """.trimIndent()

    val waypoints = listOf(
      10.0150 to 76.3050,
      10.0261 to 76.3125,
      10.0325 to 76.3150,
    )
    val parsed = OsrmRoutingClient.parseOsrmRouteResponse(
      jsonText = mockOsrmJson,
      fallbackWaypoints = waypoints,
      hasUserOrigin = true,
      primaryStopName = "Edappally",
    )
    assertNotNull(parsed)
    assertTrue(parsed!!.isRoadNetworkRoute)
    assertEquals(1820.0, parsed.activeLegDistanceMeters, 1.0)
    assertEquals(3, parsed.activeLegGeometry.size)
    assertEquals(2, parsed.upcomingGeometry.size)
    assertEquals("Turn right onto Pukkattupady Rd", parsed.activeLegSteps.first().instruction)

    val fallback = OsrmRoutingClient.buildFallbackRoute(
      waypoints = waypoints,
      hasUserOrigin = true,
      primaryStopName = "Edappally",
    )
    assertFalse(fallback.isRoadNetworkRoute)
    assertTrue(fallback.activeLegDistanceMeters > 1000.0)
    assertTrue(fallback.activeLegSteps.first().instruction.contains("Edappally"))
  }

  @Test
  fun transportScreen_rendersFullDashboardAndAllowsFilterAndRouteSelection() {
    composeTestRule.setContent {
      HomelyTheme {
        TransportScreen(
          shops = sampleShops,
          orders = sampleOrders,
          routes = listOf(sampleRoute),
          userDisplayName = "Arun Kumar",
          onMarkOrderDelivered = { _, _, _ -> },
          onNavigateToRoutes = {},
          onOpenSyncServers = {},
          onShowFeedback = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("HOMELY BY HOMIE").assertIsDisplayed()
    composeTestRule.onNodeWithTag("transport_route_selector").assertIsDisplayed()
    composeTestRule.onNodeWithTag("real_osm_map_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("transport_turn_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("transport_navigate_primary_btn").assertIsDisplayed()

    // Check filter interaction
    composeTestRule.onNodeWithTag("transport_filter_${TransportStopFilter.PENDING.name.lowercase()}")
      .performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun transportScreen_enforcesSequentialDeliveryCompletion() {
    val deliveredOrderIds = mutableListOf<String>()
    val feedbackMessages = mutableListOf<String>()

    composeTestRule.setContent {
      HomelyTheme {
        TransportScreen(
          shops = sampleShops,
          orders = sampleOrders,
          routes = listOf(sampleRoute),
          userDisplayName = "Arun Kumar",
          onMarkOrderDelivered = { orderId, _, _ -> deliveredOrderIds += orderId },
          onNavigateToRoutes = {},
          onOpenSyncServers = {},
          onShowFeedback = { msg -> feedbackMessages += msg },
        )
      }
    }

    composeTestRule.waitForIdle()
    val stopList = composeTestRule.onNodeWithTag("transport_stop_list")

    // Stop 01 is active and has enabled Mark Delivered button
    stopList.performScrollToNode(hasTestTag("transport_upcoming_stop_1"))
    composeTestRule.onNodeWithTag("transport_upcoming_stop_1").assertIsDisplayed()
    stopList.performScrollToNode(hasTestTag("transport_mark_delivered_1"))
    composeTestRule.onNodeWithTag("transport_mark_delivered_1").assertIsDisplayed()

    // Select Stop 02: must NOT allow Mark Delivered out of order; shows locked sequential indicator instead
    stopList.performScrollToNode(hasTestTag("transport_upcoming_stop_2"))
    composeTestRule.onNodeWithTag("transport_upcoming_stop_2").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("transport_mark_delivered_2").assertDoesNotExist()
    stopList.performScrollToNode(hasTestTag("transport_locked_stop_2"))
    composeTestRule.onNodeWithTag("transport_locked_stop_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("transport_locked_stop_2").performClick()
    composeTestRule.waitForIdle()
    assertTrue(deliveredOrderIds.isEmpty())
    assertTrue(feedbackMessages.any { it.contains("01") })

    // Complete Stop 01 in sequence -> unlocks Stop 02 automatically
    stopList.performScrollToNode(hasTestTag("transport_mark_delivered_1"))
    composeTestRule.onNodeWithTag("transport_mark_delivered_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm_pod_delivered_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(listOf("ord_1"), deliveredOrderIds)
    stopList.performScrollToNode(hasTestTag("transport_mark_delivered_2"))
    composeTestRule.onNodeWithTag("transport_mark_delivered_2").assertIsDisplayed()
  }

  @Test
  fun deliveryPodDialog_opensAndConfirms() {
    val stop = buildTransportRoutePlanState(
      route = sampleRoute,
      allShops = sampleShops,
      allOrders = sampleOrders,
    ).stops.first()
    var confirmedNote: String? = null
    composeTestRule.setContent {
      HomelyTheme {
        com.example.ui.screens.DeliveryPodDialog(
          stop = stop,
          onConfirmDelivered = { confirmedNote = it },
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm_pod_delivered_button").performClick()
    composeTestRule.waitForIdle()
    assertNotNull(confirmedNote)
  }

  @Test
  fun shortestPathDefaultOrdering_picksClosestChainRegardlessOfManualStopOrderIds() {
    // User is next to Priya Supermarket (10.0561, 76.3225), while manual stopOrderIds starts at Edappally (south end).
    // Shortest path from north end must run southwards: Priya -> Pathadipalam -> Toll -> Edappally.
    val northPlan = buildTransportRoutePlanState(
      route = sampleRoute,
      allShops = sampleShops,
      allOrders = sampleOrders,
      userLatitude = 10.0580,
      userLongitude = 76.3230,
    )
    assertTrue(northPlan.isShortestPathMode)
    assertEquals(
      listOf("s_priya", "s_pathadipalam", "s_toll", "s_edappally"),
      northPlan.stops.map { it.shop.id },
    )
    assertEquals(listOf(1, 2, 3, 4), northPlan.stops.map { it.stopNumber })

    // Disabling shortest path restores manual stopOrderIds order
    val manualPlan = buildTransportRoutePlanState(
      route = sampleRoute,
      allShops = sampleShops,
      allOrders = sampleOrders,
      userLatitude = 10.0580,
      userLongitude = 76.3230,
      useShortestPath = false,
    )
    assertFalse(manualPlan.isShortestPathMode)
    assertEquals(
      listOf("s_edappally", "s_toll", "s_pathadipalam", "s_priya"),
      manualPlan.stops.map { it.shop.id },
    )
  }

  @Test
  fun transportScreen_showsAllShopsToDeliverAndStartNavigationOpensNumberedMap() {
    composeTestRule.setContent {
      HomelyTheme {
        TransportScreen(
          shops = sampleShops,
          orders = sampleOrders,
          routes = listOf(sampleRoute),
          userDisplayName = "Arun Kumar",
          onMarkOrderDelivered = { _, _, _ -> },
          onNavigateToRoutes = {},
          onOpenSyncServers = {},
          onShowFeedback = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    val stopList = composeTestRule.onNodeWithTag("transport_stop_list")

    // All 4 shops must be in the list immediately without truncation or expand button
    composeTestRule.onNodeWithTag("transport_expand_upcoming_btn").assertDoesNotExist()
    stopList.performScrollToNode(hasTestTag("transport_upcoming_stop_4"))
    composeTestRule.onNodeWithTag("transport_upcoming_stop_4").assertIsDisplayed()

    // Start Navigation button opens fullscreen map immediately with numbered chips 1, 2, 3, 4
    composeTestRule.onNodeWithText("Start Navigation").assertIsDisplayed()
    composeTestRule.onNodeWithTag("transport_navigate_primary_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("transport_exit_fullscreen_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_stop_chip_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_stop_chip_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_stop_chip_3").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_stop_chip_4").assertIsDisplayed()

    // Navigate forward to Shop 2 using Next button
    composeTestRule.onNodeWithTag("nav_next_stop_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_jump_active_stop_button").assertIsDisplayed()

    // Jump back to active Stop 1 via chip
    composeTestRule.onNodeWithTag("nav_stop_chip_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fullscreen_mark_delivered_button").assertIsDisplayed()
  }
}
