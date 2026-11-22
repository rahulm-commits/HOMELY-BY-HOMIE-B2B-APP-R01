package com.example.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.ui.theme.CobaltInfo
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.DeepSlateInk
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ForestSecondary
import com.example.ui.theme.MutedSlateInk
import com.example.ui.theme.RouteColorPalette
import com.example.ui.theme.RouteColorSpec
import com.example.ui.theme.RouteComboPattern
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.WarmIvorySurfaceVariant
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class MapPinDisplay(
  val shop: Shop,
  val stopNumber: Int?,
)

enum class StopPinState {
  COMPLETED,
  ACTIVE_TARGET,
  UPCOMING,
}

data class NavigationMapPin(
  val shop: Shop,
  val stopNumber: Int,
  val state: StopPinState,
  val distanceLabel: String = "",
)

internal data class MapCameraState(
  val centerLat: Double,
  val centerLng: Double,
  val zoom: Float,
)

private data class OsmTileKey(
  val z: Int,
  val x: Int,
  val y: Int,
)

private data class VisibleTilePlacement(
  val key: OsmTileKey,
  val dstOffset: IntOffset,
  val dstSize: IntSize,
)

private data class RenderedPinHitbox(
  val shopId: String,
  val center: Offset,
  val radiusPx: Float,
)

private data class PreparedPinRender(
  val shop: Shop,
  val shortName: String,
  val stopNumber: Int,
  val isUnallocated: Boolean,
  val colorSpec: RouteColorSpec?,
)

private data class PreparedRouteTrack(
  val route: DeliveryRoute,
  val spec: RouteColorSpec,
  val coordinates: List<Pair<Double, Double>>,
)

private class MeasuredCityMarker(
  val lat: Double,
  val lng: Double,
  val layout: TextLayoutResult,
)

private class MeasuredPinRender(
  val pin: PreparedPinRender,
  val glyphLayout: TextLayoutResult,
  val labelLayout: TextLayoutResult?,
)

private class MeasuredNavPinRender(
  val pin: NavigationMapPin,
  val glyphLayout: TextLayoutResult,
  val labelLayout: TextLayoutResult?,
)

private class MeasuredTrackRender(
  val track: PreparedRouteTrack,
  val labelLayout: TextLayoutResult?,
)

private class MapCanvasScratch(densityPx: Float) {
  val path: Path = Path()
  val clipCirclePath: Path = Path()
  val triPath: Path = Path()
  val screenPointsBuffer: MutableList<Offset> = ArrayList(32)

