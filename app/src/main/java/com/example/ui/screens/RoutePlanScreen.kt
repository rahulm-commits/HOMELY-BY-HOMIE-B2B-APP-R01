package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.example.ui.components.StandardCompactSearchBar
import com.example.ui.components.fetchCurrentDeviceLocation
import com.example.ui.components.hasLocationPermission
import kotlinx.coroutines.delay

@Composable
fun RoutePlanScreen(
  shops: List<Shop>,
  routes: List<DeliveryRoute>,
  onSaveRoute: (DeliveryRoute, String) -> Unit,
  onUpdateShopRoute: (shopId: String, newRouteId: String, feedbackMsg: String) -> Unit,
  onBulkAssignShopsToRoute: (shopIds: List<String>, targetRouteId: String, feedbackMsg: String) -> Unit,
  onOpenSyncServers: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activeShops = remember(shops) { shops.filter { it.status == ShopStatus.ACTIVE } }

  var selectedRouteId by rememberSaveable { mutableStateOf(RoutesCatalog.ALL) }
  var selectedShopId by rememberSaveable { mutableStateOf<String?>(null) }
  var showPolylines by rememberSaveable { mutableStateOf(true) }
  var isFullscreenMap by rememberSaveable { mutableStateOf(false) }
  var isDrawerExpanded by rememberSaveable { mutableStateOf(false) }
  var activeDrawerTab by rememberSaveable { mutableStateOf(DispatchDrawerTab.ROUTES) }
  var showTopSearch by rememberSaveable { mutableStateOf(false) }
  var topSearchQuery by rememberSaveable { mutableStateOf("") }
  var checkedShopIds by remember { mutableStateOf(setOf<String>()) }

  var userLatitude by remember { mutableStateOf<Double?>(null) }
  var userLongitude by remember { mutableStateOf<Double?>(null) }
  var isFetchingLocation by remember { mutableStateOf(false) }

  var recentStatusChipText by remember { mutableStateOf<String?>(null) }
  var routeBeingEdited by remember { mutableStateOf<DeliveryRoute?>(null) }
  var showRouteModal by remember { mutableStateOf(false) }
  var routeManagingStops by remember { mutableStateOf<DeliveryRoute?>(null) }

  val readActualGps: () -> Unit = {
    isFetchingLocation = true
    fetchCurrentDeviceLocation(
      context = context,
      highAccuracy = true,
      onSuccess = { lat, lng, _ ->
        isFetchingLocation = false
        userLatitude = lat
        userLongitude = lng
      },
      onError = { msg ->
        isFetchingLocation = false
        recentStatusChipText = msg.take(26)
      },
    )
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      readActualGps()
    } else {
      recentStatusChipText = "Location permission needed"
    }
  }

  val requestActualLocation: () -> Unit = {
    if (hasLocationPermission(context)) {
      readActualGps()
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )
      )
    }
  }

  // Automatically fetch live device GPS when permission is already granted
  LaunchedEffect(Unit) {
    if (hasLocationPermission(context)) {
      readActualGps()
    }
  }

  LaunchedEffect(recentStatusChipText) {
    if (recentStatusChipText != null) {
      delay(2800L)
      recentStatusChipText = null
    }
  }

  BackHandler(enabled = isFullscreenMap) {
    isFullscreenMap = false
  }

  val displayedShops = remember(activeShops, topSearchQuery) {
    val q = topSearchQuery.trim().lowercase()
    if (q.isEmpty()) {
      activeShops
    } else {
      activeShops.filter { s ->
        s.name.lowercase().contains(q) ||
          s.shopCode.lowercase().contains(q) ||
          s.address.lowercase().contains(q) ||
          s.ownerName.lowercase().contains(q) ||
          s.phone.lowercase().contains(q)
      }
    }
  }

  val selectedShop = remember(displayedShops, selectedShopId) {
    displayedShops.firstOrNull { it.id == selectedShopId }
  }

  val unallocatedCount = remember(activeShops) {
    activeShops.count { it.routeId == RoutesCatalog.UNALLOCATED || it.routeId.isBlank() }
  }

  if (isFullscreenMap) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
    ) {
      InteractiveRouteMapViewport(
        shops = displayedShops,
        routes = routes,
        selectedRouteId = selectedRouteId,
        selectedShop = selectedShop,
        showPolylines = showPolylines,
        isFullscreen = true,
        userLatitude = userLatitude,
        userLongitude = userLongitude,
        isFetchingLocation = isFetchingLocation,
        recentStatusChipText = recentStatusChipText,
        onSelectRouteFilter = { routeId ->
          selectedRouteId = routeId
          if (routeId != RoutesCatalog.ALL && selectedShop?.routeId != routeId) {
            selectedShopId = displayedShops.firstOrNull { it.routeId == routeId }?.id
          }
        },
        onCreateNewRouteClick = {
          routeBeingEdited = null
          showRouteModal = true
        },
        onTogglePolylines = { showPolylines = !showPolylines },
        onToggleFullscreen = { isFullscreenMap = false },
        onRequestActualLocation = requestActualLocation,
        onSelectShop = { shop -> selectedShopId = shop?.id },
        onReassignShopRoute = { shop, targetRouteId ->
          val targetName = routes.firstOrNull { it.id == targetRouteId }?.name ?: "Unallocated"
          onUpdateShopRoute(shop.id, targetRouteId, "${shop.name} moved to $targetName")
          recentStatusChipText = "Moved to ${targetName.substringAfter(":").trim().take(18)}"
        },
        onEditSelectedRouteShopsClick = routes.firstOrNull { it.id == selectedRouteId }?.let { focusedRoute ->
          { routeManagingStops = focusedRoute }
        },
        modifier = Modifier.fillMaxSize(),
      )
    }
  } else {
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background),
    ) {
      // Compact Header unified with OrderScreen & ShopListScreen
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Text(
                text = "Route Planning",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  text = "${routes.size} routes • ${activeShops.size} shops",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                color = if (showTopSearch) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = CircleShape,
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .clickable {
                    showTopSearch = !showTopSearch
                    if (!showTopSearch) topSearchQuery = ""
                  }
                  .testTag("route_top_search_button"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search Routes and Stores",
                    tint = if (showTopSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp),
                  )
                }
              }

              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .clickable {
                    activeDrawerTab = DispatchDrawerTab.UNALLOCATED
                    isDrawerExpanded = true
                  }
                  .testTag("route_notifications_button"),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Notifications,
                  contentDescription = "Pending route allocations",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(21.dp),
                )
                if (unallocatedCount > 0) {
                  Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .offset(x = (-3).dp, y = 4.dp)
                      .size(16.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = unallocatedCount.coerceAtMost(99).toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError,
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.width(2.dp))
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .clickable(onClick = onOpenSyncServers)
                  .testTag("route_profile_sync_button"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "Sync and Server Settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp),
                  )
                }
              }
            }
          }

          AnimatedVisibility(visible = showTopSearch) {
            StandardCompactSearchBar(
              query = topSearchQuery,
              onQueryChange = { topSearchQuery = it },
              placeholder = "Filter map pins & routes by shop, code or city...",
              fieldTestTag = "route_top_search_input",
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        }
      }

      // Main Scrollable Body: Real Map Viewport + Route Dispatch Drawer
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState()),
      ) {
        val mapHeight = if (isDrawerExpanded) 340.dp else 440.dp
        InteractiveRouteMapViewport(
          shops = displayedShops,
          routes = routes,
          selectedRouteId = selectedRouteId,
          selectedShop = selectedShop,
          showPolylines = showPolylines,
          isFullscreen = false,
          userLatitude = userLatitude,
          userLongitude = userLongitude,
          isFetchingLocation = isFetchingLocation,
          recentStatusChipText = recentStatusChipText,
          onSelectRouteFilter = { routeId ->
            selectedRouteId = routeId
            if (routeId != RoutesCatalog.ALL && selectedShop?.routeId != routeId) {
              selectedShopId = displayedShops.firstOrNull { it.routeId == routeId }?.id
            }
          },
          onCreateNewRouteClick = {
            routeBeingEdited = null
            showRouteModal = true
          },
          onTogglePolylines = { showPolylines = !showPolylines },
          onToggleFullscreen = { isFullscreenMap = true },
          onRequestActualLocation = requestActualLocation,
          onSelectShop = { shop -> selectedShopId = shop?.id },
          onReassignShopRoute = { shop, targetRouteId ->
            val targetName = routes.firstOrNull { it.id == targetRouteId }?.name ?: "Unallocated"
            onUpdateShopRoute(shop.id, targetRouteId, "${shop.name} moved to $targetName")
            recentStatusChipText = "Moved to ${targetName.substringAfter(":").trim().take(18)}"
          },
          onEditSelectedRouteShopsClick = routes.firstOrNull { it.id == selectedRouteId }?.let { focusedRoute ->
            { routeManagingStops = focusedRoute }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(mapHeight),
        )

        AllocationDispatchDrawer(
          shops = displayedShops,
          routes = routes,
          selectedRouteId = selectedRouteId,
          activeTab = activeDrawerTab,
          isExpanded = isDrawerExpanded,
          checkedShopIds = checkedShopIds,
          onToggleExpand = { isDrawerExpanded = !isDrawerExpanded },
          onSelectTab = { activeDrawerTab = it },
          onSelectRoute = { routeId ->
            selectedRouteId = if (selectedRouteId == routeId) RoutesCatalog.ALL else routeId
            if (selectedRouteId != RoutesCatalog.ALL) {
              selectedShopId = displayedShops.firstOrNull { it.routeId == selectedRouteId }?.id
            }
          },
          onClearRouteFilter = { selectedRouteId = RoutesCatalog.ALL },
          onToggleShopChecked = { shopId ->
            checkedShopIds = if (shopId in checkedShopIds) {
              checkedShopIds - shopId
            } else {
              checkedShopIds + shopId
            }
          },
          onFocusShopOnMap = { shop ->
            selectedShopId = shop.id
          },
          onEditRoute = { route ->
            routeBeingEdited = route
            showRouteModal = true
          },
          onCreateNewRoute = {
            routeBeingEdited = null
            showRouteModal = true
          },
          onEditSelectedRouteShops = { route ->
            routeManagingStops = route
          },
          onRemoveShopFromRoute = { shop ->
            onUpdateShopRoute(shop.id, RoutesCatalog.UNALLOCATED, "${shop.name} removed from route")
            recentStatusChipText = "Unallocated ${shop.shopCode}"
          },
          onAutoGroupUnallocated = { unallocatedList ->
            if (routes.isNotEmpty() && unallocatedList.isNotEmpty()) {
              unallocatedList.forEachIndexed { idx, shop ->
                val targetRoute = routes[idx % routes.size]
                onUpdateShopRoute(
                  shop.id,
                  targetRoute.id,
                  "Auto-grouped ${unallocatedList.size} shops across ${routes.size} routes",
                )
              }
              recentStatusChipText = "Auto-grouped ${unallocatedList.size} stops"
              activeDrawerTab = DispatchDrawerTab.ROUTES
            }
          },
          onMoveShopStopOrder = { route, newStopOrderIds ->
            onSaveRoute(route.copy(stopOrderIds = newStopOrderIds), "Updated ${route.name} stop order")
            recentStatusChipText = "Stop Order Saved"
          },
          modifier = Modifier.offset(y = (-14).dp),
        )
      }
    }
  }

  // Edit / Create Route Modal Sheet
  if (showRouteModal) {
    val editing = routeBeingEdited
    val usedColors = remember(routes, editing) {
      routes.filter { it.id != editing?.id }.map { it.colorIndex }.toSet()
    }
    val assignedCrates = remember(activeShops, editing) {
      if (editing == null) 0 else activeShops.count { it.routeId == editing.id }
    }
    EditRouteBottomSheet(
      initialRoute = editing,
      usedColorIndices = usedColors,
      assignedCratesCount = assignedCrates,
      nextRouteNumber = routes.size + 1,
      onDismiss = { showRouteModal = false },
      onSaveRoute = { savedRoute ->
        val actionMsg = if (editing == null) {
          "Created ${savedRoute.name}"
        } else {
          "Saved ${savedRoute.name} & driver details"
        }
        onSaveRoute(savedRoute, actionMsg)
        recentStatusChipText = "Route & Driver Updated"
        showRouteModal = false
      },
    )
  }

  // Manage Shops inside a Selected Route
  routeManagingStops?.let { targetRoute ->
    EditRouteShopsSheet(
      route = targetRoute,
      allShops = activeShops,
      onDismiss = { routeManagingStops = null },
      onApplyChanges = { toAssignIds, toUnassignIds ->
        if (toAssignIds.isNotEmpty()) {
          onBulkAssignShopsToRoute(
            toAssignIds,
            targetRoute.id,
            "Assigned ${toAssignIds.size} shop(s) to ${targetRoute.name}",
          )
        }
        if (toUnassignIds.isNotEmpty()) {
          onBulkAssignShopsToRoute(
            toUnassignIds,
            RoutesCatalog.UNALLOCATED,
            "Updated ${targetRoute.name} stops",
          )
        }
        if (toAssignIds.isNotEmpty() || toUnassignIds.isNotEmpty()) {
          val retainedSorted = RoutesCatalog.sortShopsByRouteSequence(
            activeShops.filter { it.routeId == targetRoute.id && it.id !in toUnassignIds },
            targetRoute,
          ).map { it.id }
          val updatedStopOrder = (retainedSorted + toAssignIds.filter { it !in retainedSorted })
          onSaveRoute(
            targetRoute.copy(stopOrderIds = updatedStopOrder),
            "Updated ${targetRoute.name} stops",
          )
          recentStatusChipText = "${targetRoute.name.substringAfter(":").trim().take(14)} Updated"
        }
        routeManagingStops = null
      },
    )
  }
}
