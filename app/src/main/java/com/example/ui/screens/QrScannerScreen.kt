package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.RestaurantTable
import com.example.data.model.TableStatus
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.EspressoInk
import com.example.ui.theme.OliveEmerald
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SaffronVibrant
import com.example.ui.theme.WarmIvory
import com.example.util.TableQrParser
import com.example.util.ZxingQrDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QrScannerScreen(
    tables: List<RestaurantTable>,
    currentTableLabel: String?,
    language: AppLanguage,
    allowManualEntry: Boolean = false,
    onQrScanned: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val isArabic = language == AppLanguage.AR

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var scanFeedback by remember { mutableStateOf<String?>(null) }
    var manualInput by remember(currentTableLabel) {
        mutableStateOf(currentTableLabel?.let { "https://asilaltawooq.sa/menu?table=$it" } ?: "")
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            scanFeedback = if (isArabic) {
                "تم رفض إذن الكاميرا — يمكنك مسح رمز QR من صورة."
            } else {
                "Camera permission denied — you can scan your table QR code from a photo."
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val decoded = decodeQrFromUri(context, uri)
            if (decoded == null) {
                scanFeedback = if (isArabic) {
                    "لم يتم العثور على رمز QR في الصورة المختارة."
                } else {
                    "No QR code found in the selected photo."
                }
            } else {
                val parsed = TableQrParser.parseTableLabel(decoded)
                if (parsed != null) {
                    if (onQrScanned(parsed)) {
                        onDismiss()
                    }
                } else {
                    scanFeedback = if (isArabic) {
                        "الرمز الممسوح ($decoded) لا يخص طاولات أصيل الطاووق."
                    } else {
                        "Scanned QR \"$decoded\" is not an Asil Al Tawooq table code."
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF100C0A)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 720.dp

            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    ) {
                        ScannerCameraStage(
                            hasCameraPermission = hasCameraPermission,
                            cameraError = cameraError,
                            isArabic = isArabic,
                            torchEnabled = torchEnabled,
                            hasFlashUnit = hasFlashUnit,
                            onCameraReady = { cam ->
                                boundCamera = cam
                                hasFlashUnit = cam.cameraInfo.hasFlashUnit()
                                cameraError = null
                            },
                            onCameraFailure = { err ->
                                cameraError = err
                            },
                            onToggleTorch = {
                                val next = !torchEnabled
                                torchEnabled = next
                                boundCamera?.cameraControl?.enableTorch(next)
                            },
                            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            onValidTableDecoded = { tableLabel ->
                                if (onQrScanned(tableLabel)) {
                                    onDismiss()
                                }
                            },
                            onInvalidQrDecoded = { raw ->
                                scanFeedback = if (isArabic) {
                                    "الرمز ($raw) لا يطابق رمز طاولة."
                                } else {
                                    "QR \"$raw\" is not a valid table code."
                                }
                            },
                            onDismiss = onDismiss
                        )
                    }

                    ScannerControlPanel(
                        tables = tables,
                        currentTableLabel = currentTableLabel,
                        allowManualEntry = allowManualEntry,
                        isArabic = isArabic,
                        scanFeedback = scanFeedback,
                        manualInput = manualInput,
                        onManualInputChange = { manualInput = it },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onJoinPayload = { payload ->
                            if (onQrScanned(payload)) {
                                onDismiss()
                            } else {
                                scanFeedback = if (isArabic) {
                                    "صيغة الطاولة غير صالحة أو غير مفعلة"
                                } else {
                                    "Invalid or inactive table code"
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        ScannerCameraStage(
                            hasCameraPermission = hasCameraPermission,
                            cameraError = cameraError,
                            isArabic = isArabic,
                            torchEnabled = torchEnabled,
                            hasFlashUnit = hasFlashUnit,
                            onCameraReady = { cam ->
                                boundCamera = cam
                                hasFlashUnit = cam.cameraInfo.hasFlashUnit()
                                cameraError = null
                            },
                            onCameraFailure = { err ->
                                cameraError = err
                            },
                            onToggleTorch = {
                                val next = !torchEnabled
                                torchEnabled = next
                                boundCamera?.cameraControl?.enableTorch(next)
                            },
                            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            onValidTableDecoded = { tableLabel ->
                                if (onQrScanned(tableLabel)) {
                                    onDismiss()
                                }
                            },
                            onInvalidQrDecoded = { raw ->
                                scanFeedback = if (isArabic) {
                                    "الرمز ($raw) لا يطابق رمز طاولة."
                                } else {
                                    "QR \"$raw\" is not a valid table code."
                                }
                            },
                            onDismiss = onDismiss
                        )
                    }

                    ScannerControlPanel(
                        tables = tables,
                        currentTableLabel = currentTableLabel,
                        allowManualEntry = allowManualEntry,
                        isArabic = isArabic,
                        scanFeedback = scanFeedback,
                        manualInput = manualInput,
                        onManualInputChange = { manualInput = it },
                        onPickPhoto = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onJoinPayload = { payload ->
                            if (onQrScanned(payload)) {
                                onDismiss()
                            } else {
                                scanFeedback = if (isArabic) {
                                    "صيغة الطاولة غير صالحة أو غير مفعلة"
                                } else {
                                    "Invalid or inactive table code"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannerCameraStage(
    hasCameraPermission: Boolean,
    cameraError: String?,
    isArabic: Boolean,
    torchEnabled: Boolean,
    hasFlashUnit: Boolean,
    onCameraReady: (Camera) -> Unit,
    onCameraFailure: (String) -> Unit,
    onToggleTorch: () -> Unit,
    onRequestPermission: () -> Unit,
    onValidTableDecoded: (String) -> Unit,
    onInvalidQrDecoded: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF120E0B))
    ) {
        if (hasCameraPermission && cameraError == null) {
            ZxingCameraPreview(
                onCameraReady = onCameraReady,
                onCameraFailure = onCameraFailure,
                onValidTableDecoded = onValidTableDecoded,
                onInvalidQrDecoded = onInvalidQrDecoded,
                modifier = Modifier.fillMaxSize()
            )
            ScannerReticleOverlay(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SaffronPrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = SaffronVibrant,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (!hasCameraPermission) {
                        if (isArabic) "السماح بالكاميرا لمسح رمز الطاولة" else "Allow Camera to Scan Table QR"
                    } else {
                        if (isArabic) "الكاميرا غير متوفرة على هذا الجهاز" else "Camera Preview Unavailable"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (!hasCameraPermission) {
                        if (isArabic) {
                            "وجّه الكاميرا نحو رمز QR الموجود على طاولتك للانضمام للجلسة تلقائياً."
                        } else {
                            "Point your camera at the QR code on your table to join your table session automatically."
                        }
                    } else {
                        cameraError ?: ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD6C7BA),
                    textAlign = TextAlign.Center
                )
                if (!hasCameraPermission) {
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("qr_grant_camera_btn")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isArabic) "منح إذن الكاميرا" else "Enable Camera")
                    }
                }
            }
        }

        // Top overlay HUD (Close, Title, Flash Torch)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x881A1410))
                    .testTag("qr_scanner_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = if (isArabic) "إغلاق الماسح" else "Close scanner",
                    tint = Color.White
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x991A1410)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = SaffronVibrant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "ماسح QR للطاولة" else "ZXing Table QR Scanner",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (hasCameraPermission && hasFlashUnit) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (torchEnabled) SaffronPrimary else Color(0x881A1410))
                        .testTag("qr_torch_btn")
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (isArabic) "تبديل الفلاش" else "Toggle flashlight",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        if (hasCameraPermission && cameraError == null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xAA1A1410),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = if (isArabic) {
                        "ضع رمز QR الخاص بالطاولة داخل الإطار للانضمام تلقائياً"
                    } else {
                        "Align your table QR code inside the frame to join automatically"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFBE9D8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ZxingCameraPreview(
    onCameraReady: (Camera) -> Unit,
    onCameraFailure: (String) -> Unit,
    onValidTableDecoded: (String) -> Unit,
    onInvalidQrDecoded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val decoder = remember { ZxingQrDecoder() }
    val hasJoinedTable = remember { AtomicBoolean(false) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        providerFuture.addListener(
            {
                try {
                    val provider = providerFuture.get()
                    cameraProviderRef = provider

                    val preview = Preview.Builder().build().apply {
                        surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            if (hasJoinedTable.get()) return@setAnalyzer
                            val decoded = decoder.decodeImageProxy(imageProxy) ?: return@setAnalyzer
                            val tableLabel = TableQrParser.parseTableLabel(decoded)
                            if (tableLabel != null) {
                                if (hasJoinedTable.compareAndSet(false, true)) {
                                    mainExecutor.execute { onValidTableDecoded(tableLabel) }
                                }
                            } else {
                                mainExecutor.execute { onInvalidQrDecoded(decoded) }
                            }
                        } finally {
                            imageProxy.close()
                        }
                    }

                    val selector = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }

                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis
                    )
                    onCameraReady(camera)
                } catch (e: Exception) {
                    Log.w("QrScannerScreen", "CameraX bind failed", e)
                    onCameraFailure(e.localizedMessage ?: "No compatible camera found.")
                }
            },
            mainExecutor
        )

        onDispose {
            runCatching { cameraProviderRef?.unbindAll() }
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
private fun ScannerReticleOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan_line")
    val laserProgress by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = modifier) {
        val boxSize = minOf(size.width, size.height) * 0.64f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f
        val right = left + boxSize
        val bottom = top + boxSize
        val scrim = Color(0x900C0806)

        // Draw 4 scrim bands around the open center square
        drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, top))
        drawRect(scrim, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
        drawRect(scrim, topLeft = Offset(0f, top), size = Size(left, boxSize))
        drawRect(scrim, topLeft = Offset(right, top), size = Size(size.width - right, boxSize))

        val strokePx = 4.dp.toPx()
        val cornerLen = 28.dp.toPx()
        val accent = SaffronVibrant

        // Top-Left
        drawLine(accent, Offset(left, top), Offset(left + cornerLen, top), strokePx, StrokeCap.Round)
        drawLine(accent, Offset(left, top), Offset(left, top + cornerLen), strokePx, StrokeCap.Round)
        // Top-Right
        drawLine(accent, Offset(right - cornerLen, top), Offset(right, top), strokePx, StrokeCap.Round)
        drawLine(accent, Offset(right, top), Offset(right, top + cornerLen), strokePx, StrokeCap.Round)
        // Bottom-Left
        drawLine(accent, Offset(left, bottom - cornerLen), Offset(left, bottom), strokePx, StrokeCap.Round)
        drawLine(accent, Offset(left, bottom), Offset(left + cornerLen, bottom), strokePx, StrokeCap.Round)
        // Bottom-Right
        drawLine(accent, Offset(right - cornerLen, bottom), Offset(right, bottom), strokePx, StrokeCap.Round)
        drawLine(accent, Offset(right, bottom - cornerLen), Offset(right, bottom), strokePx, StrokeCap.Round)

        // Animated laser line inside scan reticle
        val laserY = top + (boxSize * laserProgress)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, SaffronVibrant, Color(0xFFFFE0B2), SaffronVibrant, Color.Transparent),
                startX = left + 10f,
                endX = right - 10f
            ),
            start = Offset(left + 14f, laserY),
            end = Offset(right - 14f, laserY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScannerControlPanel(
    tables: List<RestaurantTable>,
    currentTableLabel: String?,
    allowManualEntry: Boolean,
    isArabic: Boolean,
    scanFeedback: String?,
    manualInput: String,
    onManualInputChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onJoinPayload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = WarmIvory,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableRestaurant, contentDescription = null, tint = SaffronPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "تحديد الطاولة عبر QR" else "Identify Table via QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EspressoInk
                    )
                }

                OutlinedButton(
                    onClick = onPickPhoto,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("qr_pick_photo_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isArabic) "مسح صورة" else "From Photo")
                }
            }

            if (!currentTableLabel.isNullOrBlank()) {
                Text(
                    text = if (isArabic) "الطاولة الحالية: $currentTableLabel" else "Current table: $currentTableLabel",
                    style = MaterialTheme.typography.labelLarge,
                    color = SaffronPrimary,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = if (isArabic) {
                        "امسح رمز QR الخاص بطاولتك أو اختر صورة رمز QR للبدء."
                    } else {
                        "Scan your table's unique QR code with the camera or pick a QR photo to unlock ordering."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!scanFeedback.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scanFeedback,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (allowManualEntry) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = onManualInputChange,
                        label = { Text(if (isArabic) "رمز الطاولة أو الرابط" else "Staff: QR Code or Table #") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SaffronPrimary,
                            focusedLabelColor = SaffronPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("qr_payload_input")
                    )
                    Button(
                        onClick = { onJoinPayload(manualInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("qr_join_manual_btn")
                    ) {
                        Text(if (isArabic) "انضمام" else "Join")
                    }
                }

                Text(
                    text = if (isArabic) "اختبار سريع للطاولات (للطاقم):" else "Staff Quick Table Test:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val displayTables = if (tables.isNotEmpty()) {
                        tables
                    } else {
                        (1..4).map { RestaurantTable(tableNumber = it, label = "T%02d".format(it)) }
                    }
                    displayTables.forEach { table ->
                        val isSelected = table.label.equals(currentTableLabel, ignoreCase = true)
                        val isAvailable = table.statusEnum() == TableStatus.AVAILABLE
                        FilterChip(
                            selected = isSelected,
                            onClick = { onJoinPayload(table.label) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isAvailable) OliveEmerald else SaffronVibrant)
                                )
                            },
                            label = {
                                Text(
                                    text = table.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SaffronPrimary.copy(alpha = 0.16f),
                                selectedLabelColor = SaffronPrimary
                            ),
                            modifier = Modifier.testTag("qr_table_chip_${table.label}")
                        )
                    }
                }
            }
        }
    }
}

private suspend fun decodeQrFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val maxDim = maxOf(info.size.width, info.size.height)
                if (maxDim > 1400) {
                    val scale = 1400f / maxDim
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } ?: return@withContext null

        val softwareBitmap = if (rawBitmap.config != Bitmap.Config.ARGB_8888) {
            rawBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            rawBitmap
        }
        withContext(Dispatchers.Default) {
            ZxingQrDecoder().decodeBitmap(softwareBitmap)
        }
    } catch (e: Exception) {
        Log.w("QrScannerScreen", "Failed to decode photo QR", e)
        null
    }
}
