package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PendingOrderEntity
import com.example.data.local.PendingPaymentEntity
import com.example.data.local.PendingProductEntity
import com.example.data.local.PendingShopEntity
import com.example.data.local.PendingStockUpdateEntity
import com.example.data.local.PendingSupplierBillEntity
import com.example.data.local.PendingSupplierEntity
import com.example.data.local.ServerSettings
import com.example.data.remote.LaptopHealthInfo
import com.example.ui.theme.semanticColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncServerScreen(
  settings: ServerSettings,
  pendingShops: List<PendingShopEntity>,
  pendingOrders: List<PendingOrderEntity>,
  pendingStockUpdates: List<PendingStockUpdateEntity> = emptyList(),
  pendingProducts: List<PendingProductEntity> = emptyList(),
  pendingPayments: List<PendingPaymentEntity> = emptyList(),
  pendingSupplierBills: List<PendingSupplierBillEntity> = emptyList(),
  pendingSuppliers: List<PendingSupplierEntity> = emptyList(),
  isLocalTestMode: Boolean,
  isOnline: Boolean,
  isSyncing: Boolean,
  laptopHealth: LaptopHealthInfo?,
  onSaveSettings: (
    globalSyncEnabled: Boolean,
    laptopServerEnabled: Boolean,
    laptopServerUrl: String,
    laptopApiToken: String,
    autoDeleteSyncedFromPhone: Boolean,
  ) -> Unit,
  onTestLaptopConnection: (url: String, token: String) -> Unit,
  onSyncNow: () -> Unit,
  onPullFromLaptop: () -> Unit,
  onSaveAndTestLaptopConnection: (
    globalSyncEnabled: Boolean,
    laptopServerEnabled: Boolean,
    laptopServerUrl: String,
    laptopApiToken: String,
    autoDeleteSyncedFromPhone: Boolean,
  ) -> Unit = { _, _, url, token, _ -> onTestLaptopConnection(url, token) },
  onSaveAndSyncNow: (
    globalSyncEnabled: Boolean,
    laptopServerEnabled: Boolean,
    laptopServerUrl: String,
    laptopApiToken: String,
    autoDeleteSyncedFromPhone: Boolean,
  ) -> Unit = { g, l, url, token, autoDel ->
    onSaveSettings(g, l, url, token, autoDel)
    onSyncNow()
  },
  onSaveAndPullFromLaptop: (
    globalSyncEnabled: Boolean,
    laptopServerEnabled: Boolean,
    laptopServerUrl: String,
    laptopApiToken: String,
    autoDeleteSyncedFromPhone: Boolean,
  ) -> Unit = { g, l, url, token, autoDel ->
    onSaveSettings(g, l, url, token, autoDel)
    onPullFromLaptop()
  },
  onDeleteSyncedFromPhone: () -> Unit,
  onDeletePendingShop: (localId: String) -> Unit,
  onDeletePendingOrder: (localId: String) -> Unit,
  onDeletePendingProduct: (localId: String) -> Unit = {},
  onDeletePendingPayment: (localId: String) -> Unit = {},
  onDeletePendingSupplierBill: (localId: String) -> Unit = {},
  onDeletePendingSupplier: (localId: String) -> Unit = {},
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var globalEnabled by remember { mutableStateOf(settings.globalSyncEnabled) }
  var laptopEnabled by remember { mutableStateOf(settings.laptopServerEnabled) }
  var laptopUrl by remember { mutableStateOf(settings.laptopServerUrl) }
  var laptopToken by remember { mutableStateOf(settings.laptopApiToken) }
  var autoDeleteSynced by remember { mutableStateOf(settings.autoDeleteSyncedFromPhone) }
  var showLaptopGuide by remember { mutableStateOf(false) }
  var confirmForceDeleteShop by remember { mutableStateOf<PendingShopEntity?>(null) }

  LaunchedEffect(
    settings.globalSyncEnabled,
    settings.laptopServerEnabled,
    settings.laptopServerUrl,
    settings.laptopApiToken,
    settings.autoDeleteSyncedFromPhone,
  ) {
    globalEnabled = settings.globalSyncEnabled
    laptopEnabled = settings.laptopServerEnabled
    laptopUrl = settings.laptopServerUrl
    laptopToken = settings.laptopApiToken
    autoDeleteSynced = settings.autoDeleteSyncedFromPhone
  }

  val requireGlobal = settings.globalSyncEnabled && !isLocalTestMode
  val requireLaptop = settings.laptopServerEnabled && settings.laptopServerUrl.isNotBlank()

  val syncedShopsCount = remember(pendingShops, requireGlobal, requireLaptop) {
    pendingShops.count { it.isFullySynced(requireGlobal, requireLaptop) }
  }
  val syncedOrdersCount = remember(pendingOrders, requireGlobal, requireLaptop) {
    pendingOrders.count { it.isFullySynced(requireGlobal, requireLaptop) }
  }
  val syncedStocksCount = remember(pendingStockUpdates, requireGlobal) {
    pendingStockUpdates.count { !requireGlobal || it.globalSynced }
  }
  val syncedProductsCount = remember(pendingProducts, requireGlobal) {
    pendingProducts.count { it.isFullySynced(requireGlobal) }
  }
  val syncedPaymentsCount = remember(pendingPayments, requireGlobal) {
    pendingPayments.count { it.isFullySynced(requireGlobal) }
  }
  val syncedBillsCount = remember(pendingSupplierBills, requireGlobal) {
    pendingSupplierBills.count { it.isFullySynced(requireGlobal) }
  }
  val syncedSuppliersCount = remember(pendingSuppliers, requireGlobal) {
    pendingSuppliers.count { it.isFullySynced(requireGlobal) }
  }
  val totalQueueRecords = pendingShops.size + pendingOrders.size + pendingStockUpdates.size +
    pendingProducts.size + pendingPayments.size + pendingSupplierBills.size + pendingSuppliers.size
  val totalSyncedReadyToDelete = syncedShopsCount + syncedOrdersCount + syncedStocksCount +
    syncedProductsCount + syncedPaymentsCount + syncedBillsCount + syncedSuppliersCount
  val totalAwaitingUpload = totalQueueRecords - totalSyncedReadyToDelete

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("sync_back_button")) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
          },
          title = {
            Column {
              Text(
                text = "Phone Queue & Dual Servers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Offline Storage • Global Cloud • Laptop LAN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Phone Storage & Sync Action Summary
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                  )
                }
                Text(
                  text = "Phone Temporary Storage",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
              Surface(
                color = if (totalAwaitingUpload > 0) MaterialTheme.semanticColors.warningContainer else MaterialTheme.semanticColors.successContainer,
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  text = if (totalAwaitingUpload > 0) "$totalAwaitingUpload Waiting Sync" else "All Synced",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = if (totalAwaitingUpload > 0) MaterialTheme.semanticColors.warning else MaterialTheme.semanticColors.success,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
              }
            }

            Text(
              text = "New shops & orders are saved safely on your phone first so zero records are lost offline. Once synced to your server(s), you can delete them from the phone.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              MiniQueueStat(
                label = "Pending Upload",
                count = totalAwaitingUpload,
                container = MaterialTheme.semanticColors.warningContainer,
                accent = MaterialTheme.semanticColors.warning,
                modifier = Modifier.weight(1f),
              )
              MiniQueueStat(
                label = "Synced (Deletable)",
                count = totalSyncedReadyToDelete,
                container = MaterialTheme.semanticColors.successContainer,
                accent = MaterialTheme.semanticColors.success,
                modifier = Modifier.weight(1f),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = {
                  onSaveAndSyncNow(globalEnabled, laptopEnabled, laptopUrl, laptopToken, autoDeleteSynced)
                },
                enabled = !isSyncing,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("sync_now_button"),
              ) {
                if (isSyncing) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                } else {
                  Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isSyncing) "Syncing..." else "Sync Now")
              }

              OutlinedButton(
                onClick = onDeleteSyncedFromPhone,
                enabled = totalSyncedReadyToDelete > 0 && !isSyncing,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("delete_synced_button"),
              ) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Synced ($totalSyncedReadyToDelete)")
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Auto-delete from phone after sync",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                )
                Text(
                  text = "Removes temporary record from phone once confirmed by all active servers",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = autoDeleteSynced,
                onCheckedChange = { checked ->
                  autoDeleteSynced = checked
                  onSaveSettings(globalEnabled, laptopEnabled, laptopUrl, laptopToken, checked)
                },
                modifier = Modifier.testTag("auto_delete_switch"),
              )
            }

            if (settings.lastSyncAtMillis > 0L) {
              val ts = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                .format(Date(settings.lastSyncAtMillis))
              Text(
                text = "Last sync ($ts): ${settings.lastSyncSummary}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      // 2. Global Cloud Server Card
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.semanticColors.info.copy(alpha = 0.4f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
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
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.semanticColors.infoContainer),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.semanticColors.info,
                    modifier = Modifier.size(19.dp),
                  )
                }
                Column {
                  Text(
                    text = "Global Cloud Server (Firebase)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = when {
                      isLocalTestMode -> "Paused in Test Mode — Sign in with Google on Home to sync"
                      isOnline && globalEnabled -> "Online & Active"
                      !globalEnabled -> "Paused by user"
                      else -> "No Internet — queued locally until online"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Switch(
                checked = globalEnabled,
                onCheckedChange = { checked ->
                  globalEnabled = checked
                  onSaveSettings(checked, laptopEnabled, laptopUrl, laptopToken, autoDeleteSynced)
                },
              )
            }
          }
        }
      }

      // 3. Laptop Local Server Card
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Computer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(19.dp),
                  )
                }
                Column {
                  Text(
                    text = "Laptop Local Server (LAN / Wi-Fi)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "Save to & read data directly from your laptop server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Switch(
                checked = laptopEnabled,
                onCheckedChange = { checked ->
                  laptopEnabled = checked
                  onSaveSettings(globalEnabled, checked, laptopUrl, laptopToken, autoDeleteSynced)
                },
                modifier = Modifier.testTag("laptop_server_switch"),
              )
            }

            OutlinedTextField(
              value = laptopUrl,
              onValueChange = { laptopUrl = it },
              label = { Text("Laptop Server URL (IP & Port)") },
              placeholder = { Text("http://192.168.1.100:8787") },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("laptop_url_input"),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { laptopUrl = "http://192.168.1.100:8787" },
              ) {
                Text(
                  text = "Wi-Fi LAN (:8787)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { laptopUrl = "http://10.0.2.2:8787" },
              ) {
                Text(
                  text = "Emulator (10.0.2.2:8787)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }

            OutlinedTextField(
              value = laptopToken,
              onValueChange = { laptopToken = it },
              label = { Text("Access Token (Optional)") },
              placeholder = { Text("Leave blank unless set on laptop server") },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
              ),
              modifier = Modifier.fillMaxWidth(),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = {
                  onSaveAndTestLaptopConnection(globalEnabled, laptopEnabled, laptopUrl, laptopToken, autoDeleteSynced)
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondary,
                  contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .testTag("test_laptop_button"),
              ) {
                Icon(Icons.Rounded.WifiTethering, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Save & Test", style = MaterialTheme.typography.labelMedium)
              }

              OutlinedButton(
                onClick = {
                  onSaveAndPullFromLaptop(globalEnabled, laptopEnabled, laptopUrl, laptopToken, autoDeleteSynced)
                },
                enabled = laptopEnabled && !isSyncing,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .testTag("pull_laptop_button"),
              ) {
                Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Pull From Laptop", style = MaterialTheme.typography.labelMedium)
              }
            }

            if (laptopHealth != null) {
              Surface(
                color = if (laptopHealth.reachable) MaterialTheme.semanticColors.successContainer else MaterialTheme.semanticColors.warningContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = laptopHealth.message,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (laptopHealth.reachable) MaterialTheme.semanticColors.success else MaterialTheme.semanticColors.warning,
                  modifier = Modifier.padding(10.dp),
                )
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showLaptopGuide = !showLaptopGuide }
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Laptop Server Setup Guide (laptop_server/server.py)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
              )
              Icon(
                imageVector = if (showLaptopGuide) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = "Toggle guide",
                tint = MaterialTheme.colorScheme.primary,
              )
            }

            if (showLaptopGuide) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "1. Connect your laptop and phone to the same Wi-Fi (or phone hotspot).",
                    style = MaterialTheme.typography.bodySmall,
                  )
                  Text(
                    text = "2. Copy laptop_server files into D:\\IDEAS AND PLANS\\1 BANANA CHIPS\\LOCAL SERVER and run start_homely_server.bat (or):",
                    style = MaterialTheme.typography.bodySmall,
                  )
                  Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                  ) {
                    Text(
                      text = "python server.py",
                      style = MaterialTheme.typography.bodySmall,
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                  Text(
                    text = "3. Copy the http://192.168.x.x:8787 address printed in the terminal, paste it above, and tap Save & Test. All database & JSON files update live inside that folder.",
                    style = MaterialTheme.typography.bodySmall,
                  )
                }
              }
            }
          }
        }
      }

      // 4. Phone Queue Records Section Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Saved Records on Phone ($totalQueueRecords)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = if (totalQueueRecords == 0) "Queue Clear" else "Stored in SQLite",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (totalQueueRecords == 0) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.semanticColors.success,
                modifier = Modifier.size(34.dp),
              )
              Text(
                text = "No pending temporary records on phone",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "When you add or edit shops, orders, products, or payments offline, they stay here safely until synced.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(pendingShops, key = { it.localId }) { item ->
          val fullySynced = item.isFullySynced(requireGlobal, requireLaptop)
          PendingShopQueueCard(
            item = item,
            requireGlobal = requireGlobal,
            requireLaptop = requireLaptop,
            fullySynced = fullySynced,
            onDeleteClick = {
              if (fullySynced) {
                onDeletePendingShop(item.localId)
              } else {
                confirmForceDeleteShop = item
              }
            },
          )
        }

        items(pendingOrders, key = { it.localId }) { order ->
          val fullySynced = order.isFullySynced(requireGlobal, requireLaptop)
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(
              1.dp,
              if (fullySynced) MaterialTheme.semanticColors.success.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = "Order ${order.orderCode} • ${order.shopName}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${order.itemsSummary} (${order.itemCount} pkts)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  TargetSyncChip("Cloud", order.globalSynced, requireGlobal)
                  TargetSyncChip("Laptop", order.localServerSynced, requireLaptop)
                }
                if (order.lastError.isNotBlank() && !fullySynced) {
                  Text(
                    text = order.lastError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.semanticColors.danger,
                  )
                }
              }
              if (fullySynced) {
                IconButton(onClick = { onDeletePendingOrder(order.localId) }) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete synced order from phone",
                    tint = MaterialTheme.semanticColors.danger,
                  )
                }
              }
            }
          }
        }

        items(pendingStockUpdates, key = { "stock_${it.stockId}" }) { stockUpdate ->
          val fullySynced = !requireGlobal || stockUpdate.globalSynced
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "Stock Update • ${stockUpdate.productName}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "New quantity: ${stockUpdate.newQuantity} pk",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              TargetSyncChip(label = "Global Cloud", synced = stockUpdate.globalSynced, active = requireGlobal)
              if (stockUpdate.lastError.isNotBlank() && !fullySynced) {
                Text(
                  text = stockUpdate.lastError,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.semanticColors.danger,
                )
              }
            }
          }
        }

        items(pendingProducts, key = { "prod_${it.localId}" }) { product ->
          val fullySynced = product.isFullySynced(requireGlobal)
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Product ${product.productCode} • ${product.name}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                TargetSyncChip(label = "Global Cloud", synced = product.globalSynced, active = requireGlobal)
                if (product.lastError.isNotBlank() && !fullySynced) {
                  Text(
                    text = product.lastError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.semanticColors.danger,
                  )
                }
              }
              if (fullySynced) {
                IconButton(onClick = { onDeletePendingProduct(product.localId) }) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete synced product from phone",
                    tint = MaterialTheme.semanticColors.danger,
                  )
                }
              }
            }
          }
        }

        items(pendingPayments, key = { "pay_${it.localId}" }) { payment ->
          val fullySynced = payment.isFullySynced(requireGlobal)
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Payment ${payment.receiptCode} • ${payment.partyName}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                TargetSyncChip(label = "Global Cloud", synced = payment.globalSynced, active = requireGlobal)
                if (payment.lastError.isNotBlank() && !fullySynced) {
                  Text(
                    text = payment.lastError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.semanticColors.danger,
                  )
                }
              }
              if (fullySynced) {
                IconButton(onClick = { onDeletePendingPayment(payment.localId) }) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete synced payment from phone",
                    tint = MaterialTheme.semanticColors.danger,
                  )
                }
              }
            }
          }
        }

        items(pendingSupplierBills, key = { "bill_${it.localId}" }) { bill ->
          val fullySynced = bill.isFullySynced(requireGlobal)
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Supplier Bill ${bill.billNumber} • ${bill.supplierName}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                TargetSyncChip(label = "Global Cloud", synced = bill.globalSynced, active = requireGlobal)
                if (bill.lastError.isNotBlank() && !fullySynced) {
                  Text(
                    text = bill.lastError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.semanticColors.danger,
                  )
                }
              }
              if (fullySynced) {
                IconButton(onClick = { onDeletePendingSupplierBill(bill.localId) }) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete synced supplier bill from phone",
                    tint = MaterialTheme.semanticColors.danger,
                  )
                }
              }
            }
          }
        }

        items(pendingSuppliers, key = { "sup_${it.localId}" }) { supplier ->
          val fullySynced = supplier.isFullySynced(requireGlobal)
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Supplier ${supplier.supplierCode} • ${supplier.name}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                TargetSyncChip(label = "Global Cloud", synced = supplier.globalSynced, active = requireGlobal)
                if (supplier.lastError.isNotBlank() && !fullySynced) {
                  Text(
                    text = supplier.lastError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.semanticColors.danger,
                  )
                }
              }
              if (fullySynced) {
                IconButton(onClick = { onDeletePendingSupplier(supplier.localId) }) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete synced supplier from phone",
                    tint = MaterialTheme.semanticColors.danger,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  confirmForceDeleteShop?.let { item ->
    AlertDialog(
      onDismissRequest = { confirmForceDeleteShop = null },
      title = { Text("Delete unsynced shop?") },
      text = {
        Text("${item.name} (${item.shopCode}) has not synced to all active servers yet. Deleting it from your phone will permanently discard it.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeletePendingShop(item.localId)
            confirmForceDeleteShop = null
          },
        ) {
          Text("Delete Anyway", color = MaterialTheme.semanticColors.danger)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmForceDeleteShop = null }) {
          Text("Keep on Phone")
        }
      },
    )
  }
}

