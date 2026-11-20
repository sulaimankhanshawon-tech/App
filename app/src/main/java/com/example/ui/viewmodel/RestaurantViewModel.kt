package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gateway.GatewayVerificationResult
import com.example.data.gateway.SaudiPaymentGatewayClient
import com.example.data.model.CartItem
import com.example.data.model.GameSession
import com.example.data.model.GameType
import com.example.data.model.KitchenStation
import com.example.data.model.LoyaltyProfile
import com.example.data.model.MenuItem
import com.example.data.model.NotificationItem
import com.example.data.model.OWNER_BOOTSTRAP_EMAIL
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.ProductImage
import com.example.data.model.RequestStatus
import com.example.data.model.RestaurantSettings
import com.example.data.model.RestaurantTable
import com.example.data.model.Review
import com.example.data.model.ServiceRequest
import com.example.data.model.ServiceRequestType
import com.example.data.model.SpiceLevel
import com.example.data.model.SplitMode
import com.example.data.model.StaffMember
import com.example.data.model.StaffRole
import com.example.data.model.TableQrCode
import com.example.data.model.TableStatus
import com.example.data.model.UiState
import com.example.data.preferences.LanguagePreferences
import com.example.data.repository.RestaurantRepository
import com.example.ui.i18n.AppLanguage
import com.example.util.QrCardExporter
import com.example.util.TableQrParser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

enum class AppPortal {
    CUSTOMER_MENU,
    CUSTOMER_SERVICES_GAMES,
    CUSTOMER_BILLING,
    WAITER_DASHBOARD,
    KITCHEN_DISPLAY,
    ADMIN_DASHBOARD
}

