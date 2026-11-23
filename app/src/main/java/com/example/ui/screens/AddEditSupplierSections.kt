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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.RoutesCatalog
import com.example.data.model.SupplierCatalogHelper
import com.example.ui.components.CompactDropdownMenuItem
import com.example.ui.components.RealOsmMapView
import com.example.ui.components.rememberFileExistsAsync
import com.example.ui.components.rememberThumbnailImageRequest
import com.example.ui.theme.semanticColors
import java.util.Locale

@Composable
fun SupplierInfoSectionCard(
  supplierCode: String,
  isSupplierCodeEditable: Boolean,
  onSupplierCodeChange: (String) -> Unit,
  onToggleSupplierCodeEditable: () -> Unit,
  supplierName: String,
  onSupplierNameChange: (String) -> Unit,
  category: String,
  onCategoryChange: (String) -> Unit,
  gstin: String,
  onGstinChange: (String) -> Unit,
  kycVerified: Boolean,
  onKycVerifiedChange: (Boolean) -> Unit,
  creditTerms: String,
  onCreditTermsChange: (String) -> Unit,
  photoPath: String,
  onTakePhoto: () -> Unit,
  onUploadImage: () -> Unit,
  onRemovePhoto: () -> Unit,
) {
  val hasCustomPhoto = rememberFileExistsAsync(photoPath)
  var categoryMenuExpanded by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      SectionCardHeader(
        icon = Icons.Rounded.Storefront,
        title = "Supplier Information",
        badgeText = "Section A",
      )

      // Supplier ID
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Supplier ID",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
          )
          Text(
            text = "Auto Generated (System)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
          )
        }

        if (isSupplierCodeEditable) {
          OutlinedTextField(
            value = supplierCode,
            onValueChange = { onSupplierCodeChange(it.uppercase()) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
              IconButton(onClick = onToggleSupplierCodeEditable) {
                Icon(Icons.Rounded.Check, contentDescription = "Lock Supplier ID", tint = MaterialTheme.colorScheme.secondary)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("supplier_code_input"),
          )
        } else {
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Lock,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "[ $supplierCode ]",
                  style = MaterialTheme.typography.titleMedium,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.secondary,
                )
              }
              TextButton(onClick = onToggleSupplierCodeEditable) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit ID", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }

      // Company Name
      OutlinedTextField(
        value = supplierName,
        onValueChange = onSupplierNameChange,
        singleLine = true,
        label = { Text("Supplier / Company Name *") },
        placeholder = { Text("e.g., Malabar Wholesale Distributors") },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("supplier_name_input"),
      )

      // Category Dropdown
      Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { categoryMenuExpanded = true }
            .testTag("supplier_category_dropdown"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text(
                text = "Supply Category",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = category,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Icon(Icons.Rounded.ExpandMore, contentDescription = "Select category", modifier = Modifier.size(20.dp))
          }
        }
        DropdownMenu(
          expanded = categoryMenuExpanded,
          onDismissRequest = { categoryMenuExpanded = false },
          modifier = Modifier.heightIn(max = 260.dp),
        ) {
          SupplierCatalogHelper.defaultCategories.forEach { cat ->
            CompactDropdownMenuItem(
              text = cat,
              selected = category == cat,
              onClick = {
                onCategoryChange(cat)
                categoryMenuExpanded = false
              },
            )
          }
        }
      }

      // Photo Capture / Picker
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Supplier Photograph / Warehouse Logo (Optional)",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center,
            ) {
              if (hasCustomPhoto) {
                val photoRequest = rememberThumbnailImageRequest(
                  path = photoPath,
                  targetSizePx = 256,
                )
                AsyncImage(
                  model = photoRequest,
                  contentDescription = "Supplier facility photo",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Icon(
                  imageVector = Icons.Rounded.Storefront,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(28.dp),
                )
              }
            }
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = onTakePhoto,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Camera", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                  onClick = onUploadImage,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Gallery", style = MaterialTheme.typography.labelSmall)
                }
              }
              if (hasCustomPhoto) {
                TextButton(onClick = onRemovePhoto) {
                  Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Remove Photo", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }

      // GSTIN + KYC Verification
      OutlinedTextField(
        value = gstin,
        onValueChange = { onGstinChange(it.uppercase().take(15)) },
        singleLine = true,
        label = { Text("GSTIN / Tax Registration Number") },
        placeholder = { Text("e.g., 32AABCM4421K1Z5") },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("supplier_gstin_input"),
      )

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Verified,
              contentDescription = null,
              tint = if (kycVerified) MaterialTheme.semanticColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
              Text(
                text = "KYC Verified Partner",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "Verified GST & trade compliance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Switch(
            checked = kycVerified,
            onCheckedChange = onKycVerifiedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.testTag("supplier_kyc_switch"),
          )
        }
      }

      // Credit Terms chips
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Credit & Payment Terms",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SupplierCatalogHelper.creditTermOptions.forEach { option ->
            val selected = creditTerms == option
            FilterChip(
              selected = selected,
              onClick = { onCreditTermsChange(option) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
              ),
              label = { Text(option, style = MaterialTheme.typography.labelSmall) },
            )
          }
        }
      }
    }
  }
}

