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
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.material.icons.rounded.RemoveCircleOutline
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.SnackCatalog
import com.example.data.model.StockAlertHelper
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
  STOCK_HOLD("Stock Hold"),
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
  catalogProducts: List<CatalogProduct> = SnackCatalog.products,
  packedOrderOverrides: Map<String, Boolean>? = null,
  onUpdatePackedStatus: ((orderId: String, shopName: String, isPacked: Boolean) -> Unit)? = null,
  onMarkAllPackingComplete: ((List<OrderRecord>) -> Unit)? = null,
  onUpdateOrder: ((order: OrderRecord, items: List<Pair<CatalogProduct, Int>>, note: String, onSaved: () -> Unit) -> Unit)? = null,
  onOpenFullOrderEditor: ((OrderRecord) -> Unit)? = null,
  stockPendingReasons: Map<String, String>? = null,
  onMarkOrderStockPending: ((orderId: String, shopName: String, reason: String) -> Unit)? = null,
  onClearOrderStockPending: ((orderId: String, shopName: String) -> Unit)? = null,
  products: List<Product> = emptyList(),
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var selectedRouteId by rememberSaveable { mutableStateOf(RoutesCatalog.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by rememberSaveable { mutableStateOf(PackingStatusFilter.ALL) }
  var reverseLoadOrder by rememberSaveable { mutableStateOf(false) }
  var showReorderStopsSheet by remember { mutableStateOf(false) }
  var showEditVehicleSheet by remember { mutableStateOf(false) }
  var editingOrderId by remember { mutableStateOf<String?>(null) }
  var confirmRemovePackedOrderId by remember { mutableStateOf<String?>(null) }
  var stockPendingDialogOrderId by remember { mutableStateOf<String?>(null) }

  val loadedOverrides = remember { mutableStateMapOf<String, Int>() }
  val localPackedOverrides = remember { mutableStateMapOf<String, Boolean>() }
  val localStockPendingReasons = remember { mutableStateMapOf<String, String>() }
  val effectivePackedOverrides = remember(packedOrderOverrides, localPackedOverrides.toMap()) {
    packedOrderOverrides ?: localPackedOverrides.toMap()
  }
  val effectiveStockPendingReasons = remember(stockPendingReasons, localStockPendingReasons.toMap()) {
    stockPendingReasons ?: localStockPendingReasons.toMap()
  }

  val setOrderPackedState: (OrderRecord, Boolean) -> Unit = { order, nextPacked ->
    if (onUpdatePackedStatus != null) {
      onUpdatePackedStatus(order.id, order.shopName, nextPacked)
    } else {
      localPackedOverrides[order.id] = nextPacked
      localStockPendingReasons.remove(order.id)
      onShowFeedback(
        if (nextPacked) "${order.shopName} marked Packing Complete"
        else "Removed ${order.shopName} from Packed"
      )
    }
  }

  val markOrderStockPendingState: (OrderRecord, String) -> Unit = { order, reason ->
    val cleanReason = reason.trim().ifBlank { "Stock unavailable" }
    if (onMarkOrderStockPending != null) {
      onMarkOrderStockPending(order.id, order.shopName, cleanReason)
    } else {
      localPackedOverrides[order.id] = false
      localStockPendingReasons[order.id] = cleanReason
      onShowFeedback("${order.shopName} marked pending ($cleanReason) • Next order unlocked")
    }
  }

  val clearOrderStockPendingState: (OrderRecord) -> Unit = { order ->
    if (onClearOrderStockPending != null) {
      onClearOrderStockPending(order.id, order.shopName)
    } else {
      localStockPendingReasons.remove(order.id)
      onShowFeedback("${order.shopName} returned to packing sequence")
    }
  }

  val snapshot = remember(
    selectedRouteId,
    shops,
    orders,
    routes,
    stocks,
    loadedOverrides.toMap(),
    effectivePackedOverrides,
    effectiveStockPendingReasons,
    searchQuery,
    reverseLoadOrder,
    catalogProducts,
    products,
  ) {
    buildPackingSnapshot(
      selectedRouteId = selectedRouteId,
      shops = shops,
      orders = orders,
      routes = routes,
      stocks = stocks,
      loadedOverrides = loadedOverrides,
      packedOrderOverrides = effectivePackedOverrides,
      searchQuery = searchQuery,
      reverseLoadOrder = reverseLoadOrder,
      catalogProducts = catalogProducts,
      stockPendingReasons = effectiveStockPendingReasons,
      products = products,
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
        PackingStatusFilter.STOCK_HOLD -> !slip.isPacked && slip.isStockPending
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
  val stockPendingCount = remember(snapshot.shopSlips) { snapshot.shopSlips.count { it.isStockPending } }
  val nextInSequenceSlip = remember(snapshot.shopSlips) {
    snapshot.shopSlips.firstOrNull { it.isNextInSequence }
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
                val visibleFilters = remember(stockPendingCount, statusFilter) {
                  PackingStatusFilter.entries.filter { filter ->
                    filter != PackingStatusFilter.STOCK_HOLD ||
                      stockPendingCount > 0 ||
                      statusFilter == PackingStatusFilter.STOCK_HOLD
                  }
                }
                visibleFilters.forEach { filter ->
                  val count = when (filter) {
                    PackingStatusFilter.ALL -> snapshot.shopSlips.size
                    PackingStatusFilter.PENDING -> pendingCount
                    PackingStatusFilter.STOCK_HOLD -> stockPendingCount
                    PackingStatusFilter.PACKED -> packedCount
                  }
                  val selected = statusFilter == filter
                  FilterChip(
                    selected = selected,
                    onClick = { statusFilter = filter },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = if (filter == PackingStatusFilter.STOCK_HOLD) {
                        MaterialTheme.semanticColors.warning
                      } else {
                        MaterialTheme.colorScheme.primary
                      },
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                      containerColor = MaterialTheme.colorScheme.surface,
                      labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                      enabled = true,
                      selected = selected,
                      borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                      selectedBorderColor = if (filter == PackingStatusFilter.STOCK_HOLD) {
                        MaterialTheme.semanticColors.warning
                      } else {
                        MaterialTheme.colorScheme.primary
                      },
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

      // 5.5 Sequential Packing Guidance / Packed Section Hint
      if (statusFilter != PackingStatusFilter.PACKED && nextInSequenceSlip != null) {
        item(key = "packing_sequential_queue_strip") {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp)
              .testTag("packing_sequential_banner"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Text(
                  text = "Next in Sequence: ${nextInSequenceSlip.packageLabel} • ${nextInSequenceSlip.order.shopName}",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = if (stockPendingCount > 0) {
                    "Complete in sequence or mark pending if stock is short • $stockPendingCount on stock hold"
                  } else {
                    "Packing Complete follows sequence order. Use Pending & Next if stock is unavailable."
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (stockPendingCount > 0 && statusFilter != PackingStatusFilter.STOCK_HOLD) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  color = MaterialTheme.semanticColors.warningContainer,
                  shape = RoundedCornerShape(999.dp),
                  modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { statusFilter = PackingStatusFilter.STOCK_HOLD }
                    .testTag("packing_banner_stock_hold_chip"),
                ) {
                  Text(
                    text = "$stockPendingCount Hold",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.semanticColors.warning,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                  )
                }
              }
            }
          }
        }
      } else if (statusFilter != PackingStatusFilter.PACKED && stockPendingCount > 0) {
        item(key = "packing_stock_hold_summary_strip") {
          Surface(
            color = MaterialTheme.semanticColors.warningContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.semanticColors.warning.copy(alpha = 0.35f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.semanticColors.warning,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Available orders packed! $stockPendingCount order(s) remain on stock hold until material arrives.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      } else if (statusFilter == PackingStatusFilter.PACKED && visibleSlips.isNotEmpty()) {
        item(key = "packing_packed_section_hint") {
          Surface(
            color = MaterialTheme.semanticColors.successContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.semanticColors.success.copy(alpha = 0.28f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.semanticColors.success,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Packed Section (${visibleSlips.size}) • Mistakenly added? Tap \"Remove from Packed\" to confirm removal.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
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
              onMarkPacked = {
                setOrderPackedState(slip.order, true)
              },
              onRequestRemoveFromPacked = {
                confirmRemovePackedOrderId = slip.order.id
              },
              onRequestStockHoldNext = {
                stockPendingDialogOrderId = slip.order.id
              },
              onClearStockHold = {
                clearOrderStockPendingState(slip.order)
              },
              onSequenceLockedTap = {
                val blockingPkg = slip.blockingPackOrderNumber ?: 1
                val blockingShop = slip.blockingShopName ?: "earlier order"
                onShowFeedback("Sequence locked: Complete or hold Pkg #$blockingPkg ($blockingShop) first")
              },
              onEditClick = {
                editingOrderId = slip.order.id
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

  val activeEditSlip = remember(editingOrderId, snapshot.shopSlips) {
    editingOrderId?.let { id -> snapshot.shopSlips.firstOrNull { it.order.id == id } }
  }
  if (activeEditSlip != null) {
    QuickEditPackingOrderSheet(
      slip = activeEditSlip,
      catalogProducts = catalogProducts,
      stocks = stocks,
      onDismiss = { editingOrderId = null },
      onSaveOrderAndPacking = { updatedItems, updatedNote, nextPacked, stockHoldReason ->
        val applyStatusAfterSave = {
          when {
            stockHoldReason != null -> markOrderStockPendingState(activeEditSlip.order, stockHoldReason)
            nextPacked != activeEditSlip.isPacked -> setOrderPackedState(activeEditSlip.order, nextPacked)
            activeEditSlip.isStockPending -> clearOrderStockPendingState(activeEditSlip.order)
          }
          editingOrderId = null
        }
        if (onUpdateOrder != null) {
          onUpdateOrder(activeEditSlip.order, updatedItems, updatedNote) {
            applyStatusAfterSave()
          }
        } else {
          applyStatusAfterSave()
        }
      },
      onTogglePackedOnly = { nextPacked, stockHoldReason ->
        when {
          stockHoldReason != null -> markOrderStockPendingState(activeEditSlip.order, stockHoldReason)
          nextPacked != activeEditSlip.isPacked -> setOrderPackedState(activeEditSlip.order, nextPacked)
          activeEditSlip.isStockPending -> clearOrderStockPendingState(activeEditSlip.order)
        }
        editingOrderId = null
      },
      onOpenFullEditor = onOpenFullOrderEditor?.let { openFull ->
        {
          editingOrderId = null
          openFull(activeEditSlip.order)
        }
      },
    )
  }

  val confirmRemoveSlip = remember(confirmRemovePackedOrderId, snapshot.shopSlips) {
    confirmRemovePackedOrderId?.let { id ->
      snapshot.shopSlips.firstOrNull { it.order.id == id && it.isPacked }
    }
  }
  if (confirmRemoveSlip != null) {
    AlertDialog(
      onDismissRequest = { confirmRemovePackedOrderId = null },
      title = {
        Text(
          text = "Remove from Packed?",
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Text(
          text = "Move ${confirmRemoveSlip.order.shopName} (${confirmRemoveSlip.order.orderCode} • ${confirmRemoveSlip.packageLabel}) back to pending packing?",
        )
      },
      confirmButton = {
        Button(
          onClick = {
            setOrderPackedState(confirmRemoveSlip.order, false)
            confirmRemovePackedOrderId = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
          modifier = Modifier.testTag("packing_confirm_remove_packed_button"),
        ) {
          Text("Remove from Packed")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { confirmRemovePackedOrderId = null },
          modifier = Modifier.testTag("packing_cancel_remove_packed_button"),
        ) {
          Text("Keep Packed")
        }
      },
      modifier = Modifier.testTag("packing_remove_confirm_dialog"),
    )
  }

  val stockPendingDialogSlip = remember(stockPendingDialogOrderId, snapshot.shopSlips) {
    stockPendingDialogOrderId?.let { id ->
      snapshot.shopSlips.firstOrNull { it.order.id == id }
    }
  }
  if (stockPendingDialogSlip != null) {
    MarkStockPendingDialog(
      slip = stockPendingDialogSlip,
      stocks = stocks,
      products = products,
      onDismiss = { stockPendingDialogOrderId = null },
      onConfirm = { reason ->
        markOrderStockPendingState(stockPendingDialogSlip.order, reason)
        stockPendingDialogOrderId = null
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
  onMarkPacked: () -> Unit,
  onRequestRemoveFromPacked: () -> Unit,
  onRequestStockHoldNext: () -> Unit,
  onClearStockHold: () -> Unit,
  onSequenceLockedTap: () -> Unit,
  onEditClick: () -> Unit,
) {
  val totalUnits = slip.items.sumOf { it.quantity }.takeIf { it > 0 } ?: slip.order.itemCount
  val routeNameShort = slip.route?.name?.substringBefore(":") ?: "Unassigned"
  val isEditable = slip.order.status != OrderStatus.DELIVERED &&
    slip.order.status != OrderStatus.REJECTED

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = if (slip.isNextInSequence) 3.dp else 2.dp),
    border = BorderStroke(
      width = when {
        slip.isPacked || slip.isNextInSequence || slip.isStockPending -> 1.4.dp
        else -> 1.dp
      },
      color = when {
        slip.isPacked -> MaterialTheme.semanticColors.success.copy(alpha = 0.55f)
        slip.isStockPending -> MaterialTheme.semanticColors.warning.copy(alpha = 0.65f)
        slip.isNextInSequence -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
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
                when {
                  slip.isPacked -> MaterialTheme.semanticColors.success
                  slip.isStockPending -> MaterialTheme.semanticColors.warning
                  slip.isSequenceLocked -> MaterialTheme.colorScheme.surfaceVariant
                  else -> MaterialTheme.colorScheme.primary
                },
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "${slip.packOrderNumber}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = if (slip.isSequenceLocked) {
                MaterialTheme.colorScheme.onSurfaceVariant
              } else {
                MaterialTheme.colorScheme.onPrimary
              },
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
            val stopPart = if (slip.deliveryStopNumber > 0) "Stop #${slip.deliveryStopNumber} • " else ""
            Text(
              text = "${slip.packageLabel} • $stopPart${slip.order.orderCode} • $routeNameShort",
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

          val pillColor = when {
            slip.isPacked -> MaterialTheme.semanticColors.successContainer
            slip.isStockPending -> MaterialTheme.semanticColors.warningContainer
            slip.isNextInSequence -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
          }
          val pillInk = when {
            slip.isPacked -> MaterialTheme.semanticColors.success
            slip.isStockPending -> MaterialTheme.semanticColors.warning
            slip.isNextInSequence -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          }
          val pillIcon = when {
            slip.isPacked -> Icons.Rounded.Check
            slip.isStockPending -> Icons.Rounded.WarningAmber
            slip.isSequenceLocked -> Icons.Rounded.Lock
            else -> Icons.Rounded.HourglassTop
          }
          val pillLabel = when {
            slip.isPacked -> "Packed"
            slip.isStockPending -> "Stock Hold"
            slip.isNextInSequence -> "Next"
            else -> "Locked"
          }
          Surface(
            color = pillColor,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .clickable {
                when {
                  slip.isPacked -> onRequestRemoveFromPacked()
                  slip.isSequenceLocked -> onSequenceLockedTap()
                  else -> onMarkPacked()
                }
              }
              .testTag("packing_toggle_packed_${slip.order.id}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = pillIcon,
                contentDescription = pillLabel,
                tint = pillInk,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = pillLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = pillInk,
              )
            }
          }
        }
      }

      // Sequence / Stock Hold Status Strip
      if (slip.isNextInSequence) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "Active in Sequence (${slip.packageLabel}) • Complete or hold for stock to unlock next",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      } else if (slip.isStockPending) {
        Surface(
          color = MaterialTheme.semanticColors.warningContainer.copy(alpha = 0.6f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, MaterialTheme.semanticColors.warning.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.WarningAmber,
              contentDescription = null,
              tint = MaterialTheme.semanticColors.warning,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = "Pending Stock: ${slip.stockPendingReason} • Next orders unlocked",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      } else if (slip.isSequenceLocked) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "Locked in sequence • Pack or hold Pkg #${slip.blockingPackOrderNumber ?: 1} (${slip.blockingShopName ?: "earlier order"}) first",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Clean Packet & Quantity Rows for this Shop (tapping opens quick edit)
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .clickable(onClick = onEditClick),
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

      // Card Action Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (slip.isPacked) {
          OutlinedButton(
            onClick = onRequestRemoveFromPacked,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.error,
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("packing_remove_packed_${slip.order.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.RemoveCircleOutline,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Remove from Packed",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        } else if (slip.isNextInSequence) {
          Button(
            onClick = onMarkPacked,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.semanticColors.success,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("packing_complete_button_${slip.order.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Packing Complete",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          OutlinedButton(
            onClick = onRequestStockHoldNext,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.semanticColors.warning.copy(alpha = 0.65f)),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.semanticColors.warning,
            ),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("packing_hold_next_button_${slip.order.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.WarningAmber,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Pending & Next",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
            )
          }
        } else if (slip.isStockPending) {
          Button(
            onClick = onMarkPacked,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.semanticColors.success,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("packing_complete_button_${slip.order.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Packing Complete",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          OutlinedButton(
            onClick = onClearStockHold,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("packing_resume_stock_button_${slip.order.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Replay,
              contentDescription = "Return to queue",
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Queue",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
            )
          }
        } else {
          Button(
            onClick = onSequenceLockedTap,
            enabled = false,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag("packing_complete_button_${slip.order.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = null,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Packing Complete",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        OutlinedButton(
          onClick = onEditClick,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("packing_edit_order_${slip.order.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = if (isEditable) "Edit order items" else "Edit packing status",
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Edit",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarkStockPendingDialog(
  slip: ShopPackingSlip,
  stocks: List<StockItem>,
  products: List<Product> = emptyList(),
  onDismiss: () -> Unit,
  onConfirm: (reason: String) -> Unit,
) {
  val stockByCode = remember(stocks, products) {
    StockAlertHelper.resolveWarehouseStockMap(products, stocks)
  }
  val selectedCodes = remember(slip.order.id) { mutableStateMapOf<String, Boolean>() }
  var reasonText by remember(slip.order.id) { mutableStateOf("") }

  val resolvedReason = remember(reasonText, selectedCodes.toMap(), slip.items) {
    val picked = slip.items
      .filter { selectedCodes[it.productCode] == true }
      .map { "${it.productName} (${it.packSize})" }
    val custom = reasonText.trim()
    when {
      picked.isNotEmpty() && custom.isNotEmpty() -> "${picked.joinToString(", ")} — $custom"
      picked.isNotEmpty() -> "${picked.joinToString(", ")} unavailable"
      custom.isNotEmpty() -> custom
      else -> "Stock unavailable"
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Mark Pending & Proceed Next",
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "${slip.order.shopName} (${slip.packageLabel}) will stay pending so the next order unlocks and dispatches on time.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (slip.items.isNotEmpty()) {
          Text(
            text = "Tap unavailable material(s):",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            slip.items.forEach { item ->
              val stockQty = stockByCode[item.productCode.uppercase()]
              val isShort = stockQty != null && stockQty < item.quantity
              val selected = selectedCodes[item.productCode] == true
              FilterChip(
                selected = selected,
                onClick = { selectedCodes[item.productCode] = !selected },
                label = {
                  Text(
                    text = if (isShort) {
                      "${item.productCode} (Stock $stockQty/${item.quantity})"
                    } else {
                      "${item.productCode} (${item.quantity} pk)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                  )
                },
                modifier = Modifier.testTag("packing_stock_chip_${item.productCode}"),
              )
            }
          }
        }
        OutlinedTextField(
          value = reasonText,
          onValueChange = { reasonText = it },
          label = { Text("Reason / Missing Material") },
          placeholder = { Text("e.g., Banana Chips 100g out of stock") },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("packing_stock_pending_reason_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(resolvedReason) },
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.semanticColors.warning,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.testTag("packing_confirm_stock_pending_button"),
      ) {
        Text("Mark Pending & Next")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("packing_cancel_stock_pending_button"),
      ) {
        Text("Cancel")
      }
    },
    modifier = Modifier.testTag("packing_stock_pending_dialog"),
  )
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
