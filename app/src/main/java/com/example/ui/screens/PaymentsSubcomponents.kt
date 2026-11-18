package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AgingBucketSummary
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentLedgerHelper
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentTransaction
import com.example.data.model.ShopLedgerSummary
import com.example.data.model.SupplierBill
import com.example.ui.components.formatInr
import com.example.ui.components.openDialer
import com.example.ui.theme.semanticColors
import kotlin.math.roundToInt

@Composable
fun PaymentBentoMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  isDanger: Boolean = false,
  isSuccess: Boolean = false,
  progress: Float? = null,
) {
  val containerColor = when {
    isDanger -> MaterialTheme.semanticColors.dangerContainer
    else -> MaterialTheme.colorScheme.surface
  }
  val borderColor = when {
    isDanger -> MaterialTheme.semanticColors.danger.copy(alpha = 0.35f)
    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
  }
  val titleColor = if (isDanger) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.onSurfaceVariant
  val valueColor = when {
    isDanger -> MaterialTheme.semanticColors.danger
    isSuccess -> MaterialTheme.semanticColors.success
    else -> MaterialTheme.colorScheme.onSurface
  }
  val iconTint = when {
    isDanger -> MaterialTheme.semanticColors.danger
    isSuccess -> MaterialTheme.semanticColors.success
    else -> MaterialTheme.colorScheme.primary
  }
  val iconBg = when {
    isDanger -> MaterialTheme.semanticColors.danger.copy(alpha = 0.14f)
    isSuccess -> MaterialTheme.semanticColors.successContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = titleColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(iconBg),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
          )
        }
      }

      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = valueColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      if (progress != null) {
        LinearProgressIndicator(
          progress = { progress.coerceIn(0f, 1f) },
          color = MaterialTheme.semanticColors.success,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          strokeCap = StrokeCap.Round,
          modifier = Modifier
            .fillMaxWidth()
            .height(5.dp),
        )
      }

      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = if (isDanger) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ShopPaymentCard(
  ledger: ShopLedgerSummary,
  totalOrderCount: Int,
  onViewInvoicesClick: () -> Unit,
  onRecordPaymentClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val shop = ledger.shop
  val isOverdue = ledger.maxDaysOverdue > 0
  val isSettled = ledger.isSettled
  val hasPending = ledger.outstandingBalance > 0.5
  val paidPct = if (ledger.invoicedTotal > 0.0) {
    ((ledger.paidTotal / ledger.invoicedTotal) * 100.0).coerceIn(0.0, 100.0).roundToInt()
  } else {
    0
  }

  val accentColor = when {
    isOverdue -> MaterialTheme.semanticColors.danger
    isSettled -> MaterialTheme.semanticColors.success
    hasPending -> MaterialTheme.semanticColors.warning
    else -> MaterialTheme.colorScheme.outline
  }
  val statusBgColor = when {
    isOverdue -> MaterialTheme.semanticColors.dangerContainer
    isSettled -> MaterialTheme.semanticColors.successContainer
    hasPending -> MaterialTheme.semanticColors.warningContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(
      1.dp,
      if (isOverdue) MaterialTheme.semanticColors.danger.copy(alpha = 0.35f)
      else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("shop_payment_card_${shop.shopCode}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min),
    ) {
      Box(
        modifier = Modifier
          .width(5.dp)
          .fillMaxHeight()
          .background(accentColor),
      )

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Header row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  text = shop.shopCode,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }

              Surface(
                color = statusBgColor,
                shape = RoundedCornerShape(999.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                  Icon(
                    imageVector = when {
                      isOverdue -> Icons.Rounded.Alarm
                      isSettled -> Icons.Rounded.CheckCircle
                      hasPending -> Icons.Rounded.Schedule
                      else -> Icons.Rounded.Storefront
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp),
                  )
                  Text(
                    text = ledger.statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                  )
                }
              }

              if (shop.isPendingSync) {
                Icon(
                  imageVector = Icons.Rounded.CloudOff,
                  contentDescription = "Pending sync",
                  tint = MaterialTheme.semanticColors.warning,
                  modifier = Modifier.size(14.dp),
                )
              }
            }

            Text(
              text = shop.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = listOfNotNull(
                shop.ownerName.takeIf { it.isNotBlank() },
                shop.address.takeIf { it.isNotBlank() },
              ).joinToString(" • "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (shop.phone.isNotBlank()) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = CircleShape,
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { openDialer(context, shop.phone) }
                .testTag("call_shop_${shop.shopCode}"),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.Call,
                  contentDescription = "Call ${shop.name}",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }

        // Financial Metrics Ribbon
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Outstanding Balance",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = formatInr(ledger.outstandingBalance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                  isOverdue -> MaterialTheme.semanticColors.danger
                  isSettled -> MaterialTheme.semanticColors.success
                  else -> MaterialTheme.colorScheme.onSurface
                },
              )
              Text(
                text = when {
                  isOverdue -> "Overdue: ${formatInr(ledger.overdueAmount)}"
                  isSettled -> "All Invoices Cleared"
                  ledger.invoicedTotal <= 0.0 -> "No invoices billed yet"
                  else -> "${ledger.pendingOrderCount} open invoice(s)"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isOverdue) {
                  MaterialTheme.semanticColors.danger
                } else if (isSettled) {
                  MaterialTheme.semanticColors.success
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = if (isSettled) "Total Volume" else "Invoiced / Paid",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = if (isSettled) {
                  formatInr(ledger.invoicedTotal)
                } else {
                  "${formatInr(ledger.invoicedTotal)} / ${formatInr(ledger.paidTotal)}"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = if (isSettled) "100% Collected" else "Paid $paidPct%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.semanticColors.success,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }

        // Recent Payment Strip
        ledger.recentPayment?.let { recent ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.semanticColors.success,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "Recent: ${formatInr(recent.amount)} via ${PaymentMethod.displayLabel(recent.paymentMethod)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = recent.paymentDate,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Action Buttons
        if (isSettled) {
          OutlinedButton(
            onClick = onViewInvoicesClick,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("view_shop_ledger_${shop.shopCode}"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "View Payment Ledger & Statement",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = onViewInvoicesClick,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("view_shop_ledger_${shop.shopCode}"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Invoices ($totalOrderCount)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
              )
            }
            Button(
              onClick = onRecordPaymentClick,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier
                .weight(1.15f)
                .height(48.dp)
                .testTag("record_payment_button_${shop.shopCode}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.AddCard,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "+ Record Payment",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun PaymentTransactionCard(
  transaction: PaymentTransaction,
  modifier: Modifier = Modifier,
) {
  val isSupplierPayout = transaction.direction == PaymentDirection.PAY_SUPPLIER
  val methodIcon = when (transaction.paymentMethod) {
    PaymentMethod.CASH -> Icons.Rounded.Payments
    PaymentMethod.UPI -> Icons.Rounded.QrCode2
    PaymentMethod.BANK -> Icons.Rounded.AccountBalance
    else -> Icons.Rounded.Description
  }
  val amountColor = if (isSupplierPayout) MaterialTheme.colorScheme.primary else MaterialTheme.semanticColors.success

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("payment_receipt_card_${transaction.receiptCode}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
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
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (isSupplierPayout) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.semanticColors.successContainer
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = methodIcon,
              contentDescription = null,
              tint = amountColor,
              modifier = Modifier.size(18.dp),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = transaction.receiptCode,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              if (transaction.isPendingSync) {
                Icon(
                  imageVector = Icons.Rounded.CloudOff,
                  contentDescription = "Pending sync",
                  tint = MaterialTheme.semanticColors.warning,
                  modifier = Modifier.size(14.dp),
                )
              }
            }
            Text(
              text = "${transaction.partyName} (${transaction.partyCode})",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = (if (isSupplierPayout) "-" else "+") + formatInr(transaction.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor,
          )
          Text(
            text = PaymentMethod.displayLabel(transaction.paymentMethod),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = listOfNotNull(
            transaction.invoiceRef.takeIf { it.isNotBlank() }?.let { "Ref: $it" },
            transaction.referenceNumber.takeIf { it.isNotBlank() },
            transaction.recordedBy.takeIf { it.isNotBlank() }?.let { "By $it" },
          ).joinToString(" • ").ifBlank { "Standard voucher" },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = transaction.paymentDate,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (transaction.note.isNotBlank()) {
        Text(
          text = transaction.note,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun AgingBucketCard(
  bucket: AgingBucketSummary,
  totalAmount: Double,
  index: Int,
  modifier: Modifier = Modifier,
) {
  val barColor: Color = when (index) {
    0 -> MaterialTheme.semanticColors.success
    1 -> MaterialTheme.semanticColors.warning
    2 -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.semanticColors.danger
  }
  val ratio = if (totalAmount > 0.0) (bucket.amount / totalAmount).toFloat().coerceIn(0f, 1f) else 0f

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = bucket.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${bucket.subtitle} • ${bucket.accountCount} account(s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = formatInr(bucket.amount),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = barColor,
        )
      }

      LinearProgressIndicator(
        progress = { ratio },
        color = barColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        modifier = Modifier
          .fillMaxWidth()
          .height(7.dp),
      )
    }
  }
}

@Composable
fun OverdueAlertsDialog(
  overdueShops: List<ShopLedgerSummary>,
  overdueBills: List<SupplierBill>,
  onDismiss: () -> Unit,
  onSelectShop: (ShopLedgerSummary) -> Unit,
  onSelectBill: (SupplierBill) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Overdue Payment Alerts",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (overdueShops.isEmpty() && overdueBills.isEmpty()) {
          Text(
            text = "All customer collections and supplier payouts are currently on schedule.",
            style = MaterialTheme.typography.bodyMedium,
          )
        } else {
          if (overdueShops.isNotEmpty()) {
            Text(
              text = "Retailer Collections (${overdueShops.size})",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.semanticColors.danger,
            )
            overdueShops.take(4).forEach { ledger ->
              Surface(
                color = MaterialTheme.semanticColors.dangerContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { onSelectShop(ledger) },
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = ledger.shop.name,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${ledger.maxDaysOverdue}d overdue",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.semanticColors.danger,
                    )
                  }
                  Text(
                    text = formatInr(ledger.overdueAmount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.semanticColors.danger,
                  )
                }
              }
            }
          }
          if (overdueBills.isNotEmpty()) {
            Text(
              text = "Supplier Bills (${overdueBills.size})",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.semanticColors.warning,
            )
            overdueBills.take(4).forEach { bill ->
              val days = PaymentLedgerHelper.computeSupplierDaysOverdue(bill)
              Surface(
                color = MaterialTheme.semanticColors.warningContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { onSelectBill(bill) },
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = bill.supplierName,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${bill.billNumber} • ${days}d overdue",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.semanticColors.warning,
                    )
                  }
                  Text(
                    text = formatInr(bill.outstandingAmount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.semanticColors.warning,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}
