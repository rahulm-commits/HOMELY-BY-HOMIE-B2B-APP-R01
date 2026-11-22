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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PinDrop
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Supplier
import com.example.data.model.SupplierCatalogHelper
import com.example.data.model.SupplierStatus
import com.example.ui.components.StandardCompactSearchBar
import com.example.ui.components.openDialer
import com.example.ui.components.openWhatsApp
import com.example.ui.components.rememberFileExistsAsync
import com.example.ui.components.rememberThumbnailImageRequest
import com.example.ui.theme.semanticColors

private enum class SupplierFilterTab(val label: String) {
  ALL("All Suppliers"),
  RECENTLY_ADDED("Recently Added"),
  RECENTLY_UPDATED("Recently Updated"),
  MISSING_DETAILS("Missing Details"),
  INACTIVE("Inactive"),
}

@Composable
fun SuppliersScreen(
  suppliers: List<Supplier>,
  pendingQueueCount: Int = 0,
  onOpenSyncServers: () -> Unit,
  onAddSupplierClick: () -> Unit,
  onEditSupplierClick: (Supplier) -> Unit,
  onChangeSupplierStatus: (supplierId: String, newStatus: String, feedbackLabel: String) -> Unit,
  onNavigateToSupplierPayouts: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf(SupplierFilterTab.ALL) }
  var selectedSheetSupplier by remember { mutableStateOf<Supplier?>(null) }
  var showAlertsDialog by remember { mutableStateOf(false) }
  val searchFocusRequester = remember { FocusRequester() }

  val nonDeleted = remember(suppliers) {
    suppliers.filter { it.status != SupplierStatus.DELETED }
  }
  val activeSuppliers = remember(nonDeleted) {
    nonDeleted.filter { it.status == SupplierStatus.ACTIVE }
  }
  val inactiveSuppliers = remember(nonDeleted) {
    nonDeleted.filter { it.status == SupplierStatus.INACTIVE }
  }
  val recentlyAdded = remember(nonDeleted) {
    nonDeleted.filter { SupplierCatalogHelper.isRecentlyAdded(it) }
  }
  val recentlyUpdated = remember(nonDeleted) {
    nonDeleted.filter { SupplierCatalogHelper.isRecentlyUpdated(it) }
  }
  val missingDetails = remember(nonDeleted) {
    nonDeleted.filter { SupplierCatalogHelper.hasMissingDetails(it) }
  }
  val kycVerifiedPercent = remember(nonDeleted) {
    if (nonDeleted.isEmpty()) 100
    else ((nonDeleted.count { it.kycVerified } * 100f) / nonDeleted.size).toInt()
  }
  val alertCount = remember(missingDetails, nonDeleted, pendingQueueCount) {
    missingDetails.size + maxOf(0, nonDeleted.count { !it.kycVerified }) + if (pendingQueueCount > 0) 1 else 0
  }

  val filteredSuppliers = remember(
    suppliers,
    searchQuery,
    selectedFilter,
  ) {
    val base = when (selectedFilter) {
      SupplierFilterTab.ALL -> activeSuppliers
      SupplierFilterTab.RECENTLY_ADDED -> recentlyAdded
      SupplierFilterTab.RECENTLY_UPDATED -> recentlyUpdated
      SupplierFilterTab.MISSING_DETAILS -> missingDetails
      SupplierFilterTab.INACTIVE -> inactiveSuppliers
    }
    val q = searchQuery.trim().lowercase()
    val qDigits = q.filter { it.isDigit() }
    if (q.isEmpty()) {
      base
    } else {
      base.filter { sup ->
        sup.name.lowercase().contains(q) ||
          sup.supplierCode.lowercase().contains(q) ||
          sup.contactPerson.lowercase().contains(q) ||
          sup.category.lowercase().contains(q) ||
          sup.address.lowercase().contains(q) ||
          sup.phone.lowercase().contains(q) ||
          (qDigits.isNotEmpty() && sup.phone.filter { it.isDigit() }.contains(qDigits))
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Header bar matching HOMELY BY HOMIE operational style
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "HOMELY BY HOMIE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "Suppliers",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "• Kochi Central Hub",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.secondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              IconButton(
                onClick = { searchFocusRequester.requestFocus() },
                modifier = Modifier.testTag("supplier_header_search_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Search,
                  contentDescription = "Focus supplier search",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Box(contentAlignment = Alignment.Center) {
                IconButton(
                  onClick = { showAlertsDialog = true },
                  modifier = Modifier.testTag("supplier_alerts_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Supplier alerts",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (alertCount > 0) {
                  Surface(
                    color = MaterialTheme.semanticColors.danger,
                    shape = CircleShape,
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .padding(top = 6.dp, end = 6.dp)
                      .size(16.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = alertCount.coerceAtMost(9).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError,
                      )
                    }
                  }
                }
              }

              Surface(
                color = if (pendingQueueCount > 0) {
                  MaterialTheme.semanticColors.warningContainer
                } else {
                  MaterialTheme.colorScheme.secondaryContainer
                },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .clickable(onClick = onOpenSyncServers)
                  .testTag("supplier_sync_button"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "Sync status",
                    tint = if (pendingQueueCount > 0) {
                      MaterialTheme.semanticColors.warning
                    } else {
                      MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = if (pendingQueueCount > 0) "Queue ($pendingQueueCount)" else "Sync",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (pendingQueueCount > 0) {
                      MaterialTheme.semanticColors.warning
                    } else {
                      MaterialTheme.colorScheme.onSecondaryContainer
                    },
                  )
                }
              }
            }
          }

          // Metrics Badges Ribbon
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(999.dp),
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
                    .background(MaterialTheme.colorScheme.secondary),
                )
                Text(
                  text = "${nonDeleted.size} Registered Suppliers",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
              }
            }

            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
              shape = RoundedCornerShape(999.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Hub,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = "Kochi Central Hub",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.secondary,
                )
              }
            }

            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(999.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Verified,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = "$kycVerifiedPercent% KYC Verified",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          // Search + Horizontal Filter Chips
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            StandardCompactSearchBar(
              query = searchQuery,
              onQueryChange = { searchQuery = it },
              placeholder = "Search by supplier name, ID, contact person, phone...",
              fieldTestTag = "supplier_search_input",
              modifier = Modifier.focusRequester(searchFocusRequester),
            )

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              SupplierFilterTab.entries.forEach { tab ->
                val isSelected = selectedFilter == tab
                val count = when (tab) {
                  SupplierFilterTab.ALL -> activeSuppliers.size
                  SupplierFilterTab.RECENTLY_ADDED -> recentlyAdded.size
                  SupplierFilterTab.RECENTLY_UPDATED -> recentlyUpdated.size
                  SupplierFilterTab.MISSING_DETAILS -> missingDetails.size
                  SupplierFilterTab.INACTIVE -> inactiveSuppliers.size
                }
                val labelText = if (tab == SupplierFilterTab.MISSING_DETAILS && count == 0) {
                  tab.label
                } else {
                  "${tab.label} ($count)"
                }
                val leadingIcon = when (tab) {
                  SupplierFilterTab.ALL -> null
                  SupplierFilterTab.RECENTLY_ADDED -> Icons.Rounded.FiberNew
                  SupplierFilterTab.RECENTLY_UPDATED -> Icons.Rounded.Update
                  SupplierFilterTab.MISSING_DETAILS -> Icons.Rounded.Warning
                  SupplierFilterTab.INACTIVE -> null
                }

                FilterChip(
                  selected = isSelected,
                  onClick = { selectedFilter = tab },
                  leadingIcon = leadingIcon?.let { icon ->
                    {
                      Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                      )
                    }
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
                  border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                  ),
                  label = {
                    Text(
                      text = labelText,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    )
                  },
                  modifier = Modifier
                    .height(34.dp)
                    .testTag("supplier_filter_${tab.name.lowercase()}"),
                )
              }

              if (searchQuery.isNotBlank() || selectedFilter != SupplierFilterTab.ALL) {
                TextButton(
                  onClick = {
                    searchQuery = ""
                    selectedFilter = SupplierFilterTab.ALL
                  },
                  modifier = Modifier.testTag("clear_supplier_filters"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.FilterAltOff,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Clear Filters",
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
              }
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
      }

      if (filteredSuppliers.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
          contentAlignment = Alignment.Center,
        ) {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("no_suppliers_matched_card"),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp),
                  )
                }
              }
              Text(
                text = "No suppliers matched",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Try adjusting your search keywords, phone digits, or clear category filter pills.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Button(
                onClick = {
                  searchQuery = ""
                  selectedFilter = SupplierFilterTab.ALL
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondary,
                  contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier.testTag("reset_supplier_filters_button"),
              ) {
                Text("Reset Search Filters")
              }
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("suppliers_lazy_list"),
          contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 92.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(
            filteredSuppliers,
            key = { it.id.ifBlank { it.supplierCode } },
            contentType = { "supplier_card" },
          ) { supplier ->
            SupplierCardItem(
              supplier = supplier,
              onCardClick = { selectedSheetSupplier = supplier },
              onEditClick = { onEditSupplierClick(supplier) },
              onMoreClick = { selectedSheetSupplier = supplier },
              onCallClick = { openDialer(context, supplier.phone) },
              onWhatsAppClick = {
                openWhatsApp(
                  context = context,
                  phone = supplier.phone,
                  message = "Hello ${supplier.contactPerson}, contacting from Homely By Homie regarding ${supplier.name}.",
                )
              },
            )
          }
        }
      }
    }

    ExtendedFloatingActionButton(
      onClick = onAddSupplierClick,
      icon = {
        Icon(
          imageVector = Icons.Rounded.PersonAdd,
          contentDescription = null,
        )
      },
      text = {
        Text(
          text = "+ Add Supplier",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
      },
      containerColor = MaterialTheme.colorScheme.secondary,
      contentColor = MaterialTheme.colorScheme.onSecondary,
      shape = RoundedCornerShape(999.dp),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 14.dp)
        .testTag("add_supplier_fab"),
    )
  }

  selectedSheetSupplier?.let { target ->
    SupplierQuickActionsSheet(
      supplier = target,
      onDismiss = { selectedSheetSupplier = null },
      onEditProfile = {
        selectedSheetSupplier = null
        onEditSupplierClick(target)
      },
      onToggleActiveStatus = { newStatus, label ->
        selectedSheetSupplier = null
        onChangeSupplierStatus(target.id, newStatus, label)
      },
      onPayBillsClick = {
        selectedSheetSupplier = null
        onNavigateToSupplierPayouts()
      },
    )
  }

  if (showAlertsDialog) {
    SupplierAlertsDialog(
      missingDetailsSuppliers = missingDetails,
      unverifiedSuppliers = nonDeleted.filter { !it.kycVerified },
      pendingQueueCount = pendingQueueCount,
      onDismiss = { showAlertsDialog = false },
      onSelectSupplier = { sup ->
        showAlertsDialog = false
        onEditSupplierClick(sup)
      },
      onOpenSync = {
        showAlertsDialog = false
        onOpenSyncServers()
      },
    )
  }
}

