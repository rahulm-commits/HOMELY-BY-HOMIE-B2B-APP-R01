package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ServerConfigStore
import com.example.data.local.SnackRouteDatabase
import com.example.data.model.CatalogProduct
import com.example.data.model.InquiryStatus
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.example.data.model.SnackCatalog
import com.example.data.remote.LaptopServerClient
import com.example.data.repository.LocalTestRepository
import com.example.data.repository.SyncCoordinatorRepository
import com.example.ui.screens.OrderScreen
import com.example.ui.screens.PlaceOrderScreen
import com.example.ui.theme.SnackRouteTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class OrderPickCallEditDeleteAndDeliveryTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun orderScreen_displaysPickTheCall_andNoUnattendedLabels() {
    val shop = Shop(
      id = "shop_call_1",
      shopCode = "SHP-101",
      name = "Malabar Heritage Bakery",
      ownerName = "Hameed",
      phone = "9847011223",
      address = "Fort Road, Kannur",
      pinCode = "670001",
      routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
      status = ShopStatus.ACTIVE,
      inquiryStatus = InquiryStatus.PENDING_INQUIRY,
    )

    var lastFeedback = ""
    composeTestRule.setContent {
      SnackRouteTheme(darkTheme = false) {
        OrderScreen(
          shops = listOf(shop),
          orders = emptyList(),
          onPlaceOrderClick = {},
          onMarkInquiryStatus = { _, _, feedback -> lastFeedback = feedback },
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Pick the Call (0)").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pick the Call").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithText("Unattended Orders", substring = true).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("Mark Unattended", substring = true).fetchSemanticsNodes().size)

    composeTestRule.onNodeWithText("Pick the Call").performClick()
    composeTestRule.waitForIdle()
    assertTrue(lastFeedback.contains("Pick the Call"))
  }

  @Test
  fun orderScreen_confirmedOrders_supportsEditAndConfirmedDelete() {
    val shop = Shop(
      id = "shop_1",
      shopCode = "SHP-101",
      name = "Malabar Heritage Bakery",
      ownerName = "Hameed",
      phone = "9847011223",
      address = "Fort Road, Kannur",
      pinCode = "670001",
      routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
      status = ShopStatus.ACTIVE,
    )
    val confirmedOrder = OrderRecord(
      id = "ord_conf_1",
      orderCode = "ORD-7701",
      shopId = shop.id,
      shopCode = shop.shopCode,
      shopName = shop.name,
      routeId = shop.routeId,
      status = OrderStatus.CONFIRMED,
      totalAmount = 450.0,
      itemCount = 10,
      itemsSummary = "BC100 x10",
      orderDate = "2026-11-19",
      note = "Morning crate",
    )

    var editedOrder: OrderRecord? = null
    var deletedOrder: OrderRecord? = null

    composeTestRule.setContent {
      SnackRouteTheme(darkTheme = false) {
        OrderScreen(
          shops = listOf(shop),
          orders = listOf(confirmedOrder),
          onPlaceOrderClick = {},
          onMarkInquiryStatus = { _, _, _ -> },
          onEditOrderClick = { editedOrder = it },
          onDeleteOrderClick = { deletedOrder = it },
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("order_tab_confirmed_orders").performClick()
    composeTestRule.waitForIdle()

    // No oversized full-width text buttons
    assertEquals(0, composeTestRule.onAllNodesWithText("Edit Order").fetchSemanticsNodes().size)

    val editNode = composeTestRule.onNodeWithTag("edit_order_ORD-7701").assertIsDisplayed()
    val deleteNode = composeTestRule.onNodeWithTag("delete_order_ORD-7701").assertIsDisplayed()
    val shopBounds = composeTestRule.onNodeWithText("Malabar Heritage Bakery").getUnclippedBoundsInRoot()
    val editBounds = editNode.getUnclippedBoundsInRoot()
    val deleteBounds = deleteNode.getUnclippedBoundsInRoot()

    // Compact icons kept in the top-right corner above the shop details row
    assertTrue("Expected compact edit icon width, got ${editBounds.width}", editBounds.width <= 52.dp)
    assertTrue("Expected compact delete icon width, got ${deleteBounds.width}", deleteBounds.width <= 52.dp)
    assertTrue("Edit icon should sit right of shop start", editBounds.left > shopBounds.left)
    assertTrue("Delete icon should sit to the right of edit icon", deleteBounds.left >= editBounds.right - 2.dp)
    assertTrue("Top-right icons should align above shop title", editBounds.top < shopBounds.top)

    editNode.performClick()
    assertEquals("ord_conf_1", editedOrder?.id)

    deleteNode.performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Delete Order?").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_delete_order_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals("ord_conf_1", deletedOrder?.id)
  }

  @Test
  fun placeOrderScreen_editModePrefillsQuantitiesAndSavesChanges() {
    val shop = Shop(
      id = "shop_1",
      shopCode = "SHP-101",
      name = "Malabar Heritage Bakery",
      routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
    )
    val bc100 = SnackCatalog.products.first { it.code == "BC100" }
    val existingOrder = OrderRecord(
      id = "ord_edit_1",
      orderCode = "ORD-8802",
      shopId = shop.id,
      shopCode = shop.shopCode,
      shopName = shop.name,
      routeId = shop.routeId,
      status = OrderStatus.CONFIRMED,
      totalAmount = bc100.defaultPrice * 10,
      itemCount = 10,
      itemsSummary = "BC100 x10",
      note = "Urgent morning van",
    )

    var updatedItems: List<Pair<CatalogProduct, Int>> = emptyList()
    var updatedNote = ""

    composeTestRule.setContent {
      SnackRouteTheme(darkTheme = false) {
        PlaceOrderScreen(
          shop = shop,
          existingOrder = existingOrder,
          catalogProducts = SnackCatalog.products,
          onConfirmOrder = { items, note ->
            updatedItems = items
            updatedNote = note
          },
          onBack = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Edit Order").assertIsDisplayed()
    composeTestRule.onNodeWithText("10 packets selected").assertIsDisplayed()
    composeTestRule.onNodeWithText("Save Changes").assertIsDisplayed()

    composeTestRule.onNodeWithTag("add_qty_BC100").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("15 packets selected").assertIsDisplayed()

    composeTestRule.onNodeWithTag("confirm_order_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(15, updatedItems.firstOrNull { it.first.code == "BC100" }?.second)
    assertEquals("Urgent morning van", updatedNote)
  }

  @Test
  fun localTestMode_placeUpdateDeleteOrderAndRepeatedDeliveryStayExactCount() = runBlocking {
    val context: Context = ApplicationProvider.getApplicationContext()
    val roomDb = Room.inMemoryDatabaseBuilder(context, SnackRouteDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val configStore = ServerConfigStore(context)
    configStore.updateServerConfig(
      globalSyncEnabled = false,
      laptopServerEnabled = false,
      laptopServerUrl = "",
      laptopApiToken = "",
      autoDeleteSyncedFromPhone = false,
    )
    val localRepo = LocalTestRepository()
    val coordinator = SyncCoordinatorRepository(
      appContext = context,
      primary = localRepo,
      dao = roomDb.snackRouteDao(),
      configStore = configStore,
      laptopClient = LaptopServerClient(),
      currentUserId = LocalTestRepository.LOCAL_UID,
      isLocalTestMode = true,
    )

    try {
      val initialOrders = coordinator.observeOrders(LocalTestRepository.LOCAL_UID).first()
      val initialDeliveredCount = initialOrders.count { it.status == OrderStatus.DELIVERED }
      val shop = coordinator.observeShops(LocalTestRepository.LOCAL_UID).first().first()
      val bc100 = SnackCatalog.products.first { it.code == "BC100" }
      val jb200 = SnackCatalog.products.first { it.code == "JB200" }

      // Placing 1 order in Local Test Mode must create exactly 1 order (no duplicate local_ord copy)
      val orderId = coordinator.placeOrder(shop, listOf(bc100 to 10), "Morning").getOrThrow()
      val afterPlace = coordinator.observeOrders(LocalTestRepository.LOCAL_UID).first()
      assertEquals(initialOrders.size + 1, afterPlace.size)
      assertEquals(1, afterPlace.count { it.id == orderId })

      // Updating order details keeps total count unchanged and updates items/amount/note
      coordinator.updateOrderDetails(orderId, listOf(bc100 to 12, jb200 to 5), "Updated note").getOrThrow()
      val afterEdit = coordinator.observeOrders(LocalTestRepository.LOCAL_UID).first()
      assertEquals(initialOrders.size + 1, afterEdit.size)
      val edited = afterEdit.first { it.id == orderId }
      assertEquals(17, edited.itemCount)
      assertEquals("Updated note", edited.note)
      assertTrue(edited.itemsSummary.contains("BC100 x12"))

      // Marking delivered once AND clicking again must increment delivered count by exactly 1 (never double to +2/+6)
      coordinator.updateOrderStatus(orderId, OrderStatus.DELIVERED, "POD Signed").getOrThrow()
      coordinator.updateOrderStatus(orderId, OrderStatus.DELIVERED, "POD Signed").getOrThrow()
      val afterDeliver = coordinator.observeOrders(LocalTestRepository.LOCAL_UID).first()
      assertEquals(initialDeliveredCount + 1, afterDeliver.count { it.status == OrderStatus.DELIVERED })

      // Deleting an order removes it completely from observed orders
      coordinator.deleteOrder(orderId).getOrThrow()
      val afterDelete = coordinator.observeOrders(LocalTestRepository.LOCAL_UID).first()
      assertFalse(afterDelete.any { it.id == orderId })
      assertEquals(initialOrders.size, afterDelete.size)
    } finally {
      roomDb.close()
    }
  }
}
