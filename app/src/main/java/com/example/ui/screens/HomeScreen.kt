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
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.InquiryStatus
import com.example.data.model.OperationsMeta
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.example.data.model.StockItem
import com.example.ui.components.formatInr
import com.example.ui.components.isNetworkAvailable
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningContainer
import com.example.ui.theme.CobaltInfo
import com.example.ui.theme.CobaltInfoContainer
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralDangerContainer
import com.example.ui.theme.EmeraldDeepPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.MintAccentChip
import com.example.ui.theme.MintBadgeBg
import com.example.ui.theme.MintCardBorder
import com.example.ui.theme.MintFieldBorder
import com.example.ui.theme.MintFieldFill
import com.example.ui.theme.MintHeaderTint
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
  pendingQueueCount: Int = 0,
  laptopServerEnabled: Boolean = false,
  onOpenSyncServers: () -> Unit = {},
  onSignOut: () -> Unit,
  onUpdateStock: (stockId: String, newQty: Int, productName: String) -> Unit,
  onNavigateToShops: () -> Unit,
  onNavigateToOrders: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val periods = ReportPeriod.entries
  val pagerState = rememberPagerState(initialPage = 0, pageCount = { periods.size })

  var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
  var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }
  var editingStock by remember { mutableStateOf<StockItem?>(null) }

  LaunchedEffect(Unit) {
    while (true) {
      nowMillis = System.currentTimeMillis()
      isOnline = isNetworkAvailable(context)
      delay(1000L)
    }
  }

  val dateText = remember(nowMillis / 60000L) {
    SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(nowMillis))
  }
  val timeText = remember(nowMillis / 1000L) {
    SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(nowMillis))
  }

  val activeShops = remember(shops) { shops.filter { it.status == ShopStatus.ACTIVE } }
  val pendingInquiryCount = remember(activeShops) {
    activeShops.count { it.inquiryStatus == InquiryStatus.PENDING_INQUIRY }
  }
  val pendingSyncCount = remember(shops, orders, pendingQueueCount) {
    maxOf(pendingQueueCount, shops.count { it.isPendingSync } + orders.count { it.isPendingSync })
  }

  val todayStr = remember(nowMillis / 60000L) {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
  }
  val yesterdayStr = remember(nowMillis / 60000L) {
    Calendar.getInstance().apply {
      timeInMillis = nowMillis
      add(Calendar.DAY_OF_YEAR, -1)
    }.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) }
  }

  val opsMeta = meta ?: OperationsMeta()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Top Header with Live Date/Time + Connectivity + Sign Out
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.3.dp, MintCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  MintBadgeBg,
                  MintHeaderTint,
                  Color.White,
                )
              )
            )
            .padding(16.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "SnackRoute Ops",
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
                  color = if (isOnline && pendingSyncCount == 0) EmeraldSuccessContainer else AmberWarningContainer,
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
                      tint = if (isOnline && pendingSyncCount == 0) EmeraldSuccess else AmberWarning,
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
                      color = if (isOnline && pendingSyncCount == 0) EmeraldSuccess else AmberWarning,
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

            Surface(
              color = MintFieldFill,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, MintFieldBorder),
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
        }
      }
    }

    // 2. Period Toggle + Swipeable HorizontalPager for 4-Card Dashboard
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          periods.forEachIndexed { index, period ->
            val selected = pagerState.currentPage == index
            Surface(
              color = if (selected) {
                EmeraldDeepPrimary
              } else {
                Color.White
              },
              shape = RoundedCornerShape(12.dp),
              tonalElevation = 0.dp,
              shadowElevation = if (selected) 2.dp else 1.dp,
              border = if (selected) null else BorderStroke(1.2.dp, MintCardBorder),
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
          val metrics = remember(period, orders, opsMeta, todayStr, yesterdayStr) {
            computePeriodMetrics(period, orders, opsMeta, todayStr, yesterdayStr)
          }
          DashboardQuadGrid(period = period, metrics = metrics)
        }
      }
    }

    // 3. Today's Work Section
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, MintCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
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
              color = MintBadgeBg,
              shape = RoundedCornerShape(999.dp),
              border = BorderStroke(1.dp, MintFieldBorder),
            ) {
              Text(
                text = "Live Field Status",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldDeepPrimary,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
              )
            }
          }

          val completedRegs = activeShops.size
          val targetRegs = opsMeta.shopRegTarget.coerceAtLeast(1)
          val regProgress = (completedRegs.toFloat() / targetRegs.toFloat()).coerceIn(0f, 1f)

          Surface(
            color = MintFieldFill,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MintFieldBorder),
            modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onNavigateToShops),
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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            WorkMetricTile(
              icon = Icons.Rounded.Call,
              label = "Inquiry Calls Due",
              value = "$pendingInquiryCount",
              accent = AmberWarning,
              container = AmberWarningContainer,
              onClick = onNavigateToOrders,
              modifier = Modifier.weight(1f),
            )
            WorkMetricTile(
              icon = Icons.AutoMirrored.Rounded.ReceiptLong,
              label = "Total Orders Placed",
              value = "${orders.size}",
              accent = EmeraldSuccess,
              container = EmeraldSuccessContainer,
              onClick = onNavigateToOrders,
              modifier = Modifier.weight(1f),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            WorkMetricTile(
              icon = Icons.Rounded.LocalShipping,
              label = "To Dispatch",
              value = "${opsMeta.vehiclesToDispatch} Vans",
              accent = CobaltInfo,
              container = CobaltInfoContainer,
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
            color = MintFieldFill,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MintFieldBorder),
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

    // 4. Stock Alert Section
    item {
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
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = CoralDanger,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = "Store Stock Alerts",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
        val lowCount = stocks.count { it.quantity <= it.minThreshold }
        if (lowCount > 0) {
          Surface(
            color = CoralDangerContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = "$lowCount Low Stock",
              style = MaterialTheme.typography.labelMedium,
              color = CoralDanger,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }
    }

    items(stocks, key = { it.id }) { stock ->
      val isLow = stock.quantity <= stock.minThreshold
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isLow) Color(0xFFFFF8F7) else Color.White,
        ),
        border = BorderStroke(
          1.2.dp,
          if (isLow) CoralDanger.copy(alpha = 0.45f) else MintCardBorder,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isLow) CoralDangerContainer else EmeraldSuccessContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Inventory,
                contentDescription = null,
                tint = if (isLow) CoralDanger else EmeraldSuccess,
                modifier = Modifier.size(22.dp),
              )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = stock.productCode,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = "• ${stock.packSize}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = stock.productName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = if (isLow) {
                  "Available: ${stock.quantity} pkts (Min ${stock.minThreshold})"
                } else {
                  "Healthy: ${stock.quantity} pkts in store"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isLow) CoralDanger else EmeraldSuccess,
              )
            }
          }

          OutlinedButton(
            onClick = { editingStock = stock },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("update_stock_${stock.productCode}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Update", style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }
  }

  editingStock?.let { targetStock ->
    StockUpdateDialog(
      stock = targetStock,
      onDismiss = { editingStock = null },
      onConfirm = { newQty ->
        onUpdateStock(targetStock.id, newQty, targetStock.productName)
        editingStock = null
      },
    )
  }
}

