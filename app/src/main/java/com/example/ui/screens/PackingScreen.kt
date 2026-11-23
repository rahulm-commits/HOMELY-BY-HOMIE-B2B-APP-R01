package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.ui.theme.RouteColorDot
import com.example.ui.theme.RouteColorPalette
import com.example.ui.theme.semanticColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PackingStatusFilter(val label: String) {
  ALL("All"),
  PENDING("Pending"),
  PACKED("Packed"),
}

@OptIn(ExperimentalFoundationApi::class)
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
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by rememberSaveable { mutableStateOf(PackingStatusFilter.ALL) }
  var reverseLoadOrder by rememberSaveable { mutableStateOf(false) }
  var showReorderStopsSheet by remember { mutableStateOf(false) }
  var showEditVehicleSheet by remember { mutableStateOf(false) }

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

  val saveExcelLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(XLSX_MIME_TYPE),
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
          stream.write(buildPackingExcelBytes(snapshot, selectedRouteId))
        } ?: error("Unable to open output stream")
      }.onSuccess {
        onShowFeedback("Saved Excel packing list (.xlsx)")
      }.onFailure { err ->
        onShowFeedback("Save failed: ${err.message ?: "unknown error"}")
      }
    }
  }

  val allPacketLines = remember(snapshot.categoryGroups) {
    snapshot.categoryGroups.flatMap { it.items }
  }

  val visibleSlips = remember(snapshot.shopSlips, searchQuery, statusFilter) {
    val q = searchQuery.trim().lowercase(Locale.US)
    snapshot.shopSlips.filter { slip ->
      val matchesStatus = when (statusFilter) {
        PackingStatusFilter.ALL -> true
        PackingStatusFilter.PENDING -> !slip.isPacked
        PackingStatusFilter.PACKED -> slip.isPacked
      }
      val matchesQuery = q.isEmpty() ||
        slip.order.shopName.lowercase(Locale.US).contains(q) ||
        slip.order.shopCode.lowercase(Locale.US).contains(q) ||
        slip.order.orderCode.lowercase(Locale.US).contains(q) ||
        slip.order.itemsSummary.lowercase(Locale.US).contains(q) ||
        slip.items.any {
          it.productName.lowercase(Locale.US).contains(q) ||
            it.packSize.lowercase(Locale.US).contains(q) ||
            it.productCode.lowercase(Locale.US).contains(q)
        }
      matchesStatus && matchesQuery
    }
  }

  val pendingCount = remember(snapshot.shopSlips) { snapshot.shopSlips.count { !it.isPacked } }
  val packedCount = remember(snapshot.shopSlips) { snapshot.shopSlips.count { it.isPacked } }

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

  val triggerExcelExport: (preferOpenViewer: Boolean) -> Unit = { preferViewer ->
    exportPackingExcel(
      context = context,
      snapshot = snapshot,
      selectedRouteId = selectedRouteId,
      preferOpenViewer = preferViewer,
    ).onSuccess { file ->
      onShowFeedback("Excel sheet ready: ${file.name}")
    }.onFailure { err ->
      onShowFeedback("Excel export failed: ${err.message ?: "error"}")
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .testTag("packing_lazy_list"),
      contentPadding = PaddingValues(bottom = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // 1. Scrollable Top Bar + Route Filter
      item(key = "packing_top_header") {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 0.dp,
          shadowElevation = 1.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Packing List",
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
                    contentDescription = "Print or open Excel sheet",
                    onClick = { triggerExcelExport(true) },
                    modifier = Modifier.testTag("packing_header_print_button"),
                  )
                  HeaderCircleIconButton(
                    icon = Icons.Rounded.Share,
                    contentDescription = "Share Excel packing list",
                    onClick = { triggerExcelExport(false) },
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
      }

      // 2. Existing Vehicle Details Card
      item(key = "packing_operational_dispatch") {
        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
          OperationalDispatchCard(
            snapshot = snapshot,
            selectedRouteId = selectedRouteId,
            todayDisplay = todayHeader,
            onEditVehicleClick = if (snapshot.selectedRoute != null) {
              { showEditVehicleSheet = true }
            } else {
              null
            },
          )
        }
      }

      // 3. Sticky Search Bar + Filter Toggles
      stickyHeader(key = "packing_sticky_controls") {
        Surface(
          color = MaterialTheme.colorScheme.background,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 5.dp),
              verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              StandardCompactSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search shop, order, or packet...",
                fieldTestTag = "packing_search_input",
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
              )

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                PackingStatusFilter.entries.forEach { filter ->
                  val count = when (filter) {
                    PackingStatusFilter.ALL -> snapshot.shopSlips.size
                    PackingStatusFilter.PENDING -> pendingCount
                    PackingStatusFilter.PACKED -> packedCount
                  }
                  val selected = statusFilter == filter
                  FilterChip(
                    selected = selected,
                    onClick = { statusFilter = filter },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primary,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                      containerColor = MaterialTheme.colorScheme.surface,
                      labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                      enabled = true,
                      selected = selected,
                      borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                      selectedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                    label = {
                      Text(
                        text = "${filter.label} ($count)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                      )
                    },
                    modifier = Modifier
                      .height(32.dp)
                      .testTag("packing_filter_${filter.name.lowercase()}"),
                  )
                }

                FilterChip(
                  selected = reverseLoadOrder,
                  onClick = { reverseLoadOrder = !reverseLoadOrder },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Rounded.SwapVert,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                    )
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
                  border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = reverseLoadOrder,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                  ),
                  label = {
                    Text(
                      text = if (reverseLoadOrder) "Reverse Load" else "Stop #1 First",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                    )
                  },
                  modifier = Modifier
                    .height(32.dp)
                    .testTag("packing_order_toggle"),
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          }
        }
      }

      // 4. Overall Count of Different Packets
      item(key = "packing_overall_packet_counts") {
        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
          OverallPacketCountCard(
            sizeTargets = snapshot.productionBySize,
            packetLines = allPacketLines,
            totalPackets = allPacketLines.sumOf { it.orderedPacks },
          )
        }
      }

      // 5. Shop-by-Shop Order Details Header
      item(key = "packing_shops_section_header") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Storefront,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "Shop Order Details (${visibleSlips.size})",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (snapshot.selectedRoute != null && snapshot.shopSlips.size > 1) {
              Text(
                text = "Reorder Stops",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .clickable { showReorderStopsSheet = true }
                  .padding(horizontal = 6.dp, vertical = 3.dp)
                  .testTag("packing_reorder_stops_button"),
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = "$packedCount / ${snapshot.shopSlips.size} Packed",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }
      }

      // 6. Shop Order Cards
      if (visibleSlips.isEmpty()) {
        item(key = "empty_packing_card", contentType = "empty_state") {
          Box(modifier = Modifier.padding(horizontal = 12.dp)) {
            EmptyPackingStateCard(
              hasOrders = snapshot.routeFilteredOrders.isNotEmpty(),
              onNavigateToOrders = onNavigateToOrders,
              onClearFilters = if (searchQuery.isNotBlank() || statusFilter != PackingStatusFilter.ALL) {
                {
                  searchQuery = ""
                  statusFilter = PackingStatusFilter.ALL
                }
              } else {
                null
              },
            )
          }
        }
      } else {
        itemsIndexed(
          items = visibleSlips,
          key = { _, slip -> slip.order.id.ifBlank { slip.order.orderCode } },
          contentType = { _, _ -> "shop_packing_order_card" },
        ) { idx, slip ->
          Box(modifier = Modifier.padding(horizontal = 12.dp)) {
            SimpleShopPackingOrderCard(
              displayIndex = idx + 1,
              slip = slip,
              onTogglePacked = {
                val next = !slip.isPacked
                packedOrderOverrides[slip.order.id] = next
                onShowFeedback(if (next) "${slip.order.shopName} marked Packed" else "${slip.order.shopName} marked Pending")
              },
            )
          }
        }
      }
    }

    // Bottom Print / Excel Export Bar
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
          .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = { triggerExcelExport(true) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
              .testTag("packing_primary_print_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.TableChart,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Print / Export Excel (.xlsx)",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          OutlinedButton(
            onClick = {
              saveExcelLauncher.launch(buildPackingExcelFileName(snapshot, selectedRouteId))
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .height(46.dp)
              .testTag("packing_save_excel_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Download,
              contentDescription = null,
              modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Save .xlsx",
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
              text = "${snapshot.selectedRoute?.stagingBay ?: "Central Hub"} • ${snapshot.totalShopsCount} Shop(s)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = "${snapshot.totalOrderedPacks} Total Packets • Excel Ready",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
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

  if (showEditVehicleSheet && routeForReordering != null) {
    val usedColors = remember(routes, routeForReordering) {
      routes.filter { it.id != routeForReordering.id }.map { it.colorIndex }.toSet()
    }
    val assignedCrates = remember(shops, routeForReordering) {
      shops.count { it.routeId == routeForReordering.id }
    }
    EditRouteBottomSheet(
      initialRoute = routeForReordering,
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

@Composable
private fun OverallPacketCountCard(
  sizeTargets: List<PacketSizeTarget>,
  packetLines: List<PackingSkuLine>,
  totalPackets: Int,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("packing_overall_counts_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(11.dp),
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
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Overall Packet Count",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(999.dp),
        ) {
          Text(
            text = "Total: $totalPackets pk",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
          )
        }
      }

      if (sizeTargets.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          sizeTargets.forEach { target ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Text(
                  text = target.packSize,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "${target.orderedPacks} pk",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }

      if (packetLines.isEmpty()) {
        Text(
          text = "No packets match your current route or search.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
          packetLines.forEachIndexed { idx, line ->
            if (idx > 0) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = line.productName,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false),
                )
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(6.dp),
                ) {
                  Text(
                    text = line.packSize,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
              Text(
                text = "${line.orderedPacks} pk",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SimpleShopPackingOrderCard(
  displayIndex: Int,
  slip: ShopPackingSlip,
  onTogglePacked: () -> Unit,
) {
  val totalUnits = slip.items.sumOf { it.quantity }.takeIf { it > 0 } ?: slip.order.itemCount
  val routeNameShort = slip.route?.name?.substringBefore(":") ?: "Unassigned"

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(
      width = if (slip.isPacked) 1.3.dp else 1.dp,
      color = if (slip.isPacked) {
        MaterialTheme.semanticColors.success.copy(alpha = 0.55f)
      } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
      },
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("packing_shop_card_${slip.order.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(11.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(
                if (slip.isPacked) MaterialTheme.semanticColors.success
                else MaterialTheme.colorScheme.primary,
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (slip.deliveryStopNumber > 0) "${slip.deliveryStopNumber}" else "$displayIndex",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = slip.order.shopName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${slip.order.orderCode} • $routeNameShort • ${slip.order.orderDate}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = "$totalUnits pk",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }

          Surface(
            color = if (slip.isPacked) MaterialTheme.semanticColors.successContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .clickable(onClick = onTogglePacked)
              .testTag("packing_toggle_packed_${slip.order.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = if (slip.isPacked) Icons.Rounded.Check else Icons.Rounded.HourglassTop,
                contentDescription = if (slip.isPacked) "Packed" else "Pending",
                tint = if (slip.isPacked) MaterialTheme.semanticColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = if (slip.isPacked) "Packed" else "Pending",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (slip.isPacked) MaterialTheme.semanticColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Clean Packet & Quantity Rows for this Shop
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (slip.items.isNotEmpty()) {
            slip.items.forEachIndexed { idx, line ->
              if (idx > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "${line.productName} (${line.packSize})",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = "${line.quantity} pk",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = slip.order.itemsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = "${slip.order.itemCount} pk",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }

      if (slip.order.note.isNotBlank()) {
        Text(
          text = "Note: ${slip.order.note}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
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
  onEditVehicleClick: (() -> Unit)? = null,
) {
  val route = snapshot.selectedRoute
  val routeSpec = route?.let { RouteColorPalette.specForIndex(it.colorIndex) }
  val parsedVehicle = remember(route?.vehicle) {
    route?.vehicle?.let { parseVehicleSummary(it) }
  }
  val vehicleTitle = parsedVehicle?.modelName ?: when (selectedRouteId) {
    RoutesCatalog.UNALLOCATED -> "Unassigned Orders Queue"
    else -> "Combined Dispatch Fleet"
  }
  val regPill = parsedVehicle?.plateNumber
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
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp),
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
                modifier = Modifier.weight(1f, fill = false),
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
            val bayAndVehicleDetails = buildString {
              append(route?.stagingBay ?: "Central Warehouse Hub")
              if (!parsedVehicle?.extraDetails.isNullOrBlank()) {
                append(" • ")
                append(parsedVehicle?.extraDetails)
              }
            }
            Text(
              text = bayAndVehicleDetails,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (onEditVehicleClick != null) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
              shape = CircleShape,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onEditVehicleClick)
                .testTag("packing_edit_vehicle_button"),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.Edit,
                  contentDescription = "Edit vehicle and route details",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(17.dp),
                )
              }
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

      // 3 KPI Summary Tiles
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
          label = "Total Packets",
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
private fun EmptyPackingStateCard(
  hasOrders: Boolean,
  onNavigateToOrders: () -> Unit,
  onClearFilters: (() -> Unit)? = null,
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
        text = if (hasOrders) "No shop orders match your filter" else "No orders queued on this route",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "Select another route above or confirm shop orders to populate packing quantities.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onClearFilters != null) {
          Button(onClick = onClearFilters, shape = RoundedCornerShape(10.dp)) {
            Text("Reset Filters")
          }
        }
        OutlinedButton(onClick = onNavigateToOrders, shape = RoundedCornerShape(10.dp)) {
          Text("Go to Orders")
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
        .padding(horizontal = 14.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp),
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
            text = route.name,
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
                  text = shop.shopCode,
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
