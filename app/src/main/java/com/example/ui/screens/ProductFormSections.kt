package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ProductCatalogHelper
import com.example.data.model.ProductVariant
import com.example.ui.components.rememberThumbnailImageRequest
import com.example.ui.theme.semanticColors
import java.io.File
import java.util.Locale

@Composable
internal fun ProductFormSectionHeader(
  icon: ImageVector,
  title: String,
  subtitle: String,
  badgeLabel: String,
  badgeIsSecondary: Boolean = false,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          color = if (badgeIsSecondary) {
            MaterialTheme.colorScheme.secondaryContainer
          } else {
            MaterialTheme.colorScheme.primaryContainer
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.size(34.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = if (badgeIsSecondary) {
                MaterialTheme.colorScheme.onSecondaryContainer
              } else {
                MaterialTheme.colorScheme.onPrimaryContainer
              },
              modifier = Modifier.size(18.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Surface(
        color = if (badgeIsSecondary) {
          MaterialTheme.colorScheme.surfaceVariant
        } else {
          MaterialTheme.colorScheme.secondaryContainer
        },
        shape = CircleShape,
      ) {
        Text(
          text = badgeLabel,
          style = MaterialTheme.typography.labelSmall,
          color = if (badgeIsSecondary) {
            MaterialTheme.colorScheme.onSurfaceVariant
          } else {
            MaterialTheme.colorScheme.onSecondaryContainer
          },
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductDropdownField(
  label: String,
  selectedValue: String,
  options: List<String>,
  onOptionSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  fieldTestTag: String = "",
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = selectedValue,
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
        .fillMaxWidth()
        .let { if (fieldTestTag.isNotEmpty()) it.testTag(fieldTestTag) else it },
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option, style = MaterialTheme.typography.bodyMedium) },
          onClick = {
            onOptionSelected(option)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
internal fun ProductPhotoProofCard(
  photoPath: String,
  onTakePhoto: () -> Unit,
  onUploadImage: () -> Unit,
  onRemovePhoto: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface),
        ) {
          if (photoPath.isNotBlank()) {
            val photoRequest = rememberThumbnailImageRequest(
              path = photoPath,
              targetSizePx = 240,
              fallbackDrawableRes = R.drawable.img_snacks_banner,
            )
            AsyncImage(
              model = photoRequest,
              contentDescription = "Product packaging photo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Image(
              painter = painterResource(id = R.drawable.img_snacks_banner),
              contentDescription = "Default product preview",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          val fileName = remember(photoPath) {
            if (photoPath.isBlank()) "Default packaging preview" else File(photoPath).name
          }
          Text(
            text = fileName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = if (photoPath.isBlank()) {
              "Capture or pick packaging proof photo"
            } else {
              "Saved on device • Ready for catalog"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (photoPath.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = "Replace",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onUploadImage),
              )
              Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "Remove",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.semanticColors.danger,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onRemovePhoto),
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onTakePhoto,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("take_product_photo_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.AddAPhoto,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Camera Photo", style = MaterialTheme.typography.labelMedium)
        }
        Button(
          onClick = onUploadImage,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("upload_product_photo_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Upload Image", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

@Composable
internal fun ProductVariantCard(
  variant: ProductVariant,
  index: Int,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val margin = ProductCatalogHelper.computeMarginPercent(variant.wholesalePrice, variant.mrp)
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(
      1.dp,
      if (variant.isPopular) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
      } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
      },
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("variant_card_$index"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (variant.sku.isNotBlank()) {
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = variant.sku,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          Text(
            text = variant.packSize,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (variant.isPopular) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = "Popular",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = String.format(Locale.US, "Wholesale: ₹%.2f", variant.wholesalePrice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
          if (variant.mrp > 0.0) {
            Text(
              text = String.format(Locale.US, "MRP: ₹%.2f", variant.mrp),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
              color = MaterialTheme.semanticColors.successContainer,
              shape = CircleShape,
            ) {
              Text(
                text = String.format(Locale.US, "%.1f%% Margin", margin),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.semanticColors.success,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onEdit,
          modifier = Modifier.size(36.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = "Edit variant ${variant.packSize}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
          )
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(36.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Remove variant ${variant.packSize}",
            tint = MaterialTheme.semanticColors.danger,
            modifier = Modifier.size(17.dp),
          )
        }
      }
    }
  }
}

@Composable
internal fun AddCategoryDialog(
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var categoryName by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Custom Category") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Create a new product classification for billing and catalog filtering.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = categoryName,
          onValueChange = { categoryName = it },
          label = { Text("Category Name") },
          placeholder = { Text("e.g. Beverages & Tea") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_category_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(categoryName.trim()) },
        enabled = categoryName.trim().isNotEmpty(),
        modifier = Modifier.testTag("confirm_add_category_button"),
      ) {
        Text("Add Category")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
internal fun VariantEditorCard(
  existing: ProductVariant?,
  suggestedSku: String,
  onConfirm: (ProductVariant) -> Unit,
  onDismiss: () -> Unit,
) {
  var sku by remember(existing, suggestedSku) { mutableStateOf(existing?.sku ?: suggestedSku) }
  var packSize by remember(existing) { mutableStateOf(existing?.packSize ?: "") }
  var wholesaleText by remember(existing) {
    mutableStateOf(
      if (existing != null && existing.wholesalePrice > 0) existing.wholesalePrice.toString() else ""
    )
  }
  var mrpText by remember(existing) {
    mutableStateOf(if (existing != null && existing.mrp > 0) existing.mrp.toString() else "")
  }
  var isPopular by remember(existing) { mutableStateOf(existing?.isPopular ?: false) }

  val wholesale = wholesaleText.toDoubleOrNull() ?: 0.0
  val mrp = mrpText.toDoubleOrNull() ?: 0.0
  val margin = ProductCatalogHelper.computeMarginPercent(wholesale, mrp)

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("variant_editor_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (existing == null) "New Variant SKU" else "Edit Variant SKU",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        if (mrp > 0.0 && wholesale > 0.0) {
          Surface(
            color = MaterialTheme.semanticColors.successContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = String.format(Locale.US, "%.1f%% Margin", margin),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.semanticColors.success,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }
      OutlinedTextField(
        value = sku,
        onValueChange = { sku = it },
        label = { Text("Variant SKU *") },
        placeholder = { Text("e.g. SKU-BNC-01") },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("variant_sku_input"),
      )
      OutlinedTextField(
        value = packSize,
        onValueChange = { packSize = it },
        label = { Text("Pack Size Label *") },
        placeholder = { Text("e.g. 100g Retail Pack") },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("variant_pack_input"),
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = wholesaleText,
          onValueChange = { next -> wholesaleText = next.filter { it.isDigit() || it == '.' } },
          label = { Text("Wholesale ₹ *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("variant_wholesale_input"),
        )
        OutlinedTextField(
          value = mrpText,
          onValueChange = { next -> mrpText = next.filter { it.isDigit() || it == '.' } },
          label = { Text("MRP ₹") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("variant_mrp_input"),
        )
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isPopular = !isPopular },
      ) {
        Checkbox(
          checked = isPopular,
          onCheckedChange = { isPopular = it },
        )
        Text(
          text = "Highlight as Popular Pack",
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = onDismiss) { Text("Cancel") }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = {
            onConfirm(
              ProductVariant(
                sku = sku.trim(),
                packSize = packSize.trim(),
                wholesalePrice = wholesale,
                mrp = mrp,
                isPopular = isPopular,
              )
            )
          },
          enabled = sku.isNotBlank() && packSize.isNotBlank() && wholesale > 0.0,
          modifier = Modifier.testTag("confirm_variant_button"),
        ) {
          Text(if (existing == null) "Add Variant" else "Save Variant")
        }
      }
    }
  }
}
