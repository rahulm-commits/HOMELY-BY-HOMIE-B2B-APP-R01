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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.formatCoordinatePair
import com.example.ui.components.openDialer
import com.example.ui.theme.CobaltInfo
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralDangerContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.MintBadgeBg
import com.example.ui.theme.MintCardBorder
import com.example.ui.theme.MintFieldBorder
import com.example.ui.theme.MintFieldFill
import com.example.ui.theme.RouteColorDot
import com.example.ui.theme.RouteColorPalette
import com.example.ui.theme.RouteColorSpec

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
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  var routeDropdownExpanded by remember { mutableStateOf(false) }
  var recenterUserTrigger by remember { mutableIntStateOf(0) }
  var fitBoundsTrigger by remember { mutableIntStateOf(0) }

  val routeMap = remember(routes) { routes.associateBy { it.id } }
  val selectedRoute = routeMap[selectedRouteId]
  val selectedColorSpec = selectedRoute?.let { RouteColorPalette.specForIndex(it.colorIndex) }
  val selectedShopCount = remember(shops, selectedRouteId) {
    if (selectedRouteId == RoutesCatalog.ALL) shops.size else shops.count { it.routeId == selectedRouteId }
  }
  val hasLiveGps = userLatitude != null && userLongitude != null &&
    (userLatitude != 0.0 || userLongitude != 0.0)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceVariant),
  ) {
    // Real Interactive OpenStreetMap Viewport with live GPS + real shop coordinates
    RealOsmMapView(
      shops = shops,
      routes = routes,
      selectedRouteId = selectedRouteId,
      selectedShopId = selectedShop?.id,
      showPolylines = showPolylines,
      userLatitude = userLatitude,
      userLongitude = userLongitude,
      recenterUserTrigger = recenterUserTrigger,
      fitBoundsTrigger = fitBoundsTrigger,
      onPinSelected = { shopId ->
        val tapped = shops.firstOrNull { it.id == shopId }
        onSelectShop(tapped)
      },
      onMapSurfaceTapped = { onSelectShop(null) },
      modifier = Modifier.fillMaxSize(),
    )

    // Top Floating Controls Layer styled consistently with OrderScreen & ShopListScreen
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
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      // Route Filter Dropdown + Create Route "+" Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(modifier = Modifier.weight(1f)) {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.5.dp,
            border = BorderStroke(1.2.dp, MintCardBorder),
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
                  text = selectedRoute?.name ?: "All Routes",
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
            modifier = Modifier.fillMaxWidth(0.84f),
          ) {
            val allSelected = selectedRouteId == RoutesCatalog.ALL
            DropdownMenuItem(
              leadingIcon = {
                Box(
                  modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                )
              },
              text = {
                Text(
                  text = "All Routes (${shops.size} shops)",
                  fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal,
                )
              },
              trailingIcon = if (allSelected) {
                { Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
              } else null,
              onClick = {
                routeDropdownExpanded = false
                onSelectRouteFilter(RoutesCatalog.ALL)
              },
            )
            HorizontalDivider()
            routes.forEach { route ->
              val isSelected = selectedRouteId == route.id
              val count = shops.count { it.routeId == route.id }
              val spec = RouteColorPalette.specForIndex(route.colorIndex)
              DropdownMenuItem(
                leadingIcon = { RouteColorDot(spec = spec, size = 14.dp) },
                text = {
                  Text(
                    text = "${route.name} ($count shops)",
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                trailingIcon = if (isSelected) {
                  { Icon(Icons.Rounded.Check, contentDescription = null, tint = spec.primary) }
                } else null,
                onClick = {
                  routeDropdownExpanded = false
                  onSelectRouteFilter(route.id)
                },
              )
            }
            HorizontalDivider()
            DropdownMenuItem(
              leadingIcon = {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              },
              text = {
                Text(
                  text = "Create New Route",
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                )
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

      // Live GPS Indicator Pill + Status/Edit Stops + Map Action Buttons
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
          Surface(
            color = if (hasLiveGps) EmeraldSuccessContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(
              1.dp,
              if (hasLiveGps) EmeraldSuccess.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            ),
            shadowElevation = 1.dp,
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .clickable {
                onRequestActualLocation()
                if (hasLiveGps) recenterUserTrigger++
              },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(
                    when {
                      isFetchingLocation -> MaterialTheme.colorScheme.primary
                      hasLiveGps -> CobaltInfo
                      else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                  ),
              )
              Text(
                text = when {
                  isFetchingLocation -> "Locating GPS..."
                  hasLiveGps -> formatCoordinatePair(userLatitude!!, userLongitude!!)
                  else -> "Tap for Live GPS"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (hasLiveGps) EmeraldSuccess else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          if (!recentStatusChipText.isNullOrBlank()) {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(999.dp),
              shadowElevation = 2.dp,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = null,
                  tint = EmeraldSuccess,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = recentStatusChipText,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = EmeraldSuccess,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          } else if (selectedRoute != null && onEditSelectedRouteShopsClick != null) {
            Surface(
              color = MaterialTheme.colorScheme.secondary,
              shape = RoundedCornerShape(999.dp),
              shadowElevation = 2.dp,
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onEditSelectedRouteShopsClick)
                .testTag("edit_selected_route_shops_chip"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.AltRoute,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSecondary,
                  modifier = Modifier.size(13.dp),
                )
                Text(
                  text = "Edit Stops",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondary,
                )
              }
            }
          }
        }

        // Right Map Utility Buttons: Live GPS Center, Fit All Pins, Toggle Polylines, Fullscreen
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
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
    }

    // Bottom-Left Helpful Tip Chip when no popup is open
    if (selectedShop == null) {
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(start = 12.dp, bottom = 22.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
          )
          Text(
            text = "OpenStreetMap • Tap pin to assign route",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Floating Interactive Selected Shop Card Popup
    AnimatedVisibility(
      visible = selectedShop != null,
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
          routes = routes,
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
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    border = BorderStroke(1.3.dp, MintCardBorder),
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
                color = currentSpec?.primary ?: CoralDangerContainer,
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
                    color = currentSpec?.contrastInk ?: CoralDanger,
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
        color = MintFieldFill,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MintFieldBorder),
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
          text = "Quick Assign Route:",
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
            val shortLabel = "+ R${idx + 1} ${route.name.substringAfter(":").trim().take(12)}"
            Surface(
              color = if (isCurrent) spec.primary else MintBadgeBg,
              shape = RoundedCornerShape(9.dp),
              border = if (isCurrent && spec.secondary != null) {
                BorderStroke(1.5.dp, spec.secondary)
              } else if (!isCurrent) {
                BorderStroke(1.dp, MintFieldBorder)
              } else null,
              modifier = Modifier
                .height(36.dp)
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
                  text = shortLabel,
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
            color = if (isUnalloc) CoralDanger else CoralDangerContainer,
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
                color = if (isUnalloc) Color.White else CoralDanger,
              )
            }
          }
        }
      }
    }
  }
}
