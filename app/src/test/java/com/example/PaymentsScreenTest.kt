package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentLedgerHelper
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentTransaction
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.SupplierBill
import com.example.data.model.SupplierBillStatus
import com.example.data.repository.LocalTestRepository
import com.example.ui.screens.PaymentsScreen
import com.example.ui.theme.HomelyTheme
import com.example.ui.viewmodel.HomelyViewModel
import com.example.ui.viewmodel.UiState
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class PaymentsScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun viewModel_withLocalRepository_recordsCustomerAndSupplierPayments() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(testDispatcher)
    try {
      val repo = LocalTestRepository()
      val vm = HomelyViewModel(repo, LocalTestRepository.LOCAL_UID)
      val job1 = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.paymentsState.collect {} }
      val job2 = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.supplierBillsState.collect {} }
      advanceUntilIdle()

      val initialPayments = (vm.paymentsState.first { it is UiState.Success } as UiState.Success).data
      assertTrue(initialPayments.isNotEmpty())

      var recordedId = ""
      vm.recordPayment(
        payment = PaymentTransaction(
          direction = PaymentDirection.PAY_SUPPLIER,
          partyId = "seed_sup_1",
          partyCode = "SUP-001",
          partyName = "Kera Pure Coconut Mills",
          invoiceRef = "BILL-402",
          amount = 12000.0,
          paymentMethod = PaymentMethod.BANK,
          referenceNumber = "NEFT/TEST/001",
          paymentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
          recordedBy = "Tester",
        ),
        isOnline = true,
      ) { savedId -> recordedId = savedId }
      advanceUntilIdle()
      assertTrue(recordedId.isNotBlank())

      val updatedBills = (vm.supplierBillsState.first { it is UiState.Success } as UiState.Success).data
      val settledBill = updatedBills.firstOrNull { it.id == "seed_sup_1" }
      assertNotNull(settledBill)
      assertEquals(SupplierBillStatus.SETTLED, settledBill?.status)

      job1.cancel()
      job2.cancel()
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun paymentsScreen_switchesModesTabsAndRecordsCollection() {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val fiveDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -5) }
      .let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) }

    val shops = listOf(
      Shop(
        id = "s1",
        shopCode = "SHP-101",
        name = "Malabar Heritage Supermarket",
        ownerName = "Abdul Rahman",
        phone = "9847012345",
        address = "Edappally, Kochi",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
      ),
    )
    val orders = listOf(
      OrderRecord(
        id = "o1",
        orderCode = "ORD-2050",
        shopId = "s1",
        shopCode = "SHP-101",
        shopName = "Malabar Heritage Supermarket",
        routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
        status = OrderStatus.DELIVERED,
        totalAmount = 10000.0,
        itemCount = 20,
        itemsSummary = "Banana Chips 20x",
        orderDate = fiveDaysAgo,
      ),
    )
    val payments = PaymentLedgerHelper.starterPayments("u1", today, fiveDaysAgo)
    val bills = PaymentLedgerHelper.starterSupplierBills("u1", today) { offset ->
      Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
        .let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) }
    }

    var recordedTx: PaymentTransaction? = null

    composeTestRule.setContent {
      HomelyTheme(darkTheme = false) {
        PaymentsScreen(
          shops = shops,
          orders = orders,
          payments = payments,
          supplierBills = bills,
          routes = RoutesCatalog.starterRoutes,
          userDisplayName = "Tester",
          onRecordPayment = { tx, _, onSaved ->
            recordedTx = tx
            onSaved("pay_new_1")
          },
          onSaveSupplierBill = { _, _, onSaved -> onSaved() },
        )
      }
    }

    val list = composeTestRule.onNodeWithTag("payments_lazy_list")

    // Shop ledger rendered
    composeTestRule.onNodeWithText("Shop Payments").assertIsDisplayed()
    list.performScrollToNode(androidx.compose.ui.test.hasTestTag("shop_payment_card_SHP-101"))
    composeTestRule.onNodeWithTag("shop_payment_card_SHP-101").assertIsDisplayed()

    // Open Record Payment sheet and save
    list.performScrollToNode(androidx.compose.ui.test.hasTestTag("record_payment_button_SHP-101"))
    composeTestRule.onNodeWithTag("record_payment_button_SHP-101").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm_record_payment_button").assertExists()
    composeTestRule.onNodeWithTag("confirm_record_payment_button").performClick()
    composeTestRule.waitForIdle()
    assertNotNull(recordedTx)
    assertEquals(PaymentDirection.COLLECT, recordedTx?.direction)

    // Switch to Supplier Payouts mode
    list.performScrollToNode(androidx.compose.ui.test.hasText("Shop Payments"))
    composeTestRule.onNodeWithTag("payment_mode_suppliers").performClick()
    composeTestRule.onNodeWithText("Supplier Payouts").assertIsDisplayed()
    list.performScrollToNode(androidx.compose.ui.test.hasTestTag("supplier_bill_card_SUP-001"))
    composeTestRule.onNodeWithTag("supplier_bill_card_SUP-001").assertIsDisplayed()

    // Switch to Payouts tab & Aging tab
    list.performScrollToNode(androidx.compose.ui.test.hasTestTag("tab_payments_receipts"))
    composeTestRule.onNodeWithTag("tab_payments_receipts").performClick()
    list.performScrollToNode(androidx.compose.ui.test.hasTestTag("payment_receipt_card_PAYOUT-2026-4401"))
    composeTestRule.onNodeWithTag("payment_receipt_card_PAYOUT-2026-4401").assertIsDisplayed()
    list.performScrollToNode(androidx.compose.ui.test.hasTestTag("tab_payments_aging"))
    composeTestRule.onNodeWithTag("tab_payments_aging").performClick()
    list.performScrollToNode(androidx.compose.ui.test.hasText("Upcoming / Current"))
    composeTestRule.onNodeWithText("Upcoming / Current").assertIsDisplayed()
  }
}
