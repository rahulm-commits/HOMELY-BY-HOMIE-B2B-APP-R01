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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryRoute
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentLedgerHelper
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentTransaction
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.ShopLedgerSummary
import com.example.data.model.SupplierBill
import com.example.data.model.SupplierBillStatus
import com.example.ui.components.StandardCompactSearchBar
import com.example.ui.components.formatInr
import com.example.ui.components.isNetworkAvailable
import com.example.ui.theme.semanticColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PaymentsSectionTab { LEDGER, TRANSACTIONS, AGING }

private enum class LedgerStatusFilter { ALL, PENDING, OVERDUE, SETTLED }

private enum class LedgerSortMode(val label: String) {
  HIGHEST_BALANCE("Highest Balance"),
  OVERDUE_FIRST("Overdue First"),
  NAME_ASC("Name A-Z"),
}

@Composable
fun PaymentsScreen(
  shops: List<Shop>,
  orders: List<OrderRecord>,
  payments: List<PaymentTransaction>,
  supplierBills: List<SupplierBill>,
  routes: List<DeliveryRoute>,
  userDisplayName: String,
  pendingQueueCount: Int = 0,
  laptopServerEnabled: Boolean = false,
  initialDirection: String = PaymentDirection.COLLECT,
  onRecordPayment: (PaymentTransaction, Boolean, (String) -> Unit) -> Unit,
  onSaveSupplierBill: (SupplierBill, Boolean, () -> Unit) -> Unit,
  onOpenSyncServers: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val isOnline = remember(context) { isNetworkAvailable(context) }
  val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

  var selectedDirection by rememberSaveable { mutableStateOf(initialDirection) }
  LaunchedEffect(initialDirection) { selectedDirection = initialDirection }

  var selectedTab by rememberSaveable { mutableStateOf(PaymentsSectionTab.LEDGER) }
  var selectedFilter by rememberSaveable { mutableStateOf(LedgerStatusFilter.ALL) }
  var sortMode by rememberSaveable { mutableStateOf(LedgerSortMode.HIGHEST_BALANCE) }
  var selectedRoute by rememberSaveable { mutableStateOf(RoutesCatalog.ALL) }
  var showRouteMenu by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }

  var showOverdueDialog by remember { mutableStateOf(false) }
  var activePaymentTarget by remember { mutableStateOf<PaymentSheetTarget?>(null) }
  var detailShopLedger by remember { mutableStateOf<ShopLedgerSummary?>(null) }
  var detailSupplierBill by remember { mutableStateOf<SupplierBill?>(null) }
  var showAddSupplierBillSheet by remember { mutableStateOf(false) }

  val isSupplierMode = selectedDirection == PaymentDirection.PAY_SUPPLIER

  val allShopLedgers = remember(shops, orders, payments) {
    PaymentLedgerHelper.computeShopLedgers(shops, orders, payments)
  }
  val routeFilteredLedgers = remember(allShopLedgers, selectedRoute) {
    if (selectedRoute == RoutesCatalog.ALL) allShopLedgers
    else allShopLedgers.filter { it.shop.routeId == selectedRoute }
  }

  val collectPayments = remember(payments) {
    payments.filter { it.direction == PaymentDirection.COLLECT }
      .sortedWith(compareByDescending<PaymentTransaction> { it.paymentDate }.thenByDescending { it.createdAt?.seconds ?: 0L })
  }
  val supplierPayouts = remember(payments) {
    payments.filter { it.direction == PaymentDirection.PAY_SUPPLIER }
      .sortedWith(compareByDescending<PaymentTransaction> { it.paymentDate }.thenByDescending { it.createdAt?.seconds ?: 0L })
  }

  val overdueShopLedgers = remember(routeFilteredLedgers) {
    routeFilteredLedgers.filter { it.maxDaysOverdue > 0 }.sortedByDescending { it.overdueAmount }
  }
  val overdueSupplierBills = remember(supplierBills) {
    supplierBills.filter { PaymentLedgerHelper.effectiveSupplierStatus(it) == SupplierBillStatus.OVERDUE }
      .sortedByDescending { it.outstandingAmount }
  }
  val totalAlertCount = overdueShopLedgers.size + overdueSupplierBills.size

  val pendingShopsCount = remember(routeFilteredLedgers) { routeFilteredLedgers.count { it.outstandingBalance > 0.5 } }
  val totalOutstanding = remember(routeFilteredLedgers) { routeFilteredLedgers.sumOf { it.outstandingBalance } }
  val totalOverdueCredit = remember(overdueShopLedgers) { overdueShopLedgers.sumOf { it.overdueAmount } }
  val collectedTodayAmount = remember(collectPayments, todayIso) {
    collectPayments.filter { it.paymentDate == todayIso }.sumOf { it.amount }
  }
  val collectedTodayCount = remember(collectPayments, todayIso) {
    collectPayments.count { it.paymentDate == todayIso }
  }

  val totalPayables = remember(supplierBills) { supplierBills.sumOf { it.outstandingAmount } }
  val pendingBillsCount = remember(supplierBills) { supplierBills.count { it.outstandingAmount > 0.5 } }
  val overduePayables = remember(overdueSupplierBills) { overdueSupplierBills.sumOf { it.outstandingAmount } }
  val totalPaidSuppliers = remember(supplierPayouts) { supplierPayouts.sumOf { it.amount } }
  val settledBillsCount = remember(supplierBills) {
    supplierBills.count { PaymentLedgerHelper.effectiveSupplierStatus(it) == SupplierBillStatus.SETTLED }
  }

  val filteredShopLedgers = remember(routeFilteredLedgers, selectedFilter, searchQuery, sortMode) {
    val q = searchQuery.trim().lowercase()
    routeFilteredLedgers.filter { entry ->
      val matchFilter = when (selectedFilter) {
        LedgerStatusFilter.ALL -> true
        LedgerStatusFilter.PENDING -> entry.outstandingBalance > 0.5
        LedgerStatusFilter.OVERDUE -> entry.maxDaysOverdue > 0
        LedgerStatusFilter.SETTLED -> entry.isSettled
      }
      val matchSearch = q.isEmpty() ||
        entry.shop.name.lowercase().contains(q) ||
        entry.shop.shopCode.lowercase().contains(q) ||
        entry.shop.ownerName.lowercase().contains(q) ||
        entry.shop.phone.contains(q) ||
        entry.unpaidOrders.any { it.orderCode.lowercase().contains(q) }
      matchFilter && matchSearch
    }.sortedWith(
      when (sortMode) {
        LedgerSortMode.HIGHEST_BALANCE -> compareByDescending<ShopLedgerSummary> { it.outstandingBalance }.thenBy { it.shop.name.lowercase() }
        LedgerSortMode.OVERDUE_FIRST -> compareByDescending<ShopLedgerSummary> { it.maxDaysOverdue }.thenByDescending { it.outstandingBalance }
        LedgerSortMode.NAME_ASC -> compareBy<ShopLedgerSummary> { it.shop.name.lowercase() }
      }
    )
  }

  val filteredSupplierBills = remember(supplierBills, selectedFilter, searchQuery, sortMode) {
    val q = searchQuery.trim().lowercase()
    supplierBills.filter { bill ->
      val effStatus = PaymentLedgerHelper.effectiveSupplierStatus(bill)
      val matchFilter = when (selectedFilter) {
        LedgerStatusFilter.ALL -> true
        LedgerStatusFilter.PENDING -> bill.outstandingAmount > 0.5
        LedgerStatusFilter.OVERDUE -> effStatus == SupplierBillStatus.OVERDUE
        LedgerStatusFilter.SETTLED -> effStatus == SupplierBillStatus.SETTLED
      }
      val matchSearch = q.isEmpty() ||
        bill.supplierName.lowercase().contains(q) ||
        bill.supplierCode.lowercase().contains(q) ||
        bill.billNumber.lowercase().contains(q) ||
        bill.category.lowercase().contains(q)
      matchFilter && matchSearch
    }.sortedWith(
      when (sortMode) {
        LedgerSortMode.HIGHEST_BALANCE -> compareByDescending<SupplierBill> { it.outstandingAmount }.thenBy { it.supplierName.lowercase() }
        LedgerSortMode.OVERDUE_FIRST -> compareByDescending<SupplierBill> { PaymentLedgerHelper.computeSupplierDaysOverdue(it) }.thenByDescending { it.outstandingAmount }
        LedgerSortMode.NAME_ASC -> compareBy<SupplierBill> { it.supplierName.lowercase() }
      }
    )
  }

  val activeTransactions = remember(isSupplierMode, collectPayments, supplierPayouts, searchQuery) {
    val base = if (isSupplierMode) supplierPayouts else collectPayments
    val q = searchQuery.trim().lowercase()
    if (q.isEmpty()) base else base.filter { tx ->
      tx.partyName.lowercase().contains(q) ||
        tx.partyCode.lowercase().contains(q) ||
        tx.receiptCode.lowercase().contains(q) ||
        tx.invoiceRef.lowercase().contains(q) ||
        tx.referenceNumber.lowercase().contains(q) ||
        PaymentMethod.displayLabel(tx.paymentMethod).lowercase().contains(q)
    }
  }

  val shopAgingBuckets = remember(routeFilteredLedgers) { PaymentLedgerHelper.computeShopAgingBuckets(routeFilteredLedgers) }
  val supplierAgingBuckets = remember(supplierBills) { PaymentLedgerHelper.computeSupplierAgingBuckets(supplierBills) }

  val nonRejectedOrderCountsByShopKey = remember(shops, orders) {
    val activeOrders = orders.filter { it.status != OrderStatus.REJECTED }
    val countsByShopId = HashMap<String, Int>(shops.size)
    for (shop in shops) {
      val c = activeOrders.count { ord ->
        ord.shopId == shop.id || (shop.shopCode.isNotBlank() && ord.shopCode == shop.shopCode)
      }
      countsByShopId[shop.id] = c
    }
    countsByShopId
  }

  val routeOptions = remember(routes) {
    (listOf(RoutesCatalog.ALL) + routes.map { it.id } + RoutesCatalog.UNALLOCATED).distinct()
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("payments_lazy_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // 1. Header Bar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "HOMELY BY HOMIE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = if (isSupplierMode) "Supplier Payouts" else "Shop Payments",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = if (isOnline && pendingQueueCount == 0) {
              MaterialTheme.semanticColors.successContainer
            } else {
              MaterialTheme.semanticColors.warningContainer
            },
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .clickable(onClick = onOpenSyncServers)
              .testTag("payments_sync_pill"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = if (isOnline && pendingQueueCount == 0) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                contentDescription = "Sync status",
                tint = if (isOnline && pendingQueueCount == 0) MaterialTheme.semanticColors.success else MaterialTheme.semanticColors.warning,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = when {
                  pendingQueueCount > 0 -> "$pendingQueueCount Queued"
                  laptopServerEnabled -> "Hub + Cloud"
                  isOnline -> "Live Sync"
                  else -> "Offline"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOnline && pendingQueueCount == 0) MaterialTheme.semanticColors.success else MaterialTheme.semanticColors.warning,
              )
            }
          }

          IconButton(
            onClick = { showOverdueDialog = true },
            modifier = Modifier.testTag("payments_alerts_bell"),
          ) {
            BadgedBox(
              badge = {
                if (totalAlertCount > 0) {
                  Badge(
                    containerColor = MaterialTheme.semanticColors.danger,
                    contentColor = MaterialTheme.colorScheme.onError,
                  ) {
                    Text(totalAlertCount.toString())
                  }
                }
              },
            ) {
              Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = "Overdue alerts",
                tint = MaterialTheme.colorScheme.onBackground,
              )
            }
          }
        }
      }
    }

    // 2. Mode Switcher (Customer Collections vs Supplier Payouts) + Route Pill
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            ModeSwitcherSegment(
              label = "Collect from Shops",
              icon = Icons.Rounded.Storefront,
              selected = !isSupplierMode,
              onClick = {
                selectedDirection = PaymentDirection.COLLECT
                selectedFilter = LedgerStatusFilter.ALL
              },
              modifier = Modifier
                .weight(1f)
                .testTag("payment_mode_collect"),
            )
            ModeSwitcherSegment(
              label = "Pay Suppliers",
              icon = Icons.Rounded.Factory,
              selected = isSupplierMode,
              onClick = {
                selectedDirection = PaymentDirection.PAY_SUPPLIER
                selectedFilter = LedgerStatusFilter.ALL
              },
              modifier = Modifier
                .weight(1f)
                .testTag("payment_mode_suppliers"),
            )
          }
        }

        if (!isSupplierMode) {
          Box {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
              shape = RoundedCornerShape(999.dp),
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { showRouteMenu = true }
                .testTag("payments_route_filter_pill"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.AltRoute,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.size(15.dp),
                )
                Text(
                  text = if (selectedRoute == RoutesCatalog.ALL) "All Delivery Routes" else selectedRoute,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            DropdownMenu(
              expanded = showRouteMenu,
              onDismissRequest = { showRouteMenu = false },
            ) {
              routeOptions.forEach { routeId ->
                DropdownMenuItem(
                  text = {
                    Text(if (routeId == RoutesCatalog.ALL) "All Delivery Routes" else routeId)
                  },
                  onClick = {
                    selectedRoute = routeId
                    showRouteMenu = false
                  },
                )
              }
            }
          }
        }
      }
    }

    // 3. 2x2 Bento Summary Matrix
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isSupplierMode) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PaymentBentoMetricCard(
              title = "Total Outstanding",
              value = formatInr(totalOutstanding),
              subtitle = "Across $pendingShopsCount shops",
              icon = Icons.Rounded.AccountBalanceWallet,
              modifier = Modifier.weight(1f),
            )
            PaymentBentoMetricCard(
              title = "Overdue Credit",
              value = formatInr(totalOverdueCredit),
              subtitle = if (overdueShopLedgers.isEmpty()) "No overdue accounts" else "Past due • ${overdueShopLedgers.size} shops",
              icon = Icons.Rounded.WarningAmber,
              isDanger = overdueShopLedgers.isNotEmpty(),
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PaymentBentoMetricCard(
              title = "Collected Today",
              value = formatInr(collectedTodayAmount),
              subtitle = "$collectedTodayCount collection(s)",
              icon = Icons.Rounded.Payments,
              isSuccess = true,
              modifier = Modifier.weight(1f),
            )
            val totalShops = routeFilteredLedgers.size.coerceAtLeast(1)
            val settledRatio = ((totalShops - pendingShopsCount).toFloat() / totalShops.toFloat()).coerceIn(0f, 1f)
            PaymentBentoMetricCard(
              title = "Pending Retailers",
              value = "$pendingShopsCount / ${routeFilteredLedgers.size}",
              subtitle = "${(settledRatio * 100).toInt()}% accounts cleared",
              icon = Icons.Rounded.Storefront,
              progress = settledRatio,
              modifier = Modifier.weight(1f),
            )
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PaymentBentoMetricCard(
              title = "Total Payables",
              value = formatInr(totalPayables),
              subtitle = "Across $pendingBillsCount bill(s)",
              icon = Icons.Rounded.Factory,
              modifier = Modifier.weight(1f),
            )
            PaymentBentoMetricCard(
              title = "Overdue Bills",
              value = formatInr(overduePayables),
              subtitle = if (overdueSupplierBills.isEmpty()) "No overdue bills" else "Past due • ${overdueSupplierBills.size} bills",
              icon = Icons.Rounded.WarningAmber,
              isDanger = overdueSupplierBills.isNotEmpty(),
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PaymentBentoMetricCard(
              title = "Paid to Suppliers",
              value = formatInr(totalPaidSuppliers),
              subtitle = "${supplierPayouts.size} payout voucher(s)",
              icon = Icons.Rounded.Payments,
              isSuccess = true,
              modifier = Modifier.weight(1f),
            )
            val totalBills = supplierBills.size.coerceAtLeast(1)
            val settledRatio = (settledBillsCount.toFloat() / totalBills.toFloat()).coerceIn(0f, 1f)
            PaymentBentoMetricCard(
              title = "Settled Bills",
              value = "$settledBillsCount / ${supplierBills.size}",
              subtitle = "${(settledRatio * 100).toInt()}% vendor bills settled",
              icon = Icons.Rounded.CheckCircle,
              progress = settledRatio,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }

    // 4. 3-Tab Suite
    item {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          SectionTabChip(
            label = if (isSupplierMode) "Supplier Bills" else "Shop Ledger",
            selected = selectedTab == PaymentsSectionTab.LEDGER,
            onClick = { selectedTab = PaymentsSectionTab.LEDGER },
            modifier = Modifier.weight(1f).testTag("tab_payments_ledger"),
          )
          SectionTabChip(
            label = if (isSupplierMode) "Payouts (${supplierPayouts.size})" else "Receipts (${collectPayments.size})",
            selected = selectedTab == PaymentsSectionTab.TRANSACTIONS,
            onClick = { selectedTab = PaymentsSectionTab.TRANSACTIONS },
            modifier = Modifier.weight(1f).testTag("tab_payments_receipts"),
          )
          SectionTabChip(
            label = "Aging Analysis",
            selected = selectedTab == PaymentsSectionTab.AGING,
            onClick = { selectedTab = PaymentsSectionTab.AGING },
            modifier = Modifier.weight(1f).testTag("tab_payments_aging"),
          )
        }
      }
    }

    // 5. Search, Filters & Sort (when on Ledger or Transactions)
    if (selectedTab != PaymentsSectionTab.AGING) {
      item {
        StandardCompactSearchBar(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          placeholder = if (isSupplierMode) {
            "Search supplier, code, or bill number..."
          } else {
            "Search shop name, code, owner, or order..."
          },
          fieldTestTag = "payments_search_input",
        )
      }
    }

    if (selectedTab == PaymentsSectionTab.LEDGER) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          val allCount = if (isSupplierMode) supplierBills.size else routeFilteredLedgers.size
          val pendCount = if (isSupplierMode) pendingBillsCount else pendingShopsCount
          val ovdCount = if (isSupplierMode) overdueSupplierBills.size else overdueShopLedgers.size
          val setCount = if (isSupplierMode) settledBillsCount else routeFilteredLedgers.count { it.isSettled }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf(
              LedgerStatusFilter.ALL to "All ($allCount)",
              LedgerStatusFilter.PENDING to "Pending ($pendCount)",
              LedgerStatusFilter.OVERDUE to "Overdue ($ovdCount)",
              LedgerStatusFilter.SETTLED to (if (isSupplierMode) "Settled ($setCount)" else "Fully Paid ($setCount)"),
            ).forEach { (filter, label) ->
              FilterChip(
                selected = selectedFilter == filter,
                onClick = { selectedFilter = filter },
                label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(999.dp),
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable {
                  val modes = LedgerSortMode.entries
                  sortMode = modes[(modes.indexOf(sortMode) + 1) % modes.size]
                }
                .testTag("payments_sort_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(15.dp))
                Text(
                  text = "Sort: ${sortMode.label}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }

            val shown = if (isSupplierMode) filteredSupplierBills.size else filteredShopLedgers.size
            val total = if (isSupplierMode) supplierBills.size else routeFilteredLedgers.size
            Text(
              text = "Showing $shown of $total",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          if (isSupplierMode) {
            Button(
              onClick = { showAddSupplierBillSheet = true },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("add_supplier_bill_button"),
            ) {
              Icon(Icons.Rounded.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add Supplier Payable Bill", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 6. Content Body per Tab & Mode
    when (selectedTab) {
      PaymentsSectionTab.LEDGER -> {
        if (!isSupplierMode) {
          if (filteredShopLedgers.isEmpty()) {
            item(key = "empty_shops_ledger", contentType = "empty_placeholder") {
              EmptyPaymentsPlaceholder("No shops match the selected payment filter.")
            }
          } else {
            items(
              filteredShopLedgers,
              key = { it.shop.id.ifBlank { it.shop.shopCode } },
              contentType = { "shop_payment_card" },
            ) { ledger ->
              val shopOrderCount = nonRejectedOrderCountsByShopKey[ledger.shop.id] ?: 0
              ShopPaymentCard(
                ledger = ledger,
                totalOrderCount = shopOrderCount,
                onViewInvoicesClick = { detailShopLedger = ledger },
                onRecordPaymentClick = { activePaymentTarget = PaymentSheetTarget.ShopCollection(ledger) },
              )
            }
          }
        } else {
          if (filteredSupplierBills.isEmpty()) {
            item(key = "empty_supplier_bills", contentType = "empty_placeholder") {
              EmptyPaymentsPlaceholder("No supplier bills match the selected filter.")
            }
          } else {
            items(
              filteredSupplierBills,
              key = { it.id.ifBlank { it.billNumber } },
              contentType = { "supplier_bill_card" },
            ) { bill ->
              SupplierBillCard(
                bill = bill,
                onPayClick = { activePaymentTarget = PaymentSheetTarget.SupplierPayout(bill) },
                onViewDetailsClick = { detailSupplierBill = bill },
              )
            }
          }
        }
      }

      PaymentsSectionTab.TRANSACTIONS -> {
        if (activeTransactions.isEmpty()) {
          item(key = "empty_transactions", contentType = "empty_placeholder") {
            EmptyPaymentsPlaceholder(
              if (isSupplierMode) "No supplier payouts recorded yet." else "No shop collection receipts recorded yet."
            )
          }
        } else {
          items(
            activeTransactions,
            key = { it.id.ifBlank { it.receiptCode } },
            contentType = { "payment_tx_card" },
          ) { tx ->
            PaymentTransactionCard(transaction = tx)
          }
        }
      }

      PaymentsSectionTab.AGING -> {
        val buckets = if (isSupplierMode) supplierAgingBuckets else shopAgingBuckets
        val bucketTotal = buckets.sumOf { it.amount }
        itemsIndexed(
          items = buckets,
          key = { idx, bucket -> "${if (isSupplierMode) "sup" else "shop"}_${idx}_${bucket.label}" },
          contentType = { _, _ -> "aging_bucket_card" },
        ) { idx, bucket ->
          AgingBucketCard(
            bucket = bucket,
            totalAmount = bucketTotal,
            index = idx,
          )
        }
      }
    }
  }

  // Overdue Alerts Dialog
  if (showOverdueDialog) {
    OverdueAlertsDialog(
      overdueShops = overdueShopLedgers,
      overdueBills = overdueSupplierBills,
      onDismiss = { showOverdueDialog = false },
      onSelectShop = { ledger ->
        showOverdueDialog = false
        selectedDirection = PaymentDirection.COLLECT
        activePaymentTarget = PaymentSheetTarget.ShopCollection(ledger)
      },
      onSelectBill = { bill ->
        showOverdueDialog = false
        selectedDirection = PaymentDirection.PAY_SUPPLIER
        activePaymentTarget = PaymentSheetTarget.SupplierPayout(bill)
      },
    )
  }

  // Record Payment / Supplier Payout Modal Sheet
  activePaymentTarget?.let { target ->
    RecordPaymentModalSheet(
      target = target,
      userDisplayName = userDisplayName,
      onDismiss = { activePaymentTarget = null },
      onConfirmPayment = { tx ->
        onRecordPayment(tx, isOnline) {
          activePaymentTarget = null
        }
      },
    )
  }

  // Shop Ledger Detail Modal Sheet
  detailShopLedger?.let { ledger ->
    val liveLedger = allShopLedgers.firstOrNull { it.shop.id == ledger.shop.id } ?: ledger
    val shopOrders = orders.filter { ord ->
      ord.status != OrderStatus.REJECTED &&
        (ord.shopId == liveLedger.shop.id || (liveLedger.shop.shopCode.isNotBlank() && ord.shopCode == liveLedger.shop.shopCode))
    }.sortedByDescending { it.orderDate }
    val shopPays = collectPayments.filter { pay ->
      pay.partyId == liveLedger.shop.id || (liveLedger.shop.shopCode.isNotBlank() && pay.partyCode == liveLedger.shop.shopCode)
    }
    ShopLedgerDetailSheet(
      ledger = liveLedger,
      shopOrders = shopOrders,
      shopPayments = shopPays,
      onDismiss = { detailShopLedger = null },
      onRecordPaymentClick = {
        detailShopLedger = null
        activePaymentTarget = PaymentSheetTarget.ShopCollection(liveLedger)
      },
    )
  }

  // Supplier Bill Detail Modal Sheet
  detailSupplierBill?.let { bill ->
    val liveBill = supplierBills.firstOrNull { it.id == bill.id } ?: bill
    val billPays = supplierPayouts.filter { pay ->
      pay.partyId == liveBill.id || pay.invoiceRef == liveBill.billNumber || pay.partyCode == liveBill.supplierCode
    }
    SupplierBillDetailSheet(
      bill = liveBill,
      billPayouts = billPays,
      onDismiss = { detailSupplierBill = null },
      onPayClick = {
        detailSupplierBill = null
        activePaymentTarget = PaymentSheetTarget.SupplierPayout(liveBill)
      },
    )
  }

  // Add New Supplier Bill Modal Sheet
  if (showAddSupplierBillSheet) {
    val nextSupCode = String.format(Locale.US, "SUP-%03d", supplierBills.size + 1)
    val nextBillNo = String.format(Locale.US, "BILL-%d", 420 + supplierBills.size)
    AddSupplierBillSheet(
      suggestedSupplierCode = nextSupCode,
      suggestedBillNumber = nextBillNo,
      onDismiss = { showAddSupplierBillSheet = false },
      onSaveBill = { newBill ->
        onSaveSupplierBill(newBill, isOnline) {
          showAddSupplierBillSheet = false
        }
      },
    )
  }
}

@Composable
private fun ModeSwitcherSegment(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
    shape = RoundedCornerShape(10.dp),
    shadowElevation = if (selected) 2.dp else 0.dp,
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun SectionTabChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
    shape = RoundedCornerShape(9.dp),
    shadowElevation = if (selected) 1.dp else 0.dp,
    modifier = modifier
      .clip(RoundedCornerShape(9.dp))
      .clickable(onClick = onClick),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
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

@Composable
private fun EmptyPaymentsPlaceholder(message: String) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
