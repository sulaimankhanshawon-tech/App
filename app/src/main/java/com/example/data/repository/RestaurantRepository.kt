package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.BillStatus
import com.example.data.model.CartItem
import com.example.data.model.CategoryEntity
import com.example.data.model.GameSession
import com.example.data.model.GameType
import com.example.data.model.KitchenStationEntity
import com.example.data.model.LoyaltyProfile
import com.example.data.model.MenuItem
import com.example.data.model.NotificationItem
import com.example.data.model.OWNER_BOOTSTRAP_EMAIL
import com.example.data.model.Order
import com.example.data.model.OrderItemRecord
import com.example.data.model.OrderPriority
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.ProductImage
import com.example.data.model.RESTAURANT_ID
import com.example.data.model.RequestStatus
import com.example.data.model.RestaurantSettings
import com.example.data.model.RestaurantTable
import com.example.data.model.Review
import com.example.data.model.ServiceRequest
import com.example.data.model.ServiceRequestType
import com.example.data.model.SplitMode
import com.example.data.model.StaffMember
import com.example.data.model.StaffRole
import com.example.data.model.TableQrCode
import com.example.data.model.TableStatus
import com.example.util.CompressedImageResult
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

class RestaurantRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun isOwnerAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && user.email.equals(OWNER_BOOTSTRAP_EMAIL, ignoreCase = true)
    }

    private fun <T> Flow<T>.logAndRethrow(path: String, expectedUid: String): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
    }

    fun observeMyStaffMember(uid: String, email: String): Flow<StaffMember?> = flow {
        val currentUid = requireUserId()
        if (isOwnerAdmin()) {
            emit(
                StaffMember(
                    id = currentUid,
                    uid = currentUid,
                    restaurantId = RESTAURANT_ID,
                    name = auth.currentUser?.displayName ?: "Owner Admin",
                    email = email.ifBlank { OWNER_BOOTSTRAP_EMAIL },
                    role = StaffRole.OWNER.name,
                    active = true,
                    assignedZone = "All Zones"
                )
            )
            return@flow
        }

        val uidDocFlow: Flow<StaffMember?> = db.collection("staff").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(StaffMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                else null
            }
            .catch { emit(null) }

        val cleanEmail = email.trim().lowercase()
        val emailDocFlow: Flow<StaffMember?> = if (cleanEmail.isNotBlank()) {
            db.collection("staff").document(cleanEmail)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(StaffMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { emit(null) }
        } else {
            flowOf(null)
        }

        emitAll(
            combine(uidDocFlow, emailDocFlow) { byUid, byEmail ->
                val resolved = listOfNotNull(byUid, byEmail).firstOrNull { it.active }
                resolved
            }
        )
    }

    fun observeMenuItems(): Flow<List<MenuItem>> = flow {
        val uid = requireUserId()
        val path = "menu_items"
        emitAll(
            db.collection(path)
                .whereEqualTo("restaurantId", RESTAURANT_ID)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MenuItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.nameEn }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeProductImages(): Flow<List<ProductImage>> = flow {
        val uid = requireUserId()
        val path = "product_images"
        emitAll(
            db.collection(path)
                .whereEqualTo("restaurantId", RESTAURANT_ID)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ProductImage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeCategories(): Flow<List<CategoryEntity>> = flow {
        val uid = requireUserId()
        val path = "categories"
        emitAll(
            db.collection(path)
                .whereEqualTo("restaurantId", RESTAURANT_ID)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CategoryEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeKitchenStations(): Flow<List<KitchenStationEntity>> = flow {
        val uid = requireUserId()
        val path = "kitchen_stations"
        emitAll(
            db.collection(path)
                .whereEqualTo("restaurantId", RESTAURANT_ID)
                .snapshots()
                .map { snap ->
                    snap.toObjects(KitchenStationEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeTables(): Flow<List<RestaurantTable>> = flow {
        val uid = requireUserId()
        val path = "restaurant_tables"
        emitAll(
            db.collection(path)
                .whereEqualTo("restaurantId", RESTAURANT_ID)
                .snapshots()
                .map { snap ->
                    snap.toObjects(RestaurantTable::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.tableNumber }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeQrCodes(): Flow<List<TableQrCode>> = flow {
        val uid = requireUserId()
        val path = "qr_codes"
        emitAll(
            db.collection(path)
                .whereEqualTo("restaurantId", RESTAURANT_ID)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TableQrCode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.tableNumber }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeOrders(isStaffOrAdmin: Boolean): Flow<List<Order>> = flow {
        val uid = requireUserId()
        val path = "orders"
        val query = if (isStaffOrAdmin) {
            db.collection(path).whereEqualTo("restaurantId", RESTAURANT_ID)
        } else {
            db.collection(path).whereEqualTo("userId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeOrderItems(isStaffOrAdmin: Boolean): Flow<List<OrderItemRecord>> = flow {
        val uid = requireUserId()
        val path = "order_items"
        val query = if (isStaffOrAdmin) {
            db.collection(path).whereEqualTo("restaurantId", RESTAURANT_ID)
        } else {
            db.collection(path).whereEqualTo("userId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(OrderItemRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeServiceRequests(isStaffOrAdmin: Boolean): Flow<List<ServiceRequest>> = flow {
        val uid = requireUserId()
        val path = "service_requests"
        val query = if (isStaffOrAdmin) {
            db.collection(path).whereEqualTo("restaurantId", RESTAURANT_ID)
        } else {
            db.collection(path).whereEqualTo("userId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(ServiceRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observePayments(isStaffOrAdmin: Boolean): Flow<List<PaymentRecord>> = flow {
        val uid = requireUserId()
        val path = "payments"
        val query = if (isStaffOrAdmin) {
            db.collection(path).whereEqualTo("restaurantId", RESTAURANT_ID)
        } else {
            db.collection(path).whereEqualTo("userId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeLoyaltyProfile(userId: String): Flow<LoyaltyProfile?> = flow {
        val uid = requireUserId()
        val path = "loyalty_profiles/$userId"
        emitAll(
            db.collection("loyalty_profiles").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(LoyaltyProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observePublishedReviews(): Flow<List<Review>> = flow {
        val uid = requireUserId()
        val path = "reviews"
        emitAll(
            db.collection(path)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeGameSessions(): Flow<List<GameSession>> = flow {
        val uid = requireUserId()
        val path = "game_sessions"
        emitAll(
            db.collection(path)
                .whereEqualTo("restaurantId", RESTAURANT_ID)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GameSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeSettings(): Flow<RestaurantSettings> = flow {
        val uid = requireUserId()
        val path = "restaurant_settings/config"
        emitAll(
            db.collection("restaurant_settings").document("config")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(RestaurantSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: RestaurantSettings()
                    } else {
                        RestaurantSettings()
                    }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeStaffMembers(isAdminOrManager: Boolean): Flow<List<StaffMember>> = flow {
        val uid = requireUserId()
        val path = "staff"
        val query = if (isAdminOrManager || isOwnerAdmin()) {
            db.collection(path).whereEqualTo("restaurantId", RESTAURANT_ID)
        } else {
            db.collection(path).whereEqualTo("uid", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(StaffMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name }
                }
                .logAndRethrow(path, uid)
        )
    }

    fun observeNotifications(isStaffOrAdmin: Boolean): Flow<List<NotificationItem>> = flow {
        val uid = requireUserId()
        val path = "notifications"
        val query = if (isStaffOrAdmin) {
            db.collection(path).whereEqualTo("restaurantId", RESTAURANT_ID)
        } else {
            db.collection(path).whereEqualTo("targetUserId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logAndRethrow(path, uid)
        )
    }

    suspend fun syncCurrentUserProfile(
        displayName: String,
        email: String?,
        staffRole: String,
        activeTableLabel: String?
    ): Result<Unit> = runCatchingFirestore(OperationType.WRITE, "users") {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snap = docRef.get().await()
        val safeName = displayName.ifBlank { "Jeddah Guest" }.take(120)
        val safeEmail = email?.takeIf { it.isNotBlank() }?.take(150)
        val safeTable = activeTableLabel?.takeIf { it.isNotBlank() }?.take(30)

        if (!snap.exists()) {
            val payload = mapOf(
                "uid" to uid,
                "restaurantId" to RESTAURANT_ID,
                "displayName" to safeName,
                "email" to safeEmail,
                "staffRole" to staffRole,
                "activeTableLabel" to safeTable,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
        } else {
            val updates = mutableMapOf<String, Any>(
                "displayName" to safeName,
                "staffRole" to staffRole,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (safeEmail != null) updates["email"] = safeEmail
            if (safeTable != null) updates["activeTableLabel"] = safeTable
            docRef.update(updates).await()
        }
    }

    suspend fun ensureLoyaltyProfile(displayName: String, email: String?): Result<Unit> = runCatchingFirestore(OperationType.CREATE, "loyalty_profiles") {
        val uid = requireUserId()
        val docRef = db.collection("loyalty_profiles").document(uid)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "restaurantId" to RESTAURANT_ID,
                "displayName" to displayName.ifBlank { "Jeddah Guest" }.take(100),
                "email" to email?.take(150),
                "points" to 60,
                "totalSpentSar" to 0.0,
                "tier" to "BRONZE",
                "redeemedRewards" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
        }
    }

    suspend fun bootstrapRestaurantIfNeeded(): Result<Unit> = runCatchingFirestore(OperationType.WRITE, "menu_items") {
        requireUserId()
        if (!isOwnerAdmin()) return@runCatchingFirestore

        // 1. Roles
        val rolesExisting = db.collection("roles").whereEqualTo("restaurantId", RESTAURANT_ID).limit(1).get().await()
        if (rolesExisting.isEmpty) {
            val batch = db.batch()
            DefaultSeedData.defaultRolePayloads().forEach { (id, map) ->
                batch.set(db.collection("roles").document(id), map)
            }
            batch.commit().await()
        }

        // 2. Categories
        val categoriesExisting = db.collection("categories").whereEqualTo("restaurantId", RESTAURANT_ID).limit(1).get().await()
        if (categoriesExisting.isEmpty) {
            val batch = db.batch()
            DefaultSeedData.defaultCategoryPayloads().forEach { (id, map) ->
                batch.set(db.collection("categories").document(id), map)
            }
            batch.commit().await()
        }

        // 3. Kitchen Stations
        val stationsExisting = db.collection("kitchen_stations").whereEqualTo("restaurantId", RESTAURANT_ID).limit(1).get().await()
        if (stationsExisting.isEmpty) {
            val batch = db.batch()
            DefaultSeedData.defaultKitchenStationPayloads().forEach { (id, map) ->
                batch.set(db.collection("kitchen_stations").document(id), map)
            }
            batch.commit().await()
        }

        // 4. Menu Items
        val menuExisting = db.collection("menu_items")
            .whereEqualTo("restaurantId", RESTAURANT_ID)
            .limit(1)
            .get()
            .await()
        if (menuExisting.isEmpty) {
            val batch = db.batch()
            DefaultSeedData.defaultMenuPayloads().forEach { (id, map) ->
                batch.set(db.collection("menu_items").document(id), map)
            }
            batch.commit().await()
        }

        // 5. Restaurant Tables (T01-T04) & QR Codes
        val tablesExisting = db.collection("restaurant_tables")
            .whereEqualTo("restaurantId", RESTAURANT_ID)
            .limit(1)
            .get()
            .await()
        if (tablesExisting.isEmpty) {
            val batch = db.batch()
            DefaultSeedData.defaultTablePayloads().forEach { (id, map) ->
                batch.set(db.collection("restaurant_tables").document(id), map)
            }
            DefaultSeedData.defaultQrCodePayloads().forEach { (id, map) ->
                batch.set(db.collection("qr_codes").document(id), map)
            }
            batch.commit().await()
        } else {
            val qrExisting = db.collection("qr_codes").whereEqualTo("restaurantId", RESTAURANT_ID).limit(1).get().await()
            if (qrExisting.isEmpty) {
                val batch = db.batch()
                DefaultSeedData.defaultQrCodePayloads().forEach { (id, map) ->
                    batch.set(db.collection("qr_codes").document(id), map)
                }
                batch.commit().await()
            }
        }

        // 6. Restaurant Settings
        val settingsRef = db.collection("restaurant_settings").document("config")
        if (!settingsRef.get().await().exists()) {
            settingsRef.set(
                mapOf(
                    "restaurantId" to RESTAURANT_ID,
                    "restaurantNameEn" to "Asil Al Tawooq",
                    "restaurantNameAr" to "أصيل الطاووق",
                    "city" to "Jeddah, Saudi Arabia",
                    "wifiSsid" to "AsilAlTawooq_Guest_5G",
                    "wifiPassword" to "TawooqJeddah2026",
                    "vatPercent" to 15.0,
                    "openingHours" to "Daily 12:00 PM – 2:00 AM",
                    "googleReviewUrl" to "https://www.google.com/maps/search/?api=1&query=Asil+Al+Tawooq+Jeddah",
                    "gatewayProvider" to "MOYASAR",
                    "gatewayPublicKey" to "",
                    "gatewayMode" to "TEST",
                    "gatewayEndpoint" to "https://api.moyasar.com/v1/payments",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        // 7. Bootstrapped Owner staff doc
        val uid = requireUserId()
        val staffRef = db.collection("staff").document(uid)
        if (!staffRef.get().await().exists()) {
            staffRef.set(
                mapOf(
                    "uid" to uid,
                    "restaurantId" to RESTAURANT_ID,
                    "name" to (auth.currentUser?.displayName ?: "Owner Admin"),
                    "email" to (auth.currentUser?.email ?: OWNER_BOOTSTRAP_EMAIL),
                    "role" to StaffRole.OWNER.name,
                    "active" to true,
                    "assignedZone" to "All Zones",
                    "ordersHandled" to 0,
                    "avgResponseSec" to 30,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun uploadProductImage(
        compressed: CompressedImageResult,
        productId: String? = null
    ): Result<String> = runCatchingFirestore(OperationType.CREATE, "product_images") {
        val uid = requireUserId()
        val docRef = db.collection("product_images").document()
        val payload = mutableMapOf<String, Any>(
            "restaurantId" to RESTAURANT_ID,
            "mimeType" to compressed.mimeType,
            "width" to compressed.width,
            "height" to compressed.height,
            "sizeBytes" to compressed.sizeBytes,
            "base64Data" to compressed.base64Data,
            "uploadedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!productId.isNullOrBlank()) {
            payload["productId"] = productId.take(128)
        }
        docRef.set(payload).await()
        docRef.id
    }

    suspend fun seedDemoOrdersForTesting(tables: List<RestaurantTable>): Result<Unit> = runCatchingFirestore(OperationType.CREATE, "orders") {
        val uid = requireUserId()
        val targetTable = tables.firstOrNull { it.active } ?: tables.firstOrNull() ?: error("No tables found.")
        val orderRef = db.collection("orders").document()
        val demoItems = listOf(
            mapOf(
                "itemId" to "menu_tawook_royal",
                "nameEn" to "Royal Charcoal Chicken Tawook",
                "nameAr" to "طاووق دجاج مشوي على الفحم الملكي",
                "kitchenStation" to "GRILL",
                "quantity" to 2,
                "unitPriceSar" to 46.0,
                "selectedSize" to "Regular",
                "spiceLevel" to "MILD",
                "extras" to listOf("Extra Garlic Toum +4 SAR"),
                "notes" to "Demo test ticket",
                "allergyNote" to "Dairy free saj",
                "itemStatus" to OrderStatus.PREPARING.name,
                "prepMinutes" to 14
            ),
            mapOf(
                "itemId" to "menu_drink_mint",
                "nameEn" to "Jeddah Fresh Mint Lemonade",
                "nameAr" to "ليمون بالنعناع الطازج المنعش",
                "kitchenStation" to "DRINKS",
                "quantity" to 2,
                "unitPriceSar" to 16.0,
                "selectedSize" to "Regular",
                "spiceLevel" to "MILD",
                "extras" to emptyList<String>(),
                "notes" to "",
                "allergyNote" to "",
                "itemStatus" to OrderStatus.READY.name,
                "prepMinutes" to 4
            )
        )
        orderRef.set(
            mapOf(
                "userId" to uid,
                "restaurantId" to RESTAURANT_ID,
                "tableId" to targetTable.id,
                "tableNumber" to targetTable.tableNumber,
                "orderNumber" to "DEMO-${(1000..9999).random()}",
                "customerName" to "Demo Guest",
                "status" to OrderStatus.PREPARING.name,
                "priority" to OrderPriority.HIGH.name,
                "waiterConfirmed" to true,
                "waiterConfirmedBy" to targetTable.assignedWaiterName,
                "items" to demoItems,
                "specialInstructions" to "DEMO DATA: Testing kitchen & billing sync",
                "allergySummary" to "Dairy sensitivity",
                "subtotalSar" to 132.0,
                "vatSar" to 19.8,
                "discountSar" to 0.0,
                "tipSar" to 0.0,
                "totalSar" to 151.8,
                "paidAmountSar" to 0.0,
                "paymentStatus" to "UNPAID",
                "assignedWaiterName" to targetTable.assignedWaiterName,
                "demoData" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun submitCustomerOrder(
        table: RestaurantTable,
        customerName: String,
        cartItems: List<CartItem>,
        specialInstructions: String,
        discountSar: Double,
        vatPercent: Double,
        clientRequestId: String
    ): Result<String> = runCatchingFirestore(OperationType.CREATE, "orders") {
        val uid = requireUserId()
        require(table.active) { "Table ${table.label} is currently deactivated." }
        require(cartItems.isNotEmpty()) { "Cart cannot be empty." }

        val orderRef = db.collection("orders").document()
        val orderNumber = "AAT-${(1000..9999).random()}"
        val subtotal = cartItems.sumOf { it.lineTotal() }
        val taxable = (subtotal - discountSar).coerceAtLeast(0.0)
        val vatSar = (taxable * (vatPercent / 100.0)).let { Math.round(it * 100.0) / 100.0 }
        val totalSar = (taxable + vatSar).let { Math.round(it * 100.0) / 100.0 }

        val lineMaps = cartItems.map { cart ->
            mapOf(
                "itemId" to cart.menuItem.id,
                "nameEn" to cart.menuItem.nameEn.take(100),
                "nameAr" to cart.menuItem.nameAr.take(100),
                "kitchenStation" to cart.menuItem.kitchenStation,
                "quantity" to cart.quantity,
                "unitPriceSar" to cart.menuItem.priceSar,
                "selectedSize" to cart.selectedSize,
                "spiceLevel" to cart.spiceLevel.name,
                "extras" to cart.selectedExtras,
                "notes" to cart.notes.take(200),
                "allergyNote" to cart.allergyNote.take(150),
                "itemStatus" to OrderStatus.NEW_ORDER.name,
                "prepMinutes" to cart.menuItem.prepMinutes
            )
        }

        val allergySummary = cartItems.mapNotNull {
            val parts = buildList {
                if (it.menuItem.allergens.isNotEmpty()) add("${it.menuItem.nameEn}: ${it.menuItem.allergens.joinToString()}")
                if (it.allergyNote.isNotBlank()) add(it.allergyNote)
            }
            if (parts.isEmpty()) null else parts.joinToString(" • ")
        }.joinToString(" | ").take(300)

        val payload = mapOf(
            "userId" to uid,
            "restaurantId" to RESTAURANT_ID,
            "tableId" to table.id,
            "tableNumber" to table.tableNumber,
            "orderNumber" to orderNumber,
            "clientRequestId" to clientRequestId.take(80),
            "customerName" to customerName.ifBlank { "Table ${table.tableNumber} Guest" }.take(100),
            "status" to OrderStatus.NEW_ORDER.name,
            "priority" to OrderPriority.NORMAL.name,
            "waiterConfirmed" to false,
            "items" to lineMaps,
            "specialInstructions" to specialInstructions.takeIf { it.isNotBlank() }?.take(400),
            "allergySummary" to allergySummary.takeIf { it.isNotBlank() },
            "subtotalSar" to subtotal,
            "vatSar" to vatSar,
            "discountSar" to discountSar,
            "tipSar" to 0.0,
            "totalSar" to totalSar,
            "paidAmountSar" to 0.0,
            "paymentStatus" to "UNPAID",
            "assignedWaiterName" to table.assignedWaiterName,
            "demoData" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>

        val batch = db.batch()
        batch.set(orderRef, payload)

        cartItems.forEachIndexed { idx, cart ->
            val itemDocRef = db.collection("order_items").document("${orderRef.id}_$idx")
            batch.set(
                itemDocRef,
                mapOf(
                    "orderId" to orderRef.id,
                    "userId" to uid,
                    "restaurantId" to RESTAURANT_ID,
                    "tableId" to table.id,
                    "tableNumber" to table.tableNumber,
                    "productId" to cart.menuItem.id.ifBlank { "custom_$idx" },
                    "nameEn" to cart.menuItem.nameEn.take(120),
                    "nameAr" to cart.menuItem.nameAr.ifBlank { cart.menuItem.nameEn }.take(120),
                    "kitchenStation" to cart.menuItem.kitchenStation,
                    "quantity" to cart.quantity,
                    "unitPriceSar" to cart.menuItem.priceSar,
                    "lineTotalSar" to cart.lineTotal(),
                    "selectedSize" to cart.selectedSize,
                    "spiceLevel" to cart.spiceLevel.name,
                    "extras" to cart.selectedExtras,
                    "notes" to cart.notes.take(200),
                    "allergyNote" to cart.allergyNote.take(150),
                    "itemStatus" to OrderStatus.NEW_ORDER.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.update(
            db.collection("restaurant_tables").document(table.id),
            mapOf(
                "status" to TableStatus.ORDERING.name,
                "currentOrderId" to orderRef.id,
                "billStatus" to BillStatus.OPEN.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()

        createNotificationInternal(
            targetRole = "WAITER",
            targetUserId = uid,
            title = "New Order $orderNumber • ${table.label}",
            message = "${cartItems.sumOf { it.quantity }} items placed for ${table.label} (${table.zone}).",
            category = "ORDER",
            tableNumber = table.tableNumber
        )

        orderNumber
    }

    suspend fun confirmOrderToKitchen(order: Order, waiterName: String): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "orders/${order.id}") {
            val uid = requireUserId()
            val updatedItems = order.items.map { item ->
                mapOf(
                    "itemId" to item.itemId,
                    "nameEn" to item.nameEn,
                    "nameAr" to item.nameAr,
                    "kitchenStation" to item.kitchenStation,
                    "quantity" to item.quantity,
                    "unitPriceSar" to item.unitPriceSar,
                    "selectedSize" to item.selectedSize,
                    "spiceLevel" to item.spiceLevel,
                    "extras" to item.extras,
                    "notes" to item.notes,
                    "allergyNote" to item.allergyNote,
                    "itemStatus" to OrderStatus.ACCEPTED.name,
                    "prepMinutes" to item.prepMinutes
                )
            }
            val batch = db.batch()
            batch.update(
                db.collection("orders").document(order.id),
                mapOf(
                    "status" to OrderStatus.ACCEPTED.name,
                    "waiterConfirmed" to true,
                    "waiterConfirmedBy" to waiterName.ifBlank { "Waiter" }.take(100),
                    "items" to updatedItems,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            order.items.indices.forEach { idx ->
                val itemDocRef = db.collection("order_items").document("${order.id}_$idx")
                // Update normalized order_items if it exists (safe best-effort outside batch or separate try)
                runCatching {
                    itemDocRef.update(
                        mapOf(
                            "itemStatus" to OrderStatus.ACCEPTED.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            if (order.tableId.isNotBlank()) {
                batch.update(
                    db.collection("restaurant_tables").document(order.tableId),
                    mapOf(
                        "status" to TableStatus.FOOD_PREP.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            createNotificationInternal(
                targetRole = "KITCHEN",
                targetUserId = uid,
                title = "Confirmed Order ${order.orderNumber} • T%02d".format(order.tableNumber),
                message = "Confirmed by $waiterName • ${order.items.sumOf { it.quantity }} items ready for prep.",
                category = "KITCHEN",
                tableNumber = order.tableNumber
            )
        }

    suspend fun createServiceRequest(
        table: RestaurantTable,
        type: ServiceRequestType,
        note: String
    ): Result<Unit> = runCatchingFirestore(OperationType.CREATE, "service_requests") {
        val uid = requireUserId()
        require(table.active) { "Table ${table.label} is currently deactivated." }
        val reqRef = db.collection("service_requests").document()
        val payload = mapOf(
            "userId" to uid,
            "restaurantId" to RESTAURANT_ID,
            "tableId" to table.id,
            "tableNumber" to table.tableNumber,
            "requestType" to type.name,
            "note" to note.takeIf { it.isNotBlank() }?.take(300),
            "status" to RequestStatus.PENDING.name,
            "assignedWaiterName" to table.assignedWaiterName,
            "demoData" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>

        reqRef.set(payload).await()

        val nextTableStatus = if (type == ServiceRequestType.BILL) TableStatus.BILL_REQUESTED else TableStatus.NEEDS_ASSISTANCE
        val nextBillStatus = if (type == ServiceRequestType.BILL) BillStatus.REQUESTED.name else table.billStatus
        db.collection("restaurant_tables").document(table.id).update(
            mapOf(
                "status" to nextTableStatus.name,
                "billStatus" to nextBillStatus,
                "needsAssistance" to true,
                "activeRequestCount" to FieldValue.increment(1),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        createNotificationInternal(
            targetRole = "WAITER",
            targetUserId = uid,
            title = "${type.labelEn} • ${table.label}",
            message = note.ifBlank { "Guest at ${table.label} needs ${type.labelEn}." }.take(300),
            category = if (type == ServiceRequestType.BILL) "BILL" else "REQUEST",
            tableNumber = table.tableNumber
        )
    }

    suspend fun updateServiceRequestStatus(request: ServiceRequest, newStatus: RequestStatus): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "service_requests/${request.id}") {
            requireUserId()
            db.collection("service_requests").document(request.id).update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (newStatus == RequestStatus.COMPLETED) {
                db.collection("restaurant_tables").document(request.tableId).update(
                    mapOf(
                        "needsAssistance" to false,
                        "status" to TableStatus.OCCUPIED.name,
                        "activeRequestCount" to 0,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

    suspend fun updateOrderStatus(order: Order, newStatus: OrderStatus): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "orders/${order.id}") {
            val uid = requireUserId()
            val updatedItems = order.items.map { item ->
                mapOf(
                    "itemId" to item.itemId,
                    "nameEn" to item.nameEn,
                    "nameAr" to item.nameAr,
                    "kitchenStation" to item.kitchenStation,
                    "quantity" to item.quantity,
                    "unitPriceSar" to item.unitPriceSar,
                    "selectedSize" to item.selectedSize,
                    "spiceLevel" to item.spiceLevel,
                    "extras" to item.extras,
                    "notes" to item.notes,
                    "allergyNote" to item.allergyNote,
                    "itemStatus" to newStatus.name,
                    "prepMinutes" to item.prepMinutes
                )
            }
            val orderUpdates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "items" to updatedItems,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus == OrderStatus.ACCEPTED && !order.waiterConfirmed) {
                orderUpdates["waiterConfirmed"] = true
                orderUpdates["waiterConfirmedBy"] = auth.currentUser?.displayName ?: "Kitchen/Staff"
            }
            db.collection("orders").document(order.id).update(orderUpdates).await()

            val mappedTableStatus = when (newStatus) {
                OrderStatus.NEW_ORDER, OrderStatus.ACCEPTED -> TableStatus.ORDERING
                OrderStatus.PREPARING, OrderStatus.READY -> TableStatus.FOOD_PREP
                OrderStatus.SERVED -> TableStatus.FOOD_SERVED
                OrderStatus.PAID -> TableStatus.AVAILABLE
                OrderStatus.CANCELLED -> TableStatus.OCCUPIED
            }
            if (order.tableId.isNotBlank()) {
                db.collection("restaurant_tables").document(order.tableId).update(
                    mapOf(
                        "status" to mappedTableStatus.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (newStatus == OrderStatus.READY) {
                createNotificationInternal(
                    targetRole = "WAITER",
                    targetUserId = uid,
                    title = "Food Ready • ${order.orderNumber}",
                    message = "Table T%02d order is ready for pickup.".format(order.tableNumber),
                    category = "READY",
                    tableNumber = order.tableNumber
                )
            }
        }

    suspend fun updateTableStatus(tableId: String, status: TableStatus, waiterName: String? = null): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "restaurant_tables/$tableId") {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (waiterName != null) updates["assignedWaiterName"] = waiterName.take(80)
            if (status == TableStatus.AVAILABLE) {
                updates["needsAssistance"] = false
                updates["activeRequestCount"] = 0
                updates["billStatus"] = BillStatus.NONE.name
            }
            db.collection("restaurant_tables").document(tableId).update(updates).await()
        }

    suspend fun joinTableSession(table: RestaurantTable): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "restaurant_tables/${table.id}") {
            requireUserId()
            require(table.active) { "Table ${table.label} is currently deactivated." }
            val nextStatus = if (table.statusEnum() == TableStatus.AVAILABLE) {
                TableStatus.OCCUPIED.name
            } else {
                table.status
            }
            val nextBillStatus = if (table.billEnum() == BillStatus.NONE) {
                BillStatus.OPEN.name
            } else {
                table.billStatus
            }
            db.collection("restaurant_tables").document(table.id).update(
                mapOf(
                    "status" to nextStatus,
                    "billStatus" to nextBillStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun recordPaymentTransaction(
        order: Order,
        amountSar: Double,
        tipSar: Double,
        method: PaymentMethod,
        splitMode: SplitMode,
        gatewayProvider: String,
        transactionId: String,
        status: String,
        receiptEmail: String?,
        selectedItemIds: List<String>
    ): Result<String> = runCatchingFirestore(OperationType.CREATE, "payments") {
        val uid = requireUserId()
        val payRef = db.collection("payments").document()
        val payload = mapOf(
            "userId" to uid,
            "restaurantId" to RESTAURANT_ID,
            "orderId" to order.id,
            "tableNumber" to order.tableNumber,
            "amountSar" to amountSar,
            "vatSar" to order.vatSar,
            "tipSar" to tipSar,
            "method" to method.name,
            "splitMode" to splitMode.name,
            "gatewayProvider" to gatewayProvider.take(40),
            "gatewayTransactionId" to transactionId.take(120),
            "status" to status,
            "receiptEmail" to receiptEmail?.takeIf { it.isNotBlank() }?.take(150),
            "selectedItemIds" to selectedItemIds.take(40),
            "demoData" to order.demoData,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>

        payRef.set(payload).await()

        val newPaid = order.paidAmountSar + amountSar
        val isFullyPaid = status == "VERIFIED_PAID" && newPaid + 0.05 >= (order.totalSar + tipSar)
        val paymentStatus = when {
            status == "VERIFIED_PAID" && isFullyPaid -> "PAID"
            status == "VERIFIED_PAID" -> "PARTIAL"
            else -> order.paymentStatus
        }

        val orderUpdates = mutableMapOf<String, Any>(
            "tipSar" to (order.tipSar + tipSar),
            "paidAmountSar" to if (status == "VERIFIED_PAID") newPaid else order.paidAmountSar,
            "paymentStatus" to paymentStatus,
            "paymentMethod" to method.name,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isFullyPaid) {
            orderUpdates["status"] = OrderStatus.PAID.name
        }
        db.collection("orders").document(order.id).update(orderUpdates).await()

        if (isFullyPaid && order.tableId.isNotBlank()) {
            db.collection("restaurant_tables").document(order.tableId).update(
                mapOf(
                    "billStatus" to BillStatus.PAID.name,
                    "status" to TableStatus.AVAILABLE.name,
                    "needsAssistance" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val earnedPoints = (amountSar / 5.0).toInt().coerceAtLeast(1)
            val loyaltyRef = db.collection("loyalty_profiles").document(uid)
            val loyaltySnap = loyaltyRef.get().await()
            if (loyaltySnap.exists()) {
                val currentPoints = (loyaltySnap.getLong("points") ?: 0L).toInt() + earnedPoints
                val nextTier = when {
                    currentPoints >= 1000 -> "ROYAL"
                    currentPoints >= 500 -> "GOLD"
                    currentPoints >= 200 -> "SILVER"
                    else -> "BRONZE"
                }
                loyaltyRef.update(
                    mapOf(
                        "points" to currentPoints,
                        "totalSpentSar" to FieldValue.increment(amountSar),
                        "tier" to nextTier,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

        createNotificationInternal(
            targetRole = "MANAGER",
            targetUserId = uid,
            title = if (status == "VERIFIED_PAID") "Payment Confirmed • ${order.orderNumber}" else "Payment Requested • ${order.orderNumber}",
            message = "${"%.2f".format(amountSar)} SAR via ${method.labelEn} (Table T%02d)".format(order.tableNumber),
            category = "PAYMENT",
            tableNumber = order.tableNumber
        )

        payRef.id
    }

    suspend fun confirmCashOrBankPayment(payment: PaymentRecord, order: Order?): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "payments/${payment.id}") {
            requireUserId()
            db.collection("payments").document(payment.id).update(
                mapOf(
                    "status" to "VERIFIED_PAID",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (order != null) {
                val newPaid = order.paidAmountSar + payment.amountSar
                val isFullyPaid = newPaid + 0.05 >= (order.totalSar + order.tipSar)
                db.collection("orders").document(order.id).update(
                    mapOf(
                        "paidAmountSar" to newPaid,
                        "paymentStatus" to if (isFullyPaid) "PAID" else "PARTIAL",
                        "status" to if (isFullyPaid) OrderStatus.PAID.name else order.status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (isFullyPaid && order.tableId.isNotBlank()) {
                    db.collection("restaurant_tables").document(order.tableId).update(
                        mapOf(
                            "billStatus" to BillStatus.PAID.name,
                            "status" to TableStatus.AVAILABLE.name,
                            "needsAssistance" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        }

    suspend fun redeemLoyaltyPoints(pointsCost: Int): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "loyalty_profiles") {
            val uid = requireUserId()
            val ref = db.collection("loyalty_profiles").document(uid)
            val snap = ref.get().await()
            val current = (snap.getLong("points") ?: 0L).toInt()
            require(current >= pointsCost) { "Not enough loyalty points." }
            ref.update(
                mapOf(
                    "points" to (current - pointsCost),
                    "redeemedRewards" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun updateLoyaltyBirthday(birthdayMmDd: String): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "loyalty_profiles") {
            val uid = requireUserId()
            db.collection("loyalty_profiles").document(uid).update(
                mapOf(
                    "birthdayMmDd" to birthdayMmDd.take(10),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun submitReview(
        orderId: String?,
        tableNumber: Int,
        authorName: String,
        overall: Int,
        food: Int,
        service: Int,
        comment: String
    ): Result<Unit> = runCatchingFirestore(OperationType.CREATE, "reviews") {
        val uid = requireUserId()
        val ref = db.collection("reviews").document()
        val payload = mapOf(
            "userId" to uid,
            "restaurantId" to RESTAURANT_ID,
            "orderId" to orderId?.takeIf { it.isNotBlank() },
            "tableNumber" to tableNumber,
            "authorName" to authorName.ifBlank { "Valued Guest" }.take(100),
            "overallRating" to overall.coerceIn(1, 5),
            "foodRating" to food.coerceIn(1, 5),
            "serviceRating" to service.coerceIn(1, 5),
            "comment" to comment.take(500),
            "published" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
        ref.set(payload).await()

        createNotificationInternal(
            targetRole = "OWNER",
            targetUserId = uid,
            title = "New ${overall}★ Guest Feedback",
            message = "${authorName.ifBlank { "Guest" }} (T%02d): ${comment.take(80)}".format(tableNumber),
            category = "FEEDBACK",
            tableNumber = tableNumber
        )
    }

    suspend fun createGameSession(tableNumber: Int, gameType: GameType, hostName: String): Result<String> =
        runCatchingFirestore(OperationType.CREATE, "game_sessions") {
            val uid = requireUserId()
            val ref = db.collection("game_sessions").document()
            val roomCode = "T${tableNumber}-${(100..999).random()}"
            ref.set(
                mapOf(
                    "restaurantId" to RESTAURANT_ID,
                    "hostUserId" to uid,
                    "roomCode" to roomCode,
                    "tableNumber" to tableNumber,
                    "gameType" to gameType.name,
                    "status" to "ACTIVE",
                    "currentQuestionIndex" to 0,
                    "playerNames" to listOf(hostName.ifBlank { "Host" }.take(40)),
                    "playerScores" to mapOf(uid to 0L),
                    "participantUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            ref.id
        }

    suspend fun joinAndAnswerGame(
        session: GameSession,
        playerName: String,
        pointsDelta: Long,
        advanceQuestion: Boolean
    ): Result<Unit> = runCatchingFirestore(OperationType.UPDATE, "game_sessions/${session.id}") {
        val uid = requireUserId()
        val updatedUids = (session.participantUids + uid).distinct().take(12)
        val updatedNames = if (uid in session.participantUids) session.playerNames else (session.playerNames + playerName.take(40)).take(12)
        val updatedScores = session.playerScores.toMutableMap().apply {
            this[uid] = (this[uid] ?: 0L) + pointsDelta
        }
        val nextIndex = if (advanceQuestion) session.currentQuestionIndex + 1 else session.currentQuestionIndex
        val totalQuestions = DefaultSeedData.defaultTriviaQuestions(session.gameTypeEnum()).size
        val nextStatus = if (nextIndex >= totalQuestions) "FINISHED" else "ACTIVE"

        db.collection("game_sessions").document(session.id).update(
            mapOf(
                "participantUids" to updatedUids,
                "playerNames" to updatedNames,
                "playerScores" to updatedScores,
                "currentQuestionIndex" to nextIndex.coerceAtMost(totalQuestions),
                "status" to nextStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun saveMenuItem(item: MenuItem): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "menu_items") {
            requireUserId()
            val isNew = item.id.isBlank()
            val docRef = if (isNew) db.collection("menu_items").document() else db.collection("menu_items").document(item.id)
            val baseMap = mutableMapOf<String, Any?>(
                "restaurantId" to RESTAURANT_ID,
                "nameEn" to item.nameEn.trim().take(120),
                "nameAr" to item.nameAr.ifBlank { item.nameEn }.trim().take(120),
                "descriptionEn" to item.descriptionEn.trim().take(500),
                "descriptionAr" to item.descriptionAr.trim().take(500),
                "ingredientsEn" to item.ingredientsEn.trim().take(400),
                "ingredientsAr" to item.ingredientsAr.trim().take(400),
                "allergens" to item.allergens.take(10),
                "category" to item.category,
                "kitchenStation" to item.kitchenStation,
                "spiceLevel" to item.spiceLevel,
                "portionSize" to item.portionSize.ifBlank { "Regular" }.take(80),
                "priceSar" to item.priceSar.coerceIn(0.0, 5000.0),
                "prepMinutes" to item.prepMinutes.coerceIn(1, 180),
                "available" to item.available,
                "hidden" to item.hidden,
                "featured" to item.featured,
                "isMealDeal" to item.isMealDeal,
                "dealSummary" to item.dealSummary?.takeIf { it.isNotBlank() }?.take(300),
                "offerLabel" to item.offerLabel?.takeIf { it.isNotBlank() }?.take(80),
                "imageKey" to item.imageKey.ifBlank { "grills" },
                "imageId" to item.imageId?.takeIf { it.isNotBlank() }?.take(128),
                "imageUrl" to item.imageUrl?.takeIf { it.isNotBlank() }?.take(500),
                "sizes" to item.sizes.take(8),
                "extras" to item.extras.take(15),
                "demoData" to item.demoData,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isNew) {
                baseMap["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(baseMap.filterValues { it != null } as Map<String, Any>).await()
            } else {
                val updateMap = baseMap.mapValues { (_, v) -> v ?: FieldValue.delete() }
                docRef.update(updateMap).await()
            }
        }

    suspend fun deleteMenuItem(itemId: String): Result<Unit> =
        runCatchingFirestore(OperationType.DELETE, "menu_items/$itemId") {
            requireUserId()
            db.collection("menu_items").document(itemId).delete().await()
        }

    suspend fun saveTable(
        tableNumber: Int,
        zone: String,
        capacity: Int,
        waiterName: String,
        active: Boolean = true
    ): Result<Unit> =
        runCatchingFirestore(OperationType.CREATE, "restaurant_tables") {
            requireUserId()
            val label = "T%02d".format(tableNumber)
            val docId = "table_%02d".format(tableNumber)
            val qrId = "qr_$label"
            val qrUrl = DefaultSeedData.tableQrUrl(label)

            val docRef = db.collection("restaurant_tables").document(docId)
            val qrRef = db.collection("qr_codes").document(qrId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "zone" to zone.ifBlank { "Main Hall" }.take(60),
                        "capacity" to capacity.coerceIn(1, 30),
                        "assignedWaiterName" to waiterName.ifBlank { "Tariq Al-Harbi" }.take(80),
                        "active" to active,
                        "qrCodeId" to qrId,
                        "qrPayload" to qrUrl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "restaurantId" to RESTAURANT_ID,
                        "tableNumber" to tableNumber,
                        "label" to label,
                        "zone" to zone.ifBlank { "Main Hall" }.take(60),
                        "capacity" to capacity.coerceIn(1, 30),
                        "active" to active,
                        "status" to TableStatus.AVAILABLE.name,
                        "assignedWaiterName" to waiterName.ifBlank { "Tariq Al-Harbi" }.take(80),
                        "billStatus" to BillStatus.NONE.name,
                        "needsAssistance" to false,
                        "activeRequestCount" to 0,
                        "qrCodeId" to qrId,
                        "qrPayload" to qrUrl,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val qrSnap = qrRef.get().await()
            if (qrSnap.exists()) {
                qrRef.update(
                    mapOf(
                        "tableId" to docId,
                        "tableNumber" to tableNumber,
                        "tableLabel" to label,
                        "qrPayload" to qrUrl,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                qrRef.set(
                    mapOf(
                        "restaurantId" to RESTAURANT_ID,
                        "tableId" to docId,
                        "tableNumber" to tableNumber,
                        "tableLabel" to label,
                        "qrPayload" to qrUrl,
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

    suspend fun toggleTableActive(table: RestaurantTable): Result<Unit> =
        saveTable(
            tableNumber = table.tableNumber,
            zone = table.zone,
            capacity = table.capacity,
            waiterName = table.assignedWaiterName,
            active = !table.active
        )

    suspend fun deleteTable(table: RestaurantTable): Result<Unit> =
        runCatchingFirestore(OperationType.DELETE, "restaurant_tables/${table.id}") {
            requireUserId()
            db.collection("restaurant_tables").document(table.id).delete().await()
            val qrId = table.qrCodeId ?: "qr_${table.label}"
            runCatching { db.collection("qr_codes").document(qrId).delete().await() }
        }

    suspend fun saveStaffMember(
        docIdOrUid: String,
        name: String,
        email: String,
        role: StaffRole,
        zone: String,
        active: Boolean = true
    ): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "staff") {
            requireUserId()
            val cleanEmail = email.trim().lowercase()
            require(cleanEmail.isNotBlank()) { "Staff email cannot be empty." }
            val resolvedDocId = docIdOrUid.trim().ifBlank { cleanEmail }
                .replace(Regex("[^a-zA-Z0-9_.\\-@]"), "_")
            require(resolvedDocId.isNotBlank()) { "Staff identifier invalid." }
            val ref = db.collection("staff").document(resolvedDocId)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "name" to name.ifBlank { cleanEmail.substringBefore("@") }.take(100),
                        "email" to cleanEmail.take(150),
                        "role" to role.name,
                        "active" to active,
                        "assignedZone" to zone.ifBlank { "All Zones" }.take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "uid" to resolvedDocId,
                        "restaurantId" to RESTAURANT_ID,
                        "name" to name.ifBlank { cleanEmail.substringBefore("@") }.take(100),
                        "email" to cleanEmail.take(150),
                        "role" to role.name,
                        "active" to active,
                        "assignedZone" to zone.ifBlank { "All Zones" }.take(80),
                        "ordersHandled" to 0,
                        "avgResponseSec" to 40,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

    suspend fun toggleStaffActive(member: StaffMember): Result<Unit> =
        saveStaffMember(
            docIdOrUid = member.id,
            name = member.name,
            email = member.email,
            role = member.roleEnum(),
            zone = member.assignedZone,
            active = !member.active
        )

    suspend fun deleteStaffMember(staffDocId: String): Result<Unit> =
        runCatchingFirestore(OperationType.DELETE, "staff/$staffDocId") {
            requireUserId()
            db.collection("staff").document(staffDocId).delete().await()
        }

    suspend fun saveSettings(settings: RestaurantSettings): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "restaurant_settings/config") {
            requireUserId()
            db.collection("restaurant_settings").document("config").set(
                mapOf(
                    "restaurantId" to RESTAURANT_ID,
                    "restaurantNameEn" to settings.restaurantNameEn.take(100),
                    "restaurantNameAr" to settings.restaurantNameAr.take(100),
                    "city" to settings.city.take(80),
                    "wifiSsid" to settings.wifiSsid.take(64),
                    "wifiPassword" to settings.wifiPassword.take(64),
                    "vatPercent" to settings.vatPercent.coerceIn(0.0, 50.0),
                    "openingHours" to settings.openingHours.take(120),
                    "googleReviewUrl" to settings.googleReviewUrl.take(300),
                    "gatewayProvider" to settings.gatewayProvider,
                    "gatewayPublicKey" to settings.gatewayPublicKey.take(200),
                    "gatewayMode" to settings.gatewayMode,
                    "gatewayEndpoint" to settings.gatewayEndpoint.take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> =
        runCatchingFirestore(OperationType.UPDATE, "notifications/$notificationId") {
            requireUserId()
            db.collection("notifications").document(notificationId).update(
                mapOf(
                    "read" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    private suspend fun createNotificationInternal(
        targetRole: String,
        targetUserId: String,
        title: String,
        message: String,
        category: String,
        tableNumber: Int
    ) {
        try {
            val uid = auth.currentUser?.uid ?: return
            db.collection("notifications").document().set(
                mapOf(
                    "restaurantId" to RESTAURANT_ID,
                    "targetRole" to targetRole,
                    "targetUserId" to targetUserId,
                    "createdBy" to uid,
                    "title" to title.take(120),
                    "message" to message.take(400),
                    "category" to category,
                    "read" to false,
                    "tableNumber" to tableNumber,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("RestaurantRepo", "Notification dispatch skipped: ${e.message}")
        }
    }

    fun exportBackupJson(
        menu: List<MenuItem>,
        tables: List<RestaurantTable>,
        orders: List<Order>,
        settings: RestaurantSettings
    ): String {
        return JSONObject().apply {
            put("restaurant", settings.restaurantNameEn)
            put("city", settings.city)
            put("menuCount", menu.size)
            put("tableCount", tables.size)
            put("orderCount", orders.size)
            put("menu", JSONArray(menu.map { m ->
                JSONObject().apply {
                    put("id", m.id)
                    put("nameEn", m.nameEn)
                    put("nameAr", m.nameAr)
                    put("category", m.category)
                    put("priceSar", m.priceSar)
                    put("available", m.available)
                }
            }))
            put("orders", JSONArray(orders.map { o ->
                JSONObject().apply {
                    put("orderNumber", o.orderNumber)
                    put("tableNumber", o.tableNumber)
                    put("status", o.status)
                    put("totalSar", o.totalSar)
                    put("vatSar", o.vatSar)
                    put("demoData", o.demoData)
                }
            }))
        }.toString(2)
    }

    private inline fun <T> runCatchingFirestore(
        op: OperationType,
        path: String,
        block: () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, path)
            Result.failure(e)
        }
    }
}
