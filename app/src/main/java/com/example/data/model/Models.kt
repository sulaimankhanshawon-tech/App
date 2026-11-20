package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

const val RESTAURANT_ID = "asil_al_tawooq_jeddah"
const val OWNER_BOOTSTRAP_EMAIL = "sulaimankhanshawon@gmail.com"

inline fun <reified T : Enum<T>> safeEnum(raw: String?, default: T): T {
    return if (raw.isNullOrBlank()) default
    else enumValues<T>().firstOrNull { it.name == raw } ?: default
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MenuCategory(val labelEn: String, val labelAr: String, val defaultStation: KitchenStation) {
    CHARCOAL_GRILLS("Charcoal Grills", "مشويات على الفحم", KitchenStation.GRILL),
    CHICKEN_TAWOOK("Chicken Tawook", "طاووق دجاج", KitchenStation.GRILL),
    SHAWARMA("Shawarma", "شاورما", KitchenStation.SHAWARMA),
    RICE_MEALS("Rice Meals", "وجبات الأرز", KitchenStation.GENERAL),
    SANDWICHES("Sandwiches", "ساندويتشات", KitchenStation.SANDWICH),
    PASTA("Pasta", "باستا", KitchenStation.PASTA),
    APPETIZERS("Appetizers", "مقبلات", KitchenStation.GENERAL),
    SAUCES("Sauces", "صلصات", KitchenStation.GENERAL),
    COLD_DRINKS("Cold Drinks", "مشروبات باردة", KitchenStation.DRINKS),
    SPECIAL_DEALS("Special Deals", "عروض خاصة", KitchenStation.GENERAL)
}

enum class KitchenStation(val labelEn: String, val labelAr: String) {
    GRILL("Charcoal Grill Station", "محطة المشويات والفحم"),
    SHAWARMA("Shawarma Station", "محطة الشاورما"),
    SANDWICH("Sandwich Station", "محطة الساندويتش"),
    PASTA("Pasta Station", "محطة الباستا"),
    DRINKS("Drinks Station", "محطة المشروبات"),
    GENERAL("General Kitchen", "المطبخ العام")
}

enum class SpiceLevel(val labelEn: String, val labelAr: String) {
    MILD("Mild", "بارد"),
    MEDIUM("Medium", "متوسط"),
    HOT("Hot", "حار")
}

enum class TableStatus(val labelEn: String, val labelAr: String) {
    AVAILABLE("Available", "متاحة"),
    OCCUPIED("Occupied", "مشغولة"),
    ORDERING("Ordering", "جاري الطلب"),
    FOOD_PREP("Preparing", "قيد التحضير"),
    FOOD_SERVED("Served", "تم التقديم"),
    BILL_REQUESTED("Bill Requested", "طلب الفاتورة"),
    NEEDS_ASSISTANCE("Needs Assistance", "يحتاج مساعدة")
}

enum class BillStatus(val labelEn: String, val labelAr: String) {
    NONE("No Bill", "لا توجد فاتورة"),
    OPEN("Open", "مفتوحة"),
    REQUESTED("Requested", "مطلوبة"),
    PAID("Paid", "مدفوعة")
}

enum class OrderStatus(val labelEn: String, val labelAr: String) {
    NEW_ORDER("New Order", "طلب جديد"),
    ACCEPTED("Accepted", "مقبول"),
    PREPARING("Preparing", "قيد التحضير"),
    READY("Ready", "جاهز للتقديم"),
    SERVED("Served", "تم التقديم"),
    PAID("Paid", "مدفوع"),
    CANCELLED("Cancelled", "ملغي")
}

enum class OrderPriority(val labelEn: String, val labelAr: String) {
    NORMAL("Normal", "عادي"),
    HIGH("High", "أولوية عالية"),
    URGENT("Urgent", "عاجل")
}

enum class ServiceRequestType(val labelEn: String, val labelAr: String) {
    CALL_WAITER("Call Waiter", "استدعاء النادل"),
    WATER("Request Water", "طلب ماء"),
    EXTRA_SAUCE("Extra Sauce", "صلصة إضافية"),
    CUTLERY("Cutlery", "أدوات طعام"),
    CLEANING("Cleaning", "تنظيف الطاولة"),
    BILL("Request Bill", "طلب الفاتورة"),
    OTHER("Other Request", "طلب آخر")
}

enum class RequestStatus(val labelEn: String, val labelAr: String) {
    PENDING("Pending", "قيد الانتظار"),
    ACCEPTED("Accepted", "تم القبول"),
    IN_PROGRESS("In Progress", "جاري التنفيذ"),
    COMPLETED("Completed", "مكتمل")
}

enum class PaymentMethod(val labelEn: String, val labelAr: String) {
    MADA("Mada", "مدى"),
    APPLE_PAY("Apple Pay", "Apple Pay"),
    GOOGLE_PAY("Google Pay", "Google Pay"),
    CARD("Credit/Debit Card", "بطاقة ائتمان"),
    BANK_TRANSFER("Bank Transfer", "تحويل بنكي"),
    CASH_REQUEST("Cash at Table", "نقداً عند الطاولة")
}

enum class SplitMode(val labelEn: String, val labelAr: String) {
    FULL("Full Bill", "كامل الفاتورة"),
    EQUAL("Equal Split", "تقسيم متساوي"),
    BY_ITEMS("By Items", "حسب الأصناف")
}

enum class StaffRole(val labelEn: String, val labelAr: String) {
    OWNER("Owner", "المالك"),
    MANAGER("Manager", "المدير"),
    WAITER("Waiter", "نادل"),
    KITCHEN("Kitchen Staff", "طاقم المطبخ");

    companion object {
        fun fromRaw(raw: String?): StaffRole? = when (raw?.uppercase()) {
            "OWNER", "ADMIN" -> OWNER
            "MANAGER", "CASHIER" -> MANAGER
            "WAITER" -> WAITER
            "KITCHEN" -> KITCHEN
            else -> null
        }
    }
}

enum class GameType(val labelEn: String, val labelAr: String) {
    TABLE_TRIVIA("Table Trivia", "تحدي معلومات الطاولة"),
    FOOD_GUESSING("Food Guessing", "خمن الطبق"),
    QUICK_QUIZ("Quick Quiz", "اختبار سريع")
}

@Keep
@IgnoreExtraProperties
data class MenuItem(
    @DocumentId val id: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val nameEn: String = "",
    val nameAr: String = "",
    val descriptionEn: String = "",
    val descriptionAr: String = "",
    val ingredientsEn: String = "",
    val ingredientsAr: String = "",
    val allergens: List<String> = emptyList(),
    val category: String = MenuCategory.CHICKEN_TAWOOK.name,
    val kitchenStation: String = KitchenStation.GRILL.name,
    val spiceLevel: String = SpiceLevel.MILD.name,
    val portionSize: String = "Regular",
    val priceSar: Double = 0.0,
    val prepMinutes: Int = 12,
    val available: Boolean = true,
    val hidden: Boolean = false,
    val featured: Boolean = false,
    @get:PropertyName("isMealDeal")
    @PropertyName("isMealDeal")
    val isMealDeal: Boolean = false,
    val dealSummary: String? = null,
    val offerLabel: String? = null,
    val imageKey: String = "grills",
    val imageId: String? = null,
    val imageUrl: String? = null,
    val sizes: List<String> = listOf("Regular", "Large", "Family"),
    val extras: List<String> = listOf("Extra Garlic Toum +4 SAR", "Cheddar Cheese +6 SAR", "Crispy Fries +9 SAR"),
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): MenuCategory = safeEnum(category, MenuCategory.CHICKEN_TAWOOK)
    fun stationEnum(): KitchenStation = safeEnum(kitchenStation, KitchenStation.GRILL)
    fun spiceEnum(): SpiceLevel = safeEnum(spiceLevel, SpiceLevel.MILD)
}

