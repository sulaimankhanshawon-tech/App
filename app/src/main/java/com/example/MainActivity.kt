package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.gateway.SaudiPaymentGatewayClient
import com.example.data.model.RestaurantSettings
import com.example.data.model.StaffRole
import com.example.data.model.UiState
import com.example.data.preferences.LanguagePreferences
import com.example.data.repository.RestaurantRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.LoginMode
import com.example.ui.auth.signOutUser
import com.example.ui.components.LocalProductImages
import com.example.ui.components.NotificationsDialog
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.getStrings
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.BillingFeedbackScreen
import com.example.ui.screens.CustomerMenuScreen
import com.example.ui.screens.CustomerServicesGamesScreen
import com.example.ui.screens.KitchenDisplayScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.WaiterDashboardScreen
import com.example.ui.theme.AsilAlTawooqTheme
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.SaffronPrimary
import com.example.ui.viewmodel.AppPortal
import com.example.ui.viewmodel.RestaurantViewModel
import com.example.util.TableQrParser
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var deepLinkTableCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkTableCode = extractTableFromIntent(intent)
        setContent {
            AsilRootApp(initialTableCode = deepLinkTableCode)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkTableCode = extractTableFromIntent(intent)
    }

    private fun extractTableFromIntent(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        return TableQrParser.parseTableLabel(uri.toString())
            ?: (uri.lastPathSegment ?: uri.getQueryParameter("table"))?.let { TableQrParser.parseTableLabel(it) }
    }
}

