package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.StockItem
import com.example.ui.components.StandardCompactSearchBar
import com.example.ui.components.StandardRouteDropdown
import com.example.ui.theme.semanticColors
import com.example.ui.theme.RouteColorDot
import com.example.ui.theme.RouteColorPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PackingTab(val label: String, val icon: ImageVector) {
  CONSOLIDATED("Consolidated", Icons.Rounded.Inventory2),
  SHOP_SLIPS("Shop Slips", Icons.Rounded.Storefront),
  A4_MANIFEST("A4 Manifest", Icons.Rounded.Description),
}

@Composable
fun PackingScreen(
  shops: List<Shop>,
  orders: List<OrderRecord>,
  routes: List<DeliveryRoute>,
  stocks: List<StockItem>,
  onUpdateStock: (stockId: String, newQty: Int, productName: String) -> Unit,
  onOpenSyncServers: () -> Unit,
  onNavigateToRoutes: () -> Unit,
  onNavigateToOrders: () -> Unit,
  onShowFeedback: (String) -> Unit,
  onSaveRoute: (DeliveryRoute, String) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var selectedRouteId by rememberSaveable { mutableStateOf(RoutesCatalog.ALL) }
  var activeTab by rememberSaveable { mutableStateOf(PackingTab.CONSOLIDATED) }
  var searchQuery by remember { mutableStateOf("") }
  var onlyShortages by remember { mutableStateOf(false) }
  var reverseLoadOrder by rememberSaveable { mutableStateOf(true) }
  var showReorderStopsSheet by remember { mutableStateOf(false) }

  val loadedOverrides = remember { mutableStateMapOf<String, Int>() }
  val packedOrderOverrides = remember { mutableStateMapOf<String, Boolean>() }

  val snapshot = remember(
    selectedRouteId,
    shops,
    orders,
    routes,
    stocks,
    loadedOverrides.toMap(),
    packedOrderOverrides.toMap(),
    searchQuery,
    reverseLoadOrder,
  ) {
    buildPackingSnapshot(
      selectedRouteId = selectedRouteId,
      shops = shops,
      orders = orders,
      routes = routes,
      stocks = stocks,
      loadedOverrides = loadedOverrides,
      packedOrderOverrides = packedOrderOverrides,
      searchQuery = searchQuery,
      reverseLoadOrder = reverseLoadOrder,
    )
  }

  val displayedCategoryGroups = remember(snapshot.categoryGroups, onlyShortages) {
    if (!onlyShortages) {
      snapshot.categoryGroups
    } else {
      snapshot.categoryGroups.mapNotNull { grp ->
        val filtered = grp.items.filter { it.shortagePacks > 0 }
        if (filtered.isEmpty()) null else grp.copy(items = filtered)
      }
    }
  }

  val todayHeader = remember {
    SimpleDateFormat("EEE, dd MMM", Locale.US).format(Date())
  }

  val routeSubtitle = remember(snapshot.selectedRoute, selectedRouteId, routes) {
    when {
      snapshot.selectedRoute != null -> "${snapshot.selectedRoute.vehicle} • ${snapshot.selectedRoute.name}"
      selectedRouteId == RoutesCatalog.UNALLOCATED -> "Unassigned Queue • Needs Route Allocation"
      else -> "All Fleet Vehicles (${routes.size} active routes)"
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    // Compact Top Bar + Standard Route Selection Model
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 0.dp,
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Packing List & Dispatch",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = routeSubtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            HeaderCircleIconButton(
              icon = Icons.Rounded.Print,
              contentDescription = "Print dispatch sheet",
              onClick = {
                val manifest = buildA4ManifestShareText(snapshot, selectedRouteId)
                sharePlainText(context, "SnackRoute Packing List", manifest)
                onShowFeedback("Prepared packing list (${snapshot.totalShopsCount} stops)")
              },
              modifier = Modifier.testTag("packing_header_print_button"),
            )
            HeaderCircleIconButton(
              icon = Icons.Rounded.Share,
              contentDescription = "Share packing manifest",
              onClick = {
                val manifest = buildA4ManifestShareText(snapshot, selectedRouteId)
                sharePlainText(context, "SnackRoute Dispatch Manifest", manifest)
              },
              modifier = Modifier.testTag("packing_header_share_button"),
            )
            HeaderCircleIconButton(
              icon = Icons.Rounded.Sync,
              contentDescription = "Sync and Server Settings",
              onClick = onOpenSyncServers,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.testTag("packing_sync_button"),
            )
          }
        }

        // Standard Route Selection Dropdown shared with Orders & Shops screens
        StandardRouteDropdown(
          selectedRoute = selectedRouteId,
          routes = routes,
          onRouteSelected = { selectedRouteId = it },
          fieldTestTag = "packing_route_filter",
        )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      }
    }

    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .testTag("packing_lazy_list"),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(
        horizontal = 14.dp,
        vertical = 10.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Operational Vehicle, Crew, Payload & 3-Tile KPI Card
      item {
        OperationalDispatchCard(
          snapshot = snapshot,
          selectedRouteId = selectedRouteId,
          todayDisplay = todayHeader,
        )
      }

      // Assigned vs Unassigned Packing Queue Summary Strip
      item {
        AssignedUnassignedStrip(
          assignedOrders = snapshot.assignedOrdersCount,
          assignedPacks = snapshot.assignedPacksCount,
          unassignedOrders = snapshot.unassignedOrdersCount,
          unassignedPacks = snapshot.unassignedPacksCount,
          selectedRouteId = selectedRouteId,
          onSelectAssigned = {
            if (selectedRouteId == RoutesCatalog.UNALLOCATED) {
              selectedRouteId = RoutesCatalog.ALL
            }
            activeTab = PackingTab.SHOP_SLIPS
          },
          onSelectUnassigned = {
            selectedRouteId = RoutesCatalog.UNALLOCATED
            activeTab = PackingTab.SHOP_SLIPS
          },
        )
      }

      // Mode Switcher Segmented Tabs (Consolidated | Shop Slips | A4 Manifest)
      item {
        PackingTabSwitcher(
          activeTab = activeTab,
          skuCount = snapshot.categoryGroups.sumOf { it.items.size },
          slipsCount = snapshot.totalShopsCount,
          onSelectTab = { activeTab = it },
        )
      }

      if (activeTab == PackingTab.CONSOLIDATED && snapshot.shopSlips.any { it.isAssignedToRoute }) {
        item {
          ReverseLoadSequencePreviewCard(
            slips = snapshot.shopSlips.filter { it.isAssignedToRoute },
            selectedRoute = snapshot.selectedRoute,
            reverseLoadOrder = reverseLoadOrder,
            onOpenShopSlips = { activeTab = PackingTab.SHOP_SLIPS },
            onOpenReorderStops = { showReorderStopsSheet = true },
          )
        }
      }

      when (activeTab) {
        PackingTab.CONSOLIDATED -> {
          // Search & Filter Bar
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              StandardCompactSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Filter by snack or pack variant...",
                fieldTestTag = "packing_search_input",
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f),
              )

              Surface(
                color = if (onlyShortages) MaterialTheme.semanticColors.dangerContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                  1.dp,
                  if (onlyShortages) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
                modifier = Modifier
                  .height(42.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { onlyShortages = !onlyShortages }
                  .testTag("packing_shortage_filter_button"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = "Filter shortages",
                    tint = if (onlyShortages) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                  )
                  if (snapshot.shortageItems.isNotEmpty()) {
                    Text(
                      text = "${snapshot.shortageItems.size}",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (onlyShortages) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              }
            }
          }

          // Active Shortage Warning Banner
          if (snapshot.shortageItems.isNotEmpty()) {
            item {
              val topShort = snapshot.shortageItems.first()
              Surface(
                color = MaterialTheme.semanticColors.dangerContainer.copy(alpha = 0.72f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.semanticColors.danger.copy(alpha = 0.35f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("packing_shortage_banner"),
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.semanticColors.danger,
                    modifier = Modifier.size(20.dp),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Shortage Alert: ${topShort.productName} ${topShort.packSize}",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.semanticColors.danger,
                    )
                    Text(
                      text = "Loaded ${topShort.loadedPacks} of ${topShort.orderedPacks} pk (Short ${topShort.shortagePacks} pk) • ${snapshot.shortageItems.size} SKU(s) need production/loading",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
              }
            }
          }

          // Today's Production Target by Packet Size Card (100g, 200g, 500g...)
          if (snapshot.productionBySize.isNotEmpty()) {
            item {
              TodayProductionTargetCard(
                targets = snapshot.productionBySize,
                totalOrdered = snapshot.totalOrderedPacks,
                totalLoaded = snapshot.totalLoadedPacks,
              )
            }
          }

          // Section-by-Section Production & Packing Cards (Banana Chips, Jaggery Banana, etc.)
          if (displayedCategoryGroups.isEmpty()) {
            item {
              EmptyPackingStateCard(
                hasOrders = snapshot.routeFilteredOrders.isNotEmpty(),
                onNavigateToOrders = onNavigateToOrders,
              )
            }
          } else {
            items(
              count = displayedCategoryGroups.size,
              key = { displayedCategoryGroups[it].categoryName },
            ) { idx ->
              val group = displayedCategoryGroups[idx]
              SectionProductionPackingCard(
                group = group,
                onIncrementSku = { sku ->
                  val updated = (sku.loadedPacks + 5).coerceAtMost(sku.orderedPacks + 50)
                  loadedOverrides[sku.productCode] = updated
                  val matchingStock = stocks.firstOrNull { it.productCode.equals(sku.productCode, ignoreCase = true) }
                  if (matchingStock != null && updated > matchingStock.quantity) {
                    onUpdateStock(matchingStock.id, updated, matchingStock.productName)
                  }
                },
                onDecrementSku = { sku ->
                  val updated = (sku.loadedPacks - 5).coerceAtLeast(0)
                  loadedOverrides[sku.productCode] = updated
                },
              )
            }
          }
        }

        PackingTab.SHOP_SLIPS -> {
          item {
            ShopSlipsTabPanel(
              slips = snapshot.shopSlips,
              selectedRoute = snapshot.selectedRoute,
              reverseLoadOrder = reverseLoadOrder,
              onToggleLoadOrder = { reverseLoadOrder = it },
              onOpenReorderStops = { showReorderStopsSheet = true },
              onToggleSlipPacked = { orderId, isPacked ->
                packedOrderOverrides[orderId] = isPacked
                onShowFeedback(if (isPacked) "Marked stop slip Packed" else "Reopened stop slip")
              },
              onPrintSingleSlip = { slip ->
                sharePlainText(
                  context,
                  "Packing Slip - ${slip.order.shopName}",
                  buildSingleSlipShareText(slip),
                )
              },
              onNavigateToRoutes = onNavigateToRoutes,
            )
          }
        }

        PackingTab.A4_MANIFEST -> {
          item {
            A4ManifestTabPanel(
              snapshot = snapshot,
              selectedRouteId = selectedRouteId,
              onShareManifest = {
                sharePlainText(
                  context,
                  "A4 Dispatch Manifest",
                  buildA4ManifestShareText(snapshot, selectedRouteId),
                )
              },
            )
          }
        }
      }
    }

    // Dedicated Sticky Bottom Dispatch Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 0.dp,
      shadowElevation = 8.dp,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = {
              when (activeTab) {
                PackingTab.CONSOLIDATED, PackingTab.A4_MANIFEST -> {
                  val text = buildA4ManifestShareText(snapshot, selectedRouteId)
                  sharePlainText(context, "SnackRoute Packing List", text)
                  onShowFeedback("Sent manifest (${snapshot.totalLoadedPacks}/${snapshot.totalOrderedPacks} pk)")
                }
                PackingTab.SHOP_SLIPS -> {
                  val batchText = snapshot.shopSlips.joinToString("\n\n---\n\n") {
                    buildSingleSlipShareText(it)
                  }
                  if (batchText.isBlank()) {
                    onShowFeedback("No shop slips on this route")
                  } else {
                    sharePlainText(context, "Batch Shop Slips (${snapshot.totalShopsCount})", batchText)
                  }
                }
              }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("packing_primary_print_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Print,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = when (activeTab) {
                PackingTab.CONSOLIDATED -> "Print Vehicle Packing List"
                PackingTab.SHOP_SLIPS -> "Print Batch (${snapshot.totalShopsCount} Slips)"
                PackingTab.A4_MANIFEST -> "Print Full A4 Manifest"
              },
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          OutlinedButton(
            onClick = { activeTab = PackingTab.SHOP_SLIPS },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .height(44.dp)
              .testTag("packing_all_slips_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Description,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "All ${snapshot.totalShopsCount} Slips",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.semanticColors.success),
            )
            Text(
              text = "${snapshot.selectedRoute?.stagingBay ?: "Central Hub"} • ${snapshot.routeFilteredOrders.size} Active Order(s)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = if (snapshot.shortageItems.isEmpty() && snapshot.totalOrderedPacks > 0) "Ready for Dispatch" else "Verify Load",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (snapshot.shortageItems.isEmpty() && snapshot.totalOrderedPacks > 0) MaterialTheme.semanticColors.success else MaterialTheme.semanticColors.warning,
          )
        }
      }
    }
  }

  val routeForReordering = snapshot.selectedRoute
  if (showReorderStopsSheet && routeForReordering != null) {
    val routeShops = remember(shops, routeForReordering) {
      RoutesCatalog.sortShopsByRouteSequence(
        shops.filter { it.routeId == routeForReordering.id },
        routeForReordering,
      )
    }
    ReorderRouteStopsSheet(
      route = routeForReordering,
      orderedShops = routeShops,
      onDismiss = { showReorderStopsSheet = false },
      onSaveOrder = { newStopOrderIds ->
        onSaveRoute(
          routeForReordering.copy(stopOrderIds = newStopOrderIds),
          "Updated ${routeForReordering.name} delivery stop order",
        )
        showReorderStopsSheet = false
      },
    )
  }
}

