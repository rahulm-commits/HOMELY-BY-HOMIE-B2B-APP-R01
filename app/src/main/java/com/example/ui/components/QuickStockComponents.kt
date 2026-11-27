package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.semanticColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickStockAdjustDialog(
  productName: String,
  productCode: String,
  packSize: String,
  currentQuantity: Int,
  minThreshold: Int,
  unitLabel: String,
  inputTestTag: String = "stock_adjust_input",
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit,
) {
  var qtyText by remember(productCode, currentQuantity) { mutableStateOf(currentQuantity.toString()) }
  val parsedQty = qtyText.toIntOrNull()
  val previewQty = (parsedQty ?: currentQuantity).coerceAtLeast(0)
  val safeThreshold = minThreshold.coerceAtLeast(1)
  val willBeLow = previewQty <= safeThreshold
  val restockTarget = (safeThreshold * 2).coerceAtLeast(20)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "Quick Stock Update",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "$productCode • $packSize",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = productName,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )

        Surface(
          color = if (willBeLow) {
            MaterialTheme.semanticColors.dangerContainer.copy(alpha = 0.7f)
          } else {
            MaterialTheme.semanticColors.successContainer.copy(alpha = 0.7f)
          },
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(
            1.dp,
            if (willBeLow) {
              MaterialTheme.semanticColors.danger.copy(alpha = 0.35f)
            } else {
              MaterialTheme.semanticColors.success.copy(alpha = 0.35f)
            },
          ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = if (willBeLow) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = if (willBeLow) MaterialTheme.semanticColors.danger else MaterialTheme.semanticColors.success,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = if (willBeLow) {
                  "Low Alert (Min $safeThreshold $unitLabel)"
                } else {
                  "Healthy (Min $safeThreshold $unitLabel)"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (willBeLow) MaterialTheme.semanticColors.danger else MaterialTheme.semanticColors.success,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = "$currentQuantity → $previewQty $unitLabel",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          IconButton(
            onClick = { qtyText = (previewQty - 1).coerceAtLeast(0).toString() },
            enabled = previewQty > 0,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant),
          ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease stock by 1")
          }
          OutlinedTextField(
            value = qtyText,
            onValueChange = { input -> qtyText = input.filter { it.isDigit() } },
            label = { Text("Available ($unitLabel)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag(inputTestTag),
          )
          IconButton(
            onClick = { qtyText = (previewQty + 1).toString() },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          ) {
            Icon(
              Icons.Rounded.Add,
              contentDescription = "Increase stock by 1",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }

        Text(
          text = "One-tap quick adjustments",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.SemiBold,
        )

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf(-50, -10, -5, 5, 10, 25, 50).forEach { delta ->
            val isAdd = delta > 0
            val label = if (isAdd) "+$delta" else "$delta"
            Surface(
              color = if (isAdd) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.surfaceVariant
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  qtyText = (previewQty + delta).coerceAtLeast(0).toString()
                }
                .testTag("quick_delta_${if (isAdd) "plus_$delta" else "minus_${-delta}"}"),
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (isAdd) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                )
              }
            }
          }
        }

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = MaterialTheme.semanticColors.successContainer,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.semanticColors.success.copy(alpha = 0.3f)),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(10.dp))
              .clickable { qtyText = restockTarget.toString() },
          ) {
            Text(
              text = "Restock 2× Min ($restockTarget)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.semanticColors.success,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(10.dp))
              .clickable { qtyText = "0" },
          ) {
            Text(
              text = "Out of Stock (0)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { if (parsedQty != null) onConfirm(parsedQty.coerceAtLeast(0)) },
        enabled = parsedQty != null && parsedQty >= 0,
        modifier = Modifier.testTag("save_stock_confirm_button"),
      ) {
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

@Composable
fun InlineQuickDeltaPill(
  label: String,
  isAdd: Boolean,
  enabled: Boolean = true,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = when {
      !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
      isAdd -> MaterialTheme.colorScheme.primaryContainer
      else -> MaterialTheme.colorScheme.surfaceVariant
    },
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(
      1.dp,
      when {
        !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        isAdd -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
      },
    ),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .heightIn(min = 36.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(enabled = enabled, onClick = onClick),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = when {
          !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
          isAdd -> MaterialTheme.colorScheme.onPrimaryContainer
          else -> MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1,
      )
    }
  }
}
