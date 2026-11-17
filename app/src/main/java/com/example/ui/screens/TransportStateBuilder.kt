package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.example.ui.components.formatDurationEta
import com.example.ui.components.haversineDistanceMeters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class TransportStopFilter(val label: String) {
  ALL("All Stops"),
  PENDING("Pending"),
  COMPLETED("Completed"),
}

data class TransportStopItem(
  val stopNumber: Int,
  val shop: Shop,
  val activeOrders: List<OrderRecord>,
  val deliveredOrders: List<OrderRecord>,
  val isCompleted: Boolean,
  val isActiveTarget: Boolean,
  val totalAmountToCollect: Double,
  val deliveredAmount: Double,
  val totalPacks: Int,
  val distanceFromUserMeters: Double?,
  val primaryOrderCode: String?,
  val paymentLabel: String,
  val isCod: Boolean,
  val podNote: String,
) {
  val formattedStopNum: String
    get() = String.format(Locale.US, "%02d", stopNumber)

  val shortShopName: String
    get() = shop.name
      .substringBefore(" Bakery")
      .substringBefore(" Supermarket")
      .substringBefore(" Super Mart")
      .substringBefore(" Provision")
      .trim()
      .ifBlank { shop.name }
}

data class TransportRoutePlanState(
  val selectedRoute: DeliveryRoute?,
  val stops: List<TransportStopItem>,
  val activeTargetStop: TransportStopItem?,
  val upcomingStops: List<TransportStopItem>,
  val deliveredStops: List<TransportStopItem>,
  val totalStops: Int,
  val completedCount: Int,
  val pendingCount: Int,
  val completionPercent: Int,
  val cashToCollect: Double,
  val estimatedFinishText: String,
  val orderedWaypointsForMap: List<Pair<Double, Double>>,
  val hasUserOriginForRouting: Boolean,
)

data class VehicleSummary(
  val modelName: String,
  val plateNumber: String,
  val vehicleTypeTag: String,
)

fun parseVehicleSummary(rawVehicle: String): VehicleSummary {
  val trimmed = rawVehicle.trim()
  val parenStart = trimmed.indexOf('(')
  val parenEnd = trimmed.indexOf(')')
  val model = if (parenStart > 0) {
    trimmed.substring(0, parenStart).trim()
  } else {
    trimmed.ifBlank { "Tata Ace" }
  }
  val plate = if (parenStart >= 0 && parenEnd > parenStart + 1) {
    trimmed.substring(parenStart + 1, parenEnd).trim()
  } else {
    "KL-07-CD-4190"
  }
  val lower = model.lowercase(Locale.US)
  val typeTag = when {
    lower.contains("bolero") || lower.contains("pickup") -> "Pickup Van"
    lower.contains("dost") || lower.contains("eicher") || lower.contains("maxi") -> "Maxi Truck"
    lower.contains("ape") || lower.contains("auto") -> "3-Wheeler"
    else -> "Mini Truck"
  }
  return VehicleSummary(modelName = model, plateNumber = plate, vehicleTypeTag = typeTag)
}