@Keep
@IgnoreExtraProperties
data class RestaurantTable(
    @DocumentId val id: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val tableNumber: Int = 1,
    val label: String = "T01",
    val zone: String = "Main Hall",
    val capacity: Int = 4,
    val active: Boolean = true,
    val status: String = TableStatus.AVAILABLE.name,
    val assignedWaiterId: String? = null,
    val assignedWaiterName: String = "Tariq Al-Harbi",
    val currentOrderId: String? = null,
    val billStatus: String = BillStatus.NONE.name,
    val needsAssistance: Boolean = false,
    val activeRequestCount: Int = 0,
    val qrCodeId: String? = null,
    val qrPayload: String = "https://asilaltawooq.sa/menu?table=T01",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): TableStatus = safeEnum(status, TableStatus.AVAILABLE)
    fun billEnum(): BillStatus = safeEnum(billStatus, BillStatus.NONE)
}

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    val itemId: String = "",
    val nameEn: String = "",
    val nameAr: String = "",
    val kitchenStation: String = KitchenStation.GENERAL.name,
    val quantity: Int = 1,
    val unitPriceSar: Double = 0.0,
    val selectedSize: String = "Regular",
    val spiceLevel: String = SpiceLevel.MILD.name,
    val extras: List<String> = emptyList(),
    val notes: String = "",
    val allergyNote: String = "",
    val itemStatus: String = OrderStatus.NEW_ORDER.name,
    val prepMinutes: Int = 12
) {
    fun stationEnum(): KitchenStation = safeEnum(kitchenStation, KitchenStation.GENERAL)
    fun statusEnum(): OrderStatus = safeEnum(itemStatus, OrderStatus.NEW_ORDER)
    fun lineTotal(): Double {
        val extrasTotal = extras.sumOf { parseExtraPrice(it) }
        val sizeMultiplier = when (selectedSize) {
            "Large" -> 1.25
            "Family" -> 1.85
            else -> 1.0
        }
        return ((unitPriceSar * sizeMultiplier) + extrasTotal) * quantity
    }
}

