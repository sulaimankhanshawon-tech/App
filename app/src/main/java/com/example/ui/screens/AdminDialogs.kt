package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.KitchenStation
import com.example.data.model.MenuCategory
import com.example.data.model.MenuItem
import com.example.data.model.RestaurantTable
import com.example.data.model.SpiceLevel
import com.example.data.model.StaffRole
import com.example.ui.components.DishPhoto
import com.example.ui.theme.SaffronPrimary
import com.example.util.QrCardExporter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuEditorDialog(
    initial: MenuItem?,
    onUploadPhoto: (Context, Uri, String?, (String, String) -> Unit, (String) -> Unit) -> Unit,
    onSave: (MenuItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nameEn by remember { mutableStateOf(initial?.nameEn ?: "") }
    var nameAr by remember { mutableStateOf(initial?.nameAr ?: "") }
    var descEn by remember { mutableStateOf(initial?.descriptionEn ?: "") }
    var descAr by remember { mutableStateOf(initial?.descriptionAr ?: "") }
    var ingredientsEn by remember { mutableStateOf(initial?.ingredientsEn ?: "") }
    var ingredientsAr by remember { mutableStateOf(initial?.ingredientsAr ?: "") }
    var allergens by remember { mutableStateOf(initial?.allergens ?: emptyList()) }
    var customAllergen by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf((initial?.priceSar ?: 35.0).toString()) }
    var prepText by remember { mutableStateOf((initial?.prepMinutes ?: 12).toString()) }
    var portionSize by remember { mutableStateOf(initial?.portionSize ?: "Regular") }
    var sizes by remember { mutableStateOf(initial?.sizes?.ifEmpty { listOf("Regular", "Large", "Family") } ?: listOf("Regular", "Large", "Family")) }
    var extras by remember { mutableStateOf(initial?.extras ?: listOf("Extra Garlic Toum +4 SAR")) }
    var extraName by remember { mutableStateOf("") }
    var extraPriceText by remember { mutableStateOf("5") }
    var offerLabel by remember { mutableStateOf(initial?.offerLabel ?: "") }
    var isMealDeal by remember { mutableStateOf(initial?.isMealDeal ?: false) }
    var dealSummary by remember { mutableStateOf(initial?.dealSummary ?: "") }
    var available by remember { mutableStateOf(initial?.available ?: true) }
    var featured by remember { mutableStateOf(initial?.featured ?: false) }

    var imageId by remember { mutableStateOf(initial?.imageId) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    var category by remember { mutableStateOf(initial?.categoryEnum() ?: MenuCategory.CHICKEN_TAWOOK) }
    var station by remember { mutableStateOf(initial?.stationEnum() ?: KitchenStation.GRILL) }
    var spice by remember { mutableStateOf(initial?.spiceEnum() ?: SpiceLevel.MILD) }

    val handlePickedUri: (Uri) -> Unit = { uri ->
        uploadError = null
        isUploadingPhoto = true
        onUploadPhoto(
            context,
            uri,
            initial?.id?.takeIf { it.isNotBlank() },
            { uploadedId, cloudUrl ->
                imageId = uploadedId
                imageUrl = cloudUrl
                isUploadingPhoto = false
            },
            { err ->
                uploadError = err
                isUploadingPhoto = false
            }
        )
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) handlePickedUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            handlePickedUri(uri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = QrCardExporter.createCameraCaptureUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            uploadError = "Camera permission is required to snap a dish photo."
        }
    }

    val imgKeyPreview = when (category) {
        MenuCategory.SHAWARMA, MenuCategory.SANDWICHES -> "shawarma"
        MenuCategory.RICE_MEALS, MenuCategory.APPETIZERS -> "rice"
        MenuCategory.PASTA, MenuCategory.SAUCES -> "pasta"
        MenuCategory.COLD_DRINKS -> "drinks"
        else -> "grills"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add New Product" else "Edit ${initial.nameEn}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cloud Photo Preview & Gallery / Camera Upload
                Text("Product Photograph (Cloud Sync)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.fillMaxWidth()) {
                    DishPhoto(
                        imageKey = imgKeyPreview,
                        imageUrl = imageUrl.ifBlank { null },
                        contentDescription = nameEn.ifBlank { "Product photo" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    if (isUploadingPhoto) {
                        CircularProgressIndicator(
                            color = SaffronPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploadingPhoto,
                        modifier = Modifier.weight(1f).testTag("menu_upload_gallery_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gallery")
                    }
                    OutlinedButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val uri = QrCardExporter.createCameraCaptureUri(context)
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        enabled = !isUploadingPhoto,
                        modifier = Modifier.weight(1f).testTag("menu_upload_camera_btn")
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Camera")
                    }
                }
                if (uploadError != null) {
                    Text(uploadError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image Reference / URL (Auto-filled on upload)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Product Name (English)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("menu_name_en_input")
                )
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text("Product Name (Arabic)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("menu_name_ar_input")
                )
                OutlinedTextField(
                    value = descEn,
                    onValueChange = { descEn = it },
                    label = { Text("Description (English)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descAr,
                    onValueChange = { descAr = it },
                    label = { Text("Description (Arabic)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ingredientsEn,
                    onValueChange = { ingredientsEn = it },
                    label = { Text("Ingredients (English)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ingredientsAr,
                    onValueChange = { ingredientsAr = it },
                    label = { Text("Ingredients (Arabic)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (SAR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("menu_price_input")
                    )
                    OutlinedTextField(
                        value = prepText,
                        onValueChange = { prepText = it },
                        label = { Text("Prep Min") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = portionSize,
                    onValueChange = { portionSize = it },
                    label = { Text("Portion Size (e.g. 2 Skewers 320g)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Allergy Information:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Dairy", "Gluten", "Sesame", "Eggs", "Tree Nuts", "Peanuts", "Soy").forEach { alg ->
                        val selected = alg in allergens
                        FilterChip(
                            selected = selected,
                            onClick = { allergens = if (selected) allergens - alg else allergens + alg },
                            label = { Text(alg) }
                        )
                    }
                    allergens.filter { it !in listOf("Dairy", "Gluten", "Sesame", "Eggs", "Tree Nuts", "Peanuts", "Soy") }.forEach { alg ->
                        InputChip(
                            selected = true,
                            onClick = { allergens = allergens - alg },
                            label = { Text(alg) },
                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customAllergen,
                        onValueChange = { customAllergen = it },
                        label = { Text("Custom Allergen") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = {
                        val trimmed = customAllergen.trim()
                        if (trimmed.isNotBlank() && trimmed !in allergens) {
                            allergens = allergens + trimmed
                            customAllergen = ""
                        }
                    }) { Text("Add") }
                }

                Text("Category:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MenuCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat; station = cat.defaultStation },
                            label = { Text(cat.labelEn) }
                        )
                    }
                }

                Text("Kitchen Station:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KitchenStation.entries.forEach { st ->
                        FilterChip(selected = station == st, onClick = { station = st }, label = { Text(st.name) })
                    }
                }

                Text("Spice Level:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpiceLevel.entries.forEach { sp ->
                        FilterChip(selected = spice == sp, onClick = { spice = sp }, label = { Text(sp.labelEn) })
                    }
                }

                Text("Portion Sizes Available:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Regular", "Large", "Family", "Combo").forEach { s ->
                        val active = s in sizes
                        FilterChip(
                            selected = active,
                            onClick = {
                                val next = if (active) sizes - s else sizes + s
                                if (next.isNotEmpty()) sizes = next
                            },
                            label = { Text(s) }
                        )
                    }
                }

                Text("Optional Extras & Prices:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                extras.forEach { ex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• $ex", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { extras = extras - ex }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove extra", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = extraName,
                        onValueChange = { extraName = it },
                        label = { Text("Extra Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = extraPriceText,
                        onValueChange = { extraPriceText = it },
                        label = { Text("+SAR") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                    IconButton(onClick = {
                        val clean = extraName.trim()
                        val amt = extraPriceText.toDoubleOrNull()?.toInt() ?: 0
                        if (clean.isNotBlank()) {
                            extras = extras + "$clean +$amt SAR"
                            extraName = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Extra")
                    }
                }

                // Meal Deal & Stock Availability
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isMealDeal, onCheckedChange = { isMealDeal = it })
                    Text("Configure as Meal Deal")
                }
                if (isMealDeal) {
                    OutlinedTextField(
                        value = dealSummary,
                        onValueChange = { dealSummary = it },
                        label = { Text("Meal Deal Includes (e.g. Fries + Drink)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = offerLabel,
                    onValueChange = { offerLabel = it },
                    label = { Text("Offer Badge (e.g. Save 20% / Chef Choice)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available for Ordering")
                    Switch(checked = available, onCheckedChange = { available = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = featured, onCheckedChange = { featured = it })
                    Text("Feature in Signature & Deals Carousel")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val built = (initial ?: MenuItem()).copy(
                        nameEn = nameEn.ifBlank { "New Dish" },
                        nameAr = nameAr.ifBlank { nameEn.ifBlank { "طبق جديد" } },
                        descriptionEn = descEn,
                        descriptionAr = descAr,
                        ingredientsEn = ingredientsEn,
                        ingredientsAr = ingredientsAr,
                        allergens = allergens,
                        category = category.name,
                        kitchenStation = station.name,
                        spiceLevel = spice.name,
                        portionSize = portionSize.ifBlank { "Regular" },
                        priceSar = priceText.toDoubleOrNull() ?: 35.0,
                        prepMinutes = prepText.toIntOrNull() ?: 12,
                        available = available,
                        featured = featured,
                        isMealDeal = isMealDeal,
                        dealSummary = dealSummary.ifBlank { null },
                        offerLabel = offerLabel.ifBlank { null },
                        imageKey = imgKeyPreview,
                        imageId = imageId,
                        imageUrl = imageUrl.ifBlank { null },
                        sizes = sizes,
                        extras = extras
                    )
                    onSave(built)
                    onDismiss()
                },
                enabled = !isUploadingPhoto,
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                modifier = Modifier.testTag("menu_save_button")
            ) {
                Text("Save Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TableEditorDialog(
    initial: RestaurantTable? = null,
    suggestedNumber: Int = 5,
    onSave: (tableNumber: Int, zone: String, capacity: Int, waiterName: String, active: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var tableNumberText by remember { mutableStateOf((initial?.tableNumber ?: suggestedNumber).toString()) }
    var zone by remember { mutableStateOf(initial?.zone ?: "Main Hall") }
    var capacityText by remember { mutableStateOf((initial?.capacity ?: 4).toString()) }
    var waiterName by remember { mutableStateOf(initial?.assignedWaiterName ?: "Tariq Al-Harbi") }
    var active by remember { mutableStateOf(initial?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add New Table" else "Edit Table ${initial.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tableNumberText,
                    onValueChange = { tableNumberText = it },
                    enabled = initial == null,
                    label = { Text("Table Number (e.g. 5 -> T05)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("table_number_input")
                )
                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    label = { Text("Zone (Main Hall / Family Majlis / Jeddah Patio)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it },
                    label = { Text("Seat Capacity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = waiterName,
                    onValueChange = { waiterName = it },
                    label = { Text("Assigned Waiter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Table Active for QR Ordering")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = (initial?.tableNumber ?: tableNumberText.toIntOrNull() ?: suggestedNumber).coerceIn(1, 200)
                    val cap = capacityText.toIntOrNull()?.coerceIn(1, 30) ?: 4
                    onSave(num, zone, cap, waiterName, active)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                modifier = Modifier.testTag("table_save_button")
            ) { Text("Save Table") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StaffEditorDialog(
    callerRole: StaffRole?,
    initial: com.example.data.model.StaffMember? = null,
    onSave: (docId: String, name: String, email: String, role: StaffRole, zone: String, active: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var zone by remember { mutableStateOf(initial?.assignedZone ?: "Main Hall") }
    var role by remember { mutableStateOf(initial?.roleEnum() ?: StaffRole.WAITER) }
    var active by remember { mutableStateOf(initial?.active ?: true) }

    val availableRoles = remember(callerRole) {
        if (callerRole == StaffRole.OWNER) StaffRole.entries.toList()
        else StaffRole.entries.filter { it != StaffRole.OWNER }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Authorize Staff / Admin Account" else "Edit ${initial.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    enabled = initial == null,
                    label = { Text("Google Account Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    label = { Text("Assigned Zone / Station") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Role:")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableRoles.forEach { r ->
                        FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.labelEn) })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Active")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanEmail = email.trim().lowercase()
                    if (name.isNotBlank() && cleanEmail.contains("@")) {
                        val docId = initial?.id?.ifBlank { cleanEmail } ?: cleanEmail
                        onSave(docId, name, cleanEmail, role, zone, active)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
            ) { Text("Save Staff") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
