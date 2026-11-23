package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.remote.NavigationRouteResult
import com.example.data.remote.OsrmRoutingClient
import com.example.ui.components.fetchCurrentDeviceLocation
import com.example.ui.components.hasLocationPermission
import com.example.ui.components.haversineDistanceMeters
import com.example.ui.components.observeDeviceLocationFlow
import com.example.ui.components.openDialer
import com.example.ui.theme.semanticColors
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransportScreen(
  shops: List<Shop>,
  orders: List<OrderRecord>,
  routes: List<DeliveryRoute>,
  userDisplayName: String,
  onMarkOrderDelivered: (orderId: String, shopName: String, podNote: String) -> Unit,
  onNavigateToRoutes: () -> Unit,
  onOpenSyncServers: () -> Unit,
  onShowFeedback: (String) -> Unit,
  onSaveRoute: (DeliveryRoute, String) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  var selectedRouteId by rememberSaveable {
    mutableStateOf(
      routes.firstOrNull { it.isActive }?.id
        ?: routes.firstOrNull()?.id
        ?: RoutesCatalog.R01_KANNUR_BANGALORE
    )
  }
  LaunchedEffect(routes) {
    if (routes.isNotEmpty() && routes.none { it.id == selectedRouteId }) {
      selectedRouteId = routes.firstOrNull { it.isActive }?.id ?: routes.first().id
    }
  }

  val activeRoute = remember(routes, selectedRouteId) {
    routes.firstOrNull { it.id == selectedRouteId } ?: routes.firstOrNull()
  }

  var selectedFilter by rememberSaveable { mutableStateOf(TransportStopFilter.ALL) }
  var showSearch by rememberSaveable { mutableStateOf(false) }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var useShortestPath by rememberSaveable { mutableStateOf(true) }
  var selectedPinShopId by rememberSaveable { mutableStateOf<String?>(null) }

  var showRouteSelectorSheet by remember { mutableStateOf(false) }
  var showEditVehicleSheet by remember { mutableStateOf(false) }
  var showDirectionsSheet by remember { mutableStateOf(false) }
  var isFullscreenNav by rememberSaveable { mutableStateOf(false) }
  var stopPendingPodDialog by remember { mutableStateOf<TransportStopItem?>(null) }

  var locallyCompletedShopIds by remember { mutableStateOf(setOf<String>()) }
  var localPodNotesByShopId by remember { mutableStateOf(mapOf<String, String>()) }
  var arrivedShopIds by remember { mutableStateOf(setOf<String>()) }
  var redirectedToStopId by remember { mutableStateOf<String?>(null) }

  var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
  var userLat by remember { mutableStateOf<Double?>(null) }
  var userLng by remember { mutableStateOf<Double?>(null) }
  var followUserLocation by rememberSaveable { mutableStateOf(false) }
  var recenterTrigger by remember { mutableIntStateOf(0) }
  var fitBoundsTrigger by remember { mutableIntStateOf(0) }
  var focusShopTrigger by remember { mutableIntStateOf(0) }
  var pendingNavigateFullscreenAfterPerm by remember { mutableStateOf(false) }

  var lastRoutedLat by remember { mutableStateOf<Double?>(null) }
  var lastRoutedLng by remember { mutableStateOf<Double?>(null) }
  var routeGeometry by remember { mutableStateOf<NavigationRouteResult?>(null) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { grants ->
    val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    hasPermission = granted || hasLocationPermission(context)
    if (hasPermission) {
      followUserLocation = true
      fetchCurrentDeviceLocation(
        context = context,
        highAccuracy = true,
        onSuccess = { lat, lng, _ ->
          userLat = lat
          userLng = lng
          recenterTrigger++
          if (pendingNavigateFullscreenAfterPerm) {
            pendingNavigateFullscreenAfterPerm = false
            isFullscreenNav = true
          }
        },
        onError = { msg ->
          onShowFeedback(msg)
          if (pendingNavigateFullscreenAfterPerm) {
            pendingNavigateFullscreenAfterPerm = false
            isFullscreenNav = true
          }
        },
      )
    } else {
      onShowFeedback("Location permission needed for live GPS turn-by-turn guidance")
      if (pendingNavigateFullscreenAfterPerm) {
        pendingNavigateFullscreenAfterPerm = false
        isFullscreenNav = true
      }
    }
  }

  LaunchedEffect(hasPermission) {
    if (hasPermission) {
      observeDeviceLocationFlow(context, intervalMs = 3500L).collect { point ->
        userLat = point.latitude
        userLng = point.longitude
      }
    }
  }

  val planState = remember(
    activeRoute,
    shops,
    orders,
    locallyCompletedShopIds,
    localPodNotesByShopId,
    arrivedShopIds,
    userLat,
    userLng,
    useShortestPath,
  ) {
    buildTransportRoutePlanState(
      route = activeRoute,
      allShops = shops,
      allOrders = orders,
      locallyCompletedShopIds = locallyCompletedShopIds,
      localPodNotesByShopId = localPodNotesByShopId,
      arrivedShopIds = arrivedShopIds,
      userLatitude = userLat,
      userLongitude = userLng,
      useShortestPath = useShortestPath,
    )
  }

  // Recompute sequential OSRM route whenever stops change or driver moves >= 35 meters
  val stopCoordsKey = remember(planState.upcomingStops) {
    planState.upcomingStops.joinToString(";") { "${it.shop.id}:${it.shop.latitude},${it.shop.longitude}" }
  }
  LaunchedEffect(stopCoordsKey, userLat, userLng) {
    val curLat = userLat
    val curLng = userLng
    val movedEnough = curLat == null || curLng == null ||
      lastRoutedLat == null || lastRoutedLng == null ||
      haversineDistanceMeters(lastRoutedLat!!, lastRoutedLng!!, curLat, curLng) >= 35.0

    if (movedEnough || routeGeometry == null) {
      lastRoutedLat = curLat
      lastRoutedLng = curLng
      routeGeometry = OsrmRoutingClient.computeSequentialRoute(
        waypoints = planState.orderedWaypointsForMap,
        hasUserOrigin = planState.hasUserOriginForRouting,
        primaryStopName = planState.activeTargetStop?.shortShopName ?: "Next Stop",
      )
    }
  }

  // Force recompute immediately when the active stop sequence changes even if GPS hasn't moved
  LaunchedEffect(stopCoordsKey) {
    routeGeometry = OsrmRoutingClient.computeSequentialRoute(
      waypoints = planState.orderedWaypointsForMap,
      hasUserOrigin = planState.hasUserOriginForRouting,
      primaryStopName = planState.activeTargetStop?.shortShopName ?: "Next Stop",
    )
  }

  val requestOrRecenterGps: (enterFullscreenAfter: Boolean) -> Unit = { enterFullscreenAfter ->
    if (enterFullscreenAfter) {
      isFullscreenNav = true
    }
    if (hasLocationPermission(context)) {
      hasPermission = true
      followUserLocation = true
      recenterTrigger++
      if (userLat == null) {
        fetchCurrentDeviceLocation(
          context = context,
          highAccuracy = true,
          onSuccess = { lat, lng, _ ->
            userLat = lat
            userLng = lng
            recenterTrigger++
          },
          onError = { msg -> onShowFeedback(msg) },
        )
      }
    } else {
      pendingNavigateFullscreenAfterPerm = enterFullscreenAfter
      permissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )
      )
    }
  }

  val confirmArrivedAction: (TransportStopItem) -> Unit = { stop ->
    if (!stop.isCompleted && !locallyCompletedShopIds.contains(stop.shop.id)) {
      arrivedShopIds = arrivedShopIds + stop.shop.id
      if (redirectedToStopId == stop.shop.id) {
        redirectedToStopId = null
      }
      onShowFeedback("Arrived at Stop ${stop.formattedStopNum} (${stop.shortShopName}). Complete delivery now.")
    }
  }

  val completeStopAction: (TransportStopItem, String) -> Unit = { stop, podNote ->
    val activeTarget = planState.activeTargetStop
    when {
      stop.isCompleted || locallyCompletedShopIds.contains(stop.shop.id) -> {
        stopPendingPodDialog = null
      }
      !stop.isActiveTarget || (activeTarget != null && activeTarget.shop.id != stop.shop.id) -> {
        stopPendingPodDialog = null
        val seqPrompt = if (activeTarget != null) {
          "Deliver in sequence: complete Stop ${activeTarget.formattedStopNum} (${activeTarget.shortShopName}) first."
        } else {
          "Deliveries must follow route stop sequence."
        }
        onShowFeedback(seqPrompt)
      }
      else -> {
        stopPendingPodDialog = null
        val nextTarget = planState.upcomingStops.firstOrNull { it.shop.id != stop.shop.id }
        localPodNotesByShopId = localPodNotesByShopId + (stop.shop.id to podNote)
        locallyCompletedShopIds = locallyCompletedShopIds + stop.shop.id
        val distinctOrders = stop.activeOrders.distinctBy { it.id.ifBlank { it.orderCode } }
        distinctOrders.forEach { order ->
          onMarkOrderDelivered(order.id, stop.shop.name, podNote)
        }
        if (nextTarget != null) {
          selectedPinShopId = nextTarget.shop.id
          redirectedToStopId = nextTarget.shop.id
          followUserLocation = false
          focusShopTrigger++
          onShowFeedback(
            "Stop ${stop.formattedStopNum} completed! Redirecting to Stop ${nextTarget.formattedStopNum}: ${nextTarget.shortShopName}"
          )
        } else {
          selectedPinShopId = null
          redirectedToStopId = null
          onShowFeedback("All ${planState.totalStops} deliveries completed!")
        }
      }
    }
  }

  val query = searchQuery.trim().lowercase(Locale.US)
  val filteredUpcoming = remember(planState.upcomingStops, query) {
    if (query.isBlank()) {
      planState.upcomingStops
    } else {
      planState.upcomingStops.filter { stop ->
        stop.shop.name.lowercase(Locale.US).contains(query) ||
          stop.shop.shopCode.lowercase(Locale.US).contains(query) ||
          stop.shop.address.lowercase(Locale.US).contains(query) ||
          stop.primaryOrderCode.orEmpty().lowercase(Locale.US).contains(query)
      }
    }
  }
  val filteredDelivered = remember(planState.deliveredStops, query) {
    if (query.isBlank()) {
      planState.deliveredStops
    } else {
      planState.deliveredStops.filter { stop ->
        stop.shop.name.lowercase(Locale.US).contains(query) ||
          stop.shop.shopCode.lowercase(Locale.US).contains(query) ||
          stop.shop.address.lowercase(Locale.US).contains(query) ||
          stop.primaryOrderCode.orEmpty().lowercase(Locale.US).contains(query)
      }
    }
  }

  BackHandler(enabled = isFullscreenNav) {
    isFullscreenNav = false
  }

  if (isFullscreenNav) {
    FullscreenNavigationOverlay(
      planState = planState,
      routeGeometry = routeGeometry,
      routes = routes,
      selectedPinShopId = selectedPinShopId,
      userLatitude = userLat,
      userLongitude = userLng,
      followUserLocation = followUserLocation,
      recenterTrigger = recenterTrigger,
      fitBoundsTrigger = fitBoundsTrigger,
      hasLocationPermission = hasPermission,
      focusShopTrigger = focusShopTrigger,
      onRecenterClick = { requestOrRecenterGps(false) },
      onFitRouteClick = {
        followUserLocation = false
        fitBoundsTrigger++
      },
      onExitFullscreen = { isFullscreenNav = false },
      onPinSelected = { shopId ->
        selectedPinShopId = shopId
        followUserLocation = false
      },
      onFocusPin = { shopId ->
        selectedPinShopId = shopId
        followUserLocation = false
        focusShopTrigger++
      },
      onCallShopClick = { shop -> openDialer(context, shop.phone) },
      onOpenExternalMapsClick = {
        launchExternalGoogleMapsNavigation(
          context = context,
          activeStop = planState.activeTargetStop,
          upcomingStops = planState.upcomingStops,
          userLatitude = userLat,
          userLongitude = userLng,
        )
      },
      onOpenStopExternalMapsClick = { focusedStop ->
        launchExternalGoogleMapsNavigation(
          context = context,
          activeStop = planState.activeTargetStop,
          upcomingStops = planState.upcomingStops,
          userLatitude = userLat,
          userLongitude = userLng,
          focusedStop = focusedStop,
        )
      },
      onMarkActiveDeliveredClick = {
        val active = planState.activeTargetStop
        if (active != null && !active.isCompleted && !locallyCompletedShopIds.contains(active.shop.id)) {
          if (active.isReadyToCompleteDelivery) {
            stopPendingPodDialog = active
          } else {
            confirmArrivedAction(active)
          }
        }
      },
      onConfirmArrivedClick = {
        val active = planState.activeTargetStop
        if (active != null && !active.isCompleted && !locallyCompletedShopIds.contains(active.shop.id)) {
          confirmArrivedAction(active)
        }
      },
      isRedirectingToNextStop = planState.activeTargetStop?.shop?.id != null &&
        redirectedToStopId == planState.activeTargetStop?.shop?.id,
      modifier = modifier,
    )
  } else {
    val partnerTitle = remember(userDisplayName) {
      val clean = userDisplayName.trim()
      when {
        clean.isBlank() || clean.contains("Local Tester", ignoreCase = true) -> "Delivery Partner"
        clean.length <= 18 -> clean
        else -> clean.substringBefore(" ").take(18)
      }
    }

    Box(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
    ) {
      LazyColumn(
        contentPadding = PaddingValues(top = 0.dp, bottom = 78.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
          .fillMaxSize()
          .testTag("transport_stop_list"),
      ) {
        item(key = "transport_top_bar") {
          TransportTopBar(
            partnerTitle = partnerTitle,
            hubSubtitle = "Kochi Central Hub",
            pendingCount = planState.pendingCount,
            showSearch = showSearch,
            searchQuery = searchQuery,
            onToggleSearch = { showSearch = !showSearch },
            onSearchQueryChange = { searchQuery = it },
            onNotificationsClick = {
              selectedFilter = TransportStopFilter.PENDING
              val next = planState.activeTargetStop
              if (next != null) {
                onShowFeedback("${planState.pendingCount} pending • Next: Stop ${next.formattedStopNum} (${next.shortShopName})")
              } else {
                onShowFeedback("All ${planState.totalStops} stops completed on ${activeRoute?.name ?: "route"}")
              }
            },
            onProfileSyncClick = onOpenSyncServers,
            showInlineSearchBar = false,
          )
        }

        item(key = "route_trigger") {
          Box(modifier = Modifier.padding(horizontal = 10.dp)) {
            ActiveRouteTriggerCard(
              route = activeRoute,
              stopCount = planState.totalStops,
              onClick = { showRouteSelectorSheet = true },
            )
          }
        }

        item(key = "vehicle_progress") {
          Box(modifier = Modifier.padding(horizontal = 10.dp)) {
            VehicleProgressCard(
              planState = planState,
              onEditVehicleClick = if (activeRoute != null) {
                { showEditVehicleSheet = true }
              } else {
                null
              },
            )
          }
        }

        stickyHeader(key = "transport_sticky_controls") {
          Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              TransportSearchBar(
                showSearch = showSearch,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
              )
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp, vertical = 4.dp),
              ) {
                StopFilterChipsRow(
                  selectedFilter = selectedFilter,
                  totalCount = planState.totalStops,
                  pendingCount = planState.pendingCount,
                  completedCount = planState.completedCount,
                  onSelectFilter = { selectedFilter = it },
                )
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
          }
        }

        item(key = "map_card") {
          Box(modifier = Modifier.padding(horizontal = 10.dp)) {
            TransportMapCard(
              planState = planState,
              routeGeometry = routeGeometry,
              routes = routes,
              selectedPinShopId = selectedPinShopId,
              userLatitude = userLat,
              userLongitude = userLng,
              followUserLocation = followUserLocation,
              recenterTrigger = recenterTrigger,
              fitBoundsTrigger = fitBoundsTrigger,
              hasLocationPermission = hasPermission,
              focusShopTrigger = focusShopTrigger,
              onRecenterUserClick = { requestOrRecenterGps(false) },
              onToggleFullscreenOrFitClick = { isFullscreenNav = true },
              onPinSelected = { shopId ->
                selectedPinShopId = shopId
                followUserLocation = false
                focusShopTrigger++
              },
              onDirectionsBannerClick = { showDirectionsSheet = true },
              onLaunchExternalMapsClick = {
                launchExternalGoogleMapsNavigation(
                  context = context,
                  activeStop = planState.activeTargetStop,
                  upcomingStops = planState.upcomingStops,
                  userLatitude = userLat,
                  userLongitude = userLng,
                )
              },
            )
          }
        }

        if (planState.totalStops == 0) {
          item(key = "empty_route_card") {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              shadowElevation = 2.dp,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.LocalShipping,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(34.dp),
                )
                Text(
                  text = "No active shops assigned to ${activeRoute?.name ?: "this route"}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Allocate shops and configure stop sequence (1, 2, 3...) in Route Planning.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                  onClick = onNavigateToRoutes,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                  Text("Open Route Planning")
                }
              }
            }
          }
        }

        if (selectedFilter != TransportStopFilter.COMPLETED && filteredUpcoming.isNotEmpty()) {
          item(key = "upcoming_header") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Shops to Deliver (${filteredUpcoming.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Surface(
                color = if (useShortestPath) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .clickable {
                    useShortestPath = !useShortestPath
                    selectedPinShopId = null
                    fitBoundsTrigger++
                  }
                  .testTag("transport_route_order_toggle"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.AltRoute,
                    contentDescription = "Toggle shortest path ordering",
                    tint = if (useShortestPath) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = if (useShortestPath) "Shortest Path" else "Manual Order",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (useShortestPath) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }

          items(
            items = filteredUpcoming,
            key = { "up_${it.shop.id}" },
            contentType = { "upcoming_stop" },
          ) { stop ->
            Box(modifier = Modifier.padding(horizontal = 10.dp)) {
              UpcomingStopCard(
                stop = stop,
                isSelected = selectedPinShopId == stop.shop.id,
                activeTargetStopNumber = planState.activeTargetStop?.stopNumber,
                onCardClick = {
                  selectedPinShopId = stop.shop.id
                  followUserLocation = false
                  focusShopTrigger++
                },
                onCallShop = { openDialer(context, stop.shop.phone) },
                onConfirmArrived = {
                  val activeTarget = planState.activeTargetStop
                  if (stop.isActiveTarget && activeTarget?.shop?.id == stop.shop.id) {
                    confirmArrivedAction(stop)
                  }
                },
                onMarkDelivered = {
                  val activeTarget = planState.activeTargetStop
                  if (
                    stop.isActiveTarget &&
                    activeTarget?.shop?.id == stop.shop.id &&
                    stop.isReadyToCompleteDelivery &&
                    !stop.isCompleted &&
                    !locallyCompletedShopIds.contains(stop.shop.id)
                  ) {
                    stopPendingPodDialog = stop
                  } else if (stop.isActiveTarget && !stop.isReadyToCompleteDelivery) {
                    confirmArrivedAction(stop)
                  } else if (activeTarget != null) {
                    selectedPinShopId = activeTarget.shop.id
                    onShowFeedback("Deliver in sequence: complete Stop ${activeTarget.formattedStopNum} (${activeTarget.shortShopName}) first.")
                  }
                },
                onFocusActiveStop = {
                  val activeTarget = planState.activeTargetStop
                  if (activeTarget != null) {
                    selectedPinShopId = activeTarget.shop.id
                    focusShopTrigger++
                    onShowFeedback("Deliver in sequence: Stop ${activeTarget.formattedStopNum} (${activeTarget.shortShopName}) is next.")
                  }
                },
              )
            }
          }
        }

        if (selectedFilter != TransportStopFilter.PENDING && filteredDelivered.isNotEmpty()) {
          item(key = "delivered_header", contentType = "section_header") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Delivered Today (${filteredDelivered.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Surface(
                color = MaterialTheme.semanticColors.successContainer,
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  text = "100% On-Time",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.semanticColors.success,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }
          }

          items(
            items = filteredDelivered,
            key = { "done_${it.shop.id}" },
            contentType = { "delivered_stop" },
          ) { stop ->
            Box(modifier = Modifier.padding(horizontal = 10.dp)) {
              DeliveredStopCard(
                stop = stop,
                onClick = {
                  selectedPinShopId = stop.shop.id
                  followUserLocation = false
                  focusShopTrigger++
                },
              )
            }
          }
        }

        if (planState.totalStops > 0 && (
          (selectedFilter == TransportStopFilter.PENDING && filteredUpcoming.isEmpty()) ||
            (selectedFilter == TransportStopFilter.COMPLETED && filteredDelivered.isEmpty())
          )
        ) {
          item(key = "filter_empty_card") {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              shadowElevation = 2.dp,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = if (selectedFilter == TransportStopFilter.PENDING) {
                    "All route stops are completed!"
                  } else {
                    "No stops delivered yet"
                  },
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = if (searchQuery.isNotBlank()) {
                    "No stops match \"$searchQuery\"."
                  } else if (selectedFilter == TransportStopFilter.PENDING) {
                    "Switch to Completed to review POD records."
                  } else {
                    "Mark Stop ${planState.activeTargetStop?.formattedStopNum ?: "01"} delivered once unloaded."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }

      TransportActionDock(
        activeStop = planState.activeTargetStop,
        onCallDispatchClick = {
          val phone = activeRoute?.driverPhone?.takeIf { it.isNotBlank() }
            ?: planState.activeTargetStop?.shop?.phone.orEmpty()
          openDialer(context, phone)
        },
        onNavigatePrimaryClick = {
          val target = planState.activeTargetStop
          selectedPinShopId = target?.shop?.id
          redirectedToStopId = target?.shop?.id
          focusShopTrigger++
          requestOrRecenterGps(true)
        },
        onConfirmArrivedClick = {
          val active = planState.activeTargetStop
          if (active != null && !active.isCompleted && !locallyCompletedShopIds.contains(active.shop.id)) {
            confirmArrivedAction(active)
          }
        },
        onCompleteStopClick = {
          val active = planState.activeTargetStop
          if (active != null && !active.isCompleted && !locallyCompletedShopIds.contains(active.shop.id)) {
            if (active.isReadyToCompleteDelivery) {
              stopPendingPodDialog = active
            } else {
              confirmArrivedAction(active)
            }
          }
        },
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }

  if (showRouteSelectorSheet) {
    SelectRouteBottomSheet(
      routes = routes,
      selectedRouteId = activeRoute?.id ?: selectedRouteId,
      allShops = shops,
      onSelectRoute = { route ->
        selectedRouteId = route.id
        selectedPinShopId = null
        showRouteSelectorSheet = false
        followUserLocation = false
        fitBoundsTrigger++
      },
      onManageRoutePlanning = {
        showRouteSelectorSheet = false
        onNavigateToRoutes()
      },
      onDismiss = { showRouteSelectorSheet = false },
    )
  }

  if (showDirectionsSheet) {
    TurnByTurnDirectionsSheet(
      planState = planState,
      routeGeometry = routeGeometry,
      onLaunchExternalMaps = {
        launchExternalGoogleMapsNavigation(
          context = context,
          activeStop = planState.activeTargetStop,
          upcomingStops = planState.upcomingStops,
          userLatitude = userLat,
          userLongitude = userLng,
        )
      },
      onConfirmArrived = {
        val active = planState.activeTargetStop
        if (active != null && !active.isCompleted && !locallyCompletedShopIds.contains(active.shop.id)) {
          confirmArrivedAction(active)
        }
      },
      onMarkActiveDelivered = {
        showDirectionsSheet = false
        val active = planState.activeTargetStop
        if (active != null && !active.isCompleted && !locallyCompletedShopIds.contains(active.shop.id)) {
          if (active.isReadyToCompleteDelivery) {
            stopPendingPodDialog = active
          } else {
            confirmArrivedAction(active)
          }
        }
      },
      onDismiss = { showDirectionsSheet = false },
    )
  }

  stopPendingPodDialog?.let { targetStop ->
    DeliveryPodDialog(
      stop = targetStop,
      onConfirmDelivered = { podNote -> completeStopAction(targetStop, podNote) },
      onDismiss = { stopPendingPodDialog = null },
    )
  }

  val editableRoute = activeRoute
  if (showEditVehicleSheet && editableRoute != null) {
    val usedColors = remember(routes, editableRoute) {
      routes.filter { it.id != editableRoute.id }.map { it.colorIndex }.toSet()
    }
    val assignedCrates = remember(shops, editableRoute) {
      shops.count { it.routeId == editableRoute.id }
    }
    EditRouteBottomSheet(
      initialRoute = editableRoute,
      usedColorIndices = usedColors,
      assignedCratesCount = assignedCrates,
      nextRouteNumber = routes.size + 1,
      onDismiss = { showEditVehicleSheet = false },
      onSaveRoute = { savedRoute ->
        onSaveRoute(savedRoute, "Updated vehicle details for ${savedRoute.name}")
        showEditVehicleSheet = false
      },
    )
  }
}