fun parseExtraPrice(extraLabel: String): Double {
    val match = Regex("\\+(\\d+(?:\\.\\d+)?)").find(extraLabel)
    return match?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val tableId: String = "",
    val tableNumber: Int = 1,
    val orderNumber: String = "",
    val clientRequestId: String? = null,
    val customerName: String = "",
    val status: String = OrderStatus.NEW_ORDER.name,
    val priority: String = OrderPriority.NORMAL.name,
    val waiterConfirmed: Boolean = false,
    val waiterConfirmedBy: String? = null,
    val items: List<OrderLineItem> = emptyList(),
    val specialInstructions: String? = null,
    val allergySummary: String? = null,
    val subtotalSar: Double = 0.0,
    val vatSar: Double = 0.0,
    val discountSar: Double = 0.0,
    val tipSar: Double = 0.0,
    val totalSar: Double = 0.0,
    val paidAmountSar: Double = 0.0,
    val paymentStatus: String = "UNPAID",
    val paymentMethod: String? = null,
    val assignedWaiterName: String? = null,
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): OrderStatus = safeEnum(status, OrderStatus.NEW_ORDER)
    fun priorityEnum(): OrderPriority = safeEnum(priority, OrderPriority.NORMAL)
}

@Keep
@IgnoreExtraProperties
data class ServiceRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val tableId: String = "",
    val tableNumber: Int = 1,
    val requestType: String = ServiceRequestType.CALL_WAITER.name,
    val note: String? = null,
    val status: String = RequestStatus.PENDING.name,
    val assignedWaiterName: String? = null,
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun typeEnum(): ServiceRequestType = safeEnum(requestType, ServiceRequestType.CALL_WAITER)
    fun statusEnum(): RequestStatus = safeEnum(status, RequestStatus.PENDING)
}

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val orderId: String = "",
    val tableNumber: Int = 1,
    val amountSar: Double = 0.0,
    val vatSar: Double = 0.0,
    val tipSar: Double = 0.0,
    val method: String = PaymentMethod.MADA.name,
    val splitMode: String = SplitMode.FULL.name,
    val gatewayProvider: String = "MOYASAR",
    val gatewayTransactionId: String = "",
    val status: String = "PENDING_CASH_COLLECTION",
    val receiptEmail: String? = null,
    val selectedItemIds: List<String> = emptyList(),
    val demoData: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LoyaltyProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val displayName: String = "",
    val email: String? = null,
    val points: Int = 50,
    val totalSpentSar: Double = 0.0,
    val tier: String = "BRONZE",
    val birthdayMmDd: String? = null,
    val redeemedRewards: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val userId: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val orderId: String? = null,
    val tableNumber: Int = 1,
    val authorName: String = "",
    val overallRating: Int = 5,
    val foodRating: Int = 5,
    val serviceRating: Int = 5,
    val comment: String = "",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameSession(
    @DocumentId val id: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val hostUserId: String = "",
    val roomCode: String = "",
    val tableNumber: Int = 1,
    val gameType: String = GameType.TABLE_TRIVIA.name,
    val status: String = "WAITING",
    val currentQuestionIndex: Int = 0,
    val playerNames: List<String> = emptyList(),
    val playerScores: Map<String, Long> = emptyMap(),
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun gameTypeEnum(): GameType = safeEnum(gameType, GameType.TABLE_TRIVIA)
}