@Composable
fun SupplierContactSectionCard(
  contactPerson: String,
  onContactPersonChange: (String) -> Unit,
  contactRole: String,
  onContactRoleChange: (String) -> Unit,
  altContactPerson: String,
  onAltContactPersonChange: (String) -> Unit,
  phone: String,
  onPhoneChange: (String) -> Unit,
  altPhone: String,
  onAltPhoneChange: (String) -> Unit,
  email: String,
  onEmailChange: (String) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      SectionCardHeader(
        icon = Icons.Rounded.ContactPhone,
        title = "Contact Information",
        badgeText = "Section B",
      )

      OutlinedTextField(
        value = contactPerson,
        onValueChange = onContactPersonChange,
        singleLine = true,
        label = { Text("Primary Contact Person *") },
        placeholder = { Text("e.g., Mohammed Shafi") },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("supplier_contact_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = contactRole,
          onValueChange = onContactRoleChange,
          singleLine = true,
          label = { Text("Role / Designation") },
          placeholder = { Text("Key Account Mgr") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = altContactPerson,
          onValueChange = onAltContactPersonChange,
          singleLine = true,
          label = { Text("Alt Contact Person") },
          placeholder = { Text("Backup rep") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
      }

      OutlinedTextField(
        value = phone,
        onValueChange = { onPhoneChange(it.filter { ch -> ch.isDigit() || ch == '+' }.take(14)) },
        singleLine = true,
        label = { Text("Primary Mobile Number *") },
        prefix = { Text("+91 ") },
        placeholder = { Text("9876554321") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("supplier_phone_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = altPhone,
          onValueChange = { onAltPhoneChange(it.filter { ch -> ch.isDigit() || ch == '+' }.take(14)) },
          singleLine = true,
          label = { Text("Alt Mobile") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = email,
          onValueChange = onEmailChange,
          singleLine = true,
          label = { Text("Email Address") },
          placeholder = { Text("sales@supplier.in") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1.3f)
            .testTag("supplier_email_input"),
        )
      }
    }
  }
}

@Composable
fun SupplierLocationSectionCard(
  fetchingGps: Boolean,
  latitude: Double,
  longitude: Double,
  gpsStatusText: String,
  onCaptureGps: () -> Unit,
  address: String,
  onAddressChange: (String) -> Unit,
  pinCode: String,
  onPinCodeChange: (String) -> Unit,
  mapsLink: String,
  onMapsLinkChange: (String) -> Unit,
  onOpenMaps: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      SectionCardHeader(
        icon = Icons.Rounded.LocationOn,
        title = "Supplier Location",
        badgeText = "Section C",
      )

      Button(
        onClick = onCaptureGps,
        enabled = !fetchingGps,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (fetchingGps) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
          Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (fetchingGps) "Capturing GPS Coordinates..." else "Use Current GPS Location",
          fontWeight = FontWeight.SemiBold,
        )
      }

      if (gpsStatusText.isNotBlank()) {
        Text(
          text = gpsStatusText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.secondary,
        )
      }

      if (latitude != 0.0 || longitude != 0.0) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        ) {
          RealOsmMapView(
            shops = emptyList(),
            routes = emptyList(),
            selectedRouteId = RoutesCatalog.ALL,
            selectedShopId = null,
            showPolylines = false,
            userLatitude = latitude,
            userLongitude = longitude,
            compactPreviewMode = true,
            focusLatitude = latitude,
            focusLongitude = longitude,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      OutlinedTextField(
        value = address,
        onValueChange = onAddressChange,
        minLines = 2,
        label = { Text("Complete Business Address *") },
        placeholder = { Text("Warehouse street, industrial estate, city, state") },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("supplier_address_input"),
      )

      OutlinedTextField(
        value = pinCode,
        onValueChange = { onPinCodeChange(it.filter { ch -> ch.isDigit() }.take(8)) },
        singleLine = true,
        label = { Text("PIN Code *") },
        placeholder = { Text("e.g., 683104") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("supplier_pincode_input"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = mapsLink,
          onValueChange = onMapsLinkChange,
          singleLine = true,
          label = { Text("Google Maps Link (Optional)") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedButton(
          onClick = onOpenMaps,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(48.dp),
        ) {
          Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open Maps", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Open")
        }
      }
    }
  }
}

@Composable
fun SupplierRatesAndAuditSections(
  confirmedPriceNotes: String,
  onConfirmedPriceNotesChange: (String) -> Unit,
  dateTimeAdded: String,
  addedByName: String,
  isOnline: Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SectionCardHeader(
          icon = Icons.Rounded.CurrencyRupee,
          title = "Confirmed Supply Rates",
          badgeText = "Section D",
        )
        OutlinedTextField(
          value = confirmedPriceNotes,
          onValueChange = onConfirmedPriceNotesChange,
          minLines = 2,
          label = { Text("Contracted Rates / Commodity Price Notes") },
          placeholder = { Text("e.g., Coconut Oil Tanker: ₹162/L • Matta Rice 50kg: ₹2,450") },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("supplier_price_notes_input"),
        )
      }
    }

    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
      ),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Registration Audit Details (Read-Only)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = "Added: $dateTimeAdded • By: $addedByName",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Sync,
            contentDescription = null,
            tint = if (isOnline) MaterialTheme.semanticColors.success else MaterialTheme.semanticColors.warning,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = if (isOnline) {
              "Online — syncs immediately to Cloud Firestore"
            } else {
              "Offline — saved to device queue & synced later"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (isOnline) MaterialTheme.semanticColors.success else MaterialTheme.semanticColors.warning,
          )
        }
      }
    }
  }
}
