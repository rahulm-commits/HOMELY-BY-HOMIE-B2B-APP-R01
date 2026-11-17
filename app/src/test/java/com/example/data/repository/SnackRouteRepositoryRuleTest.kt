package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CatalogProduct
import com.example.data.model.InquiryStatus
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnackRouteRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveShop_authenticatedOwner_createsAndReadsShop() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = SnackRouteRepository(firestore, auth)

    val uniqueCode = "SHP-${UUID.randomUUID().toString().take(6)}"
    val shop = Shop(
      shopCode = uniqueCode,
      name = "Kannur Chips Palace",
      ownerName = "Rajesh Kumar",
      phone = "9847055667",
      address = "Station Road, Kannur",
      pinCode = "670001",
      routeId = RoutesCatalog.R01_KANNUR_BANGALORE,
      latitude = 11.8745,
      longitude = 75.3704,
      status = ShopStatus.ACTIVE,
      inquiryStatus = InquiryStatus.PENDING_INQUIRY,
    )

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveShop(shop) }
    assertTrue("Expected shop save success: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
    val createdId = saveResult.getOrThrow()
    assertTrue(createdId.isNotBlank())

    val shops = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeShops(uid).first { list -> list.any { it.id == createdId } }
    }
    assertEquals(uniqueCode, shops.first { it.id == createdId }.shopCode)
  }

  @Test
  fun placeOrder_authenticatedOwner_createsOrderAndUpdatesShopStatus() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = SnackRouteRepository(firestore, auth)

    val shop = Shop(
      shopCode = "SHP-${UUID.randomUUID().toString().take(5)}",
      name = "Kozhikode Bakes",
      ownerName = "Salim K",
      phone = "9895112233",
      address = "Beach Road, Kozhikode",
      pinCode = "673001",
      routeId = RoutesCatalog.R02_KANNUR_KOZHIKODE,
      status = ShopStatus.ACTIVE,
      inquiryStatus = InquiryStatus.PENDING_INQUIRY,
    )
    val shopId = withTimeout(DEFAULT_TIMEOUT_MS) { repo.saveShop(shop).getOrThrow() }
    val savedShop = shop.copy(id = shopId)

    val product = CatalogProduct("BC100", "Banana Chips", "100g", "Banana Chips", 45.0)
    val orderResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.placeOrder(savedShop, listOf(product to 10), "Morning dispatch")
    }
    assertTrue("Expected order success: ${orderResult.exceptionOrNull()}", orderResult.isSuccess)

    val orders = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeOrders(uid).first { list -> list.any { it.shopId == shopId } }
    }
    assertEquals(450.0, orders.first { it.shopId == shopId }.totalAmount, 0.01)
  }

  @Test
  fun observeShops_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = SnackRouteRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.saveShop(
        Shop(
          shopCode = "SHP-CROSS",
          name = "Alice Shop",
          ownerName = "Alice",
          phone = "9999988888",
          address = "Kannur",
          pinCode = "670001",
        )
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = SnackRouteRepository(firestore, auth)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeShops(aliceUid).first()
      }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull("Expected PERMISSION_DENIED when Bob reads Alice's shops", caught)
    val firestoreEx = caught!!.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
  }

  @Test
  fun observeShops_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = SnackRouteRepository(firestore, auth)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeShops("some_user_123").first()
      }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull("Expected PERMISSION_DENIED when unauthenticated", caught)
    val firestoreEx = caught!!.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_snack@test.com"
    const val BOB_EMAIL = "bob_snack@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3500L
  }
}
