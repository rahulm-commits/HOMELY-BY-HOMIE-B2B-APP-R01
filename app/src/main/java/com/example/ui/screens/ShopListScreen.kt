package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.example.ui.components.CompactDropdownMenuItem
import com.example.ui.components.StandardCompactSearchBar
import com.example.ui.components.StandardRouteDropdown
import com.example.ui.components.openDialer
import com.example.ui.components.openWhatsApp
import com.example.ui.components.rememberFileExistsAsync
import com.example.ui.components.rememberThumbnailImageRequest
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.semanticColors

private enum class ShopSectionTab(val label: String) {
  ALL("All Shops"),
  RECENTLY_ADDED("Recently Added"),
  RECENTLY_DELETED("Recently Deleted"),
  INACTIVE("Inactive Shops"),
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ShopListScreen(
  shops: List<Shop>,
  routes: List<DeliveryRoute> = RoutesCatalog.starterRoutes,
  pendingQueueCount: Int = 0,
  laptopServerEnabled: Boolean = false,
  onOpenSyncServers: () -> Unit = {},
  onAddShopClick: () -> Unit,
  onEditShopClick: (Shop) -> Unit,
  onChangeShopStatus: (shopId: String, newStatus: String, feedback: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedRoute by remember { mutableStateOf(RoutesCatalog.ALL) }
  var selectedTab by remember { mutableStateOf(ShopSectionTab.ALL) }

  val registeredCount = remember(shops) {
    shops.count { it.status != ShopStatus.DELETED }
  }
  val offlinePendingCount = remember(shops, pendingQueueCount) {
    maxOf(pendingQueueCount, shops.count { it.isPendingSync })
  }

  val filteredShops = remember(shops, searchQuery, selectedRoute, selectedTab) {
    val bySection = when (selectedTab) {
      ShopSectionTab.ALL -> shops.filter { it.status == ShopStatus.ACTIVE }
      ShopSectionTab.RECENTLY_ADDED -> shops.filter { it.status != ShopStatus.DELETED }.take(6)
      ShopSectionTab.RECENTLY_DELETED -> shops.filter { it.status == ShopStatus.DELETED }
      ShopSectionTab.INACTIVE -> shops.filter { it.status == ShopStatus.INACTIVE }
    }
    val byRoute = if (selectedRoute == RoutesCatalog.ALL) {
      bySection
    } else {
      bySection.filter { it.routeId == selectedRoute }
    }
    val q = searchQuery.trim().lowercase()
    if (q.isEmpty()) {
      byRoute
    } else {
      byRoute.filter { shop ->
        shop.name.lowercase().contains(q) ||
          shop.shopCode.lowercase().contains(q) ||
          shop.ownerName.lowercase().contains(q) ||
          shop.phone.lowercase().contains(q)
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 82.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      // Scrollable Top Summary & Route Selector
      item(key = "shop_top_header") {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 0.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Registered Shops",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                  .weight(1f)
                  .padding(end = 8.dp),
              )

              Box(
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .heightIn(min = 48.dp)
                  .clip(RoundedCornerShape(999.dp))
                  .clickable(onClick = onOpenSyncServers)
                  .testTag("shop_sync_servers_button"),
                contentAlignment = Alignment.Center,
              ) {
                Surface(
                  color = if (offlinePendingCount > 0) MaterialTheme.semanticColors.warningContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                  shape = RoundedCornerShape(999.dp),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Sync,
                      contentDescription = "Open Phone Queue and Servers",
                      tint = if (offlinePendingCount > 0) MaterialTheme.semanticColors.warning else MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = if (offlinePendingCount > 0) {
                        "Queue ($offlinePendingCount)"
                      } else if (laptopServerEnabled) {
                        "Servers"
                      } else {
                        "Sync"
                      },
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = if (offlinePendingCount > 0) MaterialTheme.semanticColors.warning else MaterialTheme.colorScheme.onPrimaryContainer,
                      maxLines = 1,
                      softWrap = false,
                    )
                  }
                }
              }
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              val badgeTextStyle = TextStyle(
                fontFamily = PlusJakartaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
              )
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                  .heightIn(min = 22.dp, max = 28.dp)
                  .testTag("shop_count_badge"),
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                  Text(
                    text = "$registeredCount shops",
                    style = badgeTextStyle,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
              if (offlinePendingCount > 0) {
                Surface(
                  color = MaterialTheme.semanticColors.warningContainer,
                  shape = RoundedCornerShape(999.dp),
                  modifier = Modifier
                    .heightIn(min = 22.dp, max = 28.dp)
                    .testTag("shop_pending_badge"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.CloudOff,
                      contentDescription = null,
                      tint = MaterialTheme.semanticColors.warning,
                      modifier = Modifier.size(12.dp),
                    )
                    Text(
                      text = "$offlinePendingCount pending",
                      style = badgeTextStyle,
                      color = MaterialTheme.semanticColors.warning,
                      maxLines = 1,
                      softWrap = false,
                    )
                  }
                }
              }
            }

            // Standard Route Selection Dropdown (matching OrderScreen)
            StandardRouteDropdown(
              selectedRoute = selectedRoute,
              routes = routes,
              onRouteSelected = { selectedRoute = it },
              fieldTestTag = "shop_route_filter",
            )
          }
        }
      }

      // Sticky Search Bar + Section Toggle Chips
      stickyHeader(key = "shop_sticky_controls") {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 0.dp,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              // Compact Search Bar (matching OrderScreen)
              StandardCompactSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search name, ID, contact or phone...",
                fieldTestTag = "shop_search_input",
              )

              // Section chips: All Shops, Recently Added, Recently Deleted, Inactive
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                ShopSectionTab.entries.forEach { tab ->
                  val isSelected = selectedTab == tab
                  FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primary,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
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
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                      )
                    },
                    modifier = Modifier
                      .height(34.dp)
                      .testTag("shop_tab_${tab.name.lowercase()}"),
                  )
                }
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
          }
        }
      }

      if (filteredShops.isEmpty()) {
        item(key = "shops_empty_state") {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp),
              )
              Text(
                text = "No shops in ${selectedTab.label}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Tap Add Shop below to register a shop — works offline too.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(
          filteredShops,
          key = { it.id.ifBlank { it.shopCode } },
          contentType = { "shop_card" },
        ) { shop ->
          Box(modifier = Modifier.padding(horizontal = 12.dp)) {
            ShopCardItem(
              shop = shop,
              onCall = { openDialer(context, shop.phone) },
              onWhatsApp = {
                openWhatsApp(context, shop.phone, "Hello ${shop.ownerName}, contacting from Homely by Homie regarding ${shop.name}.")
              },
              onEdit = { onEditShopClick(shop) },
              onChangeStatus = onChangeShopStatus,
            )
          }
        }
      }
    }

    // Bottom-center Add Shop button as requested
    ExtendedFloatingActionButton(
      onClick = onAddShopClick,
      icon = {
        Icon(
          imageVector = Icons.Rounded.AddBusiness,
          contentDescription = null,
        )
      },
      text = {
        Text(
          text = "Add Shop",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
      },
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 14.dp)
        .testTag("add_shop_fab"),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopCardItem(
  shop: Shop,
  onCall: () -> Unit,
  onWhatsApp: () -> Unit,
  onEdit: () -> Unit,
  onChangeStatus: (shopId: String, newStatus: String, feedback: String) -> Unit,
) {
  var menuOpen by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
      ) {
        // Small shop photo thumbnail (off-main-thread file check + bounded Coil decode)
        val hasPhotoFile = rememberFileExistsAsync(shop.photoUri)
        val imageRequest = if (hasPhotoFile) {
          rememberThumbnailImageRequest(
            path = shop.photoUri,
            targetSizePx = 220,
            fallbackDrawableRes = R.drawable.img_shop_placeholder,
          )
        } else {
          null
        }
        Surface(
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.size(68.dp),
        ) {
          if (imageRequest != null) {
            AsyncImage(
              model = imageRequest,
              contentDescription = "${shop.name} photo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Image(
              painter = painterResource(id = R.drawable.img_shop_placeholder),
              contentDescription = "${shop.name} storefront",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.align(Alignment.CenterVertically),
            ) {
              Text(
                text = shop.shopCode,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
            if (shop.isPendingSync) {
              Surface(
                color = MaterialTheme.semanticColors.warningContainer,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.CenterVertically),
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
                    text = "Offline Pending",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.semanticColors.warning,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }

          Text(
            text = shop.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "${shop.ownerName} • ${shop.phone}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "${shop.address} • PIN ${shop.pinCode}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Box {
          IconButton(onClick = { menuOpen = true }) {
            Icon(
              imageVector = Icons.Rounded.MoreHoriz,
              contentDescription = "More shop actions",
            )
          }
          DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier
              .widthIn(min = 175.dp, max = 240.dp)
              .heightIn(max = 220.dp),
          ) {
            if (shop.status == ShopStatus.DELETED) {
              CompactDropdownMenuItem(
                text = "Restore Shop",
                leadingIcon = { Icon(Icons.Rounded.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp)) },
                onClick = {
                  menuOpen = false
                  onChangeStatus(shop.id, ShopStatus.ACTIVE, "${shop.name} restored")
                },
              )
            } else {
              val toggleTarget = if (shop.status == ShopStatus.INACTIVE) ShopStatus.ACTIVE else ShopStatus.INACTIVE
              val toggleLabel = if (shop.status == ShopStatus.INACTIVE) "Mark Active" else "Mark Inactive"
              CompactDropdownMenuItem(
                text = toggleLabel,
                onClick = {
                  menuOpen = false
                  onChangeStatus(shop.id, toggleTarget, "${shop.name} marked $toggleTarget")
                },
              )
              CompactDropdownMenuItem(
                text = "Move to Recently Deleted",
                textColor = MaterialTheme.semanticColors.danger,
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.semanticColors.danger, modifier = Modifier.size(16.dp)) },
                onClick = {
                  menuOpen = false
                  onChangeStatus(shop.id, ShopStatus.DELETED, "${shop.name} moved to Recently Deleted")
                },
              )
            }
          }
        }
      }

      // Action bar: Call, WhatsApp, Edit
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = MaterialTheme.semanticColors.successContainer,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, MaterialTheme.semanticColors.success.copy(alpha = 0.45f)),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onCall),
        ) {
          Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Call,
              contentDescription = "Call ${shop.name}",
              tint = MaterialTheme.semanticColors.success,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Call",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.semanticColors.success,
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onWhatsApp),
        ) {
          Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Chat,
              contentDescription = "WhatsApp ${shop.name}",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "WhatsApp",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onEdit),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = "Edit ${shop.name}",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Edit",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
  }
}
