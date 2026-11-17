package com.example.data.remote

import com.example.ui.components.bearingToCardinal
import com.example.ui.components.computeBearingDegrees
import com.example.ui.components.haversineDistanceMeters
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class TurnStep(
  val instruction: String,
  val streetName: String,
  val distanceMeters: Double,
  val durationSeconds: Double,
  val maneuverType: String,
  val maneuverModifier: String,
  val location: Pair<Double, Double>,
)

data class NavigationRouteResult(
  val activeLegGeometry: List<Pair<Double, Double>>,
  val upcomingGeometry: List<Pair<Double, Double>>,
  val fullGeometry: List<Pair<Double, Double>>,
  val activeLegDistanceMeters: Double,
  val activeLegDurationSeconds: Double,
  val totalDistanceMeters: Double,
  val totalDurationSeconds: Double,
  val activeLegSteps: List<TurnStep>,
  val isRoadNetworkRoute: Boolean,
)

object OsrmRoutingClient {

  private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(7, TimeUnit.SECONDS)
      .build()
  }

  private val routeCache = object : LinkedHashMap<String, NavigationRouteResult>(24, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, NavigationRouteResult>?): Boolean {
      return size > 24
    }
  }

  /**
   * Computes a sequential driving route through [waypoints].
   * When [hasUserOrigin] is true, waypoints[0] is the driver's live GPS location and waypoints[1]
   * is the immediate priority stop (#1 pending), so Leg 0 highlights the fastest route to the first
   * stop before continuing through subsequent stops 2, 3, 4...
   */
  suspend fun computeSequentialRoute(
    waypoints: List<Pair<Double, Double>>,
    hasUserOrigin: Boolean,
    primaryStopName: String = "Next Stop",
  ): NavigationRouteResult {
    val validWaypoints = waypoints.filter { (lat, lng) ->
      lat.isFinite() && lng.isFinite() && (lat != 0.0 || lng != 0.0)
    }
    if (validWaypoints.size < 2) {
      return buildFallbackRoute(validWaypoints, hasUserOrigin, primaryStopName)
    }

    // OSRM public demo supports ~20 waypoints comfortably per request.
    val osrmWaypoints = validWaypoints.take(20)
    val cacheKey = buildCacheKey(osrmWaypoints, hasUserOrigin)
    synchronized(routeCache) {
      routeCache[cacheKey]?.let { return it }
    }

    val coordString = osrmWaypoints.joinToString(";") { (lat, lng) ->
      String.format(Locale.US, "%.5f,%.5f", lng, lat)
    }
    val url = "https://router.project-osrm.org/route/v1/driving/$coordString" +
      "?overview=full&geometries=geojson&steps=true"

    val parsed = withContext(Dispatchers.IO) {
      runCatching {
        val request = Request.Builder()
          .url(url)
          .header("User-Agent", "SnackRouteFieldOps/1.0")
          .build()
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) return@use null
          val body = response.body?.string().orEmpty()
          if (body.isBlank()) return@use null
          parseOsrmRouteResponse(
            jsonText = body,
            fallbackWaypoints = validWaypoints,
            hasUserOrigin = hasUserOrigin,
            primaryStopName = primaryStopName,
          )
        }
      }.getOrNull()
    }

    val result = parsed ?: buildFallbackRoute(validWaypoints, hasUserOrigin, primaryStopName)
    if (result.isRoadNetworkRoute) {
      synchronized(routeCache) {
        routeCache[cacheKey] = result
      }
    }
    return result
  }

  internal fun parseOsrmRouteResponse(
    jsonText: String,
    fallbackWaypoints: List<Pair<Double, Double>>,
    hasUserOrigin: Boolean,
    primaryStopName: String = "Next Stop",
  ): NavigationRouteResult? {
    val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: return null
    if (root.optString("code") != "Ok") return null
    val routesArr = root.optJSONArray("routes") ?: return null
    if (routesArr.length() == 0) return null

    val routeObj = routesArr.optJSONObject(0) ?: return null
    val fullGeometry = parseGeoJsonCoordinates(routeObj.optJSONObject("geometry")?.optJSONArray("coordinates"))
    val legsArr = routeObj.optJSONArray("legs") ?: JSONArray()

    val activeLegGeo = mutableListOf<Pair<Double, Double>>()
    val upcomingGeo = mutableListOf<Pair<Double, Double>>()
    val activeSteps = mutableListOf<TurnStep>()

    var activeDist = 0.0
    var activeDur = 0.0

    for (legIdx in 0 until legsArr.length()) {
      val legObj = legsArr.optJSONObject(legIdx) ?: continue
      val legDist = legObj.optDouble("distance", 0.0)
      val legDur = legObj.optDouble("duration", 0.0)
      val stepsArr = legObj.optJSONArray("steps") ?: JSONArray()

      val targetGeo = if (legIdx == 0) activeLegGeo else upcomingGeo

      for (sIdx in 0 until stepsArr.length()) {
        val stepObj = stepsArr.optJSONObject(sIdx) ?: continue
        val stepGeo = parseGeoJsonCoordinates(stepObj.optJSONObject("geometry")?.optJSONArray("coordinates"))
        appendGeometryDedup(targetGeo, stepGeo)

        if (legIdx == 0) {
          val maneuver = stepObj.optJSONObject("maneuver")
          val mType = maneuver?.optString("type").orEmpty()
          val mModifier = maneuver?.optString("modifier").orEmpty()
          val mLocArr = maneuver?.optJSONArray("location")
          val locPair = if (mLocArr != null && mLocArr.length() >= 2) {
            mLocArr.optDouble(1) to mLocArr.optDouble(0)
          } else {
            stepGeo.firstOrNull() ?: (0.0 to 0.0)
          }
          val street = stepObj.optString("name").orEmpty()
          val sDist = stepObj.optDouble("distance", 0.0)
          val sDur = stepObj.optDouble("duration", 0.0)
          activeSteps += TurnStep(
            instruction = formatManeuverInstruction(mType, mModifier, street, primaryStopName),
            streetName = street,
            distanceMeters = sDist,
            durationSeconds = sDur,
            maneuverType = mType,
            maneuverModifier = mModifier,
            location = locPair,
          )
        }
      }

      if (legIdx == 0) {
        activeDist = legDist
        activeDur = legDur
      }
    }

    // Include remaining tail coordinates if route had > 20 waypoints
    if (fallbackWaypoints.size > 20) {
      val tail = fallbackWaypoints.drop(19)
      appendGeometryDedup(upcomingGeo, tail)
    }

    val combinedFull = buildList {
      appendGeometryDedup(this, activeLegGeo)
      appendGeometryDedup(this, upcomingGeo)
    }.ifEmpty { fullGeometry.ifEmpty { fallbackWaypoints } }

    return NavigationRouteResult(
      activeLegGeometry = activeLegGeo.ifEmpty {
        if (fallbackWaypoints.size >= 2) fallbackWaypoints.take(2) else fallbackWaypoints
      },
      upcomingGeometry = upcomingGeo.ifEmpty {
        if (fallbackWaypoints.size > 2) fallbackWaypoints.drop(1) else emptyList()
      },
      fullGeometry = combinedFull,
      activeLegDistanceMeters = activeDist,
      activeLegDurationSeconds = activeDur,
      totalDistanceMeters = routeObj.optDouble("distance", activeDist),
      totalDurationSeconds = routeObj.optDouble("duration", activeDur),
      activeLegSteps = activeSteps.ifEmpty {
        buildFallbackRoute(fallbackWaypoints, hasUserOrigin, primaryStopName).activeLegSteps
      },
      isRoadNetworkRoute = true,
    )
  }

  internal fun buildFallbackRoute(
    waypoints: List<Pair<Double, Double>>,
    hasUserOrigin: Boolean,
    primaryStopName: String = "Next Stop",
  ): NavigationRouteResult {
    if (waypoints.size < 2) {
      val onlyPt = waypoints.firstOrNull()
      return NavigationRouteResult(
        activeLegGeometry = waypoints,
        upcomingGeometry = emptyList(),
        fullGeometry = waypoints,
        activeLegDistanceMeters = 0.0,
        activeLegDurationSeconds = 0.0,
        totalDistanceMeters = 0.0,
        totalDurationSeconds = 0.0,
        activeLegSteps = listOf(
          TurnStep(
            instruction = "Proceed to $primaryStopName",
            streetName = primaryStopName,
            distanceMeters = 0.0,
            durationSeconds = 0.0,
            maneuverType = "depart",
            maneuverModifier = "straight",
            location = onlyPt ?: (0.0 to 0.0),
          )
        ),
        isRoadNetworkRoute = false,
      )
    }

    // Approximate urban/highway average speed: ~34 km/h (9.44 m/s) with 1.25x road winding factor
    val windingFactor = 1.22
    val speedMetersPerSec = 9.44

    var totalDist = 0.0
    var activeDist = 0.0
    val steps = mutableListOf<TurnStep>()

    for (i in 0 until waypoints.size - 1) {
      val (lat1, lng1) = waypoints[i]
      val (lat2, lng2) = waypoints[i + 1]
      val legMeters = haversineDistanceMeters(lat1, lng1, lat2, lng2) * windingFactor
      totalDist += legMeters
      if (i == 0) {
        activeDist = legMeters
        val bearing = computeBearingDegrees(lat1, lng1, lat2, lng2)
        val cardinal = bearingToCardinal(bearing)
        val firstAction = if (hasUserOrigin) {
          "Head $cardinal toward $primaryStopName"
        } else {
          "Route leg 1: Head $cardinal"
        }
        steps += TurnStep(
          instruction = firstAction,
          streetName = primaryStopName,
          distanceMeters = legMeters,
          durationSeconds = legMeters / speedMetersPerSec,
          maneuverType = "depart",
          maneuverModifier = "straight",
          location = lat1 to lng1,
        )
        steps += TurnStep(
          instruction = "Arrive at $primaryStopName",
          streetName = primaryStopName,
          distanceMeters = 0.0,
          durationSeconds = 0.0,
          maneuverType = "arrive",
          maneuverModifier = "straight",
          location = lat2 to lng2,
        )
      }
    }

    return NavigationRouteResult(
      activeLegGeometry = waypoints.take(2),
      upcomingGeometry = if (waypoints.size > 2) waypoints.drop(1) else emptyList(),
      fullGeometry = waypoints,
      activeLegDistanceMeters = activeDist,
      activeLegDurationSeconds = activeDist / speedMetersPerSec,
      totalDistanceMeters = totalDist,
      totalDurationSeconds = totalDist / speedMetersPerSec,
      activeLegSteps = steps,
      isRoadNetworkRoute = false,
    )
  }

  private fun parseGeoJsonCoordinates(coordsArr: JSONArray?): List<Pair<Double, Double>> {
    if (coordsArr == null) return emptyList()
    return buildList(coordsArr.length()) {
      for (i in 0 until coordsArr.length()) {
        val pt = coordsArr.optJSONArray(i) ?: continue
        if (pt.length() >= 2) {
          val lng = pt.optDouble(0)
          val lat = pt.optDouble(1)
          if (lat.isFinite() && lng.isFinite()) {
            add(lat to lng)
          }
        }
      }
    }
  }

  private fun appendGeometryDedup(
    dest: MutableList<Pair<Double, Double>>,
    addition: List<Pair<Double, Double>>,
  ) {
    for (pt in addition) {
      if (dest.isEmpty() || dest.last() != pt) {
        dest.add(pt)
      }
    }
  }

  private fun formatManeuverInstruction(
    type: String,
    modifier: String,
    streetName: String,
    primaryStopName: String,
  ): String {
    val road = streetName.trim()
    val ontoRoad = if (road.isNotBlank()) " onto $road" else ""
    val cleanMod = modifier.replace('_', ' ').trim()
    return when (type.lowercase(Locale.US)) {
      "arrive" -> "Arrive at $primaryStopName"
      "depart" -> if (road.isNotBlank()) "Start on $road" else "Head toward $primaryStopName"
      "roundabout", "rotary" -> "Take roundabout exit$ontoRoad"
      "fork" -> when {
        cleanMod.contains("left") -> "Keep left at fork$ontoRoad"
        cleanMod.contains("right") -> "Keep right at fork$ontoRoad"
        else -> "Continue at fork$ontoRoad"
      }
      "merge" -> "Merge${if (cleanMod.isNotBlank()) " $cleanMod" else ""}$ontoRoad"
      "on ramp", "off ramp" -> "Take ramp${if (cleanMod.isNotBlank()) " ($cleanMod)" else ""}$ontoRoad"
      "turn", "end of road" -> when (cleanMod) {
        "left" -> "Turn left$ontoRoad"
        "right" -> "Turn right$ontoRoad"
        "slight left" -> "Turn slight left$ontoRoad"
        "slight right" -> "Turn slight right$ontoRoad"
        "sharp left" -> "Turn sharp left$ontoRoad"
        "sharp right" -> "Turn sharp right$ontoRoad"
        "uturn" -> "Make a U-turn$ontoRoad"
        else -> "Continue$ontoRoad"
      }
      "new name", "continue" -> if (road.isNotBlank()) "Continue onto $road" else "Continue straight"
      else -> when {
        cleanMod.contains("left") -> "Keep left$ontoRoad"
        cleanMod.contains("right") -> "Keep right$ontoRoad"
        road.isNotBlank() -> "Continue on $road"
        else -> "Continue toward $primaryStopName"
      }
    }
  }

  private fun buildCacheKey(waypoints: List<Pair<Double, Double>>, hasUserOrigin: Boolean): String {
    val ptsKey = waypoints.joinToString(";") { (lat, lng) ->
      String.format(Locale.US, "%.4f,%.4f", lat, lng)
    }
    return "$hasUserOrigin|$ptsKey"
  }
}
