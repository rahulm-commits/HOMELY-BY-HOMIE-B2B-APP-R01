package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class GeoPoint(
  val latitude: Double,
  val longitude: Double,
)

fun hasLocationPermission(context: Context): Boolean {
  val hasFine = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_FINE_LOCATION,
  ) == PackageManager.PERMISSION_GRANTED
  val hasCoarse = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_COARSE_LOCATION,
  ) == PackageManager.PERMISSION_GRANTED
  return hasFine || hasCoarse
}

fun formatCoordinatePair(latitude: Double, longitude: Double): String =
  String.format(Locale.US, "%.4f° N, %.4f° E", latitude, longitude)

fun haversineDistanceMeters(
  lat1: Double,
  lng1: Double,
  lat2: Double,
  lng2: Double,
): Double {
  val earthRadiusMeters = 6_371_000.0
  val dLat = Math.toRadians(lat2 - lat1)
  val dLng = Math.toRadians(lng2 - lng1)
  val rLat1 = Math.toRadians(lat1)
  val rLat2 = Math.toRadians(lat2)
  val a = sin(dLat / 2) * sin(dLat / 2) +
    cos(rLat1) * cos(rLat2) * sin(dLng / 2) * sin(dLng / 2)
  val c = 2 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
  return earthRadiusMeters * c
}

fun computeBearingDegrees(
  lat1: Double,
  lng1: Double,
  lat2: Double,
  lng2: Double,
): Double {
  val rLat1 = Math.toRadians(lat1)
  val rLat2 = Math.toRadians(lat2)
  val dLng = Math.toRadians(lng2 - lng1)
  val y = sin(dLng) * cos(rLat2)
  val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLng)
  val deg = Math.toDegrees(atan2(y, x))
  return (deg + 360.0) % 360.0
}

fun bearingToCardinal(bearingDeg: Double): String {
  val dirs = listOf("north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest")
  val index = ((bearingDeg + 22.5) / 45.0).toInt() % 8
  return dirs[index]
}

fun formatDistanceShort(meters: Double): String {
  if (!meters.isFinite() || meters <= 0.0) return "0 m"
  return if (meters < 950.0) {
    "${((meters / 10.0).roundToInt() * 10).coerceAtLeast(10)} m"
  } else {
    String.format(Locale.US, "%.1f km", meters / 1000.0)
  }
}

fun formatDurationEta(seconds: Double): String {
  if (!seconds.isFinite() || seconds <= 0.0) return "1 min"
  val totalMinutes = (seconds / 60.0).roundToInt().coerceAtLeast(1)
  val hours = totalMinutes / 60
  val mins = totalMinutes % 60
  return when {
    hours > 0 && mins > 0 -> "${hours}h ${mins}m"
    hours > 0 -> "${hours}h"
    else -> "$mins min"
  }
}

@SuppressLint("MissingPermission")
fun observeDeviceLocationFlow(
  context: Context,
  intervalMs: Long = 3500L,
): Flow<GeoPoint> = callbackFlow {
  if (!hasLocationPermission(context)) {
    close()
    return@callbackFlow
  }

  val fusedClient = LocationServices.getFusedLocationProviderClient(context)
  fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
    if (lastLoc != null && (lastLoc.latitude != 0.0 || lastLoc.longitude != 0.0)) {
      trySend(GeoPoint(lastLoc.latitude, lastLoc.longitude))
    }
  }

  val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
    .setMinUpdateIntervalMillis((intervalMs / 2L).coerceAtLeast(1500L))
    .setMinUpdateDistanceMeters(4f)
    .build()

  val callback = object : LocationCallback() {
    override fun onLocationResult(result: LocationResult) {
      val loc = result.lastLocation ?: return
      if (loc.latitude != 0.0 || loc.longitude != 0.0) {
        trySend(GeoPoint(loc.latitude, loc.longitude))
      }
    }
  }

  try {
    fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
  } catch (_: Exception) {
    // Fallback if Play Services location updates are unavailable on device
  }

  awaitClose {
    runCatching { fusedClient.removeLocationUpdates(callback) }
  }
}

@SuppressLint("MissingPermission")
fun fetchCurrentDeviceLocation(
  context: Context,
  highAccuracy: Boolean = true,
  onSuccess: (lat: Double, lng: Double, statusMsg: String) -> Unit,
  onError: (String) -> Unit,
) {
  if (!hasLocationPermission(context)) {
    onError("Location permission not granted")
    return
  }

  try {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    val tokenSource = CancellationTokenSource()
    val priority = if (highAccuracy) {
      Priority.PRIORITY_HIGH_ACCURACY
    } else {
      Priority.PRIORITY_BALANCED_POWER_ACCURACY
    }
    fusedClient.getCurrentLocation(priority, tokenSource.token)
      .addOnSuccessListener { loc ->
        if (loc != null) {
          onSuccess(
            loc.latitude,
            loc.longitude,
            "Location Locked: ${String.format(Locale.US, "%.4f, %.4f", loc.latitude, loc.longitude)}",
          )
        } else {
          fusedClient.lastLocation
            .addOnSuccessListener { lastLoc ->
              if (lastLoc != null) {
                onSuccess(
                  lastLoc.latitude,
                  lastLoc.longitude,
                  "Location Locked: ${String.format(Locale.US, "%.4f, %.4f", lastLoc.latitude, lastLoc.longitude)}",
                )
              } else {
                fallbackLocationManager(context, onSuccess, onError)
              }
            }
            .addOnFailureListener {
              fallbackLocationManager(context, onSuccess, onError)
            }
        }
      }
      .addOnFailureListener {
        fallbackLocationManager(context, onSuccess, onError)
      }
  } catch (e: Exception) {
    fallbackLocationManager(context, onSuccess, onError)
  }
}

@SuppressLint("MissingPermission")
private fun fallbackLocationManager(
  context: Context,
  onSuccess: (lat: Double, lng: Double, statusMsg: String) -> Unit,
  onError: (String) -> Unit,
) {
  try {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    val candidates: List<Location> = listOfNotNull(
      lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER),
      lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
      lm?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER),
    )
    val best = candidates.maxByOrNull { it.time }
    if (best != null) {
      onSuccess(
        best.latitude,
        best.longitude,
        "Location Locked: ${String.format(Locale.US, "%.4f, %.4f", best.latitude, best.longitude)}",
      )
    } else {
      onError("GPS signal unavailable right now. Turn on device Location and retry.")
    }
  } catch (e: Exception) {
    onError(e.localizedMessage ?: "Unable to read GPS coordinates")
  }
}
