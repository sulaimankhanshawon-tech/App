package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentRecord
import com.example.data.model.RequestStatus
import com.example.data.model.RestaurantTable
import com.example.data.model.ServiceRequest
import com.example.data.model.TableStatus
import com.example.data.model.UiState
import com.example.ui.components.LiveFloorMapBoard
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.OliveContainer
import com.example.ui.theme.OliveEmerald
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SumacCrimson

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaiterDashboardScreen(
    isOwnerOrStaff: Boolean,
    tablesState: UiState<List<RestaurantTable>>,
    ordersState: UiState<List<Order>>,
    requestsState: UiState<List<ServiceRequest>>,
    paymentsState: UiState<List<PaymentRecord>>,
    language: AppLanguage,
    onConfirmOrderToKitchen: (Order) -> Unit,
    onAdvanceRequest: (ServiceRequest, RequestStatus) -> Unit,
    onUpdateTableStatus: (String, TableStatus, String?) -> Unit,
    onVerifyCashPayment: (PaymentRecord, Order?) -> Unit
) {
    val tables = (tablesState as? UiState.Success)?.data.orEmpty()
    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val requests = (requestsState as? UiState.Success)?.data.orEmpty()
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()

    var inspectedTable by remember { mutableStateOf<RestaurantTable?>(null) }

    if (!isOwnerOrStaff) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Staff Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Sign in with an authorized Waiter, Manager, or Owner Google account to manage the live floor map, confirm orders, and respond to service calls.")
                }
            }
        }
        return
    }

    val pendingOrderConfirmations = orders.filter { it.statusEnum() == OrderStatus.NEW_ORDER && !it.waiterConfirmed }
    val activeRequests = requests.filter { it.statusEnum() != RequestStatus.COMPLETED }
    val pendingPayments = payments.filter { it.status != "VERIFIED_PAID" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Strip
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WaiterKpiCard("Orders to Confirm", "${pendingOrderConfirmations.size}", if (pendingOrderConfirmations.isNotEmpty()) AmberContainer else OliveContainer, Modifier.weight(1f))
                WaiterKpiCard("Service Calls", "${activeRequests.size}", if (activeRequests.isNotEmpty()) CrimsonContainer else OliveContainer, Modifier.weight(1f))
                WaiterKpiCard("Occupied", "${tables.count { it.statusEnum() != TableStatus.AVAILABLE }}/${tables.size}", SaffronContainer, Modifier.weight(1f))
            }
        }

        // Waiter Order Confirmation Queue -> Sends to Kitchen Stations
        if (pendingOrderConfirmations.isNotEmpty()) {
            item {
                Text(
                    text = if (language == AppLanguage.AR) "طلبات بانتظار تأكيد النادل للمطبخ" else "Orders Awaiting Waiter Confirmation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SaffronPrimary
                )
            }
            items(pendingOrderConfirmations, key = { "confirm_${it.id}" }) { ord ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberContainer.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("waiter_order_card_${ord.orderNumber}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restaurant, null, tint = SaffronPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${ord.orderNumber} • Table T%02d".format(ord.tableNumber),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            StatusPill("%.2f SAR".format(ord.totalSar), Color.White, EspressoInk)
                        }
                        Text(
                            text = ord.items.joinToString(" • ") { "${it.quantity}x ${it.nameEn} (${it.kitchenStation})" },
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!ord.specialInstructions.isNullOrBlank()) {
                            Text("Note: ${ord.specialInstructions}", style = MaterialTheme.typography.labelMedium, color = SumacCrimson)
                        }
                        Button(
                            onClick = { onConfirmOrderToKitchen(ord) },
                            colors = ButtonDefaults.buttonColors(containerColor = OliveEmerald),
                            modifier = Modifier.fillMaxWidth().testTag("waiter_confirm_${ord.orderNumber}")
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Confirm & Send to Kitchen")
                        }
                    }
                }
            }
        }

        // Live Service Request Queue
        item {
            Text(
                text = if (language == AppLanguage.AR) "طلبات الخدمة المباشرة" else "Live Table Service Queue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (activeRequests.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No pending customer service calls.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(activeRequests, key = { it.id }) { req ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonContainer.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, SumacCrimson.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, null, tint = SumacCrimson)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Table T%02d • ${req.typeEnum().labelEn}".format(req.tableNumber),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            StatusPill(req.statusEnum().labelEn, Color.White, SumacCrimson)
                        }
                        if (!req.note.isNullOrBlank()) {
                            Text(req.note, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (req.statusEnum() == RequestStatus.PENDING) {
                                Button(
                                    onClick = { onAdvanceRequest(req, RequestStatus.ACCEPTED) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                                ) {
                                    Text("Accept")
                                }
                            }
                            if (req.statusEnum() == RequestStatus.PENDING || req.statusEnum() == RequestStatus.ACCEPTED) {
                                OutlinedButton(onClick = { onAdvanceRequest(req, RequestStatus.IN_PROGRESS) }) {
                                    Text("In Progress")
                                }
                            }
                            Button(
                                onClick = { onAdvanceRequest(req, RequestStatus.COMPLETED) },
                                colors = ButtonDefaults.buttonColors(containerColor = OliveEmerald)
                            ) {
                                Text("Complete")
                            }
                        }
                    }
                }
            }
        }

        // Cashier Cash / Bank Transfer Verification Queue
        if (pendingPayments.isNotEmpty()) {
            item {
                Text(
                    text = "Cashier Payment Verification Queue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(pendingPayments, key = { "pay_${it.id}" }) { pay ->
                val linkedOrder = orders.firstOrNull { it.id == pay.orderId }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PointOfSale, null, tint = AmberGold)
                                Spacer(Modifier.width(6.dp))
                                Text("Table T%02d • %.2f SAR".format(pay.tableNumber, pay.amountSar), fontWeight = FontWeight.Bold)
                            }
                            Text("${pay.method} • Ref: ${pay.gatewayTransactionId}", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { onVerifyCashPayment(pay, linkedOrder) },
                            colors = ButtonDefaults.buttonColors(containerColor = OliveEmerald)
                        ) {
                            Text("Confirm Paid")
                        }
                    }
                }
            }
        }

        // Live Restaurant Floor Map
        item {
            Text(
                text = if (language == AppLanguage.AR) "خريطة طاولات المطعم المباشرة" else "Live Restaurant Floor Map",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            LiveFloorMapBoard(
                tables = tables,
                orders = orders,
                requests = requests,
                onInspectTable = { inspectedTable = it }
            )
        }
    }

    if (inspectedTable != null) {
        val tbl = inspectedTable!!
        AlertDialog(
            onDismissRequest = { inspectedTable = null },
            title = { Text("Manage ${tbl.label} (${tbl.zone})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Waiter: ${tbl.assignedWaiterName}")
                    Text("Current Status: ${tbl.statusEnum().labelEn}")
                    Text("Update Table Status:", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        TableStatus.entries.forEach { st ->
                            FilterChip(
                                selected = tbl.statusEnum() == st,
                                onClick = {
                                    onUpdateTableStatus(tbl.id, st, null)
                                    inspectedTable = null
                                },
                                label = { Text(st.labelEn) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedTable = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun WaiterKpiCard(title: String, value: String, bg: Color, modifier: Modifier = Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EspressoInk)
            Text(title, style = MaterialTheme.typography.labelSmall, color = EspressoInk)
        }
    }
}
