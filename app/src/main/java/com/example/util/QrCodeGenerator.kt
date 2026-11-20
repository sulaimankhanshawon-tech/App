package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeGenerator {
    fun generateQrBitmap(content: String, sizePx: Int = 512): ImageBitmap? {
        return try {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.rgb(27, 22, 18) else Color.rgb(253, 251, 247))
                }
            }
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

object TableQrParser {
    /**
     * Parses QR payloads such as:
     * - "asil://table/T04"
     * - "https://asilaltawooq.sa/t/4"
     * - "T04" or "4"
     * Returns a normalized table label like "T04", or null if invalid.
     */
    fun parseTableLabel(rawPayload: String): String? {
        val trimmed = rawPayload.trim()
        if (trimmed.isBlank()) return null

        val candidate = when {
            trimmed.startsWith("asil://table/", ignoreCase = true) ->
                trimmed.substringAfter("asil://table/").substringBefore("?").substringBefore("/")
            trimmed.contains("/t/", ignoreCase = true) ->
                trimmed.substringAfter("/t/").substringBefore("?").substringBefore("/")
            trimmed.contains("table=", ignoreCase = true) ->
                trimmed.substringAfter("table=").substringBefore("&")
            else -> trimmed
        }.trim().uppercase()

        val match = Regex("^T?(\\d{1,3})$").matchEntire(candidate) ?: return null
        val tableNum = match.groupValues[1].toIntOrNull() ?: return null
        return if (tableNum in 1..200) "T%02d".format(tableNum) else null
    }
}

class ZxingQrDecoder {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    fun decodeImageProxy(imageProxy: ImageProxy): String? {
        val format = imageProxy.format
        if (format != ImageFormat.YUV_420_888 && format != ImageFormat.YUV_422_888 && format != ImageFormat.YUV_444_888) {
            return null
        }
        val plane = imageProxy.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = plane.rowStride

        // Normalize tightly-packed luminance array if rowStride > width
        val yData = if (rowStride == width) {
            bytes
        } else {
            ByteArray(width * height).also { dst ->
                for (row in 0 until height) {
                    val srcOffset = row * rowStride
                    val length = minOf(width, bytes.size - srcOffset)
                    if (length > 0) {
                        System.arraycopy(bytes, srcOffset, dst, row * width, length)
                    }
                }
            }
        }

        return try {
            val source = PlanarYUVLuminanceSource(yData, width, height, 0, 0, width, height, false)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            reader.decodeWithState(binaryBitmap).text
        } catch (_: NotFoundException) {
            null
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    fun decodeBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return try {
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            reader.decodeWithState(binaryBitmap).text
        } catch (_: NotFoundException) {
            null
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }
}
