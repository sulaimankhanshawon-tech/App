package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentRecord
import com.example.data.model.RestaurantSettings
import com.example.data.model.SplitMode
import com.example.data.model.UiState
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.getStrings
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.OliveContainer
import com.example.ui.theme.OliveEmerald
import com.example.ui.theme.SaffronPrimary
import com.example.util.QrCodeGenerator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillingFeedbackScreen(
    ordersState: UiState<List<Order>>,
    paymentsState: UiState<List<PaymentRecord>>,
    settings: RestaurantSettings,
    userEmail: String,
    activeTableNumber: Int,
    lastGatewayMessage: String?,
    language: AppLanguage,
    onClearGatewayMessage: () -> Unit,
    onProcessPayment: (Order, Double, Double, PaymentMethod, SplitMode, String, String?, List<String>) -> Unit,
    onSubmitReview: (String?, Int, Int, Int, Int, String) -> Unit
) {
    val context = LocalContext.current
    val strings = remember(language) { getStrings(language) }
    val orders = (ordersState as? UiState.Success)?.data.orEmpty()
    val payments = (paymentsState as? UiState.Success)?.data.orEmpty()

    val tableOrders = remember(orders, activeTableNumber) {
        orders.filter { it.tableNumber == activeTableNumber }
    }
    val currentOrder = tableOrders.firstOrNull { it.paymentStatus != "PAID" && it.statusEnum() != OrderStatus.CANCELLED }
        ?: tableOrders.firstOrNull()
        ?: orders.firstOrNull { it.paymentStatus != "PAID" && it.statusEnum() != OrderStatus.CANCELLED }
        ?: orders.firstOrNull()

    var splitMode by remember { mutableStateOf(SplitMode.FULL) }
    var splitGuests by remember { mutableIntStateOf(2) }
    var selectedItemIndices by remember { mutableStateOf(setOf<Int>()) }
    var tipPercent by remember { mutableIntStateOf(0) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.MADA) }
    var gatewaySecretToken by remember { mutableStateOf("") }
    var receiptEmail by remember(userEmail) { mutableStateOf(userEmail) }

    // Feedback form state
    var overallStars by remember { mutableIntStateOf(5) }
    var foodStars by remember { mutableIntStateOf(5) }
    var serviceStars by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (currentOrder == null) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(strings.digitalBillTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.AR) "لا توجد فاتورة نشطة حالياً. أضف أطباقاً من القائمة لإرسال طلبك." else "No active bill yet. Order from the menu to generate your live Saudi VAT bill.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            // Itemized Digital Bill + Split + Payment
            item {
                val baseRemaining = (currentOrder.totalSar - currentOrder.paidAmountSar).coerceAtLeast(0.0)
                val splitBaseAmount = when (splitMode) {
                    SplitMode.FULL -> baseRemaining
                    SplitMode.EQUAL -> baseRemaining / splitGuests.coerceAtLeast(2)
                    SplitMode.BY_ITEMS -> {
                        val selectedSub = selectedItemIndices.sumOf { idx ->
                            currentOrder.items.getOrNull(idx)?.lineTotal() ?: 0.0
                        }
                        selectedSub * (1.0 + settings.vatPercent / 100.0)
                    }
                }
                val tipAmount = (splitBaseAmount * (tipPercent / 100.0)).let { Math.round(it * 100.0) / 100.0 }
                val finalPayAmount = (splitBaseAmount + tipAmount).let { Math.round(it * 100.0) / 100.0 }

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(strings.digitalBillTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("${currentOrder.orderNumber} • Table T%02d".format(currentOrder.tableNumber), style = MaterialTheme.typography.bodySmall)
                            }
                            StatusPill(
                                label = currentOrder.paymentStatus,
                                containerColor = if (currentOrder.paymentStatus == "PAID") OliveContainer else AmberContainer,
                                contentColor = if (currentOrder.paymentStatus == "PAID") OliveEmerald else AmberGold
                            )
                        }

                        HorizontalDivider()

                        // Itemized list (selectable when Split by Items)
                        currentOrder.items.forEachIndexed { idx, line ->
                            val isSelected = idx in selectedItemIndices
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = splitMode == SplitMode.BY_ITEMS) {
                                        selectedItemIndices = if (isSelected) selectedItemIndices - idx else selectedItemIndices + idx
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (splitMode == SplitMode.BY_ITEMS) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                selectedItemIndices = if (isSelected) selectedItemIndices - idx else selectedItemIndices + idx
                                            }
                                        )
                                    }
                                    Column {
                                        Text("${line.quantity}x ${if (language == AppLanguage.AR) line.nameAr else line.nameEn}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(line.selectedSize, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("%.2f %s".format(line.lineTotal(), strings.sarCurrency), fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider()
                        SummaryRow(strings.subtotal, "%.2f %s".format(currentOrder.subtotalSar, strings.sarCurrency))
                        if (currentOrder.discountSar > 0) {
                            SummaryRow(strings.discount, "-%.2f %s".format(currentOrder.discountSar, strings.sarCurrency), OliveEmerald)
                        }
                        SummaryRow(strings.vat15, "%.2f %s".format(currentOrder.vatSar, strings.sarCurrency))
                        if (currentOrder.paidAmountSar > 0) {
                            SummaryRow("Paid so far", "-%.2f %s".format(currentOrder.paidAmountSar, strings.sarCurrency), OliveEmerald)
                        }
                        SummaryRow(strings.tip, "%.2f %s".format(tipAmount, strings.sarCurrency))
                        SummaryRow(strings.total, "%.2f %s".format(finalPayAmount, strings.sarCurrency), SaffronPrimary, bold = true)

                        if (currentOrder.paymentStatus != "PAID") {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (language == AppLanguage.AR) "تقسيم الفاتورة" else "Split Bill",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SplitMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = splitMode == mode,
                                        onClick = { splitMode = mode },
                                        label = { Text(if (language == AppLanguage.AR) mode.labelAr else mode.labelEn) }
                                    )
                                }
                            }
                            if (splitMode == SplitMode.EQUAL) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Guests:")
                                    listOf(2, 3, 4, 5).forEach { count ->
                                        FilterChip(
                                            selected = splitGuests == count,
                                            onClick = { splitGuests = count },
                                            label = { Text("$count") }
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (language == AppLanguage.AR) "إكرامية اختيارية" else "Optional Tip",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0, 5, 10, 15).forEach { pct ->
                                    FilterChip(
                                        selected = tipPercent == pct,
                                        onClick = { tipPercent = pct },
                                        label = { Text("$pct%") }
                                    )
                                }
                            }

                            Text(
                                text = if (language == AppLanguage.AR) "طريقة الدفع" else "Payment Method",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                PaymentMethod.entries.forEach { m ->
                                    FilterChip(
                                        selected = selectedMethod == m,
                                        onClick = { selectedMethod = m },
                                        label = { Text(if (language == AppLanguage.AR) m.labelAr else m.labelEn) }
                                    )
                                }
                            }

                            if (selectedMethod in listOf(PaymentMethod.MADA, PaymentMethod.APPLE_PAY, PaymentMethod.GOOGLE_PAY, PaymentMethod.CARD)) {
                                OutlinedTextField(
                                    value = gatewaySecretToken,
                                    onValueChange = { gatewaySecretToken = it },
                                    label = { Text("${settings.gatewayProvider} Verification Token (Live Gateway)") },
                                    placeholder = { Text("Configured via Merchant Gateway / Secrets") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            OutlinedTextField(
                                value = receiptEmail,
                                onValueChange = { receiptEmail = it },
                                label = { Text(if (language == AppLanguage.AR) "البريد الإلكتروني للإيصال" else "Email for Digital Receipt") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val chosenIds = selectedItemIndices.mapNotNull { idx ->
                                        currentOrder.items.getOrNull(idx)?.itemId
                                    }
                                    onProcessPayment(
                                        currentOrder,
                                        finalPayAmount,
                                        tipAmount,
                                        selectedMethod,
                                        splitMode,
                                        gatewaySecretToken,
                                        receiptEmail,
                                        chosenIds
                                    )
                                },
                                enabled = finalPayAmount > 0.0,
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("pay_now_button")
                            ) {
                                Text("${strings.payNow} • %.2f ${strings.sarCurrency}".format(finalPayAmount), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Digital Receipts History (ZATCA style with QR + Email Receipt)
        val verifiedPayments = payments.filter { it.status == "VERIFIED_PAID" }
        if (verifiedPayments.isNotEmpty()) {
            item {
                val latest = verifiedPayments.first()
                val receiptPayload = "ASIL AL TAWOOQ JEDDAH | TX:${latest.gatewayTransactionId} | TOTAL:${latest.amountSar} SAR | VAT:${latest.vatSar} SAR"
                val receiptQr = remember(receiptPayload) { QrCodeGenerator.generateQrBitmap(receiptPayload, 260) }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = OliveContainer.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, null, tint = OliveEmerald)
                            Spacer(Modifier.width(8.dp))
                            Text("Digital VAT Receipt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Tx: ${latest.gatewayTransactionId}", style = MaterialTheme.typography.bodySmall)
                                Text("Method: ${latest.method} (${latest.gatewayProvider})", style = MaterialTheme.typography.bodySmall)
                                Text("Paid: %.2f SAR (incl. %.2f VAT)".format(latest.amountSar, latest.vatSar), fontWeight = FontWeight.Bold)
                            }
                            if (receiptQr != null) {
                                Image(
                                    bitmap = receiptQr,
                                    contentDescription = "Receipt QR",
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(4.dp)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val mailUri = Uri.parse("mailto:${latest.receiptEmail ?: receiptEmail}")
                                val intent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, "Asil Al Tawooq Jeddah - Digital Receipt ${latest.gatewayTransactionId}")
                                    putExtra(Intent.EXTRA_TEXT, receiptPayload)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.emailReceipt)
                        }
                    }
                }
            }
        }

        // Customer Feedback & Google Review Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, null, tint = SaffronPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.feedbackTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    StarRatingRow("Overall", overallStars) { overallStars = it }
                    StarRatingRow("Food Quality", foodStars) { foodStars = it }
                    StarRatingRow("Service", serviceStars) { serviceStars = it }

                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text(if (language == AppLanguage.AR) "شاركنا رأيك" else "Share your feedback") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onSubmitReview(currentOrder?.id, activeTableNumber, overallStars, foodStars, serviceStars, reviewComment)
                                reviewComment = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.submitReview)
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(settings.googleReviewUrl))
                                runCatching { context.startActivity(intent) }
                            }
                        ) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.rateOnGoogle)
                        }
                    }
                }
            }
        }
    }

    if (lastGatewayMessage != null) {
        AlertDialog(
            onDismissRequest = onClearGatewayMessage,
            title = { Text("Payment Status") },
            text = { Text(lastGatewayMessage) },
            confirmButton = {
                Button(onClick = onClearGatewayMessage) { Text("OK") }
            }
        )
    }
}

@Composable
private fun StarRatingRow(label: String, rating: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Row {
            (1..5).forEach { star ->
                IconButton(onClick = { onChange(star) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = "$star stars",
                        tint = SaffronPrimary
                    )
                }
            }
        }
    }
}