@Keep
@IgnoreExtraProperties
data class StaffMember(
    @DocumentId val id: String = "",
    val uid: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val name: String = "",
    val email: String = "",
    val role: String = StaffRole.WAITER.name,
    val active: Boolean = true,
    val assignedZone: String = "Main Hall",
    val ordersHandled: Int = 0,
    val avgResponseSec: Int = 45,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): StaffRole = StaffRole.fromRaw(role) ?: StaffRole.WAITER
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val displayName: String = "",
    val email: String? = null,
    val staffRole: String = "CUSTOMER",
    val activeTableLabel: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoleDefinition(
    @DocumentId val id: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val code: String = StaffRole.WAITER.name,
    val titleEn: String = "",
    val titleAr: String = "",
    val permissions: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CategoryEntity(
    @DocumentId val id: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val code: String = "",
    val nameEn: String = "",
    val nameAr: String = "",
    val defaultStation: String = KitchenStation.GENERAL.name,
    val sortOrder: Int = 0,
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ProductImage(
    @DocumentId val id: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val productId: String? = null,
    val mimeType: String = "image/jpeg",
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Int = 0,
    val base64Data: String = "",
    val uploadedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TableQrCode(
    @DocumentId val id: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val tableId: String = "",
    val tableNumber: Int = 1,
    val tableLabel: String = "T01",
    val qrPayload: String = "https://asilaltawooq.sa/menu?table=T01",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderItemRecord(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val userId: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val tableId: String = "",
    val tableNumber: Int = 1,
    val productId: String = "",
    val nameEn: String = "",
    val nameAr: String = "",
    val kitchenStation: String = KitchenStation.GENERAL.name,
    val quantity: Int = 1,
    val unitPriceSar: Double = 0.0,
    val lineTotalSar: Double = 0.0,
    val selectedSize: String = "Regular",
    val spiceLevel: String = SpiceLevel.MILD.name,
    val extras: List<String> = emptyList(),
    val notes: String = "",
    val allergyNote: String = "",
    val itemStatus: String = OrderStatus.NEW_ORDER.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class KitchenStationEntity(
    @DocumentId val id: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val code: String = KitchenStation.GENERAL.name,
    val nameEn: String = "",
    val nameAr: String = "",
    val active: Boolean = true,
    val sortOrder: Int = 0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RestaurantSettings(
    @DocumentId val id: String = "config",
    val restaurantId: String = RESTAURANT_ID,
    val restaurantNameEn: String = "Asil Al Tawooq",
    val restaurantNameAr: String = "أصيل الطاووق",
    val city: String = "Jeddah, Saudi Arabia",
    val wifiSsid: String = "AsilAlTawooq_Guest_5G",
    val wifiPassword: String = "TawooqJeddah2026",
    val vatPercent: Double = 15.0,
    val openingHours: String = "Daily 12:00 PM – 2:00 AM",
    val googleReviewUrl: String = "https://www.google.com/maps/search/?api=1&query=Asil+Al+Tawooq+Jeddah",
    val gatewayProvider: String = "MOYASAR",
    val gatewayPublicKey: String = "",
    val gatewayMode: String = "TEST",
    val gatewayEndpoint: String = "https://api.moyasar.com/v1/payments",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val restaurantId: String = RESTAURANT_ID,
    val targetRole: String = "WAITER",
    val targetUserId: String = "",
    val createdBy: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "ORDER",
    val read: Boolean = false,
    val tableNumber: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartItem(
    val cartKey: String,
    val menuItem: MenuItem,
    val quantity: Int = 1,
    val selectedSize: String = "Regular",
    val spiceLevel: SpiceLevel = SpiceLevel.MILD,
    val selectedExtras: List<String> = emptyList(),
    val notes: String = "",
    val allergyNote: String = ""
) {
    fun unitTotal(): Double {
        val extrasSum = selectedExtras.sumOf { parseExtraPrice(it) }
        val multiplier = when (selectedSize) {
            "Large" -> 1.25
            "Family" -> 1.85
            else -> 1.0
        }
        return (menuItem.priceSar * multiplier) + extrasSum
    }

    fun lineTotal(): Double = unitTotal() * quantity
}

data class TriviaQuestion(
    val questionEn: String,
    val questionAr: String,
    val optionsEn: List<String>,
    val optionsAr: List<String>,
    val correctIndex: Int
)
