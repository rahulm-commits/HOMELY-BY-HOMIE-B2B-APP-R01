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

const val DELIVERY_ARRIVAL_RADIUS_METERS = 150.0

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
  val hasArrivedByGps: Boolean = false,
  val isManualArrivalConfirmed: Boolean = false,
) {
  val formattedStopNum: String
    get() = String.format(Locale.US, "%02d", stopNumber)

  val isReadyToCompleteDelivery: Boolean
    get() = isCompleted || hasArrivedByGps || isManualArrivalConfirmed

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
  val isShortestPathMode: Boolean = true,
)

data class VehicleSummary(
  val modelName: String,
  val plateNumber: String,
  val vehicleTypeTag: String,
  val extraDetails: String = "",
)

private val platePatternRegex =
  Regex("^[A-Za-z]{2}[- ]?\\d{1,2}[- ]?[A-Za-z0-9]{1,4}[- ]?\\d{1,4}$")
private val etaTimeFormatLocal = ThreadLocal.withInitial {
  SimpleDateFormat("hh:mm a", Locale.US)
}

fun formatVehicleSummary(
  modelName: String,
  plateNumber: String,
  extraDetails: String = "",
): String {
  val cleanModel = modelName
    .replace('(', ' ')
    .replace(')', ' ')
    .replace(Regex("\\s+"), " ")
    .trim()
  val cleanPlate = plateNumber
    .replace('(', ' ')
    .replace(')', ' ')
    .replace(Regex("\\s+"), " ")
    .trim()
    .uppercase(Locale.US)
  val cleanDetails = extraDetails
    .replace(Regex("\\s*•\\s*"), ", ")
    .replace(Regex("\\s+"), " ")
    .trim()

  val base = when {
    cleanModel.isNotBlank() && cleanPlate.isNotBlank() -> "$cleanModel ($cleanPlate)"
    cleanPlate.isNotBlank() -> "Delivery Vehicle ($cleanPlate)"
    cleanModel.isNotBlank() -> cleanModel
    else -> "Tata Ace (KL-07-CD-4190)"
  }

  return if (cleanDetails.isNotBlank()) {
    "$base • $cleanDetails"
  } else {
    base
  }
}

fun parseVehicleSummary(rawVehicle: String): VehicleSummary {
  val trimmed = rawVehicle.trim()
  if (trimmed.isBlank()) {
    return VehicleSummary(
      modelName = "Tata Ace",
      plateNumber = "KL-07-CD-4190",
      vehicleTypeTag = "Mini Truck",
      extraDetails = "",
    )
  }

  val parenStart = trimmed.indexOf('(')
  val parenEnd = trimmed.indexOf(')')
  val model: String
  val plate: String
  val details: String

  if (parenStart >= 0 && parenEnd > parenStart) {
    model = trimmed.substring(0, parenStart).trim().ifBlank { "Delivery Vehicle" }
    plate = trimmed.substring(parenStart + 1, parenEnd).trim()
    details = trimmed.substring(parenEnd + 1)
      .trim()
      .removePrefix("•")
      .removePrefix("|")
      .removePrefix("-")
      .trim()
  } else {
    val sepIdx = listOf(trimmed.indexOf('•'), trimmed.indexOf('|'))
      .filter { it >= 0 }
      .minOrNull() ?: -1
    val head = if (sepIdx >= 0) trimmed.substring(0, sepIdx).trim() else trimmed
    details = if (sepIdx >= 0) trimmed.substring(sepIdx + 1).trim() else ""
    if (platePatternRegex.matches(head)) {
      model = "Delivery Vehicle"
      plate = head.uppercase(Locale.US)
    } else {
      model = head.ifBlank { "Delivery Vehicle" }
      plate = ""
    }
  }

  val combinedForTag = "$model $details".lowercase(Locale.US)
  val typeTag = when {
    combinedForTag.contains("reefer") || combinedForTag.contains("cold") || combinedForTag.contains("chiller") -> "Reefer Van"
    combinedForTag.contains("bolero") || combinedForTag.contains("pickup") -> "Pickup Van"
    combinedForTag.contains("dost") || combinedForTag.contains("eicher") || combinedForTag.contains("maxi") || combinedForTag.contains("canter") -> "Maxi Truck"
    combinedForTag.contains("ape") || combinedForTag.contains("auto") || combinedForTag.contains("3-wheeler") -> "3-Wheeler"
    combinedForTag.contains("van") || combinedForTag.contains("eecco") || combinedForTag.contains("omni") -> "Cargo Van"
    else -> "Mini Truck"
  }
  return VehicleSummary(
    modelName = model,
    plateNumber = plate,
    vehicleTypeTag = typeTag,
    extraDetails = details,
  )
}