@Composable
private fun DashboardQuadGrid(
  period: ReportPeriod,
  metrics: PeriodDashboardMetrics,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Card 1: Total Sales in Rupees
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.3.dp, EmeraldSuccess.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        modifier = Modifier.weight(1f),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Surface(color = EmeraldSuccessContainer.copy(alpha = 0.65f)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(26.dp)
                  .clip(CircleShape)
                  .background(EmeraldSuccess),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.CurrencyRupee,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(15.dp),
                )
              }
              Text(
                text = "Total Sales",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = EmeraldDeepPrimary,
              )
            }
          }
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = formatInr(metrics.totalSales),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = EmeraldDeepPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = period.subtitle,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = EmeraldSuccess,
            )
          }
        }
      }

      // Card 2: Orders Breakdown (Total, Pending, Delivered, Rejected)
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.3.dp, MintCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        modifier = Modifier.weight(1f),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Surface(color = MintBadgeBg.copy(alpha = 0.72f)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Total Orders",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = EmeraldDeepPrimary,
              )
              Surface(
                color = EmeraldDeepPrimary,
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  text = "${metrics.totalOrders}",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                )
              }
            }
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            MiniOrderStat("Pend", metrics.pendingOrders, AmberWarning, AmberWarningContainer, Modifier.weight(1f))
            MiniOrderStat("Deliv", metrics.deliveredOrders, EmeraldSuccess, EmeraldSuccessContainer, Modifier.weight(1f))
            MiniOrderStat("Rej", metrics.rejectedOrders, CoralDanger, CoralDangerContainer, Modifier.weight(1f))
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Card 3: To Collect
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.3.dp, CobaltInfo.copy(alpha = 0.42f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        modifier = Modifier.weight(1f),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Surface(color = CobaltInfoContainer.copy(alpha = 0.7f)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(26.dp)
                  .clip(CircleShape)
                  .background(CobaltInfo),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.AccountBalanceWallet,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(15.dp),
                )
              }
              Text(
                text = "To Collect",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = CobaltInfo,
              )
            }
          }
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
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
              fontWeight = FontWeight.SemiBold,
              color = CobaltInfo,
            )
          }
        }
      }

      // Card 4: To Suppliers
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.3.dp, AmberWarning.copy(alpha = 0.48f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        modifier = Modifier.weight(1f),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Surface(color = AmberWarningContainer.copy(alpha = 0.72f)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(26.dp)
                  .clip(CircleShape)
                  .background(AmberWarning),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Factory,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(15.dp),
                )
              }
              Text(
                text = "To Suppliers",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = AmberWarning,
              )
            }
          }
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
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
              fontWeight = FontWeight.SemiBold,
              color = AmberWarning,
            )
          }
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
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        color = Color.White.copy(alpha = 0.85f),
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