@Composable
fun AsilRootApp(initialTableCode: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val languagePreferences = remember(context) { LanguagePreferences(context.applicationContext) }
    val language by languagePreferences.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.EN)

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var loginIntent by remember { mutableStateOf(LoginMode.GUEST) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val isArabic = language == AppLanguage.AR
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        AsilAlTawooqTheme(isArabic = isArabic) {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    language = language,
                    initialTableLabel = initialTableCode,
                    onToggleLanguage = {
                        scope.launch {
                            languagePreferences.setLanguage(if (isArabic) AppLanguage.EN else AppLanguage.AR)
                        }
                    },
                    onLoginModeSelected = { mode -> loginIntent = mode },
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                    }
                )
            } else {
                val uid = user.uid
                key(uid) {
                    AuthenticatedRestaurantShell(
                        uid = uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: "Jeddah Guest",
                        initialTableCode = initialTableCode,
                        loginIntent = loginIntent,
                        onResetLoginIntentToGuest = { loginIntent = LoginMode.GUEST }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedRestaurantShell(
    uid: String,
    email: String,
    displayName: String,
    initialTableCode: String?,
    loginIntent: LoginMode,
    onResetLoginIntentToGuest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: RestaurantViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                RestaurantViewModel(
                    repository = RestaurantRepository(db),
                    gatewayClient = SaudiPaymentGatewayClient(),
                    languagePreferences = LanguagePreferences(app),
                    currentUserId = uid,
                    currentUserEmail = email,
                    currentUserName = displayName,
                    initialTableCode = initialTableCode
                )
            }
        }
    )

    LaunchedEffect(initialTableCode) {
        if (!initialTableCode.isNullOrBlank()) {
            viewModel.selectTableByQrOrLabel(initialTableCode)
        }
    }

    val language by viewModel.language.collectAsStateWithLifecycle()
    val strings = remember(language) { getStrings(language) }
    val activePortal by viewModel.activePortal.collectAsStateWithLifecycle()
    val activeTableLabel by viewModel.selectedTableLabel.collectAsStateWithLifecycle()
    val menuState by viewModel.menuState.collectAsStateWithLifecycle()
    val tablesState by viewModel.tablesState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val requestsState by viewModel.serviceRequestsState.collectAsStateWithLifecycle()
    val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
    val loyaltyState by viewModel.loyaltyState.collectAsStateWithLifecycle()
    val reviewsState by viewModel.reviewsState.collectAsStateWithLifecycle()
    val gamesState by viewModel.gameSessionsState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val staffState by viewModel.staffState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val loyaltyDiscountSar by viewModel.loyaltyDiscountSar.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val selectedStation by viewModel.selectedStation.collectAsStateWithLifecycle()
    val currentEpochSec by viewModel.currentEpochSec.collectAsStateWithLifecycle()
    val lastGatewayMessage by viewModel.lastGatewayMessage.collectAsStateWithLifecycle()
    val productImageBytesMap by viewModel.productImageBytesMap.collectAsStateWithLifecycle()
    val currentStaffRole by viewModel.currentStaffRole.collectAsStateWithLifecycle()
    val isStaff by viewModel.isStaff.collectAsStateWithLifecycle()
    val isAdminOrManager by viewModel.isAdminOrManager.collectAsStateWithLifecycle()
    val canAccessWaiter by viewModel.canAccessWaiter.collectAsStateWithLifecycle()
    val canAccessKitchen by viewModel.canAccessKitchen.collectAsStateWithLifecycle()
    val myStaffProfileState by viewModel.myStaffProfileState.collectAsStateWithLifecycle()
    val isSubmittingOrder by viewModel.isSubmittingOrder.collectAsStateWithLifecycle()

    val tables = (tablesState as? UiState.Success)?.data.orEmpty()
    val activeTable = activeTableLabel?.let { lbl ->
        tables.firstOrNull { it.label.equals(lbl, ignoreCase = true) && it.active }
    }
    val settings = (settingsState as? UiState.Success)?.data ?: RestaurantSettings()
    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
    val unreadCount = notifications.count { !it.read }

    var autoRoutedStaff by remember(uid) { mutableStateOf(false) }
    LaunchedEffect(loginIntent, currentStaffRole) {
        if (loginIntent == LoginMode.STAFF_ADMIN && !autoRoutedStaff && currentStaffRole != null) {
            autoRoutedStaff = true
            when (currentStaffRole) {
                StaffRole.OWNER, StaffRole.MANAGER -> viewModel.switchPortal(AppPortal.ADMIN_DASHBOARD)
                StaffRole.WAITER -> viewModel.switchPortal(AppPortal.WAITER_DASHBOARD)
                StaffRole.KITCHEN -> viewModel.switchPortal(AppPortal.KITCHEN_DISPLAY)
                null -> Unit
            }
        }
    }

    LaunchedEffect(activePortal, isAdminOrManager, canAccessWaiter, canAccessKitchen) {
        val allowed = when (activePortal) {
            AppPortal.ADMIN_DASHBOARD -> isAdminOrManager
            AppPortal.WAITER_DASHBOARD -> canAccessWaiter
            AppPortal.KITCHEN_DISPLAY -> canAccessKitchen
            else -> true
        }
        if (!allowed) {
            viewModel.switchPortal(AppPortal.CUSTOMER_MENU)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        if (!statusBanner.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusBanner!!)
            viewModel.clearStatusBanner()
        }
    }

    if (activePortal != AppPortal.CUSTOMER_MENU) {
        BackHandler {
            viewModel.switchPortal(AppPortal.CUSTOMER_MENU)
        }
    }

    var showQrScannerDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showRoleMenu by remember { mutableStateOf(false) }

    val navItems = buildList {
        add(Triple(AppPortal.CUSTOMER_MENU, strings.menuTab, Icons.Default.RestaurantMenu))
        add(Triple(AppPortal.CUSTOMER_SERVICES_GAMES, strings.tableServicesTab, Icons.Default.RoomService))
        add(Triple(AppPortal.CUSTOMER_BILLING, strings.billingTab, Icons.AutoMirrored.Filled.ReceiptLong))
        if (canAccessWaiter) {
            add(Triple(AppPortal.WAITER_DASHBOARD, strings.waiterTab, Icons.Default.Dining))
        }
        if (canAccessKitchen) {
            add(Triple(AppPortal.KITCHEN_DISPLAY, strings.kitchenTab, Icons.Default.Kitchen))
        }
        if (isAdminOrManager) {
            add(Triple(AppPortal.ADMIN_DASHBOARD, strings.adminTab, Icons.Default.AdminPanelSettings))
        }
    }

    CompositionLocalProvider(LocalProductImages provides productImageBytesMap) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 720.dp

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = EspressoInk
                        ),
                        title = {
                            Column {
                                Text(
                                    text = strings.brandTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val subtitleText = if (activeTableLabel != null) {
                                    "${strings.activeTable} $activeTableLabel • ${strings.jeddahLocation}"
                                } else {
                                    "${strings.scanQrOrSelectTable} • ${strings.jeddahLocation}"
                                }
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SaffronPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showQrScannerDialog = true },
                                modifier = Modifier.testTag("topbar_qr_button")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanQrOrSelectTable)
                            }

                            if (isAdminOrManager) {
                                IconButton(
                                    onClick = { viewModel.switchPortal(AppPortal.ADMIN_DASHBOARD) },
                                    modifier = Modifier.testTag("topbar_admin_button")
                                ) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = strings.adminTab,
                                        tint = if (activePortal == AppPortal.ADMIN_DASHBOARD) SaffronPrimary else EspressoInk
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.toggleLanguage() },
                                modifier = Modifier.testTag("topbar_language_button")
                            ) {
                                Icon(Icons.Default.Language, contentDescription = "Toggle Arabic/English")
                            }

                            IconButton(
                                onClick = { showNotificationsDialog = true },
                                modifier = Modifier.testTag("topbar_notifications_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge { Text("$unreadCount") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = strings.notifications)
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { showRoleMenu = true },
                                    modifier = Modifier.testTag("topbar_role_menu_button")
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = "Switch Portal / Account")
                                }
                                DropdownMenu(
                                    expanded = showRoleMenu,
                                    onDismissRequest = { showRoleMenu = false }
                                ) {
                                    navItems.forEach { (portal, label, icon) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            leadingIcon = { Icon(icon, null) },
                                            onClick = {
                                                viewModel.switchPortal(portal)
                                                showRoleMenu = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(strings.signOut) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                                        onClick = {
                                            showRoleMenu = false
                                            onResetLoginIntentToGuest()
                                            signOutUser(credentialManager, scope)
                                        }
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            val bottomSlice = if (navItems.size > 5) {
                                navItems.take(5)
                            } else navItems

                            bottomSlice.forEach { (portal, label, icon) ->
                                NavigationBarItem(
                                    selected = activePortal == portal,
                                    onClick = { viewModel.switchPortal(portal) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("nav_${portal.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                            Spacer(Modifier.size(8.dp))
                            navItems.forEach { (portal, label, icon) ->
                                NavigationRailItem(
                                    selected = activePortal == portal,
                                    onClick = { viewModel.switchPortal(portal) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        when (activePortal) {
                            AppPortal.CUSTOMER_MENU -> CustomerMenuScreen(
                                menuState = menuState,
                                activeTable = activeTable,
                                activeTableLabel = activeTableLabel,
                                cart = cart,
                                isSubmittingOrder = isSubmittingOrder,
                                loyaltyDiscountSar = loyaltyDiscountSar,
                                settings = settings,
                                language = language,
                                onOpenQrScanner = { showQrScannerDialog = true },
                                onAddToCart = viewModel::addToCart,
                                onUpdateCartQty = viewModel::updateCartQuantity,
                                onSubmitOrder = { instr -> viewModel.submitOrder(activeTable, instr, settings.vatPercent) }
                            )

                            AppPortal.CUSTOMER_SERVICES_GAMES -> CustomerServicesGamesScreen(
                                activeTable = activeTable,
                                activeTableLabel = activeTableLabel,
                                ordersState = ordersState,
                                loyaltyState = loyaltyState,
                                gamesState = gamesState,
                                settings = settings,
                                language = language,
                                onOpenQrScanner = { showQrScannerDialog = true },
                                onServiceRequest = { type, note -> viewModel.sendServiceRequest(activeTable, type, note) },
                                onRedeemReward = viewModel::redeemReward,
                                onSaveBirthday = viewModel::saveBirthday,
                                onStartGame = viewModel::startTableGame,
                                onAnswerGame = viewModel::answerGameQuestion,
                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )

                            AppPortal.CUSTOMER_BILLING -> BillingFeedbackScreen(
                                ordersState = ordersState,
                                paymentsState = paymentsState,
                                settings = settings,
                                userEmail = email,
                                activeTableNumber = activeTable?.tableNumber ?: 0,
                                lastGatewayMessage = lastGatewayMessage,
                                language = language,
                                onClearGatewayMessage = viewModel::clearGatewayMessage,
                                onProcessPayment = { order, amt, tip, method, split, token, rEmail, itemIds ->
                                    viewModel.processPayment(order, amt, tip, method, split, settings, token, rEmail, itemIds)
                                },
                                onSubmitReview = viewModel::submitFeedback
                            )

                            AppPortal.WAITER_DASHBOARD -> WaiterDashboardScreen(
                                isOwnerOrStaff = canAccessWaiter,
                                tablesState = tablesState,
                                ordersState = ordersState,
                                requestsState = requestsState,
                                paymentsState = paymentsState,
                                language = language,
                                onConfirmOrderToKitchen = viewModel::confirmOrderToKitchen,
                                onAdvanceRequest = viewModel::advanceServiceRequest,
                                onUpdateTableStatus = viewModel::updateTableState,
                                onVerifyCashPayment = viewModel::verifyCashOrBankPayment
                            )

                            AppPortal.KITCHEN_DISPLAY -> KitchenDisplayScreen(
                                isOwnerOrStaff = canAccessKitchen,
                                ordersState = ordersState,
                                selectedStation = selectedStation,
                                currentEpochSec = currentEpochSec,
                                language = language,
                                onSelectStation = viewModel::setKdsStationFilter,
                                onAdvanceOrder = viewModel::advanceOrderStatus
                            )

                            AppPortal.ADMIN_DASHBOARD -> AdminDashboardScreen(
                                isAdminOrManager = isAdminOrManager,
                                callerRole = currentStaffRole,
                                menuState = menuState,
                                tablesState = tablesState,
                                ordersState = ordersState,
                                requestsState = requestsState,
                                paymentsState = paymentsState,
                                reviewsState = reviewsState,
                                staffState = staffState,
                                settings = settings,
                                onUploadDishPhoto = viewModel::uploadDishPhoto,
                                onSaveMenuItem = viewModel::saveMenuItem,
                                onDeleteMenuItem = viewModel::deleteMenuItem,
                                onSaveTable = viewModel::addOrUpdateTable,
                                onToggleTableActive = viewModel::toggleTableActive,
                                onDeleteTable = viewModel::deleteTable,
                                onDownloadSingleQr = viewModel::downloadSingleQr,
                                onDownloadAllQrPdf = viewModel::downloadAllQrPdf,
                                onPrintQrTables = viewModel::printQrTables,
                                onConfirmOrder = viewModel::confirmOrderToKitchen,
                                onUpdateTableStatus = viewModel::updateTableState,
                                onSaveStaff = viewModel::addOrUpdateStaff,
                                onToggleStaffActive = viewModel::toggleStaffActive,
                                onDeleteStaff = viewModel::deleteStaffMember,
                                onSaveSettings = viewModel::saveRestaurantSettings,
                                onSeedDemoOrder = viewModel::seedDemoOrder,
                                onGenerateBackupJson = viewModel::generateBackupJson,
                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }
                    }
                }
            }
        }

        if (loginIntent == LoginMode.STAFF_ADMIN && myStaffProfileState is UiState.Success && currentStaffRole == null) {
            AlertDialog(
                onDismissRequest = onResetLoginIntentToGuest,
                title = { Text("Staff Account Not Authorized") },
                text = {
                    Text("The signed-in Google account ($email) is not assigned an active Owner, Manager, Waiter, or Kitchen Staff role. Ask the restaurant Owner to authorize your email, or continue as a dining guest.")
                },
                confirmButton = {
                    Button(onClick = onResetLoginIntentToGuest) {
                        Text("Continue as Guest")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        onResetLoginIntentToGuest()
                        signOutUser(credentialManager, scope)
                    }) {
                        Text(strings.signOut)
                    }
                }
            )
        }

        if (showQrScannerDialog) {
            QrScannerScreen(
                tables = tables,
                currentTableLabel = activeTableLabel,
                language = language,
                allowManualEntry = isStaff,
                onQrScanned = { qrPayload ->
                    val joined = viewModel.selectTableByQrOrLabel(qrPayload)
                    if (joined && activePortal != AppPortal.CUSTOMER_MENU) {
                        viewModel.switchPortal(AppPortal.CUSTOMER_MENU)
                    }
                    joined
                },
                onDismiss = { showQrScannerDialog = false }
            )
        }

        if (showNotificationsDialog) {
            NotificationsDialog(
                notifications = notifications,
                language = language,
                onMarkRead = viewModel::markNotificationRead,
                onDismiss = { showNotificationsDialog = false }
            )
        }
    }
}
