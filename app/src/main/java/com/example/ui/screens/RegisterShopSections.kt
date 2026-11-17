package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonPin
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PinDrop
import androidx.compose.material.icons.rounded.PriceCheck
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CatalogProduct
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import com.example.ui.components.RealOsmMapView
import com.example.data.model.SnackCatalog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningContainer
import com.example.ui.theme.CoralDanger
import com.example.ui.theme.CoralDangerContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.ForestContainer
import com.example.ui.theme.ForestSecondary
import com.example.ui.theme.MintBadgeBg
import com.example.ui.theme.MintCardBorder
import com.example.ui.theme.MintFieldBorder
import com.example.ui.theme.MintFieldFill
import com.example.ui.theme.MintHeaderTint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ConfirmedProductRate(
  val productCode: String,
  val productName: String,
  val packSize: String,
  val confirmedPriceText: String,
  val validityRange: String,
  val activeDays: Int = 30,
)

@Composable
fun SectionCardHeader(
  icon: ImageVector,
  title: String,
  badgeText: String,
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
      Surface(
        color = MintBadgeBg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(30.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ForestSecondary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
    Surface(
      color = MintBadgeBg,
      shape = RoundedCornerShape(999.dp),
      border = BorderStroke(1.dp, MintFieldBorder),
    ) {
      Text(
        text = badgeText,
        style = MaterialTheme.typography.labelSmall,
        color = ForestSecondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      )
    }
  }
}

@Composable
fun ShopInfoSectionCard(
  shopCode: String,
  isShopCodeEditable: Boolean,
  onShopCodeChange: (String) -> Unit,
  onToggleShopCodeEditable: () -> Unit,
  shopName: String,
  onShopNameChange: (String) -> Unit,
  photoPath: String,
  onTakePhoto: () -> Unit,
  onUploadImage: () -> Unit,
  onRemovePhoto: () -> Unit,
) {
  val hasCustomPhoto = photoPath.isNotBlank() && File(photoPath).exists()
  val isNameValid = shopName.trim().length >= 3

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = BorderStroke(1.2.dp, MintCardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      SectionCardHeader(
        icon = Icons.Rounded.Storefront,
        title = "Shop Information",
        badgeText = "Section A",
      )

      // Field 1: Unique Shop ID
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Unique Shop ID",
            style = MaterialTheme.typography.labelLarge,
            color = ForestSecondary,
          )
          Surface(
            color = ForestContainer.copy(alpha = 0.85f),
            shape = RoundedCornerShape(999.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = null,
                tint = ForestSecondary,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = "Auto Generated (System)",
                style = MaterialTheme.typography.labelSmall,
                color = ForestSecondary,
              )
            }
          }
        }

        if (isShopCodeEditable) {
          OutlinedTextField(
            value = shopCode,
            onValueChange = { onShopCodeChange(it.uppercase()) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
              IconButton(onClick = onToggleShopCodeEditable) {
                Icon(Icons.Rounded.Check, contentDescription = "Lock Shop ID", tint = ForestSecondary)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("shop_code_input"),
          )
        } else {
          Surface(
            color = MintFieldFill,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MintFieldBorder),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable(onClick = onToggleShopCodeEditable)
              .testTag("shop_code_input"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Lock,
                  contentDescription = null,
                  tint = ForestSecondary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = shopCode,
                  style = MaterialTheme.typography.titleMedium,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = ForestSecondary,
                )
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(6.dp),
                ) {
                  Text(
                    text = "Allocated",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
                Icon(
                  imageVector = Icons.Rounded.Edit,
                  contentDescription = "Edit Shop ID",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      }

      // Field 2: Shop Name
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Shop Name",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text("*", color = CoralDanger, fontWeight = FontWeight.Bold)
          }
          if (isNameValid) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = EmeraldSuccess,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "Valid",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldSuccess,
              )
            }
          }
        }

        OutlinedTextField(
          value = shopName,
          onValueChange = onShopNameChange,
          placeholder = { Text("e.g. Green Valley Stores") },
          trailingIcon = {
            Icon(
              imageVector = Icons.Rounded.Business,
              contentDescription = null,
              tint = ForestSecondary,
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MintFieldFill,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = MintFieldBorder,
            focusedBorderColor = ForestSecondary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_name_input"),
        )
      }

      // Field 3: Shop Frontage Photo
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Shop Frontage Photo",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Optional / Recommended",
            style = MaterialTheme.typography.labelSmall,
            color = ForestSecondary,
          )
        }

        Surface(
          color = MintFieldFill,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MintFieldBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
              if (hasCustomPhoto) {
                AsyncImage(
                  model = File(photoPath),
                  contentDescription = "Captured shop frontage photo",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Image(
                  painter = painterResource(id = R.drawable.img_shop_placeholder),
                  contentDescription = "Sample shop storefront preview",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }

              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.55f),
                      ),
                    )
                  )
                  .padding(10.dp),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(999.dp),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Icon(
                        imageVector = if (hasCustomPhoto) Icons.Rounded.Check else Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                      )
                      Text(
                        text = if (hasCustomPhoto) "Captured" else "Default Preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                      )
                    }
                  }

                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                      shape = RoundedCornerShape(999.dp),
                      modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onUploadImage),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.Sync,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.size(14.dp),
                        )
                        Text(
                          text = "Replace",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                      }
                    }

                    if (hasCustomPhoto) {
                      Surface(
                        color = CoralDangerContainer,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                          .clip(RoundedCornerShape(999.dp))
                          .clickable(onClick = onRemovePhoto),
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.Delete,
                          contentDescription = "Remove photo",
                          tint = CoralDanger,
                          modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .size(15.dp),
                        )
                      }
                    }
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
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  contentColor = ForestSecondary,
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .testTag("take_shop_photo_button"),
              ) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take Photo", style = MaterialTheme.typography.labelLarge)
              }

              OutlinedButton(
                onClick = onUploadImage,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(44.dp)
                  .testTag("upload_shop_photo_button"),
              ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload Image", style = MaterialTheme.typography.labelLarge)
              }
            }

            Text(
              text = "Supports JPG, PNG up to 10MB",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.align(Alignment.CenterHorizontally),
            )
          }
        }
      }
    }
  }
}

