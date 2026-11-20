package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.PaymentRecord
import com.example.data.model.RestaurantSettings
import com.example.data.model.RestaurantTable
import com.example.data.model.Review
import com.example.data.model.ServiceRequest
import com.example.data.model.StaffMember
import com.example.data.model.StaffRole
import com.example.data.model.TableStatus
import com.example.data.model.UiState
import com.example.ui.components.DishPhoto
import com.example.ui.components.SingleTableQrDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.OliveContainer
import com.example.ui.theme.OliveEmerald
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SumacCrimson

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    isAdminOrManager: Boolean,
    callerRole: StaffRole?,
    menuState: UiState<List<MenuItem>>,
    tablesState: UiState<List<RestaurantTable>>,
    ordersState: UiState<List<Order>>,
    requestsState: UiState<List<ServiceRequest>>,
    paymentsState: UiState<List<PaymentRecord>>,
    reviewsState: UiState<List<Review>>,
    staffState: UiState<List<StaffMember>>,
    settings: RestaurantSettings,
    onUploadDishPhoto: (Context, Uri, String?, (String, String) -> Unit, (String) -> Unit) -> Unit,
    onSaveMenuItem: (MenuItem) -> Unit,
    onDeleteMenuItem: (String) -> Unit,
    onSaveTable: (Int, String, Int, String, Boolean) -> Unit,
    onToggleTableActive: (RestaurantTable) -> Unit,
    onDeleteTable: (RestaurantTable) -> Unit,
    onDownloadSingleQr: (Context, RestaurantTable) -> Unit,
    onDownloadAllQrPdf: (Context, List<RestaurantTable>) -> Unit,
    onPrintQrTables: (Context, List<RestaurantTable>) -> Unit,
    onConfirmOrder: (Order) -> Unit,
    onUpdateTableStatus: (String, TableStatus, String?) -> Unit,
    onSaveStaff: (String, String, String, StaffRole, String, Boolean) -> Unit,
    onToggleStaffActive: (StaffMember) -> Unit,
    onDeleteStaff: (String) -> Unit,
    onSaveSettings: (RestaurantSettings) -> Unit,
    onSeedDemoOrder: (List<RestaurantTable>) -> Unit,
    onGenerateBackupJson: (List<MenuItem>, List<RestaurantTable>, List<Order>, RestaurantSettings) -> String,
    onShowMessage: (String) -> Unit
) {
    if (!isAdminOrManager) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Card {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Owner / Manager Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Sign in with an authorized Owner or Manager Google account to access Sales Analytics, Live Floor, Menu CRUD, QR Export & Print, Staff RBAC, and Payment Gateway Settings.")
                }
            }
        }
        return
    }

    val context = LocalContext.current
    val menu = (menuState as? UiState.Success)?.data.orEmpty()
    val tables = (tablesState as? UiState.Success)?.data.orEmpty()
    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val requests = (requestsState as? UiState.Success)?.data.orEmpty()
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
    val reviews = (reviewsState as? UiState.Success)?.data.orEmpty()
    val staff = (staffState as? UiState.Success)?.data.orEmpty()
    val clipboard = LocalClipboardManager.current

    var tabIndex by remember { mutableIntStateOf(0) }
    var includeDemoData by remember { mutableStateOf(false) }
    var timeWindow by remember { mutableStateOf("Daily") }
    var editingMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var editingTable by remember { mutableStateOf<RestaurantTable?>(null) }
    var showAddTable by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<StaffMember?>(null) }
    var showAddStaff by remember { mutableStateOf(false) }
    var qrTableDialog by remember { mutableStateOf<RestaurantTable?>(null) }
    var backupPreview by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = tabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            edgePadding = 8.dp
        ) {
            listOf("Sales & VAT", "Live Floor", "Menu", "Tables & QR", "Staff & Setup").forEachIndexed { idx, title ->
                Tab(
                    selected = tabIndex == idx,
                    onClick = { tabIndex = idx },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("admin_tab_$idx")
                )
            }
        }

        when (tabIndex) {
            0 -> {
                val nowSec = System.currentTimeMillis() / 1000L
                val windowSec = when (timeWindow) {
                    "Daily" -> 86400L
                    "Weekly" -> 7 * 86400L
                    else -> 30 * 86400L
                }
                val filteredOrders = orders.filter { ord ->
                    val demoMatch = includeDemoData || !ord.demoData
                    val createdSec = ord.createdAt?.seconds ?: nowSec
                    demoMatch && (nowSec - createdSec <= windowSec)
                }
                val totalSales = filteredOrders.sumOf { it.totalSar }
                val totalVat = filteredOrders.sumOf { it.vatSar }
                val avgOrder = if (filteredOrders.isEmpty()) 0.0 else totalSales / filteredOrders.size

                val topDishes = filteredOrders.flatMap { it.items }
                    .groupBy { it.nameEn }
                    .mapValues { (_, list) -> list.sumOf { it.quantity } }
                    .entries.sortedByDescending { it.value }
                    .take(5)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Daily", "Weekly", "Monthly").forEach { w ->
                                    FilterChip(selected = timeWindow == w, onClick = { timeWindow = w }, label = { Text(w) })
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Demo", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(4.dp))
                                Switch(checked = includeDemoData, onCheckedChange = { includeDemoData = it })
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminMetricCard("Sales (SAR)", "%.2f".format(totalSales), SaffronContainer, Modifier.weight(1f))
                            AdminMetricCard("15% Saudi VAT", "%.2f".format(totalVat), OliveContainer, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AdminMetricCard("Total Orders", "${filteredOrders.size}", AmberContainer, Modifier.weight(1f))
                            AdminMetricCard("Avg Order Value", "%.2f".format(avgOrder), MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Revenue Trend ($timeWindow)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))
                                val bars = if (filteredOrders.isEmpty()) listOf(0f) else filteredOrders.take(7).map { it.totalSar.toFloat() }
                                val maxVal = (bars.maxOrNull() ?: 1f).coerceAtLeast(1f)
                                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                                    val slotWidth = size.width / bars.size.coerceAtLeast(1)
                                    val barWidth = (slotWidth * 0.55f).coerceAtMost(44.dp.toPx())
                                    bars.forEachIndexed { idx, value ->
                                        val h = (value / maxVal) * (size.height - 16f)
                                        val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                                        drawRect(
                                            color = SaffronPrimary,
                                            topLeft = Offset(x, size.height - h),
                                            size = Size(barWidth, h.coerceAtLeast(6f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Best-Selling Products", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (topDishes.isEmpty()) {
                                    Text("No order items in selected period yet.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    topDishes.forEach { (name, qty) ->
                                        SummaryRow(name, "$qty sold")
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("Payment Methods (${payments.size} tx)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                val byMethod = payments.groupBy { it.method }.mapValues { it.value.sumOf { p -> p.amountSar } }
                                if (byMethod.isEmpty()) {
                                    Text("No payments recorded yet.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    byMethod.forEach { (m, amt) ->
                                        SummaryRow(m, "%.2f SAR".format(amt))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val avgRating = if (reviews.isEmpty()) 0.0 else reviews.map { it.overallRating }.average()
                                Text("Customer Feedback (${reviews.size} reviews • Avg ${"%.1f".format(avgRating)}★)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                reviews.take(3).forEach { r ->
                                    Text("• ${r.authorName} (${r.overallRating}★): ${r.comment}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { onSeedDemoOrder(tables) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Science, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Create Demo Order")
                                    }
                                    Button(
                                        onClick = { backupPreview = onGenerateBackupJson(menu, tables, orders, settings) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Backup, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Export Backup")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> AdminLiveFloorAndOrdersTab(
                tables = tables,
                orders = orders,
                requests = requests,
                onConfirmOrder = onConfirmOrder,
                onUpdateTableStatus = onUpdateTableStatus
            )

            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAddMenu = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("admin_add_menu_button")
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add New Product")
                        }
                    }
                    items(menu, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DishPhoto(
                                        imageKey = item.imageKey,
                                        imageUrl = item.imageUrl,
                                        contentDescription = item.nameEn,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${item.nameEn} • ${item.nameAr}", fontWeight = FontWeight.Bold)
                                        Text("${item.categoryEnum().labelEn} • ${"%.2f".format(item.priceSar)} SAR • ${item.stationEnum().name}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row {
                                        IconButton(onClick = { editingMenuItem = item }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Dish")
                                        }
                                        IconButton(onClick = { onSaveMenuItem(item.copy(hidden = !item.hidden)) }) {
                                            Icon(
                                                if (item.hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle Hidden"
                                            )
                                        }
                                        IconButton(onClick = { onDeleteMenuItem(item.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Dish", tint = SumacCrimson)
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FilterChip(
                                        selected = !item.available,
                                        onClick = { onSaveMenuItem(item.copy(available = !item.available)) },
                                        label = { Text(if (item.available) "In Stock" else "Sold Out") }
                                    )
                                    if (item.isMealDeal) {
                                        StatusPill("Meal Deal", SaffronContainer, EspressoInk)
                                    }
                                    if (item.hidden) {
                                        StatusPill("Hidden", CrimsonContainer, SumacCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> AdminTablesAndQrTab(
                tables = tables,
                onAddTable = { showAddTable = true },
                onEditTable = { editingTable = it },
                onToggleTableActive = onToggleTableActive,
                onDeleteTable = onDeleteTable,
                onShowQrPreview = { qrTableDialog = it },
                onDownloadSingleQr = { tbl -> onDownloadSingleQr(context, tbl) },
                onPrintSingleQr = { tbl -> onPrintQrTables(context, listOf(tbl)) },
                onDownloadAllQrPdf = { list -> onDownloadAllQrPdf(context, list) },
                onPrintAllQrs = { list -> onPrintQrTables(context, list) }
            )

            4 -> {
                var wifiSsid by remember(settings.wifiSsid) { mutableStateOf(settings.wifiSsid) }
                var wifiPass by remember(settings.wifiPassword) { mutableStateOf(settings.wifiPassword) }
                var hours by remember(settings.openingHours) { mutableStateOf(settings.openingHours) }
                var vatText by remember(settings.vatPercent) { mutableStateOf(settings.vatPercent.toString()) }
                var googleUrl by remember(settings.googleReviewUrl) { mutableStateOf(settings.googleReviewUrl) }
                var gwProvider by remember(settings.gatewayProvider) { mutableStateOf(settings.gatewayProvider) }
                var gwPubKey by remember(settings.gatewayPublicKey) { mutableStateOf(settings.gatewayPublicKey) }
                var gwEndpoint by remember(settings.gatewayEndpoint) { mutableStateOf(settings.gatewayEndpoint) }
                var gwMode by remember(settings.gatewayMode) { mutableStateOf(settings.gatewayMode) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Staff & Role Access (RBAC)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedButton(onClick = { showAddStaff = true }, modifier = Modifier.testTag("admin_add_staff_btn")) {
                                Text("+ Authorize Staff")
                            }
                        }
                    }
                    items(staff, key = { it.id }) { member ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${member.name} (${member.roleEnum().labelEn})", fontWeight = FontWeight.Bold)
                                    Text("${member.email} • Zone: ${member.assignedZone}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(
                                        label = if (member.active) "Active" else "Paused",
                                        containerColor = if (member.active) OliveContainer else CrimsonContainer,
                                        contentColor = if (member.active) OliveEmerald else SumacCrimson
                                    )
                                    IconButton(onClick = { editingStaff = member }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Staff")
                                    }
                                    if (callerRole == StaffRole.OWNER || member.roleEnum() != StaffRole.OWNER) {
                                        IconButton(onClick = { onDeleteStaff(member.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Staff", tint = SumacCrimson)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Restaurant & Saudi Payment Gateway Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Opening Hours") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = wifiSsid, onValueChange = { wifiSsid = it }, label = { Text("Guest Wi-Fi SSID") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = wifiPass, onValueChange = { wifiPass = it }, label = { Text("Guest Wi-Fi Password") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = vatText, onValueChange = { vatText = it }, label = { Text("Saudi VAT (%)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = googleUrl, onValueChange = { googleUrl = it }, label = { Text("Google Review Page URL") }, modifier = Modifier.fillMaxWidth())

                                Text("Saudi Payment Gateway Provider (Moyasar / HyperPay / Tap)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("MOYASAR", "HYPERPAY", "TAP").forEach { prov ->
                                        FilterChip(selected = gwProvider == prov, onClick = { gwProvider = prov }, label = { Text(prov) })
                                    }
                                    listOf("TEST", "LIVE").forEach { mode ->
                                        FilterChip(selected = gwMode == mode, onClick = { gwMode = mode }, label = { Text(mode) })
                                    }
                                }
                                OutlinedTextField(value = gwEndpoint, onValueChange = { gwEndpoint = it }, label = { Text("Gateway API Endpoint") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = gwPubKey, onValueChange = { gwPubKey = it }, label = { Text("Gateway Publishable Key") }, modifier = Modifier.fillMaxWidth())

                                Button(
                                    onClick = {
                                        onSaveSettings(
                                            settings.copy(
                                                openingHours = hours,
                                                wifiSsid = wifiSsid,
                                                wifiPassword = wifiPass,
                                                vatPercent = vatText.toDoubleOrNull() ?: 15.0,
                                                googleReviewUrl = googleUrl,
                                                gatewayProvider = gwProvider,
                                                gatewayPublicKey = gwPubKey,
                                                gatewayEndpoint = gwEndpoint,
                                                gatewayMode = gwMode
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Save All Settings")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMenu || editingMenuItem != null) {
        MenuEditorDialog(
            initial = editingMenuItem,
            onUploadPhoto = onUploadDishPhoto,
            onSave = onSaveMenuItem,
            onDismiss = { showAddMenu = false; editingMenuItem = null }
        )
    }

    if (showAddTable || editingTable != null) {
        val nextNum = (tables.maxOfOrNull { it.tableNumber } ?: 0) + 1
        TableEditorDialog(
            initial = editingTable,
            suggestedNumber = nextNum,
            onSave = onSaveTable,
            onDismiss = { showAddTable = false; editingTable = null }
        )
    }

    if (showAddStaff || editingStaff != null) {
        StaffEditorDialog(
            callerRole = callerRole,
            initial = editingStaff,
            onSave = onSaveStaff,
            onDismiss = { showAddStaff = false; editingStaff = null }
        )
    }

    if (qrTableDialog != null) {
        val tbl = qrTableDialog!!
        SingleTableQrDialog(
            table = tbl,
            onDownloadPng = { onDownloadSingleQr(context, tbl) },
            onPrintQr = { onPrintQrTables(context, listOf(tbl)) },
            onDismiss = { qrTableDialog = null }
        )
    }

    if (backupPreview != null) {
        AlertDialog(
            onDismissRequest = { backupPreview = null },
            title = { Text("JSON Backup Snapshot") },
            text = { Text(backupPreview!!.take(800)) },
            confirmButton = {
                Button(onClick = {
                    clipboard.setText(AnnotatedString(backupPreview!!))
                    onShowMessage("Full JSON backup copied to clipboard.")
                    backupPreview = null
                }) { Text("Copy JSON") }
            },
            dismissButton = {
                OutlinedButton(onClick = { backupPreview = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun AdminMetricCard(title: String, value: String, bg: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EspressoInk)
            Text(title, style = MaterialTheme.typography.labelMedium, color = EspressoInk)
        }
    }
}