fun buildTransportRoutePlanState(
  route: DeliveryRoute?,
  allShops: List<Shop>,
  allOrders: List<OrderRecord>,
  locallyCompletedShopIds: Set<String> = emptySet(),
  localPodNotesByShopId: Map<String, String> = emptyMap(),
  userLatitude: Double? = null,
  userLongitude: Double? = null,
): TransportRoutePlanState {
  val routeShops = allShops.filter { shop ->
    shop.status == ShopStatus.ACTIVE && (route == null || shop.routeId == route.id)
  }
  val orderedShops = RoutesCatalog.sortShopsByRouteSequence(routeShops, route)

  val hasValidUserGps = userLatitude != null &&
    userLongitude != null &&
    (userLatitude != 0.0 || userLongitude != 0.0)

  val preliminaryStops = orderedShops.mapIndexed { index, shop ->
    val shopOrders = allOrders.filter { ord ->
      ord.shopId == shop.id ||
        (ord.shopCode.isNotBlank() && ord.shopCode.equals(shop.shopCode, ignoreCase = true))
    }
    val activeOrders = shopOrders.filter {
      it.status == OrderStatus.PENDING || it.status == OrderStatus.CONFIRMED
    }
    val deliveredOrders = shopOrders.filter { it.status == OrderStatus.DELIVERED }

    val isDone = locallyCompletedShopIds.contains(shop.id) ||
      (deliveredOrders.isNotEmpty() && activeOrders.isEmpty())

    val collectAmt = activeOrders.sumOf { it.totalAmount }
    val doneAmt = deliveredOrders.sumOf { it.totalAmount }
    val packs = if (activeOrders.isNotEmpty()) {
      activeOrders.sumOf { it.itemCount }
    } else {
      deliveredOrders.sumOf { it.itemCount }
    }
    val primaryOrder = activeOrders.firstOrNull() ?: deliveredOrders.firstOrNull()

    val distMeters = if (
      hasValidUserGps &&
      (shop.latitude != 0.0 || shop.longitude != 0.0)
    ) {
      haversineDistanceMeters(userLatitude!!, userLongitude!!, shop.latitude, shop.longitude)
    } else {
      null
    }

    val pod = localPodNotesByShopId[shop.id]?.takeIf { it.isNotBlank() }
      ?: deliveredOrders.firstOrNull()?.note?.takeIf { it.isNotBlank() }
      ?: if (isDone) "POD Confirmed by ${shop.ownerName}" else ""

    val noteText = activeOrders.firstOrNull()?.note.orEmpty().lowercase(Locale.US)
    val isPrepaid = noteText.contains("prepaid") || noteText.contains("upi")
    val payLabel = when {
      collectAmt > 0.0 && isPrepaid -> "Prepaid (UPI Verified)"
      collectAmt > 0.0 -> "COD: ₹${collectAmt.toInt()}"
      doneAmt > 0.0 && isPrepaid -> "Prepaid"
      doneAmt > 0.0 -> "₹${doneAmt.toInt()} Collected"
      else -> "Scheduled Stop"
    }

    TransportStopItem(
      stopNumber = index + 1,
      shop = shop,
      activeOrders = activeOrders,
      deliveredOrders = deliveredOrders,
      isCompleted = isDone,
      isActiveTarget = false,
      totalAmountToCollect = collectAmt,
      deliveredAmount = doneAmt,
      totalPacks = packs,
      distanceFromUserMeters = distMeters,
      primaryOrderCode = primaryOrder?.orderCode,
      paymentLabel = payLabel,
      isCod = collectAmt > 0.0 && !isPrepaid,
      podNote = pod,
    )
  }

  val firstPendingIdx = preliminaryStops.indexOfFirst { !it.isCompleted }
  val finalStops = preliminaryStops.mapIndexed { idx, item ->
    item.copy(isActiveTarget = (idx == firstPendingIdx))
  }

  val activeStop = finalStops.firstOrNull { it.isActiveTarget }
  val upcoming = finalStops.filter { !it.isCompleted }
  val delivered = finalStops.filter { it.isCompleted }.reversed()

  val totalCount = finalStops.size
  val doneCount = delivered.size
  val pendingCount = upcoming.size
  val pct = if (totalCount > 0) ((doneCount * 100) / totalCount) else 0
  val cashToCollect = upcoming.sumOf { it.totalAmountToCollect }

  val pendingCoords = upcoming
    .map { it.shop.latitude to it.shop.longitude }
    .filter { (lat, lng) -> lat != 0.0 || lng != 0.0 }

  val waypoints = buildList {
    if (hasValidUserGps && pendingCoords.isNotEmpty()) {
      add(userLatitude!! to userLongitude!!)
    }
    addAll(pendingCoords)
  }

  val estimatedSeconds = computeRoughRouteSeconds(waypoints)
  val estFinish = if (pendingCount == 0 && totalCount > 0) {
    "Completed"
  } else {
    val cal = Calendar.getInstance()
    cal.add(Calendar.SECOND, estimatedSeconds.toInt().coerceAtLeast(15 * 60))
    "Est: ${SimpleDateFormat("hh:mm a", Locale.US).format(cal.time)}"
  }

  return TransportRoutePlanState(
    selectedRoute = route,
    stops = finalStops,
    activeTargetStop = activeStop,
    upcomingStops = upcoming,
    deliveredStops = delivered,
    totalStops = totalCount,
    completedCount = doneCount,
    pendingCount = pendingCount,
    completionPercent = pct,
    cashToCollect = cashToCollect,
    estimatedFinishText = estFinish,
    orderedWaypointsForMap = waypoints,
    hasUserOriginForRouting = hasValidUserGps && pendingCoords.isNotEmpty(),
  )
}

