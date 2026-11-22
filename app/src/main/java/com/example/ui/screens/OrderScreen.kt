package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DeliveryRoute
import com.example.data.model.InquiryStatus
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.ShopStatus
import com.example.ui.components.StandardCompactSearchBar
import com.example.ui.components.StandardRouteDropdown
import com.example.ui.components.formatInr
import com.example.ui.components.openDialer
import com.example.ui.components.openWhatsApp
import com.example.ui.components.rememberFileExistsAsync
import com.example.ui.components.rememberThumbnailImageRequest
import com.example.ui.theme.semanticColors
import java.util.Locale

private enum class OrderSectionTab(val label: String) {
  PENDING_INQUIRY("Pending Inquiry"),
  CONFIRMED_ORDERS("Confirmed Orders"),
  UNATTENDED("Pick the Call"),
  NO_ORDERS("No Orders"),
}

@Composable
fun OrderScreen(
  shops: List<Shop>,
  orders: List<OrderRecord>,
  routes: List<DeliveryRoute> = RoutesCatalog.starterRoutes,
  onPlaceOrderClick: (Shop) -> Unit,
  onMarkInquiryStatus: (shopId: String, newStatus: String, feedback: String) -> Unit,
  onEditOrderClick: (OrderRecord) -> Unit = {},
  onDeleteOrderClick: (OrderRecord) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var selectedSection by remember { mutableStateOf(OrderSectionTab.PENDING_INQUIRY) }
  var selectedRoute by remember { mutableStateOf(RoutesCatalog.ALL) }
  var searchQuery by remember { mutableStateOf("") }

  val activeShops = remember(shops) { shops.filter { it.status == ShopStatus.ACTIVE } }

  val routeFilteredShops = remember(activeShops, selectedRoute, searchQuery) {
    val q = searchQuery.trim().lowercase()
    activeShops.filter { shop ->
      val matchesRoute = selectedRoute == RoutesCatalog.ALL || shop.routeId == selectedRoute
      val matchesSearch = q.isEmpty() ||
        shop.name.lowercase().contains(q) ||
        shop.shopCode.lowercase().contains(q) ||
        shop.ownerName.lowercase().contains(q) ||
        shop.phone.lowercase().contains(q)
      matchesRoute && matchesSearch
    }
  }

  val pendingInquiryShops = remember(routeFilteredShops) {
    routeFilteredShops.filter { it.inquiryStatus == InquiryStatus.PENDING_INQUIRY }
  }
  val unattendedShops = remember(routeFilteredShops) {
    routeFilteredShops.filter { it.inquiryStatus == InquiryStatus.UNATTENDED }
  }
  val noOrderShops = remember(routeFilteredShops) {
    routeFilteredShops.filter { it.inquiryStatus == InquiryStatus.NO_ORDER }
  }

  val filteredOrders = remember(orders, selectedRoute, searchQuery) {
    val q = searchQuery.trim().lowercase()
    orders.filter { ord ->
      val matchesRoute = selectedRoute == RoutesCatalog.ALL || ord.routeId == selectedRoute
      val matchesSearch = q.isEmpty() ||
        ord.shopName.lowercase().contains(q) ||
        ord.shopCode.lowercase().contains(q) ||
        ord.orderCode.lowercase().contains(q)
      matchesRoute && matchesSearch
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 0.dp,
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      val totalRemainingOnAll = remember(activeShops) {
        activeShops.count { it.inquiryStatus == InquiryStatus.PENDING_INQUIRY }
      }
      Column(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
        // Compact Header + inline remaining calls pill
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Orders & Inquiry",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = "$totalRemainingOnAll/${activeShops.size} calls left",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }

        // Standard Route Selection Dropdown
        StandardRouteDropdown(
          selectedRoute = selectedRoute,
          routes = routes,
          onRouteSelected = { selectedRoute = it },
          fieldTestTag = "order_route_filter",
        )

        // Compact Shop Search Bar
        StandardCompactSearchBar(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          placeholder = "Search shop name, ID, or contact...",
          fieldTestTag = "order_search_input",
        )

        // Compact Selection Tabs with count in brackets
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OrderSectionTab.entries.forEach { tab ->
            val count = when (tab) {
              OrderSectionTab.PENDING_INQUIRY -> pendingInquiryShops.size
              OrderSectionTab.CONFIRMED_ORDERS -> filteredOrders.size
              OrderSectionTab.UNATTENDED -> unattendedShops.size
              OrderSectionTab.NO_ORDERS -> noOrderShops.size
            }
            val isSelected = selectedSection == tab
            FilterChip(
              selected = isSelected,
              onClick = { selectedSection = tab },
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
                  text = "${tab.label} ($count)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                )
              },
              modifier = Modifier
                .height(34.dp)
                .testTag("order_tab_${tab.name.lowercase()}"),
            )
          }
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      }
    }

    when (selectedSection) {
      OrderSectionTab.PENDING_INQUIRY -> {
        InquiryShopListContent(
          shops = pendingInquiryShops,
          emptyLabel = "No pending inquiry shops left on this filter.",
          showStatusQuickActions = true,
          onCall = { shop -> openDialer(context, shop.phone) },
          onPlaceOrder = onPlaceOrderClick,
          onWhatsApp = { shop ->
            openWhatsApp(context, shop.phone, "Hello ${shop.ownerName}, calling regarding today's Homely by Homie order for ${shop.name}.")
          },
          onMarkStatus = onMarkInquiryStatus,
        )
      }
      OrderSectionTab.CONFIRMED_ORDERS -> {
        ConfirmedOrdersListContent(
          orders = filteredOrders,
          onEditOrder = onEditOrderClick,
          onDeleteOrder = onDeleteOrderClick,
        )
      }
      OrderSectionTab.UNATTENDED -> {
        InquiryShopListContent(
          shops = unattendedShops,
          emptyLabel = "No Pick the Call shops.",
          showStatusQuickActions = false,
          onCall = { shop -> openDialer(context, shop.phone) },
          onPlaceOrder = onPlaceOrderClick,
          onWhatsApp = { shop ->
            openWhatsApp(context, shop.phone, "Hello ${shop.ownerName}, following up from Homely by Homie.")
          },
          onMarkStatus = onMarkInquiryStatus,
        )
      }
      OrderSectionTab.NO_ORDERS -> {
        InquiryShopListContent(
          shops = noOrderShops,
          emptyLabel = "No shops marked as no-order.",
          showStatusQuickActions = false,
          onCall = { shop -> openDialer(context, shop.phone) },
          onPlaceOrder = onPlaceOrderClick,
          onWhatsApp = { shop ->
            openWhatsApp(context, shop.phone, "Hello ${shop.ownerName}, checking if supply is needed for ${shop.name}.")
          },
          onMarkStatus = onMarkInquiryStatus,
        )
      }
    }
  }
}

