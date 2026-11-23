package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.ui.components.CompactDropdownMenuItem
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.formatCoordinatePair
import com.example.ui.components.openDialer
import com.example.ui.theme.RouteColorDot
import com.example.ui.theme.RouteColorPalette
import com.example.ui.theme.RouteColorSpec
import com.example.ui.theme.semanticColors
import kotlinx.coroutines.delay

data class MapEditSnapshot(
  val shopRoutes: Map<String, String>,
  val routeStopOrders: Map<String, List<String>>,
  val actionDescription: String,
)

data class MapRouteEditorDraft(
  val targetRouteId: String,
  val initialShopRoutes: Map<String, String>,
  val draftShopRoutes: Map<String, String>,
  val draftRouteStopOrders: Map<String, List<String>>,
  val history: List<MapEditSnapshot> = emptyList(),
) {
  val canUndo: Boolean get() = history.isNotEmpty()

  val pendingChangeCount: Int
    get() = draftShopRoutes.count { (shopId, routeId) ->
      initialShopRoutes[shopId] != routeId
    }

  fun stopCountForRoute(routeId: String): Int =
    draftShopRoutes.values.count { it == routeId }

  fun changedShopsByTargetRoute(): Map<String, List<String>> =
    draftShopRoutes.entries
      .filter { (shopId, newRouteId) -> initialShopRoutes[shopId] != newRouteId }
      .groupBy(keySelector = { it.value }, valueTransform = { it.key })

  fun affectedRouteIds(): Set<String> {
    val changed = mutableSetOf<String>()
    draftShopRoutes.forEach { (shopId, newRouteId) ->
      val oldRouteId = initialShopRoutes[shopId]
      if (oldRouteId != newRouteId) {
        if (!oldRouteId.isNullOrBlank() && oldRouteId != RoutesCatalog.UNALLOCATED) {
          changed += oldRouteId
        }
        if (newRouteId.isNotBlank() && newRouteId != RoutesCatalog.UNALLOCATED) {
          changed += newRouteId
        }
      }
    }
    return changed
  }
}

fun createInitialMapRouteEditorDraft(
  shops: List<Shop>,
  routes: List<DeliveryRoute>,
  initialTargetRouteId: String,
): MapRouteEditorDraft {
  val resolvedTarget = if (routes.any { it.id == initialTargetRouteId }) {
    initialTargetRouteId
  } else {
    routes.firstOrNull()?.id.orEmpty()
  }
  val initialShopMap = shops.associate { it.id to it.routeId }
  val initialStopOrders = routes.associate { route ->
    val sorted = RoutesCatalog.sortShopsByRouteSequence(
      shops.filter { it.routeId == route.id },
      route,
    ).map { it.id }
    route.id to sorted
  }
  return MapRouteEditorDraft(
    targetRouteId = resolvedTarget,
    initialShopRoutes = initialShopMap,
    draftShopRoutes = initialShopMap,
    draftRouteStopOrders = initialStopOrders,
    history = emptyList(),
  )
}

fun MapRouteEditorDraft.withTargetRoute(routeId: String): MapRouteEditorDraft =
  copy(targetRouteId = routeId)

fun MapRouteEditorDraft.toggleShopSelection(shop: Shop): MapRouteEditorDraft {
  if (targetRouteId.isBlank()) return this
  val snapshot = MapEditSnapshot(
    shopRoutes = draftShopRoutes,
    routeStopOrders = draftRouteStopOrders,
    actionDescription = "Before tapping ${shop.name}",
  )
  val currentRouteId = draftShopRoutes[shop.id] ?: shop.routeId
  val mutableRoutes = draftShopRoutes.toMutableMap()
  val mutableOrders = draftRouteStopOrders.mapValues { it.value.toMutableList() }.toMutableMap()

  if (currentRouteId == targetRouteId) {
    // Deselect from target route
    mutableRoutes[shop.id] = RoutesCatalog.UNALLOCATED
    mutableOrders[targetRouteId]?.remove(shop.id)
  } else {
    // Select for target route (removing from any prior route)
    if (currentRouteId.isNotBlank() && currentRouteId != RoutesCatalog.UNALLOCATED) {
      mutableOrders[currentRouteId]?.remove(shop.id)
    }
    mutableRoutes[shop.id] = targetRouteId
    val targetList = mutableOrders.getOrPut(targetRouteId) { mutableListOf() }
    if (shop.id !in targetList) {
      targetList.add(shop.id)
    }
  }

  return copy(
    draftShopRoutes = mutableRoutes,
    draftRouteStopOrders = mutableOrders.mapValues { it.value.toList() },
    history = history + snapshot,
  )
}

