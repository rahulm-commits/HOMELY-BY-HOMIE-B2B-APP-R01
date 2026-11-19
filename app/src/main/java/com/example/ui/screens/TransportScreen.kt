package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
  var showAllUpcoming by rememberSaveable { mutableStateOf(false) }
  var selectedPinShopId by rememberSaveable { mutableStateOf<String?>(null) }

  var showRouteSelectorSheet by remember { mutableStateOf(false) }
  var showEditVehicleSheet by remember { mutableStateOf(false) }
  var showDirectionsSheet by remember { mutableStateOf(false) }
  var isFullscreenNav by rememberSaveable { mutableStateOf(false) }
  var stopPendingPodDialog by remember { mutableStateOf<TransportStopItem?>(null) }

  var locallyCompletedShopIds by remember { mutableStateOf(setOf<String>()) }
  var localPodNotesByShopId by remember { mutableStateOf(mapOf<String, String>()) }

  var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
  var userLat by remember { mutableStateOf<Double?>(null) }
  var userLng by remember { mutableStateOf<Double?>(null) }
  var followUserLocation by rememberSaveable { mutableStateOf(false) }
  var recenterTrigger by remember { mutableIntStateOf(0) }
  var fitBoundsTrigger by remember { mutableIntStateOf(0) }
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
    userLat,
    userLng,
  ) {
    buildTransportRoutePlanState(
      route = activeRoute,
      allShops = shops,
      allOrders = orders,
      locallyCompletedShopIds = locallyCompletedShopIds,
      localPodNotesByShopId = localPodNotesByShopId,
      userLatitude = userLat,
      userLongitude = userLng,
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
      if (enterFullscreenAfter) {
        isFullscreenNav = true
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

  val completeStopAction: (TransportStopItem, String) -> Unit = { stop, podNote ->
    if (stop.isCompleted || locallyCompletedShopIds.contains(stop.shop.id)) {
      stopPendingPodDialog = null
    } else {
      stopPendingPodDialog = null
      selectedPinShopId = null
      localPodNotesByShopId = localPodNotesByShopId + (stop.shop.id to podNote)
      locallyCompletedShopIds = locallyCompletedShopIds + stop.shop.id
      val distinctOrders = stop.activeOrders.distinctBy { it.id.ifBlank { it.orderCode } }
      distinctOrders.forEach { order ->
        onMarkOrderDelivered(order.id, stop.shop.name, podNote)
      }
      if (distinctOrders.isEmpty()) {
        onShowFeedback("Stop ${stop.formattedStopNum} (${stop.shortShopName}) completed")
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
      onMarkActiveDeliveredClick = {
        val active = planState.activeTargetStop
        if (active != null && !active.isCompleted && !locallyCompletedShopIds.contains(active.shop.id)) {
          stopPendingPodDialog = active
        }
      },
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
      Column(modifier = Modifier.fillMaxSize()) {
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
        )

        LazyColumn(
          contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 84.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier
            .fillMaxSize()
            .testTag("transport_stop_list"),
        ) {
          item(key = "route_trigger") {
            ActiveRouteTriggerCard(
              route = activeRoute,
              stopCount = planState.totalStops,
              onClick = { showRouteSelectorSheet = true },
            )
          }

          item(key = "vehicle_progress") {
            VehicleProgressCard(
              planState = planState,
              onEditVehicleClick = if (activeRoute != null) {
                { showEditVehicleSheet = true }
              } else {
                null
              },
            )
          }

          item(key = "filter_chips") {
            StopFilterChipsRow(
              selectedFilter = selectedFilter,
              totalCount = planState.totalStops,
              pendingCount = planState.pendingCount,
              completedCount = planState.completedCount,
              onSelectFilter = { selectedFilter = it },
            )
          }

          item(key = "map_card") {
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
              onRecenterUserClick = { requestOrRecenterGps(false) },
              onToggleFullscreenOrFitClick = { isFullscreenNav = true },
              onPinSelected = { shopId ->
                selectedPinShopId = shopId
                followUserLocation = false
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

          if (planState.totalStops == 0) {
            item(key = "empty_route_card") {
              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
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
                  .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Upcoming Stops (${filteredUpcoming.size} Remaining)",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Auto-sequenced",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }

            val visibleUpcoming = if (!showAllUpcoming && query.isBlank() && filteredUpcoming.size > 3) {
              filteredUpcoming.take(3)
            } else {
              filteredUpcoming
            }

            items(visibleUpcoming, key = { "up_${it.shop.id}" }) { stop ->
              UpcomingStopCard(
                stop = stop,
                isSelected = selectedPinShopId == stop.shop.id,
                onCardClick = {
                  selectedPinShopId = stop.shop.id
                  followUserLocation = false
                },
                onCallShop = { openDialer(context, stop.shop.phone) },
                onMarkDelivered = {
                  if (!stop.isCompleted && !locallyCompletedShopIds.contains(stop.shop.id)) {
                    stopPendingPodDialog = stop
                  }
                },
              )
            }

            if (query.isBlank() && filteredUpcoming.size > 3) {
              item(key = "expand_upcoming_toggle") {
                val hiddenCount = filteredUpcoming.size - 3
                val finalDest = filteredUpcoming.lastOrNull()?.shortShopName ?: "Destination"
                Surface(
                  color = MaterialTheme.colorScheme.surface,
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                  shadowElevation = 2.dp,
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showAllUpcoming = !showAllUpcoming }
                    .testTag("transport_expand_upcoming_btn"),
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Rounded.AltRoute,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                      )
                      Text(
                        text = if (showAllUpcoming) {
                          "Show fewer upcoming stops"
                        } else {
                          "+ $hiddenCount more stops scheduled towards $finalDest"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Icon(
                      imageVector = if (showAllUpcoming) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                      contentDescription = "Toggle upcoming stops",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
              }
            }
          }

          if (selectedFilter != TransportStopFilter.PENDING && filteredDelivered.isNotEmpty()) {
            item(key = "delivered_header") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 4.dp),
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

            items(filteredDelivered, key = { "done_${it.shop.id}" }) { stop ->
              DeliveredStopCard(
                stop = stop,
                onClick = {
                  selectedPinShopId = stop.shop.id
                  followUserLocation = false
                },
              )
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
                modifier = Modifier.fillMaxWidth(),
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

          item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(6.dp))
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
        onNavigatePrimaryClick = { requestOrRecenterGps(true) },
        onCompleteStopClick = {
          val active = planState.activeTargetStop
          if (active != null && !active.isCompleted && !locallyCompletedShopIds.contains(active.shop.id)) {
            stopPendingPodDialog = active
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
        showAllUpcoming = false
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
      onMarkActiveDelivered = {
        showDirectionsSheet = false
        val active = planState.activeTargetStop
        if (active != null && !active.isCompleted && !locallyCompletedShopIds.contains(active.shop.id)) {
          stopPendingPodDialog = active
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
