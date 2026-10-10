package com.example.core

import com.example.data.AppDatabase
import com.example.data.ApprovedSenderEntity
import com.example.data.AuditLogEntity
import com.example.data.SubmissionHistoryEntity
import com.example.data.TransactionEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ParsedTransactionResult(
    val isValid: Boolean,
    val provider: String,
    val senderId: String,
    val transactionId: String,
    val amount: Double,
    val currency: String = "BDT",
    val customerReference: String = "",
    val balance: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val maskedOriginalSms: String = "",
    val confidenceScore: Int = 0,
    val needsReview: Boolean = false,
    val rejectReason: String? = null
)

data class ApiSettings(
    val apiUrl: String,
    val authType: String = "Bearer", // "Bearer" or "API Key"
    val authHeaderName: String = "Authorization",
    val merchantId: String = "MERCHANT_001",
    val deviceId: String = "TERMINAL_01",
    val timeoutSeconds: Int = 15,
    val preventDuplicates: Boolean = true
)

data class ApiResult(
    val isSuccess: Boolean,
    val httpCode: Int,
    val responseBody: String?,
    val errorMessage: String?,
    val destinationDomain: String
)

/**
 * Core Payment SMS processor containing required helper methods.
 * Provides modular parsing, safety filtering, and secure HTTPS API submission.
 */
object PaymentSmsProcessor {

    /**
     * Determines whether the sender ID matches any merchant-approved sender pattern.
     */
    fun isApprovedSender(sender: String, approvedList: List<ApprovedSenderEntity>): Boolean {
        if (sender.isBlank()) return false
        val cleanSender = sender.trim().lowercase(Locale.ROOT)
        return approvedList.any { entity ->
            entity.isEnabled && (
                cleanSender.contains(entity.senderPattern.lowercase(Locale.ROOT)) ||
                entity.senderPattern.lowercase(Locale.ROOT).contains(cleanSender)
            )
        }
    }

    /**
     * Safety filter: Strictly detects and blocks messages containing OTPs, PINs, passwords,
     * login codes, card credentials, or authentication tokens.
     */
    fun isSensitiveMessage(message: String): Boolean {
        if (message.isBlank()) return true
        val lower = message.lowercase(Locale.ROOT)

        val sensitiveKeywords = listOf(
            "otp",
            "one time password",
            "pin",
            "verification code",
            "security code",
            "password",
            "passcode",
            "login code",
            "auth code",
            "card number",
            "cvv",
            "nid",
            "do not share",
            "never share",
            "গোপন পিন",
            "পিন নম্বর",
            "ওটিপি",
            "ভেরিফিকেশন কোড",
            "কাউকে বলবেন না"
        )

        return sensitiveKeywords.any { keyword ->
            if (keyword == "pin" || keyword == "otp") {
                // Word boundary check for short tokens
                val pattern = Pattern.compile("\\b$keyword\\b", Pattern.CASE_INSENSITIVE)
                pattern.matcher(lower).find()
            } else {
                lower.contains(keyword)
            }
        }
    }

    /**
     * Parses payment transaction notification SMS from approved providers (bKash, Nagad, Rocket, Upay).
     * Extracts Transaction ID, Amount, Currency, Reference, and Balance.
     * Guarantees that account Balance is never confused with received Payment Amount.
     */
    fun parseTransactionSms(sender: String, message: String): ParsedTransactionResult {
        if (isSensitiveMessage(message)) {
            return ParsedTransactionResult(
                isValid = false,
                provider = detectProvider(sender, message),
                senderId = sender,
                transactionId = "",
                amount = 0.0,
                maskedOriginalSms = maskSensitiveText(message),
                confidenceScore = 0,
                needsReview = true,
                rejectReason = "Message contains sensitive OTP/PIN/Authentication content and was rejected."
            )
        }

        val provider = detectProvider(sender, message)
        val maskedSms = maskSensitiveText(message)

        // Extract Transaction ID
        // Examples: TrxID 9K34M821L, TrxID: 9X8Y7Z, Txn: TXN12345, Trx ID: B7A32K, লেনদেন আইডি: 9K34M
        val txnIdPatterns = listOf(
            Pattern.compile("(?:TrxID|Trx ID|TxnId|Txn ID|Trans ID|TXN|লেনদেন আইডি)[:\\s]+([A-Za-z0-9_]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("TrxID\\s*([A-Za-z0-9_]{6,20})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Txn|Trx)\\s*#?\\s*([A-Za-z0-9]{6,20})", Pattern.CASE_INSENSITIVE)
        )

        var extractedTxnId = ""
        for (pattern in txnIdPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                extractedTxnId = matcher.group(1)?.trim() ?: ""
                if (extractedTxnId.isNotBlank()) break
            }
        }

