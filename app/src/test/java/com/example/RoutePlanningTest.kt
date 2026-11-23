package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ServerConfigStore
import com.example.data.local.HomelyDatabase
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.remote.LaptopServerClient
import com.example.data.repository.LocalTestRepository
import com.example.data.repository.SyncCoordinatorRepository
import com.example.ui.components.MapCameraState
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.computeFitBoundsCamera
import com.example.ui.components.prepareMapRenderData
import com.example.ui.components.projectLatLngToScreen
import com.example.ui.screens.EditRouteBottomSheet
import com.example.ui.screens.RoutePlanScreen
import com.example.ui.screens.TransportScreen
import com.example.ui.screens.createInitialMapRouteEditorDraft
import com.example.ui.screens.formatVehicleSummary
import com.example.ui.screens.parseVehicleSummary
import com.example.ui.screens.toggleShopSelection
import com.example.ui.screens.undoLastChange
import com.example.ui.screens.withTargetRoute
import com.example.ui.theme.RouteColorPalette
import com.example.ui.theme.RouteComboPattern
import com.example.ui.theme.HomelyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class RoutePlanningTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var roomDb: HomelyDatabase
  private lateinit var coordinator: SyncCoordinatorRepository

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
  fun routeColorPalette_usesUniqueSolidsThenDistinctCombinations() {
    val used = mutableSetOf<Int>()
    val solidColors = mutableSetOf<androidx.compose.ui.graphics.Color>()
    // First 5 routes get distinct solid colors
    repeat(RouteColorPalette.solidCount) {
      val nextIdx = RouteColorPalette.nextUnusedColorIndex(used)
      val spec = RouteColorPalette.specForIndex(nextIdx)
      assertFalse(spec.isCombination)
      assertEquals(RouteComboPattern.SOLID, spec.pattern)
      assertTrue(spec.primaryHex.startsWith("#") && spec.primaryHex.length == 7)
      solidColors.add(spec.primary)
      used.add(nextIdx)
    }
    assertEquals(RouteColorPalette.solidCount, solidColors.size)

    // Subsequent routes automatically receive distinct dual-color combinations across multiple patterns
    val comboSpecs = mutableListOf<com.example.ui.theme.RouteColorSpec>()
    repeat(80) {
      val nextIdx = RouteColorPalette.nextUnusedColorIndex(used)
      val spec = RouteColorPalette.specForIndex(nextIdx)
      assertTrue(spec.isCombination)
      assertTrue(spec.secondary != null && spec.secondary != spec.primary)
      assertTrue(spec.secondaryHex != null && spec.secondaryHex != spec.primaryHex)
      used.add(nextIdx)
      comboSpecs += spec
    }

    assertEquals(comboSpecs.size, comboSpecs.map { it.label }.toSet().size)
    // Verify all 4 combination patterns are used across overflow routes
    assertEquals(
      setOf(
        RouteComboPattern.SPLIT_HALF,
        RouteComboPattern.RING_CORE,
        RouteComboPattern.DIAGONAL_STRIPE,
        RouteComboPattern.TOP_BOTTOM_DUO,
      ),
      comboSpecs.map { it.pattern }.toSet(),
    )

    // Even beyond all preset combinations, overflow deterministically stays in contrasting combos
    val overflowIdx = RouteColorPalette.nextUnusedColorIndex(used)
    val overflowSpec = RouteColorPalette.specForIndex(overflowIdx)
    assertTrue(overflowSpec.isCombination)
    assertTrue(overflowSpec.secondary != null && overflowSpec.secondary != overflowSpec.primary)
  }

  @Test
  fun saveNewRouteAndReassignShopOnMap_persistsAndReflectsImmediately() = runBlocking {
    coordinator.seedRoutesIfNeeded()
    val initialRoutes = coordinator.observeRoutes().first()
    assertEquals(3, initialRoutes.size)

    val usedColors = initialRoutes.map { it.colorIndex }.toSet()
    val newRoute = DeliveryRoute(
      id = "R04-FORT-KOCHI",
      name = "Route 4: Fort Kochi",
      vehicle = "Eicher Pro 2049 (KL-07-BX-4450)",
      maxCrates = 60,
      driverName = "Anand V.",
      driverPhone = "+91 98450 99887",
      colorIndex = RouteColorPalette.nextUnusedColorIndex(usedColors),
    )
    coordinator.saveRoute(newRoute)

    val updatedRoutes = coordinator.observeRoutes().first()
    assertEquals(4, updatedRoutes.size)
    assertTrue(updatedRoutes.any { it.id == "R04-FORT-KOCHI" })

    // Reassign a shop to Route 1
    coordinator.saveShop(
      Shop(
        id = "shop_map_test",
        shopCode = "SHP-900",
        name = "Priya Supermarket",
        ownerName = "K. Varghese",
        phone = "9845210982",
        address = "Edappally Toll",
        pinCode = "682024",
        routeId = RoutesCatalog.UNALLOCATED,
      )
    )
    coordinator.updateShopRoute("shop_map_test", RoutesCatalog.R01_KANNUR_BANGALORE)

    val visibleShops = coordinator.observeShops(LocalTestRepository.LOCAL_UID).first()
    val updatedShop = visibleShops.first { it.id == "shop_map_test" }
    assertEquals(RoutesCatalog.R01_KANNUR_BANGALORE, updatedShop.routeId)
  }

  @Test
  fun mapProjectionAndFitBounds_framePointsInsideViewport() {
    val camera = MapCameraState(centerLat = 11.8745, centerLng = 75.3704, zoom = 11f)
    val centerPt = projectLatLngToScreen(11.8745, 75.3704, camera, widthPx = 800f, heightPx = 600f)
    assertEquals(400f, centerPt.x, 0.5f)
    assertEquals(300f, centerPt.y, 0.5f)

    val emptyCam = computeFitBoundsCamera(emptyList(), widthPx = 800f, heightPx = 600f)
    assertTrue(emptyCam.zoom in 4f..14f)

    val routePoints = listOf(
      11.8745 to 75.3704, // Kannur
      12.9716 to 77.5946, // Bengaluru
      11.2588 to 75.7804, // Kozhikode
    )
    val fitted = computeFitBoundsCamera(
      routePoints,
      widthPx = 800f,
      heightPx = 600f,
      applyOverlayVerticalShift = false,
      worldScale = 3f,
    )
    assertTrue(fitted.zoom in 4.0f..15.0f)
    for ((lat, lng) in routePoints) {
      val pt = projectLatLngToScreen(lat, lng, fitted, widthPx = 800f, heightPx = 600f, worldScale = 3f)
      assertTrue(pt.x.isFinite() && pt.y.isFinite())
      assertTrue("Expected x inside viewport: ${pt.x}", pt.x in 0f..800f)
      assertTrue("Expected y inside viewport: ${pt.y}", pt.y in 0f..600f)
    }
  }

  @Test
  fun realOsmMapView_rendersNativeCanvasAndZoomControlsWithoutWebView() {
    val sampleRoutes = listOf(
      DeliveryRoute(
        id = RoutesCatalog.R01_KANNUR_BANGALORE,
        name = "Route 1: Kannur - Bangalore",
        vehicle = "KL-13-AB-1122",
        maxCrates = 40,
        colorIndex = 0,
      ),
      DeliveryRoute(
        id = RoutesCatalog.R02_KANNUR_KOZHIKODE,
        name = "Route 2: Kannur - Kozhikode",
        vehicle = "KL-07-CB-9081",
        maxCrates = 40,
        colorIndex = 3,
      ),
    )
    val sampleShops = listOf(
      Shop(
        id = "s1",
        shopCode = "SHP-001",
        name = "Kannur Fresh Bakery",
        ownerName = "Rajan",
        phone = "9845011223",
        address = "Fort Road, Kannur",
        pinCode = "670001",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        latitude = 11.8745,
        longitude = 75.3704,
      ),
      Shop(
        id = "s2",
        shopCode = "SHP-002",
        name = "Priya Supermarket",
        ownerName = "K. Varghese",
        phone = "9845210982",
        address = "Edappally Toll",
        pinCode = "682024",
        routeId = RoutesCatalog.UNALLOCATED,
        latitude = 10.0261,
        longitude = 76.3125,
      ),
    )

    composeTestRule.setContent {
      HomelyTheme {
        Box(modifier = Modifier.size(width = 360.dp, height = 420.dp)) {
          RealOsmMapView(
            shops = sampleShops,
            routes = sampleRoutes,
            selectedRouteId = RoutesCatalog.ALL,
            selectedShopId = null,
            showPolylines = true,
            userLatitude = 11.8745,
            userLongitude = 75.3704,
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("real_osm_map_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("map_zoom_in_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("map_zoom_out_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("map_zoom_in_button").performClick()
    composeTestRule.onNodeWithTag("map_zoom_out_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun sortShopsByRouteSequence_andPersistStopOrderIds_worksCleanly() = runBlocking {
    val shops = listOf(
      Shop(id = "a", shopCode = "SHP-01", name = "Alpha", routeId = RoutesCatalog.R01_KANNUR_BANGALORE),
      Shop(id = "b", shopCode = "SHP-02", name = "Beta", routeId = RoutesCatalog.R01_KANNUR_BANGALORE),
      Shop(id = "c", shopCode = "SHP-03", name = "Gamma", routeId = RoutesCatalog.R01_KANNUR_BANGALORE),
    )
    val customRoute = DeliveryRoute(
      id = RoutesCatalog.R01_KANNUR_BANGALORE,
      name = "Route 1: Kannur - Bangalore",
      stopOrderIds = listOf("c", "a", "b"),
    )
    coordinator.saveRoute(customRoute)

    val saved = coordinator.observeRoutes().first().first { it.id == RoutesCatalog.R01_KANNUR_BANGALORE }
    assertEquals(listOf("c", "a", "b"), saved.stopOrderIds)

    val ordered = RoutesCatalog.sortShopsByRouteSequence(shops, saved)
    assertEquals(listOf("c", "a", "b"), ordered.map { it.id })
  }

  @Test
  fun vehicleSummaryFormatAndParse_roundTripsCustomDetailsAndPlate() {
    val formatted = formatVehicleSummary(
      modelName = "Ashok Leyland Bada Dost",
      plateNumber = "kl-13-z-9988",
      extraDetails = "Insulated Reefer • Permit #KL41",
    )
    assertEquals(
      "Ashok Leyland Bada Dost (KL-13-Z-9988) • Insulated Reefer, Permit #KL41",
      formatted,
    )

    val parsed = parseVehicleSummary(formatted)
    assertEquals("Ashok Leyland Bada Dost", parsed.modelName)
    assertEquals("KL-13-Z-9988", parsed.plateNumber)
    assertEquals("Insulated Reefer, Permit #KL41", parsed.extraDetails)
    assertEquals("Reefer Van", parsed.vehicleTypeTag)

    val plateOnly = parseVehicleSummary("KL-13-AB-1122")
    assertEquals("Delivery Vehicle", plateOnly.modelName)
    assertEquals("KL-13-AB-1122", plateOnly.plateNumber)
  }

  @Test
  fun editRouteBottomSheet_allowsCustomVehicleNumberDetailsCapacityAndPreservesStops() = runBlocking {
    val initialRoute = DeliveryRoute(
      id = RoutesCatalog.R01_KANNUR_BANGALORE,
      name = "Route 1: Kannur - Bangalore",
      vehicle = "Tata Ace (KL-07-CD-4190)",
      maxCrates = 32,
      driverName = "Rajan Kumar",
      driverPhone = "+91 98452 10980",
      stopOrderIds = listOf("shop_1", "shop_2"),
    )
    var savedRoute: DeliveryRoute? = null

    composeTestRule.setContent {
      HomelyTheme {
        EditRouteBottomSheet(
          initialRoute = initialRoute,
          usedColorIndices = emptySet(),
          assignedCratesCount = 2,
          nextRouteNumber = 4,
          onDismiss = {},
          onSaveRoute = { savedRoute = it },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("modal_vehicle_model_input").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("modal_vehicle_model_input").performTextInput("Bada Dost i4")
    composeTestRule.onNodeWithTag("modal_vehicle_number_input").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("modal_vehicle_number_input").performTextInput("kl-13-ab-7788")
    composeTestRule.onNodeWithTag("modal_vehicle_capacity_input").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("modal_vehicle_capacity_input").performTextInput("48")
    composeTestRule.onNodeWithTag("modal_vehicle_details_input").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("modal_vehicle_details_input").performTextInput("Insulated Cold Box")
    composeTestRule.onNodeWithTag("save_route_modal_button").performClick()
    composeTestRule.waitForIdle()

    val result = savedRoute!!
    assertEquals("Bada Dost i4 (KL-13-AB-7788) • Insulated Cold Box", result.vehicle)
    assertEquals(48, result.maxCrates)
    assertEquals(listOf("shop_1", "shop_2"), result.stopOrderIds)

    coordinator.saveRoute(result)
    val persisted = coordinator.observeRoutes().first().first { it.id == initialRoute.id }
    assertEquals("Bada Dost i4 (KL-13-AB-7788) • Insulated Cold Box", persisted.vehicle)
    assertEquals(48, persisted.maxCrates)
    assertEquals(listOf("shop_1", "shop_2"), persisted.stopOrderIds)
  }

  @Test
  fun transportScreen_displaysVehicleDetailsAndOpensVehicleEditor() {
    val routeWithDetails = DeliveryRoute(
      id = "R01-KANNUR-BANGALORE",
      name = "Route 1: Kannur - Bangalore",
      vehicle = "Eicher Pro 2049 (KL-07-BX-4450) • Insulated Reefer",
      maxCrates = 60,
      driverName = "Rajan Kumar",
      isActive = true,
    )
    var updatedVehicle: String? = null

    composeTestRule.setContent {
      HomelyTheme {
        TransportScreen(
          shops = emptyList(),
          orders = emptyList(),
          routes = listOf(routeWithDetails),
          userDisplayName = "Rajan Kumar",
          onMarkOrderDelivered = { _, _, _ -> },
          onNavigateToRoutes = {},
          onOpenSyncServers = {},
          onShowFeedback = {},
          onSaveRoute = { route, _ -> updatedVehicle = route.vehicle },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Eicher Pro 2049").assertIsDisplayed()
    composeTestRule.onNodeWithText("KL-07-BX-4450").assertIsDisplayed()
    composeTestRule.onNodeWithText("Insulated Reefer").assertIsDisplayed()

    composeTestRule.onNodeWithTag("transport_edit_vehicle_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("modal_vehicle_number_input").performScrollTo().performTextClearance()
    composeTestRule.onNodeWithTag("modal_vehicle_number_input").performTextInput("KL-11-CC-5566")
    composeTestRule.onNodeWithTag("save_route_modal_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals("Eicher Pro 2049 (KL-11-CC-5566) • Insulated Reefer", updatedVehicle)
  }

  @Test
  fun routePlanScreen_showsRoutePillsOnMapAndAllowsDirectUnallocatedAssign() {
    val routes = listOf(
      DeliveryRoute(
        id = RoutesCatalog.R01_KANNUR_BANGALORE,
        name = "Route 1: Kannur - Bangalore",
        vehicle = "KL-13-AB-1122",
        maxCrates = 40,
        colorIndex = 0,
      ),
      DeliveryRoute(
        id = RoutesCatalog.R02_KANNUR_KOZHIKODE,
        name = "Route 2: Kannur - Kozhikode",
        vehicle = "KL-07-CB-9081",
        maxCrates = 40,
        colorIndex = 3,
      ),
    )
    val shops = listOf(
      Shop(
        id = "s_unalloc_1",
        shopCode = "SHP-101",
        name = "New Corner Bakery",
        ownerName = "Rajan",
        phone = "9845011223",
        address = "MG Road, Kochi",
        pinCode = "682016",
        routeId = RoutesCatalog.UNALLOCATED,
        latitude = 9.9675,
        longitude = 76.2901,
      ),
      Shop(
        id = "s_unalloc_2",
        shopCode = "SHP-102",
        name = "Metro Fresh Mart",
        ownerName = "Anil",
        phone = "9845099887",
        address = "Edappally",
        pinCode = "682024",
        routeId = RoutesCatalog.UNALLOCATED,
        latitude = 10.0261,
        longitude = 76.3125,
      ),
    )
    val assignedEvents = mutableListOf<Pair<String, String>>()
    val batchEvents = mutableListOf<Pair<List<String>, String>>()

    composeTestRule.setContent {
      HomelyTheme {
        RoutePlanScreen(
          shops = shops,
          routes = routes,
          onSaveRoute = { _, _ -> },
          onUpdateShopRoute = { shopId, routeId, _ -> assignedEvents += (shopId to routeId) },
          onBulkAssignShopsToRoute = { ids, routeId, _ -> batchEvents += (ids to routeId) },
        )
      }
    }

    composeTestRule.waitForIdle()
    // Route names are rendered directly inside the map viewport pill strip
    composeTestRule.onNodeWithTag("map_route_name_pills_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("map_route_pill_${RoutesCatalog.R01_KANNUR_BANGALORE}").assertIsDisplayed()
    composeTestRule.onNodeWithTag("map_route_pill_${RoutesCatalog.R02_KANNUR_KOZHIKODE}").assertIsDisplayed()

    // Open Unallocated tab and assign directly inside the unallocated shop card
    composeTestRule.onNodeWithTag("dispatch_tab_unallocated").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("unallocated_shop_card_SHP-101").performScrollTo()
    composeTestRule.onNodeWithTag("unalloc_assign_SHP-101_${RoutesCatalog.R01_KANNUR_BANGALORE}")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()
    assertEquals(listOf("s_unalloc_1" to RoutesCatalog.R01_KANNUR_BANGALORE), assignedEvents)

    // Batch select SHP-102 and assign via batch bar
    composeTestRule.onNodeWithTag("unallocated_shop_card_SHP-102").performScrollTo()
    composeTestRule.onNodeWithTag("unalloc_check_SHP-102").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("unallocated_batch_assign_bar").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("batch_assign_route_${RoutesCatalog.R02_KANNUR_KOZHIKODE}")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()
    assertEquals(
      listOf(listOf("s_unalloc_2") to RoutesCatalog.R02_KANNUR_KOZHIKODE),
      batchEvents,
    )
  }

  @Test
  fun allShopsMapMode_numbersAllShopsAcrossAllRoutesSequentially() {
    val routes = listOf(
      DeliveryRoute(id = "R1", name = "Route 1", colorIndex = 0, stopOrderIds = listOf("s1", "s2")),
      DeliveryRoute(id = "R2", name = "Route 2", colorIndex = 1, stopOrderIds = listOf("s3", "s4")),
      DeliveryRoute(id = "R3", name = "Route 3", colorIndex = 2, stopOrderIds = listOf("s5")),
    )
    val shops = listOf(
      Shop(id = "s1", shopCode = "S1", name = "Shop 1", routeId = "R1", latitude = 11.87, longitude = 75.37),
      Shop(id = "s2", shopCode = "S2", name = "Shop 2", routeId = "R1", latitude = 11.90, longitude = 75.40),
      Shop(id = "s3", shopCode = "S3", name = "Shop 3", routeId = "R2", latitude = 11.50, longitude = 75.60),
      Shop(id = "s4", shopCode = "S4", name = "Shop 4", routeId = "R2", latitude = 11.25, longitude = 75.78),
      Shop(id = "s5", shopCode = "S5", name = "Shop 5", routeId = "R3", latitude = 11.68, longitude = 76.13),
      Shop(id = "s6", shopCode = "S6", name = "Shop 6", routeId = RoutesCatalog.UNALLOCATED, latitude = 10.02, longitude = 76.31),
    )

    val (pins, tracks) = prepareMapRenderData(
      shops = shops,
      routes = routes,
      selectedRouteId = RoutesCatalog.ALL,
      editingTargetRouteId = null,
    )

    assertEquals(6, pins.size)
    assertEquals(listOf(1, 2, 3, 4, 5, 6), pins.map { it.stopNumber })
    // R1 and R2 have >=2 stops so both route polylines appear
    assertEquals(listOf("R1", "R2"), tracks.map { it.route.id })

    // In Edit Mode for R2, ALL 6 shops stay visible, R2 stops are highlighted & numbered 1..2, others dimmed
    val (editPins, editTracks) = prepareMapRenderData(
      shops = shops,
      routes = routes,
      selectedRouteId = "R2",
      editingTargetRouteId = "R2",
    )
    assertEquals(6, editPins.size)
    assertEquals(listOf("s3", "s4"), editPins.filter { it.isEditTargetSelected }.map { it.shop.id })
    assertEquals(listOf(1, 2), editPins.filter { it.isEditTargetSelected }.map { it.stopNumber })
    assertEquals(4, editPins.count { it.isDimmedForEdit })
    assertEquals(listOf("R1", "R2"), editTracks.map { it.route.id })
    assertTrue(editTracks.first { it.route.id == "R1" }.isDimmed)
    assertFalse(editTracks.first { it.route.id == "R2" }.isDimmed)
  }

  @Test
  fun mapRouteEditorDraft_selectDeselectUndoAndUpdateSavePayload() {
    val routes = listOf(
      DeliveryRoute(id = "R1", name = "Route 1", colorIndex = 0, stopOrderIds = listOf("s1")),
      DeliveryRoute(id = "R2", name = "Route 2", colorIndex = 1, stopOrderIds = emptyList()),
    )
    val s1 = Shop(id = "s1", shopCode = "S1", name = "Alpha", routeId = "R1", latitude = 11.87, longitude = 75.37)
    val s2 = Shop(id = "s2", shopCode = "S2", name = "Beta", routeId = RoutesCatalog.UNALLOCATED, latitude = 11.90, longitude = 75.40)
    val s3 = Shop(id = "s3", shopCode = "S3", name = "Gamma", routeId = RoutesCatalog.UNALLOCATED, latitude = 11.95, longitude = 75.45)

    var draft = createInitialMapRouteEditorDraft(
      shops = listOf(s1, s2, s3),
      routes = routes,
      initialTargetRouteId = "R1",
    )
    assertFalse(draft.canUndo)
    assertEquals(0, draft.pendingChangeCount)

    // Click s2 -> selects for R1
    draft = draft.toggleShopSelection(s2)
    assertEquals("R1", draft.draftShopRoutes["s2"])
    assertEquals(listOf("s1", "s2"), draft.draftRouteStopOrders["R1"])
    assertEquals(1, draft.pendingChangeCount)
    assertTrue(draft.canUndo)

    // Click s2 again -> deselects from R1
    draft = draft.toggleShopSelection(s2)
    assertEquals(RoutesCatalog.UNALLOCATED, draft.draftShopRoutes["s2"])
    assertEquals(listOf("s1"), draft.draftRouteStopOrders["R1"])
    assertEquals(0, draft.pendingChangeCount)

    // Click s3 -> selects for R1, switch to R2, click s1 -> moves s1 from R1 to R2
    draft = draft.toggleShopSelection(s3).withTargetRoute("R2").toggleShopSelection(s1)
    assertEquals("R1", draft.draftShopRoutes["s3"])
    assertEquals("R2", draft.draftShopRoutes["s1"])
    assertEquals(listOf("s3"), draft.draftRouteStopOrders["R1"])
    assertEquals(listOf("s1"), draft.draftRouteStopOrders["R2"])

    // Undo last change -> restores s1 back to R1
    draft = draft.undoLastChange()
    assertEquals("R1", draft.draftShopRoutes["s1"])
    assertEquals(listOf("s1", "s3"), draft.draftRouteStopOrders["R1"])
    assertEquals(emptyList<String>(), draft.draftRouteStopOrders["R2"])

    val payload = draft.changedShopsByTargetRoute()
    assertEquals(mapOf("R1" to listOf("s3")), payload)
  }

  @Test
  fun routePlanScreen_onMapEditMode_entersEditsSwitchesRouteUndoesAndSaves() {
    val routes = listOf(
      DeliveryRoute(
        id = RoutesCatalog.R01_KANNUR_BANGALORE,
        name = "Route 1: Kannur - Bangalore",
        vehicle = "KL-13-AB-1122",
        maxCrates = 40,
        colorIndex = 0,
        stopOrderIds = listOf("s1"),
      ),
      DeliveryRoute(
        id = RoutesCatalog.R02_KANNUR_KOZHIKODE,
        name = "Route 2: Kannur - Kozhikode",
        vehicle = "KL-07-CB-9081",
        maxCrates = 40,
        colorIndex = 3,
      ),
    )
    // Place s2 cleanly at center of bounding box so tapping center of canvas hits s2 directly
    val shops = listOf(
      Shop(
        id = "s1",
        shopCode = "SHP-101",
        name = "North Bakery",
        ownerName = "Rajan",
        phone = "9845011223",
        address = "Kannur",
        pinCode = "670001",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        latitude = 11.8745,
        longitude = 75.3704,
      ),
      Shop(
        id = "s2",
        shopCode = "SHP-102",
        name = "Center Fresh Mart",
        ownerName = "Anil",
        phone = "9845099887",
        address = "Thalassery",
        pinCode = "670101",
        routeId = RoutesCatalog.UNALLOCATED,
        latitude = 11.8745,
        longitude = 75.3704,
      ),
    )

    val bulkUpdates = mutableListOf<Pair<List<String>, String>>()
    val savedRoutes = mutableListOf<DeliveryRoute>()

    composeTestRule.setContent {
      HomelyTheme {
        RoutePlanScreen(
          shops = shops,
          routes = routes,
          onSaveRoute = { route, _ -> savedRoutes += route },
          onUpdateShopRoute = { _, _, _ -> },
          onBulkAssignShopsToRoute = { ids, routeId, _ -> bulkUpdates += (ids to routeId) },
        )
      }
    }

    composeTestRule.waitForIdle()
    // Verify "All Shops" pill & on-map Edit controls are rendered directly on the map
    composeTestRule.onNodeWithTag("map_route_pill_all").assertIsDisplayed()
    composeTestRule.onNodeWithText("All Shops (2)").assertIsDisplayed()
    composeTestRule.onNodeWithTag("map_edit_mode_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("map_surface_edit_chip").assertIsDisplayed()

    // Enter Edit Mode from the map itself
    composeTestRule.onNodeWithTag("map_edit_mode_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("map_edit_target_route_dropdown").assertIsDisplayed()
    composeTestRule.onNodeWithTag("map_edit_route_pills_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("map_edit_action_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("map_edit_undo_button").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("map_edit_save_button").assertIsDisplayed()

    // Choose Route 2 pill to update
    composeTestRule.onNodeWithTag("map_edit_route_pill_${RoutesCatalog.R02_KANNUR_KOZHIKODE}").performClick()
    composeTestRule.waitForIdle()

    // Click shop pin at center of map canvas -> assigns s2 to Route 2 and enables Undo
    composeTestRule.onNodeWithTag("real_osm_map_canvas").performTouchInput { click(center) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("map_edit_undo_button").assertIsEnabled()

    // Click again to deselect, then Undo to restore selection, then Update & Save
    composeTestRule.onNodeWithTag("real_osm_map_canvas").performTouchInput { click(center) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("map_edit_undo_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("map_edit_save_button").performClick()
    composeTestRule.waitForIdle()

    assertTrue(bulkUpdates.any { (ids, routeId) ->
      "s2" in ids && routeId == RoutesCatalog.R02_KANNUR_KOZHIKODE
    })
    assertTrue(savedRoutes.any { route ->
      route.id == RoutesCatalog.R02_KANNUR_KOZHIKODE && "s2" in route.stopOrderIds
    })
  }
}
