package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ServerConfigStore
import com.example.data.local.SnackRouteDatabase
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.remote.LaptopServerClient
import com.example.data.repository.LocalTestRepository
import com.example.data.repository.SyncCoordinatorRepository
import com.example.ui.components.MapCameraState
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.computeFitBoundsCamera
import com.example.ui.components.projectLatLngToScreen
import com.example.ui.theme.RouteColorPalette
import com.example.ui.theme.RouteComboPattern
import com.example.ui.theme.SnackRouteTheme
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
@Config(sdk = [36])
class RoutePlanningTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var roomDb: SnackRouteDatabase
  private lateinit var coordinator: SyncCoordinatorRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    roomDb = Room.inMemoryDatabaseBuilder(context, SnackRouteDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    coordinator = SyncCoordinatorRepository(
      appContext = context,
      primary = LocalTestRepository(),
      dao = roomDb.snackRouteDao(),
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
      SnackRouteTheme {
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
}
