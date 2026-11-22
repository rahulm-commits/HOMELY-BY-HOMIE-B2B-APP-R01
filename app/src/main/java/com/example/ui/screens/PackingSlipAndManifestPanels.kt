package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import androidx.compose.material3.FilterChipDefaults
import com.example.ui.theme.RouteColorDot
import com.example.ui.theme.RouteColorPalette
import com.example.ui.theme.semanticColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SlipFilterMode(val label: String) {
  ALL("All"),
  ASSIGNED("Assigned"),
  UNASSIGNED("Unassigned"),
  PACKED("Packed"),
  PENDING("Pending"),
}

@Composable
fun ShopSlipsTabPanel(
  slips: List<ShopPackingSlip>,
  selectedRoute: DeliveryRoute?,
  reverseLoadOrder: Boolean,
  onToggleLoadOrder: (Boolean) -> Unit,
  onOpenReorderStops: () -> Unit,
  onToggleSlipPacked: (String, Boolean) -> Unit,
  onPrintSingleSlip: (ShopPackingSlip) -> Unit,
  onNavigateToRoutes: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var filterMode by remember { mutableStateOf(SlipFilterMode.ALL) }

  val assignedCount = remember(slips) { slips.count { it.isAssignedToRoute } }
  val unassignedCount = remember(slips) { slips.count { !it.isAssignedToRoute } }
  val packedCount = remember(slips) { slips.count { it.isPacked } }
  val pendingCount = remember(slips) { slips.count { !it.isPacked } }

  val visibleSlips = remember(slips, filterMode) {
    when (filterMode) {
      SlipFilterMode.ALL -> slips
      SlipFilterMode.ASSIGNED -> slips.filter { it.isAssignedToRoute }
      SlipFilterMode.UNASSIGNED -> slips.filter { !it.isAssignedToRoute }
      SlipFilterMode.PACKED -> slips.filter { it.isPacked }
      SlipFilterMode.PENDING -> slips.filter { !it.isPacked }
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // LIFO Reverse Loading Sequence Guide Banner
    Surface(
      color = if (reverseLoadOrder) MaterialTheme.semanticColors.infoContainer else MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(
        1.dp,
        if (reverseLoadOrder) MaterialTheme.semanticColors.info.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
      ),
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("packing_lifo_banner"),
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Rounded.SwapVert,
              contentDescription = null,
              tint = if (reverseLoadOrder) MaterialTheme.semanticColors.info else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = if (reverseLoadOrder) "Pack & Load: Last Stop First (LIFO)" else "Delivery Order: Stop #1 → Last",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Text(
          text = if (reverseLoadOrder) {
            "Vehicle drives 1 → 2 → 3... Pack the LAST shop on top as Pkg #1 and load deep inside first, so Stop #1 loads last near the door."
          } else {
            "Showing road delivery sequence (Stop #1 first). Switch to Last → #1 when packing and loading the truck."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = if (reverseLoadOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { onToggleLoadOrder(!reverseLoadOrder) }
              .testTag("toggle_reverse_load_button"),
          ) {
            Text(
              text = if (reverseLoadOrder) "Reverse: Last → #1 ✓" else "Switch to Last → #1",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (reverseLoadOrder) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }

          if (selectedRoute != null) {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenReorderStops)
                .testTag("open_reorder_stops_button"),
            ) {
              Text(
                text = "Edit Stop Numbers (1..N)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      SlipFilterMode.entries.forEach { mode ->
        val count = when (mode) {
          SlipFilterMode.ALL -> slips.size
          SlipFilterMode.ASSIGNED -> assignedCount
          SlipFilterMode.UNASSIGNED -> unassignedCount
          SlipFilterMode.PACKED -> packedCount
          SlipFilterMode.PENDING -> pendingCount
        }
        val selected = filterMode == mode
        FilterChip(
          selected = selected,
          onClick = { filterMode = mode },
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
              text = "${mode.label} ($count)",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            )
          },
          modifier = Modifier.height(34.dp),
        )
      }
    }

    if (visibleSlips.isEmpty()) {
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
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Inventory2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
          )
          Text(
            text = "No packing slips match this route or filter",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Switch route filter above or confirm shop orders first.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      val assignedSlips = visibleSlips.filter { it.isAssignedToRoute }
      val unassignedSlips = visibleSlips.filter { !it.isAssignedToRoute }

      if (assignedSlips.isNotEmpty()) {
        SectionSectionHeaderPill(
          title = "Assigned Route Packing",
          countLabel = if (reverseLoadOrder) "${assignedSlips.size} stops (Last → #1)" else "${assignedSlips.size} stops (#1 → Last)",
          containerColor = MaterialTheme.semanticColors.successContainer,
          contentColor = MaterialTheme.semanticColors.success,
        )
        assignedSlips.forEach { slip ->
          ShopPackingSlipCard(
            slip = slip,
            reverseLoadOrder = reverseLoadOrder,
            onTogglePacked = { onToggleSlipPacked(slip.order.id, !slip.isPacked) },
            onPrintSlip = { onPrintSingleSlip(slip) },
          )
        }
      }

      if (unassignedSlips.isNotEmpty()) {
        SectionSectionHeaderPill(
          title = "Unassigned Packing Queue",
          countLabel = "${unassignedSlips.size} waiting",
          containerColor = MaterialTheme.semanticColors.dangerContainer,
          contentColor = MaterialTheme.semanticColors.danger,
          actionLabel = "Assign on Routes",
          onAction = onNavigateToRoutes,
        )
        unassignedSlips.forEach { slip ->
          ShopPackingSlipCard(
            slip = slip,
            reverseLoadOrder = reverseLoadOrder,
            onTogglePacked = { onToggleSlipPacked(slip.order.id, !slip.isPacked) },
            onPrintSlip = { onPrintSingleSlip(slip) },
          )
        }
      }
    }
  }
}

@Composable
private fun SectionSectionHeaderPill(
  title: String,
  countLabel: String,
  containerColor: Color,
  contentColor: Color,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Surface(color = containerColor, shape = RoundedCornerShape(999.dp)) {
        Text(
          text = countLabel,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = contentColor,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
      }
    }
    if (actionLabel != null && onAction != null) {
      Text(
        text = actionLabel,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onAction)
          .padding(horizontal = 6.dp, vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun ShopPackingSlipCard(
  slip: ShopPackingSlip,
  reverseLoadOrder: Boolean,
  onTogglePacked: () -> Unit,
  onPrintSlip: () -> Unit,
) {
  val routeSpec = slip.route?.let { RouteColorPalette.specForIndex(it.colorIndex) }
  val totalUnits = slip.items.sumOf { it.quantity }.takeIf { it > 0 } ?: slip.order.itemCount

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(
      width = if (slip.isPacked) 1.4.dp else 1.dp,
      color = if (slip.isPacked) MaterialTheme.semanticColors.success.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("packing_slip_card_${slip.order.id}"),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Package # + Delivery Stop # + Load Position Tag
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = if (slip.isPacked) MaterialTheme.semanticColors.success else MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(7.dp),
          ) {
            Text(
              text = "PACK #${slip.packOrderNumber} • ${slip.packageLabel}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }

          if (slip.deliveryStopNumber > 0) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(7.dp),
            ) {
              Text(
                text = "Stop #${slip.deliveryStopNumber} of ${slip.totalRouteStops}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }

        if (slip.isAssignedToRoute && reverseLoadOrder && slip.totalRouteStops > 1) {
          val positionTag = when {
            slip.packOrderNumber == 1 -> "LOAD 1ST • BACK" to MaterialTheme.semanticColors.info
            slip.deliveryStopNumber == 1 -> "LOAD LAST • DOOR" to MaterialTheme.semanticColors.warning
            else -> null
          }
          positionTag?.let { (label, ink) ->
            Surface(
              color = ink.copy(alpha = 0.14f),
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = ink,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(
                if (slip.isPacked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer,
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (slip.deliveryStopNumber > 0) "#${slip.deliveryStopNumber}" else "P${slip.packOrderNumber}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (slip.isPacked) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSecondaryContainer,
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(top = 2.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
              ) {
                Text(
                  text = slip.order.shopCode,
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
              val addr = slip.shop?.address?.takeIf { it.isNotBlank() }
                ?: slip.route?.name?.substringAfter(": ")
                ?: "Unassigned"
              Text(
                text = addr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        val statusBg = when {
          slip.isPacked -> MaterialTheme.semanticColors.successContainer
          slip.isInProgress -> MaterialTheme.semanticColors.warningContainer
          else -> MaterialTheme.semanticColors.dangerContainer
        }
        val statusInk = when {
          slip.isPacked -> MaterialTheme.semanticColors.success
          slip.isInProgress -> MaterialTheme.semanticColors.warning
          else -> MaterialTheme.semanticColors.danger
        }
        val statusIcon = when {
          slip.isPacked -> Icons.Rounded.Check
          slip.isInProgress -> Icons.Rounded.Sync
          else -> Icons.Rounded.HourglassTop
        }
        val statusText = when {
          slip.isPacked -> "Packed"
          slip.isInProgress -> "Loading"
          else -> "Pending"
        }

        Surface(
          color = statusBg,
          shape = RoundedCornerShape(999.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onTogglePacked),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = statusIcon,
              contentDescription = null,
              tint = statusInk,
              modifier = Modifier.size(13.dp),
            )
            Text(
              text = statusText,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = statusInk,
            )
          }
        }
      }

      // Items Breakdown Panel
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
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
            Text(
              text = "Order #${slip.order.orderCode} • ${slip.order.orderDate}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "$totalUnits Packs total",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          if (slip.items.isNotEmpty()) {
            slip.items.chunked(2).forEach { pair ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                pair.forEach { item ->
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.CheckCircle,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(13.dp),
                    )
                    Text(
                      text = "${item.productName.substringAfter("Kerala ")} ${item.packSize} (${item.quantity} pk)",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
              }
            }
          } else {
            Text(
              text = slip.order.itemsSummary,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Crate & Route assignment metadata row
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
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(15.dp),
          )
          Text(
            text = "Crate: ${slip.crateLabel}",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (routeSpec != null) {
            RouteColorDot(spec = routeSpec, size = 9.dp)
            Text(
              text = slip.route?.name?.substringBefore(":") ?: slip.order.routeId,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            Text(
              text = "Unassigned",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.semanticColors.danger,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = slip.route?.dispatchWindow?.substringAfter("–")?.trim()?.let { "Before $it" }
              ?: "Same-day",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      if (slip.order.note.isNotBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Note: ${slip.order.note}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
          Icon(
            imageVector = Icons.Rounded.QrCode2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "DISP-${slip.order.orderCode.filter { it.isDigit() }.ifBlank { "1001" }}",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            color = if (slip.isPacked) MaterialTheme.semanticColors.successContainer else MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onTogglePacked),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AssignmentTurnedIn,
                contentDescription = null,
                tint = if (slip.isPacked) MaterialTheme.semanticColors.success else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = if (slip.isPacked) "Packed" else "Mark Packed",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (slip.isPacked) MaterialTheme.semanticColors.success else MaterialTheme.colorScheme.primary,
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable(onClick = onPrintSlip),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Print,
                contentDescription = "Print Slip",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = if (slip.isPacked) "Print Slip" else "Print Draft",
                style = MaterialTheme.typography.labelSmall,
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
fun A4ManifestTabPanel(
  snapshot: PackingDashboardSnapshot,
  selectedRouteId: String,
  onShareManifest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var paperMode by remember { mutableStateOf("Standard A4 Sheet") }
  val todayDisplay = remember {
    SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Paper Mode:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("Standard A4 Sheet", "3-Inch Thermal").forEach { mode ->
            val active = paperMode == mode
            Surface(
              color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(8.dp),
              border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { paperMode = mode },
            ) {
              Text(
                text = mode,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
              )
            }
          }
        }
      }
    }

    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("a4_manifest_document"),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.LocalShipping,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "HOMELY BY HOMIE B2B",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Text(
              text = "Central Logistics Depot • Hub Dispatch",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Mode: $paperMode",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "DISPATCH MANIFEST",
              style = MaterialTheme.typography.labelLarge,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "ROUTE: ${snapshot.selectedRoute?.id ?: if (selectedRouteId == RoutesCatalog.UNALLOCATED) "UNASSIGNED" else "ALL"}",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Date: $todayDisplay",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Vehicle Reg:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = snapshot.selectedRoute?.vehicle ?: "Multi-Route Fleet",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Driver In-Charge:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = snapshot.selectedRoute?.driverName ?: "Fleet Team",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Route Corridor:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = snapshot.selectedRoute?.name ?: "All Corridors",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Staging Bay:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = snapshot.selectedRoute?.stagingBay ?: "Central Dock",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }

        if (snapshot.shopSlips.isNotEmpty()) {
          Text(
            text = "VEHICLE LOADING SEQUENCE (${if (snapshot.reverseLoadOrder) "LAST STOP FIRST" else "DELIVERY ORDER"})",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              snapshot.shopSlips.forEach { slip ->
                val qty = slip.items.sumOf { it.quantity }.takeIf { it > 0 } ?: slip.order.itemCount
                val stopText = if (slip.deliveryStopNumber > 0) "Stop #${slip.deliveryStopNumber}" else "Queue"
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "${slip.packageLabel} [$stopText] ${slip.order.shopName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  Text(
                    text = "$qty pk • ${slip.crateLabel}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }

        Text(
          text = "VEHICLE LOAD SUMMARY (CONSOLIDATED)",
          style = MaterialTheme.typography.labelMedium,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          shape = RoundedCornerShape(6.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "SKU Description & Size",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Ord / Loaded / Status",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        val allLines = snapshot.categoryGroups.flatMap { it.items }
        if (allLines.isEmpty()) {
          Text(
            text = "No SKU demand recorded for this route selection.",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            allLines.forEachIndexed { idx, sku ->
              val isShort = sku.shortagePacks > 0
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (isShort) MaterialTheme.semanticColors.dangerContainer.copy(alpha = 0.55f) else Color.Transparent)
                  .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "${idx + 1}. ${sku.productName} ${sku.packSize}",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Text(
                  text = "${sku.orderedPacks} / ${sku.loadedPacks} pk [${if (isShort) "SHORT -${sku.shortagePacks}" else "OK"}]",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = if (isShort) MaterialTheme.semanticColors.danger else MaterialTheme.semanticColors.success,
                )
              }
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = "Prepared by",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Warehouse Officer Stamp",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.End) {
              Text(
                text = "Verified by ${snapshot.selectedRoute?.driverName ?: "Driver"}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Received Physical Check",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        Button(
          onClick = onShareManifest,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(
            imageVector = Icons.Rounded.Print,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "  Export / Share $paperMode",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}
