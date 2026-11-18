package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.semanticColors

@Composable
internal fun ProductBasicInfoSectionCard(
  name: String,
  onNameChange: (String) -> Unit,
  sku: String,
  onSkuChange: (String) -> Unit,
  barcode: String,
  onBarcodeChange: (String) -> Unit,
  category: String,
  allCategories: List<String>,
  onCategorySelected: (String) -> Unit,
  onOpenAddCategory: () -> Unit,
  brand: String,
  onBrandChange: (String) -> Unit,
  photoPath: String,
  onTakePhoto: () -> Unit,
  onUploadImage: () -> Unit,
  onRemovePhoto: () -> Unit,
  notes: String,
  onNotesChange: (String) -> Unit,
  isActiveCatalog: Boolean,
  onActiveCatalogChange: (Boolean) -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ProductFormSectionHeader(
        icon = Icons.Rounded.Inventory,
        title = "Section A — Basic Product Information",
        subtitle = "Core catalog identification for billing, cataloging & dispatch",
        badgeLabel = "Mandatory",
      )

      OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Product Trade Name *") },
        placeholder = { Text("e.g. Kerala Special Banana Chips") },
        trailingIcon = {
          if (name.isNotBlank()) {
            Icon(
              imageVector = Icons.Rounded.Verified,
              contentDescription = "Verified title",
              tint = MaterialTheme.semanticColors.success,
            )
          }
        },
        supportingText = { Text("Displayed on retailer invoices and field rep order books") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("product_name_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = sku,
          onValueChange = onSkuChange,
          label = { Text("SKU / Code *") },
          placeholder = { Text("SKU-BNC-008") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("product_sku_input"),
        )
        OutlinedTextField(
          value = barcode,
          onValueChange = onBarcodeChange,
          label = { Text("Barcode / EAN") },
          placeholder = { Text("8901234567890") },
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.QrCode,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("product_barcode_input"),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Category Classification",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          TextButton(
            onClick = onOpenAddCategory,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.testTag("add_category_button"),
          ) {
            Text("+ Add Category", style = MaterialTheme.typography.labelMedium)
          }
        }
        ProductDropdownField(
          label = "Category",
          selectedValue = category,
          options = allCategories,
          onOptionSelected = onCategorySelected,
          fieldTestTag = "product_category_dropdown",
        )
      }

      OutlinedTextField(
        value = brand,
        onValueChange = onBrandChange,
        label = { Text("Brand Affinity") },
        placeholder = { Text("e.g. Homely Gold (In-House)") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("product_brand_input"),
      )

      Text(
        text = "Product Photographic Proof",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      ProductPhotoProofCard(
        photoPath = photoPath,
        onTakePhoto = onTakePhoto,
        onUploadImage = onUploadImage,
        onRemovePhoto = onRemovePhoto,
      )

      OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text("B2B Specifications / Notes") },
        placeholder = { Text("Ingredients, shelf life, dispatch handling notes...") },
        minLines = 2,
        maxLines = 4,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("product_notes_input"),
      )

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Active Product Catalog Status",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = if (isActiveCatalog) {
                "Live / Active across field sales order books"
              } else {
                "Inactive / Hidden from new sales orders"
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = isActiveCatalog,
            onCheckedChange = onActiveCatalogChange,
            modifier = Modifier.testTag("product_active_switch"),
          )
        }
      }
    }
  }
}
