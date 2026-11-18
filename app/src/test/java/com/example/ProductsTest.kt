package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.Product
import com.example.data.model.ProductCatalogHelper
import com.example.data.model.ProductStatus
import com.example.data.model.ProductVariant
import com.example.data.repository.LocalTestRepository
import com.example.ui.screens.AddEditProductScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.theme.SnackRouteTheme
import com.example.ui.viewmodel.SnackRouteViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ProductsTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun productCatalogHelper_variantsMarginAndOrderConversion() {
    val variants = listOf(
      ProductVariant("TP100", "100g Pouch", 36.0, 45.0, true),
      ProductVariant("TP250", "250g Family Pack", 82.0, 100.0, false),
    )
    val encoded = ProductCatalogHelper.encodeVariants(variants)
    val decoded = ProductCatalogHelper.decodeVariants(encoded)
    assertEquals(2, decoded.size)
    assertEquals("TP100", decoded[0].sku)
    assertEquals(18.0, ProductCatalogHelper.computeMarginPercent(82.0, 100.0), 0.1)

    val variantProd = Product(
      id = "p_var",
      productCode = "PRD-00201",
      sku = "SKU-TP-01",
      name = "Crispy Tapioca Chips",
      category = "Biscuits & Snacks",
      hasVariants = true,
      variantsJson = encoded,
      wholesalePrice = 36.0,
      mrp = 100.0,
      stockQuantity = 100,
      minThreshold = 25,
      status = ProductStatus.ACTIVE,
    )
    assertEquals("₹36.00 - ₹82.00", ProductCatalogHelper.formatPriceDisplay(variantProd))
    assertFalse(ProductCatalogHelper.isLowStock(variantProd))

    val lowProd = variantProd.copy(id = "p_low", stockQuantity = 15, minThreshold = 25)
    assertTrue(ProductCatalogHelper.isLowStock(lowProd))

    val inactiveProd = variantProd.copy(id = "p_inact", status = ProductStatus.INACTIVE, sku = "INACT01", hasVariants = false)
    val catalog = ProductCatalogHelper.toCatalogProducts(listOf(variantProd, inactiveProd))
    assertTrue(catalog.any { it.code == "TP100" && it.name == "Crispy Tapioca Chips" })
    assertTrue(catalog.any { it.code == "TP250" && it.defaultPrice == 82.0 })
    assertFalse(catalog.any { it.code == "INACT01" })
  }

  @Test
  fun viewModel_withLocalRepository_createsUpdatesAndReadsProducts() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(testDispatcher)
    try {
      val repo = LocalTestRepository()
      val vm = SnackRouteViewModel(repo, LocalTestRepository.LOCAL_UID)
      val collectJob = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
        vm.productsState.collect {}
      }
      advanceUntilIdle()

      val initialState = vm.productsState.first { it is UiState.Success } as UiState.Success
      assertEquals(6, initialState.data.size)

      var savedCallbackCalled = false
      val newProd = Product(
        productCode = "PRD-00107",
        sku = "SKU-CAS-009",
        name = "Roasted Cashew Salted",
        category = "Biscuits & Snacks",
        brand = "Homely Gold",
        packSize = "150g",
        wholesalePrice = 140.0,
        mrp = 175.0,
        stockQuantity = 60,
        minThreshold = 20,
        status = ProductStatus.ACTIVE,
      )
      vm.saveProduct(newProd, isOnline = true) { savedCallbackCalled = true }
      advanceUntilIdle()
      assertTrue(savedCallbackCalled)

      val afterAdd = (vm.productsState.value as UiState.Success).data
      val inserted = afterAdd.firstOrNull { it.productCode == "PRD-00107" }
      assertNotNull(inserted)
      assertEquals("Roasted Cashew Salted", inserted?.name)

      vm.updateProductStock(inserted!!.id, 12, inserted.name)
      vm.updateProductStatus(inserted.id, ProductStatus.INACTIVE, "Inactive")
      advanceUntilIdle()

      val afterUpdate = (vm.productsState.value as UiState.Success).data
        .first { it.id == inserted.id }
      assertEquals(12, afterUpdate.stockQuantity)
      assertEquals(ProductStatus.INACTIVE, afterUpdate.status)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun productsScreen_displaysCatalogAndFiltersByTabAndSearch() {
    val starter = ProductCatalogHelper.starterProducts("u1", "2026-11-18") { "2026-11-10" }
    composeTestRule.setContent {
      SnackRouteTheme {
        ProductsScreen(
          products = starter,
          pendingQueueCount = 0,
          onAddProductClick = {},
          onEditProductClick = {},
          onChangeProductStatus = { _, _, _ -> },
          onUpdateProductStock = { _, _, _ -> },
        )
      }
    }

    composeTestRule.onNodeWithText("Kerala Special Banana Chips").assertIsDisplayed()

    // Filter to Low Stock tab -> Pure Coconut Oil 1L Pouch should appear, Banana Chips should disappear
    composeTestRule.onNodeWithTag("tab_products_low").performClick()
    composeTestRule.onNodeWithText("Pure Coconut Oil 1L Pouch").assertIsDisplayed()
    composeTestRule.onNodeWithText("Kerala Special Banana Chips").assertDoesNotExist()

    // Back to All tab and search by SKU
    composeTestRule.onNodeWithTag("tab_products_all").performClick()
    composeTestRule.onNodeWithTag("products_search_input").performTextInput("SKU-PEP-012")
    composeTestRule.onNodeWithText("Premium Malabar Black Pepper").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pure Coconut Oil 1L Pouch").assertDoesNotExist()
  }

  @Test
  fun addEditProductScreen_validatesAndSubmitsMultiVariantProduct() {
    var savedProduct: Product? = null
    composeTestRule.setContent {
      SnackRouteTheme {
        AddEditProductScreen(
          existingProduct = null,
          suggestedProductCode = "PRD-00108",
          onSaveProduct = { prod, _ -> savedProduct = prod },
          onBack = {},
        )
      }
    }

    composeTestRule.onNodeWithText("[ PRD-00108 Allocated ]").assertIsDisplayed()
    composeTestRule.onNodeWithTag("product_name_input").performTextInput("Kochi Jackfruit Chips")

    // Scroll to variant toggle and enable multi-variant mode
    composeTestRule.onNodeWithTag("add_edit_product_list")
      .performScrollToNode(hasTestTag("toggle_variants_switch"))
    composeTestRule.onNodeWithTag("toggle_variants_switch").performClick()
    composeTestRule.onNodeWithTag("add_edit_product_list")
      .performScrollToNode(hasTestTag("add_variant_button"))
    composeTestRule.onNodeWithTag("add_variant_button").performClick()

    composeTestRule.onNodeWithTag("add_edit_product_list")
      .performScrollToNode(hasTestTag("variant_pack_input"))
    composeTestRule.onNodeWithTag("variant_pack_input").performTextReplacement("200g Pouch")
    composeTestRule.onNodeWithTag("add_edit_product_list")
      .performScrollToNode(hasTestTag("variant_wholesale_input"))
    composeTestRule.onNodeWithTag("variant_wholesale_input").performTextReplacement("95")
    composeTestRule.onNodeWithTag("variant_mrp_input").performTextReplacement("120")
    composeTestRule.onNodeWithTag("add_edit_product_list")
      .performScrollToNode(hasTestTag("confirm_variant_button"))
    composeTestRule.onNodeWithTag("confirm_variant_button").assertIsEnabled().performClick()

    composeTestRule.onNodeWithTag("add_edit_product_list")
      .performScrollToNode(hasTestTag("variant_card_0"))
    composeTestRule.onNodeWithTag("variant_card_0").assertIsDisplayed()

    composeTestRule.onNodeWithTag("save_product_button").performClick()

    assertNotNull(savedProduct)
    assertEquals("PRD-00108", savedProduct?.productCode)
    assertEquals("Kochi Jackfruit Chips", savedProduct?.name)
    assertTrue(savedProduct?.hasVariants == true)
    assertEquals(95.0, savedProduct?.wholesalePrice ?: 0.0, 0.01)
    assertEquals(ProductStatus.ACTIVE, savedProduct?.status)
  }
}