private fun computeRoughRouteSeconds(waypoints: List<Pair<Double, Double>>): Double {
  if (waypoints.size < 2) return 20 * 60.0
  var totalMeters = 0.0
  for (i in 0 until waypoints.size - 1) {
    totalMeters += haversineDistanceMeters(
      waypoints[i].first,
      waypoints[i].second,
      waypoints[i + 1].first,
      waypoints[i + 1].second,
    )
  }
  val drivingSeconds = (totalMeters * 1.2) / 9.44
  val stopDwellSeconds = (waypoints.size - 1) * 8 * 60.0
  return drivingSeconds + stopDwellSeconds
}

fun formatEtaSummary(distanceMeters: Double, durationSeconds: Double): String {
  if (distanceMeters <= 0.0) return "Ready"
  val kmStr = if (distanceMeters < 950) {
    "${distanceMeters.toInt()} m"
  } else {
    String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
  }
  return "$kmStr • ${formatDurationEta(durationSeconds)}"
}

fun launchExternalGoogleMapsNavigation(
  context: Context,
  activeStop: TransportStopItem?,
  upcomingStops: List<TransportStopItem>,
  userLatitude: Double?,
  userLongitude: Double?,
) {
  val validUpcoming = upcomingStops.filter { it.shop.latitude != 0.0 || it.shop.longitude != 0.0 }
  val target = activeStop?.takeIf { it.shop.latitude != 0.0 || it.shop.longitude != 0.0 }
    ?: validUpcoming.firstOrNull()

  if (target == null) {
    if (activeStop != null && activeStop.shop.address.isNotBlank()) {
      val q = Uri.encode("${activeStop.shop.name}, ${activeStop.shop.address}, ${activeStop.shop.pinCode}")
      val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$q")
      runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure { Toast.makeText(context, "No maps app available", Toast.LENGTH_SHORT).show() }
    } else {
      Toast.makeText(context, "No stop GPS coordinates on this route yet", Toast.LENGTH_SHORT).show()
    }
    return
  }

  val destinationStr = "${target.shop.latitude},${target.shop.longitude}"
  val extraWaypoints = validUpcoming
    .filter { it.shop.id != target.shop.id }
    .take(8)
    .joinToString("|") { "${it.shop.latitude},${it.shop.longitude}" }

  val uriBuilder = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
    .appendQueryParameter("api", "1")
    .appendQueryParameter("destination", destinationStr)
    .appendQueryParameter("travelmode", "driving")

  if (userLatitude != null && userLongitude != null && (userLatitude != 0.0 || userLongitude != 0.0)) {
    uriBuilder.appendQueryParameter("origin", "$userLatitude,$userLongitude")
  }
  if (extraWaypoints.isNotBlank()) {
    uriBuilder.appendQueryParameter("waypoints", extraWaypoints)
  }

  val intent = Intent(Intent.ACTION_VIEW, uriBuilder.build())
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
    val geoFallback = Uri.parse("geo:${target.shop.latitude},${target.shop.longitude}?q=$destinationStr(${Uri.encode(target.shop.name)})")
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, geoFallback))
    } catch (_: Exception) {
      Toast.makeText(context, "No maps or browser app available", Toast.LENGTH_SHORT).show()
    }
  }
}
