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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Verified
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderRecord
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentLedgerHelper
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentTransaction
import com.example.data.model.ShopLedgerSummary
import com.example.data.model.SupplierBill
import com.example.ui.components.formatInr
import com.example.ui.components.openWhatsApp
import com.example.ui.theme.semanticColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

sealed interface PaymentSheetTarget {
  data class ShopCollection(val ledger: ShopLedgerSummary) : PaymentSheetTarget
  data class SupplierPayout(val bill: SupplierBill) : PaymentSheetTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentModalSheet(
  target: PaymentSheetTarget,
  userDisplayName: String,
  onDismiss: () -> Unit,
  onConfirmPayment: (PaymentTransaction) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isSupplier = target is PaymentSheetTarget.SupplierPayout

  val partyId: String
  val partyCode: String
  val partyName: String
  val contactName: String
  val phone: String
  val outstanding: Double
  val statusText: String
  val isOverdue: Boolean
  val defaultInvoiceRef: String
  val unpaidOrders: List<OrderRecord>

  when (target) {
    is PaymentSheetTarget.ShopCollection -> {
      val shop = target.ledger.shop
      partyId = shop.id
      partyCode = shop.shopCode
      partyName = shop.name
      contactName = shop.ownerName
      phone = shop.phone
      outstanding = target.ledger.outstandingBalance
      isOverdue = target.ledger.maxDaysOverdue > 0
      statusText = if (isOverdue) {
        "${target.ledger.maxDaysOverdue}d Overdue"
      } else if (outstanding > 0.5) {
        "Payment Pending"
      } else {
        "Advance / Settled"
      }
      unpaidOrders = target.ledger.unpaidOrders
      defaultInvoiceRef = unpaidOrders.firstOrNull()?.orderCode ?: ""
    }
    is PaymentSheetTarget.SupplierPayout -> {
      val bill = target.bill
      partyId = bill.id
      partyCode = bill.supplierCode
      partyName = bill.supplierName
      contactName = bill.contactPerson
      phone = bill.phone
      outstanding = bill.outstandingAmount
      val overdueDays = PaymentLedgerHelper.computeSupplierDaysOverdue(bill)
      isOverdue = overdueDays > 0
      statusText = if (isOverdue) {
        "${overdueDays}d Overdue"
      } else {
        "Due ${bill.dueDate}"
      }
      unpaidOrders = emptyList()
      defaultInvoiceRef = bill.billNumber
    }
  }

  val initialAmount = remember(target) {
    if (outstanding > 0.5) outstanding.roundToInt().toString() else "1000"
  }
  var amountText by remember(target) { mutableStateOf(initialAmount) }
  var selectedMethod by remember(target) {
    mutableStateOf(if (isSupplier) PaymentMethod.BANK else PaymentMethod.UPI)
  }
  var referenceText by remember(target) { mutableStateOf("") }
  var noteText by remember(target) { mutableStateOf("") }

  val todayIso = remember {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }
  val todayReadable = remember {
    SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
  }
  val officerName = remember(userDisplayName) {
    userDisplayName.ifBlank { "Field Officer" }
  }

  val parsedAmount = amountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val remainingAfter = (outstanding - parsedAmount).coerceAtLeast(0.0)
  val canSubmit = parsedAmount > 0.0

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
      // Sheet Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.AddCard,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(20.dp),
            )
          }
          Column {
            Text(
              text = if (isSupplier) "Pay Supplier Invoice" else "Record Shop Payment",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = partyName,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_payment_sheet_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close sheet",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Current Balance Callout
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isSupplier) "Payable Balance" else "Current Shop Balance",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = formatInr(outstanding),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = if (isOverdue) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "$partyCode • ${contactName.ifBlank { "Primary Contact" }}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Surface(
            color = if (isOverdue) {
              MaterialTheme.semanticColors.dangerContainer
            } else {
              MaterialTheme.semanticColors.warningContainer
            },
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = statusText,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isOverdue) {
                MaterialTheme.semanticColors.danger
              } else {
                MaterialTheme.semanticColors.warning
              },
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }
      }

      // Amount Input + Quick Presets
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isSupplier) "Payout Amount (₹)" else "Amount Collected (₹)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (outstanding > 0.5) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { amountText = outstanding.roundToInt().toString() }
                .testTag("fill_full_amount_button"),
            ) {
              Text(
                text = if (isSupplier) {
                  "Pay Full (${formatInr(outstanding)})"
                } else {
                  "Collect All (${formatInr(outstanding)})"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = { input ->
            val cleaned = input.filter { it.isDigit() || it == '.' }
            if (cleaned.count { it == '.' } <= 1) {
              amountText = cleaned
            }
          },
          prefix = {
            Text(
              text = "₹ ",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          placeholder = { Text("Enter amount") },
          textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_amount_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(2000, 5000, 10000).forEach { preset ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { amountText = preset.toString() },
              ) {
                Text(
                  text = formatInr(preset.toDouble()),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                )
              }
            }
          }
          Text(
            text = "Remaining: ${formatInr(remainingAfter)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (remainingAfter <= 0.5) {
              MaterialTheme.semanticColors.success
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          )
        }
      }

      // Payment Method Selector (4 columns)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Payment Method",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PaymentMethodPill(
            label = "Cash",
            icon = Icons.Rounded.Payments,
            selected = selectedMethod == PaymentMethod.CASH,
            onClick = { selectedMethod = PaymentMethod.CASH },
            modifier = Modifier
              .weight(1f)
              .testTag("method_cash"),
          )
          PaymentMethodPill(
            label = "UPI",
            icon = Icons.Rounded.QrCode2,
            selected = selectedMethod == PaymentMethod.UPI,
            onClick = { selectedMethod = PaymentMethod.UPI },
            modifier = Modifier
              .weight(1f)
              .testTag("method_upi"),
          )
          PaymentMethodPill(
            label = "Bank Trf",
            icon = Icons.Rounded.AccountBalance,
            selected = selectedMethod == PaymentMethod.BANK,
            onClick = { selectedMethod = PaymentMethod.BANK },
            modifier = Modifier
              .weight(1f)
              .testTag("method_bank"),
          )
          PaymentMethodPill(
            label = "Cheque",
            icon = Icons.Rounded.Description,
            selected = selectedMethod == PaymentMethod.CHEQUE,
            onClick = { selectedMethod = PaymentMethod.CHEQUE },
            modifier = Modifier
              .weight(1f)
              .testTag("method_cheque"),
          )
        }
      }

      // Reference & Payment Date
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = referenceText,
          onValueChange = { referenceText = it },
          label = { Text("Ref / UTR / Chq #") },
          placeholder = { Text(if (selectedMethod == PaymentMethod.UPI) "UPI/UTR..." else "Ref code") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1.25f)
            .testTag("payment_reference_input"),
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier
            .weight(0.95f)
            .height(56.dp)
            .padding(top = 6.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.CalendarToday,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = todayReadable,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
            )
          }
        }
      }

      // Recorded By Field Officer row
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = officerName.take(2).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            }
            Text(
              text = officerName,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
            text = if (isSupplier) "Accounts" else "Verified",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.semanticColors.success,
          )
        }
      }

      // Auto-Allocated Orders / Target Bill Preview
      if (!isSupplier && unpaidOrders.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Invoice Allocation (Oldest First)",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "${unpaidOrders.size} open",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.semanticColors.success,
              fontWeight = FontWeight.SemiBold,
            )
          }
          var remainingToAllocate = parsedAmount
          unpaidOrders.take(3).forEach { ord ->
            val allocated = minOf(ord.totalAmount, remainingToAllocate).coerceAtLeast(0.0)
            remainingToAllocate = (remainingToAllocate - allocated).coerceAtLeast(0.0)
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
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
                    text = "${ord.orderCode} • ${ord.orderDate}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = ord.itemsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = "Inv ${formatInr(ord.totalAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = "Alloc ${formatInr(allocated)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.semanticColors.success,
                  )
                }
              }
            }
          }
        }
      } else if (isSupplier) {
        val bill = (target as PaymentSheetTarget.SupplierPayout).bill
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
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
                text = "${bill.billNumber} • ${bill.category}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = bill.notes.ifBlank { "Supplier invoice due ${bill.dueDate}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Text(
              text = if (remainingAfter <= 0.5) "Will Settle" else "Part Payout",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (remainingAfter <= 0.5) MaterialTheme.semanticColors.success else MaterialTheme.semanticColors.warning,
            )
          }
        }
      }

      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it },
        label = { Text("Remarks / Note (optional)") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("payment_note_input"),
      )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

      // Action Buttons (pinned below scrollable form)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(0.8f)
            .height(50.dp),
        ) {
          Text("Cancel", fontWeight = FontWeight.SemiBold)
        }
        Button(
          onClick = {
            if (canSubmit) {
              onConfirmPayment(
                PaymentTransaction(
                  direction = if (isSupplier) PaymentDirection.PAY_SUPPLIER else PaymentDirection.COLLECT,
                  partyId = partyId,
                  partyCode = partyCode,
                  partyName = partyName,
                  contactName = contactName,
                  phone = phone,
                  invoiceRef = defaultInvoiceRef,
                  amount = parsedAmount,
                  paymentMethod = selectedMethod,
                  referenceNumber = referenceText.trim(),
                  paymentDate = todayIso,
                  recordedBy = officerName,
                  note = noteText.trim(),
                )
              )
            }
          },
          enabled = canSubmit,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .weight(1.4f)
            .height(50.dp)
            .testTag("confirm_record_payment_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Verified,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isSupplier) "Confirm Payout" else "Save Payment",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun PaymentMethodPill(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(12.dp),
    border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopLedgerDetailSheet(
  ledger: ShopLedgerSummary,
  shopOrders: List<OrderRecord>,
  shopPayments: List<PaymentTransaction>,
  onDismiss: () -> Unit,
  onRecordPaymentClick: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val shop = ledger.shop

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = shop.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${shop.shopCode} • ${shop.ownerName} • ${shop.phone}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close")
        }
      }

      // Summary banner
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text("Invoiced", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatInr(ledger.invoicedTotal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Collected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatInr(ledger.paidTotal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.semanticColors.success)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text("Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              formatInr(ledger.outstandingBalance),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (ledger.outstandingBalance > 0.5) MaterialTheme.semanticColors.danger else MaterialTheme.semanticColors.success,
            )
          }
        }
      }

      Text(
        text = "Invoices / Orders (${shopOrders.size})",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (shopOrders.isEmpty()) {
        Text(
          text = "No active orders recorded for this shop yet.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        shopOrders.forEach { ord ->
          val isUnpaid = ledger.unpaidOrders.any { it.id == ord.id }
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                  contentDescription = null,
                  tint = if (isUnpaid) MaterialTheme.semanticColors.warning else MaterialTheme.semanticColors.success,
                  modifier = Modifier.size(18.dp),
                )
                Column {
                  Text(
                    text = "${ord.orderCode} • ${ord.orderDate}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = ord.itemsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = formatInr(ord.totalAmount),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = if (isUnpaid) "Open" else "Cleared",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isUnpaid) MaterialTheme.semanticColors.warning else MaterialTheme.semanticColors.success,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

      Text(
        text = "Payment Receipts (${shopPayments.size})",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (shopPayments.isEmpty()) {
        Text(
          text = "No payments collected from this shop yet.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        shopPayments.forEach { pay ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = null,
                  tint = MaterialTheme.semanticColors.success,
                  modifier = Modifier.size(18.dp),
                )
                Column {
                  Text(
                    text = "${pay.receiptCode} • ${PaymentMethod.displayLabel(pay.paymentMethod)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = listOfNotNull(
                      pay.paymentDate.takeIf { it.isNotBlank() },
                      pay.referenceNumber.takeIf { it.isNotBlank() },
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Text(
                text = formatInr(pay.amount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.semanticColors.success,
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = {
            val reminderMsg = "Namaste ${shop.ownerName}, reminder from SnackRoute regarding ${shop.name} (${shop.shopCode}): outstanding balance is ${formatInr(ledger.outstandingBalance)}. Please let us know when we can collect."
            openWhatsApp(context, shop.phone, reminderMsg)
          },
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp),
        ) {
          Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Remind", fontWeight = FontWeight.SemiBold)
        }
        Button(
          onClick = onRecordPaymentClick,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1.3f)
            .height(48.dp),
        ) {
          Icon(Icons.Rounded.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Record Payment", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