@Composable
fun OwnerContactSectionCard(
  ownerName: String,
  onOwnerNameChange: (String) -> Unit,
  phone: String,
  onPhoneChange: (String) -> Unit,
  secondaryPhone: String,
  onSecondaryPhoneChange: (String) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = BorderStroke(1.2.dp, MintCardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SectionCardHeader(
        icon = Icons.Rounded.PersonPin,
        title = "Owner / Contact Information",
        badgeText = "Section B",
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Owner Name",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text("*", color = CoralDanger, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
          value = ownerName,
          onValueChange = onOwnerNameChange,
          placeholder = { Text("Rajesh Kumar") },
          trailingIcon = {
            Icon(Icons.Rounded.Badge, contentDescription = null, tint = ForestSecondary)
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MintFieldFill,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = MintFieldBorder,
            focusedBorderColor = ForestSecondary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_owner_input"),
        )
      }

      PhoneWithCountryPrefixField(
        label = "Primary Mobile Number",
        required = true,
        value = phone,
        onValueChange = onPhoneChange,
        placeholder = "98765 43210",
        helperText = "Used for order dispatches, SMS receipts & digital OTPs",
        trailingIcon = Icons.Rounded.PhoneIphone,
        testTag = "shop_phone_input",
      )

      PhoneWithCountryPrefixField(
        label = "Alternative Mobile Number",
        required = false,
        value = secondaryPhone,
        onValueChange = onSecondaryPhoneChange,
        placeholder = "98765 00000",
        helperText = "Optional backup number for delivery coordinates",
        trailingIcon = Icons.Rounded.ContactPhone,
        testTag = "shop_secondary_phone_input",
      )
    }
  }
}

@Composable
private fun PhoneWithCountryPrefixField(
  label: String,
  required: Boolean,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  helperText: String,
  trailingIcon: ImageVector,
  testTag: String,
) {
  Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (required) {
          Text("*", color = CoralDanger, fontWeight = FontWeight.Bold)
        }
      }
      if (!required) {
        Text(
          text = "Optional",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    OutlinedTextField(
      value = value,
      onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '+' }.take(13)) },
      placeholder = { Text(placeholder) },
      leadingIcon = {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.padding(start = 6.dp),
        ) {
          Text(
            text = "IN +91",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
          )
        }
      },
      trailingIcon = {
        Icon(
          imageVector = trailingIcon,
          contentDescription = null,
          tint = if (required) ForestSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = MintFieldFill,
        focusedContainerColor = Color.White,
        unfocusedBorderColor = MintFieldBorder,
        focusedBorderColor = ForestSecondary,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag(testTag),
    )

    Text(
      text = helperText,
      style = MaterialTheme.typography.labelSmall,
      color = ForestSecondary,
    )
  }
}

