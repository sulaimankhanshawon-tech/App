package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.KitchenStation
import com.example.data.model.Order
import com.example.data.model.OrderPriority
import com.example.data.model.OrderStatus
import com.example.data.model.UiState
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.AmberGold
import com.example.ui.theme.KdsBorder
import com.example.ui.theme.KdsCardBg
import com.example.ui.theme.KdsDarkBg
import com.example.ui.theme.OliveEmerald
import com.example.ui.theme.SaffronVibrant
import com.example.ui.theme.SumacCrimson

@Composable
fun KitchenDisplayScreen(
    isOwnerOrStaff: Boolean,
    ordersState: UiState<List<Order>>,
    selectedStation: KitchenStation?,
    currentEpochSec: Long,
    language: AppLanguage,
    onSelectStation: (KitchenStation?) -> Unit,
    onAdvanceOrder: (Order, OrderStatus) -> Unit
) {
    if (!isOwnerOrStaff) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Card {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Kitchen Display Access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Sign in with an authorized Kitchen Chef or Owner account to view station tickets.")
                }
            }
        }
        return
    }

    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val activeTickets = orders.filter {
        val st = it.statusEnum()
        st != OrderStatus.SERVED && st != OrderStatus.PAID && st != OrderStatus.CANCELLED
    }.filter { order ->
        selectedStation == null || order.items.any { it.stationEnum() == selectedStation }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KdsDarkBg)
    ) {
        // KDS Header & Station Filter Bar
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = SaffronVibrant, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.AR) "شاشة المطبخ المباشرة (KDS)" else "KITCHEN DISPLAY SYSTEM",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusPill("${activeTickets.size} Active", SaffronVibrant, Color.White)
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStation == null,
                    onClick = { onSelectStation(null) },
                    label = { Text("All Stations") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SaffronVibrant,
                        selectedLabelColor = Color.White,
                        labelColor = Color.White.copy(alpha = 0.8f)
                    )
                )
                KitchenStation.entries.forEach { station ->
                    FilterChip(
                        selected = selectedStation == station,
                        onClick = { onSelectStation(station) },
                        label = { Text(if (language == AppLanguage.AR) station.labelAr else station.labelEn) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SaffronVibrant,
                            selectedLabelColor = Color.White,
                            labelColor = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }

        if (activeTickets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "All kitchen tickets cleared — ready for new orders.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(activeTickets, key = { it.id }) { order ->
                    val createdSec = order.createdAt?.seconds ?: currentEpochSec
                    val elapsedSec = (currentEpochSec - createdSec).coerceAtLeast(0L)
                    val targetPrepMin = order.items.maxOfOrNull { it.prepMinutes } ?: 12
                    val isDelayed = elapsedSec > (targetPrepMin * 60L)
                    val mins = elapsedSec / 60L
                    val secs = elapsedSec % 60L

                    val stationItems = if (selectedStation == null) order.items
                    else order.items.filter { it.stationEnum() == selectedStation }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = KdsCardBg),
                        border = BorderStroke(
                            width = if (isDelayed) 2.5.dp else 1.dp,
                            color = if (isDelayed) SumacCrimson else KdsBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kds_ticket_${order.orderNumber}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "${order.orderNumber} • TABLE T%02d".format(order.tableNumber),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (order.priorityEnum() != OrderPriority.NORMAL) {
                                            StatusPill(order.priorityEnum().labelEn, SumacCrimson, Color.White)
                                        }
                                        if (order.waiterConfirmed) {
                                            StatusPill("Waiter Confirmed", OliveEmerald, Color.White)
                                        } else {
                                            StatusPill("New / Unconfirmed", AmberGold, Color.Black)
                                        }
                                        if (order.demoData) {
                                            StatusPill("DEMO", Color(0xFF475569), Color.White)
                                        }
                                    }
                                    Text(
                                        text = "Status: ${order.statusEnum().labelEn} • Target: ${targetPrepMin}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SaffronVibrant
                                    )
                                }

                                Surface(
                                    color = if (isDelayed) SumacCrimson else Color(0xFF332B24),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Timer, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "%02d:%02d".format(mins, secs) + if (isDelayed) " DELAYED" else "",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (!order.allergySummary.isNullOrBlank()) {
                                Surface(color = SumacCrimson.copy(alpha = 0.25f), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WarningAmber, null, tint = Color(0xFFFCA5A5))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "ALLERGY ALERT: ${order.allergySummary}",
                                            color = Color(0xFFFEE2E2),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (!order.specialInstructions.isNullOrBlank()) {
                                Text(
                                    text = "Note: ${order.specialInstructions}",
                                    color = Color(0xFFFDE68A),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            HorizontalDivider(color = KdsBorder)

                            stationItems.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.quantity}x  ${item.nameEn} (${item.selectedSize})",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Spice: ${item.spiceLevel}" +
                                                (if (item.extras.isNotEmpty()) " • ${item.extras.joinToString()}" else "") +
                                                (if (item.notes.isNotBlank()) " • Note: ${item.notes}" else ""),
                                            color = Color.White.copy(alpha = 0.78f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    StatusPill(item.stationEnum().name, Color(0xFF332B24), Color(0xFFFFD8B2))
                                }
                            }

                            // 5-Step Kitchen Order Workflow Controls
                            val nextAction: Pair<String, OrderStatus>? = when (order.statusEnum()) {
                                OrderStatus.NEW_ORDER -> "Accept Order" to OrderStatus.ACCEPTED
                                OrderStatus.ACCEPTED -> "Start Preparing" to OrderStatus.PREPARING
                                OrderStatus.PREPARING -> "Mark Ready" to OrderStatus.READY
                                OrderStatus.READY -> "Mark Served" to OrderStatus.SERVED
                                else -> null
                            }

                            if (nextAction != null) {
                                val btnColor = when (nextAction.second) {
                                    OrderStatus.ACCEPTED -> AmberGold
                                    OrderStatus.PREPARING -> SaffronVibrant
                                    OrderStatus.READY -> OliveEmerald
                                    OrderStatus.SERVED -> Color(0xFF0F766E)
                                    else -> SaffronVibrant
                                }
                                Button(
                                    onClick = { onAdvanceOrder(order, nextAction.second) },
                                    colors = ButtonDefaults.buttonColors(containerColor = btnColor, contentColor = Color.White),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("kds_advance_${order.orderNumber}")
                                ) {
                                    Text(nextAction.first, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
