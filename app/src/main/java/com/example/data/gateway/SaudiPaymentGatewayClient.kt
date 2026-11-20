package com.example.data.gateway

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface GatewayVerificationResult {
    data class VerifiedPaid(
        val transactionId: String,
        val provider: String,
        val rawStatus: String
    ) : GatewayVerificationResult

    data class ConfigurationRequired(
        val provider: String,
        val message: String
    ) : GatewayVerificationResult

    data class Failed(
        val reason: String
    ) : GatewayVerificationResult
}

/**
 * Real HTTPS client for Saudi payment gateways (Moyasar / HyperPay / Tap).
 * Never simulates a successful card/Mada/Apple Pay transaction:
 * only returns VerifiedPaid when the live gateway HTTP API returns a verified paid/captured status.
 */
class SaudiPaymentGatewayClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun verifyOrCreatePayment(
        provider: String,
        endpoint: String,
        publicKey: String,
        secretOrToken: String,
        amountSar: Double,
        orderNumber: String,
        methodCode: String
    ): GatewayVerificationResult = withContext(Dispatchers.IO) {
        if (publicKey.isBlank() || endpoint.isBlank() || secretOrToken.isBlank()) {
            return@withContext GatewayVerificationResult.ConfigurationRequired(
                provider = provider,
                message = "Live $provider merchant credentials are not connected yet. Configure the gateway public key & backend verification token in Admin -> Payment Gateway Settings, or use Cash / Bank Transfer verification."
            )
        }

        try {
            val amountHalalas = (amountSar * 100).toInt().coerceAtLeast(100)
            val payload = JSONObject().apply {
                put("amount", amountHalalas)
                put("currency", "SAR")
                put("description", "Asil Al Tawooq Jeddah Order $orderNumber")
                put("source", JSONObject().apply {
                    put("type", methodCode.lowercase())
                })
                put("publishable_api_key", publicKey)
            }.toString()

            val authHeader = "Basic " + Base64.encodeToString("$secretOrToken:".toByteArray(), Base64.NO_WRAP)
            val request = Request.Builder()
                .url(endpoint.trim())
                .addHeader("Authorization", authHeader)
                .addHeader("Accept", "application/json")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext GatewayVerificationResult.Failed(
                        "Gateway ($provider) rejected request (HTTP ${response.code}): ${body.take(180)}"
                    )
                }
                val json = JSONObject(body)
                val status = json.optString("status", "").lowercase()
                val txId = json.optString("id", "")
                if ((status == "paid" || status == "captured" || status == "verified") && txId.isNotBlank()) {
                    GatewayVerificationResult.VerifiedPaid(
                        transactionId = txId,
                        provider = provider,
                        rawStatus = status
                    )
                } else {
                    GatewayVerificationResult.Failed(
                        "Transaction not yet verified by $provider (status: ${status.ifBlank { "unknown" }})."
                    )
                }
            }
        } catch (e: Exception) {
            GatewayVerificationResult.Failed(
                e.localizedMessage ?: "Failed to connect to $provider payment gateway."
            )
        }
    }
}
