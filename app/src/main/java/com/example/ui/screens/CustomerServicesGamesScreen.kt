package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GameSession
import com.example.data.model.GameType
import com.example.data.model.LoyaltyProfile
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.RestaurantSettings
import com.example.data.model.RestaurantTable
import com.example.data.model.ServiceRequestType
import com.example.data.model.UiState
import com.example.data.repository.DefaultSeedData
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.getStrings
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.OliveContainer
import com.example.ui.theme.OliveEmerald
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SaffronVibrant
import com.example.util.QrCodeGenerator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerServicesGamesScreen(
    activeTable: RestaurantTable?,
    activeTableLabel: String?,
    ordersState: UiState<List<Order>>,
    loyaltyState: UiState<LoyaltyProfile?>,
    gamesState: UiState<List<GameSession>>,
    settings: RestaurantSettings,
    language: AppLanguage,
    onOpenQrScanner: () -> Unit,
    onServiceRequest: (ServiceRequestType, String) -> Unit,
    onRedeemReward: () -> Unit,
    onSaveBirthday: (String) -> Unit,
    onStartGame: (Int, GameType) -> Unit,
    onAnswerGame: (GameSession, Boolean, Boolean) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val strings = remember(language) { getStrings(language) }
    val clipboard = LocalClipboardManager.current
    var showOtherDialog by remember { mutableStateOf(false) }
    var otherNote by remember { mutableStateOf("") }
    var birthdayInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Order Tracker
        item {
            val activeOrder = (ordersState as? UiState.Success)?.data
                ?.firstOrNull {
                    it.statusEnum() != OrderStatus.PAID &&
                        it.statusEnum() != OrderStatus.CANCELLED &&
                        (activeTable == null || it.tableNumber == activeTable.tableNumber)
                }
            if (activeOrder != null) {
                val progress = when (activeOrder.statusEnum()) {
                    OrderStatus.NEW_ORDER -> 0.2f
                    OrderStatus.ACCEPTED -> 0.4f
                    OrderStatus.PREPARING -> 0.65f
                    OrderStatus.READY -> 0.9f
                    OrderStatus.SERVED -> 1f
                    else -> 1f
                }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeOrder.orderNumber} • ${strings.activeTable} T%02d".format(activeOrder.tableNumber),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(
                                label = if (language == AppLanguage.AR) activeOrder.statusEnum().labelAr else activeOrder.statusEnum().labelEn,
                                containerColor = OliveContainer,
                                contentColor = OliveEmerald
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            color = SaffronPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${activeOrder.items.sumOf { it.quantity }} items • Waiter: ${activeOrder.assignedWaiterName ?: "Assigned"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. 7 Customer Table Service Buttons
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (activeTableLabel.isNullOrBlank()) {
                            if (language == AppLanguage.AR) "خدمة الطاولة الفورية (امسح QR)" else "Instant Table Service (Scan QR)"
                        } else {
                            if (language == AppLanguage.AR) "خدمة الطاولة الفورية ($activeTableLabel)" else "Instant Table Service ($activeTableLabel)"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeTable == null) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(onClick = onOpenQrScanner, modifier = Modifier.fillMaxWidth()) {
                            Text(if (language == AppLanguage.AR) "امسح رمز QR الطاولة لطلب الخدمة" else "Scan Table QR Code First")
                        }
                    } else {
                        Text(
                            text = if (language == AppLanguage.AR) {
                                "النادل المسؤول: ${activeTable.assignedWaiterName}"
                            } else {
                                "Assigned Waiter: ${activeTable.assignedWaiterName}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SaffronPrimary
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    val actions: List<Triple<ServiceRequestType, String, ImageVector>> = listOf(
                        Triple(ServiceRequestType.CALL_WAITER, strings.callWaiter, Icons.Default.NotificationsActive),
                        Triple(ServiceRequestType.WATER, strings.requestWater, Icons.Default.LocalDrink),
                        Triple(ServiceRequestType.EXTRA_SAUCE, strings.requestSauce, Icons.Default.Restaurant),
                        Triple(ServiceRequestType.CUTLERY, strings.requestCutlery, Icons.Default.Dining),
                        Triple(ServiceRequestType.CLEANING, strings.requestCleaning, Icons.Default.CleaningServices),
                        Triple(ServiceRequestType.BILL, strings.requestBill, Icons.Default.ReceiptLong)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        actions.forEach { (type, label, icon) ->
                            FilledTonalButton(
                                onClick = {
                                    if (activeTable == null) onOpenQrScanner()
                                    else onServiceRequest(type, "")
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("service_btn_${type.name.lowercase()}")
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                if (activeTable == null) onOpenQrScanner()
                                else showOtherDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(strings.otherRequest)
                        }
                    }
                }
            }
        }

        // 3. Guest Wi-Fi Access Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EspressoInk),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Wifi, null, tint = SaffronVibrant, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(strings.wifiTitle, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("SSID: ${settings.wifiSsid}", color = Color(0xFFFFD8B2), style = MaterialTheme.typography.bodySmall)
                            Text("Pass: ${settings.wifiPassword}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(settings.wifiPassword))
                            onShowMessage(strings.passwordCopied)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        modifier = Modifier.testTag("copy_wifi_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.copyPassword)
                    }
                }
            }
        }

        // 4. Digital Loyalty Card & Birthday Rewards
        item {
            val profile = (loyaltyState as? UiState.Success)?.data
            val points = profile?.points ?: 60
            val tier = profile?.tier ?: "BRONZE"
            val qrBitmap = remember(profile?.userId) {
                QrCodeGenerator.generateQrBitmap("asil://loyalty/${profile?.userId ?: "guest"}", 240)
            }
            Card(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF4A2508), SaffronPrimary)))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.loyaltyCardTitle, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${profile?.displayName ?: "Guest"} • $tier TIER", color = Color(0xFFFFE6CE), style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "$points ${strings.pointsLabel}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "Loyalty QR Card",
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                        .padding(4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onRedeemReward,
                                enabled = points >= 150,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EspressoInk),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.redeemReward, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Cake, null, tint = Color(0xFFFFE6CE), modifier = Modifier.size(18.dp))
                            if (!profile?.birthdayMmDd.isNullOrBlank()) {
                                Text(
                                    text = "Birthday Reward Active (${profile?.birthdayMmDd})",
                                    color = Color(0xFFFFE6CE),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                OutlinedTextField(
                                    value = birthdayInput,
                                    onValueChange = { birthdayInput = it },
                                    placeholder = { Text("MM-DD", color = Color.White.copy(alpha = 0.7f)) },
                                    singleLine = true,
                                    modifier = Modifier.width(120.dp)
                                )
                                TextButton(onClick = { if (birthdayInput.isNotBlank()) onSaveBirthday(birthdayInput) }) {
                                    Text("Save Birthday", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Multiplayer Restaurant Games (Table Trivia / Food Guessing / Quick Quiz)
        item {
            val sessions = (gamesState as? UiState.Success)?.data.orEmpty()
            val tableNum = activeTable?.tableNumber ?: 1

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SportsEsports, null, tint = SaffronPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.gamesTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (language == AppLanguage.AR) {
                            "العب مع ضيوف طاولتك أو نافس الطاولات المجاورة أثناء تحضير طلبك."
                        } else {
                            "Play optional family-friendly trivia & food guessing games with your table or compete against nearby tables!"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GameType.entries.forEach { gt ->
                            FilledTonalButton(onClick = { onStartGame(tableNum, gt) }) {
                                Text(if (language == AppLanguage.AR) gt.labelAr else gt.labelEn)
                            }
                        }
                    }

                    val activeSession = sessions.firstOrNull { it.status != "FINISHED" } ?: sessions.firstOrNull()
                    if (activeSession != null) {
                        val questions = DefaultSeedData.defaultTriviaQuestions(activeSession.gameTypeEnum())
                        val qIndex = activeSession.currentQuestionIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
                        val currentQ = questions.getOrNull(qIndex)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Room ${activeSession.roomCode} • Table T%02d".format(activeSession.tableNumber),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SaffronPrimary
                                    )
                                    StatusPill(activeSession.status, SaffronContainer, EspressoInk)
                                }

                                if (activeSession.status != "FINISHED" && currentQ != null) {
                                    Text(
                                        text = if (language == AppLanguage.AR) currentQ.questionAr else currentQ.questionEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val opts = if (language == AppLanguage.AR) currentQ.optionsAr else currentQ.optionsEn
                                    opts.forEachIndexed { idx, opt ->
                                        OutlinedButton(
                                            onClick = {
                                                onAnswerGame(activeSession, idx == currentQ.correctIndex, true)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(opt)
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EmojiEvents, null, tint = SaffronPrimary)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Match Completed! Leaderboard:", fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Live Leaderboard
                                Text(
                                    text = "Players (${activeSession.playerNames.size}): " +
                                        activeSession.participantUids.mapIndexed { idx, uid ->
                                            val pName = activeSession.playerNames.getOrNull(idx) ?: "Player"
                                            val pts = activeSession.playerScores[uid] ?: 0L
                                            "$pName: ${pts}pts"
                                        }.joinToString(" • "),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = OliveEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOtherDialog) {
        AlertDialog(
            onDismissRequest = { showOtherDialog = false },
            title = { Text(strings.otherRequest) },
            text = {
                OutlinedTextField(
                    value = otherNote,
                    onValueChange = { otherNote = it },
                    label = { Text(if (language == AppLanguage.AR) "اكتب طلبك للنادل" else "Write your request for the waiter") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onServiceRequest(ServiceRequestType.OTHER, otherNote)
                    otherNote = ""
                    showOtherDialog = false
                }) {
                    Text(if (language == AppLanguage.AR) "إرسال" else "Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtherDialog = false }) {
                    Text(if (language == AppLanguage.AR) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
