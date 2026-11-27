package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AssignmentReturn
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.InquiryStatus
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentLedgerHelper
import com.example.data.model.PaymentTransaction
import com.example.data.model.Product
import com.example.data.model.ProductStatus
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.example.data.model.StockAlertHelper
import com.example.data.model.StockItem
import com.example.data.model.SupplierBill
import com.example.data.model.UnifiedStockAlert
import com.example.data.model.UnifiedStockSource
import com.example.ui.components.InlineQuickDeltaPill
import com.example.ui.components.QuickStockAdjustDialog
import com.example.ui.components.formatInr
import com.example.ui.components.isNetworkAvailable
import com.example.ui.theme.semanticColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ReportPeriod(val label: String, val subtitle: String) {
  TODAY("Today", "Live today metrics"),
  YESTERDAY("Yesterday", "Previous day closing"),
  WEEK("This Week", "Last 7 days summary"),
  MONTH("This Month", "Monthly rolling totals"),
}

private data class PeriodDashboardMetrics(
  val totalSales: Double,
  val totalOrders: Int,
  val pendingOrders: Int,
  val deliveredOrders: Int,
  val rejectedOrders: Int,
  val toCollectAmount: Double,
  val toCollectShops: Int,
  val toSuppliersAmount: Double,
  val toSuppliersCount: Int,
)