@Composable
private fun MiniQueueStat(
  label: String,
  count: Int,
  container: Color,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = container,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = "$count",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = accent,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun PendingShopQueueCard(
  item: PendingShopEntity,
  requireGlobal: Boolean,
  requireLaptop: Boolean,
  fullySynced: Boolean,
  onDeleteClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(
      1.dp,
      if (fullySynced) MaterialTheme.semanticColors.success.copy(alpha = 0.55f) else MaterialTheme.semanticColors.warning.copy(alpha = 0.55f),
    ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
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
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = item.shopCode,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Text(
            text = item.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        IconButton(
          onClick = onDeleteClick,
          modifier = Modifier.size(36.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = if (fullySynced) "Delete synced shop from phone" else "Delete unsynced shop",
            tint = if (fullySynced) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Text(
        text = "${item.ownerName} • ${item.phone} • ${item.address}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TargetSyncChip(label = "Global Cloud", synced = item.globalSynced, active = requireGlobal)
        TargetSyncChip(label = "Laptop Server", synced = item.localServerSynced, active = requireLaptop)
      }

      if (item.lastError.isNotBlank() && !fullySynced) {
        Surface(
          color = MaterialTheme.semanticColors.dangerContainer.copy(alpha = 0.65f),
          shape = RoundedCornerShape(6.dp),
        ) {
          Text(
            text = item.lastError,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.semanticColors.danger,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun TargetSyncChip(
  label: String,
  synced: Boolean,
  active: Boolean,
) {
  val (bg, fg, statusText, icon) = when {
    synced -> Quad(MaterialTheme.semanticColors.successContainer, MaterialTheme.semanticColors.success, "$label: Synced", Icons.Rounded.CheckCircle)
    active -> Quad(MaterialTheme.semanticColors.warningContainer, MaterialTheme.semanticColors.warning, "$label: Pending", Icons.Rounded.Schedule)
    else -> Quad(MaterialTheme.semanticColors.infoContainer.copy(alpha = 0.55f), MaterialTheme.semanticColors.info, "$label: Off", Icons.Rounded.Schedule)
  }
  Surface(
    color = bg,
    shape = RoundedCornerShape(6.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
      Text(
        text = statusText,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = fg,
      )
    }
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
