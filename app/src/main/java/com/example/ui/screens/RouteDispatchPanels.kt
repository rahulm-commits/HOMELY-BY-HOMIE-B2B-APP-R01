package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material.icons.rounded.WrongLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.ui.components.StandardCompactSearchBar
import com.example.ui.components.openDialer
import com.example.ui.components.openWhatsApp
import com.example.ui.theme.RouteColorDot
import com.example.ui.theme.RouteColorPalette
import com.example.ui.theme.RouteProgressBar
import com.example.ui.theme.semanticColors
import kotlin.math.roundToInt

enum class DispatchDrawerTab {
  UNALLOCATED,
  ROUTES,
  MISSING_GPS,
}

private val vehiclePresets = listOf(
  "Tata Ace (KL-07-CD-4190)" to 32,
  "Mahindra Bolero Maxi (KL-07-CB-9081)" to 40,
  "Ashok Leyland Dost (KL-07-CC-1120)" to 30,
  "Eicher Pro 2049 (KL-07-BX-4450)" to 60,
)

private val stagingBayPresets = listOf(
  "Bay #03 - North Dock",
  "Bay #01 - South Dock",
  "Bay #02 - Main Facility",
  "Bay #05 - Central Dock",
)

@Composable
fun AllocationDispatchDrawer(
  shops: List<Shop>,
  routes: List<DeliveryRoute>,
  selectedRouteId: String,
  activeTab: DispatchDrawerTab,
  isExpanded: Boolean,
  checkedShopIds: Set<String>,
  onToggleExpand: () -> Unit,
  onSelectTab: (DispatchDrawerTab) -> Unit,
  onSelectRoute: (String) -> Unit,
  onClearRouteFilter: () -> Unit,
  onToggleShopChecked: (String) -> Unit,
  onFocusShopOnMap: (Shop) -> Unit,
  onEditRoute: (DeliveryRoute) -> Unit,
  onCreateNewRoute: () -> Unit,
  onEditSelectedRouteShops: (DeliveryRoute) -> Unit,
  onRemoveShopFromRoute: (Shop) -> Unit,
  onAutoGroupUnallocated: (List<Shop>) -> Unit,
  onMoveShopStopOrder: (DeliveryRoute, List<String>) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var unallocatedQuery by remember { mutableStateOf("") }

  val unallocatedShops = remember(shops, unallocatedQuery) {
    val q = unallocatedQuery.trim().lowercase()
    shops.filter { shop ->
      val isUnalloc = shop.routeId == RoutesCatalog.UNALLOCATED || shop.routeId.isBlank()
      val matchesSearch = q.isEmpty() ||
        shop.name.lowercase().contains(q) ||
        shop.address.lowercase().contains(q) ||
        shop.phone.lowercase().contains(q) ||
        shop.shopCode.lowercase().contains(q)
      isUnalloc && matchesSearch
    }
  }
  val totalUnallocatedCount = remember(shops) {
    shops.count { it.routeId == RoutesCatalog.UNALLOCATED || it.routeId.isBlank() }
  }
  val missingGpsShops = remember(shops) {
    shops.filter { it.latitude == 0.0 && it.longitude == 0.0 }
  }

  val selectedRoute = remember(routes, selectedRouteId) {
    routes.firstOrNull { it.id == selectedRouteId }
  }
  val selectedRouteShops = remember(shops, selectedRouteId, selectedRoute) {
    if (selectedRouteId == RoutesCatalog.ALL) {
      emptyList()
    } else {
      RoutesCatalog.sortShopsByRouteSequence(
        shops.filter { it.routeId == selectedRouteId },
        selectedRoute,
      )
    }
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    shadowElevation = 8.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
    ) {
      // Handle & Dispatch Sheet Header
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onToggleExpand)
          .padding(top = 10.dp, bottom = 8.dp)
          .testTag("drawer_header_toggle"),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier
            .width(38.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.LocalShipping,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = "Route Dispatch",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              color = if (totalUnallocatedCount > 0) MaterialTheme.semanticColors.dangerContainer else MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = "$totalUnallocatedCount Unallocated",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (totalUnallocatedCount > 0) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
            Icon(
              imageVector = if (isExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
              contentDescription = if (isExpanded) "Collapse drawer" else "Expand drawer",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      if (selectedRoute != null) {
        SelectedRouteChecklistPanel(
          route = selectedRoute,
          routeShops = selectedRouteShops,
          checkedShopIds = checkedShopIds,
          onToggleChecked = onToggleShopChecked,
          onFocusShopOnMap = onFocusShopOnMap,
          onEditRouteDetails = { onEditRoute(selectedRoute) },
          onEditRouteShops = { onEditSelectedRouteShops(selectedRoute) },
          onRemoveFromRoute = onRemoveShopFromRoute,
          onMoveStopUp = { idx ->
            if (idx > 0) {
              val mutable = selectedRouteShops.map { it.id }.toMutableList()
              val moved = mutable.removeAt(idx)
              mutable.add(idx - 1, moved)
              onMoveShopStopOrder(selectedRoute, mutable)
            }
          },
          onMoveStopDown = { idx ->
            if (idx in 0 until selectedRouteShops.lastIndex) {
              val mutable = selectedRouteShops.map { it.id }.toMutableList()
              val moved = mutable.removeAt(idx)
              mutable.add(idx + 1, moved)
              onMoveShopStopOrder(selectedRoute, mutable)
            }
          },
          onClearSelection = onClearRouteFilter,
        )
      }

      // M3 Segmented Tabs matching OrderScreen / ShopListScreen tabs
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          DrawerSegmentedTab(
            label = "Unallocated ($totalUnallocatedCount)",
            selected = activeTab == DispatchDrawerTab.UNALLOCATED,
            onClick = { onSelectTab(DispatchDrawerTab.UNALLOCATED) },
            modifier = Modifier
              .weight(1f)
              .testTag("dispatch_tab_unallocated"),
          )
          DrawerSegmentedTab(
            label = "My Routes (${routes.size})",
            selected = activeTab == DispatchDrawerTab.ROUTES,
            onClick = { onSelectTab(DispatchDrawerTab.ROUTES) },
            modifier = Modifier
              .weight(1f)
              .testTag("dispatch_tab_routes"),
          )
          DrawerSegmentedTab(
            label = "No GPS (${missingGpsShops.size})",
            selected = activeTab == DispatchDrawerTab.MISSING_GPS,
            onClick = { onSelectTab(DispatchDrawerTab.MISSING_GPS) },
            modifier = Modifier
              .weight(1f)
              .testTag("dispatch_tab_missing_gps"),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      when (activeTab) {
        DispatchDrawerTab.ROUTES -> {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            routes.forEach { route ->
              val assignedCount = shops.count { it.routeId == route.id }
              val spec = RouteColorPalette.specForIndex(route.colorIndex)
              val pct = if (route.maxCrates > 0) {
                ((assignedCount.toFloat() / route.maxCrates.toFloat()) * 100f).roundToInt().coerceAtLeast(0)
              } else 0
              val progress = if (route.maxCrates > 0) {
                (assignedCount.toFloat() / route.maxCrates.toFloat()).coerceIn(0f, 1f)
              } else 0f
              val isFocusedRoute = selectedRouteId == route.id

              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isFocusedRoute) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isFocusedRoute) 3.dp else 2.dp),
                border = BorderStroke(
                  width = if (isFocusedRoute) 1.6.dp else 1.dp,
                  color = if (isFocusedRoute) spec.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { onSelectRoute(route.id) }
                  .testTag("route_card_${route.id}"),
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      modifier = Modifier.weight(1f),
                    ) {
                      RouteColorDot(spec = spec, size = 16.dp)
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = route.name,
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = if (spec.isCombination) spec.label else "${spec.label} • ${route.driverName}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }

                    Row(
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Surface(
                        color = if (isFocusedRoute) spec.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier
                          .size(34.dp)
                          .clip(CircleShape)
                          .clickable { onSelectRoute(route.id) }
                          .testTag("view_route_${route.id}"),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "View ${route.name} shops",
                            tint = spec.primary,
                            modifier = Modifier.size(18.dp),
                          )
                        }
                      }
                      Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier
                          .size(34.dp)
                          .clip(CircleShape)
                          .clickable { onEditRoute(route) }
                          .testTag("edit_route_${route.id}"),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit ${route.name} & Driver Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                          )
                        }
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = route.vehicle,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "$assignedCount / ${route.maxCrates} Crates ($pct%)",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = spec.primary,
                    )
                  }

                  RouteProgressBar(
                    progress = progress.coerceAtLeast(if (assignedCount > 0) 0.05f else 0f),
                    spec = spec,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(7.dp),
                  )
                }
              }
            }

            // Create New Delivery Route CTA matching M3 Primary / Secondary CTAs
            Button(
              onClick = onCreateNewRoute,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("create_new_route_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.AddCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Create New Delivery Route",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        DispatchDrawerTab.UNALLOCATED -> {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            StandardCompactSearchBar(
              query = unallocatedQuery,
              onQueryChange = { unallocatedQuery = it },
              placeholder = "Search unallocated shops...",
              fieldTestTag = "unallocated_search_input",
              containerColor = MaterialTheme.colorScheme.surface,
            )

            if (unallocatedShops.isEmpty()) {
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "All active shops are assigned to delivery routes.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(14.dp),
                )
              }
            } else {
              unallocatedShops.forEach { shop ->
                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
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
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                          )
                        }
                      }
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = shop.name,
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "${shop.address} • ${shop.phone}",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "Unallocated • ${shop.shopCode}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.semanticColors.danger,
                        )
                      }
                    }
                    Surface(
                      color = MaterialTheme.colorScheme.primary,
                      shape = RoundedCornerShape(9.dp),
                      modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onFocusShopOnMap(shop) },
                    ) {
                      Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                      ) {
                        Text(
                          text = "+ Assign",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onPrimary,
                        )
                      }
                    }
                  }
                }
              }

              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.AutoFixHigh,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(22.dp),
                    )
                    Column {
                      Text(
                        text = "Smart Route Optimizer",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                      )
                      Text(
                        text = "Auto-assign remaining ${unallocatedShops.size} stops",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                      )
                    }
                  }
                  Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                      .height(36.dp)
                      .clip(RoundedCornerShape(9.dp))
                      .clickable { onAutoGroupUnallocated(unallocatedShops) }
                      .testTag("auto_group_unallocated_button"),
                  ) {
                    Box(
                      modifier = Modifier.padding(horizontal = 12.dp),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = "Auto Group",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        DispatchDrawerTab.MISSING_GPS -> {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              color = MaterialTheme.semanticColors.dangerContainer,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.WrongLocation,
                  contentDescription = null,
                  tint = MaterialTheme.semanticColors.danger,
                  modifier = Modifier.size(24.dp),
                )
                Column {
                  Text(
                    text = "${missingGpsShops.size} Stores Need GPS Coordinates",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.semanticColors.danger,
                  )
                  Text(
                    text = "Request shopkeeper WhatsApp pin or edit shop GPS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.semanticColors.danger.copy(alpha = 0.85f),
                  )
                }
              }
            }

            if (missingGpsShops.isEmpty()) {
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "All active shops already have real GPS coordinates locked.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(14.dp),
                )
              }
            } else {
              missingGpsShops.forEach { shop ->
                Card(
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = shop.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = shop.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                      color = MaterialTheme.colorScheme.secondaryContainer,
                      shape = RoundedCornerShape(9.dp),
                      modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable {
                          openWhatsApp(
                            context,
                            shop.phone,
                            "Please share live GPS location pin for ${shop.name} (${shop.shopCode}) for SnackRoute dispatch.",
                          )
                        },
                    ) {
                      Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                      ) {
                        Text(
                          text = "Request GPS",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SelectedRouteChecklistPanel(
  route: DeliveryRoute,
  routeShops: List<Shop>,
  checkedShopIds: Set<String>,
  onToggleChecked: (String) -> Unit,
  onFocusShopOnMap: (Shop) -> Unit,
  onEditRouteDetails: () -> Unit,
  onEditRouteShops: () -> Unit,
  onRemoveFromRoute: (Shop) -> Unit,
  onMoveStopUp: (Int) -> Unit,
  onMoveStopDown: (Int) -> Unit,
  onClearSelection: () -> Unit,
) {
  val context = LocalContext.current
  val spec = RouteColorPalette.specForIndex(route.colorIndex)
  val checkedCount = routeShops.count { it.id in checkedShopIds }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    border = BorderStroke(1.4.dp, spec.primary.copy(alpha = 0.7f)),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .testTag("selected_route_checklist_panel"),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          RouteColorDot(spec = spec, size = 14.dp)
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${route.name} • Shop Checklist",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = if (routeShops.size > 1) {
                "${routeShops.size} stops (1→${routeShops.size}) • Packs reverse (${routeShops.size}→1)"
              } else {
                "${routeShops.size} shops assigned • $checkedCount verified"
              },
              style = MaterialTheme.typography.labelSmall,
              color = spec.primary,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = spec.primary,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onEditRouteShops)
              .testTag("manage_route_stops_button"),
          ) {
            Text(
              text = "Edit Stops",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = spec.contrastInk,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .clickable(onClick = onEditRouteDetails),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = "Edit route details",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
              )
            }
          }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .clickable(onClick = onClearSelection),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Show all routes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }

      if (routeShops.isEmpty()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "No shops assigned to this route yet.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "+ Add Stops",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = spec.primary,
              modifier = Modifier.clickable(onClick = onEditRouteShops),
            )
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          routeShops.forEachIndexed { index, shop ->
            val checked = shop.id in checkedShopIds
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onFocusShopOnMap(shop) }
                .testTag("checklist_shop_${shop.shopCode}"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                  checked = checked,
                  onCheckedChange = { onToggleChecked(shop.id) },
                  colors = CheckboxDefaults.colors(checkedColor = spec.primary),
                )
                Surface(
                  color = spec.primary.copy(alpha = 0.14f),
                  shape = CircleShape,
                  modifier = Modifier.size(26.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = "${index + 1}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = spec.primary,
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = shop.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (checked) TextDecoration.LineThrough else null,
                    color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${shop.shopCode} • ${shop.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                if (routeShops.size > 1) {
                  Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .clickable(enabled = index > 0) { onMoveStopUp(index) }
                      .testTag("route_move_up_${shop.shopCode}"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Move ${shop.name} earlier",
                        tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(2.dp))
                  Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .clickable(enabled = index < routeShops.lastIndex) { onMoveStopDown(index) }
                      .testTag("route_move_down_${shop.shopCode}"),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Move ${shop.name} later",
                        tint = if (index < routeShops.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(4.dp))
                }
                Surface(
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = CircleShape,
                  modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { openDialer(context, shop.phone) },
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.Call,
                      contentDescription = "Call ${shop.name}",
                      tint = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(15.dp),
                    )
                  }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                  color = MaterialTheme.semanticColors.dangerContainer,
                  shape = CircleShape,
                  modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { onRemoveFromRoute(shop) },
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.RemoveCircleOutline,
                      contentDescription = "Remove ${shop.name} from route",
                      tint = MaterialTheme.semanticColors.danger,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DrawerSegmentedTab(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
    shape = RoundedCornerShape(9.dp),
    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) else null,
    shadowElevation = if (selected) 2.dp else 0.dp,
    modifier = modifier
      .height(36.dp)
      .clip(RoundedCornerShape(9.dp))
      .clickable(onClick = onClick),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRouteBottomSheet(
  initialRoute: DeliveryRoute?,
  usedColorIndices: Set<Int>,
  assignedCratesCount: Int,
  nextRouteNumber: Int,
  onDismiss: () -> Unit,
  onSaveRoute: (DeliveryRoute) -> Unit,
) {
  val isCreate = initialRoute == null
  val defaultColorIndex = remember(initialRoute, usedColorIndices) {
    initialRoute?.colorIndex ?: RouteColorPalette.nextUnusedColorIndex(usedColorIndices)
  }

  var routeName by remember {
    mutableStateOf(
      initialRoute?.name ?: "Route $nextRouteNumber: New Corridor"
    )
  }
  var selectedVehicle by remember {
    mutableStateOf(initialRoute?.vehicle ?: vehiclePresets[nextRouteNumber % vehiclePresets.size].first)
  }
  var maxCrates by remember {
    mutableIntStateOf(
      initialRoute?.maxCrates ?: vehiclePresets[nextRouteNumber % vehiclePresets.size].second
    )
  }
  var driverName by remember {
    mutableStateOf(initialRoute?.driverName ?: "Anand V.")
  }
  var driverPhone by remember {
    mutableStateOf(initialRoute?.driverPhone ?: "+91 98450 99887")
  }
  var stagingBay by remember {
    mutableStateOf(initialRoute?.stagingBay ?: stagingBayPresets[nextRouteNumber % stagingBayPresets.size])
  }
  var dispatchWindow by remember {
    mutableStateOf(initialRoute?.dispatchWindow ?: "06:30 AM – 10:30 AM")
  }
  var selectedColorIndex by remember { mutableIntStateOf(defaultColorIndex) }
  var vehicleDropdownOpen by remember { mutableStateOf(false) }
  var bayDropdownOpen by remember { mutableStateOf(false) }

  val colorSpec = RouteColorPalette.specForIndex(selectedColorIndex)
  val loadPct = if (maxCrates > 0) ((assignedCratesCount.toFloat() / maxCrates) * 100f).roundToInt() else 0
  val loadProgress = if (maxCrates > 0) (assignedCratesCount.toFloat() / maxCrates).coerceIn(0f, 1f) else 0f

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isCreate) "CREATE DELIVERY ROUTE" else "EDIT ROUTE & DRIVER",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = routeName.ifBlank { "New Route" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = CircleShape,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onDismiss),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 480.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
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
              ) {
                Icon(
                  imageVector = Icons.Rounded.Inventory2,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Crate Load Capacity",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = "$assignedCratesCount / $maxCrates Crates ($loadPct%)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colorSpec.primary,
              )
            }
            RouteProgressBar(
              progress = loadProgress.coerceAtLeast(if (assignedCratesCount > 0) 0.05f else 0f),
              spec = colorSpec,
              trackColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier
                .fillMaxWidth()
                .height(9.dp),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (colorSpec.isCombination) "Overflow Combination Pattern" else "Palette Color",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = colorSpec.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorSpec.primary,
              )
            }
          }
        }

        OutlinedTextField(
          value = routeName,
          onValueChange = { routeName = it },
          label = { Text("Route Name") },
          leadingIcon = {
            Icon(Icons.AutoMirrored.Rounded.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("modal_route_name_input"),
        )

        Box(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = "$selectedVehicle • Cap: $maxCrates Crates",
            onValueChange = {},
            readOnly = true,
            label = { Text("Assigned Vehicle") },
            leadingIcon = {
              Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = { Icon(Icons.Rounded.UnfoldMore, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          )
          Box(
            modifier = Modifier
              .matchParentSize()
              .clickable { vehicleDropdownOpen = true },
          )
          DropdownMenu(
            expanded = vehicleDropdownOpen,
            onDismissRequest = { vehicleDropdownOpen = false },
          ) {
            vehiclePresets.forEach { (vehName, cap) ->
              DropdownMenuItem(
                text = { Text("$vehName • Cap: $cap Crates") },
                onClick = {
                  selectedVehicle = vehName
                  maxCrates = cap
                  vehicleDropdownOpen = false
                },
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = driverName,
            onValueChange = { driverName = it },
            label = { Text("Driver Name") },
            leadingIcon = {
              Icon(Icons.Rounded.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("modal_driver_name_input"),
          )
          OutlinedTextField(
            value = driverPhone,
            onValueChange = { driverPhone = it },
            label = { Text("Driver Contact") },
            leadingIcon = {
              Icon(Icons.Rounded.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("modal_driver_phone_input"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
              value = stagingBay,
              onValueChange = {},
              readOnly = true,
              label = { Text("Staging Bay") },
              leadingIcon = {
                Icon(Icons.Rounded.Warehouse, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
              },
              trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            )
            Box(
              modifier = Modifier
                .matchParentSize()
                .clickable { bayDropdownOpen = true },
            )
            DropdownMenu(
              expanded = bayDropdownOpen,
              onDismissRequest = { bayDropdownOpen = false },
            ) {
              stagingBayPresets.forEach { bay ->
                DropdownMenuItem(
                  text = { Text(bay) },
                  onClick = {
                    stagingBay = bay
                    bayDropdownOpen = false
                  },
                )
              }
            }
          }

          OutlinedTextField(
            value = dispatchWindow,
            onValueChange = { dispatchWindow = it },
            label = { Text("Dispatch Window") },
            leadingIcon = {
              Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Route Color / Combination (${colorSpec.label})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          val displayPresets = remember(selectedColorIndex) {
            val base = RouteColorPalette.allPresets.take(25)
            val current = RouteColorPalette.specForIndex(selectedColorIndex)
            if (base.any { it.index == current.index }) base else base + current
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            displayPresets.forEach { preset ->
              val isPicked = selectedColorIndex == preset.index
              Surface(
                color = if (isPicked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                border = BorderStroke(
                  width = if (isPicked) 2.5.dp else 1.dp,
                  color = if (isPicked) preset.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                ),
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .clickable { selectedColorIndex = preset.index }
                  .testTag("color_swatch_${preset.index}"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  RouteColorDot(spec = preset, size = 22.dp)
                }
              }
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "Cancel",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Button(
          onClick = {
            val safeName = routeName.trim().ifBlank { "Route $nextRouteNumber" }
            val generatedId = initialRoute?.id ?: buildString {
              append("R")
              append(nextRouteNumber.toString().padStart(2, '0'))
              append("-")
              append(safeName.uppercase().replace(Regex("[^A-Z0-9]+"), "-").trim('-').take(20))
            }
            onSaveRoute(
              DeliveryRoute(
                id = generatedId,
                name = safeName,
                vehicle = selectedVehicle,
                maxCrates = maxCrates,
                driverName = driverName.trim().ifBlank { "Field Driver" },
                driverPhone = driverPhone.trim(),
                dispatchWindow = dispatchWindow.trim(),
                stagingBay = stagingBay,
                colorIndex = selectedColorIndex,
                isActive = true,
              )
            )
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .weight(2f)
            .height(48.dp)
            .testTag("save_route_modal_button"),
        ) {
          Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Save Route",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRouteShopsSheet(
  route: DeliveryRoute,
  allShops: List<Shop>,
  onDismiss: () -> Unit,
  onApplyChanges: (toAssignIds: List<String>, toUnassignIds: List<String>) -> Unit,
) {
  val spec = RouteColorPalette.specForIndex(route.colorIndex)
  var query by remember { mutableStateOf("") }
  val selectedShopIds = remember(route.id, allShops) {
    mutableStateListOf<String>().apply {
      addAll(allShops.filter { it.routeId == route.id }.map { it.id })
    }
  }

  val filtered = remember(allShops, query) {
    val q = query.trim().lowercase()
    allShops.filter { s ->
      q.isEmpty() ||
        s.name.lowercase().contains(q) ||
        s.shopCode.lowercase().contains(q) ||
        s.address.lowercase().contains(q)
    }
  }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          RouteColorDot(spec = spec, size = 16.dp)
          Column {
            Text(
              text = "EDIT SHOPS IN ROUTE",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = spec.primary,
            )
            Text(
              text = "${route.name} (${selectedShopIds.size} selected)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = CircleShape,
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onDismiss),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, contentDescription = "Close")
          }
        }
      }

      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search shop to add/remove from route...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
      )

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 380.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(filtered, key = { it.id.ifBlank { it.shopCode } }) { shop ->
          val checked = shop.id in selectedShopIds
          Surface(
            color = if (checked) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                if (checked) selectedShopIds.remove(shop.id) else selectedShopIds.add(shop.id)
              },
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = checked,
                onCheckedChange = { isNowChecked ->
                  if (isNowChecked && shop.id !in selectedShopIds) {
                    selectedShopIds.add(shop.id)
                  } else if (!isNowChecked) {
                    selectedShopIds.remove(shop.id)
                  }
                },
                colors = CheckboxDefaults.colors(checkedColor = spec.primary),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = shop.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp),
                  )
                  Text(
                    text = "${shop.shopCode} • Currently: ${shop.routeId.substringBefore("-")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
          }
        }

        Button(
          onClick = {
            val originallyInRoute = allShops.filter { it.routeId == route.id }.map { it.id }.toSet()
            val targetSet = selectedShopIds.toSet()
            val toAssign = targetSet.filter { it !in originallyInRoute }
            val toUnassign = originallyInRoute.filter { it !in targetSet }
            onApplyChanges(toAssign, toUnassign)
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .weight(2f)
            .height(46.dp)
            .testTag("apply_route_shops_button"),
        ) {
          Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Apply Route Stops", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