@Composable
fun HomeScreen(
  shops: List<Shop>,
  orders: List<OrderRecord>,
  stocks: List<StockItem>,
  meta: OperationsMeta?,
  userDisplayName: String,
  isLocalTestMode: Boolean = false,
  isSigningIn: Boolean = false,
  pendingQueueCount: Int = 0,
  laptopServerEnabled: Boolean = false,
  onOpenSyncServers: () -> Unit = {},
  onSignInWithGoogle: () -> Unit = {},
  onSignOut: () -> Unit,
  onUpdateStock: (stockId: String, newQty: Int, productName: String) -> Unit,
  onNavigateToShops: () -> Unit,
  onNavigateToOrders: () -> Unit,
  payments: List<PaymentTransaction> = emptyList(),
  supplierBills: List<SupplierBill> = emptyList(),
  onNavigateToPayments: (String) -> Unit = {},
  products: List<Product> = emptyList(),
  onUpdateProductStock: (productId: String, newQty: Int, productName: String) -> Unit = { _, _, _ -> },
  onNavigateToProducts: () -> Unit = {},
  onAddProductClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val periods = ReportPeriod.entries
  val pagerState = rememberPagerState(initialPage = 0, pageCount = { periods.size })

  var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }
  var editingAlert by remember { mutableStateOf<UnifiedStockAlert?>(null) }
  var showOnlyLowStockAlerts by remember { mutableStateOf(false) }
  var stockSearchQuery by remember { mutableStateOf("") }

  val unifiedStockAlerts = remember(products, stocks) {
    StockAlertHelper.buildUnifiedAlerts(products = products, stocks = stocks)
  }
  val lowStockAlertsCount = remember(unifiedStockAlerts) {
    unifiedStockAlerts.count { it.isLow }
  }
  val visibleStockAlerts = remember(unifiedStockAlerts, showOnlyLowStockAlerts, stockSearchQuery) {
    val q = stockSearchQuery.trim().lowercase()
    unifiedStockAlerts.filter { alert ->
      val matchesFilter = !showOnlyLowStockAlerts || alert.isLow
      val matchesSearch = q.isEmpty() ||
        alert.name.lowercase().contains(q) ||
        alert.code.lowercase().contains(q) ||
        alert.secondaryCode.lowercase().contains(q) ||
        alert.category.lowercase().contains(q)
      matchesFilter && matchesSearch
    }
  }

  val applyQuickStockChange: (UnifiedStockAlert, Int) -> Unit = { alert, targetQty ->
    val safeQty = targetQty.coerceAtLeast(0)
    when (alert.source) {
      UnifiedStockSource.PRODUCT -> onUpdateProductStock(alert.targetId, safeQty, alert.name)
      UnifiedStockSource.STOCK_ITEM -> onUpdateStock(alert.targetId, safeQty, alert.name)
    }
  }

  DisposableEffect(context) {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        isOnline = true
      }

      override fun onLost(network: Network) {
        isOnline = isNetworkAvailable(context)
      }
    }
    if (cm != null) {
      runCatching {
        val request = NetworkRequest.Builder()
          .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .build()
        cm.registerNetworkCallback(request, callback)
      }
    }
    onDispose {
      if (cm != null) {
        runCatching { cm.unregisterNetworkCallback(callback) }
      }
    }
  }

  val activeShops = remember(shops) { shops.filter { it.status == ShopStatus.ACTIVE } }
  val pendingInquiryCount = remember(activeShops) {
    activeShops.count { it.inquiryStatus == InquiryStatus.PENDING_INQUIRY }
  }
  val pendingSyncCount = remember(shops, orders, pendingQueueCount) {
    maxOf(pendingQueueCount, shops.count { it.isPendingSync } + orders.count { it.isPendingSync })
  }

  val todayEpochDay = remember { System.currentTimeMillis() / 86_400_000L }
  val todayStr = remember(todayEpochDay) {
    homeIsoDateFormatLocal.get()!!.format(Date())
  }
  val yesterdayStr = remember(todayEpochDay) {
    Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, -1)
    }.let { homeIsoDateFormatLocal.get()!!.format(it.time) }
  }

  val opsMeta = meta ?: OperationsMeta()
  val liveToCollectStats = remember(shops, orders, payments) {
    if (shops.isEmpty() && payments.isEmpty()) {
      null
    } else {
      val ledgers = PaymentLedgerHelper.computeShopLedgers(shops, orders, payments)
      val outstandingShops = ledgers.filter { it.outstandingBalance > 0.5 }
      outstandingShops.sumOf { it.outstandingBalance } to outstandingShops.size
    }
  }
  val liveToSuppliersStats = remember(supplierBills) {
    if (supplierBills.isEmpty()) {
      null
    } else {
      val pendingBills = supplierBills.filter { it.outstandingAmount > 0.5 }
      pendingBills.sumOf { it.outstandingAmount } to pendingBills.size
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 18.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // 1. Top Header with Live Date/Time + Connectivity + Sign Out
    item(key = "home_header", contentType = "home_header") {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                  MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.20f),
                  MaterialTheme.colorScheme.surface,
                )
              )
            )
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Homely by Homie Ops",
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = if (userDisplayName.isNotBlank()) "Executive: $userDisplayName" else "Field Distribution",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  color = if (isOnline && pendingSyncCount == 0) MaterialTheme.semanticColors.successContainer else MaterialTheme.semanticColors.warningContainer,
                  shape = RoundedCornerShape(20.dp),
                  modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenSyncServers)
                    .testTag("open_sync_servers_pill"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = if (isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                      contentDescription = "Open Phone Queue and Server Settings",
                      tint = if (isOnline && pendingSyncCount == 0) MaterialTheme.semanticColors.success else MaterialTheme.semanticColors.warning,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = when {
                        pendingSyncCount > 0 -> "$pendingSyncCount Pending Sync"
                        isLocalTestMode && laptopServerEnabled -> "Laptop Sync On"
                        isLocalTestMode -> "Servers & Queue"
                        isOnline && laptopServerEnabled -> "Cloud + Laptop"
                        isOnline -> "Online • Sync"
                        else -> "Offline Queue"
                      },
                      style = MaterialTheme.typography.labelMedium,
                      color = if (isOnline && pendingSyncCount == 0) MaterialTheme.semanticColors.success else MaterialTheme.semanticColors.warning,
                    )
                  }
                }
                IconButton(
                  onClick = onSignOut,
                  modifier = Modifier.testTag("sign_out_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Sign out",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            HomeLiveClockStrip()

            if (isLocalTestMode) {
              Surface(
                color = MaterialTheme.semanticColors.warningContainer.copy(alpha = 0.72f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.semanticColors.warning.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(
                    modifier = Modifier
                      .weight(1f)
                      .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                  ) {
                    Text(
                      text = if (pendingSyncCount > 0) {
                        "$pendingSyncCount record(s) queued on phone"
                      } else {
                        "Test Mode (No Login)"
                      },
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = if (laptopServerEnabled) {
                        "Laptop LAN sync active • Sign in for Cloud"
                      } else {
                        "Sign in with Google to sync to Cloud, or use Laptop LAN"
                      },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Button(
                    onClick = onSignInWithGoogle,
                    enabled = !isSigningIn,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("home_google_sign_in_button"),
                  ) {
                    if (isSigningIn) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                      )
                    } else {
                      Text(
                        text = "Sign in",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
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

    // 2. Period Toggle + Swipeable HorizontalPager for 4-Card Dashboard
    item(key = "home_dashboard_pager", contentType = "home_dashboard_pager") {
      Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          periods.forEachIndexed { index, period ->
            val selected = pagerState.currentPage == index
            Surface(
              color = if (selected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.surface
              },
              shape = RoundedCornerShape(12.dp),
              tonalElevation = 0.dp,
              shadowElevation = if (selected) 2.dp else 1.dp,
              border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  scope.launch { pagerState.animateScrollToPage(index) }
                }
                .testTag("period_tab_${period.name.lowercase()}"),
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
              ) {
                Text(
                  text = period.label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxWidth(),
          pageSpacing = 12.dp,
        ) { pageIndex ->
          val period = periods[pageIndex]
          val metrics = remember(period, orders, opsMeta, todayStr, yesterdayStr, liveToCollectStats, liveToSuppliersStats) {
            computePeriodMetrics(
              period = period,
              orders = orders,
              meta = opsMeta,
              todayStr = todayStr,
              yesterdayStr = yesterdayStr,
              liveToCollect = liveToCollectStats,
              liveToSuppliers = liveToSuppliersStats,
            )
          }
          DashboardQuadGrid(
            period = period,
            metrics = metrics,
            onCollectClick = { onNavigateToPayments(PaymentDirection.COLLECT) },
            onSuppliersClick = { onNavigateToPayments(PaymentDirection.PAY_SUPPLIER) },
          )
        }
      }
    }

    // 3. Today's Work Section
    item(key = "home_todays_work", contentType = "home_todays_work") {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
            Text(
              text = "Today's Work",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = "Live Field Status",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
              )
            }
          }

          val completedRegs = activeShops.size
          val targetRegs = opsMeta.shopRegTarget.coerceAtLeast(1)
          val regProgress = (completedRegs.toFloat() / targetRegs.toFloat()).coerceIn(0f, 1f)

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onNavigateToShops),
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "Shops Registration Target",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
                Text(
                  text = "$completedRegs / $targetRegs completed",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              LinearProgressIndicator(
                progress = { regProgress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            WorkMetricTile(
              icon = Icons.Rounded.Call,
              label = "Inquiry Calls Due",
              value = "$pendingInquiryCount",
              accent = MaterialTheme.semanticColors.warning,
              container = MaterialTheme.semanticColors.warningContainer,
              onClick = onNavigateToOrders,
              modifier = Modifier.weight(1f),
            )
            WorkMetricTile(
              icon = Icons.AutoMirrored.Rounded.ReceiptLong,
              label = "Total Orders Placed",
              value = "${orders.size}",
              accent = MaterialTheme.semanticColors.success,
              container = MaterialTheme.semanticColors.successContainer,
              onClick = onNavigateToOrders,
              modifier = Modifier.weight(1f),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            WorkMetricTile(
              icon = Icons.Rounded.LocalShipping,
              label = "To Dispatch",
              value = "${opsMeta.vehiclesToDispatch} Vans",
              accent = MaterialTheme.semanticColors.info,
              container = MaterialTheme.semanticColors.infoContainer,
              onClick = null,
              modifier = Modifier.weight(1f),
            )
            WorkMetricTile(
              icon = Icons.Rounded.LocalShipping,
              label = "Vehicles On Road",
              value = "${opsMeta.vehiclesOnRoad} Active",
              accent = MaterialTheme.colorScheme.secondary,
              container = MaterialTheme.colorScheme.secondaryContainer,
              onClick = null,
              modifier = Modifier.weight(1f),
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.AssignmentReturn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Pending Order Returns Collected",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Text(
                text = "${opsMeta.returnsCollected} Crates",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }

    // 4. Unified Stock Alert Section (All Products + Stock Items, including newly added products)
    item(key = "home_stock_alerts_header", contentType = "home_stock_header") {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
              imageVector = Icons.Rounded.WarningAmber,
              contentDescription = null,
              tint = if (lowStockAlertsCount > 0) MaterialTheme.semanticColors.danger else MaterialTheme.semanticColors.success,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = "Store Stock Alerts",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            if (lowStockAlertsCount > 0) {
              Surface(
                color = MaterialTheme.semanticColors.dangerContainer,
                shape = RoundedCornerShape(8.dp),
              ) {
                Text(
                  text = "$lowStockAlertsCount Low Stock",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.semanticColors.danger,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            } else {
              Surface(
                color = MaterialTheme.semanticColors.successContainer,
                shape = RoundedCornerShape(8.dp),
              ) {
                Text(
                  text = "All Healthy",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.semanticColors.success,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilterChip(
            selected = !showOnlyLowStockAlerts,
            onClick = { showOnlyLowStockAlerts = false },
            label = { Text("All (${unifiedStockAlerts.size})") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.testTag("home_stock_filter_all"),
          )
          FilterChip(
            selected = showOnlyLowStockAlerts,
            onClick = { showOnlyLowStockAlerts = true },
            label = { Text("Low Alerts ($lowStockAlertsCount)") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.semanticColors.dangerContainer,
              selectedLabelColor = MaterialTheme.semanticColors.danger,
            ),
            modifier = Modifier.testTag("home_stock_filter_low"),
          )
          Spacer(modifier = Modifier.weight(1f))
          TextButton(
            onClick = if (products.isNotEmpty()) onNavigateToProducts else onAddProductClick,
            modifier = Modifier.testTag("home_stock_catalog_button"),
          ) {
            Text(if (products.isNotEmpty()) "Catalog" else "+ Product")
          }
        }

        if (unifiedStockAlerts.size > 4) {
          OutlinedTextField(
            value = stockSearchQuery,
            onValueChange = { stockSearchQuery = it },
            placeholder = { Text("Quick search product, SKU, or category...") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            trailingIcon = if (stockSearchQuery.isNotBlank()) {
              {
                TextButton(onClick = { stockSearchQuery = "" }) {
                  Text("Clear")
                }
              }
            } else {
              null
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("home_stock_search_input"),
          )
        }
      }
    }

    if (visibleStockAlerts.isEmpty()) {
      item(key = "home_stock_alerts_empty", contentType = "home_stock_empty") {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (showOnlyLowStockAlerts) {
                "No low-stock alerts right now. All tracked products are above threshold."
              } else {
                "No matching products found."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
            )
            if (showOnlyLowStockAlerts || stockSearchQuery.isNotBlank()) {
              TextButton(
                onClick = {
                  showOnlyLowStockAlerts = false
                  stockSearchQuery = ""
                },
              ) {
                Text("Show All")
              }
            }
          }
        }
      }
    } else {
      items(visibleStockAlerts, key = { it.key }, contentType = { "home_stock_item" }) { alert ->
        val isLow = alert.isLow
        val isInactive = alert.status == ProductStatus.INACTIVE
        val containerColor = when {
          isLow -> MaterialTheme.semanticColors.dangerContainer.copy(alpha = 0.48f)
          isInactive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          else -> MaterialTheme.colorScheme.surface
        }
        val borderColor = when {
          isLow -> MaterialTheme.semanticColors.danger.copy(alpha = 0.45f)
          else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        }
        val statusIconBg = when {
          isLow -> MaterialTheme.semanticColors.dangerContainer
          isInactive -> MaterialTheme.colorScheme.surfaceVariant
          else -> MaterialTheme.semanticColors.successContainer
        }
        val statusIconTint = when {
          isLow -> MaterialTheme.semanticColors.danger
          isInactive -> MaterialTheme.colorScheme.onSurfaceVariant
          else -> MaterialTheme.semanticColors.success
        }

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = containerColor),
          border = BorderStroke(1.dp, borderColor),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusIconBg),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = if (isLow) Icons.Rounded.WarningAmber else Icons.Rounded.Inventory,
                    contentDescription = null,
                    tint = statusIconTint,
                    modifier = Modifier.size(22.dp),
                  )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Text(
                      text = alert.code,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "• ${alert.packSize}",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    if (alert.isRecentlyAdded) {
                      Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp),
                      ) {
                        Text(
                          text = "NEW",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onTertiaryContainer,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                      }
                    }
                  }
                  Text(
                    text = alert.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = when {
                      isInactive -> "Inactive • ${alert.quantity} ${alert.unitLabel} (Min ${alert.minThreshold})"
                      alert.isCritical && alert.quantity == 0 -> "Out of stock! (Min ${alert.minThreshold} ${alert.unitLabel})"
                      isLow -> "Low Stock: ${alert.quantity} ${alert.unitLabel} (Min ${alert.minThreshold})"
                      else -> "Healthy: ${alert.quantity} ${alert.unitLabel} (Min ${alert.minThreshold})"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isLow) FontWeight.SemiBold else FontWeight.Normal,
                    color = statusIconTint,
                  )
                }
              }

              OutlinedButton(
                onClick = { editingAlert = alert },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("update_stock_${alert.code}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Edit,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Update", style = MaterialTheme.typography.labelLarge)
              }
            }

            // One-tap inline stock quick adjust bar
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              InlineQuickDeltaPill(
                label = "-10",
                isAdd = false,
                enabled = alert.quantity > 0,
                onClick = { applyQuickStockChange(alert, (alert.quantity - 10).coerceAtLeast(0)) },
                modifier = Modifier.testTag("home_quick_minus10_${alert.code}"),
              )
              InlineQuickDeltaPill(
                label = "-1",
                isAdd = false,
                enabled = alert.quantity > 0,
                onClick = { applyQuickStockChange(alert, (alert.quantity - 1).coerceAtLeast(0)) },
                modifier = Modifier.testTag("home_quick_minus1_${alert.code}"),
              )
              Surface(
                color = if (isLow) {
                  MaterialTheme.semanticColors.danger
                } else {
                  MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { editingAlert = alert },
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                  Text(
                    text = "${alert.quantity} ${alert.unitLabel}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isLow) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              InlineQuickDeltaPill(
                label = "+1",
                isAdd = true,
                onClick = { applyQuickStockChange(alert, alert.quantity + 1) },
                modifier = Modifier.testTag("home_quick_plus1_${alert.code}"),
              )
              InlineQuickDeltaPill(
                label = "+10",
                isAdd = true,
                onClick = { applyQuickStockChange(alert, alert.quantity + 10) },
                modifier = Modifier.testTag("home_quick_plus10_${alert.code}"),
              )
            }
          }
        }
      }
    }
  }

  editingAlert?.let { targetAlert ->
    QuickStockAdjustDialog(
      productName = targetAlert.name,
      productCode = targetAlert.code,
      packSize = targetAlert.packSize,
      currentQuantity = targetAlert.quantity,
      minThreshold = targetAlert.minThreshold,
      unitLabel = targetAlert.unitLabel,
      onDismiss = { editingAlert = null },
      onConfirm = { newQty ->
        applyQuickStockChange(targetAlert, newQty)
        editingAlert = null
      },
    )
  }
}

private val homeIsoDateFormatLocal = ThreadLocal.withInitial {
  SimpleDateFormat("yyyy-MM-dd", Locale.US)
}
private val homeDisplayDateFormatLocal = ThreadLocal.withInitial {
  SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
}
private val homeDisplayTimeFormatLocal = ThreadLocal.withInitial {
  SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
}

@Composable
private fun HomeLiveClockStrip() {
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000L)
      nowMillis = System.currentTimeMillis()
    }
  }
  val dateText = remember(nowMillis / 60_000L) {
    homeDisplayDateFormatLocal.get()!!.format(Date(nowMillis))
  }
  val timeText = remember(nowMillis / 1000L) {
    homeDisplayTimeFormatLocal.get()!!.format(Date(nowMillis))
  }

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Schedule,
          contentDescription = "Current date and time",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = dateText,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = timeText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun DashboardQuadGrid(
  period: ReportPeriod,
  metrics: PeriodDashboardMetrics,
  onCollectClick: () -> Unit = {},
  onSuppliersClick: () -> Unit = {},
) {
  Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      // Card 1: Total Sales in Rupees
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.weight(1f),
      ) {
        Column(
          modifier = Modifier.padding(11.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.CurrencyRupee,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
            }
            Text(
              text = "Total Sales",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = formatInr(metrics.totalSales),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = period.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Card 2: Orders Breakdown (Total, Pending, Delivered, Rejected)
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.weight(1f),
      ) {
        Column(
          modifier = Modifier.padding(11.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Total Orders",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = "${metrics.totalOrders}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            MiniOrderStat("Pend", metrics.pendingOrders, MaterialTheme.semanticColors.warning, MaterialTheme.semanticColors.warningContainer, Modifier.weight(1f))
            MiniOrderStat("Deliv", metrics.deliveredOrders, MaterialTheme.semanticColors.success, MaterialTheme.semanticColors.successContainer, Modifier.weight(1f))
            MiniOrderStat("Rej", metrics.rejectedOrders, MaterialTheme.semanticColors.danger, MaterialTheme.semanticColors.dangerContainer, Modifier.weight(1f))
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      // Card 3: To Collect
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(18.dp))
          .clickable(onClick = onCollectClick)
          .testTag("home_to_collect_card"),
      ) {
        Column(
          modifier = Modifier.padding(11.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.semanticColors.infoContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.semanticColors.info,
                modifier = Modifier.size(16.dp),
              )
            }
            Text(
              text = "To Collect",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = formatInr(metrics.toCollectAmount),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "From ${metrics.toCollectShops} shops",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.semanticColors.info,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      // Card 4: To Suppliers
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(18.dp))
          .clickable(onClick = onSuppliersClick)
          .testTag("home_to_suppliers_card"),
      ) {
        Column(
          modifier = Modifier.padding(11.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.semanticColors.warningContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Factory,
                contentDescription = null,
                tint = MaterialTheme.semanticColors.warning,
                modifier = Modifier.size(16.dp),
              )
            }
            Text(
              text = "To Suppliers",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = formatInr(metrics.toSuppliersAmount),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${metrics.toSuppliersCount} suppliers due",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.semanticColors.warning,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }
  }
}

@Composable
private fun MiniOrderStat(
  label: String,
  count: Int,
  color: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = containerColor.copy(alpha = 0.6f),
    shape = RoundedCornerShape(10.dp),
    modifier = modifier,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
    ) {
      Text(
        text = "$count",
        style = MaterialTheme.typography.titleMedium,
        color = color,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
      )
    }
  }
}

@Composable
private fun WorkMetricTile(
  icon: ImageVector,
  label: String,
  value: String,
  accent: Color,
  container: Color,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = container.copy(alpha = 0.78f),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = CircleShape,
        modifier = Modifier.size(32.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Column {
        Text(
          text = value,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}



private fun computePeriodMetrics(
  period: ReportPeriod,
  orders: List<OrderRecord>,
  meta: OperationsMeta,
  todayStr: String,
  yesterdayStr: String,
  liveToCollect: Pair<Double, Int>? = null,
  liveToSuppliers: Pair<Double, Int>? = null,
): PeriodDashboardMetrics {
  val baseCollectAmt = liveToCollect?.first ?: meta.toCollectAmount
  val baseCollectCount = liveToCollect?.second ?: meta.toCollectShops
  val baseSupplierAmt = liveToSuppliers?.first ?: meta.toSuppliersAmount
  val baseSupplierCount = liveToSuppliers?.second ?: meta.toSuppliersCount
  return when (period) {
    ReportPeriod.TODAY -> {
      val todayOrders = orders.filter { it.orderDate == todayStr }
      PeriodDashboardMetrics(
        totalSales = todayOrders.filter { it.status != OrderStatus.REJECTED }.sumOf { it.totalAmount },
        totalOrders = todayOrders.size,
        pendingOrders = todayOrders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.CONFIRMED },
        deliveredOrders = todayOrders.count { it.status == OrderStatus.DELIVERED },
        rejectedOrders = todayOrders.count { it.status == OrderStatus.REJECTED },
        toCollectAmount = baseCollectAmt,
        toCollectShops = baseCollectCount,
        toSuppliersAmount = baseSupplierAmt,
        toSuppliersCount = baseSupplierCount,
      )
    }
    ReportPeriod.YESTERDAY -> {
      val yOrders = orders.filter { it.orderDate == yesterdayStr }
      PeriodDashboardMetrics(
        totalSales = yOrders.filter { it.status != OrderStatus.REJECTED }.sumOf { it.totalAmount }.coerceAtLeast(12400.0),
        totalOrders = yOrders.size.coerceAtLeast(1),
        pendingOrders = yOrders.count { it.status == OrderStatus.PENDING },
        deliveredOrders = yOrders.count { it.status == OrderStatus.DELIVERED }.coerceAtLeast(1),
        rejectedOrders = yOrders.count { it.status == OrderStatus.REJECTED },
        toCollectAmount = (baseCollectAmt * 0.85),
        toCollectShops = (baseCollectCount - 1).coerceAtLeast(if (baseCollectCount > 0) 1 else 0),
        toSuppliersAmount = baseSupplierAmt,
        toSuppliersCount = baseSupplierCount,
      )
    }
    ReportPeriod.WEEK -> {
      PeriodDashboardMetrics(
        totalSales = orders.filter { it.status != OrderStatus.REJECTED }.sumOf { it.totalAmount } + 54200.0,
        totalOrders = orders.size + 14,
        pendingOrders = orders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.CONFIRMED },
        deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED } + 13,
        rejectedOrders = orders.count { it.status == OrderStatus.REJECTED },
        toCollectAmount = baseCollectAmt,
        toCollectShops = baseCollectCount,
        toSuppliersAmount = baseSupplierAmt + 12000.0,
        toSuppliersCount = baseSupplierCount,
      )
    }
    ReportPeriod.MONTH -> {
      PeriodDashboardMetrics(
        totalSales = orders.filter { it.status != OrderStatus.REJECTED }.sumOf { it.totalAmount } + 248600.0,
        totalOrders = orders.size + 62,
        pendingOrders = orders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.CONFIRMED },
        deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED } + 58,
        rejectedOrders = orders.count { it.status == OrderStatus.REJECTED } + 3,
        toCollectAmount = baseCollectAmt + 19500.0,
        toCollectShops = baseCollectCount + 4,
        toSuppliersAmount = baseSupplierAmt + 42000.0,
        toSuppliersCount = baseSupplierCount + 2,
      )
    }
  }
}