private const val DEFAULT_HUB_LAT = 9.9312
private const val DEFAULT_HUB_LNG = 76.2673

fun orderShopsByShortestPath(
  routeShops: List<Shop>,
  userLatitude: Double? = null,
  userLongitude: Double? = null,
): List<Shop> {
  if (routeShops.size <= 1) return routeShops
  val withGps = routeShops.filter { it.latitude != 0.0 || it.longitude != 0.0 }
  val withoutGps = routeShops
    .filter { it.latitude == 0.0 && it.longitude == 0.0 }
    .sortedWith(compareBy<Shop> { it.shopCode }.thenBy { it.name }.thenBy { it.id })

  if (withGps.size <= 1) return withGps + withoutGps

  val anchorLat = if (userLatitude != null && userLongitude != null && (userLatitude != 0.0 || userLongitude != 0.0)) {
    userLatitude
  } else {
    DEFAULT_HUB_LAT
  }
  val anchorLng = if (userLatitude != null && userLongitude != null && (userLatitude != 0.0 || userLongitude != 0.0)) {
    userLongitude
  } else {
    DEFAULT_HUB_LNG
  }

  val remaining = withGps
    .sortedWith(compareBy<Shop> { it.shopCode }.thenBy { it.name }.thenBy { it.id })
    .toMutableList()
  val tour = mutableListOf<Shop>()
  var curLat = anchorLat
  var curLng = anchorLng

  while (remaining.isNotEmpty()) {
    var bestIdx = 0
    var bestDist = haversineDistanceMeters(curLat, curLng, remaining[0].latitude, remaining[0].longitude)
    for (idx in 1 until remaining.size) {
      val candidate = remaining[idx]
      val d = haversineDistanceMeters(curLat, curLng, candidate.latitude, candidate.longitude)
      if (d < bestDist - 1e-6) {
        bestDist = d
        bestIdx = idx
      }
    }
    val chosen = remaining.removeAt(bestIdx)
    tour.add(chosen)
    curLat = chosen.latitude
    curLng = chosen.longitude
  }

  // 2-opt open-path refinement anchored at (anchorLat, anchorLng)
  val n = tour.size
  if (n >= 3) {
    var improved = true
    var passes = 0
    while (improved && passes < 32) {
      improved = false
      passes++
      for (i in 0 until n - 1) {
        val prevLat = if (i == 0) anchorLat else tour[i - 1].latitude
        val prevLng = if (i == 0) anchorLng else tour[i - 1].longitude
        for (j in i + 1 until n) {
          val nextAfterJ = if (j + 1 < n) tour[j + 1] else null
          val before = haversineDistanceMeters(prevLat, prevLng, tour[i].latitude, tour[i].longitude) +
            if (nextAfterJ != null) {
              haversineDistanceMeters(tour[j].latitude, tour[j].longitude, nextAfterJ.latitude, nextAfterJ.longitude)
            } else {
              0.0
            }
          val after = haversineDistanceMeters(prevLat, prevLng, tour[j].latitude, tour[j].longitude) +
            if (nextAfterJ != null) {
              haversineDistanceMeters(tour[i].latitude, tour[i].longitude, nextAfterJ.latitude, nextAfterJ.longitude)
            } else {
              0.0
            }
          if (after + 1e-3 < before) {
            var left = i
            var right = j
            while (left < right) {
              val tmp = tour[left]
              tour[left] = tour[right]
              tour[right] = tmp
              left++
              right--
            }
            improved = true
          }
        }
      }
    }
  }

  return tour + withoutGps
}

