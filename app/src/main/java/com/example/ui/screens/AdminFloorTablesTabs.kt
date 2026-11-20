package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.material3.Switch
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
import com.example.data.model.RestaurantTable
import com.example.data.model.ServiceRequest
import com.example.data.model.TableStatus
import com.example.ui.components.LiveFloorMapBoard
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.OliveContainer
import com.example.ui.theme.OliveEmerald
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SumacCrimson

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminLiveFloorAndOrdersTab(
    tables: List<RestaurantTable>,
    orders: List<Order>,
    requests: List<ServiceRequest>,
    onConfirmOrder: (Order) -> Unit,
    onUpdateTableStatus: (String, TableStatus, String?) -> Unit
) {
    var inspectedTable by remember { mutableStateOf<RestaurantTable?>(null) }
    val pendingConfirmOrders = remember(orders) {
        orders.filter { it.statusEnum() == OrderStatus.NEW_ORDER && !it.waiterConfirmed }
    }
    val activeOrders = remember(orders) {
        orders.filter { it.statusEnum() != OrderStatus.PAID && it.statusEnum() != OrderStatus.CANCELLED }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Live Floor Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LiveFloorMapBoard(
                tables = tables,
                orders = orders,
                requests = requests,
                onInspectTable = { inspectedTable = it }
            )
        }

        if (pendingConfirmOrders.isNotEmpty()) {
            item {
                Text(
                    text = "Orders Awaiting Confirmation (${pendingConfirmOrders.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SumacCrimson
                )
            }
            items(pendingConfirmOrders, key = { "pending_${it.id}" }) { ord ->
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
                            Text("${ord.orderNumber} • Table T%02d".format(ord.tableNumber), fontWeight = FontWeight.Bold)
                            Text("${ord.items.sumOf { it.quantity }} items • %.2f SAR • ${ord.customerName}".format(ord.totalSar), style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { onConfirmOrder(ord) },
                            colors = ButtonDefaults.buttonColors(containerColor = OliveEmerald)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Send to Kitchen")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Active Orders (${activeOrders.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (activeOrders.isEmpty()) {
            item {
                Text("No active restaurant orders at the moment.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(activeOrders, key = { "ord_${it.id}" }) { ord ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${ord.orderNumber} • Table T%02d (${ord.customerName})".format(ord.tableNumber), fontWeight = FontWeight.Bold)
                            Text(
                                "${ord.items.sumOf { it.quantity }} items • %.2f SAR • Stations: ${ord.items.map { it.kitchenStation }.distinct().joinToString()}".format(ord.totalSar),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        StatusPill(
                            label = ord.statusEnum().labelEn,
                            containerColor = if (ord.waiterConfirmed) OliveContainer else AmberContainer,
                            contentColor = if (ord.waiterConfirmed) OliveEmerald else AmberGold
                        )
                    }
                }
            }
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
                    Text("Status: ${tbl.statusEnum().labelEn} • Active: ${if (tbl.active) "Yes" else "No"}")
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
fun AdminTablesAndQrTab(
    tables: List<RestaurantTable>,
    onAddTable: () -> Unit,
    onEditTable: (RestaurantTable) -> Unit,
    onToggleTableActive: (RestaurantTable) -> Unit,
    onDeleteTable: (RestaurantTable) -> Unit,
    onShowQrPreview: (RestaurantTable) -> Unit,
    onDownloadSingleQr: (RestaurantTable) -> Unit,
    onPrintSingleQr: (RestaurantTable) -> Unit,
    onDownloadAllQrPdf: (List<RestaurantTable>) -> Unit,
    onPrintAllQrs: (List<RestaurantTable>) -> Unit
) {
    var tablePendingDelete by remember { mutableStateOf<RestaurantTable?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddTable,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("admin_add_table_button")
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add New Table")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onDownloadAllQrPdf(tables) },
                        enabled = tables.isNotEmpty(),
                        modifier = Modifier.weight(1f).testTag("admin_download_all_qr_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Download All QR")
                    }
                    Button(
                        onClick = { onPrintAllQrs(tables) },
                        enabled = tables.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = EspressoInk),
                        modifier = Modifier.weight(1f).testTag("admin_print_all_qr_btn")
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Print All QRs")
                    }
                }
            }
        }

        items(tables, key = { it.id }) { tbl ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (tbl.active) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFECECEC)
                ),
                modifier = Modifier.fillMaxWidth().testTag("admin_table_card_${tbl.label}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${tbl.label} = Table ${tbl.tableNumber}", fontWeight = FontWeight.Bold)
                                StatusPill(
                                    label = if (tbl.active) "Active" else "Inactive",
                                    containerColor = if (tbl.active) OliveContainer else CrimsonContainer,
                                    contentColor = if (tbl.active) OliveEmerald else SumacCrimson
                                )
                            }
                            Text("${tbl.zone} • ${tbl.capacity} Seats • Waiter: ${tbl.assignedWaiterName}", style = MaterialTheme.typography.bodySmall)
                            Text(tbl.qrPayload, style = MaterialTheme.typography.labelSmall, color = SaffronPrimary)
                        }
                        Switch(
                            checked = tbl.active,
                            onCheckedChange = { onToggleTableActive(tbl) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onShowQrPreview(tbl) },
                            colors = ButtonDefaults.buttonColors(containerColor = EspressoInk),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("QR")
                        }
                        OutlinedButton(
                            onClick = { onDownloadSingleQr(tbl) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PNG")
                        }
                        OutlinedButton(
                            onClick = { onPrintSingleQr(tbl) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Print")
                        }
                        IconButton(onClick = { onEditTable(tbl) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Table")
                        }
                        IconButton(onClick = { tablePendingDelete = tbl }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Table", tint = SumacCrimson)
                        }
                    }
                }
            }
        }
    }

    if (tablePendingDelete != null) {
        val target = tablePendingDelete!!
        AlertDialog(
            onDismissRequest = { tablePendingDelete = null },
            title = { Text("Delete Table ${target.label}?") },
            text = { Text("This removes Table ${target.label} and invalidates its QR code (${target.qrPayload}).") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTable(target)
                        tablePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SumacCrimson)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { tablePendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