@Composable
fun ShopLocationSectionCard(
  fetchingGps: Boolean,
  latitude: Double,
  longitude: Double,
  liveUserLatitude: Double? = null,
  liveUserLongitude: Double? = null,
  gpsStatusText: String,
  onCaptureGps: () -> Unit,
  address: String,
  onAddressChange: (String) -> Unit,
  pinCode: String,
  onPinCodeChange: (String) -> Unit,
  selectedRoute: String,
  availableRoutes: List<DeliveryRoute> = RoutesCatalog.starterRoutes,
  onRouteSelect: (String) -> Unit,
  mapsLink: String,
  onMapsLinkChange: (String) -> Unit,
  onOpenMaps: () -> Unit,
) {
  var routeDropdownOpen by remember { mutableStateOf(false) }
  val hasCoordinates = latitude != 0.0 || longitude != 0.0

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = BorderStroke(1.2.dp, MintCardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SectionCardHeader(
        icon = Icons.Rounded.PinDrop,
        title = "Shop Location",
        badgeText = "Section C",
      )

      // Use Current GPS Location Button
      Surface(
        color = MintBadgeBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MintCardBorder),
        modifier = Modifier
          .fillMaxWidth()
          .minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(12.dp))
          .clickable(enabled = !fetchingGps, onClick = onCaptureGps)
          .testTag("capture_gps_button"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (fetchingGps) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = ForestSecondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Fetching GPS Lock...",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = ForestSecondary,
            )
          } else {
            Icon(
              imageVector = if (hasCoordinates) Icons.Rounded.CheckCircle else Icons.Rounded.MyLocation,
              contentDescription = null,
              tint = ForestSecondary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (hasCoordinates) "Update Current GPS Location" else "Use Current GPS Location",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = ForestSecondary,
            )
          }
        }
      }

      if (gpsStatusText.isNotBlank()) {
        Text(
          text = gpsStatusText,
          style = MaterialTheme.typography.bodySmall,
          color = if (hasCoordinates) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Shop Location / Address
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Shop Location / Address",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text("*", color = CoralDanger, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
          value = address,
          onValueChange = onAddressChange,
          placeholder = { Text("Enter shop address or area (e.g. Edappally, Kochi, Kerala)") },
          minLines = 3,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MintFieldFill,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = MintFieldBorder,
            focusedBorderColor = ForestSecondary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_address_input"),
        )
      }

      // Pin Code + Delivery Route row (keeps route filters & PIN validation seamless)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = pinCode,
          onValueChange = { onPinCodeChange(it.filter { c -> c.isDigit() }.take(10)) },
          label = { Text("Pin Code *") },
          placeholder = { Text("682024") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MintFieldFill,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = MintFieldBorder,
            focusedBorderColor = ForestSecondary,
          ),
          modifier = Modifier
            .weight(0.78f)
            .testTag("shop_pincode_input"),
        )

        Box(modifier = Modifier.weight(1.22f)) {
          OutlinedTextField(
            value = selectedRoute,
            onValueChange = {},
            readOnly = true,
            label = { Text("Delivery Route") },
            trailingIcon = {
              Icon(Icons.Rounded.ExpandMore, contentDescription = null)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              unfocusedContainerColor = MintFieldFill,
              focusedContainerColor = Color.White,
              unfocusedBorderColor = MintFieldBorder,
              focusedBorderColor = ForestSecondary,
            ),
            modifier = Modifier.fillMaxWidth(),
          )
          Box(
            modifier = Modifier
              .matchParentSize()
              .clickable { routeDropdownOpen = true },
          )
          val routeOptions = remember(availableRoutes) {
            (availableRoutes.map { it.id } + RoutesCatalog.UNALLOCATED).distinct()
          }
          DropdownMenu(
            expanded = routeDropdownOpen,
            onDismissRequest = { routeDropdownOpen = false },
          ) {
            routeOptions.forEach { route ->
              DropdownMenuItem(
                text = { Text(route) },
                onClick = {
                  onRouteSelect(route)
                  routeDropdownOpen = false
                },
              )
            }
          }
        }
      }

      // Google Maps Link + Open button
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Google Maps Link",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Optional",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        OutlinedTextField(
          value = mapsLink,
          onValueChange = onMapsLinkChange,
          placeholder = { Text("Paste Google Maps location link") },
          leadingIcon = {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = ForestSecondary)
          },
          trailingIcon = {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .padding(end = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenMaps),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "Open",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = ForestSecondary,
                )
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                  contentDescription = "Open in Google Maps",
                  tint = ForestSecondary,
                  modifier = Modifier.size(13.dp),
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MintFieldFill,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = MintFieldBorder,
            focusedBorderColor = ForestSecondary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_maps_link_input"),
        )
      }

      // Actual OpenStreetMap Preview centered on Shop GPS or Device Live GPS
      MapPinVisualizerCard(
        latitude = latitude,
        longitude = longitude,
        liveUserLatitude = liveUserLatitude,
        liveUserLongitude = liveUserLongitude,
        addressHint = address,
        onClick = {
          if (latitude == 0.0 && longitude == 0.0) onCaptureGps() else onOpenMaps()
        },
      )
    }
  }
}

