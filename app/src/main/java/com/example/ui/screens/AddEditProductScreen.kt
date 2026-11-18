package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.Product
import com.example.data.model.ProductCatalogHelper
import com.example.data.model.ProductStatus
import com.example.data.model.ProductVariant
import com.example.ui.components.copyPickedImageToInternalStorage
import com.example.ui.components.createCameraPhotoFile
import com.example.ui.components.isNetworkAvailable
import com.example.ui.theme.semanticColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
  existingProduct: Product?,
  suggestedProductCode: String,
  availableCategories: List<String> = ProductCatalogHelper.defaultCategories,
  onSaveProduct: (Product, isOnline: Boolean) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val isEditing = existingProduct != null

  val productCode = remember(existingProduct, suggestedProductCode) {
    existingProduct?.productCode?.ifBlank { suggestedProductCode } ?: suggestedProductCode
  }
  val codeSuffix = remember(productCode) { productCode.takeLast(3).ifBlank { "001" } }

  var name by remember(existingProduct) { mutableStateOf(existingProduct?.name ?: "") }
  var sku by remember(existingProduct) {
    mutableStateOf(existingProduct?.sku?.ifBlank { "SKU-PRD-$codeSuffix" } ?: "SKU-PRD-$codeSuffix")
  }
  var barcode by remember(existingProduct) { mutableStateOf(existingProduct?.barcode ?: "") }
  var category by remember(existingProduct) {
    mutableStateOf(existingProduct?.category?.ifBlank { "Biscuits & Snacks" } ?: "Biscuits & Snacks")
  }
  var brand by remember(existingProduct) { mutableStateOf(existingProduct?.brand ?: "") }
  var photoPath by remember(existingProduct) { mutableStateOf(existingProduct?.photoUri ?: "") }
  var notes by remember(existingProduct) { mutableStateOf(existingProduct?.notes ?: "") }
  var isActiveCatalog by remember(existingProduct) {
    mutableStateOf(existingProduct?.status != ProductStatus.INACTIVE)
  }

  var baseUnit by remember(existingProduct) {
    mutableStateOf(existingProduct?.baseUnit?.ifBlank { "Gram (g)" } ?: "Gram (g)")
  }
  var sellingUnit by remember(existingProduct) {
    mutableStateOf(existingProduct?.sellingUnit?.ifBlank { "Pouch / Packet" } ?: "Pouch / Packet")
  }
  var casePack by remember(existingProduct) { mutableStateOf(existingProduct?.casePack ?: "") }
  var stockQtyText by remember(existingProduct) {
    mutableStateOf((existingProduct?.stockQuantity ?: 50).toString())
  }
  var minThresholdText by remember(existingProduct) {
    mutableStateOf((existingProduct?.minThreshold ?: 30).toString())
  }

  var hasVariants by remember(existingProduct) {
    mutableStateOf(existingProduct?.hasVariants ?: false)
  }
  val variants = remember(existingProduct) {
    mutableStateListOf<ProductVariant>().apply {
      if (existingProduct != null && existingProduct.hasVariants) {
        addAll(ProductCatalogHelper.decodeVariants(existingProduct.variantsJson))
      }
    }
  }

  var packSize by remember(existingProduct) { mutableStateOf(existingProduct?.packSize ?: "") }
  var wholesalePriceText by remember(existingProduct) {
    mutableStateOf(
      if (existingProduct != null && !existingProduct.hasVariants && existingProduct.wholesalePrice > 0.0) {
        existingProduct.wholesalePrice.toString()
      } else {
        ""
      }
    )
  }
  var mrpText by remember(existingProduct) {
    mutableStateOf(
      if (existingProduct != null && !existingProduct.hasVariants && existingProduct.mrp > 0.0) {
        existingProduct.mrp.toString()
      } else {
        ""
      }
    )
  }

  val customCategories = remember { mutableStateListOf<String>() }
  val allCategories = remember(availableCategories, customCategories.toList(), category) {
    (availableCategories + customCategories + category)
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
  }

  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var showVariantDialog by remember { mutableStateOf(false) }
  var editingVariantIndex by remember { mutableStateOf<Int?>(null) }
  var showDiscardDialog by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val hasUnsavedEdits = remember(
    existingProduct,
    name,
    sku,
    barcode,
    brand,
    photoPath,
    notes,
    casePack,
    packSize,
    wholesalePriceText,
    variants.size,
  ) {
    if (existingProduct != null) {
      name != existingProduct.name ||
        sku != existingProduct.sku ||
        barcode != existingProduct.barcode ||
        brand != existingProduct.brand ||
        photoPath != existingProduct.photoUri ||
        notes != existingProduct.notes
    } else {
      name.isNotBlank() ||
        barcode.isNotBlank() ||
        brand.isNotBlank() ||
        photoPath.isNotBlank() ||
        notes.isNotBlank() ||
        wholesalePriceText.isNotBlank() ||
        variants.isNotEmpty()
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

  // Camera & Zero-Permission Photo Picker launchers
  var pendingCameraFile by remember { mutableStateOf<File?>(null) }
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success ->
    val file = pendingCameraFile
    if (success && file != null && file.exists() && file.length() > 0L) {
      photoPath = file.absolutePath
      scope.launch { snackbarHostState.showSnackbar("Product photo captured") }
    } else {
      file?.delete()
    }
    pendingCameraFile = null
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri: Uri? ->
    if (uri != null) {
      val savedPath = copyPickedImageToInternalStorage(context, uri)
      if (savedPath != null) {
        photoPath = savedPath
        scope.launch { snackbarHostState.showSnackbar("Product image uploaded") }
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
      Toast.makeText(context, "Camera app unavailable. Use Upload Image instead.", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
      Toast.makeText(context, "Could not launch camera. Try Upload Image.", Toast.LENGTH_SHORT).show()
    }
  }

  val launchImageUpload: () -> Unit = {
    photoPickerLauncher.launch(
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )
  }

  val submitProduct: (isDraft: Boolean) -> Unit = { isDraft ->
    val cleanName = name.trim()
    val cleanSku = sku.trim()
    val stockQty = stockQtyText.toIntOrNull() ?: 0
    val minThresh = minThresholdText.toIntOrNull() ?: 30
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    if (cleanName.isBlank()) {
      validationError = "Please enter the Product Trade Name."
    } else if (cleanSku.isBlank()) {
      validationError = "Please enter an SKU / Internal Code."
    } else if (!isDraft && hasVariants && variants.isEmpty()) {
      validationError = "Add at least one variant SKU or disable multi-variant mode."
    } else if (!isDraft && !hasVariants && (wholesalePriceText.toDoubleOrNull() ?: 0.0) <= 0.0) {
      validationError = "Please enter a valid Wholesale Price greater than 0."
    } else {
      validationError = null
      val computedWholesale: Double
      val computedMrp: Double
      val computedPackSize: String
      val encodedVariants: String

      if (hasVariants && variants.isNotEmpty()) {
        computedWholesale = variants.minOf { it.wholesalePrice }
        computedMrp = variants.maxOf { it.mrp }
        computedPackSize = if (variants.size > 1) {
          "${variants.first().packSize} - ${variants.last().packSize}"
        } else {
          variants.first().packSize
        }
        encodedVariants = ProductCatalogHelper.encodeVariants(variants)
      } else {
        computedWholesale = wholesalePriceText.toDoubleOrNull() ?: 0.0
        computedMrp = mrpText.toDoubleOrNull() ?: 0.0
        computedPackSize = packSize.trim().ifBlank { baseUnit }
        encodedVariants = ""
      }

      val targetStatus = when {
        isDraft -> ProductStatus.DRAFT
        isActiveCatalog -> ProductStatus.ACTIVE
        else -> ProductStatus.INACTIVE
      }

      val model = Product(
        id = existingProduct?.id ?: "",
        userId = existingProduct?.userId ?: "",
        productCode = productCode,
        sku = cleanSku,
        barcode = barcode.trim(),
        name = cleanName,
        category = category.trim().ifBlank { "Biscuits & Snacks" },
        brand = brand.trim(),
        baseUnit = baseUnit,
        sellingUnit = sellingUnit,
        casePack = casePack.trim(),
        packSize = computedPackSize,
        wholesalePrice = computedWholesale,
        mrp = computedMrp,
        stockQuantity = stockQty.coerceAtLeast(0),
        minThreshold = minThresh.coerceAtLeast(1),
        photoUri = photoPath,
        notes = notes.trim(),
        status = targetStatus,
        salesVelocity = existingProduct?.salesVelocity ?: 50,
        hasVariants = hasVariants && variants.isNotEmpty(),
        variantsJson = encodedVariants,
        addedDate = existingProduct?.addedDate?.ifBlank { today } ?: today,
        createdAt = existingProduct?.createdAt,
        updatedAt = existingProduct?.updatedAt,
      )
      onSaveProduct(model, isNetworkAvailable(context))
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
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
              modifier = Modifier.testTag("add_product_back_button"),
            ) {
              Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
          },
          title = {
            Column {
              Text(
                text = if (isEditing) "Edit Product" else "Add Product",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Homely by Homie FMCG",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
              )
            }
          },
          actions = {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
              modifier = Modifier.padding(end = 12.dp),
            ) {
              Text(
                text = "[ $productCode Allocated ]",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              )
            }
          },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
      }
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = handleBackNavigation,
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
              modifier = Modifier.testTag("cancel_product_button"),
            ) {
              Text("Cancel", maxLines = 1)
            }
            FilledTonalButton(
              onClick = { submitProduct(true) },
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
              modifier = Modifier.testTag("save_draft_product_button"),
            ) {
              Text("Save Draft", maxLines = 1)
            }
            Button(
              onClick = { submitProduct(false) },
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("save_product_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isEditing) "Update Product" else "Save & Register",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
        .testTag("add_edit_product_list"),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      if (validationError != null) {
        item {
          Surface(
            color = MaterialTheme.semanticColors.dangerContainer,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.semanticColors.danger.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = validationError.orEmpty(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.semanticColors.danger,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }

      // Section A — Basic Product Information
      item(key = "section_a_basic") {
        ProductBasicInfoSectionCard(
          name = name,
          onNameChange = {
            name = it
            validationError = null
          },
          sku = sku,
          onSkuChange = {
            sku = it
            validationError = null
          },
          barcode = barcode,
          onBarcodeChange = { barcode = it },
          category = category,
          allCategories = allCategories,
          onCategorySelected = { category = it },
          onOpenAddCategory = { showAddCategoryDialog = true },
          brand = brand,
          onBrandChange = { brand = it },
          photoPath = photoPath,
          onTakePhoto = launchCameraCapture,
          onUploadImage = launchImageUpload,
          onRemovePhoto = { photoPath = "" },
          notes = notes,
          onNotesChange = { notes = it },
          isActiveCatalog = isActiveCatalog,
          onActiveCatalogChange = { isActiveCatalog = it },
        )
      }

      // Section B — Unit & Packaging Hierarchy (Core settings)
      item(key = "section_b_units") {
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
              icon = Icons.Rounded.Inventory2,
              title = "Section B — Unit & Packaging Hierarchy",
              subtitle = "Slab rates, case packing & customer wholesale variants",
              badgeLabel = "Weights & Slabs",
              badgeIsSecondary = true,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              ProductDropdownField(
                label = "Base Weight Unit",
                selectedValue = baseUnit,
                options = ProductCatalogHelper.baseUnits,
                onOptionSelected = { baseUnit = it },
                modifier = Modifier.weight(1f),
              )
              ProductDropdownField(
                label = "Selling Unit Type",
                selectedValue = sellingUnit,
                options = ProductCatalogHelper.sellingUnits,
                onOptionSelected = { sellingUnit = it },
                modifier = Modifier.weight(1f),
              )
            }

            OutlinedTextField(
              value = casePack,
              onValueChange = { casePack = it },
              label = { Text("Wholesale Shipper Case Pack") },
              placeholder = { Text("e.g. 24 packets per outer master carton") },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = stockQtyText,
                onValueChange = { next -> stockQtyText = next.filter { it.isDigit() } },
                label = { Text("Current Stock Qty") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_stock_input"),
              )
              OutlinedTextField(
                value = minThresholdText,
                onValueChange = { next -> minThresholdText = next.filter { it.isDigit() } },
                label = { Text("Min Reorder Alert") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_min_stock_input"),
              )
            }

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
                    text = "Multiple Pack Sizes / Variants",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "Sell across 50g, 100g, 200g & 500g slabs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = hasVariants,
                  onCheckedChange = { enabled ->
                    hasVariants = enabled
                    validationError = null
                  },
                  modifier = Modifier.testTag("toggle_variants_switch"),
                )
              }
            }
          }
        }
      }

      if (hasVariants) {
        item(key = "variant_header") {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Configured Variant SKUs (${variants.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              FilledTonalButton(
                onClick = {
                  editingVariantIndex = null
                  showVariantDialog = true
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_variant_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Variant SKU", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }

        if (showVariantDialog) {
          item(key = "variant_editor_card") {
            val idx = editingVariantIndex
            val existingVar = if (idx != null && idx in variants.indices) variants[idx] else null
            val nextSuggestedSku = "${sku.ifBlank { "SKU-PRD" }}-0${variants.size + 1}"
            VariantEditorCard(
              existing = existingVar,
              suggestedSku = nextSuggestedSku,
              onConfirm = { savedVariant ->
                if (idx != null && idx in variants.indices) {
                  variants[idx] = savedVariant
                } else {
                  variants.add(savedVariant)
                }
                validationError = null
                showVariantDialog = false
              },
              onDismiss = { showVariantDialog = false },
            )
          }
        }

        if (variants.isEmpty() && !showVariantDialog) {
          item(key = "variant_empty_prompt") {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  editingVariantIndex = null
                  showVariantDialog = true
                },
            ) {
              Text(
                text = "Tap + Add Variant SKU to configure pack sizes and wholesale rates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
              )
            }
          }
        } else {
          itemsIndexed(
            items = variants,
            key = { index, variant -> "${index}_${variant.sku}_${variant.packSize}" },
          ) { index, variant ->
            ProductVariantCard(
              variant = variant,
              index = index,
              onEdit = {
                editingVariantIndex = index
                showVariantDialog = true
              },
              onDelete = { variants.removeAt(index) },
            )
          }
        }
      } else {
        item(key = "single_pack_pricing") {
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
              OutlinedTextField(
                value = packSize,
                onValueChange = { packSize = it },
                label = { Text("Pack Size") },
                placeholder = { Text("e.g. 200g Pouch or 1L Bottle") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("product_pack_size_input"),
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedTextField(
                  value = wholesalePriceText,
                  onValueChange = { next ->
                    wholesalePriceText = next.filter { it.isDigit() || it == '.' }
                    validationError = null
                  },
                  label = { Text("Wholesale Price ₹ *") },
                  placeholder = { Text("e.g. 85.00") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("product_price_input"),
                )
                OutlinedTextField(
                  value = mrpText,
                  onValueChange = { next -> mrpText = next.filter { it.isDigit() || it == '.' } },
                  label = { Text("MRP ₹") },
                  placeholder = { Text("e.g. 105.00") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("product_mrp_input"),
                )
              }
              val singleW = wholesalePriceText.toDoubleOrNull() ?: 0.0
              val singleMrp = mrpText.toDoubleOrNull() ?: 0.0
              if (singleW > 0.0 && singleMrp > 0.0) {
                val margin = ProductCatalogHelper.computeMarginPercent(singleW, singleMrp)
                Surface(
                  color = MaterialTheme.semanticColors.successContainer,
                  shape = CircleShape,
                ) {
                  Text(
                    text = String.format(Locale.US, "Calculated Retailer Margin: %.1f%%", margin),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.semanticColors.success,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddCategoryDialog) {
    AddCategoryDialog(
      onConfirm = { newCat ->
        if (newCat.isNotBlank()) {
          if (!allCategories.any { it.equals(newCat, ignoreCase = true) }) {
            customCategories.add(newCat)
          }
          category = newCat
        }
        showAddCategoryDialog = false
      },
      onDismiss = { showAddCategoryDialog = false },
    )
  }

  if (showDiscardDialog) {
    AlertDialog(
      onDismissRequest = { showDiscardDialog = false },
      title = { Text("Discard unsaved changes?") },
      text = {
        Text("You have unsaved edits for this product. Save Draft or discard them.")
      },
      confirmButton = {
        Button(
          onClick = {
            showDiscardDialog = false
            onBack()
          },
        ) {
          Text("Discard")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
      },
    )
  }
}