  val coastStroke = Stroke(width = 2f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val ghatsStroke = Stroke(width = 24f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val hwOuterStroke = Stroke(width = 4f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val hwInnerStroke = Stroke(width = 2.2f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val trackHaloStroke = Stroke(width = 7.5f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val trackPrimaryStroke = Stroke(width = 4.2f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val trackSplitStroke = Stroke(width = 3.2f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val trackRingOuterStroke = Stroke(width = 6.2f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val trackRingMidStroke = Stroke(width = 3.4f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val trackRingCoreStroke = Stroke(width = 2.2f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val trackBgStroke = Stroke(width = 4.8f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val trackDiagStripeStroke = Stroke(
    width = 3.4f * densityPx,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * densityPx, 12f * densityPx), 0f),
  )
  val trackDashStroke = Stroke(
    width = 2.8f * densityPx,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f * densityPx, 8f * densityPx), 0f),
  )
  val pillTrackStroke = Stroke(width = 1.6f * densityPx)
  val pillPinStroke = Stroke(width = 1f * densityPx)
  val pinSelectedStroke = Stroke(width = 3f * densityPx)
  val pinDefaultStroke = Stroke(width = 2f * densityPx)
  val navUpcomingHaloStroke = Stroke(width = 6f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val navUpcomingDashStroke = Stroke(
    width = 3.5f * densityPx,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round,
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * densityPx, 7f * densityPx), 0f),
  )
  val navActiveHaloStroke = Stroke(width = 8.5f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val navActiveStroke = Stroke(width = 5f * densityPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val navPinSelectedStroke = Stroke(width = 2.6f * densityPx)
  val navPinDefaultStroke = Stroke(width = 1.8f * densityPx)
}

private const val OSM_TILE_SIZE = 256.0
private const val MIN_ZOOM = 3.5f
private const val MAX_ZOOM = 18.0f
private const val DEFAULT_REGION_LAT = 12.85
private const val DEFAULT_REGION_LNG = 76.10
private const val DEFAULT_REGION_ZOOM = 6.2f

private val CartographicLand = Color(0xFFEFECE2)
private val CartographicPark = Color(0xFFDDECD7)
private val CartographicGrid = Color(0xFFD5CCB8)
private val CartographicHighwayOuter = Color(0xFFE0B467)
private val CartographicHighwayInner = Color(0xFFFFE59D)
private val CartographicOcean = Color(0xFFCDE2F6)
private val CartographicCoastStroke = Color(0xFF93B8DC)

private val regionHubCities = listOf(
  Triple("Pune", 18.5204, 73.8567),
  Triple("Kolhapur", 16.7050, 74.2433),
  Triple("Belagavi", 15.8497, 74.4977),
  Triple("Hubballi", 15.3647, 75.1240),
  Triple("Bengaluru", 12.9716, 77.5946),
  Triple("Mysuru", 12.2958, 76.6394),
  Triple("Mangaluru", 12.9141, 74.8560),
  Triple("Kannur", 11.8745, 75.3704),
  Triple("Thalassery", 11.7491, 75.4890),
  Triple("Kozhikode", 11.2588, 75.7804),
  Triple("Thrissur", 10.5276, 76.2144),
  Triple("Aluva", 10.1004, 76.3570),
  Triple("Kochi", 9.9312, 76.2673),
)

private val westOceanCoastCoords = arrayOf(
  20.5 to 66.0,
  20.5 to 72.7,
  19.07 to 72.82,
  17.00 to 73.28,
  15.49 to 73.80,
  12.91 to 74.82,
  11.87 to 75.35,
  11.25 to 75.77,
  9.93 to 76.24,
  8.52 to 76.92,
  7.80 to 77.55,
  7.80 to 66.0,
)

private val westernGhatsCoords = arrayOf(
  14.5 to 74.6,
  12.1 to 75.7,
  11.4 to 76.5,
  10.1 to 77.1,
)

private val highwayNh66 = listOf(
  19.0760 to 72.8777,
  18.5204 to 73.8567,
  15.4909 to 73.8278,
  14.8185 to 74.1416,
  12.9141 to 74.8560,
  11.8745 to 75.3704,
  11.7491 to 75.4890,
  11.2588 to 75.7804,
  10.5276 to 76.2144,
  9.9312 to 76.2673,
  8.5241 to 76.9366,
)

private val highwayNh48 = listOf(
  18.5204 to 73.8567,
  16.7050 to 74.2433,
  15.8497 to 74.4977,
  15.3647 to 75.1240,
  14.4644 to 75.9218,
  13.3379 to 77.1173,
  12.9716 to 77.5946,
)

private val highwayNh275And544 = listOf(
  11.8745 to 75.3704,
  12.4244 to 75.7382,
  12.2958 to 76.6394,
  12.9716 to 77.5946,
)

private val highwayKochiBlr = listOf(
  9.9312 to 76.2673,
  10.5276 to 76.2144,
  11.0168 to 76.9558,
  11.6643 to 78.1460,
  12.9716 to 77.5946,
)

private val allHighways = arrayOf(highwayNh66, highwayNh48, highwayNh275And544, highwayKochiBlr)

internal fun latToWorldY(lat: Double, zoom: Float): Double {
  val clamped = lat.coerceIn(-85.0511, 85.0511)
  val rad = clamped * PI / 180.0
  val s = sin(rad).coerceIn(-0.999999, 0.999999)
  return (0.5 - ln((1.0 + s) / (1.0 - s)) / (4.0 * PI)) * OSM_TILE_SIZE * 2.0.pow(zoom.toDouble())
}

internal fun lngToWorldX(lng: Double, zoom: Float): Double {
  return ((lng + 180.0) / 360.0) * OSM_TILE_SIZE * 2.0.pow(zoom.toDouble())
}

internal fun worldXToLng(x: Double, zoom: Float): Double {
  val scale = OSM_TILE_SIZE * 2.0.pow(zoom.toDouble())
  return ((x / scale) * 360.0 - 180.0).coerceIn(-179.9, 179.9)
}

internal fun worldYToLat(y: Double, zoom: Float): Double {
  val scale = OSM_TILE_SIZE * 2.0.pow(zoom.toDouble())
  val n = PI - 2.0 * PI * y / scale
  return ((180.0 / PI) * atan(sinh(n))).coerceIn(-85.0, 85.0)
}

internal fun projectLatLngToScreen(
  lat: Double,
  lng: Double,
  camera: MapCameraState,
  widthPx: Float,
  heightPx: Float,
  worldScale: Float = 1f,
): Offset {
  val cx = lngToWorldX(camera.centerLng, camera.zoom)
  val cy = latToWorldY(camera.centerLat, camera.zoom)
  val px = lngToWorldX(lng, camera.zoom)
  val py = latToWorldY(lat, camera.zoom)
  return Offset(
    x = (widthPx / 2f + (px - cx) * worldScale).toFloat(),
    y = (heightPx / 2f + (py - cy) * worldScale).toFloat(),
  )
}

internal fun computeFitBoundsCamera(
  points: List<Pair<Double, Double>>,
  widthPx: Float,
  heightPx: Float,
  singlePointZoom: Float = 13.5f,
  applyOverlayVerticalShift: Boolean = true,
  worldScale: Float = 1f,
): MapCameraState {
  val validPoints = points.filter { (lat, lng) ->
    lat.isFinite() && lng.isFinite() && (lat != 0.0 || lng != 0.0)
  }
  if (validPoints.isEmpty()) {
    return MapCameraState(DEFAULT_REGION_LAT, DEFAULT_REGION_LNG, DEFAULT_REGION_ZOOM)
  }
  if (validPoints.size == 1) {
    val (lat, lng) = validPoints.first()
    return MapCameraState(lat, lng, singlePointZoom)
  }

  var minLat = 90.0
  var maxLat = -90.0
  var minLng = 180.0
  var maxLng = -180.0
  for ((lat, lng) in validPoints) {
    minLat = min(minLat, lat)
    maxLat = max(maxLat, lat)
    minLng = min(minLng, lng)
    maxLng = max(maxLng, lng)
  }

  val latSpan = maxLat - minLat
  val centerLat = ((minLat + maxLat) / 2.0) + if (applyOverlayVerticalShift) latSpan * 0.08 else 0.0
  val centerLng = (minLng + maxLng) / 2.0

  val logicalPadW = max(90.0, (widthPx / max(1f, worldScale)) - 64.0)
  val logicalPadH = max(90.0, (heightPx / max(1f, worldScale)) - 96.0)
  var chosenZoom = 4.5f
  var z = 15.0f
  while (z >= 4.0f) {
    val spanX = abs(lngToWorldX(maxLng, z) - lngToWorldX(minLng, z))
    val spanY = abs(latToWorldY(minLat, z) - latToWorldY(maxLat, z))
    if (spanX <= logicalPadW && spanY <= logicalPadH) {
      chosenZoom = z
      break
    }
    z -= 0.5f
  }
  return MapCameraState(centerLat.coerceIn(-84.0, 84.0), centerLng.coerceIn(-179.0, 179.0), chosenZoom)
}

private object OsmTileRepository {
  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(6, TimeUnit.SECONDS)
      .readTimeout(8, TimeUnit.SECONDS)
      .build()
  }

  private val memoryCache = object : LinkedHashMap<OsmTileKey, ImageBitmap>(180, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<OsmTileKey, ImageBitmap>?): Boolean {
      return size > 180
    }
  }
  private val inFlight = mutableSetOf<OsmTileKey>()
  private val failedTimestamps = mutableMapOf<OsmTileKey, Long>()
  private const val RETRY_COOLDOWN_MS = 20_000L

  @Synchronized
  fun getCached(key: OsmTileKey): ImageBitmap? = memoryCache[key]

  @Synchronized
  fun getCachedFallbackParent(key: OsmTileKey): Pair<OsmTileKey, ImageBitmap>? {
    if (key.z <= 3) return null
    val pz = key.z - 1
    val px = Math.floorDiv(key.x, 2)
    val py = Math.floorDiv(key.y, 2)
    val parentKey = OsmTileKey(pz, px, py)
    return memoryCache[parentKey]?.let { parentKey to it }
  }

  @Synchronized
  fun shouldFetch(key: OsmTileKey): Boolean {
    if (memoryCache.containsKey(key) || inFlight.contains(key)) return false
    val lastFailedAt = failedTimestamps[key]
    if (lastFailedAt != null && System.currentTimeMillis() - lastFailedAt < RETRY_COOLDOWN_MS) {
      return false
    }
    inFlight.add(key)
    return true
  }

  @Synchronized
  private fun markLoaded(key: OsmTileKey, bitmap: ImageBitmap) {
    inFlight.remove(key)
    failedTimestamps.remove(key)
    memoryCache[key] = bitmap
  }

  @Synchronized
  private fun markFailed(key: OsmTileKey) {
    inFlight.remove(key)
    failedTimestamps[key] = System.currentTimeMillis()
  }

  @Synchronized
  private fun clearInFlightIfUnloaded(key: OsmTileKey) {
    inFlight.remove(key)
  }

  private var legacyCacheCleaned = false

  suspend fun fetchTile(context: Context, key: OsmTileKey): Boolean = withContext(Dispatchers.IO) {
    var settled = false
    try {
      if (!legacyCacheCleaned) {
        legacyCacheCleaned = true
        runCatching {
          File(context.cacheDir, "osm_tiles_v2").deleteRecursively()
        }
      }

      val diskFile = File(context.cacheDir, "osm_tiles_v3/${key.z}_${key.x}_${key.y}.png")
      if (diskFile.exists() && diskFile.length() > 200L) {
        val decoded = runCatching {
          BitmapFactory.decodeFile(diskFile.absolutePath)?.asImageBitmap()
        }.getOrNull()
        if (decoded != null) {
          markLoaded(key, decoded)
          settled = true
          return@withContext true
        }
      }

      val urls = listOf(
        "https://tile.openstreetmap.org/${key.z}/${key.x}/${key.y}.png",
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/${key.z}/${key.y}/${key.x}",
      )

      val userAgent = "HomelyByHomieFieldOps/1.0 (${context.packageName})"
      for (url in urls) {
        val bytes = runCatching {
          val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()
          httpClient.newCall(request).execute().use { response ->
            val contentType = response.header("Content-Type").orEmpty()
            if (response.isSuccessful && (contentType.isBlank() || contentType.startsWith("image/", ignoreCase = true))) {
              response.body?.bytes()
            } else {
              null
            }
          }
        }.getOrNull()

        if (bytes != null && bytes.size > 200) {
          val decoded = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
          }.getOrNull()
          if (decoded != null) {
            runCatching {
              diskFile.parentFile?.mkdirs()
              diskFile.writeBytes(bytes)
            }
            markLoaded(key, decoded)
            settled = true
            return@withContext true
          }
        }
      }

      markFailed(key)
      settled = true
      false
    } finally {
      if (!settled) {
        clearInFlightIfUnloaded(key)
      }
    }
  }
}

@Composable
fun RealOsmMapView(
  shops: List<Shop>,
  routes: List<DeliveryRoute>,
  selectedRouteId: String,
  selectedShopId: String?,
  showPolylines: Boolean,
  userLatitude: Double?,
  userLongitude: Double?,
  recenterUserTrigger: Int = 0,
  fitBoundsTrigger: Int = 0,
  compactPreviewMode: Boolean = false,
  focusLatitude: Double? = null,
  focusLongitude: Double? = null,
  navigationPins: List<NavigationMapPin>? = null,
  activeLegPolyline: List<Pair<Double, Double>> = emptyList(),
  upcomingRoutePolyline: List<Pair<Double, Double>> = emptyList(),
  followUserLocation: Boolean = false,
  showBuiltInZoomControls: Boolean = true,
  fitBoundsIncludesUser: Boolean = false,
  focusShopTrigger: Int = 0,
  onPinSelected: (String) -> Unit = {},
  onMapSurfaceTapped: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  if (LocalInspectionMode.current) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(CartographicLand),
      contentAlignment = Alignment.Center,
    ) {
      Text("Map Preview", style = MaterialTheme.typography.labelMedium, color = DeepSlateInk)
    }
    return
  }

  val context = LocalContext.current
  val density = LocalDensity.current
  val worldScale = density.density.coerceIn(1f, 3.5f)
  val viewConfiguration = LocalViewConfiguration.current
  val textMeasurer = rememberTextMeasurer()

  val latestOnPinSelected by rememberUpdatedState(onPinSelected)
  val latestOnMapTapped by rememberUpdatedState(onMapSurfaceTapped)

  val preparedData = remember(shops, routes, selectedRouteId, navigationPins) {
    if (navigationPins != null) {
      emptyList<PreparedPinRender>() to emptyList()
    } else {
      prepareMapRenderData(shops, routes, selectedRouteId)
    }
  }
  val preparedPins = preparedData.first
  val preparedTracks = preparedData.second
  val validNavPins = remember(navigationPins) {
    navigationPins?.filter { it.shop.latitude != 0.0 || it.shop.longitude != 0.0 }
  }

  var canvasSize by remember { mutableStateOf(IntSize.Zero) }
  var camera by remember {
    mutableStateOf(
      if (validNavPins != null) {
        val navPoints = buildList {
          if (fitBoundsIncludesUser && userLatitude != null && userLongitude != null &&
            (userLatitude != 0.0 || userLongitude != 0.0)
          ) {
            add(userLatitude to userLongitude)
          }
          validNavPins.forEach { add(it.shop.latitude to it.shop.longitude) }
        }
        computeFitBoundsCamera(navPoints, 1080f, 800f, singlePointZoom = 14.0f, worldScale = worldScale)
      } else {
        computeInitialCamera(
          pins = preparedPins,
          userLatitude = userLatitude,
          userLongitude = userLongitude,
          compactPreviewMode = compactPreviewMode,
          focusLatitude = focusLatitude,
          focusLongitude = focusLongitude,
          worldScale = worldScale,
        )
      }
    )
  }
  var hasFramedWithSize by remember { mutableStateOf(false) }
  var userHasPanned by remember { mutableStateOf(false) }
  var lastFramedRouteId by remember { mutableStateOf(selectedRouteId) }
  var lastFocusCoords by remember { mutableStateOf(focusLatitude to focusLongitude) }
  var lastPinCount by remember { mutableIntStateOf(validNavPins?.size ?: preparedPins.size) }
  var tileRevision by remember { mutableIntStateOf(0) }
  val hitboxes = remember { mutableListOf<RenderedPinHitbox>() }

  // Compute visible tile grid outside DrawScope so network fetches never happen inside onDraw
  val visibleTiles = remember(camera, canvasSize, worldScale) {
    computeVisibleTiles(camera = camera, canvasSize = canvasSize, worldScale = worldScale)
  }
  val visibleTileKeys = remember(visibleTiles) {
    visibleTiles.map { it.key }
  }

  // Key async tile loads strictly on visibleTileKeys so pixel panning within the same grid
  // does not cancel and re-launch coroutines at 120 Hz
  LaunchedEffect(visibleTileKeys) {
    val toFetch = visibleTileKeys.filter { OsmTileRepository.shouldFetch(it) }
    if (toFetch.isEmpty()) return@LaunchedEffect
    coroutineScope {
      val results = toFetch.map { key ->
        async { OsmTileRepository.fetchTile(context, key) }
      }.awaitAll()
      if (results.any { it }) {
        tileRevision++
      }
    }
  }

  // Auto-frame when viewport is measured, route changes, shops load asynchronously, or compact focus changes
  LaunchedEffect(
    canvasSize,
    selectedRouteId,
    compactPreviewMode,
    focusLatitude,
    focusLongitude,
    preparedPins.size,
    validNavPins?.size,
  ) {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect
    val currentPinCount = validNavPins?.size ?: preparedPins.size
    val focusChanged = compactPreviewMode && lastFocusCoords != (focusLatitude to focusLongitude)
    val routeChanged = lastFramedRouteId != selectedRouteId
    val pinsJustLoaded = !userHasPanned && lastPinCount == 0 && currentPinCount > 0

    if (routeChanged) {
      userHasPanned = false
    }

    if (!hasFramedWithSize || routeChanged || focusChanged || pinsJustLoaded) {
      camera = if (validNavPins != null) {
        val navPts = buildList {
          if (fitBoundsIncludesUser && userLatitude != null && userLongitude != null &&
            (userLatitude != 0.0 || userLongitude != 0.0)
          ) {
            add(userLatitude to userLongitude)
          }
          validNavPins.forEach { add(it.shop.latitude to it.shop.longitude) }
        }
        if (navPts.isEmpty() && userLatitude != null && userLongitude != null &&
          (userLatitude != 0.0 || userLongitude != 0.0)
        ) {
          MapCameraState(userLatitude, userLongitude, 14.0f)
        } else {
          computeFitBoundsCamera(
            points = navPts,
            widthPx = canvasSize.width.toFloat(),
            heightPx = canvasSize.height.toFloat(),
            singlePointZoom = 14.0f,
            applyOverlayVerticalShift = false,
            worldScale = worldScale,
          )
        }
      } else {
        computeAutoFrameCamera(
          pins = preparedPins,
          userLatitude = userLatitude,
          userLongitude = userLongitude,
          compactPreviewMode = compactPreviewMode,
          focusLatitude = focusLatitude,
          focusLongitude = focusLongitude,
          widthPx = canvasSize.width.toFloat(),
          heightPx = canvasSize.height.toFloat(),
          worldScale = worldScale,
        )
      }
      hasFramedWithSize = true
      lastFramedRouteId = selectedRouteId
      lastFocusCoords = focusLatitude to focusLongitude
      lastPinCount = currentPinCount
    } else {
      lastPinCount = currentPinCount
    }
  }

  LaunchedEffect(followUserLocation, userLatitude, userLongitude) {
    if (
      followUserLocation &&
      !userHasPanned &&
      userLatitude != null &&
      userLongitude != null &&
      (userLatitude != 0.0 || userLongitude != 0.0)
    ) {
      camera = MapCameraState(userLatitude, userLongitude, max(camera.zoom, 14.5f))
    }
  }

  LaunchedEffect(recenterUserTrigger, userLatitude, userLongitude) {
    if (
      recenterUserTrigger > 0 &&
      userLatitude != null &&
      userLongitude != null &&
      (userLatitude != 0.0 || userLongitude != 0.0)
    ) {
      userHasPanned = false
      camera = MapCameraState(userLatitude, userLongitude, 14.5f)
    }
  }

  LaunchedEffect(fitBoundsTrigger) {
    if (fitBoundsTrigger > 0 && canvasSize.width > 0 && canvasSize.height > 0) {
      userHasPanned = false
      camera = if (validNavPins != null) {
        val navPts = buildList {
          if (fitBoundsIncludesUser && userLatitude != null && userLongitude != null &&
            (userLatitude != 0.0 || userLongitude != 0.0)
          ) {
            add(userLatitude to userLongitude)
          }
          validNavPins.forEach { add(it.shop.latitude to it.shop.longitude) }
        }
        computeFitBoundsCamera(
          points = navPts,
          widthPx = canvasSize.width.toFloat(),
          heightPx = canvasSize.height.toFloat(),
          singlePointZoom = 14.0f,
          applyOverlayVerticalShift = false,
          worldScale = worldScale,
        )
      } else {
        computeAutoFrameCamera(
          pins = preparedPins,
          userLatitude = userLatitude,
          userLongitude = userLongitude,
          compactPreviewMode = compactPreviewMode,
          focusLatitude = focusLatitude,
          focusLongitude = focusLongitude,
          widthPx = canvasSize.width.toFloat(),
          heightPx = canvasSize.height.toFloat(),
          worldScale = worldScale,
        )
      }
    }
  }

  LaunchedEffect(focusShopTrigger, selectedShopId) {
    if (focusShopTrigger > 0 && selectedShopId != null) {
      val navShop = validNavPins?.firstOrNull { it.shop.id == selectedShopId }?.shop
      val regularShop = preparedPins.firstOrNull { it.shop.id == selectedShopId }?.shop
      val targetShop = navShop ?: regularShop
      if (targetShop != null && (targetShop.latitude != 0.0 || targetShop.longitude != 0.0)) {
        userHasPanned = true
        camera = MapCameraState(
          centerLat = targetShop.latitude,
          centerLng = targetShop.longitude,
          zoom = max(camera.zoom, 14.5f),
        )
      }
    }
  }

  val pinLabelStyle = MaterialTheme.typography.labelSmall.copy(
    fontSize = 10.5.sp,
    fontWeight = FontWeight.SemiBold,
  )
  val pinGlyphStyle = MaterialTheme.typography.labelSmall.copy(
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
  )
  val cityLabelStyle = MaterialTheme.typography.labelSmall.copy(
    fontSize = 9.5.sp,
    fontWeight = FontWeight.Medium,
  )
  val userPillStyle = MaterialTheme.typography.labelSmall.copy(
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
  )

  val scratch = remember(density.density) { MapCanvasScratch(density.density) }
  val measuredCities = remember(textMeasurer, cityLabelStyle) {
    regionHubCities.map { (cityName, cLat, cLng) ->
      MeasuredCityMarker(
        lat = cLat,
        lng = cLng,
        layout = textMeasurer.measure(cityName, style = cityLabelStyle),
      )
    }
  }
  val measuredUserPill = remember(textMeasurer, userPillStyle, compactPreviewMode) {
    if (compactPreviewMode) null else textMeasurer.measure("My Location", style = userPillStyle)
  }
  val measuredTracks = remember(preparedTracks, compactPreviewMode, textMeasurer, pinLabelStyle, density.density) {
    val maxLabelW = with(density) { 148.dp.toPx().roundToInt() }
    val trackTitleStyle = pinLabelStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
    preparedTracks.map { track ->
      val label = if (!compactPreviewMode) {
        val shortTitle = track.route.name.substringAfter(":").trim().ifBlank { track.route.name }
        textMeasurer.measure(
          text = shortTitle,
          style = trackTitleStyle,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          constraints = Constraints(maxWidth = maxLabelW),
        )
      } else {
        null
      }
      MeasuredTrackRender(track = track, labelLayout = label)
    }
  }
  val orderedMeasuredNavPins = remember(validNavPins, selectedShopId, textMeasurer, pinGlyphStyle, pinLabelStyle, density.density) {
    if (validNavPins == null) {
      null
    } else {
      val sorted = validNavPins.sortedBy { pin ->
        when {
          pin.shop.id == selectedShopId -> 3
          pin.state == StopPinState.ACTIVE_TARGET -> 2
          pin.state == StopPinState.UPCOMING -> 1
          else -> 0
        }
      }
      val activeGlyphStyle = pinGlyphStyle.copy(fontSize = 11.sp)
      val defaultGlyphStyle = pinGlyphStyle.copy(fontSize = 9.5.sp)
      val activeLabelStyle = pinLabelStyle.copy(fontSize = 10.sp)
      val defaultLabelStyle = pinLabelStyle.copy(fontSize = 9.sp)
      val maxLabelW = with(density) { 148.dp.toPx().roundToInt() }

      sorted.map { pin ->
        val isActive = pin.state == StopPinState.ACTIVE_TARGET
        val isCompleted = pin.state == StopPinState.COMPLETED
        val isSelected = pin.shop.id == selectedShopId
        val glyph = pin.stopNumber.toString()
        val glyphLayout = textMeasurer.measure(
          text = glyph,
          style = if (isActive || isSelected) activeGlyphStyle else defaultGlyphStyle,
        )
        val shortShop = pin.shop.name
          .substringBefore(" Bakery")
          .substringBefore(" Supermarket")
          .substringBefore(" Super Mart")
          .substringBefore(" Provision")
          .trim()
          .ifBlank { pin.shop.name }
        val label = when {
          isActive && pin.distanceLabel.isNotBlank() -> "${pin.stopNumber}. $shortShop (${pin.distanceLabel})"
          isSelected && pin.distanceLabel.isNotBlank() -> "${pin.stopNumber}. $shortShop (${pin.distanceLabel})"
          isCompleted -> "${pin.stopNumber}. ${shortShop.take(12)} ✓"
          else -> "${pin.stopNumber}. ${shortShop.take(14)}"
        }
        val labelLayout = textMeasurer.measure(
          text = label,
          style = if (isActive || isSelected) activeLabelStyle else defaultLabelStyle,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          constraints = Constraints(maxWidth = maxLabelW),
        )
        MeasuredNavPinRender(pin = pin, glyphLayout = glyphLayout, labelLayout = labelLayout)
      }
    }
  }
  val orderedMeasuredPins = remember(preparedPins, selectedShopId, compactPreviewMode, textMeasurer, pinGlyphStyle, pinLabelStyle, density.density) {
    val maxTextWidth = with(density) { 128.dp.toPx().roundToInt() }
    preparedPins
      .sortedBy { if (it.shop.id == selectedShopId) 1 else 0 }
      .map { pin ->
        val glyph = when {
          pin.isUnallocated -> "?"
          pin.stopNumber > 0 -> pin.stopNumber.toString()
          else -> "•"
        }
        val glyphLayout = textMeasurer.measure(text = glyph, style = pinGlyphStyle)
        val labelLayout = if (!compactPreviewMode && pin.shortName.isNotEmpty()) {
          val label = buildString {
            if (pin.stopNumber > 0 && !pin.isUnallocated) {
              append("#${pin.stopNumber} ")
            }
            append(pin.shortName)
          }
          textMeasurer.measure(
            text = label,
            style = pinLabelStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = maxTextWidth),
          )
        } else {
          null
        }
        MeasuredPinRender(pin = pin, glyphLayout = glyphLayout, labelLayout = labelLayout)
      }
  }

  val touchSlopPx = viewConfiguration.touchSlop

  val interactionModifier = if (compactPreviewMode) {
    Modifier
  } else {
    Modifier.pointerInput(worldScale) {
      awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startPos = down.position
        var lastPos = startPos
        var maxMoved = 0f
        var multiTouchUsed = false

        while (true) {
          val moveEvent = awaitPointerEvent(PointerEventPass.Main)
          val activeChanges = moveEvent.changes.filter { it.pressed }
          if (activeChanges.isEmpty()) {
            if (!multiTouchUsed && maxMoved <= touchSlopPx) {
              val tappedPin = hitboxes.lastOrNull { hb ->
                hypot(lastPos.x - hb.center.x, lastPos.y - hb.center.y) <= hb.radiusPx
              }
              if (tappedPin != null) {
                latestOnPinSelected(tappedPin.shopId)
              } else {
                latestOnMapTapped()
              }
            }
            break
          }

          if (activeChanges.size >= 2) {
            multiTouchUsed = true
          }

          val panDelta = moveEvent.calculatePan()
          val zoomFactor = moveEvent.calculateZoom()
          val currentPos = activeChanges.first().position
          maxMoved = max(maxMoved, hypot(currentPos.x - startPos.x, currentPos.y - startPos.y))
          lastPos = currentPos

          if (panDelta != Offset.Zero || (zoomFactor != 1f && zoomFactor.isFinite())) {
            userHasPanned = true
            val prevCam = camera
            val nextZoom = (prevCam.zoom + (ln(zoomFactor.coerceIn(0.5f, 2.0f)) / ln(2f)))
              .coerceIn(MIN_ZOOM, MAX_ZOOM)
            val cx = lngToWorldX(prevCam.centerLng, nextZoom) - (panDelta.x / worldScale)
            val cy = latToWorldY(prevCam.centerLat, nextZoom) - (panDelta.y / worldScale)
            camera = MapCameraState(
              centerLat = worldYToLat(cy, nextZoom),
              centerLng = worldXToLng(cx, nextZoom),
              zoom = nextZoom,
            )
            moveEvent.changes.forEach { it.consume() }
          }
        }
      }
    }
  }

  val loadedTileCount = remember(visibleTiles, tileRevision) {
    visibleTiles.count { OsmTileRepository.getCached(it.key) != null }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .clipToBounds()
      .background(CartographicLand)
      .onSizeChanged { canvasSize = it }
      .testTag("real_osm_map_canvas"),
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .then(interactionModifier),
    ) {
      @Suppress("UNUSED_VARIABLE")
      val revisionTick = tileRevision
      val w = size.width
      val h = size.height
      if (w <= 0f || h <= 0f) return@Canvas

      // 1. Rich Geographic Road Map Base (Ocean, Coastline, Highways, Major Cities)
      drawGeographicBaseLayer(
        w = w,
        h = h,
        camera = camera,
        worldScale = worldScale,
        measuredCities = measuredCities,
        scratch = scratch,
      )

      // 2. Real OpenStreetMap / Street Tiles overlaid cleanly at logical DPI scale
      for (placement in visibleTiles) {
        val key = placement.key
        val tileBitmap = OsmTileRepository.getCached(key)
        if (tileBitmap != null) {
          drawImage(
            image = tileBitmap,
            dstOffset = placement.dstOffset,
            dstSize = placement.dstSize,
          )
        } else {
          val parent = OsmTileRepository.getCachedFallbackParent(key)
          if (parent != null) {
            val (pKey, pBitmap) = parent
            val subX = key.x - pKey.x * 2
            val subY = key.y - pKey.y * 2
            val srcHalf = pBitmap.width / 2
            drawImage(
              image = pBitmap,
              srcOffset = IntOffset(subX * srcHalf, subY * srcHalf),
              srcSize = IntSize(srcHalf, srcHalf),
              dstOffset = placement.dstOffset,
              dstSize = placement.dstSize,
            )
          }
        }
      }

      // 3. Route Polylines (Navigation Mode vs Route Planning Mode)
      if (showPolylines) {
        if (orderedMeasuredNavPins != null) {
          drawNavigationPolylines(
            activeLegPolyline = activeLegPolyline,
            upcomingRoutePolyline = upcomingRoutePolyline,
            camera = camera,
            widthPx = w,
            heightPx = h,
            worldScale = worldScale,
            scratch = scratch,
          )
        } else {
          drawRouteTracks(
            tracks = measuredTracks,
            camera = camera,
            widthPx = w,
            heightPx = h,
            worldScale = worldScale,
            compactPreviewMode = compactPreviewMode,
            scratch = scratch,
          )
        }
      }

      // 4. Live Device GPS Marker
      if (userLatitude != null && userLongitude != null && (userLatitude != 0.0 || userLongitude != 0.0)) {
        val userPos = projectLatLngToScreen(userLatitude, userLongitude, camera, w, h, worldScale)
        drawUserGpsMarker(
          pos = userPos,
          measuredUserPill = measuredUserPill,
        )
      }

      // 5. Shop Pins / Sequential Numbered Navigation Targets
      hitboxes.clear()
      val tapHitRadiusPx = 28.dp.toPx()

      if (orderedMeasuredNavPins != null) {
        for (item in orderedMeasuredNavPins) {
          val pin = item.pin
          val pt = projectLatLngToScreen(pin.shop.latitude, pin.shop.longitude, camera, w, h, worldScale)
          if (pt.x < -80f || pt.x > w + 80f || pt.y < -80f || pt.y > h + 80f) continue
          val isSelected = pin.shop.id == selectedShopId
          drawNavigationTargetPin(
            center = pt,
            item = item,
            isSelected = isSelected,
            scratch = scratch,
          )
          hitboxes += RenderedPinHitbox(
            shopId = pin.shop.id,
            center = pt,
            radiusPx = tapHitRadiusPx,
          )
        }
      } else {
        for (item in orderedMeasuredPins) {
          val pin = item.pin
          val pt = projectLatLngToScreen(pin.shop.latitude, pin.shop.longitude, camera, w, h, worldScale)
          if (pt.x < -80f || pt.x > w + 80f || pt.y < -80f || pt.y > h + 80f) continue
          val isSelected = pin.shop.id == selectedShopId
          drawShopPinBadge(
            center = pt,
            item = item,
            isSelected = isSelected,
            scratch = scratch,
          )
          hitboxes += RenderedPinHitbox(
            shopId = pin.shop.id,
            center = pt,
            radiusPx = tapHitRadiusPx,
          )
        }
      }

      // 6. Focus Teardrop Pin (Add/Edit Shop preview card)
      if (focusLatitude != null && focusLongitude != null && (focusLatitude != 0.0 || focusLongitude != 0.0)) {
        val focusPos = projectLatLngToScreen(focusLatitude, focusLongitude, camera, w, h, worldScale)
        drawFocusTeardropMarker(focusPos, scratch)
      }
    }

    // Native M3 Zoom +/- Controls
    if (!compactPreviewMode && showBuiltInZoomControls) {
      Column(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 136.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        MapZoomControlButton(
          icon = Icons.Rounded.Add,
          contentDescription = "Zoom in map",
          onClick = {
            userHasPanned = true
            camera = camera.copy(zoom = (camera.zoom + 1f).coerceIn(MIN_ZOOM, MAX_ZOOM))
          },
          modifier = Modifier.testTag("map_zoom_in_button"),
        )
        MapZoomControlButton(
          icon = Icons.Rounded.Remove,
          contentDescription = "Zoom out map",
          onClick = {
            userHasPanned = true
            camera = camera.copy(zoom = (camera.zoom - 1f).coerceIn(MIN_ZOOM, MAX_ZOOM))
          },
          modifier = Modifier.testTag("map_zoom_out_button"),
        )
      }

      Surface(
        color = CartographicLand.copy(alpha = 0.90f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 10.dp, bottom = 18.dp),
      ) {
        Text(
          text = if (loadedTileCount > 0) "© OpenStreetMap • Street View" else "South India Corridor Map",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = MutedSlateInk,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
    }
  }
}

private fun computeVisibleTiles(
  camera: MapCameraState,
  canvasSize: IntSize,
  worldScale: Float,
): List<VisibleTilePlacement> {
  if (canvasSize.width <= 0 || canvasSize.height <= 0) return emptyList()
  val w = canvasSize.width.toDouble()
  val h = canvasSize.height.toDouble()
  val tileZoom = camera.zoom.roundToInt().coerceIn(3, 18)
  val totalScale = (2.0.pow((camera.zoom - tileZoom).toDouble()) * worldScale)
  val scaledTileSize = (OSM_TILE_SIZE * totalScale)

  val centerWorldX = lngToWorldX(camera.centerLng, tileZoom.toFloat())
  val centerWorldY = latToWorldY(camera.centerLat, tileZoom.toFloat())

  val startWorldX = centerWorldX - (w / 2.0) / totalScale
  val startWorldY = centerWorldY - (h / 2.0) / totalScale
  val endWorldX = centerWorldX + (w / 2.0) / totalScale
  val endWorldY = centerWorldY + (h / 2.0) / totalScale

  val nTiles = 1 shl tileZoom
  val minTx = floor(startWorldX / OSM_TILE_SIZE).toInt()
  val maxTx = floor(endWorldX / OSM_TILE_SIZE).toInt()
  val minTy = floor(startWorldY / OSM_TILE_SIZE).toInt()
  val maxTy = floor(endWorldY / OSM_TILE_SIZE).toInt()

  val placements = ArrayList<VisibleTilePlacement>()
  for (tx in minTx..maxTx) {
    for (ty in minTy..maxTy) {
      if (ty < 0 || ty >= nTiles) continue
      val wrappedX = ((tx % nTiles) + nTiles) % nTiles
      val dstX = (w / 2.0 + (tx * OSM_TILE_SIZE - centerWorldX) * totalScale).roundToInt()
      val dstY = (h / 2.0 + (ty * OSM_TILE_SIZE - centerWorldY) * totalScale).roundToInt()
      val sizePx = (scaledTileSize + 1.5).roundToInt().coerceAtLeast(1)
      placements += VisibleTilePlacement(
        key = OsmTileKey(tileZoom, wrappedX, ty),
        dstOffset = IntOffset(dstX, dstY),
        dstSize = IntSize(sizePx, sizePx),
      )
    }
  }
  return placements
}

@Composable
private fun MapZoomControlButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    shape = RoundedCornerShape(10.dp),
    shadowElevation = 3.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .size(40.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

private fun prepareMapRenderData(
  shops: List<Shop>,
  routes: List<DeliveryRoute>,
  selectedRouteId: String,
): Pair<List<PreparedPinRender>, List<PreparedRouteTrack>> {
  val routeMap = routes.associateBy { it.id }

  val stopNumberByShopId = mutableMapOf<String, Int>()
  val sortedShopsByRouteId = routes.associate { route ->
    val sorted = RoutesCatalog.sortShopsByRouteSequence(
      shops.filter { it.routeId == route.id },
      route,
    )
    sorted.forEachIndexed { idx, shop ->
      stopNumberByShopId[shop.id] = idx + 1
    }
    route.id to sorted
  }

  val visibleShops = if (selectedRouteId == RoutesCatalog.ALL) {
    shops
  } else {
    shops.filter { it.routeId == selectedRouteId || it.routeId == RoutesCatalog.UNALLOCATED || it.routeId.isBlank() }
  }

  val routesToDraw = if (selectedRouteId == RoutesCatalog.ALL) {
    routes
  } else {
    routes.filter { it.id == selectedRouteId }
  }

  val tracks = routesToDraw.mapNotNull { route ->
    val coords = sortedShopsByRouteId[route.id].orEmpty()
      .filter { it.latitude != 0.0 || it.longitude != 0.0 }
      .map { it.latitude to it.longitude }
    if (coords.size < 2) {
      null
    } else {
      PreparedRouteTrack(
        route = route,
        spec = RouteColorPalette.specForIndex(route.colorIndex),
        coordinates = coords,
      )
    }
  }

  val pins = visibleShops.mapNotNull { shop ->
    if (shop.latitude == 0.0 && shop.longitude == 0.0) return@mapNotNull null
    val isUnalloc = shop.routeId == RoutesCatalog.UNALLOCATED || !routeMap.containsKey(shop.routeId)
    val stopNum = if (!isUnalloc) {
      stopNumberByShopId[shop.id] ?: 0
    } else {
      0
    }
    val spec = routeMap[shop.routeId]?.let { RouteColorPalette.specForIndex(it.colorIndex) }
    PreparedPinRender(
      shop = shop,
      shortName = shop.name.substringBefore(" Bakery").substringBefore(" Supermarket").trim(),
      stopNumber = stopNum,
      isUnallocated = isUnalloc,
      colorSpec = spec,
    )
  }

  return pins to tracks
}

private fun computeInitialCamera(
  pins: List<PreparedPinRender>,
  userLatitude: Double?,
  userLongitude: Double?,
  compactPreviewMode: Boolean,
  focusLatitude: Double?,
  focusLongitude: Double?,
  worldScale: Float,
): MapCameraState {
  if (compactPreviewMode) {
    val targetLat = focusLatitude?.takeIf { it != 0.0 } ?: userLatitude?.takeIf { it != 0.0 }
    val targetLng = focusLongitude?.takeIf { it != 0.0 } ?: userLongitude?.takeIf { it != 0.0 }
    if (targetLat != null && targetLng != null) {
      return MapCameraState(targetLat, targetLng, 14.0f)
    }
  }
  val points = pins.map { it.shop.latitude to it.shop.longitude }
  return if (points.isNotEmpty()) {
    computeFitBoundsCamera(points, 1080f, 1100f, worldScale = worldScale)
  } else if (userLatitude != null && userLongitude != null && (userLatitude != 0.0 || userLongitude != 0.0)) {
    MapCameraState(userLatitude, userLongitude, 13.5f)
  } else {
    MapCameraState(DEFAULT_REGION_LAT, DEFAULT_REGION_LNG, DEFAULT_REGION_ZOOM)
  }
}

private fun computeAutoFrameCamera(
  pins: List<PreparedPinRender>,
  userLatitude: Double?,
  userLongitude: Double?,
  compactPreviewMode: Boolean,
  focusLatitude: Double?,
  focusLongitude: Double?,
  widthPx: Float,
  heightPx: Float,
  worldScale: Float,
): MapCameraState {
  if (compactPreviewMode) {
    val targetLat = focusLatitude?.takeIf { it != 0.0 } ?: userLatitude?.takeIf { it != 0.0 }
    val targetLng = focusLongitude?.takeIf { it != 0.0 } ?: userLongitude?.takeIf { it != 0.0 }
    if (targetLat != null && targetLng != null) {
      return MapCameraState(targetLat, targetLng, 14.0f)
    }
  }
  val pts = pins.map { it.shop.latitude to it.shop.longitude }
  if (pts.isEmpty() && userLatitude != null && userLongitude != null && (userLatitude != 0.0 || userLongitude != 0.0)) {
    return MapCameraState(userLatitude, userLongitude, 13.5f)
  }
  return computeFitBoundsCamera(
    points = pts,
    widthPx = widthPx,
    heightPx = heightPx,
    singlePointZoom = 13.0f,
    applyOverlayVerticalShift = !compactPreviewMode,
    worldScale = worldScale,
  )
}

private fun DrawScope.drawGeographicBaseLayer(
  w: Float,
  h: Float,
  camera: MapCameraState,
  worldScale: Float,
  measuredCities: List<MeasuredCityMarker>,
  scratch: MapCanvasScratch,
) {
  drawRect(color = CartographicLand, topLeft = Offset.Zero, size = Size(w, h))

  val cx = lngToWorldX(camera.centerLng, camera.zoom)
  val cy = latToWorldY(camera.centerLat, camera.zoom)
  val halfW = w / 2f
  val halfH = h / 2f

  // Arabian Sea polygon west of India's west coast
  val path = scratch.path
  path.rewind()
  for (i in westOceanCoastCoords.indices) {
    val (oLat, oLng) = westOceanCoastCoords[i]
    val sx = (halfW + (lngToWorldX(oLng, camera.zoom) - cx) * worldScale).toFloat()
    val sy = (halfH + (latToWorldY(oLat, camera.zoom) - cy) * worldScale).toFloat()
    if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
  }
  path.close()
  drawPath(path = path, color = CartographicOcean)
  drawPath(path = path, color = CartographicCoastStroke, style = scratch.coastStroke)

  // Soft Western Ghats forest/park terrain tint
  path.rewind()
  for (i in westernGhatsCoords.indices) {
    val (gLat, gLng) = westernGhatsCoords[i]
    val sx = (halfW + (lngToWorldX(gLng, camera.zoom) - cx) * worldScale).toFloat()
    val sy = (halfH + (latToWorldY(gLat, camera.zoom) - cy) * worldScale).toFloat()
    if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
  }
  drawPath(path = path, color = CartographicPark.copy(alpha = 0.72f), style = scratch.ghatsStroke)

  // Coordinate graticule
  val gridDegStep = when {
    camera.zoom >= 13f -> 0.02
    camera.zoom >= 10f -> 0.1
    camera.zoom >= 7.5f -> 0.5
    else -> 1.5
  }
  val halfWorldW = (w / 2.0) / worldScale
  val halfWorldH = (h / 2.0) / worldScale
  val minLng = worldXToLng(cx - halfWorldW, camera.zoom)
  val maxLng = worldXToLng(cx + halfWorldW, camera.zoom)
  val maxLat = worldYToLat(cy - halfWorldH, camera.zoom)
  val minLat = worldYToLat(cy + halfWorldH, camera.zoom)
  val gridColor = CartographicGrid.copy(alpha = 0.5f)
  val gridStrokePx = 1.dp.toPx()

  var lng = floor(minLng / gridDegStep) * gridDegStep
  var guard = 0
  while (lng <= maxLng && guard < 36) {
    val sx = (halfW + (lngToWorldX(lng, camera.zoom) - cx) * worldScale).toFloat()
    drawLine(
      color = gridColor,
      start = Offset(sx, 0f),
      end = Offset(sx, h),
      strokeWidth = gridStrokePx,
    )
    lng += gridDegStep
    guard++
  }

  var lat = floor(minLat / gridDegStep) * gridDegStep
  guard = 0
  while (lat <= maxLat && guard < 36) {
    val sy = (halfH + (latToWorldY(lat, camera.zoom) - cy) * worldScale).toFloat()
    drawLine(
      color = gridColor,
      start = Offset(0f, sy),
      end = Offset(w, sy),
      strokeWidth = gridStrokePx,
    )
    lat += gridDegStep
    guard++
  }

  // Major highway corridors (visible even before tiles load)
  val hwOuterColor = CartographicHighwayOuter.copy(alpha = 0.85f)
  for (corridor in allHighways) {
    path.rewind()
    for (i in corridor.indices) {
      val (hLat, hLng) = corridor[i]
      val sx = (halfW + (lngToWorldX(hLng, camera.zoom) - cx) * worldScale).toFloat()
      val sy = (halfH + (latToWorldY(hLat, camera.zoom) - cy) * worldScale).toFloat()
      if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
    }
    drawPath(path = path, color = hwOuterColor, style = scratch.hwOuterStroke)
    drawPath(path = path, color = CartographicHighwayInner, style = scratch.hwInnerStroke)
  }

  // City reference markers (using pre-measured TextLayouts)
  val outerR = 3.5.dp.toPx()
  val innerR = 2.dp.toPx()
  val labelDx = 5.dp.toPx()
  val cityTextColor = DeepSlateInk.copy(alpha = 0.72f)
  for (city in measuredCities) {
    val sx = (halfW + (lngToWorldX(city.lng, camera.zoom) - cx) * worldScale).toFloat()
    val sy = (halfH + (latToWorldY(city.lat, camera.zoom) - cy) * worldScale).toFloat()
    if (sx < -30f || sx > w + 30f || sy < -30f || sy > h + 30f) continue
    val pt = Offset(sx, sy)
    drawCircle(color = Color.White, radius = outerR, center = pt)
    drawCircle(color = MutedSlateInk, radius = innerR, center = pt)
    drawText(
      textLayoutResult = city.layout,
      color = cityTextColor,
      topLeft = Offset(sx + labelDx, sy - city.layout.size.height / 2f),
    )
  }
}

private fun DrawScope.drawRouteTracks(
  tracks: List<MeasuredTrackRender>,
  camera: MapCameraState,
  widthPx: Float,
  heightPx: Float,
  worldScale: Float,
  compactPreviewMode: Boolean,
  scratch: MapCanvasScratch,
) {
  val offsetPx = 1.8.dp.toPx()
  val labelCenters = ArrayList<Pair<Offset, MeasuredTrackRender>>(tracks.size)
  val path = scratch.path
  val screenPts = scratch.screenPointsBuffer

  for (item in tracks) {
    val track = item.track
    if (track.coordinates.size < 2) continue

    screenPts.clear()
    path.rewind()
    for (i in track.coordinates.indices) {
      val (lat, lng) = track.coordinates[i]
      val pt = projectLatLngToScreen(lat, lng, camera, widthPx, heightPx, worldScale)
      screenPts.add(pt)
      if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }

    val pri = track.spec.primary
    val sec = track.spec.secondary
    val pattern = track.spec.pattern

    drawPath(path = path, color = Color.White.copy(alpha = 0.88f), style = scratch.trackHaloStroke)

    if (sec == null || pattern == RouteComboPattern.SOLID) {
      drawPath(path = path, color = pri, style = scratch.trackPrimaryStroke)
    } else if (pattern == RouteComboPattern.SPLIT_HALF) {
      drawParallelTrack(screenPts, pri, scratch.trackSplitStroke, offsetPx, scratch.clipCirclePath)
      drawParallelTrack(screenPts, sec, scratch.trackSplitStroke, -offsetPx, scratch.clipCirclePath)
    } else if (pattern == RouteComboPattern.RING_CORE) {
      drawPath(path = path, color = pri, style = scratch.trackRingOuterStroke)
      drawPath(path = path, color = Color.White, style = scratch.trackRingMidStroke)
      drawPath(path = path, color = sec, style = scratch.trackRingCoreStroke)
    } else if (pattern == RouteComboPattern.DIAGONAL_STRIPE) {
      drawPath(path = path, color = pri, style = scratch.trackBgStroke)
      drawPath(path = path, color = sec, style = scratch.trackDiagStripeStroke)
    } else {
      drawPath(path = path, color = pri, style = scratch.trackBgStroke)
      drawPath(path = path, color = sec, style = scratch.trackDashStroke)
    }

    if (!compactPreviewMode) {
      val midIdx = (screenPts.size - 1) / 2
      val a = screenPts[midIdx]
      val b = if (midIdx + 1 < screenPts.size) screenPts[midIdx + 1] else a
      val midPt = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
      labelCenters.add(midPt to item)
    }
  }

  if (!compactPreviewMode && labelCenters.isNotEmpty()) {
    val topSafe = 96.dp.toPx()
    val bottomSafe = heightPx - 56.dp.toPx()
    val padH = 7.dp.toPx()
    val padV = 3.dp.toPx()
    val dotGap = 5.dp.toPx()
    val dotRadius = 3.5.dp.toPx()
    val corner = CornerRadius(999f, 999f)

    for ((midPt, item) in labelCenters) {
      val measured = item.labelLayout ?: continue
      val track = item.track
      if (midPt.x < -40f || midPt.x > widthPx + 40f || midPt.y < topSafe || midPt.y > bottomSafe) continue
      val pillW = padH * 2f + dotRadius * 2f + dotGap + measured.size.width
      val pillH = measured.size.height + padV * 2f
      val pillLeft = (midPt.x - pillW / 2f).coerceIn(8f, max(8f, widthPx - pillW - 8f))
      val pillTop = (midPt.y - pillH / 2f).coerceIn(topSafe, max(topSafe, bottomSafe - pillH))

      drawRoundRect(
        color = Color.White.copy(alpha = 0.94f),
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = corner,
      )
      drawRoundRect(
        color = track.spec.primary,
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = corner,
        style = scratch.pillTrackStroke,
      )
      val dotCenter = Offset(pillLeft + padH + dotRadius, pillTop + pillH / 2f)
      drawCircle(color = track.spec.primary, radius = dotRadius, center = dotCenter)
      if (track.spec.secondary != null) {
        drawCircle(color = track.spec.secondary, radius = dotRadius * 0.45f, center = dotCenter)
      }
      drawText(
        textLayoutResult = measured,
        color = DeepSlateInk,
        topLeft = Offset(dotCenter.x + dotRadius + dotGap, pillTop + padV),
      )
    }
  }
}

private fun DrawScope.drawParallelTrack(
  pts: List<Offset>,
  color: Color,
  stroke: Stroke,
  normalOffset: Float,
  reusablePath: Path,
) {
  reusablePath.rewind()
  for (i in 0 until pts.size - 1) {
    val a = pts[i]
    val b = pts[i + 1]
    val dx = b.x - a.x
    val dy = b.y - a.y
    val len = max(1f, hypot(dx, dy))
    val nx = (-dy / len) * normalOffset
    val ny = (dx / len) * normalOffset
    reusablePath.moveTo(a.x + nx, a.y + ny)
    reusablePath.lineTo(b.x + nx, b.y + ny)
  }
  drawPath(path = reusablePath, color = color, style = stroke)
}

private fun DrawScope.drawShopPinBadge(
  center: Offset,
  item: MeasuredPinRender,
  isSelected: Boolean,
  scratch: MapCanvasScratch,
) {
  val pin = item.pin
  val r = if (isSelected) 15.dp.toPx() else 12.5.dp.toPx()
  val d = r * 2f

  drawCircle(
    color = Color.Black.copy(alpha = 0.22f),
    radius = r,
    center = center + Offset(0f, 2.dp.toPx()),
  )

  val spec = pin.colorSpec
  val pri = if (pin.isUnallocated) Color.White else (spec?.primary ?: Color(0xFF1F4E43))
  val sec = spec?.secondary
  val pat = spec?.pattern ?: RouteComboPattern.SOLID

  if (pin.isUnallocated || sec == null || pat == RouteComboPattern.SOLID) {
    // Fast path: solid circle avoids allocating and pushing clipPath on GPU
    drawCircle(color = pri, radius = r, center = center)
  } else if (pat == RouteComboPattern.RING_CORE) {
    drawCircle(color = pri, radius = r, center = center)
    drawCircle(color = Color.White, radius = r * 0.62f, center = center)
    drawCircle(color = sec, radius = r * 0.44f, center = center)
  } else {
    val circlePath = scratch.clipCirclePath
    circlePath.rewind()
    circlePath.addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
    clipPath(circlePath) {
      if (pat == RouteComboPattern.SPLIT_HALF) {
        drawRect(color = pri, topLeft = Offset(center.x - r, center.y - r), size = Size(r, d))
        drawRect(color = sec, topLeft = Offset(center.x, center.y - r), size = Size(r, d))
      } else if (pat == RouteComboPattern.TOP_BOTTOM_DUO) {
        drawRect(color = pri, topLeft = Offset(center.x - r, center.y - r), size = Size(d, r))
        drawRect(color = sec, topLeft = Offset(center.x - r, center.y), size = Size(d, r))
      } else {
        drawRect(color = pri, topLeft = Offset(center.x - r, center.y - r), size = Size(d, d))
        val tri = scratch.triPath
        tri.rewind()
        tri.moveTo(center.x + r, center.y - r)
        tri.lineTo(center.x + r, center.y + r)
        tri.lineTo(center.x - r, center.y + r)
        tri.close()
        drawPath(path = tri, color = sec)
      }
    }
  }

  drawCircle(
    color = when {
      isSelected -> SaffronPrimary
      pin.isUnallocated -> CoralDanger
      else -> Color.White
    },
    radius = r,
    center = center,
    style = if (isSelected) scratch.pinSelectedStroke else scratch.pinDefaultStroke,
  )

  val glyphLayout = item.glyphLayout
  val glyphTopLeft = Offset(
    x = center.x - glyphLayout.size.width / 2f,
    y = center.y - glyphLayout.size.height / 2f,
  )
  if (!pin.isUnallocated && sec != null && pat == RouteComboPattern.RING_CORE) {
    drawCircle(
      color = Color.Black.copy(alpha = 0.36f),
      radius = r * 0.46f,
      center = center,
    )
  }
  drawText(
    textLayoutResult = glyphLayout,
    color = if (pin.isUnallocated) CoralDanger else Color.White,
    topLeft = glyphTopLeft,
  )

  val measuredLabel = item.labelLayout
  if (measuredLabel != null) {
    val padH = 6.dp.toPx()
    val padV = 3.dp.toPx()
    val pillW = measuredLabel.size.width + padH * 2f
    val pillH = measuredLabel.size.height + padV * 2f
    val pillLeft = (center.x - pillW / 2f).coerceIn(6f, max(6f, size.width - pillW - 6f))
    val pillTop = center.y - r - pillH - 4.dp.toPx()

    if (pillTop >= 2f) {
      val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())
      drawRoundRect(
        color = if (isSelected) SaffronPrimary else Color.White.copy(alpha = 0.94f),
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = corner,
      )
      drawRoundRect(
        color = if (isSelected) Color.White else CartographicGrid,
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = corner,
        style = scratch.pillPinStroke,
      )
      drawText(
        textLayoutResult = measuredLabel,
        color = if (isSelected) Color.White else DeepSlateInk,
        topLeft = Offset(pillLeft + padH, pillTop + padV),
      )
    }
  }
}

private fun DrawScope.drawUserGpsMarker(
  pos: Offset,
  measuredUserPill: TextLayoutResult?,
) {
  if (pos.x < -60f || pos.x > size.width + 60f || pos.y < -60f || pos.y > size.height + 60f) return
  drawCircle(color = CobaltInfo.copy(alpha = 0.16f), radius = 20.dp.toPx(), center = pos)
  drawCircle(color = Color.White, radius = 9.dp.toPx(), center = pos)
  drawCircle(color = CobaltInfo, radius = 6.5.dp.toPx(), center = pos)

  if (measuredUserPill != null) {
    val padH = 6.dp.toPx()
    val padV = 2.5.dp.toPx()
    val pillW = measuredUserPill.size.width + padH * 2f
    val pillH = measuredUserPill.size.height + padV * 2f
    val rx = pos.x - pillW / 2f
    val ry = pos.y + 12.dp.toPx()
    val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    drawRoundRect(
      color = CobaltInfo,
      topLeft = Offset(rx, ry),
      size = Size(pillW, pillH),
      cornerRadius = corner,
    )
    drawText(
      textLayoutResult = measuredUserPill,
      color = Color.White,
      topLeft = Offset(rx + padH, ry + padV),
    )
  }
}

private fun DrawScope.drawFocusTeardropMarker(
  pos: Offset,
  scratch: MapCanvasScratch,
) {
  if (pos.x < -60f || pos.x > size.width + 60f || pos.y < -60f || pos.y > size.height + 60f) return
  val haloR = 18.dp.toPx()
  val headR = 10.dp.toPx()
  val headOffset = 12.dp.toPx()

  drawCircle(color = SaffronPrimary.copy(alpha = 0.22f), radius = haloR, center = pos)

  val tipPath = scratch.path
  tipPath.rewind()
  tipPath.moveTo(pos.x - 7.dp.toPx(), pos.y - 7.dp.toPx())
  tipPath.lineTo(pos.x + 7.dp.toPx(), pos.y - 7.dp.toPx())
  tipPath.lineTo(pos.x, pos.y)
  tipPath.close()
  drawPath(path = tipPath, color = SaffronPrimary)
  drawCircle(color = SaffronPrimary, radius = headR, center = Offset(pos.x, pos.y - headOffset))
  drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(pos.x, pos.y - headOffset))
}

private fun DrawScope.drawNavigationPolylines(
  activeLegPolyline: List<Pair<Double, Double>>,
  upcomingRoutePolyline: List<Pair<Double, Double>>,
  camera: MapCameraState,
  widthPx: Float,
  heightPx: Float,
  worldScale: Float,
  scratch: MapCanvasScratch,
) {
  val path = scratch.path
  if (upcomingRoutePolyline.size >= 2) {
    path.rewind()
    for (i in upcomingRoutePolyline.indices) {
      val (lat, lng) = upcomingRoutePolyline[i]
      val pt = projectLatLngToScreen(lat, lng, camera, widthPx, heightPx, worldScale)
      if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }
    drawPath(path = path, color = Color.White.copy(alpha = 0.85f), style = scratch.navUpcomingHaloStroke)
    drawPath(path = path, color = CobaltInfo.copy(alpha = 0.72f), style = scratch.navUpcomingDashStroke)
  }

  if (activeLegPolyline.size >= 2) {
    path.rewind()
    for (i in activeLegPolyline.indices) {
      val (lat, lng) = activeLegPolyline[i]
      val pt = projectLatLngToScreen(lat, lng, camera, widthPx, heightPx, worldScale)
      if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }
    drawPath(path = path, color = Color.White.copy(alpha = 0.94f), style = scratch.navActiveHaloStroke)
    drawPath(path = path, color = SaffronPrimary, style = scratch.navActiveStroke)
  }
}

private fun DrawScope.drawNavigationTargetPin(
  center: Offset,
  item: MeasuredNavPinRender,
  isSelected: Boolean,
  scratch: MapCanvasScratch,
) {
  val pin = item.pin
  val isActive = pin.state == StopPinState.ACTIVE_TARGET
  val isCompleted = pin.state == StopPinState.COMPLETED
  val r = when {
    isActive -> 16.dp.toPx()
    isSelected -> 14.5.dp.toPx()
    else -> 12.dp.toPx()
  }

  if (isActive) {
    drawCircle(
      color = SaffronPrimary.copy(alpha = 0.22f),
      radius = 26.dp.toPx(),
      center = center,
    )
  }

  drawCircle(
    color = Color.Black.copy(alpha = 0.22f),
    radius = r,
    center = center + Offset(0f, 2.dp.toPx()),
  )

  val fillColor = when {
    isActive -> SaffronPrimary
    isCompleted -> EmeraldSuccess
    isSelected -> ForestSecondary
    else -> WarmIvorySurfaceVariant
  }
  val textColor = when {
    isActive || isCompleted || isSelected -> Color.White
    else -> DeepSlateInk
  }
  val borderColor = when {
    isActive -> Color.White
    isSelected -> ForestSecondary
    isCompleted -> Color.White
    else -> SaffronPrimary.copy(alpha = 0.65f)
  }

  drawCircle(color = fillColor, radius = r, center = center)
  drawCircle(
    color = borderColor,
    radius = r,
    center = center,
    style = if (isActive || isSelected) scratch.navPinSelectedStroke else scratch.navPinDefaultStroke,
  )

  val glyphLayout = item.glyphLayout
  drawText(
    textLayoutResult = glyphLayout,
    color = textColor,
    topLeft = Offset(
      x = center.x - glyphLayout.size.width / 2f,
      y = center.y - glyphLayout.size.height / 2f,
    ),
  )

  val measured = item.labelLayout
  if (measured != null) {
    val padH = 6.dp.toPx()
    val padV = 2.5.dp.toPx()
    val pillW = measured.size.width + padH * 2f
    val pillH = measured.size.height + padV * 2f
    val pillLeft = (center.x - pillW / 2f).coerceIn(6f, max(6f, size.width - pillW - 6f))
    val belowTop = center.y + r + 3.dp.toPx()
    val pillTop = if (belowTop + pillH <= size.height - 4f) {
      belowTop
    } else {
      (center.y - r - pillH - 3.dp.toPx()).coerceAtLeast(4f)
    }

    if (pillTop >= 2f && pillTop + pillH <= size.height - 2f) {
      val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())
      drawRoundRect(
        color = when {
          isActive -> SaffronPrimary
          isSelected -> ForestSecondary
          isCompleted -> EmeraldSuccess.copy(alpha = 0.92f)
          else -> Color.Black.copy(alpha = 0.78f)
        },
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = corner,
      )
      drawText(
        textLayoutResult = measured,
        color = Color.White,
        topLeft = Offset(pillLeft + padH, pillTop + padV),
      )
    }
  }
}
