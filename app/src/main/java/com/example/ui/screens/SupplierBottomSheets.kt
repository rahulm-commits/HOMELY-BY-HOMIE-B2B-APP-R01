package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Supplier
import com.example.data.model.SupplierStatus
import com.example.ui.theme.semanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierQuickActionsSheet(
  supplier: Supplier,
  onDismiss: () -> Unit,
  onEditProfile: () -> Unit,
  onToggleActiveStatus: (newStatus: String, feedbackLabel: String) -> Unit,
  onPayBillsClick: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 10.dp),
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
              .size(10.dp)
              .clip(CircleShape)
              .background(
                if (supplier.status == SupplierStatus.ACTIVE) {
                  MaterialTheme.semanticColors.success
                } else {
                  MaterialTheme.semanticColors.warning
                }
              ),
          )
          Column {
            Text(
              text = supplier.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${supplier.supplierCode} • ${supplier.creditTerms}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close quick actions")
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

      // Scrollable quick action items
      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 6.dp),
      ) {
        QuickActionRow(
          icon = Icons.Rounded.EditNote,
          title = "Edit Supplier Details",
          subtitle = "Business name, category & trade profile",
          onClick = onEditProfile,
          testTag = "qa_edit_supplier_details",
        )
        QuickActionRow(
          icon = Icons.Rounded.Badge,
          title = "Update Contact & Key Person",
          subtitle = "Primary phone, escalation email & reps",
          onClick = onEditProfile,
          testTag = "qa_update_contact",
        )
        QuickActionRow(
          icon = Icons.Rounded.LocationOn,
          title = "Modify Address & Location",
          subtitle = "Warehouse hub, city & delivery pin code",
          onClick = onEditProfile,
          testTag = "qa_modify_location",
        )
        QuickActionRow(
          icon = Icons.Rounded.Description,
          title = "Manage GST & Procurement Documents",
          subtitle = "KYC certificates, GSTIN & tax licenses",
          onClick = onEditProfile,
          testTag = "qa_manage_gst",
        )
        QuickActionRow(
          icon = Icons.Rounded.Payments,
          title = "Update Credit & Pricing Tier",
          subtitle = "Credit period, payment terms & slab rating",
          onClick = onEditProfile,
          testTag = "qa_credit_tier",
        )
        QuickActionRow(
          icon = Icons.Rounded.CurrencyRupee,
          title = "Enter Confirmed Price for Products",
          subtitle = "Daily spot rates & contracted unit prices",
          onClick = onEditProfile,
          isHighlighted = true,
          testTag = "qa_confirmed_prices",
        )

        val nextStatus = if (supplier.status == SupplierStatus.INACTIVE) {
          SupplierStatus.ACTIVE
        } else {
          SupplierStatus.INACTIVE
        }
        val statusActionTitle = if (supplier.status == SupplierStatus.INACTIVE) {
          "Mark Supplier Active"
        } else {
          "Mark Supplier Inactive"
        }
        QuickActionRow(
          icon = if (supplier.status == SupplierStatus.INACTIVE) {
            Icons.Rounded.CheckCircleOutline
          } else {
            Icons.Rounded.PauseCircleOutline
          },
          title = statusActionTitle,
          subtitle = "Switch procurement directory visibility",
          onClick = { onToggleActiveStatus(nextStatus, "${supplier.name} marked $nextStatus") },
          testTag = "qa_toggle_supplier_status",
        )
        QuickActionRow(
          icon = Icons.Rounded.DeleteOutline,
          title = "Archive / Remove Supplier",
          subtitle = "Move supplier entry out of active directory",
          onClick = {
            onToggleActiveStatus(SupplierStatus.DELETED, "${supplier.name} removed from directory")
          },
          isDanger = true,
          testTag = "qa_delete_supplier",
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

      // Pinned bottom CTA pair
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onEditProfile,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
          ),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("sheet_full_profile_button"),
        ) {
          Icon(Icons.Rounded.Store, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Full Profile", fontWeight = FontWeight.Bold)
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onPayBillsClick)
            .testTag("sheet_pay_bills_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.AccountBalanceWallet,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Pay / Bills",
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

@Composable
private fun QuickActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  isHighlighted: Boolean = false,
  isDanger: Boolean = false,
  testTag: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 10.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Surface(
      color = when {
        isDanger -> MaterialTheme.semanticColors.dangerContainer
        isHighlighted -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
      },
      shape = CircleShape,
      modifier = Modifier.size(38.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = when {
            isDanger -> MaterialTheme.semanticColors.danger
            isHighlighted -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.secondary
          },
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (isDanger) MaterialTheme.semanticColors.danger else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Icon(
      imageVector = Icons.Rounded.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp),
    )
  }
}

@Composable
fun SupplierAlertsDialog(
  missingDetailsSuppliers: List<Supplier>,
  unverifiedSuppliers: List<Supplier>,
  pendingQueueCount: Int,
  onDismiss: () -> Unit,
  onSelectSupplier: (Supplier) -> Unit,
  onOpenSync: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Supplier Compliance & Sync Alerts") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (pendingQueueCount > 0) {
          Surface(
            color = MaterialTheme.semanticColors.warningContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = onOpenSync),
          ) {
            Text(
              text = "$pendingQueueCount offline change(s) queued • Tap to Sync Servers",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.semanticColors.warning,
              modifier = Modifier.padding(10.dp),
            )
          }
        }
        val combined = (missingDetailsSuppliers + unverifiedSuppliers).distinctBy { it.id }
        if (combined.isEmpty() && pendingQueueCount == 0) {
          Text(
            text = "All registered suppliers have complete contact, GSTIN, and KYC verification.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          combined.forEach { sup ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectSupplier(sup) },
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "${sup.supplierCode} • ${sup.name}",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
                val reason = buildList {
                  if (sup.gstin.isBlank()) add("GSTIN missing")
                  if (sup.email.isBlank()) add("Email missing")
                  if (!sup.kycVerified) add("KYC pending")
                }.joinToString(" • ").ifBlank { "Needs profile review" }
                Text(
                  text = "$reason • Tap to update",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}
