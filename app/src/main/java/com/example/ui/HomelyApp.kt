package com.example.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.local.ServerConfigStore
import com.example.data.local.HomelyDatabase
import com.example.data.model.PaymentDirection
import com.example.data.model.Product
import com.example.data.model.ProductCatalogHelper
import com.example.data.model.Shop
import com.example.data.model.Supplier
import com.example.data.remote.LaptopServerClient
import com.example.data.repository.LocalTestRepository
import com.example.data.repository.HomelyRepository
import com.example.data.repository.SyncCoordinatorRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.AppDestination
import com.example.ui.components.HomelyBottomBar
import com.example.ui.components.isNetworkAvailable
import com.example.ui.screens.AddEditProductScreen
import com.example.ui.screens.AddEditShopScreen
import com.example.ui.screens.AddEditSupplierScreen
import com.example.ui.screens.DummyModuleScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrderScreen
import com.example.ui.screens.PackingScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.PlaceOrderScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.RoutePlanScreen
import com.example.ui.screens.ShopListScreen
import com.example.ui.screens.SuppliersScreen
import com.example.ui.screens.SyncServerScreen
import com.example.ui.screens.TransportScreen
import com.example.ui.viewmodel.HomelyViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

const val ROUTE_MAIN = "main"
const val ROUTE_ADD_EDIT_SHOP = "add_edit_shop?shopId={shopId}"
const val ROUTE_ADD_EDIT_PRODUCT = "add_edit_product?productId={productId}"
const val ROUTE_ADD_EDIT_SUPPLIER = "add_edit_supplier?supplierId={supplierId}"
const val ROUTE_PLACE_ORDER = "place_order/{shopId}?orderId={orderId}"
const val ROUTE_SYNC_SERVERS = "sync_servers"

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun HomelyApp(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var isTestModeBypass by rememberSaveable { mutableStateOf(false) }
  val user = currentUser

  if (user == null && !isTestModeBypass) {
    AuthScreen(
      onAuthSuccess = { isTestModeBypass = false },
      onSkipForTesting = { isTestModeBypass = true },
    )
  } else if (user == null && isTestModeBypass) {
    androidx.compose.runtime.key(LocalTestRepository.LOCAL_UID) {
      AuthenticatedAppShell(
        currentUserId = LocalTestRepository.LOCAL_UID,
        userDisplayName = "Local Tester (No Login)",
        isLocalTestMode = true,
        onExitTestMode = { isTestModeBypass = false },
      )
    }
  } else {
    val authUser = user!!
    androidx.compose.runtime.key(authUser.uid) {
      AuthenticatedAppShell(
        currentUserId = authUser.uid,
        userDisplayName = authUser.displayName ?: authUser.email ?: "",
        isLocalTestMode = false,
        onExitTestMode = {},
      )
    }
  }
}

