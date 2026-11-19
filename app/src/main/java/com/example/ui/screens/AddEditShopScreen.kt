package com.example.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.DeliveryRoute
import com.example.data.model.RoutesCatalog
import com.example.data.model.Shop
import com.example.data.model.SnackCatalog
import com.example.ui.components.copyPickedImageToInternalStorage
import com.example.ui.components.createCameraPhotoFile
import com.example.ui.components.fetchCurrentDeviceLocation
import com.example.ui.components.hasLocationPermission
import com.example.ui.components.isNetworkAvailable
import com.example.ui.components.openExternalMapOrUrl
import com.example.ui.theme.semanticColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditShopScreen(
  existingShop: Shop?,
  suggestedShopCode: String,
  addedByName: String = "Field Officer",
  availableRoutes: List<DeliveryRoute> = RoutesCatalog.starterRoutes,
  isSaving: Boolean = false,
  onSaveShop: (shop: Shop, isOnline: Boolean) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isOnline = remember { isNetworkAvailable(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  var shopCode by remember { mutableStateOf(existingShop?.shopCode ?: suggestedShopCode) }
  var isShopCodeEditable by remember { mutableStateOf(false) }
  var shopName by remember { mutableStateOf(existingShop?.name ?: "") }
  var ownerName by remember { mutableStateOf(existingShop?.ownerName ?: "") }
  var phone by remember { mutableStateOf(existingShop?.phone ?: "") }
  var secondaryPhone by remember { mutableStateOf(existingShop?.secondaryPhone ?: "") }
  var address by remember { mutableStateOf(existingShop?.address ?: "") }
  var pinCode by remember { mutableStateOf(existingShop?.pinCode ?: "") }
  var selectedRoute by remember { mutableStateOf(existingShop?.routeId ?: RoutesCatalog.R01_KANNUR_BANGALORE) }
  var mapsLink by remember { mutableStateOf(existingShop?.mapsLink ?: "") }
  var note by remember { mutableStateOf(existingShop?.note ?: "") }
  var photoPath by remember { mutableStateOf(existingShop?.photoUri ?: "") }
  var latitude by remember { mutableStateOf(existingShop?.latitude ?: 0.0) }
  var longitude by remember { mutableStateOf(existingShop?.longitude ?: 0.0) }
  var liveUserLatitude by remember { mutableStateOf<Double?>(null) }
  var liveUserLongitude by remember { mutableStateOf<Double?>(null) }
  var gpsStatusText by remember {
    mutableStateOf(
      if (existingShop != null && (existingShop.latitude != 0.0 || existingShop.longitude != 0.0)) {
        String.format(Locale.US, "Location Locked (%.4f, %.4f)", existingShop.latitude, existingShop.longitude)
      } else {
        ""
      }
    )
  }
  var fetchingGps by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (hasLocationPermission(context)) {
      fetchCurrentDeviceLocation(
        context = context,
        highAccuracy = false,
        onSuccess = { lat, lng, _ ->
          liveUserLatitude = lat
          liveUserLongitude = lng
        },
        onError = {},
      )
    }
  }

  var showNotesSheet by remember { mutableStateOf(false) }
  var draftNote by remember { mutableStateOf(note) }
  var showDiscardDialog by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  var hasConfirmedPricing by remember {
    mutableStateOf(!existingShop?.confirmedPriceNotes.isNullOrBlank())
  }
  val defaultProduct = remember { SnackCatalog.products.first() }
  val defaultValidity = remember { formatValidityRangeFromToday(30) }
  val productRateRows = remember {
    mutableStateListOf<ConfirmedProductRate>().apply {
      val parsed = parseConfirmedRates(existingShop?.confirmedPriceNotes.orEmpty(), defaultValidity)
      if (parsed.isNotEmpty()) {
        addAll(parsed)
      } else {
        add(
          ConfirmedProductRate(
            productCode = defaultProduct.code,
            productName = defaultProduct.name,
            packSize = defaultProduct.packSize,
            confirmedPriceText = String.format(Locale.US, "%.2f", defaultProduct.defaultPrice),
            validityRange = defaultValidity,
            activeDays = 30,
          )
        )
      }
    }
  }

  val hasUnsavedEdits = remember(
    shopName, ownerName, phone, secondaryPhone, address, pinCode, mapsLink, note, photoPath, existingShop
  ) {
    if (existingShop != null) {
      shopName != existingShop.name ||
        ownerName != existingShop.ownerName ||
        phone != existingShop.phone ||
        address != existingShop.address ||
        note != existingShop.note ||
        photoPath != existingShop.photoUri
    } else {
      shopName.isNotBlank() || ownerName.isNotBlank() || phone.isNotBlank() ||
        address.isNotBlank() || note.isNotBlank() || photoPath.isNotBlank()
    }
  }

  val handleBackNavigation: () -> Unit = {
    if (hasUnsavedEdits) {
      showDiscardDialog = true
    } else {
      onBack()
    }
  }
  BackHandler(onBack = handleBackNavigation)

  val dateTimeAddedDisplay = remember(existingShop) {
    val date = existingShop?.createdAt?.toDate() ?: Date()
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(date)
  }

  // Pending camera output file reference
  var pendingCameraFile by remember { mutableStateOf<File?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success ->
    val file = pendingCameraFile
    if (success && file != null && file.exists() && file.length() > 0L) {
      photoPath = file.absolutePath
      scope.launch { snackbarHostState.showSnackbar("Shop frontage photo captured") }
    } else {
      file?.delete()
    }
    pendingCameraFile = null
    pendingCameraUri = null
  }

  // Zero-permission Android Photo Picker (Google Play compliant)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      val savedPath = copyPickedImageToInternalStorage(context, uri)
      if (savedPath != null) {
        photoPath = savedPath
        scope.launch { snackbarHostState.showSnackbar("Shop photo uploaded") }
      }
    }
  }

  val launchCameraCapture: () -> Unit = {
    try {
      val photoFile = createCameraPhotoFile(context)
      val authority = "${context.packageName}.fileprovider"
      val uri = FileProvider.getUriForFile(context, authority, photoFile)
      pendingCameraFile = photoFile
      pendingCameraUri = uri
      cameraLauncher.launch(uri)
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(context, "Camera app not available. Use Upload Image instead.", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
      Toast.makeText(context, "Could not launch camera. Try Upload Image.", Toast.LENGTH_SHORT).show()
    }
  }

  val launchImageUpload: () -> Unit = {
    photoPickerLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      fetchingGps = true
      fetchCurrentDeviceLocation(
        context = context,
        highAccuracy = true,
        onSuccess = { lat, lng, msg ->
          fetchingGps = false
          latitude = lat
          longitude = lng
          liveUserLatitude = lat
          liveUserLongitude = lng
          gpsStatusText = msg
          if (mapsLink.isBlank()) {
            mapsLink = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", lat, lng)
          }
        },
        onError = { msg ->
          fetchingGps = false
          gpsStatusText = msg
        },
      )
    } else {
      gpsStatusText = "Location permission denied. Allow location access to lock GPS."
    }
  }

  val triggerGpsCapture: () -> Unit = {
    if (hasLocationPermission(context)) {
      fetchingGps = true
      fetchCurrentDeviceLocation(
        context = context,
        highAccuracy = true,
        onSuccess = { lat, lng, msg ->
          fetchingGps = false
          latitude = lat
          longitude = lng
          liveUserLatitude = lat
          liveUserLongitude = lng
          gpsStatusText = msg
          if (mapsLink.isBlank()) {
            mapsLink = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f", lat, lng)
          }
        },
        onError = { msg ->
          fetchingGps = false
          gpsStatusText = msg
        },
      )
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION,
        )
      )
    }
  }

  val submitShopForm: () -> Unit = {
    if (!isSaving) {
      val inferredPin = pinCode.trim().ifBlank {
        Regex("\\b\\d{6}\\b").find(address)?.value ?: ""
      }
    when {
      shopCode.trim().length < 2 -> validationError = "Enter a valid Unique Shop ID"
      shopName.trim().isEmpty() -> validationError = "Shop Name is required"
      ownerName.trim().isEmpty() -> validationError = "Owner Name is required"
      phone.trim().length < 5 -> validationError = "Valid Primary Mobile Number is required"
      address.trim().isEmpty() -> validationError = "Shop Location / Address is required"
      inferredPin.length < 3 -> validationError = "Enter a valid Pin Code (or include 6-digit PIN in address)"
      else -> {
        validationError = null
        val priceSummary = if (hasConfirmedPricing && productRateRows.isNotEmpty()) {
          productRateRows
            .filter { it.confirmedPriceText.isNotBlank() }
            .joinToString(", ") { row ->
              "${row.productCode} (${row.packSize}): ₹${row.confirmedPriceText} [${row.validityRange}]"
            }
            .take(595)
        } else {
          ""
        }

        val newShop = (existingShop ?: Shop()).copy(
          shopCode = shopCode.trim(),
          name = shopName.trim().take(120),
          ownerName = ownerName.trim().take(100),
          phone = phone.trim().take(24),
          secondaryPhone = secondaryPhone.trim().take(24),
          address = address.trim().take(300),
          pinCode = inferredPin.take(16),
          routeId = selectedRoute,
          photoUri = photoPath,
          latitude = latitude,
          longitude = longitude,
          note = note.trim().take(500),
          confirmedPriceNotes = priceSummary,
          mapsLink = mapsLink.trim().take(600),
          addedBy = (existingShop?.addedBy?.ifBlank { addedByName } ?: addedByName).take(120),
        )
        onSaveShop(newShop, isOnline)
      }
    }
    }
  }

  val userInitials = remember(addedByName) {
    addedByName.trim()
      .split(" ")
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifBlank { "FO" }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          navigationIcon = {
            IconButton(onClick = handleBackNavigation) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Go back")
            }
          },
          title = {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(30.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              Text(
                text = if (existingShop == null) "Register Shop" else "Edit ${existingShop.shopCode}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
            }
          },
          actions = {
            Surface(
              color = MaterialTheme.colorScheme.secondary,
              shape = CircleShape,
              modifier = Modifier
                .padding(end = 12.dp)
                .size(34.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = userInitials,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondary,
                )
              }
            }
          },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      }
    },
    bottomBar = {
      Surface(
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
          Row(
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable(onClick = handleBackNavigation)
              .testTag("cancel_shop_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          Button(
            onClick = submitShopForm,
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.secondary,
              contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            modifier = Modifier
              .weight(2f)
              .height(48.dp)
              .testTag("save_shop_button"),
          ) {
            if (isSaving) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSecondary,
              )
            } else {
              Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = when {
                isSaving -> "Saving..."
                !isOnline -> "Save Offline"
                existingShop == null -> "Register Shop"
                else -> "Update Shop"
              },
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }
        }
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Sub-header: Status pill + Step 1 of 1 + Notes quick trigger
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                )
                Text(
                  text = if (existingShop == null) "New Registration" else "Edit Registration",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.secondary,
                )
              }
            }
            Text(
              text = "Step 1 of 1",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary,
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                draftNote = note
                showNotesSheet = true
              }
              .testTag("open_notes_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.StickyNote2,
                contentDescription = "Open shop notes",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = "Notes",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
              )
              Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
              ) {
                Text(
                  text = if (note.isBlank()) "0" else "1",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
              }
            }
          }
        }
      }

      if (!isOnline) {
        item {
          Surface(
            color = MaterialTheme.semanticColors.warningContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.semanticColors.warning)
              Text(
                text = "No internet detected. Shop details will be saved locally on your phone and synced automatically when online.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.semanticColors.warning,
              )
            }
          }
        }
      }

      // SECTION A: Shop Information
      item {
        ShopInfoSectionCard(
          shopCode = shopCode,
          isShopCodeEditable = isShopCodeEditable,
          onShopCodeChange = { shopCode = it },
          onToggleShopCodeEditable = { isShopCodeEditable = !isShopCodeEditable },
          shopName = shopName,
          onShopNameChange = { shopName = it },
          photoPath = photoPath,
          onTakePhoto = launchCameraCapture,
          onUploadImage = launchImageUpload,
          onRemovePhoto = {
            photoPath = ""
            scope.launch { snackbarHostState.showSnackbar("Shop photo removed") }
          },
        )
      }

      // SECTION B: Owner / Contact Information
      item {
        OwnerContactSectionCard(
          ownerName = ownerName,
          onOwnerNameChange = { ownerName = it },
          phone = phone,
          onPhoneChange = { phone = it },
          secondaryPhone = secondaryPhone,
          onSecondaryPhoneChange = { secondaryPhone = it },
        )
      }

      // SECTION C: Shop Location
      item {
        ShopLocationSectionCard(
          fetchingGps = fetchingGps,
          latitude = latitude,
          longitude = longitude,
          liveUserLatitude = liveUserLatitude,
          liveUserLongitude = liveUserLongitude,
          gpsStatusText = gpsStatusText,
          onCaptureGps = triggerGpsCapture,
          address = address,
          onAddressChange = { updated ->
            address = updated
            if (pinCode.isBlank()) {
              val detectedPin = Regex("\\b\\d{6}\\b").find(updated)?.value
              if (detectedPin != null) {
                pinCode = detectedPin
              }
            }
          },
          pinCode = pinCode,
          onPinCodeChange = { pinCode = it },
          selectedRoute = selectedRoute,
          availableRoutes = availableRoutes,
          onRouteSelect = { selectedRoute = it },
          mapsLink = mapsLink,
          onMapsLinkChange = { mapsLink = it },
          onOpenMaps = { openExternalMapOrUrl(context, mapsLink, latitude, longitude) },
        )
      }

      // SECTION D: Confirmed Product Pricing
      item {
        ConfirmedPricingSectionCard(
          enabled = hasConfirmedPricing,
          onToggleEnabled = { hasConfirmedPricing = it },
          rateRows = productRateRows,
          onUpdateRow = { idx, updated ->
            if (idx in productRateRows.indices) {
              productRateRows[idx] = updated
            }
          },
          onAddRow = {
            val nextProduct = SnackCatalog.products[productRateRows.size % SnackCatalog.products.size]
            productRateRows.add(
              ConfirmedProductRate(
                productCode = nextProduct.code,
                productName = nextProduct.name,
                packSize = nextProduct.packSize,
                confirmedPriceText = String.format(Locale.US, "%.2f", nextProduct.defaultPrice),
                validityRange = formatValidityRangeFromToday(30),
                activeDays = 30,
              )
            )
          },
          onRemoveRow = { idx ->
            if (idx in productRateRows.indices && productRateRows.size > 1) {
              productRateRows.removeAt(idx)
            }
          },
        )
      }

      // Registration Information (System Metadata)
      item {
        RegistrationMetadataCard(
          dateTimeAdded = dateTimeAddedDisplay,
          addedByName = existingShop?.addedBy?.ifBlank { addedByName } ?: addedByName,
          isOnline = isOnline,
        )
      }

      if (validationError != null) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = validationError!!,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }

  // Interactive Shop Dispatch Notes ModalBottomSheet
  if (showNotesSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showNotesSheet = false },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = CircleShape,
              modifier = Modifier.size(38.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Rounded.StickyNote2,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                )
              }
            }
            Column {
              Text(
                text = "Shop Dispatch Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "Field notes for delivery routing & fulfillment",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          IconButton(onClick = { showNotesSheet = false }) {
            Icon(Icons.Rounded.Close, contentDescription = "Close notes")
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Special Operational Instructions",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary,
          )
          OutlinedTextField(
            value = draftNote,
            onValueChange = { draftNote = it },
            placeholder = {
              Text("Preferred delivery timing, vehicle entry width, gate passes, credit terms...")
            },
            minLines = 4,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("shop_note_input"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .clickable { showNotesSheet = false },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
          }

          Button(
            onClick = {
              note = draftNote.trim()
              showNotesSheet = false
              scope.launch {
                snackbarHostState.showSnackbar("Notes updated for $shopCode")
              }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.secondary,
              contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            modifier = Modifier
              .weight(1f)
              .height(48.dp),
          ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Notes")
          }
        }
      }
    }
  }

  if (showDiscardDialog) {
    AlertDialog(
      onDismissRequest = { showDiscardDialog = false },
      title = { Text("Discard unsaved shop details?") },
      text = { Text("You have entered shop registration details that are not saved yet.") },
      confirmButton = {
        TextButton(
          onClick = {
            showDiscardDialog = false
            onBack()
          },
        ) {
          Text("Discard")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDiscardDialog = false }) {
          Text("Keep Editing")
        }
      },
    )
  }
}

