package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.model.RestaurantTable
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class CompressedImageResult(
    val width: Int,
    val height: Int,
    val sizeBytes: Int,
    val base64Data: String,
    val mimeType: String = "image/jpeg"
)

object QrCardExporter {

    fun renderTableQrCardBitmap(table: RestaurantTable, cardWidth: Int = 720, cardHeight: Int = 920): Bitmap {
        val bmp = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Warm ivory card background
        canvas.drawColor(Color.rgb(253, 249, 242))

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = Color.rgb(198, 93, 20)
        }
        canvas.drawRoundRect(RectF(18f, 18f, cardWidth - 18f, cardHeight - 18f), 36f, 36f, borderPaint)

        // Top espresso header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(34, 24, 18)
        }
        canvas.drawRoundRect(RectF(32f, 32f, cardWidth - 32f, 188f), 26f, 26f, headerPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ASIL AL TAWOOQ • أصيل الطاووق", cardWidth / 2f, 102f, brandPaint)

        val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 212, 168)
            textAlign = Paint.Align.CENTER
            textSize = 23f
        }
        canvas.drawText("Jeddah, Saudi Arabia • Smart Dining", cardWidth / 2f, 148f, cityPaint)

        // Table label headline
        val tableTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(198, 93, 20)
            textAlign = Paint.Align.CENTER
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("TABLE ${table.label}  (Table ${table.tableNumber})", cardWidth / 2f, 264f, tableTitlePaint)

        val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(80, 68, 58)
            textAlign = Paint.Align.CENTER
            textSize = 26f
        }
        canvas.drawText("${table.zone} • ${table.capacity} Guests", cardWidth / 2f, 306f, zonePaint)

        // QR Code matrix in center
        val qrSize = 420
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(table.qrPayload, BarcodeFormat.QR_CODE, qrSize, qrSize, hints)
        val qrBitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
        for (x in 0 until qrSize) {
            for (y in 0 until qrSize) {
                qrBitmap.setPixel(x, y, if (matrix[x, y]) Color.rgb(27, 22, 18) else Color.WHITE)
            }
        }
        val qrLeft = (cardWidth - qrSize) / 2f
        val qrTop = 338f
        val qrBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(
            RectF(qrLeft - 18f, qrTop - 18f, qrLeft + qrSize + 18f, qrTop + qrSize + 18f),
            24f,
            24f,
            qrBoxPaint
        )
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        val scanPromptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(34, 24, 18)
            textAlign = Paint.Align.CENTER
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Scan QR to View Menu & Order", cardWidth / 2f, 825f, scanPromptPaint)

        val urlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(120, 100, 86)
            textAlign = Paint.Align.CENTER
            textSize = 20f
        }
        canvas.drawText(table.qrPayload, cardWidth / 2f, 868f, urlPaint)

        return bmp
    }

    suspend fun downloadSingleQrPng(context: Context, table: RestaurantTable): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bmp = renderTableQrCardBitmap(table)
                val fileName = "AsilAlTawooq_${table.label}_QR.png"
                val savedUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AsilAlTawooq_QR")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: error("Could not create MediaStore entry.")
                    resolver.openOutputStream(uri)?.use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri
                } else {
                    val dir = File(context.cacheDir, "qr_exports").apply { mkdirs() }
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
                savedUri
            }
        }

    suspend fun downloadAllQrPdf(context: Context, tables: List<RestaurantTable>): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(tables.isNotEmpty()) { "No tables available to export." }
                val pdf = createQrPdfDocument(tables.sortedBy { it.tableNumber })
                val fileName = "AsilAlTawooq_All_Table_QRs.pdf"

                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/AsilAlTawooq_QR")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val mediaUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: error("Failed to create PDF in Downloads.")
                    resolver.openOutputStream(mediaUri)?.use { out -> pdf.writeTo(out) }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(mediaUri, values, null, null)
                    mediaUri
                } else {
                    val dir = File(context.cacheDir, "qr_exports").apply { mkdirs() }
                    val file = File(dir, fileName)
                    FileOutputStream(file).use { out -> pdf.writeTo(out) }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
                pdf.close()
                uri
            }
        }

    fun shareOrOpenUri(context: Context, uri: Uri, mimeType: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun printQrTables(context: Context, tables: List<RestaurantTable>) {
        if (tables.isEmpty()) return
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val sortedTables = tables.sortedBy { it.tableNumber }
        val jobName = if (sortedTables.size == 1) {
            "Asil_Al_Tawooq_${sortedTables.first().label}_QR"
        } else {
            "Asil_Al_Tawooq_All_Table_QRs"
        }

        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val pageCount = ((sortedTables.size + 3) / 4).coerceAtLeast(1)
                    val info = PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(pageCount)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (destination == null) {
                        callback?.onWriteFailed("Missing print destination.")
                        return
                    }
                    val doc = createQrPdfDocument(sortedTables)
                    try {
                        FileOutputStream(destination.fileDescriptor).use { out ->
                            doc.writeTo(out)
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    } finally {
                        doc.close()
                    }
                }
            },
            null
        )
    }

    private fun createQrPdfDocument(tables: List<RestaurantTable>): PdfDocument {
        val doc = PdfDocument()
        // A4 at 72 dpi = 595 x 842
        val pageW = 595
        val pageH = 842
        val margin = 28f
        val gap = 18f
        val cellW = (pageW - margin * 2 - gap) / 2f
        val cellH = (pageH - margin * 2 - gap) / 2f

        tables.chunked(4).forEachIndexed { pageIndex, chunk ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageIndex + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            chunk.forEachIndexed { idx, table ->
                val col = idx % 2
                val row = idx / 2
                val left = margin + col * (cellW + gap)
                val top = margin + row * (cellH + gap)
                val cardBmp = renderTableQrCardBitmap(table, 540, 690)
                val dst = RectF(left, top, left + cellW, top + cellH)
                canvas.drawBitmap(cardBmp, null, dst, null)
            }
            doc.finishPage(page)
        }
        return doc
    }

    suspend fun compressImageUriToProductImage(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 720,
        maxBytes: Int = 520_000
    ): Result<CompressedImageResult> = withContext(Dispatchers.IO) {
        runCatching {
            val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val maxDim = maxOf(info.size.width, info.size.height)
                    if (maxDim > maxDimensionPx) {
                        val scale = maxDimensionPx.toFloat() / maxDim
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: error("Could not read selected image.")
            }

            val scaled = if (maxOf(rawBitmap.width, rawBitmap.height) > maxDimensionPx) {
                val scale = maxDimensionPx.toFloat() / maxOf(rawBitmap.width, rawBitmap.height)
                Bitmap.createScaledBitmap(
                    rawBitmap,
                    (rawBitmap.width * scale).toInt().coerceAtLeast(1),
                    (rawBitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                rawBitmap
            }

            var quality = 82
            var bytes: ByteArray
            do {
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                bytes = baos.toByteArray()
                quality -= 10
            } while (bytes.size > maxBytes && quality >= 32)

            require(bytes.size <= 740_000) { "Compressed photo exceeds 740KB cloud limit." }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            CompressedImageResult(
                width = scaled.width,
                height = scaled.height,
                sizeBytes = bytes.size,
                base64Data = b64
            )
        }
    }

    fun createCameraCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
        val file = File(dir, "dish_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
