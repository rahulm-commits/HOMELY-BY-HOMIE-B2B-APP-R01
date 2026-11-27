package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import com.example.data.model.CatalogProduct
import com.example.data.model.OrderStatus
import com.example.data.model.StockItem
import com.example.ui.components.StandardCompactSearchBar
import com.example.ui.components.formatInr
import com.example.ui.theme.semanticColors

private enum class EditPackingStatus {
  PENDING,
  STOCK_HOLD,
  COMPLETE,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickEditPackingOrderSheet(
  slip: ShopPackingSlip,
  catalogProducts: List<CatalogProduct>,
  stocks: List<StockItem> = emptyList(),
  onDismiss: () -> Unit,
  onSaveOrderAndPacking: (
    items: List<Pair<CatalogProduct, Int>>,
    note: String,
    isPacked: Boolean,
    stockHoldReason: String?,
  ) -> Unit,
  onTogglePackedOnly: (isPacked: Boolean, stockHoldReason: String?) -> Unit,
  onOpenFullEditor: (() -> Unit)? = null,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isOrderEditable = slip.order.status != OrderStatus.DELIVERED &&
    slip.order.status != OrderStatus.REJECTED
  val canMarkComplete = !slip.isSequenceLocked
  val canMarkStockHold = !slip.isPacked && (slip.isNextInSequence || slip.isStockPending)

  val availableCatalog = remember(catalogProducts, slip.items) {
    val byCode = catalogProducts.associateBy { it.code.uppercase() }.toMutableMap()
    slip.items.forEach { parsed ->
      val key = parsed.productCode.uppercase()
      if (!byCode.containsKey(key)) {
        byCode[key] = CatalogProduct(
          code = parsed.productCode,
          name = parsed.productName,
          packSize = parsed.packSize,
          category = parsed.category,
          defaultPrice = parsed.unitPrice,
        )
      }
    }
    byCode.values.toList()
  }

  val quantities = remember(slip.order.id, slip.order.itemsSummary) {
    mutableStateMapOf<String, Int>().apply {
      parseOrderLineItems(slip.order.itemsSummary, availableCatalog).forEach { parsed ->
        val matched = availableCatalog.firstOrNull {
          it.code.equals(parsed.productCode, ignoreCase = true)
        } ?: availableCatalog.firstOrNull {
          it.name.equals(parsed.productName, ignoreCase = true)
        }
        if (matched != null && parsed.quantity > 0) {
          this[matched.code] = (this[matched.code] ?: 0) + parsed.quantity
        }
      }
    }
  }

  val initialStatus = remember(slip.order.id, slip.isPacked, slip.isStockPending) {
    when {
      slip.isPacked -> EditPackingStatus.COMPLETE
      slip.isStockPending -> EditPackingStatus.STOCK_HOLD
      else -> EditPackingStatus.PENDING
    }
  }

  var note by remember(slip.order.id, slip.order.note) { mutableStateOf(slip.order.note) }
  var selectedStatus by remember(slip.order.id, initialStatus) { mutableStateOf(initialStatus) }
  var stockHoldReason by remember(slip.order.id, slip.stockPendingReason) {
    mutableStateOf(slip.stockPendingReason)
  }
  val selectedShortCodes = remember(slip.order.id) { mutableStateMapOf<String, Boolean>() }
  var showRemoveFromPackedConfirm by remember { mutableStateOf(false) }
  var showAddItemPicker by remember { mutableStateOf(false) }
  var addItemQuery by remember { mutableStateOf("") }

  val stockByCode = remember(stocks) { stocks.associateBy { it.productCode.uppercase() } }

  val selectedItems = remember(quantities.toMap(), availableCatalog) {
    availableCatalog.mapNotNull { product ->
      val qty = quantities[product.code] ?: 0
      if (qty > 0) product to qty else null
    }
  }
  val totalPacks = selectedItems.sumOf { it.second }
  val totalAmount = selectedItems.sumOf { (prod, qty) -> prod.defaultPrice * qty }

  val addableProducts = remember(availableCatalog, addItemQuery, quantities.toMap()) {
    val q = addItemQuery.trim().lowercase()
    availableCatalog.filter { prod ->
      val matchesQuery = q.isEmpty() ||
        prod.name.lowercase().contains(q) ||
        prod.code.lowercase().contains(q) ||
        prod.packSize.lowercase().contains(q)
      matchesQuery
    }
  }

  val resolvedStockReason = remember(stockHoldReason, selectedShortCodes.toMap(), slip.items) {
    val picked = slip.items
      .filter { selectedShortCodes[it.productCode] == true }
      .map { "${it.productName} (${it.packSize})" }
    val custom = stockHoldReason.trim()
    when {
      picked.isNotEmpty() && custom.isNotEmpty() -> "${picked.joinToString(", ")} — $custom"
      picked.isNotEmpty() -> "${picked.joinToString(", ")} unavailable"
      custom.isNotEmpty() -> custom
      else -> "Stock unavailable"
    }
  }

  val commitChanges: () -> Unit = {
    val nextPacked = selectedStatus == EditPackingStatus.COMPLETE
    val nextHoldReason = if (selectedStatus == EditPackingStatus.STOCK_HOLD) resolvedStockReason else null
    if (isOrderEditable) {
      onSaveOrderAndPacking(selectedItems, note, nextPacked, nextHoldReason)
    } else {
      onTogglePackedOnly(nextPacked, nextHoldReason)
    }
  }

  val attemptCommit: () -> Unit = {
    val isRemovingFromPacked = slip.isPacked && selectedStatus != EditPackingStatus.COMPLETE
    if (isRemovingFromPacked) {
      showRemoveFromPackedConfirm = true
    } else {
      commitChanges()
    }
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
        .padding(horizontal = 14.dp, vertical = 6.dp)
        .testTag("packing_quick_edit_sheet"),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Sheet Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Edit Packing & Order",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${slip.order.shopName} • ${slip.order.orderCode} • ${slip.packageLabel}",
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
          if (isOrderEditable && onOpenFullEditor != null) {
            TextButton(
              onClick = onOpenFullEditor,
              modifier = Modifier.testTag("packing_edit_open_full_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Catalog", style = MaterialTheme.typography.labelMedium)
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("packing_edit_close_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Close edit sheet",
            )
          }
        }
      }

      // Packing Status Selector (Pending / Stock Hold / Packing Complete)
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Packing Status",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val pendingSelected = selectedStatus == EditPackingStatus.PENDING
            Surface(
              color = if (pendingSelected) MaterialTheme.semanticColors.warningContainer else MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(
                width = 1.dp,
                color = if (pendingSelected) MaterialTheme.semanticColors.warning else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
              ),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable { selectedStatus = EditPackingStatus.PENDING }
                .testTag("packing_edit_status_pending"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.HourglassTop,
                  contentDescription = null,
                  tint = if (pendingSelected) MaterialTheme.semanticColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Pending",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = if (pendingSelected) MaterialTheme.semanticColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (canMarkStockHold) {
              val holdSelected = selectedStatus == EditPackingStatus.STOCK_HOLD
              Surface(
                color = if (holdSelected) MaterialTheme.semanticColors.warningContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                  width = 1.dp,
                  color = if (holdSelected) MaterialTheme.semanticColors.warning else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
                modifier = Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { selectedStatus = EditPackingStatus.STOCK_HOLD }
                  .testTag("packing_edit_status_stock_hold"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = if (holdSelected) MaterialTheme.semanticColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Stock Hold",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (holdSelected) MaterialTheme.semanticColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            val completeSelected = selectedStatus == EditPackingStatus.COMPLETE
            Surface(
              color = when {
                !canMarkComplete -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                completeSelected -> MaterialTheme.semanticColors.successContainer
                else -> MaterialTheme.colorScheme.surface
              },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(
                width = 1.dp,
                color = when {
                  !canMarkComplete -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                  completeSelected -> MaterialTheme.semanticColors.success
                  else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
              ),
              modifier = Modifier
                .weight(1.25f)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = canMarkComplete) {
                  selectedStatus = EditPackingStatus.COMPLETE
                }
                .testTag("packing_edit_status_complete"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = if (canMarkComplete) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                  contentDescription = null,
                  tint = when {
                    !canMarkComplete -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    completeSelected -> MaterialTheme.semanticColors.success
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (canMarkComplete) "Packing Complete" else "Locked",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = when {
                    !canMarkComplete -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    completeSelected -> MaterialTheme.semanticColors.success
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  },
                )
              }
            }
          }

          if (!canMarkComplete) {
            Text(
              text = "Sequence locked: Complete or mark pending Pkg #${slip.blockingPackOrderNumber ?: 1} (${slip.blockingShopName ?: "earlier order"}) first. You can still edit items below.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          if (selectedStatus == EditPackingStatus.STOCK_HOLD) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Mark unavailable material so next orders pack on time:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.semanticColors.warning,
                fontWeight = FontWeight.SemiBold,
              )
              if (slip.items.isNotEmpty()) {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  slip.items.forEach { item ->
                    val stockQty = stockByCode[item.productCode.uppercase()]?.quantity
                    val isLow = stockQty != null && stockQty < item.quantity
                    val selected = selectedShortCodes[item.productCode] == true
                    FilterChip(
                      selected = selected,
                      onClick = {
                        selectedShortCodes[item.productCode] = !selected
                      },
                      label = {
                        Text(
                          text = if (isLow) "${item.productCode} (Stk $stockQty/${item.quantity})" else item.productCode,
                          style = MaterialTheme.typography.labelSmall,
                        )
                      },
                    )
                  }
                }
              }
              OutlinedTextField(
                value = stockHoldReason,
                onValueChange = { stockHoldReason = it },
                label = { Text("Unavailable material / reason") },
                placeholder = { Text("e.g., BC100 Banana Chips out of stock") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("packing_edit_stock_reason_input"),
              )
            }
          }
        }
      }

      if (!isOrderEditable) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Order is already ${slip.order.status}. Item quantities are locked, but you can still update packing status above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(10.dp),
          )
        }
        Button(
          onClick = attemptCommit,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("packing_edit_save_status_button"),
        ) {
          Text(
            text = when (selectedStatus) {
              EditPackingStatus.COMPLETE -> "Save as Packing Complete"
              EditPackingStatus.STOCK_HOLD -> "Mark Pending (Stock Hold)"
              EditPackingStatus.PENDING -> if (slip.isPacked) "Remove from Packed" else "Keep Pending"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      } else {
        // Scrollable Items & Note Editor
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 330.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Packet Quantities (${selectedItems.size} items)",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedButton(
              onClick = { showAddItemPicker = !showAddItemPicker },
              shape = RoundedCornerShape(999.dp),
              modifier = Modifier
                .height(34.dp)
                .testTag("packing_edit_toggle_add_item"),
            ) {
              Icon(
                imageVector = if (showAddItemPicker) Icons.Rounded.Close else Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (showAddItemPicker) "Done Adding" else "Add Item",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          AnimatedVisibility(visible = showAddItemPicker) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                StandardCompactSearchBar(
                  query = addItemQuery,
                  onQueryChange = { addItemQuery = it },
                  placeholder = "Search product to add...",
                  fieldTestTag = "packing_edit_add_search",
                  containerColor = MaterialTheme.colorScheme.surface,
                  modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  addableProducts.take(6).forEach { prod ->
                    val curQty = quantities[prod.code] ?: 0
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                          quantities[prod.code] = curQty + 5
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = "${prod.name} (${prod.packSize})",
                          style = MaterialTheme.typography.bodySmall,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "${prod.code} • ${formatInr(prod.defaultPrice)}/pk",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(999.dp),
                      ) {
                        Text(
                          text = if (curQty > 0) "+5 ($curQty)" else "+5 pk",
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          if (selectedItems.isEmpty()) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Order needs at least 1 packet. Tap \"Add Item\" above to add a product.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(10.dp),
              )
            }
          } else {
            selectedItems.forEach { (prod, qty) ->
              EditableOrderItemRow(
                product = prod,
                quantity = qty,
                canRemove = selectedItems.size > 1,
                onQuantityChange = { next ->
                  if (next <= 0) {
                    quantities.remove(prod.code)
                  } else {
                    quantities[prod.code] = next.coerceAtMost(100000)
                  }
                },
                onRemove = {
                  quantities.remove(prod.code)
                },
              )
            }
          }

          OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Packing / Order Note") },
            placeholder = { Text("Add crate, timing, or delivery instructions...") },
            maxLines = 2,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("packing_edit_note_input"),
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "$totalPacks Packets Total",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Order Value: ${formatInr(totalAmount)}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Button(
            onClick = attemptCommit,
            enabled = selectedItems.isNotEmpty() && totalPacks > 0,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
              .height(48.dp)
              .testTag("packing_edit_save_button"),
          ) {
            Text(
              text = when {
                slip.isPacked && selectedStatus != EditPackingStatus.COMPLETE -> "Remove from Packed"
                selectedStatus == EditPackingStatus.STOCK_HOLD -> "Mark Pending & Next"
                else -> "Save Changes"
              },
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }

  if (showRemoveFromPackedConfirm) {
    AlertDialog(
      onDismissRequest = { showRemoveFromPackedConfirm = false },
      title = {
        Text(
          text = "Remove from Packed?",
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Text(
          text = "Are you sure you want to remove ${slip.order.shopName} (${slip.order.orderCode}) from Packed and return it to the pending packing queue?",
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showRemoveFromPackedConfirm = false
            commitChanges()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
          modifier = Modifier.testTag("packing_edit_confirm_remove_packed"),
        ) {
          Text("Remove from Packed")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showRemoveFromPackedConfirm = false },
          modifier = Modifier.testTag("packing_edit_cancel_remove_packed"),
        ) {
          Text("Keep Packed")
        }
      },
      modifier = Modifier.testTag("packing_edit_remove_confirm_dialog"),
    )
  }
}

@Composable
private fun EditableOrderItemRow(
  product: CatalogProduct,
  quantity: Int,
  canRemove: Boolean,
  onQuantityChange: (Int) -> Unit,
  onRemove: () -> Unit,
) {
  var textQty by remember(product.code, quantity) { mutableStateOf(quantity.toString()) }

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("packing_edit_row_${product.code}"),
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${product.name} (${product.packSize})",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "${product.code} • ${formatInr(product.defaultPrice * quantity)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (canRemove) {
          IconButton(
            onClick = onRemove,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .size(36.dp)
              .testTag("packing_edit_remove_${product.code}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = "Remove ${product.name}",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        StepChipButton(
          label = "-5",
          enabled = quantity > 1,
          onClick = { onQuantityChange((quantity - 5).coerceAtLeast(1)) },
          testTag = "packing_edit_minus5_${product.code}",
        )
        SmallCircleStepperButton(
          icon = Icons.Rounded.Remove,
          contentDescription = "Decrease ${product.name}",
          enabled = quantity > 1,
          onClick = { onQuantityChange((quantity - 1).coerceAtLeast(1)) },
          testTag = "packing_edit_minus1_${product.code}",
        )

        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
          modifier = Modifier
            .weight(1f)
            .height(40.dp),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
          ) {
            BasicTextField(
              value = textQty,
              onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(5)
                textQty = digits
                val parsed = digits.toIntOrNull()
                if (parsed != null && parsed > 0) {
                  onQuantityChange(parsed)
                }
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              textStyle = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
              ),
              cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("packing_edit_qty_input_${product.code}"),
            )
          }
        }

        SmallCircleStepperButton(
          icon = Icons.Rounded.Add,
          contentDescription = "Increase ${product.name}",
          enabled = true,
          onClick = { onQuantityChange(quantity + 1) },
          testTag = "packing_edit_plus1_${product.code}",
        )
        StepChipButton(
          label = "+5",
          enabled = true,
          onClick = { onQuantityChange(quantity + 5) },
          testTag = "packing_edit_plus5_${product.code}",
        )
      }
    }
  }
}

@Composable
private fun SmallCircleStepperButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  enabled: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    shape = CircleShape,
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .size(36.dp)
      .clip(CircleShape)
      .clickable(enabled = enabled, onClick = onClick)
      .testTag(testTag),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun StepChipButton(
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(
      1.dp,
      if (enabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
    ),
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .height(36.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(enabled = enabled, onClick = onClick)
      .testTag(testTag),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 10.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
      )
    }
  }
}
