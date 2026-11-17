package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import java.io.File
import java.text.NumberFormat
import java.util.Locale

enum class AppDestination(
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val isPrimaryBottomBar: Boolean,
  val isBuilt: Boolean,
  val testTag: String,
) {
  HOME("Home", Icons.Rounded.Home, Icons.Outlined.Home, true, true, "nav_home"),
  SHOPS("Shops", Icons.Rounded.Storefront, Icons.Outlined.Storefront, true, true, "nav_shops"),
  ORDERS("Orders", Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, true, true, "nav_orders"),
  PACKING("Packing", Icons.Rounded.Inventory2, Icons.Outlined.Inventory2, true, true, "nav_packing"),
  TRANSPORT("Transport", Icons.Rounded.LocalShipping, Icons.Outlined.LocalShipping, true, true, "nav_transport"),
  SUPPLIERS("Suppliers", Icons.Rounded.Factory, Icons.Outlined.Factory, false, false, "nav_suppliers"),
  ROUTES("Routes", Icons.AutoMirrored.Rounded.AltRoute, Icons.AutoMirrored.Outlined.AltRoute, false, true, "nav_routes"),
  PAYMENTS("Payments", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, false, false, "nav_payments"),
  PRODUCTS("Products", Icons.Rounded.Warehouse, Icons.Outlined.Warehouse, false, false, "nav_products");

  companion object {
    val bottomBarPrimary = entries.filter { it.isPrimaryBottomBar }
    val expandedMenuItems = entries.filter { !it.isPrimaryBottomBar }
  }
}

fun formatInr(amount: Double): String {
  val format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
  format.maximumFractionDigits = 0
  return format.format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardRouteDropdown(
  selectedRoute: String,
  routes: List<DeliveryRoute>,
  onRouteSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  fieldTestTag: String = "route_filter",
) {
  var expanded by remember { mutableStateOf(false) }
  val routeOptions = remember(routes) {
    (listOf(RoutesCatalog.ALL) + routes.map { it.id } + RoutesCatalog.UNALLOCATED).distinct()
  }
  val displayText = if (selectedRoute == RoutesCatalog.ALL) "All Routes" else selectedRoute

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = displayText,
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = { Text("Route") },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
      },
      colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
        unfocusedContainerColor = com.example.ui.theme.MintFieldFill,
        focusedContainerColor = Color.White,
        unfocusedBorderColor = com.example.ui.theme.MintFieldBorder,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
      ),
      textStyle = MaterialTheme.typography.bodyMedium,
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier
        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
        .fillMaxWidth()
        .testTag(fieldTestTag),
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      routeOptions.forEach { route ->
        DropdownMenuItem(
          text = {
            Text(
              text = if (route == RoutesCatalog.ALL) "All Routes" else route,
              style = MaterialTheme.typography.bodyMedium,
            )
          },
          onClick = {
            onRouteSelected(route)
            expanded = false
          },
          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        )
      }
    }
  }
}

@Composable
fun StandardCompactSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  fieldTestTag: String,
  modifier: Modifier = Modifier,
  containerColor: Color = com.example.ui.theme.MintFieldFill,
  trailingIcon: ImageVector? = null,
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, com.example.ui.theme.MintFieldBorder),
    modifier = modifier
      .fillMaxWidth()
      .height(42.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
      Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (query.isEmpty()) {
          Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
          ),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(fieldTestTag),
        )
      }
      if (query.isNotEmpty()) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = "Clear search",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .size(18.dp)
            .clickable { onQueryChange("") },
        )
      } else if (trailingIcon != null) {
        Icon(
          imageVector = trailingIcon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

fun openDialer(context: Context, phone: String) {
  val clean = phone.filter { it.isDigit() || it == '+' }
  if (clean.isBlank()) {
    Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
    return
  }
  val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
    Toast.makeText(context, "No dialer app available", Toast.LENGTH_SHORT).show()
  }
}

fun openWhatsApp(context: Context, phone: String, message: String = "") {
  val digits = phone.filter { it.isDigit() }
  if (digits.isBlank()) {
    Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
    return
  }
  val withCountry = if (digits.length == 10) "91$digits" else digits
  val encoded = Uri.encode(message)
  val uri = Uri.parse("https://wa.me/$withCountry" + if (encoded.isNotBlank()) "?text=$encoded" else "")
  val intent = Intent(Intent.ACTION_VIEW, uri)
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
    Toast.makeText(context, "WhatsApp or browser not available", Toast.LENGTH_SHORT).show()
  }
}

