package com.example.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.Supplier
import com.example.data.model.SupplierCatalogHelper
import com.example.data.model.SupplierStatus
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
fun AddEditSupplierScreen(
  existingSupplier: Supplier?,
  suggestedSupplierCode: String,
  addedByName: String = "Field Officer",
  onSaveSupplier: (supplier: Supplier, isOnline: Boolean) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isOnline = remember { isNetworkAvailable(context) }
  val snackbarHostState = remember { SnackbarHostState() }

  var supplierCode by remember { mutableStateOf(existingSupplier?.supplierCode ?: suggestedSupplierCode) }
  var isSupplierCodeEditable by remember { mutableStateOf(false) }
  var supplierName by remember { mutableStateOf(existingSupplier?.name ?: "") }
  var category by remember {
    mutableStateOf(
      existingSupplier?.category?.ifBlank { SupplierCatalogHelper.defaultCategories.first() }
        ?: SupplierCatalogHelper.defaultCategories.first()
    )
  }
  var gstin by remember { mutableStateOf(existingSupplier?.gstin ?: "") }
  var kycVerified by remember { mutableStateOf(existingSupplier?.kycVerified ?: true) }
  var creditTerms by remember { mutableStateOf(existingSupplier?.creditTerms ?: "Net 15 Days") }
  var photoPath by remember { mutableStateOf(existingSupplier?.photoUri ?: "") }

  var contactPerson by remember { mutableStateOf(existingSupplier?.contactPerson ?: "") }
  var contactRole by remember { mutableStateOf(existingSupplier?.contactRole ?: "") }
  var altContactPerson by remember { mutableStateOf(existingSupplier?.altContactPerson ?: "") }
  var phone by remember { mutableStateOf(existingSupplier?.phone ?: "") }
  var altPhone by remember { mutableStateOf(existingSupplier?.altPhone ?: "") }
  var email by remember { mutableStateOf(existingSupplier?.email ?: "") }

  var address by remember { mutableStateOf(existingSupplier?.address ?: "") }
  var pinCode by remember { mutableStateOf(existingSupplier?.pinCode ?: "") }
  var mapsLink by remember { mutableStateOf(existingSupplier?.mapsLink ?: "") }
  var latitude by remember { mutableStateOf(existingSupplier?.latitude ?: 0.0) }
  var longitude by remember { mutableStateOf(existingSupplier?.longitude ?: 0.0) }
  var fetchingGps by remember { mutableStateOf(false) }
  var gpsStatusText by remember {
    mutableStateOf(
      if (existingSupplier != null && (existingSupplier.latitude != 0.0 || existingSupplier.longitude != 0.0)) {
        String.format(Locale.US, "Location Locked (%.4f, %.4f)", existingSupplier.latitude, existingSupplier.longitude)
      } else {
        ""
      }
    )
  }

  var confirmedPriceNotes by remember { mutableStateOf(existingSupplier?.confirmedPriceNotes ?: "") }
  var notes by remember { mutableStateOf(existingSupplier?.notes ?: "") }
  var showNotesSheet by remember { mutableStateOf(false) }
  var draftNote by remember { mutableStateOf(notes) }
  var showDiscardDialog by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val hasUnsavedEdits = remember(
    supplierName,
    contactPerson,
    phone,
    email,
    address,
    pinCode,
    gstin,
    confirmedPriceNotes,
    notes,
    photoPath,
    existingSupplier,
  ) {
    if (existingSupplier != null) {
      supplierName != existingSupplier.name ||
        contactPerson != existingSupplier.contactPerson ||
        phone != existingSupplier.phone ||
        address != existingSupplier.address ||
        notes != existingSupplier.notes
    } else {
      supplierName.isNotBlank() ||
        contactPerson.isNotBlank() ||
        phone.isNotBlank() ||
        address.isNotBlank() ||
        notes.isNotBlank()
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

  val dateTimeAddedDisplay = remember(existingSupplier) {
    val date = existingSupplier?.createdAt?.toDate() ?: Date()
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(date)
  }

  // Camera & Zero-permission Android Photo Picker
  var pendingCameraFile by remember { mutableStateOf<File?>(null) }
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success ->
    val file = pendingCameraFile
    if (success && file != null && file.exists() && file.length() > 0L) {
      photoPath = file.absolutePath
      scope.launch { snackbarHostState.showSnackbar("Supplier facility photo captured") }
    } else {
      file?.delete()
    }
    pendingCameraFile = null
  }
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      val savedPath = copyPickedImageToInternalStorage(context, uri)
      if (savedPath != null) {
        photoPath = savedPath
        scope.launch { snackbarHostState.showSnackbar("Supplier image uploaded") }
      }
    }
  }

  val launchCameraCapture: () -> Unit = {
    try {
      val photoFile = createCameraPhotoFile(context)
      val authority = "${context.packageName}.fileprovider"
      val uri = FileProvider.getUriForFile(context, authority, photoFile)
      pendingCameraFile = photoFile
      cameraLauncher.launch(uri)
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(context, "Camera app not available. Use Gallery instead.", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
      Toast.makeText(context, "Could not launch camera. Try Gallery.", Toast.LENGTH_SHORT).show()
    }
  }

  val launchImageUpload: () -> Unit = {
    photoPickerLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )
  }

  val captureLocationNow: () -> Unit = {
    fetchingGps = true
    fetchCurrentDeviceLocation(
      context = context,
      highAccuracy = true,
      onSuccess = { lat, lng, msg ->
        fetchingGps = false
        latitude = lat
        longitude = lng
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
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      captureLocationNow()
    } else {
      gpsStatusText = "Location permission denied. Allow location access to lock GPS."
    }
  }

  val triggerGpsCapture: () -> Unit = {
    if (hasLocationPermission(context)) {
      captureLocationNow()
    } else {
      locationPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION,
        )
      )
    }
  }

  val submitSupplierForm: () -> Unit = {
    val inferredPin = pinCode.trim().ifBlank {
      Regex("\\b\\d{6}\\b").find(address)?.value ?: ""
    }
    when {
      supplierCode.trim().length < 2 -> validationError = "Enter a valid Supplier ID"
      supplierName.trim().isEmpty() -> validationError = "Supplier / Company Name is required"
      contactPerson.trim().isEmpty() -> validationError = "Primary Contact Person is required"
      phone.trim().length < 5 -> validationError = "Valid Primary Mobile Number is required"
      address.trim().isEmpty() -> validationError = "Business Address is required"
      inferredPin.length < 3 -> validationError = "Enter a valid PIN Code"
      else -> {
        validationError = null
        val savedSupplier = (existingSupplier ?: Supplier()).copy(
          supplierCode = supplierCode.trim().take(32),
          name = supplierName.trim().take(140),
          category = category.trim().ifBlank { "Wholesale Staples & Grains" }.take(80),
          contactPerson = contactPerson.trim().take(100),
          contactRole = contactRole.trim().take(64),
          altContactPerson = altContactPerson.trim().take(100),
          phone = phone.trim().take(24),
          altPhone = altPhone.trim().take(24),
          email = email.trim().take(120),
          address = address.trim().take(300),
          pinCode = inferredPin.take(16),
          mapsLink = mapsLink.trim().take(600),
          latitude = latitude,
          longitude = longitude,
          photoUri = photoPath.take(600),
          gstin = gstin.trim().take(32),
          kycVerified = kycVerified,
          creditTerms = creditTerms.trim().take(64),
          confirmedPriceNotes = confirmedPriceNotes.trim().take(600),
          notes = notes.trim().take(600),
          status = existingSupplier?.status ?: SupplierStatus.ACTIVE,
          addedBy = (existingSupplier?.addedBy?.ifBlank { addedByName } ?: addedByName).take(120),
        )
        onSaveSupplier(savedSupplier, isOnline)
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
            IconButton(
              onClick = handleBackNavigation,
              modifier = Modifier.testTag("back_supplier_form_button"),
            ) {
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
                    imageVector = Icons.Rounded.Factory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              Text(
                text = if (existingSupplier == null) "Register Supplier" else "Edit ${existingSupplier.supplierCode}",
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
              .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                .testTag("cancel_supplier_button"),
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
              onClick = submitSupplierForm,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
              ),
              modifier = Modifier
                .weight(2f)
                .height(48.dp)
                .testTag("save_supplier_button"),
            ) {
              Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = when {
                  !isOnline -> "Save Offline"
                  existingSupplier == null -> "Register Supplier"
                  else -> "Update Supplier"
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
        .background(MaterialTheme.colorScheme.background)
        .testTag("add_edit_supplier_list"),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      // Sub-header strip: Step 1 of 1 + Add Details + Notes quick button
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
              Text(
                text = "Step 1 of 1",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = if (existingSupplier == null) "Add Details" else "Edit Profile",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                draftNote = notes
                showNotesSheet = true
              }
              .testTag("open_supplier_notes_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.StickyNote2,
                contentDescription = "Open supplier notes",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = "Notes (${if (notes.isBlank()) "0" else "1"})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
              )
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
                text = "No internet detected. Supplier details will be saved offline and synced automatically when online.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.semanticColors.warning,
              )
            }
          }
        }
      }

      item {
        SupplierInfoSectionCard(
          supplierCode = supplierCode,
          isSupplierCodeEditable = isSupplierCodeEditable,
          onSupplierCodeChange = { supplierCode = it },
          onToggleSupplierCodeEditable = { isSupplierCodeEditable = !isSupplierCodeEditable },
          supplierName = supplierName,
          onSupplierNameChange = { supplierName = it },
          category = category,
          onCategoryChange = { category = it },
          gstin = gstin,
          onGstinChange = { gstin = it },
          kycVerified = kycVerified,
          onKycVerifiedChange = { kycVerified = it },
          creditTerms = creditTerms,
          onCreditTermsChange = { creditTerms = it },
          photoPath = photoPath,
          onTakePhoto = launchCameraCapture,
          onUploadImage = launchImageUpload,
          onRemovePhoto = {
            photoPath = ""
            scope.launch { snackbarHostState.showSnackbar("Supplier photo removed") }
          },
        )
      }

      item {
        SupplierContactSectionCard(
          contactPerson = contactPerson,
          onContactPersonChange = { contactPerson = it },
          contactRole = contactRole,
          onContactRoleChange = { contactRole = it },
          altContactPerson = altContactPerson,
          onAltContactPersonChange = { altContactPerson = it },
          phone = phone,
          onPhoneChange = { phone = it },
          altPhone = altPhone,
          onAltPhoneChange = { altPhone = it },
          email = email,
          onEmailChange = { email = it },
        )
      }

      item {
        SupplierLocationSectionCard(
          fetchingGps = fetchingGps,
          latitude = latitude,
          longitude = longitude,
          gpsStatusText = gpsStatusText,
          onCaptureGps = triggerGpsCapture,
          address = address,
          onAddressChange = { updated ->
            address = updated
            if (pinCode.isBlank()) {
              Regex("\\b\\d{6}\\b").find(updated)?.value?.let { detected ->
                pinCode = detected
              }
            }
          },
          pinCode = pinCode,
          onPinCodeChange = { pinCode = it },
          mapsLink = mapsLink,
          onMapsLinkChange = { mapsLink = it },
          onOpenMaps = { openExternalMapOrUrl(context, mapsLink, latitude, longitude) },
        )
      }

      item {
        SupplierRatesAndAuditSections(
          confirmedPriceNotes = confirmedPriceNotes,
          onConfirmedPriceNotesChange = { confirmedPriceNotes = it },
          dateTimeAdded = dateTimeAddedDisplay,
          addedByName = existingSupplier?.addedBy?.ifBlank { addedByName } ?: addedByName,
          isOnline = isOnline,
        )
      }

      if (validationError != null) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("supplier_validation_error"),
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

  // Procurement Notes ModalBottomSheet (scrollable body + pinned CTA row)
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
          .navigationBarsPadding(),
      ) {
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
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
                  text = "Procurement Notes",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "Lead times, MOQ, dispatch schedules & quality notes",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            IconButton(onClick = { showNotesSheet = false }) {
              Icon(Icons.Rounded.Close, contentDescription = "Close notes")
            }
          }

          OutlinedTextField(
            value = draftNote,
            onValueChange = { draftNote = it },
            placeholder = { Text("Enter supply lead times, MOQ, loading dock instructions...") },
            minLines = 4,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              focusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("supplier_note_input"),
          )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
              notes = draftNote.trim()
              showNotesSheet = false
              scope.launch { snackbarHostState.showSnackbar("Notes updated for $supplierCode") }
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
      title = { Text("Discard unsaved supplier details?") },
      text = { Text("You have entered supplier details that have not been saved yet.") },
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
