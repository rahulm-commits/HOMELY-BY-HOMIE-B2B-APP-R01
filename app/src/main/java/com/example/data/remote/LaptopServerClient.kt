package com.example.data.remote

import com.example.data.local.ServerConfigStore
import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.Supplier
import com.example.data.model.SupplierBill
import com.google.firebase.Timestamp
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class LaptopHealthInfo(
  val reachable: Boolean,
  val serverName: String = "",
  val shopCount: Int = 0,
  val orderCount: Int = 0,
  val message: String = "",
)

class LaptopServerClient {

  private val client = OkHttpClient.Builder()
    .connectTimeout(3, TimeUnit.SECONDS)
    .readTimeout(6, TimeUnit.SECONDS)
    .writeTimeout(6, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  suspend fun checkHealth(baseUrl: String, token: String = ""): LaptopHealthInfo =
    withContext(Dispatchers.IO) {
      val normalized = ServerConfigStore.normalizeServerUrl(baseUrl)
      if (normalized.isBlank()) {
        return@withContext LaptopHealthInfo(false, message = "Enter Laptop Server URL first")
      }
      val requestBuilder = Request.Builder()
        .url("$normalized/api/health")
        .get()
      if (token.isNotBlank()) {
        requestBuilder.header("X-Homely-Token", token)
      }
      try {
        client.newCall(requestBuilder.build()).execute().use { response ->
          val bodyText = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            return@withContext LaptopHealthInfo(
              reachable = false,
              message = "HTTP ${response.code}: ${bodyText.ifBlank { response.message }}",
            )
          }
          val json = JSONObject(bodyText)
          LaptopHealthInfo(
            reachable = json.optBoolean("ok", true),
            serverName = json.optString("server", "Homely by Homie Laptop Server"),
            shopCount = json.optInt("shopCount", 0),
            orderCount = json.optInt("orderCount", 0),
            message = "Connected (${json.optInt("shopCount", 0)} shops, ${json.optInt("orderCount", 0)} orders)",
          )
        }
      } catch (e: IOException) {
        LaptopHealthInfo(
          reachable = false,
          message = formatReachabilityError(normalized, e),
        )
      } catch (e: Exception) {
        LaptopHealthInfo(
          reachable = false,
          message = e.localizedMessage ?: "Invalid response from laptop server",
        )
      }
    }