@Composable
private fun HeaderCircleIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tint: Color = MaterialTheme.colorScheme.onSurface,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    shape = CircleShape,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .size(36.dp)
      .clip(CircleShape)
      .clickable(onClick = onClick),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(19.dp),
      )
    }
  }
}

@Composable
private fun OperationalDispatchCard(
  snapshot: PackingDashboardSnapshot,
  selectedRouteId: String,
  todayDisplay: String,
) {
  val route = snapshot.selectedRoute
  val routeSpec = route?.let { RouteColorPalette.specForIndex(it.colorIndex) }
  val vehicleTitle = route?.vehicle?.substringBefore(" (") ?: when (selectedRouteId) {
    RoutesCatalog.UNALLOCATED -> "Unassigned Orders Queue"
    else -> "Combined Dispatch Fleet"
  }
  val regPill = route?.vehicle?.substringAfter("(", "")?.substringBefore(")")
    ?.takeIf { it.isNotBlank() } ?: if (route != null) route.id else "ALL ROUTES"

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("packing_operational_banner"),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = routeSpec?.primary ?: MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.LocalShipping,
                contentDescription = null,
                tint = routeSpec?.contrastInk ?: MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(21.dp),
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = vehicleTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  text = regPill,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                )
              }
            }
            Text(
              text = route?.stagingBay ?: "Central Warehouse Hub",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = todayDisplay,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = route?.dispatchWindow?.substringBefore("–")?.trim()?.let { "Depart $it" }
              ?: "Same-day",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Crew Details Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = "Driver: ${route?.driverName ?: "All Route Drivers"}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        if (route != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            RouteColorDot(spec = routeSpec!!, size = 10.dp)
            Text(
              text = route.driverPhone,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Payload Weight & Progress Bar
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Scale,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = String.format(
                Locale.US,
                "Payload Weight: %.1f kg / %.0f kg",
                snapshot.payloadWeightKg,
                snapshot.maxPayloadKg,
              ),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Text(
            text = "${snapshot.payloadPercent}% Payload",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        LinearProgressIndicator(
          progress = { snapshot.payloadPercent / 100f },
          color = routeSpec?.primary ?: MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp)),
        )
      }

      // 3 KPI Summary Tiles matching image: Shops Packed | Total SKU Units | Staged Crates
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        KpiMiniTile(
          label = "Shops Packed",
          value = "${snapshot.shopsPackedCount} / ${snapshot.totalShopsCount}",
          valueColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
        KpiMiniTile(
          label = "Total SKU Units",
          value = "${snapshot.totalOrderedPacks} pk",
          valueColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        KpiMiniTile(
          label = "Staged Crates",
          value = "${snapshot.stagedCratesCount} Crates",
          valueColor = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun KpiMiniTile(
  label: String,
  value: String,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = valueColor,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun AssignedUnassignedStrip(
  assignedOrders: Int,
  assignedPacks: Int,
  unassignedOrders: Int,
  unassignedPacks: Int,
  selectedRouteId: String,
  onSelectAssigned: () -> Unit,
  onSelectUnassigned: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("assigned_unassigned_section"),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Surface(
      color = if (selectedRouteId != RoutesCatalog.UNALLOCATED) MaterialTheme.semanticColors.successContainer else MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, MaterialTheme.semanticColors.success.copy(alpha = 0.35f)),
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onSelectAssigned)
        .testTag("assigned_packing_pill"),
    ) {
      Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(
          text = "Assigned Packing",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.semanticColors.success,
        )
        Text(
          text = "$assignedOrders orders • $assignedPacks pk",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    Surface(
      color = if (selectedRouteId == RoutesCatalog.UNALLOCATED) MaterialTheme.semanticColors.dangerContainer else MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(
        1.dp,
        if (unassignedOrders > 0) MaterialTheme.semanticColors.danger.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
      ),
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onSelectUnassigned)
        .testTag("unassigned_packing_pill"),
    ) {
      Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(
          text = "Unassigned Packing",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (unassignedOrders > 0) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "$unassignedOrders orders • $unassignedPacks pk",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun PackingTabSwitcher(
  activeTab: PackingTab,
  skuCount: Int,
  slipsCount: Int,
  onSelectTab: (PackingTab) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      PackingTab.entries.forEach { tab ->
        val selected = activeTab == tab
        val badgeText = when (tab) {
          PackingTab.CONSOLIDATED -> "$skuCount"
          PackingTab.SHOP_SLIPS -> "$slipsCount"
          PackingTab.A4_MANIFEST -> null
        }
        Surface(
          color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
          shape = RoundedCornerShape(9.dp),
          border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) else null,
          shadowElevation = if (selected) 2.dp else 0.dp,
          modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable { onSelectTab(tab) }
            .testTag("packing_tab_${tab.name.lowercase()}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = tab.icon,
              contentDescription = null,
              tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = tab.label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (badgeText != null) {
              Spacer(modifier = Modifier.width(4.dp))
              Surface(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  text = badgeText,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  fontWeight = FontWeight.Bold,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TodayProductionTargetCard(
  targets: List<PacketSizeTarget>,
  totalOrdered: Int,
  totalLoaded: Int,
) {
  val totalToPrepare = (totalOrdered - totalLoaded).coerceAtLeast(0)
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("production_target_by_size_card"),
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
            imageVector = Icons.Rounded.Factory,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Today's Production Target",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Surface(
          color = if (totalToPrepare > 0) MaterialTheme.semanticColors.warningContainer else MaterialTheme.semanticColors.successContainer,
          shape = RoundedCornerShape(999.dp),
        ) {
          Text(
            text = if (totalToPrepare > 0) "Prepare $totalToPrepare pk" else "Target Ready",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (totalToPrepare > 0) MaterialTheme.semanticColors.warning else MaterialTheme.semanticColors.success,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        targets.forEach { target ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
              1.dp,
              if (target.toPreparePacks > 0) MaterialTheme.semanticColors.warning.copy(alpha = 0.55f)
              else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            ),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = "${target.packSize} Packs",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "${target.orderedPacks} pk required",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = if (target.toPreparePacks > 0) {
                  "Prep ${target.toPreparePacks} pk (${target.loadedPacks} loaded)"
                } else {
                  "All ${target.loadedPacks} pk ready"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (target.toPreparePacks > 0) MaterialTheme.semanticColors.warning else MaterialTheme.semanticColors.success,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionProductionPackingCard(
  group: PackingCategoryGroup,
  onIncrementSku: (PackingSkuLine) -> Unit,
  onDecrementSku: (PackingSkuLine) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("packing_section_${group.categoryName.lowercase().replace(" ", "_")}"),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Section Header Band matching image (Dot + Category Title + Loaded/Ordered (%))
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                  if (group.completionPercent >= 100) MaterialTheme.colorScheme.primary
                  else MaterialTheme.semanticColors.warning,
                ),
            )
            Text(
              text = group.categoryName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = "${group.totalLoadedPacks} / ${group.totalOrderedPacks} pk (${group.completionPercent}%)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (group.completionPercent >= 100) MaterialTheme.colorScheme.primary else MaterialTheme.semanticColors.warning,
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      group.items.forEachIndexed { idx, sku ->
        if (idx > 0) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
        }
        PackingSkuVariantRow(
          sku = sku,
          onIncrement = { onIncrementSku(sku) },
          onDecrement = { onDecrementSku(sku) },
        )
      }
    }
  }
}

@Composable
private fun PackingSkuVariantRow(
  sku: PackingSkuLine,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
) {
  val isShort = sku.shortagePacks > 0
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = "${sku.packSize} ${sku.productName}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (isShort) {
            Surface(color = MaterialTheme.semanticColors.dangerContainer, shape = RoundedCornerShape(999.dp)) {
              Text(
                text = "Short: ${sku.shortagePacks} pk",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.semanticColors.danger,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
          } else {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(999.dp)) {
              Text(
                text = sku.productCode,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        Text(
          text = "Allocated to ${sku.allocatedDropCount} retail drop(s) • Stock: ${sku.warehouseStockPacks} pk",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Stepper / Completed Pill matching attached reference image
      if (!isShort) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = MaterialTheme.semanticColors.successContainer,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .clickable(onClick = onDecrement),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.semanticColors.success,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "${sku.loadedPacks} / ${sku.orderedPacks} pk",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.semanticColors.success,
              )
            }
          }
        }
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .clickable(onClick = onDecrement),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Remove,
                  contentDescription = "Decrease loaded quantity",
                  modifier = Modifier.size(16.dp),
                )
              }
              Text(
                text = sku.loadedPacks.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .clickable(onClick = onIncrement),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = "Increase loaded quantity",
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
          Text(
            text = "/ ${sku.orderedPacks}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    LinearProgressIndicator(
      progress = { sku.progressFraction },
      color = if (isShort) MaterialTheme.semanticColors.danger else MaterialTheme.semanticColors.success,
      trackColor = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
        .clip(RoundedCornerShape(999.dp)),
    )
  }
}

@Composable
private fun EmptyPackingStateCard(
  hasOrders: Boolean,
  onNavigateToOrders: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Inventory2,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(30.dp),
      )
      Text(
        text = if (hasOrders) "No SKUs match your search or shortage filter" else "No orders queued on this route",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "Select another route above or confirm shop orders to populate packing quantities.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      OutlinedButton(onClick = onNavigateToOrders, shape = RoundedCornerShape(10.dp)) {
        Text("Go to Orders")
      }
    }
  }
}

private fun sharePlainText(context: Context, title: String, content: String) {
  val sendIntent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, title)
    putExtra(Intent.EXTRA_TEXT, content)
  }
  runCatching {
    context.startActivity(Intent.createChooser(sendIntent, title))
  }
}

@Composable
private fun ReverseLoadSequencePreviewCard(
  slips: List<ShopPackingSlip>,
  selectedRoute: DeliveryRoute?,
  reverseLoadOrder: Boolean,
  onOpenShopSlips: () -> Unit,
  onOpenReorderStops: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("packing_reverse_load_preview"),
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
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Rounded.SwapVert,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = if (reverseLoadOrder) "Vehicle Pack Sequence (Last Stop → #1)" else "Delivery Sequence (Stop #1 → Last)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpenShopSlips)
            .testTag("packing_open_shop_packages_cta"),
        ) {
          Text(
            text = "Open Packages (${slips.size})",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        slips.take(4).forEach { slip ->
          val qty = slip.items.sumOf { it.quantity }.takeIf { it > 0 } ?: slip.order.itemCount
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
              1.dp,
              if (slip.packOrderNumber == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
              else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onOpenShopSlips),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = "${slip.packageLabel} • Stop #${slip.deliveryStopNumber}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "${slip.order.shopName.take(18)} ($qty pk)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
              )
            }
          }
        }
      }

      if (selectedRoute != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Pack #1 goes inside truck first so Stop #1 comes out first.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Reorder 1..N",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable(onClick = onOpenReorderStops)
              .padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReorderRouteStopsSheet(
  route: DeliveryRoute,
  orderedShops: List<Shop>,
  onDismiss: () -> Unit,
  onSaveOrder: (List<String>) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val currentOrder = remember(route.id, orderedShops) {
    mutableStateListOf<Shop>().apply { addAll(orderedShops) }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Delivery Stop Sequence (1 → ${currentOrder.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${route.name} • Last stop packs first as Pkg #1",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = "Close stop reorder",
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onDismiss)
            .padding(4.dp),
        )
      }

      if (currentOrder.isEmpty()) {
        Text(
          text = "No shops assigned to this route yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 12.dp),
        )
      } else {
        currentOrder.forEachIndexed { idx, shop ->
          val deliveryStop = idx + 1
          val packNum = currentOrder.size - idx
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(28.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "$deliveryStop",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                }
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = shop.name,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${shop.shopCode} • Packs as Pkg #$packNum",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .size(34.dp)
                  .clip(CircleShape)
                  .clickable(enabled = idx > 0) {
                    val moved = currentOrder.removeAt(idx)
                    currentOrder.add(idx - 1, moved)
                  }
                  .testTag("reorder_up_${shop.id}"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Move ${shop.name} earlier",
                    tint = if (idx > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                  )
                }
              }

              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .size(34.dp)
                  .clip(CircleShape)
                  .clickable(enabled = idx < currentOrder.lastIndex) {
                    val moved = currentOrder.removeAt(idx)
                    currentOrder.add(idx + 1, moved)
                  }
                  .testTag("reorder_down_${shop.id}"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Move ${shop.name} later",
                    tint = if (idx < currentOrder.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                  )
                }
              }
            }
          }
        }
      }

      Button(
        onClick = { onSaveOrder(currentOrder.map { it.id }) },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("save_reordered_stops_button"),
      ) {
        Text(
          text = "Save Stop Order & Update Packing",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