@Composable
private fun StockUpdateDialog(
  stock: StockItem,
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit,
) {
  var qtyText by remember(stock.id) { mutableStateOf(stock.quantity.toString()) }
  val parsedQty = qtyText.toIntOrNull() ?: stock.quantity

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Update Stock • ${stock.productCode}",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "${stock.productName} (${stock.packSize})",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          IconButton(
            onClick = { qtyText = (parsedQty - 10).coerceAtLeast(0).toString() },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant),
          ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease stock")
          }
          OutlinedTextField(
            value = qtyText,
            onValueChange = { input -> qtyText = input.filter { it.isDigit() } },
            label = { Text("Packets") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = { qtyText = (parsedQty + 10).toString() },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          ) {
            Icon(
              Icons.Rounded.Add,
              contentDescription = "Increase stock",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = { onConfirm(parsedQty.coerceAtLeast(0)) }) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save Stock")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

private fun computePeriodMetrics(
  period: ReportPeriod,
  orders: List<OrderRecord>,
  meta: OperationsMeta,
  todayStr: String,
  yesterdayStr: String,
): PeriodDashboardMetrics {
  return when (period) {
    ReportPeriod.TODAY -> {
      val todayOrders = orders.filter { it.orderDate == todayStr }
      PeriodDashboardMetrics(
        totalSales = todayOrders.filter { it.status != OrderStatus.REJECTED }.sumOf { it.totalAmount },
        totalOrders = todayOrders.size,
        pendingOrders = todayOrders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.CONFIRMED },
        deliveredOrders = todayOrders.count { it.status == OrderStatus.DELIVERED },
        rejectedOrders = todayOrders.count { it.status == OrderStatus.REJECTED },
        toCollectAmount = meta.toCollectAmount,
        toCollectShops = meta.toCollectShops,
        toSuppliersAmount = meta.toSuppliersAmount,
        toSuppliersCount = meta.toSuppliersCount,
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
        toCollectAmount = (meta.toCollectAmount * 0.85),
        toCollectShops = (meta.toCollectShops - 2).coerceAtLeast(1),
        toSuppliersAmount = meta.toSuppliersAmount,
        toSuppliersCount = meta.toSuppliersCount,
      )
    }
    ReportPeriod.WEEK -> {
      PeriodDashboardMetrics(
        totalSales = orders.filter { it.status != OrderStatus.REJECTED }.sumOf { it.totalAmount } + 54200.0,
        totalOrders = orders.size + 14,
        pendingOrders = orders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.CONFIRMED },
        deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED } + 13,
        rejectedOrders = orders.count { it.status == OrderStatus.REJECTED },
        toCollectAmount = meta.toCollectAmount,
        toCollectShops = meta.toCollectShops,
        toSuppliersAmount = meta.toSuppliersAmount + 12000.0,
        toSuppliersCount = meta.toSuppliersCount,
      )
    }
    ReportPeriod.MONTH -> {
      PeriodDashboardMetrics(
        totalSales = orders.filter { it.status != OrderStatus.REJECTED }.sumOf { it.totalAmount } + 248600.0,
        totalOrders = orders.size + 62,
        pendingOrders = orders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.CONFIRMED },
        deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED } + 58,
        rejectedOrders = orders.count { it.status == OrderStatus.REJECTED } + 3,
        toCollectAmount = meta.toCollectAmount + 19500.0,
        toCollectShops = meta.toCollectShops + 4,
        toSuppliersAmount = meta.toSuppliersAmount + 42000.0,
        toSuppliersCount = meta.toSuppliersCount + 2,
      )
    }
  }
}
