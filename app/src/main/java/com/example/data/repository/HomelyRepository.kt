package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.InquiryStatus
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentLedgerHelper
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.ProductCatalogHelper
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.example.data.model.StockItem
import com.example.data.model.Supplier
import com.example.data.model.SupplierBill
import com.example.data.model.SupplierBillStatus
import com.example.data.model.SupplierCatalogHelper
import com.example.data.model.SupplierStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class HomelyRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) : HomelyDataSource {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  override fun observeShops(userId: String): Flow<List<Shop>> = flow {
    val path = "users/$userId/shops"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("shops")
        .snapshots(MetadataChanges.INCLUDE)
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              ?.copy(isPendingSync = doc.metadata.hasPendingWrites())
          }.sortedByDescending { it.updatedAt?.seconds ?: Long.MAX_VALUE }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  override fun observeOrders(userId: String): Flow<List<OrderRecord>> = flow {
    val path = "users/$userId/orders"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("orders")
        .snapshots(MetadataChanges.INCLUDE)
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(OrderRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              ?.copy(isPendingSync = doc.metadata.hasPendingWrites())
          }.sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  override fun observeStocks(userId: String): Flow<List<StockItem>> = flow {
    val path = "users/$userId/stocks"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("stocks")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(StockItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.quantity - it.minThreshold }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  override fun observeProducts(userId: String): Flow<List<Product>> = flow {
    val path = "users/$userId/products"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("products")
        .snapshots(MetadataChanges.INCLUDE)
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              ?.copy(isPendingSync = doc.metadata.hasPendingWrites())
          }.sortedByDescending { it.updatedAt?.seconds ?: Long.MAX_VALUE }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  override fun observePayments(userId: String): Flow<List<PaymentTransaction>> = flow {
    val path = "users/$userId/payments"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("payments")
        .snapshots(MetadataChanges.INCLUDE)
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(PaymentTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              ?.copy(isPendingSync = doc.metadata.hasPendingWrites())
          }.sortedWith(
            compareByDescending<PaymentTransaction> { it.paymentDate }
              .thenByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
          )
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  override fun observeSupplierBills(userId: String): Flow<List<SupplierBill>> = flow {
    val path = "users/$userId/supplierBills"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("supplierBills")
        .snapshots(MetadataChanges.INCLUDE)
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(SupplierBill::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              ?.copy(isPendingSync = doc.metadata.hasPendingWrites())
          }.sortedByDescending { it.updatedAt?.seconds ?: Long.MAX_VALUE }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  override fun observeSuppliers(userId: String): Flow<List<Supplier>> = flow {
    val path = "users/$userId/suppliers"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("suppliers")
        .snapshots(MetadataChanges.INCLUDE)
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(Supplier::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              ?.copy(isPendingSync = doc.metadata.hasPendingWrites())
          }.sortedByDescending { it.updatedAt?.seconds ?: Long.MAX_VALUE }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  override fun observeMeta(userId: String): Flow<OperationsMeta?> = flow {
    val path = "users/$userId/meta/summary"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("meta")
        .document("summary")
        .snapshots()
        .map { snapshot ->
          snapshot.toObject(OperationsMeta::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  override suspend fun saveShop(shop: Shop): Result<String> {
    return saveShopWithId(shop = shop, isUpdate = shop.id.isNotBlank())
  }

  suspend fun saveShopWithId(
    shop: Shop,
    isUpdate: Boolean,
    awaitServerAck: Boolean = false,
  ): Result<String> {
    val uid = requireUserId()
    val shopsCol = db.collection("users").document(uid).collection("shops")
    val docRef = if (shop.id.isNotBlank()) shopsCol.document(shop.id) else shopsCol.document()
    val today = todayString()

    val safeLatitude = shop.latitude.takeIf { it.isFinite() } ?: 0.0
    val safeLongitude = shop.longitude.takeIf { it.isFinite() } ?: 0.0

    val createMap = mapOf(
      "userId" to uid,
      "shopCode" to shop.shopCode.trim(),
      "name" to shop.name.trim(),
      "ownerName" to shop.ownerName.trim(),
      "phone" to shop.phone.trim(),
      "secondaryPhone" to shop.secondaryPhone.trim(),
      "address" to shop.address.trim(),
      "pinCode" to shop.pinCode.trim(),
      "routeId" to shop.routeId,
      "photoUri" to shop.photoUri,
      "latitude" to safeLatitude,
      "longitude" to safeLongitude,
      "note" to shop.note.trim(),
      "status" to shop.status,
      "inquiryStatus" to shop.inquiryStatus,
      "enquiryDate" to shop.enquiryDate.ifBlank { today },
      "callDate" to shop.callDate.ifBlank { today },
      "lastOrderDate" to shop.lastOrderDate,
      "lastCallDate" to shop.lastCallDate.ifBlank { today },
      "confirmedPriceNotes" to shop.confirmedPriceNotes.trim(),
      "mapsLink" to shop.mapsLink.trim(),
      "addedBy" to shop.addedBy.trim(),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val updateMap = mapOf(
      "shopCode" to shop.shopCode.trim(),
      "name" to shop.name.trim(),
      "ownerName" to shop.ownerName.trim(),
      "phone" to shop.phone.trim(),
      "secondaryPhone" to shop.secondaryPhone.trim(),
      "address" to shop.address.trim(),
      "pinCode" to shop.pinCode.trim(),
      "routeId" to shop.routeId,
      "photoUri" to shop.photoUri,
      "latitude" to safeLatitude,
      "longitude" to safeLongitude,
      "note" to shop.note.trim(),
      "status" to shop.status,
      "inquiryStatus" to shop.inquiryStatus,
      "enquiryDate" to shop.enquiryDate.ifBlank { today },
      "callDate" to shop.callDate.ifBlank { today },
      "lastOrderDate" to shop.lastOrderDate,
      "lastCallDate" to shop.lastCallDate,
      "confirmedPriceNotes" to shop.confirmedPriceNotes.trim(),
      "mapsLink" to shop.mapsLink.trim(),
      "addedBy" to shop.addedBy.trim(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS

    return try {
      val task = if (isUpdate) docRef.update(updateMap) else docRef.set(createMap)
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (isUpdate && e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
        return try {
          val acked = withTimeoutOrNull(waitMs) {
            docRef.set(createMap).await()
            true
          } ?: false
          if (awaitServerAck && !acked) {
            return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
          }
          Result.success(docRef.id)
        } catch (nested: Exception) {
          if (nested is CancellationException) throw nested
          handleFirestoreError(nested, OperationType.CREATE, docRef.path)
          Result.failure(nested)
        }
      }
      if (!isUpdate && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        // Document may already exist (createdAt rule blocks .set() overwrite); retry as partial update.
        val existingDoc = runCatching {
          withTimeoutOrNull(waitMs) { docRef.get().await() }
        }.getOrNull()
        if (existingDoc != null && existingDoc.exists()) {
          return try {
            val acked = withTimeoutOrNull(waitMs) {
              docRef.update(updateMap).await()
              true
            } ?: false
            if (awaitServerAck && !acked) {
              return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
            }
            Result.success(docRef.id)
          } catch (nested: Exception) {
            if (nested is CancellationException) throw nested
            handleFirestoreError(nested, OperationType.UPDATE, docRef.path)
            Result.failure(nested)
          }
        }
      }
      handleFirestoreError(
        e,
        if (isUpdate) OperationType.UPDATE else OperationType.CREATE,
        docRef.path,
      )
      Result.failure(e)
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isUpdate) OperationType.UPDATE else OperationType.CREATE,
        docRef.path,
      )
      Result.failure(e)
    }
  }

  override fun observeRoutes(): Flow<List<DeliveryRoute>> = flowOf(RoutesCatalog.starterRoutes)

  override suspend fun seedRoutesIfNeeded(): Result<Unit> = Result.success(Unit)

  override suspend fun saveRoute(route: DeliveryRoute): Result<Unit> = Result.success(Unit)

  override suspend fun updateShopRoute(shopId: String, newRouteId: String): Result<Unit> =
    updateShopRouteWithAck(shopId, newRouteId, awaitServerAck = false)

  suspend fun updateShopRouteWithAck(
    shopId: String,
    newRouteId: String,
    awaitServerAck: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("shops").document(shopId)
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val task = docRef.update(
        mapOf(
          "routeId" to newRouteId,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun updateShopStatus(shopId: String, newStatus: String): Result<Unit> =
    updateShopStatusWithAck(shopId, newStatus, awaitServerAck = false)

  suspend fun updateShopStatusWithAck(
    shopId: String,
    newStatus: String,
    awaitServerAck: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("shops").document(shopId)
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val task = docRef.update(
        mapOf(
          "status" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun updateShopInquiryStatus(
    shopId: String,
    inquiryStatus: String,
    markCalledToday: Boolean,
  ): Result<Unit> =
    updateShopInquiryStatusWithAck(shopId, inquiryStatus, markCalledToday, awaitServerAck = false)

  suspend fun updateShopInquiryStatusWithAck(
    shopId: String,
    inquiryStatus: String,
    markCalledToday: Boolean = true,
    awaitServerAck: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("shops").document(shopId)
    val updates = mutableMapOf<String, Any>(
      "inquiryStatus" to inquiryStatus,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (markCalledToday) {
      updates["lastCallDate"] = todayString()
    }
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val task = docRef.update(updates)
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun placeOrder(
    shop: Shop,
    items: List<Pair<CatalogProduct, Int>>,
    note: String,
  ): Result<String> {
    val orderCode = "ORD-${(System.currentTimeMillis() % 9000 + 1000)}"
    return placeOrderWithId(
      orderId = "",
      orderCode = orderCode,
      shop = shop,
      items = items,
      note = note,
    )
  }

  suspend fun placeOrderWithId(
    orderId: String,
    orderCode: String,
    shop: Shop,
    items: List<Pair<CatalogProduct, Int>>,
    note: String,
  ): Result<String> {
    val uid = requireUserId()
    val validItems = items.filter { it.second > 0 }
    if (validItems.isEmpty()) {
      return Result.failure(IllegalArgumentException("Select at least one product to place an order."))
    }

    val ordersCol = db.collection("users").document(uid).collection("orders")
    val orderDoc = if (orderId.isNotBlank()) ordersCol.document(orderId) else ordersCol.document()
    val shopDoc = db.collection("users").document(uid).collection("shops").document(shop.id)
    val today = todayString()
    val totalAmount = validItems.sumOf { (prod, qty) -> prod.defaultPrice * qty }
    val itemCount = validItems.sumOf { it.second }
    val itemsSummary = validItems.joinToString(", ") { (prod, qty) -> "${prod.code} x$qty" }.take(780)

    return placeRawOrder(
      orderDocId = orderDoc.id,
      orderCode = orderCode,
      shopId = shop.id,
      shopCode = shop.shopCode,
      shopName = shop.name,
      routeId = shop.routeId,
      status = OrderStatus.CONFIRMED,
      totalAmount = totalAmount,
      itemCount = itemCount,
      itemsSummary = itemsSummary,
      orderDate = today,
      note = note,
    )
  }

  suspend fun placeRawOrder(
    orderDocId: String,
    orderCode: String,
    shopId: String,
    shopCode: String,
    shopName: String,
    routeId: String,
    status: String,
    totalAmount: Double,
    itemCount: Int,
    itemsSummary: String,
    orderDate: String,
    note: String,
    awaitServerAck: Boolean = false,
  ): Result<String> {
    val uid = requireUserId()
    val orderDoc = db.collection("users").document(uid).collection("orders").document(orderDocId)
    val shopDoc = db.collection("users").document(uid).collection("shops").document(shopId)
    val today = todayString()

    return try {
      val batch = db.batch()
      batch.set(
        orderDoc,
        mapOf(
          "userId" to uid,
          "orderCode" to orderCode,
          "shopId" to shopId,
          "shopCode" to shopCode,
          "shopName" to shopName,
          "routeId" to routeId,
          "status" to status,
          "totalAmount" to totalAmount,
          "itemCount" to itemCount,
          "itemsSummary" to itemsSummary.take(780),
          "orderDate" to orderDate.ifBlank { today },
          "note" to note.trim(),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        shopDoc,
        mapOf(
          "inquiryStatus" to InquiryStatus.CONFIRMED,
          "lastOrderDate" to orderDate.ifBlank { today },
          "lastCallDate" to today,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      val task = batch.commit()
      val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(orderDoc.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
        // Shop doc may only live on laptop server or not yet on Firestore; write order standalone.
        return try {
          val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
          val acked = withTimeoutOrNull(waitMs) {
            orderDoc.set(
              mapOf(
                "userId" to uid,
                "orderCode" to orderCode,
                "shopId" to shopId,
                "shopCode" to shopCode,
                "shopName" to shopName,
                "routeId" to routeId,
                "status" to status,
                "totalAmount" to totalAmount,
                "itemCount" to itemCount,
                "itemsSummary" to itemsSummary.take(780),
                "orderDate" to orderDate.ifBlank { today },
                "note" to note.trim(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            ).await()
            true
          } ?: false
          if (awaitServerAck && !acked) {
            return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
          }
          Result.success(orderDoc.id)
        } catch (nested: Exception) {
          if (nested is CancellationException) throw nested
          handleFirestoreError(nested, OperationType.CREATE, orderDoc.path)
          Result.failure(nested)
        }
      }
      handleFirestoreError(e, OperationType.CREATE, orderDoc.path)
      Result.failure(e)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, orderDoc.path)
      Result.failure(e)
    }
  }

  override suspend fun updateOrderStatus(
    orderId: String,
    newStatus: String,
    note: String?,
  ): Result<Unit> = updateOrderStatusWithAck(
    orderId = orderId,
    newStatus = newStatus,
    note = note,
    awaitServerAck = false,
  )

  suspend fun updateOrderStatusWithAck(
    orderId: String,
    newStatus: String,
    note: String? = null,
    awaitServerAck: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("orders").document(orderId)
    val updates = mutableMapOf<String, Any>(
      "status" to newStatus,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (note != null) {
      updates["note"] = note.trim().take(500)
    }
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val task = docRef.update(updates)
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun updateOrderDetails(
    orderId: String,
    items: List<Pair<CatalogProduct, Int>>,
    note: String,
  ): Result<Unit> {
    val validItems = items.filter { it.second > 0 }
    if (validItems.isEmpty()) {
      return Result.failure(IllegalArgumentException("Select at least one product to update the order."))
    }
    return updateOrderDetailsWithAck(
      orderId = orderId,
      totalAmount = validItems.sumOf { (prod, qty) -> prod.defaultPrice * qty },
      itemCount = validItems.sumOf { it.second },
      itemsSummary = validItems.joinToString(", ") { (prod, qty) -> "${prod.code} x$qty" }.take(780),
      note = note,
      awaitServerAck = false,
    )
  }

  suspend fun updateOrderDetailsWithAck(
    orderId: String,
    totalAmount: Double,
    itemCount: Int,
    itemsSummary: String,
    note: String,
    awaitServerAck: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("orders").document(orderId)
    val updates = mapOf<String, Any>(
      "totalAmount" to totalAmount.coerceAtLeast(0.0),
      "itemCount" to itemCount.coerceIn(1, 100000),
      "itemsSummary" to itemsSummary.trim().ifBlank { "Order items" }.take(780),
      "note" to note.trim().take(500),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val task = docRef.update(updates)
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun deleteOrder(orderId: String): Result<Unit> =
    deleteOrderWithAck(orderId = orderId, awaitServerAck = false)

  suspend fun deleteOrderWithAck(
    orderId: String,
    awaitServerAck: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("orders").document(orderId)
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val task = docRef.delete()
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
        Result.success(Unit)
      } else {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        Result.failure(e)
      }
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun updateStockQuantity(stockId: String, newQuantity: Int): Result<Unit> =
    updateStockQuantityWithAck(stockId, newQuantity, awaitServerAck = false)

  suspend fun updateStockQuantityWithAck(
    stockId: String,
    newQuantity: Int,
    awaitServerAck: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("stocks").document(stockId)
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val safeQty = newQuantity.coerceAtLeast(0)
      val task = docRef.update(
        mapOf(
          "quantity" to safeQty,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getShopsOnce(userId: String): Result<List<Shop>> {
    val path = "users/$userId/shops"
    return try {
      val snapshot = db.collection("users").document(userId).collection("shops").get().await()
      val items = snapshot.documents.mapNotNull {
        it.toObject(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      Result.success(items)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  suspend fun getOrdersOnce(userId: String): Result<List<OrderRecord>> {
    val path = "users/$userId/orders"
    return try {
      val snapshot = db.collection("users").document(userId).collection("orders").get().await()
      val items = snapshot.documents.mapNotNull {
        it.toObject(OrderRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      Result.success(items)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  override suspend fun saveProduct(product: Product): Result<String> =
    saveProductWithId(product = product, isUpdate = product.id.isNotBlank(), awaitServerAck = false)

  suspend fun saveProductWithId(
    product: Product,
    isUpdate: Boolean,
    awaitServerAck: Boolean = false,
  ): Result<String> {
    val uid = requireUserId()
    val col = db.collection("users").document(uid).collection("products")
    val docRef = if (product.id.isNotBlank()) col.document(product.id) else col.document()
    val today = todayString()

    val createMap = mapOf(
      "userId" to uid,
      "productCode" to product.productCode.trim().take(32),
      "sku" to product.sku.trim().take(40),
      "barcode" to product.barcode.trim().take(64),
      "name" to product.name.trim().take(140),
      "category" to product.category.trim().ifBlank { "Biscuits & Snacks" }.take(80),
      "brand" to product.brand.trim().take(100),
      "baseUnit" to product.baseUnit.trim().ifBlank { "Gram (g)" }.take(40),
      "sellingUnit" to product.sellingUnit.trim().ifBlank { "Pouch / Packet" }.take(40),
      "casePack" to product.casePack.trim().take(200),
      "packSize" to product.packSize.trim().take(64),
      "wholesalePrice" to product.wholesalePrice.coerceAtLeast(0.0),
      "mrp" to product.mrp.coerceAtLeast(0.0),
      "stockQuantity" to product.stockQuantity.coerceIn(0, 1000000),
      "minThreshold" to product.minThreshold.coerceIn(0, 100000),
      "photoUri" to product.photoUri.take(600),
      "notes" to product.notes.trim().take(600),
      "status" to product.status,
      "salesVelocity" to product.salesVelocity.coerceIn(0, 1000000),
      "hasVariants" to product.hasVariants,
      "variantsJson" to product.variantsJson.take(4000),
      "addedDate" to product.addedDate.ifBlank { today }.take(32),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val updateMap = mapOf(
      "productCode" to product.productCode.trim().take(32),
      "sku" to product.sku.trim().take(40),
      "barcode" to product.barcode.trim().take(64),
      "name" to product.name.trim().take(140),
      "category" to product.category.trim().ifBlank { "Biscuits & Snacks" }.take(80),
      "brand" to product.brand.trim().take(100),
      "baseUnit" to product.baseUnit.trim().ifBlank { "Gram (g)" }.take(40),
      "sellingUnit" to product.sellingUnit.trim().ifBlank { "Pouch / Packet" }.take(40),
      "casePack" to product.casePack.trim().take(200),
      "packSize" to product.packSize.trim().take(64),
      "wholesalePrice" to product.wholesalePrice.coerceAtLeast(0.0),
      "mrp" to product.mrp.coerceAtLeast(0.0),
      "stockQuantity" to product.stockQuantity.coerceIn(0, 1000000),
      "minThreshold" to product.minThreshold.coerceIn(0, 100000),
      "photoUri" to product.photoUri.take(600),
      "notes" to product.notes.trim().take(600),
      "status" to product.status,
      "salesVelocity" to product.salesVelocity.coerceIn(0, 1000000),
      "hasVariants" to product.hasVariants,
      "variantsJson" to product.variantsJson.take(4000),
      "addedDate" to product.addedDate.ifBlank { today }.take(32),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val actuallyUpdate = if (awaitServerAck && !isUpdate && product.id.isNotBlank()) {
        val snap = withTimeoutOrNull(waitMs) { docRef.get().await() }
        snap?.exists() == true
      } else {
        isUpdate
      }
      val task = if (actuallyUpdate) docRef.update(updateMap) else docRef.set(createMap)
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (isUpdate && e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
        return try {
          val acked = withTimeoutOrNull(waitMs) {
            docRef.set(createMap).await()
            true
          } ?: false
          if (awaitServerAck && !acked) {
            return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
          }
          Result.success(docRef.id)
        } catch (nested: Exception) {
          if (nested is CancellationException) throw nested
          handleFirestoreError(nested, OperationType.CREATE, docRef.path)
          Result.failure(nested)
        }
      }
      handleFirestoreError(
        e,
        if (isUpdate) OperationType.UPDATE else OperationType.CREATE,
        docRef.path,
      )
      Result.failure(e)
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isUpdate) OperationType.UPDATE else OperationType.CREATE,
        docRef.path,
      )
      Result.failure(e)
    }
  }

  override suspend fun updateProductStatus(productId: String, newStatus: String): Result<Unit> =
    updateProductStatusWithAck(productId, newStatus, awaitServerAck = false)

  suspend fun updateProductStatusWithAck(
    productId: String,
    newStatus: String,
    awaitServerAck: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("products").document(productId)
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val task = docRef.update(
        mapOf(
          "status" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun updateProductStock(productId: String, newQuantity: Int): Result<Unit> =
    updateProductStockWithAck(productId, newQuantity, awaitServerAck = false)

  suspend fun updateProductStockWithAck(
    productId: String,
    newQuantity: Int,
    awaitServerAck: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("products").document(productId)
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val safeQty = newQuantity.coerceIn(0, 1000000)
      val task = docRef.update(
        mapOf(
          "stockQuantity" to safeQty,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getProductsOnce(userId: String): Result<List<Product>> {
    val path = "users/$userId/products"
    return try {
      val snapshot = db.collection("users").document(userId).collection("products").get().await()
      val items = snapshot.documents.mapNotNull {
        it.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      Result.success(items)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  private fun productPayloadMap(uid: String, prod: Product): Map<String, Any> = mapOf(
    "userId" to uid,
    "productCode" to prod.productCode,
    "sku" to prod.sku,
    "barcode" to prod.barcode,
    "name" to prod.name,
    "category" to prod.category,
    "brand" to prod.brand,
    "baseUnit" to prod.baseUnit,
    "sellingUnit" to prod.sellingUnit,
    "casePack" to prod.casePack,
    "packSize" to prod.packSize,
    "wholesalePrice" to prod.wholesalePrice,
    "mrp" to prod.mrp,
    "stockQuantity" to prod.stockQuantity,
    "minThreshold" to prod.minThreshold,
    "photoUri" to prod.photoUri,
    "notes" to prod.notes,
    "status" to prod.status,
    "salesVelocity" to prod.salesVelocity,
    "hasVariants" to prod.hasVariants,
    "variantsJson" to prod.variantsJson,
    "addedDate" to prod.addedDate,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
  )

  private fun paymentPayloadMap(uid: String, payment: PaymentTransaction, today: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "receiptCode" to payment.receiptCode.trim().take(40),
    "direction" to payment.direction,
    "partyId" to payment.partyId.trim().take(128),
    "partyCode" to payment.partyCode.trim().take(32),
    "partyName" to payment.partyName.trim().take(140),
    "contactName" to payment.contactName.trim().take(100),
    "phone" to payment.phone.trim().take(24),
    "invoiceRef" to payment.invoiceRef.trim().take(64),
    "amount" to payment.amount.coerceAtLeast(0.01),
    "paymentMethod" to payment.paymentMethod,
    "referenceNumber" to payment.referenceNumber.trim().take(80),
    "paymentDate" to payment.paymentDate.ifBlank { today }.take(32),
    "recordedBy" to payment.recordedBy.trim().take(100),
    "note" to payment.note.trim().take(500),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
  )

  private fun supplierBillPayloadMap(uid: String, bill: SupplierBill, today: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "supplierCode" to bill.supplierCode.trim().take(32),
    "supplierName" to bill.supplierName.trim().take(140),
    "contactPerson" to bill.contactPerson.trim().take(100),
    "phone" to bill.phone.trim().take(24),
    "location" to bill.location.trim().take(140),
    "category" to bill.category.trim().take(64),
    "billNumber" to bill.billNumber.trim().take(40),
    "totalBilled" to bill.totalBilled.coerceAtLeast(0.0),
    "paidAmount" to bill.paidAmount.coerceAtLeast(0.0),
    "billDate" to bill.billDate.ifBlank { today }.take(32),
    "dueDate" to bill.dueDate.ifBlank { daysOffsetString(7) }.take(32),
    "status" to bill.status,
    "notes" to bill.notes.trim().take(500),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
  )

  override suspend fun recordPayment(payment: PaymentTransaction): Result<String> =
    savePaymentWithId(payment = payment, isUpdate = payment.id.isNotBlank(), awaitServerAck = false)

  suspend fun savePaymentWithId(
    payment: PaymentTransaction,
    isUpdate: Boolean,
    awaitServerAck: Boolean = false,
  ): Result<String> {
    if (payment.amount <= 0.0) {
      return Result.failure(IllegalArgumentException("Payment amount must be greater than zero."))
    }
    val uid = requireUserId()
    val col = db.collection("users").document(uid).collection("payments")
    val docRef = if (payment.id.isNotBlank()) col.document(payment.id) else col.document()
    val today = todayString()
    val normalized = payment.copy(
      receiptCode = payment.receiptCode.ifBlank {
        val prefix = if (payment.direction == PaymentDirection.PAY_SUPPLIER) "PAYOUT" else "RCPT"
        "$prefix-2026-${(System.currentTimeMillis() % 9000 + 1000)}"
      },
      paymentDate = payment.paymentDate.ifBlank { today },
      paymentMethod = if (payment.paymentMethod in PaymentMethod.allMethods) payment.paymentMethod else PaymentMethod.UPI,
      direction = if (payment.direction in listOf(PaymentDirection.COLLECT, PaymentDirection.PAY_SUPPLIER)) {
        payment.direction
      } else {
        PaymentDirection.COLLECT
      },
    )
    val createMap = paymentPayloadMap(uid, normalized, today)
    val updateMap = mapOf(
      "invoiceRef" to normalized.invoiceRef.trim().take(64),
      "referenceNumber" to normalized.referenceNumber.trim().take(80),
      "note" to normalized.note.trim().take(500),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val actuallyUpdate = if (awaitServerAck && !isUpdate && payment.id.isNotBlank()) {
        val snap = withTimeoutOrNull(waitMs) { docRef.get().await() }
        snap?.exists() == true
      } else {
        isUpdate
      }
      val task = if (actuallyUpdate) docRef.update(updateMap) else docRef.set(createMap)
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (isUpdate && e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
        return try {
          val acked = withTimeoutOrNull(waitMs) {
            docRef.set(createMap).await()
            true
          } ?: false
          if (awaitServerAck && !acked) {
            return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
          }
          Result.success(docRef.id)
        } catch (nested: Exception) {
          if (nested is CancellationException) throw nested
          handleFirestoreError(nested, OperationType.CREATE, docRef.path)
          Result.failure(nested)
        }
      }
      handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docRef.path)
      Result.failure(e)
    } catch (e: Exception) {
      handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun saveSupplierBill(bill: SupplierBill): Result<String> =
    saveSupplierBillWithId(bill = bill, isUpdate = bill.id.isNotBlank(), awaitServerAck = false)

  suspend fun saveSupplierBillWithId(
    bill: SupplierBill,
    isUpdate: Boolean,
    awaitServerAck: Boolean = false,
  ): Result<String> {
    val uid = requireUserId()
    val col = db.collection("users").document(uid).collection("supplierBills")
    val docRef = if (bill.id.isNotBlank()) col.document(bill.id) else col.document()
    val today = todayString()
    val normalizedStatus = PaymentLedgerHelper.effectiveSupplierStatus(bill)
    val normalized = bill.copy(
      status = if (normalizedStatus in listOf(SupplierBillStatus.PENDING, SupplierBillStatus.OVERDUE, SupplierBillStatus.SETTLED)) {
        normalizedStatus
      } else {
        SupplierBillStatus.PENDING
      },
      billDate = bill.billDate.ifBlank { today },
      dueDate = bill.dueDate.ifBlank { daysOffsetString(7) },
    )
    val createMap = supplierBillPayloadMap(uid, normalized, today)
    val updateMap = mapOf(
      "supplierCode" to normalized.supplierCode.trim().take(32),
      "supplierName" to normalized.supplierName.trim().take(140),
      "contactPerson" to normalized.contactPerson.trim().take(100),
      "phone" to normalized.phone.trim().take(24),
      "location" to normalized.location.trim().take(140),
      "category" to normalized.category.trim().take(64),
      "billNumber" to normalized.billNumber.trim().take(40),
      "totalBilled" to normalized.totalBilled.coerceAtLeast(0.0),
      "paidAmount" to normalized.paidAmount.coerceAtLeast(0.0),
      "billDate" to normalized.billDate.take(32),
      "dueDate" to normalized.dueDate.take(32),
      "status" to normalized.status,
      "notes" to normalized.notes.trim().take(500),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val actuallyUpdate = if (awaitServerAck && !isUpdate && bill.id.isNotBlank()) {
        val snap = withTimeoutOrNull(waitMs) { docRef.get().await() }
        snap?.exists() == true
      } else {
        isUpdate
      }
      val task = if (actuallyUpdate) docRef.update(updateMap) else docRef.set(createMap)
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (isUpdate && e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
        return try {
          val acked = withTimeoutOrNull(waitMs) {
            docRef.set(createMap).await()
            true
          } ?: false
          if (awaitServerAck && !acked) {
            return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
          }
          Result.success(docRef.id)
        } catch (nested: Exception) {
          if (nested is CancellationException) throw nested
          handleFirestoreError(nested, OperationType.CREATE, docRef.path)
          Result.failure(nested)
        }
      }
      handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docRef.path)
      Result.failure(e)
    } catch (e: Exception) {
      handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getPaymentsOnce(userId: String): Result<List<PaymentTransaction>> {
    val path = "users/$userId/payments"
    return try {
      val snapshot = db.collection("users").document(userId).collection("payments").get().await()
      val items = snapshot.documents.mapNotNull {
        it.toObject(PaymentTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      Result.success(items)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  suspend fun getSupplierBillsOnce(userId: String): Result<List<SupplierBill>> {
    val path = "users/$userId/supplierBills"
    return try {
      val snapshot = db.collection("users").document(userId).collection("supplierBills").get().await()
      val items = snapshot.documents.mapNotNull {
        it.toObject(SupplierBill::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      Result.success(items)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  private fun supplierPayloadMap(uid: String, supplier: Supplier, today: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "supplierCode" to supplier.supplierCode.trim().ifBlank { "SUP-000001" }.take(32),
    "name" to supplier.name.trim().ifBlank { "Supplier" }.take(140),
    "category" to supplier.category.trim().ifBlank { "Wholesale Staples & Grains" }.take(80),
    "contactPerson" to supplier.contactPerson.trim().ifBlank { "Contact Person" }.take(100),
    "contactRole" to supplier.contactRole.trim().take(64),
    "altContactPerson" to supplier.altContactPerson.trim().take(100),
    "phone" to supplier.phone.trim().let { if (it.length >= 5) it else "00000" }.take(24),
    "altPhone" to supplier.altPhone.trim().take(24),
    "email" to supplier.email.trim().take(120),
    "address" to supplier.address.trim().ifBlank { "Address pending" }.take(300),
    "pinCode" to supplier.pinCode.trim().let { if (it.length >= 3) it else "000000" }.take(16),
    "mapsLink" to supplier.mapsLink.trim().take(600),
    "latitude" to supplier.latitude,
    "longitude" to supplier.longitude,
    "photoUri" to supplier.photoUri.take(600),
    "gstin" to supplier.gstin.trim().take(32),
    "kycVerified" to supplier.kycVerified,
    "creditTerms" to supplier.creditTerms.trim().take(64),
    "confirmedPriceNotes" to supplier.confirmedPriceNotes.trim().take(600),
    "notes" to supplier.notes.trim().take(600),
    "status" to if (supplier.status in listOf(SupplierStatus.ACTIVE, SupplierStatus.INACTIVE, SupplierStatus.DELETED)) {
      supplier.status
    } else {
      SupplierStatus.ACTIVE
    },
    "addedDate" to supplier.addedDate.ifBlank { today }.take(32),
    "addedBy" to supplier.addedBy.trim().take(120),
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
  )

  override suspend fun saveSupplier(supplier: Supplier): Result<String> =
    saveSupplierWithId(supplier = supplier, isUpdate = supplier.id.isNotBlank(), awaitServerAck = false)

  suspend fun saveSupplierWithId(
    supplier: Supplier,
    isUpdate: Boolean,
    awaitServerAck: Boolean = false,
  ): Result<String> {
    val uid = requireUserId()
    val col = db.collection("users").document(uid).collection("suppliers")
    val docRef = if (supplier.id.isNotBlank()) col.document(supplier.id) else col.document()
    val today = todayString()
    val createMap = supplierPayloadMap(uid, supplier, today)
    val updateMap = mapOf(
      "supplierCode" to supplier.supplierCode.trim().ifBlank { "SUP-000001" }.take(32),
      "name" to supplier.name.trim().ifBlank { "Supplier" }.take(140),
      "category" to supplier.category.trim().ifBlank { "Wholesale Staples & Grains" }.take(80),
      "contactPerson" to supplier.contactPerson.trim().ifBlank { "Contact Person" }.take(100),
      "contactRole" to supplier.contactRole.trim().take(64),
      "altContactPerson" to supplier.altContactPerson.trim().take(100),
      "phone" to supplier.phone.trim().let { if (it.length >= 5) it else "00000" }.take(24),
      "altPhone" to supplier.altPhone.trim().take(24),
      "email" to supplier.email.trim().take(120),
      "address" to supplier.address.trim().ifBlank { "Address pending" }.take(300),
      "pinCode" to supplier.pinCode.trim().let { if (it.length >= 3) it else "000000" }.take(16),
      "mapsLink" to supplier.mapsLink.trim().take(600),
      "latitude" to supplier.latitude,
      "longitude" to supplier.longitude,
      "photoUri" to supplier.photoUri.take(600),
      "gstin" to supplier.gstin.trim().take(32),
      "kycVerified" to supplier.kycVerified,
      "creditTerms" to supplier.creditTerms.trim().take(64),
      "confirmedPriceNotes" to supplier.confirmedPriceNotes.trim().take(600),
      "notes" to supplier.notes.trim().take(600),
      "status" to if (supplier.status in listOf(SupplierStatus.ACTIVE, SupplierStatus.INACTIVE, SupplierStatus.DELETED)) {
        supplier.status
      } else {
        SupplierStatus.ACTIVE
      },
      "addedDate" to supplier.addedDate.ifBlank { today }.take(32),
      "addedBy" to supplier.addedBy.trim().take(120),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val actuallyUpdate = if (awaitServerAck && !isUpdate && supplier.id.isNotBlank()) {
        val snap = withTimeoutOrNull(waitMs) { docRef.get().await() }
        snap?.exists() == true
      } else {
        isUpdate
      }
      val task = if (actuallyUpdate) docRef.update(updateMap) else docRef.set(createMap)
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (isUpdate && e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
        return try {
          val acked = withTimeoutOrNull(waitMs) {
            docRef.set(createMap).await()
            true
          } ?: false
          if (awaitServerAck && !acked) {
            return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
          }
          Result.success(docRef.id)
        } catch (nested: Exception) {
          if (nested is CancellationException) throw nested
          handleFirestoreError(nested, OperationType.CREATE, docRef.path)
          Result.failure(nested)
        }
      }
      handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docRef.path)
      Result.failure(e)
    } catch (e: Exception) {
      handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun updateSupplierStatus(supplierId: String, newStatus: String): Result<Unit> =
    updateSupplierStatusWithAck(supplierId, newStatus, awaitServerAck = false)

  suspend fun updateSupplierStatusWithAck(
    supplierId: String,
    newStatus: String,
    awaitServerAck: Boolean = true,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("suppliers").document(supplierId)
    val waitMs = if (awaitServerAck) SERVER_ACK_TIMEOUT_MS else OFFLINE_WRITE_GRACE_MS
    return try {
      val task = docRef.update(
        mapOf(
          "status" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
      val acked = withTimeoutOrNull(waitMs) {
        task.await()
        true
      } ?: false
      if (awaitServerAck && !acked) {
        return Result.failure(IllegalStateException("Timed out waiting for Global Cloud server"))
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getSuppliersOnce(userId: String): Result<List<Supplier>> {
    val path = "users/$userId/suppliers"
    return try {
      val snapshot = db.collection("users").document(userId).collection("suppliers").get().await()
      val items = snapshot.documents.mapNotNull {
        it.toObject(Supplier::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      Result.success(items)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  override suspend fun seedStarterDataIfNeeded(): Result<Unit> {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val metaDocRef = userRef.collection("meta").document("summary")
    return try {
      val existingMeta = withTimeoutOrNull(SERVER_ACK_TIMEOUT_MS) { metaDocRef.get().await() }
        ?: return Result.failure(IllegalStateException("Timed out checking Cloud starter data"))
      if (existingMeta.exists()) {
        // Also ensure starter products, supplierBills, payments & suppliers are seeded for returning accounts
        val existingProducts = withTimeoutOrNull(SERVER_ACK_TIMEOUT_MS) {
          userRef.collection("products").limit(1).get().await()
        }
        if (existingProducts != null && existingProducts.isEmpty) {
          val prodBatch = db.batch()
          ProductCatalogHelper.starterProducts(uid, todayString(), ::daysAgoString).forEach { prod ->
            val doc = userRef.collection("products").document(prod.id)
            prodBatch.set(doc, productPayloadMap(uid, prod))
          }
          prodBatch.commit().await()
        }
        val existingBills = withTimeoutOrNull(SERVER_ACK_TIMEOUT_MS) {
          userRef.collection("supplierBills").limit(1).get().await()
        }
        if (existingBills != null && existingBills.isEmpty) {
          val todayStr = todayString()
          val billBatch = db.batch()
          PaymentLedgerHelper.starterSupplierBills(uid, todayStr, ::daysOffsetString).forEach { bill ->
            val doc = userRef.collection("supplierBills").document(bill.id)
            billBatch.set(doc, supplierBillPayloadMap(uid, bill, todayStr))
          }
          PaymentLedgerHelper.starterPayments(uid, todayStr, daysAgoString(1)).forEach { pay ->
            val mapped = when (pay.partyCode) {
              "SHP-101" -> pay.copy(partyId = "seed_shop_1")
              "SHP-104" -> pay.copy(partyId = "seed_shop_4")
              else -> pay
            }
            val doc = userRef.collection("payments").document(mapped.id)
            billBatch.set(doc, paymentPayloadMap(uid, mapped, todayStr))
          }
          billBatch.commit().await()
        }
        val existingSuppliers = withTimeoutOrNull(SERVER_ACK_TIMEOUT_MS) {
          userRef.collection("suppliers").limit(1).get().await()
        }
        if (existingSuppliers != null && existingSuppliers.isEmpty) {
          val todayStr = todayString()
          val supBatch = db.batch()
          SupplierCatalogHelper.starterSuppliers(uid, todayStr, ::daysAgoString).forEach { sup ->
            val doc = userRef.collection("suppliers").document(sup.id)
            supBatch.set(doc, supplierPayloadMap(uid, sup, todayStr))
          }
          supBatch.commit().await()
        }
        return Result.success(Unit)
      }
      val existingShops = withTimeoutOrNull(SERVER_ACK_TIMEOUT_MS) {
        userRef.collection("shops").limit(1).get().await()
      } ?: return Result.failure(IllegalStateException("Timed out checking Cloud starter shops"))
      if (!existingShops.isEmpty) {
        return Result.success(Unit)
      }

      val today = todayString()
      val yesterday = daysAgoString(1)
      val threeDaysAgo = daysAgoString(3)
      val sevenDaysAgo = daysAgoString(7)

      val batch = db.batch()

      batch.set(
        metaDocRef,
        mapOf(
          "userId" to uid,
          "shopRegTarget" to 10,
          "vehiclesToDispatch" to 3,
          "vehiclesOnRoad" to 2,
          "returnsCollected" to 4,
          "toCollectAmount" to 46800.0,
          "toCollectShops" to 11,
          "toSuppliersAmount" to 28500.0,
          "toSuppliersCount" to 4,
          "seeded" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      val starterShops = listOf(
        mapOf(
          "userId" to uid,
          "shopCode" to "SHP-101",
          "name" to "Malabar Heritage Bakery",
          "ownerName" to "Suresh Nair",
          "phone" to "9847012345",
          "secondaryPhone" to "9847011223",
          "address" to "Fort Road, Near Town Hall, Kannur",
          "pinCode" to "670001",
          "routeId" to RoutesCatalog.R01_KANNUR_BANGALORE,
          "photoUri" to "",
          "latitude" to 11.8745,
          "longitude" to 75.3704,
          "note" to "Takes weekly Banana Chips & Mixture crates",
          "status" to ShopStatus.ACTIVE,
          "inquiryStatus" to InquiryStatus.PENDING_INQUIRY,
          "enquiryDate" to today,
          "callDate" to today,
          "lastOrderDate" to threeDaysAgo,
          "lastCallDate" to yesterday,
          "confirmedPriceNotes" to "BC100: ₹42, JB200: ₹92",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "shopCode" to "SHP-102",
          "name" to "Kaveri Super Mart",
          "ownerName" to "Prakash Shetty",
          "phone" to "9448023456",
          "secondaryPhone" to "",
          "address" to "12th Cross, Malleshwaram, Bangalore",
          "pinCode" to "560003",
          "routeId" to RoutesCatalog.R01_KANNUR_BANGALORE,
          "photoUri" to "",
          "latitude" to 13.0031,
          "longitude" to 77.5643,
          "note" to "Call before 11 AM for weekend stock",
          "status" to ShopStatus.ACTIVE,
          "inquiryStatus" to InquiryStatus.PENDING_INQUIRY,
          "enquiryDate" to today,
          "callDate" to today,
          "lastOrderDate" to sevenDaysAgo,
          "lastCallDate" to threeDaysAgo,
          "confirmedPriceNotes" to "BC200: ₹85",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "shopCode" to "SHP-103",
          "name" to "Calicut Spice & Bakes",
          "ownerName" to "Faisal Rahman",
          "phone" to "9895034567",
          "secondaryPhone" to "9895034500",
          "address" to "SM Street, Palayam, Kozhikode",
          "pinCode" to "673001",
          "routeId" to RoutesCatalog.R02_KANNUR_KOZHIKODE,
          "photoUri" to "",
          "latitude" to 11.2588,
          "longitude" to 75.7804,
          "note" to "Confirmed morning route",
          "status" to ShopStatus.ACTIVE,
          "inquiryStatus" to InquiryStatus.CONFIRMED,
          "enquiryDate" to today,
          "callDate" to today,
          "lastOrderDate" to today,
          "lastCallDate" to today,
          "confirmedPriceNotes" to "JB200: ₹90, MX500: ₹162",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "shopCode" to "SHP-104",
          "name" to "Deccan Fresh Corner",
          "ownerName" to "Anand Kulkarni",
          "phone" to "9822045678",
          "secondaryPhone" to "",
          "address" to "FC Road, Shivajinagar, Pune",
          "pinCode" to "411004",
          "routeId" to RoutesCatalog.R03_BANGALORE_PUNE,
          "photoUri" to "",
          "latitude" to 18.5204,
          "longitude" to 73.8567,
          "note" to "Owner unavailable on Monday mornings",
          "status" to ShopStatus.ACTIVE,
          "inquiryStatus" to InquiryStatus.UNATTENDED,
          "enquiryDate" to yesterday,
          "callDate" to today,
          "lastOrderDate" to sevenDaysAgo,
          "lastCallDate" to yesterday,
          "confirmedPriceNotes" to "",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "shopCode" to "SHP-105",
          "name" to "Thalassery Chips Centre",
          "ownerName" to "Meera Krishnan",
          "phone" to "9745056789",
          "secondaryPhone" to "",
          "address" to "Logans Road, Thalassery",
          "pinCode" to "670101",
          "routeId" to RoutesCatalog.R02_KANNUR_KOZHIKODE,
          "photoUri" to "",
          "latitude" to 11.7491,
          "longitude" to 75.4890,
          "note" to "Stock sufficient until Friday",
          "status" to ShopStatus.ACTIVE,
          "inquiryStatus" to InquiryStatus.NO_ORDER,
          "enquiryDate" to yesterday,
          "callDate" to today,
          "lastOrderDate" to threeDaysAgo,
          "lastCallDate" to today,
          "confirmedPriceNotes" to "",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "shopCode" to "SHP-106",
          "name" to "Green Valley Provision",
          "ownerName" to "Ramesh Gowda",
          "phone" to "9900167890",
          "secondaryPhone" to "",
          "address" to "Jayanagar 4th Block, Bangalore",
          "pinCode" to "560011",
          "routeId" to RoutesCatalog.R01_KANNUR_BANGALORE,
          "photoUri" to "",
          "latitude" to 12.9250,
          "longitude" to 77.5938,
          "note" to "Temporarily closed for renovation",
          "status" to ShopStatus.INACTIVE,
          "inquiryStatus" to InquiryStatus.NO_ORDER,
          "enquiryDate" to sevenDaysAgo,
          "callDate" to sevenDaysAgo,
          "lastOrderDate" to sevenDaysAgo,
          "lastCallDate" to sevenDaysAgo,
          "confirmedPriceNotes" to "",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      val shopIds = mutableListOf<String>()
      starterShops.forEachIndexed { idx, payload ->
        val doc = userRef.collection("shops").document("seed_shop_${idx + 1}")
        shopIds.add(doc.id)
        batch.set(doc, payload)
      }

      val starterOrders = listOf(
        mapOf(
          "userId" to uid,
          "orderCode" to "ORD-2041",
          "shopId" to shopIds[2],
          "shopCode" to "SHP-103",
          "shopName" to "Calicut Spice & Bakes",
          "routeId" to RoutesCatalog.R02_KANNUR_KOZHIKODE,
          "status" to OrderStatus.CONFIRMED,
          "totalAmount" to 6420.0,
          "itemCount" to 60,
          "itemsSummary" to "JB200 x40, MX500 x20",
          "orderDate" to today,
          "note" to "Morning van dispatch",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "orderCode" to "ORD-2040",
          "shopId" to shopIds[0],
          "shopCode" to "SHP-101",
          "shopName" to "Malabar Heritage Bakery",
          "routeId" to RoutesCatalog.R01_KANNUR_BANGALORE,
          "status" to OrderStatus.DELIVERED,
          "totalAmount" to 8950.0,
          "itemCount" to 95,
          "itemsSummary" to "BC100 x50, BC200 x30, JB200 x15",
          "orderDate" to today,
          "note" to "Delivered via Van #02",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "orderCode" to "ORD-2039",
          "shopId" to shopIds[1],
          "shopCode" to "SHP-102",
          "shopName" to "Kaveri Super Mart",
          "routeId" to RoutesCatalog.R01_KANNUR_BANGALORE,
          "status" to OrderStatus.PENDING,
          "totalAmount" to 4250.0,
          "itemCount" to 50,
          "itemsSummary" to "BC200 x50",
          "orderDate" to today,
          "note" to "Awaiting packing",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "orderCode" to "ORD-2035",
          "shopId" to shopIds[3],
          "shopCode" to "SHP-104",
          "shopName" to "Deccan Fresh Corner",
          "routeId" to RoutesCatalog.R03_BANGALORE_PUNE,
          "status" to OrderStatus.DELIVERED,
          "totalAmount" to 12400.0,
          "itemCount" to 110,
          "itemsSummary" to "BC500 x30, JB200 x50, TP200 x30",
          "orderDate" to yesterday,
          "note" to "Yesterday route",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "orderCode" to "ORD-2032",
          "shopId" to shopIds[4],
          "shopCode" to "SHP-105",
          "shopName" to "Thalassery Chips Centre",
          "routeId" to RoutesCatalog.R02_KANNUR_KOZHIKODE,
          "status" to OrderStatus.REJECTED,
          "totalAmount" to 1980.0,
          "itemCount" to 22,
          "itemsSummary" to "MR200 x22",
          "orderDate" to threeDaysAgo,
          "note" to "Shop closed during delivery window",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      starterOrders.forEachIndexed { idx, payload ->
        val doc = userRef.collection("orders").document("seed_order_${idx + 1}")
        batch.set(doc, payload)
      }

      val starterStocks = listOf(
        mapOf(
          "userId" to uid,
          "productCode" to "BC100",
          "productName" to "Banana Chips",
          "packSize" to "100g",
          "category" to "Banana Chips",
          "quantity" to 24,
          "minThreshold" to 50,
          "unitPrice" to 45.0,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "productCode" to "JB200",
          "productName" to "Jaggery Banana",
          "packSize" to "200g",
          "category" to "Jaggery & Sweet",
          "quantity" to 18,
          "minThreshold" to 40,
          "unitPrice" to 96.0,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "productCode" to "MX500",
          "productName" to "Kerala Spicy Mixture",
          "packSize" to "500g",
          "category" to "Mixtures & Savories",
          "quantity" to 14,
          "minThreshold" to 25,
          "unitPrice" to 170.0,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
        mapOf(
          "userId" to uid,
          "productCode" to "TP200",
          "productName" to "Tapioca Chips",
          "packSize" to "200g",
          "category" to "Tapioca & Crackers",
          "quantity" to 85,
          "minThreshold" to 30,
          "unitPrice" to 65.0,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      starterStocks.forEachIndexed { idx, payload ->
        val doc = userRef.collection("stocks").document("seed_stock_${idx + 1}")
        batch.set(doc, payload)
      }

      ProductCatalogHelper.starterProducts(uid, today, ::daysAgoString).forEach { prod ->
        val doc = userRef.collection("products").document(prod.id)
        batch.set(doc, productPayloadMap(uid, prod))
      }

      PaymentLedgerHelper.starterSupplierBills(uid, today, ::daysOffsetString).forEach { bill ->
        val doc = userRef.collection("supplierBills").document(bill.id)
        batch.set(doc, supplierBillPayloadMap(uid, bill, today))
      }

      PaymentLedgerHelper.starterPayments(uid, today, yesterday).forEach { pay ->
        val mapped = when (pay.partyCode) {
          "SHP-101" -> pay.copy(partyId = shopIds.getOrNull(0) ?: pay.partyId)
          "SHP-104" -> pay.copy(partyId = shopIds.getOrNull(3) ?: pay.partyId)
          else -> pay
        }
        val doc = userRef.collection("payments").document(mapped.id)
        batch.set(doc, paymentPayloadMap(uid, mapped, today))
      }

      SupplierCatalogHelper.starterSuppliers(uid, today, ::daysAgoString).forEach { sup ->
        val doc = userRef.collection("suppliers").document(sup.id)
        batch.set(doc, supplierPayloadMap(uid, sup, today))
      }

      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, metaDocRef.path)
      Result.failure(e)
    }
  }

  private fun todayString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }

  private fun daysAgoString(days: Int): String {
    return daysOffsetString(-days)
  }

  private fun daysOffsetString(offsetDays: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, offsetDays)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
  }

  companion object {
    private const val OFFLINE_WRITE_GRACE_MS = 1400L
    private const val SERVER_ACK_TIMEOUT_MS = 15000L
  }
}