@Composable
private fun InquiryShopListContent(
  shops: List<Shop>,
  emptyLabel: String,
  showStatusQuickActions: Boolean,
  onCall: (Shop) -> Unit,
  onPlaceOrder: (Shop) -> Unit,
  onWhatsApp: (Shop) -> Unit,
  onMarkStatus: (shopId: String, newStatus: String, feedback: String) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (shops.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = emptyLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      items(
        shops,
        key = { it.id.ifBlank { it.shopCode } },
        contentType = { "inquiry_shop_card" },
      ) { shop ->
        PendingInquiryShopCard(
          shop = shop,
          showStatusQuickActions = showStatusQuickActions,
          onCall = { onCall(shop) },
          onPlaceOrder = { onPlaceOrder(shop) },
          onWhatsApp = { onWhatsApp(shop) },
          onMarkUnattended = {
            onMarkStatus(shop.id, InquiryStatus.UNATTENDED, "${shop.name} moved to Pick the Call")
          },
          onMarkNoOrder = {
            onMarkStatus(shop.id, InquiryStatus.NO_ORDER, "${shop.name} marked No Order")
          },
          onRestorePending = {
            onMarkStatus(shop.id, InquiryStatus.PENDING_INQUIRY, "${shop.name} moved back to Pending Inquiry")
          },
        )
      }
    }
  }
}