private fun parseConfirmedRates(
  rawNotes: String,
  defaultValidity: String,
): List<ConfirmedProductRate> {
  if (rawNotes.isBlank()) return emptyList()
  return rawNotes.split(",")
    .mapNotNull { chunk ->
      val trimmed = chunk.trim()
      if (trimmed.isEmpty()) return@mapNotNull null
      val colonIdx = trimmed.indexOf(":")
      if (colonIdx <= 0) return@mapNotNull null

      val left = trimmed.substring(0, colonIdx).trim()
      val right = trimmed.substring(colonIdx + 1).trim()
      val code = left.substringBefore(" ").trim()
      val matchedProduct = SnackCatalog.products.firstOrNull { it.code == code }
        ?: SnackCatalog.products.first()

      val validity = if (right.contains("[") && right.contains("]")) {
        right.substringAfter("[").substringBefore("]").trim()
      } else {
        defaultValidity
      }
      val pricePart = right.substringBefore("[").replace("₹", "").trim()
        .ifBlank { String.format(Locale.US, "%.2f", matchedProduct.defaultPrice) }

      ConfirmedProductRate(
        productCode = matchedProduct.code,
        productName = matchedProduct.name,
        packSize = matchedProduct.packSize,
        confirmedPriceText = pricePart,
        validityRange = validity,
        activeDays = 30,
      )
    }
}
