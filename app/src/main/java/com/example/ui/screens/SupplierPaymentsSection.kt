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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentLedgerHelper
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentTransaction
import com.example.data.model.SupplierBill
import com.example.data.model.SupplierBillStatus
import com.example.ui.components.formatInr
import com.example.ui.components.openDialer
import com.example.ui.theme.semanticColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SupplierBillCard(
  bill: SupplierBill,
  onPayClick: () -> Unit,
  onViewDetailsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val effectiveStatus = PaymentLedgerHelper.effectiveSupplierStatus(bill)
  val overdueDays = PaymentLedgerHelper.computeSupplierDaysOverdue(bill)
  val isOverdue = effectiveStatus == SupplierBillStatus.OVERDUE
  val isSettled = effectiveStatus == SupplierBillStatus.SETTLED
  val paidPct = if (bill.totalBilled > 0.0) {
    ((bill.paidAmount / bill.totalBilled) * 100.0).coerceIn(0.0, 100.0).roundToInt()
  } else {
    100
  }

  val accentColor = when {
    isOverdue -> MaterialTheme.semanticColors.danger
    isSettled -> MaterialTheme.semanticColors.success
    else -> MaterialTheme.semanticColors.warning
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
      .testTag("supplier_bill_card_${bill.supplierCode}"),
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
          .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        // Header row: codes + status + call button
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
                  text = bill.supplierCode,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(999.dp),
              ) {
                Text(
                  text = bill.billNumber,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
              Surface(
                color = when {
                  isOverdue -> MaterialTheme.semanticColors.dangerContainer
                  isSettled -> MaterialTheme.semanticColors.successContainer
                  else -> MaterialTheme.semanticColors.warningContainer
                },
                shape = RoundedCornerShape(999.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                  Icon(
                    imageVector = when {
                      isOverdue -> Icons.Rounded.Alarm
                      isSettled -> Icons.Rounded.CheckCircle
                      else -> Icons.Rounded.Schedule
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp),
                  )
                  Text(
                    text = when {
                      isOverdue -> "Overdue (${overdueDays}d)"
                      isSettled -> "Settled"
                      else -> "Due ${bill.dueDate}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                  )
                }
              }
              if (bill.isPendingSync) {
                Icon(
                  imageVector = Icons.Rounded.CloudOff,
                  contentDescription = "Pending sync",
                  tint = MaterialTheme.semanticColors.warning,
                  modifier = Modifier.size(14.dp),
                )
              }
            }

            Text(
              text = bill.supplierName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = listOfNotNull(
                bill.contactPerson.takeIf { it.isNotBlank() },
                bill.category.takeIf { it.isNotBlank() },
                bill.location.takeIf { it.isNotBlank() },
              ).joinToString(" • "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (bill.phone.isNotBlank()) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = CircleShape,
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { openDialer(context, bill.phone) },
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.Call,
                  contentDescription = "Call ${bill.supplierName}",
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
                text = "Balance Payable",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = formatInr(bill.outstandingAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                  isOverdue -> MaterialTheme.semanticColors.danger
                  isSettled -> MaterialTheme.semanticColors.success
                  else -> MaterialTheme.colorScheme.onSurface
                },
              )
              Text(
                text = if (isSettled) "Invoice fully paid" else "Due: ${bill.dueDate}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isOverdue) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "Billed / Paid",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "${formatInr(bill.totalBilled)} / ${formatInr(bill.paidAmount)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Paid $paidPct%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.semanticColors.success,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        }

        if (bill.notes.isNotBlank()) {
          Text(
            text = bill.notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onViewDetailsClick,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isSettled) "View Ledger" else "Bill Details",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
            )
          }

          if (!isSettled) {
            Button(
              onClick = onPayClick,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier
                .weight(1.1f)
                .height(48.dp)
                .testTag("pay_supplier_button_${bill.supplierCode}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Payments,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Pay Supplier",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierBillDetailSheet(
  bill: SupplierBill,
  billPayouts: List<PaymentTransaction>,
  onDismiss: () -> Unit,
  onPayClick: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 14.dp)
        .padding(bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = bill.supplierName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${bill.supplierCode} • ${bill.billNumber} • ${bill.category}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close")
        }
      }

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
            Text("Total Billed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatInr(bill.totalBilled), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Settled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatInr(bill.paidAmount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.semanticColors.success)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text("Payable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              formatInr(bill.outstandingAmount),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (bill.outstandingAmount > 0.5) MaterialTheme.semanticColors.danger else MaterialTheme.semanticColors.success,
            )
          }
        }
      }

      if (bill.notes.isNotBlank()) {
        Text(
          text = "Supply Details: ${bill.notes}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

      Text(
        text = "Payout Vouchers (${billPayouts.size})",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
      )
      if (billPayouts.isEmpty()) {
        Text(
          text = "No payouts recorded against this bill yet.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        billPayouts.forEach { pay ->
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
              Column(modifier = Modifier.weight(1f)) {
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
                )
              }
              Text(
                text = formatInr(pay.amount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      if (bill.outstandingAmount > 0.5) {
        Button(
          onClick = onPayClick,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        ) {
          Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Pay ${formatInr(bill.outstandingAmount)}", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplierBillSheet(
  suggestedSupplierCode: String,
  suggestedBillNumber: String,
  onDismiss: () -> Unit,
  onSaveBill: (SupplierBill) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var supplierCode by remember { mutableStateOf(suggestedSupplierCode) }
  var supplierName by remember { mutableStateOf("") }
  var contactPerson by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Raw Materials") }
  var billNumber by remember { mutableStateOf(suggestedBillNumber) }
  var totalBilledText by remember { mutableStateOf("") }
  var dueInDays by remember { mutableIntStateOf(7) }
  var notes by remember { mutableStateOf("") }

  val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
  val dueDateIso = remember(dueInDays) {
    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dueInDays) }
      .let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) }
  }

  val parsedTotal = totalBilledText.toDoubleOrNull() ?: 0.0
  val isValid = supplierCode.trim().length >= 2 &&
    supplierName.trim().isNotEmpty() &&
    billNumber.trim().isNotEmpty() &&
    parsedTotal > 0.0

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 14.dp)
        .padding(bottom = 16.dp),
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
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Factory,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
          }
          Text(
            text = "Add Supplier Payable Bill",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close")
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = supplierCode,
          onValueChange = { supplierCode = it },
          label = { Text("Supplier Code *") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = billNumber,
          onValueChange = { billNumber = it },
          label = { Text("Bill Number *") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
      }

      OutlinedTextField(
        value = supplierName,
        onValueChange = { supplierName = it },
        label = { Text("Supplier Business Name *") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("supplier_name_input"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = contactPerson,
          onValueChange = { contactPerson = it },
          label = { Text("Contact Person") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' } },
          label = { Text("Phone") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("Category") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text("Location / Hub") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
      }

      OutlinedTextField(
        value = totalBilledText,
        onValueChange = { input ->
          val cleaned = input.filter { it.isDigit() || it == '.' }
          if (cleaned.count { it == '.' } <= 1) totalBilledText = cleaned
        },
        label = { Text("Total Bill Amount (₹) *") },
        prefix = { Text("₹ ") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("supplier_bill_amount_input"),
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Due Date: $dueDateIso",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(0 to "Due Today", 7 to "Net 7 Days", 15 to "Net 15 Days", 30 to "Net 30 Days").forEach { (days, label) ->
            val selected = dueInDays == days
            Surface(
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(999.dp),
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { dueInDays = days },
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Supply Notes (optional)") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Button(
        onClick = {
          if (isValid) {
            onSaveBill(
              SupplierBill(
                supplierCode = supplierCode.trim(),
                supplierName = supplierName.trim(),
                contactPerson = contactPerson.trim(),
                phone = phone.trim(),
                location = location.trim(),
                category = category.trim().ifBlank { "Raw Materials" },
                billNumber = billNumber.trim(),
                totalBilled = parsedTotal,
                paidAmount = 0.0,
                billDate = todayIso,
                dueDate = dueDateIso,
                status = SupplierBillStatus.PENDING,
                notes = notes.trim(),
              )
            )
          }
        },
        enabled = isValid,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_supplier_bill_button"),
      ) {
        Icon(Icons.Rounded.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Save Supplier Bill", fontWeight = FontWeight.Bold)
      }
    }
  }
}