@Composable
private fun AuthenticatedAppShell(
  currentUserId: String,
  userDisplayName: String,
  isLocalTestMode: Boolean,
  onExitTestMode: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: HomelyViewModel = viewModel(
    key = "$currentUserId:$isLocalTestMode",
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val primary = if (isLocalTestMode) {
          LocalTestRepository()
        } else {
          val databaseId = app.getString(R.string.firestore_database_id)
          val db = FirebaseFirestore.getInstance(databaseId)
          HomelyRepository(db, Firebase.auth)
        }
        val roomDb = HomelyDatabase.getInstance(app)
        val coordinator = SyncCoordinatorRepository(
          appContext = app,
          primary = primary,
          dao = roomDb.homelyDao(),
          configStore = ServerConfigStore(app),
          laptopClient = LaptopServerClient(),
          currentUserId = currentUserId,
          isLocalTestMode = isLocalTestMode,
        )
        HomelyViewModel(coordinator, currentUserId)
      }
    },
  )

  val shopsState by viewModel.shopsState.collectAsStateWithLifecycle()
  val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
  val stocksState by viewModel.stocksState.collectAsStateWithLifecycle()
  val productsState by viewModel.productsState.collectAsStateWithLifecycle()
  val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
  val supplierBillsState by viewModel.supplierBillsState.collectAsStateWithLifecycle()
  val suppliersState by viewModel.suppliersState.collectAsStateWithLifecycle()
  val metaState by viewModel.metaState.collectAsStateWithLifecycle()
  val routesList by viewModel.routesState.collectAsStateWithLifecycle()
  val serverSettings by viewModel.serverSettings.collectAsStateWithLifecycle()
  val pendingShops by viewModel.pendingShops.collectAsStateWithLifecycle()
  val pendingOrders by viewModel.pendingOrders.collectAsStateWithLifecycle()
  val pendingStockUpdates by viewModel.pendingStockUpdates.collectAsStateWithLifecycle()
  val pendingProducts by viewModel.pendingProducts.collectAsStateWithLifecycle()
  val pendingPayments by viewModel.pendingPayments.collectAsStateWithLifecycle()
  val pendingSupplierBills by viewModel.pendingSupplierBills.collectAsStateWithLifecycle()
  val pendingSuppliers by viewModel.pendingSuppliers.collectAsStateWithLifecycle()
  val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
  val laptopHealth by viewModel.laptopHealth.collectAsStateWithLifecycle()

  val requireGlobal = serverSettings.globalSyncEnabled && !isLocalTestMode
  val requireLaptop = serverSettings.laptopServerEnabled && serverSettings.laptopServerUrl.isNotBlank()
  val pendingUploadCount = remember(
    pendingShops,
    pendingOrders,
    pendingStockUpdates,
    pendingProducts,
    pendingPayments,
    pendingSupplierBills,
    pendingSuppliers,
    requireGlobal,
    requireLaptop,
  ) {
    pendingShops.count { !it.isFullySynced(requireGlobal, requireLaptop) } +
      pendingOrders.count { !it.isFullySynced(requireGlobal, requireLaptop) } +
      pendingStockUpdates.count { requireGlobal && !it.globalSynced } +
      pendingProducts.count { !it.isFullySynced(requireGlobal) } +
      pendingPayments.count { !it.isFullySynced(requireGlobal) } +
      pendingSupplierBills.count { !it.isFullySynced(requireGlobal) } +
      pendingSuppliers.count { !it.isFullySynced(requireGlobal) }
  }

  // Automatically trigger background sync when network/Wi-Fi becomes available
  DisposableEffect(context, viewModel) {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        viewModel.onNetworkAvailable()
      }
    }
    if (cm != null) {
      val req = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
      try {
        cm.registerNetworkCallback(req, callback)
      } catch (_: Exception) {
        // Ignore if permission or OS constraint prevents callback registration
      }
    }
    onDispose {
      if (cm != null) {
        try {
          cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
      }
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(viewModel) {
    viewModel.feedback.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  val navController = rememberNavController()
  var currentTab by rememberSaveable { mutableStateOf(AppDestination.HOME) }
  var isMoreExpanded by remember { mutableStateOf(false) }
  var paymentInitialDirection by rememberSaveable { mutableStateOf(PaymentDirection.COLLECT) }

  val shopsList = (shopsState as? UiState.Success)?.data.orEmpty()
  val ordersList = (ordersState as? UiState.Success)?.data.orEmpty()
  val stocksList = (stocksState as? UiState.Success)?.data.orEmpty()
  val productsList = (productsState as? UiState.Success)?.data.orEmpty()
  val paymentsList = (paymentsState as? UiState.Success)?.data.orEmpty()
  val supplierBillsList = (supplierBillsState as? UiState.Success)?.data.orEmpty()
  val suppliersList = (suppliersState as? UiState.Success)?.data.orEmpty()

  NavHost(
    navController = navController,
    startDestination = ROUTE_MAIN,
  ) {
    composable(ROUTE_MAIN) {
      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          HomelyBottomBar(
            currentDestination = currentTab,
            isMoreExpanded = isMoreExpanded,
            onToggleMore = { isMoreExpanded = !isMoreExpanded },
            onSelectDestination = { dest ->
              currentTab = dest
              isMoreExpanded = false
            },
          )
        },
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        ) {
          when {
            shopsState is UiState.Loading && shopsList.isEmpty() -> {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
              }
            }
            shopsState is UiState.Error && shopsList.isEmpty() -> {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text((shopsState as UiState.Error).message)
              }
            }
            else -> {
              when (currentTab) {
                AppDestination.HOME -> HomeScreen(
                  shops = shopsList,
                  orders = ordersList,
                  stocks = stocksList,
                  meta = metaState,
                  userDisplayName = userDisplayName,
                  isLocalTestMode = isLocalTestMode,
                  pendingQueueCount = pendingUploadCount,
                  laptopServerEnabled = serverSettings.laptopServerEnabled,
                  onOpenSyncServers = {
                    navController.navigate(ROUTE_SYNC_SERVERS)
                  },
                  onSignOut = {
                    if (isLocalTestMode) {
                      onExitTestMode()
                    } else {
                      signOutUser(credentialManager = credentialManager, scope = scope)
                    }
                  },
                  onUpdateStock = { stockId, newQty, name ->
                    viewModel.updateStockQuantity(stockId, newQty, name)
                  },
                  onNavigateToShops = {
                    currentTab = AppDestination.SHOPS
                    isMoreExpanded = false
                  },
                  onNavigateToOrders = {
                    currentTab = AppDestination.ORDERS
                    isMoreExpanded = false
                  },
                  payments = paymentsList,
                  supplierBills = supplierBillsList,
                  onNavigateToPayments = { direction ->
                    paymentInitialDirection = direction
                    currentTab = AppDestination.PAYMENTS
                    isMoreExpanded = false
                  },
                )

                AppDestination.SHOPS -> ShopListScreen(
                  shops = shopsList,
                  routes = routesList,
                  pendingQueueCount = pendingShops.count { !it.isFullySynced(requireGlobal, requireLaptop) },
                  laptopServerEnabled = serverSettings.laptopServerEnabled,
                  onOpenSyncServers = {
                    navController.navigate(ROUTE_SYNC_SERVERS)
                  },
                  onAddShopClick = {
                    navController.navigate("add_edit_shop")
                  },
                  onEditShopClick = { shop ->
                    navController.navigate("add_edit_shop?shopId=${shop.id}")
                  },
                  onChangeShopStatus = { shopId, status, label ->
                    viewModel.updateShopStatus(shopId, status, label)
                  },
                )

                AppDestination.ORDERS -> OrderScreen(
                  shops = shopsList,
                  orders = ordersList,
                  routes = routesList,
                  onPlaceOrderClick = { shop ->
                    navController.navigate("place_order/${shop.id}")
                  },
                  onEditOrderClick = { order ->
                    navController.navigate("place_order/${order.shopId}?orderId=${order.id}")
                  },
                  onDeleteOrderClick = { order ->
                    viewModel.deleteOrder(order)
                  },
                  onMarkInquiryStatus = { shopId, status, label ->
                    viewModel.updateShopInquiryStatus(shopId, status, label)
                  },
                )

                AppDestination.ROUTES -> RoutePlanScreen(
                  shops = shopsList,
                  routes = routesList,
                  onSaveRoute = { route, msg ->
                    viewModel.saveRoute(route, msg)
                  },
                  onUpdateShopRoute = { shopId, newRouteId, msg ->
                    viewModel.updateShopRoute(shopId, newRouteId, msg)
                  },
                  onBulkAssignShopsToRoute = { shopIds, targetRouteId, msg ->
                    viewModel.bulkAssignShopsToRoute(shopIds, targetRouteId, msg)
                  },
                  onOpenSyncServers = {
                    navController.navigate(ROUTE_SYNC_SERVERS)
                  },
                )

                AppDestination.PACKING -> PackingScreen(
                  shops = shopsList,
                  orders = ordersList,
                  routes = routesList,
                  stocks = stocksList,
                  onUpdateStock = { stockId, newQty, name ->
                    viewModel.updateStockQuantity(stockId, newQty, name)
                  },
                  onOpenSyncServers = {
                    navController.navigate(ROUTE_SYNC_SERVERS)
                  },
                  onNavigateToRoutes = {
                    currentTab = AppDestination.ROUTES
                    isMoreExpanded = false
                  },
                  onNavigateToOrders = {
                    currentTab = AppDestination.ORDERS
                    isMoreExpanded = false
                  },
                  onShowFeedback = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                  },
                  onSaveRoute = { route, msg ->
                    viewModel.saveRoute(route, msg)
                  },
                )

                AppDestination.TRANSPORT -> TransportScreen(
                  shops = shopsList,
                  orders = ordersList,
                  routes = routesList,
                  userDisplayName = userDisplayName,
                  onMarkOrderDelivered = { orderId, shopName, podNote ->
                    viewModel.markOrderDelivered(orderId, shopName, podNote)
                  },
                  onNavigateToRoutes = {
                    currentTab = AppDestination.ROUTES
                    isMoreExpanded = false
                  },
                  onOpenSyncServers = {
                    navController.navigate(ROUTE_SYNC_SERVERS)
                  },
                  onShowFeedback = { msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                  },
                  onSaveRoute = { route, msg ->
                    viewModel.saveRoute(route, msg)
                  },
                )

                AppDestination.PRODUCTS -> ProductsScreen(
                  products = productsList,
                  pendingQueueCount = pendingProducts.count { !it.isFullySynced(requireGlobal) },
                  onOpenSyncServers = {
                    navController.navigate(ROUTE_SYNC_SERVERS)
                  },
                  onAddProductClick = {
                    navController.navigate("add_edit_product")
                  },
                  onEditProductClick = { product ->
                    navController.navigate("add_edit_product?productId=${product.id}")
                  },
                  onChangeProductStatus = { productId, status, label ->
                    viewModel.updateProductStatus(productId, status, label)
                  },
                  onUpdateProductStock = { productId, newQty, name ->
                    viewModel.updateProductStock(productId, newQty, name)
                  },
                )

                AppDestination.PAYMENTS -> PaymentsScreen(
                  shops = shopsList,
                  orders = ordersList,
                  payments = paymentsList,
                  supplierBills = supplierBillsList,
                  routes = routesList,
                  userDisplayName = userDisplayName,
                  pendingQueueCount = pendingUploadCount,
                  laptopServerEnabled = serverSettings.laptopServerEnabled,
                  initialDirection = paymentInitialDirection,
                  onRecordPayment = { payment, isOnline, onSaved ->
                    viewModel.recordPayment(payment, isOnline, onSaved)
                  },
                  onSaveSupplierBill = { bill, isOnline, onSaved ->
                    viewModel.saveSupplierBill(bill, isOnline, onSaved)
                  },
                  onOpenSyncServers = {
                    navController.navigate(ROUTE_SYNC_SERVERS)
                  },
                )

                AppDestination.SUPPLIERS -> SuppliersScreen(
                  suppliers = suppliersList,
                  pendingQueueCount = pendingUploadCount,
                  onOpenSyncServers = {
                    navController.navigate(ROUTE_SYNC_SERVERS)
                  },
                  onAddSupplierClick = {
                    navController.navigate("add_edit_supplier")
                  },
                  onEditSupplierClick = { supplier ->
                    navController.navigate("add_edit_supplier?supplierId=${supplier.id}")
                  },
                  onChangeSupplierStatus = { supplierId, status, label ->
                    viewModel.updateSupplierStatus(supplierId, status, label)
                  },
                  onNavigateToSupplierPayouts = {
                    paymentInitialDirection = PaymentDirection.PAY_SUPPLIER
                    currentTab = AppDestination.PAYMENTS
                    isMoreExpanded = false
                  },
                )

                else -> DummyModuleScreen(destination = currentTab)
              }
            }
          }
        }
      }
    }

    composable(
      route = ROUTE_ADD_EDIT_SHOP,
      arguments = listOf(
        navArgument("shopId") {
          type = NavType.StringType
          nullable = true
          defaultValue = null
        }
      ),
    ) { backStackEntry ->
      val shopId = backStackEntry.arguments?.getString("shopId")
      val existingShop: Shop? = remember(shopId, shopsList) {
        if (shopId.isNullOrBlank()) null else shopsList.firstOrNull { it.id == shopId }
      }
      val suggestedCode = remember(shopsList.size) {
        String.format(java.util.Locale.US, "SHOP-%06d", 107 + shopsList.size)
      }
      var isSavingShop by remember { mutableStateOf(false) }
      AddEditShopScreen(
        existingShop = existingShop,
        suggestedShopCode = suggestedCode,
        addedByName = userDisplayName.ifBlank { "Field Officer" },
        availableRoutes = routesList,
        isSaving = isSavingShop,
        onSaveShop = { shop, isOnline ->
          if (!isSavingShop) {
            isSavingShop = true
            viewModel.saveShop(
              shop = shop,
              isOnline = isOnline,
              onFinished = { isSavingShop = false },
              onSaved = { navController.popBackStack() },
            )
          }
        },
        onBack = { navController.popBackStack() },
      )
    }

    composable(
      route = ROUTE_ADD_EDIT_PRODUCT,
      arguments = listOf(
        navArgument("productId") {
          type = NavType.StringType
          nullable = true
          defaultValue = null
        }
      ),
    ) { backStackEntry ->
      val productId = backStackEntry.arguments?.getString("productId")
      val existingProduct: Product? = remember(productId, productsList) {
        if (productId.isNullOrBlank()) null else productsList.firstOrNull { it.id == productId }
      }
      val suggestedCode = remember(productsList.size) {
        String.format(java.util.Locale.US, "PRD-%05d", 107 + productsList.size)
      }
      val availableCats = remember(productsList) {
        (ProductCatalogHelper.defaultCategories + productsList.map { it.category }).distinct()
      }
      AddEditProductScreen(
        existingProduct = existingProduct,
        suggestedProductCode = suggestedCode,
        availableCategories = availableCats,
        onSaveProduct = { product, isOnline ->
          viewModel.saveProduct(product, isOnline) {
            navController.popBackStack()
          }
        },
        onBack = { navController.popBackStack() },
      )
    }

    composable(
      route = ROUTE_ADD_EDIT_SUPPLIER,
      arguments = listOf(
        navArgument("supplierId") {
          type = NavType.StringType
          nullable = true
          defaultValue = null
        }
      ),
    ) { backStackEntry ->
      val supplierId = backStackEntry.arguments?.getString("supplierId")
      val existingSupplier: Supplier? = remember(supplierId, suppliersList) {
        if (supplierId.isNullOrBlank()) null else suppliersList.firstOrNull { it.id == supplierId }
      }
      val suggestedCode = remember(suppliersList.size) {
        String.format(java.util.Locale.US, "SUP-%06d", 48 + suppliersList.size)
      }
      AddEditSupplierScreen(
        existingSupplier = existingSupplier,
        suggestedSupplierCode = suggestedCode,
        addedByName = userDisplayName.ifBlank { "Field Officer" },
        onSaveSupplier = { supplier, isOnline ->
          viewModel.saveSupplier(supplier, isOnline) {
            navController.popBackStack()
          }
        },
        onBack = { navController.popBackStack() },
      )
    }

    composable(
      route = ROUTE_PLACE_ORDER,
      arguments = listOf(
        navArgument("shopId") { type = NavType.StringType },
        navArgument("orderId") {
          type = NavType.StringType
          nullable = true
          defaultValue = null
        },
      ),
    ) { backStackEntry ->
      val shopId = backStackEntry.arguments?.getString("shopId").orEmpty()
      val orderId = backStackEntry.arguments?.getString("orderId")
      val existingOrder = remember(orderId, ordersList) {
        if (orderId.isNullOrBlank()) null
        else ordersList.firstOrNull { it.id == orderId || it.orderCode == orderId }
      }
      val shop = remember(shopId, shopsList, existingOrder) {
        shopsList.firstOrNull { it.id == shopId }
          ?: existingOrder?.let { ord ->
            shopsList.firstOrNull { it.shopCode == ord.shopCode }
              ?: Shop(
                id = ord.shopId.ifBlank { shopId },
                shopCode = ord.shopCode,
                name = ord.shopName,
                routeId = ord.routeId,
              )
          }
      }
      if (shop == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("Shop not found")
        }
      } else {
        val liveCatalogProducts = remember(productsList) {
          ProductCatalogHelper.toCatalogProducts(productsList)
        }
        PlaceOrderScreen(
          shop = shop,
          existingOrder = existingOrder,
          catalogProducts = liveCatalogProducts,
          onConfirmOrder = { items, note ->
            if (existingOrder != null) {
              viewModel.updateOrder(existingOrder, items, note) {
                navController.popBackStack()
              }
            } else {
              viewModel.placeOrder(shop, items, note) {
                navController.popBackStack()
              }
            }
          },
          onBack = { navController.popBackStack() },
        )
      }
    }

    composable(ROUTE_SYNC_SERVERS) {
      SyncServerScreen(
        settings = serverSettings,
        pendingShops = pendingShops,
        pendingOrders = pendingOrders,
        pendingStockUpdates = pendingStockUpdates,
        pendingProducts = pendingProducts,
        pendingPayments = pendingPayments,
        pendingSupplierBills = pendingSupplierBills,
        pendingSuppliers = pendingSuppliers,
        isLocalTestMode = isLocalTestMode,
        isOnline = isNetworkAvailable(context),
        isSyncing = isSyncing,
        laptopHealth = laptopHealth,
        onSaveSettings = { g, l, url, token, autoDel ->
          viewModel.saveServerSettings(g, l, url, token, autoDel)
        },
        onTestLaptopConnection = { url, token ->
          viewModel.testLaptopConnection(url, token)
        },
        onSyncNow = { viewModel.syncNow(silent = false) },
        onPullFromLaptop = { viewModel.pullFromLaptopServer() },
        onSaveAndTestLaptopConnection = { g, l, url, token, autoDel ->
          viewModel.saveAndTestLaptopConnection(g, l, url, token, autoDel)
        },
        onSaveAndSyncNow = { g, l, url, token, autoDel ->
          viewModel.saveAndSyncNow(g, l, url, token, autoDel)
        },
        onSaveAndPullFromLaptop = { g, l, url, token, autoDel ->
          viewModel.saveAndPullFromLaptopServer(g, l, url, token, autoDel)
        },
        onDeleteSyncedFromPhone = { viewModel.deleteSyncedFromPhone() },
        onDeletePendingShop = { localId -> viewModel.deletePendingShop(localId) },
        onDeletePendingOrder = { localId -> viewModel.deletePendingOrder(localId) },
        onDeletePendingProduct = { localId -> viewModel.deletePendingProduct(localId) },
        onDeletePendingPayment = { localId -> viewModel.deletePendingPayment(localId) },
        onDeletePendingSupplierBill = { localId -> viewModel.deletePendingSupplierBill(localId) },
        onDeletePendingSupplier = { localId -> viewModel.deletePendingSupplier(localId) },
        onBack = { navController.popBackStack() },
      )
    }
  }
}