@Composable
private fun PendingInquiryShopCard(
  shop: Shop,
  showStatusQuickActions: Boolean,
  onCall: () -> Unit,
  onPlaceOrder: () -> Unit,
  onWhatsApp: () -> Unit,
  onMarkUnattended: () -> Unit,
  onMarkNoOrder: () -> Unit,
  onRestorePending: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Tinted Dates Header Strip
      Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Event,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "Enquiry: ${shop.enquiryDate.ifBlank { "Today" }}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Surface(
            color = MaterialTheme.semanticColors.warningContainer,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, MaterialTheme.semanticColors.warning.copy(alpha = 0.35f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.semanticColors.warning,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text = "Call Due: ${shop.callDate.ifBlank { "Today" }}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.semanticColors.warning,
              )
            }
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

      Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
      // Photo + Shop Identity + Contact + Address + GPS
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val hasPhotoFile = rememberFileExistsAsync(shop.photoUri)
        val imageRequest = if (hasPhotoFile) {
          rememberThumbnailImageRequest(
            path = shop.photoUri,
            targetSizePx = 192,
            fallbackDrawableRes = R.drawable.img_shop_placeholder,
          )
        } else {
          null
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.size(58.dp),
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
              contentDescription = "${shop.name} photo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = shop.shopCode,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
            Text(
              text = shop.routeId.substringBefore("-"),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
          Text(
            text = shop.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
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
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(13.dp),
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(13.dp),
            )
            Text(
              text = "${shop.address}, ${shop.pinCode}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // Coordinates + Last Order Date + Last Call Date
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.GpsFixed,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(13.dp),
            )
            val coords = if (shop.latitude != 0.0 || shop.longitude != 0.0) {
              String.format(Locale.US, "%.4f° N, %.4f° E", shop.latitude, shop.longitude)
            } else {
              "Coordinates not captured"
            }
            Text(
              text = "GPS: $coords",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = "Last Order: ${shop.lastOrderDate.ifBlank { "—" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.PhoneInTalk,
                contentDescription = null,
                tint = MaterialTheme.semanticColors.success,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text = "Last Call: ${shop.lastCallDate.ifBlank { "—" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Primary Action Row: Call, Place Order, WhatsApp
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          color = MaterialTheme.semanticColors.successContainer,
          shape = RoundedCornerShape(9.dp),
          border = BorderStroke(1.dp, MaterialTheme.semanticColors.success.copy(alpha = 0.4f)),
          modifier = Modifier
            .weight(0.9f)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onCall)
            .testTag("inquiry_call_${shop.shopCode}"),
        ) {
          Row(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Rounded.Call, contentDescription = "Call", tint = MaterialTheme.semanticColors.success, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Call", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.semanticColors.success)
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(9.dp),
          modifier = Modifier
            .weight(1.3f)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onPlaceOrder)
            .testTag("place_order_${shop.shopCode}"),
        ) {
          Row(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Rounded.AddShoppingCart,
              contentDescription = "Place Order",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              "Place Order",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(9.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
          modifier = Modifier
            .weight(0.9f)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onWhatsApp),
        ) {
          Row(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.AutoMirrored.Rounded.Chat,
              contentDescription = "WhatsApp",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              "WA",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }

      // Status transitions for Pending / Unattended / No Order
      if (showStatusQuickActions) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onMarkUnattended),
          ) {
            Row(
              modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Pick the Call",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onMarkNoOrder),
          ) {
            Row(
              modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "No Order Today",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      } else {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onRestorePending),
        ) {
          Row(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Replay,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Move Back to Pending Inquiry",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
      }
    }
  }
}

@Composable
private fun ConfirmedOrdersListContent(
  orders: List<OrderRecord>,
  onEditOrder: (OrderRecord) -> Unit,
  onDeleteOrder: (OrderRecord) -> Unit,
) {
  var orderPendingDelete by remember { mutableStateOf<OrderRecord?>(null) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (orders.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
              modifier = Modifier.size(48.dp),
            )
            Text(
              text = "No confirmed orders matching this filter",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(
        orders,
        key = { it.id.ifBlank { it.orderCode } },
        contentType = { "confirmed_order_card" },
      ) { order ->
        val canEditOrder = order.status != OrderStatus.DELIVERED && order.status != OrderStatus.REJECTED
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                  ) {
                    Text(
                      text = order.orderCode,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                  }
                  Text(
                    text = order.shopCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  if (canEditOrder) {
                    IconButton(
                      onClick = { onEditOrder(order) },
                      modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("edit_order_${order.orderCode}"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit order ${order.orderCode}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                  IconButton(
                    onClick = { orderPendingDelete = order },
                    modifier = Modifier
                      .minimumInteractiveComponentSize()
                      .testTag("delete_order_${order.orderCode}"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.DeleteOutline,
                      contentDescription = "Delete order ${order.orderCode}",
                      tint = MaterialTheme.colorScheme.error,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Column(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = order.shopName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  color = MaterialTheme.semanticColors.successContainer,
                  shape = RoundedCornerShape(999.dp),
                  border = BorderStroke(1.dp, MaterialTheme.semanticColors.success.copy(alpha = 0.35f)),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.CheckCircle,
                      contentDescription = null,
                      tint = MaterialTheme.semanticColors.success,
                      modifier = Modifier.size(13.dp),
                    )
                    Text(
                      text = order.status.replaceFirstChar { it.uppercase() },
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.semanticColors.success,
                    )
                  }
                }
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Inventory2,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = order.itemsSummary,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              if (order.note.isNotBlank()) {
                Text(
                  text = "Note: ${order.note}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = "${order.itemCount} pkts • ${order.orderDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = formatInr(order.totalAmount),
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

  orderPendingDelete?.let { targetOrder ->
    AlertDialog(
      onDismissRequest = { orderPendingDelete = null },
      title = {
        Text(
          text = "Delete Order?",
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Text(
          text = "Delete ${targetOrder.orderCode} for ${targetOrder.shopName}? This will remove the order from your confirmed list and synced servers.",
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteOrder(targetOrder)
            orderPendingDelete = null
          },
          colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
          ),
          modifier = Modifier.testTag("confirm_delete_order_button"),
        ) {
          Text("Delete", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { orderPendingDelete = null },
          modifier = Modifier.testTag("cancel_delete_order_button"),
        ) {
          Text("Cancel")
        }
      },
    )
  }
}