fun buildTransportRoutePlanState(
  route: DeliveryRoute?,
  allShops: List<Shop>,
  allOrders: List<OrderRecord>,
  locallyCompletedShopIds: Set<String> = emptySet(),
  localPodNotesByShopId: Map<String, String> = emptyMap(),
  arrivedShopIds: Set<String> = emptySet(),
  userLatitude: Double? = null,
  userLongitude: Double? = null,
  useShortestPath: Boolean = true,
): TransportRoutePlanState {
  val routeShops = allShops.filter { shop ->
    shop.status == ShopStatus.ACTIVE && (route == null || shop.routeId == route.id)
  }
  val orderedShops = if (useShortestPath) {
    orderShopsByShortestPath(
      routeShops = routeShops,
      userLatitude = userLatitude,
      userLongitude = userLongitude,
    )
  } else {
    RoutesCatalog.sortShopsByRouteSequence(routeShops, route)
  }

  val hasValidUserGps = userLatitude != null &&
    userLongitude != null &&
    (userLatitude != 0.0 || userLongitude != 0.0)

  val uniqueOrders = allOrders.distinctBy { it.id.ifBlank { it.orderCode } }
  val preliminaryStops = orderedShops.mapIndexed { index, shop ->
    val shopOrders = uniqueOrders.filter { ord ->
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

    val arrivedByGps = distMeters != null && distMeters <= DELIVERY_ARRIVAL_RADIUS_METERS
    val arrivedManually = arrivedShopIds.contains(shop.id)

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
      hasArrivedByGps = arrivedByGps,
      isManualArrivalConfirmed = arrivedManually,
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

  val lastCompletedCoord = finalStops
    .takeWhile { it.isCompleted }
    .lastOrNull { it.shop.latitude != 0.0 || it.shop.longitude != 0.0 }
    ?.let { it.shop.latitude to it.shop.longitude }

  val waypoints = buildList {
    if (hasValidUserGps && pendingCoords.isNotEmpty()) {
      add(userLatitude!! to userLongitude!!)
    } else if (lastCompletedCoord != null && pendingCoords.isNotEmpty()) {
      add(lastCompletedCoord)
    }
    addAll(pendingCoords)
  }

  val estimatedSeconds = computeRoughRouteSeconds(waypoints)
  val estFinish = if (pendingCount == 0 && totalCount > 0) {
    "Completed"
  } else {
    val cal = Calendar.getInstance()
    cal.add(Calendar.SECOND, estimatedSeconds.toInt().coerceAtLeast(15 * 60))
    "Est: ${etaTimeFormatLocal.get()!!.format(cal.time)}"
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
    isShortestPathMode = useShortestPath,
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
  focusedStop: TransportStopItem? = null,
) {
  val validUpcoming = upcomingStops.filter { it.shop.latitude != 0.0 || it.shop.longitude != 0.0 }
  val preferredStop = focusedStop ?: activeStop
  val target = preferredStop?.takeIf { it.shop.latitude != 0.0 || it.shop.longitude != 0.0 }
    ?: activeStop?.takeIf { it.shop.latitude != 0.0 || it.shop.longitude != 0.0 }
    ?: validUpcoming.firstOrNull()

  if (target == null) {
    if (preferredStop != null && preferredStop.shop.address.isNotBlank()) {
      val q = Uri.encode("${preferredStop.shop.name}, ${preferredStop.shop.address}, ${preferredStop.shop.pinCode}")
      val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$q")
      runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure { Toast.makeText(context, "No maps app available", Toast.LENGTH_SHORT).show() }
    } else {
      Toast.makeText(context, "No stop GPS coordinates on this route yet", Toast.LENGTH_SHORT).show()
    }
    return
  }

  val destinationStr = "${target.shop.latitude},${target.shop.longitude}"
  val turnByTurnUri = Uri.parse("google.navigation:q=$destinationStr&mode=d")
  val turnByTurnIntent = Intent(Intent.ACTION_VIEW, turnByTurnUri).apply {
    setPackage("com.google.android.apps.maps")
  }
  if (runCatching { context.startActivity(turnByTurnIntent) }.isSuccess) {
    return
  }

  val uriBuilder = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
    .appendQueryParameter("api", "1")
    .appendQueryParameter("destination", destinationStr)
    .appendQueryParameter("travelmode", "driving")

  if (userLatitude != null && userLongitude != null && (userLatitude != 0.0 || userLongitude != 0.0)) {
    uriBuilder.appendQueryParameter("origin", "$userLatitude,$userLongitude")
  }

  val intent = Intent(Intent.ACTION_VIEW, uriBuilder.build())
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
    val geoFallback = Uri.parse("geo:$destinationStr?q=$destinationStr(${Uri.encode(target.shop.name)})")
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, geoFallback))
    } catch (_: Exception) {
      Toast.makeText(context, "No maps or browser app available", Toast.LENGTH_SHORT).show()
    }
  }
}