@Composable
private fun SupplierCardItem(
  supplier: Supplier,
  onCardClick: () -> Unit,
  onEditClick: () -> Unit,
  onMoreClick: () -> Unit,
  onCallClick: () -> Unit,
  onWhatsAppClick: () -> Unit,
) {
  val isNew = remember(supplier.addedDate, supplier.createdAt) {
    SupplierCatalogHelper.isRecentlyAdded(supplier)
  }
  val hasMissing = remember(supplier) {
    SupplierCatalogHelper.hasMissingDetails(supplier)
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onCardClick)
      .testTag("supplier_card_${supplier.supplierCode}"),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Top Card Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
          .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = "[ ${supplier.supplierCode} ]",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
          if (isNew) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = "NEW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
          }
          if (supplier.isPendingSync) {
            Surface(
              color = MaterialTheme.semanticColors.warningContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.CloudOff,
                  contentDescription = null,
                  tint = MaterialTheme.semanticColors.warning,
                  modifier = Modifier.size(11.dp),
                )
                Text(
                  text = "Offline",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.semanticColors.warning,
                )
              }
            }
          } else if (hasMissing) {
            Surface(
              color = MaterialTheme.semanticColors.dangerContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = "Incomplete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.semanticColors.danger,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          Text(
            text = "• ${supplier.category}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onEditClick,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("edit_supplier_${supplier.supplierCode}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = "Edit ${supplier.name}",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp),
            )
          }
          IconButton(
            onClick = onMoreClick,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("more_supplier_${supplier.supplierCode}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.MoreVert,
              contentDescription = "Quick actions for ${supplier.name}",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      // Core Details
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
      ) {
        SupplierThumbnailBadge(supplier = supplier)

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = supplier.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(15.dp),
            )
            val contactLine = buildString {
              append("Contact: ${supplier.contactPerson}")
              if (supplier.contactRole.isNotBlank()) {
                append(" (${supplier.contactRole})")
              }
            }
            Text(
              text = contactLine,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.PinDrop,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(15.dp),
            )
            val locText = listOf(supplier.address, supplier.pinCode)
              .filter { it.isNotBlank() }
              .joinToString(" • ")
              .ifBlank { "Location not set" }
            Text(
              text = locText,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Mail,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = supplier.email.ifBlank { "Email missing — tap Edit to add" },
              style = MaterialTheme.typography.bodySmall,
              color = if (supplier.email.isBlank()) {
                MaterialTheme.semanticColors.danger
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

      // Bottom Phone & Direct Call / WhatsApp Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Rounded.Call,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = SupplierCatalogHelper.formatPhoneDisplay(supplier.phone),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onCallClick)
              .testTag("call_supplier_${supplier.supplierCode}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = "Call ${supplier.name}",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "Call",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.secondary,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onWhatsAppClick)
              .testTag("whatsapp_supplier_${supplier.supplierCode}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Chat,
                contentDescription = "WhatsApp ${supplier.name}",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "WhatsApp",
                style = MaterialTheme.typography.labelMedium,
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

@Composable
private fun SupplierThumbnailBadge(supplier: Supplier) {
  val shortCode = remember(supplier.category) {
    when {
      supplier.category.contains("FMCG", ignoreCase = true) -> "FMCG"
      supplier.category.contains("Oil", ignoreCase = true) -> "OIL"
      supplier.category.contains("Produce", ignoreCase = true) -> "FRESH"
      supplier.category.contains("Packaging", ignoreCase = true) -> "PACK"
      else -> "SUP"
    }
  }
  val statusDotColor = when (supplier.status) {
    SupplierStatus.ACTIVE -> MaterialTheme.semanticColors.success
    SupplierStatus.INACTIVE -> MaterialTheme.semanticColors.warning
    else -> MaterialTheme.semanticColors.danger
  }

  Box(
    modifier = Modifier
      .size(64.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant),
  ) {
    val hasPhotoFile = rememberFileExistsAsync(supplier.photoUri)
    val supplierImageRequest = if (hasPhotoFile) {
      rememberThumbnailImageRequest(
        path = supplier.photoUri,
        targetSizePx = 192,
      )
    } else {
      null
    }
    if (supplierImageRequest != null) {
      AsyncImage(
        model = supplierImageRequest,
        contentDescription = "${supplier.name} facility",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.Inventory2,
          contentDescription = "${supplier.category} icon",
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(28.dp),
        )
        Text(
          text = shortCode,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.secondary,
        )
      }
    }

    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(4.dp)
        .size(12.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(2.dp)
          .clip(CircleShape)
          .background(statusDotColor),
      )
    }
  }
}
