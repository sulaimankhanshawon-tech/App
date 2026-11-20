package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.RestaurantSettings
import com.example.data.model.RestaurantTable
import com.example.data.model.SpiceLevel
import com.example.data.model.UiState
import com.example.ui.components.DishPhoto
import com.example.ui.components.SpiceBadge
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.getStrings
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.OliveContainer
import com.example.ui.theme.OliveEmerald
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SumacCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerMenuScreen(
    menuState: UiState<List<MenuItem>>,
    activeTable: RestaurantTable?,
    activeTableLabel: String?,
    cart: List<CartItem>,
    isSubmittingOrder: Boolean,
    loyaltyDiscountSar: Double,
    settings: RestaurantSettings,
    language: AppLanguage,
    onOpenQrScanner: () -> Unit,
    onAddToCart: (MenuItem, Int, String, SpiceLevel, List<String>, String, String) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onSubmitOrder: (String) -> Unit
) {
    val strings = remember(language) { getStrings(language) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<MenuCategory?>(null) }
    var inspectedDish by remember { mutableStateOf<MenuItem?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 104.dp)
        ) {
            // Editorial Hero Banner + Active Table QR Pill
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = EspressoInk),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.height(176.dp).fillMaxWidth()) {
                            DishPhoto(
                                imageKey = "grills",
                                imageUrl = null,
                                contentDescription = "Asil Al Tawooq Jeddah Hero",
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(EspressoInk.copy(alpha = 0.92f), EspressoInk.copy(alpha = 0.45f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = SaffronPrimary,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .clickable(onClick = onOpenQrScanner)
                                            .testTag("table_qr_pill")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (activeTableLabel.isNullOrBlank()) {
                                                    if (language == AppLanguage.AR) "امسح QR الطاولة" else "Scan Table QR"
                                                } else {
                                                    "${strings.activeTable} $activeTableLabel" + (activeTable?.zone?.let { " • $it" } ?: "")
                                                },
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                    StatusPill(
                                        label = settings.openingHours.take(18),
                                        containerColor = Color.White.copy(alpha = 0.16f),
                                        contentColor = Color.White
                                    )
                                }

                                Column {
                                    Text(
                                        text = if (language == AppLanguage.AR) "مشويات على الفحم الأصيل" else "Charcoal-Fired Jeddah Craft",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (language == AppLanguage.AR) {
                                            "تخصيص كامل للطبق • إرسال فوري للمطبخ"
                                        } else {
                                            "Full dish customization • Instant live kitchen sync"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFFFD8B2)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (activeTable == null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SaffronContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(onClick = onOpenQrScanner)
                            .testTag("scan_table_prompt_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.AR) "امسح رمز QR على طاولتك للطلب" else "Scan your table QR code to unlock ordering",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EspressoInk
                                )
                                Text(
                                    text = if (language == AppLanguage.AR) "سيتم ربط طلبك وفاتورتك بالطاولة تلقائياً" else "Your order, service requests, and bill bind automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EspressoInk.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = SaffronPrimary)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchMenuHint) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("menu_search_field")
                )
            }

            // 10 Menu Categories Carousel
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(strings.allCategories) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SaffronPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    MenuCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(if (language == AppLanguage.AR) cat.labelAr else cat.labelEn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SaffronPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            when (menuState) {
                is UiState.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SaffronPrimary)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CrimsonContainer),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text(
                                text = menuState.message,
                                color = SumacCrimson,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val visibleItems = menuState.data.filter { !it.hidden }
                    val featuredItems = visibleItems.filter { it.featured && it.available }
                    val filteredItems = visibleItems.filter { item ->
                        val catMatch = selectedCategory == null || item.categoryEnum() == selectedCategory
                        val q = searchQuery.trim().lowercase()
                        val searchMatch = q.isEmpty() ||
                            item.nameEn.lowercase().contains(q) ||
                            item.nameAr.contains(q) ||
                            item.descriptionEn.lowercase().contains(q)
                        catMatch && searchMatch
                    }

                    if (selectedCategory == null && searchQuery.isBlank() && featuredItems.isNotEmpty()) {
                        item {
                            Text(
                                text = if (language == AppLanguage.AR) "الأكثر طلباً والعروض" else "Signature & Special Deals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(featuredItems, key = { "feat_${it.id}" }) { dish ->
                                    FeaturedDishCard(
                                        dish = dish,
                                        language = language,
                                        sarLabel = strings.sarCurrency,
                                        onClick = { inspectedDish = dish }
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    if (filteredItems.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (language == AppLanguage.AR) "لا توجد أطباق مطابقة." else "No dishes match your filter.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { dish ->
                            DishListCard(
                                dish = dish,
                                language = language,
                                sarLabel = strings.sarCurrency,
                                soldOutLabel = strings.soldOut,
                                prepUnit = strings.prepTimeMin,
                                onClick = { inspectedDish = dish },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating Cart Bar
        AnimatedVisibility(
            visible = cart.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val cartSubtotal = cart.sumOf { it.lineTotal() }
            val count = cart.sumOf { it.quantity }
            Button(
                onClick = { showCartSheet = true },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EspressoInk, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("view_cart_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(SaffronPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$count", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(strings.viewCart, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "%.2f %s".format(cartSubtotal, strings.sarCurrency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD8B2)
                    )
                }
            }
        }
    }

    // Dish Detail & Customization Bottom Sheet
    if (inspectedDish != null) {
        DishCustomizationSheet(
            dish = inspectedDish!!,
            language = language,
            onDismiss = { inspectedDish = null },
            onAdd = { qty, size, spice, extras, notes, allergy ->
                onAddToCart(inspectedDish!!, qty, size, spice, extras, notes, allergy)
                inspectedDish = null
            }
        )
    }

    // Cart Review & Order Submission Bottom Sheet
    if (showCartSheet) {
        CartCheckoutSheet(
            cart = cart,
            activeTable = activeTable,
            activeTableLabel = activeTableLabel,
            isSubmittingOrder = isSubmittingOrder,
            loyaltyDiscountSar = loyaltyDiscountSar,
            vatPercent = settings.vatPercent,
            language = language,
            onDismiss = { showCartSheet = false },
            onOpenQrScanner = {
                showCartSheet = false
                onOpenQrScanner()
            },
            onUpdateQty = onUpdateCartQty,
            onSubmit = { instructions ->
                onSubmitOrder(instructions)
                showCartSheet = false
            }
        )
    }
}

@Composable
private fun FeaturedDishCard(
    dish: MenuItem,
    language: AppLanguage,
    sarLabel: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(modifier = Modifier.height(118.dp).fillMaxWidth()) {
                DishPhoto(
                    imageKey = dish.imageKey,
                    imageUrl = dish.imageUrl,
                    contentDescription = dish.nameEn,
                    modifier = Modifier.fillMaxSize()
                )
                if (!dish.offerLabel.isNullOrBlank()) {
                    StatusPill(
                        label = dish.offerLabel,
                        containerColor = SaffronPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (language == AppLanguage.AR) dish.nameAr else dish.nameEn,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%.2f %s".format(dish.priceSar, sarLabel),
                        style = MaterialTheme.typography.titleSmall,
                        color = SaffronPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    SpiceBadge(dish.spiceEnum(), language)
                }
            }
        }
    }
}

@Composable
private fun DishListCard(
    dish: MenuItem,
    language: AppLanguage,
    sarLabel: String,
    soldOutLabel: String,
    prepUnit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = dish.available, onClick = onClick)
            .testTag("dish_card_${dish.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DishPhoto(
                imageKey = dish.imageKey,
                imageUrl = dish.imageUrl,
                contentDescription = dish.nameEn,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.AR) dish.nameAr else dish.nameEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!dish.available) {
                        StatusPill(soldOutLabel, CrimsonContainer, SumacCrimson)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (language == AppLanguage.AR) dish.descriptionAr.ifBlank { dish.descriptionEn } else dish.descriptionEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%.2f %s".format(dish.priceSar, sarLabel),
                        style = MaterialTheme.typography.titleMedium,
                        color = SaffronPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(3.dp))
                            Text("${dish.prepMinutes} $prepUnit", style = MaterialTheme.typography.labelMedium)
                        }
                        SpiceBadge(dish.spiceEnum(), language)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DishCustomizationSheet(
    dish: MenuItem,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onAdd: (Int, String, SpiceLevel, List<String>, String, String) -> Unit
) {
    val strings = remember(language) { getStrings(language) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by remember { mutableIntStateOf(1) }
    var selectedSize by remember { mutableStateOf(dish.sizes.firstOrNull() ?: "Regular") }
    var selectedSpice by remember { mutableStateOf(dish.spiceEnum()) }
    var selectedExtras by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }
    var allergyNote by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            DishPhoto(
                imageKey = dish.imageKey,
                imageUrl = dish.imageUrl,
                contentDescription = dish.nameEn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (language == AppLanguage.AR) dish.nameAr else dish.nameEn,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${dish.portionSize} • ${dish.prepMinutes} ${strings.prepTimeMin} • ${dish.stationEnum().labelEn}",
                style = MaterialTheme.typography.labelLarge,
                color = SaffronPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (language == AppLanguage.AR) dish.descriptionAr.ifBlank { dish.descriptionEn } else dish.descriptionEn,
                style = MaterialTheme.typography.bodyLarge
            )
            if (dish.isMealDeal && !dish.dealSummary.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(color = SaffronContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Meal Deal: ${dish.dealSummary}",
                        style = MaterialTheme.typography.labelLarge,
                        color = EspressoInk,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // Ingredients & Allergens
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${strings.ingredients}: ${if (language == AppLanguage.AR) dish.ingredientsAr.ifBlank { dish.ingredientsEn } else dish.ingredientsEn}", style = MaterialTheme.typography.bodyMedium)
                    if (dish.allergens.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WarningAmber, null, tint = SumacCrimson, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${strings.allergens}: ${dish.allergens.joinToString(", ")}",
                                color = SumacCrimson,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(strings.sizes, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                dish.sizes.forEach { sizeOption ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedSize = sizeOption }
                            .padding(end = 8.dp)
                    ) {
                        RadioButton(selected = selectedSize == sizeOption, onClick = { selectedSize = sizeOption })
                        Text(sizeOption, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(strings.spiceLevel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpiceLevel.entries.forEach { sp ->
                    FilterChip(
                        selected = selectedSpice == sp,
                        onClick = { selectedSpice = sp },
                        label = { Text(if (language == AppLanguage.AR) sp.labelAr else sp.labelEn) }
                    )
                }
            }

            if (dish.extras.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(strings.extras, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                dish.extras.forEach { extra ->
                    val checked = extra in selectedExtras
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedExtras = if (checked) selectedExtras - extra else selectedExtras + extra
                            }
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selectedExtras = if (checked) selectedExtras - extra else selectedExtras + extra
                            }
                        )
                        Text(extra, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = allergyNote,
                onValueChange = { allergyNote = it },
                label = { Text(if (language == AppLanguage.AR) "تنبيه حساسية للمطبخ" else "Allergy Alert for Kitchen (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(strings.specialInstructions) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp)
                ) {
                    IconButton(onClick = { if (quantity > 1) quantity-- }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text("$quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { quantity++ }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                Spacer(Modifier.width(12.dp))

                val previewTotal = CartItem("preview", dish, quantity, selectedSize, selectedSpice, selectedExtras.toList()).lineTotal()
                Button(
                    onClick = {
                        onAdd(quantity, selectedSize, selectedSpice, selectedExtras.toList(), notes, allergyNote)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("confirm_add_to_cart_button")
                ) {
                    Text("${strings.addToCart} • %.2f ${strings.sarCurrency}".format(previewTotal), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartCheckoutSheet(
    cart: List<CartItem>,
    activeTable: RestaurantTable?,
    activeTableLabel: String?,
    isSubmittingOrder: Boolean,
    loyaltyDiscountSar: Double,
    vatPercent: Double,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onSubmit: (String) -> Unit
) {
    val strings = remember(language) { getStrings(language) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var instructions by remember { mutableStateOf("") }

    val subtotal = cart.sumOf { it.lineTotal() }
    val taxable = (subtotal - loyaltyDiscountSar).coerceAtLeast(0.0)
    val vat = taxable * (vatPercent / 100.0)
    val total = taxable + vat

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (activeTableLabel.isNullOrBlank()) {
                    strings.viewCart
                } else {
                    "${strings.viewCart} • ${strings.activeTable} $activeTableLabel"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            cart.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.AR) item.menuItem.nameAr else item.menuItem.nameEn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.selectedSize} • ${item.spiceLevel.labelEn}" +
                                if (item.selectedExtras.isNotEmpty()) " • ${item.selectedExtras.joinToString()}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onUpdateQty(item.cartKey, -1) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Remove one")
                        }
                        Text("${item.quantity}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onUpdateQty(item.cartKey, 1) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add one")
                        }
                    }
                    Text(
                        text = "%.2f".format(item.lineTotal()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider()
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text(strings.specialInstructions) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            SummaryRow(strings.subtotal, "%.2f %s".format(subtotal, strings.sarCurrency))
            if (loyaltyDiscountSar > 0) {
                SummaryRow(strings.discount, "-%.2f %s".format(loyaltyDiscountSar, strings.sarCurrency), OliveEmerald)
            }
            SummaryRow(strings.vat15, "%.2f %s".format(vat, strings.sarCurrency))
            SummaryRow(strings.total, "%.2f %s".format(total, strings.sarCurrency), SaffronPrimary, bold = true)

            Spacer(Modifier.height(16.dp))
            if (activeTable == null) {
                Button(
                    onClick = onOpenQrScanner,
                    colors = ButtonDefaults.buttonColors(containerColor = EspressoInk),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("cart_scan_qr_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (language == AppLanguage.AR) "امسح QR الطاولة لإرسال الطلب" else "Scan Table QR to Submit Order", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { onSubmit(instructions) },
                    enabled = !isSubmittingOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_order_button")
                ) {
                    if (isSubmittingOrder) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.submitOrder, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, valueColor: Color = EspressoInk, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
