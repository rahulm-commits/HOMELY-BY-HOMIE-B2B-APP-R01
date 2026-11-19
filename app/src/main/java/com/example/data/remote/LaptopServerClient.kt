package com.example.data.remote

import com.example.data.local.ServerConfigStore
import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.Shop
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
        requestBuilder.header("X-SnackRoute-Token", token)
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
            serverName = json.optString("server", "SnackRoute Laptop Server"),
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
        requestBuilder.header("X-SnackRoute-Token", token)
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
        requestBuilder.header("X-SnackRoute-Token", token)
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
        requestBuilder.header("X-SnackRoute-Token", token)
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
        requestBuilder.header("X-SnackRoute-Token", token)
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
        requestBuilder.header("X-SnackRoute-Token", token)
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
        requestBuilder.header("X-SnackRoute-Token", token)
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
        requestBuilder.header("X-SnackRoute-Token", token)
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
}