fun copyPickedImageToInternalStorage(context: Context, sourceUri: Uri): String? {
  return try {
    val dir = File(context.filesDir, "shop_photos").apply { mkdirs() }
    val dest = File(dir, "shop_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(sourceUri)?.use { input ->
      dest.outputStream().use { output ->
        input.copyTo(output)
      }
    }
    dest.absolutePath
  } catch (_: Exception) {
    null
  }
}

fun isNetworkAvailable(context: Context): Boolean {
  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
  val network = cm.activeNetwork ?: return false
  val caps = cm.getNetworkCapabilities(network) ?: return false
  return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun openExternalMapOrUrl(context: Context, url: String, lat: Double = 0.0, lng: Double = 0.0) {
  val targetUri = when {
    url.trim().startsWith("http://") || url.trim().startsWith("https://") || url.trim().startsWith("geo:") -> {
      Uri.parse(url.trim())
    }
    lat != 0.0 || lng != 0.0 -> {
      Uri.parse("https://maps.google.com/?q=$lat,$lng")
    }
    else -> null
  }
  if (targetUri == null) {
    Toast.makeText(context, "Enter a Google Maps link or capture GPS first", Toast.LENGTH_SHORT).show()
    return
  }
  val intent = Intent(Intent.ACTION_VIEW, targetUri)
  try {
    context.startActivity(intent)
  } catch (_: Exception) {
    Toast.makeText(context, "No maps or browser app available", Toast.LENGTH_SHORT).show()
  }
}

fun createCameraPhotoFile(context: Context): File {
  val dir = File(context.filesDir, "shop_photos").apply { mkdirs() }
  return File(dir, "shop_cam_${System.currentTimeMillis()}.jpg")
}

@Composable
fun SnackRouteBottomBar(
  currentDestination: AppDestination,
  isMoreExpanded: Boolean,
  onToggleMore: () -> Unit,
  onSelectDestination: (AppDestination) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    // Expansion menu revealing the remaining 4 navigation options
    AnimatedVisibility(
      visible = isMoreExpanded,
      enter = fadeIn() + slideInVertically { it / 2 },
      exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
      Surface(
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "More Modules (4)",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .clickable(onClick = onToggleMore),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close more menu",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            AppDestination.expandedMenuItems.forEach { item ->
              val selected = currentDestination == item
              Surface(
                color = if (selected) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .weight(1f)
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onSelectDestination(item) }
                  .testTag(item.testTag),
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center,
                  modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                ) {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.title,
                    tint = if (selected) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(22.dp),
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) {
                      MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                      MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Bottom Navigation Bar: 5 primary buttons + 3-dot Expansion Menu button = 9 total modules
    Surface(
      tonalElevation = 0.dp,
      shadowElevation = 8.dp,
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.navigationBarsPadding()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AppDestination.bottomBarPrimary.forEach { item ->
            val selected = currentDestination == item && !isMoreExpanded
            BottomNavSlot(
              label = item.title,
              icon = if (selected) item.selectedIcon else item.unselectedIcon,
              selected = selected,
              onClick = { onSelectDestination(item) },
              modifier = Modifier
                .weight(1f)
                .testTag(item.testTag),
            )
          }

          val isExpandedItemActive = !currentDestination.isPrimaryBottomBar
          BottomNavSlot(
            label = "More",
            icon = if (isMoreExpanded || isExpandedItemActive) Icons.Rounded.MoreVert else Icons.Outlined.MoreVert,
            selected = isMoreExpanded || isExpandedItemActive,
            badgeDot = isExpandedItemActive,
            onClick = onToggleMore,
            modifier = Modifier
              .weight(0.85f)
              .testTag("more_menu_button"),
          )
        }
      }
    }
  }
}

@Composable
private fun BottomNavSlot(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  badgeDot: Boolean = false,
) {
  Column(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(14.dp))
        .background(
          if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
        .padding(horizontal = 10.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (selected) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.size(21.dp),
      )
      if (badgeDot) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .size(7.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
        )
      }
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = if (selected) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      },
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
