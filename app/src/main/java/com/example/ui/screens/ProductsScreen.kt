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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Product
import com.example.data.model.ProductCatalogHelper
import com.example.data.model.ProductStatus
import com.example.ui.components.CompactDropdownMenuItem
import com.example.ui.components.InlineQuickDeltaPill
import com.example.ui.components.QuickStockAdjustDialog
import com.example.ui.components.StandardCompactSearchBar
import com.example.ui.components.rememberThumbnailImageRequest
import com.example.ui.theme.semanticColors

private enum class ProductStatusTab(val label: String) {
  ALL("All"),
  ACTIVE("Active"),
  LOW_STOCK("Low Stock"),
  INACTIVE("Inactive"),
}

private enum class ProductSortOption(val label: String) {
  HIGHEST_SELLING("Highest Selling (Velocity)"),
  STOCK_LOW_TO_HIGH("Stock Level: Low to High"),
  RECENTLY_ADDED("Recently Added First"),
  NAME_A_Z("Product Name (A - Z)"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductsScreen(
  products: List<Product>,
  pendingQueueCount: Int = 0,
  onOpenSyncServers: () -> Unit = {},
  onAddProductClick: () -> Unit,
  onEditProductClick: (Product) -> Unit,
  onChangeProductStatus: (productId: String, newStatus: String, feedbackLabel: String) -> Unit,
  onUpdateProductStock: (productId: String, newQuantity: Int, productName: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedTab by remember { mutableStateOf(ProductStatusTab.ALL) }
  var selectedCategory by remember { mutableStateOf("All Categories") }
  var sortOption by remember { mutableStateOf(ProductSortOption.HIGHEST_SELLING) }
  var showSortSheet by remember { mutableStateOf(false) }
  var showBarcodeLookupDialog by remember { mutableStateOf(false) }
  var stockDialogProduct by remember { mutableStateOf<Product?>(null) }
  var showSearchBarFocusedHint by remember { mutableStateOf(false) }

  val totalCount = products.size
  val activeCount = remember(products) { products.count { it.status == ProductStatus.ACTIVE } }
  val lowStockCount = remember(products) { products.count { ProductCatalogHelper.isLowStock(it) } }
  val inactiveCount = remember(products) {
    products.count { it.status == ProductStatus.INACTIVE || it.status == ProductStatus.DRAFT }
  }
  val lowAndInactiveCount = lowStockCount + inactiveCount
  val recentAddedCount = remember(products) {
    products.count { ProductCatalogHelper.isRecentlyAdded(it) }
  }
  val offlinePendingCount = remember(products, pendingQueueCount) {
    maxOf(pendingQueueCount, products.count { it.isPendingSync })
  }

  val categoryChips = remember(products) {
    val dynamicCats = products.map { it.category.trim() }.filter { it.isNotBlank() }
    listOf("All Categories") + (ProductCatalogHelper.defaultCategories + dynamicCats).distinct()
  }

  val filteredProducts = remember(products, searchQuery, selectedTab, selectedCategory, sortOption) {
    val byTab = when (selectedTab) {
      ProductStatusTab.ALL -> products
      ProductStatusTab.ACTIVE -> products.filter { it.status == ProductStatus.ACTIVE }
      ProductStatusTab.LOW_STOCK -> products.filter { ProductCatalogHelper.isLowStock(it) }
      ProductStatusTab.INACTIVE -> products.filter {
        it.status == ProductStatus.INACTIVE || it.status == ProductStatus.DRAFT
      }
    }
    val byCategory = if (selectedCategory == "All Categories") {
      byTab
    } else {
      byTab.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }
    val q = searchQuery.trim().lowercase()
    val bySearch = if (q.isEmpty()) {
      byCategory
    } else {
      byCategory.filter { prod ->
        prod.name.lowercase().contains(q) ||
          prod.productCode.lowercase().contains(q) ||
          prod.sku.lowercase().contains(q) ||
          prod.barcode.lowercase().contains(q) ||
          prod.brand.lowercase().contains(q) ||
          prod.category.lowercase().contains(q)
      }
    }
    when (sortOption) {
      ProductSortOption.HIGHEST_SELLING -> bySearch.sortedWith(
        compareByDescending<Product> { it.salesVelocity }.thenBy { it.name.lowercase() }
      )
      ProductSortOption.STOCK_LOW_TO_HIGH -> bySearch.sortedWith(
        compareBy<Product> { it.stockQuantity }.thenBy { it.name.lowercase() }
      )
      ProductSortOption.RECENTLY_ADDED -> bySearch.sortedWith(
        compareByDescending<Product> { it.addedDate }.thenBy { it.name.lowercase() }
      )
      ProductSortOption.NAME_A_Z -> bySearch.sortedBy { it.name.lowercase() }
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
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      // Scrollable Top App Identity Header
      item(key = "products_identity_header") {
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
                .padding(horizontal = 12.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "HOMELY BY HOMIE",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = "Products",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "• Kochi Central Hub",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                IconButton(
                  onClick = { showSearchBarFocusedHint = !showSearchBarFocusedHint },
                  modifier = Modifier.testTag("header_search_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search catalog",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                IconButton(
                  onClick = { selectedTab = ProductStatusTab.LOW_STOCK },
                  modifier = Modifier.testTag("header_alerts_button"),
                ) {
                  BadgedBox(
                    badge = {
                      if (lowAndInactiveCount > 0) {
                        Badge(
                          containerColor = MaterialTheme.semanticColors.danger,
                          contentColor = Color.White,
                        ) {
                          Text("$lowAndInactiveCount")
                        }
                      }
                    },
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Notifications,
                      contentDescription = "Low stock & inactive alerts",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = CircleShape,
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenSyncServers),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.Person,
                      contentDescription = "Sync & account status",
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(19.dp),
                    )
                  }
                }
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
          }
        }
      }

      // Scrollable Overview Header + Top Add Product Button
      item(key = "products_overview_header") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
              text = "Products",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onBackground,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Manage your grocery product catalogue",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Button(
            onClick = onAddProductClick,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier.testTag("top_add_product_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Product", style = MaterialTheme.typography.labelLarge)
          }
        }
      }

      // Scrollable Hub Status & Live Sync Bar
      item(key = "products_hub_status") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = MaterialTheme.semanticColors.successContainer,
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.semanticColors.success.copy(alpha = 0.35f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.semanticColors.success),
              )
              Text(
                text = "Kochi Central Hub",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.semanticColors.success,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onOpenSyncServers)
              .padding(horizontal = 4.dp, vertical = 2.dp),
          ) {
            Icon(
              imageVector = if (offlinePendingCount > 0) Icons.Rounded.CloudOff else Icons.Rounded.CloudDone,
              contentDescription = null,
              tint = if (offlinePendingCount > 0) {
                MaterialTheme.semanticColors.warning
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = if (offlinePendingCount > 0) {
                "$offlinePendingCount pending sync"
              } else {
                "Live Inventory Synced"
              },
              style = MaterialTheme.typography.labelMedium,
              color = if (offlinePendingCount > 0) {
                MaterialTheme.semanticColors.warning
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            )
          }
        }
      }

      // Scrollable 2x2 KPI Summary Cards
      item(key = "products_kpi_grid") {
        Column(
          modifier = Modifier.padding(horizontal = 12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            ProductKpiCard(
              label = "Total SKUs",
              value = "$totalCount",
              subtitle = "Catalogue total",
              icon = Icons.Rounded.Inventory2,
              accentColor = MaterialTheme.colorScheme.primary,
              iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
              onClick = { selectedTab = ProductStatusTab.ALL },
              modifier = Modifier.weight(1f),
            )
            ProductKpiCard(
              label = "Active Live",
              value = "$activeCount",
              subtitle = "In distribution",
              icon = Icons.Rounded.Verified,
              accentColor = MaterialTheme.semanticColors.success,
              iconContainerColor = MaterialTheme.semanticColors.successContainer,
              onClick = { selectedTab = ProductStatusTab.ACTIVE },
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            ProductKpiCard(
              label = "Low & Inactive",
              value = "$lowAndInactiveCount",
              valueTrailing = "($lowStockCount Low)",
              subtitle = "Action required",
              icon = Icons.Rounded.WarningAmber,
              accentColor = MaterialTheme.semanticColors.danger,
              iconContainerColor = MaterialTheme.semanticColors.dangerContainer,
              onClick = { selectedTab = ProductStatusTab.LOW_STOCK },
              modifier = Modifier.weight(1f),
            )
            ProductKpiCard(
              label = "Recent Added",
              value = "$recentAddedCount",
              subtitle = "Last 14 days",
              icon = Icons.Rounded.NewReleases,
              accentColor = MaterialTheme.colorScheme.secondary,
              iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
              onClick = {
                sortOption = ProductSortOption.RECENTLY_ADDED
                selectedTab = ProductStatusTab.ALL
              },
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Sticky Search Bar + Status Tabs + Category/Sort Toggle Controls
      stickyHeader(key = "products_sticky_controls") {
        Surface(
          color = MaterialTheme.colorScheme.background,
          tonalElevation = 0.dp,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              // Search & Quick Barcode/SKU Lookup Bar
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                StandardCompactSearchBar(
                  query = searchQuery,
                  onQueryChange = { searchQuery = it },
                  placeholder = if (showSearchBarFocusedHint) {
                    "Type PRD-00101, SKU-BNC-001, or product name..."
                  } else {
                    "Search by name, SKU, or barcode..."
                  },
                  fieldTestTag = "products_search_input",
                  containerColor = MaterialTheme.colorScheme.surface,
                  modifier = Modifier.weight(1f),
                )
                Surface(
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                  modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showBarcodeLookupDialog = true }
                    .testTag("products_barcode_lookup_button"),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Rounded.QrCodeScanner,
                      contentDescription = "Quick Barcode or SKU Lookup",
                      tint = MaterialTheme.colorScheme.onSecondaryContainer,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
              }

              // Status Tabs Row
              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  val tabs = listOf(
                    Triple(ProductStatusTab.ALL, "All ($totalCount)", "tab_products_all"),
                    Triple(ProductStatusTab.ACTIVE, "Active ($activeCount)", "tab_products_active"),
                    Triple(ProductStatusTab.LOW_STOCK, "Low Stock ($lowStockCount)", "tab_products_low"),
                    Triple(ProductStatusTab.INACTIVE, "Inactive ($inactiveCount)", "tab_products_inactive"),
                  )
                  tabs.forEach { (tab, label, tag) ->
                    val isSelected = selectedTab == tab
                    val textColor = when {
                      isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                      tab == ProductStatusTab.LOW_STOCK -> MaterialTheme.semanticColors.danger
                      else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                      color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedTab = tab }
                        .testTag(tag),
                    ) {
                      Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                      )
                    }
                  }
                }
              }

              // Category Chips + Sort Pill
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  categoryChips.forEach { category ->
                    val selected = selectedCategory == category
                    FilterChip(
                      selected = selected,
                      onClick = { selectedCategory = category },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      ),
                      border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                      ),
                      label = {
                        Text(
                          text = category,
                          style = MaterialTheme.typography.labelMedium,
                          maxLines = 1,
                        )
                      },
                      modifier = Modifier.height(34.dp),
                    )
                  }
                }

                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showSortSheet = true }
                    .testTag("sort_products_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Rounded.Sort,
                      contentDescription = "Sort products",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp),
                    )
                    Text(
                      text = when (sortOption) {
                        ProductSortOption.HIGHEST_SELLING -> "Sort: Sales"
                        ProductSortOption.STOCK_LOW_TO_HIGH -> "Sort: Stock"
                        ProductSortOption.RECENTLY_ADDED -> "Sort: Recent"
                        ProductSortOption.NAME_A_Z -> "Sort: A-Z"
                      },
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          }
        }
      }

      // Empty State or Filtered Product Cards
      if (filteredProducts.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
              )
              Text(
                text = "No matching products",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Adjust filters or tap + Add Product to register a new SKU.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              OutlinedButton(
                onClick = {
                  searchQuery = ""
                  selectedTab = ProductStatusTab.ALL
                  selectedCategory = "All Categories"
                },
              ) {
                Text("Reset Filters")
              }
            }
          }
        }
      } else {
        items(
          filteredProducts,
          key = { it.id.ifBlank { it.productCode } },
          contentType = { "product_card" },
        ) { prod ->
          Box(modifier = Modifier.padding(horizontal = 12.dp)) {
            ProductCatalogCard(
              product = prod,
              onEditClick = { onEditProductClick(prod) },
              onToggleStatus = {
                val next = if (prod.status == ProductStatus.ACTIVE) {
                  ProductStatus.INACTIVE
                } else {
                  ProductStatus.ACTIVE
                }
                val msg = if (next == ProductStatus.ACTIVE) {
                  "${prod.name} marked Active"
                } else {
                  "${prod.name} marked Inactive"
                }
                onChangeProductStatus(prod.id, next, msg)
              },
              onQuickRestock = {
                onUpdateProductStock(prod.id, prod.stockQuantity + 20, prod.name)
              },
              onQuickStockChange = { targetQty ->
                onUpdateProductStock(prod.id, targetQty.coerceAtLeast(0), prod.name)
              },
              onOpenStockAdjust = { stockDialogProduct = prod },
            )
          }
        }
      }
    }

    // Floating Bottom Quick Action Pills
    Row(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 14.dp, bottom = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        onClick = onOpenSyncServers,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shadowElevation = 6.dp,
        modifier = Modifier.testTag("sync_catalog_pill"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = if (offlinePendingCount > 0) Icons.Rounded.Sync else Icons.Rounded.CheckCircle,
            contentDescription = "Sync catalog status",
            tint = MaterialTheme.semanticColors.success,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = if (offlinePendingCount > 0) "Sync ($offlinePendingCount)" else "Synced",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Button(
        onClick = onAddProductClick,
        shape = CircleShape,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
        modifier = Modifier.testTag("add_product_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add Product", style = MaterialTheme.typography.labelLarge)
      }
    }
  }

  if (showSortSheet) {
    ModalBottomSheet(
      onDismissRequest = { showSortSheet = false },
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "Sort Products By",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        ProductSortOption.entries.forEach { opt ->
          val selected = sortOption == opt
          Surface(
            color = if (selected) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                sortOption = opt
                showSortSheet = false
              },
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = opt.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              )
              RadioButton(
                selected = selected,
                onClick = {
                  sortOption = opt
                  showSortSheet = false
                },
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }

  if (showBarcodeLookupDialog) {
    BarcodeQuickLookupDialog(
      products = products,
      onApplyQuery = { code ->
        searchQuery = code
        showBarcodeLookupDialog = false
      },
      onDismiss = { showBarcodeLookupDialog = false },
    )
  }

  val adjustingProduct = stockDialogProduct
  if (adjustingProduct != null) {
    QuickStockAdjustDialog(
      productName = adjustingProduct.name,
      productCode = adjustingProduct.productCode,
      packSize = adjustingProduct.packSize.ifBlank { adjustingProduct.baseUnit }.ifBlank { "Standard" },
      currentQuantity = adjustingProduct.stockQuantity,
      minThreshold = adjustingProduct.minThreshold,
      unitLabel = ProductCatalogHelper.unitShortLabel(adjustingProduct.sellingUnit),
      inputTestTag = "stock_adjust_input",
      onConfirm = { newQty ->
        onUpdateProductStock(adjustingProduct.id, newQty, adjustingProduct.name)
        stockDialogProduct = null
      },
      onDismiss = { stockDialogProduct = null },
    )
  }
}

@Composable
private fun ProductKpiCard(
  label: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  iconContainerColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  valueTrailing: String? = null,
) {
  Card(
    onClick = onClick,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = accentColor,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Surface(
          color = iconContainerColor,
          shape = CircleShape,
          modifier = Modifier.size(28.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = value,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = if (label.contains("Low")) accentColor else MaterialTheme.colorScheme.onSurface,
        )
        if (!valueTrailing.isNullOrBlank()) {
          Text(
            text = valueTrailing,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 3.dp),
          )
        }
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun ProductCatalogCard(
  product: Product,
  onEditClick: () -> Unit,
  onToggleStatus: () -> Unit,
  onQuickRestock: () -> Unit,
  onQuickStockChange: (Int) -> Unit,
  onOpenStockAdjust: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val variants = remember(product.hasVariants, product.variantsJson) {
    if (product.hasVariants) ProductCatalogHelper.decodeVariants(product.variantsJson) else emptyList()
  }
  val isLow = ProductCatalogHelper.isLowStock(product)
  val isCritical = isLow && product.stockQuantity <= (product.minThreshold / 2).coerceAtLeast(10)
  val isInactive = product.status != ProductStatus.ACTIVE
  val unitShort = ProductCatalogHelper.unitShortLabel(product.sellingUnit)

  val cornerBadgeText = when {
    isInactive && product.stockQuantity == 0 -> "0 $unitShort Stock"
    isCritical -> "Crit. ${product.stockQuantity} $unitShort"
    isLow -> "Low ${product.stockQuantity}$unitShort"
    variants.isNotEmpty() -> "${variants.size} VAR"
    product.packSize.isNotBlank() -> product.packSize.uppercase()
    else -> product.baseUnit
  }

  val cornerBadgeColor = when {
    isInactive && product.stockQuantity == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
    isLow -> MaterialTheme.semanticColors.danger
    else -> MaterialTheme.colorScheme.primary
  }
  val cornerBadgeTextColor = when {
    isInactive && product.stockQuantity == 0 -> MaterialTheme.colorScheme.onSurface
    isLow -> Color.White
    else -> MaterialTheme.colorScheme.onPrimary
  }

  val statusChipColor = when (product.status) {
    ProductStatus.ACTIVE -> MaterialTheme.semanticColors.successContainer
    ProductStatus.DRAFT -> MaterialTheme.semanticColors.warningContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
  }
  val statusTextColor = when (product.status) {
    ProductStatus.ACTIVE -> MaterialTheme.semanticColors.success
    ProductStatus.DRAFT -> MaterialTheme.semanticColors.warning
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  val footerContainerColor = when {
    isLow -> MaterialTheme.semanticColors.dangerContainer.copy(alpha = 0.55f)
    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
  }

  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (isInactive) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
      } else {
        MaterialTheme.colorScheme.surface
      },
    ),
    border = BorderStroke(
      width = 1.dp,
      color = if (isLow) {
        MaterialTheme.semanticColors.danger.copy(alpha = 0.4f)
      } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
      },
    ),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .alpha(if (isInactive) 0.88f else 1f)
      .testTag("product_card_${product.productCode}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(11.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
      ) {
        // Thumbnail with corner pill
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
          val grayscaleFilter = remember(isInactive) {
            if (isInactive) {
              ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            } else {
              null
            }
          }
          val productImageRequest = rememberThumbnailImageRequest(
            path = product.photoUri,
            targetSizePx = 240,
            fallbackDrawableRes = R.drawable.img_snacks_banner,
          )
          if (productImageRequest != null) {
            AsyncImage(
              model = productImageRequest,
              contentDescription = product.name,
              contentScale = ContentScale.Crop,
              colorFilter = grayscaleFilter,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Image(
              painter = painterResource(id = R.drawable.img_snacks_banner),
              contentDescription = product.name,
              contentScale = ContentScale.Crop,
              colorFilter = grayscaleFilter,
              modifier = Modifier.fillMaxSize(),
            )
          }
          Surface(
            color = cornerBadgeColor,
            shape = CircleShape,
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(4.dp),
          ) {
            Text(
              text = cornerBadgeText,
              style = MaterialTheme.typography.labelSmall,
              color = cornerBadgeTextColor,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                color = statusChipColor,
                shape = CircleShape,
              ) {
                Text(
                  text = product.status.uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  color = statusTextColor,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
              if (product.isPendingSync) {
                Surface(
                  color = MaterialTheme.semanticColors.warningContainer,
                  shape = CircleShape,
                ) {
                  Text(
                    text = "PENDING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.semanticColors.warning,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onEditClick,
                modifier = Modifier
                  .size(36.dp)
                  .testTag("edit_product_${product.productCode}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Edit,
                  contentDescription = "Edit ${product.name}",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp),
                )
              }
              Box {
                IconButton(
                  onClick = { menuExpanded = true },
                  modifier = Modifier
                    .size(36.dp)
                    .testTag("menu_product_${product.productCode}"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More actions for ${product.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                }
                DropdownMenu(
                  expanded = menuExpanded,
                  onDismissRequest = { menuExpanded = false },
                  modifier = Modifier
                    .widthIn(min = 190.dp, max = 250.dp)
                    .heightIn(max = 240.dp),
                ) {
                  CompactDropdownMenuItem(
                    text = "Edit Product & Pricing",
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = {
                      menuExpanded = false
                      onEditClick()
                    },
                  )
                  CompactDropdownMenuItem(
                    text = "Quick Restock (+20 $unitShort)",
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = {
                      menuExpanded = false
                      onQuickRestock()
                    },
                  )
                  CompactDropdownMenuItem(
                    text = "Adjust Stock Quantity",
                    leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = {
                      menuExpanded = false
                      onOpenStockAdjust()
                    },
                  )
                  CompactDropdownMenuItem(
                    text = if (product.status == ProductStatus.ACTIVE) "Mark as Inactive" else "Mark as Active",
                    leadingIcon = { Icon(Icons.Rounded.Storefront, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    onClick = {
                      menuExpanded = false
                      onToggleStatus()
                    },
                  )
                }
              }
            }
          }

          Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = buildString {
              append(product.category)
              if (product.brand.isNotBlank()) append(" • ${product.brand}")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = product.productCode,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
            if (product.sku.isNotBlank()) {
              Text(
                text = "• ${product.sku}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      // Wholesale Price & One-Tap Stock Strip
      Surface(
        color = footerContainerColor,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              val topLabel = when {
                variants.isNotEmpty() -> "WHOLESALE SLAB"
                product.mrp > 0.0 -> "WHOLESALE (MRP ₹${product.mrp.toInt()})"
                else -> "WHOLESALE PRICE"
              }
              Text(
                text = topLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = ProductCatalogHelper.formatPriceDisplay(product),
                  style = MaterialTheme.typography.titleMedium,
                  color = if (isLow) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "/ ${product.sellingUnit.substringBefore(" /").lowercase()}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Column(
              horizontalAlignment = Alignment.End,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenStockAdjust)
                .testTag("product_stock_badge_${product.productCode}"),
            ) {
              val chipColor = when {
                isLow -> MaterialTheme.semanticColors.danger
                isInactive -> MaterialTheme.colorScheme.surfaceContainerHighest
                else -> MaterialTheme.semanticColors.successContainer
              }
              val chipTextColor = when {
                isLow -> Color.White
                isInactive -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.semanticColors.success
              }
              Surface(
                color = chipColor,
                shape = CircleShape,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = when {
                      isLow -> Icons.Rounded.WarningAmber
                      isInactive -> Icons.Rounded.Schedule
                      else -> Icons.Rounded.CheckCircle
                    },
                    contentDescription = null,
                    tint = chipTextColor,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = when {
                      isInactive && product.stockQuantity == 0 -> "Restocking Soon"
                      isLow -> "${product.stockQuantity} $unitShort left"
                      else -> "${product.stockQuantity} $unitShort"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = chipTextColor,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = when {
                  isCritical -> "Critical (Min ${product.minThreshold})"
                  isLow -> "Min Reorder: ${product.minThreshold} $unitShort"
                  isInactive -> "Inactive in catalogue"
                  product.stockQuantity >= 120 -> "Healthy • Tap to edit"
                  else -> "Optimal • Tap to edit"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isLow) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            InlineQuickDeltaPill(
              label = "-10",
              isAdd = false,
              enabled = product.stockQuantity > 0,
              onClick = { onQuickStockChange((product.stockQuantity - 10).coerceAtLeast(0)) },
              modifier = Modifier.testTag("product_quick_minus10_${product.productCode}"),
            )
            InlineQuickDeltaPill(
              label = "-1",
              isAdd = false,
              enabled = product.stockQuantity > 0,
              onClick = { onQuickStockChange((product.stockQuantity - 1).coerceAtLeast(0)) },
              modifier = Modifier.testTag("product_quick_minus1_${product.productCode}"),
            )
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenStockAdjust),
            ) {
              Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Tune,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${product.stockQuantity} $unitShort",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                )
              }
            }
            InlineQuickDeltaPill(
              label = "+1",
              isAdd = true,
              onClick = { onQuickStockChange(product.stockQuantity + 1) },
              modifier = Modifier.testTag("product_quick_plus1_${product.productCode}"),
            )
            InlineQuickDeltaPill(
              label = "+10",
              isAdd = true,
              onClick = { onQuickStockChange(product.stockQuantity + 10) },
              modifier = Modifier.testTag("product_quick_plus10_${product.productCode}"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BarcodeQuickLookupDialog(
  products: List<Product>,
  onApplyQuery: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var codeInput by remember { mutableStateOf("") }
  val samples = remember(products) { products.take(4) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Barcode / SKU Lookup") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Enter an EAN barcode, internal SKU, or product code to jump straight to that item.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = codeInput,
          onValueChange = { codeInput = it },
          label = { Text("SKU / Barcode / Product Code") },
          placeholder = { Text("e.g. SKU-BNC-001 or 8901234567890") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("barcode_lookup_input"),
        )
        if (samples.isNotEmpty()) {
          Text(
            text = "Quick Select:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            samples.forEach { sample ->
              FilterChip(
                selected = false,
                onClick = { onApplyQuery(sample.sku.ifBlank { sample.productCode }) },
                label = {
                  Text(
                    text = sample.sku.ifBlank { sample.productCode },
                    style = MaterialTheme.typography.labelSmall,
                  )
                },
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onApplyQuery(codeInput.trim()) },
        enabled = codeInput.isNotBlank(),
      ) {
        Text("Find SKU")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}