@OptIn(ExperimentalCoroutinesApi::class)
class RestaurantViewModel(
    private val repository: RestaurantRepository,
    private val gatewayClient: SaudiPaymentGatewayClient,
    private val languagePreferences: LanguagePreferences,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserName: String,
    initialTableCode: String?
) : ViewModel() {

    val isBootstrapOwner: Boolean = currentUserEmail.equals(OWNER_BOOTSTRAP_EMAIL, ignoreCase = true)

    val language: StateFlow<AppLanguage> = languagePreferences.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.EN)

    val myStaffProfileState: StateFlow<UiState<StaffMember?>> = repository.observeMyStaffMember(currentUserId, currentUserEmail)
        .asUiState("my_staff_profile")

    val currentStaffRole: StateFlow<StaffRole?> = myStaffProfileState
        .map { state ->
            when {
                isBootstrapOwner -> StaffRole.OWNER
                state is UiState.Success -> state.data?.takeIf { it.active }?.let { StaffRole.fromRaw(it.role) }
                else -> null
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            if (isBootstrapOwner) StaffRole.OWNER else null
        )

    val isStaff: StateFlow<Boolean> = currentStaffRole
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, isBootstrapOwner)

    val isAdminOrManager: StateFlow<Boolean> = currentStaffRole
        .map { it == StaffRole.OWNER || it == StaffRole.MANAGER }
        .stateIn(viewModelScope, SharingStarted.Eagerly, isBootstrapOwner)

    val canAccessWaiter: StateFlow<Boolean> = currentStaffRole
        .map { it == StaffRole.OWNER || it == StaffRole.MANAGER || it == StaffRole.WAITER }
        .stateIn(viewModelScope, SharingStarted.Eagerly, isBootstrapOwner)

    val canAccessKitchen: StateFlow<Boolean> = currentStaffRole
        .map { it == StaffRole.OWNER || it == StaffRole.MANAGER || it == StaffRole.KITCHEN }
        .stateIn(viewModelScope, SharingStarted.Eagerly, isBootstrapOwner)

    private val _activePortal = MutableStateFlow(AppPortal.CUSTOMER_MENU)
    val activePortal: StateFlow<AppPortal> = _activePortal.asStateFlow()

    private val _selectedTableLabel = MutableStateFlow(
        initialTableCode?.let { TableQrParser.parseTableLabel(it) }
    )
    val selectedTableLabel: StateFlow<String?> = _selectedTableLabel.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _isSubmittingOrder = MutableStateFlow(false)
    val isSubmittingOrder: StateFlow<Boolean> = _isSubmittingOrder.asStateFlow()

    private val _loyaltyDiscountSar = MutableStateFlow(0.0)
    val loyaltyDiscountSar: StateFlow<Double> = _loyaltyDiscountSar.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _selectedStation = MutableStateFlow<KitchenStation?>(null)
    val selectedStation: StateFlow<KitchenStation?> = _selectedStation.asStateFlow()

    private val _lastGatewayMessage = MutableStateFlow<String?>(null)
    val lastGatewayMessage: StateFlow<String?> = _lastGatewayMessage.asStateFlow()

    // Live 1-second ticker for KDS preparation timers & delay highlights
    val currentEpochSec: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis() / 1000L)
            delay(1000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), System.currentTimeMillis() / 1000L)

    val menuState: StateFlow<UiState<List<MenuItem>>> = repository.observeMenuItems()
        .asUiState("menu_items")

    val productImagesState: StateFlow<UiState<List<ProductImage>>> = repository.observeProductImages()
        .asUiState("product_images")

    val productImageBytesMap: StateFlow<Map<String, ByteArray>> = productImagesState
        .map { state ->
            val list = (state as? UiState.Success)?.data.orEmpty()
            buildMap {
                list.forEach { img ->
                    if (img.id.isNotBlank() && img.base64Data.isNotBlank()) {
                        runCatching { Base64.decode(img.base64Data, Base64.DEFAULT) }
                            .onSuccess { bytes -> put(img.id, bytes) }
                    }
                }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val tablesState: StateFlow<UiState<List<RestaurantTable>>> = repository.observeTables()
        .asUiState("restaurant_tables")

    val qrCodesState: StateFlow<UiState<List<TableQrCode>>> = repository.observeQrCodes()
        .asUiState("qr_codes")

    val ordersState: StateFlow<UiState<List<Order>>> = isStaff
        .flatMapLatest { staffFlag -> repository.observeOrders(staffFlag) }
        .asUiState("orders")

    val serviceRequestsState: StateFlow<UiState<List<ServiceRequest>>> = isStaff
        .flatMapLatest { staffFlag -> repository.observeServiceRequests(staffFlag) }
        .asUiState("service_requests")

    val paymentsState: StateFlow<UiState<List<PaymentRecord>>> = isStaff
        .flatMapLatest { staffFlag -> repository.observePayments(staffFlag) }
        .asUiState("payments")

    val loyaltyState: StateFlow<UiState<LoyaltyProfile?>> = repository.observeLoyaltyProfile(currentUserId)
        .asUiState("loyalty_profiles")

    val reviewsState: StateFlow<UiState<List<Review>>> = repository.observePublishedReviews()
        .asUiState("reviews")

    val gameSessionsState: StateFlow<UiState<List<GameSession>>> = repository.observeGameSessions()
        .asUiState("game_sessions")

    val settingsState: StateFlow<UiState<RestaurantSettings>> = repository.observeSettings()
        .asUiState("restaurant_settings")

    val staffState: StateFlow<UiState<List<StaffMember>>> = isAdminOrManager
        .flatMapLatest { adminFlag -> repository.observeStaffMembers(adminFlag) }
        .asUiState("staff")

    val notificationsState: StateFlow<UiState<List<NotificationItem>>> = isStaff
        .flatMapLatest { staffFlag -> repository.observeNotifications(staffFlag) }
        .asUiState("notifications")

    init {
        viewModelScope.launch {
            repository.ensureLoyaltyProfile(currentUserName, currentUserEmail)
            if (isBootstrapOwner) {
                repository.bootstrapRestaurantIfNeeded()
            }
        }

        // Sync user profile record in /users/{uid} whenever staff role or active table changes
        viewModelScope.launch {
            currentStaffRole.collect { role ->
                repository.syncCurrentUserProfile(
                    displayName = currentUserName,
                    email = currentUserEmail,
                    staffRole = role?.name ?: "CUSTOMER",
                    activeTableLabel = _selectedTableLabel.value
                )
            }
        }

        // Keep cart prices/names/availability synced in real time when Admin edits menu items
        viewModelScope.launch {
            menuState.filterIsInstance<UiState.Success<List<MenuItem>>>().collect { state ->
                val byId = state.data.associateBy { it.id }
                val currentCart = _cart.value
                if (currentCart.isNotEmpty()) {
                    val synced = currentCart.mapNotNull { c ->
                        val updated = byId[c.menuItem.id]
                        if (updated == null || updated.hidden || !updated.available) null
                        else c.copy(menuItem = updated)
                    }
                    if (synced != currentCart) {
                        _cart.value = synced
                    }
                }
            }
        }

        // Auto-join deep-linked table from QR URL
        val deepLinkLabel = initialTableCode?.let { TableQrParser.parseTableLabel(it) }
        if (deepLinkLabel != null) {
            viewModelScope.launch {
                val loadedTables = tablesState.filterIsInstance<UiState.Success<List<RestaurantTable>>>()
                    .first { it.data.isNotEmpty() }.data
                val matched = loadedTables.firstOrNull { it.label.equals(deepLinkLabel, ignoreCase = true) }
                if (matched != null && matched.active) {
                    repository.joinTableSession(matched)
                } else if (matched != null && !matched.active) {
                    _selectedTableLabel.value = null
                    _statusBanner.value = "Table ${matched.label} is currently inactive."
                }
            }
        }
    }

    private fun <T> Flow<T>.asUiState(label: String): StateFlow<UiState<T>> {
        return map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("RestaurantVM", "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN
            languagePreferences.setLanguage(next)
        }
    }

    fun switchPortal(portal: AppPortal) {
        val allowed = when (portal) {
            AppPortal.ADMIN_DASHBOARD -> isAdminOrManager.value
            AppPortal.WAITER_DASHBOARD -> canAccessWaiter.value
            AppPortal.KITCHEN_DISPLAY -> canAccessKitchen.value
            else -> true
        }
        if (allowed) {
            _activePortal.value = portal
        } else {
            _statusBanner.value = "Staff authorization required for this section."
        }
    }

    fun selectTableByQrOrLabel(qrOrLabel: String): Boolean {
        val normalized = TableQrParser.parseTableLabel(qrOrLabel)
        if (normalized == null) {
            _statusBanner.value = if (language.value == AppLanguage.AR) {
                "رمز QR غير صالح لطاولات المطعم."
            } else {
                "Invalid table QR code."
            }
            return false
        }

        val tables = (tablesState.value as? UiState.Success)?.data.orEmpty()
        val matchedTable = tables.firstOrNull { it.label.equals(normalized, ignoreCase = true) }
        if (tables.isNotEmpty() && matchedTable == null) {
            _statusBanner.value = if (language.value == AppLanguage.AR) {
                "الطاولة $normalized غير مسجلة في المطعم."
            } else {
                "Table $normalized does not exist."
            }
            return false
        }
        if (matchedTable != null && !matchedTable.active) {
            _statusBanner.value = if (language.value == AppLanguage.AR) {
                "الطاولة ${matchedTable.label} غير مفعلة حالياً. يرجى سؤال النادل."
            } else {
                "Table ${matchedTable.label} is currently inactive. Please ask staff."
            }
            return false
        }

        _selectedTableLabel.value = normalized

        if (matchedTable != null) {
            viewModelScope.launch {
                repository.joinTableSession(matchedTable)
                    .onFailure { Log.w("RestaurantVM", "Failed to mark table session joined", it) }
                repository.syncCurrentUserProfile(
                    displayName = currentUserName,
                    email = currentUserEmail,
                    staffRole = currentStaffRole.value?.name ?: "CUSTOMER",
                    activeTableLabel = normalized
                )
            }
            val activeGame = (gameSessionsState.value as? UiState.Success)?.data
                ?.firstOrNull { it.tableNumber == matchedTable.tableNumber && it.status != "FINISHED" }
            _statusBanner.value = if (language.value == AppLanguage.AR) {
                buildString {
                    append("تم ربط طلباتك بطاولة ${matchedTable.label} (${matchedTable.zone})")
                    append(" • النادل: ${matchedTable.assignedWaiterName}")
                    if (activeGame != null) append(" • غرفة ألعاب: ${activeGame.roomCode}")
                }
            } else {
                buildString {
                    append("Bound to Table ${matchedTable.label} (${matchedTable.zone})")
                    append(" • Waiter: ${matchedTable.assignedWaiterName}")
                    if (activeGame != null) append(" • Game Room ${activeGame.roomCode}")
                }
            }
        } else {
            _statusBanner.value = if (language.value == AppLanguage.AR) {
                "تم ربط طاولة $normalized"
            } else {
                "Bound to Table $normalized"
            }
        }
        return true
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun clearGatewayMessage() {
        _lastGatewayMessage.value = null
    }

    fun addToCart(
        menuItem: MenuItem,
        quantity: Int,
        size: String,
        spice: SpiceLevel,
        extras: List<String>,
        notes: String,
        allergyNote: String
    ) {
        val key = "${menuItem.id}|$size|${spice.name}|${extras.sorted().joinToString(",")}|$notes|$allergyNote"
        val current = _cart.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.cartKey == key }
        if (existingIdx >= 0) {
            val old = current[existingIdx]
            current[existingIdx] = old.copy(quantity = old.quantity + quantity)
        } else {
            current.add(
                CartItem(
                    cartKey = key,
                    menuItem = menuItem,
                    quantity = quantity,
                    selectedSize = size,
                    spiceLevel = spice,
                    selectedExtras = extras,
                    notes = notes,
                    allergyNote = allergyNote
                )
            )
        }
        _cart.value = current
        _statusBanner.value = "${menuItem.nameEn} added to order"
    }

    fun updateCartQuantity(cartKey: String, delta: Int) {
        val current = _cart.value.mapNotNull { item ->
            if (item.cartKey == cartKey) {
                val newQty = item.quantity + delta
                if (newQty <= 0) null else item.copy(quantity = newQty)
            } else item
        }
        _cart.value = current
    }

    fun submitOrder(table: RestaurantTable?, specialInstructions: String, vatPercent: Double) {
        if (_isSubmittingOrder.value) return
        if (table == null) {
            _statusBanner.value = if (language.value == AppLanguage.AR) {
                "يرجى مسح رمز QR الخاص بطاولتك أولاً."
            } else {
                "Please scan your table QR code first before ordering."
            }
            return
        }
        if (!table.active) {
            _statusBanner.value = "Table ${table.label} is inactive."
            return
        }
        val items = _cart.value
        if (items.isEmpty()) return

        _isSubmittingOrder.value = true
        val requestId = UUID.randomUUID().toString()
        viewModelScope.launch {
            try {
                val res = repository.submitCustomerOrder(
                    table = table,
                    customerName = currentUserName,
                    cartItems = items,
                    specialInstructions = specialInstructions,
                    discountSar = _loyaltyDiscountSar.value,
                    vatPercent = vatPercent,
                    clientRequestId = requestId
                )
                res.onSuccess { orderNumber ->
                    _cart.value = emptyList()
                    _loyaltyDiscountSar.value = 0.0
                    _statusBanner.value = "Order $orderNumber placed for Table ${table.label}!"
                }.onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Failed to submit order"
                }
            } finally {
                _isSubmittingOrder.value = false
            }
        }
    }

    fun confirmOrderToKitchen(order: Order) {
        viewModelScope.launch {
            repository.confirmOrderToKitchen(order, currentUserName)
                .onSuccess { _statusBanner.value = "Order ${order.orderNumber} confirmed & sent to kitchen stations!" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to confirm order" }
        }
    }

    fun sendServiceRequest(table: RestaurantTable?, type: ServiceRequestType, note: String = "") {
        if (table == null) {
            _statusBanner.value = if (language.value == AppLanguage.AR) {
                "يرجى مسح رمز QR الخاص بطاولتك أولاً."
            } else {
                "Please scan your table QR code first."
            }
            return
        }
        viewModelScope.launch {
            repository.createServiceRequest(table, type, note)
                .onSuccess { _statusBanner.value = "${type.labelEn} sent to ${table.assignedWaiterName}" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Request failed" }
        }
    }

    fun advanceServiceRequest(request: ServiceRequest, status: RequestStatus) {
        viewModelScope.launch {
            repository.updateServiceRequestStatus(request, status)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun setKdsStationFilter(station: KitchenStation?) {
        _selectedStation.value = station
    }

    fun advanceOrderStatus(order: Order, nextStatus: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(order, nextStatus)
                .onSuccess { _statusBanner.value = "${order.orderNumber} -> ${nextStatus.labelEn}" }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun updateTableState(tableId: String, status: TableStatus, waiterName: String? = null) {
        viewModelScope.launch {
            repository.updateTableStatus(tableId, status, waiterName)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun processPayment(
        order: Order,
        amountSar: Double,
        tipSar: Double,
        method: PaymentMethod,
        splitMode: SplitMode,
        settings: RestaurantSettings,
        gatewaySecretToken: String,
        receiptEmail: String?,
        selectedItemIds: List<String>
    ) {
        viewModelScope.launch {
            when (method) {
                PaymentMethod.CASH_REQUEST -> {
                    val txId = "CASH-REQ-${System.currentTimeMillis()}"
                    repository.recordPaymentTransaction(
                        order = order,
                        amountSar = amountSar,
                        tipSar = tipSar,
                        method = method,
                        splitMode = splitMode,
                        gatewayProvider = "CASH_DESK",
                        transactionId = txId,
                        status = "PENDING_CASH_COLLECTION",
                        receiptEmail = receiptEmail,
                        selectedItemIds = selectedItemIds
                    ).onSuccess {
                        _lastGatewayMessage.value = "Waiter & Cashier notified to collect %.2f SAR cash at Table T%02d.".format(amountSar, order.tableNumber)
                    }.onFailure {
                        _lastGatewayMessage.value = it.localizedMessage
                    }
                }
                PaymentMethod.BANK_TRANSFER -> {
                    val txId = "BANK-REF-${(100000..999999).random()}"
                    repository.recordPaymentTransaction(
                        order = order,
                        amountSar = amountSar,
                        tipSar = tipSar,
                        method = method,
                        splitMode = splitMode,
                        gatewayProvider = "SAUDI_BANK_IBAN",
                        transactionId = txId,
                        status = "PENDING_BANK_TRANSFER",
                        receiptEmail = receiptEmail,
                        selectedItemIds = selectedItemIds
                    ).onSuccess {
                        _lastGatewayMessage.value = "Bank transfer reference $txId recorded. Cashier verification pending."
                    }.onFailure {
                        _lastGatewayMessage.value = it.localizedMessage
                    }
                }
                PaymentMethod.MADA, PaymentMethod.APPLE_PAY, PaymentMethod.GOOGLE_PAY, PaymentMethod.CARD -> {
                    val result = gatewayClient.verifyOrCreatePayment(
                        provider = settings.gatewayProvider,
                        endpoint = settings.gatewayEndpoint,
                        publicKey = settings.gatewayPublicKey,
                        secretOrToken = gatewaySecretToken,
                        amountSar = amountSar,
                        orderNumber = order.orderNumber,
                        methodCode = method.name
                    )
                    when (result) {
                        is GatewayVerificationResult.VerifiedPaid -> {
                            repository.recordPaymentTransaction(
                                order = order,
                                amountSar = amountSar,
                                tipSar = tipSar,
                                method = method,
                                splitMode = splitMode,
                                gatewayProvider = result.provider,
                                transactionId = result.transactionId,
                                status = "VERIFIED_PAID",
                                receiptEmail = receiptEmail,
                                selectedItemIds = selectedItemIds
                            ).onSuccess {
                                _lastGatewayMessage.value = "Verified ${result.provider} payment confirmed! Tx: ${result.transactionId}"
                            }
                        }
                        is GatewayVerificationResult.ConfigurationRequired -> {
                            _lastGatewayMessage.value = result.message
                        }
                        is GatewayVerificationResult.Failed -> {
                            _lastGatewayMessage.value = result.reason
                        }
                    }
                }
            }
        }
    }

    fun verifyCashOrBankPayment(payment: PaymentRecord, order: Order?) {
        viewModelScope.launch {
            repository.confirmCashOrBankPayment(payment, order)
                .onSuccess { _statusBanner.value = "Payment confirmed & receipt finalized." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun redeemReward() {
        viewModelScope.launch {
            repository.redeemLoyaltyPoints(150)
                .onSuccess {
                    _loyaltyDiscountSar.value = 15.0
                    _statusBanner.value = "15 SAR loyalty discount applied to your next order!"
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Not enough points (need 150)" }
        }
    }

    fun saveBirthday(birthdayMmDd: String) {
        viewModelScope.launch {
            repository.updateLoyaltyBirthday(birthdayMmDd)
                .onSuccess { _statusBanner.value = "Birthday reward date saved!" }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun submitFeedback(orderId: String?, tableNumber: Int, overall: Int, food: Int, service: Int, comment: String) {
        viewModelScope.launch {
            repository.submitReview(orderId, tableNumber, currentUserName, overall, food, service, comment)
                .onSuccess { _statusBanner.value = "Shukran! Thank you for your review." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun startTableGame(tableNumber: Int, type: GameType) {
        viewModelScope.launch {
            repository.createGameSession(tableNumber, type, currentUserName)
                .onSuccess { _statusBanner.value = "Table game room created!" }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun answerGameQuestion(session: GameSession, isCorrect: Boolean, advance: Boolean) {
        viewModelScope.launch {
            repository.joinAndAnswerGame(
                session = session,
                playerName = currentUserName,
                pointsDelta = if (isCorrect) 25L else 0L,
                advanceQuestion = advance
            )
        }
    }

    fun uploadDishPhoto(
        context: Context,
        uri: Uri,
        productId: String?,
        onUploaded: (imageId: String, imageUrl: String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val compressed = QrCardExporter.compressImageUriToProductImage(context, uri)
            compressed.onSuccess { result ->
                repository.uploadProductImage(result, productId)
                    .onSuccess { imageId ->
                        onUploaded(imageId, "cloud_img://$imageId")
                        _statusBanner.value = "Food photo uploaded to cloud storage!"
                    }
                    .onFailure { err ->
                        onError(err.localizedMessage ?: "Cloud upload failed.")
                    }
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Failed to process selected photo.")
            }
        }
    }

    fun saveMenuItem(item: MenuItem) {
        viewModelScope.launch {
            repository.saveMenuItem(item)
                .onSuccess { _statusBanner.value = "${item.nameEn} saved & synced to live menu." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun deleteMenuItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteMenuItem(itemId)
                .onSuccess { _statusBanner.value = "Menu item deleted." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun addOrUpdateTable(tableNumber: Int, zone: String, capacity: Int, waiterName: String, active: Boolean = true) {
        viewModelScope.launch {
            repository.saveTable(tableNumber, zone, capacity, waiterName, active)
                .onSuccess { _statusBanner.value = "Table T%02d saved with unique QR.".format(tableNumber) }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun toggleTableActive(table: RestaurantTable) {
        viewModelScope.launch {
            repository.toggleTableActive(table)
                .onSuccess {
                    val stateWord = if (!table.active) "activated" else "deactivated"
                    _statusBanner.value = "Table ${table.label} $stateWord."
                }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun deleteTable(table: RestaurantTable) {
        viewModelScope.launch {
            repository.deleteTable(table)
                .onSuccess { _statusBanner.value = "Table ${table.label} deleted." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun downloadSingleQr(context: Context, table: RestaurantTable) {
        viewModelScope.launch {
            QrCardExporter.downloadSingleQrPng(context, table)
                .onSuccess { uri ->
                    _statusBanner.value = "QR saved for Table ${table.label} (Pictures/AsilAlTawooq_QR)."
                    QrCardExporter.shareOrOpenUri(context, uri, "image/png", "Share Table ${table.label} QR")
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "QR export failed" }
        }
    }

    fun downloadAllQrPdf(context: Context, tables: List<RestaurantTable>) {
        viewModelScope.launch {
            QrCardExporter.downloadAllQrPdf(context, tables)
                .onSuccess { uri ->
                    _statusBanner.value = "All ${tables.size} table QR cards exported to Downloads PDF!"
                    QrCardExporter.shareOrOpenUri(context, uri, "application/pdf", "Share All Table QR Codes (PDF)")
                }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "PDF export failed" }
        }
    }

    fun printQrTables(context: Context, tables: List<RestaurantTable>) {
        QrCardExporter.printQrTables(context, tables)
    }

    fun addOrUpdateStaff(docId: String, name: String, email: String, role: StaffRole, zone: String, active: Boolean = true) {
        viewModelScope.launch {
            repository.saveStaffMember(docId, name, email, role, zone, active)
                .onSuccess { _statusBanner.value = "Staff member $name saved." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun toggleStaffActive(member: StaffMember) {
        viewModelScope.launch {
            repository.toggleStaffActive(member)
                .onSuccess { _statusBanner.value = "${member.name} updated." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun deleteStaffMember(staffDocId: String) {
        viewModelScope.launch {
            repository.deleteStaffMember(staffDocId)
                .onSuccess { _statusBanner.value = "Staff member removed." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun saveRestaurantSettings(settings: RestaurantSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
                .onSuccess { _statusBanner.value = "Restaurant & Gateway settings saved." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun seedDemoOrder(tables: List<RestaurantTable>) {
        viewModelScope.launch {
            repository.seedDemoOrdersForTesting(tables)
                .onSuccess { _statusBanner.value = "Demo test order created (tagged demoData=true)." }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun generateBackupJson(
        menu: List<MenuItem>,
        tables: List<RestaurantTable>,
        orders: List<Order>,
        settings: RestaurantSettings
    ): String = repository.exportBackupJson(menu, tables, orders, settings)
}