@Composable
private fun MapPinVisualizerCard(
  latitude: Double,
  longitude: Double,
  liveUserLatitude: Double?,
  liveUserLongitude: Double?,
  addressHint: String,
  onClick: () -> Unit,
) {
  val hasCoords = latitude != 0.0 || longitude != 0.0
  val hasLiveGps = liveUserLatitude != null && liveUserLongitude != null &&
    (liveUserLatitude != 0.0 || liveUserLongitude != 0.0)

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(158.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    RealOsmMapView(
      shops = emptyList(),
      routes = emptyList(),
      selectedRouteId = RoutesCatalog.ALL,
      selectedShopId = null,
      showPolylines = false,
      userLatitude = liveUserLatitude,
      userLongitude = liveUserLongitude,
      compactPreviewMode = true,
      focusLatitude = if (hasCoords) latitude else liveUserLatitude,
      focusLongitude = if (hasCoords) longitude else liveUserLongitude,
      onMapSurfaceTapped = onClick,
      modifier = Modifier.fillMaxSize(),
    )

    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
      shape = RoundedCornerShape(999.dp),
      shadowElevation = 3.dp,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 10.dp, start = 10.dp, end = 10.dp),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.PinDrop,
          contentDescription = null,
          tint = ForestSecondary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = when {
            hasCoords -> String.format(Locale.US, "GPS Pin (%.4f° N, %.4f° E)", latitude, longitude)
            hasLiveGps -> String.format(Locale.US, "Live GPS Ready (%.4f, %.4f) • Tap to lock", liveUserLatitude, liveUserLongitude)
            else -> {
              val shortAddr = addressHint.trim().substringAfterLast(",").trim()
              if (shortAddr.isNotBlank()) "$shortAddr • Tap to lock live GPS" else "Tap to lock actual GPS on map"
            }
          },
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun ConfirmedPricingSectionCard(
  enabled: Boolean,
  onToggleEnabled: (Boolean) -> Unit,
  rateRows: List<ConfirmedProductRate>,
  onUpdateRow: (index: Int, updated: ConfirmedProductRate) -> Unit,
  onAddRow: () -> Unit,
  onRemoveRow: (index: Int) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = BorderStroke(1.2.dp, MintCardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SectionCardHeader(
        icon = Icons.Rounded.PriceCheck,
        title = "Confirmed Product Pricing",
        badgeText = "Section D • B2B Rates",
      )

      // Interactive Toggle Card
      Surface(
        color = MintFieldFill,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MintFieldBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            color = ForestContainer,
            shape = CircleShape,
            modifier = Modifier.size(36.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Payments,
                contentDescription = null,
                tint = ForestSecondary,
                modifier = Modifier.size(18.dp),
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Enter Confirmed Price for Products",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Set agreed wholesale rates for this retailer during onboarding",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Switch(
            checked = enabled,
            onCheckedChange = onToggleEnabled,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = ForestSecondary,
            ),
            modifier = Modifier.testTag("confirmed_pricing_switch"),
          )
        }
      }

      if (enabled) {
        rateRows.forEachIndexed { index, row ->
          ConfirmedProductRateCard(
            rate = row,
            canRemove = rateRows.size > 1,
            onUpdate = { updated -> onUpdateRow(index, updated) },
            onRemove = { onRemoveRow(index) },
          )
        }

        OutlinedButton(
          onClick = onAddRow,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, ForestSecondary.copy(alpha = 0.35f)),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = ForestSecondary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("add_product_rate_button"),
        ) {
          Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "+ Add Another Product Rate",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
private fun ConfirmedProductRateCard(
  rate: ConfirmedProductRate,
  canRemove: Boolean,
  onUpdate: (ConfirmedProductRate) -> Unit,
  onRemove: () -> Unit,
) {
  val distinctProductNames = remember {
    SnackCatalog.products.map { it.name }.distinct()
  }
  val matchingVariants = remember(rate.productName) {
    SnackCatalog.products.filter { it.name == rate.productName }.ifEmpty { SnackCatalog.products }
  }
  val selectedCatalogProduct: CatalogProduct = remember(rate.productCode, rate.productName, rate.packSize) {
    SnackCatalog.products.firstOrNull { it.code == rate.productCode }
      ?: matchingVariants.firstOrNull { it.packSize == rate.packSize }
      ?: matchingVariants.first()
  }

  var productMenuOpen by remember { mutableStateOf(false) }
  var packMenuOpen by remember { mutableStateOf(false) }
  var showValidityDialog by remember { mutableStateOf(false) }

  val stdWholesale = selectedCatalogProduct.defaultPrice
  val enteredPrice = rate.confirmedPriceText.toDoubleOrNull()
  val marginNote = remember(enteredPrice, stdWholesale) {
    if (enteredPrice == null) {
      "Std list: ₹${String.format(Locale.US, "%.2f", stdWholesale)}"
    } else {
      val diff = stdWholesale - enteredPrice
      when {
        diff > 0.01 -> String.format(Locale.US, "₹%.2f below list (Margin Protected)", diff)
        diff < -0.01 -> String.format(Locale.US, "₹%.2f above list price", -diff)
        else -> "At standard wholesale rate"
      }
    }
  }

  Surface(
    color = MintFieldFill,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MintFieldBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Select Product header + SKU code
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Select Product",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text("*", color = CoralDanger, fontWeight = FontWeight.Bold)
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = selectedCatalogProduct.code,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (canRemove) {
            Icon(
              imageVector = Icons.Rounded.Delete,
              contentDescription = "Remove product rate",
              tint = CoralDanger,
              modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onRemove),
            )
          }
        }
      }

      // Product dropdown
      Box {
        OutlinedTextField(
          value = "${selectedCatalogProduct.name} [${selectedCatalogProduct.code}]",
          onValueChange = {},
          readOnly = true,
          leadingIcon = { Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp)) },
          trailingIcon = { Icon(Icons.Rounded.ExpandMore, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier.fillMaxWidth(),
        )
        Box(
          modifier = Modifier
            .matchParentSize()
            .clickable { productMenuOpen = true },
        )
        DropdownMenu(
          expanded = productMenuOpen,
          onDismissRequest = { productMenuOpen = false },
        ) {
          distinctProductNames.forEach { prodName ->
            val firstVariant = SnackCatalog.products.first { it.name == prodName }
            DropdownMenuItem(
              text = { Text("$prodName (${firstVariant.category})") },
              onClick = {
                onUpdate(
                  rate.copy(
                    productCode = firstVariant.code,
                    productName = firstVariant.name,
                    packSize = firstVariant.packSize,
                    confirmedPriceText = String.format(Locale.US, "%.2f", firstVariant.defaultPrice),
                  )
                )
                productMenuOpen = false
              },
            )
          }
        }
      }

      // Select Pack Size header + Std Wholesale label
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Select Pack Size",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text("*", color = CoralDanger, fontWeight = FontWeight.Bold)
        }
        Text(
          text = String.format(Locale.US, "Std Wholesale: ₹%.2f", stdWholesale),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Box {
        OutlinedTextField(
          value = "${selectedCatalogProduct.packSize} Pouch (Std: ₹${String.format(Locale.US, "%.2f", stdWholesale)})",
          onValueChange = {},
          readOnly = true,
          leadingIcon = { Icon(Icons.Rounded.Straighten, contentDescription = null, modifier = Modifier.size(18.dp)) },
          trailingIcon = { Icon(Icons.Rounded.ExpandMore, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier.fillMaxWidth(),
        )
        Box(
          modifier = Modifier
            .matchParentSize()
            .clickable { packMenuOpen = true },
        )
        DropdownMenu(
          expanded = packMenuOpen,
          onDismissRequest = { packMenuOpen = false },
        ) {
          matchingVariants.forEach { variant ->
            DropdownMenuItem(
              text = {
                Text("${variant.packSize} Pouch (Std: ₹${String.format(Locale.US, "%.2f", variant.defaultPrice)})")
              },
              onClick = {
                onUpdate(
                  rate.copy(
                    productCode = variant.code,
                    packSize = variant.packSize,
                    confirmedPriceText = String.format(Locale.US, "%.2f", variant.defaultPrice),
                  )
                )
                packMenuOpen = false
              },
            )
          }
        }
      }

      // Confirmed Selling Price per Pack + live margin badge
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f),
          ) {
            Text(
              text = "Confirmed Selling Price",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
            Text("*", color = CoralDanger, fontWeight = FontWeight.Bold)
          }
          Surface(
            color = ForestContainer.copy(alpha = 0.8f),
            shape = RoundedCornerShape(999.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.TrendingDown,
                contentDescription = null,
                tint = ForestSecondary,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text = marginNote,
                style = MaterialTheme.typography.labelSmall,
                color = ForestSecondary,
              )
            }
          }
        }

        OutlinedTextField(
          value = rate.confirmedPriceText,
          onValueChange = { raw ->
            val sanitized = raw.filter { it.isDigit() || it == '.' }
            if (sanitized.count { it == '.' } <= 1) {
              onUpdate(rate.copy(confirmedPriceText = sanitized.take(8)))
            }
          },
          leadingIcon = {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.padding(start = 6.dp),
            ) {
              Text(
                text = "₹",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          },
          suffix = {
            Text(
              text = "/ pack",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier.fillMaxWidth(),
        )
      }

      // Date Agreed / Validity
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Date Agreed / Validity",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "${rate.activeDays} Days Active",
            style = MaterialTheme.typography.labelSmall,
            color = ForestSecondary,
          )
        }
        OutlinedTextField(
          value = rate.validityRange,
          onValueChange = { onUpdate(rate.copy(validityRange = it)) },
          leadingIcon = {
            IconButton(onClick = { showValidityDialog = true }) {
              Icon(Icons.Rounded.CalendarMonth, contentDescription = "Select validity preset", tint = ForestSecondary)
            }
          },
          trailingIcon = {
            TextButton(onClick = { showValidityDialog = true }) {
              Text("Presets", style = MaterialTheme.typography.labelSmall)
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }

  if (showValidityDialog) {
    AlertDialog(
      onDismissRequest = { showValidityDialog = false },
      title = { Text("Select Rate Validity") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(15, 30, 60, 90).forEach { days ->
            val range = formatValidityRangeFromToday(days)
            OutlinedButton(
              onClick = {
                onUpdate(rate.copy(validityRange = range, activeDays = days))
                showValidityDialog = false
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("$days Days ($range)")
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showValidityDialog = false }) {
          Text("Close")
        }
      },
    )
  }
}

@Composable
fun RegistrationMetadataCard(
  dateTimeAdded: String,
  addedByName: String,
  isOnline: Boolean,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = BorderStroke(1.2.dp, MintCardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Registration Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Text(
          text = "System Metadata",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      MetadataRowItem(
        icon = Icons.Rounded.Schedule,
        label = "Date & Time Added",
        value = dateTimeAdded,
      )

      Surface(
        color = MintFieldFill,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MintFieldBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Badge,
              contentDescription = null,
              tint = ForestSecondary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "Added By",
              style = MaterialTheme.typography.labelMedium,
              color = ForestSecondary,
            )
          }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = addedByName,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
              color = MintBadgeBg,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = "#FO-402",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = ForestSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      Surface(
        color = MintFieldFill,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MintFieldBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Sync,
              contentDescription = null,
              tint = ForestSecondary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "Sync Status",
              style = MaterialTheme.typography.labelMedium,
              color = ForestSecondary,
            )
          }
          Surface(
            color = if (isOnline) EmeraldSuccessContainer else AmberWarningContainer,
            shape = RoundedCornerShape(999.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(if (isOnline) EmeraldSuccess else AmberWarning),
              )
              Text(
                text = if (isOnline) "Ready to Cloud Sync" else "Draft / Saved Locally",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isOnline) EmeraldSuccess else AmberWarning,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetadataRowItem(
  icon: ImageVector,
  label: String,
  value: String,
) {
  Surface(
    color = MintFieldFill,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, MintFieldBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

fun formatValidityRangeFromToday(days: Int = 30): String {
  val fmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
  val start = Date()
  val cal = Calendar.getInstance().apply {
    time = start
    add(Calendar.DAY_OF_YEAR, days)
  }
  return "${fmt.format(start)} - ${fmt.format(cal.time)}"
}