fun MapRouteEditorDraft.undoLastChange(): MapRouteEditorDraft {
  val last = history.lastOrNull() ?: return this
  return copy(
    draftShopRoutes = last.shopRoutes,
    draftRouteStopOrders = last.routeStopOrders,
    history = history.dropLast(1),
  )
}

@Composable
fun InteractiveRouteMapViewport(
  shops: List<Shop>,
  routes: List<DeliveryRoute>,
  selectedRouteId: String,
  selectedShop: Shop?,
  showPolylines: Boolean,
  isFullscreen: Boolean,
  userLatitude: Double?,
  userLongitude: Double?,
  isFetchingLocation: Boolean,
  recentStatusChipText: String?,
  onSelectRouteFilter: (String) -> Unit,
  onCreateNewRouteClick: () -> Unit,
  onTogglePolylines: () -> Unit,
  onToggleFullscreen: () -> Unit,
  onRequestActualLocation: () -> Unit,
  onSelectShop: (Shop?) -> Unit,
  onReassignShopRoute: (Shop, String) -> Unit,
  onEditSelectedRouteShopsClick: (() -> Unit)? = null,
  editDraft: MapRouteEditorDraft? = null,
  onEnterMapEditMode: () -> Unit = {},
  onSelectEditTargetRoute: (String) -> Unit = {},
  onToggleShopInEditMode: (Shop) -> Unit = {},
  onUndoMapEdit: () -> Unit = {},
  onCancelMapEdit: () -> Unit = {},
  onSaveMapEdit: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  var routeDropdownExpanded by remember { mutableStateOf(false) }
  var editTargetDropdownExpanded by remember { mutableStateOf(false) }
  var recenterUserTrigger by remember { mutableIntStateOf(0) }
  var fitBoundsTrigger by remember { mutableIntStateOf(0) }
  var mapTapEditHighlight by remember { mutableStateOf(false) }

  LaunchedEffect(mapTapEditHighlight) {
    if (mapTapEditHighlight) {
      delay(3000L)
      mapTapEditHighlight = false
    }
  }

  val isEditMode = editDraft != null

  val previewShops = remember(shops, editDraft) {
    if (editDraft == null) {
      shops
    } else {
      shops.map { shop ->
        val draftedRouteId = editDraft.draftShopRoutes[shop.id]
        if (draftedRouteId != null && draftedRouteId != shop.routeId) {
          shop.copy(routeId = draftedRouteId)
        } else {
          shop
        }
      }
    }
  }

  val previewRoutes = remember(routes, editDraft) {
    if (editDraft == null) {
      routes
    } else {
      routes.map { route ->
        val draftedStopOrder = editDraft.draftRouteStopOrders[route.id]
        if (draftedStopOrder != null && draftedStopOrder != route.stopOrderIds) {
          route.copy(stopOrderIds = draftedStopOrder)
        } else {
          route
        }
      }
    }
  }

  val routeMap = remember(previewRoutes) { previewRoutes.associateBy { it.id } }
  val selectedRoute = routeMap[selectedRouteId]
  val selectedColorSpec = selectedRoute?.let { RouteColorPalette.specForIndex(it.colorIndex) }
  val selectedShopCount = remember(previewShops, selectedRouteId) {
    if (selectedRouteId == RoutesCatalog.ALL) previewShops.size else previewShops.count { it.routeId == selectedRouteId }
  }
  val editingTargetRoute = editDraft?.let { routeMap[it.targetRouteId] } ?: previewRoutes.firstOrNull()
  val editingTargetColorSpec = editingTargetRoute?.let { RouteColorPalette.specForIndex(it.colorIndex) }
  val hasLiveGps = userLatitude != null && userLongitude != null &&
    (userLatitude != 0.0 || userLongitude != 0.0)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceVariant),
  ) {
    // Real Interactive OpenStreetMap Viewport with live GPS + real shop coordinates
    RealOsmMapView(
      shops = previewShops,
      routes = previewRoutes,
      selectedRouteId = selectedRouteId,
      selectedShopId = if (isEditMode) null else selectedShop?.id,
      showPolylines = showPolylines,
      userLatitude = userLatitude,
      userLongitude = userLongitude,
      recenterUserTrigger = recenterUserTrigger,
      fitBoundsTrigger = fitBoundsTrigger,
      editingTargetRouteId = editDraft?.targetRouteId,
      onPinSelected = { shopId ->
        val tapped = previewShops.firstOrNull { it.id == shopId }
        if (isEditMode && tapped != null) {
          onToggleShopInEditMode(tapped)
        } else {
          mapTapEditHighlight = false
          onSelectShop(tapped)
        }
      },
      onMapSurfaceTapped = {
        if (!isEditMode) {
          if (selectedShop != null) {
            onSelectShop(null)
          } else {
            mapTapEditHighlight = true
          }
        }
      },
      modifier = Modifier.fillMaxSize(),
    )

    // Top Floating Controls Layer
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
              MaterialTheme.colorScheme.background.copy(alpha = 0.62f),
              Color.Transparent,
            )
          )
        )
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      if (editDraft != null) {
        // EDIT MODE TOP HEADER: Choose Target Route + New Route + Fit Bounds + Fullscreen
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = Modifier.weight(1f)) {
            val targetStopCount = editingTargetRoute?.let { editDraft.stopCountForRoute(it.id) } ?: 0
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(12.dp),
              shadowElevation = 2.dp,
              border = BorderStroke(
                1.5.dp,
                editingTargetColorSpec?.primary ?: MaterialTheme.colorScheme.primary,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { editTargetDropdownExpanded = true }
                .testTag("map_edit_target_route_dropdown"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  if (editingTargetColorSpec != null) {
                    RouteColorDot(spec = editingTargetColorSpec, size = 12.dp)
                  }
                  Text(
                    text = "Edit: ${editingTargetRoute?.name ?: "Select Route"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                  )
                  Surface(
                    color = (editingTargetColorSpec?.primary ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp),
                  ) {
                    Text(
                      text = "$targetStopCount stops",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = editingTargetColorSpec?.primary ?: MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                  }
                }
                Icon(
                  imageVector = Icons.Rounded.ArrowDropDown,
                  contentDescription = "Choose route to update",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            DropdownMenu(
              expanded = editTargetDropdownExpanded,
              onDismissRequest = { editTargetDropdownExpanded = false },
              modifier = Modifier
                .widthIn(min = 210.dp, max = 280.dp)
                .heightIn(max = 260.dp),
            ) {
              previewRoutes.forEach { route ->
                val isSelected = editDraft.targetRouteId == route.id
                val count = editDraft.stopCountForRoute(route.id)
                val spec = RouteColorPalette.specForIndex(route.colorIndex)
                CompactDropdownMenuItem(
                  text = "${route.name} ($count stops)",
                  selected = isSelected,
                  leadingIcon = { RouteColorDot(spec = spec, size = 12.dp) },
                  trailingIcon = if (isSelected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, tint = spec.primary, modifier = Modifier.size(15.dp)) }
                  } else null,
                  onClick = {
                    editTargetDropdownExpanded = false
                    onSelectEditTargetRoute(route.id)
                  },
                )
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
              CompactDropdownMenuItem(
                text = "Create New Route",
                textColor = MaterialTheme.colorScheme.primary,
                leadingIcon = {
                  Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                },
                onClick = {
                  editTargetDropdownExpanded = false
                  onCreateNewRouteClick()
                },
              )
            }
          }

          MapUtilityCircleButton(
            icon = Icons.Rounded.FitScreen,
            contentDescription = "Fit all shops on map",
            active = false,
            onClick = { fitBoundsTrigger++ },
            modifier = Modifier.testTag("map_fit_bounds_button"),
          )
          MapUtilityCircleButton(
            icon = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            contentDescription = if (isFullscreen) "Exit Full Screen" else "Full Screen View",
            active = isFullscreen,
            onClick = onToggleFullscreen,
            modifier = Modifier.testTag("map_fullscreen_button"),
          )
        }

        // EDIT MODE ROUTE PILLS: Quick 1-tap route selection while keeping all shops visible
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("map_edit_route_pills_row"),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          previewRoutes.forEachIndexed { idx, route ->
            val isTarget = editDraft.targetRouteId == route.id
            val spec = RouteColorPalette.specForIndex(route.colorIndex)
            val count = editDraft.stopCountForRoute(route.id)
            val cleanRouteName = route.name.substringAfter(":").trim().ifBlank { route.name }
            Surface(
              color = if (isTarget) spec.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
              shape = RoundedCornerShape(999.dp),
              border = BorderStroke(
                width = if (isTarget) 1.5.dp else 1.dp,
                color = if (isTarget && spec.secondary != null) spec.secondary else spec.primary.copy(alpha = 0.65f),
              ),
              shadowElevation = if (isTarget) 3.dp else 1.dp,
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { onSelectEditTargetRoute(route.id) }
                .testTag("map_edit_route_pill_${route.id}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                RouteColorDot(spec = spec, size = 9.dp)
                Text(
                  text = "R${idx + 1} $cleanRouteName ($count)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isTarget) FontWeight.Bold else FontWeight.SemiBold,
                  color = if (isTarget) spec.contrastInk else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                )
              }
            }
          }
        }
      } else {
        // NORMAL MODE TOP ROW 1: Route Filter Dropdown + Create Route "+" Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = Modifier.weight(1f)) {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(12.dp),
              shadowElevation = 2.dp,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
              modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { routeDropdownExpanded = true }
                .testTag("route_map_selector_dropdown"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  if (selectedColorSpec != null) {
                    RouteColorDot(spec = selectedColorSpec, size = 12.dp)
                  } else {
                    Box(
                      modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    )
                  }
                  Text(
                    text = selectedRoute?.name ?: "All Shops",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                  )
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(999.dp),
                  ) {
                    Text(
                      text = "$selectedShopCount shops",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                  }
                }
                Icon(
                  imageVector = Icons.Rounded.ArrowDropDown,
                  contentDescription = "Select route",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            DropdownMenu(
              expanded = routeDropdownExpanded,
              onDismissRequest = { routeDropdownExpanded = false },
              modifier = Modifier
                .widthIn(min = 210.dp, max = 280.dp)
                .heightIn(max = 260.dp),
            ) {
              val allSelected = selectedRouteId == RoutesCatalog.ALL
              CompactDropdownMenuItem(
                text = "All Shops (${previewShops.size} shops)",
                selected = allSelected,
                leadingIcon = {
                  Box(
                    modifier = Modifier
                      .size(11.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary),
                  )
                },
                onClick = {
                  routeDropdownExpanded = false
                  onSelectRouteFilter(RoutesCatalog.ALL)
                },
              )
              previewRoutes.forEach { route ->
                val isSelected = selectedRouteId == route.id
                val count = previewShops.count { it.routeId == route.id }
                val spec = RouteColorPalette.specForIndex(route.colorIndex)
                CompactDropdownMenuItem(
                  text = "${route.name} ($count shops)",
                  selected = isSelected,
                  leadingIcon = { RouteColorDot(spec = spec, size = 12.dp) },
                  trailingIcon = if (isSelected) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, tint = spec.primary, modifier = Modifier.size(15.dp)) }
                  } else null,
                  onClick = {
                    routeDropdownExpanded = false
                    onSelectRouteFilter(route.id)
                  },
                )
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
              CompactDropdownMenuItem(
                text = "Create New Route",
                textColor = MaterialTheme.colorScheme.primary,
                leadingIcon = {
                  Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                },
                onClick = {
                  routeDropdownExpanded = false
                  onCreateNewRouteClick()
                },
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable(onClick = onCreateNewRouteClick)
              .testTag("map_add_route_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Create New Delivery Route",
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }

        // NORMAL MODE TOP ROW 2: Live GPS + On-Map Edit Button + Status + Map Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
          ) {
            Surface(
              color = if (hasLiveGps) MaterialTheme.semanticColors.successContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
              shape = RoundedCornerShape(999.dp),
              border = BorderStroke(
                1.dp,
                if (hasLiveGps) MaterialTheme.semanticColors.success.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
              ),
              shadowElevation = 1.dp,
              modifier = Modifier
                .weight(1f, fill = false)
                .height(34.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable {
                  onRequestActualLocation()
                  if (hasLiveGps) recenterUserTrigger++
                },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                      when {
                        isFetchingLocation -> MaterialTheme.colorScheme.primary
                        hasLiveGps -> MaterialTheme.semanticColors.info
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                      }
                    ),
                )
                Text(
                  text = when {
                    isFetchingLocation -> "GPS..."
                    hasLiveGps -> formatCoordinatePair(userLatitude!!, userLongitude!!)
                    else -> "GPS"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = if (hasLiveGps) MaterialTheme.semanticColors.success else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            // Always-accessible Edit CTA right inside the map itself — fixed height & non-wrapping so it never changes shape
            Surface(
              color = MaterialTheme.colorScheme.secondary,
              shape = RoundedCornerShape(999.dp),
              shadowElevation = 2.dp,
              modifier = Modifier
                .height(34.dp)
                .widthIn(min = 66.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onEnterMapEditMode)
                .testTag("map_edit_mode_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Edit,
                  contentDescription = "Edit routes on map",
                  tint = MaterialTheme.colorScheme.onSecondary,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = "Edit",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondary,
                  maxLines = 1,
                  softWrap = false,
                )
              }
            }

            if (!recentStatusChipText.isNullOrBlank()) {
              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(999.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                  .weight(1f, fill = false)
                  .height(34.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.semanticColors.success,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = recentStatusChipText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.semanticColors.success,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }

          // Right Map Utility Buttons: Live GPS Center, Fit All Pins, Toggle Polylines, Fullscreen
          Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            MapUtilityCircleButton(
              icon = Icons.Rounded.MyLocation,
              contentDescription = "Center on my live GPS location",
              active = hasLiveGps,
              onClick = {
                onRequestActualLocation()
                if (hasLiveGps) recenterUserTrigger++
              },
              modifier = Modifier.testTag("map_center_gps_button"),
            )
            MapUtilityCircleButton(
              icon = Icons.Rounded.FitScreen,
              contentDescription = "Fit all route stops on map",
              active = false,
              onClick = { fitBoundsTrigger++ },
              modifier = Modifier.testTag("map_fit_bounds_button"),
            )
            MapUtilityCircleButton(
              icon = Icons.AutoMirrored.Rounded.AltRoute,
              contentDescription = "Toggle route polylines",
              active = showPolylines,
              onClick = onTogglePolylines,
              modifier = Modifier.testTag("map_toggle_routes_button"),
            )
            MapUtilityCircleButton(
              icon = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
              contentDescription = if (isFullscreen) "Exit Full Screen" else "Full Screen View",
              active = isFullscreen,
              onClick = onToggleFullscreen,
              modifier = Modifier.testTag("map_fullscreen_button"),
            )
          }
        }

        // Direct On-Map Route Name Pills Strip ("All Shops" shows every shop + route numbered 1, 2, 3...)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("map_route_name_pills_row"),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val isAllSelected = selectedRouteId == RoutesCatalog.ALL
          Surface(
            color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(
              width = 1.dp,
              color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            ),
            shadowElevation = 1.dp,
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .clickable { onSelectRouteFilter(RoutesCatalog.ALL) }
              .testTag("map_route_pill_all"),
          ) {
            Text(
              text = "All Shops (${previewShops.size})",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }

          previewRoutes.forEachIndexed { idx, route ->
            val isRouteSelected = selectedRouteId == route.id
            val spec = RouteColorPalette.specForIndex(route.colorIndex)
            val count = previewShops.count { it.routeId == route.id }
            val cleanRouteName = route.name.substringAfter(":").trim().ifBlank { route.name }
            Surface(
              color = if (isRouteSelected) spec.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
              shape = RoundedCornerShape(999.dp),
              border = BorderStroke(
                width = if (isRouteSelected) 1.5.dp else 1.dp,
                color = if (isRouteSelected && spec.secondary != null) spec.secondary else spec.primary.copy(alpha = 0.65f),
              ),
              shadowElevation = if (isRouteSelected) 3.dp else 1.dp,
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable {
                  onSelectRouteFilter(if (isRouteSelected) RoutesCatalog.ALL else route.id)
                }
                .testTag("map_route_pill_${route.id}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                RouteColorDot(spec = spec, size = 9.dp)
                Text(
                  text = "R${idx + 1} $cleanRouteName ($count)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (isRouteSelected) FontWeight.Bold else FontWeight.SemiBold,
                  color = if (isRouteSelected) spec.contrastInk else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    }

    // Bottom Interactive Prompt / Action Bar
    if (editDraft != null) {
      val targetRoute = editingTargetRoute
      val targetSpec = editingTargetColorSpec
      val targetStopCount = targetRoute?.let { editDraft.stopCountForRoute(it.id) } ?: 0
      val cleanTargetName = targetRoute?.name?.substringAfter(":")?.trim()?.ifBlank { targetRoute.name } ?: "Route"

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(
          1.2.dp,
          (targetSpec?.primary ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.45f),
        ),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(start = 10.dp, end = 10.dp, bottom = 14.dp)
          .testTag("map_edit_action_bar"),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f),
            ) {
              if (targetSpec != null) {
                RouteColorDot(spec = targetSpec, size = 10.dp)
              }
              Text(
                text = "$cleanTargetName • $targetStopCount stops",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Surface(
              color = if (editDraft.pendingChangeCount > 0) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.surfaceVariant
              },
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = if (editDraft.pendingChangeCount > 0) {
                  "${editDraft.pendingChangeCount} modified"
                } else {
                  "Tap shops to select / deselect"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (editDraft.pendingChangeCount > 0) {
                  MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = onUndoMapEdit,
              enabled = editDraft.canUndo,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .height(42.dp)
                .alpha(if (editDraft.canUndo) 1f else 0.5f)
                .testTag("map_edit_undo_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = "Undo last map change",
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Undo",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
              )
            }

            OutlinedButton(
              onClick = onCancelMapEdit,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .height(42.dp)
                .testTag("map_edit_cancel_button"),
            ) {
              Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
              )
            }

            Button(
              onClick = onSaveMapEdit,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .height(42.dp)
                .testTag("map_edit_save_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "Update & Save",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
              )
            }
          }
        }
      }
    } else if (selectedShop == null) {
      // Bottom-Left Interactive Edit Pill directly on the map with consistent fixed pill geometry
      Surface(
        color = if (mapTapEditHighlight) {
          MaterialTheme.colorScheme.secondary
        } else {
          MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        },
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(
          1.dp,
          if (mapTapEditHighlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(start = 12.dp, bottom = 20.dp)
          .height(34.dp)
          .clip(RoundedCornerShape(999.dp))
          .clickable(onClick = onEnterMapEditMode)
          .testTag("map_surface_edit_chip"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = "Edit routes on map",
            tint = if (mapTapEditHighlight) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "Edit Routes on Map",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (mapTapEditHighlight) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }

    // Floating Interactive Selected Shop Card Popup (Normal mode only)
    AnimatedVisibility(
      visible = !isEditMode && selectedShop != null,
      enter = fadeIn() + slideInVertically { it / 2 },
      exit = fadeOut() + slideOutVertically { it / 2 },
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(start = 12.dp, end = 12.dp, bottom = 20.dp),
    ) {
      selectedShop?.let { shop ->
        val currentRoute = routeMap[shop.routeId]
        val currentSpec = currentRoute?.let { RouteColorPalette.specForIndex(it.colorIndex) }
        SelectedShopMapPopupCard(
          shop = shop,
          currentRoute = currentRoute,
          currentSpec = currentSpec,
          routes = previewRoutes,
          onCall = { openDialer(context, shop.phone) },
          onClose = { onSelectShop(null) },
          onReassignRoute = { targetRouteId -> onReassignShopRoute(shop, targetRouteId) },
        )
      }
    }
  }
}

@Composable
private fun MapUtilityCircleButton(
  icon: ImageVector,
  contentDescription: String,
  active: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    shape = CircleShape,
    shadowElevation = 3.dp,
    border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    modifier = modifier
      .size(38.dp)
      .clip(CircleShape)
      .clickable(onClick = onClick),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun SelectedShopMapPopupCard(
  shop: Shop,
  currentRoute: DeliveryRoute?,
  currentSpec: RouteColorSpec?,
  routes: List<DeliveryRoute>,
  onCall: () -> Unit,
  onClose: () -> Unit,
  onReassignRoute: (String) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Store,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = shop.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
              )
              Surface(
                color = currentSpec?.primary ?: MaterialTheme.semanticColors.dangerContainer,
                shape = RoundedCornerShape(999.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  if (currentSpec?.isCombination == true && currentSpec.secondary != null) {
                    Box(
                      modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(currentSpec.secondary),
                    )
                  }
                  Text(
                    text = currentRoute?.name?.substringAfter(": ")?.take(18) ?: "Unallocated",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = currentSpec?.contrastInk ?: MaterialTheme.semanticColors.danger,
                    maxLines = 1,
                  )
                }
              }
            }
            Text(
              text = "ID: ${shop.shopCode} • ${shop.ownerName}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Box(
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClose)
            .testTag("close_shop_popup_button"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close shop card",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      // Details Strip: Address + Phone Call
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = shop.address,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(
            modifier = Modifier
              .weight(0.9f)
              .clip(RoundedCornerShape(6.dp))
              .clickable(onClick = onCall)
              .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Call,
              contentDescription = "Call ${shop.name}",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = shop.phone,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // Quick Assign Corridor Buttons
      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
          text = "Assign to Route:",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          routes.forEachIndexed { idx, route ->
            val spec = RouteColorPalette.specForIndex(route.colorIndex)
            val isCurrent = shop.routeId == route.id
            val routeLabel = route.name.substringAfter(":").trim().ifBlank { route.name }
            val shortLabel = "R${idx + 1}: $routeLabel"
            Surface(
              color = if (isCurrent) spec.primary else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(9.dp),
              border = if (isCurrent && spec.secondary != null) {
                BorderStroke(1.5.dp, spec.secondary)
              } else if (!isCurrent) {
                BorderStroke(1.dp, spec.primary.copy(alpha = 0.5f))
              } else null,
              modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .clickable { onReassignRoute(route.id) }
                .testTag("quick_assign_${route.id}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                RouteColorDot(spec = spec, size = 10.dp)
                Text(
                  text = if (isCurrent) "✓ $shortLabel" else "+ $shortLabel",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isCurrent) spec.contrastInk else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                )
              }
            }
          }

          val isUnalloc = shop.routeId == RoutesCatalog.UNALLOCATED
          Surface(
            color = if (isUnalloc) MaterialTheme.semanticColors.danger else MaterialTheme.semanticColors.dangerContainer,
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier
              .height(36.dp)
              .clip(RoundedCornerShape(9.dp))
              .clickable { onReassignRoute(RoutesCatalog.UNALLOCATED) }
              .testTag("quick_assign_unallocated"),
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 10.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "Unassign",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isUnalloc) MaterialTheme.colorScheme.onError else MaterialTheme.semanticColors.danger,
              )
            }
          }
        }
      }
    }
  }
}
