package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.NotificationItem
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.RequestStatus
import com.example.data.model.RestaurantTable
import com.example.data.model.ServiceRequest
import com.example.data.model.SpiceLevel
import com.example.data.model.TableStatus
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.OliveContainer
import com.example.ui.theme.OliveEmerald
import com.example.ui.theme.RoyalTeal
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SumacCrimson
import com.example.ui.theme.TealContainer
import com.example.util.QrCodeGenerator

val LocalProductImages = staticCompositionLocalOf<Map<String, ByteArray>> { emptyMap() }

@DrawableRes
fun drawableForImageKey(imageKey: String): Int = when (imageKey.lowercase()) {
    "shawarma", "sandwich" -> R.drawable.img_cat_shawarma
    "rice", "appetizers" -> R.drawable.img_cat_rice
    "pasta", "sauces" -> R.drawable.img_cat_pasta
    "drinks" -> R.drawable.img_cat_drinks
    else -> R.drawable.img_cat_grills
}

@Composable
fun DishPhoto(
    imageKey: String,
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val fallbackRes = drawableForImageKey(imageKey)
    val cloudImages = LocalProductImages.current
    val cloudModel: Any? = remember(imageUrl, cloudImages) {
        when {
            imageUrl.isNullOrBlank() -> null
            imageUrl.startsWith("cloud_img://") -> cloudImages[imageUrl.removePrefix("cloud_img://")]
            else -> imageUrl
        }
    }

    if (cloudModel != null) {
        AsyncImage(
            model = cloudModel,
            contentDescription = contentDescription,
            placeholder = painterResource(fallbackRes),
            error = painterResource(fallbackRes),
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(fallbackRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun SpiceBadge(spice: SpiceLevel, language: AppLanguage) {
    val (bg, fg) = when (spice) {
        SpiceLevel.MILD -> OliveContainer to OliveEmerald
        SpiceLevel.MEDIUM -> AmberContainer to AmberGold
        SpiceLevel.HOT -> CrimsonContainer to SumacCrimson
    }
    Surface(color = bg, shape = RoundedCornerShape(50)) {
        Text(
            text = if (language == AppLanguage.AR) spice.labelAr else spice.labelEn,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusPill(label: String, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Surface(color = containerColor, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun NotificationsDialog(
    notifications: List<NotificationItem>,
    language: AppLanguage,
    onMarkRead: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = SaffronPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(if (language == AppLanguage.AR) "التنبيهات المباشرة" else "Live Notifications")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Text(
                    if (language == AppLanguage.AR) "لا توجد تنبيهات جديدة." else "No alerts yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (notif.read) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (!notif.read) onMarkRead(notif.id) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EspressoInk,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!notif.read) {
                                        Icon(
                                            Icons.Default.DoneAll,
                                            contentDescription = "Mark read",
                                            tint = SaffronPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(notif.message, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${notif.targetRole} • ${notif.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(if (language == AppLanguage.AR) "إغلاق" else "Close")
            }
        }
    )
}

@Composable
fun SingleTableQrDialog(
    table: RestaurantTable,
    onDownloadPng: (() -> Unit)? = null,
    onPrintQr: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(table.qrPayload) { QrCodeGenerator.generateQrBitmap(table.qrPayload, 512) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode2, null, tint = SaffronPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Table ${table.label} (Table ${table.tableNumber})")
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "QR Code for ${table.label}",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(10.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("ASIL AL TAWOOQ • أصيل الطاووق", fontWeight = FontWeight.Bold)
                Text(table.qrPayload, style = MaterialTheme.typography.bodySmall, color = SaffronPrimary)
                Text("${table.zone} • ${table.capacity} Guests • Waiter: ${table.assignedWaiterName}", style = MaterialTheme.typography.bodySmall)
                if (!table.active) {
                    Spacer(Modifier.height(6.dp))
                    StatusPill("Table Deactivated", CrimsonContainer, SumacCrimson)
                }
                if (onDownloadPng != null || onPrintQr != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (onDownloadPng != null) {
                            OutlinedButton(
                                onClick = onDownloadPng,
                                modifier = Modifier.weight(1f).testTag("qr_dialog_download_btn")
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save PNG")
                            }
                        }
                        if (onPrintQr != null) {
                            Button(
                                onClick = onPrintQr,
                                colors = ButtonDefaults.buttonColors(containerColor = EspressoInk),
                                modifier = Modifier.weight(1f).testTag("qr_dialog_print_btn")
                            ) {
                                Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Print")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveFloorMapBoard(
    tables: List<RestaurantTable>,
    orders: List<Order>,
    requests: List<ServiceRequest>,
    onInspectTable: (RestaurantTable) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedZone by remember { mutableStateOf<String?>(null) }
    val zones = remember(tables) { tables.map { it.zone }.distinct() }
    val filteredTables = remember(tables, selectedZone) {
        tables.filter { selectedZone == null || it.zone == selectedZone }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedZone == null,
                onClick = { selectedZone = null },
                label = { Text("All Zones (${tables.size})") }
            )
            zones.forEach { z ->
                FilterChip(
                    selected = selectedZone == z,
                    onClick = { selectedZone = z },
                    label = { Text(z) }
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            filteredTables.forEach { table ->
                val status = table.statusEnum()
                val (bg, fg) = when {
                    !table.active -> Color(0xFFE5E7EB) to Color(0xFF6B7280)
                    status == TableStatus.AVAILABLE -> OliveContainer to OliveEmerald
                    status == TableStatus.NEEDS_ASSISTANCE || status == TableStatus.BILL_REQUESTED -> CrimsonContainer to SumacCrimson
                    status == TableStatus.FOOD_PREP || status == TableStatus.ORDERING -> AmberContainer to AmberGold
                    status == TableStatus.FOOD_SERVED -> TealContainer to RoyalTeal
                    else -> SaffronContainer to EspressoInk
                }
                val tableOrders = orders.filter {
                    (it.tableId == table.id || it.tableNumber == table.tableNumber) &&
                        it.statusEnum() != OrderStatus.PAID &&
                        it.statusEnum() != OrderStatus.CANCELLED
                }
                val latestOrder = tableOrders.firstOrNull()
                    ?: orders.firstOrNull { it.id == table.currentOrderId }
                val pendingRequests = requests.count {
                    (it.tableId == table.id || it.tableNumber == table.tableNumber) &&
                        it.statusEnum() != RequestStatus.COMPLETED
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = if (table.needsAssistance || pendingRequests > 0) BorderStroke(2.dp, SumacCrimson) else null,
                    modifier = Modifier
                        .width(172.dp)
                        .clickable { onInspectTable(table) }
                        .testTag("floor_table_${table.label}")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TableRestaurant, null, tint = fg, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(table.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = fg)
                            }
                            if (!table.active) {
                                StatusPill("OFF", Color.White, Color(0xFF6B7280))
                            } else if (table.needsAssistance || pendingRequests > 0) {
                                Icon(Icons.Default.Warning, contentDescription = "Needs Assistance", tint = SumacCrimson, modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = if (table.active) status.labelEn else "Deactivated",
                            style = MaterialTheme.typography.labelLarge,
                            color = fg,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${table.zone} • ${table.capacity} seats", style = MaterialTheme.typography.labelSmall, color = EspressoInk)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(13.dp), tint = EspressoInk)
                            Spacer(Modifier.width(3.dp))
                            Text(table.assignedWaiterName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        if (latestOrder != null) {
                            Text(
                                text = "Order ${latestOrder.orderNumber} (${"%.0f".format(latestOrder.totalSar)} SAR)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EspressoInk
                            )
                            Text(
                                text = "Prep: ${latestOrder.statusEnum().labelEn}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SaffronPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text("No active orders", style = MaterialTheme.typography.labelSmall, color = EspressoInk.copy(alpha = 0.7f))
                        }
                        if (pendingRequests > 0) {
                            Text(
                                text = "$pendingRequests open service req",
                                style = MaterialTheme.typography.labelSmall,
                                color = SumacCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Pay: ${latestOrder?.paymentStatus ?: table.billEnum().labelEn}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EspressoInk
                        )
                    }
                }
            }
        }
    }
}
