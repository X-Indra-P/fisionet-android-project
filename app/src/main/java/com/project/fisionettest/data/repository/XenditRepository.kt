package com.project.fisionettest.data.repository

import android.util.Base64
import com.project.fisionettest.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class XenditInvoiceRequest(
    val external_id: String,
    val amount: Double,
    val payer_email: String,
    val description: String,
    val success_redirect_url: String,   // Di-pass dari CashierFragment dengan trx_id
    val failure_redirect_url: String,
    val currency: String = "IDR"
)

data class XenditInvoiceResult(
    val invoiceId: String,
    val invoiceUrl: String,
    val status: String
)

class XenditRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun buildCredentials(): String {
        val secretKey = BuildConfig.XENDIT_SECRET_KEY
        return Base64.encodeToString("$secretKey:".toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Buat invoice Xendit.
     * success_redirect_url sudah menyertakan trx_id, contoh:
     *   fisionet://payment-success?trx_id=123
     */
    suspend fun createInvoice(
        transactionId: Int,
        amount: Double,
        payerEmail: String,
        description: String
    ): XenditInvoiceResult {
        val externalId = "FISIO-TRX-$transactionId-${System.currentTimeMillis()}"

        val body = XenditInvoiceRequest(
            external_id          = externalId,
            amount               = amount,
            payer_email          = payerEmail,
            description          = description,
            // Sertakan trx_id agar saat redirect kita tahu transaksi mana
            success_redirect_url = "fisionet://payment-success?trx_id=$transactionId",
            failure_redirect_url = "fisionet://payment-failed?trx_id=$transactionId"
        )

        val response = client.post("https://api.xendit.co/v2/invoices") {
            header("Authorization", "Basic ${buildCredentials()}")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        val responseText = response.bodyAsText()
        val jsonObj: JsonObject = json.parseToJsonElement(responseText).jsonObject

        val invoiceId  = jsonObj["id"]?.jsonPrimitive?.content ?: ""
        val invoiceUrl = jsonObj["invoice_url"]?.jsonPrimitive?.content ?: ""
        val status     = jsonObj["status"]?.jsonPrimitive?.content ?: "PENDING"

        if (invoiceId.isBlank()) {
            val errMsg = jsonObj["message"]?.jsonPrimitive?.content ?: "Unknown error"
            throw Exception("Gagal membuat invoice Xendit: $errMsg")
        }

        return XenditInvoiceResult(
            invoiceId  = invoiceId,
            invoiceUrl = invoiceUrl,
            status     = status
        )
    }

    /**
     * Cek status invoice Xendit berdasarkan xendit_id (invoice ID).
     * Return: "PAID", "PENDING", "EXPIRED", "SETTLED", dll.
     */
    suspend fun getInvoiceStatus(xenditInvoiceId: String): String {
        val response = client.get("https://api.xendit.co/v2/invoices/$xenditInvoiceId") {
            header("Authorization", "Basic ${buildCredentials()}")
        }
        val responseText = response.bodyAsText()
        val jsonObj: JsonObject = json.parseToJsonElement(responseText).jsonObject
        return jsonObj["status"]?.jsonPrimitive?.content ?: "PENDING"
    }
}
