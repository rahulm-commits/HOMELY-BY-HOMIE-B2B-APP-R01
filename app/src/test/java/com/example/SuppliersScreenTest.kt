package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.data.model.Supplier
import com.example.data.model.SupplierCatalogHelper
import com.example.data.model.SupplierStatus
import com.example.data.repository.LocalTestRepository
import com.example.ui.screens.AddEditSupplierScreen
import com.example.ui.screens.SuppliersScreen
import com.example.ui.theme.HomelyTheme
import com.example.ui.viewmodel.HomelyViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class SuppliersScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun viewModel_withLocalRepository_loadsSavesAndUpdatesSupplierStatus() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(testDispatcher)
    try {
      val repo = LocalTestRepository()
      val vm = HomelyViewModel(repo, LocalTestRepository.LOCAL_UID)
      val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        vm.suppliersState.collect {}
      }
      advanceUntilIdle()

      val initial = (vm.suppliersState.first { it is UiState.Success } as UiState.Success).data
      assertEquals(4, initial.size)
      assertTrue(initial.any { it.supplierCode == "SUP-000044" })

      var savedCallbackInvoked = false
      vm.saveSupplier(
        supplier = Supplier(
          supplierCode = "SUP-000048",
          name = "Wayanad Spices Co-op",
          category = "Edible Oils & Spices",
          contactPerson = "Gopalakrishnan Nair",
          phone = "9847099887",
          email = "trade@wayanadspices.in",
          address = "Kalpetta, Wayanad, Kerala",
          pinCode = "673121",
        ),
        isOnline = true,
      ) {
        savedCallbackInvoked = true
      }
      advanceUntilIdle()
      assertTrue(savedCallbackInvoked)

      val afterSave = (vm.suppliersState.first { it is UiState.Success } as UiState.Success).data
      val inserted = afterSave.firstOrNull { it.supplierCode == "SUP-000048" }
      assertNotNull(inserted)
      assertEquals("Wayanad Spices Co-op", inserted?.name)

      vm.updateSupplierStatus(inserted!!.id, SupplierStatus.INACTIVE, "Marked inactive")
      advanceUntilIdle()

      val afterStatusUpdate = (vm.suppliersState.first { it is UiState.Success } as UiState.Success).data
      val updated = afterStatusUpdate.firstOrNull { it.id == inserted.id }
      assertEquals(SupplierStatus.INACTIVE, updated?.status)

      job.cancel()
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun suppliersScreen_rendersCardsFiltersSearchesAndOpensQuickActions() {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = fmt.format(Date())
    val starterSuppliers = SupplierCatalogHelper.starterSuppliers("u1", today) { offset ->
      Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }.let { fmt.format(it.time) }
    }

    var payBillsNavigated = false

    composeTestRule.setContent {
      HomelyTheme(darkTheme = false) {
        SuppliersScreen(
          suppliers = starterSuppliers,
          pendingQueueCount = 0,
          onOpenSyncServers = {},
          onAddSupplierClick = {},
          onEditSupplierClick = {},
          onChangeSupplierStatus = { _, _, _ -> },
          onNavigateToSupplierPayouts = { payBillsNavigated = true },
        )
      }
    }
    composeTestRule.waitForIdle()

    // Metrics badge and starter cards rendered
    composeTestRule.onNodeWithText("4 Registered Suppliers").assertIsDisplayed()
    composeTestRule.onNodeWithText("Malabar Wholesale Distributors").assertIsDisplayed()

    val list = composeTestRule.onNodeWithTag("suppliers_lazy_list")
    list.performScrollToNode(hasTestTag("supplier_card_SUP-000047"))
    composeTestRule.onNodeWithText("Cochin Spices & Oil Mill").assertIsDisplayed()
    composeTestRule.onNode(
      hasTestTag("supplier_card_SUP-000047") and hasText("NEW")
    ).assertIsDisplayed()

    // Filter by Recently Added (SUP-000047 today & SUP-000046 12d ago; excludes SUP-000044 20d ago)
    composeTestRule.onNodeWithTag("supplier_filter_recently_added").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("supplier_card_SUP-000044").assertDoesNotExist()
    list.performScrollToNode(hasTestTag("supplier_card_SUP-000047"))
    composeTestRule.onNodeWithTag("supplier_card_SUP-000047").assertIsDisplayed()

    // Switch back to All Suppliers & test search empty state + reset
    composeTestRule.onNodeWithTag("supplier_filter_all").performClick()
    composeTestRule.onNodeWithTag("supplier_search_input").performTextInput("NoSuchSupplierXYZ")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("No suppliers matched").assertIsDisplayed()

    composeTestRule.onNodeWithTag("reset_supplier_filters_button").performClick()
    composeTestRule.waitForIdle()
    list.performScrollToNode(hasTestTag("supplier_card_SUP-000044"))
    composeTestRule.onNodeWithTag("supplier_card_SUP-000044").assertIsDisplayed()

    // Open Quick Actions Sheet for SUP-000044
    composeTestRule.onNodeWithTag("more_supplier_SUP-000044").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("qa_edit_supplier_details").assertExists()
    composeTestRule.onNodeWithTag("qa_confirmed_prices").assertExists()
    composeTestRule.onNodeWithTag("sheet_full_profile_button").assertExists()
    composeTestRule.onNodeWithTag("sheet_pay_bills_button").assertExists().performClick()
    composeTestRule.waitForIdle()
    assertTrue(payBillsNavigated)
  }

  @Test
  fun addEditSupplierScreen_validatesRequiredFieldsAndSubmitsForm() {
    var savedSupplier: Supplier? = null

    composeTestRule.setContent {
      HomelyTheme(darkTheme = false) {
        AddEditSupplierScreen(
          existingSupplier = null,
          suggestedSupplierCode = "SUP-000048",
          addedByName = "Field Officer",
          onSaveSupplier = { supplier, _ -> savedSupplier = supplier },
          onBack = {},
        )
      }
    }
    composeTestRule.waitForIdle()

    val formList = composeTestRule.onNodeWithTag("add_edit_supplier_list")

    // Empty submit triggers validation error
    composeTestRule.onNodeWithTag("save_supplier_button").performClick()
    composeTestRule.waitForIdle()
    formList.performScrollToNode(hasTestTag("supplier_validation_error"))
    composeTestRule.onNodeWithTag("supplier_validation_error").assertIsDisplayed()

    // Fill required fields
    formList.performScrollToNode(hasTestTag("supplier_name_input"))
    composeTestRule.onNodeWithTag("supplier_name_input").performTextInput("  Ernakulam Spice Depot  ")

    formList.performScrollToNode(hasTestTag("supplier_contact_input"))
    composeTestRule.onNodeWithTag("supplier_contact_input").performTextInput("Raju Nair")

    formList.performScrollToNode(hasTestTag("supplier_phone_input"))
    composeTestRule.onNodeWithTag("supplier_phone_input").performTextInput("9847011223")

    formList.performScrollToNode(hasTestTag("supplier_address_input"))
    composeTestRule.onNodeWithTag("supplier_address_input").performTextInput("Market Road, Aluva 683101")

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("save_supplier_button").performClick()
    composeTestRule.waitForIdle()

    assertNotNull(savedSupplier)
    assertEquals("SUP-000048", savedSupplier?.supplierCode)
    assertEquals("Ernakulam Spice Depot", savedSupplier?.name)
    assertEquals("Raju Nair", savedSupplier?.contactPerson)
    assertEquals("9847011223", savedSupplier?.phone)
    assertEquals("683101", savedSupplier?.pinCode)
  }
}