  suspend fun pushShop(baseUrl: String, token: String, shop: Shop): Result<Unit> =
    withContext(Dispatchers.IO) {
      val normalized = ServerConfigStore.normalizeServerUrl(baseUrl)
      if (normalized.isBlank()) {
        return@withContext Result.failure(IllegalStateException("Laptop server URL is empty"))
      }
      val payload = shopToJson(shop).toString()
      val requestBuilder = Request.Builder()
        .url("$normalized/api/shops")
        .post(payload.toRequestBody(jsonMediaType))
      if (token.isNotBlank()) {
        requestBuilder.header("X-Homely-Token", token)
      }
      try {
        client.newCall(requestBuilder.build()).execute().use { response ->
          if (response.isSuccessful) {
            Result.success(Unit)
          } else {
            val err = response.body?.string().orEmpty().ifBlank { response.message }
            Result.failure(IOException("Laptop HTTP ${response.code}: $err"))
          }
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun fetchShops(baseUrl: String, token: String): Result<List<Shop>> =
    withContext(Dispatchers.IO) {
      val normalized = ServerConfigStore.normalizeServerUrl(baseUrl)
      if (normalized.isBlank()) {
        return@withContext Result.failure(IllegalStateException("Laptop server URL is empty"))
      }
      val requestBuilder = Request.Builder()
        .url("$normalized/api/shops")
        .get()
      if (token.isNotBlank()) {
        requestBuilder.header("X-Homely-Token", token)
      }
      try {
        client.newCall(requestBuilder.build()).execute().use { response ->
          val body = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            return@withContext Result.failure(IOException("Laptop HTTP ${response.code}: $body"))
          }
          val root = body.trim()
          val array = if (root.startsWith("[")) {
            JSONArray(root)
          } else {
            JSONObject(root).optJSONArray("shops") ?: JSONArray()
          }
          val parsed = buildList {
            for (i in 0 until array.length()) {
              val obj = array.optJSONObject(i) ?: continue
              add(jsonToShop(obj))
            }
          }
          Result.success(parsed)
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun pushOrder(baseUrl: String, token: String, order: OrderRecord): Result<Unit> =
    withContext(Dispatchers.IO) {
      val normalized = ServerConfigStore.normalizeServerUrl(baseUrl)
      if (normalized.isBlank()) {
        return@withContext Result.failure(IllegalStateException("Laptop server URL is empty"))
      }
      val payload = orderToJson(order).toString()
      val requestBuilder = Request.Builder()
        .url("$normalized/api/orders")
        .post(payload.toRequestBody(jsonMediaType))
      if (token.isNotBlank()) {
        requestBuilder.header("X-Homely-Token", token)
      }
      try {
        client.newCall(requestBuilder.build()).execute().use { response ->
          if (response.isSuccessful) {
            Result.success(Unit)
          } else {
            val err = response.body?.string().orEmpty().ifBlank { response.message }
            Result.failure(IOException("Laptop HTTP ${response.code}: $err"))
          }
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun fetchOrders(baseUrl: String, token: String): Result<List<OrderRecord>> =
    withContext(Dispatchers.IO) {
      val normalized = ServerConfigStore.normalizeServerUrl(baseUrl)
      if (normalized.isBlank()) {
        return@withContext Result.failure(IllegalStateException("Laptop server URL is empty"))
      }
      val requestBuilder = Request.Builder()
        .url("$normalized/api/orders")
        .get()
      if (token.isNotBlank()) {
        requestBuilder.header("X-Homely-Token", token)
      }
      try {
        client.newCall(requestBuilder.build()).execute().use { response ->
          val body = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            return@withContext Result.failure(IOException("Laptop HTTP ${response.code}: $body"))
          }
          val root = body.trim()
          val array = if (root.startsWith("[")) {
            JSONArray(root)
          } else {
            JSONObject(root).optJSONArray("orders") ?: JSONArray()
          }
          val parsed = buildList {
            for (i in 0 until array.length()) {
              val obj = array.optJSONObject(i) ?: continue
              add(jsonToOrder(obj))
            }
          }
          Result.success(parsed)
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun deleteOrder(baseUrl: String, token: String, orderId: String): Result<Unit> =
    withContext(Dispatchers.IO) {
      val normalized = ServerConfigStore.normalizeServerUrl(baseUrl)
      if (normalized.isBlank()) {
        return@withContext Result.failure(IllegalStateException("Laptop server URL is empty"))
      }
      val requestBuilder = Request.Builder()
        .url("$normalized/api/orders/$orderId")
        .delete()
      if (token.isNotBlank()) {
        requestBuilder.header("X-Homely-Token", token)
      }
      try {
        client.newCall(requestBuilder.build()).execute().use { response ->
          if (response.isSuccessful || response.code == 404) {
            Result.success(Unit)
          } else {
            val err = response.body?.string().orEmpty().ifBlank { response.message }
            Result.failure(IOException("Laptop HTTP ${response.code}: $err"))
          }
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun pushRoute(baseUrl: String, token: String, route: DeliveryRoute): Result<Unit> =
    withContext(Dispatchers.IO) {
      val normalized = ServerConfigStore.normalizeServerUrl(baseUrl)
      if (normalized.isBlank()) {
        return@withContext Result.failure(IllegalStateException("Laptop server URL is empty"))
      }
      val payload = routeToJson(route).toString()
      val requestBuilder = Request.Builder()
        .url("$normalized/api/routes")
        .post(payload.toRequestBody(jsonMediaType))
      if (token.isNotBlank()) {
        requestBuilder.header("X-Homely-Token", token)
      }
      try {
        client.newCall(requestBuilder.build()).execute().use { response ->
          when {
            response.isSuccessful -> Result.success(Unit)
            response.code == 404 -> Result.success(Unit) // Older server.py without /api/routes
            else -> {
              val err = response.body?.string().orEmpty().ifBlank { response.message }
              Result.failure(IOException("Laptop HTTP ${response.code}: $err"))
            }
          }
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun fetchRoutes(baseUrl: String, token: String): Result<List<DeliveryRoute>> =
    withContext(Dispatchers.IO) {
      val normalized = ServerConfigStore.normalizeServerUrl(baseUrl)
      if (normalized.isBlank()) {
        return@withContext Result.failure(IllegalStateException("Laptop server URL is empty"))
      }
      val requestBuilder = Request.Builder()
        .url("$normalized/api/routes")
        .get()
      if (token.isNotBlank()) {
        requestBuilder.header("X-Homely-Token", token)
      }
      try {
        client.newCall(requestBuilder.build()).execute().use { response ->
          if (response.code == 404) {
            return@withContext Result.success(emptyList())
          }
          val body = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            return@withContext Result.failure(IOException("Laptop HTTP ${response.code}: $body"))
          }
          val root = body.trim()
          val array = if (root.startsWith("[")) {
            JSONArray(root)
          } else {
            JSONObject(root).optJSONArray("routes") ?: JSONArray()
          }
          val parsed = buildList {
            for (i in 0 until array.length()) {
              val obj = array.optJSONObject(i) ?: continue
              add(jsonToRoute(obj))
            }
          }
          Result.success(parsed)
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun formatReachabilityError(normalizedUrl: String, e: IOException): String {
    return when (e) {
      is ConnectException, is SocketTimeoutException ->
        "Cannot reach $normalizedUrl — start server.py on laptop & connect to same Wi-Fi"
      else -> e.localizedMessage ?: "Cannot reach $normalizedUrl"
    }
  }

  private fun shopToJson(shop: Shop): JSONObject = JSONObject().apply {
    put("id", shop.id.ifBlank { shop.shopCode })
    put("userId", shop.userId)
    put("shopCode", shop.shopCode)
    put("name", shop.name)
    put("ownerName", shop.ownerName)
    put("phone", shop.phone)
    put("secondaryPhone", shop.secondaryPhone)
    put("address", shop.address)
    put("pinCode", shop.pinCode)
    put("routeId", shop.routeId)
    put("photoUri", shop.photoUri)
    put("latitude", shop.latitude)
    put("longitude", shop.longitude)
    put("note", shop.note)
    put("status", shop.status)
    put("inquiryStatus", shop.inquiryStatus)
    put("enquiryDate", shop.enquiryDate)
    put("callDate", shop.callDate)
    put("lastOrderDate", shop.lastOrderDate)
    put("lastCallDate", shop.lastCallDate)
    put("confirmedPriceNotes", shop.confirmedPriceNotes)
    put("mapsLink", shop.mapsLink)
    put("addedBy", shop.addedBy)
    put("updatedAtMillis", shop.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
  }

  private fun jsonToShop(obj: JSONObject): Shop {
    val updatedMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
    val ts = Timestamp(Date(updatedMillis))
    return Shop(
      id = obj.optString("id", obj.optString("shopCode")),
      userId = obj.optString("userId", ""),
      shopCode = obj.optString("shopCode", ""),
      name = obj.optString("name", ""),
      ownerName = obj.optString("ownerName", ""),
      phone = obj.optString("phone", ""),
      secondaryPhone = obj.optString("secondaryPhone", ""),
      address = obj.optString("address", ""),
      pinCode = obj.optString("pinCode", ""),
      routeId = obj.optString("routeId", ""),
      photoUri = obj.optString("photoUri", ""),
      latitude = obj.optDouble("latitude", 0.0),
      longitude = obj.optDouble("longitude", 0.0),
      note = obj.optString("note", ""),
      status = obj.optString("status", "active"),
      inquiryStatus = obj.optString("inquiryStatus", "pending_inquiry"),
      enquiryDate = obj.optString("enquiryDate", ""),
      callDate = obj.optString("callDate", ""),
      lastOrderDate = obj.optString("lastOrderDate", ""),
      lastCallDate = obj.optString("lastCallDate", ""),
      confirmedPriceNotes = obj.optString("confirmedPriceNotes", ""),
      mapsLink = obj.optString("mapsLink", ""),
      addedBy = obj.optString("addedBy", ""),
      createdAt = ts,
      updatedAt = ts,
      isPendingSync = false,
    )
  }

  private fun orderToJson(order: OrderRecord): JSONObject = JSONObject().apply {
    put("id", order.id.ifBlank { order.orderCode })
    put("userId", order.userId)
    put("orderCode", order.orderCode)
    put("shopId", order.shopId)
    put("shopCode", order.shopCode)
    put("shopName", order.shopName)
    put("routeId", order.routeId)
    put("status", order.status)
    put("totalAmount", order.totalAmount)
    put("itemCount", order.itemCount)
    put("itemsSummary", order.itemsSummary)
    put("orderDate", order.orderDate)
    put("note", order.note)
    put("updatedAtMillis", order.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
  }

  private fun jsonToOrder(obj: JSONObject): OrderRecord {
    val updatedMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
    val ts = Timestamp(Date(updatedMillis))
    return OrderRecord(
      id = obj.optString("id", obj.optString("orderCode")),
      userId = obj.optString("userId", ""),
      orderCode = obj.optString("orderCode", ""),
      shopId = obj.optString("shopId", ""),
      shopCode = obj.optString("shopCode", ""),
      shopName = obj.optString("shopName", ""),
      routeId = obj.optString("routeId", ""),
      status = obj.optString("status", "confirmed"),
      totalAmount = obj.optDouble("totalAmount", 0.0),
      itemCount = obj.optInt("itemCount", 0),
      itemsSummary = obj.optString("itemsSummary", ""),
      orderDate = obj.optString("orderDate", ""),
      note = obj.optString("note", ""),
      createdAt = ts,
      updatedAt = ts,
      isPendingSync = false,
    )
  }

  private fun routeToJson(route: DeliveryRoute): JSONObject = JSONObject().apply {
    put("id", route.id)
    put("name", route.name)
    put("vehicle", route.vehicle)
    put("maxCrates", route.maxCrates)
    put("driverName", route.driverName)
    put("driverPhone", route.driverPhone)
    put("dispatchWindow", route.dispatchWindow)
    put("stagingBay", route.stagingBay)
    put("colorIndex", route.colorIndex)
    put("isActive", route.isActive)
    put("stopOrderIds", JSONArray(route.stopOrderIds))
    put("updatedAtMillis", System.currentTimeMillis())
  }

  private fun jsonToRoute(obj: JSONObject): DeliveryRoute {
    val stopsArr = obj.optJSONArray("stopOrderIds")
    val stopIds = if (stopsArr != null) {
      buildList {
        for (i in 0 until stopsArr.length()) {
          val id = stopsArr.optString(i, "").trim()
          if (id.isNotEmpty()) add(id)
        }
      }
    } else {
      emptyList()
    }
    return DeliveryRoute(
      id = obj.optString("id", ""),
      name = obj.optString("name", obj.optString("id", "")),
      vehicle = obj.optString("vehicle", "Tata Ace"),
      maxCrates = obj.optInt("maxCrates", 35),
      driverName = obj.optString("driverName", "Field Driver"),
      driverPhone = obj.optString("driverPhone", ""),
      dispatchWindow = obj.optString("dispatchWindow", "06:30 AM – 10:30 AM"),
      stagingBay = obj.optString("stagingBay", "Bay #03 - Central Dock"),
      colorIndex = obj.optInt("colorIndex", 0),
      isActive = obj.optBoolean("isActive", true),
      stopOrderIds = stopIds,
    )
  }

  suspend fun pushProduct(baseUrl: String, token: String, product: Product): Result<Unit> =
    postEntityAllowing404(baseUrl, token, "/api/products", productToJson(product))

  suspend fun fetchProducts(baseUrl: String, token: String): Result<List<Product>> =
    fetchListAllowing404(baseUrl, token, "/api/products", "products", ::jsonToProduct)

  suspend fun pushPayment(baseUrl: String, token: String, payment: PaymentTransaction): Result<Unit> =
    postEntityAllowing404(baseUrl, token, "/api/payments", paymentToJson(payment))

  suspend fun fetchPayments(baseUrl: String, token: String): Result<List<PaymentTransaction>> =
    fetchListAllowing404(baseUrl, token, "/api/payments", "payments", ::jsonToPayment)

  suspend fun pushSupplierBill(baseUrl: String, token: String, bill: SupplierBill): Result<Unit> =
    postEntityAllowing404(baseUrl, token, "/api/supplier_bills", supplierBillToJson(bill))

  suspend fun fetchSupplierBills(baseUrl: String, token: String): Result<List<SupplierBill>> =
    fetchListAllowing404(baseUrl, token, "/api/supplier_bills", "supplierBills", ::jsonToSupplierBill)

  suspend fun pushSupplier(baseUrl: String, token: String, supplier: Supplier): Result<Unit> =
    postEntityAllowing404(baseUrl, token, "/api/suppliers", supplierToJson(supplier))

  suspend fun fetchSuppliers(baseUrl: String, token: String): Result<List<Supplier>> =
    fetchListAllowing404(baseUrl, token, "/api/suppliers", "suppliers", ::jsonToSupplier)

  private suspend fun postEntityAllowing404(
    baseUrl: String,
    token: String,
    path: String,
    payloadJson: JSONObject,
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val normalized = ServerConfigStore.normalizeServerUrl(baseUrl)
    if (normalized.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Laptop server URL is empty"))
    }
    val requestBuilder = Request.Builder()
      .url("$normalized$path")
      .post(payloadJson.toString().toRequestBody(jsonMediaType))
    if (token.isNotBlank()) {
      requestBuilder.header("X-Homely-Token", token)
    }
    try {
      client.newCall(requestBuilder.build()).execute().use { response ->
        when {
          response.isSuccessful -> Result.success(Unit)
          response.code == 404 -> Result.success(Unit) // Older server.py compatibility
          else -> {
            val err = response.body?.string().orEmpty().ifBlank { response.message }
            Result.failure(IOException("Laptop HTTP ${response.code}: $err"))
          }
        }
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private suspend fun <T> fetchListAllowing404(
    baseUrl: String,
    token: String,
    path: String,
    arrayKey: String,
    mapper: (JSONObject) -> T,
  ): Result<List<T>> = withContext(Dispatchers.IO) {
    val normalized = ServerConfigStore.normalizeServerUrl(baseUrl)
    if (normalized.isBlank()) {
      return@withContext Result.failure(IllegalStateException("Laptop server URL is empty"))
    }
    val requestBuilder = Request.Builder()
      .url("$normalized$path")
      .get()
    if (token.isNotBlank()) {
      requestBuilder.header("X-Homely-Token", token)
    }
    try {
      client.newCall(requestBuilder.build()).execute().use { response ->
        if (response.code == 404) {
          return@withContext Result.success(emptyList())
        }
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          return@withContext Result.failure(IOException("Laptop HTTP ${response.code}: $body"))
        }
        val root = body.trim()
        val array = if (root.startsWith("[")) {
          JSONArray(root)
        } else {
          JSONObject(root).optJSONArray(arrayKey) ?: JSONArray()
        }
        val parsed = buildList {
          for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            add(mapper(obj))
          }
        }
        Result.success(parsed)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun productToJson(p: Product): JSONObject = JSONObject().apply {
    put("id", p.id.ifBlank { p.productCode })
    put("userId", p.userId)
    put("productCode", p.productCode)
    put("sku", p.sku)
    put("barcode", p.barcode)
    put("name", p.name)
    put("category", p.category)
    put("brand", p.brand)
    put("baseUnit", p.baseUnit)
    put("sellingUnit", p.sellingUnit)
    put("casePack", p.casePack)
    put("packSize", p.packSize)
    put("wholesalePrice", p.wholesalePrice)
    put("mrp", p.mrp)
    put("stockQuantity", p.stockQuantity)
    put("minThreshold", p.minThreshold)
    put("photoUri", p.photoUri)
    put("notes", p.notes)
    put("status", p.status)
    put("salesVelocity", p.salesVelocity)
    put("hasVariants", p.hasVariants)
    put("variantsJson", p.variantsJson)
    put("addedDate", p.addedDate)
    put("updatedAtMillis", p.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
  }

  private fun jsonToProduct(obj: JSONObject): Product {
    val updatedMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
    val ts = Timestamp(Date(updatedMillis))
    return Product(
      id = obj.optString("id", obj.optString("productCode")),
      userId = obj.optString("userId", ""),
      productCode = obj.optString("productCode", ""),
      sku = obj.optString("sku", ""),
      barcode = obj.optString("barcode", ""),
      name = obj.optString("name", ""),
      category = obj.optString("category", "Biscuits & Snacks"),
      brand = obj.optString("brand", ""),
      baseUnit = obj.optString("baseUnit", "Gram (g)"),
      sellingUnit = obj.optString("sellingUnit", "Pouch / Packet"),
      casePack = obj.optString("casePack", ""),
      packSize = obj.optString("packSize", ""),
      wholesalePrice = obj.optDouble("wholesalePrice", 0.0),
      mrp = obj.optDouble("mrp", 0.0),
      stockQuantity = obj.optInt("stockQuantity", 0),
      minThreshold = obj.optInt("minThreshold", 30),
      photoUri = obj.optString("photoUri", ""),
      notes = obj.optString("notes", ""),
      status = obj.optString("status", "active"),
      salesVelocity = obj.optInt("salesVelocity", 0),
      hasVariants = obj.optBoolean("hasVariants", false),
      variantsJson = obj.optString("variantsJson", ""),
      addedDate = obj.optString("addedDate", ""),
      createdAt = ts,
      updatedAt = ts,
      isPendingSync = false,
    )
  }

  private fun paymentToJson(p: PaymentTransaction): JSONObject = JSONObject().apply {
    put("id", p.id.ifBlank { p.receiptCode })
    put("userId", p.userId)
    put("receiptCode", p.receiptCode)
    put("direction", p.direction)
    put("partyId", p.partyId)
    put("partyCode", p.partyCode)
    put("partyName", p.partyName)
    put("invoiceRef", p.invoiceRef)
    put("amount", p.amount)
    put("paymentMethod", p.paymentMethod)
    put("paymentDate", p.paymentDate)
    put("referenceNumber", p.referenceNumber)
    put("note", p.note)
    put("updatedAtMillis", p.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
  }

  private fun jsonToPayment(obj: JSONObject): PaymentTransaction {
    val updatedMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
    val ts = Timestamp(Date(updatedMillis))
    return PaymentTransaction(
      id = obj.optString("id", obj.optString("receiptCode")),
      userId = obj.optString("userId", ""),
      receiptCode = obj.optString("receiptCode", ""),
      direction = obj.optString("direction", "collect"),
      partyId = obj.optString("partyId", ""),
      partyCode = obj.optString("partyCode", ""),
      partyName = obj.optString("partyName", ""),
      invoiceRef = obj.optString("invoiceRef", ""),
      amount = obj.optDouble("amount", 0.0),
      paymentMethod = obj.optString("paymentMethod", "UPI"),
      paymentDate = obj.optString("paymentDate", ""),
      referenceNumber = obj.optString("referenceNumber", ""),
      note = obj.optString("note", ""),
      createdAt = ts,
      updatedAt = ts,
      isPendingSync = false,
    )
  }

  private fun supplierBillToJson(b: SupplierBill): JSONObject = JSONObject().apply {
    put("id", b.id.ifBlank { b.billNumber })
    put("userId", b.userId)
    put("supplierCode", b.supplierCode)
    put("supplierName", b.supplierName)
    put("contactPerson", b.contactPerson)
    put("phone", b.phone)
    put("location", b.location)
    put("category", b.category)
    put("billNumber", b.billNumber)
    put("totalBilled", b.totalBilled)
    put("paidAmount", b.paidAmount)
    put("billDate", b.billDate)
    put("dueDate", b.dueDate)
    put("status", b.status)
    put("notes", b.notes)
    put("updatedAtMillis", b.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
  }

  private fun jsonToSupplierBill(obj: JSONObject): SupplierBill {
    val updatedMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
    val ts = Timestamp(Date(updatedMillis))
    return SupplierBill(
      id = obj.optString("id", obj.optString("billNumber")),
      userId = obj.optString("userId", ""),
      supplierCode = obj.optString("supplierCode", ""),
      supplierName = obj.optString("supplierName", ""),
      contactPerson = obj.optString("contactPerson", ""),
      phone = obj.optString("phone", ""),
      location = obj.optString("location", ""),
      category = obj.optString("category", ""),
      billNumber = obj.optString("billNumber", ""),
      totalBilled = obj.optDouble("totalBilled", 0.0),
      paidAmount = obj.optDouble("paidAmount", 0.0),
      billDate = obj.optString("billDate", ""),
      dueDate = obj.optString("dueDate", ""),
      status = obj.optString("status", "pending"),
      notes = obj.optString("notes", ""),
      createdAt = ts,
      updatedAt = ts,
      isPendingSync = false,
    )
  }

  private fun supplierToJson(s: Supplier): JSONObject = JSONObject().apply {
    put("id", s.id.ifBlank { s.supplierCode })
    put("userId", s.userId)
    put("supplierCode", s.supplierCode)
    put("name", s.name)
    put("category", s.category)
    put("contactPerson", s.contactPerson)
    put("contactRole", s.contactRole)
    put("altContactPerson", s.altContactPerson)
    put("phone", s.phone)
    put("altPhone", s.altPhone)
    put("email", s.email)
    put("address", s.address)
    put("pinCode", s.pinCode)
    put("mapsLink", s.mapsLink)
    put("photoUri", s.photoUri)
    put("gstin", s.gstin)
    put("creditTerms", s.creditTerms)
    put("confirmedPriceNotes", s.confirmedPriceNotes)
    put("notes", s.notes)
    put("status", s.status)
    put("addedDate", s.addedDate)
    put("addedBy", s.addedBy)
    put("updatedAtMillis", s.updatedAt?.toDate()?.time ?: System.currentTimeMillis())
  }

  private fun jsonToSupplier(obj: JSONObject): Supplier {
    val updatedMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
    val ts = Timestamp(Date(updatedMillis))
    return Supplier(
      id = obj.optString("id", obj.optString("supplierCode")),
      userId = obj.optString("userId", ""),
      supplierCode = obj.optString("supplierCode", ""),
      name = obj.optString("name", ""),
      category = obj.optString("category", ""),
      contactPerson = obj.optString("contactPerson", ""),
      contactRole = obj.optString("contactRole", ""),
      altContactPerson = obj.optString("altContactPerson", ""),
      phone = obj.optString("phone", ""),
      altPhone = obj.optString("altPhone", ""),
      email = obj.optString("email", ""),
      address = obj.optString("address", ""),
      pinCode = obj.optString("pinCode", ""),
      mapsLink = obj.optString("mapsLink", ""),
      photoUri = obj.optString("photoUri", ""),
      gstin = obj.optString("gstin", ""),
      creditTerms = obj.optString("creditTerms", ""),
      confirmedPriceNotes = obj.optString("confirmedPriceNotes", ""),
      notes = obj.optString("notes", ""),
      status = obj.optString("status", "active"),
      addedDate = obj.optString("addedDate", ""),
      addedBy = obj.optString("addedBy", ""),
      createdAt = ts,
      updatedAt = ts,
      isPendingSync = false,
    )
  }
}