        // Extract Balance first to avoid collision with received payment amount
        var balance: Double? = null
        val balancePatterns = listOf(
            Pattern.compile("(?:Balance|Current Balance|New Balance|ব্যালেন্স)[:\\s]+(?:Tk|BDT|৳)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Tk|BDT|৳)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:is your balance)", Pattern.CASE_INSENSITIVE)
        )
        for (bp in balancePatterns) {
            val m = bp.matcher(message)
            if (m.find()) {
                val rawVal = m.group(1)?.replace(",", "")
                balance = rawVal?.toDoubleOrNull()
                break
            }
        }

        // Extract Payment Amount (Payment received, Received Tk, You have received, etc.)
        // English and Bengali patterns
        val amountPatterns = listOf(
            Pattern.compile("(?:received|Payment received|Paid|Cash In|received amount|টাকা গ্রহণ|পেমেন্ট)[:\\s]*(?:Tk|BDT|৳)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Tk|BDT|৳)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:received|from|received from|পেমেন্ট পেয়েছেন)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Tk|BDT|৳)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        )

        var extractedAmount = 0.0
        for (ap in amountPatterns) {
            val m = ap.matcher(message)
            while (m.find()) {
                val candidateStr = m.group(1)?.replace(",", "")
                val candidateVal = candidateStr?.toDoubleOrNull() ?: 0.0
                // Crucial rule: Do not treat balance as payment amount!
                if (candidateVal > 0.0 && candidateVal != balance) {
                    extractedAmount = candidateVal
                    break
                }
            }
            if (extractedAmount > 0.0) break
        }

        // Extract Customer Reference or Sender Phone
        // Examples: from 01712345678, Ref: INV-99, Reference: 018...
        var customerRef = ""
        val refPatterns = listOf(
            Pattern.compile("(?:from|হতে|প্রেরক)[:\\s]+([0-9+]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Ref|Reference|রেফারেন্স)[:\\s]+([A-Za-z0-9_\\-]+)", Pattern.CASE_INSENSITIVE)
        )
        for (rp in refPatterns) {
            val m = rp.matcher(message)
            if (m.find()) {
                val foundRef = m.group(1)?.trim() ?: ""
                customerRef = maskCustomerNumber(foundRef)
                break
            }
        }

        val tempResult = ParsedTransactionResult(
            isValid = extractedTxnId.isNotBlank() && extractedAmount > 0.0,
            provider = provider,
            senderId = sender,
            transactionId = extractedTxnId,
            amount = extractedAmount,
            currency = "BDT",
            customerReference = customerRef,
            balance = balance,
            maskedOriginalSms = maskedSms
        )

        val confidence = calculateConfidence(tempResult, message)
        val needsReview = confidence < 80 || extractedTxnId.isBlank() || extractedAmount <= 0.0

        return tempResult.copy(
            confidenceScore = confidence,
            needsReview = needsReview,
            rejectReason = if (extractedAmount <= 0.0) "Could not identify valid payment amount." else null
        )
    }

    /**
     * Computes a reliable confidence score (0-100) based on parsing certainty.
     */
    fun calculateConfidence(parsed: ParsedTransactionResult, rawText: String): Int {
        var score = 0
        if (parsed.transactionId.isNotBlank() && parsed.transactionId.length >= 6) {
            score += 40
        }
        if (parsed.amount > 0.0) {
            score += 30
        }
        if (parsed.provider != "Unknown" && parsed.senderId.isNotBlank()) {
            score += 15
        }
        if (parsed.customerReference.isNotBlank() || rawText.contains("from", ignoreCase = true) || rawText.contains("হতে")) {
            score += 15
        }

        // Deductions
        if (parsed.balance != null && parsed.amount == parsed.balance) {
            // Suspicious collision between balance and amount
            score -= 30
        }
        return score.coerceIn(0, 100)
    }

    /**
     * Generates a unique SHA-256 hash for duplicate transaction detection.
     */
    fun generateTransactionHash(provider: String, transactionId: String, amount: Double): String {
        val input = "$provider:$transactionId:${String.format(Locale.US, "%.2f", amount)}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Checks if a transaction has already been registered locally to prevent duplicate transmissions.
     */
    fun isDuplicateTransaction(
        txnId: String,
        hash: String,
        existingTxns: List<TransactionEntity>
    ): Boolean {
        if (txnId.isBlank() && hash.isBlank()) return false
        return existingTxns.any { existing ->
            (existing.transactionId.isNotBlank() && existing.transactionId.equals(txnId, ignoreCase = true)) ||
            (existing.transactionHash.isNotBlank() && existing.transactionHash == hash)
        }
    }

    /**
     * Builds safe, sanitized JSON payload for HTTPS API reconciliation.
     * Contains only non-sensitive merchant payment fields.
     */
    fun buildJsonPayload(tx: TransactionEntity, settings: ApiSettings): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val receivedIso = isoFormat.format(Date(tx.timestamp))

        return """
        {
          "merchant_id": "${escapeJson(settings.merchantId)}",
          "provider": "${escapeJson(tx.provider.lowercase(Locale.ROOT))}",
          "transaction_id": "${escapeJson(tx.transactionId)}",
          "amount": ${String.format(Locale.US, "%.2f", tx.amount)},
          "currency": "${escapeJson(tx.currency)}",
          "reference": "${escapeJson(tx.customerReference)}",
          "received_at": "$receivedIso",
          "source": "merchant_sms_review",
          "device_id": "${escapeJson(settings.deviceId)}"
        }
        """.trimIndent()
    }

    /**
     * Transmits the reviewed transaction to the merchant's configured HTTPS API.
     * Strictly rejects HTTP endpoints. Uses configured timeout and auth header.
     */
    fun sendTransactionToApi(
        tx: TransactionEntity,
        settings: ApiSettings,
        secret: String
    ): ApiResult {
        val urlString = settings.apiUrl.trim()

        // Strict HTTPS enforcement
        if (!urlString.startsWith("https://", ignoreCase = true)) {
            return ApiResult(
                isSuccess = false,
                httpCode = 0,
                responseBody = null,
                errorMessage = "Security Violation: Only secure HTTPS endpoints are allowed. HTTP is strictly rejected.",
                destinationDomain = extractDomain(urlString)
            )
        }

        val domain = extractDomain(urlString)
        val client = OkHttpClient.Builder()
            .connectTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()

        val jsonPayload = buildJsonPayload(tx, settings)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(urlString)
            .post(body)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        // Add configured Authorization header
        if (secret.isNotBlank()) {
            val headerVal = if (settings.authType == "Bearer") "Bearer $secret" else secret
            requestBuilder.header(settings.authHeaderName, headerVal)
        }

        return try {
            val response = client.newCall(requestBuilder.build()).execute()
            val code = response.code
            val respBody = response.body?.string()

            ApiResult(
                isSuccess = response.isSuccessful,
                httpCode = code,
                responseBody = respBody,
                errorMessage = if (response.isSuccessful) null else "Server returned HTTP status $code",
                destinationDomain = domain
            )
        } catch (e: IOException) {
            ApiResult(
                isSuccess = false,
                httpCode = 0,
                responseBody = null,
                errorMessage = "Network connection failed: ${e.message}",
                destinationDomain = domain
            )
        } catch (e: Exception) {
            ApiResult(
                isSuccess = false,
                httpCode = 0,
                responseBody = null,
                errorMessage = "API Error: ${e.message}",
                destinationDomain = domain
            )
        }
    }

    /**
     * Saves or updates a parsed transaction in the local Room database with audit logging.
     */
    suspend fun savePendingTransaction(db: AppDatabase, tx: TransactionEntity): Long {
        val rowId = db.transactionDao().insertTransaction(tx)
        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "TRANSACTION_SAVED",
                details = "Saved Txn: ${tx.transactionId} (${tx.provider}, Tk ${tx.amount}) with status: ${tx.status}"
            )
        )
        return rowId
    }

    fun detectProvider(sender: String, message: String): String {
        val s = sender.lowercase(Locale.ROOT)
        val m = message.lowercase(Locale.ROOT)
        return when {
            s.contains("bkash") || m.contains("bkash") || m.contains("বিকাশ") -> "bKash"
            s.contains("nagad") || m.contains("nagad") || m.contains("নগদ") -> "Nagad"
            s.contains("16216") || s.contains("rocket") || m.contains("rocket") || m.contains("রকেট") -> "Rocket"
            s.contains("upay") || m.contains("upay") || m.contains("উপায়") -> "Upay"
            else -> if (sender.isNotBlank()) sender.uppercase(Locale.ROOT) else "Payment"
        }
    }

    fun maskCustomerNumber(raw: String): String {
        val digitsOnly = raw.filter { it.isDigit() }
        return if (digitsOnly.length >= 10) {
            val prefix = digitsOnly.take(3)
            val suffix = digitsOnly.takeLast(3)
            "$prefix****$suffix"
        } else {
            raw
        }
    }

    fun maskSensitiveText(message: String): String {
        // Redacts any 4-6 digit sequences resembling potential PINs or confidential tokens
        return message.replace(Regex("\\b(\\d{4,6})\\b")) { match ->
            val value = match.value
            if (value.length <= 4) "****" else "${value.take(2)}***"
        }
    }

    private fun extractDomain(urlStr: String): String {
        return try {
            URL(urlStr).host
        } catch (_: Exception) {
            "invalid-url"
        }
    }

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
}
