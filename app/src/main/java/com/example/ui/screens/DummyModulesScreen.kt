package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppDestination
import com.example.ui.theme.EmeraldDeepPrimary
import com.example.ui.theme.MintAccentChip
import com.example.ui.theme.MintBadgeBg
import com.example.ui.theme.MintCardBorder

@Composable
fun DummyModuleScreen(
  destination: AppDestination,
  modifier: Modifier = Modifier,
) {
  val plannedBullets = when (destination) {
    AppDestination.PACKING -> listOf(
      "Assigned & unassigned packing queues",
      "Daily production targets by packet size (100g, 200g, 500g)",
      "Section-by-section batch counters (Banana Chips, Jaggery Banana)",
    )
    AppDestination.TRANSPORT -> listOf(
      "Route dropdown selector & delivery progress line chart",
      "Order IDs, contact numbers, and packet contents checklist",
      "Nearby navigation & Delivered / Next dispatch flow",
    )
    AppDestination.SUPPLIERS -> listOf(
      "Registered supplier count, search & status tabs",
      "Call, WhatsApp, Edit & bottom-center Add Supplier action",
      "Confirmed product pricing & GPS supplier coordinates",
    )
    AppDestination.ROUTES -> listOf(
      "Interactive route map with color-coded shop allocation",
      "Excel-style table (SNo | Shop ID | Shop Name | Contact | Address)",
      "Updatable dispatch schedules across R01, R02, R03",
    )
    AppDestination.PAYMENTS -> listOf(
      "All Shops, Payment Pending, and Payment Completed sections",
      "Pending balance call shortcuts per shop",
      "Cash & UPI partner collection recording",
    )
    AppDestination.PRODUCTS -> listOf(
      "Add & update snack SKUs, pack sizes, and wholesale prices",
      "Configure low-stock alert thresholds",
      "Manage supplier cost rates & product codes",
    )
    else -> emptyList()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.3.dp, MintCardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MintBadgeBg),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = destination.selectedIcon,
            contentDescription = destination.title,
            tint = EmeraldDeepPrimary,
            modifier = Modifier.size(36.dp),
          )
        }

        Surface(
          color = MintAccentChip,
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = "DUMMY MODULE • NEXT PHASE",
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldDeepPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }

        Text(
          text = destination.title,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
          text = "This placeholder page reserves navigation for Phase 2 while Home, Shops, and Orders are active.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          plannedBullets.forEach { bullet ->
            Row(
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = bullet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }
  }
}
